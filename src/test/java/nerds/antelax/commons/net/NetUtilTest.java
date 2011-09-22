package nerds.antelax.commons.net;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedList;

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

}
