/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.sandesha2.interop;

import org.tempuri.EchoStringResponse;

public class RMInteropServiceCallbackHandlerImpl extends
		RMInteropServiceCallbackHandler {

	private boolean completed = false;

	public RMInteropServiceCallbackHandlerImpl(Object clientData) {
		super(clientData);
	}

	public Object getClientData() {
		// TODO Auto-generated method stub
		return super.getClientData();
	}

	public void receiveErrorEchoString(Exception e) {
		System.out.println("EchoString callback got error:");
		e.printStackTrace();
		completed = true;
	}

	public void receiveResultEchoString(EchoStringResponse echoStringResponse) {
		String text = echoStringResponse.getEchoStringResponse()
				.getEchoStringReturn();
		System.out.println("EchoSting callback got text:" + text);
		completed = true;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

}
