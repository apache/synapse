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
package org.apache.sandesha2.security;

import java.io.File;

import org.apache.sandesha2.scenarios.RMScenariosTest;

public class SecurityScenariosTest extends RMScenariosTest {

	public SecurityScenariosTest() {
		super("SecurityScenariosTest");
		this.repoPath = "target" + File.separator + "repos" + File.separator + "secure-server";
		this.axis2_xml = repoPath + File.separator + "server_axis2.xml";
		
		this.repoPathClient = "target" + File.separator + "repos" + File.separator + "secure-client";
		this.axis2_xmlClient = repoPathClient + File.separator + "client_axis2.xml";
	}

}
