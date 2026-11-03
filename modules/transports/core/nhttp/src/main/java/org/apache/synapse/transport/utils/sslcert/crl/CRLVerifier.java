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

package org.apache.synapse.transport.utils.sslcert.crl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.*;
import org.apache.synapse.transport.utils.sslcert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is used to verify a certificate is revoked or not by using the Certificate Revocation
 * List published by the CA.
 */
public class CRLVerifier implements RevocationVerifier {

    private static final Log log = LogFactory.getLog(CRLVerifier.class);

    private CRLCache cache;

    /**
     * Tolerance applied to both ends of a CRL's validity window, to allow for clock skew between
     * this host and the CA that published it.
     */
    private static final long CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    /**
     * How old a CRL may be when it carries no nextUpdate, which RFC 5280 section 5.1.2.5
     * leaves optional.
     */
    private static final long MAX_CRL_AGE_MILLIS = 24 * 60 * 60 * 1000L;

    public CRLVerifier(CRLCache cache) {
        this.cache = cache;
    }

    /**
     * Checks revocation status (Good, Revoked) of the peer certificate.
     *
     * @param peerCert   peer certificate
     * @param issuerCert issuer of the peer certificate; every CRL used here is verified against it.
     * @return revocation status of the peer certificate.
     * @throws CertificateVerificationException
     *
     */
    @Override
    public RevocationStatus checkRevocationStatus(X509Certificate peerCert, X509Certificate issuerCert)
            throws CertificateVerificationException {

        List<String> list = getCrlDistributionPoints(peerCert);
        //check with distributions points in the list one by one. if one fails go to the other.
        for (String crlUrl : list) {
            if (log.isDebugEnabled()) {
                log.debug("Trying to get CRL for URL: " + crlUrl);
            }

            if (cache != null) {
                X509CRL x509CRL = cache.getCacheValue(crlUrl);
                if (x509CRL != null) {
                    //If cant be casted, we have used the wrong cache.
                    try {
                        verifyCRL(x509CRL, issuerCert);
                        RevocationStatus status = getRevocationStatus(x509CRL, peerCert);
                        log.debug("CRL taken from cache....");
                        return status;
                    } catch (CertificateVerificationException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Discarding the cached CRL for " + crlUrl +
                                    " and downloading it again.", e);
                        }
                        cache.removeCacheValue(crlUrl);
                    }
                }
            }

            //todo: Do we need to check if URL has the same domain name as issuerCert?
            try {
                X509CRL x509CRL = downloadCRLFromWeb(crlUrl);
                if (x509CRL != null) {
                    verifyCRL(x509CRL, issuerCert);
                    if (cache != null) {
                        cache.setCacheValue(crlUrl, x509CRL);
                    }
                    return getRevocationStatus(x509CRL, peerCert);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Either the url is bad or cannot build X509CRL. Check with the next " +
                            "url in the list.", e);
                }
            }
        }
        throw new CertificateVerificationException("Cannot check revocation status with the " +
                "certificate");
    }

    private RevocationStatus getRevocationStatus(X509CRL x509CRL, X509Certificate peerCert) {
        if (x509CRL.isRevoked(peerCert)) {
            return RevocationStatus.REVOKED;
        } else {
            return RevocationStatus.GOOD;
        }
    }

    /**
     * Verifies a CRL was published by the issuer of the certificate being checked and is still
     * current, rejecting forged or stale CRLs and indirect CRLs signed by a separate CRL issuer.
     *
     * @param x509CRL    the CRL that was fetched or taken from the cache.
     * @param issuerCert the issuer of the certificate being checked.
     * @throws CertificateVerificationException if the CRL is not validly signed by that issuer or
     *                                          is no longer current.
     */
    private void verifyCRL(X509CRL x509CRL, X509Certificate issuerCert)
            throws CertificateVerificationException {

        if (issuerCert == null) {
            throw new CertificateVerificationException("Cannot verify a CRL without the issuer " +
                    "certificate");
        }

        if (!x509CRL.getIssuerX500Principal().equals(issuerCert.getSubjectX500Principal())) {
            throw new CertificateVerificationException("CRL issuer " +
                    x509CRL.getIssuerX500Principal() + " is not the issuer of the certificate " +
                    "being checked");
        }

        try {
            x509CRL.verify(issuerCert.getPublicKey());
        } catch (GeneralSecurityException e) {
            throw new CertificateVerificationException("CRL is not signed by the issuer of the " +
                    "certificate being checked", e);
        }

        long now = System.currentTimeMillis();
        Date thisUpdate = x509CRL.getThisUpdate();
        Date nextUpdate = x509CRL.getNextUpdate();

        if (thisUpdate == null) {
            throw new CertificateVerificationException("CRL does not say when it was issued");
        }
        if (thisUpdate.getTime() > now + CLOCK_SKEW_MILLIS) {
            throw new CertificateVerificationException("CRL is not valid yet. It is dated " +
                    thisUpdate);
        }
        if (nextUpdate != null && nextUpdate.getTime() < now - CLOCK_SKEW_MILLIS) {
            throw new CertificateVerificationException("CRL expired on " + nextUpdate);
        } else if (nextUpdate == null && thisUpdate.getTime() < now - MAX_CRL_AGE_MILLIS) {
            throw new CertificateVerificationException("CRL carries no nextUpdate and is older " +
                    "than the accepted maximum. It is dated " + thisUpdate);
        }
    }

    /**
     * Downloads CRL from the crlUrl. Does not support HTTPS
     */
    protected X509CRL downloadCRLFromWeb(String crlURL)
            throws IOException, CertificateVerificationException {
        InputStream crlStream = null;
        try {
            URL url = new URL(crlURL);
            crlStream = url.openStream();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509CRL) cf.generateCRL(crlStream);
        } catch (MalformedURLException e) {
            throw new CertificateVerificationException("CRL Url is malformed", e);
        } catch (IOException e) {
            throw new CertificateVerificationException("Cant reach URI: " + crlURL +
                    " - only support HTTP", e);
        } catch (CertificateException e) {
            throw new CertificateVerificationException(e);
        } catch (CRLException e) {
            throw new CertificateVerificationException("Cannot generate X509CRL from the " +
                    "stream data", e);
        } finally {
            if (crlStream != null)
                crlStream.close();
        }
    }

    /**
     * Extracts all CRL distribution point URLs from the "CRL Distribution Point"
     * extension in a X.509 certificate. If CRL distribution point extension is
     * unavailable, returns an empty list.
     */
    private List<String> getCrlDistributionPoints(X509Certificate cert)
            throws CertificateVerificationException {

        //Gets the DER-encoded OCTET string for the extension value for CRLDistributionPoints
        byte[] crlDPExtensionValue = cert.getExtensionValue(
                X509Extensions.CRLDistributionPoints.getId());
        if (crlDPExtensionValue == null)
            throw new CertificateVerificationException("Certificate doesn't have CRL " +
                    "distribution points");
        //crlDPExtensionValue is encoded in ASN.1 format.
        ASN1InputStream asn1In = new ASN1InputStream(crlDPExtensionValue);
        // DER (Distinguished Encoding Rules) is one of ASN.1 encoding rules defined in ITU-T X.690,
        // 2002, specification. ASN.1 encoding rules can be used to encode any data object into a
        // binary file. Read the object in octets.
        CRLDistPoint distPoint;
        try {
            DEROctetString crlDEROctetString = (DEROctetString) asn1In.readObject();
            //Get Input stream in octets
            ASN1InputStream asn1InOctets = new ASN1InputStream(crlDEROctetString.getOctets());
            ASN1Primitive asn1Primitive = asn1InOctets.readObject();
            distPoint = CRLDistPoint.getInstance(asn1Primitive);
        } catch (IOException e) {
            throw new CertificateVerificationException("Cannot read certificate to get CRL urls", e);
        }

        List<String> crlUrls = new ArrayList<String>();
        //Loop through ASN1Encodable DistributionPoints
        for (DistributionPoint dp : distPoint.getDistributionPoints()) {
            //get ASN1Encodable DistributionPointName
            DistributionPointName dpn = dp.getDistributionPoint();
            if (dpn != null && dpn.getType() == DistributionPointName.FULL_NAME) {
                //Create ASN1Encodable General Names
                GeneralName[] genNames = GeneralNames.getInstance(dpn.getName()).getNames();
                // Look for a URI
                //todo: May be able to check for OCSP url specifically.
                for (GeneralName genName : genNames) {
                    if (genName.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        //DERIA5String contains an ascii string.
                        //A IA5String is a restricted character string type in the ASN.1 notation
                        String url = DERIA5String.getInstance(genName.getName()).getString().trim();
                        crlUrls.add(url);
                    }
                }
            }
        }

        if (crlUrls.isEmpty()) {
            throw new CertificateVerificationException("Cant get CRL urls from certificate");
        }

        return crlUrls;
    }
}
