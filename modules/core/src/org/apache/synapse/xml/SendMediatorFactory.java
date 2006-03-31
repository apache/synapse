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

import javax.xml.namespace.QName;


import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.SynapseEnvironment;
import org.apache.axiom.om.OMElement;

public class SendMediatorFactory extends AbstractMediatorFactory {


    private static final QName SEND_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            "send");



    public Mediator createMediator(SynapseEnvironment se, OMElement el) {
        SendMediator sm =  new SendMediator();
        super.setNameOnMediator(se, el,sm);
        return sm;

    }

    public QName getTagQName() {

        return SEND_Q;
    }

}
