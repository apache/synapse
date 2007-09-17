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

package org.apache.synapse.core.axis2;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.Deployer;
import org.apache.axis2.deployment.DeploymentClassLoader;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.config.xml.MediatorFactoryFinder;
import org.apache.synapse.config.xml.MediatorSerializer;
import org.apache.synapse.config.xml.MediatorSerializerFinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * This will support the hot deployment and hot update of the mediators at runtime using the
 * Axis2 concepts of deployers
 */
public class MediatorDeployer implements Deployer {

    /**
     * Holds the log variable for logging purposes
     */
    private static final Log log = LogFactory.getLog(MediatorDeployer.class);

    /**
     * ConfigurationContext of Axis2
     */
    private ConfigurationContext cfgCtx = null;

    /**
     * Initializes the Deployer
     *
     * @param configurationContext - ConfigurationContext of Axis2 from which
     *  the deployer is initialized
     */
    public void init(ConfigurationContext configurationContext) {
        this.cfgCtx = configurationContext;
    }

    /**
     * This will be called when there is a change in the specified deployement
     * folder (in the axis2.xml) and this will load the relevant classe to the system and
     * registeres them with the MediatorFactoryFinder
     *
     * @param deploymentFileData - describes the updated file
     * @throws DeploymentException - in case an error on the deployment
     */
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {

        log.info("Loading mediator from: " + deploymentFileData.getAbsolutePath());

        // get the context class loader for the later restore of the context class loader
        ClassLoader prevCl = Thread.currentThread().getContextClassLoader();

        try {

            DeploymentClassLoader urlCl = new DeploymentClassLoader(
                    new URL[]{deploymentFileData.getFile().toURL()}, null, prevCl);
            Thread.currentThread().setContextClassLoader(urlCl);

            // MediatorFactory registration
            URL facURL = urlCl.findResource(
                    "META-INF/services/org.apache.synapse.config.xml.MediatorFactory");
            if (facURL != null) {
                InputStream facStream = facURL.openStream();
                InputStreamReader facreader = new InputStreamReader(facStream);

                StringBuffer facSB = new StringBuffer();
                int c;
                while ((c = facreader.read()) != -1) {
                    facSB.append((char) c);
                }

                String[] facClassName = facSB.toString().split("\n");
                for (int i=0; i<facClassName.length; i++) {
                    log.info("Registering the Mediator factory: " + facClassName[i]);
                    Class facClass = urlCl.loadClass(facClassName[i]);
                    MediatorFactory facInst = (MediatorFactory) facClass.newInstance();
                    MediatorFactoryFinder.getInstance()
                            .getFactoryMap().put(facInst.getTagQName(), facClass);
                    log.info("Mediator loaded and registered for " +
                            "the tag name: " + facInst.getTagQName());
                }
            } else {
                handleException("Unable to find the MediatorFactory implementation. " +
                        "Unable to register the MediatorFactory with the FactoryFinder");
            }

            // MediatorSerializer registration
            URL serURL = urlCl.findResource(
                    "META-INF/services/org.apache.synapse.config.xml.MediatorSerializer");
            if (serURL != null) {
                InputStream serStream = serURL.openStream();
                InputStreamReader serReader = new InputStreamReader(serStream);

                StringBuffer serSB = new StringBuffer();
                int c;
                while ((c = serReader.read()) != -1) {
                    serSB.append((char) c);
                }

                String[] serClassName = serSB.toString().split("\n");
                for (int i=0; i<serClassName.length; i++) {
                    log.info("Registering the Mediator serializer: " + serClassName[i]);
                    Class serClass = urlCl.loadClass(serClassName[i]);
                    MediatorSerializer serInst = (MediatorSerializer) serClass.newInstance();
                    MediatorSerializerFinder.getInstance()
                            .getSerializerMap().put(serInst.getMediatorClassName(), serInst);
                    log.info("Mediator loaded and registered for " +
                            "the serialization as: " + serInst.getMediatorClassName());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to find the MediatorSerializer implementation. " +
                            "Unable to register the MediatorSerializer with the SerializerFinder");
                }
            }

        } catch (IOException e) {
            handleException("I/O error in reading the mediator jar file", e);
        } catch (ClassNotFoundException e) {
            handleException("Unable to find the specified class on the path or in the jar file", e);
        } catch (IllegalAccessException e) {
            handleException("Unable to load the class from the jar", e);
        } catch (InstantiationException e) {
            handleException("Unable to instantiate the class specified", e);
        } finally {
            // restore the class loader back
            if (log.isDebugEnabled()) {
                log.debug("Restoring the context class loader to the original");
            }
            Thread.currentThread().setContextClassLoader(prevCl);
        }
    }

    /**
     * This will not be implemented because we do not support changing the directory at runtime
     *
     * @param string -
     */
    public void setDirectory(String string) {
        // we do not support changing the directory
    }

    /**
     * This will not be implemented because we do not support changing the extension at runtime
     *
     * @param string -
     */
    public void setExtension(String string) {
        // we do not support changing the extension
    }

    /**
     * This will be called when a particulr jar file is deleted from the specified folder
     *
     * @param string - filename of the deleted file
     * @throws DeploymentException - incase of an error in undeployment
     */
    public void unDeploy(String string) throws DeploymentException {
        // todo: implement the undeployement
    }

    private void handleException(String message, Exception e) throws DeploymentException {
        if (log.isDebugEnabled()) {
            log.debug(message, e);
        }
        throw new DeploymentException(message, e);
    }

    private void handleException(String message) throws DeploymentException {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
        throw new DeploymentException(message);
    }
}
