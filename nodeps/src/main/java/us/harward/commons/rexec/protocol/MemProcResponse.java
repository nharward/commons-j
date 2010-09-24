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

import us.harward.commons.util.DbC;

public final class MemProcResponse extends Response {

    private final MemProcRequest.Type type;
    private final long                value;

    public MemProcResponse(final MemProcRequest request, final long value) {
        super(Type.MEMPROC_RESPONSE);
        type = request.type();
        this.value = value;
    }

    public MemProcResponse(final MemProcRequest request, final Exception thrown) {
        super(Type.MEMPROC_RESPONSE, thrown);
        type = request.type();
        value = Long.MIN_VALUE;
    }

    MemProcResponse(final ByteBuffer source) throws Exception {
        super(AbstractMessage.Type.MEMPROC_RESPONSE, source);
        DbC.precondition(source.remaining() == 12);
        type = MemProcRequest.Type.values()[source.getInt()];
        value = source.getLong();
    }

    public MemProcRequest.Type type() {
        return type;
    }

    public long value() {
        return value;
    }

    @Override
    protected void serializeBody(final Collection<ByteBuffer> sink) {
        super.serializeBody(sink);
        DbC.precondition(sink != null);
        final ByteBuffer body = ByteBuffer.allocate(12);
        body.putInt(type.ordinal());
        body.putLong(value);
        body.flip();
        sink.add(body.asReadOnlyBuffer());
    }

    @Override
    public String toString() {
        return "MemProcResponse [type=" + type + ", value=" + value + ", toString()=" + super.toString() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + (int) (value ^ (value >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MemProcResponse other = (MemProcResponse) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (value != other.value)
            return false;
        return true;
    }

}
