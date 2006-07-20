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

import org.apache.axis2.description.*;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.njms.JMSConstants;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.policy.util.PolicyReader;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.Policy;

import java.net.URL;
import java.io.IOException;
import java.util.*;

/**
 * <proxy name="string" [description="string"] [transports="(http|https|jms)+|all"]>
 *   <target sequence="name" | endpoint="name"/>?   // default is main sequence
 *   <wsdl url="url">?
 *   <schema url="url">*
 *   <policy url="url">*
 *   <property name="string" value="string"/>*
 * </proxy>
 */
public class ProxyService {

    private static final Log log = LogFactory.getLog(ProxyService.class);
    /** The proxy service name */
    private String name;
    /** The proxy service description */
    private String description;
    /** The transport/s over which this service should be exposed */
    private String transports;
    /** The target endpoint, if assigned */
    private String targetEndpoint = null;
    /** The target sequence, if assigned */
    private String targetSequence = null;
    // if a target endpoint or sequence is not specified,
    // the default Synapse main mediator will be used
    /** A list properties */
    private Map properties = new HashMap();

    /** The URL for the base WSDL, if specified */
    private URL wsdl;
    /** The URLs for any supplied schemas */
    private URL[] schemas;
    /** The URLs for any supplied policies that would apply at the service level */
    private List serviceLevelPolicies = new ArrayList();

    public static final String ALL_TRANSPORTS = "all";

    public ProxyService() {}

    public AxisService buildAxisService() {

        AxisService proxyService = null;
        if (wsdl != null) {
            try {
                WSDL11ToAxisServiceBuilder wsdl2AxisServiceBuilder =
                    new WSDL11ToAxisServiceBuilder(wsdl.openStream(), null, null);
                proxyService = wsdl2AxisServiceBuilder.populateService();
                proxyService.setWsdlfound(true);
            } catch (AxisFault af) {
                handleException("Error building service from WSDL at : " + wsdl, af);
            } catch (IOException ioe) {
                handleException("Error reading WSDL from URL : " + wsdl, ioe);
            }
        } else {
            proxyService = new AxisService();
        }

        // Set the name and description. Currently Axis2 uses the name as the
        // default Service destination
        proxyService.setName(name);
        if (description != null) {
            proxyService.setServiceDescription(description);
        }

        // process transports and expose over requested transports. If none
        // is specified, default to all transports using service name as
        // destination
        if (transports == null || ALL_TRANSPORTS.equals(transports)) {
            // default to all transports using service name as destination
        } else {
            StringTokenizer st = new StringTokenizer(transports, " ");
            ArrayList transportList = new ArrayList();
            for (int i=0; i<st.countTokens(); i++) {
                transportList.add(st.nextToken());
            }
            proxyService.setExposedTransports(transportList);
        }

        // process parameters
        Iterator iter = properties.keySet().iterator();
        while (iter.hasNext()) {
            String name  = (String) iter.next();
            String value = (String) properties.get(name);
            if (JMSConstants.CONFAC_PARAM.equals(name) ||
                JMSConstants.DEST_PARAM.equals(name)) {

                Parameter p = new Parameter();
                p.setName(name);
                p.setValue(value);

                try {
                    proxyService.addParameter(p);
                } catch (AxisFault af) {
                    handleException("Error setting property : " + name + "" +
                        "to proxy service as a Parameter", af);
                }
            }
        }

        // if service level policies are specified, apply them
        if (!serviceLevelPolicies.isEmpty()) {
            PolicyReader reader = PolicyFactory.getPolicyReader(PolicyFactory.OM_POLICY_READER);
            Policy svcEffectivePolicy = null;

            URL policyUrl = null;
            try {
                iter = serviceLevelPolicies.iterator();
                while (iter.hasNext()) {
                    policyUrl = (URL) iter.next();
                    if (svcEffectivePolicy == null) {
                        svcEffectivePolicy = reader.readPolicy(policyUrl.openStream());
                    } else {
                        svcEffectivePolicy.merge(reader.readPolicy(policyUrl.openStream()));
                    }
                }
            } catch (IOException ioe) {
                handleException("Error reading policy from URL : " + policyUrl, ioe);
            }

            PolicyInclude policyInclude = new PolicyInclude();
            policyInclude.addPolicyElement(PolicyInclude.SERVICE_POLICY, svcEffectivePolicy);
            proxyService.setPolicyInclude(policyInclude);
        }

        // create a custom message receiver for this proxy service to use a given named
        // endpoint or sequence for forwarding/message mediation
        ProxyServiceMessageReceiver msgRcvr = new ProxyServiceMessageReceiver();
        msgRcvr.setName(name);
        if (targetEndpoint != null) {
            msgRcvr.setTargetEndpoint(targetEndpoint);
        } else if (targetSequence != null) {
            msgRcvr.setTargetSequence(targetSequence);
        }

        iter = proxyService.getOperations();
        while (iter.hasNext()) {
            AxisOperation op = (AxisOperation) iter.next();
            op.setMessageReceiver(msgRcvr);
        }

        return proxyService;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransports() {
        return transports;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public void setTransports(String transports) {
        this.transports = transports;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getTargetSequence() {
        return targetSequence;
    }

    public void setTargetSequence(String targetSequence) {
        this.targetSequence = targetSequence;
    }

    public URL getWsdl() {
        return wsdl;
    }

    public void setWsdl(URL wsdl) {
        this.wsdl = wsdl;
    }

    public URL[] getSchemas() {
        return schemas;
    }

    public void setSchemas(URL[] schemas) {
        this.schemas = schemas;
    }

    public List getServiceLevelPolicies() {
        return serviceLevelPolicies;
    }

    public void addServiceLevelPoliciy(URL serviceLevelPolicy) {
        this.serviceLevelPolicies.add(serviceLevelPolicy);
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
