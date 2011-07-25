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

package us.harward.commons.net.mom.pubsub;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

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

    private static final Logger            logger = LoggerFactory.getLogger(PubSubClient.class);
    private static final Random            RANDOM = new SecureRandom();

    private final InetSocketAddress[]      servers;
    private final ClientMessageHandler     handler;

    private final ChannelFactory           factory;
    private final ClientBootstrap          bootstrap;
    private AtomicReference<ChannelFuture> activeChannelFuture;

    public PubSubClient(final ExecutorService service, final InetSocketAddress... servers) {
        this(service, null, servers);
    }

    public PubSubClient(final ExecutorService service, final NetworkConnectionLifecycleCallback lifecycleCallback,
            final InetSocketAddress... servers) {
        Preconditions.checkNotNull(service, "ExecutorService cannot be null");
        Preconditions.checkNotNull(servers, "Must give at least one server address to connect to");
        this.servers = servers;
        activeChannelFuture = new AtomicReference<ChannelFuture>(null);
        handler = new ClientMessageHandler(service);
        factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() {
                return Channels.pipeline(new SimpleChannelUpstreamHandler() {

                    @Override
                    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
                        logger.info("Connection established to {}", e.getChannel().getRemoteAddress());
                        if (lifecycleCallback != null)
                            lifecycleCallback.connectionUp(e.getChannel().getRemoteAddress());
                        super.channelConnected(ctx, e);
                    }

                    @Override
                    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
                        activeChannelFuture.set(null);
                        logger.info("Connection to {} lost", e.getChannel().getRemoteAddress());
                        PubSubClient.this.start();
                        if (lifecycleCallback != null)
                            lifecycleCallback.connectionDown(e.getChannel().getRemoteAddress());
                        super.channelDisconnected(ctx, e);
                    }

                }, MessageCodec.decoder(), MessageCodec.encoder(), handler);
            }

        });
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
    }

    public void start() {
        if (activeChannelFuture.get() != null)
            return;
        activeChannelFuture.set(bootstrap.connect(servers[RANDOM.nextInt(servers.length)]));
    }

    public void stop() throws InterruptedException {
        ChannelFuture cf = activeChannelFuture.getAndSet(null);
        if (cf != null) {
            cf = cf.getChannel().close();
            try {
                cf.await();
            } finally {
                factory.releaseExternalResources();
            }
            logger.info("Client shut down");
        }
    }

    void subscribe(final String topic, final MessageCallback... callbacks) {
        handler.subscribe(topic, callbacks);
    }

    void unsubscribe(final String topic, final MessageCallback... callbacks) {
        handler.unsubscribe(topic, callbacks);
    }

    Future<Boolean> publish(final byte[] message, final String topic) {
        return publish(ByteBuffer.wrap(message), topic);
    }

    Future<Boolean> publish(final byte[] message, final int offset, final int length, final String topic) {
        return publish(ByteBuffer.wrap(message, offset, length), topic);
    }

    Future<Boolean> publish(final ByteBuffer message, final String topic) {
        Preconditions.checkNotNull(message, "Message can be empty but not null");
        Preconditions.checkNotNull(topic, "Topic can be empty but not null");
        final ChannelFuture cf = activeChannelFuture.get();
        final Channel channel = cf != null ? cf.getChannel() : null;
        final Future<Boolean> rv;
        if (channel != null) {
            rv = new NettyToJDKFuture(channel.write(new ApplicationMessage(message, topic)));
        } else
            rv = NettyToJDKFuture.WRITE_FAILED;
        return rv;
    }

}
