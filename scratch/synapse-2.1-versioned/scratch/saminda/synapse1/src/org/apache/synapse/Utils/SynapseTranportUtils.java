package org.apache.synapse.Utils;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.attachments.MIMEHelper;
import org.apache.axis2.util.Utils;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.om.impl.llom.builder.StAXBuilder;
import org.apache.axis2.om.impl.llom.builder.StAXOMBuilder;
import org.apache.axis2.om.impl.llom.OMNamespaceImpl;
import org.apache.axis2.om.impl.llom.mtom.MTOMStAXSOAPModelBuilder;
import org.apache.axis2.om.impl.MTOMConstants;
import org.apache.axis2.om.*;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAP12Constants;
import org.apache.axis2.soap.SOAP11Constants;
import org.apache.axis2.soap.SOAPFactory;
import org.apache.axis2.soap.impl.llom.builder.StAXSOAPModelBuilder;
import org.apache.axis2.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.soap.impl.llom.SOAPProcessingException;
import org.apache.axis2.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.engine.SynapseEngine;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.parsers.FactoryConfigurationError;
import java.io.*;
import java.util.Map;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 1:40:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseTranportUtils {

    public static void processHTTPPostRequest(
        MessageContext msgContext,
        InputStream in,
        OutputStream out,
        String contentType,
        String soapActionHeader,
        String requestURI,
        ConfigurationContext configurationContext)
        throws AxisFault {
        boolean soap11 = false;
        try {

            //remove the starting and trailing " from the SOAP Action
            if (soapActionHeader != null
                && soapActionHeader.startsWith("\"")
                && soapActionHeader.endsWith("\"")) {

                soapActionHeader =
                    soapActionHeader.substring(
                        1,
                        soapActionHeader.length() - 1);
            }
            //fill up the Message Contexts
            msgContext.setWSAAction(soapActionHeader);
            msgContext.setSoapAction(soapActionHeader);
            msgContext.setTo(new EndpointReference(requestURI));
            msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
            msgContext.setServerSide(true);

            SOAPEnvelope envelope = null;
            StAXBuilder builder = null;
            if (contentType != null) {
                if (contentType
                    .indexOf(HTTPConstants.HEADER_ACCEPT_MULTIPART_RELATED)
                    > -1) {
                    //It is MTOM
                    builder = selectBuilderForMIME(msgContext, in, contentType);
                    envelope = (SOAPEnvelope) builder.getDocumentElement();
                } else {
                    Reader reader = new InputStreamReader(in);

                    XMLStreamReader xmlreader;
                    //Figure out the char set encoding and create the reader

                    //If charset is not specified
                    if ( getCharSetEncoding(contentType) == null ) {
                        xmlreader =
                            XMLInputFactory
                                .newInstance()
                                .createXMLStreamReader(
                                in,
                                MessageContext.DEFAULT_CHAR_SET_ENCODING);
                        //Set the encoding scheme in the message context
                        msgContext.setProperty(
                            MessageContext.CHARACTER_SET_ENCODING,
                            MessageContext.DEFAULT_CHAR_SET_ENCODING);
                    } else {
                        //get the type of char encoding
                        String charSetEnc = getCharSetEncoding(contentType);
                        xmlreader =
                            XMLInputFactory
                                .newInstance()
                                .createXMLStreamReader(
                                in,
                                charSetEnc);

                        //Setting the value in msgCtx
                        msgContext.setProperty(
                            MessageContext.CHARACTER_SET_ENCODING,
                            charSetEnc);

                    }
                    if (contentType
                        .indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE)
                        > -1) {
                        soap11 = false;
                        //it is SOAP 1.2
                        builder =
                            new StAXSOAPModelBuilder(
                                xmlreader,
                                SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI);
                        envelope = (SOAPEnvelope) builder.getDocumentElement();
                    } else if (
                        contentType.indexOf(
                            SOAP11Constants.SOAP_11_CONTENT_TYPE)
                            > -1) {
                        soap11 = true;
                        //it is SOAP 1.1

//                            msgContext.getProperty(
//                                Constants.Configuration.ENABLE_REST);
                        /**
                         * Configuration via Deployment
                         */

                        Parameter enable  = msgContext.getParameter(Constants.Configuration.ENABLE_REST);

                        if ((soapActionHeader == null
                                || soapActionHeader.length() == 0)
                                && enable != null) {
                            if (Constants.VALUE_TRUE
                                    .equals(enable.getValue())) {
                                //If the content Type is text/xml (BTW which is the SOAP 1.1 Content type ) and
                                //the SOAP Action is absent it is rest !!
                                msgContext.setDoingREST(true);

                                SOAPFactory soapFactory = new SOAP11Factory();
                                builder = new StAXOMBuilder(xmlreader);
                                builder.setOmbuilderFactory(soapFactory);
                                envelope = soapFactory.getDefaultEnvelope();
                                envelope.getBody().addChild(
                                        builder.getDocumentElement());
                            }
                        } else {
                            builder =
                                new StAXSOAPModelBuilder(
                                    xmlreader,
                                    SOAP11Constants
                                        .SOAP_ENVELOPE_NAMESPACE_URI);
                            envelope =
                                (SOAPEnvelope) builder.getDocumentElement();
                        }
                    }

                }

            }

            String charsetEncoding = builder.getDocument().getCharsetEncoding();
            if(charsetEncoding != null && !"".equals(charsetEncoding) &&
                    !((String)msgContext.getProperty(MessageContext.CHARACTER_SET_ENCODING))
                            .equalsIgnoreCase(charsetEncoding)){
                String faultCode;
                if(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(envelope.getNamespace().getName())){
                   faultCode = SOAP12Constants.FAULT_CODE_SENDER;
                }else {
                    faultCode = SOAP11Constants.FAULT_CODE_SENDER;
                }
                throw new AxisFault("Character Set Encoding from " +
                        "transport information do not match with " +
                        "character set encoding in the received SOAP message", faultCode);
            }

            /**
             * setting the postinterface...
             */
            msgContext.setEnvelope(envelope);
            AxisEngine axisEngine = new AxisEngine(configurationContext);
            SynapseEngine synapseEngine = new SynapseEngine();
            /**
             * inversion of control
             */
            synapseEngine.setAxisEngine(axisEngine);
            /**
             * call for the SynapseEngine, which is the wrapper of AxisEngine
             * Thus, making Synapse to wrap the Axis2
             */

            synapseEngine.excecuite(msgContext);

        } catch (SOAPProcessingException e) {
            throw new AxisFault(e);

        } catch (AxisFault e) {
            //rethrow
            throw e;
        } catch (OMException e) {
            throw new AxisFault(e);
        } catch (XMLStreamException e) {
            throw new AxisFault(e);
        } catch (FactoryConfigurationError e) {
            throw new AxisFault(e);
        } catch (UnsupportedEncodingException e) {
            throw new AxisFault(e);
        } finally {
            if (msgContext.getEnvelope() == null && !soap11) {
                msgContext.setEnvelope(
                    new SOAP12Factory().createSOAPEnvelope());
            }

        }
    }

    /**
     * Extracts and returns the character set encoding from the
     * Content-type header
     * Example:
     * Content-Type: text/xml; charset=utf-8
     * @param contentType
     */
    private static String getCharSetEncoding(String contentType) {
        int index = contentType.indexOf(HTTPConstants.CHAR_SET_ENCODING);
        if(index == -1) { //Charset encoding not found in the contect-type header
            //Using the default UTF-8
            return MessageContext.DEFAULT_CHAR_SET_ENCODING;
        }

        //If there are spaces around the '=' sign
        int indexOfEq = contentType.indexOf("=", index);
        //There can be situations where "charset" is not the last parameter of the Content-Type header
        int indexOfSemiColon = contentType.indexOf(";", indexOfEq);
        String value;
        if (indexOfSemiColon > 0) {
            value = (contentType.substring(indexOfEq + 1, indexOfSemiColon));
        } else {
            value = (contentType.substring(indexOfEq + 1, contentType.length()))
                    .trim();
        }

        //There might be "" around the value - if so remove them
        value = value.replaceAll("\"", "");

        if("null".equalsIgnoreCase(value)){
            return null;
        }

        return value.trim();

    }


    public static SOAPEnvelope createEnvelopeFromGetRequest(
        String requestUrl,
        Map map) {
        String[] values =
            Utils.parseRequestURLForServiceAndOperation(requestUrl);

        if (values[1] != null && values[0] != null) {
            String operation = values[1];
            SOAPFactory soapFactory = new SOAP11Factory();
            SOAPEnvelope envelope = soapFactory.getDefaultEnvelope();

            OMNamespace omNs =
                soapFactory.createOMNamespace(values[0], "services");
            OMNamespace defualtNs = new OMNamespaceImpl("", null);

            OMElement opElement = soapFactory.createOMElement(operation, omNs);

            Iterator it = map.keySet().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                String value = (String) map.get(name);
                OMElement omEle = soapFactory.createOMElement(name, defualtNs);
                omEle.setText(value);
                opElement.addChild(omEle);
            }

            envelope.getBody().addChild(opElement);
            return envelope;
        } else {
            return null;
        }
    }

    public static StAXBuilder selectBuilderForMIME(
        MessageContext msgContext,
        InputStream inStream,
        String contentTypeString)
        throws OMException,
            XMLStreamException, FactoryConfigurationError,
            UnsupportedEncodingException {
        StAXBuilder builder = null;


        Parameter parameter_cache_attachment = msgContext.getParameter(
                Constants.Configuration.CACHE_ATTACHMENTS);
         boolean fileCacheForAttachments ;
        if(parameter_cache_attachment == null){
            fileCacheForAttachments = false;
        }  else {
           fileCacheForAttachments =
            (Constants
                .VALUE_TRUE
                .equals(
                    parameter_cache_attachment.getValue()));
        }
        String attachmentRepoDir = null;
        String attachmentSizeThreshold = null;
        Parameter parameter;
        if (fileCacheForAttachments) {
            parameter = msgContext.getParameter(Constants.Configuration.ATTACHMENT_TEMP_DIR);
            attachmentRepoDir = parameter==null?"":parameter.getValue().toString();

            parameter = msgContext
                    .getParameter(Constants.Configuration.FILE_SIZE_THRESHOLD);
            attachmentSizeThreshold = parameter==null?"":parameter.getValue().toString();
        }

        MIMEHelper mimeHelper = new MIMEHelper(inStream, contentTypeString,
                fileCacheForAttachments, attachmentRepoDir,attachmentSizeThreshold);

        String charSetEncoding = getCharSetEncoding(mimeHelper.getSOAPPartContentType());
        XMLStreamReader reader;
        if(charSetEncoding == null || "null".equalsIgnoreCase(charSetEncoding)){
             reader = XMLInputFactory.newInstance()
                .createXMLStreamReader(
                        new BufferedReader(new InputStreamReader(mimeHelper
                                .getSOAPPartInputStream(),
                                charSetEncoding)));
            msgContext.setProperty(MessageContext.CHARACTER_SET_ENCODING, charSetEncoding);

        }else {
            reader = XMLInputFactory.newInstance()
                .createXMLStreamReader(
                        new BufferedReader(new InputStreamReader(mimeHelper
                                .getSOAPPartInputStream())));
            msgContext.setProperty(MessageContext.CHARACTER_SET_ENCODING, MessageContext.UTF_8);

        }


        /*
		 * put a reference to Attachments in to the message context
		 */
        msgContext.setProperty(MTOMConstants.ATTACHMENTS, mimeHelper);
        if (mimeHelper.getAttachmentSpecType().equals(MTOMConstants.MTOM_TYPE)) {
            /*
             * Creates the MTOM specific MTOMStAXSOAPModelBuilder
             */
            builder =
                new MTOMStAXSOAPModelBuilder(
                    reader,
                    mimeHelper,
                    null);
        } else if (
            mimeHelper.getAttachmentSpecType().equals(MTOMConstants.SWA_TYPE)) {
            builder =
                new StAXSOAPModelBuilder(
                    reader,
                    SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI);
        }
        return builder;
    }

    public static boolean checkEnvelopeForOptimise(SOAPEnvelope envelope) {
        return isOptimised(envelope);
    }

    private static boolean isOptimised(OMElement element) {
        Iterator childrenIter = element.getChildren();
        boolean isOptimized = false;
        while (childrenIter.hasNext() && !isOptimized) {
            OMNode node = (OMNode) childrenIter.next();
            if (OMNode.TEXT_NODE == node.getType()
                && ((OMText) node).isOptimized()) {
                isOptimized = true;
            } else if (OMNode.ELEMENT_NODE == node.getType()) {
                isOptimized = isOptimised((OMElement) node);
            }
        }
        return isOptimized;
    }

    public static boolean doWriteMTOM(MessageContext msgContext) {
        boolean enableMTOM = false;
        if (msgContext.getParameter(Constants.Configuration.ENABLE_MTOM)
            != null) {
            enableMTOM =
                Constants.VALUE_TRUE.equals(
                    msgContext.getParameter(
                        Constants.Configuration.ENABLE_MTOM).getValue());
        } else if(msgContext.getProperty(Constants.Configuration.ENABLE_MTOM) != null) {
            enableMTOM =
                Constants.VALUE_TRUE.equals(
                    msgContext.getProperty(
                        Constants.Configuration.ENABLE_MTOM));
        }
        boolean envelopeContainsOptimise =
            HTTPTransportUtils.checkEnvelopeForOptimise(
                msgContext.getEnvelope());
        boolean doMTOM = enableMTOM && envelopeContainsOptimise;
        msgContext.setDoingMTOM(doMTOM);
        return doMTOM;
    }

    public static boolean isDoingREST(MessageContext msgContext) {
        boolean enableREST = false;
        if (msgContext.getParameter(Constants.Configuration.ENABLE_REST)
            != null) {
            enableREST =
                Constants.VALUE_TRUE.equals(
                    msgContext.getParameter(
                        Constants.Configuration.ENABLE_REST).getValue());
        } else if(msgContext.getProperty(Constants.Configuration.ENABLE_REST) != null) {
            enableREST =
                Constants.VALUE_TRUE.equals(
                    msgContext.getProperty(
                        Constants.Configuration.ENABLE_REST));
        }
        msgContext.setDoingREST(enableREST);
        return enableREST;

    }

    public static boolean isDoingRESTThoughPost(MessageContext msgContext) {
        boolean restThroughPost = true;

        if (msgContext.getParameter(Constants.Configuration.ENABLE_REST_THROUGH_GET)
            != null) {
            restThroughPost =
                Constants.VALUE_TRUE.equals(
                    msgContext.getParameter(
                        Constants.Configuration.ENABLE_REST_THROUGH_GET).getValue());
            restThroughPost = false;
        } else if(msgContext.getProperty(Constants.Configuration.ENABLE_REST_THROUGH_GET) != null) {
            restThroughPost =
                Constants.VALUE_TRUE.equals(
                    msgContext.getProperty(
                        Constants.Configuration.ENABLE_REST_THROUGH_GET));
            restThroughPost =false;
        }
        msgContext.setRestThroughPOST(!restThroughPost);
        return restThroughPost;

    }
}
