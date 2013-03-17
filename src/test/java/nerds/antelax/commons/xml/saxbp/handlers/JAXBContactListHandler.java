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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;

import nerds.antelax.commons.xml.jaxbtest.rolodex.ContactList;
import nerds.antelax.commons.xml.saxbp.annotations.JAXBHandler;
import nerds.antelax.commons.xml.saxbp.annotations.SAXBPHandler;

@SAXBPHandler
public class JAXBContactListHandler {

    private final Collection<ContactList> contactLists = new LinkedList<ContactList>();

    @JAXBHandler(@XmlElement(namespace = "http://nerds.antelax.xmlns/rolodex", name = "contacts"))
    public void contactList(final JAXBElement<ContactList> element) {
        contactLists.add(element.getValue());
    }

    public Collection<ContactList> getContactLists() {
        return Collections.unmodifiableCollection(contactLists);
    }

    public void reset() {
        contactLists.clear();
    }

}
