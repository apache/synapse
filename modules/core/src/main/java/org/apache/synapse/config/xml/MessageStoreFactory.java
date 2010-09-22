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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.store.InMemoryMessageStore;
import org.apache.synapse.message.store.MessageStore;
import org.apache.synapse.message.store.RedeliveryProcessor;
import org.apache.axis2.util.JavaUtils;


import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

/**
 * Create an instance of the given Message Store, and sets properties on it.
 * <p/>
 * &lt;messageStore name="string" class="classname" [sequence = "string" ] &gt;
 * &lt;redelivery&gt;
 * &lt;interval&gt;delay in seconds &lt;/interval&gt;
 * &lt;maximumRedeliveries&gt;maximum_number_of_redeliveries_to attempt  &lt;/maximumRedeliveries&gt;
 * &lt;/redelivery&gt;
 * &lt;/messageStore&gt;
 */
public class MessageStoreFactory {

    private static final Log log = LogFactory.getLog(MessageStoreFactory.class);

    public static final QName CLASS_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "class");
    public static final QName NAME_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");
    public static final QName SEQUENCE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "sequence");

    private static final QName REDELIVERY_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "redelivery");
    private static final QName DELAY_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "interval");
    private static final QName MAX_REDELIVERIES = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "maximumRedeliveries");
    private static final QName ENABLE_EXPONENTIAL_BACKOFF = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "exponentialBackoff");
    private static final QName BACKOFF_MULTIPLIER = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "backoffMutiplier");
    public static final QName PARAMETER_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
            "parameter");

    @SuppressWarnings({"UnusedDeclaration"})
    public static MessageStore createMessageStore(OMElement elem, Properties properties) {

        OMAttribute clss = elem.getAttribute(CLASS_Q);
        MessageStore messageStore;
        if (clss != null) {
            try {
                Class cls = Class.forName(clss.getAttributeValue());
                messageStore = (MessageStore) cls.newInstance();
            } catch (Exception e) {
                handleException("Error while instantiating the message store", e);
                return null;
            }
        } else {
            messageStore = new InMemoryMessageStore();
        }

        OMAttribute nameAtt = elem.getAttribute(NAME_Q);
        if (nameAtt != null) {
            messageStore.setName(nameAtt.getAttributeValue());
        } else {
            handleException("Message Store name not specified");
        }

        OMAttribute sequenceAtt = elem.getAttribute(SEQUENCE_Q);
        if(sequenceAtt != null) {
            messageStore.setSequence(sequenceAtt.getAttributeValue());
        }

        OMElement redeliveryElem = elem.getFirstChildWithName(REDELIVERY_Q);

        if (redeliveryElem != null) {
            RedeliveryProcessor redeliveryProcessor = populateRedeliveryProcessor(redeliveryElem,
                    messageStore);
            messageStore.setRedeliveryProcessor(redeliveryProcessor);
        }

        messageStore.setParameters(getParameters(elem));
        return messageStore;
    }

    private static RedeliveryProcessor populateRedeliveryProcessor(OMElement element,
                                                                   MessageStore messageStore) {

        RedeliveryProcessor redeliveryProcessor = new RedeliveryProcessor(messageStore);

        OMElement intervalElm = element.getFirstChildWithName(DELAY_Q);
        if (intervalElm != null) {
            int delay = 1000 * Integer.parseInt(intervalElm.getText());
            redeliveryProcessor.setRedeliveryDelay(delay);
        }

        OMElement maxRedeliveryElm = element.getFirstChildWithName(MAX_REDELIVERIES);

        if (maxRedeliveryElm != null) {
            int maxRedeliveries = Integer.parseInt(maxRedeliveryElm.getText());
            redeliveryProcessor.setMaxRedeleveries(maxRedeliveries);
        }

        OMElement expBOElm = element.getFirstChildWithName(ENABLE_EXPONENTIAL_BACKOFF);

        if (expBOElm != null) {
            if (JavaUtils.isTrueExplicitly(expBOElm.getText())) {
                redeliveryProcessor.setExponentialBackoff(true);
                OMElement multiplierElm = element.getFirstChildWithName(BACKOFF_MULTIPLIER);
                if (multiplierElm != null) {
                    int mulp = Integer.parseInt(multiplierElm.getText());
                    redeliveryProcessor.setBackOffMultiplier(mulp);
                }
            }
        }

        return redeliveryProcessor;
    }

    private static Map<String,Object> getParameters(OMElement elem) {
        Iterator params = elem.getChildrenWithName(PARAMETER_Q);
        Map<String, Object> parameters = new HashMap<String, Object>();

        while (params.hasNext()) {
            Object o = params.next();
            if (o instanceof OMElement) {
                OMElement prop = (OMElement) o;
                OMAttribute paramName = prop.getAttribute(NAME_Q);
                String paramValue = prop.getText();
                if (paramName != null) {
                    if (paramValue != null) {
                        parameters.put(paramName.getAttributeValue(), paramValue);
                    }
                } else {
                    handleException("Invalid MessageStore parameter - Parameter must have a name ");
                }
            }
        }
        return parameters ;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
