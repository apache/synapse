package org.apache.synapse.message;

import org.apache.synapse.MessageContext;

public interface MessageProducer {
    /**
     * Stores the given message to the store associated with this message consumer.
     *
     * @param synCtx Message to be saved.
     * @return {@code true} if storing of the message is successful, {@code false} otherwise.
     */
    boolean storeMessage(MessageContext synCtx);

    /**
     * Cleans up this message consumer
     *
     * @return {@code true} if clean up is successful, {@code false} otherwise.
     */
    boolean cleanup();

    /**
     * Sets the ID of this message consumer.
     *
     * @param id ID
     */
    public void setId(int id);

    /**
     * Returns the ID of this message  consumer.
     *
     * @return ID
     */
    public String getId();
}