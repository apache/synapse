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


import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.async.Callback;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.ClientUtils;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.SynapseException;
import org.apache.wsdl.WSDLConstants;


import javax.xml.namespace.QName;
import java.util.HashMap;

/**
 * DynamicAxisOperation which switch dynamically between MEPs
 */
public class DynamicAxisOperation extends InOutAxisOperation {
    public DynamicAxisOperation() {
        super();
    }

    public DynamicAxisOperation(QName name) {
        super(name);
    }

    public void addMessageContext(MessageContext msgContext,
                                  OperationContext opContext) throws AxisFault {
        HashMap mep = opContext.getMessageContexts();
        MessageContext immsgContext = (MessageContext) mep
                .get(MESSAGE_LABEL_IN_VALUE);
        MessageContext outmsgContext = (MessageContext) mep
                .get(MESSAGE_LABEL_OUT_VALUE);

        if ((immsgContext != null) && (outmsgContext != null)) {
            throw new AxisFault(
                    "Invalid message addition , operation context completed");
        }

        if (outmsgContext == null) {
            mep.put(MESSAGE_LABEL_OUT_VALUE, msgContext);
        } else {
            mep.put(MESSAGE_LABEL_IN_VALUE, msgContext);
            opContext.setComplete(true);
        }
    }


    public OperationClient createClient(ServiceContext sc, Options options) {
        return new DynamicOperationClient(this,sc,options);
    }

}

class DynamicOperationClient implements OperationClient {
    private DynamicAxisOperation axisOp;
    private ServiceContext sc;
    private OperationContext oc;
    private Options options;

    public DynamicOperationClient(DynamicAxisOperation axisOp, ServiceContext sc, Options options){
        this.options = options;
        this.axisOp = axisOp;
        this.sc = sc;
        this.oc = new OperationContext(axisOp,sc);
        this.oc.setParent(this.sc);
    }

    public void setOptions(Options options) {
        // Not supported
    }

    public Options getOptions() {
        throw new SynapseException("Not Supported");
    }

    public void addMessageContext(MessageContext mc) throws AxisFault {
        mc.setServiceContext(sc);
        axisOp.registerOperationContext(mc, oc);
    }

    public MessageContext getMessageContext(String messageLabel) throws AxisFault {
        return oc.getMessageContext(messageLabel);
    }

    public void setCallback(Callback callback) {
        // Not supported
    }

    public void execute(boolean block) throws AxisFault {
        if (block) {
            ConfigurationContext cc = sc.getConfigurationContext();

            // copy interesting info from options to message context.
            MessageContext mc = oc
                    .getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE);
            if (mc == null) {
                throw new AxisFault(
                        "Out message context is null ,"
                                + " please set the out message context before calling this method");
            }

            EndpointReference toEPR = mc.getTo();

            TransportOutDescription transportOut = ClientUtils.inferOutTransport(cc
                    .getAxisConfiguration(), toEPR, mc);
            mc.setTransportOut(transportOut);

            /*
            Options need to Infer TransportInDescription
            */
            if (mc.getTransportIn() == null) {
                TransportInDescription transportIn = options.getTransportIn();
                if (transportIn == null) {
                    mc.setTransportIn(ClientUtils.inferInTransport(cc
                            .getAxisConfiguration(), options, mc));
                } else {
                    mc.setTransportIn(transportIn);
                }
            }

            if (mc.getSoapAction() == null || "".equals(mc.getSoapAction())) {
                Parameter soapaction = axisOp.getParameter(AxisOperation.SOAP_ACTION);
                if (soapaction != null) {
                    mc.setSoapAction((String) soapaction.getValue());
                }
            }

            oc.addMessageContext(mc);
            // ship it out
            AxisEngine engine = new AxisEngine(cc);
            engine.send(mc);
        }

    }
    public OperationContext getOperationContext() {
    	return oc;
    }

    public void reset() throws AxisFault {
        // Not supported
    }

    public void complete(MessageContext msgCtxt) throws AxisFault {
        // Not supported
    }


}
