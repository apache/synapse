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
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.config.xml.ProxyServiceFactory;
import org.apache.synapse.core.axis2.ProxyService;

/**
 *  Handles the <code>ProxyService</code> deployment and undeployment tasks
 *
 * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer
 */
public class ProxyServiceDeployer extends AbstractSynapseArtifactDeployer {

    private static Log log = LogFactory.getLog(SequenceDeployer.class);

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName) {

        if (log.isDebugEnabled()) {
            log.debug("ProxyService Deployment from file : " + fileName + " : Started");
        }

        try {
            ProxyService proxy = ProxyServiceFactory.createProxy(artifactConfig);
            if (proxy != null) {
                proxy.setFileName(fileName);
                if (log.isDebugEnabled()) {
                    log.debug("ProxyService named '" + proxy.getName()
                            + "' has been built from the file " + fileName);
                }

                if (proxy.getTargetInLineEndpoint() instanceof ManagedLifecycle) {
                    proxy.getTargetInLineEndpoint().init(getSynapseEnvironment());
                }
                if (proxy.getTargetInLineInSequence() != null) {
                    proxy.getTargetInLineInSequence().init(getSynapseEnvironment());
                }
                if (proxy.getTargetInLineOutSequence() != null) {
                    proxy.getTargetInLineOutSequence().init(getSynapseEnvironment());
                }
                if (proxy.getTargetInLineFaultSequence() != null) {
                    proxy.getTargetInLineFaultSequence().init(getSynapseEnvironment());
                }
                
                if (log.isDebugEnabled()) {
                    log.debug("Initialized the ProxyService : " + proxy.getName());
                }
                
                proxy.buildAxisService(getSynapseConfiguration(),
                        getSynapseConfiguration().getAxisConfiguration());
                if (log.isDebugEnabled()) {
                    log.debug("Started the ProxyService : " + proxy.getName());
                }
                getSynapseConfiguration().addProxyService(proxy.getName(), proxy);
                if (log.isDebugEnabled()) {
                    log.debug("ProxyService Deployment from file : " + fileName + " : Completed");
                }
                log.info("ProxyService named '" + proxy.getName()
                        + "' has been deployed from file : " + fileName);
                return proxy.getName();
            } else {
                log.error("ProxyService Deployment Failed. The artifact described in the file "
                        + fileName + " is not a ProxyService");
            }
        } catch (Exception e) {
            log.error("ProxyService Deployment from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("ProxyService Undeployment of the proxy named : "
                    + artifactName + " : Started");
        }
        
        try {
            ProxyService proxy = getSynapseConfiguration().getProxyService(artifactName);
            if (proxy != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Stopping the ProxyService named : " + artifactName);
                }
                proxy.stop(getSynapseConfiguration());
                getSynapseConfiguration().removeProxyService(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("ProxyService Undeployment of the proxy named : "
                            + artifactName + " : Completed");
                }
            } else {
                log.error("Couldn't find the ProxyService named : " + artifactName);
            }
        } catch (Exception e) {
            log.error("ProxyService Undeployement of proxy named : " + artifactName + " : Failed");
        }
    }
}
