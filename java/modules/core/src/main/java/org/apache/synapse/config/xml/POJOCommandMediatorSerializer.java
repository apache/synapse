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
import org.apache.synapse.mediators.ext.POJOCommandMediator;
import org.apache.synapse.util.SynapseXPath;

import java.util.Iterator;

/**
 * Creates an instance of a Class mediator using XML configuration specified
 * <p/>
 * <pre>
 * &lt;pojoCommand name=&quot;class-name&quot;&gt;
 *   &lt;property name=&quot;string&quot; value=&quot;literal&quot; expression=&quot;xpath&quot;?
 *          context-name=&quot;string&quot;? &gt;
 *      either literal or XML child
 *   &lt;/property&gt;
 *   &lt;property name=&quot;string&quot; expression=&quot;XPATH expression&quot;
 *                action=(&quot;ReadMessage&quot; | &quot;UpdateMessage&quot; |
 *                  &quot;ReadAndUpdateMessage&quot;) context-name=&quot;string&quot;? /&gt;
 *   &lt;property name=&quot;string&quot; context-name=&quot;string&quot;
 *                action=(&quot;ReadContext&quot; | &quot;UpdateContext&quot; |
 *                  &quot;ReadAndUpdateContext&quot;) expression=&quot;XPATH expression&quot;? /&gt;
 * &lt;/pojoCommand&gt;
 * </pre>
 */
public class POJOCommandMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeMediator(OMElement parent, Mediator m) {
        
        if (!(m instanceof POJOCommandMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }
        
        POJOCommandMediator mediator = (POJOCommandMediator) m;
        
        OMElement pojoCommand = fac.createOMElement("pojoCommand", synNS);
        saveTracingState(pojoCommand, mediator);

        if (mediator.getCommand() != null && mediator.getCommand().getClass().getName() != null) {
            pojoCommand.addAttribute(fac.createOMAttribute(
                "name", nullNS, mediator.getCommand().getName()));
        } else {
            handleException("Invalid POJO Command mediator. The command class name is required");
        }

        for (Iterator itr = mediator.
            getStaticSetterProperties().keySet().iterator(); itr.hasNext(); ) {

            String propName = (String) itr.next();
            Object value = mediator.getStaticSetterProperties().get(propName);
            OMElement prop = fac.createOMElement(PROP_Q);
            prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));

            if (value instanceof String) {
                prop.addAttribute(fac.createOMAttribute("value", nullNS, (String) value));
            } else if (value instanceof OMElement) {
                prop.addChild((OMElement) value);
            } else {
                handleException("Unable to serialize the command " +
                    "mediator property with the naem " + propName + " : Unknown type");
            }

            if (mediator.getContextGetterProperties().containsKey(propName)) {
                prop.addAttribute(fac.createOMAttribute("context-name", nullNS,
                    mediator.getContextGetterProperties().get(propName)));
            } else if (mediator.getMessageGetterProperties().containsKey(propName)) {
                SynapseXPathSerializer.serializeXPath(
                    mediator.getMessageGetterProperties().get(propName), prop, "expression");
            }
            pojoCommand.addChild(prop);
        }

        for (Iterator itr = mediator.
            getMessageSetterProperties().keySet().iterator(); itr.hasNext(); ) {

            String propName = (String) itr.next();
            OMElement prop = fac.createOMElement(PROP_Q);
            prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));
            SynapseXPathSerializer.serializeXPath(
                mediator.getMessageSetterProperties().get(propName), prop, "expression");

            if (mediator.getMessageGetterProperties().containsKey(propName)) {
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "ReadAndUpdateMessage"));
            } else if (mediator.getContextGetterProperties().containsKey(propName)) {
                prop.addAttribute(fac.createOMAttribute("context-name", nullNS,
                    mediator.getContextGetterProperties().get(propName)));
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "ReadMessage"));                
            } else {
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "ReadMessage"));                                
            }
            pojoCommand.addChild(prop);
        }

        for (Iterator itr = mediator.
            getContextSetterProperties().keySet().iterator(); itr.hasNext(); ) {

            String propName = (String) itr.next();
            OMElement prop = fac.createOMElement(PROP_Q);
            prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));
            prop.addAttribute(fac.createOMAttribute("context-name", nullNS,
                mediator.getContextSetterProperties().get(propName)));

            if (mediator.getContextGetterProperties().containsKey(propName)) {
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "ReadAndUpdateContext"));
            } else if (mediator.getMessageGetterProperties().containsKey(propName)) {
                SynapseXPathSerializer.serializeXPath(
                    mediator.getMessageGetterProperties().get(propName), prop, "expression");
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "ReadContext"));
            } else {
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "ReadContext"));                
            }
            pojoCommand.addChild(prop);
        }

        for (Iterator itr = mediator.
            getContextGetterProperties().keySet().iterator(); itr.hasNext(); ) {

            String propName = (String) itr.next();
            if (!isSerialized(propName, mediator)) {
                String value = mediator.getContextGetterProperties().get(propName);
                OMElement prop = fac.createOMElement(PROP_Q);
                prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));
                prop.addAttribute(fac.createOMAttribute("context-name", nullNS, value));
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "UpdateContext"));
                pojoCommand.addChild(prop);
            }
        }

        for (Iterator itr = mediator.
            getMessageGetterProperties().keySet().iterator(); itr.hasNext(); ) {

            String propName = (String) itr.next();
            if (!isSerialized(propName, mediator)) {
                OMElement prop = fac.createOMElement(PROP_Q);
                prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));
                SynapseXPathSerializer.serializeXPath(
                    mediator.getMessageGetterProperties().get(propName), prop, "expression");
                prop.addAttribute(fac.createOMAttribute("action", nullNS, "UpdateMessage"));
                pojoCommand.addChild(prop);
            }
        }

        if (parent != null) {
            parent.addChild(pojoCommand);
        }
        return pojoCommand;
    }

    private boolean isSerialized(String propName, POJOCommandMediator m) {
        return m.getContextSetterProperties().containsKey(propName) ||
            m.getStaticSetterProperties().containsKey(propName) ||
            m.getMessageSetterProperties().containsKey(propName);
    }

    public String getMediatorClassName() {
        return POJOCommandMediator.class.getName();
    }
}
