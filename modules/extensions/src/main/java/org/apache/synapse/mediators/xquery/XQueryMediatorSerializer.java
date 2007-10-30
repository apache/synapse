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
package org.apache.synapse.mediators.xquery;

import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.Mediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

import net.sf.saxon.javax.xml.xquery.XQItemType;

import javax.xml.namespace.QName;

/**
 * Serialize the given XQuery mediator into a XML
 * <p/>
 * <pre>
 * <xquery [target="XPath"]>
 *   <dataSource>*
 *      <property name="string" value="literal"/>*
 *   </dataSource>
 *   <variable name="string"  type="int" value="literal" [key="string"] [expression=XPath] />*
 * </xquery>
 * </pre>
 */
public class XQueryMediatorSerializer extends AbstractMediatorSerializer {

    private static final Log log = LogFactory.getLog(XQueryMediatorSerializer.class);

    private static final OMNamespace XQUERY_NS
        = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "xquery");

    public OMElement serializeMediator(OMElement parent, Mediator m) {

        if (!(m instanceof XQueryMediator)) {
            handleException("Invalid Mediator has passed to serializer");
        }
        XQueryMediator queryMediator = (XQueryMediator) m;

        OMElement xquery = fac.createOMElement("xquery", synNS);
        String key = queryMediator.getQueryKey();
        if (key != null) {
            xquery.addAttribute(fac.createOMAttribute(
                "key", nullNS, key));

        }

        saveTracingState(xquery, queryMediator);

        AXIOMXPath targetXPath = queryMediator.getTarget();
        if (targetXPath != null &&
            !XQueryMediator.DEFAULT_XPATH.toString().equals(targetXPath.toString())) {
            xquery.addAttribute(fac.createOMAttribute(
                "target", nullNS, targetXPath.toString()));
            serializeNamespaces(xquery, targetXPath);
        }

        List pros = queryMediator.getDataSourceProperties();
        if (pros != null && !pros.isEmpty()) {
            OMElement dataSource = fac.createOMElement("dataSource", synNS);
            serializeProperties(dataSource, pros);
            xquery.addChild(dataSource);
        }

        List list = queryMediator.getVariables();
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                if (o instanceof MediatorBaseVariable) {
                    MediatorBaseVariable variable = (MediatorBaseVariable) o;
                    QName name = variable.getName();
                    Object value = variable.getValue();
                    if (name != null && value != null) {
                        OMElement baseElement = fac.createOMElement("variable", synNS);
                        baseElement.addAttribute(fac.createOMAttribute(
                            "name", nullNS, name.getLocalPart()));
                        baseElement.addAttribute(fac.createOMAttribute(
                            "value", nullNS, (String) value));
                        String type = null;
                        int varibelType = variable.getType();
                        if (XQItemType.XQBASETYPE_INT == varibelType) {
                            type = "INT";
                        } else if (XQItemType.XQBASETYPE_INTEGER == varibelType) {
                            type = "INTEGER";
                        } else if (XQItemType.XQBASETYPE_BOOLEAN == varibelType) {
                            type = "BOOLEAN";
                        } else if (XQItemType.XQBASETYPE_BYTE == varibelType) {
                            type = "BYTE";
                        } else if (XQItemType.XQBASETYPE_DOUBLE == varibelType) {
                            type = "DOUBLE";
                        } else if (XQItemType.XQBASETYPE_SHORT == varibelType) {
                            type = "SHORT";
                        } else if (XQItemType.XQBASETYPE_LONG == varibelType) {
                            type = "LONG";
                        } else if (XQItemType.XQBASETYPE_FLOAT == varibelType) {
                            type = "FLOAT";
                        } else if (XQItemType.XQBASETYPE_STRING == varibelType) {
                            type = "STRING";
                        } else if (XQItemType.XQITEMKIND_DOCUMENT == varibelType) {
                            type = "DOCUMENT";
                        } else if (XQItemType.XQITEMKIND_DOCUMENT_ELEMENT == varibelType) {
                            type = "DOCUMENT_ELEMENT";
                        } else if (XQItemType.XQITEMKIND_ELEMENT == varibelType) {
                            type = "ELEMENT";
                        } else {
                            handleException("Unknown Type " + varibelType);
                        }
                        if (type != null) {
                            baseElement.addAttribute(fac.createOMAttribute(
                                "type", nullNS, type));

                        }
                        xquery.addChild(baseElement);
                    }
                } else if (o instanceof MediatorCustomVariable) {
                    MediatorCustomVariable variable = (MediatorCustomVariable) o;
                    QName name = variable.getName();
                    if (name != null) {
                        OMElement customElement = fac.createOMElement("variable", synNS);
                        customElement.addAttribute(fac.createOMAttribute(
                            "name", nullNS, name.getLocalPart()));
                        String regkey = variable.getRegKey();
                        if (regkey != null) {
                            customElement.addAttribute(fac.createOMAttribute(
                                "key", nullNS, regkey));
                        }
                        AXIOMXPath expression = variable.getExpression();
                        if (expression != null && !XQueryMediator.DEFAULT_XPATH.toString().
                            equals(expression.toString())) {
                            customElement.addAttribute(fac.createOMAttribute(
                                "expression", nullNS, expression.toString()));
                            serializeNamespaces(customElement, expression);
                        }
                        String type = null;
                        int varibelType = variable.getType();
                        if (XQItemType.XQITEMKIND_DOCUMENT == varibelType) {
                            type = "DOCUMENT";
                        } else if (XQItemType.XQITEMKIND_DOCUMENT_ELEMENT == varibelType) {
                            type = "DOCUMENT_ELEMENT";
                        } else if (XQItemType.XQITEMKIND_ELEMENT == varibelType) {
                            type = "ELEMENT";
                        } else if (XQItemType.XQBASETYPE_INT == varibelType) {
                            type = "INT";
                        } else if (XQItemType.XQBASETYPE_INTEGER == varibelType) {
                            type = "INTEGER";
                        } else if (XQItemType.XQBASETYPE_BOOLEAN == varibelType) {
                            type = "BOOLEAN";
                        } else if (XQItemType.XQBASETYPE_BYTE == varibelType) {
                            type = "BYTE";
                        } else if (XQItemType.XQBASETYPE_DOUBLE == varibelType) {
                            type = "DOUBLE";
                        } else if (XQItemType.XQBASETYPE_SHORT == varibelType) {
                            type = "SHORT";
                        } else if (XQItemType.XQBASETYPE_LONG == varibelType) {
                            type = "LONG";
                        } else if (XQItemType.XQBASETYPE_FLOAT == varibelType) {
                            type = "FLOAT";
                        } else if (XQItemType.XQBASETYPE_STRING == varibelType) {
                            type = "STRING";
                        } else {
                            handleException("Unknown Type " + varibelType);
                        }
                        if (type != null) {
                            customElement.addAttribute(fac.createOMAttribute(
                                "type", nullNS, type));

                        }
                        xquery.addChild(customElement);
                    }
                }
            }
        }
        if (parent != null) {
            parent.addChild(xquery);
        }
        return xquery;
    }

    public String getMediatorClassName() {
        return XQueryMediator.class.getName();
    }


}
