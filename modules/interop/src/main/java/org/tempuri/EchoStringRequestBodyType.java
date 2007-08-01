/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tempuri;

import javax.xml.stream.XMLStreamException;

import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

/**
 * EchoStringRequestBodyType bean class
 */

public class EchoStringRequestBodyType implements
		org.apache.axis2.databinding.ADBBean {
	/*
	 * This type was generated from the piece of schema that had name =
	 * EchoStringRequest.BodyType Namespace URI = http://tempuri.org/ Namespace
	 * Prefix = ns1
	 */

	/**
	 * field for Text
	 */

	protected java.lang.String localText;

	/*
	 * This tracker boolean wil be used to detect whether the user called the
	 * set method for this attribute. It will be used to determine whether to
	 * include this field in the serialized XML
	 */
	protected boolean localTextTracker = false;

	/**
	 * Auto generated getter method
	 * 
	 * @return java.lang.String
	 */
	public java.lang.String getText() {
		return localText;
	}

	/**
	 * Auto generated setter method
	 * 
	 * @param param
	 *            Text
	 */
	public void setText(java.lang.String param) {

		// update the setting tracker
		localTextTracker = true;

		this.localText = param;

	}

	/**
	 * field for Sequence
	 */

	protected java.lang.String localSequence;

	/*
	 * This tracker boolean wil be used to detect whether the user called the
	 * set method for this attribute. It will be used to determine whether to
	 * include this field in the serialized XML
	 */
	protected boolean localSequenceTracker = false;

	/**
	 * Auto generated getter method
	 * 
	 * @return java.lang.String
	 */
	public java.lang.String getSequence() {
		return localSequence;
	}

	/**
	 * Auto generated setter method
	 * 
	 * @param param
	 *            Sequence
	 */
	public void setSequence(java.lang.String param) {

		// update the setting tracker
		localSequenceTracker = true;

		this.localSequence = param;

	}

	/**
	 * 
	 * @param parentQName
	 * @param factory
	 * @return org.apache.axiom.om.OMElement
	 */
	public org.apache.axiom.om.OMElement getOMElement(
			final javax.xml.namespace.QName parentQName,
			final org.apache.axiom.om.OMFactory factory) {

		org.apache.axiom.om.OMDataSource dataSource = new org.apache.axis2.databinding.ADBDataSource(
				this, parentQName) {

			public void serialize(javax.xml.stream.XMLStreamWriter xmlWriter)
					throws javax.xml.stream.XMLStreamException {

				java.lang.String prefix = parentQName.getPrefix();
				java.lang.String namespace = parentQName.getNamespaceURI();

				if (namespace != null) {
					java.lang.String writerPrefix = xmlWriter
							.getPrefix(namespace);
					if (writerPrefix != null) {
						xmlWriter.writeStartElement(namespace, parentQName
								.getLocalPart());
					} else {
						if (prefix == null) {
							prefix = org.apache.axis2.databinding.utils.BeanUtil
									.getUniquePrefix();
						}

						xmlWriter.writeStartElement(prefix, parentQName
								.getLocalPart(), namespace);
						xmlWriter.writeNamespace(prefix, namespace);
						xmlWriter.setPrefix(prefix, namespace);
					}
				} else {
					xmlWriter.writeStartElement(parentQName.getLocalPart());
				}

				if (localTextTracker) {
					namespace = "http://tempuri.org/";

					if (!namespace.equals("")) {
						prefix = xmlWriter.getPrefix(namespace);

						if (prefix == null) {
							prefix = org.apache.axis2.databinding.utils.BeanUtil
									.getUniquePrefix();

							xmlWriter.writeStartElement(prefix, "Text",
									namespace);
							xmlWriter.writeNamespace(prefix, namespace);
							xmlWriter.setPrefix(prefix, namespace);

						} else {
							xmlWriter.writeStartElement(namespace, "Text");
						}

					} else {
						xmlWriter.writeStartElement("Text");
					}

					if (localText == null) {
						// write the nil attribute
						writeAttribute("xsi",
								"http://www.w3.org/2001/XMLSchema-instance",
								"nil", "true", xmlWriter);
					} else {

						xmlWriter
								.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil
										.convertToString(localText));

					}

					xmlWriter.writeEndElement();
				}
				if (localSequenceTracker) {
					namespace = "http://tempuri.org/";

					if (!namespace.equals("")) {
						prefix = xmlWriter.getPrefix(namespace);

						if (prefix == null) {
							prefix = org.apache.axis2.databinding.utils.BeanUtil
									.getUniquePrefix();

							xmlWriter.writeStartElement(prefix, "Sequence",
									namespace);
							xmlWriter.writeNamespace(prefix, namespace);
							xmlWriter.setPrefix(prefix, namespace);

						} else {
							xmlWriter.writeStartElement(namespace, "Sequence");
						}

					} else {
						xmlWriter.writeStartElement("Sequence");
					}

					if (localSequence == null) {
						// write the nil attribute
						writeAttribute("xsi",
								"http://www.w3.org/2001/XMLSchema-instance",
								"nil", "true", xmlWriter);
					} else {

						xmlWriter
								.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil
										.convertToString(localSequence));

					}

					xmlWriter.writeEndElement();
				}

				xmlWriter.writeEndElement();

			}

			/**
			 * Util method to write an attribute with the ns prefix
			 */
			private void writeAttribute(java.lang.String prefix,
					java.lang.String namespace, java.lang.String attName,
					java.lang.String attValue,
					javax.xml.stream.XMLStreamWriter xmlWriter)
					throws javax.xml.stream.XMLStreamException {
				if (xmlWriter.getPrefix(namespace) == null) {
					xmlWriter.writeNamespace(prefix, namespace);
					xmlWriter.setPrefix(prefix, namespace);

				}

				xmlWriter.writeAttribute(namespace, attName, attValue);

			}

			/**
			 * Util method to write an attribute without the ns prefix
			 */
			private void writeAttribute(java.lang.String namespace,
					java.lang.String attName, java.lang.String attValue,
					javax.xml.stream.XMLStreamWriter xmlWriter)
					throws javax.xml.stream.XMLStreamException {
				if (namespace.equals("")) {
					xmlWriter.writeAttribute(attName, attValue);
				} else {
					registerPrefix(xmlWriter, namespace);
					xmlWriter.writeAttribute(namespace, attName, attValue);
				}
			}

			/**
			 * Register a namespace prefix
			 */
			private java.lang.String registerPrefix(
					javax.xml.stream.XMLStreamWriter xmlWriter,
					java.lang.String namespace)
					throws javax.xml.stream.XMLStreamException {
				java.lang.String prefix = xmlWriter.getPrefix(namespace);

				if (prefix == null) {
					prefix = createPrefix();

					while (xmlWriter.getNamespaceContext().getNamespaceURI(
							prefix) != null) {
						prefix = createPrefix();
					}

					xmlWriter.writeNamespace(prefix, namespace);
					xmlWriter.setPrefix(prefix, namespace);
				}

				return prefix;
			}

			/**
			 * Create a prefix
			 */
			private java.lang.String createPrefix() {
				return "ns" + (int) Math.random();
			}

			public void serialize(MTOMAwareXMLStreamWriter arg0) throws XMLStreamException {
				// TODO Auto-generated method stub
				
			}
		};

		return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(
				parentQName, factory, dataSource);

	}

	/**
	 * databinding method to get an XML representation of this object
	 * 
	 */
	public javax.xml.stream.XMLStreamReader getPullParser(
			javax.xml.namespace.QName qName) {

		java.util.ArrayList elementList = new java.util.ArrayList();
		java.util.ArrayList attribList = new java.util.ArrayList();

		if (localTextTracker) {
			elementList.add(new javax.xml.namespace.QName(
					"http://tempuri.org/", "Text"));

			elementList.add(localText == null ? null
					: org.apache.axis2.databinding.utils.ConverterUtil
							.convertToString(localText));
		}
		if (localSequenceTracker) {
			elementList.add(new javax.xml.namespace.QName(
					"http://tempuri.org/", "Sequence"));

			elementList.add(localSequence == null ? null
					: org.apache.axis2.databinding.utils.ConverterUtil
							.convertToString(localSequence));
		}

		return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(
				qName, elementList.toArray(), attribList.toArray());

	}

	/**
	 * Factory class that keeps the parse method
	 */
	public static class Factory {

		/**
		 * static method to create the object Precondition: If this object is an
		 * element, the current or next start element starts this object and any
		 * intervening reader events are ignorable If this object is not an
		 * element, it is a complex type and the reader is at the event just
		 * after the outer start element Postcondition: If this object is an
		 * element, the reader is positioned at its end element If this object
		 * is a complex type, the reader is positioned at the end element of its
		 * outer element
		 */
		public static EchoStringRequestBodyType parse(
				javax.xml.stream.XMLStreamReader reader)
				throws java.lang.Exception {
			EchoStringRequestBodyType object = new EchoStringRequestBodyType();
			int event;
			try {

				while (!reader.isStartElement() && !reader.isEndElement())
					reader.next();

				if (reader.getAttributeValue(
						"http://www.w3.org/2001/XMLSchema-instance", "type") != null) {
					java.lang.String fullTypeName = reader
							.getAttributeValue(
									"http://www.w3.org/2001/XMLSchema-instance",
									"type");
					if (fullTypeName != null) {
						java.lang.String nsPrefix = fullTypeName.substring(0,
								fullTypeName.indexOf(":"));
						nsPrefix = nsPrefix == null ? "" : nsPrefix;

						java.lang.String type = fullTypeName
								.substring(fullTypeName.indexOf(":") + 1);
						if (!"EchoStringRequest.BodyType".equals(type)) {
							// find namespace for the prefix
							java.lang.String nsUri = reader
									.getNamespaceContext().getNamespaceURI(
											nsPrefix);
							return (EchoStringRequestBodyType) org.tempuri.ExtensionMapper
									.getTypeObject(nsUri, type, reader);
						}

					}

				}

				// Note all attributes that were handled. Used to differ normal
				// attributes
				// from anyAttributes.
				java.util.Vector handledAttributes = new java.util.Vector();

				boolean isReaderMTOMAware = false;

				try {
					isReaderMTOMAware = java.lang.Boolean.TRUE
							.equals(reader
									.getProperty(org.apache.axiom.om.OMConstants.IS_DATA_HANDLERS_AWARE));
				} catch (java.lang.IllegalArgumentException e) {
					isReaderMTOMAware = false;
				}

				reader.next();

				while (!reader.isStartElement() && !reader.isEndElement())
					reader.next();

				if (reader.isStartElement()
						&& new javax.xml.namespace.QName("http://tempuri.org/",
								"Text").equals(reader.getName())) {

					if (!"true"
							.equals(reader
									.getAttributeValue(
											"http://www.w3.org/2001/XMLSchema-instance",
											"nil"))) {

						java.lang.String content = reader.getElementText();

						object
								.setText(org.apache.axis2.databinding.utils.ConverterUtil
										.convertToString(content));

					} else {
						reader.getElementText(); // throw away text nodes if
													// any.
					}

					reader.next();

				} // End of if for expected property start element

				while (!reader.isStartElement() && !reader.isEndElement())
					reader.next();

				if (reader.isStartElement()
						&& new javax.xml.namespace.QName("http://tempuri.org/",
								"Sequence").equals(reader.getName())) {

					if (!"true"
							.equals(reader
									.getAttributeValue(
											"http://www.w3.org/2001/XMLSchema-instance",
											"nil"))) {

						java.lang.String content = reader.getElementText();

						object
								.setSequence(org.apache.axis2.databinding.utils.ConverterUtil
										.convertToString(content));

					} else {
						reader.getElementText(); // throw away text nodes if
													// any.
					}

					reader.next();

				} // End of if for expected property start element

				while (!reader.isStartElement() && !reader.isEndElement())
					reader.next();
				if (reader.isStartElement())
					// A start element we are not expecting indicates a trailing
					// invalid property
					throw new java.lang.RuntimeException(
							"Unexpected subelement " + reader.getLocalName());

			} catch (javax.xml.stream.XMLStreamException e) {
				throw new java.lang.Exception(e);
			}

			return object;
		}

	}// end of factory class

}
