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
package org.apache.synapse.config;

import javax.xml.namespace.QName;

import org.apache.synapse.SynapseContext;
import org.apache.synapse.config.Constants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.transform.FaultMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

/**
 *
 *         <p>
 *         <xmp><synapse:fault name="optional"/> </xmp>
 * 	TODO add the ability to configure the fault codes, etc
 * 
 */
public class FaultMediatorFactory extends AbstractMediatorFactory {

    private static final QName HEADER_Q = new QName(Constants.SYNAPSE_NAMESPACE, "fault");

    private static final QName ATT_VERSION_Q = new QName(Constants.NULL_NAMESPACE, "version");
    private static final QName CODE_Q        = new QName(Constants.SYNAPSE_NAMESPACE, "code");
    private static final QName REASON_Q      = new QName(Constants.SYNAPSE_NAMESPACE, "reason");

    private static final QName ATT_VALUE_Q = new QName(Constants.NULL_NAMESPACE, "name");
    private static final QName ATT_EXPR_Q  = new QName(Constants.NULL_NAMESPACE, "expression");

    private static final String SOAP11 = "soap11";
    private static final String SOAP12 = "soap12";

    public Mediator createMediator(SynapseContext synMsg, OMElement elem) {

        FaultMediator faultMediator = new FaultMediator();

        OMAttribute version = elem.getAttribute(ATT_VERSION_Q);
        if (version != null) {
            if (SOAP11.equals(version.getAttributeValue())) {
                faultMediator.setSoapVersion(FaultMediator.SOAP11);
            } else if (SOAP12.equals(version.getAttributeValue())) {
                faultMediator.setSoapVersion(FaultMediator.SOAP12);
            }
        }

/*      TODO revisit later!
        OMElement code = elem.getFirstChildWithName(CODE_Q);
        if (code != null) {
            OMAttribute value = code.getAttribute(ATT_VALUE_Q);
            OMAttribute expression = code.getAttribute(ATT_EXPR_Q);

            if (value != null) {
                faultMediator.setCode(new QName(value.getAttributeValue()));
            } else if (expression != null) {
                //faultMediator.setCode();
            } else {
                //TODO throw exception
            }

        } else {
            //TODO exception
        }
*/

        OMElement reason = elem.getFirstChildWithName(REASON_Q);
        if (reason != null) {
            faultMediator.setReason(reason.getText());
        }else {
            //TODO exception
        }

        return faultMediator;
    }

    public QName getTagQName() {
        return HEADER_Q;
    }

}
