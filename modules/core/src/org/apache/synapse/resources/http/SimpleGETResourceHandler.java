package org.apache.synapse.resources.http;

import org.apache.synapse.resources.ResourceHandler;
import org.apache.synapse.SynapseException;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
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

public class SimpleGETResourceHandler implements ResourceHandler {

    protected HashMap propertyMap = new HashMap();

    public OMElement get(String uri) {
        OMElement response = null;
        HttpClient httpClient = new HttpClient();
        /*
         Following code technically gives the mediators to do a simple Http GET and aquire
         some resources. Any mediator can use this method to quire its resources.
        */
        GetMethod httpGet = new GetMethod(uri);
        try {

            httpClient.executeMethod(httpGet);
            if (httpGet.getStatusCode() == HttpStatus.SC_OK) {
                response = processResponse(httpGet.getResponseBodyAsStream());
            }
        } catch (IOException e) {
            throw new SynapseException(e);
        } finally {
            httpGet.releaseConnection();
        }
        if (response == null)
            throw new SynapseException("There are no valid resonse resources");
        return response;
    }

    public void setProperty(String name, String value) {
        propertyMap.put(name, value);
    }

    public String getProperty(String name) {
        return (String) propertyMap.get(name);
    }

    public String[] getPropertyNames() {

        return ((String[]) propertyMap.keySet()
                .toArray(new String[propertyMap.size()]));
    }

    public boolean isUpdated(String uriRoot){ // used to poll if resource has changed (Pull model)

        //todo: implement this method.
        return false;
    }

    private static OMElement processResponse(InputStream inputStream) {
        if (inputStream == null) {
            throw new SynapseException(
                    "Input Stream cannot be null for Resource request");
        }
        try {
            XMLStreamReader parser = XMLInputFactory.newInstance()
                    .createXMLStreamReader(inputStream);
            StAXOMBuilder builder =
                    new StAXOMBuilder(OMAbstractFactory.getOMFactory(), parser);
            OMElement resourceElement = builder.getDocumentElement();
            resourceElement.build(); //make sure we are in safe side
            return resourceElement;
        } catch (XMLStreamException e) {
            throw new SynapseException(e);
        }
    }
}
