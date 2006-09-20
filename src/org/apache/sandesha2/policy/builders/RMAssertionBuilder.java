/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
package org.apache.sandesha2.policy.builders;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.xml.XmlPrimtiveAssertion;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.util.SandeshaPropertyBean;

public class RMAssertionBuilder implements AssertionBuilder {

    public Assertion build(OMElement element, AssertionBuilderFactory factory)
            throws IllegalArgumentException {

        SandeshaPropertyBean propertyBean = new SandeshaPropertyBean();
        Policy policy = PolicyEngine.getPolicy(element.getFirstElement());

        processElements(policy.getPolicyComponents(), propertyBean);
        return propertyBean;
    }

    public QName[] getKnownElements() {
        return new QName[] { new QName(
                Sandesha2Constants.Assertions.URI_RM_POLICY_NS, "RMAssertion") };
    }

    private void processElements(List policyComponents,
            SandeshaPropertyBean propertyBean) {

        XmlPrimtiveAssertion xmlPrimtiveAssertion;

        for (Iterator iterator = policyComponents.iterator(); iterator
                .hasNext();) {
            xmlPrimtiveAssertion = (XmlPrimtiveAssertion) iterator.next();
            OMElement element = xmlPrimtiveAssertion.getValue();
            String name = element.getLocalName();

            if (Sandesha2Constants.Assertions.ELEM_ACK_INTERVAL.equals(name)) {
                propertyBean.setAcknowledgementInterval(Long.parseLong(element
                        .getText().trim()));

            } else if (Sandesha2Constants.Assertions.ELEM_EXP_BACKOFF
                    .equals(name)) {
                propertyBean.setExponentialBackoff(Boolean.valueOf(
                        element.getText().trim()).booleanValue());

            } else if (Sandesha2Constants.Assertions.ELEM_INACTIVITY_TIMEOUT
                    .equals(name)) {
                propertyBean.setInactiveTimeoutInterval(Long.parseLong(element
                        .getText().trim()));

            } else if (Sandesha2Constants.Assertions.ELEM_INACTIVITY_TIMEOUT_MEASURES
                    .equals(name)) {
                propertyBean
                        .setInactiveTimeoutInterval(propertyBean
                                .getInactiveTimeoutInterval(), element
                                .getText().trim());
            }
        }
    }

}
