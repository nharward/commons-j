package nerds.antelax.commons.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class NetUtilTest {

    @Test
    public void hostPortPairsFromString() {
        final Collection<InetSocketAddress> expected = new LinkedList<InetSocketAddress>();
        final int iterations = new SecureRandom().nextInt(20) + 1;
        final StringBuffer sb = new StringBuffer();
        for (int pos = 0; pos < iterations; ++pos) {
            if (pos > 0)
                sb.append(",  \t");
            expected.add(new InetSocketAddress("myhost" + pos, pos));
            sb.append("myhost" + pos + ":" + pos);
        }
        final Collection<InetSocketAddress> actual = NetUtil.hostPortPairsFromString(sb.toString(), iterations);
        assert actual.containsAll(expected);
        assert expected.containsAll(actual);
    }

    @Test(dataProvider = "conflicting")
    public void willConflictDuringBind(final InetSocketAddress local, final InetSocketAddress unknown) {
        assert NetUtil.bindConflict(local).apply(unknown);
    }

    @Test(dataProvider = "non-conflicting")
    public void willNotConflictDuringBind(final InetSocketAddress local, final InetSocketAddress unknown) {
        assert !NetUtil.bindConflict(local).apply(unknown);
    }

    @DataProvider(name = "conflicting", parallel = true)
    public static Object[][] conflictingAddresses() throws UnknownHostException {
        final InetAddress local = InetAddress.getByName("127.0.0.1");
        final InetAddress host = InetAddress.getLocalHost();
        return new Object[][] { new Object[] { new InetSocketAddress(local, 1000), new InetSocketAddress(local, 1000) },
                new Object[] { new InetSocketAddress(host, 1000), new InetSocketAddress(host, 1000) },
                new Object[] { new InetSocketAddress(1000), new InetSocketAddress(local, 1000) },
                new Object[] { new InetSocketAddress(1000), new InetSocketAddress(host, 1000) },
                new Object[] { new InetSocketAddress(local, 1000), new InetSocketAddress(1000) },
                new Object[] { new InetSocketAddress(host, 1000), new InetSocketAddress(1000) } };
    }

    @DataProvider(name = "non-conflicting", parallel = true)
    public static Object[][] nonConflictingAddresses() throws UnknownHostException {
        final InetAddress local = InetAddress.getByName("127.0.0.1");
        final InetAddress host = InetAddress.getLocalHost();
        return new Object[][] { new Object[] { new InetSocketAddress(local, 1000), new InetSocketAddress(local, 1001) },
                new Object[] { new InetSocketAddress(host, 1000), new InetSocketAddress(host, 1001) },
                new Object[] { new InetSocketAddress(1000), new InetSocketAddress(local, 1001) },
                new Object[] { new InetSocketAddress(1000), new InetSocketAddress(host, 1001) },
                new Object[] { new InetSocketAddress(local, 1000), new InetSocketAddress(1001) },
                new Object[] { new InetSocketAddress(host, 1000), new InetSocketAddress(1001) } };
    }

}
