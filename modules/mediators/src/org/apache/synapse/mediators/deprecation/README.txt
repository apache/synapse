Hi everybody,

Note
-----

This is the first shot towards a Deprecation mediator.
A trivial explanation would be - A service configured as deprecated between certain dates/time will not
be accessible for requests coming in between those dates.
You are free to declare more than one set of dates/time and enable or disable the set when required.

The mediator depends on the UTC - Date/time settings and these are converted and offset to local time,
during processing, internally.

It implements the Mediator interface and hence has the "boolean mediate(SynapseMessage)" method.
The configuration data about the deprecation for a particular service is provided in a 
"deprecation.xml" file.

When a SynapseContext is sent to the DeprecationMediator it checks wether the service to which the request is
addressed is deprecatd or not. It sets the value in the "synapse.deprecation.result" property in the synapseContext.

Return Values
--------------
True - Service is not deprecated, the request can go on.
False - The service is deprecated, need not send the request to the service.

To try out the mediator
------------------------

 - Modify the deprecation.xml present in the 
      * aar (in case you are using the binary)
      * src\sampleMediators\deprecation\META-INF (in case you are working with the source)
   to suit the configuration you want.
 - Send in a request to Synapse and see it work/get rejected depending on the settings.

~Lots of scope for enhancement and improvement. Will keep adding up!