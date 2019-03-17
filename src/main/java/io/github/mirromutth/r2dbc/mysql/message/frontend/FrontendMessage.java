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

package io.github.mirromutth.r2dbc.mysql.message.frontend;

import io.github.mirromutth.r2dbc.mysql.core.MySqlSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A message sent from a frontend client to a backend server.
 */
public interface FrontendMessage {

    /**
     * Encode to single or multi {@link ByteBuf}.
     *
     * @param bufAllocator connection's {@link ByteBufAllocator}
     * @param sequenceId   message sequence id
     * @param session      current MySQL session
     * @return should be instance of {@code Mono<ByteBuf>} or {@code Flux<ByteBuf>} usually.
     */
    Flux<ByteBuf> encode(ByteBufAllocator bufAllocator, AtomicInteger sequenceId, MySqlSession session);

    /**
     * Most frontend messages can be exchanged for backend messages, but not all.
     * Like {@link ExitMessage} or
     *
     * @return {@code true} if this front end message can be exchanged for backend messages.
     */
    boolean isExchanged();
}
