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
package org.apache.synapse.mediators.bsf;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.config.xml.MediatorFactory;

/**
 * Creates an instance of a Script mediator. <p/>
 * 
 * <pre>
 *  &lt;script key=&quot;property-key&quot; &lt;script/&gt;
 * </pre>
 * 
 */
public class ScriptMediatorFactory implements MediatorFactory {

    private static final QName TAG_NAME = new QName(Constants.SYNAPSE_NAMESPACE, "script");

    public Mediator createMediator(OMElement elem) {

        OMAttribute scriptKey = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (scriptKey == null) {
            throw new SynapseException("must specify 'key' for script source");
        }
        ScriptMediator sm = new ScriptMediator(scriptKey.getAttributeValue());
        return sm;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}
