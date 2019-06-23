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

import io.github.mirromutth.r2dbc.mysql.collation.CharCollation;
import reactor.util.annotation.Nullable;

/**
 * An implementation of {@link MySqlAuthProvider} for type "mysql_native_password".
 */
final class MySqlNativeAuthProvider implements MySqlAuthProvider {

    static final String TYPE = "mysql_native_password";

    static final MySqlNativeAuthProvider INSTANCE = new MySqlNativeAuthProvider();

    private static final String ALGORITHM = "SHA-1";

    private static final boolean IS_LEFT_SALT = true;

    private MySqlNativeAuthProvider() {
    }

    @Override
    public boolean isSslNecessary() {
        return false;
    }

    /**
     * SHA1(password) all bytes xor SHA1( "random data from MySQL server" + SHA1(SHA1(password)) )
     * <p>
     * {@inheritDoc}
     */
    @Override
    public byte[] fastAuthPhase(@Nullable CharSequence password, @Nullable byte[] salt, CharCollation collation) {
        return AuthHelper.defaultFastAuthPhase(ALGORITHM, IS_LEFT_SALT, password, salt, collation);
    }

    @Override
    public byte[] fullAuthPhase(@Nullable CharSequence password, CharCollation collation) {
        // "mysql_native_password" not support full authentication.
        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
