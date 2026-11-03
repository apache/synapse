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

package org.apache.synapse.transport.utils.sslcert.ocsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.apache.synapse.transport.utils.sslcert.*;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Used to check if a Certificate is revoked or not by its CA using Online Certificate
 * Status Protocol (OCSP).
 */
public class OCSPVerifier implements RevocationVerifier {

    private static final Log log = LogFactory.getLog(OCSPVerifier.class);

    private OCSPCache cache;

    /**
     * Tolerance applied to both ends of a response's validity window. RFC 6960 section 4.2.2.1
     * requires clients to allow for clock skew between themselves and the responder.
     */
    private static final long CLOCK_SKEW_MILLIS = 5 * 60 * 1000L;

    /**
     * How old a response may be when it carries no nextUpdate, which RFC 6960 section 4.2.2.1
     * leaves optional.
     */
    private static final long MAX_RESPONSE_AGE_MILLIS = 24 * 60 * 60 * 1000L;

    public OCSPVerifier(OCSPCache cache) {
        this.cache = cache;
    }

    /**
     * Gets the revocation status (Good, Revoked or Unknown) of the given peer certificate.
     *
     * @param peerCert   The certificate we want to check if revoked.
     * @param issuerCert its issuer, paired by PathChecker; sound because PKIX validation already confirmed chain order.
     * @return revocation status of the peer certificate.
     * @throws CertificateVerificationException
     *
     */
    @Override
    public RevocationStatus checkRevocationStatus(X509Certificate peerCert, X509Certificate issuerCert)
            throws CertificateVerificationException {

        if (issuerCert == null) {
            throw new CertificateVerificationException("Cannot check revocation status without " +
                    "the issuer certificate");
        }

        CertificateID certificateID = generateCertificateID(issuerCert, peerCert.getSerialNumber());

        //check cache
        if (cache != null) {
            SingleResp resp = cache.getCacheValue(peerCert.getSerialNumber());
            if (resp != null) {
                try {
                    verifyCertificateId(certificateID, resp);
                    verifyResponseFreshness(resp);
                    //If cant be casted, we have used the wrong cache.
                    RevocationStatus status = getRevocationStatus(resp);
                    log.debug("OCSP response taken from cache....");
                    return status;
                } catch (CertificateVerificationException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Discarding the cached OCSP response for serial number " +
                                peerCert.getSerialNumber() + " and asking the responder again.", e);
                    }
                    cache.removeCacheValue(peerCert.getSerialNumber());
                }
            }
        }

        OCSPReq request = generateOCSPRequest(certificateID);
        //This list will sometimes have non ocsp urls as well.
        List<String> locations = getAIALocations(peerCert);

        for (String serviceUrl : locations) {

            SingleResp[] responses;
            try {
                OCSPResp ocspResponse = getOCSPResponse(serviceUrl, request);
                if (OCSPResp.SUCCESSFUL != ocspResponse.getStatus()) {
                    continue; // Server didn't give the response right.
                }

                BasicOCSPResp basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();
                verifyResponseSignature(basicResponse, issuerCert);
                responses = (basicResponse == null) ? null : basicResponse.getResponses();
                //todo use the super exception
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not obtain a usable OCSP response from " + serviceUrl +
                            ". Trying the next url in the list.", e);
                }
                continue;
            }

            if (responses != null && responses.length == 1) {
                SingleResp resp = responses[0];
                verifyCertificateId(certificateID, resp);
                verifyResponseFreshness(resp);
                RevocationStatus status = getRevocationStatus(resp);
                if (cache != null)
                    cache.setCacheValue(peerCert.getSerialNumber(), resp, request, serviceUrl);
                return status;
            }
        }
        throw new CertificateVerificationException("Cant get Revocation Status from OCSP.");
    }

    /**
     * Verifies the response is signed by the certificate issuer, or by a responder the issuer
     * authorised via the id-kp-OCSPSigning extended key usage (RFC 6960 section 4.2.2.2).
     *
     * @param basicResponse the response to check.
     * @param issuerCert    the issuer of the certificate whose status was requested.
     * @throws CertificateVerificationException if the response is not signed by a key the issuer
     *                                          authorised for this purpose.
     */
    private void verifyResponseSignature(BasicOCSPResp basicResponse, X509Certificate issuerCert)
            throws CertificateVerificationException {

        if (basicResponse == null) {
            throw new CertificateVerificationException("OCSP response carries no basic response " +
                    "to verify");
        }

        X509CertificateHolder issuerHolder;
        try {
            issuerHolder = new JcaX509CertificateHolder(issuerCert);
        } catch (CertificateEncodingException e) {
            throw new CertificateVerificationException("Cannot read the issuer certificate while " +
                    "verifying the OCSP response signature", e);
        }

        if (isSignedBy(basicResponse, issuerHolder)) {
            return;
        }

        X509CertificateHolder[] responderCerts = basicResponse.getCerts();
        if (responderCerts != null) {
            for (X509CertificateHolder responderCert : responderCerts) {
                if (hasOcspSigningExtendedKeyUsage(responderCert)
                        && responderCert.isValidOn(new Date())
                        && isIssuedBy(responderCert, issuerHolder)
                        && isSignedBy(basicResponse, responderCert)) {
                    return;
                }
            }
        }

        throw new CertificateVerificationException("OCSP response is not signed by the " +
                "certificate issuer or by a responder the issuer delegated to");
    }

    /**
     * @return true if the response signature verifies under the given certificate's public key.
     */
    private boolean isSignedBy(BasicOCSPResp basicResponse, X509CertificateHolder signerCert) {
        try {
            ContentVerifierProvider verifierProvider =
                    new JcaContentVerifierProviderBuilder().build(signerCert);
            return basicResponse.isSignatureValid(verifierProvider);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("OCSP response signature did not verify under " +
                        signerCert.getSubject(), e);
            }
            return false;
        }
    }

    /**
     * @return true if the certificate names the issuer as its issuer AND actually carries the
     * issuer's signature; the name alone is not sufficient.
     */
    private boolean isIssuedBy(X509CertificateHolder cert, X509CertificateHolder issuerCert) {
        try {
            if (!cert.getIssuer().equals(issuerCert.getSubject())) {
                return false;
            }
            ContentVerifierProvider verifierProvider =
                    new JcaContentVerifierProviderBuilder().build(issuerCert);
            return cert.isSignatureValid(verifierProvider);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Responder certificate " + cert.getSubject() +
                        " is not validly issued by " + issuerCert.getSubject(), e);
            }
            return false;
        }
    }

    /**
     * @return true if the certificate carries the id-kp-OCSPSigning extended key usage, which is
     * how a CA marks a certificate as authorised to answer OCSP on its behalf.
     */
    private boolean hasOcspSigningExtendedKeyUsage(X509CertificateHolder cert) {
        Extension extension = cert.getExtension(Extension.extendedKeyUsage);
        if (extension == null) {
            return false;
        }
        ExtendedKeyUsage extendedKeyUsage = ExtendedKeyUsage.getInstance(extension.getParsedValue());
        return extendedKeyUsage != null && extendedKeyUsage.hasKeyPurposeId(KeyPurposeId.id_kp_OCSPSigning);
    }

    /**
     * Verifies the response answers for the certificate actually asked about, not merely one
     * sharing its cache key.
     *
     * @throws CertificateVerificationException if the response is about a different certificate.
     */
    private void verifyCertificateId(CertificateID requestedCertificateId, SingleResp response)
            throws CertificateVerificationException {

        if (!requestedCertificateId.equals(response.getCertID())) {
            throw new CertificateVerificationException("OCSP response does not answer for the " +
                    "certificate it was requested for");
        }
    }

    /**
     * Verifies that the response is still within its validity window.
     *
     * @throws CertificateVerificationException if the response is stale or not yet valid.
     */
    private void verifyResponseFreshness(SingleResp response)
            throws CertificateVerificationException {

        long now = System.currentTimeMillis();
        Date thisUpdate = response.getThisUpdate();
        Date nextUpdate = response.getNextUpdate();

        if (thisUpdate == null) {
            throw new CertificateVerificationException("OCSP response does not say when it was " +
                    "issued");
        }
        if (thisUpdate.getTime() > now + CLOCK_SKEW_MILLIS) {
            throw new CertificateVerificationException("OCSP response is not valid yet. It is " +
                    "dated " + thisUpdate);
        }
        if (nextUpdate != null) {
            if (nextUpdate.getTime() < now - CLOCK_SKEW_MILLIS) {
                throw new CertificateVerificationException("OCSP response expired on " + nextUpdate);
            }
        } else if (thisUpdate.getTime() < now - MAX_RESPONSE_AGE_MILLIS) {
            throw new CertificateVerificationException("OCSP response carries no nextUpdate and " +
                    "is older than the accepted maximum. It is dated " + thisUpdate);
        }
    }

    private RevocationStatus getRevocationStatus(SingleResp resp) throws CertificateVerificationException {
        CertificateStatus status = resp.getCertStatus();
        if (status == CertificateStatus.GOOD) {
            return RevocationStatus.GOOD;
        } else if (status instanceof RevokedStatus) {
            return RevocationStatus.REVOKED;
        } else if (status instanceof UnknownStatus) {
            return RevocationStatus.UNKNOWN;
        }
        throw new CertificateVerificationException("Cant recognize Certificate Status");
    }

    /**
     * Gets an ASN.1 encoded OCSP response (as defined in RFC 2560) from the given service URL. Currently supports
     * only HTTP.
     *
     * @param serviceUrl URL of the OCSP endpoint.
     * @param request    an OCSP request object.
     * @return OCSP response encoded in ASN.1 structure.
     * @throws CertificateVerificationException
     *
     */
    protected OCSPResp getOCSPResponse(String serviceUrl,
                                       OCSPReq request) throws CertificateVerificationException {
        try {
            //Todo: Use http client.
            byte[] array = request.getEncoded();
            if (serviceUrl.startsWith("http")) {
                HttpURLConnection con;
                URL url = new URL(serviceUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestProperty("Content-Type", "application/ocsp-request");
                con.setRequestProperty("Accept", "application/ocsp-response");
                con.setDoOutput(true);
                OutputStream out = con.getOutputStream();
                DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
                dataOut.write(array);

                dataOut.flush();
                dataOut.close();

                //Check errors in response:
                if (con.getResponseCode() / 100 != 2) {
                    throw new CertificateVerificationException("Error getting ocsp response." +
                            "Response code is " + con.getResponseCode());
                }

                //Get Response
                InputStream in = (InputStream) con.getContent();
                return new OCSPResp(in);
            } else {
                throw new CertificateVerificationException("Only http is supported for ocsp calls");
            }
        } catch (IOException e) {
            throw new CertificateVerificationException("Cannot get ocspResponse from url: " + serviceUrl, e);
        }
    }

    /**
     * Generates an OCSP request for the given certificate identity.
     *
     * @param id the CertID of the certificate to ask about.
     * @return generated OCSP request.
     * @throws CertificateVerificationException
     */
    private OCSPReq generateOCSPRequest(CertificateID id)
            throws CertificateVerificationException {

        //TODO: Have to check if this is OK with synapse implementation.
        //Add provider BC
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try {
            // basic request generation with nonce
            OCSPReqBuilder builder = new OCSPReqBuilder();
            builder.addRequest(id);

            // create details for nonce extension. The nonce extension is used to bind
            // a request to a response to prevent replay attacks. As the name implies,
            // the nonce value is something that the client should only use once within a reasonably
            // small period.
            BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
            Extension ext = new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, new DEROctetString(new DEROctetString(nonce.toByteArray()).getEncoded()));
            builder.setRequestExtensions(new Extensions(new Extension[]{ext}));
        
            return builder.build();
        } catch (OCSPException | IOException e) {
            throw new CertificateVerificationException("Cannot generate OCSP Request with the " +
                    "given certificate", e);
        }
    }

    /**
     * Builds the CertID (RFC 2560) identifying the certificate being asked about, usable for
     * both a fresh request and a comparison against a cached response.
     *
     * @param issuerCert   is the Certificate of the Issuer of the peer certificate we are interested in.
     * @param serialNumber of the peer certificate.
     * @return the CertID for that certificate.
     * @throws CertificateVerificationException
     */
    private CertificateID generateCertificateID(X509Certificate issuerCert, BigInteger serialNumber)
            throws CertificateVerificationException {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try {
            //  CertID structure is used to uniquely identify certificates that are the subject of
            // an OCSP request or response and has an ASN.1 definition. CertID structure is defined
            // in RFC 2560
            DigestCalculator digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
            return new CertificateID(digestCalculator, new JcaX509CertificateHolder(issuerCert), serialNumber);
        } catch (OCSPException | OperatorCreationException | CertificateEncodingException e) {
            throw new CertificateVerificationException("Cannot identify the certificate to be " +
                    "checked with OCSP", e);
        }
    }

    /**
     * Authority Information Access (AIA) is a non-critical extension in an X509 Certificate. This contains the
     * URL of the OCSP endpoint if one is available.
     * TODO: This might contain non OCSP urls as well. Handle this.
     *
     * @param cert is the certificate
     * @return a lit of URLs in AIA extension of the certificate which will hopefully contain an OCSP endpoint.
     * @throws CertificateVerificationException
     *
     */
    private List<String> getAIALocations(X509Certificate cert) throws CertificateVerificationException {

        //Gets the DER-encoded OCTET string for the extension value for Authority information access Points
        byte[] aiaExtensionValue = cert.getExtensionValue(X509Extensions.AuthorityInfoAccess.getId());
        if (aiaExtensionValue == null) {
            throw new CertificateVerificationException("Certificate doesn't have authority " +
                    "information access points");
        }
        //might have to pass an ByteArrayInputStream(aiaExtensionValue)
        ASN1InputStream asn1In = new ASN1InputStream(aiaExtensionValue);
        AuthorityInformationAccess authorityInformationAccess;

        try {
            DEROctetString aiaDEROctetString = (DEROctetString) (asn1In.readObject());
            ASN1InputStream asn1InOctets = new ASN1InputStream(aiaDEROctetString.getOctets());
            ASN1Sequence aiaASN1Sequence = (ASN1Sequence) asn1InOctets.readObject();
            authorityInformationAccess = AuthorityInformationAccess.getInstance(aiaASN1Sequence);
        } catch (IOException e) {
            throw new CertificateVerificationException("Cannot read certificate to get OCSP URLs", e);
        }

        List<String> ocspUrlList = new ArrayList<String>();
        AccessDescription[] accessDescriptions = authorityInformationAccess.getAccessDescriptions();
        for (AccessDescription accessDescription : accessDescriptions) {

            GeneralName gn = accessDescription.getAccessLocation();
            if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                DERIA5String str = DERIA5String.getInstance(gn.getName());
                String accessLocation = str.getString();
                ocspUrlList.add(accessLocation);
            }
        }
        if (ocspUrlList.isEmpty()) {
            throw new CertificateVerificationException("Cant get OCSP urls from certificate");
        }

        return ocspUrlList;
    }

}
