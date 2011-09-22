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
package nerds.antelax.commons.net;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;

public final class NetUtil {

    private NetUtil() {
        throw new RuntimeException("Utility class, not for instantiation");
    }

    /**
     * Parses a comma-separated string of <code>&lt;host&gt;:&lt;port&gt;</code> pairs into a {@link Collection} of
     * {@link InetSocketAddress} values. If any particular host/port pair is a host only, the default port value is used.
     * 
     * @param hostPortPairs
     *            the comma-separated list of host/port pairs
     * @param defaultPort
     *            the port to use if not specified in <code>hostPortPairs</code>
     */
    public static Collection<InetSocketAddress> hostPortPairsFromString(final String hostPortPairs, final int defaultPort) {
        final Collection<InetSocketAddress> rv = new LinkedList<InetSocketAddress>();
        if (hostPortPairs != null && hostPortPairs.trim().length() > 0) {
            for (final String pair : hostPortPairs.split(",")) {
                final String[] parts = pair.split(":");
                if (parts.length > 0) {
                    final String host = parts[0];
                    final int port = parts.length > 1 ? Integer.parseInt(parts[1]) : defaultPort;
                    rv.add(new InetSocketAddress(host, port));
                }
            }
        }
        return rv;
    }

}
