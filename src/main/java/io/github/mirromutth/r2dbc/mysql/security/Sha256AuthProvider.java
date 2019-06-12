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

package io.github.mirromutth.r2dbc.mysql.security;

import io.github.mirromutth.r2dbc.mysql.internal.MySqlSession;
import reactor.util.annotation.NonNull;

import static io.github.mirromutth.r2dbc.mysql.internal.EmptyArrays.EMPTY_BYTES;
import static io.github.mirromutth.r2dbc.mysql.util.AssertUtils.requireNonNull;

/**
 * An implementation of {@link MySqlAuthProvider} for type "sha256_password".
 */
final class Sha256AuthProvider implements MySqlAuthProvider {

    static final String TYPE = "sha256_password";

    static final Sha256AuthProvider INSTANCE = new Sha256AuthProvider();

    private Sha256AuthProvider() {
    }

    @Override
    public boolean isSslNecessary() {
        return true;
    }

    @NonNull
    @Override
    public byte[] fastAuthPhase(MySqlSession session) {
        requireNonNull(session, "session must not be null");

        CharSequence password = session.getPassword();

        if (password == null || password.length() <= 0) {
            return EMPTY_BYTES;
        }

        // TODO: implement fast authentication

        return EMPTY_BYTES;
    }

    @Override
    public byte[] fullAuthPhase(MySqlSession session) {
        // TODO: implement full authentication

        return EMPTY_BYTES;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
