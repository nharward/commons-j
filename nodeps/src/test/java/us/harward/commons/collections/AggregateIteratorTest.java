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

import static org.junit.Assert.fail;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

public class AggregateIteratorTest {

    private int numberOfTrials = 0;

    @Before
    public void random() {
        numberOfTrials = new SecureRandom().nextInt(1000) + 101;
    }

    @Test
    public final void iteratorOfIterators() {
        final Collection<Iterator<Integer>> collection = new LinkedList<Iterator<Integer>>();
        for (int pos = 0; pos < numberOfTrials; ++pos) {
            collection.add(Arrays.asList(0).iterator());
            collection.add(Arrays.asList(1, 2).iterator());
            collection.add(Arrays.asList(3, 4, 5).iterator());
            collection.add(Arrays.asList(6, 7, 8, 9).iterator());
        }
        final AggregateIterator<Integer> iterator = new AggregateIterator<Integer>(collection.iterator());
        for (int pos = 0; pos < numberOfTrials; ++pos) {
            for (int expected = 0; expected < 10; ++expected) {
                assert iterator.hasNext() : "Expecting another element";
                final Integer result = iterator.next();
                assert result == expected : "Expecting " + expected + " but got result " + result;
            }
        }
        assert !iterator.hasNext();
        assert !new AggregateIterator<Integer>(new LinkedList<Iterator<Integer>>().iterator()).hasNext();
    }

    @Test
    public final void collectionOfCollections() {
        final Collection<Collection<Integer>> collection = new LinkedList<Collection<Integer>>();
        for (int pos = 0; pos < numberOfTrials; ++pos) {
            collection.add(Arrays.asList(0));
            collection.add(Arrays.asList(1, 2));
            collection.add(Arrays.asList(3, 4, 5));
            collection.add(Arrays.asList(6, 7, 8, 9));
        }
        final AggregateIterator<Integer> iterator = new AggregateIterator<Integer>(collection);
        for (int pos = 0; pos < numberOfTrials; ++pos) {
            for (int expected = 0; expected < 10; ++expected) {
                assert iterator.hasNext() : "Expecting another element";
                final Integer result = iterator.next();
                assert result == expected : "Expecting " + expected + " but got result " + result;
            }
        }
        assert !iterator.hasNext();
        assert !new AggregateIterator<Integer>(new LinkedList<Collection<Integer>>()).hasNext();
    }

    @Test
    @SuppressWarnings("unchecked")
    public final void arrayOfIterators() {
        final AggregateIterator<Integer> iterator = new AggregateIterator<Integer>(Arrays.asList(0).iterator(), Arrays.asList(1, 2)
                .iterator(), Arrays.asList(3, 4, 5).iterator(), Arrays.asList(6, 7, 8, 9).iterator());
        for (int expected = 0; expected < 10; ++expected) {
            assert iterator.hasNext() : "Expecting another element";
            final Integer result = iterator.next();
            assert result == expected : "Expecting " + expected + " but got result " + result;
        }
        assert !iterator.hasNext();
        assert !new AggregateIterator<Integer>(Collections.EMPTY_LIST.iterator()).hasNext();
    }

    @Test
    @SuppressWarnings("unchecked")
    public final void arrayOfCollections() {
        final AggregateIterator<Integer> iterator = new AggregateIterator<Integer>(Arrays.asList(0), Arrays.asList(1, 2), Arrays
                .asList(3, 4, 5), Arrays.asList(6, 7, 8, 9));
        for (int expected = 0; expected < 10; ++expected) {
            assert iterator.hasNext() : "Expecting another element";
            final Integer result = iterator.next();
            assert result == expected : "Expecting " + expected + " but got result " + result;
        }
        assert !iterator.hasNext();
        assert !new AggregateIterator<Integer>(Collections.EMPTY_LIST).hasNext();
    }

    @Test
    @SuppressWarnings("unchecked")
    public final void remove() {
        final List<String> strings = new LinkedList<String>();
        strings.add("one");
        strings.add("two");
        strings.add("three");
        final AggregateIterator<String> iterator = new AggregateIterator<String>(strings.iterator());
        assert iterator.hasNext();
        assert "one".equals(iterator.next());
        iterator.remove();
        assert iterator.hasNext();
        assert "two".equals(iterator.next());
        assert iterator.hasNext();
        assert "three".equals(iterator.next());
        iterator.remove();
        assert strings.size() == 1;
        assert strings.contains("two");
        assert !iterator.hasNext();
    }

    @Test
    @SuppressWarnings("unchecked")
    public final void exceptionCases() {
        final Object o = new Object();
        final Collection<Object> collection = new LinkedList<Object>();
        collection.add(o);
        final AggregateIterator<Object> iterator = new AggregateIterator<Object>(collection);
        assert iterator.hasNext();
        try {
            iterator.remove();
            fail("Expected next() to fail");
        } catch (final IllegalStateException ise) {
            assert o == iterator.next();
            iterator.remove();
            try {
                iterator.remove();
                fail("Expected remove() to fail");
            } catch (final IllegalStateException ise2) {
                assert !iterator.hasNext();
                try {
                    iterator.next();
                } catch (final NoSuchElementException nsee) {
                    assert collection.isEmpty();
                }
            }
        }
    }

}
