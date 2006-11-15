Running the User Guide samples
==============================

To run these samples, please use Ant 1.5 or above. Ant can be downloaded from 
http://ant.apache.org

For help on running these samples try
> ant help

To start Synapse with the default configuration execute <SYNAPSE>\bin\synapse.bat 
(on Windows and .sh on Unix). This starts up an instance of Synapse using the Synapse 
and Axis2 configuration files found at <SYNAPSE>\synapse_repository\conf. 

Specific sample configurations could be started with the command
<SYNAPSE>\bin\synapse.bat -sample <number> or the equivalent in Unix, which will pick 
up the Synapse configuration file to be used from 
<SYNAPSE>\synapse_repository\conf\sample\synapse_sample_<number>.xml

These samples are based on the WebserviceX and invesbot stock quote services, and the client
programs sends a stock quote request which maybe already in the standard format as required by
these services, or a custom format. In the examples the request is simply forwarded, transformed
and forwarded, validated & transformed and then forwarded to the actual services using the 
different configurations.


The Samples are avilable in three categories

1. 	The samples.userguide package contains the samples from Synapse M1, and these should be 
		tested against the equivalent M2 Synapse configurations - default or sample 0. i.e. by
		executing <SYNAPSE>\bin\synapse.bat or <SYNAPSE>\bin\synapse.bat -sample 0 or equivalents
		under Unix
		
		StockQuoteClient (ant stockquote)
			- Sends a stock quote request to the WebserviceX stock quote service by
			specifying the EPR to WebServiceX, but the transport URL to Synapse.
					
		ProxyStockQuoteClient (ant proxystockquote)
			- Sends the same stock quote request using the HTTP proxy model.
			There is no WS-Addressing To URL but we set the HTTP proxy URL to point to Synapse. This 
			results in the destination XMethods URL being embedded in the POST header. Synapse will 
			pick this out and use it to direct the message
		
		DumbStockQuoteClient (ant dumbstockquote)
			-Sends the same stock quote request to XMethods stockquote service. There is no EPR and 
			there is no proxy config. It's sort of a Gateway case. It relies on a Synapse config that 
			will look at the URL or message and send it to the right place
		
2. 	The samples.mediation package contains samples which illustrate and showcase the different 
		mediators and the new configuration language syntax.
		
		CustomStockQuoteClient (ant customquote)
			- Synapse server should be started with <SYNAPSE>\bin\synapse.bat -sample 1  or equivanlent 
			under Unix to run this sample. This sample shows the introduction of support to handle custom 
			stock quote requests to the previous configuration used in the examples of section 1. 

			- The configuration used in this sample transforms the custom request messages into the format 
			understood by the actual services. It also shows the usage of the <in> and <out> mediators and 
			support for the correlation of messgaes, so that when a standard stock quote response is received, 
			Synapse knows how to mediate this response back to the correct client - i.e. as is or performing a 
			transformation back to a custom format. 

			- Depending on which JDK you use, you will have to setup Xerces 2.8.0 such that the JDK will 
			properly pick it up (i.e. xml-apis.jar and xercesImpl.jar). Usually this could be accomplished by 
			placing the Xerces JAR's into the <JAVA_HOME>\lib\endorsed directory of the JDK (or JRE). You could 
			also place Xerces jars into <SYNAPSE>\lib\endorsed directory without altering your JDK/JRE. In 
			addition the synapse-extensions.jar should be placed into the <SYNAPSE>\lib folder so that the 
			extensions are properly picked up. As the Spring extension and Transform mediator is bundled with 
			the extensions distribution you will also need the Spring 1.2.8 JAR file(spring.jar) and Xalan JAR 
			files (xalan.jar and serializer.jar) placed into the <SYNAPSE>\lib 

			
		AdvancedQuoteClient (ant advancedquote)
			- Synapse server should be started with <SYNAPSE>\bin\synapse.bat -sample 2 or equivanlent
			under Unix to run this sample. This sample shows the validation mediator extension. The validation
			mediator is kept outside of the core Synapse distribution as it relies on the Xerces parser. 
			
			- Depending on which JDK you use, you will have to setup Xerces 2.8.0 such that the JDK will 
			properly pick it up (i.e. xml-apis.jar and xercesImpl.jar). Usually this could be accomplished by 
			placing the Xerces JAR's into the <JAVA_HOME>\lib\endorsed directory of the JDK (or JRE). You could 
			also place Xerces jars into <SYNAPSE>\lib\endorsed directory without altering your JDK/JRE. In 
			addition the synapse-extensions.jar should be placed into the <SYNAPSE>\lib folder so that the 
			extensions are properly picked up. As the Spring extension and Transform mediator is bundled with 
			the extensions distribution you will also need the Spring 1.2.8 JAR file(spring.jar) and Xalan JAR 
			files (xalan.jar and serializer.jar) placed into the <SYNAPSE>\lib
		
3.	The samples.config package contains a sample which shows how a custom Synapse/Axis2 instance could be 
		started	up and configured. The examples demonstrates that the SynapseConfiguration to be used could be
		created programatically as well, and thus not dependent on a XML configuration file.
		
		To start up this server, use the custom.bat or equivalent file. To run the simple client to test this 
		configuration run the SimpleStockQuoteClient example from Ant. The programatically created Synapse 
		configuration simply sends the messages coming into Synapse using to thier implicit destinations. 
		i.e. Using WS-A To address. This excercise will require you to place the Spring.jar file into the <SYNAPSE>\lib
		directory.
		
		The test client can be started by ant simplequote

Thanks
The Synapse team