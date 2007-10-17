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

package org.apache.synapse.startup.quartz;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.StartupSerializer;
import org.apache.synapse.config.xml.XMLConfigConstants;

import javax.xml.namespace.QName;

public class SimpleQuartzSerializer implements StartupSerializer {

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS
        = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");
    protected static final OMNamespace nullNS
        = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");
    protected static final QName PROP_Q
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property", "syn");

    public OMElement serializeStartup(OMElement parent, Startup s) {

        if (!(s instanceof SimpleQuartz)) {
            throw new SynapseException("called TaskSerializer on some other " +
                    "kind of startup" + s.getClass().getName());
        }

        SimpleQuartz sq = (SimpleQuartz) s;

        OMElement task = fac.createOMElement("task", synNS, parent);
        task.addAttribute("name", sq.getName(), nullNS);
        task.addAttribute("class", sq.getJobClass(), nullNS);

        OMElement el = fac.createOMElement("trigger", synNS, task);
        if (sq.getInterval() == 1 && sq.getCount() == 1) {
            el.addAttribute("once", "true", nullNS);
        } else if (sq.getCron() != null) {
            el.addAttribute("cron", sq.getCron(), nullNS);
        } else {
            if (sq.getCount() != -1) {
                el.addAttribute("count", Integer.toString(sq.getCount()), nullNS);
            }

            if (sq.getInterval() != 0) {
                el.addAttribute("interval", Long.toString(sq.getInterval()), nullNS);
            }
        }
        
        for (Object o : sq.getProperties()) {
            OMElement prop = (OMElement) o;
            task.addChild(prop.cloneOMElement());
        }

        return task;
    }

}
