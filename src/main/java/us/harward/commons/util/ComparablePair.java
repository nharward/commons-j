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

/**
 * Same as {@link Pair} but with both members {@link Comparable} as well. Obeys the comparison contract described here:
 * http://www.cplusplus.com/reference/std/utility/pair/
 */
public final class ComparablePair<T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends Pair<T1, T2> implements
        Comparable<ComparablePair<T1, T2>> {

    public ComparablePair(final T1 first, final T2 second) {
        super(first, second);
    }

    public ComparablePair(final ComparablePair<T1, T2> pair) {
        super(pair);
    }

    @Override
    public int compareTo(final ComparablePair<T1, T2> cp) {
        final int firstCompare = first().compareTo(cp.first());
        return firstCompare != 0 ? firstCompare : second().compareTo(cp.second());
    }

}
