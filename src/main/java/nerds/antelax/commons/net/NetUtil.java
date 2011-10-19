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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public final class NetUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

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
                    final String host = parts[0].trim();
                    final int port = parts.length > 1 ? Integer.parseInt(parts[1]) : defaultPort;
                    rv.add(new InetSocketAddress(host, port));
                }
            }
        }
        return rv;
    }

    /**
     * @return a {@link Predicate} that indicates where {@link InetSocketAddress} objects passed to it will conflict (if bound by a
     *         server) with the passed in {@link InetSocketAddress}.
     */
    public static Predicate<InetSocketAddress> bindConflict(final InetSocketAddress bound) {
        return new BindConflictPredicate(bound);
    }

    /**
     * @return a {@link Predicate} that matches {@link InetAddress} objects that are local to the current machine
     */
    public static Predicate<InetAddress> machineLocalAddress() {
        return MachineLocalPredicate.INSTANCE;
    }

    /**
     * @return a {@link Predicate} that matches {@link InetSocketAddress} objects that are local to the current machine
     */
    public static Predicate<InetSocketAddress> machineLocalSocketAddress() {
        return MachineLocalWithSocketPredicate.INSTANCE;
    }

    private static final class MachineLocalPredicate implements Predicate<InetAddress> {

        private static final MachineLocalPredicate INSTANCE = new MachineLocalPredicate();

        @Override
        public boolean apply(final InetAddress input) {
            try {
                return input.isLoopbackAddress() || input.isAnyLocalAddress() || NetworkInterface.getByInetAddress(input) != null;
            } catch (final SocketException se) {
                logger.error("Exception retrieving network interface for address[" + input + "]", se);
                return false;
            }
        }

    }

    private static final class MachineLocalWithSocketPredicate implements Predicate<InetSocketAddress> {

        private static final MachineLocalWithSocketPredicate INSTANCE = new MachineLocalWithSocketPredicate();

        @Override
        public boolean apply(final InetSocketAddress input) {
            return machineLocalAddress().apply(input.getAddress());
        }

    }

    private static final class BindConflictPredicate implements Predicate<InetSocketAddress> {

        private final InetSocketAddress bound;

        private BindConflictPredicate(final InetSocketAddress bound) {
            Preconditions.checkNotNull(bound);
            Preconditions.checkArgument(machineLocalSocketAddress().apply(bound),
                    "Bound socket address should be local to this machine");
            this.bound = bound;
        }

        @Override
        public boolean apply(final InetSocketAddress input) {
            if (bound.getPort() != input.getPort())
                return false;
            else if (!machineLocalSocketAddress().apply(input))
                return false;
            else if (bound.getAddress().isAnyLocalAddress() || input.getAddress().isAnyLocalAddress())
                return true;
            else
                return bound.getAddress().equals(input.getAddress());
        }

    }

}
