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

package nerds.antelax.commons.xml.saxbp;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.bind.JAXBException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import nerds.antelax.commons.xml.XMLTestBase;
import nerds.antelax.commons.xml.saxbp.handlers.DocumentHandler;
import nerds.antelax.commons.xml.saxbp.handlers.JAXBAddressTypeHandler;
import nerds.antelax.commons.xml.saxbp.handlers.JAXBContactListHandler;
import nerds.antelax.commons.xml.saxbp.handlers.StAXContactListHandler;
import nerds.antelax.commons.xml.saxbp.handlers.StAXPersonHandler;

import org.testng.annotations.Test;

public class SAXBPParserTest extends XMLTestBase {

    @Test
    public final void basicJAXBParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final JAXBContactListHandler handler = new JAXBContactListHandler();
        assertTrue(handler.getContactLists().isEmpty());
        new SAXBPParser().parse(rolodexXml(), rolodexContext(), handler);
        assertEquals(1, handler.getContactLists().size());
    }

    @Test
    public final void basicStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler handler = new StAXContactListHandler();
        assertTrue(handler.getContactListEvents().isEmpty());
        new SAXBPParser().parse(rolodexXml(), null, handler);
        assertEquals(1, handler.getContactListEvents().size());
    }

    @Test
    public final void mixedJaxbAndStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler clHandler = new StAXContactListHandler();
        final StAXPersonHandler pHandler = new StAXPersonHandler();
        final JAXBAddressTypeHandler atHandler = new JAXBAddressTypeHandler();
        final DocumentHandler dHandler = new DocumentHandler();

        assertTrue(clHandler.getContactListEvents().isEmpty());
        assertTrue(pHandler.getPeople().isEmpty());
        assertTrue(atHandler.getAddresses().isEmpty());
        assertFalse(dHandler.started());
        assertFalse(dHandler.ended());

        new SAXBPParser().parse(rolodexXml(), rolodexContext(), clHandler, pHandler, atHandler, dHandler);

        assertEquals(1, clHandler.getContactListEvents().size());
        assertEquals(2, pHandler.getPeople().size());
        assertEquals(4, atHandler.getAddresses().size());
        assertTrue(dHandler.started());
        assertTrue(dHandler.ended());
    }

}
