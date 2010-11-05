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

package us.harward.commons.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class WorkflowTest {

    private static class Multiplier implements Function<Long, Long> {

        private final long multiplier;

        private Multiplier(final long multiplier) {
            this.multiplier = multiplier;
        }

        public Long apply(final Long input) {
            return multiplier * input;
        };

    }

    private static class EvenPredicate implements Predicate<Long> {

        @Override
        public boolean apply(final Long input) {
            return input % 2 == 0;
        }

    }

    private static class OddPredicate extends EvenPredicate {

        @Override
        public boolean apply(final Long input) {
            return !super.apply(input);
        }

    }

    private final AtomicLong      result = new AtomicLong();
    private final Sink<Long>      sink   = new Sink<Long>() {

                                             @Override
                                             public void drop(final Long t) {
                                                 result.addAndGet(t);
                                             }
                                         };
    private final Predicate<Long> odd    = new OddPredicate();
    private final Predicate<Long> even   = new EvenPredicate();
    private final Multiplier      triple = new Multiplier(3);

    @Before
    public final void setup() {
        result.set(0);
    }

    @Test
    public final void create() {
        final Workflow<Long, Long> wf = Workflow.create(even, triple, even);
        assert wf != null;
        assert result.get() == 0;
        wf.apply(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L), sink);
        assert result.get() == (0L + 2L + 4L + 6L) * 3;
    }

    @Test
    public final void wrap() {
        final Workflow<Long, Long> wf = Workflow.wrap(triple);
        assert wf != null;
        assert result.get() == 0;
        wf.apply(Arrays.asList(1L, 2L, 3L, 4L, 5L), sink);
        assert result.get() == (1L + 2L + 3L + 4L + 5L) * 3;
    }

    @Test
    public final void compose() {
        final Workflow<Long, Long> times27 = Workflow.compose(Workflow.wrap(triple), Workflow.compose(Workflow.wrap(triple),
                Workflow.wrap(triple)));
        assert times27 != null;
        assert result.get() == 0;
        times27.apply(Arrays.asList(1L, 2L, 3L, 4L, 5L, 1001L), sink);
        assert result.get() == (1L + 2L + 3L + 4L + 5L + 1001L) * 27;
    }

    @Test
    public final void inputPredicate() {
        final Workflow<Long, Long> wf = Workflow.create(even, triple, null);
        assert wf != null;
        assert result.get() == 0;
        wf.apply(Arrays.asList(3L, 2L, 7L, 6L), sink);
        assert result.get() == (2L + 6L) * 3;
    }

    @Test
    public final void outputPredicate() {
        final Workflow<Long, Long> wf = Workflow.create(null, triple, odd);
        assert wf != null;
        assert result.get() == 0;
        wf.apply(Arrays.asList(3L, 2L, 7L, 6L), sink);
        assert result.get() == (3L + 7L) * 3;
    }

    @Test
    public final void mixedPredicates() {
        final Workflow<Long, Long> wf = Workflow.create(even, triple, odd);
        assert wf != null;
        assert result.get() == 0;
        wf.apply(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L), sink);
        assert result.get() == 0L;
    }

    @Test
    public final void mixedTypes() {
        final Function<String, Long> digitsFromString = new Function<String, Long>() {

            @Override
            public Long apply(final String input) {
                final String digitsOnly = input.replaceAll("\\D", "");
                return Long.decode(digitsOnly);
            }
        };
        final Predicate<String> hasDigits = new Predicate<String>() {

            @Override
            public boolean apply(final String input) {
                return input != null && input.matches(".*\\d.*");
            }
        };
        final Predicate<Long> nnLong = Predicates.notNull();
        final Workflow<String, Long> toDigitsWF = Workflow.create(hasDigits, digitsFromString, nnLong);
        final Workflow<String, Long> digitsTriple = Workflow.compose(Workflow.create(odd, triple, odd), toDigitsWF);
        assert result.get() == 0L;
        digitsTriple.apply(Arrays.asList("Nothing here", "Hello, world1", "foo2bar", "Good3bye, cruel wor7d", "1a23b45c"), sink);
        assert result.get() == (1L + 37L + 12345L) * 3;
    }

    @Test
    public final void predicateSinks() {
        final AtomicLong preCount = new AtomicLong(0);
        final Sink<Long> preSink = new Sink<Long>() {

            @Override
            public void drop(final Long t) {
                preCount.addAndGet(t);
            }
        };
        final AtomicLong postCount = new AtomicLong(0);
        final Sink<Long> postSink = new Sink<Long>() {

            @Override
            public void drop(final Long t) {
                postCount.addAndGet(t);
            }
        };
        assert preCount.get() == 0;
        assert postCount.get() == 0;
        assert result.get() == 0;
        final Workflow<Long, Long> wf = Workflow.create(even, triple, odd);
        wf.apply(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L), sink, preSink, postSink);
        assert preCount.get() == 1L + 3L + 5L;
        assert postCount.get() == (0L + 2L + 4L) * 3;
        assert result.get() == 0L;
    }

    @Test
    public final void composedPredicateSinks() {
        final AtomicLong preCount = new AtomicLong(0);
        final Sink<Long> preSink = new Sink<Long>() {

            @Override
            public void drop(final Long t) {
                preCount.addAndGet(t);
            }
        };
        final AtomicLong postCount = new AtomicLong(0);
        final Sink<Long> postSink = new Sink<Long>() {

            @Override
            public void drop(final Long t) {
                postCount.addAndGet(t);
            }
        };
        final Predicate<Long> equals9 = new Predicate<Long>() {

            @Override
            public boolean apply(final Long input) {
                return input == 9;
            }
        };
        final Predicate<Long> not27 = new Predicate<Long>() {

            @Override
            public boolean apply(final Long input) {
                return input != 27;
            }
        };
        assert preCount.get() == 0;
        assert postCount.get() == 0;
        assert result.get() == 0;
        final Workflow<Long, Long> stage1 = Workflow.wrap(triple);
        final Workflow<Long, Long> stage2 = Workflow.create(odd, triple, equals9);
        final Workflow<Long, Long> stage3 = Workflow.create(null, triple, not27);

        final Workflow<Long, Long> wf = Workflow.compose(stage3, Workflow.compose(stage2, stage1));
        assert preCount.get() == 0;
        assert postCount.get() == 0;
        assert result.get() == 0;
        wf.apply(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), sink, preSink, postSink);
        assert preCount.get() == 2L + 3L + 4L + 5L + 6L;
        assert postCount.get() == (1L) * 3 * 3 * 3;
        assert result.get() == 0L;
    }

}
