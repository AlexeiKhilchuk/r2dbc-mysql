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

package io.github.mirromutth.r2dbc.mysql;

import io.r2dbc.spi.Connection;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base class considers data integration unit tests in prepare query for implementations of {@link IntegrationTestSupport}.
 */
abstract class PrepareQueryIntegrationTestSupport extends IntegrationTestSupport {

    PrepareQueryIntegrationTestSupport(MySqlConnectionConfiguration configuration) {
        super(configuration);
    }

    @Test
    @Override
    void varchar() {
        testType(String.class, "VARCHAR(50)", "", null, "data");
    }

    @Test
    @Override
    void date() {
        testType(LocalDate.class, "DATE", null, MIN_DATE, MAX_DATE);
    }

    @Test
    @Override
    void time() {
        testType(LocalTime.class, "TIME", null, MIN_TIME, MAX_TIME);
        testType(Duration.class, "TIME", null, MIN_DURATION, MAX_DURATION);
    }

    @Override
    Mono<Void> testTime(Connection connection, Duration origin, LocalTime time) {
        return Mono.from(connection.createStatement("INSERT INTO test VALUES(DEFAULT,?)")
            .bind(0, origin)
            .returnGeneratedValues("id")
            .execute())
            .flatMapMany(IntegrationTestSupport::extractId)
            .concatMap(id -> connection.createStatement("SELECT value FROM test WHERE id=?")
                .bind(0, id)
                .execute())
            .flatMap(r -> extractOptionalField(r, LocalTime.class))
            .map(Optional::get)
            .doOnNext(t -> assertEquals(t, time))
            .then(Mono.from(connection.createStatement("DELETE FROM test WHERE id>0")
                .execute()))
            .flatMap(IntegrationTestSupport::extractRowsUpdated)
            .then();
    }

    @Test
    @Override
    void dateTime() {
        testType(LocalDateTime.class, "DATETIME", null, MIN_DATE_TIME, MAX_DATE_TIME);
    }

    @Test
    @Override
    void timestamp() {
        // TIMESTAMP must not be null when database version less than 8.0
        testType(LocalDateTime.class, "TIMESTAMP", MIN_TIMESTAMP, MAX_TIMESTAMP);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    @Override
    protected final <T> void testType(Class<T> type, String defined, T... values) {
        // Should use simple statement for table definition.
        connectionFactory.create()
            .flatMap(connection -> Mono.from(connection.createStatement(String.format("CREATE TEMPORARY TABLE test(id INT PRIMARY KEY AUTO_INCREMENT,value %s)", defined))
                .execute())
                .flatMap(CompatibilityTestSupport::extractRowsUpdated)
                .thenMany(Flux.fromIterable(convertOptional(values)).concatMap(value -> testOne(connection, type, value.orElse(null))))
                .concatWith(close(connection))
                .then())
            .as(StepVerifier::create)
            .verifyComplete();
    }

    private static <T> List<Optional<T>> convertOptional(T[] values) {
        List<Optional<T>> optionals = new ArrayList<>(values.length);

        for (T value : values) {
            optionals.add(Optional.ofNullable(value));
        }

        return optionals;
    }

    private static <T> Mono<Void> testOne(MySqlConnection connection, Class<T> type, @Nullable T value) {
        MySqlStatement insert = connection.createStatement("INSERT INTO test VALUES(DEFAULT,?)");
        MySqlStatement valueSelect;

        if (value == null) {
            insert.bindNull(0, type);
            valueSelect = connection.createStatement("SELECT value FROM test WHERE value IS NULL");
        } else {
            insert.bind(0, value);
            valueSelect = connection.createStatement("SELECT value FROM test WHERE value=?")
                .bind(0, value);
        }

        return Mono.from(insert.returnGeneratedValues("id")
            .execute())
            .flatMap(result -> extractRowsUpdated(result)
                .doOnNext(u -> assertEquals(u, 1))
                .thenMany(extractId(result))
                .collectList()
                .map(ids -> {
                    assertEquals(1, ids.size());
                    return ids.get(0);
                }))
            .flatMap(id -> Mono.from(connection.createStatement("SELECT value FROM test WHERE id=?")
                .bind(0, id)
                .execute()))
            .flatMapMany(r -> extractOptionalField(r, type))
            .collectList()
            .doOnNext(data -> assertEquals(data, Collections.singletonList(Optional.ofNullable(value))))
            .then(Mono.from(valueSelect.execute()))
            .flatMapMany(r -> extractOptionalField(r, type))
            .collectList()
            .doOnNext(data -> assertEquals(data, Collections.singletonList(Optional.ofNullable(value))))
            .then(Mono.from(connection.createStatement("DELETE FROM test WHERE id>0").execute()))
            .flatMap(CompatibilityTestSupport::extractRowsUpdated)
            .doOnNext(u -> assertEquals(u, 1))
            .then();
    }
}
