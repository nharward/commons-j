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

public final class MemProcRequest extends Request {

    enum Type {
        PROCESSORS, MAX_MEMORY, FREE_MEMORY, TOTAL_MEMORY
    }

    private final Type type;

    public MemProcRequest(final Type type) {
        super(AbstractMessage.Type.MEMPROC_REQUEST);
        this.type = type;
    }

    MemProcRequest(final ByteBuffer source) throws Exception {
        super(AbstractMessage.Type.MEMPROC_REQUEST);
        DbC.precondition(source.remaining() == 4);
        type = Type.values()[source.getInt()];
    }

    public Type type() {
        return type;
    }

    @Override
    protected void serializeBody(final Collection<ByteBuffer> sink) {
        DbC.precondition(sink != null);
        final ByteBuffer body = ByteBuffer.allocate(4);
        body.putInt(type.ordinal());
        body.flip();
        sink.add(body.asReadOnlyBuffer());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        final MemProcRequest other = (MemProcRequest) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MemoryRequest [type=" + type + ", toString()=" + super.toString() + "]";
    }

}
