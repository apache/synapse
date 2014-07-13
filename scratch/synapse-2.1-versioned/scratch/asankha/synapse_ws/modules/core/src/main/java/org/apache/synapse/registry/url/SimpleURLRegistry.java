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
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.registry.AbstractRegistry;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.registry.RegistryEntry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * A Simple HTTP GET based registry which will work with a Web Server / WebDAV
 * <p/>
 * This saves the root server URL, and appends the a given key to construct the
 * full URL to locate resources
 */
public class SimpleURLRegistry extends AbstractRegistry implements Registry {

    private static final Log log = LogFactory.getLog(SimpleURLRegistry.class);

    private static final int MAX_KEYS = 200;

    public OMNode lookup(String key) {

        log.info("==> Repository fetch of resource with key : " + key);
        URLConnection urlc = null;
        try {
            URL url = SynapseConfigUtils.getURLFromPath(getRoot() + key);
            if (url == null) {
                return null;
            }
            urlc = url.openConnection();
            urlc.connect();
        } catch (IOException e) {
            return null;
        }

        try {
            XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(urlc.getInputStream());
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            return builder.getDocumentElement();

        } catch (MalformedURLException e) {
            handleException("Invalid URL reference " + getRoot() + key, e);
        } catch (FileNotFoundException fnf) {
            return null;
        } catch (IOException e) {
            handleException("IO Error reading from URL " + getRoot() + key, e);
        } catch (XMLStreamException e) {
            handleException("XML Error reading from URL " + getRoot() + key, e);
        }
        return null;
    }

    public RegistryEntry getRegistryEntry(String key) {
        if (log.isDebugEnabled()) {
            log.debug("Perform RegistryEntry lookup for key : " + key);
        }
        try {
            URL url = SynapseConfigUtils.getURLFromPath(getRoot() + key);
            if (url == null) {
                return null;
            }
            URLConnection urlc = url.openConnection();
            urlc.setReadTimeout(30000);
            urlc.setRequestProperty("Connection", "Close");

            URLRegistryEntry wre = new URLRegistryEntry();
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
                wre.setCachableDuration(getCachableDuration());
            }
            return wre;

        } catch (MalformedURLException e) {
            handleException("Invalid URL reference " + getRoot() + key, e);
        } catch (IOException e) {
            handleException("IO Error reading from URL " + getRoot() + key, e);
        } catch (URISyntaxException e) {
            handleException("URI Syntax error reading from URL " + getRoot() + key, e);
        }
        return null;
    }

    public void addConfigProperty(String name, String value) {

        if (name.equals("root")) {

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
        }

        super.addConfigProperty(name, value);
    }

    public String getRoot() {
        String root = (String) properties.get("root");
        if (root == null) {
            return "";
        } else {
            return root;
        }
    }

    public long getCachableDuration() {
        String cachableDuration = (String) properties.get("cachableDuration");
        return cachableDuration == null ? 1500 : Long.parseLong(cachableDuration);
    }

    public RegistryEntry[] getChildren(RegistryEntry entry) {
        URL url;
        if (entry == null) {
            URLRegistryEntry urlEntry = new URLRegistryEntry();
            urlEntry.setKey("");
            entry = urlEntry;
        }
        url = SynapseConfigUtils.getURLFromPath(getRoot() + entry.getKey());
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
                    URLRegistryEntry registryEntry = new URLRegistryEntry();
                    if (entry.getKey().equals("")) {
                        registryEntry.setKey(key);
                    } else {
                        if (entry.getKey().endsWith("/")) {
                            registryEntry.setKey(entry.getKey() + key);
                        } else {
                            registryEntry.setKey(entry.getKey() + "/" + key);
                        }
                    }

                    entryList.add(registryEntry);
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
}
