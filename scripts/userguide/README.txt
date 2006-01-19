Running Userguide Samples
=========================

This README contains the details on running User guide samples with Ant. Following lines exactly map with the User guide instructions of running Samples. 

First the user has to start the Simple Synapse Server. User has to type the following commond on-line

ant synapse

The above will start Synapse as a server on port 8080.

Using the following combinations, User will be able to run StockQuoteClient sample as stated in User guide. 

1.ant
2.ant -Dsymbol=IBM -Durl=http://www.webservicex.net/stockquote.asmx -Dsynapseurl=http://www.webservicex.net/stockquote.asmx
3.ant -Dsymbol=IBM -Durl=http://localhost:8080/StockQuote -Dsynapseurl=http://localhost:8080
4.ant -Dsymbol=IBM -Durl=http://stockquote -Dsynapseurl=http://localhost:8080
5.ant -Dsymbol=MSFT -Durl=http://stockquote -Dsynapseurl=http://localhost:8080

Using the following line, User will be able to run LoggingClient sample as stated in User guide.

ant log_with_addressing

When you are running logging sample please remove following elements from synapse.xml.  
<stage name="sender">
	<send/>
</stage>

The above samples use Synapse standard synapse.xml.  
