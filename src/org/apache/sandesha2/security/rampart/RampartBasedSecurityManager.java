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

package org.apache.sandesha2.security.rampart;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.neethi.Policy;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.SimpleTokenStore;
import org.apache.rahas.Token;
import org.apache.rahas.TokenStorage;
import org.apache.rahas.TrustException;
import org.apache.rahas.TrustUtil;
import org.apache.rahas.client.STSClient;
import org.apache.rampart.RampartException;
import org.apache.rampart.policy.RampartPolicyBuilder;
import org.apache.rampart.policy.RampartPolicyData;
import org.apache.rampart.util.RampartUtil;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.security.SecurityManager;
import org.apache.sandesha2.security.SecurityToken;
import org.apache.ws.secpolicy.WSSPolicyException;
import org.apache.ws.secpolicy.model.SecureConversationToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityTokenReference;

import javax.xml.namespace.QName;

import java.util.List;


public class RampartBasedSecurityManager extends SecurityManager {

    TokenStorage storage = null;
    
    /**
     * @param context
     */
    public RampartBasedSecurityManager(ConfigurationContext context) {
        super(context);

        this.storage = (TokenStorage)context.getProperty(
                        TokenStorage.TOKEN_STORAGE_KEY);
        if(this.storage == null) {
            this.storage = new SimpleTokenStore();
            context.setProperty(
                    TokenStorage.TOKEN_STORAGE_KEY, this.storage);
        }

    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#checkProofOfPossession(org.apache.sandesha2.security.SecurityToken, org.apache.axiom.om.OMElement, org.apache.axis2.context.MessageContext)
     */
    public void checkProofOfPossession(SecurityToken token,
            OMElement messagePart, MessageContext message)
            throws SandeshaException {
        //Rampart verifies this no need to check again :-?
    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#createSecurityTokenReference(org.apache.sandesha2.security.SecurityToken, org.apache.axis2.context.MessageContext)
     */
    public OMElement createSecurityTokenReference(SecurityToken token,
            MessageContext message) throws SandeshaException {
        
        OMFactory fac = message.getEnvelope().getOMFactory();
        
        RampartSecurityToken rampartToken = (RampartSecurityToken)token;
        OMElement element = rampartToken.getToken().getAttachedReference();
        if(element == null) {
            element = rampartToken.getToken().getUnattachedReference();
        }
        
        if(element == null) {
            //Now use the token id and construct the ref element
            element = fac.createOMElement(SecurityTokenReference.SECURITY_TOKEN_REFERENCE, WSConstants.WSSE_LN, WSConstants.WSSE_PREFIX);
            OMElement refElem = fac.createOMElement(Reference.TOKEN, element);
            refElem.addAttribute("ValueType", "http://schemas.xmlsoap.org/ws/2005/02/sc/sct", null);
            refElem.addAttribute("URI", rampartToken.getToken().getId(), null);
        }
        
        return this.convertOMElement(fac, element);
    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#getSecurityToken(org.apache.axis2.context.MessageContext)
     */
    public SecurityToken getSecurityToken(MessageContext message)
            throws SandeshaException {
        String contextIdentifierKey = RampartUtil.getContextIdentifierKey(message);
        String identifier = (String)RampartUtil.getContextMap(message).get(contextIdentifierKey);
        
        if(identifier == null && !message.isServerSide()) {
            try {
                OMElement rstTmpl = RampartUtil.createRSTTempalteForSCT(
                        ConversationConstants.VERSION_05_02, 
                        RahasConstants.VERSION_05_02);
                
                String action = TrustUtil.getActionValue(
                        RahasConstants.VERSION_05_02,
                        RahasConstants.RST_ACTION_SCT);
                
                Policy servicePolicy = message.getEffectivePolicy();
                if(servicePolicy == null) {
                    throw new SandeshaException("service policy missing");
                }
                List it = (List)servicePolicy.getAlternatives().next();
                RampartPolicyData rpd = RampartPolicyBuilder.build(it);
                
                SecureConversationToken secConvTok = null;
                
                org.apache.ws.secpolicy.model.Token encrtok = rpd.getEncryptionToken();
                secConvTok = (encrtok != null && encrtok instanceof SecureConversationToken) ? (SecureConversationToken)encrtok : null;
                
                if(secConvTok == null) {
                    org.apache.ws.secpolicy.model.Token sigtok = rpd.getSignatureToken();
                    secConvTok = (sigtok != null && sigtok instanceof SecureConversationToken) ? (SecureConversationToken)sigtok : null;
                }
                
                if(secConvTok != null) {
                    
                    Policy issuerPolicy = secConvTok.getBootstrapPolicy();
                    
                    STSClient client = new STSClient(message.getConfigurationContext());
                    client.setAction(action);
                    client.setRstTemplate(rstTmpl);
                    client.setCryptoInfo(RampartUtil.getEncryptionCrypto(rpd
                            .getRampartConfig()), RampartUtil.getPasswordCB(
                            message, rpd));
                    Token tok = client.requestSecurityToken(servicePolicy,
                            message.getTo().getAddress(), issuerPolicy, null);
                    this.storage.add(tok);
                    
                    contextIdentifierKey = RampartUtil.getContextIdentifierKey(message);
                    RampartUtil.getContextMap(message).put(
                                                        contextIdentifierKey,
                                                        tok.getId());
                    identifier = tok.getId();
                    
                } else {
                    throw new SandeshaException("No SecureConversationToken in policy");
                }
                
            } catch (RampartException e) {
                throw new SandeshaException(e.getMessage(), e);
            } catch (TrustException e) {
                throw new SandeshaException(e.getMessage(), e);
            } catch (WSSPolicyException e) {
                throw new SandeshaException(e.getMessage(), e);
            }
        }

        
        return this.recoverSecurityToken(identifier);

    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#getSecurityToken(org.apache.axiom.om.OMElement, org.apache.axis2.context.MessageContext)
     */
    public SecurityToken getSecurityToken(OMElement theSTR,
            MessageContext message) throws SandeshaException {

        OMElement refElem = theSTR.getFirstChildWithName(Reference.TOKEN);
        String id = refElem.getAttributeValue(new QName("URI"));
        return this.recoverSecurityToken(id);
    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#getTokenRecoveryData(org.apache.sandesha2.security.SecurityToken)
     */
    public String getTokenRecoveryData(SecurityToken token)
            throws SandeshaException {
        return ((RampartSecurityToken)token).getToken().getId();
    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#initSecurity(org.apache.axis2.description.AxisModule)
     */
    public void initSecurity(AxisModule moduleDesc) {
    }

    /* (non-Javadoc)
     * @see org.apache.sandesha2.security.SecurityManager#recoverSecurityToken(java.lang.String)
     */
    public SecurityToken recoverSecurityToken(String tokenData)
            throws SandeshaException {
        try {
            Token token = this.storage.getToken(tokenData);
            if(token != null) {
                return new RampartSecurityToken(token);
            } else {
                String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.errorRetrievingSecurityToken);
                throw new SandeshaException(msg);
            }
        } catch (TrustException e) {
            String msg = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.errorRetrievingSecurityToken);
            throw new SandeshaException(msg);
        }
    }

    
    private OMElement convertOMElement(OMFactory fac, OMElement elem) {
        return new StAXOMBuilder(fac, elem.getXMLStreamReader()).getDocumentElement();
    }
}
