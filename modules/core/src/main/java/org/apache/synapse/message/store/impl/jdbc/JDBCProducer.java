package org.apache.synapse.message.store.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.message.MessageProducer;

/**
 * JDBC Store Producer
 */
public class JDBCProducer implements MessageProducer {

    /**
     * Logger for the class
     */
    private static final Log logger = LogFactory.getLog(JDBCProducer.class.getName());

    /**
     * Store for the producer
     */
    private JDBCMessageStore store;

    /**
     * Id of the producer
     */
    private String producerId;

    /**
     * Initialize producer
     *
     * @param store - JDBC message store
     */
    public JDBCProducer(JDBCMessageStore store) {
        this.store = store;
    }

    /**
     * Add a message to the end of the table. If fetching success return true else false
     *
     * @param synCtx message to insert
     * @return -  success/failure of fetching
     */
    @Override
    public boolean storeMessage(MessageContext synCtx) {
        boolean success = false;
        try {
            success = store.store(synCtx);
            store.enqueued();
        } catch (SynapseException e) {
            logger.error("Error while storing message : " + synCtx.getMessageID(), e);
        }
        return success;
    }

    /**
     * Cleanup the producer
     *
     * @return true Since no producer specific things
     */
    @Override
    public boolean cleanup() {
        return true;
    }

    /**
     * Set producer id
     *
     * @param id ID
     */
    @Override
    public void setId(int id) {
        producerId = "[" + store.getName() + "-P-" + id + "]";
    }

    /**
     * Get producer id
     *
     * @return producerId - Producer identifier
     */
    @Override
    public String getId() {
        if (producerId == null) {
            return "[unknown-producer]";
        }
        return producerId;
    }
}
