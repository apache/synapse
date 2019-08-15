/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.securevault.keystore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.securevault.ICACertsLoader;
import org.apache.synapse.securevault.SecureVaultException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * Constructs a keyStore from CA certificates
 */
public class CACertsLoader implements ICACertsLoader {

    private static Log log = LogFactory.getLog(CACertsLoader.class);

    /**
     * Constructs a keyStore from the path provided.
     *
     * @param CACertificateFilesPath - directory which contains Certificate Authority
     *                               Certificates in PEM encoding.
     */
    public KeyStore loadTrustStore(String CACertificateFilesPath) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("Creating KeyStore from given CA certificates" +
                        " in the given directory : " + CACertificateFilesPath);
            }

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null, null);

            File certsPath = new File(CACertificateFilesPath);

            File[] certs = certsPath.listFiles();

            for (File currentCert : certs) {
                FileInputStream inStream = new FileInputStream(currentCert);
                BufferedInputStream bis = new BufferedInputStream(inStream);

                CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                Certificate cert = certFactory.generateCertificate(bis);

                trustStore.setCertificateEntry(currentCert.getName(), cert);

                bis.close();
                inStream.close();
            }

            return trustStore;
        } catch (IOException e) {
            handleException("IOError when reading certificates from " +
                    "directory : " + CACertificateFilesPath, e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Error creating a KeyStore", e);
        } catch (KeyStoreException e) {
            handleException("Error creating a KeyStore", e);
        } catch (CertificateException e) {
            handleException("Error creating a KeyStore", e);
        }
        return null;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SecureVaultException(msg, e);
    }
}
