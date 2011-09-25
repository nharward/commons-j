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
package nerds.antelax.commons.net.pubsub;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import nerds.antelax.commons.net.NetUtil;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 * <p>
 * Reads the servlet initialization parameter <code>pubsub-cluster</code> as a comma separated list of &lt;host[:port]&gt; pairs. If
 * the local machine is among those listed, a new {@link PubSubServer} instance will be created and started when this listeners
 * context is initialized, and stopped when destroyed. Hostnames not matching the local host will be used as remote masters. This
 * list of remotes is statically available via the {@link #remoteServers()} method for use with clients.
 * </p>
 * <p>
 * If {@link #remoteServers()} is used by clients, it's important that this listener is initialized <em>prior</em> to that of the
 * clients.
 * </p>
 */
public class PubSubServerContextListener implements ServletContextListener {

    public static final String                                          CLUSTER_INIT_PARAM = "pubsub-cluster";

    private static final Collection<InetSocketAddress>                  EMPTY;
    private static final AtomicReference<Collection<InetSocketAddress>> REMOTE_SERVERS;

    static {
        EMPTY = Collections.unmodifiableCollection(new LinkedList<InetSocketAddress>());
        REMOTE_SERVERS = new AtomicReference<Collection<InetSocketAddress>>(EMPTY);
    }

    private PubSubServer                                                server;

    @Override
    public synchronized void contextInitialized(final ServletContextEvent ctx) {
        final ServletContext sc = ctx.getServletContext();
        final Collection<InetSocketAddress> cluster = sc != null ? NetUtil.hostPortPairsFromString(
                sc.getInitParameter(CLUSTER_INIT_PARAM), PubSubServer.DEFAULT_ADDRESS.getPort()) : EMPTY;
        REMOTE_SERVERS.set(Collections2.filter(cluster, Predicates.not(NetUtil.machineLocalSocketAddress())));
        if (Iterables.any(cluster, NetUtil.machineLocalSocketAddress())) {
            server = new PubSubServer(cluster);
            ctx.getServletContext().log("Starting PubSub server, this machine is part of the cluster definition[" + cluster + "]");
            server.start();
        } else {
            server = null;
            ctx.getServletContext().log("No PubSub server started, remotes available for final clients are " + remoteServers());
        }
    }

    @Override
    public synchronized void contextDestroyed(final ServletContextEvent ctx) {
        if (server != null)
            try {
                server.stop();
            } catch (final InterruptedException ie) {
                ctx.getServletContext().log("Interrupted while stopping PubSub server", ie);
            }
        server = null;
    }

    public static Collection<InetSocketAddress> remoteServers() {
        return REMOTE_SERVERS.get();
    }

}
