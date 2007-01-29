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
