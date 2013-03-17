// Copyright 2013 Nathaniel Harward
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
