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
package org.apache.synapse.mediators.javascript;

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
 *  <javascript/>
 *     <![CDATA[ script here ]]>
 *  </javascript>
 */
public class JavaScriptMediatorSerializer extends AbstractMediatorSerializer
    implements MediatorSerializer {

    private static final OMNamespace jsNS = fac.createOMNamespace(Constants.SYNAPSE_NAMESPACE+"/js", "js");

    private static final Log log = LogFactory.getLog(JavaScriptMediatorSerializer.class);

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof JavaScriptMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        JavaScriptMediator mediator = (JavaScriptMediator) m;
        OMElement js = fac.createOMElement("javascript", jsNS);

        js.addChild(fac.createOMText(mediator.getScript(), XMLStreamConstants.CDATA));

        if (parent != null) {
            parent.addChild(js);
        }
        return js;
    }

    public String getMediatorClassName() {
        return JavaScriptMediator.class.getName();
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
