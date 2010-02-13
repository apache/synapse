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
import org.apache.synapse.config.xml.eventing.EventSourceFactory;
import org.apache.synapse.eventing.SynapseEventSource;

/**
 *  Handles the <code>EventSource</code> deployment and undeployment tasks
 *
 * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer
 */
public class EventSourceDeployer extends AbstractSynapseArtifactDeployer {

    private static Log log = LogFactory.getLog(SequenceDeployer.class);

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName) {

        if (log.isDebugEnabled()) {
            log.debug("EventSource Deployment from file : " + fileName + " : Started");
        }

        try {
            SynapseEventSource es = EventSourceFactory.createEventSource(artifactConfig);
            if (es != null) {
                es.setFileName(fileName);
                if (log.isDebugEnabled()) {
                    log.debug("EventSource named '" + es.getName()
                            + "' has been built from the file " + fileName);
                }
                es.buildService(getSynapseConfiguration().getAxisConfiguration());
                if (log.isDebugEnabled()) {
                    log.debug("Initialized the EventSource : " + es.getName());
                }
                getSynapseConfiguration().addEventSource(es.getName(), es);
                if (log.isDebugEnabled()) {
                    log.debug("EventSource Deployment from file : " + fileName + " : Completed");
                }
                log.info("EventSource named '" + es.getName()
                        + "' has been deployed from file : " + fileName);
                return es.getName();
            } else {
                log.error("EventSource Deployment Failed. The artifact described in the file "
                        + fileName + " is not an EventSource");
            }
        } catch (Exception e) {
            log.error("EventSource Deployment from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("EventSource Undeployment of the sequence named : "
                    + artifactName + " : Started");
        }
        
        try {
            SynapseEventSource es = getSynapseConfiguration().getEventSource(artifactName);
            if (es != null) {
                getSynapseConfiguration().removeEventSource(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("EventSource Undeployment of the EventSource named : "
                            + artifactName + " : Completed");
                }
            } else {
                log.error("Couldn't find the EventSource named : " + artifactName);
            }
        } catch (Exception e) {
            log.error("EventSource Undeployement of EventSource named : "
                    + artifactName + " : Failed");
        }
    }
}
