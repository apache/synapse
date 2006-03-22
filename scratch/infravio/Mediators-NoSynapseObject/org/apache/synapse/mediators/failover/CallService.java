package org.apache.synapse.mediators.failover;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMDocument;
import org.apache.axis2.soap.SOAP12Constants;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAPFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2FlexibleMEPClient;
import org.apache.synapse.axis2.Axis2SynapseMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by IntelliJ IDEA.
 * User: Vikas
 * Date: Dec 29, 2005
 * Time: 11:51:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class CallService {
    public SynapseEnvironment environment;
    private Log log = LogFactory.getLog(getClass());
    private MessageContext newMsg = new MessageContext();
    Axis2SynapseMessage synapseMessage;

     public CallService(SynapseEnvironment synapseEnvironment) {
        //Setting the environment
        this.environment = synapseEnvironment;
        log.info("Call Service Constructor");
    }

    public SynapseMessage execute(long timeoutValue, SynapseMessage synapseMsg) {
        synapseMessage = (Axis2SynapseMessage) synapseMsg;
        log.info("Execute Called");
        if (timeoutValue == 0) {
            try {
                newMsg = Axis2FlexibleMEPClient.send(synapseMessage.getMessageContext());
                synapseMessage = new Axis2SynapseMessage(newMsg);
            }
            catch (AxisFault axisFault) {
                thePivot();
                axisFault.printStackTrace();
            }
        } else {
            final CallThread thread  = new CallThread();
            Timer timer = new Timer(false);
            log.info("Timer Defined");
            TimerTask task = new TimerTask() {
                public void run() {

                    if (thread.completed) {
                        thread.interrupt();
                    }
                    /* The above method does not stop blocking SoapThread*/
                    thread.interrupted = true;
                }
            };

            timer.schedule(task, timeoutValue);
            thread.start();
            log.info("Thread started!");
            while (!thread.interrupted) {

                try {

                    Thread.sleep(timeoutValue / 100);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            timer.cancel();
            if (thread.fault != null) {
                thread.interrupted = true;
            }
        }
        return synapseMessage;
    }

    private void thePivot() {
        log.info("EXCEPTION CAUGHT FOR FAILOVER!");

        SOAPFactory factory;
        SOAPEnvelope envelope = synapseMessage.getEnvelope();
        if (envelope.getNamespace().getName().equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
            factory = OMAbstractFactory.getSOAP12Factory();
        } else {
            factory = OMAbstractFactory.getSOAP11Factory();
        }
        try {
            OMDocument soapFaultDocument = factory.createOMDocument();
            SOAPEnvelope faultEnvelope = factory.getDefaultFaultEnvelope();
            soapFaultDocument.addChild(faultEnvelope);
            newMsg.setEnvelope(faultEnvelope);
            synapseMessage = new Axis2SynapseMessage(newMsg);
            synapseMessage.setFaultResponse(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     public class CallThread extends Thread {

        public boolean interrupted = false;
        public boolean completed = false;
        String fault;

        public void run() {
            try {
                newMsg = Axis2FlexibleMEPClient.send(synapseMessage.getMessageContext());
                synapseMessage = new Axis2SynapseMessage(newMsg);
                this.completed = true;
            } catch (AxisFault axisFault) {
                axisFault.printStackTrace();
                thePivot();
            }

        }
    }
}
