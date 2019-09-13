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

package dev.miku.r2dbc.mysql.message.server;

import dev.miku.r2dbc.mysql.internal.AssertUtils;
import dev.miku.r2dbc.mysql.internal.CodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Change authentication plugin type and salt message.
 */
public final class AuthChangeMessage implements ServerMessage {

    private final String authType;

    private final byte[] salt;

    private AuthChangeMessage(String authType, byte[] salt) {
        this.authType = AssertUtils.requireNonNull(authType, "authType must not be null");
        this.salt = AssertUtils.requireNonNull(salt, "salt must not be null");
    }

    public String getAuthType() {
        return authType;
    }

    public byte[] getSalt() {
        return salt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthChangeMessage)) {
            return false;
        }

        AuthChangeMessage that = (AuthChangeMessage) o;

        if (!authType.equals(that.authType)) {
            return false;
        }
        return Arrays.equals(salt, that.salt);
    }

    @Override
    public int hashCode() {
        int result = authType.hashCode();
        result = 31 * result + Arrays.hashCode(salt);
        return result;
    }

    @Override
    public String toString() {
        return String.format("AuthChangeMessage{authType=%s, salt=%s}", authType, Arrays.toString(salt));
    }

    static AuthChangeMessage decode(ByteBuf buf) {
        String authType = CodecUtils.readCString(buf, StandardCharsets.US_ASCII);
        int bytes = buf.readableBytes();

        if (bytes > 0 && buf.getByte(buf.writerIndex() - 1) == 0) {
            // Remove last 0.
            return new AuthChangeMessage(authType, ByteBufUtil.getBytes(buf, buf.readerIndex(), bytes - 1));
        } else {
            return new AuthChangeMessage(authType, ByteBufUtil.getBytes(buf));
        }
    }
}
