Running Userguide Samples
=========================

This README contains the details on running User guide samples with Ant. Following lines exactly map with the User guide instructions of running Samples. 

First the user has to start the Simple Synapse Server. User has to type the following commond on-line

ant synapse

The above will start Synapse as a server on port 8080.

Using the following combinations, User will be able to run StockQuoteClient sample as stated in User guide. 

1.ant -Darg1=IBM -Darg2=http://www.webservicex.net/stockquote.asmx -Darg3=http://localhost:8080
2.ant -Darg1=IBM -Darg2=http://www.webservicex.net/stockquote.asmx -Darg3=http://www.webservicex.net/stockquote.asmx
3.ant 
4.ant -Darg1=IBM -Darg2=urn:xmethods-delayed-quotes
5.ant -Darg1=MSFT

Using the following line, User will be able to run LoggingClient sample as stated in User guide.

ant log_with_addressing

When you are running logging sample please remove following elements from synapse.xml.  
<stage name="sender">
	<send/>
</stage>

The above samples use Synapse standard synapse.xml.  
