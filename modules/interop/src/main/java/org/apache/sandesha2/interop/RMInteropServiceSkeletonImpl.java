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

import org.tempuri.EchoStringResponse;
import org.tempuri.EchoStringResponseBodyType;

public class RMInteropServiceSkeletonImpl extends RMInteropServiceSkeleton {

	HashMap<String, String> sequenceTextMap = new HashMap<String, String>();

	public EchoStringResponse EchoString(org.tempuri.EchoString echoString) {

		String sequence = echoString.getEchoString().getSequence();
		String text = echoString.getEchoString().getText();

		System.out.println("EchoString got text '" + text
				+ "', for the sequence '" + sequence + "'.");

		String oldText = (String) sequenceTextMap.get(sequence);
		String newText = oldText == null ? text : oldText + text;

		sequenceTextMap.put(sequence, newText);

		EchoStringResponse echoStringResponse = new EchoStringResponse();
		echoStringResponse
				.setEchoStringResponse(new EchoStringResponseBodyType());
		echoStringResponse.getEchoStringResponse().setEchoStringReturn(newText);

		return echoStringResponse;
	}

	public void Ping(org.tempuri.Ping ping) {
		String text = ping.getText();
		System.out.println("Ping got text '" + text + "'");
	}

}
