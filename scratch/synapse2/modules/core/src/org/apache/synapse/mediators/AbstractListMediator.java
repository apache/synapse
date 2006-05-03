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

package org.apache.synapse.mediators;

import org.apache.synapse.SynapseMessage;
import org.apache.synapse.api.Mediator;
import org.apache.synapse.api.ListMediator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractListMediator extends AbstractMediator implements ListMediator {

    protected List mediators = new ArrayList();

    public boolean mediate(SynapseMessage synMsg) {
        log.debug(getType() + " mediate()");

        Iterator it = mediators.iterator();
        while (it.hasNext()) {
            Mediator m = (Mediator) it.next();
            if (!m.mediate(synMsg)) {
                return false;
            }
        }
        return true;
    }

    public List getList() {
        return mediators;
    }

    public boolean addChild(Mediator m) {
        return mediators.add(m);
    }

    public Mediator getChild(int pos) {
        return (Mediator) mediators.get(pos);
    }

    public boolean removeChild(Mediator m) {
        return mediators.remove(m);
    }

    public Mediator removeChild(int pos) {
        return (Mediator) mediators.remove(pos);
    }
}
