Hi everybody,

Note
-----

This is the first shot towards a Deprecation mediator.
A trivial explanation would be - A service configured as deprecated between certain 
dates/time will not
be accessible for requests coming in between those dates.
You are free to declare more than one set of dates/time and enable or disable the set when required.

The mediator depends on the UTC - Date/time settings and these are converted 
and offset to local time,during processing, internally.

It implements the Mediator interface and hence has the "boolean mediate(SynapseMessage)" method.
The configuration data about the deprecation for a particular service is provided in a 
"deprecation.xml" file.

When a SynapseContext is sent to the DeprecationMediator it checks wether the service 
to which the request is addressed is deprecatd or not. 
It sets the value in the "synapse.deprecation.result" property in the synapseContext.

Return Values
--------------
True - Service is not deprecated, the request can go on.
False - The service is deprecated, need not send the request to the service.

Running Deprecation Mediator Sample
===================================

1. Copy synapse.xml in this repository to synapse_repository in the binary distribution.
2. Place the DeprecationMediator.aar in the synape_repository/services.
3. Using the Ant command "ant synapse", start the stand-alone synapse server.
   This will be start at port 8080. 
4. Use Ant command "ant" StockQuoteClient available in this folder to run the sample. 
 
You will get an answer if the system date is not between 06/09/2005:00:00 and 07/02/2006:00:00 
[DD/MM/yyyy:HH:mm]

The prior configuration is available in deprecation.xml, which is located in 
DeprecationMediator.aar's META-INF folder. 

Note : 
The service is deprecated between these dates, i.e. it is not available or is not to be used.
