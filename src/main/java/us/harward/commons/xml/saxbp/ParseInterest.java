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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import us.harward.commons.util.Pair;
import us.harward.commons.xml.saxbp.annotations.JAXBHandler;
import us.harward.commons.xml.saxbp.annotations.SAXBPHandler;

/**
 * This class handles all of the terrible noise that is introspection and dynamic method invocation on POJO callbacks. Yuck!
 */
final class ParseInterest implements EventFilter {

    private final QName        qName;
    private final Unmarshaller jaxbUnmarshaller;
    private final Class<?>     jaxbClass;
    private final Object       handler;
    private final Method       handlerMethod;

    private ParseInterest(final QName qName, final JAXBContext jaxbContext, final Class<?> jaxbClass, final Object handler,
            final Method handlerMethod) throws JAXBException {
        assert qName != null;
        assert handler != null;
        assert handlerMethod != null;
        this.qName = qName;
        if (jaxbClass != null) {
            assert jaxbContext != null;
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        } else {
            jaxbUnmarshaller = null;
        }
        this.jaxbClass = jaxbClass;
        this.handler = handler;
        this.handlerMethod = handlerMethod;
    }

    @Override
    public boolean accept(final XMLEvent event) {
        return event.isStartElement() && ((StartElement) event).getName().equals(qName);
    }

    /**
     * Should be called only when the next event in the stream reader is the parse event we are interested in.
     * 
     * @throws JAXBException
     * @throws XMLStreamException
     */
    void handleNextEvent(final XMLEventReader reader) throws JAXBException, XMLStreamException {
        final Object event = jaxbClass != null ? jaxbUnmarshaller.unmarshal(reader, jaxbClass) : reader.nextEvent();
        try {
            handlerMethod.invoke(handler, event);
        } catch (final IllegalArgumentException iare) {
            throw new JAXBException("Unable to invoke handler method", iare);
        } catch (final IllegalAccessException iace) {
            throw new JAXBException("Unable to invoke handler method", iace);
        } catch (final InvocationTargetException ite) {
            throw new JAXBException("Unable to invoke handler method", ite);
        }
    }

    static Collection<ParseInterest> fromHandlers(final JAXBContext jaxbContext, final Object... handlers) throws SAXBPException,
            JAXBException {
        final Map<QName, ParseInterest> interest = new HashMap<QName, ParseInterest>();
        for (final Object handler : handlers) {
            if (!handler.getClass().isAnnotationPresent(SAXBPHandler.class))
                throw new SAXBPException("Handler [" + handler + "] is not annotated with " + SAXBPHandler.class.getName());
            for (final Method method : handler.getClass().getMethods()) {
                try {
                    if (method.isAnnotationPresent(XmlElement.class)) {
                        final QName qName = checkQName(method, method.getAnnotation(XmlElement.class), interest);
                        interest.put(qName, new ParseInterest(qName, null, null, handler, method));
                    } else if (method.isAnnotationPresent(XmlElements.class)) {
                        for (final XmlElement xmlElement : method.getAnnotation(XmlElements.class).value()) {
                            final QName qName = checkQName(method, xmlElement, interest);
                            interest.put(qName, new ParseInterest(qName, null, null, handler, method));
                        }
                    } else if (method.isAnnotationPresent(JAXBHandler.class)) {
                        final Pair<QName, Class<?>> qNameClassPair = checkQName(method, interest);
                        interest.put(qNameClassPair.first(), new ParseInterest(qNameClassPair.first(), jaxbContext, qNameClassPair
                                .second(), handler, method));
                    }
                } catch (final SAXBPException saxbpe) {
                    throw new SAXBPException("Exception while examining handler[" + handler + "], method[" + method + "]", saxbpe);
                }
            }
        }
        return Collections.unmodifiableCollection(interest.values());
    }

    static QName checkQName(final Method method, final XmlElement element, final Map<QName, ParseInterest> registeredInterest)
            throws SAXBPException {
        if (element.name() == null || element.name().trim().isEmpty())
            throw new SAXBPException("Null or empty element name in XmlElement annotation");
        final QName qName = new QName(element.namespace(), element.name());
        if (registeredInterest.containsKey(qName))
            throw new SAXBPException("XML element[" + qName + "] already handled by method "
                    + registeredInterest.get(qName).handlerMethod);
        final Class<?>[] args = method.getParameterTypes();
        if (args.length != 1 || !XMLEvent.class.getName().equals(args[0].getName()))
            throw new SAXBPException("Method must take a single argument of type " + XMLEvent.class.getName());
        return qName;
    }

    static Pair<QName, Class<?>> checkQName(final Method method, final Map<QName, ParseInterest> registeredInterest)
            throws SAXBPException {
        final XmlElement element = method.getAnnotation(JAXBHandler.class).value();
        if (element.name() == null || element.name().trim().isEmpty())
            throw new SAXBPException("Null or empty element name in [embedded] XmlElement annotation");
        final QName qName = new QName(element.namespace(), element.name());
        if (registeredInterest.containsKey(qName))
            throw new SAXBPException("XML element[" + qName + "] already handled by method "
                    + registeredInterest.get(qName).handlerMethod);
        return new Pair<QName, Class<?>>(qName, checkAndGetJaxbClass(method));
    }

    private static Class<?> checkAndGetJaxbClass(final Method method) throws SAXBPException {
        // My goodness this is disgusting
        final Class<?>[] methodArgTypes = method.getParameterTypes();
        if (methodArgTypes.length != 1 || !JAXBElement.class.getName().equals(methodArgTypes[0].getName()))
            throw new SAXBPException("JAXB handler method must take a single argument of type JAXBElement<...>");
        final Type jaxbType = ((ParameterizedType) method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
        if (!(jaxbType instanceof Class<?>))
            throw new SAXBPException("JAXB handler method must take a single argument of type JAXBElement<...>");
        return (Class<?>) jaxbType;
    }

}
