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

import java.util.HashMap;

import org.tempuri.EchoStringRequest;
import org.tempuri.EchoStringResponse;
import org.tempuri.PingRequest;

public class RMInteropServiceSkeletonImpl extends RMInteropServiceSkeleton {

	HashMap sequenceTextMap = new HashMap ();
	
	public EchoStringResponse echoString(EchoStringRequest echoStringRequest) {
		String sequence = echoStringRequest.getEchoString().getSequence();
		String text = echoStringRequest.getEchoString().getText();
		
		System.out.println("EchoString got text '" + text + "', for the sequence '" + sequence + "'.") ;
		
		String oldText = (String) sequenceTextMap.get(sequence);
		String newText = oldText==null?text:oldText+text;
		sequenceTextMap.put(sequence, newText);
		
		EchoStringResponse echoStringResponse = new EchoStringResponse ();
		echoStringResponse.getEchoStringResponse().setEchoStringReturn(newText);
		
		return echoStringResponse;
	}

	public EchoStringResponse EchoString(EchoStringRequest echoStringRequest) {
		return echoString(echoStringRequest);
	}

	public void ping(PingRequest pingRequest) {
		String text = pingRequest.getText();
		System.out.println("Ping got text '" + text + "'");
	}

	public void Ping(PingRequest pingRequest) {
		ping(pingRequest);
	}

}
