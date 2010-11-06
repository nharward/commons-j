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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Represents an extremely simple workflow that transforms an input type to an output type. Input and output objects are validated
 * against predicates (if given, otherwise "true" is assumed) and routed to error sinks if they fail (if passed to the family of
 * <code>apply(...)</code> methods).
 * <p>
 * The intention here is to reduce code complexity for applications that have a series of steps to produce output from some given
 * input, by way of the {@link #compose(Workflow, Workflow)} method:
 * <ul>
 * <li>Remove the application's burden of writing plumbing code (with embedded error-checking) to get an object through each step of
 * the transformation</li>
 * <li>Encourage better application code modularity by isolating domain-specific code into {@link Predicate} and {@link Function}
 * objects for each step of the workflow; improves re-use and makes TDD somewhat reasonable in the real world :)</li>
 * <li>Reduce code noise: null checks and the like can be put in {@link Predicate} classes where they belong so {@link Function}
 * implementations that do the "real" work can be cleaner and more easily maintained</li>
 * </ul>
 */
public class Workflow<F, T> {

    protected final Predicate<F>   precondition;
    protected final Function<F, T> function;
    protected final Predicate<T>   postcondition;

    /**
     * Constructs a simple workflow that applies the given {@link Function} to all inputs that pass the pre-condition. Upon
     * execution, only outputs that pass the postcondition will be passed on to a final destination {@link Sink}.
     * 
     * @param precondition
     *            a {@link Predicate} to filter bad inputs from participating in the workflow; may be null (meaning no filtering is
     *            done) done)
     * @param function
     *            the core of the workflow; this is the function that does the "work" by transforming input into some useful output
     * @param postcondition
     *            a {@link Predicate} to filter bad outputs from the transformation {@link Function} from being sent to the final
     *            destination {@link Sink}; may be null (meaning no filtering is done)
     */
    public static <F, T> Workflow<F, T> create(final Predicate<F> precondition, final Function<F, T> function,
            final Predicate<T> postcondition) {
        Preconditions.checkNotNull(function);
        return new Workflow<F, T>(precondition, function, postcondition);
    }

    /**
     * Equivalent to calling {@link #create(Predicate, Function, Predicate)} with null predicates.
     * 
     * @see {@link #create(Predicate, Function, Predicate)}
     */
    public static <F, T> Workflow<F, T> wrap(final Function<F, T> function) {
        return create(null, function, null);
    }

    /**
     * Composes two {@link Workflow}s together. Note the a failure of a predicate in [a sequence of] composed {@link Workflow}s will
     * cause the <em>original input</em> value to be passed to the pre-condition failure {@link Sink} during a call to one of the
     * <code>apply(...)</code> methods.
     * 
     * @see {@link Functions#compose(Function, Function)}
     */
    public static <A, B, C> Workflow<A, C> compose(final Workflow<B, C> g, final Workflow<A, B> f) {
        return create(f.precondition, new ComposedFunctionWithPredicate<A, B, C>(g.function, f.function, Predicates.and(
                f.postcondition, g.precondition)), g.postcondition);
    }

    private Workflow(final Predicate<F> precondition, final Function<F, T> function, final Predicate<T> postcondition) {
        this.function = function;
        this.precondition = nonNullPredicate(precondition);
        this.postcondition = nonNullPredicate(postcondition);

        Preconditions.checkNotNull(this.precondition);
        Preconditions.checkNotNull(this.function);
        Preconditions.checkNotNull(this.postcondition);
    }

    /**
     * Applies the workflow for the set of input objects, placing all (successful) outputs on the given sink.
     * 
     * @param input
     *            the set of inputs to run through the workflow, cannot be null
     * @param output
     *            the sink for objects resulting from the workflow that pass the postcondition predicate
     */
    public void apply(final Iterable<F> inputs, final Sink<T> output) {
        apply(inputs, output, null, null);
    }

    /**
     * Applies the workflow for the set of input objects, placing all (successful) outputs on the given sink.
     * 
     * @param input
     *            the set of inputs to run through the workflow, cannot be null
     * @param output
     *            the sink for objects resulting from the workflow that pass the postcondition predicate
     */
    public void apply(final F[] inputs, final Sink<T> output) {
        apply(inputs, output, null, null);
    }

    /**
     * Same as {@link #apply(Iterable, Sink)}, but allows for collection of rejected inputs or outputs via alternate sinks.
     * 
     * @see {@link #apply(Iterable, Sink)}
     * @param preconditionFailedSink
     *            a {@link Sink} where all inputs failing the precondition will be sent, may be null
     * @param postconditionFailedSink
     *            a {@link Sink} where all outputs failing the postcondition will be sent, may be null
     */
    public void apply(final Iterable<F> inputs, final Sink<T> output, final Sink<F> preconditionFailedSink,
            final Sink<T> postconditionFailedSink) {
        Preconditions.checkNotNull(inputs);
        // The sinks are cached and made non-null, so that if they are null they aren't re-constructed downstream for every input
        final Sink<T> outputSink = nonNullSink(output);
        final Sink<F> preFailSink = nonNullSink(preconditionFailedSink);
        final Sink<T> postFailSink = nonNullSink(postconditionFailedSink);
        for (final F input : inputs)
            processInput(input, outputSink, preFailSink, postFailSink);
    }

    /**
     * Same as {@link #apply(F[], Sink)}, but allows for collection of rejected inputs or outputs via alternate sinks.
     * 
     * @see {@link #apply(F[], Sink)}
     * @param preconditionFailedSink
     *            a {@link Sink} where all inputs failing the precondition will be sent, may be null
     * @param postconditionFailedSink
     *            a {@link Sink} where all outputs failing the postcondition will be sent, may be null
     */
    public void apply(final F[] inputs, final Sink<T> output, final Sink<F> preconditionFailedSink,
            final Sink<T> postconditionFailedSink) {
        Preconditions.checkNotNull(inputs);
        // The sinks are cached and made non-null, so that if they are null they aren't re-constructed downstream for every input
        final Sink<T> outputSink = nonNullSink(output);
        final Sink<F> preFailSink = nonNullSink(preconditionFailedSink);
        final Sink<T> postFailSink = nonNullSink(postconditionFailedSink);
        for (final F input : inputs)
            processInput(input, outputSink, preFailSink, postFailSink);
    }

    /**
     * Routes a particular input through this workflow, checking against predicates and sending to appropriate {@link Sink}s
     * depending on how things go.
     */
    protected void processInput(final F input, final Sink<T> output, final Sink<F> preconditionFailedSink,
            final Sink<T> postconditionFailedSink) {
        final Sink<T> outputSink = nonNullSink(output);
        final Sink<F> preFailure = nonNullSink(preconditionFailedSink);
        final Sink<T> postFailure = nonNullSink(postconditionFailedSink);
        if (precondition.apply(input)) {
            try {
                final T result = function.apply(input);
                if (postcondition.apply(result)) {
                    outputSink.drop(result);
                } else {
                    postFailure.drop(result);
                }
            } catch (final ComposedFunctionPredicateFailedException cfpfe) {
                // A composed workflow had an internal predicate fail somewhere in the chain, reject the *original* input to the
                // error sink so it can be captured
                preFailure.drop(input);
            }
        } else {
            preFailure.drop(input);
        }
    }

    protected static <T> Sink<T> nonNullSink(final Sink<T> sink) {
        return sink != null ? sink : new DevNullSink<T>();
    }

    protected static <T> Predicate<T> nonNullPredicate(final Predicate<T> predicate) {
        if (predicate != null)
            return predicate;
        else
            return Predicates.alwaysTrue();
    }

    /**
     * When throw, an instance of this class indicates that a composed {@link Function} intermediate value failed an internal
     * {@link Predicate}.
     */
    @SuppressWarnings("serial")
    protected static final class ComposedFunctionPredicateFailedException extends RuntimeException {
    }

    /**
     * Composes two {@link Function}s, however it checks the an intermediate value against a {@link Predicate} and throws
     * {@link ComposedFunctionPredicateFailedException} if the predicate fails, rather than passing the value to the second
     * {@link Function}.
     */
    protected static final class ComposedFunctionWithPredicate<A, B, C> implements Function<A, C> {

        private final Function<A, B> f;
        private final Function<B, C> g;
        private final Predicate<B>   predicate;

        private ComposedFunctionWithPredicate(final Function<B, C> g, final Function<A, B> f, final Predicate<B> predicate) {
            Preconditions.checkNotNull(g);
            Preconditions.checkNotNull(f);
            Preconditions.checkNotNull(predicate);
            this.g = g;
            this.f = f;
            this.predicate = predicate;
        }

        @Override
        public C apply(final A input) throws ComposedFunctionPredicateFailedException {
            final B intermediate = f.apply(input);
            if (!predicate.apply(intermediate))
                throw new ComposedFunctionPredicateFailedException();
            return g.apply(intermediate);
        };

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ComposedFunctionWithPredicate<?, ?, ?>) {
                final ComposedFunctionWithPredicate<?, ?, ?> other = (ComposedFunctionWithPredicate<?, ?, ?>) obj;
                return f.equals(other.f) && g.equals(other.g) && predicate.equals(other.predicate);
            } else {
                return false;
            }
        }

    }

}