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

import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.jms.JMSConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Util;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.PolicyReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * <proxy name="string" [description="string"] [transports="(http|https|jms)+|all"]>
 *   <target sequence="name" | endpoint="name"/>?   // default is main sequence
 *   <wsdl key="string">?
 *   <schema key="string">*
 *   <policy key="string">*
 *   <property name="string" value="string"/>*
 *   <enableRM/>+
 *   <enableSec/>+
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

    /** The key for the base WSDL, if specified */
    private String wsdlKey;
    /** The keys for any supplied schemas */
    private List schemaKeys = new ArrayList();
    /** The keys for any supplied policies that would apply at the service level */
    private List serviceLevelPolicies = new ArrayList();
    /** Should WS RM (default configuration) be engaged on this service */
    private boolean wsRMEnabled = false;
    /** Should WS Sec (default configuration) be engaged on this service */
    private boolean wsSecEnabled = false;

    public static final String ALL_TRANSPORTS = "all";

    public ProxyService() {}

    public AxisService buildAxisService(SynapseConfiguration synCfg, AxisConfiguration axisCfg) {

        AxisService proxyService = null;
        if (wsdlKey != null) {
            try {
                InputStream wsdlInputStream = Util.getInputStream(synCfg.getProperty(wsdlKey));
                // detect version of the WSDL 1.1 or 2.0
                OMNamespace documentElementNS = new StAXOMBuilder(wsdlInputStream).getDocumentElement().getNamespace();

                wsdlInputStream = Util.getInputStream(synCfg.getProperty(wsdlKey));

                if (documentElementNS != null) {
                    WSDLToAxisServiceBuilder wsdlToAxisServiceBuilder = null;
                    if (WSDLConstants.WSDL20_2006Constants.DEFAULT_NAMESPACE_URI.
                        equals(documentElementNS.getNamespaceURI())) {
                        wsdlToAxisServiceBuilder =
                            new WSDL20ToAxisServiceBuilder(wsdlInputStream, null, null);

                    } else if (org.apache.axis2.namespace.Constants.NS_URI_WSDL11.
                        equals(documentElementNS.getNamespaceURI())) {
                        wsdlToAxisServiceBuilder =
                            new WSDL11ToAxisServiceBuilder(wsdlInputStream, null, null);
                    } else {
                        handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
                    }

                    assert wsdlToAxisServiceBuilder != null;
                    proxyService = wsdlToAxisServiceBuilder.populateService();
                    proxyService.setWsdlfound(true);

                } else {
                    handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
                }

            } catch (XMLStreamException e) {
                handleException("Error reading WSDL defined by registry key : " + wsdlKey, e);
            } catch (AxisFault af) {
                handleException("Error building service from WSDL defined by registry key : " + wsdlKey, af);
            } catch (IOException ioe) {
                handleException("Error reading WSDL from WSDL defined by registry key : " + wsdlKey, ioe);
            }
        } else {
            // this is for POX... create a dummy service and an operation for which
            // our SynapseDispatcher will properly dispatch to
            proxyService = new AxisService();
            AxisOperation mediateOperation =
                new InOutAxisOperation(new QName("mediate"));
            proxyService.addOperation(mediateOperation);
        }

        // Set the name and description. Currently Axis2 uses the name as the
        // default Service destination
        assert proxyService != null;
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
        /*if (!serviceLevelPolicies.isEmpty()) {
            PolicyReader reader = PolicyFactory.getPolicyReader(PolicyFactory.OM_POLICY_READER);
            Policy svcEffectivePolicy = null;

            String policyKey;
            iter = serviceLevelPolicies.iterator();
            while (iter.hasNext()) {
                policyKey = (String) iter.next();
                if (svcEffectivePolicy == null) {
                    svcEffectivePolicy = reader.readPolicy(
                        Util.getStreamSource(synCfg.getProperty(policyKey)).getInputStream());
                } else {
                    svcEffectivePolicy.merge(reader.readPolicy(
                        Util.getStreamSource(synCfg.getProperty(policyKey)).getInputStream()));
                }
            }

            PolicyInclude policyInclude = new PolicyInclude();
            policyInclude.addPolicyElement(PolicyInclude.SERVICE_POLICY, svcEffectivePolicy);
            proxyService.setPolicyInclude(policyInclude);
        }
*/
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

        // should RM be engaged on this service?
        if (wsRMEnabled) {
            try {
                proxyService.engageModule(
                    axisCfg.getModule(Constants.SANDESHA2_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS RM module on proxy service : " + name, axisFault);
            }
        }

        // should Security be engaged on this service?
        if (wsSecEnabled) {
            try {
                proxyService.engageModule(
                    axisCfg.getModule(Constants.RAMPART_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS Sec module on proxy service : " + name, axisFault);
            }
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
        return transports != null ? transports : ALL_TRANSPORTS;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public Map getPropertyMap() {
        return this.properties;
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

    public String getWSDLKey() {
        return wsdlKey;
    }

    public void setWSDLKey(String wsdlKey) {
        this.wsdlKey = wsdlKey;
    }

    public List getSchemas() {
        return schemaKeys;
    }

    public void setSchemas(List schemas) {
        this.schemaKeys = schemas;
    }

    public List getServiceLevelPolicies() {
        return serviceLevelPolicies;
    }

    public void addServiceLevelPoliciy(URL serviceLevelPolicy) {
        this.serviceLevelPolicies.add(serviceLevelPolicy);
    }

    public boolean isWsRMEnabled() {
        return wsRMEnabled;
    }

    public void setWsRMEnabled(boolean wsRMEnabled) {
        this.wsRMEnabled = wsRMEnabled;
    }

    public boolean isWsSecEnabled() {
        return wsSecEnabled;
    }

    public void setWsSecEnabled(boolean wsSecEnabled) {
        this.wsSecEnabled = wsSecEnabled;
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
