package org.apache.synapse.commons.util.secret;

/**
 *
 */
public interface SecretCallbackHandler {

    public void handle(SecretCallback[] secretCallbacks);
}
