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

package org.apache.synapse.config.xml;

import javax.xml.namespace.QName;


import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.SynapseContext;
import org.apache.axiom.om.OMElement;

/**
 * The Send mediator factory parses a Send element and creates an instance of the mediator
 *
 * //TODO support endpoints, failover and loadbalacing
 *
 * The <send> element is used to send messages out of Synapse to some endpoint. In the simplest case,
 * the place to send the message to is implicit in the message (via a property of the message itself)-
 * that is indicated by the following
 *  <send/>
 *
 * If the message is to be sent to one or more endpoints, then the following is used:
 *  <send>
 *   (endpointref | endpoint)+
 *  </send>
 * where the endpointref token refers to the following:
 * <endpoint ref="name"/>
 * and the endpoint token refers to an anonymous endpoint defined inline:
 *  <endpoint address="url"/>
 * If the message is to be sent to an endpoint selected by load balancing across a set of endpoints,
 * then it is indicated by the following:
 * <send>
 *   <load-balance algorithm="uri">
 *     (endpointref | endpoint)+
 *   </load-balance>
 * </send>
 * Similarly, if the message is to be sent to an endpoint with failover semantics, then it is indicated by the following:
 * <send>
 *   <failover>
 *     (endpointref | endpoint)+
 *   </failover>
 * </send>
 */
public class SendMediatorFactory extends AbstractMediatorFactory {

    private static final QName SEND_Q = new QName(Constants.SYNAPSE_NAMESPACE, "send");

    public Mediator createMediator(SynapseContext synMsg, OMElement el) {
        SendMediator sm =  new SendMediator();
        return sm;
    }

    public QName getTagQName() {
        return SEND_Q;
    }

}
