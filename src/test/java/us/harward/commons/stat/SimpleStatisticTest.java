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

package us.harward.commons.stat;

import java.security.SecureRandom;
import java.util.Random;

import org.junit.Test;

public class SimpleStatisticTest {

    @Test
    public final void testSimpleStatistic() {
        final SimpleStatistic stat = new SimpleStatistic();
        assert stat.name() != null : "Default constructor causes null name";
        assert stat.sampleCount() == 0 : "Sample count != 0 -> " + stat.sampleCount();
        assert stat.minimum() == 0 : "Minimum != 0 -> " + stat.minimum();
        assert stat.maximum() == 0 : "Maximum != 0 -> " + stat.maximum();
        assert stat.sum() == 0 : "Sum != 0 -> " + stat.sum();
        assert stat.average() == 0 : "Average != 0 -> " + stat.average();
    }

    @Test
    public final void testSimpleStatisticString() {
        final String name = "Some string" + System.currentTimeMillis();
        final SimpleStatistic stat = new SimpleStatistic(name);
        assert stat.name() != null : "Named constructor causes null name";
        assert name.equals(stat.name()) : "Named constructor changes name";
        assert stat.sampleCount() == 0 : "Sample count != 0 -> " + stat.sampleCount();
        assert stat.minimum() == 0 : "Minimum != 0 -> " + stat.minimum();
        assert stat.maximum() == 0 : "Maximum != 0 -> " + stat.maximum();
        assert stat.sum() == 0 : "Sum != 0 -> " + stat.sum();
        assert stat.average() == 0 : "Average != 0 -> " + stat.average();
    }

    @Test
    public final void testAddSample() {
        final SimpleStatistic stat = new SimpleStatistic();
        stat.addSample(0);
        stat.addSample(1);
        stat.addSample(-1);
        assert stat.sampleCount() == 3 : "Sample count != 3 -> " + stat.sampleCount();
        assert stat.minimum() == -1 : "Minimum != -1 -> " + stat.minimum();
        assert stat.maximum() == 1 : "Maximum != 1 -> " + stat.maximum();
        assert stat.sum() == 0 : "Sum != 0 -> " + stat.sum();
        assert stat.average() == 0 : "Average != 0 -> " + stat.average();
        stat.addSample(0);
        stat.addSample(1000);
        stat.addSample(-1000);
        assert stat.sampleCount() == 6 : "Sample count != 6 -> " + stat.sampleCount();
        assert stat.minimum() == -1000 : "Minimum != -1 -> " + stat.minimum();
        assert stat.maximum() == 1000 : "Maximum != 1 -> " + stat.maximum();
        assert stat.sum() == 0 : "Sum != 0 -> " + stat.sum();
        assert stat.average() == 0 : "Average != 0 -> " + stat.average();
    }

    @Test
    public final void testSampleCount() {
        for (int pos = 0; pos < 10; ++pos) {
            final SimpleStatistic stat = new SimpleStatistic();
            final int samples = new SecureRandom().nextInt(10000);
            for (int pos2 = 0; pos2 < samples; ++pos2)
                stat.addSample(pos2);
            assert stat.sampleCount() == samples : "Sample count != " + samples + " -> " + stat.sampleCount();
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
        assert stat.minimum() == expected : "Minimum != " + expected + " -> " + stat.minimum();
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
        assert stat.maximum() == expected : "Maximum != " + expected + " -> " + stat.maximum();
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
        assert stat.sum() == expected : "Sum != " + expected + " -> " + stat.sum();
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
        assert stat.average() == expected : "Average != " + expected + " -> " + stat.average();
    }

}
