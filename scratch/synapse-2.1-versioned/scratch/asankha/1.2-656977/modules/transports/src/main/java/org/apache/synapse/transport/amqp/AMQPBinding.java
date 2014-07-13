package org.apache.synapse.transport.amqp;


public class AMQPBinding {

    private String exchangeName = "amq.direct";
    private String exchangeType = "direct";
    private String routingKey;
    private boolean primary;

    public AMQPBinding(){
    }

    public AMQPBinding(String exchangeName, String exchangeType, String routingKey, boolean primary)
    {
        super();
        this.exchangeName = exchangeName;
        this.exchangeType = exchangeType;
        this.routingKey = routingKey;
        this.primary = primary;
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
    public String getRoutingKey()
    {
        return routingKey;
    }
    public void setRoutingKey(String routingKey)
    {
        this.routingKey = routingKey;
    }

    public boolean isPrimary()
    {
        return primary;
    }

    public void setPrimary(boolean primary)
    {
        this.primary = primary;
    }

}
