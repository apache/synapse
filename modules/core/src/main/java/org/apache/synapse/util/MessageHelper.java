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

package org.apache.synapse.util;

import org.apache.axiom.om.OMCloneOptions;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.axiom.soap.*;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.client.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.statistics.ErrorLog;
import org.apache.synapse.aspects.statistics.StatisticsLog;
import org.apache.synapse.aspects.statistics.StatisticsRecord;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.template.TemplateContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;


/**
 *
 */
public class MessageHelper {


    private static Log log = LogFactory.getLog(MessageHelper.class);

    /**
     * This method will simulate cloning the message context and creating an exact copy of the
     * passed message. One should use this method with care; that is because, inside the new MC,
     * most of the attributes of the MC like opCtx and so on are still kept as references inside
     * the axis2 MessageContext for performance improvements. (Note: You don't have to worry
     * about the SOAPEnvelope, it is a cloned copy and not a reference from any other MC)
     *
     * @param synCtx - this will be cloned 
     * @return cloned Synapse MessageContext
     * @throws AxisFault if there is a failure in creating the new Synapse MC or in a failure in
     *          cloning the underlying axis2 MessageContext
     * 
     * @see MessageHelper#cloneAxis2MessageContext 
     */
    public static MessageContext cloneMessageContext(MessageContext synCtx) throws AxisFault {

        // creates the new MessageContext and clone the internal axis2 MessageContext
        // inside the synapse message context and place that in the new one
        MessageContext newCtx = synCtx.getEnvironment().createMessageContext();
        Axis2MessageContext axis2MC = (Axis2MessageContext) newCtx;
        axis2MC.setAxis2MessageContext(
            cloneAxis2MessageContext(((Axis2MessageContext) synCtx).getAxis2MessageContext()));

        newCtx.setConfiguration(synCtx.getConfiguration());
        newCtx.setEnvironment(synCtx.getEnvironment());
        newCtx.setContextEntries(synCtx.getContextEntries());

        // set the parent correlation details to the cloned MC -
        //                              for the use of aggregation like tasks
        newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION, synCtx.getMessageID());

        // copying the core parameters of the synapse MC
        newCtx.setTo(synCtx.getTo());
        newCtx.setReplyTo(synCtx.getReplyTo());
        newCtx.setSoapAction(synCtx.getSoapAction());
        newCtx.setWSAAction(synCtx.getWSAAction());
        newCtx.setResponse(synCtx.isResponse());
        newCtx.setTracingState(synCtx.getTracingState());

        // copy all the synapse level properties to the newCtx
        for (Object o : synCtx.getPropertyKeySet()) {
            String key = (String) o;                    // MessageContext API enforce key to be a String
            Object obj = synCtx.getProperty(key);
            if (obj instanceof String || obj instanceof Integer) {  // For immutable
                // Do nothing
            } else if (obj instanceof ArrayList) {
                obj = cloneArrayList((ArrayList) obj);
            } else if (obj instanceof Stack
                    && key.equals(SynapseConstants.SYNAPSE__FUNCTION__STACK)) {
                obj = getClonedTemplateStack((Stack<TemplateContext>) obj);
            } else if (obj instanceof StatisticsRecord) {
                obj = getClonedStatisticRecord((StatisticsRecord) obj);
            } else if (obj instanceof OMElement) {
                obj = ((OMElement) obj).cloneOMElement();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Deep clone not happened for property : " + key + ". Class type : "
                             + obj.getClass().getName());
                }
            }
            newCtx.setProperty(key, obj);
        }
        
        // Make deep copy of fault stack so that parent will not be lost it's fault stack
        Stack<FaultHandler> faultStack = synCtx.getFaultStack();
        if (!faultStack.isEmpty()) {
            
            List<FaultHandler> newFaultStack = new ArrayList<FaultHandler>();
            newFaultStack.addAll(faultStack);
            
            for (FaultHandler faultHandler : newFaultStack) {
                if (faultHandler != null) {
                    newCtx.pushFaultHandler(faultHandler);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.info("Parent's Fault Stack : " + faultStack
                    + " : Child's Fault Stack :" + newCtx.getFaultStack());
        }
        
        return newCtx;
    }

    /**
     * This method will simulate cloning the message context and creating an exact copy of the
     * passed message. One should use this method with care; that is because, inside the new MC,
     * most of the attributes of the MC like opCtx and so on are still kept as references. Otherwise
     * there will be performance issues. But ..... this may reveal in some conflicts in the cloned message
     * if you try to do advanced mediations with the cloned message, in which case you should
     * manually get a clone of the changing part of the MC and set that cloned part to your MC.
     * Changing the MC after doing that will solve most of the issues. (Note: You don't have to worry
     * about the SOAPEnvelope, it is a cloned copy and not a reference from any other MC)
     *
     * @param mc - this will be cloned for getting an exact copy
     * @return cloned MessageContext from the given mc
     * @throws AxisFault if there is a failure in copying the certain attributes of the
     *          provided message context
     */
    public static org.apache.axis2.context.MessageContext cloneAxis2MessageContext(
        org.apache.axis2.context.MessageContext mc) throws AxisFault {

        org.apache.axis2.context.MessageContext newMC = clonePartially(mc);
        newMC.setEnvelope(cloneSOAPEnvelope(mc.getEnvelope()));
        newMC.setOptions(cloneOptions(mc.getOptions()));
        
        newMC.setServiceContext(mc.getServiceContext());
        newMC.setOperationContext(mc.getOperationContext());
        newMC.setAxisMessage(mc.getAxisMessage());
        if (newMC.getAxisMessage() != null) {
            newMC.getAxisMessage().setParent(mc.getAxisOperation());
        }
        newMC.setAxisService(mc.getAxisService());

        // copying transport related parts from the original
        newMC.setTransportIn(mc.getTransportIn());
        newMC.setTransportOut(mc.getTransportOut());
        newMC.setProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO,
            mc.getProperty(org.apache.axis2.Constants.OUT_TRANSPORT_INFO));

        newMC.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
            getClonedTransportHeaders(mc));
        
        
        if(newMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE) != null){
        	//clone passthrough pipe here..writer...
        	 NHttpServerConnection conn = (NHttpServerConnection) newMC.getProperty("pass-through.Source-Connection");
        	 if(conn != null){
        		  SourceConfiguration sourceConfiguration = (SourceConfiguration) newMC.getProperty(
                          "PASS_THROUGH_SOURCE_CONFIGURATION");
        		  Pipe pipe = new Pipe(conn, sourceConfiguration.getBufferFactory().getBuffer(), "source", sourceConfiguration);
        		  newMC.setProperty(PassThroughConstants.PASS_THROUGH_PIPE,pipe);
        	 }
        }
        
        return newMC;
    }

    public static Map getClonedTransportHeaders(org.apache.axis2.context.MessageContext msgCtx) {
        
        Map headers = (Map) msgCtx.
                getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Map<String, Object> clonedHeaders;
        if (headers instanceof TreeMap) {
            clonedHeaders = new TreeMap<String, Object>(new Comparator<String>() {
                public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        } else {
            clonedHeaders = new HashMap<String, Object>();
        }

        if (headers != null && !headers.isEmpty()) {
            for (Object o : headers.keySet()) {
                String headerName = (String) o;
                clonedHeaders.put(headerName, headers.get(headerName));
            }
        }

        return clonedHeaders;
    }

    public static org.apache.axis2.context.MessageContext clonePartially(
        org.apache.axis2.context.MessageContext ori) throws AxisFault {

        org.apache.axis2.context.MessageContext newMC
            = new org.apache.axis2.context.MessageContext();
        
        // do not copy options from the original
        newMC.setConfigurationContext(ori.getConfigurationContext());
        newMC.setMessageID(UIDGenerator.generateURNString());
        newMC.setTo(ori.getTo());
        newMC.setSoapAction(ori.getSoapAction());

        newMC.setProperty(org.apache.axis2.Constants.Configuration.CHARACTER_SET_ENCODING,
                ori.getProperty(org.apache.axis2.Constants.Configuration.CHARACTER_SET_ENCODING));
        newMC.setProperty(org.apache.axis2.Constants.Configuration.ENABLE_MTOM,
                ori.getProperty(org.apache.axis2.Constants.Configuration.ENABLE_MTOM));
        newMC.setProperty(org.apache.axis2.Constants.Configuration.ENABLE_SWA,
                ori.getProperty(org.apache.axis2.Constants.Configuration.ENABLE_SWA));
        newMC.setProperty(Constants.Configuration.HTTP_METHOD,
            ori.getProperty(Constants.Configuration.HTTP_METHOD));
        //coping the Message type from req to res to get the message formatters working correctly.
        newMC.setProperty(Constants.Configuration.MESSAGE_TYPE,
                ori.getProperty(Constants.Configuration.MESSAGE_TYPE));

        newMC.setDoingREST(ori.isDoingREST());
        newMC.setDoingMTOM(ori.isDoingMTOM());
        newMC.setDoingSwA(ori.isDoingSwA());

        // If the original request carries any attachments, copy them to the clone
        // as well. Note that with the change introduced by AXIS2-5308 we can simply
        // copy the reference to the original Attachments object. This should also enable
        // streaming of MIME parts in certain scenarios.
        newMC.setAttachmentMap(ori.getAttachmentMap());

        Iterator itr = ori.getPropertyNames();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            if (key != null) {
                // In a clustered environment, all the properties that need to be replicated,
                // are replicated explicitly  by the corresponding Mediators (Ex: throttle,
                // cache), and therefore we should avoid any implicit replication
                newMC.setNonReplicableProperty(key, ori.getPropertyNonReplicable(key));
            }
        }

        newMC.setServerSide(false);

        return newMC;
    }

    /**
     * This method will clone the provided SOAPEnvelope and returns the cloned envelope
     * as an exact copy of the provided envelope
     *
     * @param envelope - this will be cloned to get the new envelope
     * @return cloned SOAPEnvelope from the provided one
     */
    public static SOAPEnvelope cloneSOAPEnvelope(SOAPEnvelope envelope) {
        OMCloneOptions options = new OMCloneOptions();
        options.setPreserveModel(true);
        return (SOAPEnvelope)envelope.clone(options);
    }

    /**
     * Clones the given {@link org.apache.axis2.client.Options} object. This is not a deep copy
     * because this will be called for each and every message going out from synapse. The parent
     * of the cloning options object is kept as a reference.
     *
     * @param options cloning object
     * @return cloned Options object
     */
    public static Options cloneOptions(Options options) {

        // create new options object and set the parent
        Options clonedOptions = new Options(options.getParent());

        // copy general options
        clonedOptions.setCallTransportCleanup(options.isCallTransportCleanup());
        clonedOptions.setExceptionToBeThrownOnSOAPFault(options.isExceptionToBeThrownOnSOAPFault());
        clonedOptions.setManageSession(options.isManageSession());
        clonedOptions.setSoapVersionURI(options.getSoapVersionURI());
        clonedOptions.setTimeOutInMilliSeconds(options.getTimeOutInMilliSeconds());
        clonedOptions.setUseSeparateListener(options.isUseSeparateListener());

        // copy transport related options
        clonedOptions.setListener(options.getListener());
        clonedOptions.setTransportIn(options.getTransportIn());
        clonedOptions.setTransportInProtocol(options.getTransportInProtocol());
        clonedOptions.setTransportOut(clonedOptions.getTransportOut());

        // copy username and password options
        clonedOptions.setUserName(options.getUserName());
        clonedOptions.setPassword(options.getPassword());

        // cloen the property set of the current options object
        for (Object o : options.getProperties().keySet()) {
            String key = (String) o;
            clonedOptions.setProperty(key, options.getProperty(key));
        }

        return clonedOptions;
    }

    /**
     * Removes Submission and Final WS-Addressing headers and return the SOAPEnvelope from the given
     * message context
     *
     * @param axisMsgCtx the Axis2 Message context
     * @return the resulting SOAPEnvelope
     */
    public static SOAPEnvelope removeAddressingHeaders(
            org.apache.axis2.context.MessageContext axisMsgCtx) {

        SOAPEnvelope env = axisMsgCtx.getEnvelope();
        SOAPHeader soapHeader = env.getHeader();
        ArrayList addressingHeaders;

        if (soapHeader != null) {
            addressingHeaders =
                soapHeader.getHeaderBlocksWithNSURI(AddressingConstants.Submission.WSA_NAMESPACE);

            if (addressingHeaders != null && addressingHeaders.size() != 0) {
                detachAddressingInformation(addressingHeaders);

            } else {
                addressingHeaders =
                    soapHeader.getHeaderBlocksWithNSURI(AddressingConstants.Final.WSA_NAMESPACE);
                if (addressingHeaders != null && addressingHeaders.size() != 0) {
                    detachAddressingInformation(addressingHeaders);
                }
            }
        }
        return env;
    }

    /**
     * Remove WS-A headers
     *
     * @param headerInformation headers to be removed
     */
    private static void detachAddressingInformation(ArrayList headerInformation) {
        for (Object o : headerInformation) {
            if (o instanceof SOAPHeaderBlock) {
                SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) o;
                headerBlock.detach();
            } else if (o instanceof OMElement) {
                // work around for a known addressing bug which sends non SOAPHeaderBlock objects
                OMElement om = (OMElement) o;
                OMNamespace ns = om.getNamespace();
                if (ns != null && (
                    AddressingConstants.Submission.WSA_NAMESPACE.equals(ns.getNamespaceURI()) ||
                        AddressingConstants.Final.WSA_NAMESPACE.equals(ns.getNamespaceURI()))) {
                    om.detach();
                }
            }
        }
    }

    /**
     * Get the Policy object for the given name from the Synapse configuration at runtime
     * 
     * @param synCtx the current synapse configuration to get to the synapse configuration
     * @param propertyKey the name of the property which holds the Policy required
     * @return the Policy object with the given name, from the configuration
     */
    public static Policy getPolicy(org.apache.synapse.MessageContext synCtx, String propertyKey) {
        Object property = synCtx.getEntry(propertyKey);
        if (property != null && property instanceof OMElement) {
            return PolicyEngine.getPolicy((OMElement) property);
        } else {
            handleException("Cannot locate policy from the property : " + propertyKey);
        }
        return null;
    }

    /**
     * Remove the headers that are marked as processed.
     * @param axisMsgCtx the Axis2 Message context
     * @param preserveAddressing if true preserve the addressing headers     
     */
    public static void removeProcessedHeaders(org.apache.axis2.context.MessageContext axisMsgCtx,
                                              boolean preserveAddressing) {
        SOAPEnvelope env = axisMsgCtx.getEnvelope();
        SOAPHeader soapHeader = env.getHeader();

        if (soapHeader != null) {
            Iterator it = soapHeader.getChildElements();
            while (it.hasNext()) {
                Object o = it.next();
                if (o instanceof SOAPHeaderBlock) {
                    SOAPHeaderBlock headerBlock = (SOAPHeaderBlock) o;
                    if (!preserveAddressing) {
                        // if we don't need to preserve addressing headers remove without checking
                        if (headerBlock.isProcessed()) {
                            it.remove();
                        }
                    } else {
                        // else remove only if not an addressing header
                        if (!isAddressingHeader(headerBlock)) {
                            if (headerBlock.isProcessed()) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }        
    }

    /**
     * Return true if the SOAP header is an addressing header
     * @param headerBlock SOAP header block to be checked
     * @return true if the SOAP header is an addressing header
     */
    private static boolean isAddressingHeader(SOAPHeaderBlock headerBlock) {
        OMNamespace ns = headerBlock.getNamespace();
        return ns != null && (
                AddressingConstants.Submission.WSA_NAMESPACE.equals(ns.getNamespaceURI()) ||
                        AddressingConstants.Final.WSA_NAMESPACE.equals(ns.getNamespaceURI()));
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    /*
     * This method will deep clone array list by creating a new ArrayList and cloning and adding each element in it
     */
    private static ArrayList<Object> cloneArrayList(ArrayList<Object> originalArrayList) {
        ArrayList<Object> clonedArrayList = null;
        if (originalArrayList != null) {
            clonedArrayList = new ArrayList<Object>();
            for (Object obj : originalArrayList) {
                if (obj instanceof SOAPHeaderBlock) {
                    SOAPFactory fac = (SOAPFactory) ((SOAPHeaderBlock) obj).getOMFactory();
                    obj = ((SOAPHeaderBlock) obj).cloneOMElement();
                    try {
                        obj = ElementHelper.toSOAPHeaderBlock((OMElement) obj, fac);
                    } catch (Exception e) {
                        handleException(e.getLocalizedMessage());
                    }
                } else if (obj instanceof SOAPEnvelope) {
                    SOAPEnvelope env = (SOAPEnvelope) obj;
                    obj = MessageHelper.cloneSOAPEnvelope(env);
                } else if (obj instanceof OMElement) {
                    obj = ((OMElement) obj).cloneOMElement();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Array List deep clone not implemented for Class type : " + obj.getClass().getName());
                    }
                }
                clonedArrayList.add(obj);
            }
        }
        return clonedArrayList;
    }

    /**
     * Get a clone of a Template Function stack
     *
     * @param oriTemplateStack original template function stack to be cloned
     * @return clone of a Template Function stack
     */
    private static Stack<TemplateContext> getClonedTemplateStack(Stack<TemplateContext> oriTemplateStack) {

        Stack<TemplateContext> clonedTemplateStack = new Stack<TemplateContext>();

        for (TemplateContext oriTemplateCtx : oriTemplateStack) {
            TemplateContext clonedTemplateCtx =
                    new TemplateContext(oriTemplateCtx.getName(), oriTemplateCtx.getParameters());

            Map oriValueMap = oriTemplateCtx.getMappedValues();
            Map clonedValueMap = new HashMap();
            for (Object key : oriValueMap.keySet()) {
                Object value = oriValueMap.get(key);
                if (value instanceof ArrayList) {
                    value = cloneArrayList((ArrayList<Object>) value);
                }
                clonedValueMap.put(key, value);
            }
            clonedTemplateCtx.setMappedValues(clonedValueMap);
            clonedTemplateStack.push(clonedTemplateCtx);
        }
        return clonedTemplateStack;
    }


    /**
     * Get clone of Statistic Record
     *
     * @param oriRecord original statistic record
     * @return clone of Statistic Record
     */
    private static StatisticsRecord getClonedStatisticRecord (StatisticsRecord oriRecord) {

        StatisticsRecord clonedRecord =
                new StatisticsRecord(oriRecord.getId(), oriRecord.getClientIP(), oriRecord.getClientHost());

        clonedRecord.setOwner(oriRecord.getOwner());
        clonedRecord.setEndReported(oriRecord.isEndReported());

        // Clone stats logs
        List<StatisticsLog> oriStatisticsLogs = oriRecord.getAllStatisticsLogs();
        for (StatisticsLog oriLog : oriStatisticsLogs) {

            StatisticsLog clonedLog = new StatisticsLog(oriLog.getId(), oriLog.getComponentType());
            clonedLog.setTime(oriLog.getTime());
            clonedLog.setResponse(oriLog.isResponse());
            clonedLog.setFault(oriLog.isFault());
            clonedLog.setEndAnyLog(oriLog.isEndAnyLog());

            // Error Log
            ErrorLog oriErrorLog = oriLog.getErrorLog();
            if (oriErrorLog != null) {
                ErrorLog clonedErrorLog = new ErrorLog(oriErrorLog.getErrorCode());
                if (oriErrorLog.getException() != null) {
                    clonedErrorLog.setException(oriErrorLog.getException());
                }
                clonedErrorLog.setErrorMessage(oriErrorLog.getErrorMessage());
                clonedErrorLog.setErrorDetail(oriErrorLog.getErrorDetail());
                clonedLog.setErrorLog(clonedErrorLog);
            }
            clonedRecord.collect(clonedLog);
        }
        return clonedRecord;
    }

}
