/*
 * Copyright 2007 The Apache Software Foundation.
 * Copyright 2007 International Business Machines Corp.
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
package org.apache.sandesha2.scenarios;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.client.SandeshaClientConstants;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.util.SandeshaUtil;

public class UnsupportedScenariosTest extends RMScenariosTest {

	public UnsupportedScenariosTest () {
		super ("UnsupportedScenariosTest");
	}
	
	public void setUp () throws Exception {
		super.setUp();
		
		// Override the config to disable make connection
		SandeshaPolicyBean config = SandeshaUtil.getDefaultPropertyBean(configContext.getAxisConfiguration());
		config.setEnableMakeConnection(false);
	}
	
	public void test2WayWithoutMakeConnectionFails() throws Exception  {
		// Run a ping test with sync acks - this should work even though MakeConnection is disabled
		runPing(false, false);
		
		// Run an echo test with sync acks - this should fail as MakeConnection is disabled
		AxisFault fault = null;
		Options clientOptions = new Options();
		clientOptions.setProperty(SandeshaClientConstants.RM_SPEC_VERSION,Sandesha2Constants.SPEC_VERSIONS.v1_1);
		try {
			runEcho(clientOptions, false, false, false, false, true);
		} catch (AxisFault e) {
			fault = e;
			System.out.println("Caught expected fault: " + e);
		}
		assertNotNull("Expected fault", fault);
	}

	//
	// Dummy test methods to stop us invoking the parent methods
	//
	public void testAsyncEcho() throws Exception {
		// Do nothing
	}
	public void testPing() throws Exception {
		// Do nothing
	}
	public void testSyncEcho() throws Exception {
		// Do nothing
	}

}
