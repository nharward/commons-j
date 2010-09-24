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
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

import org.junit.Test;

import us.harward.commons.util.DbC;

public class MessageSerializationTest {

    private final Random random = new SecureRandom();

    @Test
    public final void memprocRequest() throws Exception {
        for (final MemProcRequest.Type type : MemProcRequest.Type.values()) {
            final MemProcRequest request = new MemProcRequest(type);
            assert request.equals(doppleganger(request));
        }
    }

    @Test
    public final void memprocResponse() throws Exception {
        for (final MemProcRequest.Type type : MemProcRequest.Type.values()) {
            final MemProcRequest request = new MemProcRequest(type);
            MemProcResponse response = new MemProcResponse(request, random.nextLong());
            assert response.equals(doppleganger(response));
            assert response.getException() == null;
            response = new MemProcResponse(request, new Exception("Mouseface"));
            assert response.equals(doppleganger(response));
            assert response.getException().getMessage().equals("Mouseface");
            response = new MemProcResponse(request, new Exception());
            assert response.equals(doppleganger(response));
            assert response.getException().getMessage().equals("");
            response = new MemProcResponse(request, (Exception) null);
            assert response.equals(doppleganger(response));
            assert response.getException() == null;
        }
    }

    private static AbstractMessage doppleganger(final AbstractMessage message) throws Exception {
        DbC.precondition(message != null);
        final Collection<ByteBuffer> buffers = new LinkedList<ByteBuffer>();
        message.serialize(buffers);
        int serializedLength = 0;
        for (final ByteBuffer b : buffers)
            serializedLength += b.remaining();
        final ByteBuffer buffer = ByteBuffer.allocate(serializedLength);
        for (final ByteBuffer b : buffers)
            buffer.put(b);
        buffer.flip();
        return AbstractMessage.deserialize(buffer.asReadOnlyBuffer());
    }

}
