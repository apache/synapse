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
package org.apache.synapse.config;

import java.util.Iterator;



import org.apache.synapse.SynapseContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.ListMediator;
import org.apache.synapse.api.Mediator;
import org.apache.axiom.om.OMElement;

/**
 *
 * 
 * <p> This is the abstract parent of any tag which is a "Node" - so &ltstage>, &ltin>&ltout> and &ltnever> all fit this model
 * <p>It recursively creates a list of processors from the children. 
 *
 */
public abstract class AbstractListMediatorFactory extends AbstractMediatorFactory {

    public void addChildren(SynapseContext synCtx, OMElement el, ListMediator m)
    {
        Iterator it = el.getChildElements();
        while (it.hasNext()) {
            OMElement child = (OMElement) it.next();
            Mediator med = MediatorFactoryFinder.getInstance().getMediator(synCtx, child);
            if (med != null) {
                m.addChild(med);
            } else {
                String msg = "Unknown mediator : " + child.getLocalName();
                log.error(msg);
                throw new SynapseException(msg);
            }
        }
    }



}
