package org.apache.synapse.commons.jmx;


import org.apache.synapse.commons.security.secret.SecretInformation;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import java.util.Collections;

/**
 * Handles the authentication for JMX management.
 */

public class JmxSecretAuthenticator implements JMXAuthenticator {

    private SecretInformation secretInformation;
    
    public JmxSecretAuthenticator(SecretInformation secretInformation) {
        this.secretInformation = secretInformation;
    }
    
    public Subject authenticate(Object credentials) {

        if (credentials == null) {
            throw new SecurityException("Credentials required");
        }

        if (!(credentials instanceof String[])) {
            throw new SecurityException("Credentials should be String[]");
        }

        // Only expect username/password, therefore the credentials should have two entries
        final String[] aCredentials = (String[]) credentials;
        if (aCredentials.length < 2) {
            throw new SecurityException("Credentials should have the username and password");
        }

        String username = aCredentials[0];
        String password = (aCredentials[1] != null ? aCredentials[1] : "");

        // perform authentication
        if (secretInformation.getUser().equals(username) && password.equals(secretInformation.getResolvedPassword())) {
            return new Subject(true,
                Collections.singleton(new JMXPrincipal(username)),
                Collections.EMPTY_SET,
                Collections.EMPTY_SET);
        } else {
            throw new SecurityException("Username and/or password are incorrect, " +
                "or you do not have the necessary access rights.");
        }
    }
}
