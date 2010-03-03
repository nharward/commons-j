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

package us.harward.commons.xml.saxbp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElement;

/**
 * This annotation denotes a handler method for a specific JAXB class. Methods annotated with {@link JAXBHandler} must take a single
 * argument, a {@link JAXBElement} instance with a specific [JAXB-generated] class type. If the specific class type is simply
 * {@link Object} then it's up to JAXB whether or not to throw an exception.
 */
@Documented
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JAXBHandler {

    /**
     * Only the {@link XmlElement#namespace()} and {@link XmlElement#name()} values are examined.
     */
    XmlElement value();

}
