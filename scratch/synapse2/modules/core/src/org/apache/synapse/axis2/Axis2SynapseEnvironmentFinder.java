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

package org.apache.synapse.axis2;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * <p/>
 * The SynapseEnvironment needs to be set up and then is used by the SynapseMessageReceiver to inject messages.
 * This class is used by the SynapseMessageReceiver to find the environment. The env is stored in a Parameter to the Axis2 config
 */
public class Axis2SynapseEnvironmentFinder implements Constants {
    private static Log log = LogFactory.getLog(Axis2SynapseEnvironmentFinder.class);

    public static synchronized SynapseEnvironment getSynapseEnvironment(
        MessageContext mc) {
        AxisConfiguration ac =
            mc.getConfigurationContext().getAxisConfiguration();
        Parameter synapseEnvParam = ac.getParameter(SYNAPSE_ENVIRONMENT);
        if (synapseEnvParam == null) {
            log.debug("synapse env not available - creating");
            Parameter param = ac.getParameter(SYNAPSECONFIGURATION);
            if (param == null) {
                throw new SynapseException("no parameter '"
                    + SYNAPSECONFIGURATION + "' in axis2.config");
            }
            String synapseConfig = (String) param.getValue();
            InputStream is = mc.getAxisService().getClassLoader()
                .getResourceAsStream(synapseConfig.trim());

            StAXOMBuilder builder;
            try {
                builder = new StAXOMBuilder(is);

            } catch (XMLStreamException e1) {
                throw new SynapseException(
                    "Trouble parsing Synapse Configuration ", e1);

            }
            OMElement config = builder.getDocumentElement();
            config.build();
            Axis2SynapseEnvironment se = new Axis2SynapseEnvironment(config, mc
                .getAxisService().getClassLoader());

            synapseEnvParam = new Parameter(SYNAPSE_ENVIRONMENT, null);
            synapseEnvParam.setValue(se);
            try {
                ac.addParameter(synapseEnvParam);
            } catch (AxisFault e) {
                throw new SynapseException(e);
            }
        }
        return (SynapseEnvironment) synapseEnvParam.getValue();

    }

    public static synchronized void setSynapseEnvironment(MessageContext mc, SynapseEnvironment se) {
        AxisConfiguration ac =
            mc.getConfigurationContext().getAxisConfiguration();
        Parameter synapseEnvParam = new Parameter(SYNAPSE_ENVIRONMENT, null);
        synapseEnvParam.setValue(se);
        try {
            ac.addParameter(synapseEnvParam);
        } catch (AxisFault e) {
            throw new SynapseException(e);
        }

    }

}
