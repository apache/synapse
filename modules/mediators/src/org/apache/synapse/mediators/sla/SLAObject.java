/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.mediators.sla;

public class SLAObject implements Comparable {

    int priority;
    long time;
    Object object;

    public SLAObject() {
    }

    public SLAObject(int priority, long time, Object object) {
        this.priority = priority;
        this.time = time;
        this.object = object;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int index) {
        this.priority = index;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public int compareTo(Object o2) {
        SLAObject obj1 = this;
        SLAObject obj2 = (SLAObject) o2;
        if (obj1.getTime() == obj2.getTime()) {
            if (obj1.getPriority() == obj2.getPriority()) {
                if (obj1.hashCode() == obj2.hashCode()) {
                    return 0;
                } else if (obj1.hashCode() < obj2.hashCode()) {
                    return -1;
                } else return 1;
            } else if (obj1.getPriority() < obj2.getPriority()) {
                return -1;
            } else {
                return 1;
            }
        } else if (obj1.getTime() < obj2.getTime()) {
            return -1;
        } else {
            return 1;
        }
    }
}
