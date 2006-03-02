package org.apache.synapse.processors.conditions;

import org.apache.synapse.xml.AbstractListProcessorConfigurator;
import org.apache.synapse.xml.Constants;
import org.apache.synapse.Processor;
import org.apache.synapse.SynapseEnvironment;
import org.apache.axis2.om.OMElement;

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

public class DefaultProcessorConfigurator
        extends AbstractListProcessorConfigurator {

    private static final QName DEFAULT_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"default");
    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        DefaultProcessor dp = new DefaultProcessor();
        super.addChildrenAndSetName(se,el,dp);
        return dp;  
    }

    public QName getTagQName() {
        return DEFAULT_Q;
    }
}
