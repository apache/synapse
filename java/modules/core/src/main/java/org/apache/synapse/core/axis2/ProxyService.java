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

import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.Util;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.Policy;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
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
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);
    /** The proxy service name */
    private String name;
    /** The proxy service description */
    private String description;
    /** The transport/s over which this service should be exposed */
    //private String transports;
    private ArrayList transports;
    /** The target endpoint, if assigned */
    private String targetEndpoint = null;
    /** The target inSequence, if assigned */
    private String targetInSequence = null;
    /** The target outSequence, if assigned */
    private String targetOutSequence = null;
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
    /** This will say weather need to start the service at the load or not */
    private boolean startOnLoad = true;
    /** This will hold the status of the proxy weather it is running or not */
    private boolean running = false;

    public static final String ALL_TRANSPORTS = "all";

    /** The variable that indicate tracing on or off for the current mediator */
    protected int traceState = Constants.TRACING_UNSET;

    public ProxyService() {}

    public AxisService buildAxisService(SynapseConfiguration synCfg, AxisConfiguration axisCfg) {

        AxisService proxyService = null;
        if (wsdlKey != null) {
            try {
                InputStream wsdlInputStream = Util.getInputStream(synCfg.getProperty(wsdlKey));
                // detect version of the WSDL 1.1 or 2.0
                OMNamespace documentElementNS = new StAXOMBuilder(
                        wsdlInputStream).getDocumentElement().getNamespace();

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

                    if (wsdlToAxisServiceBuilder == null) {
                        throw new SynapseException(
                                "Could not get the WSDL to Axis Service Builder");
                    }
                    proxyService = wsdlToAxisServiceBuilder.populateService();
                    proxyService.setWsdlFound(true);

                } else {
                    handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
                }

            } catch (XMLStreamException e) {
                handleException("Error reading WSDL defined by registry key : " + wsdlKey, e);
            } catch (AxisFault af) {
                handleException("Error building service from WSDL defined by registry key : "
                        + wsdlKey, af);
            } catch (IOException ioe) {
                handleException("Error reading WSDL from WSDL defined by registry key : "
                        + wsdlKey, ioe);
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
        if (proxyService == null) {
            throw new SynapseException("Could not create a proxy service");
        }
        proxyService.setName(name);
        if (description != null) {
            proxyService.setServiceDescription(description);
        }

        // process transports and expose over requested transports. If none
        // is specified, default to all transports using service name as
        // destination
        if (transports == null || transports.size() == 0) {
            // default to all transports using service name as destination
        } else {
            proxyService.setExposedTransports(transports);
        }

        // process parameters
        Iterator iter = properties.keySet().iterator();
        while (iter.hasNext()) {
            String name  = (String) iter.next();
            String value = (String) properties.get(name);

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

        // if service level policies are specified, apply them
        if (!serviceLevelPolicies.isEmpty()) {

            Policy svcEffectivePolicy = null;

            iter = serviceLevelPolicies.iterator();
            while (iter.hasNext()) {
                String policyKey = (String) iter.next();
                if (svcEffectivePolicy == null) {
                    svcEffectivePolicy = PolicyEngine.getPolicy(
                        Util.getStreamSource(synCfg.getProperty(policyKey)).getInputStream());
                } else {
                    svcEffectivePolicy = (Policy) svcEffectivePolicy.merge(
                        PolicyEngine.getPolicy(
                            Util.getStreamSource(synCfg.getProperty(policyKey)).getInputStream()));
                }
            }
            PolicyInclude pi = proxyService.getPolicyInclude();
            pi.addPolicyElement(PolicyInclude.AXIS_SERVICE_POLICY, svcEffectivePolicy);
        }

        // create a custom message receiver for this proxy service to use a given named
        // endpoint or sequence for forwarding/message mediation
        ProxyServiceMessageReceiver msgRcvr = new ProxyServiceMessageReceiver();
        msgRcvr.setName(name);
        if (targetEndpoint != null) {
            msgRcvr.setTargetEndpoint(targetEndpoint);
        } else {
            if (targetInSequence != null) {
                msgRcvr.setTargetInSequence(targetInSequence);
            }
            if (targetOutSequence != null) {
                msgRcvr.setTargetOutSequence(targetOutSequence);
            }
        }

        iter = proxyService.getOperations();
        while (iter.hasNext()) {
            AxisOperation op = (AxisOperation) iter.next();
            op.setMessageReceiver(msgRcvr);
        }

        try {
            axisCfg.addService(proxyService);
            this.setRunning(true);
        } catch (AxisFault axisFault) {
            handleException("Unable to start the Proxy Service");
        }

        // should RM be engaged on this service?
        if (wsRMEnabled) {
            try {
                proxyService.engageModule(axisCfg.getModule(
                        Constants.SANDESHA2_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS RM module on proxy service : " + name, axisFault);
            }
        }

        // should Security be engaged on this service?
        if (wsSecEnabled) {
            try {
                proxyService.engageModule(axisCfg.getModule(
                        Constants.RAMPART_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS Sec module on proxy service : "
                        + name, axisFault);
            }
        }
        return proxyService;
    }

    public void start(SynapseConfiguration synCfg) {
        AxisConfiguration axisConfig = synCfg.getAxisConfiguration();
        axisConfig.getServiceForActivation(this.getName()).setActive(true);
        this.setRunning(true);
    }

    public void stop(SynapseConfiguration synCfg) {
        AxisConfiguration axisConfig = synCfg.getAxisConfiguration().getAxisConfiguration();
        try {
            axisConfig.getService(this.getName()).setActive(false);
            this.setRunning(false);
        } catch (AxisFault axisFault) {
            handleException(axisFault.getMessage());
        }
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

    public ArrayList getTransports() {
        return transports;
    }

    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public Map getPropertyMap() {
        return this.properties;
    }

    public void setTransports(ArrayList transports) {
        this.transports = transports;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getTargetInSequence() {
        return targetInSequence;
    }

    public void setTargetInSequence(String targetInSequence) {
        this.targetInSequence = targetInSequence;
    }

    public String getTargetOutSequence() {
        return targetOutSequence;
    }

    public void setTargetOutSequence(String targetOutSequence) {
        this.targetOutSequence = targetOutSequence;
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

    public void addServiceLevelPolicy(String serviceLevelPolicy) {
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

    public boolean isStartOnLoad() {
        return startOnLoad;
    }

    public void setStartOnLoad(boolean startOnLoad) {
        this.startOnLoad = startOnLoad;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Returns the int value that indicate the tracing state
     *
     * @return Returns the int value that indicate the tracing state
     */
    public int getTraceState() {
        return traceState;
    }

    /**
     * Set the tracing State variable
     *
     * @param traceState
     */
    public void setTraceState(int traceState) {
        this.traceState = traceState;
    }
}
