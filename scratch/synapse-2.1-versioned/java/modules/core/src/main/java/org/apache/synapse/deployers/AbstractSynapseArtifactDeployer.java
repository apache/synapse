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
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.util.XMLPrettyPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.ServerContextInformation;
import org.apache.synapse.ServerState;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;

import java.io.*;
import java.util.Properties;

/**
 * Implements the generic logic for the synapse artifact deployment and provide a deployment
 * framework for the synapse.</p>
 *
 * <p>Any  synapse artifact which requires the hot deployment or hot update features should extend
 * this and just needs to concentrate on the deployment logic. By default setting the file
 * extension and directory dynamically is not supported.
 *
 * @see org.apache.axis2.deployment.Deployer
 */
public abstract class AbstractSynapseArtifactDeployer extends AbstractDeployer {

    private static final Log log = LogFactory.getLog(AbstractSynapseArtifactDeployer.class);
    protected  Log deployerLog;
    protected ConfigurationContext cfgCtx;

    protected AbstractSynapseArtifactDeployer() {
        deployerLog = LogFactory.getLog(this.getClass());
    }

    /**
     * Initializes the Synapse artifact deployment
     * 
     * @param configCtx Axis2 ConfigurationContext
     */
    public void init(ConfigurationContext configCtx) {
        this.cfgCtx = configCtx;
    }

    private boolean isHotDeploymentEnabled() {
        try {
            return getSynapseConfiguration().isAllowHotUpdate();
        } catch (DeploymentException e) {
            log.warn("Error while retrieving the SynapseConfiguration", e);
            return false;
        }
    }

    /**
     * This method is called by the axis2 deployment framework and it performs a synapse artifact
     * specific yet common across all the artifacts, set of tasks and delegate the actual deployment
     * to the respective artifact deployers.
     *
     * @param deploymentFileData file to be used for the deployment
     * @throws DeploymentException in-case of an error in deploying the file
     * 
     * @see AbstractSynapseArtifactDeployer#deploySynapseArtifact(org.apache.axiom.om.OMElement,
     * String,java.util.Properties)
     */
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {

        String filename = SynapseArtifactDeploymentStore.getNormalizedAbsolutePath(
                deploymentFileData.getAbsolutePath());
        if (log.isDebugEnabled()) {
            log.debug("Deployment of the synapse artifact from file : " + filename + " : STARTED");
        }

        if (getServerContextInformation().getServerState() != ServerState.STARTED) {
            // synapse server has not yet being started
            if (log.isDebugEnabled()) {
                log.debug("Skipped the artifact deployment (since the Synapse " +
                        "server doesn't seem to be started yet), from file : "
                        + deploymentFileData.getAbsolutePath());
            }
            return;
        }

        if (!isHotDeploymentEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Hot deployment has been suspended - Ignoring");
            }
            return;
        }

        SynapseArtifactDeploymentStore deploymentStore =
                getSynapseConfiguration().getArtifactDeploymentStore();

        // check whether this is triggered by a restore, if it is a restore we do not want to
        // deploy it again
        if (deploymentStore.isRestoredFile(filename)) {
            if (log.isDebugEnabled()) {
                log.debug("Restored artifact detected with filename : " + filename);
            }
            // only one deployment trigger can happen after a restore and hence remove it from
            // restoredFiles at the first hit, allowing the further deployments/updates to take
            // place as usual
            deploymentStore.removeRestoredFile(filename);
            return;
        }
        
        try {
            InputStream in = FileUtils.openInputStream(new File(filename));
            try {
                // construct the xml element from the file, it has to be XML,
                // since all synapse artifacts are XML based
                OMElement element = OMXMLBuilderFactory.createOMBuilder(in).getDocumentElement();
                Properties properties = new Properties();
                properties.put(SynapseConstants.RESOLVE_ROOT, getSynapseEnvironment()
                        .getServerContextInformation()
                        .getServerConfigurationInformation().getResolveRoot());
                String artifactName = null;
                if (deploymentStore.isUpdatingArtifact(filename)) {

                    if (log.isDebugEnabled()) {
                        log.debug("Updating artifact detected with filename : " + filename);
                    }
                    // this is an hot-update case
                    String existingArtifactName
                            = deploymentStore.getUpdatingArtifactWithFileName(filename);
                    deploymentStore.removeUpdatingArtifact(filename);
                    try {
                        artifactName = updateSynapseArtifact(
                                element, filename, existingArtifactName, properties);
                    } catch (SynapseArtifactDeploymentException sade) {
                        log.error("Update of the Synapse Artifact from file : "
                                + filename + " : Failed!", sade);
                        log.info("The updated file has been backed up into : "
                                + backupFile(deploymentFileData.getFile()));
                        log.info("Restoring the existing artifact into the file : " + filename);
                        restoreSynapseArtifact(existingArtifactName);
                        artifactName = existingArtifactName;
                    }
                } else {
                    // new artifact hot-deployment case
                    try {
                        artifactName = deploySynapseArtifact(element, filename, properties);
                    } catch (SynapseArtifactDeploymentException sade) {
                        log.error("Deployment of the Synapse Artifact from file : "
                                + filename + " : Failed!", sade);
                        log.info("The file has been backed up into : "
                                + backupFile(deploymentFileData.getFile()));
                    }
                }
                if (artifactName != null) {
                    deploymentStore.addArtifact(filename, artifactName);
                }
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            handleDeploymentError("Deployment of synapse artifact failed. Error reading "
                    + filename + " : " + ex.getMessage(), ex, filename);
        } catch (OMException ex) {
            handleDeploymentError("Deployment of synapse artifact failed. Error parsing "
                    + filename + " : " + ex.getMessage(), ex, filename);
        }

        if (log.isDebugEnabled()) {
            log.debug("Deployment of the synapse artifact from file : "
                    + filename + " : COMPLETED");
        }
    }

    /**
     * This is the method called by the axis2 framework for undeployment of the artifacts. As in
     * the deploy case this performs some common tasks across all the synapse artifacts and fall
     * back to the artifact specific logic of undeployment.
     *
     * @param fileName file describing the artifact to be undeployed
     * @throws DeploymentException in case of an error in undeployment
     *
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#undeploySynapseArtifact(
     * String)
     */
    public void undeploy(String fileName) throws DeploymentException {

        if (!isHotDeploymentEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Hot deployment has been suspended - Ignoring");
            }
            return;
        }

        fileName = SynapseArtifactDeploymentStore.getNormalizedAbsolutePath(fileName);
        if (log.isDebugEnabled()) {
            log.debug("Undeployment of the synapse artifact from file : "
                    + fileName + " : STARTED");
        }

        SynapseArtifactDeploymentStore deploymentStore =
                getSynapseConfiguration().getArtifactDeploymentStore();

        // We want to eliminate the undeployment when we are backing up these files
        if (deploymentStore.isBackedUpArtifact(fileName)) {

            if (log.isDebugEnabled()) {
                log.debug("BackedUp artifact detected with filename : " + fileName);
            }
            // only one undeployment trigger can happen after a backup and hence remove it from
            // backedUpFiles at the first hit, allowing the further undeployment to take place
            // as usual
            deploymentStore.removeBackedUpArtifact(fileName);
            return;
        }

        if (deploymentStore.containsFileName(fileName)) {
            File undeployingFile = new File(fileName);
            // axis2 treats Hot-Update as (Undeployment + deployment), where synapse needs to
            // differentiate the Hot-Update from the above two, since it needs some validations for
            // a real undeployment. Also this makes sure a zero downtime of the synapse artifacts
            // which are being Hot-deployed
            if (undeployingFile.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("Marking artifact as updating from file : " + fileName);
                }
                // if the file exists, which means it has been updated and is a Hot-Update case
                if (!deploymentStore.isRestoredFile(fileName)) {
                    deploymentStore.addUpdatingArtifact(
                            fileName, deploymentStore.getArtifactNameForFile(fileName));
                    deploymentStore.removeArtifactWithFileName(fileName);
                }
            } else {
                // if the file doesn't exists then it is an actual undeployment
                String artifactName = deploymentStore.getArtifactNameForFile(fileName);
                try {
                    undeploySynapseArtifact(artifactName);
                    deploymentStore.removeArtifactWithFileName(fileName);
                } catch (SynapseArtifactDeploymentException sade) {
                    log.error("Unable to undeploy the artifact from file : " + fileName, sade);
                    log.info("Restoring the artifact into the file : " + fileName);
                    restoreSynapseArtifact(artifactName);
                }
            }
        } else {
            String msg = "Artifact representing the filename "
                    + fileName + " is not deployed on Synapse";
            log.error(msg);
            throw new DeploymentException(msg);
        }

        if (log.isDebugEnabled()) {
            log.debug("UnDeployment of the synapse artifact from file : "
                    + fileName + " : COMPLETED");
        }
    }

    // We do not support dynamically setting the directory nor the extension
    public void setDirectory(String directory) {}
    public void setExtension(String extension) {}

    /**
     * All synapse artifact deployers MUST implement this method and it handles artifact specific
     * deployment tasks of those artifacts.
     *
     * @param artifactConfig built element representing the artifact to be deployed loaded
     * from the file
     * @param fileName file name from which this artifact is being loaded
     * @param properties Properties associated with the artifact
     * @return String artifact name created by the deployment task
     * 
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#deploy(
     * org.apache.axis2.deployment.repository.util.DeploymentFileData)
     */
    public abstract String deploySynapseArtifact(OMElement artifactConfig, String fileName,
                                                 Properties properties);

    /**
     * All synapse artifact deployers MUST implement this method and it handles artifact specific
     * update tasks of those artifacts.
     *
     * @param artifactConfig built element representing the artifact to be deployed loaded
     * from the file
     * @param fileName file name from which this artifact is being loaded
     * @param existingArtifactName name of the artifact that was being deployed using
     * the updated file
     * @param properties bag of properties with the additional information
     * @return String artifact name created by the update task
     */
    public abstract String updateSynapseArtifact(OMElement artifactConfig, String fileName,
                                                 String existingArtifactName,
                                                 Properties properties);

    /**
     * All synapse artifact deployers MUST implement this method and it handles artifact specific
     * undeployment tasks of those artifacts.
     *
     * @param artifactName name of the artifact to be undeployed
     *
     * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer#undeploy(String)
     */
    public abstract void undeploySynapseArtifact(String artifactName);

    /**
     * All synapse artifact deployers MUST implement this method and it handles artifact specific
     * restore tasks of those artifacts upon a failure of an update or undeployment.
     *
     * @param artifactName name of the artifact to be restored
     */
    public abstract void restoreSynapseArtifact(String artifactName);

    protected SynapseConfiguration getSynapseConfiguration() throws DeploymentException {
        Parameter synCfgParam =
                cfgCtx.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_CONFIG);
        if (synCfgParam == null) {
            throw new DeploymentException("SynapseConfiguration not found. " +
                    "Unable to continue the deployment operation.");
        }
        return (SynapseConfiguration) synCfgParam.getValue();
    }

    protected SynapseEnvironment getSynapseEnvironment() throws DeploymentException {
        Parameter synCfgParam =
                cfgCtx.getAxisConfiguration().getParameter(SynapseConstants.SYNAPSE_ENV);
        if (synCfgParam == null) {
            throw new DeploymentException("SynapseEnvironment not found. " +
                    "Unable to continue the deployment operation.");
        }
        return (SynapseEnvironment) synCfgParam.getValue();
    }

    protected ServerConfigurationInformation getServerConfigurationInformation() 
            throws DeploymentException {
        Parameter serverCfgParam =
                cfgCtx.getAxisConfiguration().getParameter(
                        SynapseConstants.SYNAPSE_SERVER_CONFIG_INFO);
        if (serverCfgParam == null) {
            throw new DeploymentException("SynapseConfigurationInformation not found. " +
                    "Unable to continue the deployment operation.");
        }
        return (ServerConfigurationInformation) serverCfgParam.getValue();
    }

    protected ServerContextInformation getServerContextInformation()
            throws DeploymentException {
        Parameter serverCtxParam =
                cfgCtx.getAxisConfiguration().getParameter(
                        SynapseConstants.SYNAPSE_SERVER_CTX_INFO);
        if (serverCtxParam == null) {
            throw new DeploymentException("ServerContextInformation not found. " +
                    "Unable to continue the deployment operation.");
        }
        return (ServerContextInformation) serverCtxParam.getValue();
    }

    protected void writeToFile(OMElement content, String fileName) throws Exception {
        // this is not good, but I couldn't think of a better design :-(
        SynapseArtifactDeploymentStore deploymentStore =
                getSynapseConfiguration().getArtifactDeploymentStore();
        deploymentStore.addRestoredArtifact(fileName);
        OutputStream out = FileUtils.openOutputStream(new File(fileName));
        XMLPrettyPrinter.prettify(content, out);
        out.flush();
        out.close();
    }

    protected void waitForCompletion() {
        long timeout = 2000L;
        Parameter param = cfgCtx.getAxisConfiguration().getParameter("hotupdate.timeout");
        if (param != null && param.getValue() != null) {
            timeout = Long.parseLong(param.getValue().toString());
        }

        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ignored) {

        }
    }

    protected void handleSynapseArtifactDeploymentError(String msg) {
        deployerLog.error(msg);
        throw new SynapseArtifactDeploymentException(msg);
    }

    protected void handleSynapseArtifactDeploymentError(String msg, Exception e) {
        deployerLog.error(msg, e);
        throw new SynapseArtifactDeploymentException(msg, e);
    }

    private void handleDeploymentError(String msg, Exception e, String fileName)
            throws DeploymentException {
        fileName = SynapseArtifactDeploymentStore.getNormalizedAbsolutePath(fileName);
        log.error(msg, e);
        SynapseArtifactDeploymentStore deploymentStore =
                getSynapseConfiguration().getArtifactDeploymentStore();
        if (deploymentStore.isUpdatingArtifact(fileName)) {
            backupFile(new File(fileName));
            log.info("Restoring the existing artifact into the file : " + fileName);
            restoreSynapseArtifact(deploymentStore.getUpdatingArtifactWithFileName(fileName));
            deploymentStore.addArtifact(
                    fileName, deploymentStore.getUpdatingArtifactWithFileName(fileName));
            deploymentStore.removeUpdatingArtifact(fileName);
        }
    }

    private String backupFile(File file) throws DeploymentException {
        String filePath = SynapseArtifactDeploymentStore.getNormalizedAbsolutePath(
                file.getAbsolutePath());
        SynapseArtifactDeploymentStore deploymentStore =
                getSynapseConfiguration().getArtifactDeploymentStore();

        deploymentStore.addBackedUpArtifact(filePath);
        String backupFilePath = filePath + ".back";
        int backupIndex = 0;
        while (backupIndex >= 0) {
            if (new File(backupFilePath).exists()) {
                backupIndex++;
                backupFilePath = filePath + "." + backupIndex + ".back";
            } else {
                backupIndex = -1;
                try {
                    FileUtils.moveFile(file, new File(backupFilePath));
                } catch (IOException e) {
                    handleSynapseArtifactDeploymentError("Error while backing up the artifact: " +
                            file.getName(), e);
                }
            }
        }
        return backupFilePath;
    }
}
