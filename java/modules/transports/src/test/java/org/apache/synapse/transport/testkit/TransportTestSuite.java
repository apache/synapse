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

package org.apache.synapse.transport.testkit;

import java.text.ParseException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.commons.lang.StringUtils;
import org.apache.synapse.transport.testkit.channel.AsyncChannel;
import org.apache.synapse.transport.testkit.channel.RequestResponseChannel;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.filter.FilterExpression;
import org.apache.synapse.transport.testkit.filter.FilterExpressionParser;
import org.apache.synapse.transport.testkit.message.RESTMessage;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.message.RESTMessage.Parameter;
import org.apache.synapse.transport.testkit.server.AsyncEndpoint;
import org.apache.synapse.transport.testkit.server.Endpoint;
import org.apache.synapse.transport.testkit.tests.TestResourceSet;
import org.apache.synapse.transport.testkit.tests.TestResourceSetTransition;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;
import org.apache.synapse.transport.testkit.tests.async.BinaryTestCase;
import org.apache.synapse.transport.testkit.tests.async.RESTTestCase;
import org.apache.synapse.transport.testkit.tests.async.SwATestCase;
import org.apache.synapse.transport.testkit.tests.async.TextPlainTestCase;
import org.apache.synapse.transport.testkit.tests.async.XMLAsyncMessageTestCase;
import org.apache.synapse.transport.testkit.tests.echo.XMLRequestResponseMessageTestCase;
import org.apache.synapse.transport.testkit.util.LogManager;

public class TransportTestSuite extends TestSuite {
    public static final String testString = "\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre";
    
    public static final MessageTestData ASCII_TEST_DATA = new MessageTestData("ASCII", "test string", "us-ascii");
    public static final MessageTestData UTF8_TEST_DATA = new MessageTestData("UTF8", testString, "UTF-8");
    public static final MessageTestData LATIN1_TEST_DATA = new MessageTestData("Latin1", testString, "ISO-8859-1");
    
    private static final MessageTestData[] messageTestData = new MessageTestData[] {
        ASCII_TEST_DATA,
        UTF8_TEST_DATA,
        LATIN1_TEST_DATA,
    };
    
    private static final RESTMessage restTestMessage1 = new RESTMessage(new Parameter[] {
        new Parameter("param1", "value1"),
        new Parameter("param2", "value2"),
    });
    
    private static final RESTMessage restTestMessage2 = new RESTMessage(new Parameter[] {
            new Parameter("param", "value1"),
            new Parameter("param", "value2"),
        });
    
    private final Class<?> testClass;
    private final List<FilterExpression> excludes = new LinkedList<FilterExpression>();
    private final boolean reuseResources;
    private boolean invertExcludes;
    private int nextId = 1;
    
    public TransportTestSuite(Class<?> testClass, boolean reuseResources) {
        this.testClass = testClass;
        this.reuseResources = reuseResources;
    }
    
    public TransportTestSuite(Class<?> testClass) {
        this(testClass, true);
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public void addExclude(String filter) throws ParseException {
        excludes.add(FilterExpressionParser.parse(filter));
    }

    public void setInvertExcludes(boolean invertExcludes) {
        this.invertExcludes = invertExcludes;
    }

    public void addSOAP11Test(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, MessageTestData data, Object... resources) {
        addTest(new XMLAsyncMessageTestCase(channel, client, endpoint, XMLMessage.Type.SOAP11, data, resources));
    }
    
    public void addSOAP12Test(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, MessageTestData data, Object... resources) {
        addTest(new XMLAsyncMessageTestCase(channel, client, endpoint, XMLMessage.Type.SOAP12, data, resources));
    }
    
    public void addSOAPTests(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addSOAP11Test(channel, client, endpoint, data, resources);
            addSOAP12Test(channel, client, endpoint, data, resources);
        }
    }
    
    public void addPOXTest(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, MessageTestData data, Object... resources) {
        addTest(new XMLAsyncMessageTestCase(channel, client, endpoint, XMLMessage.Type.POX, data, resources));
    }
    
    public void addPOXTests(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, client, endpoint, data, resources);
        }
    }
    
    public void addPOXTest(RequestResponseChannel channel, RequestResponseTestClient<XMLMessage,XMLMessage> client, Endpoint endpoint, MessageTestData data, Object... resources) {
        addTest(new XMLRequestResponseMessageTestCase(channel, client, endpoint, XMLMessage.Type.POX, data, resources));
    }
    
    public void addPOXTests(RequestResponseChannel channel, RequestResponseTestClient<XMLMessage,XMLMessage> client, Endpoint endpoint, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, client, endpoint, data, resources);
        }
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public void addSwATests(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpoint<XMLMessage> endpoint, Object... resources) {
        addTest(new SwATestCase(channel, client, endpoint, resources));
    }
    
    public void addTextPlainTest(AsyncChannel channel, AsyncTestClient<String> client, AsyncEndpoint<String> endpoint, MessageTestData data, Object... resources) {
        addTest(new TextPlainTestCase(channel, client, endpoint, data, resources));
    }
    
    public void addTextPlainTests(AsyncChannel channel, AsyncTestClient<String> client, AsyncEndpoint<String> endpoint, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addTextPlainTest(channel, client, endpoint, data, resources);
        }
    }
    
    public void addBinaryTest(AsyncChannel channel, AsyncTestClient<byte[]> client, AsyncEndpoint<byte[]> endpoint, Object... resources) {
        addTest(new BinaryTestCase(channel, client, endpoint, resources));
    }

    public void addRESTTests(AsyncChannel channel, AsyncTestClient<RESTMessage> client, AsyncEndpoint<RESTMessage> endpoint, Object... resources) {
        addTest(new RESTTestCase(channel, client, endpoint, restTestMessage1, resources));
        // TODO: regression test for SYNAPSE-431
//        addTest(new RESTTestCase(env, channel, client, endpoint, restTestMessage2));
    }

    @Override
    public void addTest(Test test) {
        if (test instanceof TransportTestCase) {
            TransportTestCase ttest = (TransportTestCase)test;
            Map<String,String> map = ttest.getNameComponents();
            boolean excluded = false;
            for (FilterExpression exclude : excludes) {
                if (exclude.matches(map)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded != invertExcludes) {
                return;
            }
            ttest.init(StringUtils.leftPad(String.valueOf(nextId++), 4, '0'),
                       reuseResources, testClass);
            ttest.getResourceSet().resolve();
        }
        super.addTest(test);
    }

    @Override
    public void run(TestResult result) {
        LogManager logManager = LogManager.INSTANCE;
        if (!reuseResources) {
            super.run(result);
        } else {
            TestResourceSet resourceSet = null;
            for (Enumeration<?> e = tests(); e.hasMoreElements(); ) {
                Test test = (Test)e.nextElement();
                if (test instanceof TransportTestCase) {
                    TransportTestCase ttest = (TransportTestCase)test;
                    TestResourceSet newResourceSet = ttest.getResourceSet();
                    try {
                        if (resourceSet == null) {
                            logManager.setTestCase(ttest);
                            newResourceSet.setUp();
                        } else {
                            TestResourceSetTransition transition = new TestResourceSetTransition(resourceSet, newResourceSet);
                            transition.tearDown();
                            logManager.setTestCase(ttest);
                            transition.setUp();
                        }
                    } catch (Throwable t) {
                        result.addError(this, t);
                        return;
                    }
                    resourceSet = newResourceSet;
                }
                runTest(test, result);
            }
            if (resourceSet != null) {
                try {
                    resourceSet.tearDown();
                    logManager.setTestCase(null);
                } catch (Throwable t) {
                    result.addError(this, t);
                    return;
                }
            }
        }
    }
}
