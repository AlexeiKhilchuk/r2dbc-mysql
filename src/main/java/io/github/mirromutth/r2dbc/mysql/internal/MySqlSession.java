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

package io.github.mirromutth.r2dbc.mysql.internal;

import io.github.mirromutth.r2dbc.mysql.ServerVersion;
import io.github.mirromutth.r2dbc.mysql.collation.CharCollation;
import io.github.mirromutth.r2dbc.mysql.constant.AuthTypes;
import io.github.mirromutth.r2dbc.mysql.constant.ZeroDateOption;
import io.github.mirromutth.r2dbc.mysql.authentication.MySqlAuthProvider;
import reactor.util.annotation.Nullable;

import static io.github.mirromutth.r2dbc.mysql.constant.EmptyArrays.EMPTY_BYTES;
import static io.github.mirromutth.r2dbc.mysql.internal.AssertUtils.requireNonNull;

/**
 * The MySQL session considers the behavior of server or client.
 * <p>
 * WARNING: It is internal data structure, do NOT use it outer than {@literal r2dbc-mysql},
 * try configure {@code ConnectionFactoryOptions} or {@code MySqlConnectionConfiguration}
 * to control session and client behavior.
 */
public final class MySqlSession {

    private volatile int connectionId = -1;

    private volatile ServerVersion serverVersion = ServerVersion.NONE;

    private final String database;

    private final ZeroDateOption zeroDateOption;

    /**
     * Client character collation.
     */
    private final CharCollation collation = CharCollation.clientCharCollation();

    private volatile int capabilities = 0;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile MySqlAuthProvider authProvider;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile String username;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile CharSequence password;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile byte[] salt;

    public MySqlSession(String database, ZeroDateOption zeroDateOption, String username, @Nullable CharSequence password) {
        this.database = requireNonNull(database, "database must not be null");
        this.zeroDateOption = requireNonNull(zeroDateOption, "zeroDateOption must not be null");
        this.username = requireNonNull(username, "username must not be null");
        this.password = password;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public ServerVersion getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
    }

    public CharCollation getCollation() {
        return collation;
    }

    public String getDatabase() {
        return database;
    }

    public ZeroDateOption getZeroDateOption() {
        return zeroDateOption;
    }

    public int getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(int capabilities) {
        this.capabilities = capabilities;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public CharSequence getPassword() {
        return password;
    }

    @Nullable
    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(@Nullable byte[] salt) {
        this.salt = salt;
    }

    public String getAuthType() {
        MySqlAuthProvider machine = this.authProvider;

        if (machine == null) {
            return AuthTypes.NO_AUTH_PROVIDER;
        }

        return machine.getType();
    }

    public void setAuthProvider(MySqlAuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    /**
     * Generate an authorization for fast authentication phase.
     */
    public byte[] fastPhaseAuthorization() {
        MySqlAuthProvider authProvider = this.authProvider;

        if (authProvider == null) {
            return EMPTY_BYTES;
        }

        return authProvider.fastAuthPhase(password, salt, collation);
    }

    /**
     * All authentication data should be remove when connection phase completed or client closed in connection phase.
     */
    public void clearAuthentication() {
        this.username = null;
        this.password = null;
        this.salt = null;
        this.authProvider = null;
    }
}
