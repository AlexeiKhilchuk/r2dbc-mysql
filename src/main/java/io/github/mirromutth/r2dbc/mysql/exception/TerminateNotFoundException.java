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

package io.github.mirromutth.r2dbc.mysql.exception;

import io.r2dbc.spi.R2dbcException;

/**
 * Client want get a string that is terminated by a 0x00 byte,
 * but 0x00 byte not found.
 */
public final class TerminateNotFoundException extends R2dbcException {

    public TerminateNotFoundException() {
        super("Terminate 0x00 byte not found");
    }
}
