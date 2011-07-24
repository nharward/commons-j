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

final class MessageBuilder {

    private final ChannelBuffer type;
    private final ChannelBuffer length;
    private ChannelBuffer       body;

    MessageBuilder() {
        type = ChannelBuffers.buffer(4);
        length = ChannelBuffers.buffer(4);
        body = null;
    }

    /**
     * This message builder may be re-used from scratch immediately following when a non-null message is returned from this method.
     * At such a point when a non-null return value is given here, all internal state is reset.
     * 
     * @param buffer
     *            more data to add to this builder
     * @return the message decoded by reading some or all of <code>buffer</code> or <code>null</code> if more data is needed
     */
    Message add(final ChannelBuffer buffer) throws MessageFormatException {
        Preconditions.checkNotNull(buffer);
        if (buffer.readable() && type.writable())
            type.writeBytes(buffer, Math.min(buffer.readableBytes(), type.writableBytes()));
        if (buffer.readable() && length.writable())
            length.writeBytes(buffer, Math.min(buffer.readableBytes(), length.writableBytes()));
        if (!length.writable() && body == null)
            body = ChannelBuffers.buffer(length.readInt());
        if (body != null && buffer.readable() && body.writable())
            body.writeBytes(buffer, Math.min(buffer.readableBytes(), body.writableBytes()));
        final Message message;
        if (body != null && !body.writable()) {
            final Message.Type t = Message.Type.findByCode(type.toString(Charsets.UTF_8));
            if (t == Message.Type.Application)
                message = new ApplicationMessage(body);
            else if (t == Message.Type.Subscription)
                message = new SubscriptionMessage(body);
            else
                throw new MessageFormatException("Unknown message type[" + type.toString(Charsets.UTF_8) + "]");
        } else
            message = null;
        return message;
    }

}
