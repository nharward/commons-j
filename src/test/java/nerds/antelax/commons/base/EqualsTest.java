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

package nerds.antelax.commons.base;

import static nerds.antelax.commons.base.Equals.allowNull;
import static nerds.antelax.commons.base.Equals.disallowNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

public class EqualsTest {

    @Test
    public final void testAllowNull() {
        assertTrue(allowNull(null, null));
        assertTrue(allowNull("Testing", "Testing"));
        assertFalse(allowNull("Testing", "NotEqual"));
        assertFalse(allowNull(null, "Testing"));
        assertFalse(allowNull("Testing", null));
    }

    @Test
    public final void testDisallowNull() {
        assertFalse(disallowNull(null, null));
        assertTrue(disallowNull("Testing", "Testing"));
        assertFalse(disallowNull("Testing", "NotEqual"));
        assertFalse(disallowNull(null, "Testing"));
        assertFalse(disallowNull("Testing", null));
    }

}
