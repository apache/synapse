/**
 *
 */
package org.apache.synapse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;

public class RMIRegistryController {

    public static final Log log = LogFactory.getLog(RMIRegistryController.class);

    private static RMIRegistryController ourInstance = new RMIRegistryController();
    private Registry localRegistry;
    private boolean weCreatedRMIReg = false;

    public static RMIRegistryController getInstance() {
        return ourInstance;
    }

    private RMIRegistryController() {}

    /**
     * Creates a RMI local registry with given port
     *
     * @param port The port
     */
    public void createLocalRegistry(int port) {
        try {
            localRegistry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            String msg = "Couldn't create a local registry(RMI) : port " + port +
                    " already in use.";
            handleException(msg, e);
        }
    }

    /**
     * removes if there is a RMI local registry instance
     */
    public void removeLocalRegistry() {
        if (localRegistry != null) {
            try {
                log.info("Removing the RMI registy instance from the RMI runtime ");
                UnicastRemoteObject.unexportObject(localRegistry, true);
            } catch (NoSuchObjectException e) {
                String msg = "Error when stoping localregistry(RMI)";
                handleException(msg, e);
            }
        }
    }


    /**
     * Helper methods for handle errors.
     *
     * @param msg The error message
     * @param e   The exception
     */
    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}
