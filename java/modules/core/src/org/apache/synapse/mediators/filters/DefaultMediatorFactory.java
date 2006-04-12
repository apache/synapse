package org.apache.synapse.mediators.filters;

import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.filters.DefaultMediator;
import org.apache.synapse.xml.AbstractListMediatorFactory;
import org.apache.synapse.xml.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;
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

public class DefaultMediatorFactory
        extends AbstractListMediatorFactory
        {

    private static final QName DEFAULT_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"default");
    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        DefaultMediator dm = new DefaultMediator();
        super.addChildrenAndSetName(se,el,dm);
        return dm;  
    }

    public QName getTagQName() {
        return DEFAULT_Q;
    }
}
