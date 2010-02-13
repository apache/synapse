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
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.Deployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.Parameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ServerManager;
import org.apache.synapse.ServerState;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the generic logic for the synapse artifact deployment and provide a deployment framework
 * for the synapse.</p>
 *
 * <p>Any  synapse artifact which requires the hot deployment or hot update features should extend this and
 * just needs to concentrate on the deployment logic. By default setting the file extension and directory dynamically
 * is not supported.
 *
 * @see org.apache.axis2.deployment.Deployer
 */
public abstract class AbstractSynapseArtifactDeployer implements Deployer {

    private static final Log log = LogFactory.getLog(AbstractSynapseArtifactDeployer.class);
    protected ConfigurationContext cfgCtx;

    /**
     * Initializes the Synapse artifact deployment
     * 
     * @param configCtx Axis2 ConfigurationContext
     */
    public void init(ConfigurationContext configCtx) {
        this.cfgCtx = configCtx;
    }

    /**
     *  This method is called by the axis2 deployment framework and it performs a synapse artifact specific
     * yet common across all the artifacts, set of tasks and delegate the actual deployment to the respective
     * artifact deployers.
     *
     * @param deploymentFileData file to be used for the deployment
     * @throws DeploymentException in-case of an error in deploying the file
     * 
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#deploySynapseArtifact(
     * org.apache.axiom.om.OMElement, String)
     */
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {

        if (ServerManager.getInstance().getServerState() != ServerState.STARTED) {
            // synapse server has not yet being started
            if (log.isDebugEnabled()) {
                log.debug("Skipped the artifact deployment (since the Synapse " +
                        "server doesn't seem to be started yet), from file : "
                        + deploymentFileData.getAbsolutePath());
            }
            return;
        }

        String filename = deploymentFileData.getAbsolutePath();
        try {
            InputStream in = new FileInputStream(filename);
            try {
                // construct the xml element from the file, it has to be XML,
                // since all synapse artifacts are XML based
                OMElement element = new StAXOMBuilder(
                        StAXUtils.createXMLStreamReader(in)).getDocumentElement();
                String artifatcName = deploySynapseArtifact(element, filename);
                if (artifatcName != null) {
                    FileNameToArtifactNameHolder.getInstance().addArtifact(filename, artifatcName);
                }
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            throw new DeploymentException("Error reading "
                    + filename + " : " + ex.getMessage(), ex);
        } catch (XMLStreamException ex) {
            throw new DeploymentException("Error parsing "
                    + filename + " : " + ex.getMessage(), ex);
        } catch (OMException ex) {
            throw new DeploymentException("Error parsing "
                    + filename + " : " + ex.getMessage(), ex);
        }
    }

    /**
     * This is the method called by the axis2 framework for undeployment of the artifacts. As in the deploy
     * case this performs some common tasks across all the synapse artifacts and fall back to the artifact
     * specific logic of undeployment.
     *
     * @param fileName file describing the artifact to be undeployed
     * @throws DeploymentException in case of an error in undeployment
     *
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#undeploySynapseArtifact(String) 
     */
    public void unDeploy(String fileName) throws DeploymentException {
        FileNameToArtifactNameHolder holder = FileNameToArtifactNameHolder.getInstance();
        if (holder.containsFileName(fileName)) {
            undeploySynapseArtifact(holder.getArtifactNameForFile(fileName));
            holder.removeArtifactWithFileName(fileName);
        } else {
            throw new DeploymentException("Artifact representing the filename " + fileName
                    + " is not deployed on Synapse");
        }
    }

    // We do not support dynamically setting the directory nor the extension
    public void setDirectory(String directory) {}
    public void setExtension(String extension) {}

    /**
     * All synapse artifact deployers MUST implement this method and it handles artifact specific deployment
     * tasks of those artifacts.
     *
     * @param artifactConfig built element representing the artifact to be deployed loaded from the file
     * @param fileName file name from which this artifact is being loaded
     * @return String artifact name created by the deployment task
     * 
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#deploy(
     * org.apache.axis2.deployment.repository.util.DeploymentFileData)
     */
    public abstract String deploySynapseArtifact(OMElement artifactConfig, String fileName);

    /**
     * All synapse artifact deployers MUST implement this method and it handles artifact specific undeployment
     * tasks of those artifacts.
     *
     * @param artifactName name of the artifact to be undeployed
     *
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#unDeploy(String) 
     */
    public abstract void undeploySynapseArtifact(String artifactName);

    protected SynapseConfiguration getSynapseConfiguration() throws DeploymentException {
        Parameter synCfgParam =
                cfgCtx.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_CONFIG);
        if (synCfgParam == null) {
            throw new DeploymentException("SynapseConfiguration not found. " +
                    "Are you sure that you are running Synapse?");
        }
        return (SynapseConfiguration) synCfgParam.getValue();
    }

    protected SynapseEnvironment getSynapseEnvironment() throws DeploymentException {
        Parameter synCfgParam =
                cfgCtx.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_ENV);
        if (synCfgParam == null) {
            throw new DeploymentException("SynapseEnvironment not found. " +
                    "Are you sure that you are running Synapse?");
        }
        return (SynapseEnvironment) synCfgParam.getValue();
    }
}
