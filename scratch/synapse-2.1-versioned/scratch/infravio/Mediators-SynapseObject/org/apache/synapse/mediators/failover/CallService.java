package org.apache.synapse.mediators.failover;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.axis2.Axis2FlexibleMEPClient;
import org.apache.synapse.axis2.Axis2SynapseMessage;

import java.util.Timer;
import java.util.TimerTask;

import com.infravio.threads.SimpleWorkManager;


/**
 * Created by IntelliJ IDEA.
 * User: Vikas
 * Date: Dec 29, 2005
 * Time: 11:51:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class CallService {
    public SynapseEnvironment environment;
    SynapseMessage synapseMsg;
    private Log log = LogFactory.getLog(getClass());
    private MessageContext newMsg = new MessageContext();
    Axis2SynapseMessage synapseMessage;

    public CallService(SynapseEnvironment synapseEnvironment) {
        //Setting the environment
        this.environment = synapseEnvironment;
        log.info("Call Service Constructor");

        //thread = new CallThread();
    }

    public SynapseMessage execute(long timeoutValue, SynapseMessage synapseMsg) {
        this.synapseMsg = synapseMsg;
        synapseMessage = (Axis2SynapseMessage) synapseMsg;
        log.info("Execute Called");
        if (timeoutValue == 0) {
            try {
                newMsg = Axis2FlexibleMEPClient.send(synapseMessage.getMessageContext());
                synapseMessage = new Axis2SynapseMessage(newMsg);
            }
            catch (AxisFault axisFault) {
                // thePivot();
                // axisFault.printStackTrace();
                AxisEngine ae =
                        new AxisEngine(((Axis2SynapseMessage) synapseMsg).getMessageContext().getConfigurationContext());
                try {
                    synapseMessage = new Axis2SynapseMessage(ae.createFaultMessageContext(((Axis2SynapseMessage) synapseMsg).getMessageContext(), axisFault));
                    System.out.println("THE RESPONSE ===== " + synapseMessage.getEnvelope());
                    synapseMessage.setFaultResponse(true);
                } catch (AxisFault axisFault1) {
                    axisFault1.printStackTrace();
                }
            }
        } else {
            final CallThread thread = new CallThread();
            final SimpleWorkManager manager = new SimpleWorkManager(5);
            Timer timer = new Timer(false);
            log.info("Timer Defined");
            TimerTask task = new TimerTask() {
                public void run() {

                    if (thread.completed) {
                       // manager.destroy();
                        //thread.interrupt();
                    }
                    /* The above method does not stop blocking SoapThread*/
                    thread.interrupted = true;
                }
            };

            timer.schedule(task, timeoutValue);
            //thread.start();
            manager.addTask(thread);
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

    public class CallThread implements Runnable {
        //  public class CallThread extends Thread {

        public boolean interrupted = false;
        public boolean completed = false;
        String fault;

        public void run() {
            try {
                newMsg = Axis2FlexibleMEPClient.send(synapseMessage.getMessageContext());
                synapseMessage = new Axis2SynapseMessage(newMsg);
                this.completed = true;
            } catch (AxisFault axisFault) {
                AxisEngine ae =
                        new AxisEngine(((Axis2SynapseMessage) synapseMsg).getMessageContext().getConfigurationContext());
                try {
                    synapseMessage = new Axis2SynapseMessage(ae.createFaultMessageContext(((Axis2SynapseMessage) synapseMsg).getMessageContext(), axisFault));
                    System.out.println("THE RESPONSE ===== " + synapseMessage.getEnvelope());
                    synapseMessage.setFaultResponse(true);
                } catch (AxisFault axisFault1) {
                    axisFault1.printStackTrace();
                }
            }

        }
    }
}
