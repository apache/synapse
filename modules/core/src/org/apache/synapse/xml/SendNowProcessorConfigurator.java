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
import org.apache.synapse.processors.builtin.SendNowProcessor;
import org.apache.axiom.om.OMElement;

import javax.xml.namespace.QName;

/**
 */
public class SendNowProcessorConfigurator extends AbstractProcessorConfigurator{
    private static final QName SEND_NOW_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"sendnow");
    public Processor createProcessor(SynapseEnvironment se, OMElement el) {
        return new SendNowProcessor();
    }

    public QName getTagQName() {
        return SEND_NOW_Q;
    }
}
