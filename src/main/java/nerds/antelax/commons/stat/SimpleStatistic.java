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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Joiner.MapJoiner;

public class SimpleStatistic {

    private static final MapJoiner toStringJoiner = Joiner.on("; ").useForNull("<null>").withKeyValueSeparator(" = ");
    private static final String    NO_NAME        = "Unnamed";

    private final String           name;
    private final AtomicLong       sampleCount;
    private final AtomicLong       minimum;
    private final AtomicLong       maximum;
    private final AtomicLong       sum;

    public SimpleStatistic() {
        this(NO_NAME);
    }

    public SimpleStatistic(final String name) {
        Preconditions.checkNotNull(name, "Statistic name must be set");
        this.name = name;
        sampleCount = new AtomicLong();
        minimum = new AtomicLong();
        maximum = new AtomicLong();
        sum = new AtomicLong();
    }

    public synchronized void addSample(final long sample) {
        sampleCount.incrementAndGet();
        if (sample < minimum.get())
            minimum.set(sample);
        if (sample > maximum.get())
            maximum.set(sample);
        sum.addAndGet(sample);
    }

    public String name() {
        return name;
    }

    public long sampleCount() {
        return sampleCount.get();
    }

    public long minimum() {
        return minimum.get();
    }

    public long maximum() {
        return maximum.get();
    }

    public long sum() {
        return sum.get();
    }

    public long average() {
        return sampleCount.get() == 0 ? 0 : sum.get() / sampleCount.get();
    }

    @Override
    public String toString() {
        final Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("sample count", sampleCount());
        fields.put("minimum", minimum());
        fields.put("maximum", maximum());
        fields.put("sum", sum());
        fields.put("average", average());
        return "{" + getClass().getSimpleName() + "(" + name + "): " + toStringJoiner.join(fields) + "}";
    }

}
