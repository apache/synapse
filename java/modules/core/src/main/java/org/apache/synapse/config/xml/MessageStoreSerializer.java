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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.store.MessageStore;

import java.util.Iterator;

/**
 * Serialize an instance of the given Message Store, and sets properties on it.
 * <p/>
 * &lt;messageStore name="string" class="classname" [sequence = "string" ] &gt;
 * &lt;redelivery&gt;
 * &lt;interval&gt;delay in seconds &lt;/interval&gt;
 * &lt;maximumRedeliveries&gt;maximum_number_of_redeliveries_to attempt  &lt;/maximumRedeliveries&gt;
 * &lt;/redelivery&gt;
 * &lt;/messageStore&gt;
 */
public class MessageStoreSerializer {

    private static final Log log = LogFactory.getLog(MessageStoreSerializer.class);

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = SynapseConstants.SYNAPSE_OMNAMESPACE;
    protected static final OMNamespace nullNS = fac.createOMNamespace(
            XMLConfigConstants.NULL_NAMESPACE, "");

    public static OMElement serializeMessageStore(OMElement parent, MessageStore messageStore) {

        OMElement store = fac.createOMElement(XMLConfigConstants.MESSAGE_STORE_ELT);

        if (messageStore.getProviderClass() != null) {
            store.addAttribute(fac.createOMAttribute("class", nullNS,
                    messageStore.getProviderClass()));
        } else {
            handleException("Invalid MessageStore. Provider is required");
        }

        if (messageStore.getSequence() != null) {
            store.addAttribute(fac.createOMAttribute("sequence", nullNS,
                    messageStore.getSequence()));
        }

        if (messageStore.getName() != null) {
            store.addAttribute(fac.createOMAttribute("name", nullNS,
                    messageStore.getSequence()));
        } else {
            handleException("Message store Name not specified");
        }

        //Redelivery processor
        OMElement redilevery = fac.createOMElement("redelivery", synNS);
        int reDeliveryDelay = messageStore.getRedeliveryProcessor().getRedeliveryDelay()/1000;

        OMElement delay = fac.createOMElement("interval", synNS);
        delay.setText(String.valueOf(reDeliveryDelay));

        redilevery.addChild(delay);

        int maxRedeliveries = messageStore.getRedeliveryProcessor().getMaxRedeleveries();
        OMElement maxRedeliveryElm = fac.createOMElement("maximumRedeliveries", synNS);
        maxRedeliveryElm.setText(String.valueOf(maxRedeliveries));

        redilevery.addChild(maxRedeliveryElm);

        if (messageStore.getRedeliveryProcessor().isExponentialBackoffEnable()) {
             OMElement expBOElm = fac.createOMElement("exponentialBackoff", synNS);
             expBOElm.setText("true");
             redilevery.addChild(expBOElm);

             OMElement multiplierElm = fac.createOMElement("backoffMutiplier", synNS);
             int multiplier = messageStore.getRedeliveryProcessor().getBackOffMultiplier();
             if (multiplier > 0) {
                 multiplierElm.setText(String.valueOf(multiplier));
                 redilevery.addChild(multiplierElm);
             }
        }

        store.addChild(redilevery);

        if (messageStore.getParameters() != null) {
            Iterator iter = messageStore.getParameters().keySet().iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                String value = (String) messageStore.getParameters().get(name);
                OMElement property = fac.createOMElement("parameter", synNS);
                property.addAttribute(fac.createOMAttribute(
                        "name", nullNS, name));
                property.setText(value.trim());
                store.addChild(property);
            }
        }

        if (parent != null) {
            parent.addChild(store);
        }
        return store;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
