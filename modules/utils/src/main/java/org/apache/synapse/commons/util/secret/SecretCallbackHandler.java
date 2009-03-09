package org.apache.synapse.commons.util.secret;

/**
 * Get the required secrets needed from varies secret providers
 */
public interface SecretCallbackHandler {

    /**
     * Retrieve the secrets requested in the provided SecretCallbacks.
     *
     * @param secretCallbacks secretCallbacks
     */
    public void handle(SecretCallback[] secretCallbacks);
}
