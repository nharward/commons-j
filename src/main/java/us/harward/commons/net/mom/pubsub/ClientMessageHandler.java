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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.harward.commons.net.mom.pubsub.PubSubClient.MessageCallback;

import com.google.common.base.Preconditions;

@Sharable
final class ClientMessageHandler extends SimpleChannelHandler {

    private static final Logger                                         logger = LoggerFactory
                                                                                       .getLogger(ClientMessageHandler.class);

    private final AtomicReference<Channel>                              activeChannel;
    private final Map<String, Collection<PubSubClient.MessageCallback>> subscribers;
    private final Lock                                                  lock;
    private final ExecutorService                                       callbackService;

    ClientMessageHandler(final ExecutorService callbackService) {
        Preconditions.checkNotNull(callbackService, "ExecutorService cannot be null");
        activeChannel = new AtomicReference<Channel>(null);
        this.callbackService = callbackService;
        subscribers = new ConcurrentHashMap<String, Collection<PubSubClient.MessageCallback>>();
        lock = new ReentrantLock();
    }

    void subscribe(final String topic, final PubSubClient.MessageCallback... callbacks) {
        lock.lock();
        try {
            final Collection<PubSubClient.MessageCallback> group;
            if (subscribers.containsKey(topic)) {
                group = subscribers.get(topic);
            } else {
                group = new CopyOnWriteArrayList<PubSubClient.MessageCallback>();
                subscribers.put(topic, group);
                final Channel channel = activeChannel.get();
                if (channel != null) {
                    channel.write(new SubscriptionMessage(true, topic));
                }
            }
            for (final MessageCallback callback : callbacks)
                group.add(callback);
        } finally {
            lock.unlock();
        }
    }

    void unsubscribe(final String topic, final PubSubClient.MessageCallback... callbacks) {
        lock.lock();
        try {
            if (!subscribers.containsKey(topic))
                return;
            final Collection<PubSubClient.MessageCallback> group = subscribers.get(topic);
            for (final MessageCallback callback : callbacks)
                group.remove(callback);
            if (group.isEmpty()) {
                subscribers.remove(topic);
                final Channel channel = activeChannel.get();
                if (channel != null) {
                    channel.write(new SubscriptionMessage(false, topic));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        activeChannel.set(e.getChannel());
        final ArrayList<String> al = new ArrayList<String>();
        for (final String topic : subscribers.keySet())
            al.add(topic);
        final String[] topics = new String[al.size()];
        al.toArray(topics);
        e.getChannel().write(new SubscriptionMessage(true, topics));
        super.channelConnected(ctx, e);
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        activeChannel.set(null);
        super.channelDisconnected(ctx, e);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final Object o = e.getMessage();
        if (o instanceof ApplicationMessage) {
            handleApplicationMessage(ctx, (ApplicationMessage) o);
        } else
            super.messageReceived(ctx, e);
    }

    private void handleApplicationMessage(final ChannelHandlerContext ctx, final ApplicationMessage msg) {
        final Collection<PubSubClient.MessageCallback> callbacks = subscribers.get(msg.topic);
        if (callbacks != null && !callbacks.isEmpty()) {
            final ByteBuffer appMsg = msg.applicationBody();
            for (final PubSubClient.MessageCallback callback : callbacks)
                callbackService.submit(new CallbackInvoker(callback, appMsg.asReadOnlyBuffer()));
        }
    }

    private static final class CallbackInvoker implements Runnable {

        private final MessageCallback callback;
        private final ByteBuffer      message;

        private CallbackInvoker(final PubSubClient.MessageCallback callback, final ByteBuffer message) {
            this.callback = callback;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                callback.onMessage(message);
            } catch (final Exception e) {
                logger.error("Caught exception during message callback on " + callback, e);
            }
        }

    }

}
