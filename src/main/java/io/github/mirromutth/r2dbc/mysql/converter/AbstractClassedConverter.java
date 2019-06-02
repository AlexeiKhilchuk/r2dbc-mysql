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

import java.lang.reflect.Type;

/**
 * Converter for classed type without generic type, like {@link String} or {@link String[]}.
 */
abstract class AbstractClassedConverter<T> implements Converter<T, Class<? super T>> {

    private final Class<? extends T> clazz;

    AbstractClassedConverter(Class<? extends T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public final boolean canRead(ColumnType type, short definitions, int precision, int collationId, Type target, MySqlSession session) {
        if (!(target instanceof Class<?>)) {
            return false;
        }

        return ((Class<?>) target).isAssignableFrom(clazz) && doCanRead(type, definitions);
    }

    abstract boolean doCanRead(ColumnType type, short definitions);
}
