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

import javax.xml.bind.JAXBException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import junit.framework.Assert;

import org.junit.Test;

import us.harward.commons.xml.XMLTestBase;
import us.harward.commons.xml.saxbp.handlers.DocumentHandler;
import us.harward.commons.xml.saxbp.handlers.JAXBAddressTypeHandler;
import us.harward.commons.xml.saxbp.handlers.JAXBContactListHandler;
import us.harward.commons.xml.saxbp.handlers.StAXContactListHandler;
import us.harward.commons.xml.saxbp.handlers.StAXPersonHandler;

public class SAXBPParserTest extends XMLTestBase {

    @Test
    public final void basicJAXBParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final JAXBContactListHandler handler = new JAXBContactListHandler();
        Assert.assertTrue(handler.getContactLists().isEmpty());
        new SAXBPParser().parse(rolodexXml(), rolodexContext(), handler);
        Assert.assertEquals(1, handler.getContactLists().size());
    }

    @Test
    public final void basicStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler handler = new StAXContactListHandler();
        Assert.assertTrue(handler.getContactListEvents().isEmpty());
        new SAXBPParser().parse(rolodexXml(), null, handler);
        Assert.assertEquals(1, handler.getContactListEvents().size());
    }

    @Test
    public final void mixedJaxbAndStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler clHandler = new StAXContactListHandler();
        final StAXPersonHandler pHandler = new StAXPersonHandler();
        final JAXBAddressTypeHandler atHandler = new JAXBAddressTypeHandler();
        final DocumentHandler dHandler = new DocumentHandler();

        Assert.assertTrue(clHandler.getContactListEvents().isEmpty());
        Assert.assertTrue(pHandler.getPeople().isEmpty());
        Assert.assertTrue(atHandler.getAddresses().isEmpty());
        Assert.assertFalse(dHandler.started());
        Assert.assertFalse(dHandler.ended());

        new SAXBPParser().parse(rolodexXml(), rolodexContext(), clHandler, pHandler, atHandler, dHandler);

        Assert.assertEquals(1, clHandler.getContactListEvents().size());
        Assert.assertEquals(2, pHandler.getPeople().size());
        Assert.assertEquals(4, atHandler.getAddresses().size());
        Assert.assertTrue(dHandler.started());
        Assert.assertTrue(dHandler.ended());
    }

}
