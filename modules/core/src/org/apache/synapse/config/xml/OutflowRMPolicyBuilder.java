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
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.neethi.Policy;
import org.apache.ws.policy.util.OMPolicyReader;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.PolicyReader;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Build an Apache Sandesha Policy using the given
 * XML fragment from a Synapse configuration file.
 *
 * <reliablemessaging enabled="true" [policy="url"]>
 *   <wsp:Policy ...>*
 * </reliablemessaging>
 */
public class OutflowRMPolicyBuilder {

    private static final Log log = LogFactory.getLog(OutflowRMPolicyBuilder.class);

    /**
     * Return a Policy to be used by the WS-RM client with Sandesha. First looks for a 'policy'
     * attribute which specifies a URL for the policy. If one is found, it is read to find the
     * policy. Else we look for a child Policy element within the reliable messaging element.
     * @param elem the Synapse configuration XML element
     * @return the Policy if RM is enabled and a policy is specified, null if not
     */
    public static Policy getRMPolicy(OMElement elem) {
        // if RM is not enabled, return null.
        if (!isRMEnabled(elem)) {
            return null;
        }

        OMElement wsrm = elem.getFirstChildWithName(new QName(Constants.NULL_NAMESPACE, "reliablemessaging"));
        if (wsrm != null) {
            OMAttribute policyUrl = wsrm.getAttribute(new QName(Constants.NULL_NAMESPACE, "policy"));
            if (policyUrl != null) {
                PolicyReader reader = PolicyFactory.getPolicyReader(PolicyFactory.OM_POLICY_READER);
                try {
                    URL url = new URL(policyUrl.getAttributeValue());
                    // TODO fix this return reader.readPolicy(url.openStream());
                    return null;
                } catch (MalformedURLException e) {
                    handleException("Invalid policy URL : " + policyUrl.getAttributeValue(), e);
                } catch (IOException e) {
                    handleException("Error reading from policy at URL : " + policyUrl.getAttributeValue(), e);
                }
            } else {
                OMElement policy = wsrm.getFirstChildWithName(
                    new QName(org.apache.axis2.namespace.Constants.URI_POLICY, "Policy"));
                if (policy != null) {
                    OMPolicyReader reader = (OMPolicyReader) PolicyFactory.getPolicyReader(PolicyFactory.OM_POLICY_READER);
                    // TODO fix this return reader.readPolicy(policy);
                    return null;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * Is WS-RM enabled?
     * @param elem the Synapse configuration XML element
     * @return true if WS-RM is enabled
     */
    public static boolean isRMEnabled(OMElement elem) {
        OMElement wsrm = elem.getFirstChildWithName(new QName(Constants.NULL_NAMESPACE, "reliablemessaging"));
        if (wsrm != null) {
            OMAttribute enabled = wsrm.getAttribute(new QName(Constants.NULL_NAMESPACE, "enabled"));
            if (enabled != null) {
                return "true".equals(enabled.getAttributeValue());
            }
        }
        return false;
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
