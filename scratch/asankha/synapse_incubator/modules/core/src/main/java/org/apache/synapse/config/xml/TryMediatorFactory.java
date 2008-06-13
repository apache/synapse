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
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.base.TryMediator;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 *  Builds an instance of a Try mediator through the Synapse configuration. It follows the following
 *
 * <pre>
 * &lt;try&gt;
 *   &lt;sequence&gt;
 *      mediator+
 *   &lt;/sequence&gt;
 *   &lt;onError&gt;
 *      mediator+
 *   &lt;/onError&gt;
 *   &lt;finally&gt;
 *      mediator+
 *   &lt;/finally&gt;?
 * &lt;/try&gt;
 * </pre>
 */
public class TryMediatorFactory extends AbstractListMediatorFactory {

    private static final Log log = LogFactory.getLog(TryMediatorFactory.class);

    private static final QName TRY_Q = new QName(Constants.SYNAPSE_NAMESPACE, "try");

    public Mediator createMediator(OMElement elem) {
        
        TryMediator tryMediator = new TryMediator();

        // process sequence of the try mediator
        OMElement seq = elem.getFirstChildWithName(
            new QName(Constants.SYNAPSE_NAMESPACE, "sequence"));
        if (seq != null) {
            super.addChildren(seq, tryMediator);
        } else {
            handleException("A 'sequence' element is required for a 'try' mediator");
        }

        // process onError mediators
        OMElement error = elem.getFirstChildWithName(
            new QName(Constants.SYNAPSE_NAMESPACE, "onError"));
        if (error != null) {
            Iterator it = error.getChildElements();
            while (it.hasNext()) {
                OMElement child = (OMElement) it.next();
                Mediator med = MediatorFactoryFinder.getInstance().getMediator(child);
                if (med != null) {
                    tryMediator.getErrorHandlerMediators().add(med);
                } else {
                    handleException("Unknown mediator : " + child.getLocalName());
                }
            }
        } else {
            handleException("A 'onError' element is required for a 'try' mediator");
        }

        // process finally mediators - if any
        OMElement fin = elem.getFirstChildWithName(
            new QName(Constants.SYNAPSE_NAMESPACE, "finally"));
        if (fin != null) {
            Iterator it = fin.getChildElements();
            while (it.hasNext()) {
                OMElement child = (OMElement) it.next();
                Mediator med = MediatorFactoryFinder.getInstance().getMediator(child);
                if (med != null) {
                    tryMediator.getFinallyMediators().add(med);
                } else {
                    handleException("Unknown mediator : " + child.getLocalName());
                }
            }
        }
        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        initMediator(tryMediator,elem);

        return tryMediator;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return TRY_Q;
    }
}
