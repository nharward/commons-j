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

import static us.harward.commons.util.Equals.allowNull;
import static us.harward.commons.util.Equals.disallowNull;
import junit.framework.Assert;

import org.junit.Test;

public class EqualsTest {

    @Test
    public final void testAllowNull() {
        Assert.assertTrue(allowNull(null, null));
        Assert.assertTrue(allowNull("Testing", "Testing"));
        Assert.assertFalse(allowNull("Testing", "NotEqual"));
        Assert.assertFalse(allowNull(null, "Testing"));
        Assert.assertFalse(allowNull("Testing", null));
    }

    @Test
    public final void testDisallowNull() {
        Assert.assertFalse(disallowNull(null, null));
        Assert.assertTrue( disallowNull("Testing", "Testing"));
        Assert.assertFalse(disallowNull("Testing", "NotEqual"));
        Assert.assertFalse(disallowNull(null, "Testing"));
        Assert.assertFalse(disallowNull("Testing", null));
    }

}
