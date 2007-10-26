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
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.net.*;

/**
 * <proxy-service name="string" [transports="(http |https |jms )+|all"] [trace="enable|disable"]>
 *    <description>..</description>?
 *    <target [inSequence="name"] [outSequence="name"] [faultSequence="name"] [endpoint="name"]>
 *       <endpoint>...</endpoint>
 *       <inSequence>...</inSequence>
 *       <outSequence>...</outSequence>
 *       <faultSequence>...</faultSequence>
 *    </target>?
 *    <publishWSDL uri=".." key="string">
 *       <wsdl:definition>...</wsdl:definition>?
 *       <wsdl20:description>...</wsdl20:description>?
 *    </publishWSDL>?
 *    <enableSec/>?
 *    <enableRM/>?
 *    <policy key="string">?
 *       // optional service parameters
 *    <parameter name="string">
 *       text | xml
 *    </parameter>?
 * </proxy-service>
 */
public class ProxyService {

    private static final Log log = LogFactory.getLog(ProxyService.class);
    private static final Log trace = LogFactory.getLog(SynapseConstants.TRACE_LOGGER);
    private final Log serviceLog;
    /**
     * The name of the proxy service
     */
    private String name;
    /**
     * The proxy service description. This could be optional informative text about the service
     */
    private String description;
    /**
     * The transport/s over which this service should be exposed, or defaults to all available
     */
    private ArrayList transports;
    /**
     * The target endpoint key
     */
    private String targetEndpoint = null;
    /**
     * The target inSequence key
     */
    private String targetInSequence = null;
    /**
     * The target outSequence key
     */
    private String targetOutSequence = null;
    /**
     * The target faultSequence key
     */
    private String targetFaultSequence = null;
    /**
     * The inlined definition of the target endpoint, if defined
     */
    private Endpoint targetInLineEndpoint = null;
    /**
     * The inlined definition of the target in-sequence, if defined
     */
    private SequenceMediator targetInLineInSequence = null;
    /**
     * The inlined definition of the target out-sequence, if defined
     */
    private SequenceMediator targetInLineOutSequence = null;
    /**
     * The inlined definition of the target fault-sequence, if defined
     */
    private SequenceMediator targetInLineFaultSequence = null;
    /**
     * A list of any service parameters (e.g. JMS parameters etc)
     */
    private Map parameters = new HashMap();
    /**
     * The key for the base WSDL
     */
    private String wsdlKey;
    /**
     * The URI for the base WSDL, if defined as a URL
     */
    private URI wsdlURI;
    /**
     * The inlined representation of the service WSDL, if defined inline
     */
    private Object inLineWSDL;
    /**
     * The keys for any supplied policies that would apply at the service level
     */
    private List serviceLevelPolicies = new ArrayList();
    /**
     * Should WS RM be engaged on this service
     */
    private boolean wsRMEnabled = false;
    /**
     * Should WS Sec be engaged on this service
     */
    private boolean wsSecEnabled = false;
    /**
     * Should this service be started by default on initialization?
     */
    private boolean startOnLoad = true;
    /**
     * Is this service running now?
     */
    private boolean running = false;

    public static final String ALL_TRANSPORTS = "all";

    /**
     * To decide to whether statistics should have collected or not
     */
    private int statisticsState = SynapseConstants.STATISTICS_UNSET;
    /**
     * The variable that indicate tracing on or off for the current mediator
     */
    protected int traceState = SynapseConstants.TRACING_UNSET;


    /**
     * Constructor
     *
     * @param name the name of the Proxy service
     */
    public ProxyService(String name) {
        this.name = name;
        serviceLog = LogFactory.getLog(SynapseConstants.SERVICE_LOGGER_PREFIX + name);
    }

    /**
     * Build the underlying Axis2 service from the Proxy service definition
     *
     * @param synCfg the Synapse configuration
     * @param axisCfg the Axis2 configuration
     * @return the Axis2 service for the Proxy
     */
    public AxisService buildAxisService(SynapseConfiguration synCfg, AxisConfiguration axisCfg) {

        auditInfo("Building Axis service for Proxy service : " + name);
        AxisService proxyService = null;

        // get the wsdlElement as an OMElement
        if (trace()) {
            trace.info("Loading the WSDL : " +
                (wsdlKey != null ? " key = " + wsdlKey :
                (wsdlURI != null ? " URI = " + wsdlURI : " <Inlined>")));
        }

        InputStream wsdlInputStream = null;
        OMElement wsdlElement = null;

        if (wsdlKey != null) {
            synCfg.getEntryDefinition(wsdlKey);
            Object keyObject = synCfg.getEntry(wsdlKey);
            if (keyObject instanceof OMElement) {
                wsdlElement = (OMElement) keyObject;
            }
        } else if (inLineWSDL != null) {
            wsdlElement = (OMElement) inLineWSDL;
        } else if (wsdlURI != null) {
            try {
                URL url = wsdlURI.toURL();
                wsdlElement = SynapseConfigUtils.getOMElementFromURL(url.toString());
            } catch (MalformedURLException e) {
                handleException("Malformed URI for wsdl", e);
            } catch (IOException e) {
                handleException("Error reading from wsdl URI", e);
            }
        }

        // if a WSDL was found
        if (wsdlElement != null) {
            OMNamespace wsdlNamespace = wsdlElement.getNamespace();

            // serialize and create an inputstream to read WSDL
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                if (trace()) trace.info("Serializing wsdlElement found to build an Axis2 service");
                wsdlElement.serialize(baos);
                wsdlInputStream = new ByteArrayInputStream(baos.toByteArray());
            } catch (XMLStreamException e) {
                handleException("Error converting to a StreamSource", e);
            }

            if (wsdlInputStream != null) {

                try {
                    // detect version of the WSDL 1.1 or 2.0
                    if (trace()) trace.info("WSDL Namespace is : " + wsdlNamespace.getNamespaceURI());

                    if (wsdlNamespace != null) {
                        boolean isWSDL11 = false;
                        WSDLToAxisServiceBuilder wsdlToAxisServiceBuilder = null;

                        if (WSDL2Constants.WSDL_NAMESPACE.
                                equals(wsdlNamespace.getNamespaceURI())) {
                            wsdlToAxisServiceBuilder =
                                    new WSDL20ToAxisServiceBuilder(wsdlInputStream, null, null);
                            wsdlToAxisServiceBuilder.setBaseUri(wsdlURI != null ? wsdlURI.toString() : "");

                        } else if (org.apache.axis2.namespace.Constants.NS_URI_WSDL11.
                                equals(wsdlNamespace.getNamespaceURI())) {
                            wsdlToAxisServiceBuilder =
                                    new WSDL11ToAxisServiceBuilder(wsdlInputStream, null, null);
                            isWSDL11 = true;
                        } else {
                            handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
                        }

                        if (wsdlToAxisServiceBuilder == null) {
                            throw new SynapseException(
                                    "Could not get the WSDL to Axis Service Builder");
                        }

                        if (trace()) {
                            trace.info("Populating Axis2 service using WSDL");
                            if (trace.isTraceEnabled()) {
                                trace.trace("WSDL : " + wsdlElement.toString());
                            }
                        }
                        proxyService = wsdlToAxisServiceBuilder.populateService();
                        List schemaList = proxyService.getSchema();
                        if (schemaList != null && schemaList.size() > 0) {
                            // just pick the first schema's target namespace as Axis2's
                            // HTTPTransportUtils code already contains a bug where it uses the
                            // services' schema target NS for each operation
                            proxyService.setSchemaTargetNamespace(
                                proxyService.getSchema(0).getTargetNamespace());
                        }
                        proxyService.setWsdlFound(true);

                        if (isWSDL11) {
                            // workaround to support WSDL 2.0 generation when only a WSDL 1.1
                            // is supplied
                            Collection endpoints = proxyService.getEndpoints().values();
                            Iterator iter = endpoints.iterator();
                            while (iter.hasNext()) {
                                AxisEndpoint endpoint = (AxisEndpoint) iter.next();
                                Iterator children = endpoint.getBinding().getChildren();
                                while (children.hasNext()) {
                                    AxisBindingOperation axisBindingOperation =
                                        (AxisBindingOperation) children.next();
                                    axisBindingOperation.setProperty(
                                        WSDL2Constants.ATTR_WHTTP_IGNORE_UNCITED, new Boolean(false));
                                }
                            }
                        }

                    } else {
                        handleException("Unknown WSDL format.. not WSDL 1.1 or WSDL 2.0");
                    }

                } catch (AxisFault af) {
                    handleException("Error building service from WSDL", af);
                } catch (IOException ioe) {
                    handleException("Error reading WSDL", ioe);
                }
            }
        } else {
            // this is for POX... create a dummy service and an operation for which
            // our SynapseDispatcher will properly dispatch to
            if (trace()) trace.info("Did not find a WSDL. Assuming a POX or Legacy service");
            proxyService = new AxisService();
            AxisOperation mediateOperation = new InOutAxisOperation(new QName("mediate"));
            proxyService.addOperation(mediateOperation);
        }

        // Set the name and description. Currently Axis2 uses the name as the
        // default Service destination
        if (proxyService == null) {
            throw new SynapseException("Could not create a proxy service");
        }
        proxyService.setName(name);
        if (description != null) {
            proxyService.setDocumentation(description);
        }

        // process transports and expose over requested transports. If none
        // is specified, default to all transports using service name as
        // destination
        if (transports == null || transports.size() == 0) {
            // default to all transports using service name as destination
        } else {
            if (trace()) trace.info("Exposing transports : " + transports);
            proxyService.setExposedTransports(transports);
        }

        // process parameters
        if (trace()) trace.info("Setting service parameters : " + parameters);
        Iterator iter = parameters.keySet().iterator();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            Object value = parameters.get(name);

            Parameter p = new Parameter();
            p.setName(name);
            p.setValue(value);

            try {
                proxyService.addParameter(p);
            } catch (AxisFault af) {
                handleException("Error setting parameter : " + name + "" +
                        "to proxy service as a Parameter", af);
            }
        }

        if (trace()) trace.info("Setting service level policies : " + serviceLevelPolicies);
        // if service level policies are specified, apply them

        if (!serviceLevelPolicies.isEmpty()) {
            Policy svcEffectivePolicy = null;
            iter = serviceLevelPolicies.iterator();

            while (iter.hasNext()) {
                String policyKey = (String) iter.next();
                synCfg.getEntryDefinition(policyKey);
                Object policyProp = synCfg.getEntry(policyKey);
                if (policyProp != null) {
                    if (svcEffectivePolicy == null) {
                        svcEffectivePolicy = PolicyEngine.getPolicy(
                            SynapseConfigUtils.getStreamSource(policyProp).getInputStream());
                    } else {
                        svcEffectivePolicy = (Policy) svcEffectivePolicy.merge(
                            PolicyEngine.getPolicy(SynapseConfigUtils.getStreamSource(policyProp).getInputStream()));
                    }
                }
            }
            PolicyInclude pi = proxyService.getPolicyInclude();
            if (pi != null && svcEffectivePolicy != null) {
                if (trace()) {
                    if (trace.isTraceEnabled()) {
                        trace.trace("Effective policy applied : " + svcEffectivePolicy);
                    }
                }
                pi.addPolicyElement(PolicyInclude.AXIS_SERVICE_POLICY, svcEffectivePolicy);
            }
        }

        // create a custom message receiver for this proxy service 
        ProxyServiceMessageReceiver msgRcvr = new ProxyServiceMessageReceiver();
        msgRcvr.setName(name);
        msgRcvr.setProxy(this);

        iter = proxyService.getOperations();
        while (iter.hasNext()) {
            AxisOperation op = (AxisOperation) iter.next();
            op.setMessageReceiver(msgRcvr);
        }

        try {
            auditInfo("Adding service " + name + " to the Axis2 configuration");
            axisCfg.addService(proxyService);
            this.setRunning(true);
        } catch (AxisFault axisFault) {
            try {
                if (axisCfg.getService(proxyService.getName()) != null) {
                    if (trace()) trace.info("Removing service " + name + " due to error : "
                        + axisFault.getMessage());
                    axisCfg.removeService(proxyService.getName());
                }
            } catch (AxisFault ignore) {}
            handleException("Error adding Proxy service to the Axis2 engine", axisFault);
        }

        // todo: need to remove this and engage modules by looking at policies
        // should RM be engaged on this service?
        if (wsRMEnabled) {
            auditInfo("WS-Reliable messaging is enabled for service : " + name);
            try {
                proxyService.engageModule(axisCfg.getModule(
                    SynapseConstants.SANDESHA2_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS RM module on proxy service : " + name, axisFault);
            }
        }

        // should Security be engaged on this service?
        if (wsSecEnabled) {
            auditInfo("WS-Security is enabled for service : " + name);
            try {
                proxyService.engageModule(axisCfg.getModule(
                    SynapseConstants.RAMPART_MODULE_NAME), axisCfg);
            } catch (AxisFault axisFault) {
                handleException("Error loading WS Sec module on proxy service : "
                        + name, axisFault);
            }
        }

        auditInfo("Successfully created the Axis2 service for Proxy service : " + name);
        return proxyService;
    }

    /**
     * Start the proxy service
     * @param synCfg the synapse configuration
     */
    public void start(SynapseConfiguration synCfg) {
        AxisConfiguration axisConfig = synCfg.getAxisConfiguration();
        if (axisConfig != null) {
            axisConfig.getServiceForActivation(this.getName()).setActive(true);
            this.setRunning(true);
            auditInfo("Started the proxy service : " + name);
        } else {
            auditWarn("Unable to start proxy service : " + name + 
                ". Couldn't access Axis configuration");
        }
    }

    /**
     * Stop the proxy service
     * @param synCfg the synapse configuration
     */
    public void stop(SynapseConfiguration synCfg) {
        AxisConfiguration axisConfig = synCfg.getAxisConfiguration();
        if (axisConfig != null) {
            try {
                AxisService as = axisConfig.getService(this.getName());
                if (as != null) {
                    as.setActive(false);
                }
                this.setRunning(false);
                auditInfo("Started the proxy service : " + name);
            } catch (AxisFault axisFault) {
                handleException("Error stopping the proxy service : " + name, axisFault);
            }
        } else {
            auditWarn("Unable to stop proxy service : " + name +
                ". Couldn't access Axis configuration");
        }
    }

    private void handleException(String msg) {
        serviceLog.error(msg);
        log.error(msg);
        if (trace()) trace.error(msg);
        throw new SynapseException(msg);
    }

    private void handleException(String msg, Exception e) {
        serviceLog.error(msg);
        log.error(msg, e);
        if (trace()) trace.error(msg + " :: " + e.getMessage());
        throw new SynapseException(msg, e);
    }

    /**
     * Write to the general log, as well as any service specific logs the audit message at INFO
     * @param message the INFO level audit message
     */
    private void auditInfo(String message) {
        log.info(message);
        serviceLog.info(message);
        if (trace()) {
            trace.info(message);
        }
    }

    /**
     * Write to the general log, as well as any service specific logs the audit message at WARN
     * @param message the WARN level audit message
     */
    private void auditWarn(String message) {
        log.warn(message);
        serviceLog.warn(message);
        if (trace()) {
            trace.warn(message);
        }
    }

    /**
     * Return true if tracing should be enabled
     * @return true if tracing is enabled for this service
     */
    private boolean trace() {
        return traceState == SynapseConstants.TRACING_ON;
    }

    public String getName() {
        return name;
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

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    public Map getParameterMap() {
        return this.parameters;
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

    /**
     * To check whether statistics should have collected or not
     *
     * @return Returns the int value that indicate statistics is enabled or not.
     */
    public int getStatisticsState() {
        return statisticsState;
    }

    /**
     * To set the statistics enable variable value
     *
     * @param statisticsState
     */
    public void setStatisticsState(int statisticsState) {
        this.statisticsState = statisticsState;
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

    public String getTargetFaultSequence() {
        return targetFaultSequence;
    }

    public void setTargetFaultSequence(String targetFaultSequence) {
        this.targetFaultSequence = targetFaultSequence;
    }

    public Object getInLineWSDL() {
        return inLineWSDL;
    }

    public void setInLineWSDL(Object inLineWSDL) {
        this.inLineWSDL = inLineWSDL;
    }

    public URI getWsdlURI() {
        return wsdlURI;
    }

    public void setWsdlURI(URI wsdlURI) {
        this.wsdlURI = wsdlURI;
    }

    public Endpoint getTargetInLineEndpoint() {
        return targetInLineEndpoint;
    }

    public void setTargetInLineEndpoint(Endpoint targetInLineEndpoint) {
        this.targetInLineEndpoint = targetInLineEndpoint;
    }

    public SequenceMediator getTargetInLineInSequence() {
        return targetInLineInSequence;
    }

    public void setTargetInLineInSequence(SequenceMediator targetInLineInSequence) {
        this.targetInLineInSequence = targetInLineInSequence;
    }

    public SequenceMediator getTargetInLineOutSequence() {
        return targetInLineOutSequence;
    }

    public void setTargetInLineOutSequence(SequenceMediator targetInLineOutSequence) {
        this.targetInLineOutSequence = targetInLineOutSequence;
    }

    public SequenceMediator getTargetInLineFaultSequence() {
        return targetInLineFaultSequence;
    }

    public void setTargetInLineFaultSequence(SequenceMediator targetInLineFaultSequence) {
        this.targetInLineFaultSequence = targetInLineFaultSequence;
    }

}
