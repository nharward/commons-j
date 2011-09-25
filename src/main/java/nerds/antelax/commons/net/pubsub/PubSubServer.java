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

import static nerds.antelax.commons.net.NetUtil.hostPortPairsFromString;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nerds.antelax.commons.net.NetUtil;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

public final class PubSubServer {

    public static final InetSocketAddress       DEFAULT_ADDRESS = new InetSocketAddress(6302);

    private static final Logger                 logger          = LoggerFactory.getLogger(PubSubServer.class);

    private final Collection<InetSocketAddress> listenAddresses;
    private final ExecutorService               bossService;
    private final ExecutorService               workerService;
    private final ChannelFactory                factory;
    private final ServerBootstrap               bootstrap;
    private final ChannelGroup                  openChannels;

    private final ServerMessageHandler          sharedMessageHandler;

    /**
     * Giving a null or empty argument starts a local server. If a non-null, non-empty argument is given then start a server,
     * listening to all local socket addresses in the argument. Any non-local socket addresses are considered remote peer servers.
     * 
     * @throws IllegalArgumentException
     *             if the {@link Collection} argument is not empty but contains no local addresses to bind to
     */
    public PubSubServer(final Collection<InetSocketAddress> clusterDefinition) {
        final Collection<InetSocketAddress> localAddrs = clusterDefinition != null ? Collections2.filter(clusterDefinition,
                NetUtil.machineLocalSocketAddress()) : new LinkedList<InetSocketAddress>();
        final Collection<InetSocketAddress> remoteAddrs = clusterDefinition != null ? Collections2.filter(clusterDefinition,
                Predicates.not(NetUtil.machineLocalSocketAddress())) : new LinkedList<InetSocketAddress>();
        if (clusterDefinition == null || clusterDefinition.isEmpty())
            localAddrs.add(DEFAULT_ADDRESS);
        else
            Preconditions.checkArgument(!localAddrs.isEmpty(),
                    "Attempt to start a server on a machine that is not part of the cluster definition");

        listenAddresses = Collections.unmodifiableCollection(localAddrs);
        openChannels = new DefaultChannelGroup(getClass().getName());
        bossService = Executors.newCachedThreadPool();
        workerService = Executors.newCachedThreadPool();
        factory = new NioServerSocketChannelFactory(bossService, workerService);
        bootstrap = new ServerBootstrap(factory);
        final UUID ourServerID = UUID.randomUUID();
        logger.info("New server created with ID: {}", ourServerID);
        sharedMessageHandler = new ServerMessageHandler(new Predicate<Object>() {

            @Override
            public boolean apply(final Object o) {
                if (o instanceof Message) {
                    final Message m = (Message) o;
                    return m.ttl() > 0 && !m.serverID().equals(ourServerID);
                } else
                    return true;
            }

        }, Collections.unmodifiableCollection(remoteAddrs));
        final ChannelDownstreamHandler uuidPopulatingHandler = new SimpleChannelDownstreamHandler() {

            @Override
            public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
                final Object o = e.getMessage();
                if (o instanceof Message) {
                    final Message m = (Message) o;
                    if (m.serverID() == null || m.serverID().equals(Message.NO_UUID))
                        m.serverID(ourServerID);
                }
                super.writeRequested(ctx, e);
            }

        };
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(MessageCodec.decoder(), MessageCodec.encoder(), uuidPopulatingHandler,
                        sharedMessageHandler);
            }

        });
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
    }

    public void start() {
        sharedMessageHandler.start();
        for (final InetSocketAddress address : listenAddresses) {
            logger.info("Starting listener on {}", address);
            openChannels.add(bootstrap.bind(address));
        }
        logger.info("Server startup complete");
    }

    public void stop() throws InterruptedException {
        logger.info("Server shutting down...");
        final ChannelGroupFuture future = openChannels.close();
        try {
            future.await();
        } finally {
            sharedMessageHandler.stop();
            factory.releaseExternalResources();
            workerService.shutdown();
            bossService.shutdown();
        }
        logger.info("Server shut down.");
    }

    /**
     * @param args
     *            host:port pairs that define the cluster (this machine needs to be part of that cluster), if none given then the
     *            default on all interfaces is used. If stdin is available then once a single byte is read the server will be
     *            stopped (useful for command line debugging), but if not the server will run indefinitely.
     * @throws Throwable
     *             if unable to parse command line arguments as host:port pairs
     */
    public static void main(final String[] args) throws Throwable {
        final Collection<InetSocketAddress> cluster = args.length > 0 ? hostPortPairsFromString(args[0], DEFAULT_ADDRESS.getPort())
                : new LinkedList<InetSocketAddress>();
        final PubSubServer pss = new PubSubServer(cluster);
        pss.start();
        if (System.in.read() != -1)
            pss.stop();
    }
}
