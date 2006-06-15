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

package sandesha2.samples.simpleServer;

import java.io.File;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;

public class SimpleSandesha2Server {

	private static String SANDESHA2_HOME = "<SANDESHA2_HOME>"; //Change this to ur path.
	
	private static String AXIS2_SERVER_PATH = SANDESHA2_HOME + File.separator + "target" + File.separator +"repos" + File.separator + "server" + File.separator;   //this will be available after a maven build
	
	public static void main(String[] args) throws AxisFault {

		String axisServerRepo = null;
		if (args!=null && args.length>0)
			axisServerRepo = args[0];
		
		if (axisServerRepo!=null && !"".equals(axisServerRepo)) {
			AXIS2_SERVER_PATH = axisServerRepo;
		}

		if ("<SANDESHA2_HOME>".equals(SANDESHA2_HOME)){
			System.out.println("ERROR: Please change <SANDESHA2_HOME> to your Sandesha2 installation directory.");
			return;
		}
		
		System.out.println("Starting sandesha2 server...");
		
		String axis2_xml = AXIS2_SERVER_PATH + "server_axis2.xml";
		ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(AXIS2_SERVER_PATH,axis2_xml);
		
		SimpleHTTPServer server = new SimpleHTTPServer  (configContext,8080);
		
		server.start();
		
	}
}
