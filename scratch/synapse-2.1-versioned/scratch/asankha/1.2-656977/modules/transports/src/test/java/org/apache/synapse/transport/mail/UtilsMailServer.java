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

import org.apache.synapse.transport.UtilsTransportServer;
import org.apache.synapse.transport.mail.MailTransportListener;
import org.apache.synapse.transport.mail.MailTransportSender;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.Parameter;

import java.util.List;
import java.util.ArrayList;


public class UtilsMailServer extends UtilsTransportServer {

    public void start() throws Exception {

        TransportOutDescription trpOutDesc =
            new TransportOutDescription(MailConstants.TRANSPORT_NAME);
        // gmail
        trpOutDesc.addParameter(new Parameter("mail.smtp.class", TestTransport.class.getName()));
        trpOutDesc.addParameter(new Parameter("mail.smtp.host", "smtp.gmail.com"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.port", "587"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.starttls.enable", "true"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.user", "synapse.test.1"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.from", "synapse.test.1@gmail.com"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.password", "mailpassword"));
        trpOutDesc.addParameter(new Parameter("mail.smtp.auth", "true"));

        trpOutDesc.setSender(new MailTransportSender());

        TransportInDescription trpInDesc =
            new TransportInDescription(MailConstants.TRANSPORT_NAME);
        trpInDesc.setReceiver(new MailTransportListener());
        super.start(trpInDesc, trpOutDesc);

        // Service1 - polls synapse.test.6@gmail.com using POP3/SSL, and writes the response to
        // synapse.test.1@gmail.com and deletes request on success. Polls every 5 secs
        List parameters = new ArrayList();
        //gmail
        parameters.add(new Parameter("transport.mail.Address", "synapse.test.6@gmail.com"));
        // local james
        parameters.add(new Parameter("transport.mail.Address", "synapse.test.6@localhost"));
        //parameters.add(new Parameter("transport.mail.ReplyAddress", "synapse.test.1@gmail.com"));
        parameters.add(new Parameter("transport.mail.Protocol", "test-store"));
        //parameters.add(new Parameter("transport.mail.ContentType", "text/xml"));
        parameters.add(new Parameter("transport.mail.ActionAfterProcess", "DELETE"));
        parameters.add(new Parameter("transport.PollInterval", "1"));

        // gmail
        parameters.add(new Parameter("mail.pop3.host", "pop.gmail.com"));
        parameters.add(new Parameter("mail.pop3.port", "995"));
        parameters.add(new Parameter("mail.pop3.user", "synapse.test.6"));
        parameters.add(new Parameter("mail.pop3.password", "mailpassword"));

        parameters.add(new Parameter("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory"));
        parameters.add(new Parameter("mail.pop3.socketFactory.fallback", "false"));
        parameters.add(new Parameter("mail.pop3.socketFactory.port", "995"));

        deployEchoService("Service1", parameters);

        // Service2 - polls synapse.test.7@gmail.com using IMAP/SSL, and writes the response to
        // synapse.test.1@gmail.com and deletes request on success. Polls every 5 secs
        parameters = new ArrayList();
        parameters.add(new Parameter("transport.mail.Address", "synapse.test.7@gmail.com"));
        //parameters.add(new Parameter("transport.mail.ReplyAddress", "synapse.test.1@gmail.com"));
        parameters.add(new Parameter("transport.mail.Protocol", "test-store"));
        //parameters.add(new Parameter("transport.mail.ContentType", "text/xml"));
        //parameters.add(new Parameter("transport.mail.ActionAfterProcess", "DELETE"));
        parameters.add(new Parameter("transport.PollInterval", "1"));

        parameters.add(new Parameter("mail.imap.host", "imap.gmail.com"));
        parameters.add(new Parameter("mail.imap.port", "993"));
        parameters.add(new Parameter("mail.imap.user", "synapse.test.7"));
        parameters.add(new Parameter("mail.imap.password", "mailpassword"));

        parameters.add(new Parameter("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory"));
        parameters.add(new Parameter("mail.imap.socketFactory.fallback", "false"));
        parameters.add(new Parameter("mail.imap.socketFactory.port", "993"));

        deployEchoService("Service2", parameters);
    }

}
