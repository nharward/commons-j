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

package nerds.antelax.commons.xml;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import nerds.antelax.commons.xml.jaxbtest.rolodex.ContactList;

public class XMLTestBase {

    public InputStream rolodexXml() {
        return xmlStream("rolodex.xml");
    }

    public InputStream xmlStream(final String filename) {
        return XMLTestBase.class.getResourceAsStream("/xml/" + filename);
    }

    protected static JAXBContext rolodexContext() throws JAXBException {
        return JAXBContext.newInstance(ContactList.class.getPackage().getName());
    }

}
