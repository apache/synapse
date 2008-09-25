package org.apache.synapse.transport.jms;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.internet.ContentType;
import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.InOnlyAxisOperation;
import org.apache.axis2.engine.MessageReceiver;
import org.apache.axis2.transport.jms.JMSConstants;
import org.apache.synapse.transport.testkit.client.AsyncTestClient;
import org.apache.synapse.transport.testkit.client.ClientOptions;
import org.apache.synapse.transport.testkit.server.axis2.AxisEndpoint;
import org.apache.synapse.transport.testkit.tests.TestResourceSet;
import org.apache.synapse.transport.testkit.tests.TransportTestCase;

public class ConcurrencyTest extends TransportTestCase {
    private static class BlockingMessageReceiver implements MessageReceiver {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicInteger messageCount = new AtomicInteger();
        
        public void receive(MessageContext msgContext) throws AxisFault {
            System.out.println("MESSAGE");
            messageCount.incrementAndGet();
            try {
                latch.await();
            } catch (InterruptedException ex) {
            }
        }
        
        public void unblock() {
            latch.countDown();
        }
        
        public int getMessageCount() {
            return messageCount.get();
        }
    }
    
    private final JMSAsyncChannel channel = new JMSAsyncChannel(JMSConstants.DESTINATION_TYPE_QUEUE, ContentTypeMode.TRANSPORT);
    private final AsyncTestClient<byte[]> client = new JMSAsyncClient<byte[]>(JMSBytesMessageFactory.INSTANCE);
    
    public ConcurrencyTest() {
        addResource(new JMSTransportDescriptionFactory(false, false));
        addResource(new QpidTestEnvironment());
        addResource(channel);
        addResource(client);
    }
    
    public void _test() throws Exception {
        final BlockingMessageReceiver messageReceiver = new BlockingMessageReceiver();
        
        TestResourceSet resources = new TestResourceSet(getResourceSet());
        resources.addResource(new AxisEndpoint() {
            @Override
            protected AxisOperation createOperation() {
                AxisOperation operation = new InOnlyAxisOperation(new QName("default"));
                operation.setMessageReceiver(messageReceiver);
                return operation;
            }
        });
        
        resources.setUp();
        
        ClientOptions options = new ClientOptions(new ContentType("application/octet-stream"), null);
        for (int i=0; i<100; i++) {
            client.sendMessage(options, options.getBaseContentType(), new byte[2048]);
        }
        
        Thread.sleep(500);
        
        int messagesReceived = messageReceiver.getMessageCount();
        
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
                messageReceiver.unblock();
            }
        }.start();
        
        resources.tearDown();
        
        int remainingInQueue = channel.getMessageCount();
        
        System.out.println("Messages received by MessageReceiver: " + messagesReceived);
        System.out.println("Remaining in queue: " + remainingInQueue);
        System.out.println("Total accepted: " + messageReceiver.getMessageCount());
    }
}
