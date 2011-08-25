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

import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Preconditions;

abstract class Message {

    protected enum Type {

        Subscription("SUB "), Application("APPL");

        private final String code;
        private final byte[] bytes;

        private Type(final String code) {
            Preconditions.checkNotNull(code);
            this.code = code;
            bytes = this.code.getBytes(Charsets.UTF_8);
            Preconditions.checkNotNull(bytes);
            Preconditions.checkArgument(bytes.length == 4, "Codes must be 4 bytes when converted to UTF-8");
        }

        static Type findByCode(final String search) {
            for (final Type t : values())
                if (t.code.equals(search))
                    return t;
            return null;
        }

    }

    final static UUID NO_UUID = new UUID(0l, 0l);

    final Type        type;
    private short     ttl;
    private UUID      sourceID;
    private UUID      serverID;

    protected Message(final Type type) {
        this.type = type;
        ttl = 2;
        sourceID = NO_UUID;
        serverID = NO_UUID;
    }

    void ttl(final short ttl) {
        this.ttl = ttl;
    }

    short ttl() {
        return ttl;
    }

    void sourceID(final UUID sourceID) {
        this.sourceID = sourceID;
    }

    UUID sourceID() {
        return this.sourceID;
    }

    void serverID(final UUID sourceID) {
        this.serverID = sourceID;
    }

    UUID serverID() {
        return this.serverID;
    }

    final void marshall(final ChannelBuffer buffer) {
        final ChannelBuffer bodyBuffer = ChannelBuffers.dynamicBuffer(estimatedBodySize());
        marshallBody(bodyBuffer);
        buffer.writeBytes(type.bytes);
        buffer.writeShort(ttl);
        if (sourceID != null) {
            buffer.writeLong(sourceID.getMostSignificantBits());
            buffer.writeLong(sourceID.getLeastSignificantBits());
        } else {
            buffer.writeLong(NO_UUID.getMostSignificantBits());
            buffer.writeLong(NO_UUID.getLeastSignificantBits());
        }
        if (serverID != null) {
            buffer.writeLong(serverID.getMostSignificantBits());
            buffer.writeLong(serverID.getLeastSignificantBits());
        } else {
            buffer.writeLong(NO_UUID.getMostSignificantBits());
            buffer.writeLong(NO_UUID.getLeastSignificantBits());
        }
        buffer.writeInt(bodyBuffer.readableBytes());
        buffer.writeBytes(bodyBuffer);
    }

    final int headerSize() {
        return 4 // Type
                + 2 // TTL
                + 16 // Source ID
                + 16 // Server ID
                + 4; // Body length
    }

    abstract int estimatedBodySize();

    abstract void marshallBody(final ChannelBuffer buffer);

    @Override
    public String toString() {
        final ToStringHelper tsh = Objects.toStringHelper(getClass());
        tsh.add("type", type.name());
        tsh.add("ttl", ttl);
        tsh.add("source UUID", sourceID);
        tsh.add("server UUID", serverID);
        tsh.add("body length", estimatedBodySize());
        return tsh.toString();
    }

    static final Builder newBuilder() {
        return new Builder();
    }

    static final class Builder {

        private final ChannelBuffer type;
        private final ChannelBuffer ttl;
        private final ChannelBuffer sourceID;
        private final ChannelBuffer serverID;
        private final ChannelBuffer length;
        private ChannelBuffer       body;

        Builder() {
            type = ChannelBuffers.buffer(4);
            ttl = ChannelBuffers.buffer(2);
            sourceID = ChannelBuffers.buffer(16);
            serverID = ChannelBuffers.buffer(16);
            length = ChannelBuffers.buffer(4);
            body = null;
        }

        /**
         * This message builder may be re-used from scratch immediately following when a non-null message is returned from this
         * method. At such a point when a non-null return value is given here, all internal state is reset.
         * 
         * @param buffer
         *            more data to add to this builder
         * @return the message decoded by reading some or all of <code>buffer</code> or <code>null</code> if more data is needed
         */
        Message add(final ChannelBuffer buffer) throws MessageFormatException {
            Preconditions.checkNotNull(buffer);
            if (buffer.readable() && type.writable())
                type.writeBytes(buffer, Math.min(buffer.readableBytes(), type.writableBytes()));
            if (buffer.readable() && ttl.writable())
                ttl.writeBytes(buffer, Math.min(buffer.readableBytes(), ttl.writableBytes()));
            if (buffer.readable() && sourceID.writable())
                sourceID.writeBytes(buffer, Math.min(buffer.readableBytes(), sourceID.writableBytes()));
            if (buffer.readable() && serverID.writable())
                serverID.writeBytes(buffer, Math.min(buffer.readableBytes(), serverID.writableBytes()));
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
                message.ttl(ttl.readShort());
                message.sourceID(new UUID(sourceID.readLong(), sourceID.readLong()));
                message.serverID(new UUID(serverID.readLong(), serverID.readLong()));
                type.clear();
                ttl.clear();
                sourceID.clear();
                serverID.clear();
                length.clear();
                body = null;
            } else
                message = null;
            return message;
        }

    }

}
