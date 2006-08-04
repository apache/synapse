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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Endpoint;
import org.apache.synapse.mediators.builtin.SendMediator;

import java.util.Iterator;

/**
 * //TODO support endpoints, failover and loadbalacing
 *
 * The &lt;send&gt; element is used to send messages out of Synapse to some endpoint. In the simplest case,
 * the place to send the message to is implicit in the message (via a property of the message itself)-
 * that is indicated by the following
 * <pre>
 *  &lt;send/&gt;
 * </pre>
 *
 * If the message is to be sent to one or more endpoints, then the following is used:
 * <pre>
 *  &lt;send&gt;
 *   (endpointref | endpoint)+
 *  &lt;/send&gt;
 * </pre>
 * where the endpointref token refers to the following:
 * <pre>
 * &lt;endpoint ref="name"/&gt;
 * </pre>
 * and the endpoint token refers to an anonymous endpoint defined inline:
 * <pre>
 *  &lt;endpoint address="url"/&gt;
 * </pre>
 * If the message is to be sent to an endpoint selected by load balancing across a set of endpoints,
 * then it is indicated by the following:
 * <pre>
 * &lt;send&gt;
 *   &lt;load-balance algorithm="uri"&gt;
 *     (endpointref | endpoint)+
 *   &lt;/load-balance&gt;
 * &lt;/send&gt;
 * </pre>
 * Similarly, if the message is to be sent to an endpoint with failover semantics, then it is indicated by the following:
 * <pre>
 * &lt;send&gt;
 *   &lt;failover&gt;
 *     (endpointref | endpoint)+
 *   &lt;/failover&gt;
 * &lt;/send&gt;
 * </pre>
 */
public class SendMediatorSerializer extends AbstractMediatorSerializer
    implements MediatorSerializer {

    private static final Log log = LogFactory.getLog(SendMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof SendMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        SendMediator mediator = (SendMediator) m;
        OMElement send = fac.createOMElement("send", synNS);

        if (mediator.getEndpoints() != null) {
            Iterator iter = mediator.getEndpoints().iterator();
            while (iter.hasNext()) {
                Endpoint endpt = (Endpoint) iter.next();
                EndpointSerializer.serializeEndpoint(endpt, send);
            }
        }

        if (parent != null) {
            parent.addChild(send);
        }
        return send;
    }

    public String getMediatorClassName() {
        return SendMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
