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

package org.apache.synapse.deployers;


import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.commons.executors.config.PriorityExecutorFactory;
import org.apache.synapse.commons.executors.config.PriorityExecutorSerializer;
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;

import java.io.File;
import java.util.Properties;

public class PriorityExecutorDeployer extends AbstractSynapseArtifactDeployer {
    private static Log log = LogFactory.getLog(LocalEntryDeployer.class);

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig,
                                        String fileName, Properties properties) {
        if (log.isDebugEnabled()) {
            log.debug("PriorityExecutor Deployment from file : " + fileName + " : Started");
        }

        try {
            PriorityExecutor e = PriorityExecutorFactory.createExecutor(
                    SynapseConstants.SYNAPSE_NAMESPACE, artifactConfig, true, properties);
            if (e != null) {
                e.setFileName((new File(fileName)).getName());
                if (log.isDebugEnabled()) {
                    log.debug("PriorityExecutor with name '" + e.getName()
                            + "' has been built from the file " + fileName);
                }
                getSynapseConfiguration().addPriorityExecutor(e.getName(), e);

                e.init();

                if (log.isDebugEnabled()) {
                    log.debug("PriorityExecutor Deployment from file : " + fileName + " : Completed");
                }
                log.info("PriorityExecutor named '" + e.getName()
                        + "' has been deployed from file : " + fileName);
                return e.getName();
            } else {
                handleSynapseArtifactDeploymentError("PriorityExecutor Deployment Failed. " +
                        "The artifact described in the file " + fileName +
                        " is not a PriorityExecutor");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "PriorityExecutor Deployment from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    @Override
    public String updateSynapseArtifact(OMElement artifactConfig, String fileName,
                                        String existingArtifactName, Properties properties) {
        if (log.isDebugEnabled()) {
            log.debug("PriorityExecutor Update from file : " + fileName + " : Started");
        }

        try {
            PriorityExecutor e = PriorityExecutorFactory.createExecutor(
                    SynapseConstants.SYNAPSE_NAMESPACE, artifactConfig, true, properties);
            if (e != null) {
                e.setFileName((new File(fileName)).getName());
                if (log.isDebugEnabled()) {
                    log.debug("PriorityExecutor with key '" + e.getName()
                            + "' has been built from the file " + fileName);
                }
                PriorityExecutor existingExecutor =
                        getSynapseConfiguration().removeExecutor(existingArtifactName);
                if (!existingArtifactName.equals(e.getName())) {
                    log.info("PriorityExecutor named " + existingArtifactName +
                            " has been Undeployed");
                }

                if (existingExecutor != null) {
                    existingExecutor.destroy();
                }

                getSynapseConfiguration().addPriorityExecutor(e.getName(), e);
                if (log.isDebugEnabled()) {
                    log.debug("PriorityExecutor " + (existingArtifactName.equals(e.getName()) ?
                            "update" : "deployment") + " from file : " + fileName + " : Completed");
                }
                log.info("PriorityExecutor named '" + e.getName()
                        + "' has been " + (existingArtifactName.equals(e.getName()) ?
                            "updated" : "deployed") + " from file : " + fileName);

                e.init();

                return e.getName();
            } else {
                handleSynapseArtifactDeploymentError("PriorityExecutor Update Failed. The artifact " +
                        "described in the file " + fileName + " is not a LocalEntry");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "PriorityExecutor Update from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {
        if (log.isDebugEnabled()) {
            log.debug("PriorityExecutor Undeployment of the entry named : "
                    + artifactName + " : Started");
        }

        try {
            PriorityExecutor e = getSynapseConfiguration().getPriorityExecutors().get(artifactName);
            if (e != null) {
                e = getSynapseConfiguration().removeExecutor(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("PriorityExecutor Undeployment of the entry named : "
                            + artifactName + " : Completed");
                }
                e.destroy();
                log.info("PriorityExecutor named '" + e.getName() + "' has been undeployed");
            } else if (log.isDebugEnabled()) {
                log.debug("PriorityExecutor " + artifactName + " has already been undeployed");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "PriorityExecutor Undeployement of entry named : " +
                            artifactName + " : Failed", e);
        }
    }

    @Override
    public void restoreSynapseArtifact(String artifactName) {
         if (log.isDebugEnabled()) {
            log.debug("Restoring PriorityExecutor with name : " + artifactName + " : Started");
        }

        try {
            PriorityExecutor e = getSynapseConfiguration().getPriorityExecutors().get(artifactName);
            OMElement entryElem = PriorityExecutorSerializer.serialize(null, e,
                    SynapseConstants.SYNAPSE_NAMESPACE);
            if (e.getFileName() != null) {
                String fileName = getServerConfigurationInformation().getSynapseXMLLocation()
                        + File.separator + MultiXMLConfigurationBuilder.EXECUTORS_DIR
                        + File.separator + e.getFileName();
                writeToFile(entryElem, fileName);
                if (log.isDebugEnabled()) {
                    log.debug("Restoring the PriorityExecutor with name : "
                            + artifactName + " : Completed");
                }
                log.info("PriorityExecutor named '" + artifactName + "' has been restored");
            } else {
                handleSynapseArtifactDeploymentError("Couldn't restore the PriorityExecutor named '"
                        + artifactName + "', filename cannot be found");
            }
        } catch (Exception e) {
            handleSynapseArtifactDeploymentError(
                    "Restoring of the PriorityExecutor named '" + artifactName + "' has failed", e);
        }
    }
}
