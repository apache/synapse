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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.impl.nio.reactor.SSLIOSessionHandler;
import org.apache.http.params.HttpParams;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axiom.om.OMElement;

import javax.net.ssl.*;
import javax.xml.namespace.QName;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.net.URL;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.IOException;

public class HttpCoreNIOSSLSender extends HttpCoreNIOSender{

    private static final Log log = LogFactory.getLog(HttpCoreNIOSSLSender.class);

    protected IOEventDispatch getEventDispatch(
        NHttpClientHandler handler, SSLContext sslContext,
        SSLIOSessionHandler sslIOSessionHandler, HttpParams params) {
        return new SSLClientIOEventDispatch(handler, sslContext, sslIOSessionHandler, params);
    }

    /**
     * Create the SSLContext to be used by this listener
     * @param transportOut the Axis2 transport configuration
     * @return the SSLContext to be used
     */
    protected SSLContext getSSLContext(TransportOutDescription transportOut) throws AxisFault {

        KeyManager[] keymanagers  = null;
        TrustManager[] trustManagers = null;

        Parameter keyParam    = transportOut.getParameter("keystore");
        Parameter trustParam  = transportOut.getParameter("truststore");

        if (keyParam != null) {
            OMElement ksEle      = keyParam.getParameterElement().getFirstElement();
            String location      = ksEle.getFirstChildWithName(new QName("Location")).getText();
            String type          = ksEle.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = ksEle.getFirstChildWithName(new QName("Password")).getText();
            String keyPassword   = ksEle.getFirstChildWithName(new QName("KeyPassword")).getText();

            try {
                KeyStore keyStore = KeyStore.getInstance(type);
                URL url = getClass().getClassLoader().getResource(location);
                if (log.isDebugEnabled()) {
                    log.debug("Loading Key Store from URL : " + url);
                }

                keyStore.load(url.openStream(), storePassword.toCharArray());
                KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
                kmfactory.init(keyStore, keyPassword.toCharArray());
                keymanagers = kmfactory.getKeyManagers();

            } catch (GeneralSecurityException gse) {
                log.error("Error loading Key store : " + location, gse);
                throw new AxisFault("Error loading Key store : " + location, gse);
            } catch (IOException ioe) {
                log.error("Error opening Key store : " + location, ioe);
                throw new AxisFault("Error opening Key store : " + location, ioe);
            }
        }

        if (trustParam != null) {
            OMElement tsEle      = trustParam.getParameterElement().getFirstElement();
            String location      = tsEle.getFirstChildWithName(new QName("Location")).getText();
            String type          = tsEle.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = tsEle.getFirstChildWithName(new QName("Password")).getText();

            try {
                KeyStore trustStore = KeyStore.getInstance(type);
                URL url = getClass().getClassLoader().getResource(location);
                if (log.isDebugEnabled()) {
                    log.debug("Loading Trust Key Store from URL : " + url);
                }

                trustStore.load(url.openStream(), storePassword.toCharArray());
                TrustManagerFactory trustManagerfactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
                trustManagerfactory.init(trustStore);
                trustManagers = trustManagerfactory.getTrustManagers();

            } catch (GeneralSecurityException gse) {
                log.error("Error loading Key store : " + location, gse);
                throw new AxisFault("Error loading Key store : " + location, gse);
            } catch (IOException ioe) {
                log.error("Error opening Key store : " + location, ioe);
                throw new AxisFault("Error opening Key store : " + location, ioe);
            }
        }

        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(keymanagers, trustManagers, null);
            return sslcontext;
            
        } catch (GeneralSecurityException gse) {
            log.error("Unable to create SSL context with the given configuration", gse);
            throw new AxisFault("Unable to create SSL context with the given configuration", gse);
        }
    }

    /**
     * Create the SSLIOSessionHandler to initialize the host name verification at the following
     * levels, through an Axis2 transport configuration parameter as follows:
     * HostnameVerifier - Default, DefaultAndLocalhost, Strict, AllowAll
     *
     * @param transportOut the Axis2 transport configuration
     * @return the SSLIOSessionHandler to be used
     * @throws AxisFault if a configuration error occurs
     */
    protected SSLIOSessionHandler getSSLIOSessionHandler(TransportOutDescription transportOut) throws AxisFault {

        final Parameter hostnameVerifier = transportOut.getParameter("HostnameVerifier");

        return new SSLIOSessionHandler() {

            public void initalize(SSLEngine sslengine, HttpParams params) {
            }

            public void verify(SocketAddress remoteAddress, SSLSession session)
                throws SSLException {

                String address = null;
                if (remoteAddress instanceof InetSocketAddress) {
                    address = ((InetSocketAddress) remoteAddress).getHostName();
                } else {
                    address = remoteAddress.toString();
                }

                boolean valid = false;
                if (hostnameVerifier != null) {
                    if ("Strict".equals(hostnameVerifier.getValue())) {
                        valid = HostnameVerifier.STRICT.verify(address, session);
                    } else if ("AllowAll".equals(hostnameVerifier.getValue())) {
                        valid = HostnameVerifier.ALLOW_ALL.verify(address, session);
                    } else if ("DefaultAndLocalhost".equals(hostnameVerifier.getValue())) {
                        valid = HostnameVerifier.DEFAULT_AND_LOCALHOST.verify(address, session);
                    }
                } else {
                    valid = HostnameVerifier.DEFAULT.verify(address, session);
                }

                if (!valid) {
                    throw new SSLException("Host name verification failed for host : " + address);    
                }
            }
        };
    }
}
