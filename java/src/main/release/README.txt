Apache Synapse 1.1 build  (November 2007) - http://ws.apache.org/synapse/
------------------------------------------------------------------------------------------

-------------------
First Steps
===================

Once you extract the downloaded binary distribution, it will create the following directory
structure.

	synapse
		/bin
			synapse.sh
			synapse-daemon.sh
			synapse.bat
			run-synapse-service.bat
			install-synapse-service.bat
			uninstall-synapse-service.bat
		/docs
			<documentation>
		/lib
			<libraries>
			/patches
			    <any patches to be applied>
			/endorsed
			    <any endorsed JARs>
			trust.jks
			identity.jks
			log4j.properties
			providers.xml
		/repository
			/conf
				synapse.xml
				axis2.xml
				wrapper.conf
			    /sample
				    <sample configuration files>
				    /resources
					    <sample resources>
			/modules
		/samples
			/axis2Client
				<ant script to run sample clients>
			/axis2Server
				axis2Server.sh
				axis2Server.bat
				/src/
					<sample services source>


You could start Synapse using the bin/synapse.sh or bin/synapse.bat script, which will load 
the configuration found in repository/conf/synapse.xml. To configure the underlying Axis2
SOAP engine (e.g. to enable JMS) you need to configure the repository/conf/axis2.xml. To 
configure logging levels and to turn on/off debug level logging, please configure the 
lib/log4j.properties file, and set the line "log4j.category.org.apache.synapse=INFO" as
"log4j.category.org.apache.synapse=DEBUG" to turn on debug logging.

-------------------
Documentation
===================
 
Documentation can be found in the 'docs' directory included with the binary distribution 
and in the 'src/site/resources' directory in the source distribution. 

For Synapse mediation samples please see the Synapse_Quickstart.html, Synapse_Samples.html 
and Synapse_Samples_Setup.html

For more information on the Synapse Configuration language syntax and usage refer to
Synapse_Configuration_Language.html

-------------------
Getting Started
===================

Refer to the Synapse_Quickstart.html document to get started with Synapse in just a couple of minutes.

More indepth samples could be found in Synapse_Samples_Setup.html and Synapse_Samples.html found in
the docs directory.

The actual sample Synapse configurations could be found at <SYNAPSE>/respository/conf/sample.
The resources sub-directory contains the sample XSLT transformations, XSD schemas, WS policies
and all other resources required to demonstrate various aspects of Synapse.

-------------------
Support
===================

Please refer to the release_notes.txt file for information on common issues and the solutions.

Any issues with this release can be reported to Apache Synapse mailing list or in the JIRA issue tracker.

Mailing list subscription:
    synapse-dev-subscribe@ws.apache.org
    synapse-user-subscribe@ws.apache.org

Jira:
    http://issues.apache.org/jira/browse/Synapse

Thank you for using Synapse!
The Synapse Team. 

