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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseContext;
import org.apache.synapse.SynapseException;

import java.io.InputStream;

/**
 * <p/>
 * The SynapseContext needs to be set up and then is used by the SynapseMessageReceiver to inject messages.
 * This class is used by the SynapseMessageReceiver to find the environment. The env is stored in a Parameter to the Axis2 config
 */
public class Axis2SynapseContextFinder implements Constants {

    private static Log log = LogFactory.getLog(Axis2SynapseContextFinder.class);

    public static synchronized SynapseContext getSynapseContext(MessageContext mc) {

        AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
        Parameter synapseCtxParam = ac.getParameter(SYNAPSE_CONTEXT);

        if (synapseCtxParam == null) {

            log.debug("Synapse Context not available. Creating...");
            Parameter param = ac.getParameter(SYNAPSE_CONFIGURATION);

            if (param == null) {
                throw new SynapseException("Axis2 configuration does not specify a '" +
                    SYNAPSE_CONFIGURATION + "' parameter");
            }

            String synapseConfig = (String) param.getValue();
            InputStream is = mc.getAxisService().getClassLoader().getResourceAsStream(synapseConfig.trim());

            Axis2SynapseContext synCtx = new Axis2SynapseContext(is, mc.getAxisService().getClassLoader());
            setSynapseContext(mc, synCtx);
        }
        return (SynapseContext) ac.getParameter(SYNAPSE_CONTEXT).getValue();
    }

    public static synchronized void setSynapseContext(MessageContext mc, SynapseContext synCtx) {

        AxisConfiguration ac = mc.getConfigurationContext().getAxisConfiguration();
        Parameter synapseCtxParam = new Parameter(SYNAPSE_CONTEXT, null);
        synapseCtxParam.setValue(synCtx);

        try {
            ac.addParameter(synapseCtxParam);
        } catch (AxisFault e) {
            String msg = "Could not add parameter '" + SYNAPSE_CONTEXT + "' to the Axis2 configuration";
            log.error(msg);
            throw new SynapseException(msg, e);
        }
    }

}
