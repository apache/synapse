/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.fix;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import quickfix.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * The FIXSessionFactory is responsible for creating and managing FIX sessions. A FIX session can be
 * initiated in one of two modes, namely the acceptor mode and the initiator mode. FIX sessions
 * requested by the transport listener at service deployment are created in acceptor mode. When
 * the transport sender is about to send a FIX message it will check whether a valid FIX session exists.
 * If not it will request the FIXSessionFactory to create a new session in the initiator mode.
 * <p/>
 * To create a new FIX session (in either mode) the FIXSessionFactory has to create a LogFactory (nullable),
 * and a MessageStoreFactroy. By default this implementation attempts to pass null as the LogFactory and a
 * MemoryStoreFactory as the MessageStoreFactory. These can be configured in the services.xml as follows.
 * <p/>
 * <parameter name="transport.fix.AcceptorLogger">file</parameter>
 * (acceptable values: console, file, jdbc)
 * <p/>
 * <parameter name="transport.fix.AcceptorMessageStore">file</parameter>
 * (acceptable values: file, jdbc, memory, sleepycat)
 * <p/>
 * The configuraion details related to these factories has to be specified in the FIX configuration file
 * as requested by the Quickfix/J API.
 */
public class FIXSessionFactory {

    /** A Map Containing all the FIX Acceptors created by this factory, keyed by the service name */
    private Map<String,Acceptor> acceptorStore;
    /** A Map containing all the FIX Initiators created by this factory, keyed by FIX EPR */
    private Map<String,Initiator> initiatorStore;
    /** A Map containing all the FIX application created for initiators, keyed by FIX EPR */
    private Map<String, Application> applicationStore;
    /** An ApplicationFactory handles creating FIX Applications (FIXIncomingMessageHandler Objects) */
    private FIXApplicationFactory applicationFactory;

    private Log log;

    public FIXSessionFactory(FIXApplicationFactory applicationFactory) {
        this.applicationFactory = applicationFactory;
        this.log = LogFactory.getLog(this.getClass());
        this.acceptorStore = new HashMap<String,Acceptor>();
        this.initiatorStore = new HashMap<String, Initiator>();
        this.applicationStore = new HashMap<String, Application>();
    }

    /**
     * Get the FIX configuration settings and initialize a new FIX session for the specified
     * service. Create an Acceptor and a new FIX Application. Put the Acceptor into the
     * acceptorStore keyed by the service name and start it.
     *
     * @param service the AxisService
     */
    public void createFIXAcceptor(AxisService service) {

        //Try to get an InputStream to the FIX configuration file
        InputStream fixConfigStream = getFIXConfigAsStream(service, true);

        if (fixConfigStream != null) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug ("Initializing a new FIX session for the service " + service.getName());
                }

                SessionSettings settings = new SessionSettings(fixConfigStream);
                MessageStoreFactory storeFactory = getMessageStoreFactory(service, settings, true);
                MessageFactory messageFactory = new DefaultMessageFactory();
                quickfix.LogFactory logFactory = getLogFactory(service, settings, true);
                //Get a new FIX Application
                Application messageHandler = applicationFactory.getFIXApplication(service, true);
                //Create a new FIX Acceptor
                Acceptor acceptor = new SocketAcceptor(
                        messageHandler,
                        storeFactory,
                        settings,
                        logFactory,
                        messageFactory);

                acceptorStore.put(service.getName(),acceptor);
                acceptor.start();
                return;
            } catch (ConfigError e) {
                log.error("Error in the specified FIX configuration. Unable to initialize a " +
                        "FIX session for the service " + service.getName(), e);
            }
        }
        log.error("Unable to initialize a FIX session for the service " + service.getName());
    }

    /**
     * Extract the parameters embedded in the given EPR and initialize a new FIX session.
     * Create a new FIX initiator and a new FIX Application.Put the initiator into the
     * initiatorStore keyed by the EPR and start the initiator.
     *
     * @param fixEPR the EPR to send FIX messages
     * @param service the AxisService
     * @param sessionID the SessionID of the session created
     * @throws org.apache.axis2.AxisFault Exception thrown
     */
    public void createFIXInitiator(String fixEPR, AxisService service, SessionID sessionID) throws AxisFault {

        if (log.isDebugEnabled()) {
            log.debug("Initializing a new FIX initiator for the service " + service.getName());
        }
        SessionSettings settings;
        InputStream fixConfigStream = getFIXConfigAsStream(service, false);

        if (fixConfigStream == null) {
            settings = new SessionSettings();
            settings.setLong(sessionID, FIXConstants.HEART_BY_INT, FIXConstants.DEFAULT_HEART_BT_INT_VALUE);
            settings.setString(sessionID, FIXConstants.START_TIME, FIXConstants.DEFAULT_START_TIME_VALUE);
            settings.setString(sessionID, FIXConstants.END_TIME, FIXConstants.DEFAULT_END_TIME_VALUE);
        } else {
            try {
                settings = new SessionSettings(fixConfigStream);
            } catch (ConfigError e) {
                throw new AxisFault("Error in the specified FIX configuration. Unable to initialize a " +
                        "FIX session for the service " + service.getName(), e);
            }
        }

        Hashtable<String,String> properties = FIXUtils.getProperties(fixEPR);
        Iterator<String> keys = properties.keySet().iterator();
        while (keys.hasNext()) {
            String currentKey = keys.next();
            settings.setString(sessionID, currentKey, properties.get(currentKey));
        }

        String[] socketAddressElements = FIXUtils.getSocketAddressElements(fixEPR);
        settings.setString(sessionID, FIXConstants.CONNECTION_TYPE, FIXConstants.FIX_INITIATOR);
        settings.setString(sessionID, FIXConstants.SOCKET_CONNECT_HOST, socketAddressElements[0]);
        settings.setString(sessionID, FIXConstants.SOCKET_CONNECT_PORT, socketAddressElements[1]);

        quickfix.LogFactory logFactory = getLogFactory(service, settings, false);
        MessageStoreFactory storeFactory = getMessageStoreFactory(service, settings, false);
        MessageFactory messageFactory = new DefaultMessageFactory();
        //Get a new FIX application
        Application messageHandler = applicationFactory.getFIXApplication(service, false);

        try {
           //Create a new FIX initiator
            Initiator initiator = new SocketInitiator(
                    messageHandler,
                    storeFactory,
                    settings,
                    logFactory,
                    messageFactory);

            initiatorStore.put(fixEPR, initiator);
            applicationStore.put(fixEPR, messageHandler);
            initiator.start();

            FIXIncomingMessageHandler fixMessageHandler = (FIXIncomingMessageHandler) messageHandler;
            log.info("Waiting for logon procedure to complete...");
            fixMessageHandler.acquire();

        } catch (ConfigError e) {
            throw new AxisFault("Error in the specified FIX configuration. Unable to initialize a " +
                    "FIX initiator.", e);
        } catch (InterruptedException ignore) { }
    }

    /**
     * Get the FIX Acceptor for the specified service from the sessionStore Map and
     * stop it. Then remove the Acceptor from the Map.
     *
     * @param service the AxisService
     */
    public void disposeFIXAcceptor(AxisService service) {
        if (log.isDebugEnabled()) {
            log.debug("Stopping the FIX acceptor for the service " + service.getName());
        }
        //Get the Acceptor for the service
        Acceptor acceptor = acceptorStore.get(service.getName());
        if (acceptor != null) {
            //Stop the Acceptor
            acceptor.stop();
            log.info("FIX session for service " + service.getName() + " terminated...");
            //Remove the Acceptor from the store
            acceptorStore.remove(service.getName());
        }
    }

    /**
     * Returns an array of Strings representing EPRs for the specified service
     *
     * @param serviceName the name of the service
     * @param ip the IP address of the host
     * @return an array of EPRs for the specified service
     */
    public String[] getServiceEPRs(String serviceName, String ip) {
        if (log.isDebugEnabled()) {
            log.debug("Getting EPRs for the service " + serviceName);
        }
        //Get the acceptpr for the specified service
        SocketAcceptor acceptor = (SocketAcceptor) acceptorStore.get(serviceName);

        if (acceptor != null) {
            return FIXUtils.generateEPRs(acceptor, serviceName, ip);
        } else
            return null;
    }

    /**
     * Finds a FIX Acceptor for the specified service from the acceptorStore
     *
     * @param serviceName the name of the AxisService
     * @return a FIX Acceptor for the service
     */
    public Acceptor getAccepter(String serviceName) {
        return acceptorStore.get(serviceName);
    }

    /**
     * Finds a FIX initiator for the specified EPR from the initiatorStore
     *
     * @param fixEPR a valid FIX EPR
     * @return  a FIX initiator for the EPR
     */
   public Initiator getInitiator(String fixEPR) {
        return initiatorStore.get(fixEPR);
    }

    /**
     * Get the FIX configuration URL from the services.xml.
     *
     * @param service the AxisService
     * @param acceptor boolean value indicating the FIX application type
     * @return an InputStream to the FIX configuration file/resource
     */
    private InputStream getFIXConfigAsStream(AxisService service, boolean acceptor) {
        InputStream fixConfigStream = null;
        Parameter fixConfigURLParam;

        if (acceptor) {
            fixConfigURLParam = service.getParameter(FIXConstants.FIX_ACCEPTOR_CONFIG_URL_PARAM);
        } else {
            fixConfigURLParam = service.getParameter(FIXConstants.FIX_INITIATOR_CONFIG_URL_PARAM);
        }

        if (fixConfigURLParam != null) {
            String fixConfigURLValue = fixConfigURLParam.getValue().toString();
            try {
                URL url = new URL(fixConfigURLValue);
                fixConfigStream = url.openStream();
            } catch (MalformedURLException e) {
                log.error("The FIX configuration URL " + fixConfigURLValue + " is" +
                       " malformed.", e);
            } catch (IOException e) {
                log.error("Error while reading from the URL " + fixConfigURLValue, e);
            }
        } else {
            log.error("FIX configuration URL is not specified for the service " + service.getName());
        }

        return fixConfigStream;
    }

    /**
     * Creates a Quickfix LogFactory object for logging as specified in the services.xml and
     * the FIX configuration file. Default is null.
     *
     * @param service the AxisService
     * @param settings SessionSettings to be used with the service
     * @param acceptor a boolean value indicating the type of the FIX application
     * @return a LogFactory for the FIX application
     */
    private quickfix.LogFactory getLogFactory(AxisService service, SessionSettings settings,
                                              boolean acceptor) {

        quickfix.LogFactory logFactory = null;
        Parameter fixLogMethod;

        //Read the parameter from the services.xml
        if (acceptor) {
           fixLogMethod = service.getParameter(FIXConstants.FIX_ACCEPTOR_LOGGER_PARAM);
        } else {
            fixLogMethod = service.getParameter(FIXConstants.FIX_INITIATOR_LOGGER_PARAM);
        }

        if (fixLogMethod != null) {
               String method = fixLogMethod.getValue().toString();
                log.info("FIX logging method = " + method);

                if (FIXConstants.FILE_BASED_MESSAGE_LOGGING.equals(method)) {
                    logFactory = new FileLogFactory(settings);
                } else if (FIXConstants.JDBC_BASED_MESSAGE_LOGGING.equals(method)) {
                    logFactory = new JdbcLogFactory(settings);
                } else if (FIXConstants.CONSOLE_BASED_MESSAGE_LOGGING.equals(method)) {
                    logFactory = new ScreenLogFactory();
                } else {
                    log.warn("Invalid acceptor log method " + method + ". Using defaults.");
                }
        }
        return logFactory;
    }

    /**
     * Creates a Quickfix MessageStoreFactory for storing FIX messages as specified in the services.xml
     * and the FIX configuration file. Default is FileStoreFactory.
     *
     * @param service the AxisService
     * @param settings SessionSettings to be used with the service
     * @param acceptor a boolean value indicating the type of the FIX application
     * @return a MessageStoreFactory for the FIX application
     */
    private MessageStoreFactory getMessageStoreFactory(AxisService service, SessionSettings settings,
                                                       boolean acceptor) {

        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        Parameter msgLogMethod;

        //Read the parameter from the services.xml
        if (acceptor) {
            msgLogMethod = service.getParameter(FIXConstants.FIX_ACCEPTOR_MESSAGE_STORE_PARAM);
        } else {
            msgLogMethod = service.getParameter(FIXConstants.FIX_INITIATOR_MESSAGE_STORE_PARAM);
        }

        if (msgLogMethod != null) {
            String method = msgLogMethod.getValue().toString();
            log.info("FIX message logging method = " + method);

            if (FIXConstants.JDBC_BASED_MESSAGE_STORE.equals(method)) {
                storeFactory = new JdbcStoreFactory(settings);
            } else if (FIXConstants.SLEEPYCAT_BASED_MESSAGE_STORE.equals(method)) {
                storeFactory = new SleepycatStoreFactory(settings);
            } else if (FIXConstants.FILE_BASED_MESSAGE_STORE.equals(method)) {
                storeFactory = new FileStoreFactory(settings);
            } else if (!FIXConstants.MEMORY_BASED_MESSAGE_STORE.equals(method)) {
                log.warn("Invalid message store " + method + ". Using defaults.");
            }
        }

        return storeFactory;
    }
    
    public Application getApplication(String fixEPR) {
        return applicationStore.get(fixEPR);
    }
}