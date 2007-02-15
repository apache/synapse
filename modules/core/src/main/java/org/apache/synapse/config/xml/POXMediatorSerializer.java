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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.builtin.POXMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This serialize a pox mediator instance
 * <p/>
 * <pre>
 * &lt;pox value="true|false"/&gt;
 * </pre>
 */

public class POXMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(POXMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {
        if (!(m instanceof POXMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        POXMediator poxMediator = (POXMediator) m;
        OMElement pox = fac.createOMElement("pox", synNS);
        boolean restEnable = poxMediator.getValue();
        pox.addAttribute(fac.createOMAttribute(
                "value", nullNS, Boolean.toString(restEnable)));

        if (parent != null) {
            parent.addChild(pox);
        }
        return pox;
    }

    public String getMediatorClassName() {
        return POXMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
