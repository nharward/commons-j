// Copyright 2011 Nathaniel Harward
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
package nerds.antelax.commons.base;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * A {@link Function} that forwards to other {@link Function}s based on the return value of a {@link Predicate}.
 * 
 * @param <F>
 *            the input object type
 * @param <T>
 *            the output object type
 */
class DecisionFunction<F, T> implements Function<F, T> {

    private final Predicate<F>   predicate;
    private final Function<F, T> successFunction;
    private final Function<F, T> failureFunction;

    /**
     * Constructs a new {@link Function} that delegates to other {@link Function}s based on the results of a {@link Predicate}.
     * 
     * @param predicate
     *            the {@link Predicate} on which to base the decision
     * @param successFunction
     *            the {@link Function} to invoke if {@link #predicate} returns <code>true</code>
     * @param failureFunction
     *            the {@link Function} to invoke if {@link #predicate} returns <code>false</code>
     */
    DecisionFunction(final Predicate<F> predicate, final Function<F, T> successFunction, final Function<F, T> failureFunction) {
        Preconditions.checkNotNull(predicate);
        Preconditions.checkNotNull(successFunction);
        Preconditions.checkNotNull(failureFunction);
        this.predicate = predicate;
        this.successFunction = successFunction;
        this.failureFunction = failureFunction;
    }

    @Override
    public T apply(final F argument) {
        return predicate.apply(argument) ? successFunction.apply(argument) : failureFunction.apply(argument);
    }

}
