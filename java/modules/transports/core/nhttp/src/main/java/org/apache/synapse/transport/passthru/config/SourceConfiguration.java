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

package org.apache.synapse.transport.passthru.config;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.*;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.axis2.AxisFault;

import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.HttpGetRequestProcessor;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.connections.SourceConnections;

import java.net.UnknownHostException;

/**
 * This class stores configurations specific to the Listeners
 */
public class SourceConfiguration extends BaseConfiguration {

    private Log log = LogFactory.getLog(SourceConfiguration.class);

    /** Response factory used for creating HTTP Responses */
    private HttpResponseFactory responseFactory = null;

    /** port of the listener */
    private int port = 8280;

    private String bindAddress = null;

    /** Object to manage the source connections */
    private SourceConnections sourceConnections = null;

    private String host;

    private String transportName;

    /** The EPR prefix for services available over this transport */
    private String serviceEPRPrefix;

    /** The EPR prefix for services with custom URI available over this transport */
    private String customEPRPrefix;
    
    /** SSLContext if this listener is a SSL listener */
    private boolean ssl = false;

    /** WSDL processor for Get requests*/
    private HttpGetRequestProcessor httpGetRequestProcessor = null;

    public SourceConfiguration(ConfigurationContext configurationContext,
                               TransportInDescription description,
                               WorkerPool pool, boolean ssl) throws AxisFault {

        super(configurationContext, description, pool);
        this.transportName = description.getName();
        this.ssl = ssl;
        this.responseFactory = new DefaultHttpResponseFactory();
        this.sourceConnections = new SourceConnections();
        this.port = ParamUtils.getRequiredParamInt(parameters, "port");

        Parameter bindAddressParameter = parameters.getParameter(PassThroughConstants.BIND_ADDRESS);
        if (bindAddressParameter != null) {
            this.bindAddress = ((String) bindAddressParameter.getValue()).trim();
        }

        Parameter hostParameter = parameters.getParameter(TransportListener.HOST_ADDRESS);
        if (hostParameter != null) {
            host = ((String) hostParameter.getValue()).trim();
        } else {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                log.warn("Unable to lookup local host name, using 'localhost'");
            }
        }

        Parameter param = parameters.getParameter(PassThroughConstants.WSDL_EPR_PREFIX);
        if (param != null) {
            serviceEPRPrefix = getServiceEPRPrefix(configurationContext, (String) param.getValue());
            customEPRPrefix = (String) param.getValue();
        } else {
            serviceEPRPrefix = getServiceEPRPrefix(configurationContext, host, port);
            customEPRPrefix = transportName + "://" + host + ":" +
                    (port == 80 ? "" : port) + "/";
        }

        // create http Get processor
        param = parameters.getParameter(NhttpConstants.HTTP_GET_PROCESSOR);
        if (param != null && param.getValue() != null) {
            httpGetRequestProcessor = createHttpGetProcessor(param.getValue().toString());
            if (httpGetRequestProcessor == null) {
                handleException("Cannot create HttpGetRequestProcessor");
            }
        }
    }

    @Override
    protected HttpProcessor initHttpProcessor() {
        String  server = conf.getStringProperty(
                PassThroughConfigPNames.SERVER_HEADER_VALUE,
                "Synapse-PT-HttpComponents-NIO");
        return new ImmutableHttpProcessor(
                new ResponseDate(),
                new ResponseServer(server),
                new ResponseContent(),
                new ResponseConnControl());
    }

    public HttpResponseFactory getResponseFactory() {
        return responseFactory;
    }

    public int getPort() {
        return port;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public SourceConnections getSourceConnections() {
        return sourceConnections;
    }

    public String getTransportName() {
        return transportName;
    }

    public String getServiceEPRPrefix() {
        return serviceEPRPrefix;
    }

    public String getCustomEPRPrefix() {
        return customEPRPrefix;
    }

    public boolean isSsl() {
		return ssl;
	}

	public HttpGetRequestProcessor getHttpGetRequestProcessor() {
		return httpGetRequestProcessor;
	}

	/**
     * Return the EPR prefix for services made available over this transport
     * @param cfgCtx configuration context to retrieve the service context path
     * @param wsdlEPRPrefix specified wsdlPrefix
     *
     * @return wsdlEPRPrefix for the listener
     */
    protected String getServiceEPRPrefix(ConfigurationContext cfgCtx, String wsdlEPRPrefix) {
        return wsdlEPRPrefix +
            (!cfgCtx.getServiceContextPath().startsWith("/") ? "/" : "") +
            cfgCtx.getServiceContextPath() +
            (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
    }

    /**
     * Return the EPR prefix for services made available over this transport
     * @param cfgCtx configuration context to retrieve the service context path
     * @param host name of the host
     * @param port listening port
     * @return wsdlEPRPrefix for the listener
     */
	protected String getServiceEPRPrefix(ConfigurationContext cfgCtx,
			String host, int port) {
		if (!ssl) {
			return "http://"
					+ host
					+ (port == 80 ? "" : ":" + port)
					+ (!cfgCtx.getServiceContextPath().startsWith("/") ? "/"
							: "")
					+ cfgCtx.getServiceContextPath()
					+ (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
		} else {

			return "https://"
					+ host
					+ (port == 443 ? "" : ":" + port)
					+ (!cfgCtx.getServiceContextPath().startsWith("/") ? "/"
							: "")
					+ cfgCtx.getServiceContextPath()
					+ (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
		}
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
    
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
    
    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }
}
