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
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.xml.EntryFactory;

/**
 *  Handles the <code>LocalEntry</code> deployment and undeployment tasks
 *
 * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer
 */
public class LocalEntryDeployer extends AbstractSynapseArtifactDeployer {

    private static Log log = LogFactory.getLog(LocalEntryDeployer.class);

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName) {

        if (log.isDebugEnabled()) {
            log.debug("LocalEntry Deployment from file : " + fileName + " : Started");
        }

        try {
            Entry e = EntryFactory.createEntry(artifactConfig);
            if (e != null) {
                e.setFileName(fileName);
                if (log.isDebugEnabled()) {
                    log.debug("LocalEntry with key '" + e.getKey()
                            + "' has been built from the file " + fileName);
                }
                getSynapseConfiguration().addEntry(e.getKey(), e);
                if (log.isDebugEnabled()) {
                    log.debug("LocalEntry Deployment from file : " + fileName + " : Completed");
                }
                log.info("LocalEntry named '" + e.getKey()
                        + "' has been deployed from file : " + fileName);
                return e.getKey();
            } else {
                log.error("LocalEntry Deployment Failed. The artifact described in the file "
                        + fileName + " is not a LocalEntry");
            }
        } catch (Exception e) {
            log.error("LocalEntry Deployment from the file : " + fileName + " : Failed.", e);
        }

        return null;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {

        if (log.isDebugEnabled()) {
            log.debug("LocalEntry Undeployment of the entry named : "
                    + artifactName + " : Started");
        }
        
        try {
            Entry e = getSynapseConfiguration().getDefinedEntries().get(artifactName);
            if (e != null && e.getType() != Entry.REMOTE_ENTRY) {
                getSynapseConfiguration().removeEntry(artifactName);
                if (log.isDebugEnabled()) {
                    log.debug("LocalEntry Undeployment of the entry named : "
                            + artifactName + " : Completed");
                }
            } else {
                log.error("Couldn't find the LocalEntry named : " + artifactName);
            }
        } catch (Exception e) {
            log.error("LocalEntry Undeployement of entry named : " + artifactName + " : Failed");
        }
    }
}
