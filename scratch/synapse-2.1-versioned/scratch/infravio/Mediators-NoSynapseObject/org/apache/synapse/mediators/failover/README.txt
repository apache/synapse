Hi everybody,

Note
-----

Failover is the act of switching to a secondary service in case the primary service fails.
Hence, logically, we can configure failover only when we have 2 or more endpoints providing similar
service.

The failover process can be initiated on timeout and or faults.

In case of 'failover on faults' the FailoverMediator keeps switching to secondary services,
until all the secondary services are tried or one of them returns a successful result.

In case failover based on timeout is active, the participating end-points would be given 
timeout values and the connection would be forced to close and a fault would be returned if no 
response arrives within that many milliseconds and then 'failover on fault' logic kicks in.


Return Values
--------------
True - Failover performed successfully.
False - Error while processing.

Running Failover Mediator Sample
===================================

1. Copy synapse.xml in this repository to synapse_repository in the binary distribution.
2. Place the FailoverMediator.aar in the synape_repository/services.
3. Using the Ant command "ant synapse", start the stand-alone synapse server. 
   This will start at port 8080. 
4. Use Ant command "ant" StockQuoteClient available in this folder to run the sample. 

The configuration is available in failover.xml, which is located in FailoverMediator.aar's 
META-INF folder. 