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
package org.apache.synapse.mediators.transform;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.HeaderType;
import org.apache.synapse.Util;
import org.apache.synapse.SynapseMessageContext;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The header mediator is able to set a given value as a SOAP header, or remove a given
 * header from the current message instance. This supports the headers currently
 * supported by the HeaderType class. If an expression is supplied, its runtime value
 * is evaluated using the current message. Unless the action is set to remove, the
 * default behaviour of this mediator is to set a header value.
 *
 * @see HeaderType
 */
public class HeaderMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(HeaderMediator.class);

    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;

    /** The name of the header @see HeaderType */
    private String name = null;
    /** The literal value to be set as the header (if one was specified) */
    private String value = null;
    /** Set the header (ACTION_SET) or remove it (ACTION_REMOVE). Defaults to ACTION_SET */
    private int action = ACTION_SET;
    /** An expression which should be evaluated, and the result set as the header value */
    private AXIOMXPath expression = null;

    private HeaderType headerType = new HeaderType();

    /**
     * Sets/Removes a SOAP header on the current message
     *
     * @param synCtx the current message which is altered as necessary
     * @return true always
     */
    public boolean mediate(SynapseMessageContext synCtx) {
        log.debug(getType() + " mediate()");

        if (action == ACTION_SET) {
            headerType.setHeader(
                synCtx,
                (getValue() != null ? getValue() :
                    Util.getStringValue(getExpression(), synCtx)));

        } else {
            headerType.removeHeader(synCtx);
        }
        return true;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.headerType.setHeaderType(name);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AXIOMXPath getExpression() {
        return expression;
    }

    public void setExpression(AXIOMXPath expression) {
        this.expression = expression;
    }
}
