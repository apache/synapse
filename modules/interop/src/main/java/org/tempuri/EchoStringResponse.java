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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.ADBException;

/**
 * EchoStringResponse bean class
 */

public class EchoStringResponse implements org.apache.axis2.databinding.ADBBean {

	public static final javax.xml.namespace.QName MY_QNAME = new javax.xml.namespace.QName(
			"http://tempuri.org/", "echoStringResponse", "ns1");

	/**
	 * field for EchoStringResponse
	 */

	protected org.tempuri.EchoStringResponseBodyType localEchoStringResponse;

	/**
	 * Auto generated getter method
	 * 
	 * @return org.tempuri.EchoStringResponseBodyType
	 */
	public org.tempuri.EchoStringResponseBodyType getEchoStringResponse() {
		return localEchoStringResponse;
	}

	/**
	 * Auto generated setter method
	 * 
	 * @param param
	 *            EchoStringResponse
	 */
	public void setEchoStringResponse(
			org.tempuri.EchoStringResponseBodyType param) {

		this.localEchoStringResponse = param;

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

				// We can safely assume an element has only one type associated
				// with it

				if (localEchoStringResponse == null) {
					java.lang.String namespace = "http://tempuri.org/";

					if (!namespace.equals("")) {
						java.lang.String prefix = xmlWriter
								.getPrefix(namespace);

						if (prefix == null) {
							prefix = org.apache.axis2.databinding.utils.BeanUtil
									.getUniquePrefix();

							xmlWriter.writeStartElement(prefix,
									"echoStringResponse", namespace);
							xmlWriter.writeNamespace(prefix, namespace);
							xmlWriter.setPrefix(prefix, namespace);

						} else {
							xmlWriter.writeStartElement(namespace,
									"echoStringResponse");
						}

					} else {
						xmlWriter.writeStartElement("echoStringResponse");
					}

					// write the nil attribute
					writeAttribute("xsi",
							"http://www.w3.org/2001/XMLSchema-instance", "nil",
							"true", xmlWriter);
					xmlWriter.writeEndElement();
				} else {
					localEchoStringResponse.getOMElement(MY_QNAME, factory)
							.serialize(xmlWriter);
				}

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

		};

		// ignore the QName passed in - we send only OUR QName!
		return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(MY_QNAME,
				factory, dataSource);

	}

	/**
	 * databinding method to get an XML representation of this object
	 * 
	 */
	public javax.xml.stream.XMLStreamReader getPullParser(
			javax.xml.namespace.QName qName) {

		// We can safely assume an element has only one type associated with it

		if (localEchoStringResponse == null) {
			return new org.apache.axis2.databinding.utils.reader.NullXMLStreamReader(
					MY_QNAME);
		} else {
			return localEchoStringResponse.getPullParser(MY_QNAME);
		}

	}
	
	public void serialize(final QName parentQName,
		XMLStreamWriter xmlWriter)throws XMLStreamException, ADBException{}

	public void serialize(final QName parentQName,
		XMLStreamWriter xmlWriter,
		boolean serializeType)throws XMLStreamException, ADBException{}

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
		public static EchoStringResponse parse(
				javax.xml.stream.XMLStreamReader reader)
				throws java.lang.Exception {
			EchoStringResponse object = new EchoStringResponse();
			int event;
			try {

				while (!reader.isStartElement() && !reader.isEndElement())
					reader.next();

				if ("true".equals(reader.getAttributeValue(
						"http://www.w3.org/2001/XMLSchema-instance", "nil"))) {
					// Skip the element and report the null value. It cannot
					// have subelements.
					while (!reader.isEndElement())
						reader.next();
					return null;
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

				while (!reader.isEndElement()) {
					if (reader.isStartElement()) {

						if (reader.isStartElement()
								&& new javax.xml.namespace.QName(
										"http://tempuri.org/",
										"echoStringResponse").equals(reader
										.getName())) {

							object
									.setEchoStringResponse(org.tempuri.EchoStringResponseBodyType.Factory
											.parse(reader));

						} // End of if for expected property start element

						else {
							// A start element we are not expecting indicates an
							// invalid parameter was passed
							throw new java.lang.RuntimeException(
									"Unexpected subelement "
											+ reader.getLocalName());
						}

					} else
						reader.next();
				} // end of while loop

			} catch (javax.xml.stream.XMLStreamException e) {
				throw new java.lang.Exception(e);
			}

			return object;
		}

	}// end of factory class

}
