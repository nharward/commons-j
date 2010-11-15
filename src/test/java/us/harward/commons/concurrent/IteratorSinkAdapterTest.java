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

package us.harward.commons.concurrent;

import java.security.SecureRandom;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;

public class IteratorSinkAdapterTest {

    @Test
    public final void goodConstructor() {
        for (int pos = 1; pos < 100; ++pos)
            new IteratorSinkAdapter<Object>(pos);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void zeroCapacityConstructor() {
        new IteratorSinkAdapter<Object>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void negativeCapacityConstructor() {
        new IteratorSinkAdapter<Object>(-1);
    }

    @Test
    public final void drop() {
        final IteratorSinkAdapter<Object> isa = new IteratorSinkAdapter<Object>(1);
        final Random random = new SecureRandom();
        final int numObjectsToFillPerThread = 1 + random.nextInt(100);
        final int numThreads = 1 + random.nextInt(10);
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);
        final Runnable filler = new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(random.nextInt(3000));
                } catch (final InterruptedException ie) {
                    // Ignore
                }
                try {
                    for (int pos = 0; pos < numObjectsToFillPerThread; ++pos)
                        isa.drop(this);
                } finally {
                    try {
                        barrier.await();
                        isa.finish();
                    } catch (final InterruptedException ie) {
                        throw new RuntimeException("FIXME", ie);
                    } catch (final BrokenBarrierException bbe) {
                        throw new RuntimeException("FIXME", bbe);
                    }
                }
            }
        };
        for (int pos = 0; pos < numThreads; ++pos)
            new Thread(filler).start();
        int numObjectsRead = 0;
        while (isa.hasNext()) {
            final Object o = isa.next();
            assert o == filler;
            ++numObjectsRead;
        }
        assert numObjectsRead == numObjectsToFillPerThread * numThreads;
    }

    @Test(expected = ConcurrentModificationException.class)
    public final void finish() {
        final IteratorSinkAdapter<Object> isa = new IteratorSinkAdapter<Object>(1);
        isa.finish();
        assert !isa.hasNext();
        isa.drop(this);
    }

    @Test(expected = NoSuchElementException.class)
    public final void empty() {
        final IteratorSinkAdapter<Object> isa = new IteratorSinkAdapter<Object>(1);
        isa.finish();
        assert !isa.hasNext();
        isa.next();
    }

    @Test
    public final void idempotence() {
        final IteratorSinkAdapter<Object> isa = new IteratorSinkAdapter<Object>(1);
        isa.finish();
        assert !isa.hasNext();
        for (int pos = 0; pos < 1000; ++pos) {
            assert !isa.hasNext();
            isa.finish();
            assert !isa.hasNext();
        }
    }

}
