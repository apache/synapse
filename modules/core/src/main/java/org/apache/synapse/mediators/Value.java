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

package org.apache.synapse.mediators;

import org.apache.axiom.om.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;
import org.jaxen.expr.BinaryExpr;
import org.jaxen.expr.Expr;
import org.jaxen.expr.FilterExpr;
import org.jaxen.expr.FunctionCallExpr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.PathExpr;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.Step;
import org.jaxen.expr.UnaryExpr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Represents a Value
 * Handling both static and dynamic(Xpath) keys.
 * User can give Xpath expression as a key and derive
 * real key based on message context
 */
public class Value {
    private static final Log log = LogFactory.getLog(Value.class);

    /**
     * Functions that a re-compiled dynamic ("{{...}}") template parameter expression is not
     * allowed to call. document() resolves an external resource and evaluate() compiles and
     * runs a further expression at runtime; neither is evaluated for an expression that was
     * itself produced from a runtime-derived value. Such a call is reported as an error.
     */
    private static final Set<String> RESTRICTED_DYNAMIC_FUNCTIONS =
            Collections.unmodifiableSet(
                    new HashSet<String>(Arrays.asList("document", "evaluate")));

    /**
     * Name of the value attribute
     */
    private String name = null;

    /**
     * The static key value 
     */
    private String keyValue = null;
    /**
     * the dynamic key
     */
    private SynapseXPath expression = null;

    private List<OMNamespace> namespaceList = new ArrayList<OMNamespace>();
    /**
     * Create a key instance using a static key
     *
     * @param staticKey static key
     */
    public Value(String staticKey) {
        this.keyValue = staticKey;
    }

    /**
     * Create a key instance using a dynamic key (Xpath Expression)
     *
     * @param expression SynapseXpath for dynamic key
     */
    public Value(SynapseXPath expression) {
        this.expression = expression;
    }

    /**
     * Retrieving static key
     *
     * @return static key
     */
    public String getKeyValue() {
        return keyValue;
    }

    /**
     * Retrieving dynamic key
     *
     * @return SynapseXpath
     */
    public SynapseXPath getExpression() {
        if(expression == null && keyValue != null && hasExprTypeKey()){
            try {
                expression = new SynapseXPath (keyValue.substring(1, keyValue.length()-1));
                for (OMNamespace aNamespaceList : namespaceList) {
                    expression.addNamespace(aNamespaceList);
                }
            } catch (JaxenException e) {
                expression = null;
                handleException("Can not evaluate escaped expression..");

            }
        }
        return expression;
    }

    /**
     * Evaluating key based on message context
     * used when key is a xpath expression
     *
     * @param synCtx message context
     * @return string value of evaluated key
     */
    public String evaluateValue(MessageContext synCtx) {
        if (keyValue != null) {
            //if static kry: return static key
            return keyValue;
        } else if (expression != null) {
            //if dynamic key return evaluated value
            return expression.stringValueOf(synCtx);
        } else {
            handleException("Can not evaluate the key: " +
                            "key should be static or dynamic key");
            return null;
        }

    }

    public Object evaluateObjectValue(MessageContext synCtx) {
        if (keyValue != null) {
            return keyValue;
        } else if (expression != null) {
            return getObjectValue(synCtx);
        } else {
            handleException("Unable to resolve the value: " + toString());
            return null;
        }
    }

    private Object getObjectValue(MessageContext synCtx) {
        try {
            Object result = expression.selectSingleNode(synCtx);

            if (result instanceof OMText) {
                return ((OMText) result).getText();
            } else if (result instanceof OMElement) {
                return ((OMElement) result).getText();
            } else if (result instanceof OMDocument) {
                return ((OMDocument) result).getOMDocumentElement().toString();
            } else if (result instanceof OMAttribute) {
                return ((OMAttribute) result).getAttributeValue();
            } else {
                return result;
            }
        } catch (JaxenException e) {
            handleException("Failed to evaluate the XPath expression: " + expression, e);
        }
        return null;
    }

    /**
     * Get the name of the value attribute
     *
     * @return name of the value attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Handle exceptions
     *
     * @param msg error message
     */
    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * checks whether key returned by #getKeyValue() is a string of an expression type.
     * @return if true if this is an expression
     */
    public boolean hasExprTypeKey() {
        return keyValue != null && keyValue.startsWith("{") && keyValue.endsWith("}");
    }
    
    public boolean hasPropertyEvaluateExpr(){
    	return keyValue != null && keyValue.contains("get-property");
    }

    public Object evalutePropertyExpression(MessageContext synCtx) {
        SynapseXPath expr = null;
        SynapseXPath expression;
        try {
            expr = new SynapseXPath(this.keyValue.substring(1, keyValue.length() - 1));
            for (OMNamespace aNamespaceList : namespaceList) {
                expr.addNamespace(aNamespaceList);
            }

            String result = expr.stringValueOf(synCtx);
            expression = new SynapseXPath(result);
            for (OMNamespace aNamespaceList : namespaceList) {
                expression.addNamespace(aNamespaceList);
            }
        } catch (Exception e) {
            handleException("Can not evaluate escaped expression : " + expr);
            return this.expression;
        }

        String restrictedFunction = findRestrictedFunction(expression.getRootExpr());
        if (restrictedFunction != null) {
            handleException("The function '" + restrictedFunction + "' is not allowed in a "
                    + "dynamically evaluated template parameter expression");
        }
        return expression;
    }

    /**
     * Walks a parsed XPath expression tree and returns the name of the first function call
     * that is in {@link #RESTRICTED_DYNAMIC_FUNCTIONS}, or {@code null} if there is none. The
     * whole tree is inspected, including function arguments and step predicates.
     *
     * @param node a node of the parsed expression tree (an {@link Expr}, {@link Step} or
     *             {@link Predicate}); may be {@code null}
     * @return the restricted function name if one is present, otherwise {@code null}
     */
    private String findRestrictedFunction(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof FunctionCallExpr) {
            FunctionCallExpr call = (FunctionCallExpr) node;
            String functionName = call.getFunctionName();
            if (functionName != null && RESTRICTED_DYNAMIC_FUNCTIONS.contains(functionName)) {
                return functionName;
            }
            return findRestrictedFunction(call.getParameters());
        }
        if (node instanceof PathExpr) {
            PathExpr path = (PathExpr) node;
            String found = findRestrictedFunction(path.getFilterExpr());
            return found != null ? found : findRestrictedFunction(path.getLocationPath());
        }
        if (node instanceof FilterExpr) {
            FilterExpr filter = (FilterExpr) node;
            String found = findRestrictedFunction(filter.getExpr());
            return found != null ? found : findRestrictedFunction(filter.getPredicates());
        }
        if (node instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) node;
            String found = findRestrictedFunction(binary.getLHS());
            return found != null ? found : findRestrictedFunction(binary.getRHS());
        }
        if (node instanceof UnaryExpr) {
            return findRestrictedFunction(((UnaryExpr) node).getExpr());
        }
        if (node instanceof LocationPath) {
            return findRestrictedFunction(((LocationPath) node).getSteps());
        }
        if (node instanceof Step) {
            return findRestrictedFunction(((Step) node).getPredicates());
        }
        if (node instanceof Predicate) {
            return findRestrictedFunction(((Predicate) node).getExpr());
        }
        if (node instanceof List) {
            for (Object child : (List<?>) node) {
                String found = findRestrictedFunction(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    public void setNamespaces(OMElement elem){
        Iterator namespaces = elem.getNamespacesInScope();
        while (namespaces.hasNext()){
            OMNamespace ns = (OMNamespace) namespaces.next();
            namespaceList.add(ns);
        }
    }

    @Override
    public String toString() {
        return "Value {" +
                "name ='" + name + '\'' +
                (keyValue != null ? ", keyValue ='" + keyValue + '\'' : "") +
                (expression != null ? ", expression =" + expression : "") +
                '}';
    }

}


