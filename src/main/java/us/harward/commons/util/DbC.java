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
 * @see http://en.wikipedia.org/wiki/Design_by_contract
 */
public final class DbC {

    public static class PreconditionFailedException extends RuntimeException {

        private static final long serialVersionUID = 7101362425253843643L;

        public PreconditionFailedException(final String message) {
            super(message);
        }

    }

    public static class InvariantFailedException extends RuntimeException {

        private static final long serialVersionUID = 6578969914048667426L;

        public InvariantFailedException(final String message) {
            super(message);
        }

    }

    public static class PostconditionFailedException extends RuntimeException {

        private static final long serialVersionUID = 3034586030622586004L;

        public PostconditionFailedException(final String message) {
            super(message);
        }

    }

    /**
     * The same as {@code notNull(arg, "A null value here is not allowed")}.
     * 
     * @see {@link DbC#notNull(Object, String)}
     */
    public static <T> void notNull(final T arg) {
        notNull(arg, "A null value here is not allowed");
    }

    /**
     * @param arg
     *            the thing that is not supposed to be null
     * @param message
     *            message to go into the generated {@link RuntimeException}
     * @throws {@link IllegalArgumentException}
     */
    public static <T> void notNull(final T arg, final String message) {
        if (arg == null)
            throw new IllegalArgumentException(message);
    }

    /**
     * The same as {@code precondition(conditionMet, "Pre-condition failed")}.
     * 
     * @see {@link DbC#precondition(boolean, String)}
     */
    public static void precondition(final boolean conditionMet) {
        precondition(conditionMet, "Pre-condition failed");
    }

    /**
     * @param conditionMet
     *            if false will trigger an exception
     * @param message
     *            message to go into the generated {@link RuntimeException}
     * @throws {@link PreconditionFailedException}
     */
    public static void precondition(final boolean conditionMet, final String message) {
        if (!conditionMet)
            throw new PreconditionFailedException(message);
    }

    /**
     * Equivalent to calling {@code invariant(conditionMet, "Invariant failed")}.
     * 
     * @param conditionMet
     *            if false will trigger an exception
     * @throws {@link InvariantFailedException}
     */
    public static void invariant(final boolean conditionMet) {
        invariant(conditionMet, "Invariant failed");
    }

    /**
     * @param conditionMet
     *            if false will trigger an exception
     * @param message
     *            message to go into the generated {@link RuntimeException}
     * @throws {@link InvariantFailedException}
     */
    public static void invariant(final boolean conditionMet, final String message) {
        if (!conditionMet)
            throw new InvariantFailedException(message);
    }

    /**
     * Equivalent to calling {@code postcondition(conditionMet, "Post-condition failed")}.
     * 
     * @param conditionMet
     *            if false will trigger an exception
     * @throws {@link PostconditionFailedException}
     */
    public static void postcondition(final boolean conditionMet) {
        postcondition(conditionMet, "Post-condition failed");
    }

    /**
     * @param conditionMet
     *            if false will trigger an exception
     * @param message
     *            message to go into the generated {@link RuntimeException}
     * @throws {@link PostconditionFailedException}
     */
    public static void postcondition(final boolean conditionMet, final String message) {
        if (!conditionMet)
            throw new PostconditionFailedException(message);
    }

}
