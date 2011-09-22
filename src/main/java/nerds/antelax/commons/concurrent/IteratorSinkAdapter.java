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

package nerds.antelax.commons.concurrent;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nerds.antelax.commons.util.Sink;


import com.google.common.base.Preconditions;

/**
 * This class is effectively a join point between threads. You can "fill" the iterator in one thread using the {@link Sink} methods
 * (along with {@link #finish()} to indicate no more items are forthcoming) and read using the {@link Iterator} methods in another.
 * <p>
 * Note that while many threads can add items using the {@link Sink} interface, only a single reader should be accessing an instance
 * of this class at a time (as with any {@link Iterator}).
 */
public class IteratorSinkAdapter<T> implements Iterator<T>, Sink<T> {

    private final BlockingQueue<T> buffer;
    private final Lock             lock;
    private final Condition        dropEvent;
    private final Condition        drainEvent;
    private final AtomicBoolean    finished;

    /**
     * Constructs a new {@link IteratorSinkAdapter} with the given capacity.
     * 
     * @param capacity
     *            the size of the internal buffer; once filled subsequent {@link #drop(Object)} method calls will block until space
     *            is available.
     * @throws IllegalArgumentException
     *             if capacity is less than or equal to zero
     */
    public IteratorSinkAdapter(final int capacity) throws IllegalArgumentException {
        Preconditions.checkArgument(capacity > 0, "Capacity must be greater than zero, you passed: " + capacity);
        buffer = new LinkedBlockingQueue<T>(capacity);
        lock = new ReentrantLock();
        dropEvent = lock.newCondition();
        drainEvent = lock.newCondition();
        Preconditions.checkArgument(dropEvent != drainEvent, "Two brand new conditions are the same instance?  Boo!");
        finished = new AtomicBoolean(false);
    }

    /**
     * Adds an item to be iterated over. If this class has been "finished" (by having the {@link #finish()} method invoked) a
     * {@link ConcurrentModificationException} will be thrown.
     */
    @Override
    public void drop(final T t) throws ConcurrentModificationException {
        lock.lock();
        try {
            if (finished.get())
                throw new ConcurrentModificationException("finished() has already been called");
            while (buffer.remainingCapacity() == 0)
                drainEvent.awaitUninterruptibly();
            buffer.add(t);
            dropEvent.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Calling this method indicates that no more items will be dropped into it, so the {@link Iterator} side of the class can end.
     * This method is idempotent.
     */
    public void finish() {
        lock.lock();
        try {
            if (!finished.get()) {
                finished.set(true);
                dropEvent.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Indicates whether or not there is another item in the iterator. If the internal buffer is empty, this method will block until
     * either {@link #finish()} is called or another item is added via {@link #drop(Object)}.
     */
    @Override
    public boolean hasNext() {
        lock.lock();
        try {
            if (!buffer.isEmpty()) {
                return true;
            } else if (finished.get()) {
                return false;
            } else {
                dropEvent.awaitUninterruptibly();
                return !buffer.isEmpty();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves the next available item. If the internal buffer is empty, this method will block until either {@link #finish()} is
     * called or another item is added via {@link #drop(Object)}.
     */
    @Override
    public T next() {
        lock.lock();
        try {
            if (!buffer.isEmpty()) {
                drainEvent.signalAll();
                return buffer.poll();
            } else {
                if (finished.get()) {
                    throw new NoSuchElementException("Are you reading this iterator from multiple threads?  Using hasNext()?");
                } else {
                    dropEvent.awaitUninterruptibly();
                    if (finished.get()) {
                        throw new NoSuchElementException("Are you reading this iterator from multiple threads?  Using hasNext()?");
                    } else {
                        drainEvent.signalAll();
                        return buffer.poll();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(getClass().getName() + " does not support the remove() operation");
    }

}
