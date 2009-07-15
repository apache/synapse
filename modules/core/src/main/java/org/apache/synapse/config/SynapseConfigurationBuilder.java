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
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;
import org.apache.synapse.config.xml.MultiXMLConfigurationSerializer;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.DropMediator;
import org.apache.synapse.mediators.builtin.LogMediator;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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
        mainmediator.setName(SynapseConstants.MAIN_SEQUENCE_KEY);
        config.addSequence(SynapseConstants.MAIN_SEQUENCE_KEY, mainmediator);
        SequenceMediator faultmediator = new SequenceMediator();
        LogMediator fault = new LogMediator();
        fault.setLogLevel(LogMediator.FULL);
        faultmediator.addChild(fault);
        faultmediator.setName(SynapseConstants.FAULT_SEQUENCE_KEY);
        config.addSequence(SynapseConstants.FAULT_SEQUENCE_KEY, faultmediator);
        return config;
    }

    /**
     * Build a Synapse configuration from a given XML configuration file
     *
     * @param configFile Path to the Synapse configuration file or directory
     * @return the Synapse configuration model
     */
    public static SynapseConfiguration getConfiguration(String configFile) {

        File synapseConfigLocation = new File(configFile);
        if (!synapseConfigLocation.exists()) {
            throw new SynapseException("Unable to load the Synapse configuration from : " + configFile);
        }

        SynapseConfiguration synCfg = null;
        if (synapseConfigLocation.isFile()) {
            // build the Synapse configuration parsing the XML config file
            try {
                synCfg = XMLConfigurationBuilder.getConfiguration(new FileInputStream(configFile));
                log.info("Loaded Synapse configuration from : " + configFile);
                synCfg.setPathToConfigFile(new File(configFile).getAbsolutePath());
            } catch (Exception e) {
                handleException("Could not initialize Synapse : " + e.getMessage(), e);
            }

        } else if (synapseConfigLocation.isDirectory()) {
            // build the Synapse configuration by processing given directory hierarchy
            try {
                synCfg = MultiXMLConfigurationBuilder.getConfiguration(configFile);
                log.info("Loaded Synapse configuration from the directory hierarchy at : " + configFile);
                MultiXMLConfigurationSerializer s = new MultiXMLConfigurationSerializer("/home/hiranya/Desktop/myconf");
                s.serialize(synCfg);
            } catch (XMLStreamException e) {
                handleException("Could not initialize Synapse : " + e.getMessage(), e);
            }
        }


        return synCfg;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
