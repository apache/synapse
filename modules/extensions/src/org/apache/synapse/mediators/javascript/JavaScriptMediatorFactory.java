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
package org.apache.synapse.mediators.javascript;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.synapse.Constants;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.ws.commons.schema.XmlSchema;

/**
 * Creates an instance of a JavaScript mediator. <p/>
 * 
 *  <javascript/>
 *     <![CDATA[ script here ]]>
 *  </javascript>
 */
public class JavaScriptMediatorFactory implements MediatorFactory {

    // private static final Log log = LogFactory.getLog(JavaScriptMediatorFactory.class);

    private static final QName TAG_NAME = new QName(Constants.SYNAPSE_NAMESPACE + "/js", "javascript");

    /**
     * Create a JavaScript mediator
     * 
     * @param elem
     *            the OMElement that specifies the JavaScript mediator configuration
     * @return the Spring mediator instance created
     */
    public Mediator createMediator(OMElement elem) {
        if (elem.getLocalName().toLowerCase().equals(TAG_NAME.getLocalPart())) {
            OMText text = (OMText) elem.getFirstOMChild();
            String script = text.getText();
            JavaScriptMediator sm = new JavaScriptMediator();
            sm.setScript(script);
            return sm;
        }
        return null;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }

    public QName getTagSchemaType() {
        return new QName(Constants.SYNAPSE_NAMESPACE,
            getTagQName().getLocalPart() + "_type", "js");
    }
}
