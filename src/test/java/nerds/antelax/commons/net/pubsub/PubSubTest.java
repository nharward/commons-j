package nerds.antelax.commons.net.pubsub;

import static org.testng.AssertJUnit.assertFalse;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import nerds.antelax.commons.net.NetUtil;
import nerds.antelax.commons.util.Conversions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

public class PubSubTest {

    private static final Logger logger = LoggerFactory.getLogger(PubSubTest.class);
    private static final String topic  = "pubsub-test";

    @SuppressWarnings("unchecked")
    @Test(dataProvider = "server-addresses")
    public void test(final Collection<InetSocketAddress> cluster) throws InterruptedException, ExecutionException {
        Preconditions.checkArgument(Iterables.all(cluster, NetUtil.machineLocalSocketAddress()),
                "All machines in the cluster definition should be local");
        final SecureRandom random = new SecureRandom();
        final int clientsPerPort = random.nextInt(4) + 1;
        final int messagesPerClient = random.nextInt(5000) + 100;
        final int messageReceivedCount = (((cluster.size() * clientsPerPort) - 1) * messagesPerClient)
                * (cluster.size() * clientsPerPort);
        final int payloadSize = random.nextInt(1024);
        final ExecutorService clientSvc = Executors.newCachedThreadPool();
        final PubSubServer server = new PubSubServer(cluster);
        server.start();
        final Collection<PubSubClient> clients = new LinkedList<PubSubClient>();
        final AtomicLong startNanos = new AtomicLong();
        try {
            // Set up a latch so that we know when all our clients are happily connected
            final CountDownLatch latch = new CountDownLatch(cluster.size() * clientsPerPort);
            final PubSubClient.NetworkConnectionLifecycleCallback lifecycle = new PubSubClient.NetworkConnectionLifecycleCallback() {

                @Override
                public void connectionUp(final SocketAddress endpoint) {
                    latch.countDown();
                }

                @Override
                public void connectionDown(final SocketAddress endpoint) {
                }

            };

            // Set up our message callback which simply decrements from the expected number of messages that will be received
            final CountDownLatch remainingMessages = new CountDownLatch(messageReceivedCount);
            final AtomicBoolean tooManyMessages = new AtomicBoolean(false);
            final PubSubClient.MessageCallback callback = new PubSubClient.MessageCallback() {

                @Override
                public void onMessage(final ByteBuffer message) throws Exception {
                    if (remainingMessages.getCount() == 0)
                        tooManyMessages.set(true);
                    remainingMessages.countDown();
                }

            };

            // For each server address, create new clients so that each server address is used
            for (final InetSocketAddress isa : cluster) {
                for (int pos = 0; pos < clientsPerPort; ++pos) {
                    final PubSubClient client = new PubSubClient(clientSvc, lifecycle, Arrays.asList(isa));
                    client.start();
                    client.subscribe(topic, callback);
                    clients.add(client);
                }
            }

            // Let everything fire up, get connected and be happy - give 1/2 second per server for topic subscription
            assert latch.await(3, TimeUnit.MINUTES);
            Thread.sleep(500 * clients.size());

            // Loop through all clients, sending (empty) messages to all of them
            final byte[] message = new byte[payloadSize];
            Arrays.fill(message, (byte) 0x00);
            final Collection<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
            startNanos.set(System.nanoTime());
            for (final PubSubClient client : clients)
                for (int pos = 0; pos < messagesPerClient; ++pos)
                    futures.add(client.publish(message, topic));
            for (final Future<Boolean> future : futures)
                assert future.get() : "Unable to publish message";

            // Wait for up to 1 minute
            assert remainingMessages.await(1, TimeUnit.MINUTES);
            // Now see if any *extra* messages trickle in... :)
            Thread.sleep(3 * 1000);
            assertFalse(tooManyMessages.get());
        } finally {
            final long endNanos = System.nanoTime();
            final int messagesSent = cluster.size() * clientsPerPort;
            final long nsPerMessage = (endNanos - startNanos.get()) / (messagesSent + messageReceivedCount);
            logger.info(
                    "Test finished: {} server ports, {} clients per port and {} messages of payload size {} bytes sent per client ({} total messages sent/received) - finished in {}ns ({}ns per message)",
                    Conversions.asArray(cluster.size(), clientsPerPort, messagesPerClient, payloadSize,
                            (messagesSent + messageReceivedCount), (endNanos - startNanos.get()), nsPerMessage));
            for (final PubSubClient client : clients)
                try {
                    client.stop();
                } catch (final InterruptedException ie) {
                    logger.error("Unable to shut down client[" + client + "] cleanly - may need to kill test", ie);
                }
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
     * <li>single port</li>
     * <li>two ports</li>
     * <li>three ports</li>
     * <li>random between 5 and 10 ports</li>
     * </ul>
     * 
     * @throws UnknownHostException
     */
    @DataProvider(name = "server-addresses")
    public Object[][] serverAddresses() throws UnknownHostException {
        final InetAddress loopback = InetAddress.getByName("127.0.0.1");
        final InetAddress local = InetAddress.getLocalHost(); // This is usually an external-facing address
        final Function<Integer, InetSocketAddress> serverAddressBuilder = new Function<Integer, InetSocketAddress>() {

            @Override
            public InetSocketAddress apply(final Integer input) {
                return new InetSocketAddress(input % 2 == 0 ? loopback : local, PubSubServer.DEFAULT_ADDRESS.getPort() + input);
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

}
