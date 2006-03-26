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
package org.apache.synapse.mediators;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.axiom.soap.SOAPEnvelope;

/**
 *
 *
 * <p>A sample Mediator that logs the message
 * 
 */
public class LoggerTestSample implements Mediator {

    public boolean mediate(SynapseMessage mc) {
        System.out.println("LoggerTestSample.mediate:");
        if (mc.getTo() != null && mc.getTo().getAddress() != null)
            System.out.println("LoggerTestSample.mediate to:" + mc.getTo().getAddress());
        else
            System.out.println("Empty To");
        if (mc.getReplyTo() != null && mc.getReplyTo().getAddress() != null)
            System.out.println("LoggerTestSample.mediate ReplyTo:"
                    + mc.getReplyTo().getAddress());
        else
            System.out.println("Empty ReplyTo");
        SOAPEnvelope env = mc.getEnvelope();
        System.out.println(env.toString());
        System.out.println();
        return true;
    }

}