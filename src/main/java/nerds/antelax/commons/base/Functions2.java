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
import com.google.common.base.Predicate;

/**
 * Some add-ons I feel are missing from Guava. Should they appear in a later release of Guava they will be removed here.
 */
public final class Functions2 {

    /**
     * @param predicate
     *            the {@link Predicate} on which to base the decision
     * @param successFunction
     *            the {@link Function} to invoke if {@link #predicate} returns <code>true</code>
     * @param failureFunction
     *            the {@link Function} to invoke if {@link #predicate} returns <code>false</code>
     * @return a {@link Function} that delegates to other {@link Function}s based on the results of a {@link Predicate}.
     */
    public static <F, T> Function<F, T> decisionFunction(final Predicate<F> predicate, final Function<F, T> successFunction,
            final Function<F, T> failureFunction) {
        return new DecisionFunction<F, T>(predicate, successFunction, failureFunction);
    }

    /**
     * Wraps another {@link Function} and passes any thrown {@link RuntimeException} to a handler {@link Function}.
     * {@link RuntimeException}s thrown by the handler {@link Function} will not be caught.
     * 
     * @param delegate
     *            the {@link Function} to wrap
     * @param exceptionFunction
     *            a {@link Function} that takes an instance of {@link RuntimeException}, will be invoked only if/when
     *            {@link #delegate} throws
     * @param onExceptionValue
     *            the value to return to the caller if the wrapped {@link Function} throws (may be <code>null</code>)
     * @return a {@link Function} that wraps another {@link Function} and forwards any thrown exceptions to a delegate
     *         {@link Function}. If the exception handling {@link Function} itself throws a [runtime] exception it <em>will</em>
     *         propagate and will not be caught.
     */
    public static <F, T> Function<F, T> onException(final Function<F, T> delegate,
            final Function<? super RuntimeException, ?> exceptionFunction, final T onExceptionValue) {
        return new NoThrowFunction<F, T>(delegate, exceptionFunction, onExceptionValue);
    }

}
