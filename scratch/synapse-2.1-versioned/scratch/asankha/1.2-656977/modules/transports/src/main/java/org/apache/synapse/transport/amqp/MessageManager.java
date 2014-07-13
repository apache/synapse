package org.apache.synapse.transport.amqp;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.qpidity.api.Message;
import org.apache.qpidity.nclient.Session;
import org.apache.qpidity.nclient.util.MessageListener;
import org.apache.qpidity.transport.RangeSet;

public class MessageManager implements MessageListener
{
    private static final Log log = LogFactory.getLog(AMQPSender.class);
    private ArrayBlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(1,true);
    private Session session;
    private String destination;
    private String corelationId;

    public MessageManager(Session session, String destination,String corelationId){
        this.session = session;
        this.destination = destination;
    }

    /*
     *  when this mehtod is called, it is assumed that we have exclusive access
     *  to the session.
     */
    public Message receive(long timeout){
        Message m;
        session.messageFlow(destination,Session.MESSAGE_FLOW_UNIT_MESSAGE, 1);
        session.messageFlow(destination,Session.MESSAGE_FLOW_UNIT_BYTE, 0xFFFFFFFF);
        try{
            m = queue.poll(timeout, TimeUnit.MILLISECONDS);
        }catch(Exception e){
            throw new AMQPSynapseException("unable to receive message",e);
        }

        if (m == null)
        {
            log.debug("Message Didn't arrive in time, checking if one is inflight");
            // checking if one is inflight
            session.messageFlush(destination);
            session.sync();
            try{
                m = queue.take();
            }catch(Exception e){
                throw new AMQPSynapseException("unable to receive message",e);
            }
        }

        return m;
    }

    public void onMessage(Message m)
    {
        System.out.println("\n================== Received Msg ==================");
        System.out.println("Message Id : " + m.getMessageProperties().getMessageId());
        System.out.println(m.toString());
        System.out.println("================== End Msg ==================\n");

        //AMQP currently doesn't support server side filters, so doing client side temporarily
        if(corelationId.equals(m.getMessageProperties().getCorrelationId())){
            queue.add(m);
        }else{
            RangeSet r = new RangeSet();
            r.add(m.getMessageTransferId());
            session.messageRelease(r);
        }
    }
}
