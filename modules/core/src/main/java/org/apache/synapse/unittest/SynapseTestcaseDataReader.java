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

package org.apache.synapse.unittest;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.log4j.Logger;
import org.apache.synapse.unittest.testcase.data.classes.Artifact;
import org.apache.synapse.unittest.testcase.data.classes.AssertEqual;
import org.apache.synapse.unittest.testcase.data.classes.AssertNotNull;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;
import org.apache.synapse.unittest.testcase.data.holders.ArtifactData;
import org.apache.synapse.unittest.testcase.data.holders.TestCaseData;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.synapse.unittest.Constants.ARTIFACT;
import static org.apache.synapse.unittest.Constants.ARTIFACTS;
import static org.apache.synapse.unittest.Constants.ARTIFACT_KEY_ATTRIBUTE;
import static org.apache.synapse.unittest.Constants.ARTIFACT_TRANSPORTS_ATTRIBUTE;
import static org.apache.synapse.unittest.Constants.ASSERTION_ACTUAL;
import static org.apache.synapse.unittest.Constants.ASSERTION_EXPECTED;
import static org.apache.synapse.unittest.Constants.ASSERTION_MESSAGE;
import static org.apache.synapse.unittest.Constants.HTTPS_KEY;
import static org.apache.synapse.unittest.Constants.HTTP_KEY;
import static org.apache.synapse.unittest.Constants.NAME_ATTRIBUTE;
import static org.apache.synapse.unittest.Constants.SUPPORTIVE_ARTIFACTS;
import static org.apache.synapse.unittest.Constants.TEST_ARTIFACT;
import static org.apache.synapse.unittest.Constants.TEST_CASES;
import static org.apache.synapse.unittest.Constants.TEST_CASE_ASSERTIONS;
import static org.apache.synapse.unittest.Constants.TEST_CASE_ASSERTION_EQUALS;
import static org.apache.synapse.unittest.Constants.TEST_CASE_ASSERTION_NOTNULL;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PAYLOAD;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTIES;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTY_NAME;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTY_SCOPE;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTY_VALUE;
import static org.apache.synapse.unittest.Constants.TEST_CASE_REQUEST_METHOD;
import static org.apache.synapse.unittest.Constants.TEST_CASE_REQUEST_PATH;
import static org.apache.synapse.unittest.Constants.TYPE_LOCAL_ENTRY;
import static org.apache.synapse.unittest.Constants.TYPE_PROXY;


/**
 * descriptor data read class in unit test framework.
 */
class SynapseTestcaseDataReader {

    private static Logger log = Logger.getLogger(SynapseTestcaseDataReader.class.getName());
    private OMElement importXMLFile = null;

    /**
     * Constructor of the SynapseTestcaseDataReader class.
     *
     * @param descriptorData defines the descriptor data of the received message
     */
    SynapseTestcaseDataReader(String descriptorData) {
        try {
            this.importXMLFile = AXIOMUtil.stringToOM(descriptorData);

        } catch (Exception e) {
            log.error(e);
        }

    }

    /**
     * Read artifact data from the descriptor data.
     * Append artifact data into the data holder object
     *
     * @return dataHolder object with artifact data
     */
    ArtifactData readAndStoreArtifactData() throws XMLStreamException, IOException {
        ArtifactData artifactDataHolder = new ArtifactData();
        Artifact testArtifact = new Artifact();

        //Read artifact from descriptor data
        QName qualifiedArtifacts = new QName("", ARTIFACTS, "");
        OMElement artifactsNode = importXMLFile.getFirstChildWithName(qualifiedArtifacts);

        QName qualifiedTestArtifact = new QName("", TEST_ARTIFACT, "");
        OMElement testArtifactNode = artifactsNode.getFirstChildWithName(qualifiedTestArtifact);

        QName qualifiedArtifact = new QName("", ARTIFACT, "");
        OMElement testArtifactDataNode = testArtifactNode.getFirstChildWithName(qualifiedArtifact);
        String testArtifactData = testArtifactDataNode.getFirstElement().toString();
        testArtifact.setArtifact(testArtifactData);

        //Read test artifact type from synapse test data
        String testArtifactType = testArtifactDataNode.getFirstElement().getLocalName();
        testArtifact.setArtifactType(testArtifactType);

        //Read artifact name from descriptor data
        String testArtifactNameOrKey;
        if (testArtifactType.equals(TYPE_LOCAL_ENTRY)) {
            testArtifactNameOrKey
                    = testArtifactDataNode.getFirstElement().getAttributeValue(new QName(ARTIFACT_KEY_ATTRIBUTE));
        } else {
            testArtifactNameOrKey
                    = testArtifactDataNode.getFirstElement().getAttributeValue(new QName(NAME_ATTRIBUTE));
        }
        testArtifact.setArtifactNameOrKey(testArtifactNameOrKey);

        //Read artifact transport from descriptor data if artifact is a proxy
        if (testArtifact.getArtifactType().equals(TYPE_PROXY)) {
            String transport = testArtifactDataNode.getFirstElement()
                    .getAttributeValue(new QName(ARTIFACT_TRANSPORTS_ATTRIBUTE));
            if (transport == null) {
                throw new IOException("Local transport method for proxy currently not supported");
            }

            String[] transportMethods = transport.split(" ");
            if (Arrays.asList(transportMethods).contains(HTTP_KEY)) {
                testArtifact.setTransportMethod(HTTP_KEY);
            } else if (Arrays.asList(transportMethods).contains(HTTPS_KEY)) {
                testArtifact.setTransportMethod(HTTPS_KEY);
            } else {
                throw new IOException("Defined transport method for proxy currently not supported");
            }
        }

        artifactDataHolder.setTestArtifact(testArtifact);

        //Read supportive test cases data
        QName qualifiedSupportiveTestArtifact = new QName("", SUPPORTIVE_ARTIFACTS, "");
        OMElement supportiveArtifactsNode = artifactsNode.getFirstChildWithName(qualifiedSupportiveTestArtifact);

        Iterator artifactIterator = Collections.emptyIterator();
        int supportiveArtifactCount = 0;

        if (supportiveArtifactsNode != null) {
            artifactIterator = supportiveArtifactsNode.getChildElements();
        }

        while (artifactIterator.hasNext()) {
            OMElement artifact = (OMElement) artifactIterator.next();
            Artifact supportiveArtifact = new Artifact();

            //Read supportive artifact from synapse test data
            String supportiveArtifactData = artifact.getFirstElement().toString();
            supportiveArtifact.setArtifact(supportiveArtifactData);

            //Read supportive artifact type from synapse test data
            String supportiveArtifactType = artifact.getFirstElement().getLocalName();
            supportiveArtifact.setArtifactType(supportiveArtifactType);

            //Read artifact name from descriptor data
            String supportiveArtifactNameOrKey;
            if (testArtifactType.equals(TYPE_LOCAL_ENTRY)) {
                supportiveArtifactNameOrKey
                        = artifact.getFirstElement().getAttributeValue(new QName(ARTIFACT_KEY_ATTRIBUTE));
            } else {
                supportiveArtifactNameOrKey
                        = artifact.getFirstElement().getAttributeValue(new QName(NAME_ATTRIBUTE));
            }
            supportiveArtifact.setArtifactNameOrKey(supportiveArtifactNameOrKey);

            artifactDataHolder.addSupportiveArtifact(supportiveArtifact);
            supportiveArtifactCount++;
        }

        //set supportive artifact count
        artifactDataHolder.setSupportiveArtifactCount(supportiveArtifactCount);

        log.info("Artifact data from descriptor data read successfully");
        return artifactDataHolder;
    }

    /**
     * Read test-case data from the descriptor data.
     * Append test-case data into the test data holder object
     *
     * @return testCaseDataHolder object with test case data
     */
    TestCaseData readAndStoreTestCaseData() {

        TestCaseData testCaseDataHolder = new TestCaseData();

        //Set test case count as zero
        int testCasesCount = 0;

        //Read test cases from descriptor data
        QName qualifiedTestCases = new QName("", TEST_CASES, "");
        OMElement testCasesNode = importXMLFile.getFirstChildWithName(qualifiedTestCases);

        //Iterate through test-cases in descriptor data
        Iterator<?> testCaseIterator = Collections.emptyIterator();
        if (testCasesNode != null) {
            testCaseIterator = testCasesNode.getChildElements();
        }

        while (testCaseIterator.hasNext()) {
            TestCase testCase = new TestCase();
            OMElement testCaseNode = (OMElement) (testCaseIterator.next());
            String testCaseName = testCaseNode.getAttributeValue(new QName(NAME_ATTRIBUTE));
            testCase.setTestCaseName(testCaseName);

            //Read input child from test-case node
            QName qualifiedInput = new QName("", TEST_CASE_INPUT, "");
            OMElement testCaseInputNode = testCaseNode.getFirstChildWithName(qualifiedInput);

            //Read input node data of payload and properties if not null
            if (testCaseInputNode != null) {
                readTestCaseInputData(testCaseInputNode, testCase);
            }

            //Read assertions of test-case node
            QName qualifiedAssertions = new QName("", TEST_CASE_ASSERTIONS, "");
            OMElement testCaseAssertionNode = testCaseNode.getFirstChildWithName(qualifiedAssertions);
            ArrayList<AssertEqual> assertEquals = new ArrayList<AssertEqual>();
            ArrayList<AssertNotNull> assertNotNulls = new ArrayList<AssertNotNull>();
            readTestCaseAssertions(testCaseAssertionNode, assertEquals, assertNotNulls);

            //set assertion values in testCase object
            testCase.setAssertEquals(assertEquals);
            testCase.setAssertNotNull(assertNotNulls);

            //set testCase object in testCase data holder
            testCaseDataHolder.setTestCases(testCase);
            testCasesCount++;
        }

        //Set test case count in test data holder
        testCaseDataHolder.setTestCaseCount(testCasesCount);

        log.info("Test case data from descriptor data read successfully");
        return testCaseDataHolder;
    }

    /**
     * Read test case input data from the descriptor data.
     * Read payload and properties if exists
     *
     * @param testCaseInputNode node of test cases
     * @param testCase          test case object
     */
    private void readTestCaseInputData(OMElement testCaseInputNode, TestCase testCase) {
        QName qualifiedInputPayload = new QName("", TEST_CASE_INPUT_PAYLOAD, "");
        OMElement testCaseInputPayloadNode = testCaseInputNode.getFirstChildWithName(qualifiedInputPayload);

        if (testCaseInputPayloadNode != null) {
            String inputPayload = testCaseInputPayloadNode.getText();
            testCase.setInputPayload(inputPayload);
        }

        QName qualifiedInputRequestPath = new QName("", TEST_CASE_REQUEST_PATH, "");
        OMElement testCaseRequestPathNode = testCaseInputNode.getFirstChildWithName(qualifiedInputRequestPath);

        if (testCaseRequestPathNode != null) {
            String requestPath = testCaseRequestPathNode.getText();
            testCase.setRequestPath(requestPath);
        }

        QName qualifiedInputRequestMethod = new QName("", TEST_CASE_REQUEST_METHOD, "");
        OMElement testCaseRequestMethodNode = testCaseInputNode.getFirstChildWithName(qualifiedInputRequestMethod);

        if (testCaseRequestMethodNode != null) {
            String requestMethod = testCaseRequestMethodNode.getText();
            testCase.setRequestMethod(requestMethod);
        }


        QName qualifiedInputProperties = new QName("", TEST_CASE_INPUT_PROPERTIES, "");
        OMElement testCaseInputPropertyNode = testCaseInputNode.getFirstChildWithName(qualifiedInputProperties);

        if (testCaseInputPropertyNode != null) {
            Iterator<?> propertyIterator = testCaseInputPropertyNode.getChildElements();

            ArrayList<Map<String, String>> properties = new ArrayList<Map<String, String>>();
            while (propertyIterator.hasNext()) {
                OMElement propertyNode = (OMElement) (propertyIterator.next());

                String propName = propertyNode.getAttributeValue(new QName(TEST_CASE_INPUT_PROPERTY_NAME));
                String propValue = propertyNode.getAttributeValue(new QName(TEST_CASE_INPUT_PROPERTY_VALUE));
                String propScope = "default";

                if (propertyNode.getAttributeValue(new QName(TEST_CASE_INPUT_PROPERTY_SCOPE)) != null) {
                    propScope = propertyNode.getAttributeValue(new QName(TEST_CASE_INPUT_PROPERTY_SCOPE));
                }

                Map<String, String> propertyMap = new HashMap<String, String>();
                propertyMap.put(TEST_CASE_INPUT_PROPERTY_NAME, propName);
                propertyMap.put(TEST_CASE_INPUT_PROPERTY_VALUE, propValue);
                propertyMap.put(TEST_CASE_INPUT_PROPERTY_SCOPE, propScope);
                properties.add(propertyMap);
            }

            testCase.setPropertyMap(properties);
        }
    }


    /**
     * Read test case assertion data from the descriptor data.
     * Read assertEquals and assertNotNull data if exists
     *
     * @param testCaseAssertionNode node of test case assertions
     * @param assertEquals          array of assertEquals
     * @param assertNotNulls        array of assertNotNulls
     */
    private void readTestCaseAssertions(OMElement testCaseAssertionNode, ArrayList<AssertEqual> assertEquals,
                                        ArrayList<AssertNotNull> assertNotNulls) {

        //Read assertions - AssertEquals of test-case node
        Iterator<?> assertEqualsIterator =
                testCaseAssertionNode.getChildrenWithName(new QName(TEST_CASE_ASSERTION_EQUALS));

        while (assertEqualsIterator.hasNext()) {
            AssertEqual assertion = new AssertEqual();

            OMElement assertEqualNode = (OMElement) (assertEqualsIterator.next());
            QName qualifiedAssertActual = new QName("", ASSERTION_ACTUAL, "");
            OMElement assertActualNode = assertEqualNode.getFirstChildWithName(qualifiedAssertActual);
            String actual = assertActualNode.getText();
            assertion.setActual(actual);

            QName qualifiedAssertMessage = new QName("", ASSERTION_MESSAGE, "");
            OMElement assertMessageNode = assertEqualNode.getFirstChildWithName(qualifiedAssertMessage);
            String message = assertMessageNode.getText();
            assertion.setMessage(message);

            QName qualifiedExpectedMessage = new QName("", ASSERTION_EXPECTED, "");
            OMElement assertExpectedNode = assertEqualNode.getFirstChildWithName(qualifiedExpectedMessage);
            String expected = assertExpectedNode.getText();
            assertion.setExpected(expected);

            assertEquals.add(assertion);
        }

        //Read assertions - AssertNotNull of test-case node
        Iterator<?> assertNotNullIterator = testCaseAssertionNode.getChildrenWithName(
                new QName(TEST_CASE_ASSERTION_NOTNULL));
        while (assertNotNullIterator.hasNext()) {
            AssertNotNull assertion = new AssertNotNull();

            OMElement assertEqualNode = (OMElement) (assertNotNullIterator.next());
            QName qualifiedAssertActual = new QName("", ASSERTION_ACTUAL, "");
            OMElement assertActualNode = assertEqualNode.getFirstChildWithName(qualifiedAssertActual);
            String actual = assertActualNode.getText();
            assertion.setActual(actual);

            QName qualifiedAssertMessage = new QName("", ASSERTION_MESSAGE, "");
            OMElement assertMessageNode = assertEqualNode.getFirstChildWithName(qualifiedAssertMessage);
            String message = assertMessageNode.getText();
            assertion.setMessage(message);

            assertNotNulls.add(assertion);
        }
    }
}
