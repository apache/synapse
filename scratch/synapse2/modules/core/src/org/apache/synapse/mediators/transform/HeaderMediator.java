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
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.HeaderType;

public class HeaderMediator extends AbstractMediator {

    public static final int ACTION_SET = 0;
    public static final int ACTION_REMOVE = 1;

    private String name = null;
    private String value = null;
    private int action = ACTION_SET;

    private String expression = null; //TODO handle this case later
    private HeaderType headerType = new HeaderType();

    public boolean mediate(SynapseMessage synMsg) {
        log.debug(getType() + " mediate()");
        if (action == ACTION_SET) {
            headerType.setHeader(synMsg, getValue());
            //TODO support exprns later
        } else {
            //TODO remove header later
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

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}
