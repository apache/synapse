Note
-----

A SLA or Service Level Aggrement, is between a consumer and a provider, enforced through a 
contract or a policy.
In this case its enforced through the SLAMediator and the configuration is stored in the sla.xml.
Every known consumer, identified by the IP address of his system is alloted a priority for every 
service for which SLA is to be enforced.
In case more than 1 request for a given service, lands on the server at the same time,
the SLA mediator if provisioned will be able to arrange these requests based on the priority assigned to
the them for the given request. 

Priority
---------

0  has the highest priority.(it's an int value)
-1 is returned in case there is no priority configured for a 
   particular (consumer, service) combination.

Return Value
-------------

true  - SLA successfully executed.
false - Error while procesing.


Running SLA Mediator Sample
===================================

1. Copy synapse.xml in this repository to synapse_repository in the binary distribution.
2. Place the SLAMediator.aar in the synape_repository/services.
3. Using the Ant command "ant synapse", start the stand-alone synapse server. 
   This will be start at port 8080. 
4. Use Ant command "ant" StockQuoteClient available in this folder to run the sample. 

The configuration details are available in sla.xml, which is located in SLAMediator's META-INF folder. 

