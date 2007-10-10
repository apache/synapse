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

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.jaxen.JaxenException;
import org.xml.sax.InputSource;
import org.w3c.dom.Element;
import net.sf.saxon.javax.xml.xquery.*;
import net.sf.saxon.xqj.SaxonXQDataSource;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;


/**
 * The XQueryMediator  provides the means to extract and manipulate data from XML documents  using
 * XQuery . It is possible to query against the current  SOAP Message or external XML. To query
 * against the current  SOAP Message ,it is need to define custom variable with any name and type as
 * element,document,document_element By providing a expression ,It is possible to select a custom
 * node for querying.The all the variable  that  have defined  in the mediator will be available
 * during the query process .Basic variable can use bind basic type.
 * currently only support * string,int,byte,short,double,long,float and boolean * types.
 * Custom Variable can use to bind XML documents ,SOAP payload and any basic type which create
 * through the XPath expression .
 */

public class XQueryMediator extends AbstractMediator {

    /** Properties that must set to the XQDataSource */
    private List dataSourceProperties = new ArrayList();

    /** The key for lookup the xquery */
    private String queryKey;

    /** The source of the xquery */
    private String querySource;

    /** The default xpath to get the first child of the SOAPBody*/
    public static final String DEFAULT_XPATH = "//s11:Envelope/s11:Body/child::*[position()=1] | " +
                                               "//s12:Envelope/s12:Body/child::*[position()=1]";

    /** The (optional) XPath expression which yeilds the target element to attached the result  */
    private AXIOMXPath target = null;

    /** The list of variables for binding to the DyanamicContext in order to available for querying*/
    private List variables = new ArrayList();

    /** Lock used to ensure thread-safe lookup of the object from the registry  */
    private final Object resourceLock = new Object();

    /** Is it need to use DOMSource and DOMResult?   */
    private boolean isDOMRequired = true;

    /** The DataSource which use to create a connection to XML database */
    private XQDataSource cachedXQDataSource = null;

    /** connection with a specific XQuery engine.Connection will live as long as synapse live*/
    private XQConnection cachedConnection = null;     

    /** An expression that use for multiple  executions.Expression will recreate if query has changed */
    private XQPreparedExpression cachedPreparedExpression = null;
    
    public XQueryMediator() {
        // create the default XPath
        try {
            this.target = new AXIOMXPath(DEFAULT_XPATH);
            this.target.addNamespace("s11", SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
            this.target.addNamespace("s12", SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        } catch (JaxenException e) {
            handleException("Error creating target XPath expression", e);
        }
    }

    /**
     * Performs the query and attached the result to the target Node
     * @param synCtx  The current message
     * @return  true always
     */
    public boolean mediate(MessageContext synCtx) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("XQuery mediator mediate()");
            }
            boolean shouldTrace = shouldTrace(synCtx.getTracingState());
            if (shouldTrace) {
                trace.trace("Start : XQuery mediator");
            }
            if (log.isDebugEnabled()) {
                log.debug("Performing XQuery using query resource with key : " + queryKey);
            }
            performQuery(synCtx, shouldTrace);
            if (shouldTrace) {
                trace.trace("Start : XQuery mediator");
            }
            return true;
        } catch (Exception e) {
            handleException("Unable to do the query ", e);
        }
        return false;
    }

    /**
     * Perform the quering and get the result and attached to the target node
     *
     * @param synCtx      The current MessageContext
     * @param shouldTrace is need to trace ?
     */
    private void performQuery(MessageContext synCtx, boolean shouldTrace) {
        boolean reLoad = false;
        boolean needBind = false;
        XQResultSequence resultSequence ;
        Entry dp = synCtx.getConfiguration().getEntryDefinition(queryKey);
        // if the queryKey refers to a dynamic resource
        if (dp != null && dp.isDynamic()) {
            if (!dp.isCached() || dp.isExpired()) {
                reLoad = true;
            }
        }
        try {
            synchronized (resourceLock) {
                if (cachedXQDataSource == null) {
                    // A factory for XQConnection  objects
                    cachedXQDataSource = new SaxonXQDataSource();
                    //setting up the properties to the XQDataSource
                    if (dataSourceProperties != null && !dataSourceProperties.isEmpty()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Setting up properties to the XQDataSource");
                        }
                        for (int i = 0; i < dataSourceProperties.size(); i++) {
                            MediatorProperty prop = (MediatorProperty) dataSourceProperties.get(i);
                            if (prop != null) {
                                cachedXQDataSource.setProperty(prop.getName(), prop.getValue());
                            }
                        }
                    }
                }
                if (cachedConnection == null
                    || (cachedConnection != null && cachedConnection.isClosed())) {
                    //get the Connection to XML DataBase
                    if (log.isDebugEnabled()) {
                        log.debug("Creating a connection ");
                    }
                    cachedConnection = cachedXQDataSource.getConnection();
                }
                if (reLoad || querySource == null || cachedPreparedExpression == null
                    || (cachedPreparedExpression != null && cachedPreparedExpression.isClosed())) {

                    Object o = synCtx.getEntry(queryKey);
                    if (o instanceof OMElement) {
                        querySource = ((OMElement) (o)).getText();
                    } else if (o instanceof String) {
                        querySource = (String) o;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Picked up the xquery source " + querySource + "from the key " +
                                  queryKey);
                    }
                    if (shouldTrace) {
                        trace.trace("Picked up the xquery source " + querySource + "from the key " +
                                    queryKey);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Prepare an expression for the query " + querySource);
                    }
                    if (shouldTrace) {
                        trace.trace("Prepare an expression for the query " + querySource);
                    }
                    //create an XQPreparedExpression using the query source
                    cachedPreparedExpression = cachedConnection.prepareExpression(querySource);
                    // need binding because the expression just has recreated
                    needBind = true;
                }

                //Bind the external variables to the DynamicContext
                if (variables != null & !variables.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Binding  external variables to the DynamicContext");
                    }
                    for (int i = 0; i < variables.size(); i++) {
                        MediatorVariable variable = (MediatorVariable) variables.get(i);
                        boolean hasValueChanged = variable.evaluateValue(synCtx);
                        //if the value has changed or need binding because the expression has recreated
                        if (hasValueChanged || needBind) {
                            //Binds the external variable to the DynamicContext
                            bindVariable(cachedPreparedExpression, variable);
                        }
                    }
                }
                //executing the query
                resultSequence = cachedPreparedExpression.executeQuery();
            }
            if (resultSequence == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Result Sequence is null");
                }
                return;
            }
            while (resultSequence.next()) {
                XQItem xqItem = resultSequence.getItem();
                if (xqItem == null) {
                    return;
                }
                XQItemType itemType = xqItem.getItemType();
                if (itemType == null) {
                    return;
                }
                int itemKind = itemType.getItemKind();
                int baseType = itemType.getBaseType();
                if (log.isDebugEnabled()) {
                    log.debug("The XQuery Result " + xqItem.getItemAsString());
                }
                if (shouldTrace) {
                    trace.trace("The XQuery Result " + xqItem.getItemAsString());
                }
                //The target node that is going to modify
                OMNode destination = getTargetNode(synCtx);
                if (destination != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("The target node " + destination);
                    }
                    if (shouldTrace) {
                        trace.trace("The target node " + destination);
                    }
                    //If the result is XML
                    if (XQItemType.XQITEMKIND_DOCUMENT_ELEMENT == itemKind ||
                        XQItemType.XQITEMKIND_ELEMENT == itemKind ||
                        XQItemType.XQITEMKIND_DOCUMENT == itemKind) {
                        StAXOMBuilder builder = new StAXOMBuilder(
                                XMLInputFactory.newInstance().createXMLStreamReader(
                                        new StringReader(xqItem.getItemAsString())));
                        OMElement resultOM = builder.getDocumentElement();
                        if (resultOM != null) {
                            //replace the target node from the result
                            destination.insertSiblingAfter(resultOM);
                            destination.detach();
                        }
                    } else if (XQItemType.XQBASETYPE_INTEGER == baseType ||
                               XQItemType.XQBASETYPE_INT == baseType) {
                        //replace the text value of the target node by the result ,If the result is
                        // a basic type
                        ((OMElement) destination).setText(String.valueOf(xqItem.getInt()));
                    } else if (XQItemType.XQBASETYPE_BOOLEAN == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getBoolean()));
                    } else if (XQItemType.XQBASETYPE_DOUBLE == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getDouble()));
                    } else if (XQItemType.XQBASETYPE_FLOAT == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getFloat()));
                    } else if (XQItemType.XQBASETYPE_LONG == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getLong()));
                    } else if (XQItemType.XQBASETYPE_SHORT == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getShort()));
                    } else if (XQItemType.XQBASETYPE_BYTE == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getByte()));
                    } else if (XQItemType.XQBASETYPE_STRING == baseType) {
                        ((OMElement) destination).setText(String.valueOf(xqItem.getItemAsString()));
                    }
                }
                break;   // Only take the *first* value of the result sequence
            }
            resultSequence.close();  // closing the result sequence
        } catch (XQException e) {
            handleException("Error during the querying " + e.getMessage(), e);
        } catch (XMLStreamException e) {
            handleException("Error during retrieving  the Doument Node as  the result "
                            + e.getMessage(), e);
        }
    }

    /**
     * Binding a variable to the Dynamic Context in order to available during doing the querying
     *
     * @param xqDynamicContext The Dynamic Context  to which the variable will be binded
     * @param variable         The variable which contains the name and vaule for binding
     * @throws XQException throws if any error occurs when binding the variable
     */
    private void bindVariable(XQDynamicContext xqDynamicContext, MediatorVariable variable) throws XQException {
        if (variable != null) {
            QName name = variable.getName();
            int type = variable.getType();
            Object value = variable.getValue();
            if (value != null && type != -1) {
                switch (type) {
                    //Binding the basic type As-Is and XML element as an InputSource
                    case(XQItemType.XQBASETYPE_BOOLEAN): {
                        boolean booleanValue = false;
                        if (value instanceof String) {
                            booleanValue = Boolean.parseBoolean((String) value);
                        } else if (value instanceof Boolean) {
                            booleanValue = ((Boolean) value).booleanValue();
                        } else {
                            handleException("Incompatible type for the Boolean");
                        }
                        xqDynamicContext.bindBoolean(name, booleanValue, null);
                        break;
                    }
                    case(XQItemType.XQBASETYPE_INT): {
                        int intValue = -1;
                        if (value instanceof String) {
                            intValue = Integer.parseInt((String) value);
                        } else if (value instanceof Integer) {
                            intValue = ((Integer) value).intValue();
                        } else {
                            handleException("Incompatible type for the Int");
                        }
                        if (intValue != -1) {
                            xqDynamicContext.bindInt(name, intValue, null);
                        }
                        break;
                    }
                    case(XQItemType.XQBASETYPE_LONG): {
                        long longValue = -1;
                        if (value instanceof String) {
                            longValue = Long.parseLong((String) value);
                        } else if (value instanceof Long) {
                            longValue = ((Long) value).longValue();
                        } else {
                            handleException("Incompatible type for the Long");
                        }
                        if (longValue != -1) {
                            xqDynamicContext.bindLong(name, longValue, null);
                        }
                        break;
                    }
                    case(XQItemType.XQBASETYPE_SHORT): {
                        short shortValue = -1;
                        if (value instanceof String) {
                            shortValue = Short.parseShort((String) value);
                        } else if (value instanceof Short) {
                            shortValue = ((Short) value).shortValue();
                        } else {
                            handleException("Incompatible type for the Short");
                        }
                        if (shortValue != -1) {
                            xqDynamicContext.bindShort(name, shortValue, null);
                        }
                        break;
                    }
                    case(XQItemType.XQBASETYPE_DOUBLE): {
                        double doubleValue = -1;
                        if (value instanceof String) {
                            doubleValue = Double.parseDouble((String) value);
                        } else if (value instanceof Double) {
                            doubleValue = ((Double) value).doubleValue();
                        } else {
                            handleException("Incompatible type for the Double");
                        }
                        if (doubleValue != -1) {
                            xqDynamicContext.bindDouble(name, doubleValue, null);
                        }
                        break;
                    }
                    case(XQItemType.XQBASETYPE_FLOAT): {
                        float floatValue = -1;
                        if (value instanceof String) {
                            floatValue = Float.parseFloat((String) value);
                        } else if (value instanceof Float) {
                            floatValue = ((Float) value).floatValue();
                        } else {
                            handleException("Incompatible type for the Float");
                        }
                        if (floatValue != -1) {
                            xqDynamicContext.bindFloat(name, floatValue, null);
                        }
                        break;
                    }
                    case(XQItemType.XQBASETYPE_BYTE): {
                        byte byteValue = -1;
                        if (value instanceof String) {
                            byteValue = Byte.parseByte((String) value);
                        } else if (value instanceof Byte) {
                            byteValue = ((Byte) value).byteValue();
                        } else {
                            handleException("Incompatible type for the Byte");
                        }
                        if (byteValue != -1) {
                            xqDynamicContext.bindByte(name, byteValue, null);
                        }
                        break;
                    }
                    case(XQItemType.XQBASETYPE_STRING): {
                        if (value instanceof String) {
                            xqDynamicContext.bindObject(name, value, null);
                        } else {
                            handleException("Incompatible type for the String");
                        }
                        break;
                    }
                    case(XQItemType.XQITEMKIND_DOCUMENT): {
                        if (value instanceof OMNode) {
                            if (isDOMRequired) {
                                xqDynamicContext.
                                        bindObject(name,
                                                new DOMSource(((Element) ElementHelper.
                                                        importOMElement((OMElement) value,
                                                                DOOMAbstractFactory.getOMFactory())).
                                                        getOwnerDocument()), null);
                            } else {
                                xqDynamicContext.bindDocument(name,
                                                              new InputSource(SynapseConfigUtils.getInputStream(
                                                                      value)));
                            }
                        }
                        break;
                    }
                    case(XQItemType.XQITEMKIND_ELEMENT): {
                        if (value instanceof OMNode) {
                            if (isDOMRequired) {
                                xqDynamicContext.
                                        bindObject(name,
                                                new DOMSource(((Element) ElementHelper.
                                                        importOMElement((OMElement) value,
                                                                DOOMAbstractFactory.getOMFactory())).
                                                        getOwnerDocument()), null);
                            } else {
                                xqDynamicContext.bindDocument(name,
                                                              new InputSource(
                                                                  SynapseConfigUtils.getInputStream(value)));
                            }
                        }
                        break;
                    }
                    case(XQItemType.XQITEMKIND_DOCUMENT_ELEMENT): {
                        if (value instanceof OMNode) {
                            if (isDOMRequired) {
                                xqDynamicContext.
                                        bindObject(name,
                                                new DOMSource(((Element) ElementHelper.
                                                        importOMElement((OMElement) value,
                                                                DOOMAbstractFactory.getOMFactory())).
                                                        getOwnerDocument()), null);
                            } else {
                                xqDynamicContext.bindDocument(name,
                                                              new InputSource(SynapseConfigUtils.getInputStream(
                                                                      value)));
                            }
                        }
                        break;
                    }
                    default: {
                        handleException("Unsupported  type for the binding type" + type +
                                        " in the variable name " + name);
                        break;
                    }
                }
            }
        }

    }

    /**
     * Return the OMNode to be used for the attached the query result. If a target XPath is not specified,
     * this will default to the first child of the SOAP body i.e. - //*:Envelope/*:Body/child::*
     *
     * @param synCtx the message context
     * @return the OMNode against which the result should be attached
     */
    public OMNode getTargetNode(MessageContext synCtx) {
        try {
            Object o = target.evaluate(synCtx.getEnvelope());
            if (o instanceof OMNode) {
                return (OMNode) o;
            } else if (o instanceof List && !((List) o).isEmpty()) {
                Object nodeObject = ((List) o).get(0); // Always fetches *only* the first
                if (nodeObject instanceof OMNode) {
                    return (OMNode) nodeObject;
                } else {
                    handleException("The evaluation of the XPath expression "
                                    + target + " must target in an OMNode");
                }
            } else {
                handleException("The evaluation of the XPath expression "
                                + target + " must target in an OMNode");
            }
        } catch (JaxenException e) {
            handleException("Error evaluating XPath " + target + " on message");
        }
        return null;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }

    public String getQuerySource() {
        return querySource;
    }

    public void setQuerySource(String querySource) {
        this.querySource = querySource;
    }

    public void addAllVariables(List list) {
        this.variables.addAll(list);
    }

    public void addVariable(MediatorVariable variable) {
        this.variables.add(variable);
    }

    public List getDataSourceProperties() {
        return dataSourceProperties;
    }

    public List getVariables() {
        return variables;
    }

    public AXIOMXPath getTarget() {
        return target;
    }

    public void setTarget(AXIOMXPath target) {
        this.target = target;
    }

    public void addAllDataSoureProperties(List list) {
        this.dataSourceProperties.addAll(list);
    }

    public boolean isDOMRequired() {
        return isDOMRequired;
    }

    public void setDOMRequired(boolean DOMRequired) {
        isDOMRequired = DOMRequired;
    }
}
