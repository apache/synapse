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


package org.apache.synapse.mediators.builtin;

import org.apache.synapse.HeaderType;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.mediators.AbstractMediator;


/**
 * @see org.apache.synapse.HeaderType
 *      <p> Sets aspects of the header to new values.
 *      Uses HeaderType to set header values
 */
public class HeaderMediator extends AbstractMediator {

    private HeaderType headerType = new HeaderType();

    private String value = null;

    public void setHeaderType(String ht) {
        headerType.setHeaderType(ht);
    }

    public String getHeaderType() {
        return headerType.getHeaderType();
    }

    public boolean mediate(SynapseMessage sm) {

        headerType.setHeader(sm, getValue());
        return true;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
