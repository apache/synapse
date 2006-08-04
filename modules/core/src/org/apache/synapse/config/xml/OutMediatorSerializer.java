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
import org.apache.synapse.mediators.filters.OutMediator;
import org.apache.synapse.SynapseException;

public class OutMediatorSerializer extends AbstractListMediatorSerializer
    implements MediatorSerializer {

    private static final Log log = LogFactory.getLog(OutMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof OutMediator)) {
            handleException("Unsupported mediator passed out for serialization : " + m.getType());
        }

        OutMediator mediator = (OutMediator) m;
        OMElement out = fac.createOMElement("out", synNS);
        super.serializeChildren(out, mediator.getList());

        if (parent != null) {
            parent.addChild(out);
        }
        return out;
    }

    public String getMediatorClassName() {
        return OutMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
