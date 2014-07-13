
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

package org.apache.synapse.receivers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.OperationDescription;

import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.receivers.AbstractInMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is a Simple java Provider.
 */
public class Receiver extends AbstractInMessageReceiver implements
		MessageReceiver {
	/**
	 * Field log
	 */
	protected Log log = LogFactory.getLog(getClass());

	/**
	 * Field scope
	 */
	private String scope;

	/**
	 * Field method
	 */

	/**
	 * Constructor RawXMLProvider
	 */
	public Receiver() {
		scope = Constants.APPLICATION_SCOPE;
	}

	public void invokeBusinessLogic(MessageContext msgContext) throws AxisFault {
		try {

			// get the implementation class for the Web Service

			// find the WebService method

			OperationDescription op = msgContext.getOperationContext()
					.getAxisOperation();
			System.out.println(op);

			System.out.println(msgContext.getMessageID());
			System.out.println(msgContext.getSoapAction());
			System.out.println(msgContext.getTo());
			System.out.println(msgContext.getEnvelope());
		} catch (Exception e) {
			throw AxisFault.makeFault(e);
		}

	}
}
