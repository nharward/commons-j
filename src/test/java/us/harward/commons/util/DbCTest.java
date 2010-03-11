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

import org.junit.Test;

import us.harward.commons.util.DbC.InvariantFailedException;
import us.harward.commons.util.DbC.PostconditionFailedException;
import us.harward.commons.util.DbC.PreconditionFailedException;

public class DbCTest {

    private static final String MESSAGE = "This one has a message";

    @Test
    public final void notNullWithNonNull() {
        DbC.notNull(new Object());
    }

    @Test
    public final void notNullWithNonNullMsg() {
        DbC.notNull(new Object(), MESSAGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void notNullWithNull() {
        DbC.notNull(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public final void notNullWithNullMsg() {
        DbC.notNull(null, MESSAGE);
    }

    @Test
    public final void preconditionOk() {
        DbC.precondition(true);
    }

    @Test
    public final void preconditionOkMsg() {
        DbC.precondition(true, MESSAGE);
    }

    @Test(expected = PreconditionFailedException.class)
    public final void preconditionFailed() {
        DbC.precondition(false);
    }

    @Test(expected = PreconditionFailedException.class)
    public final void preconditionFailedMsg() {
        DbC.precondition(false, MESSAGE);
    }

    @Test
    public final void invariantOk() {
        DbC.invariant(true);
    }

    @Test
    public final void invariantOkMsg() {
        DbC.invariant(true, MESSAGE);
    }

    @Test(expected = InvariantFailedException.class)
    public final void invariantFailed() {
        DbC.invariant(false);
    }

    @Test(expected = InvariantFailedException.class)
    public final void invariantFailedMsg() {
        DbC.invariant(false, MESSAGE);
    }

    @Test
    public final void postconditionOk() {
        DbC.postcondition(true);
    }

    @Test
    public final void postconditionOkMsg() {
        DbC.postcondition(true, MESSAGE);
    }

    @Test(expected = PostconditionFailedException.class)
    public final void postconditionFailed() {
        DbC.postcondition(false);
    }

    @Test(expected = PostconditionFailedException.class)
    public final void postconditionFailedMsg() {
        DbC.postcondition(false, MESSAGE);
    }

}
