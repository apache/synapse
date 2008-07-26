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

import javax.activation.DataHandler;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

class MultipartSender extends MailSender {
    @Override
    protected void setupMessage(MimeMessage msg, DataHandler dh) throws Exception {
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart part1 = new MimeBodyPart();
        part1.setContent("This is an automated message.", "text/plain");
        multipart.addBodyPart(part1);
        MimeBodyPart part2 = new MimeBodyPart();
        part2.setDataHandler(dh);
        multipart.addBodyPart(part2);
        msg.setContent(multipart);
    }
}