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

package org.apache.synapse.transport.mail;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.TransportInDescription;
import org.apache.synapse.transport.testkit.TestEnvironment;
import org.apache.synapse.transport.testkit.listener.AbstractChannel;
import org.apache.synapse.transport.testkit.listener.AsyncChannel;

public class MailChannel extends AbstractChannel<TestEnvironment> implements AsyncChannel<TestEnvironment> {
    private final String address;
    
    public MailChannel(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public TransportInDescription createTransportInDescription() {
        TransportInDescription trpInDesc
            = new TransportInDescription(MailConstants.TRANSPORT_NAME);
        trpInDesc.setReceiver(new MailTransportListener());
        return trpInDesc;
    }

    @Override
    public void setupService(AxisService service) throws Exception {
        service.addParameter("transport.mail.Protocol", "test-store");
        service.addParameter("transport.mail.Address", address);
        service.addParameter("transport.PollInterval", "1");
        // TODO: logically, this should be mail.test-store.user and mail.test-store.password
        service.addParameter("mail.pop3.user", address);
        service.addParameter("mail.pop3.password", "dummy");
    }
}
