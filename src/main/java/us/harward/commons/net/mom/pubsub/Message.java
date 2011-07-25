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

    final Type type;

    protected Message(final Type type) {
        this.type = type;
    }

    final void marshall(final ChannelBuffer buffer) {
        final ChannelBuffer bodyBuffer = ChannelBuffers.dynamicBuffer(estimatedBodySize());
        marshallBody(bodyBuffer);
        buffer.writeBytes(type.bytes);
        buffer.writeInt(bodyBuffer.readableBytes());
        buffer.writeBytes(bodyBuffer);
    }

    abstract int estimatedBodySize();

    abstract void marshallBody(final ChannelBuffer buffer);

}
