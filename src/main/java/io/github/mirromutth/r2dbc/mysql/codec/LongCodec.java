/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mirromutth.r2dbc.mysql.codec;

import io.github.mirromutth.r2dbc.mysql.constant.ColumnDefinitions;
import io.github.mirromutth.r2dbc.mysql.constant.DataType;
import io.github.mirromutth.r2dbc.mysql.internal.MySqlSession;
import io.github.mirromutth.r2dbc.mysql.message.FieldValue;
import io.github.mirromutth.r2dbc.mysql.message.NormalFieldValue;
import io.github.mirromutth.r2dbc.mysql.message.ParameterValue;
import io.github.mirromutth.r2dbc.mysql.message.client.ParameterWriter;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;

/**
 * Codec for {@link long}.
 */
final class LongCodec implements PrimitiveCodec<Long> {

    static final LongCodec INSTANCE = new LongCodec();

    private LongCodec() {
    }

    @Override
    public Long decodeText(NormalFieldValue value, FieldInformation info, Class<? super Long> target, MySqlSession session) {
        // Note: no check overflow for BIGINT UNSIGNED
        return parse(value.getBuffer());
    }

    @Override
    public Long decodeBinary(NormalFieldValue value, FieldInformation info, Class<? super Long> target, MySqlSession session) {
        ByteBuf buf = value.getBuffer();
        boolean isUnsigned = (info.getDefinitions() & ColumnDefinitions.UNSIGNED) != 0;

        switch (info.getType()) {
            case BIGINT:
                // Note: no check overflow for BIGINT UNSIGNED
                return buf.readLongLE();
            case INT:
                if (isUnsigned) {
                    return buf.readUnsignedIntLE();
                } else {
                    return (long) buf.readIntLE();
                }
            case MEDIUMINT:
                // Note: MySQL return 32-bits two's complement for 24-bits integer
                return (long) buf.readIntLE();
            case SMALLINT:
                if (isUnsigned) {
                    return (long) buf.readUnsignedShortLE();
                } else {
                    return (long) buf.readShortLE();
                }
            case YEAR:
                return (long) buf.readShortLE();
            default: // TINYINT
                if (isUnsigned) {
                    return (long) buf.readUnsignedByte();
                } else {
                    return (long) buf.readByte();
                }
        }
    }

    @Override
    public boolean canDecode(FieldValue value, FieldInformation info, Type target) {
        if (!TypeConditions.isInt(info.getType()) || !(value instanceof NormalFieldValue) || !(target instanceof Class<?>)) {
            return false;
        }

        // Here is a special condition. In the application scenario, many times programmers define
        // BIGINT UNSIGNED usually for make sure the ID is not negative, in fact they just use 63-bits.
        // If users force the requirement to convert BIGINT UNSIGNED to Long, should allow this behavior
        // for better performance (BigInteger is obviously slower than long).
        if (DataType.BIGINT == info.getType() && (info.getDefinitions() & ColumnDefinitions.UNSIGNED) != 0) {
            return Long.class == target;
        } else {
            return ((Class<?>) target).isAssignableFrom(Long.class);
        }
    }

    @Override
    public boolean canEncode(Object value) {
        return value instanceof Long;
    }

    @Override
    public ParameterValue encode(Object value, MySqlSession session) {
        return encodeOfLong((Long) value);
    }

    @Override
    public boolean canPrimitiveDecode(FieldInformation info) {
        // Here is a special condition. see `canDecode`.
        return TypeConditions.isInt(info.getType());
    }

    @Override
    public Class<Long> getPrimitiveClass() {
        return Long.TYPE;
    }

    /**
     * Fast parse a negotiable integer from {@link ByteBuf} without copy.
     *
     * @param buf a {@link ByteBuf} include an integer that maybe has sign.
     * @return an integer from {@code buf}.
     */
    static long parse(ByteBuf buf) {
        long value = 0;
        int first = buf.readByte();
        final boolean isNegative;

        if (first == '-') {
            isNegative = true;
        } else if (first >= '0' && first <= '9') {
            isNegative = false;
            value = (long) (first - '0');
        } else {
            // must be '+'
            isNegative = false;
        }

        while (buf.isReadable()) {
            value = value * 10L + (buf.readByte() - '0');
        }

        if (isNegative) {
            return -value;
        } else {
            return value;
        }
    }

    static ParameterValue encodeOfLong(long value) {
        return new LongValue(value);
    }

    private static final class LongValue extends AbstractParameterValue {

        private final long value;

        private LongValue(long value) {
            this.value = value;
        }

        @Override
        public Mono<Void> writeTo(ParameterWriter writer) {
            return Mono.fromRunnable(() -> writer.writeLong(value));
        }

        @Override
        public int getNativeType() {
            return DataType.BIGINT.getType();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LongValue)) {
                return false;
            }

            LongValue longValue = (LongValue) o;

            return value == longValue.value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }
    }
}
