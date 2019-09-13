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

package dev.miku.r2dbc.mysql;

import org.junit.jupiter.api.Test;

/**
 * An implementation of {@link PrepareQueryIntegrationTestSupport} for data integration in MySQL 5.7 prepared statement.
 */
final class MySql57PrepareQueryIntegrationTest extends PrepareQueryIntegrationTestSupport {

    MySql57PrepareQueryIntegrationTest() {
        super(MySql57TestKit.CONFIGURATION);
    }

    @Test
    @Override
    void json() {
        testJson(null, "{\"data\": 1}", "[\"data\", 1]", "1");
    }
}
