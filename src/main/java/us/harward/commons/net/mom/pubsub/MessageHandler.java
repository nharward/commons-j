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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.util.internal.ConcurrentHashMap;

@Sharable
final class MessageHandler extends SimpleChannelHandler {

    private final Map<String, DefaultChannelGroup> subscribers;
    private final Lock                             lock;

    MessageHandler() {
        subscribers = new ConcurrentHashMap<String, DefaultChannelGroup>();
        lock = new ReentrantLock();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        final Object o = e.getMessage();
        if (o instanceof SubscriptionMessage) {
            handleSubscriptionRequest(ctx, (SubscriptionMessage) o);
        } else if (o instanceof ApplicationMessage) {
            handleApplicationMessage(ctx, (ApplicationMessage) o);
        } else
            super.messageReceived(ctx, e);
    }

    private void handleApplicationMessage(final ChannelHandlerContext ctx, final ApplicationMessage msg) {
        final DefaultChannelGroup group = subscribers.get(msg.topic);
        if (group != null) {
            final ChannelBuffer tmp = ChannelBuffers.dynamicBuffer(8 + msg.estimatedBodySize());
            msg.marshall(tmp);
            final ChannelBuffer netmsg = ChannelBuffers.unmodifiableBuffer(tmp);
            final Iterator<Channel> pos = group.iterator();
            final Channel source = ctx.getChannel();
            while (pos.hasNext()) {
                final Channel channel = pos.next();
                netmsg.markReaderIndex();
                netmsg.markWriterIndex();
                if (channel.getId() != source.getId())
                    channel.write(netmsg);
                netmsg.resetReaderIndex();
                netmsg.resetWriterIndex();
            }
        }
    }

    private void handleSubscriptionRequest(final ChannelHandlerContext ctx, final SubscriptionMessage msg) {
        if (msg.subscribe) {
            for (final String topic : msg.topics) {
                DefaultChannelGroup group;
                lock.lock();
                try {
                    group = subscribers.get(topic);
                    if (group == null) {
                        group = new DefaultChannelGroup(topic);
                        group.add(ctx.getChannel());
                        subscribers.put(topic, group);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } else {
            for (final String topic : msg.topics) {
                final DefaultChannelGroup group = subscribers.get(topic);
                if (group != null) {
                    group.remove(ctx.getChannel());
                    lock.lock();
                    try {
                        if (group.isEmpty())
                            subscribers.remove(topic);
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }

}
