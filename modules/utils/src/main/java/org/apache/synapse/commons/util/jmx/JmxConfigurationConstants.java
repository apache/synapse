package org.apache.synapse.commons.util.jmx;

public class JmxConfigurationConstants {
    
    /** Prefix for all properties in property file*/
    public static final String PROP_SYNAPSE_PREFIX_JMX = "synapse.jmx.";
    
    /** JNDI port property used for the JMX naming directory (RMI registry) */
    public static final String PROP_JNDI_PORT = "jndiPort";
    
    /** RMI port property used to configure the JMX RMI port (firewalled setup) */
    public static final String PROP_RMI_PORT = "rmiPort";

    /** Hostname property used to configure JMX Adapter */
    public static final String PROP_HOSTNAME = "hostname";
    
    /** Property for location of remote access file. */
    public static final String PROP_REMOTE_ACCESS_FILE = "remote.access.file";
    
    /** Property to activate remote SSL support (same as com.sun.management.jmxremote.ssl) */
    public static final String PROP_REMOTE_SSL = "remote.ssl";
    
    
    
}
