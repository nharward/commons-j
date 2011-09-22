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

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

public final class ApplicationMessage extends Message {

    final String                topic;
    private final ChannelBuffer body;

    ApplicationMessage(final byte[] body, final String topic) {
        this(ChannelBuffers.wrappedBuffer(body), topic);
    }

    ApplicationMessage(final byte[] body, final int offset, final int length, final String topic) {
        this(ChannelBuffers.wrappedBuffer(body, offset, length), topic);
    }

    ApplicationMessage(final ByteBuffer body, final String topic) {
        this(ChannelBuffers.wrappedBuffer(body), topic);
    }

    ApplicationMessage(final ChannelBuffer body, final String topic) {
        super(Type.Application);
        Preconditions.checkNotNull(body, "Message body cannot be null");
        Preconditions.checkNotNull(topic, "Topic cannot be null");
        this.topic = topic;
        final byte[] topicBytes = topic.getBytes(Charsets.UTF_8);
        this.body = ChannelBuffers.buffer(4 + topicBytes.length + body.readableBytes());
        this.body.writeInt(topicBytes.length);
        this.body.writeBytes(topicBytes);
        this.body.writeBytes(body);
    }

    ApplicationMessage(final ChannelBuffer body) {
        super(Type.Application);
        this.body = ChannelBuffers.copiedBuffer(body);
        this.body.markReaderIndex();
        final byte[] topicBytes = new byte[this.body.readInt()];
        this.body.readBytes(topicBytes);
        topic = new String(topicBytes, Charsets.UTF_8);
        this.body.resetReaderIndex();
    }

    @Override
    int estimatedBodySize() {
        return body.capacity();
    }

    @Override
    void marshallBody(final ChannelBuffer buffer) {
        buffer.writeBytes(body, 0, body.capacity());
    }

    ByteBuffer applicationBody() {
        final int offset = 4 + topic.getBytes(Charsets.UTF_8).length;
        return body.toByteBuffer(offset, body.writerIndex() - offset).asReadOnlyBuffer();
    }

}
