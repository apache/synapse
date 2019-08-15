/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.eip;

import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.AxisFault;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA. User: ruwan Date: Oct 3, 2007 Time: 7:26:09 AM To change this template
 * use File | Settings | File Templates.
 */
public class SplitTestHelperMediator extends AbstractMediator implements ManagedLifecycle {

    private List mediatedContext = new ArrayList();
    int msgcount;
    String checkString;

    public boolean mediate(MessageContext synCtx) {
        synchronized(this) {
            if (msgcount == 0) {
                SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
                try {
                    synCtx.setEnvelope(envelope);
                } catch (AxisFault ignore) {
                }
            } else {
                checkString = synCtx.getEnvelope().getBody().getFirstElement().getText();
                if ("".equals(checkString)) {
                    checkString = synCtx.getEnvelope().getBody().getFirstElement().getFirstElement().getText();                    
                }
            }
            mediatedContext.add(synCtx);
            msgcount++;
            return false;
        }
    }

    public MessageContext getMediatedContext(int position) {
        if (mediatedContext.size() > position) {
            return (MessageContext) mediatedContext.get(position);
        } else {
            return null;
        }
    }

    public void clearMediatedContexts() {
        mediatedContext.clear();
        msgcount = 0;
    }

    public String getCheckString() {
        return checkString;
    }

    public void init(SynapseEnvironment se) {
        msgcount = 0;
    }

    public void destroy() {
        clearMediatedContexts();
        msgcount = 0;
    }
}
