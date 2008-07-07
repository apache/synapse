package org.apache.synapse.security.keystore;


import org.apache.synapse.security.interfaces.ICACertsLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class CACertsLoader implements ICACertsLoader {

    /**
     * Constructs a keyStore from the path provided.
     *
     * @param CACertificateFilesPath - directory which contains Certificate Authority Certificates in PEM encoding.
     */
    public KeyStore loadTrustStore(String CACertificateFilesPath) {
        try {
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
        } catch (Exception e) {
            return null;
        }
    }
}
