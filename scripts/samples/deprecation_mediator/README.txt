Running Deprecation Mediator Sample
===================================

1. Copy synapse.xml in this repository to synapse_repository in the binary distribution.
2. Place the DeprecationMediator.aar in the synape_repository/services.
3. Using the Ant command "ant synapse", start the stand-alone synapse server. This will be start at port 8080. 
4. Use Ant command "ant" StockQuoteClient to run the sample. 
 
You will get an answer if the system date is not between 06/09/2005:00:00 and 07/02/2006:00:00 [DD/MM/yyyy:HH:mm]

The prior configuration is available at deprecation.xml, where it will located at DeprecationMediator.aar's META-INF folder. 

Note : 
The service is deprecated between these dates, i.e. it is not available or is not to be used.

