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

package us.harward.commons.rexec;

import java.io.File;
import java.io.IOException;

import us.harward.commons.util.DbC;

/**
 * <p>
 * Represents an external JVM runtime capable of executing processes and providing a proxy inside this VM. An external JVM could be
 * on the same machine in another process or on a remote machine. Useful in (at least) the following situations:
 * <ul>
 * <li>The application JVM is using more than half the available memory on a machine and fork/exec results in an
 * {@link OutOfMemoryError}</li>
 * <li>A particular executable is only available on a remote machine (perhaps a different physical architecture)</li>
 * <li>To sandbox an executable to an alternate machine, user on the local machine or user on a remote machine</li>
 * </ul>
 * </p>
 * <p>
 * This class mimics the {@link Runtime#exec()} family of methods to make things as transparent as possible.
 * </p>
 * 
 * @author nharward
 */
public abstract class ExternalRuntime {

    protected ExternalRuntime() {
        // Dummy constructor for subclasses
    }

    /**
     * Modeled after {@link Runtime#getRuntime()}
     * 
     * @return a proxy to the external runtime on the local host running on the default port
     */
    public static ExternalRuntime getRuntime() {
        DbC.invariant(false, "Not yet implemented");
        return null;
    }

    /**
     * @see {@link Runtime#availableProcessors()}
     * @return number of available processors on the external environment
     */
    public abstract int availableProcessors();

    /**
     * @see {@link Runtime#freeMemory()}
     * @return amount of free memory in the external environment
     */
    public abstract long freeMemory();

    /**
     * @see {@link Runtime#maxMemory()}
     * @return amount of maximum memory in the external environment
     */
    public abstract long maxMemory();

    /**
     * @see {@link Runtime#totalMemory()}
     * @return amount of total memory in the external environment
     */
    public abstract long totalMemory();

    /**
     * Note that the {@link File} object passed in should exist in the <em>external</em> environment
     * 
     * @see {@link Runtime#exec(String, String[], File)}
     * @return a proxy to the process on the external environment
     */
    public abstract Process exec(final String command, final String[] envp, final File dir) throws IOException;

    /**
     * @see {@link Runtime#exec(String, String[])}
     * @return a proxy to the process on the external environment
     */
    public abstract Process exec(final String command, final String[] envp) throws IOException;

    /**
     * @see {@link Runtime#exec(String)}
     * @return a proxy to the process on the external environment
     */
    public abstract Process exec(final String command) throws IOException;

    /**
     * Note that the {@link File} object passed in should exist in the <em>external</em> environment
     * 
     * @see {@link Runtime#exec(String[], String[], File)}
     * @return a proxy to the process on the external environment
     */
    public abstract Process exec(final String[] cmdarray, final String[] envp, final File dir) throws IOException;

    /**
     * @see {@link Runtime#exec(String[], String[])}
     * @return a proxy to the process on the external environment
     */
    public abstract Process exec(final String[] cmdarray, final String[] envp) throws IOException;

    /**
     * @see {@link Runtime#exec(String[])}
     * @return a proxy to the process on the external environment
     */
    public abstract Process exec(final String[] cmdarray) throws IOException;

}
