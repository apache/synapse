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

package org.apache.synapse.transport.passthru;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.ssl.SSLSetupHandler;
import org.apache.synapse.transport.utils.conn.logging.LoggingUtils;
import org.apache.synapse.transport.utils.sslcert.CertificateVerificationConfig;
import org.apache.synapse.transport.utils.sslcert.CertificateVerificationException;
import org.apache.synapse.transport.utils.sslcert.adaptor.SynapseAdaptorForOcspCrl;

import javax.net.ssl.*;
import javax.xml.namespace.QName;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PassThroughHttpSSLSender extends PassThroughHttpSender {

    private static final SynapseAdaptorForOcspCrl ocspCrl = new SynapseAdaptorForOcspCrl();

    protected IOEventDispatch getEventDispatch(NHttpClientEventHandler handler,
                                               SSLContext sslContext,
                                               SSLSetupHandler sslSetupHandler,
                                               ConnectionConfig config,
                                               TransportOutDescription transportOut) throws AxisFault {

        return LoggingUtils.getClientIODispatch(handler, config, sslContext, sslSetupHandler,
                getCustomSSLContexts(transportOut));
    }

    /**
     * Create the SSLContext to be used by this sender
     *
     * @param transportOut the Axis2 transport configuration
     * @return the SSLContext to be used
     * @throws org.apache.axis2.AxisFault if an error occurs
     */
    protected SSLContext getSSLContext(TransportOutDescription transportOut) throws AxisFault {

        Parameter keyParam    = transportOut.getParameter("keystore");
        Parameter trustParam  = transportOut.getParameter("truststore");

        OMElement ksEle = null;
        OMElement tsEle = null;

        if (keyParam != null) {
            ksEle = keyParam.getParameterElement().getFirstElement();
        }

        boolean noValidateCert = ParamUtils.getOptionalParamBoolean(transportOut,
                "novalidatecert", false);

        if (trustParam != null) {
            if (noValidateCert) {
                log.warn("Ignoring novalidatecert parameter since a truststore has been specified");
            }
            tsEle = trustParam.getParameterElement().getFirstElement();
        }

        return createSSLContext(ksEle, tsEle, noValidateCert);
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
    protected SSLSetupHandler getSSLSetupHandler(TransportOutDescription transportOut)
            throws AxisFault {

        Parameter hostnameVerifier = transportOut.getParameter("HostnameVerifier");
        String hostnameVerifierValue = hostnameVerifier != null ?
                hostnameVerifier.getValue().toString() : null;
        Parameter revocationVerifierParam = transportOut.getParameter("CertificateRevocationVerifier");
        CertificateVerificationConfig cvConfig = revocationVerifierParam != null ?
                new CertificateVerificationConfig(revocationVerifierParam) : null;
        return createSSLSetupHandler(hostnameVerifierValue, cvConfig);
    }

    /**
     * Looks for a transport parameter named customSSLProfiles and initializes zero or more
     * custom SSLContext instances. The syntax for defining custom SSL profiles is as follows.
     *
     * <parameter name="customSSLProfiles>
     *      <profile>
     *          <servers>www.test.org:80, www.test2.com:9763</servers>
     *          <KeyStore>
     *              <Location>/path/to/identity/store</Location>
     *              <Type>JKS</Type>
     *              <Password>password</Password>
     *              <KeyPassword>password</KeyPassword>
     *          </KeyStore>
     *          <TrustStore>
     *              <Location>path/tp/trust/store</Location>
     *              <Type>JKS</Type>
     *              <Password>password</Password>
     *          </TrustStore>
     *      </profile>
     * </parameter>
     *
     * Any number of profiles can be defined under the customSSLProfiles parameter.
     *
     * @param transportOut transport out description
     * @return a map of server addresses and SSL contexts
     * @throws AxisFault if at least on SSL profile is not properly configured
     */
    private Map<String, SSLContext> getCustomSSLContexts(TransportOutDescription transportOut)
            throws AxisFault {

        if (log.isDebugEnabled()) {
            log.info("Loading custom SSL profiles for the HTTPS sender");
        }

        Parameter customProfilesParam = transportOut.getParameter("customSSLProfiles");
        if (customProfilesParam == null) {
            return null;
        }

        OMElement customProfilesElt = customProfilesParam.getParameterElement();
        Iterator profiles = customProfilesElt.getChildrenWithName(new QName("profile"));
        Map<String, SSLContext> contextMap = new HashMap<String, SSLContext>();
        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement serversElt = profile.getFirstChildWithName(new QName("servers"));
            if (serversElt == null || serversElt.getText() == null) {
                String msg = "Each custom SSL profile must define at least one host:port " +
                        "pair under the servers element";
                log.error(msg);
                throw new AxisFault(msg);
            }

            String[] servers = serversElt.getText().split(",");
            OMElement ksElt = profile.getFirstChildWithName(new QName("KeyStore"));
            OMElement trElt = profile.getFirstChildWithName(new QName("TrustStore"));
            String noValCert = profile.getAttributeValue(new QName("novalidatecert"));
            boolean novalidatecert = "true".equals(noValCert);
            SSLContext sslContext = createSSLContext(ksElt, trElt, novalidatecert);

            for (String server : servers) {
                server = server.trim();
                if (!contextMap.containsKey(server)) {
                    contextMap.put(server, sslContext);
                } else {
                    log.warn("Multiple SSL profiles were found for the server : " + server + ". " +
                            "Ignoring the excessive profiles.");
                }
            }
        }

        if (contextMap.size() > 0) {
            log.info("Custom SSL profiles initialized for " + contextMap.size() + " servers");
            return contextMap;
        }
        return null;
    }

    private SSLContext createSSLContext(OMElement keyStoreElt, OMElement trustStoreElt,
                                        boolean novalidatecert) throws AxisFault {

        KeyManager[] keymanagers  = null;
        TrustManager[] trustManagers = null;


        if (keyStoreElt != null) {
            String location      = keyStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type          = keyStoreElt.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = keyStoreElt.getFirstChildWithName(new QName("Password")).getText();
            String keyPassword   = keyStoreElt.getFirstChildWithName(new QName("KeyPassword")).getText();

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
                log.error("Error loading Keystore : " + location, gse);
                throw new AxisFault("Error loading Keystore : " + location, gse);
            } catch (IOException ioe) {
                log.error("Error opening Keystore : " + location, ioe);
                throw new AxisFault("Error opening Keystore : " + location, ioe);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
            }
        }

        if (trustStoreElt != null) {
            if (novalidatecert) {
                log.warn("Ignoring novalidatecert parameter since a truststore has been specified");
            }

            String location      = trustStoreElt.getFirstChildWithName(new QName("Location")).getText();
            String type          = trustStoreElt.getFirstChildWithName(new QName("Type")).getText();
            String storePassword = trustStoreElt.getFirstChildWithName(new QName("Password")).getText();

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
        } else if (novalidatecert) {
            log.warn("Server certificate validation (trust) has been disabled. " +
                    "DO NOT USE IN PRODUCTION!");
            trustManagers = new TrustManager[] { new NoValidateCertTrustManager() };
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

    private SSLSetupHandler createSSLSetupHandler(final String hostnameVerifier,
                                                  final CertificateVerificationConfig cvConfig) throws AxisFault {

        return new SSLSetupHandler() {

            public void initalize(SSLEngine sslengine) {
            }

            public void verify(IOSession ioSession, SSLSession session) throws SSLException {
                SocketAddress remoteAddress = ioSession.getRemoteAddress();
                String address;
                if (remoteAddress instanceof InetSocketAddress) {
                    address = ((InetSocketAddress) remoteAddress).getHostName();
                } else {
                    address = remoteAddress.toString();
                }

                boolean valid = false;
                //Do HostName verification.
                if (hostnameVerifier != null) {
                    if ("Strict".equals(hostnameVerifier)) {
                        valid = HostnameVerifier.STRICT.verify(address, session);
                    } else if ("AllowAll".equals(hostnameVerifier)) {
                        valid = HostnameVerifier.ALLOW_ALL.verify(address, session);
                    } else if ("DefaultAndLocalhost".equals(hostnameVerifier)) {
                        valid = HostnameVerifier.DEFAULT_AND_LOCALHOST.verify(address, session);
                    }
                } else {
                    valid = HostnameVerifier.DEFAULT.verify(address, session);
                }

                if (!valid) {
                    throw new SSLException("Host name verification failed for host : " + address);
                }

                if (cvConfig != null) {
                    //Do revocation verification of Certificates
                    try {
                        ocspCrl.verifyRevocationStatus(session.getPeerCertificateChain(),
                                cvConfig.getCacheSize(), cvConfig.getCacheDuration());
                    } catch (CertificateVerificationException e) {
                        throw new SSLException("Certificate chain validation failed for host : " +
                                address, e);
                    }
                }
            }
        };
    }

    /**
     * Trust manager accepting any certificate.
     */
    public static class NoValidateCertTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // Do nothing: we accept any certificate
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // Do nothing: we accept any certificate
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
