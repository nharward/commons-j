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

package us.harward.commons.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import us.harward.commons.util.DbC;

/**
 * An ordered iterator over other iterators/collections. Supports the {@link #remove()} method to the extent that the underlying
 * {@link Iterator} currently being accessed does.
 */
public class AggregateIterator<T> implements Iterator<T> {

    final Iterator<Iterator<T>> iterators;
    Iterator<T>                 current;

    public AggregateIterator(final Iterator<Iterator<T>> iterators) {
        DbC.notNull(iterators);
        this.iterators = iterators;
        if (this.iterators.hasNext())
            current = this.iterators.next();
    }

    public AggregateIterator(final Collection<Collection<T>> collections) {
        DbC.notNull(collections);
        final List<Iterator<T>> iteratorList = new LinkedList<Iterator<T>>();
        for (final Collection<T> collection : collections)
            iteratorList.add(collection.iterator());
        this.iterators = iteratorList.iterator();
        if (this.iterators.hasNext())
            current = this.iterators.next();
    }

    public AggregateIterator(final Iterator<T>... iterators) {
        DbC.notNull(iterators);
        final List<Iterator<T>> iteratorList = new LinkedList<Iterator<T>>();
        for (final Iterator<T> iterator : iterators)
            iteratorList.add(iterator);
        this.iterators = iteratorList.iterator();
        if (this.iterators.hasNext())
            current = this.iterators.next();
    }

    public AggregateIterator(final Collection<T>... collections) {
        DbC.notNull(collections);
        final List<Iterator<T>> iteratorList = new LinkedList<Iterator<T>>();
        for (final Collection<T> collection : collections)
            iteratorList.add(collection.iterator());
        this.iterators = iteratorList.iterator();
        if (this.iterators.hasNext())
            current = this.iterators.next();
    }

    @Override
    public boolean hasNext() {
        if (current != null && current.hasNext()) {
            return true;
        } else if (!iterators.hasNext()) {
            current = null;
            return false;
        } else {
            current = iterators.next();
            return current.hasNext();
        }
    }

    @Override
    public T next() {
        if (current == null)
            throw new NoSuchElementException();
        else
            return current.next();
    }

    @Override
    public void remove() {
        if (current == null)
            throw new IllegalStateException();
        else
            current.remove();
    }

}
