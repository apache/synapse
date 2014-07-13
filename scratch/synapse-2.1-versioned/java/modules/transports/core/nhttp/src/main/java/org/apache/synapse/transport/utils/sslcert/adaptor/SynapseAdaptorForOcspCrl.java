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

package org.apache.synapse.transport.utils.sslcert.adaptor;

import org.apache.synapse.transport.utils.sslcert.CertificateVerificationException;
import org.apache.synapse.transport.utils.sslcert.Constants;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.security.cert.X509Certificate;

/**
 * This is the adaptor used to plug OCSP/CRL feature to synapse while solving the Jar Hell
 * problem internally using ParentLastClassLoader. This adaptor is not needed if Apache Rampart
 * and OCSP/CRL feature uses the same bouncyCastle dependency. At the time of this feature is
 * made, Apache Rampart and OCSP/CRL feature use two different versions of bouncyCastle.
 */
public class SynapseAdaptorForOcspCrl {

    private static final ClassLoader loader = new ParentLastClassLoader(
            Thread.currentThread().getContextClassLoader());

    public void verifyRevocationStatus(X509Certificate[] peerCertificates, Integer cacheSize,
                                       Integer cacheDelay) throws CertificateVerificationException {

        if (peerCertificates == null || cacheSize == null || cacheDelay == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }

        try {
            Class revocationManagerClass = loader.loadClass(Constants.REVOCATION_MANAGER);
            Method method = revocationManagerClass.getMethod(Constants.VERIFY_METHOD,
                    peerCertificates.getClass());
            Constructor constructor = revocationManagerClass.getConstructor(cacheSize.getClass(),
                    cacheDelay.getClass());
            Object revocationManager = constructor.newInstance(cacheSize, cacheDelay);
            method.invoke(revocationManager, new Object[] { peerCertificates });
        } catch (Exception e) {
            throw new CertificateVerificationException("Failed to load BouncyCastle classes for " +
                    "certificate validation", e);
        }
    }
}
