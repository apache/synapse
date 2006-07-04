package org.apache.sandesha2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.om.impl.llom.factory.OMXMLBuilderFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

public class SandeshaTestCase extends TestCase {
 
	String resourceDir = ""; //"test-resources";
    Properties properties = null;
    final String PROPERTY_FILE_NAME = "sandesha2-test.properties";
    public final int DEFAULT_SERVER_TEST_PORT = 8060;
    
	private Log log = LogFactory.getLog(getClass());
    
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
    
    public String getTestProperty (String key) {
    	if (properties!=null)
    		return properties.getProperty(key);
    	else 
    		return null;
    }

}
