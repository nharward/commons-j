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

import static org.testng.AssertJUnit.assertArrayEquals;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import java.security.SecureRandom;
import java.util.UUID;

import nerds.antelax.commons.net.pubsub.ApplicationMessage;
import nerds.antelax.commons.net.pubsub.Message;
import nerds.antelax.commons.net.pubsub.MessageFormatException;
import nerds.antelax.commons.net.pubsub.SubscriptionMessage;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

public class MessageMarshalling {

    @Test
    public final void subscriptionOneTopic() throws MessageFormatException {
        final SubscriptionMessage sm = newSubscription();
        final Message.Builder builder = Message.newBuilder();
        for (final SubscriptionMessage reconstituted : new SubscriptionMessage[] { (SubscriptionMessage) unmarshall(marshall(sm)),
                (SubscriptionMessage) builder.add(marshall(sm)), (SubscriptionMessage) builder.add(marshall(sm)) }) {
            assertSame(sm.type, reconstituted.type);
            assertEquals(sm.sourceID(), reconstituted.sourceID());
            assertEquals(sm.serverID(), reconstituted.serverID());
            assertEquals(sm.subscribe, reconstituted.subscribe);
            assertArrayEquals(sm.topics, reconstituted.topics);
        }
    }

    @Test
    public final void subscriptionManyTopics() throws MessageFormatException {
        final SubscriptionMessage sm = newSubscription("topic-" + System.currentTimeMillis(),
                "topic2-" + System.currentTimeMillis(), "topic3-" + System.currentTimeMillis());
        final Message.Builder builder = Message.newBuilder();
        for (final SubscriptionMessage reconstituted : new SubscriptionMessage[] { (SubscriptionMessage) unmarshall(marshall(sm)),
                (SubscriptionMessage) builder.add(marshall(sm)), (SubscriptionMessage) builder.add(marshall(sm)) }) {
            assertSame(sm.type, reconstituted.type);
            assertEquals(sm.sourceID(), reconstituted.sourceID());
            assertEquals(sm.serverID(), reconstituted.serverID());
            assertEquals(sm.subscribe, reconstituted.subscribe);
            assertArrayEquals(sm.topics, reconstituted.topics);
        }
    }

    @Test
    public final void application() throws MessageFormatException {
        final ApplicationMessage am = newApplication();
        final Message.Builder builder = Message.newBuilder();
        for (final ApplicationMessage reconstituted : new ApplicationMessage[] { (ApplicationMessage) unmarshall(marshall(am)),
                (ApplicationMessage) builder.add(marshall(am)), (ApplicationMessage) builder.add(marshall(am)) }) {
            assertSame(am.type, reconstituted.type);
            assertEquals(am.sourceID(), reconstituted.sourceID());
            assertEquals(am.serverID(), reconstituted.serverID());
            assertEquals(am.topic, reconstituted.topic);
            assertEquals(am.applicationBody(), reconstituted.applicationBody());
        }
    }

    private static SubscriptionMessage newSubscription() {
        return newSubscription("topic-" + System.currentTimeMillis());
    }

    private static ApplicationMessage newApplication() {
        final byte[] data = new byte[new SecureRandom().nextInt(2048)];
        for (int pos = 0; pos < data.length; ++pos)
            data[pos] = (byte) (pos % Byte.MAX_VALUE);
        final ApplicationMessage rv = new ApplicationMessage(data, "topic-" + System.currentTimeMillis());
        rv.sourceID(UUID.randomUUID());
        rv.serverID(UUID.randomUUID());
        return rv;
    }

    private static SubscriptionMessage newSubscription(final String... topics) {
        final boolean subscribed = System.currentTimeMillis() % 2 == 0 ? true : false;
        final SubscriptionMessage sm = new SubscriptionMessage(subscribed, topics);
        sm.sourceID(UUID.randomUUID());
        sm.serverID(UUID.randomUUID());
        return sm;
    }

    private static ChannelBuffer marshall(final Message m) {
        final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(m.headerSize() + m.estimatedBodySize());
        buffer.markReaderIndex();
        m.marshall(buffer);
        buffer.resetReaderIndex();
        return buffer;
    }

    private static Message unmarshall(final ChannelBuffer buffer) throws MessageFormatException {
        return Message.newBuilder().add(buffer);
    }

}
