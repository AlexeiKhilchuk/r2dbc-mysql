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

package io.github.mirromutth.r2dbc.mysql.converter;

import io.github.mirromutth.r2dbc.mysql.constant.ColumnType;
import io.github.mirromutth.r2dbc.mysql.internal.MySqlSession;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Converter for {@link float}.
 */
final class FloatConverter extends AbstractPrimitiveConverter<Float> {

    static final FloatConverter INSTANCE = new FloatConverter();

    private FloatConverter() {
        super(Float.TYPE, Float.class);
    }

    @Override
    public Float read(ByteBuf buf, short definitions, int precision, int collationId, Class<? super Float> target, MySqlSession session) {
        return Float.parseFloat(buf.toString(StandardCharsets.US_ASCII));
    }

    @Override
    boolean doCanRead(ColumnType type, short definitions, int precision) {
        return ColumnType.FLOAT == type || (precision < 7 && BigDecimalConverter.DECIMAL_TYPES.contains(type));
    }
}
