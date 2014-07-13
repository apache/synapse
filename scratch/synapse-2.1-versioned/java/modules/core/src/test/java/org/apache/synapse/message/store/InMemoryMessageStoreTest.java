/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.message.store;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;

import java.util.List;
import java.util.NoSuchElementException;

public class InMemoryMessageStoreTest extends TestCase {
    
    public void testBasics() throws Exception {
        MessageStore store = new InMemoryMessageStore();
        populateStore(store, 10);
        
        // test size()
        assertEquals(10, store.size());
        
        // test get(index)
        for (int i = 0; i < 10; i++) {
            assertEquals("ID" + i, store.get(i).getMessageID());
        }

        // test get(messageId)
        for (int i = 0; i < 10; i++) {
            assertEquals("ID" + i, store.get("ID" + i).getMessageID());
        }
        
        // test getAll()
        List<MessageContext> list = store.getAll();
        assertEquals(10, list.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("ID" + i, list.get(i).getMessageID());
        }
        
        // test peek()
        assertEquals("ID0", store.peek().getMessageID());
        
        // test poll()
        for (int i = 0; i < 10; i++) {
            assertEquals("ID" + i, store.poll().getMessageID());
        }

        populateStore(store, 10);

        // test remove()
        for (int i = 0; i < 10; i++) {
            assertEquals("ID" + i, store.remove().getMessageID());
        }
        try {
            store.remove();
            fail();
        } catch (NoSuchElementException expected) {

        }

        populateStore(store, 10);

        // test clear()
        assertEquals(10, store.size());
        store.clear();
        assertEquals(0, store.size());
    }
    
    public void testOrderedDelivery1() throws Exception {
        MessageStore store = new InMemoryMessageStore();        

        for (int i = 0; i < 100; i++) {
            store.offer(createMessageContext("ID" + i));
        }
        
        for (int i = 0; i < 100; i++) {
            assertEquals("ID" + i, store.poll().getMessageID());
        }
    }
    
    public void testOrderedDelivery2() throws  Exception {
        MessageStore store = new InMemoryMessageStore();
        store.offer(createMessageContext("FOO"));

        MessageContext msg = store.peek();
        assertEquals("FOO", msg.getMessageID());

        store.offer(createMessageContext("BAR"));
        msg = store.poll();
        assertEquals("FOO", msg.getMessageID());

        msg = store.peek();
        assertEquals("BAR", msg.getMessageID());
    }

    public void testStoreObserver() throws Exception {
        MessageStore store = new InMemoryMessageStore();
        TestObserver observer = new TestObserver();
        store.registerObserver(observer);

        for (int i = 0; i < 100; i++) {
            store.offer(createMessageContext("ID" + i));
        }
        assertEquals(100, observer.getCount());

        for (int i = 0; i < 100; i++) {
            store.poll();
        }
        assertEquals(0, observer.getCount());
    }
    
    private MessageContext createMessageContext(String identifier) throws Exception {
        MessageContext msg = TestUtils.createLightweightSynapseMessageContext("<test/>");
        msg.setMessageID(identifier);
        return msg;
    }
    
    private void populateStore(MessageStore store, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            store.offer(createMessageContext("ID" + i));
        }
    }

    private static class TestObserver implements MessageStoreObserver {
        int counter = 0;

        public void messageAdded(String messageId) {
            counter++;
        }

        public void messageRemoved(String messageId) {
            counter--;
        }

        public int getCount() {
            return counter;
        }
    }
}
