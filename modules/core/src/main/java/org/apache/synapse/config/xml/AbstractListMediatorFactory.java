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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.ListMediator;
import org.apache.synapse.Mediator;

import java.util.Iterator;

/**
 * This implements the basic logic to build a list mediator from a given XML
 * configuration. It recursively builds the child mediators of the list.
 */
public abstract class AbstractListMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(AbstractListMediatorFactory.class);

    protected static void addChildren(OMElement el, ListMediator m) {
        Iterator it = el.getChildElements();
        while (it.hasNext()) {
            OMElement child = (OMElement) it.next();
            Mediator med = MediatorFactoryFinder.getInstance().getMediator(child);
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
