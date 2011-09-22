// Copyright 2010 Nathaniel Harward
//
// This file is part of ndh-commons.
//
// ndh-commons is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// ndh-commons is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with ndh-commons. If not, see <http://www.gnu.org/licenses/>.

package nerds.antelax.commons.stat;

import java.security.SecureRandom;
import java.util.Random;

import junit.framework.Assert;

import nerds.antelax.commons.stat.SimpleStatistic;

import org.junit.Test;

public class SimpleStatisticTest {

    @Test
    public final void testSimpleStatistic() {
        final SimpleStatistic stat = new SimpleStatistic();
        checkStat(stat, "Unnamed", 0, 0, 0, 0, 0);
    }

    @Test
    public final void testSimpleStatisticString() {
        final String name = "Some string" + System.currentTimeMillis();
        final SimpleStatistic stat = new SimpleStatistic(name);
        checkStat(stat, name, 0, 0, 0, 0, 0);
    }

    @Test
    public final void testAddSample() {
        final SimpleStatistic stat = new SimpleStatistic();
        stat.addSample(0);
        stat.addSample(1);
        stat.addSample(-1);
        checkStat(stat, "Unnamed", 3, -1, 1, 0, 0);

        stat.addSample(0);
        stat.addSample(1000);
        stat.addSample(-1000);
        checkStat(stat, "Unnamed", 6, -1000, 1000, 0, 0);
    }

    @Test
    public final void testSampleCount() {
        for (int pos = 0; pos < 10; ++pos) {
            final SimpleStatistic stat = new SimpleStatistic();
            final int samples = new SecureRandom().nextInt(10000);
            for (int pos2 = 0; pos2 < samples; ++pos2)
                stat.addSample(pos2);
            Assert.assertEquals(samples, stat.sampleCount());
        }
    }

    @Test
    public final void testMinimum() {
        final Random random = new SecureRandom();
        long expected = Long.MAX_VALUE;
        final SimpleStatistic stat = new SimpleStatistic();
        for (int pos = 0; pos < 10000; ++pos) {
            final long newValue = random.nextLong();
            stat.addSample(newValue);
            if (newValue < expected)
                expected = newValue;
        }
        Assert.assertEquals(expected, stat.minimum());
    }

    @Test
    public final void testMaximum() {
        final Random random = new SecureRandom();
        long expected = Long.MIN_VALUE;
        final SimpleStatistic stat = new SimpleStatistic();
        for (int pos = 0; pos < 10000; ++pos) {
            final long newValue = random.nextLong();
            stat.addSample(newValue);
            if (newValue > expected)
                expected = newValue;
        }
        Assert.assertEquals(expected, stat.maximum());
    }

    @Test
    public final void testSum() {
        final Random random = new SecureRandom();
        final SimpleStatistic stat = new SimpleStatistic();
        long expected = 0;
        for (int pos = 0; pos < 10000; ++pos) {
            final long newValue = random.nextLong();
            stat.addSample(newValue);
            expected += newValue;
        }
        Assert.assertEquals(expected, stat.sum());
    }

    @Test
    public final void testAverage() {
        final Random random = new SecureRandom();
        final SimpleStatistic stat = new SimpleStatistic();
        long expected = 0;
        for (int pos = 0; pos < 10000; ++pos) {
            final long newValue = random.nextLong();
            stat.addSample(newValue);
            expected += newValue;
        }
        expected /= 10000;
        Assert.assertEquals(expected, stat.average());
    }

    private static void checkStat(final SimpleStatistic stat, final String name, long expectedCnt, long expectedMin, long expectedMax, long expectedSum, long expectedAvg) {
        Assert.assertNotNull("Named constructor causes null name", stat.name());
        Assert.assertEquals("Named constructor changes name", name, stat.name());
        Assert.assertEquals("Sample count != 0 -> " + stat.sampleCount(), expectedCnt, stat.sampleCount());
        Assert.assertEquals("Minimum != 0 -> " + stat.minimum(), expectedMin, stat.minimum());
        Assert.assertEquals("Maximum != 0 -> " + stat.maximum(), expectedMax, stat.maximum());
        Assert.assertEquals("Sum != 0 -> " + stat.sum(), expectedSum, stat.sum());
        Assert.assertEquals("Average != 0 -> " + stat.average(), expectedAvg, stat.average());
    }
}
