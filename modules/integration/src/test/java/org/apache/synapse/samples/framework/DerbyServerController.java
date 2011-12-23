package org.apache.synapse.samples.framework;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.synapse.samples.framework.config.DerbyConfiguration;

import java.io.File;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Responsible for starting up and shutting down
 * a derby server instance in order to run a sample test.
 */
public class DerbyServerController implements BackEndServerController {

    private static final Log log = LogFactory.getLog(DerbyServerController.class);

    private String serverName;
    private DerbyConfiguration configuration;
    private NetworkServerControl server;

    public DerbyServerController(String serverName, DerbyConfiguration configuration) {
        this.serverName = serverName;
        this.configuration = configuration;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean start() {
        log.info("Preparing to start Derby server: " + serverName);
        try {
            //server
            Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
            server = new NetworkServerControl
                    (InetAddress.getByName("localhost"), 1527);
            server.start(null);
            while (true) {
                try {
                    server.ping();
                    break;
                } catch (Exception ignored) {
                    log.info("Waiting for Derby server to start...");
                    Thread.sleep(2000);
                }
            }
            log.info("Derby is successfully started.");

        } catch (Exception e) {
            log.warn("There was an error starting Derby server: " + serverName, e);
            return false;
        }

        //client
        String dbName = "synapsedb";
        String createTableQuery = "CREATE table company(name varchar(10), id varchar(10), price double)";
        String connectionURL = "jdbc:derby://localhost:1527/" + dbName + ";create=true";

        java.util.Properties props = new java.util.Properties();
        props.put("user", "synapse");
        props.put("password", "synapse");
        props.put("create", "true");

        try {
            // on JDK 1.6 or higher, EmbeddedDriver get loaded automatically.
            Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();

            log.info("Creating the sample database and connecting to server");
            Connection conn = DriverManager.getConnection(connectionURL, props);

            Statement s = conn.createStatement();
            log.info("Creating the sample table and inserting values");
            s.execute(createTableQuery);
            s.execute("INSERT into company values ('IBM','c1',0.0)");
            s.execute(" INSERT into company values ('SUN','c2',0.0)");
            s.execute(" INSERT into company values ('MSFT','c3',0.0)");
            conn.commit();
            return true;
        } catch (Exception e) {
            log.error("Error executing SQL queries", e);
            return false;
        }
    }

    public boolean stop() {
        log.info("Shutting down Derby server...");
        try {
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (SQLException se) {
                if (se.getErrorCode() == 50000 && "XJ015".equals(se.getSQLState())) {
                    // we got the expected exception
                    log.info("Derby shut down normally");
                }
            }
            server.shutdown();
            FileUtils.deleteDirectory(new File("./synapsedb"));
            return true;

        } catch (Exception e) {
            log.warn("Error while trying to delete database directory", e);
            return false;
        }
    }
}