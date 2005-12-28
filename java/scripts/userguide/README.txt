Running StockQuoteClient

This README contains the details on running the prior sample with Ant. Following lines exactly map with the User guide instructions of running Samples. 

First the user has to start the Simple Synapse Server. User has to type the following commond on-line

ant synapse

The above will start Synapse as a server on port 8080.

Using the following combinations, User will be able to run the prior sample as stated in User guide. 

1.ant -Darg1=IBM -Darg2=http://64.124.140.30:9090/soap -Darg3=http://localhost:8080
2.ant -Darg1=IBM -Darg2=http://64.124.140.30:9090/soap -Darg3=http://64.124.140.30:9090/soap
3.ant 
4.ant -Darg1=IBM -Darg2=urn:xmethods-delayed-quotes
5.ant -Darg1=MSFT 
