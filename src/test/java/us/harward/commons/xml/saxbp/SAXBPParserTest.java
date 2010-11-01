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
        assert handler.getContactLists().isEmpty();
        new SAXBPParser().parse(rolodexXml(), rolodexContext(), handler);
        assert handler.getContactLists().size() == 1;
    }

    @Test
    public final void basicStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler handler = new StAXContactListHandler();
        assert handler.getContactListEvents().isEmpty();
        new SAXBPParser().parse(rolodexXml(), null, handler);
        assert handler.getContactListEvents().size() == 1;
    }

    @Test
    public final void mixedJaxbAndStAXParse() throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final StAXContactListHandler clHandler = new StAXContactListHandler();
        final StAXPersonHandler pHandler = new StAXPersonHandler();
        final JAXBAddressTypeHandler atHandler = new JAXBAddressTypeHandler();
        final DocumentHandler dHandler = new DocumentHandler();

        assert clHandler.getContactListEvents().isEmpty();
        assert pHandler.getPeople().isEmpty();
        assert atHandler.getAddresses().isEmpty();
        assert !dHandler.started();
        assert !dHandler.ended();

        new SAXBPParser().parse(rolodexXml(), rolodexContext(), clHandler, pHandler, atHandler, dHandler);

        assert clHandler.getContactListEvents().size() == 1;
        assert pHandler.getPeople().size() == 2;
        assert atHandler.getAddresses().size() == 4;
        assert dHandler.started();
        assert dHandler.ended();
    }

}
