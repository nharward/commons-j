// Copyright 2013 Nathaniel Harward
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

package nerds.antelax.commons.xml;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public final class JAXBUtils {

    /**
     * Uses JAXB annotations to determine which set of fields to [deep] copy from one object to another. Fields that are not common
     * or are marked as {@link XmlTransient} will be skipped. Can be used to copy similar but different objects where much is in
     * common (for example you have two different versions of a schema that vary slightly and need to up/down convert between them).
     * Currently only classes annotated with {@link XmlAccessType#FIELD} are supported (xjc uses this).
     * 
     * @return the number of fields and/or attributes copied
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IntrospectionException
     */
    public static int deepCopyCommonFields(final Object source, final Object target) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, SecurityException, NoSuchFieldException, IntrospectionException {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(target);

        final Class<?> sourceClass = source.getClass();
        final Class<?> targetClass = target.getClass();
        final PropertyDescriptor[] sourceProperties = propertyDescriptors(source);
        final PropertyDescriptor[] targetProperties = propertyDescriptors(target);

        final XmlAccessorType sourceXmlAccessorType = sourceClass.getAnnotation(XmlAccessorType.class);
        final XmlAccessorType targetXmlAccessorType = targetClass.getAnnotation(XmlAccessorType.class);
        final XmlType sourceXmlType = sourceClass.getAnnotation(XmlType.class);
        final XmlType targetXmlType = targetClass.getAnnotation(XmlType.class);

        if (sourceXmlAccessorType == null || targetXmlAccessorType == null || sourceXmlType == null || targetXmlType == null)
            return 0;

        Preconditions.checkArgument(sourceXmlAccessorType.value() == XmlAccessType.FIELD,
                "Only JAXB field access is supported at this time");
        Preconditions.checkArgument(targetXmlAccessorType.value() == XmlAccessType.FIELD,
                "Only JAXB field access is supported at this time");

        int fieldsCopied = 0;
        final String[] sourceXmlElements = combine(sourceXmlType.propOrder(), xmlAttributeProperties(sourceClass));
        final String[] targetXmlElements = combine(targetXmlType.propOrder(), xmlAttributeProperties(targetClass));
        for (final String property : sourceXmlElements) {
            if (exists(property, targetXmlElements)) {
                final PropertyDescriptor sourceProperty = findPropertyDescriptor(sourceProperties, property);
                final PropertyDescriptor targetProperty = findPropertyDescriptor(targetProperties, property);
                if (sourceClass.getDeclaredField(property).getAnnotation(XmlTransient.class) != null
                        || targetClass.getDeclaredField(property).getAnnotation(XmlTransient.class) != null)
                    continue;
                else if (sourceProperty.getPropertyType() == List.class && targetProperty.getPropertyType() == List.class) {
                    final Type targetGenericType = targetProperty.getReadMethod().getGenericReturnType();
                    if (targetGenericType instanceof ParameterizedType) {
                        final Class<?> targetSpecificClass = (Class<?>) ((ParameterizedType) targetGenericType)
                                .getActualTypeArguments()[0];
                        for (final Object sourceValue : (Iterable<?>) getJaxbProperty(source, property)) {
                            try {
                                final Object targetValue = targetSpecificClass.newInstance();
                                final int copied = deepCopyCommonFields(sourceValue, targetValue);
                                if (copied > 0) {
                                    final List<?> targetList = (List<?>) getJaxbProperty(target, property);
                                    final Method add = findMethod(targetList, "add");
                                    add.invoke(targetList, targetValue);
                                    fieldsCopied += copied;
                                }
                            } catch (final InstantiationException ie) {
                                // Gotta skip, can't create these guys
                                break;
                            }
                        }
                    }
                } else if (targetProperty.getPropertyType().isAssignableFrom(sourceProperty.getPropertyType())) {
                    final Object sourceValue = getJaxbProperty(source, property);
                    targetProperty.getWriteMethod().invoke(target, sourceValue);
                    ++fieldsCopied;
                } else {
                    final Object sourceValue = getJaxbProperty(source, property);
                    if (sourceValue == null)
                        targetProperty.getWriteMethod().invoke(target, new Object[] { null });
                    else {
                        // Try to construct a new target value and see if at least one field copies over
                        // If so great, if not then skip
                        try {
                            final Object targetValue = targetProperty.getPropertyType().newInstance();
                            final int copied = deepCopyCommonFields(sourceValue, targetValue);
                            if (copied > 0) {
                                targetProperty.getWriteMethod().invoke(target, targetValue);
                                fieldsCopied += copied;
                            }
                        } catch (final InstantiationException ie) {
                            // Ignore this field, not much we can do at this point
                        }
                    }
                }
            }
        }
        return fieldsCopied;
    }

    public static final <T> Predicate<T> hasRequiredDataPredicate() {
        return new JaxbValidator<T>();
    }

    /**
     * Checks a JAXB-annotated object to make sure all required data is present, however it does not check any data formatting
     * constraints. Traverses the entire object graph to make sure sub-elements are also correctly populated.
     * 
     * @return <code>true</code> if all attributes and fields (and sub-fields) that are required are present, or if the object is
     *         not a JAXB-generated class. Returns <code>false</code> otherwise.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IntrospectionException
     */
    public static boolean hasAllRequiredData(final Object jaxb) throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, SecurityException, NoSuchFieldException, IntrospectionException {
        Preconditions.checkNotNull(jaxb);
        final Class<?> clazz = jaxb.getClass();
        final XmlAccessorType xmlAccessorType = clazz.getAnnotation(XmlAccessorType.class);
        final XmlType xmlType = clazz.getAnnotation(XmlType.class);
        if (xmlAccessorType == null)
            return true;
        Preconditions.checkArgument(xmlAccessorType.value() == XmlAccessType.FIELD, "Only JAXB fields are supported at this time");
        if (xmlType == null)
            return true;

        final String[] properties = combine(xmlType.propOrder(), xmlAttributeProperties(clazz));
        for (final String property : properties) {
            final Field field = clazz.getDeclaredField(property);
            Preconditions.checkNotNull(field);
            final boolean isList = field.getType() == List.class;
            if (field.getAnnotation(XmlTransient.class) != null)
                continue;
            final XmlElement elementAnnotation = field.getAnnotation(XmlElement.class);
            final XmlAttribute attributeAnnotation = field.getAnnotation(XmlAttribute.class);
            final boolean required = elementAnnotation != null ? elementAnnotation.required()
                    : attributeAnnotation != null ? attributeAnnotation.required() : false;
            if (isList) {
                final List<?> list = (List<?>) getJaxbProperty(jaxb, property);
                final Boolean emptyList = (Boolean) findMethod(list, "isEmpty").invoke(list);
                if (required && emptyList)
                    return false;
                else
                    for (final Object listElement : list)
                        if (!hasAllRequiredData(listElement))
                            return false;
            } else {
                final Object propertyValue = getJaxbProperty(jaxb, property);
                if (required && propertyValue == null)
                    return false;
                else if (propertyValue != null)
                    if (!hasAllRequiredData(propertyValue))
                        return false;
            }
        }
        return true;
    }

    private static boolean exists(final String candidate, final String[] set) {
        for (int pos = 0; pos < set.length; ++pos)
            if (set[pos].equals(candidate))
                return true;
        return false;
    }

    private static String[] xmlAttributeProperties(final Class<?> clazz) {
        Preconditions.checkNotNull(clazz);
        final List<String> xmlAttributes = new ArrayList<String>(clazz.getDeclaredFields().length);
        for (final Field field : clazz.getDeclaredFields())
            if (field.getAnnotation(XmlAttribute.class) != null)
                xmlAttributes.add(field.getName());
        return xmlAttributes.toArray(new String[] {});
    }

    private static String[] combine(final String[] left, final String[] right) {
        final String[] rv = new String[left.length + right.length];
        for (int pos = 0; pos < left.length; ++pos)
            rv[pos] = left[pos];
        for (int pos = 0; pos < right.length; ++pos)
            rv[left.length + pos] = right[pos];
        return rv;
    }

    private static PropertyDescriptor[] propertyDescriptors(final Object o) throws IntrospectionException {
        return Introspector.getBeanInfo(o.getClass(), Object.class).getPropertyDescriptors();
    }

    private static PropertyDescriptor findPropertyDescriptor(final PropertyDescriptor[] descriptors, final String property) {
        for (final PropertyDescriptor pd : descriptors)
            if (property.equalsIgnoreCase(pd.getName()))
                return pd;
        return null;
    }

    private static Method findMethod(final Object o, final String methodName) {
        for (final Method m : o.getClass().getMethods())
            if (m.getName().equals(methodName))
                return m;
        return null;
    }

    private static Object getJaxbProperty(final Object jaxb, final String property) throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException, IntrospectionException {
        Preconditions.checkNotNull(jaxb);
        Preconditions.checkNotNull(property);
        final PropertyDescriptor descriptor = findPropertyDescriptor(propertyDescriptors(jaxb), property);
        Preconditions.checkNotNull(descriptor);
        final Field field = jaxb.getClass().getDeclaredField(property);
        Preconditions.checkNotNull(field);
        if (descriptor.getPropertyType() == Boolean.class) {
            final String booleanGetterMethodName = "is" + field.getName().substring(0, 1).toUpperCase()
                    + field.getName().substring(1);
            final Method booleanGetter = findMethod(jaxb, booleanGetterMethodName);
            Preconditions.checkNotNull(booleanGetter);
            return booleanGetter.invoke(jaxb);
        } else
            return descriptor.getReadMethod().invoke(jaxb);
    }

    private static final class JaxbValidator<T> implements Predicate<T> {

        @Override
        public boolean apply(final T jaxbObject) {
            try {
                Preconditions.checkNotNull(jaxbObject);
                return hasAllRequiredData(jaxbObject);
            } catch (final Throwable t) {
                return false;
            }
        }

    }

}
