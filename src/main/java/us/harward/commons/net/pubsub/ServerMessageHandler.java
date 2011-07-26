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

package us.harward.commons.net.pubsub;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.util.internal.ConcurrentHashMap;

@Sharable
final class ServerMessageHandler extends SimpleChannelHandler {

    private final Map<String, DefaultChannelGroup> subscribers;
    private final Lock                             lock;

    ServerMessageHandler() {
        subscribers = new ConcurrentHashMap<String, DefaultChannelGroup>();
        lock = new ReentrantLock();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final Object o = e.getMessage();
        if (o instanceof ApplicationMessage)
            handleApplicationMessage(ctx, (ApplicationMessage) o);
        else if (o instanceof SubscriptionMessage)
            handleSubscriptionRequest(ctx, (SubscriptionMessage) o);
        else
            super.messageReceived(ctx, e);
    }

    /*
     * Broadcast this incoming application message to all subscribers of the topic (except for the client who sent it)
     */
    private void handleApplicationMessage(final ChannelHandlerContext ctx, final ApplicationMessage msg) {
        final DefaultChannelGroup group = subscribers.get(msg.topic);
        if (group != null) {
            final Channel source = ctx.getChannel();
            final Iterator<Channel> pos = group.iterator();
            while (pos.hasNext()) {
                final Channel channel = pos.next();
                if (channel.getId() != source.getId())
                    channel.write(msg);
            }
        }
    }

    private void handleSubscriptionRequest(final ChannelHandlerContext ctx, final SubscriptionMessage msg) {
        if (msg.subscribe)
            subscribe(ctx.getChannel(), msg.topics);
        else
            unsubscribe(ctx.getChannel(), msg.topics);
    }

    private void subscribe(final Channel channel, final String... topics) {
        for (final String topic : topics) {
            lock.lock();
            try {
                final DefaultChannelGroup group;
                if (subscribers.containsKey(topic))
                    group = subscribers.get(topic);
                else {
                    group = new DefaultChannelGroup(topic);
                    subscribers.put(topic, group);
                }
                group.add(channel);
            } finally {
                lock.unlock();
            }
        }
    }

    private void unsubscribe(final Channel channel, final String... topics) {
        for (final String topic : topics) {
            lock.lock();
            try {
                final DefaultChannelGroup group = subscribers.get(topic);
                if (group != null) {
                    group.remove(channel);
                    if (group.isEmpty())
                        subscribers.remove(topic);
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
