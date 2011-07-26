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

package us.harward.commons.net.mom.pubsub;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.buffer.ChannelBuffers;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

public final class Megaphone implements PubSubClient.NetworkConnectionLifecycleCallback {

    private final AtomicBoolean      connected;
    private final PubSubClient       client;
    private final Collection<String> topics;

    private final Lock               lock;
    private final Condition          condition;

    Megaphone(final String... topics) throws Exception {
        Preconditions.checkNotNull(topics);
        Preconditions.checkArgument(topics.length > 0);
        lock = new ReentrantLock();
        condition = lock.newCondition();
        connected = new AtomicBoolean(false);
        this.topics = new LinkedList<String>();
        client = new PubSubClient(Executors.newCachedThreadPool(), this, 5, TimeUnit.SECONDS, new InetSocketAddress(
                InetAddress.getLocalHost(), PubSubServer.DEFAULT_ADDRESS.getPort()));
        for (final String topic : topics) {
            this.topics.add(topic);
            client.subscribe(topic, new MessagePrinter(topic));
        }
    }

    private void repl() throws Exception {
        client.start();
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (;;) {
            if (connected.get()) {
                System.out.printf("Enter a message to broadcast on topics %s (or 'quit'): ", topics);
                System.out.flush();
                final String line = br.readLine();
                if ("quit".equals(line)) {
                    break;
                }
                if (!connected.get()) {
                    System.out.println("Connection was dropped, message not sent");
                    continue;
                }
                final byte[] message = line.getBytes(Charsets.UTF_8);
                for (final String topic : topics)
                    client.publish(message, topic);
            } else {
                lock.lock();
                try {
                    if (!connected.get()) {
                        System.out.println("<waiting for [re]connect...>");
                        condition.await();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        br.close();
        client.stop();
    }

    @Override
    public void connectionDown(final SocketAddress endpoint) {
        connected.set(false);
    }

    @Override
    public void connectionUp(final SocketAddress endpoint) {
        connected.set(true);
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static void main(final String... args) throws Throwable {
        new Megaphone(args).repl();
    }

    private static final class MessagePrinter implements PubSubClient.MessageCallback {

        private final String topic;

        private MessagePrinter(final String topic) {
            this.topic = topic;
        }

        @Override
        public void onMessage(final ByteBuffer message) throws Exception {
            System.out.printf("%n** Incoming message on topic[%s]: '%s'%n", topic,
                    ChannelBuffers.wrappedBuffer(message).toString(Charsets.UTF_8));
        }

    }
}
