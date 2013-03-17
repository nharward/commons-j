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
        // Pass the handler in three times, so our counts should be triple the normal value
        new SAXBPParser().parse(rolodexXml(), rolodexContext(), handler, handler, handler);
        assertEquals(3, handler.getContactLists().size());
    }

    @Test
    public final void basicStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler handler = new StAXContactListHandler();
        assertTrue(handler.getContactListEvents().isEmpty());
        // Pass the handler in four times, so our counts should be quadruple the normal value
        new SAXBPParser().parse(rolodexXml(), null, handler, handler, handler, handler);
        assertEquals(4, handler.getContactListEvents().size());
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

        // Pass several handlers in multiple times
        new SAXBPParser().parse(rolodexXml(), rolodexContext(), clHandler, clHandler, pHandler, pHandler, pHandler, atHandler,
                atHandler, atHandler, dHandler, dHandler, dHandler, dHandler);

        assertEquals(2, clHandler.getContactListEvents().size());
        assertEquals(6, pHandler.getPeople().size());
        assertEquals(12, atHandler.getAddresses().size());
        assertTrue(dHandler.started());
        assertTrue(dHandler.ended());
    }
}
