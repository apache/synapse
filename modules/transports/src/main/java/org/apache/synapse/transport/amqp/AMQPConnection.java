package org.apache.synapse.transport.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpidity.ErrorCode;
import org.apache.qpidity.nclient.Client;
import org.apache.qpidity.nclient.ClosedListener;
import org.apache.qpidity.nclient.Connection;


class AMQPConnection implements ClosedListener{

    private static final Log log = LogFactory.getLog(AMQPConnection.class);

    /** Connection name as identified in the axis2.xml */
    private String name;

    /** The AMQP URL */
    private String url;

    /** the AMQP connection */
    private Connection con;

    /** the exchange name to use */
    private String exchangeName = "amq.direct";

    /** the exchangeType to use */
    private String exchangeType = "direct";

    /** if connection dropped, reconnect timeout in milliseconds; default 30 seconds */
    private long reconnectTimeout = 30000;

    public AMQPConnection()
    {
    }

    public AMQPConnection(String name, String url, String exchangeName, String exchangeType)
    {
        super();
        this.name = name;
        this.url = url;
        this.exchangeName = exchangeName;
        this.exchangeType = exchangeType;
    }

    public String getExchangeName()
    {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName)
    {
        this.exchangeName = exchangeName;
    }

    public String getExchangeType()
    {
        return exchangeType;
    }

    public void setExchangeType(String exchangeType)
    {
        this.exchangeType = exchangeType;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Connection getConnection()
    {
        return con;
    }

    public void setConnection(Connection con)
    {
        this.con = con;
    }

    public long getReconnectTimeout() {
        return reconnectTimeout;
    }

    public void setReconnectTimeout(long reconnectTimeout) {
        this.reconnectTimeout = reconnectTimeout;
    }

    public void stop(){

    }

    public void start() throws AMQPSynapseException
    {
        Connection con = Client.createConnection();
        try{
            con.connect(url);
        }catch(Exception e){
            throw new AMQPSynapseException("Error creating a connection to the broker",e);
        }
    }

    public void onClosed(ErrorCode errorCode, String reason)
    {
        log.error("AMQP connection " + name + " encountered an error, Error code:" + errorCode + " reason:" + reason);
        boolean wasError = true;

        // try to connect
        // if error occurs wait and try again
        while (wasError == true) {

            try {
               // connectAndListen();
                wasError = false;

            } catch (Exception e1) {
                log.warn("AMQP reconnection attempt failed for connection : " + name,e1);
            }

            if (wasError == true) {
                try {
                    log.info("Attempting reconnection for connection " + name +
                        " in " + getReconnectTimeout()/1000 +  " seconds");
                    Thread.sleep(getReconnectTimeout());
                } catch (InterruptedException ignore) {}
            }
        } // wasError
    }


}
