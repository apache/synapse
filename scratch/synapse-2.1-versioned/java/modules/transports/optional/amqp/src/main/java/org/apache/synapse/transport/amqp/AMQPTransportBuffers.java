/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.transport.amqp;

import org.apache.axis2.AxisFault;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Keeps the request/response messages until pick by the processing/response dispatching tasks.
 * These buffers(an instance of
 * http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/BlockingQueue.html) are used in
 * order to define an asynchronous architecture between the polling tak and actual processing which
 * will lead to higher performance.
 */
public class AMQPTransportBuffers {
    /**
     * The request message buffer which holds the request messages
     */
    private BlockingQueue<AMQPTransportMessage> requestBuffer =
            new LinkedBlockingQueue<AMQPTransportMessage>();

    /**
     * The response message buffer which holds the responses for processed messages
     */
    private BlockingQueue<AMQPTransportMessage> responseBuffer =
            new LinkedBlockingQueue<AMQPTransportMessage>();

    /**
     * Returns the response messages as a list
     *
     * @param blockSize the block blockSize of the response message list
     * @return the block of the response messages of blockSize
     * @throws AMQPTransportException in case of an error
     */
    public List<AMQPTransportMessage> getResponseMessageList(final int blockSize) throws AMQPTransportException {
        List<AMQPTransportMessage> msgList = new ArrayList<AMQPTransportMessage>();
        if (responseBuffer.size() > 0) {
            AMQPTransportUtils.moveElements(responseBuffer, msgList, blockSize);
        }
        return msgList;
    }

    /**
     * Add a response message to the response buffer
     *
     * @param msg the response message
     * @throws InterruptedException throws in case of an error
     */
    public void addResponseMessage(AMQPTransportMessage msg) throws InterruptedException {
        // it's ok to block here until space available,
        responseBuffer.put(msg);
    }

    /**
     * Returns the request message buffer in transport
     *
     * @return the request message buffer
     */
    public BlockingQueue<AMQPTransportMessage> getRequestMessageBuffer() {
        return requestBuffer;
    }

    /**
     * Add a message to the request message buffer
     *
     * @param msg the message to add into the buffer
     */
    public void addRequestMessage(AMQPTransportMessage msg) {
        requestBuffer.add(msg);
    }

    /**
     * Returns a request message from the request message buffer
     *
     * @return the request message
     */
    public AMQPTransportMessage getRequestMessage() {
        try {
            // block if there is no messages
            return requestBuffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }
}