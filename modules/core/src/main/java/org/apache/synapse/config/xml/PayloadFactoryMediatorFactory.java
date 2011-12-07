/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.*;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.transform.PayloadFactoryMediator;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Creates a payloadFactory mediator from the provided XML configuration
 * <p/>
 * <pre>
 * &lt;payloadFactory&gt;
 *   &lt;format&gt;&quot;xmlstring&quot;&lt;/format&gt;
 *   &lt;args&gt;
 *     &lt;arg (value=&quot;literal&quot; | expression=&quot;xpath&quot;)/&gt;*
 *   &lt;/args&gt;
 * &lt;/payloadFactory&gt;
 * </pre>
 */
public class PayloadFactoryMediatorFactory extends AbstractMediatorFactory {

    private static final QName PAYLOAD_FACTORY_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "payloadFactory");

    private static final QName FORMAT_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "format");
    private static final QName ARGS_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "args");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();

        OMElement formatElem = elem.getFirstChildWithName(FORMAT_Q);

        if (formatElem != null) {
            OMElement copy = formatElem.getFirstElement().cloneOMElement();
            removeIndentations(copy);
            payloadFactoryMediator.setFormat(copy.toString());
        } else {
            handleException("format element of payloadFactoryMediator is required");
        }

        OMElement argumentsElem = elem.getFirstChildWithName(ARGS_Q);

        if (argumentsElem != null) {

            Iterator itr = argumentsElem.getChildElements();

            while (itr.hasNext()) {
                OMElement argElem = (OMElement) itr.next();
                PayloadFactoryMediator.Argument arg = new PayloadFactoryMediator.Argument();
                String value;

                if ((value = argElem.getAttributeValue(ATT_VALUE)) != null) {
                    arg.setValue(value);
                } else if ((value = argElem.getAttributeValue(ATT_EXPRN)) != null) {

                    if (value.trim().length() == 0) {
                        handleException("Value of 'expression' attribute is required");
                    } else {
                        try {
                            arg.setExpression(SynapseXPathFactory.getSynapseXPath(argElem, ATT_EXPRN));
                        } catch (JaxenException e) {
                            handleException("Invalid XPath expression is provided for " +
                                    "'expression' attribute: " + value, e);
                        }
                    }

                } else {
                    handleException("Unsupported arg type. 'value' or 'expression' attribute is " +
                            "required");
                }

                payloadFactoryMediator.addArgument(arg);
            }
        }

        return payloadFactoryMediator;
    }

    public QName getTagQName() {
        return PAYLOAD_FACTORY_Q;
    }

    private void removeIndentations(OMElement element) {
        List<OMText> removables = new ArrayList<OMText>();
        removeIndentations(element, removables);
        for (OMText node : removables) {
            node.detach();
        }
    }

    private void removeIndentations(OMElement element, List<OMText> removables) {
        Iterator children = element.getChildren();
        while (children.hasNext()) {
            Object next = children.next();
            if (next instanceof OMText) {
                OMText text = (OMText) next;
                if (text.getText().trim().equals("")) {
                    removables.add(text);
                }
            } else if (next instanceof OMElement) {
                removeIndentations((OMElement) next, removables);
            }
        }
    }

}
