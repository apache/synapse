package org.apache.synapse.message;

import org.apache.synapse.MessageContext;

public interface MessageConsumer {
    /**
     * Receives the next message from the store.
     *
     * @return Synapse message context of the last message received from the store.
     */
    MessageContext receive();

    /**
     * Acknowledges the last message received so that it will be removed from the store.
     *
     * @return {@code true} if the acknowledgement is successful. {@code false} otherwise.
     */
    boolean ack();

    /**
     * Cleans up this message consumer
     *
     * @return {@code true} if cleanup is successful, {@code false} otherwise.
     */
    boolean cleanup();

    /**
     * Check availability of connectivity with the message store
     *
     * @return {@code true} if connection available, {@code false} otherwise.
     */
    boolean isAlive();

    /**
     * Sets the ID of this message consumer.
     *
     * @param i ID
     */
    public void setId(int i);

    /**
     * Returns the ID of this Message consumer.
     *
     * @return ID
     */
    public String getId();
}