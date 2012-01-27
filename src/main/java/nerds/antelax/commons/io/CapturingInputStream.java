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
package nerds.antelax.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.common.base.Preconditions;

/**
 * Wraps an existing {@link InputStream}, but as data is read from the underlying source stream it is also written to another
 * [capture] stream.
 */
public final class CapturingInputStream extends InputStream {

    private final InputStream  source;
    private final OutputStream captureStream;
    private final boolean      autoClose;

    /**
     * @param source
     *            the {@link InputStream} to read/capture from
     * @param captureStream
     *            the {@link OutputStream} to write captured data to
     * @param autoClose
     *            whether or not to close the capturing stream when the end of the {@link InputStream} is reached
     */
    public CapturingInputStream(final InputStream source, final OutputStream captureStream, final boolean autoClose) {
        Preconditions.checkArgument(source != null);
        Preconditions.checkArgument(captureStream != null);
        this.source = source;
        this.captureStream = captureStream;
        this.autoClose = autoClose;
    }

    @Override
    public int read() throws IOException {
        final int b = source.read();
        if (b != -1)
            captureStream.write(b);
        else if (autoClose)
            captureStream.close();
        return b;
    }

    @Override
    public int available() throws IOException {
        return source.available();
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    @Override
    public void mark(final int readlimit) {
        source.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return source.markSupported();
    }

    @Override
    public void reset() throws IOException {
        source.reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        return source.skip(n);
    }

}
