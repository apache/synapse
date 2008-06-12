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
package org.apache.synapse.format;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.transport.base.BaseConstants;
import org.apache.synapse.transport.base.BaseUtils;

/**
 * Message builder for binary payloads.
 * <p>
 * This builder processes the input message as binary and wraps
 * the data in a wrapper element. The name of the wrapper element can
 * be configured as a service parameter (see {@link BaseConstants#WRAPPER_PARAM}).
 * It defaults to {@link BaseConstants#DEFAULT_BINARY_WRAPPER}.
 */
public class BinaryBuilder implements Builder {
    public OMElement processDocument(InputStream inputStream,
                                     String contentType,
                                     MessageContext msgContext) throws AxisFault {
        QName wrapperQName = BaseConstants.DEFAULT_BINARY_WRAPPER;
        if (msgContext.getAxisService() != null) {
            Parameter wrapperParam = msgContext.getAxisService().getParameter(BaseConstants.WRAPPER_PARAM);
            if (wrapperParam != null) {
                wrapperQName = BaseUtils.getQNameFromString(wrapperParam.getValue());
            }
        }
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement wrapper = factory.createOMElement(wrapperQName, null);
        byte[] msgBytes;
        try {
            msgBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new AxisFault("Unable to read message payload", ex);
        }
        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(msgBytes));
        wrapper.addChild(factory.createOMText(dataHandler, true));
        msgContext.setDoingMTOM(true);
        return wrapper;
    }
}
