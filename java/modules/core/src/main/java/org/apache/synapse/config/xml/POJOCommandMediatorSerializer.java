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
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.ext.POJOCommandMediator;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Creates an instance of a Class mediator using XML configuration specified
 * <p/>
 * <pre>
 * &lt;pojoCommand name=&quot;class-name&quot;&gt;
 *   &lt;property name=&quot;string&quot; value=&quot;literal&quot;&gt;
 *      either literal or XML child
 *   &lt;/property&gt;
 *   &lt;property name=&quot;string&quot; expression=&quot;XPATH expression&quot;/&gt;
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

        for (Iterator itr = mediator.getStaticProps().keySet().iterator(); itr.hasNext(); ) {
            String propName = (String) itr.next();
            String value = (String) mediator.getStaticProps().get(propName);
            OMElement prop = fac.createOMElement(PROP_Q);
            prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));
            prop.addAttribute(fac.createOMAttribute("value", nullNS, value));
            pojoCommand.addChild(prop);
        }

        for (Iterator itr = mediator.getDynamicProps().keySet().iterator(); itr.hasNext(); ) {
            String propName = (String) itr.next();
            AXIOMXPath exprn = (AXIOMXPath) mediator.getDynamicProps().get(propName);
            OMElement prop = fac.createOMElement(PROP_Q);
            prop.addAttribute(fac.createOMAttribute("name", nullNS, propName));
            prop.addAttribute(fac.createOMAttribute("expression", nullNS,
                exprn.toString()));
            serializeNamespaces(prop, exprn);
            pojoCommand.addChild(prop);
        }

        if (parent != null) {
            parent.addChild(pojoCommand);
        }
        return pojoCommand;
    }

    public String getMediatorClassName() {
        return POJOCommandMediator.class.getName();
    }
}
