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
import org.apache.axis2.clientapi.AsyncResult;
import org.apache.axis2.clientapi.Callback;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.synapse.SynapseException;

public class ResponseCallback extends Callback {
		private MessageContext mc = null;
		public ResponseCallback(MessageContext mc) {
			this.mc = mc;
		}
		
		public void onComplete(AsyncResult result) {
			
			try {
				AxisEngine ae = new AxisEngine(mc.getSystemContext());
				ae.receive(result.getResponseMessageContext());
				ae.send(result.getResponseMessageContext());
			} catch (AxisFault e) {
				e.printStackTrace();
				throw new SynapseException(e);
			}
		}

		public void reportError(Exception e) {
			e.printStackTrace();
		}

	
}
