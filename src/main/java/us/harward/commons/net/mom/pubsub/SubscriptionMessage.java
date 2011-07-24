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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

final class SubscriptionMessage extends Message {

    private final ChannelBuffer body;
    final boolean               subscribe;
    final String[]              topics;

    SubscriptionMessage(final boolean subscribe, final String... topics) {
        super(Type.Subscription);
        Preconditions.checkNotNull(topics, "Topic list cannot be empty");
        this.subscribe = subscribe;
        this.topics = topics;
        int length = 8;
        for (final String topic : topics)
            length += 4 + topic.getBytes(Charsets.UTF_8).length;
        body = ChannelBuffers.buffer(length);
        body.writeInt(subscribe ? 1 : 0);
        body.writeInt(topics.length);
        for (final String topic : topics) {
            final byte[] topicBytes = topic.getBytes(Charsets.UTF_8);
            body.writeInt(topicBytes.length);
            body.writeBytes(topicBytes);
        }
        Preconditions.checkArgument(!body.writable(), "Body should be finished writing but contains %s more bytes for writing",
                body.writableBytes());
    }

    SubscriptionMessage(final ChannelBuffer body) {
        super(Type.Subscription);
        this.body = ChannelBuffers.copiedBuffer(body);
        subscribe = this.body.readInt() > 0;
        topics = new String[this.body.readInt()];
        for (int pos = 0; pos < topics.length; ++pos) {
            final ChannelBuffer topic = ChannelBuffers.buffer(this.body.readInt());
            this.body.readBytes(topic, topic.capacity());
            topics[pos] = topic.toString(Charsets.UTF_8);
        }
    }

    @Override
    int estimatedBodySize() {
        return body.capacity();
    }

    @Override
    void marshallBody(final ChannelBuffer buffer) {
        buffer.writeBytes(body, 0, body.capacity());
    }

}
