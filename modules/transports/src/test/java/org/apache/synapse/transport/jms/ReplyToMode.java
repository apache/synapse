package org.apache.synapse.transport.jms;

public enum ReplyToMode {
    /**
     * The reply to destination is configured on the endpoint and the
     * clients doesn't set the reply to destination in the JMS message. 
     */
    ENDPOINT,
    
    /**
     * The client specifies the reply to destination in the corresponding
     * JMS header.
     */
    CLIENT
}
