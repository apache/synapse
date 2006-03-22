Hi everybody,

Note
-----


Return Value
-------------

true  - CI successfully executed.
false - Error while procesing.


Running CI Mediator Sample
===================================

1. Copy synapse.xml in this repository to synapse_repository in the binary distribution.
2. Place the CIMediator.aar in the synape_repository/services.
3. Using the Ant command "ant synapse", start the stand-alone synapse server. 
   This will be start at port 8080. 
4. Use Ant command "ant" StockQuoteClient available in this folder to run the sample. 
 
The configuration is available in ci.xml, which is located in CIMediator.aar's META-INF folder. 

