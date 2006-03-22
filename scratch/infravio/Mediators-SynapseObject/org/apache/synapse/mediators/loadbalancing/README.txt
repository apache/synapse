Hi everybody,

Note
-----

Loadbalancing is the act of distributing the load, the requests for a particular service across 
various service endpoints.
In case a provider has more than one endpoint that provides the same service, he would like the
load of requests being made to be distributed across them.
The meta-data for the loadbalancing mediator, is configured in the loadbalancing.xml.
The strategy being supported now is round robin i.e. requests would be sent to the various participating
services in a round-robin, one after the another, fashion.
In case the service that was invoked fails to respond, the mediator switches to the next one in the line.
Its a mere pass, hence the next request will get directed to the one which was supposed to handle it if 
the previous service had not failed.


Return Values
--------------
False - Its the last mediator in the chain..A send mediator that send the request and gets the response.
The logs provide more information in case of an error.

Running Loadbalancing Mediator Sample
=======================================

1. Copy synapse.xml in this repository to synapse_repository in the binary distribution.
2. Place the LoadBalancingMediator.aar in the synape_repository/services.
3. Using the Ant command "ant synapse", start the stand-alone synapse server. 
   This will start at port 8080. 
4. Use Ant command "ant" StockQuoteClient available in this folder to run the sample. 
 
The configuration is available in loadbalancing.xml, which is located in LoadBalancingMediator.aar's 
META-INF folder. 