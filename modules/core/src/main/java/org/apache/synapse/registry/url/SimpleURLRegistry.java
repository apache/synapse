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
import org.apache.synapse.registry.AbstractRegistry;
import org.apache.synapse.registry.Registry;
import org.apache.synapse.registry.RegistryEntry;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.net.*;

/**
 * A Simple HTTP GET based registry which will work with a Web Server / WebDAV
 *
 * This saves the root server URL, and appends the a given key to construct the
 * full URL to locate resources
 */
public class SimpleURLRegistry extends AbstractRegistry implements Registry {

    private static final Log log = LogFactory.getLog(SimpleURLRegistry.class);

    public OMNode lookup(String key) {

        log.info("==> Repository fetch of resource with key : " + key);
        try {
            URL url = new URL(getRoot() + key);
            URLConnection urlc = url.openConnection();

            XMLStreamReader parser = XMLInputFactory.newInstance().
                createXMLStreamReader(urlc.getInputStream());
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            return builder.getDocumentElement();

        } catch (MalformedURLException e) {
            handleException("Invalid URL reference " + getRoot() + key, e);
        } catch (IOException e) {
            handleException("IO Error reading from URL " + getRoot() + key, e);
        } catch (XMLStreamException e) {
            handleException("XML Error reading from URL " + getRoot() + key, e);
        }
        return null;
    }

    public RegistryEntry getRegistryEntry(String key) {

        log.debug("Perform RegistryEntry lookup for key : " + key);
        try {
            URL url = new URL(getRoot() + key);
            URLConnection urlc = url.openConnection();

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

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
