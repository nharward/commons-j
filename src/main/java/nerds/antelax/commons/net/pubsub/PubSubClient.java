// Copyright 2011 Nathaniel Harward
//
// This file is part of commons-j.
//
// commons-j is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// commons-j is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with commons-j. If not, see <http://www.gnu.org/licenses/>.

package nerds.antelax.commons.net.pubsub;

import static nerds.antelax.commons.util.Conversions.asArray;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import nerds.antelax.commons.net.netty.UpstreamMessageFilteringHandler;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public final class PubSubClient {

    /**
     * Application callback interface, subscribers to a topic must implement this interface to receive messages.
     */
    public static interface MessageCallback {

        void onMessage(final ByteBuffer message) throws Exception;

    }

    /**
     * Network connection lifecycle callback, applications can optionally implement this interface to take action(s) when a network
     * connection goes up/down.
     */
    public static interface NetworkConnectionLifecycleCallback {

        void connectionUp(final SocketAddress endpoint);

        void connectionDown(final SocketAddress endpoint);

    }

    private static final Logger              logger                  = LoggerFactory.getLogger(PubSubClient.class);

    public static final int                  DEFAULT_TIMEOUT_SECONDS = 5;

    private final ClientMessageHandler       clientHandler;
    private final RoundRobinReconnectHandler reconnectHandler;

    private final ChannelFactory             factory;
    private final ClientBootstrap            bootstrap;

    public PubSubClient(final ExecutorService service, final Collection<InetSocketAddress> servers) {
        this(service, null, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS, servers);
    }

    public PubSubClient(final ExecutorService service, final NetworkConnectionLifecycleCallback lifecycleCallback,
            final Collection<InetSocketAddress> servers) {
        this(service, lifecycleCallback, DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS, servers);
    }

    public PubSubClient(final ExecutorService service, final int retryDelay, final TimeUnit retryUnits,
            final Collection<InetSocketAddress> servers) {
        this(service, null, retryDelay, retryUnits, servers);
    }

    public PubSubClient(final ExecutorService service, final NetworkConnectionLifecycleCallback lifecycleCallback,
            final int retryDelay, final TimeUnit retryUnits, final Collection<InetSocketAddress> servers) {
        this(null, null, service, lifecycleCallback, retryDelay, retryUnits, servers);
    }

    /*
     * For use in {@link PubSubServer} only, which needs to filter/intercept incoming messages from other servers directly to access
     * source/server IDs on messages and prevent endless server-to-server routing loops.
     */
    PubSubClient(final ChannelHandler incomingInterceptor, final Predicate<Object> incomingFilter, final ExecutorService service,
            final NetworkConnectionLifecycleCallback lifecycleCallback, final int retryDelay, final TimeUnit retryUnits,
            final Collection<InetSocketAddress> servers) {
        Preconditions.checkArgument((incomingInterceptor == null && incomingFilter == null)
                || (incomingInterceptor != null && incomingFilter != null));
        Preconditions.checkNotNull(service, "ExecutorService cannot be null");
        Preconditions.checkNotNull(servers, "Must give at least one server address to connect to");
        clientHandler = new ClientMessageHandler(service);
        factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        bootstrap = new ClientBootstrap(factory);
        reconnectHandler = new RoundRobinReconnectHandler(bootstrap, retryDelay, retryUnits, lifecycleCallback, servers);
        final UpstreamMessageFilteringHandler filteringHandler = incomingFilter != null ? new UpstreamMessageFilteringHandler(
                incomingFilter) : null;
        final UUID ourSourceID = UUID.randomUUID();
        logger.info("New client created with ID: {}", ourSourceID);
        final ChannelDownstreamHandler uuidPopulatingHandler = new SimpleChannelDownstreamHandler() {

            @Override
            public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
                final Object o = e.getMessage();
                if (o instanceof Message) {
                    final Message m = (Message) o;
                    if (m.sourceID() == null || m.sourceID().equals(Message.NO_UUID))
                        m.sourceID(ourSourceID);
                }
                super.writeRequested(ctx, e);
            }

        };
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                if (incomingInterceptor != null && filteringHandler != null)
                    return Channels.pipeline(reconnectHandler, MessageCodec.decoder(), MessageCodec.encoder(),
                            uuidPopulatingHandler, filteringHandler, incomingInterceptor, clientHandler);
                else
                    return Channels.pipeline(reconnectHandler, MessageCodec.decoder(), MessageCodec.encoder(),
                            uuidPopulatingHandler, clientHandler);
            }

        });
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        logger.trace(
                "New pub/sub client created w/ intercepting handler[{}], incoming filter[{}], lifecycle callback[{}], retry delay[{} {}], servers[{}]",
                asArray(incomingInterceptor, incomingFilter, lifecycleCallback, retryDelay, retryUnits, servers));
    }

    public void start() {
        logger.trace("Starting re-connect handler");
        reconnectHandler.enable();
    }

    public void stop() throws InterruptedException {
        logger.trace("Disabling/shutting down re-connect handler and releasing resources");
        reconnectHandler.disable();
        reconnectHandler.shutdown();
        factory.releaseExternalResources();
    }

    public void subscribe(final String topic, final MessageCallback... callbacks) {
        logger.trace("Subscribing callbacks [{}] for topic[{}]", asArray(callbacks, topic));
        clientHandler.subscribe(topic, callbacks);
    }

    public void unsubscribe(final String topic, final MessageCallback... callbacks) {
        logger.trace("Unsubscribing callbacks [{}] for topic[{}]", asArray(callbacks, topic));
        clientHandler.unsubscribe(topic, callbacks);
    }

    public Future<Boolean> publish(final byte[] message, final String topic) {
        return publish(ByteBuffer.wrap(message), topic);
    }

    public Future<Boolean> publish(final byte[] message, final int offset, final int length, final String topic) {
        return publish(ByteBuffer.wrap(message, offset, length), topic);
    }

    public Future<Boolean> publish(final ByteBuffer message, final String topic) {
        Preconditions.checkNotNull(message, "Message can be empty but not null");
        Preconditions.checkNotNull(topic, "Topic can be empty but not null");
        final Channel channel = reconnectHandler.channel();
        logger.trace(channel != null ? "Publishing full message of length {} to topic[{}]"
                : "Not currently connected to a server, dropping message of length {} to topic[{}]",
                asArray(message.remaining(), topic));
        return channel != null ? new NettyToJDKFuture(channel.write(new ApplicationMessage(message, topic)))
                : NettyToJDKFuture.WRITE_FAILED;
    }

}
