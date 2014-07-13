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

package org.apache.synapse.mediators.bsf;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.Constants;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.config.xml.AbstractMediatorFactory;

/**
 * Creates an instance of a Script mediator. <p/>
 * <p>
 * There are two ways of defining a script mediator, either using a registry property or 
 * inline in the Synapse config XML.
 * <p>
 * A script mediator using a registry property is defined as follows: 
 * <p>
 * <pre>
 *  &lt;script key=&quot;property-key&quot; function=&quot;script-function-name&quot; &lt;script/&gt;
 * </pre>
 * <p>
 * The property-key is a Synapse registry property containing the script source. The function is an 
 * optional attribute defining the name of the script function to call, if not specified it
 * defaults to a function named 'mediate'. The function takes a single parameter which is the 
 * Synapse MessageContext. The function may return a boolean, if it does not then true is assumed.
 * <p>
 * An inline script mediator has the script source embedded in the config XML:
 * <pre>
 *  &lt;script.LL&gt...src code...&lt;script.LL/&gt;
 * </pre>
 * <p>
 * where LL is the script language name extension. The environment of the script has the Synapse
 * MessageContext predefined in a script variable named 'mc'.
 * <p>
 * An example of an inline mediator using JavaScript/E4X which returns false if the SOAP message
 * body contains an element named 'symbol' which has a value of 'IBM' would be:
 * <p>
 * <pre>
 *  &lt;script.js&gt;mc.getPayloadXML()..symbol != "IBM";&lt;script.js/&gt;
 * </pre>
 * <p>
 * The boolean response from the inlined mediator is either the response from the evaluation of the
 * script statements or if that result is not a boolean then a response of true is assumed.
 * <p>
 * The MessageContext passed in to the script mediator has additional methods over the Synapse
 * MessageContext to enable working with the XML in a way natural to the scripting language. For
 * example when using JavaScript get/setPayloadXML use E4X XML objects, when using Ruby they
 * use REXML documents.
 */
public class ScriptMediatorFactory extends AbstractMediatorFactory {

    private static final QName TAG_NAME = new QName(Constants.SYNAPSE_NAMESPACE, "script");

    public Mediator createMediator(OMElement elem) {

        ScriptMediator sm;

        OMAttribute scriptKey = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        if (scriptKey != null) {
            OMAttribute function = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "function"));
            String functionName = (function == null) ? "mediate" : function.getAttributeValue();
            sm = new ScriptMediator(scriptKey.getAttributeValue(), functionName);
        } else if (elem.getLocalName().indexOf('.') > -1){
            sm = new InlineScriptMediator(elem.getLocalName(), elem.getText());
            ((InlineScriptMediator)sm).init();
        } else {
            throw new SynapseException("must specify 'key' attribute or inline script source");
        }

        return sm;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}
