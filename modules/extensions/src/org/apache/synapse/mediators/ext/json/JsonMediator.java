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

package org.apache.synapse.mediators.ext.json;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.json.XML;
import org.apache.synapse.json.JSONException;
import org.apache.synapse.json.JSONObject;
import org.apache.synapse.json.SynapseJsonSender;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis2.description.TransportOutDescription;

import javax.xml.namespace.QName;

public class JsonMediator implements Mediator {

    private static final Log log = LogFactory.getLog(JsonMediator.class);

    private String direction;

    private static final String JTX = "JTX";
    private static final String XJT = "XTJ";

    public boolean mediate(MessageContext synMsgCtx) {

        if (direction.equalsIgnoreCase(JTX)) {
            // Json_To_Xml

        } else if (direction.equalsIgnoreCase(XJT)) {
            // Xml_To_Json
            org.apache.axis2.context.MessageContext mc =
                    ((Axis2MessageContext) synMsgCtx).getAxis2MessageContext();
            JSONObject xmlToJSonObj = null;
            try {
                xmlToJSonObj =
                        XML.toJSONObject(synMsgCtx.getEnvelope().toString());
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


        return true;  //To change body of implemented methods use File | Settings | File Templates.
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
