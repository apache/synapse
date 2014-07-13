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

package org.apache.synapse.core.axis2;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.handlers.AbstractHandler;

import java.util.Iterator;
/*
 * 
 */

public class SynapseMustUnderstandHandler extends AbstractHandler {

    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        Object obj = msgContext.getProperty(
                org.apache.synapse.Constants.PROCESSED_MUST_UNDERSTAND);
        if (obj != null && ((Boolean) obj).booleanValue()) {
            // second phase so return
            return InvocationResponse.CONTINUE;
        }
        msgContext.setProperty(
                org.apache.synapse.Constants.PROCESSED_MUST_UNDERSTAND,
                Boolean.TRUE);
        if (!msgContext.isHeaderPresent()) {
            return InvocationResponse.CONTINUE;
        }
        SOAPEnvelope envelope = msgContext.getEnvelope();
        if (envelope.getHeader() == null) {
            return InvocationResponse.CONTINUE;
        }
        Iterator headerBlocks = envelope.getHeader().examineAllHeaderBlocks();
        while (headerBlocks.hasNext()) {
            SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) headerBlocks.next();
            // if this header block has been processed or mustUnderstand isn't
            // turned on then its cool
            if (headerBlock.isProcessed() || !headerBlock.getMustUnderstand()) {
                continue;
            }
            headerBlock.setProcessed();

        }
        return InvocationResponse.CONTINUE;
    }
}
