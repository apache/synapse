package org.apache.synapse.axis2.utils;

import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.synapse.SynapseException;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

/**
 *  Resource helper class that read resource out of file system or someother place
 *  Present code just uses GET interface to get some resources.
 *  This is simple yet powerful enough to to do a REST request with GET.
 */
public class ResourcesHandler {
    public static OMElement simpleGETRquest(String url) {
        OMElement response = null;
        HttpClient httpClient = new HttpClient();
        //todo: need a way to indentify the locally available resources and other resources
        // todo: locations can be avilable from synapse.xml or some other property file.
        /*
         Following code technically gives the mediators to do a simple Http GET and aquire
         some resources. Any mediator can use this method to quire its resources. 
        */
        GetMethod httpGet = new GetMethod(url);
        try {

            httpClient.executeMethod(httpGet);
            if (httpGet.getStatusCode() == HttpStatus.SC_OK) {
                response =  processResponse(httpGet.getResponseBodyAsStream());
            }
        } catch (IOException e) {
            throw new SynapseException(e);
        } finally{
            httpGet.releaseConnection();
        }
        if (response == null) throw new SynapseException("There are no valid resonse resources");
        return response;
    }

    private static OMElement processResponse(InputStream inputStream) {
        if (inputStream == null) {
            throw new SynapseException("Input Stream cannot be null for Resource request");
        }
        try {
            XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(OMAbstractFactory.getOMFactory(),parser);
            OMElement resourceElement = builder.getDocumentElement();
            resourceElement.build(); //make sure we are in safe side
            return  resourceElement;
        } catch (XMLStreamException e) {
            throw new SynapseException(e);
        }
    }
}
