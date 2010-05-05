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

package org.apache.synapse.eventing.builders;

import junit.framework.TestCase;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.eventing.SynapseSubscription;
import org.apache.synapse.util.UUIDGenerator;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axiom.om.OMAbstractFactory;
import org.wso2.eventing.EventingConstants;

import java.util.Calendar;
import java.util.Date;

public class MessageBuilderTest extends TestCase {

    public void testSubscriptionMessageBuilderScenarioOne() {
        String subManUrl = "http://synapse.test.com/eventing/subscriptions";
        String addressUrl = "http://www.other.example.com/OnStormWarning";
        String filterDialect = "http://www.example.org/topicFilter";
        String filter = "weather.storms";
        Date date = new Date(System.currentTimeMillis() + 3600000);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        String message =
                "<wse:Subscribe xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" " +
                "   xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
                "   xmlns:ew=\"http://www.example.com/warnings\">\n" +
                "   <wse:EndTo>\n" +
                "       <wsa:Address>http://www.example.com/MyEventSink</wsa:Address>\n" +
                "         <wsa:ReferenceProperties>\n" +
                "             <ew:MySubscription>2597</ew:MySubscription>\n" +
                "         </wsa:ReferenceProperties>\n" +
                "   </wse:EndTo>\n" +
                "   <wse:Delivery>\n" +
                "       <wse:NotifyTo>\n" +
                "         <wsa:Address>" + addressUrl + "</wsa:Address>\n" +
                "         <wsa:ReferenceProperties>\n" +
                "             <ew:MySubscription>2597</ew:MySubscription>\n" +
                "         </wsa:ReferenceProperties>\n" +
                "       </wse:NotifyTo>\n" +
                "    </wse:Delivery>\n" +
                "    <wse:Expires>" + ConverterUtil.convertToString(cal) + "</wse:Expires>\n" +
                "    <wse:Filter xmlns:ow=\"http://www.example.org/oceanwatch\"\n" +
                "              Dialect=\"" + filterDialect + "\" >" + filter +"</wse:Filter>\n" +
                "</wse:Subscribe>";

        try {
            MessageContext msgCtx = TestUtils.getAxis2MessageContext(message, null).
                    getAxis2MessageContext();
            msgCtx.setTo(new EndpointReference(subManUrl));

            SynapseSubscription sub = SubscriptionMessageBuilder.createSubscription(msgCtx);
            assertEquals(subManUrl, sub.getSubManUrl());
            assertEquals(addressUrl, sub.getAddressUrl());
            assertEquals(addressUrl, sub.getEndpointUrl());
            assertEquals(filterDialect, sub.getFilterDialect());
            assertEquals(filter, sub.getFilterValue());
            assertEquals(date, sub.getExpires().getTime());
        } catch (Exception e) {
            fail("Error while constructing the sample subscription request: " + e.getMessage());
        }
    }

    public void testSubscriptionMessageBuilderScenarioTwo() {
        String addressUrl = "http://synapse.test.com/eventing/subscriptions";
        String id = UUIDGenerator.getUUID();

        String message = "<wse:Unsubscribe xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"/>";
        try {
            MessageContext msgCtx = TestUtils.getAxis2MessageContext(message, null).
                        getAxis2MessageContext();
            msgCtx.setTo(new EndpointReference(addressUrl));
            SOAPEnvelope env = msgCtx.getEnvelope();
            SOAPHeaderBlock header = env.getHeader().addHeaderBlock(
                    EventingConstants.WSE_EN_IDENTIFIER,
                    OMAbstractFactory.getSOAP11Factory().
                            createOMNamespace(EventingConstants.WSE_EVENTING_NS, "wse"));
            header.setText(id);

            SynapseSubscription sub = SubscriptionMessageBuilder.createUnSubscribeMessage(msgCtx);
            assertEquals(id, sub.getId());
            assertEquals(addressUrl, sub.getAddressUrl());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error while constructing the sample subscription request: " + e.getMessage());
        }
    }


}
