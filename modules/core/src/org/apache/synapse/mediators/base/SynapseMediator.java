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

package org.apache.synapse.mediators.base;

import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The SynapseMediator is the "mainmediator" of the synapse engine. It is
 * given each message on arrival at the synapse engine. The synapse configuration
 * holds a reference to this special mediator instance. The SynapseMediator
 * holds the list of mediators supplied within the <rules> element of an XML
 * based Synapse configuration
 *
 * @see org.apache.synapse.config.SynapseConfiguration#getMainMediator()
 */
public class SynapseMediator extends AbstractListMediator {

    private static final Log log = LogFactory.getLog(SynapseMediator.class);

    /**
     * Perform the mediation specified by the rule set
     * @param synCtx the message context
     * @return as per standard mediate() semantics
     */
    public boolean mediate(MessageContext synCtx) {
        log.debug("Synapse main mediator :: mediate()");
        return super.mediate(synCtx);
    }
}
