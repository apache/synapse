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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Keeps track of the artifacts deployed with files inside the synapse repository</p>
 *
 * <p>For hot deployment to properly work we need to, keep track fo not only the artifacts
 * deployed by deployers but also the artifacts deployed from files at the startup as well. Otherwise
 * it is not possible to track the hot update cases. This is introduced as a <code>singleton</code>
 * for the startup to report back for the deployed artifacts at startup apart from the deployers.</p>
 *
 * @see org.apache.synapse.deployers.AbstractSynapseArtifactDeployer
 * @see org.apache.synapse.config.xml.MultiXMLConfigurationBuilder
 */
public final class SynapseArtifactDeploymentStore {

    /** Keeps track of the deployed artifacts in the synapse environment */
    private static Map<String, String> fileName2ArtifactName
            = new TreeMap<String, String>(new Comparator<String>() {
        public int compare(String o1, String o2) {
            return (new File(o1)).compareTo(new File(o2));
        }
    });

    /** Keeps track of the updating artifacts in the synapse environment in a particular instance */
    private Map<String, String> updatingArtifacts
            = new TreeMap<String, String>(new Comparator<String>() {
        public int compare(String o1, String o2) {
            return (new File(o1)).compareTo(new File(o2));
        }
    });

    /** Keeps track of the restored artifacts in the synapse environment in a particular instance */
    private List<String> restoredFiles = new ArrayList<String>();

    /** Keeps track of the backed up artifacts in the synapse environment in a particular instance */
    private List<String> backedUpFiles = new ArrayList<String>();

    private static SynapseArtifactDeploymentStore _instance;
    private static final Log log = LogFactory.getLog(SynapseArtifactDeploymentStore.class);
    private SynapseArtifactDeploymentStore() {}

    /**
     * Provides the <code>singleton</code> instance of
     * {@link SynapseArtifactDeploymentStore}
     *
     * @return the singleton instance of SynapseArtifactDeploymentStore
     */
    public static SynapseArtifactDeploymentStore getInstance() {
        
        if (_instance == null) {
            _instance = new SynapseArtifactDeploymentStore();
        }
        return _instance;
    }

    /**
     * Adds artifacts indexed with the respective filename
     * 
     * @param fileName name of the file from which the artifact being added is loaded
     * @param artifactName name of the artifact being added
     */
    public void addArtifact(String fileName, String artifactName) {

        if (!fileName2ArtifactName.containsKey(fileName)) {
            fileName2ArtifactName.put(fileName, artifactName);
        } else {
            log.error("An artifact has already been loaded from the file : " + fileName);
        }
    }

    /**
     * Checks whether there is an artifact indexed with the given <code>filename</code>
     * 
     * @param fileName artifact filename to be checked for the existence
     * @return boolean <code>true</code> if it is available, <code>false</code> if not
     */
    public boolean containsFileName(String fileName) {
        return fileName2ArtifactName.containsKey(fileName);
    }

    /**
     * Retrieves the artifact name indexed with the given <code>filename</code>
     *
     * @param fileName name of the file which maps to the artifact
     * @return String artifact name mapped with the give <code>filename</code>
     */
    public String getArtifactNameForFile(String fileName) {
        return fileName2ArtifactName.get(fileName);
    }

    /**
     * Removes the indexed artifacts to the <code>filename</code> mapping from the holder
     * 
     * @param fileName name of the file of which the artifact required to be removed
     */
    public void removeArtifactWithFileName(String fileName) {
        fileName2ArtifactName.remove(fileName);
    }

    /**
     * Adds an updating artifact for the given instance
     * 
     * @param fileName name of the file from which the artifact has been loaded
     * @param artifactName name of the actual artifact being updated
     */
    public void addUpdatingArtifact(String fileName, String artifactName) {
        updatingArtifacts.put(fileName, artifactName);
    }

    /**
     * Checks whether the given artifact is at the updating state in the given instance
     * 
     * @param fileName name of the file which describes the artifact to be checked
     * @return boolean <code>true</code> if it is at the updating state, <code>false</code> otherwise
     */
    public boolean isUpdatingArtifact(String fileName) {
        return updatingArtifacts.containsKey(fileName);
    }

    /**
     * Retrieves the artifact name corresponds to the given updating artifact file name
     *
     * @param fileName name of the file from which the artifact is being updated
     * @return String artifact name corresponds to the given file name
     */
    public String getUpdatingArtifactWithFileName(String fileName) {
        return updatingArtifacts.get(fileName);
    }

    /**
     * Removes an updating artifact
     *
     * @param fileName name of the file of the artifact to be removed from the updating artifacts
     */
    public void removeUpdatingArtifact(String fileName) {
        updatingArtifacts.remove(fileName);
    }

    /**
     * Adds an artifact which is being restored
     *
     * @param fileName name of the file of the artifact which is being restored
     */
    public void addRestoredArtifact(String fileName) {
        try {
            restoredFiles.add((new File(fileName)).getCanonicalPath());
        } catch (IOException ignore) {}
    }

    /**
     * Checks whether the given artifact is being restored
     * 
     * @param fileName name of the file to be checked
     * @return boolean <code>true</code> if the provided filename describes a restoring artifact,
     * <code>false</code> otherwise
     */
    public boolean isRestoredFile(String fileName) {
        try {
            return restoredFiles.contains((new File(fileName)).getCanonicalPath());
        } catch (IOException ignore) {}
        return false;
    }

    /**
     * Removes a restored artifact
     *
     * @param fileName name of the file of the artifact to be removed
     */
    public void removeRestoredFile(String fileName) {
        try {
            restoredFiles.remove((new File(fileName)).getCanonicalPath());
        } catch (IOException ignore) {}
    }

    /**
     * Adds an artifact to the backedUp artifacts
     *
     * @param fileName name of the file of the artifact to be added into the backedUp artifacts
     */
    public void addBackedUpArtifact(String fileName) {
        try {
            backedUpFiles.add((new File(fileName)).getCanonicalPath());
        } catch (IOException ignore) {}
    }

    /**
     * Checks whether the given artifact is being backed up
     *
     * @param fileName name of the file of the artifact to be checked
     * @return boolean <code>true</code> if the artifact is being backed up, <code>false</code> otherwise
     */
    public boolean isBackedUpArtifact(String fileName) {
        try {
            return backedUpFiles.contains((new File(fileName)).getCanonicalPath());
        } catch (IOException ignore) {}
        return false;
    }

    /**
     * Removes a backedUp artifact
     * 
     * @param fileName name of the file of the artifact to be removed
     */
    public void removeBackedUpArtifact(String fileName) {
        try {
            backedUpFiles.remove((new File(fileName)).getCanonicalPath());
        } catch (IOException ignore) {}
    }
}
