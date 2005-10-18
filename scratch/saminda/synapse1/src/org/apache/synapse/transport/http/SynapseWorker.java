package org.apache.synapse.transport.http;

import org.apache.axis2.transport.http.server.HttpRequestHandler;
import org.apache.axis2.transport.http.server.SimpleHttpServerConnection;
import org.apache.axis2.transport.http.server.SimpleRequest;
import org.apache.axis2.transport.http.server.SimpleResponse;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.SimpleHTTPOutTransportInfo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.i18n.Messages;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.Header;
import org.apache.synapse.Utils.SynapseTranportUtils;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 1:37:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseWorker implements HttpRequestHandler {
    protected Log log = LogFactory.getLog(getClass().getName());
    private ConfigurationContext configurationContext;


    public SynapseWorker(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    public boolean processRequest(final SimpleHttpServerConnection conn, final SimpleRequest request) throws IOException {
        MessageContext msgContext = null;
        SimpleResponse response = new SimpleResponse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            if (configurationContext == null) {
                throw new AxisFault(Messages.getMessage("cannotBeNullConfigurationContext"));
            }

            InputStream inStream = request.getBody();

            TransportOutDescription transportOut =
                    configurationContext.getAxisConfiguration().getTransportOut(
                            new QName(Constants.TRANSPORT_HTTP));
            msgContext =
                    new MessageContext(
                            configurationContext,
                            configurationContext.getAxisConfiguration().getTransportIn(
                                    new QName(Constants.TRANSPORT_HTTP)),
                            transportOut);
            msgContext.setServerSide(true);

            HttpVersion ver = request.getRequestLine().getHttpVersion();
            if (ver == null) {
                throw new AxisFault("HTTP version can not be Null");
            }
            String httpVersion = null;
            if (HttpVersion.HTTP_1_0.equals(ver)) {
                httpVersion = HTTPConstants.HEADER_PROTOCOL_10;
            } else if (HttpVersion.HTTP_1_1.equals(ver)) {
                httpVersion = HTTPConstants.HEADER_PROTOCOL_11;
                /**
                 * Transport Sender configuration via axis2.xml
                 */
                this.transportOutConfiguration(configurationContext,response);
            } else {
                throw new AxisFault("Unknown supported protocol version " + ver);
            }


            msgContext.setProperty(MessageContext.TRANSPORT_OUT, baos);

            //set the transport Headers
            msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, getHeaders(request));
            msgContext.setServiceGroupContextId(UUIDGenerator.getUUID());

            //This is way to provide Accsess to the transport information to the transport Sender
            msgContext.setProperty(
                    HTTPConstants.HTTPOutTransportInfo,
                    new SimpleHTTPOutTransportInfo(response));

            String soapAction = null;
            if (request.getFirstHeader(HTTPConstants.HEADER_SOAP_ACTION) != null) {
                soapAction = request.getFirstHeader(HTTPConstants.HEADER_SOAP_ACTION).getValue();
            }
            if (HTTPConstants.HEADER_GET.equals(request.getRequestLine().getMethod())) {
                //It is GET handle the Get request
//
            } else {
                ByteArrayOutputStream baosIn = new ByteArrayOutputStream();
                byte[] bytes = new byte[8192];
                int size = 0;
                while ((size = inStream.read(bytes)) > 0) {
                    baosIn.write(bytes, 0, size);
                }
                inStream = new ByteArrayInputStream(baosIn.toByteArray());

                //It is POST, handle it
                SynapseTranportUtils.processHTTPPostRequest(
                        msgContext,
                        inStream,
                        baos,
                        request.getContentType(),
                        soapAction,
                        request.getRequestLine().getUri(),
                        configurationContext);
            }

            Object contextWritten = msgContext.getOperationContext().getProperty(Constants.RESPONSE_WRITTEN);
            if (contextWritten != null &&
                    Constants.VALUE_TRUE.equals(contextWritten)) {
                response.setStatusLine(
                        request.getRequestLine().getHttpVersion(), 200, "OK");
            }
            response.setBody(new ByteArrayInputStream(baos.toByteArray()));
            setResponseHeaders(conn, request, response,
                    baos.toByteArray().length);
            conn.writeResponse(response);
        } catch (Throwable e) {
            try {
                AxisEngine engine = new AxisEngine(configurationContext);
                if (msgContext != null) {
                    msgContext.setProperty(MessageContext.TRANSPORT_OUT, baos);
                    MessageContext faultContext = engine.createFaultMessageContext(msgContext, e);
                    response.setStatusLine(request.getRequestLine().getHttpVersion(), 500, "Internal server error");
                    engine.sendFault(faultContext);
                    response.setBody(new ByteArrayInputStream(baos.toByteArray()));
                    setResponseHeaders(conn, request, response,baos.toByteArray().length);
                    conn.writeResponse(response);
                } else {
                    log.error(e, e);
                }
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
            log.error(e.getMessage(), e);
        }
        return true;
    }

    private void setResponseHeaders(final SimpleHttpServerConnection conn, SimpleRequest request, SimpleResponse response, long contentLength) {
        if (!response.containsHeader("Connection")) {
            // See if the the client explicitly handles connection persistence
            Header connheader = request.getFirstHeader("Connection");
            if (connheader != null) {
                if (connheader.getValue().equalsIgnoreCase("keep-alive")) {
                    Header header = new Header("Connection", "keep-alive");
                    response.addHeader(header);
                    conn.setKeepAlive(true);
                }
                if (connheader.getValue().equalsIgnoreCase("close")) {
                    Header header = new Header("Connection", "close");
                    response.addHeader(header);
                    conn.setKeepAlive(false);
                }
            } else {
                // Use protocol default connection policy
                if (response.getHttpVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
                    conn.setKeepAlive(true);
                } else {
                    conn.setKeepAlive(false);
                }
            }
        }
        if (!response.containsHeader("Transfer-Encoding")) {
            if (contentLength != 0) {
                Header header = new Header("Content-Length",
                        String.valueOf(contentLength));
                response.addHeader(header);
            }
        }
    }

    private Map getHeaders(SimpleRequest request) {
        HashMap headerMap = new HashMap();
        Header[] headers = request.getHeaders();
        for (int i = 0; i < headers.length; i++) {
            headerMap.put(headers[i].getName(), headers[i].getValue());
        }
        return headerMap;
    }


    /**
     *   Simple Axis Transport Selection via deployment
     * @param configContext
     * @param response
     *
     */

    private void transportOutConfiguration(ConfigurationContext configContext, SimpleResponse response) {
        AxisConfiguration axisConf = configContext.getAxisConfiguration();
        HashMap trasportOuts = axisConf.getTransportsOut();
        Iterator values = trasportOuts.values().iterator();

        String httpVersion = HTTPConstants.HEADER_PROTOCOL_11;
        while (values.hasNext()) {
            TransportOutDescription transportOut = (TransportOutDescription) values.next();
            // reading axis2.xml for transport senders..
            Parameter version =
                    transportOut.getParameter(HTTPConstants.PROTOCOL_VERSION);
            if (version != null) {
                if (HTTPConstants.HEADER_PROTOCOL_11.equals(version.getValue())) {
                    httpVersion = HTTPConstants.HEADER_PROTOCOL_11;
                    Parameter transferEncoding =
                            transportOut.getParameter(HTTPConstants.HEADER_TRANSFER_ENCODING);
                    if (transferEncoding != null){
                        if (HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED.equals(transferEncoding.getValue())) {
                            response.setHeader(new Header(HTTPConstants.HEADER_TRANSFER_ENCODING,
                                    HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED));
                        }
                    } else {
                        continue;
                    }
                } else {
                    if (HTTPConstants.HEADER_PROTOCOL_10.equals(version.getValue())) {
                        httpVersion = HTTPConstants.HEADER_PROTOCOL_10;
                    }
                }
            }

        }
    }
}
