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
package logbean;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.synapse.mediator.Mediator;
import org.apache.synapse.mediator.MediatorException;

/**
 * TODO: add comment
 */
public class Logger implements Mediator {

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.synapse.mediator.Mediator#mediate(org.apache.axis2.context.MessageContext)
     */
    public boolean mediate(MessageContext mc) throws MediatorException {
        System.out.println("Logger.mediate:");
        if (mc.getTo()!=null && mc.getTo().getAddress()!=null) System.out.println("Logger.mediate to:" + mc.getTo().getAddress());
        else System.out.println("Empty To");
        if (mc.getReplyTo()!=null && mc.getReplyTo().getAddress()!=null) System.out.println("Logger.mediate ReplyTo:" + mc.getReplyTo().getAddress());
        else System.out.println("Empty ReplyTo");

        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = xof.createXMLStreamWriter(System.out);
            SOAPEnvelope env = mc.getEnvelope();
            env.serialize(writer);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
        return true;
    }

}