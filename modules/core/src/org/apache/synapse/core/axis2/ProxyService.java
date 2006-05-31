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

import org.apache.axis2.description.WSDL11ToAxisServiceBuilder;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisOperation;
import org.apache.synapse.SynapseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * <proxy name="string" type="wsdl|jms|rest" [description="string"]>
 *   <endpoint protocols="(http|https|jms)+|all" uri="uri">
 *   <target sequence="name" | endpoint="name"/>?
 *   <wsdl url="url">?
 *   <schema url="url">*
 *   <policy url="url">*
 * </proxy>
 */
public class ProxyService {

    private static final Log log = LogFactory.getLog(ProxyService.class);

    public static final int WSDL_TYPE   = 1;
    public static final int JMS_TYPE    = 2;
    public static final int REST_TYPE   = 3;

    public static final String PROTO_ALL   = "all";

    private String name;
    private int type = WSDL_TYPE;           // default
    private String description;

    /** The endpoint protocols the proxy service should be available on. This should contain the
     * value 'all' to enable this service on all available transports (i.e. default) or should
     * specify a comma seperated list of Axis2 transports
     */
    private String endpointProtocols = PROTO_ALL; // default
    /** The endpoint URI where the proxy service should be available on */
    private String endpointURI = null;

    /** The target endpoint, if assigned */
    private String targetEndpoint = null;
    /** The target sequence, if assigned */
    private String targetSequence = null;
    // if a target endpoint or sequence is not specified, the default Synapse main mediator will be used

    /** The URL for the base WSDL */
    private URL wsdl;
    /** The URLs for any supplied schemas */
    private URL[] schemas;
    /** The URLs for any supplied policies */
    private URL[] policies;

    public ProxyService() {}

    public AxisService buildAxisService() {
        if (type == ProxyService.WSDL_TYPE) {
            try {
                if (wsdl == null) {
                    handleException("A WSDL URL is required for a WSDL based proxy service");
                } else if (name == null) {
                    handleException("A name is required for a proxy service");
                } else if (endpointURI == null) {
                    handleException("The endpoint URI for the proxy service has not been specified");
                } else {

                    WSDL11ToAxisServiceBuilder wsdl2AxisServiceBuilder =
                        new WSDL11ToAxisServiceBuilder(wsdl.openStream(), null, null);
                    AxisService proxyService = wsdl2AxisServiceBuilder.populateService();
                    proxyService.setWsdlfound(true);

                    // Set the name and description. Currently Axis2 uses the name as the Service name/URI
                    proxyService.setName(name);
                    proxyService.setServiceDescription(description);

                    // TODO
                    // Axis still does not allow us to set a pre-defined URL for a deployed service
                    // proxyService.setEndpointURI(endpointURI);

                    // set exposed transports http|https|jms|all.. etc
                    if (!PROTO_ALL.equals(endpointProtocols)) {
                        StringTokenizer st = new StringTokenizer(endpointProtocols, ",");
                        String[] transposrts = new String[st.countTokens()];
                        for (int i=0; i<transposrts.length; i++) {
                            transposrts[i] = st.nextToken();
                        }
                        proxyService.setExposeTransports(transposrts);
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

                    Iterator iter = proxyService.getOperations();
                    while (iter.hasNext()) {
                        AxisOperation op = (AxisOperation) iter.next();
                        op.setMessageReceiver(msgRcvr);
                    }

                    return proxyService;
                }

            } catch (IOException e) {
                handleException("Error opening input stream to WSDL at URL : " + wsdl, e);
            }
        } else {
            // TODO
            throw new UnsupportedOperationException("Only WSDL Proxy services are supported");
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEndpointProtocols() {
        return endpointProtocols;
    }

    public void setEndpointProtocols(String endpointProtocols) {
        this.endpointProtocols = endpointProtocols;
    }

    public String getEndpointURI() {
        return endpointURI;
    }

    public void setEndpointURI(String endpointURI) {
        this.endpointURI = endpointURI;
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

    public URL[] getPolicies() {
        return policies;
    }

    public void setPolicies(URL[] policies) {
        this.policies = policies;
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
