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

package org.apache.synapse.processors.builtin.axis2;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.*;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEngine;

import org.apache.axis2.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseMessage;


import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.synapse.axis2.EmptyMessageReceiver;

import org.apache.synapse.processors.AbstractProcessor;

import java.util.Iterator;

/**
 * <p/>
 * This class turns on the addressing module and then calls an empty
 * service There's probably a better way but this should work!
 */
public class AddressingInProcessor extends AbstractProcessor {
    private Log log = LogFactory.getLog(getClass());

    public boolean process(SynapseEnvironment se, SynapseMessage smc) {
        log.debug("process");
        try {
            MessageContext mc = ((Axis2SynapseMessage) smc)
                    .getMessageContext();
            ///////////////////////////////////////////////////////////////////
            // Default Configurations. We are not going to alter these configurtions
            ConfigurationContext cc = mc.getConfigurationContext();
            AxisConfiguration ac = cc.getAxisConfiguration();
            //////////////////////////////////////////////////////////////////
//            AxisService as = ac.getService(Constants.EMPTYMEDIATOR);
//            if (as == null)
//                throw new SynapseException("cannot locate service "
//                        + Constants.EMPTYMEDIATOR);

            ///////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////
            // making addressing on/off behavior possible
            // default addressing is on. Allow MessageContext to pass through the chain
            // and fill the addressingHeaderInformation. Once the chain is excuted old ConfigurationContext
            // set to the MessageContext.
            // inorder to make this possible, we create a new ConfigurationContext
            // from scratch. Then add the service, operation and MessageReceiver
            // programatically. After the invocation, old ConfigurationContext and
            // AxisConfiguration is set to the MessageContext

            ConfigurationContextFactory configCtxFac =
                    new ConfigurationContextFactory();
            ConfigurationContext configCtx =
                    configCtxFac.buildConfigurationContext(null);
            AxisConfiguration axisConfig = configCtx.getAxisConfiguration();

            AxisService service = new AxisService(Constants.EMPTYMEDIATOR);
            service.setClassLoader(ac.getServiceClassLoader());
            AxisOperation axisOp =
                    new InOutAxisOperation(Constants.MEDIATE_OPERATION_NAME);
            axisOp.setMessageReceiver(new EmptyMessageReceiver());
            service.addOperation(axisOp);
            axisConfig.addService(service);

            mc.setConfigurationContext(configCtx);

            //setging the addressing enable ConfigurationContext to SynapeMessage

            smc.setProperty(Constants.ADDRESSING_PROCESSED_CONFIGURATION_CONTEXT,
                    configCtx);
            //////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////
            //AxisEngine ae = new AxisEngine(cc);
            AxisEngine ae = new AxisEngine(configCtx);

            // see if addressing already engage
//            boolean addressingModuleEngage = false;
//            for (Iterator iterator = ac.getEngagedModules().iterator();
//                 iterator.hasNext();) {
//                QName qname = (QName) iterator.next();
//                if (qname.getLocalPart()
//                        .equals(org.apache.axis2.Constants.MODULE_ADDRESSING)) {
//                    addressingModuleEngage = true;
//                    break;
//                }
//            }
//            if (!addressingModuleEngage) {
//                ac.engageModule(new QName(
//                        org.apache.axis2.Constants.MODULE_ADDRESSING));
//            }
//            AxisOperation ao = as
//                    .getOperation(Constants.MEDIATE_OPERATION_NAME);
//            OperationContext oc = OperationContextFactory
//                    .createOperationContext(ao.getAxisSpecifMEPConstant(), ao);
            OperationContext oc = OperationContextFactory
                    .createOperationContext(axisOp.getAxisSpecifMEPConstant(),
                            axisOp);
            //ao.registerOperationContext(mc, oc);
            axisOp.registerOperationContext(mc, oc);

            //ServiceContext sc = Utils.fillContextInformation(ao, as, cc);
            ServiceContext sc =
                    Utils.fillContextInformation(axisOp, service, configCtx);
            oc.setParent(sc);

            mc.setOperationContext(oc);
            mc.setServiceContext(sc);

//            mc.setAxisOperation(ao);
//            mc.setAxisService(as);
            mc.setAxisOperation(axisOp);
            mc.setAxisService(service);

            ae.receive(mc);
            //////////////////////////////////////////////////////////////////////
            // Now the MessageContext is filled with SOAP properties. We don't need to send
            // this message anymore time through AddressingInHandler. But we need to send it through
            // AddressingOutHandler
            // Thus, setting the Default ConfigurationContext
            mc.setConfigurationContext(cc);
            /////////////////////////////////////////////////////////////////////

        } catch (AxisFault e) {
            throw new SynapseException(e);
        }
        return true;
    }


}
