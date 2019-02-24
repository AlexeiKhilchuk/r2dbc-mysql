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

package io.github.mirromutth.r2dbc.mysql.core;

import io.github.mirromutth.r2dbc.mysql.collation.CharCollation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test cases for {@link CharCollation}
 */
class CharCollationTest {

    @Test
    void fromId() {
        assertNotNull(CharCollation.fromId(33)); // utf-8 general ci
        assertNotNull(CharCollation.fromId(224)); // utf-8 more 4-bytes unicode ci
        assertNotEquals(CharCollation.fromId(33), CharCollation.fromId(224));
    }
}
