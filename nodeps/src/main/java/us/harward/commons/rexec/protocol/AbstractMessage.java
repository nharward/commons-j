// Copyright 2010 Nathaniel Harward
//
// This file is part of ndh-commons.
//
// ndh-commons is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// ndh-commons is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with ndh-commons. If not, see <http://www.gnu.org/licenses/>.

package us.harward.commons.rexec.protocol;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;

import us.harward.commons.util.DbC;

public abstract class AbstractMessage {

    protected enum Type {
        MEMPROC_REQUEST, MEMPROC_RESPONSE
    }

    private final Type type;

    protected AbstractMessage(final Type type) {
        DbC.precondition(type != null, "Message type cannot be null");
        this.type = type;
    }

    /**
     * Writes (potentially) a series of {@link ByteBuffer} objects to the passed {@link Collection} that represent this object. Each
     * {@link ByteBuffer} will be in position for reading and is the callers to manipulate, and are guaranteed to be read-only.
     * 
     * @param sink
     *            must be non-null
     */
    public final void serialize(final Collection<ByteBuffer> sink) {
        DbC.precondition(sink != null);
        final Collection<ByteBuffer> localSink = new LinkedList<ByteBuffer>();
        final ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(type.ordinal());
        header.flip();
        localSink.add(header.asReadOnlyBuffer());
        serializeBody(localSink);
        for (final ByteBuffer buffer : localSink)
            DbC.invariant(buffer.isReadOnly());
        sink.addAll(localSink);
    }

    /**
     * Upon marshalling a message the size of the resulting byte stream is known. It is assumed that the mechanism doing the
     * marshalling/unmarshalling can keep track of this and upon unmarshalling provide a buffer large enough to hold [at least] the
     * data that was marshalled.
     * 
     * @throws Exception
     *             if there is some problem with the data being passed in
     * @return a concrete instance of {@link AbstractMessage}, or <em>null</em> if there is not enough data
     */
    public static AbstractMessage deserialize(final ByteBuffer source) throws Exception {
        DbC.precondition(source != null);
        DbC.precondition(source.remaining() >= 4);
        final Type type = Type.values()[source.getInt()];
        if (type == Type.MEMPROC_REQUEST)
            return new MemProcRequest(source);
        else if (type == Type.MEMPROC_RESPONSE)
            return new MemProcResponse(source);
        else
            throw new Exception("Unknown message type: " + type);
    }

    protected abstract void serializeBody(final Collection<ByteBuffer> sink);

    @Override
    public String toString() {
        return "AbstractMessage [type=" + type + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AbstractMessage other = (AbstractMessage) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

}
