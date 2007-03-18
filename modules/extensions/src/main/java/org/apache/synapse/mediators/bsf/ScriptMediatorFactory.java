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
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.Constants;

/**
 * Creates an instance of a Script mediator for inline or external script mediation for BSF
 * scripting languages.
 *
 *  * <pre>
 *    &lt;script [key=&quot;entry-key&quot;]
 *      [function=&quot;script-function-name&quot;] language="javascript|groovy|ruby"&gt
 *      (text | xml)?
 *    &lt;/script&gt;
 * </pre>
 *
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

        ScriptMediator mediator;
        OMAttribute keyAtt  = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "key"));
        OMAttribute langAtt = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "language"));
        OMAttribute funcAtt = elem.getAttribute(new QName(Constants.NULL_NAMESPACE, "function"));

        if (langAtt == null) {
            throw new SynapseException("The 'language' attribute is required for a script mediator");
            // TODO: couldn't this be determined from the key in some scenarios?
        }
        if (keyAtt == null && funcAtt != null) {
            throw new SynapseException("Cannot use 'function' attribute without 'key' attribute for a script mediator");
        }

        if (keyAtt != null) {
            String functionName = (funcAtt == null ? null : funcAtt.getAttributeValue());
            mediator = new ScriptMediator(
                langAtt.getAttributeValue(), keyAtt.getAttributeValue(), functionName);
        } else {
            mediator = new ScriptMediator(langAtt.getAttributeValue(), elem.getText());
        }

        initMediator(mediator, elem);
        return mediator;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}
