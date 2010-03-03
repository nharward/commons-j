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

package us.harward.commons.xml.saxbp;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import us.harward.commons.xml.XMLTestBase;
import us.harward.commons.xml.jaxbtest.rolodex.ContactList;
import us.harward.commons.xml.saxbp.handlers.JAXBContactListHandler;
import us.harward.commons.xml.saxbp.handlers.StAXContactListHandler;

public class SAXBPParserTest extends XMLTestBase {

    @Test
    public final void basicJAXBParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final JAXBContactListHandler handler = new JAXBContactListHandler();
        assert handler.getContacts().isEmpty();
        new SAXBPParser().parse(rolodexXml(), rolodexContext(), handler);
        assert handler.getContacts().size() == 1;
    }

    @Test
    public final void basicStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler handler = new StAXContactListHandler();
        assert handler.getContactEvents().isEmpty();
        new SAXBPParser().parse(rolodexXml(), null, handler);
        assert handler.getContactEvents().size() == 1;
    }

    private JAXBContext rolodexContext() throws JAXBException {
        return JAXBContext.newInstance(ContactList.class.getPackage().getName());
    }

}
