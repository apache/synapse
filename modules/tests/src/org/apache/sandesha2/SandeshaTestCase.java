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

package org.apache.sandesha2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.llom.factory.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.client.async.AsyncResult;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisOperationFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SandeshaTestCase extends TestCase {
 
	String resourceDir = ""; //"test-resources";
    Properties properties = null;
    final String PROPERTY_FILE_NAME = "sandesha2-test.properties";
    public final int DEFAULT_SERVER_TEST_PORT = 8060;
    public ConfigurationContext serverConfigurationContext = null;
    private final String RMServiceName = "RMSampleService";
	private Log log = LogFactory.getLog(getClass());
    
	private final static String applicationNamespaceName = "http://tempuri.org/"; 
	private final static String echoString = "echoString";
	private final static String ping = "ping";
	private final static String Text = "Text";
	private final static String Sequence = "Sequence";
	private final static String echoStringResponse = "echoStringResponse";
	private final static String EchoStringReturn = "EchoStringReturn";

	protected SimpleHTTPServer httpServer = null;
	protected int serverPort = DEFAULT_SERVER_TEST_PORT;
	protected int waitTime = 60000; // Each test will wait up to 60 seconds, unless we override it here
	protected int tickTime = 1000;  // Each wait will check the test assertions each second
	protected String pingAction = "urn:wsrm:Ping";
	protected String echoAction = "urn:wsrm:EchoString";
	
    public SandeshaTestCase(String name) {
        super(name);
        File baseDir = new File("");
        String testRource = baseDir.getAbsolutePath() + File.separator + "test-resources";
        resourceDir = new File(testRource).getPath();
        
        String propFileStr = resourceDir + File.separator + PROPERTY_FILE_NAME;
        properties = new Properties ();
        
        try {
			FileInputStream propertyFile = new FileInputStream (new File(propFileStr));
			properties.load(propertyFile);
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}
    }
    
    public void setUp () throws Exception {
		super.setUp();
    	
		String serverPortStr = getTestProperty("test.server.port");
		if (serverPortStr!=null) {
			try {
				serverPort = Integer.parseInt(serverPortStr);
			} catch (NumberFormatException e) {
				log.error(e);
			}
		}
    }
    
	public ConfigurationContext startServer(String repoPath, String axis2_xml)
	throws Exception {

		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(repoPath,axis2_xml);

		httpServer = new SimpleHTTPServer (configContext,serverPort);
		httpServer.start();
		Thread.sleep(300);
		
		return configContext;
	}

	public void tearDown () throws Exception {
		if (httpServer!=null)
			httpServer.stop();
		
		Thread.sleep(300);
	}

	protected InputStreamReader getResource(String relativePath, String resourceName) {
        String resourceFile = resourceDir + relativePath + File.separator + resourceName;
        try {
            FileReader reader = new FileReader(resourceFile);
            return reader;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("cannot load the test-resource", e);
        }
    }

    protected SOAPEnvelope getSOAPEnvelope() {
        return OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
    }

    protected SOAPEnvelope getSOAPEnvelope(String relativePath, String resourceName) {
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(
                    getResource(relativePath, resourceName));
            OMXMLParserWrapper wrapper = OMXMLBuilderFactory.createStAXSOAPModelBuilder(
                    OMAbstractFactory.getSOAP11Factory(), reader);
            return (SOAPEnvelope) wrapper.getDocumentElement();

        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected SOAPEnvelope getEmptySOAPEnvelope() {
        return OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
    }

    protected static OMElement getEchoOMBlock(String text, String sequenceKey) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace applicationNamespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement echoStringElement = fac.createOMElement(echoString, applicationNamespace);
		OMElement textElem = fac.createOMElement(Text,applicationNamespace);
		OMElement sequenceElem = fac.createOMElement(Sequence,applicationNamespace);
		
		textElem.setText(text);
		sequenceElem.setText(sequenceKey);
		echoStringElement.addChild(textElem);
		echoStringElement.addChild(sequenceElem);
		
		return echoStringElement;
	}
    
	protected OMElement getPingOMBlock(String text) {
		OMFactory fac = OMAbstractFactory.getOMFactory();
		OMNamespace namespace = fac.createOMNamespace(applicationNamespaceName,"ns1");
		OMElement pingElem = fac.createOMElement(ping, namespace);
		OMElement textElem = fac.createOMElement(Text, namespace);
		
		textElem.setText(text);
		pingElem.addChild(textElem);

		return pingElem;
	}

	protected String checkEchoOMBlock(OMElement response) {
		assertEquals("Response namespace", applicationNamespaceName, response.getNamespace().getNamespaceURI());
		assertEquals("Response local name", echoStringResponse, response.getLocalName());
		
		OMElement echoStringReturnElem = response.getFirstChildWithName(new QName (applicationNamespaceName,EchoStringReturn));
		assertNotNull("Echo String Return", echoStringReturnElem);
		
		String resultStr = echoStringReturnElem.getText();
		return resultStr;
	}

    public String getTestProperty (String key) {
    	if (properties!=null)
    		return properties.getProperty(key);
    	 
    	return null;
    }
    
    public void overrideConfigurationContext (ConfigurationContext context,MessageReceiver messageReceiver, String operationName, boolean newOperation, int mep) throws Exception  {
    	
    	
    	AxisService rmService = context.getAxisConfiguration().getService(RMServiceName);
    	
    	AxisOperation operation = null;
    	
    	if (newOperation) {
    		operation = rmService.getOperation(new QName (operationName));
    		if (operation==null)
    			throw new Exception ("Given operation not found");
    	} else {
    		operation = AxisOperationFactory.getAxisOperation(mep);
    		rmService.addOperation(operation);
    	}
    	
    	operation.setMessageReceiver(messageReceiver);
    }

	protected class TestCallback extends Callback {

		String name = null;
		boolean completed = false;
		boolean errorRported = false;
		String resultStr;
		
		public boolean isCompleted() {
			return completed;
		}

		public boolean isErrorRported() {
			return errorRported;
		}

		public String getResult () {
			return resultStr;
		}
		
		public TestCallback (String name) {
			this.name = name;
		}
		
		public void onComplete(AsyncResult result) {
			SOAPBody body = result.getResponseEnvelope().getBody();
			OMElement contents = body.getFirstElement();
			this.resultStr = checkEchoOMBlock(contents);
			completed = true;
			System.out.println("TestCallback got text: '" + resultStr + "'");
		}

		public void onError (Exception e) {
			e.printStackTrace();
			errorRported = true;
		}
	}

}
