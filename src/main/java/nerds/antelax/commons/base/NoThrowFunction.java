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

/**
 * A {@link Function} that wraps another {@link Function} and forwards any thrown exceptions to a delegate {@link Function}. If the
 * exception handling {@link Function} itself throws a [runtime] exception it <em>will</em> propagate and will not be caught.
 */
final class NoThrowFunction<F, T> implements Function<F, T> {

    private final Function<F, T>                        delegate;
    private final Function<? super RuntimeException, ?> exceptionHandler;
    private final T                                     onExceptionValue;

    NoThrowFunction(final Function<F, T> delegate, final Function<? super RuntimeException, ?> exceptionHandler,
            final T onExceptionValue) {
        Preconditions.checkNotNull(delegate);
        Preconditions.checkNotNull(exceptionHandler);
        this.delegate = delegate;
        this.exceptionHandler = exceptionHandler;
        this.onExceptionValue = onExceptionValue;
    }

    @Override
    public T apply(final F argument) {
        try {
            return delegate.apply(argument);
        } catch (final RuntimeException re) {
            exceptionHandler.apply(re);
            return onExceptionValue;
        }
    };

}
