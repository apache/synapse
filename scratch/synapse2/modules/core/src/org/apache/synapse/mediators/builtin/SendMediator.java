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

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Constants;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Send mediator sends the message using the following semantics.
 * <p/>
 * This is a leaf mediator (i.e. further processing halts after this mediator completes)
 * <p/>
 * TODO support endpoints, loadbalancing and failover
 */
public class SendMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(SendMediator.class);
    /**
     * This is a leaf mediator. i.e. processing stops once send is invoked,
     * as it always returns false
     *
     * @param synMsg the current message to be sent
     * @return false always as this is a leaf mediator
     */
    public boolean mediate(SynapseMessage synMsg) {
        log.debug(getType() + " mediate()");
        log.debug("Sending To: " + (synMsg.getTo() != null ? synMsg.getTo().getAddress() : "null"));
        log.debug("Body : \n" + synMsg.getEnvelope());
        synMsg.getSynapseContext().getSynapseEnvironment().send(synMsg.getSynapseContext());
        return false;
    }
}
