/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.policy;


public class RMPolicy {

	public static final RMPolicyToken retransmissionIterval = new RMPolicyToken(
			"RetransmissionInterval", RMPolicyToken.SIMPLE_TOKEN,
			new String[] { "Milliseconds" });

	public static final RMPolicyToken acknowledgementInterval = new RMPolicyToken(
			"AcknowledgementInterval", RMPolicyToken.SIMPLE_TOKEN,
			new String[] {});
	
	public static final RMPolicyToken maximumRetransmissionCount = new RMPolicyToken(
			"MaximumRetransmissionCount", RMPolicyToken.SIMPLE_TOKEN,
			new String[] {});

	public static final RMPolicyToken exponentialBackoff = new RMPolicyToken(
			"ExponentialBackoff", RMPolicyToken.SIMPLE_TOKEN, new String[] {});

	public static final RMPolicyToken inactiveTimeout = new RMPolicyToken(
			"InactivityTimeout", RMPolicyToken.SIMPLE_TOKEN, new String[] {});

	public static final RMPolicyToken inactiveTimeoutMeasure = new RMPolicyToken(
			"InactivityTimeoutMeasure", RMPolicyToken.SIMPLE_TOKEN,
			new String[] {});

	public static final RMPolicyToken invokeInOrder = new RMPolicyToken(
			"InvokeInOrder", RMPolicyToken.SIMPLE_TOKEN, new String[] {});
	
	public static final RMPolicyToken messageTypeToDrop = new RMPolicyToken( 
			"MessageTypesToDrop", RMPolicyToken.SIMPLE_TOKEN, new String[] {});
	
	public static final RMPolicyToken storageManagers = new RMPolicyToken(
			"StorageManagers", RMPolicyToken.COMPLEX_TOKEN, new String[] {});
	
	public static final RMPolicyToken inMemoryStorageManager = new RMPolicyToken(
			"InMemoryStorageManager", RMPolicyToken.SIMPLE_TOKEN, new String[]{});
	
	public static final RMPolicyToken permanentStorageManager = new RMPolicyToken(
			"PermanentStorageManager", RMPolicyToken.SIMPLE_TOKEN, new String[]{});
	
//	public static final RMPolicyToken storageManager = new RMPolicyToken(
//			"StorageManager", RMPolicyToken.SIMPLE_TOKEN, new String[]{});
}
