package org.apache.synapse;

public class SynapseException extends Exception {
    public SynapseException() {
    }

    public SynapseException(String message) {
        super(message);
    }

    public SynapseException(Throwable cause) {
        super(cause);
    }

    public SynapseException(String message, Throwable cause) {
        super(message, cause);
    }
}
