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

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.ServerManager;
import org.apache.synapse.util.SynapseBinaryDataSource;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.activation.DataHandler;
import java.io.*;
import java.net.*;

public class SynapseConfigUtils {

    private static final Log log = LogFactory.getLog(SynapseConfigUtils.class);

    /**
     * Return a StreamSource for the given Object
     *
     * @param o the object
     * @return the StreamSource
     */
    public static StreamSource getStreamSource(Object o) {

        if (o == null) {
            handleException("Cannot convert null to a StreamSource");

        } else if (o instanceof OMElement) {
            OMElement omElement = (OMElement) o;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                omElement.serialize(baos);
                return new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
            } catch (XMLStreamException e) {
                handleException("Error converting to a StreamSource", e);
            }

        } else if (o instanceof OMText) {
            DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
            if (dataHandler != null) {
                try {
                    return new StreamSource(dataHandler.getInputStream());
                } catch (IOException e) {
                    handleException("Error in reading content as a stream ");
                }
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

        } else if (o instanceof OMText) {
            DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
            if (dataHandler != null) {
                try {
                    return dataHandler.getInputStream();
                } catch (IOException e) {
                    handleException("Error in reading content as a stream ");
                }
            }
        } else if (o instanceof URI) {
            try {
                return ((URI) (o)).toURL().openStream();
            } catch (IOException e) {
                handleException("Error opening stream form URI", e);
            }
        } else {
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
     *
     * @param url the URL to the resource
     * @return an Object created from the given URL
     */
    public static Object getObject(URL url) {
        try {
            if (url != null && "file".equals(url.getProtocol())) {
                try {
                    url.openStream();
                } catch (IOException ignored) {
                    String path = url.getPath();
                    if (log.isDebugEnabled()) {
                        log.debug("Can not open a connection to the URL with a path :" +
                                path);
                    }
                    String synapseHome = ServerManager.getInstance().getSynapseHome();
                    if (synapseHome != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying  to resolve an absolute path of the " +
                                    " URL using the synapse.home : " + synapseHome);
                        }
                        if (synapseHome.endsWith("/")) {
                            synapseHome = synapseHome.substring(0, synapseHome.lastIndexOf("/"));
                        }
                        url = new URL(url.getProtocol() + ":" + synapseHome + "/" + path);
                        try {
                            url.openStream();
                        } catch (IOException e) {
                            if (log.isDebugEnabled()) {
                                log.debug("Faild to resolve an absolute path of the " +
                                        " URL using the synapse.home : " + synapseHome);
                            }
                            log.warn("IO Error reading from URL " + url.getPath() + e);
                        }
                    }
                }
            }
            if (url == null) {
                return null;
            }
            URLConnection urlc = url.openConnection();
            XMLToObjectMapper xmlToObject =
                    getXmlToObjectMapper(urlc.getContentType());
            InputStream inputStream = urlc.getInputStream();
            try {
                XMLStreamReader parser = XMLInputFactory.newInstance().
                        createXMLStreamReader(inputStream);
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                OMElement omElem = builder.getDocumentElement();

                // detach from URL connection and keep in memory
                // TODO remove this 
                omElem.build();

                if (xmlToObject != null) {
                    return xmlToObject.getObjectFromOMNode(omElem);
                } else {
                    return omElem;
                }

            } catch (XMLStreamException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Content at URL : " + url + " is non XML..");
                }
                return readNonXML(url);
            } catch (OMException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Content at URL : " + url + " is non XML..");
                }
                return readNonXML(url);
            } finally {
                inputStream.close();
            }

        } catch (IOException e) {
            handleException("Error connecting to URL : " + url, e);
        }
        return null;
    }

    /**
     * Helper method to handle non-XMl resources
     *
     * @param url The resource url
     * @return The content as an OMNode
     */
    public static OMNode readNonXML(URL url) {

        try {
            // Open a new connection
            URLConnection newConnection = url.openConnection();

            BufferedInputStream newInputStream = new BufferedInputStream(
                    newConnection.getInputStream());

            OMFactory omFactory = OMAbstractFactory.getOMFactory();
            return omFactory.createOMText(
                    new DataHandler(new SynapseBinaryDataSource(newInputStream,
                            newConnection.getContentType())), true);

        } catch (IOException e) {
            handleException("Error when getting a stream from resource's content", e);
        }
        return null;
    }

    /**
     * Return an OMElement from a URL source
     *
     * @param urlStr a URL string
     * @return an OMElement of the resource
     * @throws IOException for invalid URL's or IO errors
     */
    public static OMNode getOMElementFromURL(String urlStr) throws IOException {

        URL url = getURLFromPath(urlStr);
        if (url == null) {
            return null;
        }
        URLConnection conn = url.openConnection();
        conn.setReadTimeout(getReadTimeout());
        conn.setConnectTimeout(getConnectionTimeout());
        conn.setRequestProperty("Connection", "close"); // if http is being used
        BufferedInputStream urlInStream = new BufferedInputStream(
                conn.getInputStream());
        try {
            StAXOMBuilder builder = new StAXOMBuilder(urlInStream);
            OMElement doc = builder.getDocumentElement();
            doc.build();
            return doc;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.info("Content at URL : " + url + " is non XML..");
            }

            Object content = readNonXML(url);
            if (content instanceof OMNode) {
                return (OMNode) content;
            }
        } finally {
            try {
                urlInStream.close();
            } catch (IOException ignore) {
            }
        }
        return null;
    }

    public static InputSource getInputSourceFormURI(URI uri) {
        if (uri == null) {
            if (log.isDebugEnabled()) {
                log.debug("Can not create a URL from 'null' ");
            }
            return null;
        }
        try {
            URL url = uri.toURL();
            String protocol = url.getProtocol();
            String path = url.getPath();
            if (protocol == null || "".equals(protocol)) {
                url = new URL("file:" + path);
            }
            URLConnection conn = url.openConnection();
            conn.setReadTimeout(getReadTimeout());
            conn.setConnectTimeout(getConnectionTimeout());
            conn.setRequestProperty("Connection", "close"); // if http is being used
            BufferedInputStream urlInStream = new BufferedInputStream(
                    conn.getInputStream());
            return new InputSource(urlInStream);
        } catch (MalformedURLException e) {
            handleException("Invalid URL ' " + uri + " '", e);
        } catch (IOException e) {
            handleException("Error reading at URI ' " + uri + " ' ", e);
        }
        return null;
    }

    private static int getReadTimeout() {
        return ServerManager.getInstance().getReadTimeout();
    }

    private static int getConnectionTimeout() {
        return ServerManager.getInstance().getConnectTimeout();
    }

    private static void handleException(String msg, Exception e) {
        log.warn(msg, e);
        throw new SynapseException(msg, e);
    }

    private static void handleException(String msg) {
        log.warn(msg);
        throw new SynapseException(msg);
    }

    /**
     * Return a suitable XMLToObjectMapper for the given content type if one
     * is available, else return null;
     *
     * @param contentType the content type for which a mapper is required
     * @return a suitable XMLToObjectMapper or null if none can be found
     */
    public static XMLToObjectMapper getXmlToObjectMapper(String contentType) {
        return null;
    }

    /**
     * Utility method to resolve url(only If need) path using synapse home system property
     *
     * @param path Path to the URL
     * @return Valid URL instance or null(if it is inavalid or can not open a connection to it )
     */
    public static URL getURLFromPath(String path) {
        if (path == null || "null".equals(path)) {
            if (log.isDebugEnabled()) {
                log.debug("Can not create a URL from 'null' ");
            }
            return null;
        }
        URL url = null;
        try {
            url = new URL(path);
            if ("file".equals(url.getProtocol())) {
                try {
                    url.openStream();
                } catch (MalformedURLException e) {
                    handleException("Invalid URL reference : " + path, e);
                } catch (IOException ignored) {
                    if (log.isDebugEnabled()) {
                        log.debug("Can not open a connection to the URL with a path :" +
                                path);
                    }
                    String synapseHome = ServerManager.getInstance().getSynapseHome();
                    if (synapseHome != null) {
                        if (synapseHome.endsWith("/")) {
                            synapseHome = synapseHome.substring(0, synapseHome.lastIndexOf("/"));
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Trying  to resolve an absolute path of the " +
                                    " URL using the synapse.home : " + synapseHome);
                        }
                        try {
                            url = new URL(url.getProtocol() + ":" + synapseHome + "/" +
                                    url.getPath());
                            url.openStream();
                        } catch (MalformedURLException e) {
                            handleException("Invalid URL reference " + url.getPath() + e);
                        } catch (IOException e) {
                            if (log.isDebugEnabled()) {
                                log.debug("Faild to resolve an absolute path of the " +
                                        " URL using the synapse.home : " + synapseHome);
                            }
                            log.warn("IO Error reading from URL : " + url.getPath() + e);
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            handleException("Invalid URL reference :  " + path, e);
        } catch (IOException e) {
            handleException("IO Error reading from URL : " + path, e);
        }
        return url;
    }

    public static String resolveRelativeURI(String parentLocation, String relativeLocation) {

        if (relativeLocation == null) {
            throw new IllegalArgumentException("Import URI cannot be null");
        }

        if (log.isDebugEnabled()) {
            log.debug("Resolving import URI ' " + parentLocation + " '  against base URI ' " + relativeLocation + " '  ");
        }

        URI importUri = null;
        try {
            importUri = new URI(relativeLocation);
            if (importUri.isAbsolute()) {
                return importUri.toString();
            }
        } catch (URISyntaxException e) {
            handleException("Invalid URI : " + relativeLocation, e);
        }

        if (parentLocation == null) {
            return importUri.toString();
        } else {
            // if the importuri is absolute
            if (relativeLocation.startsWith("/") || relativeLocation.startsWith("\\")) {
                if (importUri != null && !importUri.isAbsolute()) {
                    try {
                        importUri = new URI("file:" + relativeLocation);
                        return importUri.toString();
                    } catch (URISyntaxException e) {
                        handleException("Invalid URI ' " + importUri.getPath() + " '", e);
                    }
                }
            } else {
                int index = parentLocation.lastIndexOf("/");
                if (index == -1) {
                    index = parentLocation.lastIndexOf("\\");
                }
                if (index != -1) {
                    String basepath = parentLocation.substring(0, index + 1);
                    String resolvedPath = basepath + relativeLocation;
                    try {
                        URI resolvedUri = new URI(resolvedPath);
                        if (!resolvedUri.isAbsolute()) {
                            resolvedUri = new URI("file:" + resolvedPath);
                        }
                        return resolvedUri.toString();
                    } catch (URISyntaxException e) {
                        handleException("Invalid URI ' " + resolvedPath + " '", e);
                    }
                } else {
                    return importUri.toString();
                }
            }
        }
        return null;
    }
}

