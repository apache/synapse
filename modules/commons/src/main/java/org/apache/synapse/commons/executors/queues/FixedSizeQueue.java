/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.commons.executors.queues;

import org.apache.synapse.commons.executors.InternalQueue;

import java.util.concurrent.locks.Condition;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Collection;

/**
 * A bounded queue implementation for internal queues. This queue is backed by an
 * fixed size array.
 *
 * @param <E> Should implement the Importance interface
 */
public class FixedSizeQueue<E> extends AbstractQueue<E> implements InternalQueue<E> {

    /**
     * Priority of this queue
     */
    private int priority;    

    /**
     * A waiting queue when this queue is full
     */
    private Condition notFullCond;

    /**
     * Array holding the queues
     */
    private E[] array;

    /**
     * Capacity of the queue
     */
    private int capacity;

    /**
     * Number of elements in the queue
     */
    private int count = 0;

    /**
     * Head of the queue
     */
    private int head;

    /**
     * Tail of the queue
     */
    private int tail;
    

    public FixedSizeQueue(int priority, int capacity) {
        this.priority = priority;        
        this.capacity = capacity;

        array = (E[]) new Object[capacity];
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int p) {
        this.priority = p;
    }    

    public Condition getNotFullCond() {
        return notFullCond;
    }

    public void setNotFullCond(Condition notFullCond) {
        this.notFullCond = notFullCond;
    }

    public Iterator<E> iterator() {return null;}

    public int size() {
        return count;
    }

    public String toString() {
        return super.toString() + this.priority;
    }

    public boolean offer(E e) {
        if (count == array.length) {
            return false;
        } else {
            insert(e);
            return true;
        }
    }

    public E poll() {
        if (count == 0)
            return null;
        return get();
    }

    public E peek() {
        return (count == 0) ? null : array[head];
    }

    public int remainingCapacity() {
        return capacity - count;        
    }

    public int drainTo(Collection<? super E> c) {
        final E[] items = this.array;
        int i = head;
        int n = 0;
        int max = count;
        while (n < max) {
            c.add(items[i]);
            items[i] = null;
            i = increment(i);
            n++;
        }
        if (n > 0) {
            count = 0;
            tail = 0;
            head = 0;

        }
        return n;
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        final E[] items = this.array;
        int i = head;
        int n = 0;
        int max = (maxElements < count) ? maxElements : count;
        while (n < max) {
            c.add(items[i]);
            items[i] = null;
            i = increment(i);
            n++;
        }
        if (n > 0) {
            count -= n;
            head = i;
        }
        return n;
    }

    public int getCapacity() {
        return capacity;
    }

    private int increment(int i) {
        return (++i == array.length)? 0 : i;
    }

    private void insert(E e) {
        array[tail] = e;
        tail = increment(tail);
        count++;
    }

    private E get() {
        E e = array[head];
        array[head] = null;
        head = increment(head);
        count--;
        return e;
    }
}
