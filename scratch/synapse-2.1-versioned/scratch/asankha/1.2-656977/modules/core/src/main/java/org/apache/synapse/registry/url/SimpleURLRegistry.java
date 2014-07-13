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

package org.apache.synapse.registry.url;

import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.registry.AbstractRegistry;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.registry.RegistryEntry;
import org.apache.synapse.registry.RegistryEntryImpl;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * A Simple HTTP GET based registry which will work with a Web Server / WebDAV
 * <p/>
 * This saves the root server URL, and appends the a given key to construct the
 * full URL to locate resources
 */
public class SimpleURLRegistry extends AbstractRegistry implements Registry {

    private static final Log log = LogFactory.getLog(SimpleURLRegistry.class);

    private static final int MAX_KEYS = 200;
    private String root = "";

    public OMNode lookup(String key) {

        log.debug("==> Repository fetch of resource with key : " + key);

        URL url = SynapseConfigUtils.getURLFromPath(root + key);
        if (url == null) {
            return null;
        }

        BufferedInputStream inputStream;
        try {
            URLConnection connection = url.openConnection();
            connection.connect();
            inputStream = new BufferedInputStream(connection.getInputStream());
        } catch (IOException e) {
            return null;
        }

        OMNode result = null;

        if (inputStream != null) {

            try {

                XMLStreamReader parser = XMLInputFactory.newInstance().
                        createXMLStreamReader(inputStream);
                StAXOMBuilder builder = new StAXOMBuilder(parser);
                result = builder.getDocumentElement();

            } catch (OMException ignored) {

                if (log.isDebugEnabled()) {
                    log.debug("The resource at the provided URL isn't " +
                            "well-formed XML,So,takes it as a text");
                }

                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error in closing the input stream. ", e);
                }
                result = SynapseConfigUtils.readNonXML(url);

            } catch (XMLStreamException ignored) {

                if (log.isDebugEnabled()) {
                    log.debug("The resource at the provided URL isn't " +
                            "well-formed XML,So,takes it as a text");
                }

                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error in closing the input stream. ", e);
                }
                result = SynapseConfigUtils.readNonXML(url);

            } finally {

                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error in closing the input stream.", e);
                }

            }

        }
        return result;
    }

    public RegistryEntry getRegistryEntry(String key) {
        if (log.isDebugEnabled()) {
            log.debug("Perform RegistryEntry lookup for key : " + key);
        }
        try {
            URL url = SynapseConfigUtils.getURLFromPath(root + key);
            if (url == null) {
                return null;
            }
            URLConnection urlc = url.openConnection();
            urlc.setReadTimeout(30000);
            urlc.setRequestProperty("Connection", "Close");

            RegistryEntryImpl wre = new RegistryEntryImpl();
            wre.setKey(key);
            wre.setName(url.getFile());
            wre.setType(new URI(urlc.getContentType()));
            wre.setDescription("Resource at : " + url.toString());
            wre.setLastModified(urlc.getLastModified());
            wre.setVersion(urlc.getLastModified());
            if (urlc.getExpiration() > 0) {
                wre.setCachableDuration(
                        urlc.getExpiration() - System.currentTimeMillis());
            } else {
                wre.setCachableDuration(getCachableDuration(key));
            }
            return wre;

        } catch (MalformedURLException e) {
            handleException("Invalid URL reference " + root + key, e);
        } catch (IOException e) {
            handleException("IO Error reading from URL " + root + key, e);
        } catch (URISyntaxException e) {
            handleException("URI Syntax error reading from URL " + root + key, e);
        }
        return null;
    }

    public void init(Properties properties) {
        super.init(properties);
        String value = properties.getProperty("root");
        if (value != null) {

            // if the root is folder, it should always end with '/'
            // therefore, property keys do not have to begin with '/', which could be misleading
            try {
                URL url = new URL(value);
                if (url.getProtocol().equals("file")) {
                    if (!value.endsWith("/")) {
                        value = value + "/";
                    }
                }
            } catch (MalformedURLException e) {
                // don't do any thing if this is not a valid URL
            }
            root = value;
        } else {
            handleException("");
        }

    }


    public void delete(String path) {
        //TODO
    }

    public void newResource(String path, boolean isDirectory) {
        //TODO
    }

    public void updateResource(String path, Object value) {
        //TODO
    }

    public void updateRegistryEntry(RegistryEntry entry) {
        //TODO
    }


    public long getCachableDuration(String rootPath) {
        String cachableDuration = (String) properties.get("cachableDuration");
        return cachableDuration == null ? 1500 : Long.parseLong(cachableDuration);
    }

    public RegistryEntry[] getChildren(RegistryEntry entry) {
        URL url;
        if (entry == null) {
            RegistryEntryImpl entryImpl = new RegistryEntryImpl();
            entryImpl.setKey("");
            entry = entryImpl;
        }
        url = SynapseConfigUtils.getURLFromPath(root + entry.getKey());
        if (url == null) {
            return null;
        }
        if (url.getProtocol().equals("file")) {

            File file = new File(url.getFile());
            if (!file.isDirectory()) {
                return null;
            }
            InputStream inStream = null;
            try {
                inStream = (InputStream) url.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
                ArrayList entryList = new ArrayList();
                String key = "";
                while ((key = reader.readLine()) != null) {
                    RegistryEntryImpl registryEntryImpl = new RegistryEntryImpl();
                    if (entry.getKey().equals("")) {
                        registryEntryImpl.setKey(key);
                    } else {
                        if (entry.getKey().endsWith("/")) {
                            registryEntryImpl.setKey(entry.getKey() + key);
                        } else {
                            registryEntryImpl.setKey(entry.getKey() + "/" + key);
                        }
                    }

                    entryList.add(registryEntryImpl);
                }

                RegistryEntry[] entries = new RegistryEntry[entryList.size()];
                for (int i = 0; i < entryList.size(); i++) {
                    entries[i] = (RegistryEntry) entryList.get(i);
                }
                return entries;

            } catch (Exception e) {
                throw new SynapseException("Error in reading the URL.");
            }

        } else {
            throw new SynapseException("Invalid protocol.");
        }
    }

    public RegistryEntry[] getDescendants(RegistryEntry entry) {

        ArrayList list = new ArrayList();
        RegistryEntry[] entries = getChildren(entry);
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {

                if (list.size() > MAX_KEYS) {
                    break;
                }

                fillDescendants(entries[i], list);
            }
        }

        RegistryEntry[] descendants = new RegistryEntry[list.size()];
        for (int i = 0; i < list.size(); i++) {
            descendants[i] = (RegistryEntry) list.get(i);
        }

        return descendants;
    }

    private void fillDescendants(RegistryEntry parent, ArrayList list) {

        RegistryEntry[] entries = getChildren(parent);
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {

                if (list.size() > MAX_KEYS) {
                    break;
                }

                fillDescendants(entries[i], list);
            }
        } else {
            list.add(parent);
        }
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
