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

package nerds.antelax.commons.xml.saxbp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import nerds.antelax.commons.xml.saxbp.SAXBPParser;


/**
 * Marker class-level annotation denoting a callback handler for {@link SAXBPParser}. Instances of classes with this annotation
 * should have methods annotated with {@link JAXBHandler}, {@link XmlElement} or {@link XmlElements} to indicate which XML parse
 * events are interesting and whether they should be handled by using JAXB or StAX.
 */
@Documented
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SAXBPHandler {
}
