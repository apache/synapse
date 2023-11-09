package org.apache.synapse.message.store.impl.jdbc;

public class JDBCMessageStoreConstants {

    /**
     * Connection URL to database
     */
    public static final String JDBC_CONNECTION_URL = "store.jdbc.connection.url";

    /**
     * Driver to use
     */
    public static final String JDBC_CONNECTION_DRIVER = "store.jdbc.driver";

    /**
     * User Name that is used to create the connection with the broker
     */
    public static final String JDBC_USERNAME = "store.jdbc.username";

    /**
     * Password that is used to create the connection with the broker
     */
    public static final String JDBC_PASSWORD = "store.jdbc.password";

    /**
     * DataSource name exists
     */
    public static final String JDBC_DSNAME = "store.jdbc.dsName";

    /**
     * IcClass of the
     */
    public static final String JDBC_ICCLASS = "store.jdbc.icClass";


    // Optional parameters
    /**
     * Name of the database table
     */
    public static final String JDBC_TABLE = "store.jdbc.table";

    /**
     * Default name of the database table
     */
    public static final String JDBC_DEFAULT_TABLE_NAME = "jdbc_message_store";
}
