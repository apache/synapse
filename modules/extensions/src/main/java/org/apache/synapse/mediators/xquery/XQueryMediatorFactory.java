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

import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.OMElementUtils;
import org.apache.synapse.config.xml.MediatorPropertyFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.Mediator;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.util.Iterator;
import java.io.StringReader;

import net.sf.saxon.javax.xml.xquery.XQItemType;

/**
 * Creates a XQuery mediator from the given XML
 * <p/>
 * <pre>
 * <xquery [key="XPath"]>
 *   <dataSource>*
 *      <property name="string" value="literal"/>*
 *   </dataSource>
 *   <variable name="string"  type="int" value="literal" [key="string"] [expression=XPath] />*
 * </xquery>
 * </pre>
 */

public class XQueryMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(XQueryMediatorFactory.class);

    private static final QName TAG_NAME
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "xquery");
    public static final QName ATT_NAME_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "name");
    public static final QName ATT_VALUE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "value");
    public static final QName ATT_EXPR_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "expression");
    public static final QName ATT_KEY_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "key");
    public static final QName ATT_TYPE_Q = new QName(XMLConfigConstants.NULL_NAMESPACE, "type");


    public Mediator createMediator(OMElement elem) {

        XQueryMediator xQueryMediator = new XQueryMediator();
        OMAttribute xqueryKey = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "key"));
        OMAttribute attrTarget = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "target"));
        if (xqueryKey != null) {
            String queryKey = xqueryKey.getAttributeValue();
            if (queryKey != null) {
                xQueryMediator.setQueryKey(queryKey.trim());
            } else {
                handleException("The 'key' attribute is required for the XQuery mediator");
            }
        } else {
            handleException("The 'key' attribute is required for the XQuery mediator");
        }
        if (attrTarget != null) {
            String targetValue = attrTarget.getAttributeValue();
            if (targetValue != null) {
                try {
                    AXIOMXPath xp = new AXIOMXPath(targetValue.trim());
                    OMElementUtils.addNameSpaces(xp, elem, log);
                    xQueryMediator.setTarget(xp);
                } catch (JaxenException e) {
                    handleException("Invalid XPath specified for the target attribute : " +
                        targetValue);
                }
            }
        }
        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processTraceState(xQueryMediator, elem);
        OMElement dataSource = elem.getFirstChildWithName(
            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "dataSource"));
        if (dataSource != null) {
            xQueryMediator.addAllDataSoureProperties(
                MediatorPropertyFactory.getMediatorProperties(dataSource));
        }

        Iterator it = elem.getChildrenWithName(new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "variable"));
        while (it.hasNext()) {
            OMElement variableOM = (OMElement) it.next();
            String name = variableOM.getAttributeValue(ATT_NAME_Q);
            if (name != null && !"".equals(name)) {
                String type = variableOM.getAttributeValue(ATT_TYPE_Q);
                if (type != null && !"".equals(type)) {
                    String value = variableOM.getAttributeValue(ATT_VALUE_Q);
                    MediatorVariable variable;
                    if (value != null && !"".equals(value)) {
                        variable = new MediatorBaseVariable(
                            new QName(name.trim()));
                        variable.setValue(value.trim());
                    } else {
                        String key = variableOM.getAttributeValue(ATT_KEY_Q);
                        String expr = variableOM.getAttributeValue(ATT_EXPR_Q);
                        variable = new MediatorCustomVariable(
                            new QName(name.trim()));
                        if (key != null) {
                            ((MediatorCustomVariable) variable).setRegKey(key.trim());
                        }
                        if (expr != null && !"".equals(expr)) {
                            try {
                                AXIOMXPath xp = new AXIOMXPath(expr.trim());
                                OMElementUtils.addNameSpaces(xp, variableOM, log);
                                ((MediatorCustomVariable) variable).setExpression(xp);

                            } catch (JaxenException e) {
                                handleException("Invalid XPath specified for" +
                                    " the expression attribute : " + expr);
                            }
                        }
                    }
                    if ("INT".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_INT);
                    } else if ("INTEGER".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_INTEGER);
                    } else if ("BOOLEAN".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_BOOLEAN);
                    } else if ("BYTE".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_BYTE);
                    } else if ("DOUBLE".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_DOUBLE);
                    } else if ("SHORT".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_SHORT);
                    } else if ("LONG".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_LONG);
                    } else if ("FLOAT".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_FLOAT);
                    } else if ("STRING".equals(type.trim())) {
                        variable.setType(XQItemType.XQBASETYPE_STRING);
                    } else if ("DOCUMENT".equals(type.trim())) {
                        variable.setType(XQItemType.XQITEMKIND_DOCUMENT);
                    } else if ("DOCUMENT_ELEMENT".equals(type.trim())) {
                        variable.setType(XQItemType.XQITEMKIND_DOCUMENT_ELEMENT);
                    } else if ("ELEMENT".equals(type.trim())) {
                        variable.setType(XQItemType.XQITEMKIND_ELEMENT);
                    } else {
                        handleException("Unsupported Type");
                    }
                    xQueryMediator.addVariable(variable);
                }
            }
        }
        return xQueryMediator;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }


}
