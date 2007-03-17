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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;

/**
 * JSObjectConvertor converts between OMElements and JavaScript E4X XML objects
 */
public class JSOMElementConvertor extends DefaultOMElementConvertor {

    private static final Log log = LogFactory.getLog(JSOMElementConvertor.class);

    private static boolean axiomJavaScript = false;
    static {
        try {
            Class.forName("org.mozilla.javascript.xmlimpl.AxiomNode");
            axiomJavaScript = true;
        } catch (ClassNotFoundException ignore) {}
    }

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
        if (axiomJavaScript) {
            Context cx = Context.enter();
            try {
                return cx.newObject(scope, "XML", new Object[]{o});

            } finally {
                Context.exit();
            }
            
        } else {
            XmlObject xml = null;
            try {
                xml = XmlObject.Factory.parse(o.getXMLStreamReader());
            } catch (Exception e) {
                handleException("Error converting OMElement to a JavaSript XmlObject", e);
            }

            Context cx = Context.enter();
            try {

                Object wrappedXML = cx.getWrapFactory().wrap(cx, scope, xml, XmlObject.class);
                return cx.newObject(scope, "XML", new Object[]{wrappedXML});

            } finally {
                Context.exit();
            }
        }
    }

    public OMElement fromScript(Object o) {

        if (o == null) {
            handleException("Cannot convert null JavaScript Object to an OMElement");
        }

        if (!(o instanceof XMLObject)) {
            log.debug("Converting Object of type : " + o.getClass().getName() + " to an OMElement");
            return super.fromScript(o);
        }

        if (axiomJavaScript) {
            return (OMElement) ScriptableObject.callMethod(
                (Scriptable) o, "getXmlObject", new Object[0]);

        } else {
            // TODO: E4X Bug? Shouldn't need this copy, but without it the outer element gets lost???
            Scriptable jsXML =
                (Scriptable) ScriptableObject.callMethod((Scriptable) o, "copy", new Object[0]);
            Wrapper wrapper =
                (Wrapper) ScriptableObject.callMethod((XMLObject)jsXML, "getXmlObject", new Object[0]);

            XmlObject xmlObject = (XmlObject) wrapper.unwrap();

            try {
                StAXOMBuilder builder = new StAXOMBuilder(xmlObject.newInputStream());
                return builder.getDocumentElement();

            } catch (XMLStreamException e) {
                handleException("Error converting JavaScipt object of type : "
                    + o.getClass().getName() + " to an OMElement", e);
            }
            return null;
        }
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
