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

package io.github.mirromutth.r2dbc.mysql.util;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

import static io.github.mirromutth.r2dbc.mysql.constant.ProtocolConstants.TERMINAL;
import static io.github.mirromutth.r2dbc.mysql.util.AssertUtils.requireNonNegative;
import static io.github.mirromutth.r2dbc.mysql.util.AssertUtils.requireNonNull;

/**
 * Common codec methods util.
 */
public final class CodecUtils {

    private CodecUtils() {
    }

    private static final int VAR_INT_1_BYTE_LIMIT = 250;
    private static final int VAR_INT_2_BYTE_LIMIT = (1 << (Byte.SIZE << 1)) - 1;
    private static final int VAR_INT_2_BYTE_CODE = 0xFC;
    private static final int VAR_INT_3_BYTE_LIMIT = (1 << (Byte.SIZE * 3)) - 1;
    private static final int VAR_INT_3_BYTE_CODE = 0xFD;
    private static final int VAR_INT_8_BYTE_CODE = 0xFE;

    /**
     * @param buf     C-style string readable buffer
     * @param charset the string characters' set
     * @return The string of C-style.
     */
    public static String readCString(ByteBuf buf, Charset charset) {
        requireNonNull(buf, "buf must not be null");
        requireNonNull(charset, "charset must not be null");

        String result = buf.readCharSequence(buf.bytesBefore(TERMINAL), charset).toString();
        buf.skipBytes(1);
        return result;
    }

    public static ByteBuf readCStringSlice(ByteBuf buf) {
        requireNonNull(buf, "buf must not be null");

        ByteBuf result = buf.readSlice(buf.bytesBefore(TERMINAL));
        buf.skipBytes(1);
        return result;
    }

    /**
     * @param buf     that want write to this {@link ByteBuf}
     * @param value   content that want write
     * @param charset {@code value} characters' set
     */
    public static void writeCString(ByteBuf buf, CharSequence value, Charset charset) {
        requireNonNull(buf, "buf must not be null");
        requireNonNull(value, "value must not be null");
        requireNonNull(charset, "charset must not be null");

        buf.writeCharSequence(value, charset);
        buf.writeByte(TERMINAL);
    }

    /**
     * Write MySQL var int to {@code buf} for 32-bits integer.
     * <p>
     * WARNING: it is MySQL var int (size encoded integer),
     * that is not like var int usually.
     *
     * @param buf that want write to this {@link ByteBuf}
     * @param value integer that want write
     */
    public static void writeVarInt(ByteBuf buf, int value) {
        requireNonNull(buf, "buf must not be null");
        requireNonNegative(value, "value must not be negative");

        // if it greater than 3 bytes limit, it must be 8 bytes integer, this is MySQL var int rule
        if (value > VAR_INT_3_BYTE_LIMIT) {
            buf.writeByte(VAR_INT_8_BYTE_CODE).writeLongLE(value);
        } else if (value > VAR_INT_2_BYTE_LIMIT) {
            buf.writeByte(VAR_INT_3_BYTE_CODE).writeMediumLE(value);
        } else if (value > VAR_INT_1_BYTE_LIMIT) {
            buf.writeByte(VAR_INT_2_BYTE_CODE).writeShortLE(value);
        } else {
            buf.writeByte(value);
        }
    }

    public static void writeVarIntSizedString(ByteBuf buf, CharSequence value, Charset charset) {
        int size = value.length();
        if (size > 0) {
            writeVarInt(buf, size);
            buf.writeCharSequence(value, charset);
        } else {
            buf.writeByte(0);
        }
    }

    public static void writeVarIntSizedBytes(ByteBuf buf, byte[] value) {
        int size = value.length;
        if (size > 0) {
            writeVarInt(buf, size);
            buf.writeBytes(value);
        } else {
            buf.writeByte(0);
        }
    }
}
