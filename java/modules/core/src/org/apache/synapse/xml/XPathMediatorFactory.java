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

import java.util.Iterator;

import javax.xml.namespace.QName;


import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.mediators.filters.XPathMediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMNamespace;

/**
 *
 * 
 * <p>
 * This class executes a test and then processes all subsequent rules/mediations
 * if the test is true
 * 
 */
public class XPathMediatorFactory extends
        AbstractListMediatorFactory {
    private static final String XPATH = "xpath";

    private static final QName XPATH_Q = new QName(Constants.SYNAPSE_NAMESPACE,
            "xpath");

    private static final QName XPATH_EXPRESSION_ATT_Q = new QName("expr");

    /*
      * (non-Javadoc)
      *
      * @see org.apache.synapse.spi.Processor#compile(org.apache.synapse.api.SynapseEnvironment,
      *      org.apache.axis2.om.OMElement)
      */
    public Mediator  createMediator(SynapseEnvironment se, OMElement el) {
        XPathMediator xm = new XPathMediator();

        super.addChildrenAndSetName(se, el, xm);

        OMAttribute expr = el.getAttribute(XPATH_EXPRESSION_ATT_Q);
        if (expr == null) {
            throw new SynapseException(XPATH + " must have "
                    + XPATH_EXPRESSION_ATT_Q + " attribute: " + el.toString());
        }

        xm.setXPathExpr(expr.getAttributeValue());
        Iterator it = el.getAllDeclaredNamespaces();
        while (it.hasNext()) {
            OMNamespace n = (OMNamespace) it.next();
            xm.addXPathNamespace(n.getPrefix(), n.getName());
        }

        return xm;
    }

    public QName getTagQName() {

        return XPATH_Q;
    }

}
