// Copyright 2010 Nathaniel Harward
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

package nerds.antelax.commons.xml.saxbp.handlers;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.StartDocument;

import nerds.antelax.commons.xml.saxbp.annotations.SAXBPHandler;


@SAXBPHandler
public final class DocumentHandler {

    final AtomicBoolean documentStarted;
    final AtomicBoolean documentEnded;

    public DocumentHandler() {
        documentStarted = new AtomicBoolean(false);
        documentEnded = new AtomicBoolean(false);
    }

    @XmlElement(namespace = XMLConstants.NULL_NS_URI, name = "")
    public void started(final StartDocument ds) {
        documentStarted.set(true);
    }

    @XmlElement(namespace = XMLConstants.NULL_NS_URI, name = "")
    public void ended(final EndDocument ed) {
        documentEnded.set(true);
    }

    public boolean started() {
        return documentStarted.get();
    }

    public boolean ended() {
        return documentEnded.get();
    }

    public void reset() {
        documentStarted.set(false);
        documentEnded.set(false);
    }

}
