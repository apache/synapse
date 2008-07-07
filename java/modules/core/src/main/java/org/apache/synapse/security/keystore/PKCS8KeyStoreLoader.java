package org.apache.synapse.security.keystore;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.security.interfaces.IKeyStoreLoader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Constructs a KeyStore instance of type JKS from a pkcs8 private key and certificate.
 */
public class PKCS8KeyStoreLoader implements IKeyStoreLoader {

    private static Log log = LogFactory.getLog(PKCS8KeyStoreLoader.class);
    private String pkPath;
    private String certPath;
    private String keyPassword;
    private String entryAlias;

    private static final String HEADER = "-----BEGIN PRIVATE KEY-----\n";
    private static final String FOOTER = "-----END PRIVATE KEY-----";

    /**
     * constructs an instance of KeyStoreLoader
     *
     * @param pkcs8PrivateKeyPath - path to a private key file.  Key must be in PKCS8 format, PEM encoded and unencrypted.
     * @param certFilePath        - path to certificate file.  File must be PEM encoded.
     * @param keyPass             - password to secure the private key within the keystore.  This will be required later to retrieve the private key back from the keystore.
     * @param entryAlias          - alias for the given entry within the keystore.
     */
    public PKCS8KeyStoreLoader(String pkcs8PrivateKeyPath, String certFilePath, String keyPass, String entryAlias) {
        pkPath = pkcs8PrivateKeyPath;
        certPath = certFilePath;
        keyPassword = keyPass;
        this.entryAlias = entryAlias;
    }

    /**
     * returns a JKS keystore from the given private key, certificate path, key password and alias.
     */
    public KeyStore getKeyStore() {
        FileInputStream keyFile = null;
        try {
            keyFile = new FileInputStream(pkPath);
            BufferedInputStream kis = new BufferedInputStream(keyFile);
            byte[] keyBytes = new byte[kis.available()];

            kis.read(keyBytes);

            kis.close();
            keyFile.close();

            PrivateKey key = createPrivateKey(keyBytes);

            FileInputStream certificateFile = new FileInputStream(certPath);
            BufferedInputStream bis = new BufferedInputStream(certificateFile);

            CertificateFactory certFactory = CertificateFactory.getInstance("X509");

            Certificate cert = certFactory.generateCertificate(bis);

            bis.close();
            certificateFile.close();

            KeyStore newKeyStore = KeyStore.getInstance("JKS");
            newKeyStore.load(null, null);

            newKeyStore.setCertificateEntry("server Cert", cert);

            Certificate[] certChain = new Certificate[1];
            certChain[0] = cert;

            newKeyStore.setKeyEntry(entryAlias, key, keyPassword.toCharArray(), certChain);

            return newKeyStore;
        } catch (FileNotFoundException e) {
            handleException("IOError", e);
        } catch (IOException e) {
            handleException("IOError", e);
        } catch (NoSuchAlgorithmException e) {
            handleException("Error creating KeyStore", e);
        } catch (KeyStoreException e) {
            handleException("Error creating KeyStore", e);
        } catch (CertificateException e) {
            handleException("Error creating KeyStore", e);
        }
        return null;


    }


    /**
     * takes the (unencrypted) RSA private key in pkcs8 format, and creates a private key out of it
     *
     * @param keyBytes
     * @return
     */
    private PrivateKey createPrivateKey(byte[] keyBytes) {

        int dataStart = HEADER.length();
        int dataEnd = keyBytes.length - FOOTER.length() - 1;
        int dataLength = dataEnd - dataStart;
        byte[] keyContent = new byte[dataLength];

        System.arraycopy(keyBytes, dataStart, keyContent, 0, dataLength);

        PKCS8EncodedKeySpec pkcs8SpecPriv = new PKCS8EncodedKeySpec(new Base64().decode(keyContent));

        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(pkcs8SpecPriv);
        } catch (NoSuchAlgorithmException e) {
            handleException("Error getting KeyFactory instance", e);
        } catch (InvalidKeySpecException e) {
            handleException("Error generating private key", e);
        }
        return null;
    }

    protected void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    protected void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
