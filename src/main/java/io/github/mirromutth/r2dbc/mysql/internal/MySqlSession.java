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
import io.github.mirromutth.r2dbc.mysql.constant.ZeroDate;
import io.github.mirromutth.r2dbc.mysql.security.AuthStateMachine;
import reactor.util.annotation.Nullable;

import static io.github.mirromutth.r2dbc.mysql.util.AssertUtils.requireNonNull;

/**
 * It is internal util, do NOT use it outer than {@code r2dbc-mysql}, try using
 * {@code MySqlConnectConfiguration} to control session data and client behavior.
 * <p>
 * MySQL sessions.
 */
public final class MySqlSession {

    private volatile boolean useSsl;

    private volatile int connectionId = -1;

    private volatile ServerVersion serverVersion = ServerVersion.NONE;

    private volatile int serverCapabilities = 0;

    private volatile CharCollation collation = CharCollation.defaultCollation(ServerVersion.NONE);

    private final String database;

    private final ZeroDate zeroDate;

    private volatile int clientCapabilities = 0;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile AuthStateMachine authStateMachine;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile String username;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile String password;

    /**
     * It would be null after connection phase completed.
     */
    @Nullable
    private volatile byte[] salt;

    public MySqlSession(
        boolean useSsl,
        String database,
        ZeroDate zeroDate,
        String username,
        @Nullable String password
    ) {
        this.useSsl = useSsl;
        this.database = requireNonNull(database, "database must not be null");
        this.zeroDate = requireNonNull(zeroDate, "zeroDate must not be null");
        this.username = requireNonNull(username, "username must not be null");
        this.password = password;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
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

    public int getServerCapabilities() {
        return serverCapabilities;
    }

    public void setServerCapabilities(int serverCapabilities) {
        this.serverCapabilities = serverCapabilities;
    }

    public CharCollation getCollation() {
        return collation;
    }

    public void setCollation(CharCollation collation) {
        this.collation = collation;
    }

    public String getDatabase() {
        return database;
    }

    public ZeroDate getZeroDate() {
        return zeroDate;
    }

    public int getClientCapabilities() {
        return clientCapabilities;
    }

    public void setClientCapabilities(int clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(@Nullable byte[] salt) {
        this.salt = salt;
    }

    @Nullable
    public String getAuthType() {
        AuthStateMachine machine = this.authStateMachine;

        if (machine == null) {
            return null;
        }

        return machine.getType();
    }

    public void setAuthStateMachine(AuthStateMachine authStateMachine) {
        this.authStateMachine = authStateMachine;
    }

    public boolean hasNext() {
        AuthStateMachine authStateMachine = this.authStateMachine;

        if (authStateMachine == null) {
            return false;
        }

        return authStateMachine.hasNext();
    }

    /**
     * Generate current authentication and make changes to the authentication status.
     *
     * @return {@code null}
     */
    @Nullable
    public byte[] nextAuthentication() {
        AuthStateMachine authStateMachine = this.authStateMachine;

        if (authStateMachine == null) {
            return null;
        }

        return authStateMachine.nextAuthentication(this);
    }

    /**
     * All authentication data should be remove when connection phase completed or client closed in connection phase.
     */
    public void clearAuthentication() {
        this.username = null;
        this.password = null;
        this.salt = null;
        this.authStateMachine = null;
    }
}
