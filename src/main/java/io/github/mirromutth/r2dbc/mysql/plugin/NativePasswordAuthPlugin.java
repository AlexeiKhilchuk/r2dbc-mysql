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

package io.github.mirromutth.r2dbc.mysql.plugin;

import io.github.mirromutth.r2dbc.mysql.constant.AuthType;
import reactor.util.annotation.Nullable;

import java.security.MessageDigest;

import static io.github.mirromutth.r2dbc.mysql.util.EmptyArrays.EMPTY_BYTES;
import static io.github.mirromutth.r2dbc.mysql.util.AssertUtils.requireNonNull;

/**
 * MySQL Authentication Plugin for "mysql_native_password"
 */
public final class NativePasswordAuthPlugin extends AbstractAuthPlugin {

    private static final NativePasswordAuthPlugin INSTANCE = new NativePasswordAuthPlugin();

    public static NativePasswordAuthPlugin getInstance() {
        return INSTANCE;
    }

    private NativePasswordAuthPlugin() {
    }

    @Override
    public AuthType getType() {
        return AuthType.MYSQL_NATIVE_PASSWORD;
    }

    /**
     * SHA1(password) all bytes xor SHA1( "random data from MySQL server" + SHA1(SHA1(password)) )
     *
     * @param password plaintext password
     * @param scramble random scramble from MySQL server
     * @return encrypted authentication if password is not null, otherwise empty byte array.
     */
    @Override
    public byte[] encrypt(@Nullable byte[] password, byte[] scramble) {
        if (password == null) {
            return EMPTY_BYTES;
        }

        requireNonNull(scramble, "scramble must not be null");

        MessageDigest digest = loadDigest("SHA-1");
        byte[] oneRound = finalDigests(digest, password);
        byte[] twoRounds = finalDigests(digest, oneRound);

        return allBytesXor(finalDigests(digest, scramble, twoRounds), oneRound);
    }
}
