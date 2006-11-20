Running the Synapse Samples
===========================

Overview
To run the samples bundled with the Synapse distribution you will need Ant 1.5 or later.
Ant can be downloaded from http://ant.apache.org. The samples now come with a built in 
Axis2 server to host the service endpoints used in the samples, along with simple ant 
scripts to build and deploy the services into this Axis2 instance.

Starting Axis2
To start the bundled Axis2 server, go to the samples/axis2Server directory and execute the
axis2server.sh or axis2server.bat script. This starts the Axis2 server with the HTTP 
transport listner on port 9000.

The sample services are available in the samples/axis2Server/src directory. To deploy these
services go the the selected directory and run 'ant'. This will build the service archive 
file (aar) and deploy it into the Axis2 server. (i.e. copy it to the samples/axis2Server/
repository\services)

e.g. To deploy the SimpleStockQuoteService service go to the samples/axis2Server
/src/SimpleStockQuoteService and execute 'ant' without any arguments as follows: 

C:\Java\SynapseDist\synapse-0.90-SNAPSHOT\samples\axis2Server\src\SimpleStockQuoteService>ant
Buildfile: build.xml
...
  [jar] Building jar: C:\Java\SynapseDist\synapse-0.90-SNAPSHOT\samples\axis2Server\repository
\services\SimpleStockQuoteService.aar

BUILD SUCCESSFUL

Descriptions of sample Services
1. SimpleStockQuoteService 
This service has two operations, getQuote (in/out) and placeOrder (in-only). It will generate
a sample stock quote for a given symbol. 

2. SimpleStockQuoteService1 - This has the same functionality as the SimpleStockQuoteService 
service, but exists to make available a different service instance/EPR to demonstrating routing

3. SimpleStockQuoteService2 - Same as the SimpleStockQuoteService2 (Refer above)

Starting Synapse
To start Synapse with the default configuration execute the synapse.bat or synapse.sh
script in the bin directory. This starts up an instance of Synapse using the Synapse 
and Axis2 configuration files located at repository/conf

To start specific sample configurations of Synapse, use the -sample <number> switch as follows:
e.g. bin\synapse.bat -sample <number> on a Windows system will use the sample synapse.xml
configuration file located at repository\conf\sample\synapse_sample_<number>.xml

Running the sample clients
The clients are located in the samples/axis2Client directory, and should be executed with the
ant script supplied. (e.g. ant stockquote) The ant script expects an argument to select the client 
scenario to be executed. The samples/axis2Client/client_repo directory contains an Axis2 client 
repository along with the necessary modules and dependencies to make available WS-Addressing, 
WS-Security (Rampart) and WS-Reliable Messaging (Sandesha2) on the client side.

Sample 0: 
Objective: Introduction to Synapse. Shows how a message could be made to pass through Synapse 
and logged before it is delivered to its ultimate receiver.

A client could interact with Synapse in one of three modes as follows:

1. Smart Client mode
In this mode, the client addresses its messages to the ultimate receiver of the message using 
WS-Addressing, but specifies the transport URL to Synapse. This results in the messages 
reaching Synapse with the WS-Addressing information which could be used to route the message 
as appropriate.

2. Synapse as a Proxy
In this mode, the client sets Synapse as an HTTP Proxy but sets the destination EPR to
indicate the ultimate receiver of the message. This forces all outgoing HTTP messages to flow
into Synapse, which will then send the messages as appropriate.

3. Dumb Client
In this mode, the client expects Synapse to decide on the destination of the message through
appropriate routing rules using the content of the message.

Pre-Requisites:
Start the Synapse configuration numbered 0: i.e. synapse -sample 0
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

Execute the Smart Client as 'ant stockquote'
Client: samples.userguide.StockQuoteClient

If you follow through the execution of Synapse mediation rules while looking at the configuration
used synapse_sample_0.xml you will notice that the client request arrived at Synapse with a 
WS-Addressing 'To' EPR of http://localhost:9000/axis2/services/SimpleStockQuoteService

The Synapse configuration logs the message at the highest log level as the configuration specifies 
the log level as 'full'. Then Synapse sends the message using the implicit 'To' address of the
message - which is http://localhost:9000/axis2/services/SimpleStockQuoteService

Then you will see on the Axis2 server console, a message similar to the following
	Sat Nov 18 21:01:23 IST 2006 SimpleStockQuoteService :: Generating quote for : IBM

This shows that Synapse forwarded the message to the intended recepient after logging the message
and then the actual service received and processed it. The response message generated is again
received at Synapse, and flows through the same mediation rules, which logs the message and then
sends it - this time back to the client.

On the client console you should see an output similar to the following based on the message 
received by the client.
	Standard :: Stock price = $95.26454380258552
	
Execute the Proxy client as 'ant proxystockquote'
Client: samples.userguide.ProxyStockQuoteClient

You will see the exact same behaviour as per the previous example when you run this scenario. 
However this time the difference is at the client and would be noticable only if one looks at the
source code of the example class samples.userguide.ProxyStockQuoteClient

Execute the Proxy client as 'ant proxystockquote'
Client: samples.userguide.ProxyStockQuoteClient

You will see the exact same behaviour as per the previous example when you run this scenario. 
However this time the difference is at the client and would be noticable only if one looks at the
source code of the example class samples.userguide.ProxyStockQuoteClient


Sample 1: 
Objective: Introduction to simple content based routing. Shows how a message could be made to pass 
through Synapse using the Dumb Client mode, where Synapse acts as a gateway to accept all messages
and then perform mediation and routing.

Pre-Requisites:
Start the Synapse configuration numbered 1: i.e. synapse -sample 1
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

Execute the Dumb Client as 'ant dumbstockquote'
Client: samples.userguide.DumbStockQuoteClient

This time you will notice that Synapse received a message for which Synapse was set as the ultimate
receiver of the message. Based on the 'To' EPR, Synapse performed a match to the given path 
'/StockQuote' and as the request matched the XPath expression of the filter mediator, the filter 
mediators' child mediators executes, and thereby semding the message to a different endpoint as
specified by the [reusable] endpoint definition.

Sample 2:
Objective: Introduce switch-case mediator and writing and reading of local properties on a message

Pre-Requisites:
Start the Synapse configuration numbered 2: i.e. synapse -sample 2
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

Execute the 'ant stockquote' request again, and by following through the mediation rules executed
you will see that the case statements' first case for 'IBM' executed and a local property named
'symbol' was set to 'Great stock - IBM'. Subsequently this local property value is looked up by
the log mediator and logged. The message flowes through to Axis2 and then the reply back to the
client.

Sample 3:
Objective: Illustrates simple properties, and reusable endpoints and sequences

Pre-Requisites:
Start the Synapse configuration numbered 3: i.e. synapse -sample 3
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

Execute the 'ant stockquote' request again. Following through the mediation logs you will see that
once the filter and switch statments are through, the sequence named 'stockquote' is referenced
and invoked. Also the log mediator dumps a string property, and two XPath evaluation results. The
first expression fetches a local static string lieral property named 'version'. This is a global
property applicable to all messages passing through Synapse, and available for reading from any
reusable sequence or the main rules. The second expression fetches the local message property named
'symbol' which was set within the switch mediator, using the set-property mediator. However, for
both expressions the 'get-property()' XPath extension function has been used. This function will
first look for a given property within the local message, and if a property with the name cannot
be found, it performs a lookup over all global properties. In addition, the get-property() function
supports the special cases 'To', 'From', 'Action', 'FaultTo' and 'ReplyTo', which fetches the
appropriate values from the current message context.

Sample 4:
Objective: Introduction to error handling with the try and makefault mediators

Pre-Requisites:
Start the Synapse configuration numbered 4: i.e. synapse -sample 4
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

When you execute 'ant stockquote', the default stock symbol queried is 'IBM'. However you could
use the 'symbol' system property and pass it into the above (e.g. ant stockquote -Dsymbol=MSFT)
to query another symbol.

When the IBM stock quote is requested, the configuration routes it to the endpoint defined as the
'simple' endpoint, which routes the message to the SimpleStockQuoteService on the local Axis2
instance. Hence a valid response message is shown at the client.

If you lookup a stock quote for 'MSFT', Synapse is instructed to route the message to the endpoint
defined as the 'bogus' endpoint, which tries to deliver the message to a non existing service.
Hence the method encounters a connection exception, which gets handled by the enclosing try
mediator. When an error is encountered within the scope of a try mediator, it executes the mediation
logic specified in its 'onError' segment. In this example, the sequence named 'errorHandler' is
invoked, and it logs the error message as well as the error detail, and then creates a custom
fault message reading the property named 'ERROR_MESSAGE' which is set on the current message by
the try mediator on encountering an exception. At the client end, you would see the custom SOAP
fault message instead of a stock quote.

Sample 5:
Objective: Introduction to error handling within a sequence using the 'onError' sequence

Pre-Requisites:
Start the Synapse configuration numbered 5: i.e. synapse -sample 5
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

This sample demonstrates the ability to assign an existing sequence as the error handling sequence
of another. The 'onError' attribute of a sequence calls on the named sequence specified when an
exception is encountered during the processing. You could request for IBM and MSFT quotes using the
same client and experience the same behaviour from Synapse.

Sample 6:
Objective: Introduction to header, in and out mediators

Pre-Requisites:
Start the Synapse configuration numbered 6: i.e. synapse -sample 6
Start the Axis2 server and deploy the SimpleStockQuoteService1 (Refer steps above)

In this example we use the same stockquote client which sets the 'To' EPR of the message to the
SimpleStockQuoteService service. However, if this request is for the IBM stock, the configuration
invokes the header mediator and sets the 'To' value to the SimpleStockQuoteService1. Hence if you
request for the IBM stock, you should see the header mediator changing the WS-Addressing header in
the log message, and on the Axis2 server console you would notice that the SimpleStockQuoteService1
service indeed received and processed the response. You will also see that the mediation rules
now use the in and out mediators - which executes the mediators within them depending on whether
the message is an incoming request from a client, or a response message being sent to a client
from Synapse. The log message 'sending back the response' prints only during the processing of the
response message.

If a request is made for any other stock, Synapse is instructed to drop the message and hence the
client will not receive any response. The drop mediator aborts further processing of a message and
may be used to discard and reject unwanted messages.

Sample 7:
Objective: Introduction to the extension mediators, static XML properties and the validate mediator

Pre-Requisites:
Download Xerces2-j 2.8.0 or later, and copy the xml-apis.jar and xercesImpl.jar into the
lib/endorsed folder
Start the Synapse configuration numbered 7: i.e. synapse -sample 7
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

The validate mediator is bundled as an extension mediator as it depends on the Xerces2-j parser for
schema validation, to keep the core Synapse distribution compact. Hence to try this example you will
first need to download the Xerces parser and place its jar files into the lib folder to make it
available to the validate mediator.

This example shows how an XML fragment could be made available to Synapse as a property. Such a
property is called an inline XML property and is static since its content never changes after
initiation. Hence a Schema is made available to the configuration as a property named
'validate_schema'.

Synapse supports string, inline text, inline xml and URL source properties which are static
properties. In addition it supports dynamic registry based properties as introduced later.

In this example the request messages are filtered using a boolean XPath expression which checks
if the request is a getQuote request. If so the 'customsequence' sequence is invoked, which in-turn
calls on the validate mediator passing it the property key 'validate_schema'. The validate mediator
by default operates on the first child element of the SOAP body. You may specify an XPath expression
using an attribute 'source' to override this behaviour. The validate mediator now uses the
'validate_schema' property to validate the incoming message, and if the message is in error invokes
on the 'on-fail' sequence of mediators.

If you send a standard stockquote request using 'ant stockquote' you will now get a fault back with
a message 'Invalid custom quote request' as the schema validation failed. This is because the
schema used in the example expects a slightly different message than what is created by the
stock quote client. (i.e. expects a 'stocksymbol' element instead of 'symbol' to specify the
stock symbol)

Sample 8:
Objective: Introduction to URL source properties, registry based properties and the XSLT mediator

Pre-Requisites:
Download Xalan-J version 2.7.0 or later, and copy xalan.jar and serializer.jar into the Synapse
lib folder. Copy xercesImpl.jar and xml-apis.jar (of Xerces-J) into the lib/endorsed folder.
Start the Synapse configuration numbered 8: i.e. synapse -sample 8
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

This example uses the XSLT mediator to perform transformations, and the xslt tranformation is
specified as a property, similar to the way the a schema was specified to the validate mediator.
The first resource 'xslt-key-req' is specified by specifying its URL. The URL could be any valid
URL, for e.g. http://someserver/path/resource.xslt or a file:// URL etc. The second resource
'xslt-key-resp' is specified using a 'key' attribute.

In this example you will notice the new 'registry' definition. Synapse comes with a simple URL
based registry called SimpleURLRegistry. During initialization of the registry, the
SimpleURLRegistry expects to find a property named 'root', which specifies a prefix for the
registry keys used later. When the SimpleURLRegistry is used, this root is prefixed to the
property key to form the complete URL for the resource being looked up. The registry caches a
resource once requested, and caches it internally for a specified duration. Once this period
expires, it will reload the meta information about the resource and reload its cached copy if
required, the next time the resource is requested.

Hence the second XSLT resource key 'transform/transform_back.xslt' concatenated with the 'root'
of the SimpleUTLRegistry 'file:repository/conf/sample/resources/' forms the complete URL of the
resource as 'file:repository/conf/sample/resources/transform/transform_back.xslt' and caches its
value for a period of 15000 seconds.

Execute the custom quote client as 'ant customquote' and check analyze the the Synapse debug log
output as shown below

 DEBUG XSLTMediator - Transformation source :
   <m0:CheckPriceRequest xmlns:m0="http://www.apache-synapse.org/test">
   <m0:Code>IBM</m0:Code></m0:CheckPriceRequest>
 DEBUG XSLTMediator - Transformation result :
   <m:getQuote xmlns:m="http://services.samples/xsd">
   <m:request><m:symbol>IBM</m:symbol></m:request></m:getQuote>


The incoming message is now transformed into a standard stock quote request as expected by the
SimpleStockQuoteService deployed on the local Axis2 instance by the XSLT mediator. The XSLT mediator
uses Xalan-J to perform the transformations, and hence the necessary Xalan-J and Xerces-J libraries
needs to be made available to Synapse. The response from the SimpleStockQuoteService is converted
into the custom format as expected by the client during the out message processing.

During the response processing you could notice the SimpleURLRegistry fetching the resource as
shown by the debug logs below

INFO  SimpleURLRegistry - ==> Repository fetch of resource with key : transform/transform_back.xslt

If you re-run the client again immidiately (i.e within 15 seconds of the first request) you will
not see the resource being re-loaded by the registry as the cached value would be still valid.

However if you leave the system idle for 15 seconds or more and then retry the same request, you
will now notice that the registry noticed the cache expiry and checked the meta information about
the resource to check if the resource itself has changes and requires a fresh fetch from the source
URL.

DEBUG AbstractRegistry - Cached object has expired for key : transform/transransform_back.xslt
DEBUG SimpleURLRegistry - Perform RegistryEntry lookup for key : transform/transform_back.xslt
DEBUG AbstractRegistry - Expired version number is same as current version in registry
DEBUG AbstractRegistry - Renew cache lease for another 15s

Now edit the repository/conf/sample/resources/transform/transform_back.xslt file and add a blank
line at the end. Now when you run the client again, and if the cache is expired, the resource
would be re-fetched from its URL by the registry and this can be seen by the following debug log
messages

DEBUG AbstractRegistry - Cached object has expired for key : transform/transform_back.xslt
DEBUG SimpleURLRegistry - Perform RegistryEntry lookup for key : transform/transform_back.xslt
INFO  SimpleURLRegistry - ==> Repository fetch of resource with key : transform/transform_back.xslt

Thus the SimpleURLRegistry allows resource to be cached, and updates detected so that changes
could be reloaded without restarting the Synapse instance.

Proxy services
Sample 100:
Objective: Introduction to Synapse proxy services

Pre-Requisites:
Start the Synapse configuration numbered 100: i.e. synapse -sample 100
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

Once Synapse starts, you could go to http://localhost:8080/axis2/services/StockQuoteProxy?wsdl and
view the WSDL for the proxy service defined in the configuration. This WSDL is based on the source
WSDL supplied in the proxy service definition, and is updated to reflect the proxy service EPR. If
a proxy service definition does not specify a target for its messages, the Synapse mediation rules
are applied to route messages.

Execute the stock quote client by requesting for a stock quote on the proxy service as follows:
  ant stockquote -Durl=http://localhost:8080/axis2/services/StockQuoteProxy

You will now notice that the Synapse mediation rules were applied and the request was routed to the
SimpleStockQuoteService service on the local Axis2 instance. The response message is mediated using
the same rules, as an outgoing sequence is not specified in this example either. The client should
receive a stock quote reply from the proxy service. Also the client could get the WSDL for the proxy
service by requesting for http://localhost:8080/axis2/services/StockQuoteProxy?wsdl

Sample 101:
Objective: Using custom sequences and endpoints for message mediation with proxy services

Pre-Requisites:
Start the Synapse configuration numbered 101: i.e. synapse -sample 101
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

This configuration creates two proxy services with different behaviour. The first proxy service
'StockQuoteProxy1' uses the sequence named 'proxy_1' as its main mediation sequence for messages
it receives. The second proxy service 'StockQuoteProxy2' is set to directly forward messages that
are received to the endpoint named 'proxy_2_endpoint' without any mediation.

You could send a stock quote request to each of these proxy services and receive the reply generated
by the actual service hosted on the Axis2 instance. Use the -Durl=<EPR> property when executing the
client as per example 100, to request on the two proxy services.

Sample 102:
Objective: Attaching service level WS-Security policies to proxy services

Pre-Requisites:
Start the Synapse configuration numbered 102: i.e. synapse -sample 102
Copy the Apache Rampart module (e.g. rampart-1.1-SNAPSHOT.mar) into the modules directories of both
the sample Axis2 client and server: samples/axis2Client/client_repo/modules and samples/axis2Server/
repository/modules. The Rampart module could be found at repository\modules and is not duplicated
in the distributions due to its large file size.
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

To execute the sample use the stock quote client with the 'secpolicy' and 'url' system properties
passed in as follows:
e.g. ant stockquote -Durl=http://localhost:8080/axis2/services/StockQuoteProxy
        -Dsecpolicy=..\..\repository\conf\sample\resources\policy\policy_1.xml

The sample security policy policy_1.xml uses timestamps and username token tuhentication on the
stock quote request. Following the debug logs on the Synapse server you could notice the presence
of WS-Security headers on the incoming message. By requesting the WSDL of the proxy service you
could also see that the supplied policy file has been attached to the specified WSDL as well.
e.g. http://localhost:8080/axis2/services/StockQuoteProxy?wsdl

A proxy service with an attached policy - such as a WS-Security policy - ensures that messages which
goes through mediation have satisfied the constraints as specified by the supplied policy. i.e. for
example, if any WS-Security validations fail, the message will not reach Synapse mediation, but
the client would get an appropriate error message from the Apache Rampart module directly.

The mediation shows the header mediator used to strip out the wsse:Security header from the current
message before being forwarded to the SimpleStockQuoteService. Hence the message sent to the stock
quote service EPR is without any WS-Security headers as can be seen from the log messages.

Note: In this example, Apache Rampart was engaged on the proxy service through the policy specified.
However, if you wish to engage the default WS-Security policy of Rampart on a proxy service, you
could use the <enableSec/> option on a proxy service instead. This will be similar in function to
'engaging' Rampart on an Axis2 service.

Sample 103:
Objective: Using WS-Security signing and encryption with proxy services through WS-Policy 

Pre-Requisites:
Download and copy the BouncyCastle JAR file into your Synapse lib directory. (Note: the exact JAR
you need to install depends on your JDK - for JDK 1.4 I have used bcprov-jdk13-132.jar)
Start the Synapse configuration numbered 103: i.e. synapse -sample 103
Copy the Apache Rampart module (e.g. rampart-1.1-SNAPSHOT.mar) into the modules directory of
the sample Axis2 client samples/axis2Client/client_repo/modules. The Rampart module could be found
at repository\modules and is not duplicated within the distributions due to its large file size.
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)

To execute the sample use the dumb stock quote client with the 'secpolicy' and 'gatewayurl' system
properties passed in as follows:
e.g.
    ant dumbstockquote -Dsecpolicy=..\..\repository\conf\sample\resources\policy\client_policy_3.xml
    -Dgatewayurl=http://localhost:8080/axis2/services/StockQuoteProxy

The sample security policy client_policy_3.xml ensures signed and encrypted messages which request
for the stock quotes. Following the debug logs on the Synapse server you could notice the presence
of WS-Security headers on the incoming message. By requesting the WSDL of the proxy service you
could also see that the supplied policy file has been attached to the specified WSDL as well.
e.g. http://localhost:8080/axis2/services/StockQuoteProxy?wsdl

By following through the Synapse log messages you could see that the proxy service received and
decrypted the secured SOAP envelope. (If you sent the client request to Synapse through a TCP
monitor you would be able to see the raw encrypted message in transit) and forwarded this to the
simple stockquote service.

Sample 110:
Objective: Introduction to switching transports with proxy services

Pre-Requisites:
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)
Download, install and start a JMS server, and configure Synapse to listen on JMS (refer notes below)
Start the Synapse configuration numbered 110: i.e. synapse -sample 110

For this example we would use ActiveMQ as the JMS provider. Once ActiveMQ is installed and started
you should get a message as follows:
  INFO  BrokerService                  - ActiveMQ JMS Message Broker (localhost) started

You will now need to configure the Axis2 instance used by Synapse (not the sample Axis2 server) to
enable JMS support using the above provider. Refer to the Axis2 documentation on setting up JMS for
more details (http://ws.apache.org/axis2/1_1/jms-transport.html). You will also need to copy the
ActiveMQ client jar files activeio-core-3.0-beta1.jar, activemq-core-4.0-RC2.jar and
geronimo-j2ee-management_1.0_spec-1.0.jar into the lib directory to allow Synapse to connect to the
JMS provider.

For a default ActiveMQ v4.0 installation, you may uncomment the Axis2 transport listener
configuration found at repository/conf/axis2.xml as
<transportReceiver name="jms" class="org.apache.axis2.transport.jms.JMSListener"> ...

Once you start the Synapse configuration and request for the WSDL of the proxy service you will
notice that its exposed only on the JMS transport. This is because the configuration specified this
requirement in the proxy service definition.

Now lets send a stock quote request on JMS, using the dumb stock quote client as follows: (note: you
need type the command in one single line without breaks)

ant dumbstockquote -Dgatewayurl="jms:/StockQuoteProxy?transport.jms.ConnectionFactoryJNDIName=
    QueueConnectionFactory&java.naming.factory.initial=org.apache.activemq.jndi.
    ActiveMQInitialContextFactory&java.naming.provider.url=tcp://localhost:61616"

On the Synapse debug log you will notice that the JMS listener received the request message as:
    [JMSWorker-1] DEBUG ProxyServiceMessageReceiver -
        Proxy Service StockQuoteProxy received a new message...

Synapse forwarded this message to the HTTP EPR of the simple stock quote service hosted on the
sample Axis2 server, and returned the reply back to the client through a JMS temporary queue.

Note: It is possible to instruct a JMS proxy service to listen to an already existing destination
without creating a new one. To do this, use the property elements on the proxy service definition
to specify the destination and connection factory etc. 
    e.g. <property name="transport.jms.Destination" value="dynamicTopics/something.TestTopic"/>

Sample 111:
Objective: Demonstrate switching from HTTP to JMS

Pre-Requisites:
Download, install and start a JMS server, and configure Synapse to listen on JMS (refer notes below)
Configure sample Axis2 server for JMS (refer notes above)
Start the Axis2 server and deploy the SimpleStockQuoteService (Refer steps above)
Configure JMS transport for Synapse (refer notes above - sample 110)
Start the Synapse configuration numbered 111: i.e. synapse -sample 111

To switch from HTTP to JMS, edit the samples/axis2Server/repository/conf/axis2.xml for the sample
Axis2 server and enable JMS (refer notes above), and restart the server. You should now see that
the simple stock quote service is available over JMS as well at an address as the one shown below,
by looking at the WSDL of the service.

jms:/SimpleStockQuoteService?transport.jms.ConnectionFactoryJNDIName=QueueConnectionFactory&
    java.naming.factory.initial=org.apache.activemq.jndi.ActiveMQInitialContextFactory&
    java.naming.provider.url=tcp://localhost:61616

This Synapse configuration creates a proxy service over HTTP and forwards received messages to the
above EPR using JMS, and sends back the response to the client over HTTP once the simple stock quote
service responds with the stock quote reply over JMS to the Synapse server.


