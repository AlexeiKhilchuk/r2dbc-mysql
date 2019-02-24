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

package io.github.mirromutth.r2dbc.mysql.client;

import io.github.mirromutth.r2dbc.mysql.config.ConnectProperties;
import io.github.mirromutth.r2dbc.mysql.message.backend.BackendMessage;
import io.github.mirromutth.r2dbc.mysql.message.frontend.FrontendMessage;
import io.github.mirromutth.r2dbc.mysql.core.ServerSession;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.TcpClient;
import reactor.util.concurrent.WaitStrategy;

import static io.github.mirromutth.r2dbc.mysql.util.AssertUtils.requireNonNull;

/**
 * An abstraction that wraps the networking part of exchanging methods.
 */
public interface Client {

    Flux<BackendMessage> exchange(Publisher<FrontendMessage> requests);

    Mono<Void> close();

    ServerSession getSession();
}
