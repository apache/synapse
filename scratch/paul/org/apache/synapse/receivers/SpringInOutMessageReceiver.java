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

package org.apache.synapse.receivers;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.OperationDescription;
import org.apache.axis2.engine.DependencyManager;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.om.OMNamespace;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wsdl.WSDLService;

import java.lang.reflect.Method;

/**
 * This is a Simple java Provider.
 */
public class SpringInOutMessageReceiver
        extends SpringAbstractInOutSyncMessageReceiver
        implements MessageReceiver {
    /**
     * Field log
     */
    protected Log log = LogFactory.getLog(getClass());

    /**
     * Field scope
     */
    private String scope;

    /**
     * Field classLoader
     */
    private ClassLoader classLoader;

    /**
     * Constructor RawXMLProvider
     */
    public SpringInOutMessageReceiver() {
        scope = Constants.APPLICATION_SCOPE;
    }

    public void invokeBusinessLogic(MessageContext msgContext,
                                    MessageContext newmsgContext)
            throws AxisFault {
        try {

            // get the implementation class for the Web Service
            Object obj = getTheImplementationObject(msgContext);

            // find the WebService method
            Class ImplClass = obj.getClass();

            //Inject the Message Context if it is asked for
            DependencyManager.configureBusinessLogicProvider(obj, msgContext, newmsgContext);

            OperationDescription opDesc = msgContext.getOperationContext()
                    .getAxisOperation();
            Method method = findOperation(opDesc, ImplClass);
            if (method != null) {
                String style = msgContext.getOperationContext()
                        .getAxisOperation()
                        .getStyle();

                Class[] parameters = method.getParameterTypes();
                Object[] args = null;

                if (parameters == null || parameters.length == 0) {
                    args = new Object[0];
                } else if (parameters.length == 1) {
                    OMElement omElement = null;
                    if (WSDLService.STYLE_DOC.equals(style)) {
                        omElement =
                                msgContext.getEnvelope().getBody()
                                .getFirstElement();
                    } else if (WSDLService.STYLE_RPC.equals(style)) {
                        OMElement operationElement = msgContext.getEnvelope()
                                .getBody()
                                .getFirstElement();
                        if (operationElement != null) {
                            if (operationElement.getLocalName() != null &&
                                    operationElement.getLocalName().startsWith(
                                            method.getName())) {
                                omElement = operationElement.getFirstElement();
                            } else {
                                throw new AxisFault(Messages.getMessage("AandBdonotmatch","Operation Name","immediate child name",operationElement.getLocalName(),method.getName()));
                            }
                        } else {
                            throw new AxisFault(Messages.getMessage("rpcNeedmatchingChild"));
                        }
                    } else {
                        throw new AxisFault(Messages.getMessage("unknownStyle",style));
                    }
                    args = new Object[]{omElement};
                } else {
                    throw new AxisFault(Messages.getMessage("rawXmlProivdeIsLimited"));
                }

                OMElement result = (OMElement) method.invoke(obj, args);

                OMElement bodyContent = null;
                if (WSDLService.STYLE_RPC.equals(style)) {
                    OMNamespace ns = getSOAPFactory().createOMNamespace(
                            "http://soapenc/", "res");
                    bodyContent =
                            getSOAPFactory().createOMElement(
                                    method.getName() + "Response", ns);
                    bodyContent.addChild(result);
                } else {
                    bodyContent = result;
                }

                SOAPEnvelope envelope = getSOAPFactory().getDefaultEnvelope();
                if (bodyContent != null) {
                    envelope.getBody().addChild(bodyContent);
                }
                newmsgContext.setEnvelope(envelope);
            } else {
                throw new AxisFault(Messages.getMessage("methodNotImplemented",opDesc.getName().toString()));
            }
        } catch (Exception e) {
            throw AxisFault.makeFault(e);
        }

    }

    public Method findOperation(OperationDescription op, Class ImplClass) {
        Method method = null;
        String methodName = op.getName().getLocalPart();
        Method[] methods = ImplClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(methodName)) {
                method = methods[i];
                break;
            }
        }
        return method;
    }
}
