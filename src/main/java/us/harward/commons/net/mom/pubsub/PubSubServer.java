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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class PubSubServer {

    public static final InetSocketAddress       DEFAULT_ADDRESS = new InetSocketAddress(9000);

    private final Collection<InetSocketAddress> listenAddresses;
    private final ChannelFactory                factory;
    private final ServerBootstrap               bootstrap;
    private final ChannelGroup                  openChannels;

    public PubSubServer(final InetSocketAddress... listenAddresses) {
        final Collection<InetSocketAddress> laddrs = new LinkedList<InetSocketAddress>();
        if (listenAddresses != null && listenAddresses.length > 0)
            for (final InetSocketAddress address : listenAddresses)
                laddrs.add(address);
        else
            laddrs.add(DEFAULT_ADDRESS);
        this.listenAddresses = Collections.unmodifiableCollection(laddrs);
        openChannels = new DefaultChannelGroup(getClass().getName());
        factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        bootstrap = new ServerBootstrap(factory);
        final MessageHandler sharedMessageHandler = new MessageHandler();
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() {
                return Channels.pipeline(new MessageCodec(), sharedMessageHandler);
            }

        });
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
    }

    public void start() {
        for (final InetSocketAddress address : listenAddresses)
            openChannels.add(bootstrap.bind(address));
    }

    public void stop() {
        final ChannelGroupFuture future = openChannels.close();
        future.awaitUninterruptibly();
        factory.releaseExternalResources();
    }

    /**
     * @param args
     *            host:port pairs to listen on, if none given then the default of port 9000 on all interfaces is used.
     * @throws Throwable
     *             if unable to parse command line arguments as host:port pairs
     */
    public static void main(final String[] args) throws Throwable {
        final InetSocketAddress[] listenAddrs = new InetSocketAddress[args.length];
        for (int pos = 0; pos < args.length; ++pos) {
            try {
                final String[] host_port = args[pos].split(":");
                if (host_port == null || host_port.length != 2)
                    throw new Exception("Expected host:port pair, got '" + args[pos] + "' instead");
                listenAddrs[pos] = new InetSocketAddress(host_port[0], Integer.parseInt(host_port[1]));
            } catch (final Throwable t) {
                System.err.println("Unable to process argument " + (pos + 1) + " '" + args[pos] + "' as a host:port pair");
                t.printStackTrace(System.err);
                throw t;
            }
        }
        final PubSubServer pss = new PubSubServer(listenAddrs);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                pss.stop();
            }

        }));
        pss.start();
    }

}
