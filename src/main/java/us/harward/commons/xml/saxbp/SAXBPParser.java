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

import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;

import us.harward.commons.xml.saxbp.annotations.JAXBHandler;
import us.harward.commons.xml.saxbp.annotations.SAXBPHandler;

/**
 * An XML parser that is [an attempt at] a best-of combination of SAX/JAXB/StAX, hence the name. The idea is to combine the
 * convenience of JAXB and StAX with the low overhead and callback style of SAX. Good for handling large XML files where long lists
 * of repeated and perhaps cumbersome elements exist and memory consumption rules out DOM or a "full" JAXB parse but where it would
 * be very convenient to perhaps use JAXB (or StAX) for sub-elements.
 */
public class SAXBPParser {

    /**
     * Wrapper around {@link #parse(XMLEventReader, JAXBContext, Object...)}.
     * 
     * @throws SAXBPException
     * @see #parse(XMLEventReader, JAXBContext, Object...)
     */
    public void parse(final InputStream is, final JAXBContext context, final Object... saxbpHandlers) throws XMLStreamException,
            FactoryConfigurationError, JAXBException, SAXBPException {
        parse(XMLInputFactory.newInstance().createXMLEventReader(is), context, saxbpHandlers);
    }

    /**
     * Wrapper around {@link #parse(XMLEventReader, JAXBContext, Object...)}.
     * 
     * @throws SAXBPException
     * @see #parse(XMLEventReader, JAXBContext, Object...)
     */
    public void parse(final Reader reader, final JAXBContext context, final Object... saxbpHandlers) throws XMLStreamException,
            FactoryConfigurationError, JAXBException, SAXBPException {
        parse(XMLInputFactory.newInstance().createXMLEventReader(reader), context, saxbpHandlers);
    }

    /**
     * Wrapper around {@link #parse(XMLEventReader, JAXBContext, Object...)}.
     * 
     * @throws SAXBPException
     * @see #parse(XMLEventReader, JAXBContext, Object...)
     */
    public void parse(final Source source, final JAXBContext context, final Object... saxbpHandlers) throws XMLStreamException,
            FactoryConfigurationError, JAXBException, SAXBPException {
        parse(XMLInputFactory.newInstance().createXMLEventReader(source), context, saxbpHandlers);
    }

    /**
     * Wrapper around {@link #parse(XMLEventReader, JAXBContext, Object...)}.
     * 
     * @throws SAXBPException
     * @see #parse(XMLEventReader, JAXBContext, Object...)
     */
    public void parse(final XMLStreamReader reader, final JAXBContext context, final Object... saxbpHandlers)
            throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        parse(XMLInputFactory.newInstance().createXMLEventReader(reader), context, saxbpHandlers);
    }

    /**
     * Parses an {@link XMLEventReader} to completion, passing on StAX or JAXB parsing events to handlers as indicated by their
     * annotated methods.
     * 
     * @param context
     *            if using JAXB for any callbacks, the {@link JAXBContext} to use
     * @param saxbpHandlers
     *            each handler must be annotated with {@link SAXBPHandler}, and should have methods annotated with one or more of
     *            {@link JAXBHandler} (for JAXB callbacks), or {@link XmlElement} or {@link XmlElements} (for StAX callbacks)
     * @throws XMLStreamException
     * @throws FactoryConfigurationError
     * @throws JAXBException
     * @throws SAXBPException
     */
    public void parse(final XMLEventReader reader, final JAXBContext context, final Object... saxbpHandlers)
            throws XMLStreamException, FactoryConfigurationError, JAXBException, SAXBPException {
        final Collection<ParseInterest> interests = ParseInterest.fromHandlers(context, saxbpHandlers);
        for (XMLEvent event = reader.peek(); event != null; event = reader.peek()) {
            boolean interesting = false;
            for (final ParseInterest interest : interests) {
                if (interest.accept(event)) {
                    interest.handleNextEvent(reader);
                    interesting = true;
                    break;
                }
            }
            if (!interesting)
                reader.next();
        }
    }

}
