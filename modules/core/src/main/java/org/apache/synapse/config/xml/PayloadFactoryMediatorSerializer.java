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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.transform.PayloadFactoryMediator;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * Serializer for {@link PayloadFactoryMediator} instances.
 *
 * @see PayloadFactoryMediatorFactory
 */
public class PayloadFactoryMediatorSerializer extends AbstractMediatorSerializer {

    private static final String PAYLOAD_FACTORY = "payloadFactory";
    private static final String FORMAT = "format";
    private static final String ARGS = "args";
    private static final String ARG = "arg";
    private static final String VALUE = "value";
    private static final String EXPRESSION = "expression";


    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof PayloadFactoryMediator)) {
            handleException("Unsupported mediator was passed in for serialization: " + m.getType());
            return null;
        }

        PayloadFactoryMediator mediator = (PayloadFactoryMediator) m;

        OMElement payloadFactoryElem = fac.createOMElement(PAYLOAD_FACTORY, synNS);
        saveTracingState(payloadFactoryElem, mediator);

        if (mediator.getFormat() != null) {

            try {
                OMElement formatElem = fac.createOMElement(FORMAT, synNS);
                formatElem.addChild(AXIOMUtil.stringToOM(mediator.getFormat()));
                payloadFactoryElem.addChild(formatElem);
            } catch (XMLStreamException e) {
                handleException("Error while serializing payloadFactory mediator", e);
            }
        } else {
            handleException("Invalid payloadFactory mediator, format is required");
        }

        List<PayloadFactoryMediator.Argument> argList = mediator.getArgumentList();

        if (argList != null && argList.size() > 0) {

            OMElement argumentsElem = fac.createOMElement(ARGS, synNS);

            for (PayloadFactoryMediator.Argument arg : argList) {

                OMElement argElem = fac.createOMElement(ARG, synNS);

                if (arg.getValue() != null) {
                    argElem.addAttribute(fac.createOMAttribute(VALUE, nullNS, arg.getValue()));
                } else if (arg.getExpression() != null) {
                    SynapseXPathSerializer.serializeXPath(arg.getExpression(), argElem, EXPRESSION);
                }
                argumentsElem.addChild(argElem);
            }
            payloadFactoryElem.addChild(argumentsElem);
        }

        return payloadFactoryElem;
    }

    public String getMediatorClassName() {
        return PayloadFactoryMediator.class.getName();
    }

}
