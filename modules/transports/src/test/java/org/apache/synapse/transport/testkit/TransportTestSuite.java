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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.RequestResponseTestClient;
import org.apache.synapse.transport.testkit.filter.FilterExpression;
import org.apache.synapse.transport.testkit.filter.FilterExpressionParser;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;
import org.apache.synapse.transport.testkit.listener.ContentTypeMode;
import org.apache.synapse.transport.testkit.listener.MessageTestData;
import org.apache.synapse.transport.testkit.listener.RequestResponseChannel;
import org.apache.synapse.transport.testkit.message.AxisMessage;
import org.apache.synapse.transport.testkit.message.ByteArrayMessage;
import org.apache.synapse.transport.testkit.message.RESTMessage;
import org.apache.synapse.transport.testkit.message.StringMessage;
import org.apache.synapse.transport.testkit.message.XMLMessage;
import org.apache.synapse.transport.testkit.message.RESTMessage.Parameter;
import org.apache.synapse.transport.testkit.server.AsyncEndpointFactory;
import org.apache.synapse.transport.testkit.server.EndpointFactory;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;
import org.apache.synapse.transport.testkit.tests.async.BinaryTestCase;
import org.apache.synapse.transport.testkit.tests.async.RESTTestCase;
import org.apache.synapse.transport.testkit.tests.async.SwATestCase;
import org.apache.synapse.transport.testkit.tests.async.TextPlainTestCase;
import org.apache.synapse.transport.testkit.tests.async.XMLAsyncMessageTestCase;
import org.apache.synapse.transport.testkit.tests.echo.XMLRequestResponseMessageTestCase;

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
        
    private final List<FilterExpression> excludes = new LinkedList<FilterExpression>();
    private final boolean reuseServer;
    
    public TransportTestSuite(boolean reuseServer) {
        this.reuseServer = reuseServer;
    }
    
    public TransportTestSuite() {
        this(true);
    }

    public void addExclude(String filter) throws ParseException {
        excludes.add(FilterExpressionParser.parse(filter));
    }

    public void addSOAP11Test(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data, Object... resources) {
        addTest(new XMLAsyncMessageTestCase(channel, client, endpointFactory, XMLMessage.Type.SOAP11, contentTypeMode, SOAP11Constants.SOAP_11_CONTENT_TYPE, data, resources));
    }
    
    public void addSOAP12Test(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data, Object... resources) {
        addTest(new XMLAsyncMessageTestCase(channel, client, endpointFactory, XMLMessage.Type.SOAP12, contentTypeMode, SOAP12Constants.SOAP_12_CONTENT_TYPE, data, resources));
    }
    
    public void addSOAPTests(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, ContentTypeMode contentTypeMode, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addSOAP11Test(channel, client, endpointFactory, contentTypeMode, data, resources);
            addSOAP12Test(channel, client, endpointFactory, contentTypeMode, data, resources);
        }
    }
    
    public void addPOXTest(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data, Object... resources) {
        addTest(new XMLAsyncMessageTestCase(channel, client, endpointFactory, XMLMessage.Type.POX, contentTypeMode, "application/xml", data, resources));
    }
    
    public void addPOXTests(AsyncChannel channel, AsyncTestClient<XMLMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, ContentTypeMode contentTypeMode, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, client, endpointFactory, contentTypeMode, data, resources);
        }
    }
    
    public void addPOXTest(RequestResponseChannel channel, RequestResponseTestClient<XMLMessage,XMLMessage> client, EndpointFactory endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data, Object... resources) {
        addTest(new XMLRequestResponseMessageTestCase(channel, client, endpointFactory, contentTypeMode, "application/xml", XMLMessage.Type.POX, data, resources));
    }
    
    public void addPOXTests(RequestResponseChannel channel, RequestResponseTestClient<XMLMessage,XMLMessage> client, EndpointFactory endpointFactory, ContentTypeMode contentTypeMode, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addPOXTest(channel, client, endpointFactory, contentTypeMode, data, resources);
        }
    }
    
    // TODO: this test actually only makes sense if the transport supports a Content-Type header
    public void addSwATests(AsyncChannel channel, AsyncTestClient<ByteArrayMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, Object... resources) {
        addTest(new SwATestCase(channel, client, endpointFactory, resources));
    }
    
    public void addTextPlainTest(AsyncChannel channel, AsyncTestClient<StringMessage> client, AsyncEndpointFactory<StringMessage> endpointFactory, ContentTypeMode contentTypeMode, MessageTestData data, Object... resources) {
        addTest(new TextPlainTestCase(channel, client, endpointFactory, contentTypeMode, data, resources));
    }
    
    public void addTextPlainTests(AsyncChannel channel, AsyncTestClient<StringMessage> client, AsyncEndpointFactory<StringMessage> endpointFactory, ContentTypeMode contentTypeMode, Object... resources) {
        for (MessageTestData data : messageTestData) {
            addTextPlainTest(channel, client, endpointFactory, contentTypeMode, data, resources);
        }
    }
    
    public void addBinaryTest(AsyncChannel channel, AsyncTestClient<ByteArrayMessage> client, AsyncEndpointFactory<ByteArrayMessage> endpointFactory, ContentTypeMode contentTypeMode, Object... resources) {
        addTest(new BinaryTestCase(channel, client, endpointFactory, contentTypeMode, resources));
    }

    public void addRESTTests(AsyncChannel channel, AsyncTestClient<RESTMessage> client, AsyncEndpointFactory<AxisMessage> endpointFactory, Object... resources) {
        addTest(new RESTTestCase(channel, client, endpointFactory, restTestMessage1, resources));
        // TODO: regression test for SYNAPSE-431
//        addTest(new RESTTestCase(env, channel, client, endpointFactory, restTestMessage2));
    }

    @Override
    public void addTest(Test test) {
        if (test instanceof TransportTestCase) {
            TransportTestCase ttest = (TransportTestCase)test;
            Map<String,String> map = ttest.getNameComponents();
            for (FilterExpression exclude : excludes) {
                if (exclude.matches(map)) {
                    return;
                }
            }
        }
        super.addTest(test);
    }

/*
    @Override
    public void run(TestResult result) {
        if (!reuseServer) {
            super.run(result);
        } else {
            LinkedList<Test> tests = new LinkedList<Test>();
            for (Enumeration<?> e = tests(); e.hasMoreElements(); ) {
                tests.add((Test)e.nextElement());
            }
            while (!tests.isEmpty()) {
                if (result.shouldStop()) {
                    return;
                }
                Test test = tests.removeFirst();
                if (test instanceof AsyncMessageTestCase) {
                    AsyncMessageTestCase<?,?> listenerTest = (AsyncMessageTestCase<?,?>)test;
                    Channel<?> channel = listenerTest.getChannel();
                    ListenerTestServer server;
                    try {
                        server = new ListenerTestServer();
                        server.start(channel);
                    } catch (Throwable t) {
                        result.addError(this, t);
                        return;
                    }
                    listenerTest.setServer(server);
                    runTest(test, result);
                    for (Iterator<Test> it = tests.iterator(); it.hasNext(); ) {
                        if (result.shouldStop()) {
                            return;
                        }
                        test = it.next();
                        if (test instanceof AsyncMessageTestCase) {
                            listenerTest = (AsyncMessageTestCase<?,?>)test;
                            if (listenerTest.getChannel() == channel) {
                                it.remove();
                                listenerTest.setServer(server);
                                runTest(test, result);
                            }
                        }
                    }
                    try {
                        server.stop();
                    } catch (Throwable t) {
                        result.addError(this, t);
                        return;
                    }
                } else {
                    runTest(test, result);
                }
            }
        }
    }
*/
}
