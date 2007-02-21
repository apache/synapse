package org.apache.axis2.transport.nhttp;

import org.apache.axis2.description.Parameter;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.nio.reactor.SSLServerIOEventDispatch;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManager;
import javax.xml.namespace.QName;
import java.security.KeyStore;
import java.security.GeneralSecurityException;
import java.net.URL;
import java.io.IOException;

public class HttpCoreNIOSSLListener extends HttpCoreNIOListener {

    private static final Log log = LogFactory.getLog(HttpCoreNIOSSLListener.class);

    protected IOEventDispatch getEventDispatch(
        NHttpServiceHandler handler, SSLContext sslContext, HttpParams params) {
        return new SSLServerIOEventDispatch(handler,  sslContext, params);
    }

    /**
     * Create the SSLContext to be used by this listener
     * @param ksParam the Axis2 Parameter that specifies the ksParam info
     * @return the SSLContext to be used
     */
    protected SSLContext getSSLContext(Parameter ksParam) throws AxisFault {
        OMElement ksEle = ksParam.getParameterElement().getFirstElement();
        String ksLocation     = ksEle.getFirstChildWithName(new QName("Location")).getText();
        String ksType         = ksEle.getFirstChildWithName(new QName("Type")).getText();
        String ksPassword     = ksEle.getFirstChildWithName(new QName("Password")).getText();
        String pvtKeyPassword = ksEle.getFirstChildWithName(new QName("KeyPassword")).getText();

        KeyStore keystore  = null;
        try {
            keystore = KeyStore.getInstance(ksType);
            URL url = getClass().getClassLoader().getResource(ksLocation);
            log.debug("keystore loaded from url : " + url);
            keystore.load(url.openStream(), ksPassword.toCharArray());
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
            kmfactory.init(keystore, pvtKeyPassword.toCharArray());
            KeyManager[] keymanagers = kmfactory.getKeyManagers();
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(keymanagers, null, null);
            return sslcontext;
        } catch (GeneralSecurityException gse) {
            log.error("Unable to create SSL context with the given configuration", gse);
            throw new AxisFault("Unable to create SSL context with the given configuration", gse);
        } catch (IOException ioe) {
            log.error("Unable to open keystore", ioe);
            throw new AxisFault("Unable to open keystore", ioe);
        }
    }

}
