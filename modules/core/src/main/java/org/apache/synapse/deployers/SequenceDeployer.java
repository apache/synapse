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
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.MediatorFactoryFinder;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 *  Handles the <code>Sequence</code> deployment and undeployment tasks
 *
 * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer
 */
public class SequenceDeployer extends AbstractSynapseArtifactDeployer {

    private static Log log = LogFactory.getLog(SequenceDeployer.class);
    
    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName) {

        if (log.isDebugEnabled()) {
            log.debug("Sequence Deployment from file : " + fileName + " : Started");
        }

        try {    
            Mediator m = MediatorFactoryFinder.getInstance().getMediator(artifactConfig);
            if (m instanceof SequenceMediator) {
                SequenceMediator seq = (SequenceMediator) m;
                seq.setFileName(fileName);
                if (log.isDebugEnabled()) {
                    log.debug("Sequence named '" + seq.getName()
                            + "' has been built from the file " + fileName);
                }
                seq.init(getSynapseEnvironment());
                if (log.isDebugEnabled()) {
                    log.debug("Initialized the sequence : " + seq.getName());
                }
                getSynapseConfiguration().addSequence(seq.getName(), seq);
                if (log.isDebugEnabled()) {
                    log.debug("Sequence Deployment from file : " + fileName + " : Completed");
                }
                return seq.getName();
            } else {
                log.error("Sequence Deployment Failed. The artifact described in the file "
                        + fileName + " is not a Sequence");
            }
        } catch (Exception e) {
            log.error("Sequence Deployment from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("Sequence Undeployment of the sequence named : "
                    + artifactName + " : Started");
        }
        
        try {
            SequenceMediator seq
                    = getSynapseConfiguration().getDefinedSequences().get(artifactName);
            if (seq != null) {
                getSynapseConfiguration().removeSequence(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("Destroying the sequence named : " + artifactName);
                }
                seq.destroy();
                if (log.isDebugEnabled()) {
                    log.debug("Sequence Undeployment of the sequence named : "
                            + artifactName + " : Completed");
                }
            } else {
                log.error("Couldn't find the sequence named : " + artifactName);
            }
        } catch (Exception e) {
            log.error("Sequence Undeployement of sequence named : " + artifactName + " : Failed");
        }
    }
}
