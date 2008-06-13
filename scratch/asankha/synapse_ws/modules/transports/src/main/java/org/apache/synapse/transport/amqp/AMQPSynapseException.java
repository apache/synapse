package org.apache.synapse.transport.amqp;

public class AMQPSynapseException extends RuntimeException
{
    public AMQPSynapseException(String s, Exception e){
        super(s,e);
    }

    public AMQPSynapseException(String s){
        super(s);
    }
}
