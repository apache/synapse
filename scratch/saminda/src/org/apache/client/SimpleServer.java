package org.apache.client;

import org.apache.axis2.AxisFault;
import org.apache.synapse.transport.server.SimpleSynapseServer;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 8:10:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleServer {
    public static void main(String[] args ) {
        try {
            SimpleSynapseServer sss = new SimpleSynapseServer("/home/saminda/myprojects/synapse/server",8080);
            sss.start();
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }
    }
}
