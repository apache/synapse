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
package org.apache.synapse.core;

import org.apache.synapse.SynapseMessageContext;

/**
 * The SynapseEnvironment allows access into the the host SOAP engine. It allows
 * the sending of messages, classloader access etc.
 */
public interface SynapseEnvironment {

    /**
     * This method injects a new message into the Synapse engine. This is used by
     * the underlying SOAP engine to inject messages into Synapse for mediation.
     * e.g. The SynapseMessageReceiver used by Axis2 invokes this to inject new messages
     */
    public void injectMessage(SynapseMessageContext smc);

    /**
     * Mediators may get access to the relevant classloader through this
     */
    public ClassLoader getClassLoader();

    /**
     * This method allows a message to be sent through the underlying SOAP engine.
     * <p/>
     * This will send request messages on (forward), and send the response messages back to the client
     */
    public void send(SynapseMessageContext smc);
}
