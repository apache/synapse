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
package org.apache.synapse.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SOAPMessageContext;
import org.apache.axis2.engine.MessageReceiver;


import org.apache.synapse.SynapseEnvironment;

public class SynapseMessageReceiver implements MessageReceiver {
	
	
	public void receive(MessageContext mc) throws AxisFault {
		System.out.println("dispatching message");
		SynapseEnvironment env = Axis2SynapseEnvironmentFinder.getSynapseEnvironment(mc);
		env.injectMessage((SOAPMessageContext)mc);
	}
}
