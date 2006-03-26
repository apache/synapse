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

package org.apache.synapse.xml;

import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.processors.RefDefineProcessor;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

import javax.xml.namespace.QName;

/*
 */
public class RefDefineProcessorConfigurator extends AbstractProcessorConfigurator{
    private static final QName REF_DEFINE_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"refdefine");

    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        RefDefineProcessor rdp = new RefDefineProcessor();
        super.setNameOnProcessor(se, el, rdp);
		OMAttribute attr = el.getAttribute(new QName("ref"));
		if (attr==null) throw new SynapseException("<ref> must have attribute ref");
		rdp.setRefDefine(attr.getAttributeValue());
		return rdp;
    }

    public QName getTagQName() {
        return REF_DEFINE_Q;
    }
}
