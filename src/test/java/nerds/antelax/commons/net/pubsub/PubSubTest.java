package nerds.antelax.commons.net.pubsub;

import static org.testng.AssertJUnit.assertEquals;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import nerds.antelax.commons.util.Conversions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

public class PubSubTest {

    private static final Logger logger = LoggerFactory.getLogger(PubSubTest.class);
    private static final String topic  = "pubsub-test";

    @Test(dataProvider = "server-addresses")
    public void test(final Collection<InetSocketAddress> serverAddresses) throws InterruptedException, ExecutionException {
        final SecureRandom random = new SecureRandom();
        final int clientsPerServer = random.nextInt(3) + 1;
        final int messagesPerClient = random.nextInt(100) + 1;
        final ExecutorService clientSvc = Executors.newCachedThreadPool();
        final Collection<PubSubServer> servers = new LinkedList<PubSubServer>();
        final Collection<PubSubClient> clients = new LinkedList<PubSubClient>();
        try {
            // Set up a latch, so that we know when all our clients are happily connected
            final CountDownLatch latch = new CountDownLatch(serverAddresses.size() * clientsPerServer);
            final PubSubClient.NetworkConnectionLifecycleCallback lifecycle = new PubSubClient.NetworkConnectionLifecycleCallback() {

                @Override
                public void connectionUp(final SocketAddress endpoint) {
                    latch.countDown();
                }

                @Override
                public void connectionDown(final SocketAddress endpoint) {
                }
            };

            // Set up our message callback, which simply keeps track of how many messages have been received
            final AtomicInteger messageCount = new AtomicInteger(0);
            final PubSubClient.MessageCallback callback = new PubSubClient.MessageCallback() {

                @Override
                public void onMessage(final ByteBuffer message) throws Exception {
                    messageCount.incrementAndGet();
                }

            };

            // For each server address, create a new server and its clients
            for (final InetSocketAddress isa : serverAddresses) {
                final PubSubServer server = new PubSubServer(Arrays.asList(isa), Collections2.filter(serverAddresses,
                        Predicates.not(new AddressPredicate(isa))));
                server.start();
                servers.add(server);
                for (int pos = 0; pos < clientsPerServer; ++pos) {
                    final PubSubClient client = new PubSubClient(clientSvc, lifecycle, serverAddresses);
                    client.start();
                    client.subscribe(topic, callback);
                    clients.add(client);
                }
            }

            // Let everything fire up, get connected and be happy - give 1 second per server for topic subscription
            latch.await();
            Thread.sleep(1000 * clients.size());

            // Loop through all clients, sending (empty) messages to all of them
            final byte[] message = new byte[0];
            for (final PubSubClient client : clients)
                for (int pos = 0; pos < messagesPerClient; ++pos) {
                    assert client.publish(message, topic).get() : "Unable to publish message";
                    Thread.sleep(100);
                }

            // Allow some time to pass, say 10 seconds per server
            Thread.sleep(10 * 1000 * serverAddresses.size());

            // See how many messages we received, and if it's what we expect ;)
            final int expected = ((clients.size() - 1) * messagesPerClient) * clients.size();
            final int actual = messageCount.get();
            logger.info("{} servers, {} clients, {} messages per client for a total of {} messages",
                    Conversions.asArray(servers.size(), clients.size(), messagesPerClient, expected));
            assertEquals(expected, actual);
        } finally {
            for (final PubSubClient client : clients)
                try {
                    client.stop();
                } catch (final InterruptedException ie) {
                    logger.error("Unable to shut down client[" + client + "] cleanly - may need to kill test", ie);
                }
            for (final PubSubServer server : servers)
                try {
                    server.stop();
                } catch (final InterruptedException ie) {
                    logger.error("Unable to shut down server[" + server + "] cleanly - may need to kill test", ie);
                }
            clientSvc.shutdown();
        }
    }

    /**
     * Creates some interesting server configurations to test:
     * <ul>
     * <li>single server</li>
     * <li>two servers</li>
     * <li>three servers</li>
     * <li>random between 5 and 10</li>
     * </ul>
     */
    @DataProvider(name = "server-addresses")
    public Object[][] serverAddresses() {
        final Function<Integer, InetSocketAddress> serverAddressBuilder = new Function<Integer, InetSocketAddress>() {

            private final InetSocketAddress base = PubSubServer.DEFAULT_ADDRESS;

            @Override
            public InetSocketAddress apply(final Integer input) {
                return new InetSocketAddress(base.getAddress(), base.getPort() + input);
            }
        };
        final int random = new SecureRandom().nextInt(5) + 5;
        final Collection<Integer> randomCount = new ArrayList<Integer>(random);
        for (int pos = 0; pos < random; ++pos)
            randomCount.add(6 + pos);
        return new Object[][] { new Object[] { Collections2.transform(Arrays.asList(0), serverAddressBuilder) },
                new Object[] { Collections2.transform(Arrays.asList(1, 2), serverAddressBuilder) },
                new Object[] { Collections2.transform(Arrays.asList(3, 4, 5), serverAddressBuilder) },
                new Object[] { Collections2.transform(randomCount, serverAddressBuilder) } };
    }

    private static final class AddressPredicate implements Predicate<InetSocketAddress> {

        private final InetSocketAddress match;

        private AddressPredicate(final InetSocketAddress match) {
            Preconditions.checkNotNull(match);
            this.match = match;
        }

        @Override
        public boolean apply(final InetSocketAddress input) {
            return match.equals(input);
        }

    }

}
