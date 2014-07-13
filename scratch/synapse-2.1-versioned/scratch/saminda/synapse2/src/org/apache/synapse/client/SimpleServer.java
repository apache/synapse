package org.apache.synapse.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.SimpleHTTPServer;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 8:10:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleServer {
    public static void main(String[] args) {
        try {
            SimpleHTTPServer sas = new SimpleHTTPServer(
                    "/home/saminda/myprojects/synapse2/server", 8081);
            sas.start();
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }
}
