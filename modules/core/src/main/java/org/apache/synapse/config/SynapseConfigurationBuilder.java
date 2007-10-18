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
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigurationBuilder;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.DropMediator;
import org.apache.synapse.mediators.builtin.LogMediator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Builds a Synapse Configuration model with a given input
 * (e.g. XML, programmatic creation, default etc)
 */
public class SynapseConfigurationBuilder {

    private static Log log = LogFactory.getLog(SynapseConfigurationBuilder.class);

    /**
     * Return the default Synapse Configuration
     * @return the default configuration to be used
     */
    public static SynapseConfiguration getDefaultConfiguration() {
        // programatically create an empty configuration which just log and drop the messages 
        SynapseConfiguration config = new SynapseConfiguration();
        SequenceMediator mainmediator = new SequenceMediator();
        mainmediator.addChild(new LogMediator());
        mainmediator.addChild(new DropMediator());
        config.addSequence(SynapseConstants.MAIN_SEQUENCE_KEY, mainmediator);
        SequenceMediator faultmediator = new SequenceMediator();
        LogMediator fault = new LogMediator();
        fault.setLogLevel(LogMediator.FULL);
        faultmediator.addChild(fault);
        config.addSequence(SynapseConstants.FAULT_SEQUENCE_KEY, faultmediator);
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
            SynapseConfiguration synCfg
                = XMLConfigurationBuilder.getConfiguration(new FileInputStream(configFile));
            log.info("Loaded Synapse configuration from : " + configFile);
            synCfg.setPathToConfigFile(new File(configFile).getAbsolutePath());
            loadSynapseProperties(synCfg);
            return synCfg;

        } catch (FileNotFoundException fnf) {
            handleException("Cannot load Synapse configuration from : " + configFile, fnf);
        } catch (Exception e) {
            handleException("Could not initialize Synapse : " + e.getMessage(), e);
        }
        return null;
    }

    private static void loadSynapseProperties(SynapseConfiguration synCfg) {
        String props = System.getProperty(SynapseConstants.SYNAPSE_PROPERTIES);
        if (props == null) {
            props = SynapseConstants.DEFAULT_PROP_PATH;
        }
        try {
            synCfg.getProperties().load(Thread.currentThread().getContextClassLoader().getResourceAsStream(props));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to load synapse properties : Using the default tunning parameters for Synapse");
            }
        }
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
