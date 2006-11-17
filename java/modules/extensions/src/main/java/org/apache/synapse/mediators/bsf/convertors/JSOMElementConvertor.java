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

package org.apache.synapse.mediators.bsf.convertors;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.synapse.SynapseException;
import org.apache.xmlbeans.XmlObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;

/**
 * JSObjectConvertor converts between Synapse OMElements and JavaScript E4X XML objects
 */
public class JSOMElementConvertor extends DefaultOMElementConvertor {

    protected Scriptable scope;

    public JSOMElementConvertor() {
        Context cx = Context.enter();
        try {
            this.scope = cx.initStandardObjects();
        } finally {
            Context.exit();
        }
    }

    public Object toScript(OMElement o) {
        XmlObject xml;
        try {
            xml = XmlObject.Factory.parse(o.getXMLStreamReader());
        } catch (Exception e) {
            throw new SynapseException("exception getting message XML: " + e);
        }

        Context cx = Context.enter();
        try {

            Object wrappedXML = cx.getWrapFactory().wrap(cx, scope, xml, XmlObject.class);
            Scriptable jsXML = cx.newObject(scope, "XML", new Object[] { wrappedXML });

            return jsXML;

        } finally {
            Context.exit();
        }
    }

    public OMElement fromScript(Object o) {
        if (!(o instanceof XMLObject)) {
            return super.fromScript(o);
        }

        Wrapper wrapper = (Wrapper) ScriptableObject.callMethod((XMLObject)o, "getXmlObject", new Object[0]);
        XmlObject xmlObject = (XmlObject)wrapper.unwrap();
        try {

            StAXOMBuilder builder = new StAXOMBuilder(xmlObject.newInputStream());
            OMElement omElement = builder.getDocumentElement();

            return omElement;

        } catch (XMLStreamException e) {
            throw new SynapseException(e);
        }
    }

}
