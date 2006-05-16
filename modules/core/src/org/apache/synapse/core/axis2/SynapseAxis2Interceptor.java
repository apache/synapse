/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.core.axis2;

import org.apache.axis2.engine.AxisObserver;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.Constants;
import org.apache.synapse.config.xml.SynapseConfigurationBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * The Synapse Axis2 interceptor will be invoked by Axis2 upon Axis initialization.
 * This allows the Synapse engine to be initialized at Axis2 startup, and store the
 * initial Synapse configuration into the AxisConfiguration for subsequent lookup.
 */
public class SynapseAxis2Interceptor implements AxisObserver, Constants {

    private static final Log log = LogFactory.getLog(SynapseAxis2Interceptor.class);

    /**
     * This is where Synapse is initialized at Axis2 startup
     * @param axisCfg the Axis2 Configuration
     */
    public void init(AxisConfiguration axisCfg) {

        log.info("Initializing Synapse...");

        String synapseXmlLocation = null;
        // Has a system property synapse.xml overwritten the synapse config location?
        if (System.getProperty(SYNAPSE_XML) != null) {
            log.info("Loading configuration from XML file specified by the system property '" + SYNAPSE_XML +"'");
            synapseXmlLocation = System.getProperty(SYNAPSE_XML);

        } else {
            // get the synapse configuration XML file parameter
            Parameter param = axisCfg.getParameter(SYNAPSE_CONFIGURATION);
            if (param == null) {
                handleException("Axis2 configuration does not specify the '" + SYNAPSE_CONFIGURATION + "' parameter");
            } else {
                synapseXmlLocation = ((String) param.getValue()).trim();
            }
        }

        // The axis classloaders such as axisCfg.getServiceClassLoader(), axisCfg.getModuleClassLoader(),
        // axisCfg.getSystemClassLoader() are not yet initialized at this point, hence load the synapse.xml
        // from a FileInputStream as does Axis!
        InputStream is = null;
        try {
            is = new FileInputStream(synapseXmlLocation);
        } catch (FileNotFoundException fnf) {
            handleException("Cannot load Synapse configuration from : " + synapseXmlLocation, fnf);
        }

        // build the Synapse configuration parsing the XMl config file
        SynapseConfigurationBuilder cfgBuilder = null;
        try {
            cfgBuilder = new SynapseConfigurationBuilder();
            cfgBuilder.setConfiguration(is);
        } catch (Exception e) {
            handleException("Could not initialize Synapse : " + e.getMessage(), e);
        }
        log.info("Loaded Synapse configuration from : " + synapseXmlLocation);

        Parameter synapseCtxParam = new Parameter(SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(cfgBuilder.getConfig());

        Parameter synapseEnvParam = new Parameter(SYNAPSE_ENV, null);
        // Note.. will the classloader mentioned below be overwritten subsequently by Axis?
        synapseEnvParam.setValue(new Axis2SynapseEnvironment(axisCfg));

        try {
            axisCfg.addParameter(synapseCtxParam);
            axisCfg.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            handleException(
                "Could not set parameters '" + SYNAPSE_CONFIG + "' and/or '" + SYNAPSE_ENV +
                "'to the Axis2 configuration : " + e.getMessage(), e);
        }

        log.info("Synapse initialized...");
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    //---------------------------------------------------------------------------------------
    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService) {
    }

    public void moduleUpdate(AxisEvent axisEvent, AxisModule axisModule) {
    }

    public void addParameter(Parameter parameter) throws AxisFault {
    }

    public void removeParameter(Parameter parameter) throws AxisFault {
    }

    public void deserializeParameters(OMElement elem) throws AxisFault {
    }

    public Parameter getParameter(String string) {
        return null;
    }

    public ArrayList getParameters() {
        return null;
    }

    public boolean isParameterLocked(String string) {
        return false;
    }
}
