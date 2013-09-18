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

package org.apache.synapse.transport.nhttp;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.base.endpoint.URLEndpointsConfiguration;
import org.apache.axis2.transport.base.endpoint.config.URLEndpointsConfigurationFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.synapse.commons.evaluators.EvaluatorConstants;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.synapse.commons.evaluators.Parser;
import org.apache.synapse.commons.executors.ExecutorConstants;
import org.apache.synapse.commons.executors.PriorityExecutor;
import org.apache.synapse.commons.executors.config.PriorityExecutorFactory;
import org.apache.synapse.transport.nhttp.util.NhttpMetricsCollector;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * This class is being used to hold the different runtime objects used by the Listeners
 */
public class ListenerContext {

    private Log log = LogFactory.getLog(ListenerContext.class);

    /** The Axis2 configuration context */
    private ConfigurationContext cfgCtx;
    /** The Axis2 Transport In Description for the transport */
    private TransportInDescription transportIn;
    /** SSLContext if this listener is a SSL listener */
    private boolean ssl = false;
    /** JMX support */
    private NhttpMetricsCollector metrics = null;
    /** This will execute the requests based on calculate priority */
    private PriorityExecutor executor = null;
    /** parser for calculating the priority of incoming messages */
    private Parser parser = null;
    /** if false we won't dispatch to axis2 service in case of rest scenarios */
    private boolean restDispatching = true;
    /** WSDL processor for Get requests*/
    private HttpGetRequestProcessor httpGetRequestProcessor = null;
    /** The port to listen on, defaults to 8280 */
    private int port = 8280;
    /** The hostname to use, defaults to localhost */
    private String host = "localhost";
    /** The bind addresses as (address, port) pairs */
    private String bindAddress = null;

    /** Endpoints configuration for specific HTTP Urls */
    private URLEndpointsConfiguration endpoints = null;

    public ListenerContext(ConfigurationContext cfgCtx,
                           TransportInDescription transportIn,
                           boolean ssl) {
        this.cfgCtx = cfgCtx;
        this.transportIn = transportIn;
        this.ssl = ssl;
    }

    public void build() throws AxisFault {
        Parameter param = transportIn.getParameter(TransportListener.PARAM_PORT);
        if (param != null) {
            port = Integer.parseInt((String) param.getValue());
        }

        int portOffset = 0;

        try {
            portOffset = Integer.parseInt(System.getProperty(NhttpConstants.PORT_OFFSET, "0"));
        } catch (NumberFormatException e) {
            handleException("portOffset System property should be a valid Integer", e);
        }

        port = port + portOffset;
        System.setProperty(transportIn.getName() + ".nio.port", String.valueOf(port));

        param = transportIn.getParameter(NhttpConstants.BIND_ADDRESS);
        if (param != null) {
            bindAddress = ((String) param.getValue()).trim();
        }

        param = transportIn.getParameter(TransportListener.HOST_ADDRESS);
        if (param != null) {
            host = ((String) param.getValue()).trim();
        } else {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("Unable to lookup local host name, using 'localhost'");
            }
        }

        metrics = new NhttpMetricsCollector(true, transportIn.getName());

        // create the priority based executor and parser
        param = transportIn.getParameter(NhttpConstants.PRIORITY_CONFIG_FILE_NAME);
        if (param != null && param.getValue() != null) {
            createPriorityConfiguration(param.getValue().toString());
        }

        param = transportIn.getParameter(NhttpConstants.DISABLE_REST_SERVICE_DISPATCHING);
        if (param != null && param.getValue() != null) {
            if (param.getValue().equals("true")) {
                restDispatching = false;
            }
        }

        // create http Get processor
        param = transportIn.getParameter(NhttpConstants.HTTP_GET_PROCESSOR);
        if (param != null && param.getValue() != null) {
            httpGetRequestProcessor = createHttpGetProcessor(param.getValue().toString());
            if (httpGetRequestProcessor == null) {
                handleException("Cannot create HttpGetRequestProcessor");
            }
        } else {
            httpGetRequestProcessor = new DefaultHttpGetProcessor();
        }

        param = transportIn.getParameter(NhttpConstants.ENDPOINTS_CONFIGURATION);
        if (param != null && param.getValue() != null) {
            endpoints = new URLEndpointsConfigurationFactory().create(param.getValue().toString());
        }
    }

/**
     * Create a priority executor from the given file
     *
     * @param fileName file name of the executor configuration
     * @throws org.apache.axis2.AxisFault if an error occurs
     */
    private void createPriorityConfiguration(String fileName) throws AxisFault {
        OMElement definitions;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            definitions = OMXMLBuilderFactory.createOMBuilder(fis).getDocumentElement();
            assert definitions != null;
            definitions.build();
        } catch (FileNotFoundException e) {
            handleException("Priority configuration file cannot be found : " + fileName, e);
            return;
        } catch (OMException e) {
            handleException("Error parsing priority configuration xml file " + fileName, e);
            return;
        }

        OMElement executorElem = definitions.getFirstChildWithName(
                new QName(ExecutorConstants.PRIORITY_EXECUTOR));

        if (executorElem == null) {
            handleException(ExecutorConstants.PRIORITY_EXECUTOR +
                    " configuration is mandatory for priority based routing");
        }

        executor = PriorityExecutorFactory.createExecutor(
                null, executorElem, false, new Properties());
        OMElement conditionsElem = definitions.getFirstChildWithName(
                new QName(EvaluatorConstants.CONDITIONS));
        if (conditionsElem == null) {
            handleException("Conditions configuration is mandatory for priority based routing");
        }

        executor.init();

        assert conditionsElem != null;
        OMAttribute defPriorityAttr = conditionsElem.getAttribute(
                new QName(EvaluatorConstants.DEFAULT_PRIORITY));
        if (defPriorityAttr != null) {
            parser = new Parser(Integer.parseInt(defPriorityAttr.getAttributeValue()));
        } else {
            parser = new Parser();
        }

        try {
            parser.init(conditionsElem);
        } catch (EvaluatorException e) {
            handleException("Invalid " + EvaluatorConstants.CONDITIONS +
                    " configuration for priority based mediation", e);
        }

        log.info("Created a priority based executor from the configuration: " +
                fileName);
    }

    private HttpGetRequestProcessor createHttpGetProcessor(String str) throws AxisFault {
        Object obj = null;
        try {
            obj = Class.forName(str).newInstance();
        } catch (ClassNotFoundException e) {
            handleException("Error creating WSDL processor", e);
        } catch (InstantiationException e) {
            handleException("Error creating WSDL processor", e);
        } catch (IllegalAccessException e) {
            handleException("Error creating WSDL processor", e);
        }

        if (obj instanceof HttpGetRequestProcessor) {
            return (HttpGetRequestProcessor) obj;
        } else {
            handleException("Error creating WSDL processor. The HttpProcessor should be of type " +
                    "org.apache.synapse.transport.nhttp.HttpGetRequestProcessor");
        }

        return null;
    }

    public ConnectionConfig getConnectionConfig() {
        NHttpConfiguration cfg = NHttpConfiguration.getInstance();
        return cfg.getConnectionConfig();
    }

    public IOReactorConfig getReactorConfig() {
        NHttpConfiguration cfg = NHttpConfiguration.getInstance();
        return cfg.getListeningReactorConfig();
    }

    public ConfigurationContext getCfgCtx() {
        return cfgCtx;
    }

    public TransportInDescription getTransportIn() {
        return transportIn;
    }

    public boolean isSsl() {
        return ssl;
    }

    public NhttpMetricsCollector getMetrics() {
        return metrics;
    }

    public PriorityExecutor getExecutor() {
        return executor;
    }

    public Parser getParser() {
        return parser;
    }

    public boolean isRestDispatching() {
        return restDispatching;
    }

    public HttpGetRequestProcessor getHttpGetRequestProcessor() {
        return httpGetRequestProcessor;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public URLEndpointsConfiguration getEndpoints() {
        return endpoints;
    }

    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }
}
