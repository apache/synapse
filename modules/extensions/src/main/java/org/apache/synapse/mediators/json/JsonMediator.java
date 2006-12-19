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

package org.apache.synapse.mediators.json;

import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Mediator;
import org.apache.synapse.Constants;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import javax.xml.namespace.QName;

public class JsonMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(JsonMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    private String direction;

    private static final String JTX = "JTX";
    private static final String XJT = "XTJ";

    public boolean mediate(MessageContext synMsgCtx) {
        boolean shouldTrace = shouldTrace(synMsgCtx.getTracingState());
        if(shouldTrace) {
            trace.trace("Start : Json mediator ");
        }
        if (direction.equalsIgnoreCase(JTX)) {
            // Json_To_Xml

        } else if (direction.equalsIgnoreCase(XJT)) {
            // Xml_To_Json
            org.apache.axis2.context.MessageContext mc =
                    ((Axis2MessageContext) synMsgCtx).getAxis2MessageContext();
            String xmlEnvelope =synMsgCtx.getEnvelope().toString();
            if (shouldTrace) {
                trace.trace("XML Envelope : " + xmlEnvelope);
            }
            JSONObject xmlToJSonObj = null;
            try {
                xmlToJSonObj =
                        XML.toJSONObject(xmlEnvelope);
                if (shouldTrace) {
                    trace.trace("Json Object  : " + xmlToJSonObj.toString());
                }
            } catch (JSONException e) {
                log.error(e);
                handleException(
                        "JSON Encounterd an Error. Please see the logs");
            }

            mc.setProperty("JSONObject", xmlToJSonObj);

            TransportOutDescription outDescription =
                    new TransportOutDescription(new QName("http"));
            SynapseJsonSender jsonSender = new SynapseJsonSender();
            outDescription.setSender(jsonSender);

            mc.setTransportOut(outDescription);

            /*
            Underline SOAP engine should be notified if the MessageContext Contain
            Pure XML with a String as body.
            */
            mc.setDoingREST(true);

        } else {
            handleException(
                    "'direction' contain a signal other than JTX or XJT");
        }

        if(shouldTrace) {
            trace.trace("End : Json mediator ");
        }
        return true;
    }

    public String getType() {
        return JsonMediator.class.getName();
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
