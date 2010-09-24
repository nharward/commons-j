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
import java.util.Arrays;
import java.util.Collection;

import us.harward.commons.util.DbC;

public abstract class Response extends AbstractMessage {

    private static final byte[] EMPTY = new byte[0];
    private final byte[]        thrown;

    protected Response(final Type type) {
        this(type, (Exception) null);
    }

    protected Response(final Type type, final Exception thrown) {
        super(type);
        if (thrown != null) {
            this.thrown = thrown.getMessage() != null ? thrown.getMessage().getBytes() : EMPTY;
        } else {
            this.thrown = null;
        }
    }

    protected Response(final Type type, final ByteBuffer source) throws Exception {
        super(type);
        DbC.precondition(source != null);
        DbC.precondition(source.remaining() >= 4);
        final int thrownLength = source.getInt();
        DbC.invariant(thrownLength >= -1);
        DbC.precondition(source.remaining() >= thrownLength);
        if (thrownLength >= 0) {
            thrown = new byte[thrownLength];
            source.get(thrown);
        } else {
            thrown = null;
        }
    }

    public Exception getException() {
        return thrown != null ? new Exception(new String(thrown)) : null;
    }

    @Override
    protected void serializeBody(final Collection<ByteBuffer> sink) {
        DbC.precondition(sink != null);
        final ByteBuffer exceptionBuffer = ByteBuffer.allocate(4 + (thrown != null ? thrown.length : 0));
        exceptionBuffer.putInt(thrown != null ? thrown.length : -1);
        if (thrown != null)
            exceptionBuffer.put(thrown);
        exceptionBuffer.flip();
        sink.add(exceptionBuffer.asReadOnlyBuffer());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(thrown);
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
        final Response other = (Response) obj;
        if (!Arrays.equals(thrown, other.thrown))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Response [thrown=" + Arrays.toString(thrown) + ", toString()=" + super.toString() + "]";
    }

}
