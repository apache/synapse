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

package org.apache.synapse.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigurationBuilder;
import org.apache.synapse.mediators.base.SynapseMediator;
import org.apache.synapse.mediators.builtin.SendMediator;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;

/**
 * Builds a Synapse Configuration model with a given input (e.g. XML, programmatic creation, default etc)
 */
public class SynapseConfigurationBuilder implements Constants {

    private static Log log = LogFactory.getLog(SynapseConfigurationBuilder.class);

    /**
     * Return the default Synapse Configuration
     * @return the default configuration to be used
     */
    public static SynapseConfiguration getDefaultConfiguration() {
        // programatically create an empty configuration which just sends messages to thier implicit destinations
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseMediator mainmediator = new SynapseMediator();
        mainmediator.addChild(new SendMediator());
        config.addSequence("main", mainmediator);
        return config;
    }

    /**
     * Build a Synapse configuration from a given XML configuration file
     *
     * @param configFile the XML configuration
     * @return the Synapse configuration model
     */
    public static SynapseConfiguration getConfiguration(String configFile) {

        // build the Synapse configuration parsing the XML config file
        try {
            SynapseConfiguration synCfg = XMLConfigurationBuilder.getConfiguration(new FileInputStream(configFile));
            log.info("Loaded Synapse configuration from : " + configFile);
            synCfg.setPathToConfigFile(new File(configFile).getAbsolutePath());
            return synCfg;

        } catch (FileNotFoundException fnf) {
            handleException("Cannot load Synapse configuration from : " + configFile, fnf);
        } catch (Exception e) {
            handleException("Could not initialize Synapse : " + e.getMessage(), e);
        }
        return null;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
