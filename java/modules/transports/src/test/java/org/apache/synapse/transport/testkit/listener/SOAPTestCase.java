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

package org.apache.synapse.transport.testkit.listener;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;

public abstract class SOAPTestCase extends XMLMessageTestCase {
    public SOAPTestCase(ListenerTestSetup strategy, XMLMessageSender sender, String baseName, ContentTypeMode contentTypeMode, String baseContentType, MessageTestData data) {
        super(strategy, sender, baseName, contentTypeMode, baseContentType, data);
    }

    @Override
    protected abstract SOAPFactory getOMFactory();

    @Override
    protected OMElement getMessage(OMElement payload) {
        SOAPEnvelope envelope = ((SOAPFactory)factory).createSOAPEnvelope();
        SOAPBody body = ((SOAPFactory)factory).createSOAPBody();
        body.addChild(payload);
        envelope.addChild(body);
        return envelope;
    }
}