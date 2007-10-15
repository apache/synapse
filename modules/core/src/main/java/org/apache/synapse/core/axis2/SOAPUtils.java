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

package org.apache.synapse.core.axis2;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.soap.*;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

import java.util.Iterator;
import java.util.List;

public class SOAPUtils {

    private static final Log log = LogFactory.getLog(SOAPUtils.class);

    /**
     * Converts the SOAP version of the message context.  Creates a new envelope of the given SOAP
     * version, copy headers and bodies from the old envelope and sets the new envelope to the same
     * message context.
     *
     * @param axisOutMsgCtx  messageContext where version conversion is done
     * @param soapVersionURI either org.apache.axis2.namespace.Constants.URI_SOAP12_ENV or
     *                       org.apache.axis2.namespace.Constants.URI_SOAP11_ENV
     * @throws AxisFault
     */
    public static void convertSoapVersion(org.apache.axis2.context.MessageContext axisOutMsgCtx,
        String soapVersionURI) throws AxisFault {

        if (org.apache.axis2.namespace.Constants.URI_SOAP12_ENV.equals(soapVersionURI)) {
            convertSOAP11toSOAP12(axisOutMsgCtx);
        } else if (org.apache.axis2.namespace.Constants.URI_SOAP11_ENV.equals(soapVersionURI)) {
            convertSOAP12toSOAP11(axisOutMsgCtx);
        } else {
            throw new SynapseException("Invalid soapVersionURI:" + soapVersionURI);
        }
    }

    private static String SOAP_ATR_ACTOR = "actor";
    private static String SOAP_ATR_ROLE = "role";
    private static String SOAP_ATR_MUST_UNDERSTAND = "mustUnderstand";

    /**
     * Converts the version of the the message context to 1.2.
     * <br />
     * <b>Message Changes:</b>
     * <ol>
     *     <li>Convert envelope, header elements</li>
     *     <li>For each header block convert attribute actor to role</li>
     *     <li>For each header block convert mustUnderstand value type</li>
     *     <li>For each header block remove 1.1 namespaced other attributes</li>
     * </ol>
     *
     * <b>Fault Changes:</b>
     * <ol>
     *     <li>Convert fault element</li>
     *     <li>faultcode to Fault/Code</li>
     *     <li>faultstring to First Fault/Reason/Text with lang=en</li>
     * </ol>
     *
     * @param axisOutMsgCtx
     * @throws AxisFault
     */
    public static void convertSOAP11toSOAP12(
        org.apache.axis2.context.MessageContext axisOutMsgCtx) throws AxisFault {

        if(log.isDebugEnabled()) {
            log.debug("convert SOAP11 to SOAP12");
        }
        SOAPEnvelope oldEnvelope = axisOutMsgCtx.getEnvelope();

        SOAPFactory soap12Factory = OMAbstractFactory.getSOAP12Factory();
        SOAPEnvelope newEnvelope  = soap12Factory.getDefaultEnvelope();

        if (oldEnvelope.getHeader() != null) {
            Iterator itr = oldEnvelope.getHeader().getChildren();
            while (itr.hasNext()) {
                OMNode omNode = (OMNode) itr.next();

                if (omNode instanceof SOAPHeaderBlock) {
                    SOAPHeaderBlock soapHeader = (SOAPHeaderBlock) omNode;
                    SOAPHeaderBlock newSOAPHeader = soap12Factory.createSOAPHeaderBlock(
                        soapHeader.getLocalName(), soapHeader.getNamespace());
                    Iterator allAttributes = soapHeader.getAllAttributes();

                    while(allAttributes.hasNext()) {
                        OMAttribute attr = (OMAttribute) allAttributes.next();
                        if(attr.getNamespace() != null
                            && SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(
                            attr.getNamespace().getNamespaceURI())) {
                            String attrName = attr.getLocalName();

                            if(SOAP_ATR_ACTOR.equals(attrName)) {
                                OMAttribute newAtr = omNode.getOMFactory().createOMAttribute(
                                    SOAP_ATR_ROLE, newEnvelope.getNamespace(),
                                    attr.getAttributeValue());
                                newSOAPHeader.addAttribute(newAtr);

                            } else if(SOAP_ATR_MUST_UNDERSTAND.equals(attrName)) {
                                boolean isMustUnderstand = soapHeader.getMustUnderstand();
                                newSOAPHeader.setMustUnderstand(isMustUnderstand);

                            } else {
                                log.warn("removed unsupported attribute from SOAP 1.1 " +
                                    "namespace when converting to SOAP 1.2:" + attrName);
                            }

                        } else {
                            newSOAPHeader.addAttribute(attr);
                        }

                        Iterator itrChildren = soapHeader.getChildren();
                        while (itrChildren.hasNext()) {
                            newSOAPHeader.addChild(((OMNode) itrChildren.next()));
                        }

                        newEnvelope.getHeader().addChild(newSOAPHeader);
                    } // while(allAttributes.hasNext())

                } else {
                    newEnvelope.getHeader().addChild(omNode);
                }

            } // while (itr.hasNext())

        } // if (oldEnvelope.getHeader() != null)

        if (oldEnvelope.getBody() != null) {

            Iterator itrBodyChildren = oldEnvelope.getBody().getChildren();
            while (itrBodyChildren.hasNext()) {
                OMNode omNode = (OMNode) itrBodyChildren.next();

                if (omNode instanceof SOAPFault) {
                    SOAPFault soapFault = (SOAPFault) omNode;
                    if(soapFault != null) {
                        SOAPFault newSOAPFault = soap12Factory.createSOAPFault();
                        newEnvelope.getBody().addChild(newSOAPFault);
                        // get the existing envelope
                        SOAPFaultCode code = soapFault.getCode();
                        if(code != null) {
                            SOAPFaultCode newSOAPFaultCode = soap12Factory.createSOAPFaultCode();
                            newSOAPFault.setCode(newSOAPFaultCode);

                            String value = code.getText();
                            if(value != null) {
                                SOAPFaultValue newSOAPFaultValue
                                    = soap12Factory.createSOAPFaultValue(newSOAPFaultCode);
                                newSOAPFaultValue.setText(value);
                            }

                        }

                        SOAPFaultReason reason = soapFault.getReason();
                        if(reason != null) {
                            SOAPFaultReason newSOAPFaultReason
                                = soap12Factory.createSOAPFaultReason(newSOAPFault);
                            String reasonText = reason.getText();
                            if(reasonText != null) {
                                SOAPFaultText newSOAPFaultText
                                    = soap12Factory.createSOAPFaultText(newSOAPFaultReason);
                                newSOAPFaultText.setLang("en"); // hard coded
                                newSOAPFaultText.setText(reasonText);
                            }
                            newSOAPFault.setReason(newSOAPFaultReason);
                        }

                    } // if(soapFault != null)

                } else {
                    newEnvelope.getBody().addChild(omNode);

                } // if (omNode instanceof SOAPFault)

            } // while (itrBodyChildren.hasNext())

        } //if (oldEnvelope.getBody() != null)

        axisOutMsgCtx.setEnvelope(newEnvelope);
    }

    /**
     * Converts the version of the the message context to 1.1.
     * <br />
     * <b>Message Changes:</b>
     * <ol>
     *     <li>Convert envelope, header elements</li>
     *     <li>For each header block convert attribute role to actor</li>
     *     <li>For each header block convert mustUnderstand value type</li>
     *     <li>For each header block remove 1.2 namespaced other attributes</li>
     * </ol>
     *
     * <b>Fault Changes:</b>
     * <ol>
     *     <li>Convert fault element</li>
     *     <li>Fault/Code to faultcode</li>
     *     <li>First Fault/Reason/Text to faultstring</li>
     * </ol>
     * @param axisOutMsgCtx
     * @throws AxisFault
     */
    public static void convertSOAP12toSOAP11(
        org.apache.axis2.context.MessageContext axisOutMsgCtx) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("convert SOAP12 to SOAP11");
        }
        SOAPEnvelope oldEnvelope = axisOutMsgCtx.getEnvelope();

        SOAPFactory soap11Factory = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope newEnvelope  = soap11Factory.getDefaultEnvelope();
        if (oldEnvelope.getHeader() != null) {
            Iterator itr = oldEnvelope.getHeader().getChildren();
            while (itr.hasNext()) {
                OMNode omNode = (OMNode) itr.next();

                if (omNode instanceof SOAPHeaderBlock) {
                    SOAPHeaderBlock soapHeaderBlock = (SOAPHeaderBlock) omNode;
                    SOAPHeaderBlock newSOAPHeader = soap11Factory.createSOAPHeaderBlock(
                        soapHeaderBlock.getLocalName(), soapHeaderBlock.getNamespace());

                    Iterator allAttributes = soapHeaderBlock.getAllAttributes();

                    while(allAttributes.hasNext()) {
                        OMAttribute attr = (OMAttribute) allAttributes.next();
                        if(attr.getNamespace() != null
                            && SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(
                            attr.getNamespace().getNamespaceURI())) {
                            String attrName = attr.getLocalName();

                            if(SOAP_ATR_ROLE.equals(attrName)) {
                                OMAttribute newAtr = omNode.getOMFactory().createOMAttribute(
                                    SOAP_ATR_ACTOR, newEnvelope.getNamespace(),
                                    attr.getAttributeValue());
                                newSOAPHeader.addAttribute(newAtr);

                            } else if(SOAP_ATR_MUST_UNDERSTAND.equals(attrName)) {
                                boolean isMustUnderstand = soapHeaderBlock.getMustUnderstand();
                                newSOAPHeader.setMustUnderstand(isMustUnderstand);

                            } else {
                                log.warn("removed unsupported attribute from SOAP 1.2 " +
                                    "namespace when converting to SOAP 1.1:" + attrName);
                            }

                        } else {
                            newSOAPHeader.addAttribute(attr);
                        }

                        Iterator itrChildren = soapHeaderBlock.getChildren();
                        while (itrChildren.hasNext()) {
                            newSOAPHeader.addChild(((OMNode) itrChildren.next()));
                        }

                        newEnvelope.getHeader().addChild(newSOAPHeader);
                    } // while(allAttributes.hasNext())

                } else {
                    newEnvelope.getHeader().addChild(omNode);
                } // if (omNode instanceof SOAPHeaderBlock)

            } // while (itr.hasNext())

        } // if (oldEnvelope.getHeader() != null)

        if (oldEnvelope.getBody() != null) {
            Iterator itr = oldEnvelope.getBody().getChildren();
            while (itr.hasNext()) {
                OMNode omNode = (OMNode) itr.next();

                if (omNode instanceof SOAPFault) {

                    SOAPFault soapFault = (SOAPFault) omNode;
                    if(soapFault != null) {
                        SOAPFault newSOAPFault = soap11Factory.createSOAPFault();
                        newEnvelope.getBody().addChild(newSOAPFault);

                        SOAPFaultCode code = soapFault.getCode();
                        if(code != null) {
                            SOAPFaultCode newSOAPFaultCode
                                = soap11Factory.createSOAPFaultCode(newSOAPFault);

                            SOAPFaultValue value = code.getValue();
                            if(value != null) {
                                soap11Factory.createSOAPFaultValue(newSOAPFaultCode);
                                if(value.getText() != null) {
                                    newSOAPFaultCode.setText(value.getText());
                                }
                            }
                        }

                        SOAPFaultReason reason = soapFault.getReason();
                        if(reason != null) {
                            SOAPFaultReason newSOAPFaultReason
                                = soap11Factory.createSOAPFaultReason(newSOAPFault);

                            List allSoapTexts = reason.getAllSoapTexts();
                            Iterator iterAllSoapTexts = allSoapTexts.iterator();
                            while(iterAllSoapTexts.hasNext()) {
                                SOAPFaultText soapFaultText
                                    = (SOAPFaultText) iterAllSoapTexts.next();
                                SOAPFaultText newSOAPFaultText
                                    = soap11Factory.createSOAPFaultText(newSOAPFaultReason);
                                newSOAPFaultReason.setText(soapFaultText.getText());
                                break;
                            }
                        }

                    } // if(soapFault != null)

                } else {
                    newEnvelope.getBody().addChild(omNode);
                } // if (omNode instanceof SOAPFault)

            } // while (itr.hasNext())

        } // if (oldEnvelope.getBody() != null)
        axisOutMsgCtx.setEnvelope(newEnvelope);
    }

}