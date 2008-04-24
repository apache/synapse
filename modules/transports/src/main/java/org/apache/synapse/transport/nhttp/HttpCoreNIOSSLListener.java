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

import java.io.IOException;
import java.io.FileInputStream;
import java.net.SocketAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.reactor.SSLIOSessionHandler;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.HttpParams;

public class HttpCoreNIOSSLListener extends HttpCoreNIOListener {

    private static final Log log = LogFactory.getLog(HttpCoreNIOSSLListener.class);

    protected IOEventDispatch getEventDispatch(
        NHttpServiceHandler handler, SSLContext sslContext,
        SSLIOSessionHandler sslIOSessionHandler, HttpParams params) {
        return new SSLServerIOEventDispatch(handler, sslContext, sslIOSessionHandler, params);
    }

    /**
     * Return the EPR prefix for services made available over this transport
     * @return
     */
    protected String getServiceEPRPrefix(ConfigurationContext cfgCtx, String host, int port) {
        return "https://" + host + (port == 443 ? "" : ":" + port) +
            (!cfgCtx.getServiceContextPath().startsWith("/") ? "/" : "") +
            cfgCtx.getServiceContextPath() +
            (!cfgCtx.getServiceContextPath().endsWith("/") ? "/" : "");
    }


    /**
     * Create the SSLContext to be used by this listener
     * @param transportIn the Axis2 transport description
     * @return the SSLContext to be used
     */
    protected SSLContext getSSLContext(TransportInDescription transportIn) throws AxisFault {

        KeyManager[] keymanagers  = null;
        TrustManager[] trustManagers = null;

        Parameter keyParam    = transportIn.getParameter("keystore");
        Parameter trustParam  = transportIn.getParameter("truststore");

        if (keyParam != null) {
            OMElement ksEle      = keyParam.getParameterElement().getFirstElement();
            String location      = ksEle.getFirstChildWithName(new QName("Location")).getText();
            String type          = ksEle.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = ksEle.getFirstChildWithName(new QName("Password")).getText();
            String keyPassword   = ksEle.getFirstChildWithName(new QName("KeyPassword")).getText();

            FileInputStream fis = null;
            try {
                KeyStore keyStore = KeyStore.getInstance(type);
                fis = new FileInputStream(location);
                log.info("Loading Identity Keystore from : " + location);

                keyStore.load(fis, storePassword.toCharArray());
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
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
            }
        }

        if (trustParam != null) {
            OMElement tsEle      = trustParam.getParameterElement().getFirstElement();
            String location      = tsEle.getFirstChildWithName(new QName("Location")).getText();
            String type          = tsEle.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = tsEle.getFirstChildWithName(new QName("Password")).getText();

            FileInputStream fis = null;
            try {
                KeyStore trustStore = KeyStore.getInstance(type);
                fis = new FileInputStream(location);
                log.info("Loading Trust Keystore from : " + location);

                trustStore.load(fis, storePassword.toCharArray());
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
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
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
     * Create the SSLIOSessionHandler to initialize the SSL session / engine, and request for
     * client authentication at the following levels, through an Axis2 transport configuration
     * parameter as follows:
     * SSLVerifyClient - none, optional, require
     *
     * @param transportIn the Axis2 transport configuration
     * @return the SSLIOSessionHandler to be used
     * @throws AxisFault if a configuration error occurs
     */
    protected SSLIOSessionHandler getSSLIOSessionHandler(TransportInDescription transportIn) throws AxisFault {

        final Parameter clientAuth = transportIn.getParameter("SSLVerifyClient");

        return new SSLIOSessionHandler() {

            public void initalize(SSLEngine sslengine, HttpParams params) {
                if (clientAuth != null) {
                    if ("optional".equals(clientAuth.getValue())) {
                        sslengine.setWantClientAuth(true);
                    } else if ("require".equals(clientAuth.getValue())) {
                        sslengine.setNeedClientAuth(true);
                    }
                }
            }

            public void verify(SocketAddress removeAddress, SSLSession session)
                throws SSLException {}
        };
    }
}
