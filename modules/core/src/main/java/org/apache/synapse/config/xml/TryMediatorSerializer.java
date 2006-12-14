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

/**
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
public class TryMediatorSerializer extends AbstractListMediatorSerializer
     {

    private static final Log log = LogFactory.getLog(TryMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof TryMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        TryMediator mediator = (TryMediator) m;
        OMElement tryMed = fac.createOMElement("try", synNS);
        finalizeSerialization(tryMed,mediator);

        OMElement sequence = fac.createOMElement("sequence", synNS);
        super.serializeChildren(sequence, mediator.getList());
        tryMed.addChild(sequence);

        OMElement onError = fac.createOMElement("onError", synNS);
        super.serializeChildren(onError, mediator.getErrorHandlerMediators());
        tryMed.addChild(onError);

        OMElement finallyMed = fac.createOMElement("finally", synNS);
        super.serializeChildren(finallyMed, mediator.getFinallyMediators());
        tryMed.addChild(finallyMed);

        if (parent != null) {
            parent.addChild(tryMed);
        }
        return tryMed;
    }

    public String getMediatorClassName() {
        return TryMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
