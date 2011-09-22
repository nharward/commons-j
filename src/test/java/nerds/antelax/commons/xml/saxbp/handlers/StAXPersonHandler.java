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

package nerds.antelax.commons.xml.saxbp.handlers;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.stream.events.StartElement;

import nerds.antelax.commons.xml.saxbp.annotations.SAXBPHandler;

@SAXBPHandler
public class StAXPersonHandler {

    private final Collection<StartElement> people = new LinkedList<StartElement>();

    @XmlElement(namespace = "http://nerds.antelax.xmlns/rolodex", name = "person")
    public void person(final StartElement se) {
        people.add(se);
    }

    public Collection<StartElement> getPeople() {
        return Collections.unmodifiableCollection(people);
    }

    public void reset() {
        people.clear();
    }

}
