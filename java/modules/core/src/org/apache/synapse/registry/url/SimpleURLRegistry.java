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
package org.apache.synapse.registry.url;

import org.apache.synapse.registry.Registry;
import org.apache.synapse.registry.AbstractRegistry;
import org.apache.synapse.registry.RegistryEntry;
import org.apache.synapse.SynapseException;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.net.*;
import java.io.IOException;

/**
 * A Simple HTTP GET based registry which will work with a Web Server / WebDAV
 *
 * This saves the root server URL, and appends the a given key to construct the
 * full URL to locate resources
 */
public class SimpleURLRegistry extends AbstractRegistry implements Registry {

    private static final Log log = LogFactory.getLog(SimpleURLRegistry.class);

    /** The root for the URLs */
    private String root = "";

    /** default cachable duration */
    private long cachableDuration = 15000;

    public OMNode lookup(String key) {

        log.info("==> Repository fetch of resource with key : " + key);
        try {
            URL url = new URL(root + key);
            URLConnection urlc = url.openConnection();

            XMLStreamReader parser = XMLInputFactory.newInstance().
                createXMLStreamReader(urlc.getInputStream());
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            return builder.getDocumentElement();

        } catch (MalformedURLException e) {
            handleException("Invalid URL reference " + root + key, e);
        } catch (IOException e) {
            handleException("IO Error reading from URL " + root + key, e);
        } catch (XMLStreamException e) {
            handleException("XML Error reading from URL " + root + key, e);
        }
        return null;
    }

    public RegistryEntry getRegistryEntry(String key) {

        log.debug("Perform RegistryEntry lookup for key : " + key);
        try {
            URL url = new URL(root + key);
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
                wre.setCachableDuration(cachableDuration);
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

    public void setConfigProperty(String name, String value) {
        if ("root".equals(name)) {
            this.root = value;
        } else if ("cachableDuration".equals(name)) {
            this.cachableDuration = Long.parseLong(value);
        }
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
