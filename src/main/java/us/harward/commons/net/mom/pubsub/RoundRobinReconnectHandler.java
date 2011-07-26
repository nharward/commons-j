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

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/*
 * Blatently lifted and round-robin-ified from the very nice 'uptime' example included in the Netty source.
 */
final class RoundRobinReconnectHandler extends SimpleChannelUpstreamHandler {

    private static final Logger                                   logger = LoggerFactory
                                                                                 .getLogger(RoundRobinReconnectHandler.class);

    private static final Random                                   RANDOM = new SecureRandom();

    private final ClientBootstrap                                 bootstrap;
    private final PubSubClient.NetworkConnectionLifecycleCallback callback;
    private final int                                             retryDelay;
    private final TimeUnit                                        retryUnits;
    private final AtomicBoolean                                   enabled;
    private final List<InetSocketAddress>                         availableServers;
    private final List<InetSocketAddress>                         failedServers;
    private final Lock                                            lock;
    private final Timer                                           timer;
    private final AtomicReference<Channel>                        currentChannel;
    private final AtomicReference<InetSocketAddress>              currentRemoteAddress;

    RoundRobinReconnectHandler(final ClientBootstrap bootstrap, final int retryDelay, final TimeUnit retryUnits,
            final PubSubClient.NetworkConnectionLifecycleCallback callback, final InetSocketAddress... servers) {
        Preconditions.checkNotNull(bootstrap);
        Preconditions.checkNotNull(servers);
        Preconditions.checkArgument(retryDelay > 0);
        Preconditions.checkNotNull(retryUnits);
        this.bootstrap = bootstrap;
        this.callback = callback;
        this.retryDelay = retryDelay;
        this.retryUnits = retryUnits;
        availableServers = new ArrayList<InetSocketAddress>(servers.length);
        failedServers = new LinkedList<InetSocketAddress>();
        for (final InetSocketAddress isa : servers)
            if (isa != null)
                availableServers.add(isa);
        Preconditions.checkArgument(!availableServers.isEmpty(), "Server list was empty or had null values");
        enabled = new AtomicBoolean(false);
        lock = new ReentrantLock();
        timer = new HashedWheelTimer();
        currentChannel = new AtomicReference<Channel>(null);
        currentRemoteAddress = new AtomicReference<InetSocketAddress>(null);
    }

    Channel channel() {
        return currentChannel.get();
    }

    void enable() {
        enabled.set(true);
        reconnect();
    }

    void disable() {
        enabled.set(false);
    }

    void shutdown() {
        disable();
        final Channel c = currentChannel.get();
        if (c != null)
            c.close();
        timer.stop();
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        currentChannel.set(null);
        final SocketAddress remote = e.getChannel().getRemoteAddress();
        logger.debug("Disconnected from server: {}", remote);
        if (callback != null)
            callback.connectionDown(remote);
        super.channelDisconnected(ctx, e);
    }

    @Override
    public void channelClosed(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        lock.lock();
        try {
            final InetSocketAddress failedAddress;
            if (e.getChannel().getRemoteAddress() != null)
                failedAddress = (InetSocketAddress) e.getChannel().getRemoteAddress();
            else
                failedAddress = currentRemoteAddress.get();
            Preconditions.checkArgument(availableServers.contains(failedAddress));
            Preconditions.checkArgument(!failedServers.contains(failedAddress));
            availableServers.remove(failedAddress);
            failedServers.add(failedAddress);
        } finally {
            lock.unlock();
        }
        reconnect();
        super.channelClosed(ctx, e);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        currentChannel.set(e.getChannel());
        final SocketAddress remote = e.getChannel().getRemoteAddress();
        currentRemoteAddress.set((InetSocketAddress) remote);
        logger.debug("Established connection to server {}", remote);
        if (callback != null)
            callback.connectionUp(remote);
        super.channelConnected(ctx, e);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) {
        final Throwable cause = e.getCause();
        if (cause instanceof ConnectException)
            logger.warn("Unable to establish connection to {}", currentRemoteAddress.get());
        ctx.getChannel().close();
    }

    private void reconnect() {
        if (!enabled.get())
            return;
        else {
            lock.lock();
            try {
                if (availableServers.isEmpty()) {
                    logger.info("No servers are available, will re-try in [{}/{}]", retryDelay, retryUnits);
                    timer.newTimeout(new TimerTask() {

                        @Override
                        public void run(final Timeout timeout) throws Exception {
                            lock.lock();
                            try {
                                availableServers.addAll(failedServers);
                                failedServers.clear();
                                reconnect();
                            } finally {
                                lock.unlock();
                            }
                        }

                    }, retryDelay, retryUnits);
                } else {
                    final InetSocketAddress server = availableServers.get(RANDOM.nextInt(availableServers.size()));
                    currentRemoteAddress.set(server);
                    logger.debug("Have available servers[{}], calling connect() with {}", availableServers, server);
                    bootstrap.connect(server);
                }
            } finally {
                lock.unlock();
            }
        }
    }

}