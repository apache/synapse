/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URI;
import java.net.MalformedURLException;

public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    /**
     * Return a StreamSource for the given Object
     * @param o the object
     * @return the StreamSource
     */
    public static StreamSource getStreamSource(Object o) {

        if (o == null) {
            handleException("Cannot convert null to a StreamSource");

        } else if (o instanceof OMNode) {
            OMNode omNode = (OMNode) o;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                omNode.serialize(baos);
                return new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
            } catch (XMLStreamException e) {
                handleException("Error converting to a StreamSource", e);
            }

        } else {
            handleException("Cannot convert object to a StreamSource");
        }
        return null;
    }

    public static InputStream getInputStream(Object o) {

        if (o == null) {
            handleException("Cannot convert null to a StreamSource");

        } else if (o instanceof OMElement) {
            OMElement omElement = (OMElement) o;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                omElement.serialize(baos);
                return new ByteArrayInputStream(baos.toByteArray());
            } catch (XMLStreamException e) {
                handleException("Error converting to a StreamSource", e);
            }

        } else if (o instanceof URI){
            try {
                return ((URI)(o)).toURL().openStream();                   
            } catch (IOException e) {
                handleException("Error opening stream form URI",e);
            }
        }
        else {
            handleException("Cannot convert object to a StreamSource");
        }
        return null;
    }

    /**
     * Get an object from a given URL. Will first fetch the content from the
     * URL and depending on the content-type, a suitable XMLToObjectMapper
     * (if available) would be used to transform this content into an Object.
     * If a suitable XMLToObjectMapper cannot be found, the content would be
     * treated as XML and an OMNode would be returned
     * @param url the URL to the resource
     * @return an Object created from the given URL
     */
    public static Object getObject(URL url) {
        try {
            URLConnection urlc = url.openConnection();
            XMLToObjectMapper xmlToObject =
                getXmlToObjectMapper(urlc.getContentType());

            try {
                XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(urlc.getInputStream());
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement omElem =  builder.getDocumentElement();

                // detach from URL connection and keep in memory
                // TODO remove this 
                omElem.build();

                if (xmlToObject != null) {
                    return xmlToObject.getObjectFromOMNode(omElem);
                } else {
                    return omElem;
                }

            } catch (XMLStreamException e) {
                log.warn("Content at URL : " + url + " is non XML..");
                return urlc.getContent();
            }

        } catch (IOException e) {
            handleException("Error connecting to URL : " + url, e);
        }
        return null;
    }

    /**
     * Return an OMElement from a URL source
     * @param url a URL string
     * @return an OMElement of the resource
     * @throws IOException for invalid URL's or IO errors
     */
    public static OMElement getOMElementFromURL(String url) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Connection", "close"); // if http is being used
        InputStream urlInStream = conn.getInputStream();

        if (urlInStream != null) {
            try {
                StAXOMBuilder builder = new StAXOMBuilder(urlInStream);
                OMElement doc = builder.getDocumentElement();
                doc.build();
                return doc;
            } catch (XMLStreamException e) {
                log.error("Error parsing resource at URL : " + url + " as XML", e);
            } finally {
                try {
                    urlInStream.close();
                } catch (IOException ignore) {}
            }
        }
        return null;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /**
     * Return a suitable XMLToObjectMapper for the given content type if one
     * is available, else return null;
     * @param contentType the content type for which a mapper is required
     * @return a suitable XMLToObjectMapper or null if none can be found
     */
    public static XMLToObjectMapper getXmlToObjectMapper(String contentType) {
        return null;
    }
}
