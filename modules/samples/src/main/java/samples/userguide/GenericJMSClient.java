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

package samples.userguide;

import org.apache.synapse.transport.jms.JMSConstants;
import org.apache.synapse.transport.jms.JMSUtils;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

public class GenericJMSClient {

    private static String getProperty(String name, String def) {
        String result = System.getProperty(name);
        if (result == null || result.length() == 0) {
            result = def;
        }
        return result;
    }

    public static void main(String[] args) throws Exception {

        String dest  = getProperty("jms_dest", "dynamicQueues/JMSTextProxy");
        String type  = getProperty("jms_type", "text");
        String param = getProperty("jms_payload",
            getRandom(100, 0.9, true) + " " + (int) getRandom(10000, 1.0, true) + " IBM");

        GenericJMSClient app = new GenericJMSClient();
        if ("text".equalsIgnoreCase(type)) {
            app.sendTextMessage(dest, param);
        } else if ("binary".equalsIgnoreCase(type)) {
            app.sendBytesMessage(dest, getBytesFromFile(param));
        } else if ("pox".equalsIgnoreCase(type)) {
            app.sendTextMessage(dest, 
                "<m:placeOrder xmlns:m=\"http://services.samples/xsd\">\n" +
                "    <m:order>\n" +
                "        <m:price>" + getRandom(100, 0.9, true) + "</m:price>\n" +
                "        <m:quantity>" + (int) getRandom(10000, 1.0, true) + "</m:quantity>\n" +
                "        <m:symbol>" + param + "</m:symbol>\n" +
                "    </m:order>\n" +
                "</m:placeOrder>");
        } else {
            System.out.println("Unknown JMS message type");
        }
    }

    private void sendBytesMessage(String destName, byte[] payload) throws Exception {
        InitialContext ic = getInitialContext();
        ConnectionFactory confac = (ConnectionFactory) ic.lookup("ConnectionFactory");
        Connection connection = JMSUtils.createConnection(
            confac, null, null, JMSConstants.DESTINATION_TYPE_QUEUE);
        Session session = JMSUtils.createSession(
            connection, false, Session.AUTO_ACKNOWLEDGE, JMSConstants.DESTINATION_TYPE_QUEUE);

        BytesMessage bm = session.createBytesMessage();
        bm.writeBytes(payload);
        JMSUtils.sendMessageToJMSDestination(session, (Destination) ic.lookup(destName),
                JMSConstants.DESTINATION_TYPE_QUEUE,  bm);
        connection.close();
    }

    private void sendTextMessage(String destName, String payload) throws Exception {
        InitialContext ic = getInitialContext();
        ConnectionFactory confac = (ConnectionFactory) ic.lookup("ConnectionFactory");
        Connection connection = JMSUtils.createConnection(
            confac, null, null, JMSConstants.DESTINATION_TYPE_QUEUE);
        Session session = JMSUtils.createSession(
            connection, false, Session.AUTO_ACKNOWLEDGE, JMSConstants.DESTINATION_TYPE_QUEUE);

        TextMessage tm = session.createTextMessage(payload);
        JMSUtils.sendMessageToJMSDestination(session, (Destination) ic.lookup(destName),
                JMSConstants.DESTINATION_TYPE_QUEUE, tm);
        connection.close();
    }

    private InitialContext getInitialContext() throws NamingException {
        Properties env = new Properties();
        if (System.getProperty("java.naming.provider.url") == null) {
            env.put("java.naming.provider.url", "tcp://localhost:61616");
        }
        if (System.getProperty("java.naming.factory.initial") == null) {
            env.put("java.naming.factory.initial",
                "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        }
        return new InitialContext(env);
    }

    public static byte[] getBytesFromFile(String fileName) throws IOException {

        File file = new File(fileName);
        InputStream is = new FileInputStream(file);
        long length = file.length();

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
            && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
    }

    private static double getRandom(double base, double varience, boolean onlypositive) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * varience * base * rand))
            * (onlypositive ? 1 : (rand > 0.5 ? 1 : -1));
    }

}
