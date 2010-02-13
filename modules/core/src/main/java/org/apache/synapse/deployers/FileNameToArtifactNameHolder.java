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

import java.util.HashMap;
import java.util.Map;

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
public class FileNameToArtifactNameHolder {

    /**
     * Keeps track of the deployed artifacts in the synapse environment
     */
    private static Map<String, String> fileName2ArtifactName = new HashMap<String, String>();

    private static FileNameToArtifactNameHolder _instance;

    private static final Log log = LogFactory.getLog(FileNameToArtifactNameHolder.class);
    
    private FileNameToArtifactNameHolder() {}

    /**
     * Provides the <code>singleton</code> instance of
     * {@link org.apache.synapse.deployers.FileNameToArtifactNameHolder}
     *
     * @return the singleton instance of FileNameToArtifactNameHolder 
     */
    public static FileNameToArtifactNameHolder getInstance() {
        
        if (_instance == null) {
            _instance = new FileNameToArtifactNameHolder();
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
}
