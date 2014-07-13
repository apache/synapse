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
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.template.InvokeMediator;

import java.util.Map;

/**
 * Serialize a Invoke mediator to a configuration given below
 * <invoke target="">
 * <parameter name="p1" value="{expr}" />
 * <parameter name="p1" value="{{expr}}" />
 * <parameter name="p1" value="v2" />
 * ...
 * ..
 * </invoke>
 */
public class InvokeMediatorSerializer extends AbstractMediatorSerializer {
    public static final String INVOKE_N = "call-template";

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (!(m instanceof InvokeMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
            return null;
        }
        InvokeMediator mediator = (InvokeMediator) m;
        OMElement invokeElem = fac.createOMElement(INVOKE_N, synNS);

        if (mediator.getTargetTemplate() != null) {
            invokeElem.addAttribute(fac.createOMAttribute(
                    "target", nullNS, mediator.getTargetTemplate()));

            serializeParams(invokeElem, mediator);
            saveTracingState(invokeElem, mediator);
        }

        return invokeElem;
    }

    private void serializeParams(OMElement invokeElem, InvokeMediator mediator) {
        Map<String, Value> paramsMap = mediator.getpName2ExpressionMap();
        for (Map.Entry<String,Value> entry : paramsMap.entrySet()) {
            if (!"".equals(entry.getKey())) {
                OMElement paramEl = fac.createOMElement(InvokeMediatorFactory.WITH_PARAM_Q.getLocalPart(),
                                                        synNS);
                paramEl.addAttribute(fac.createOMAttribute("name", nullNS, entry.getKey()));
                //serialize value attribute
                new ValueSerializer().serializeValue(entry.getValue(), "value", paramEl);
                invokeElem.addChild(paramEl);
            }
        }

    }

    public String getMediatorClassName() {
        return InvokeMediator.class.getName();
    }
}
