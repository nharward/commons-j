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

package us.harward.commons.net.pubsub;

@SuppressWarnings("serial")
final class MessageFormatException extends Exception {

    MessageFormatException() {
        super();
    }

    MessageFormatException(final String message) {
        super(message);
    }

    MessageFormatException(final Throwable cause) {
        super(cause);
    }

    MessageFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
