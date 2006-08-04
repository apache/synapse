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
package org.apache.synapse.mediators.json;

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.config.xml.MediatorSerializer;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;

import javax.xml.stream.XMLStreamConstants;

/**
 * <x:json/> mediator belongs to the http://ws.apache.org/ns/synapse/json namespace.
 * <p/>
 * <x:json (direction="JTX"|"XTJ)"/>
 */
public class JsonMediatorSerializer extends AbstractMediatorSerializer
    implements MediatorSerializer {

    private static final OMNamespace jsonNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE+"/json", "json");

    private static final Log log = LogFactory.getLog(JsonMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof JsonMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        JsonMediator mediator = (JsonMediator) m;
        OMElement json = fac.createOMElement("json", jsonNS);

        if (mediator.getDirection() != null) {
            json.addAttribute(fac.createOMAttribute(
                "direction", nullNS, mediator.getDirection()));
        } else {
            handleException("Invalid mediator. Direction is required");
        }

        if (parent != null) {
            parent.addChild(json);
        }
        return json;
    }

    public String getMediatorClassName() {
        return JsonMediatorSerializer.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
