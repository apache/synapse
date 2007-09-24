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

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.StartupSerializer;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseException;

public class SimpleQuartzSerializer implements StartupSerializer {

    public void serializeStartup(OMElement parent, Startup s) {

        if (!(s instanceof SimpleQuartz)) {
            throw new SynapseException("called SimpleQuartzSerializer on some other " +
                    "kind of startup" + s.getClass().getName());
        }

        SimpleQuartz sq = (SimpleQuartz) s;
        OMFactory fac = parent.getOMFactory();
        OMNamespace nullNS = fac.createOMNamespace("", "");
        OMNamespace synNS = fac.createOMNamespace(XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");

        OMElement job = fac.createOMElement("job", synNS, parent);
        job.addAttribute("class", sq.getJobClass(), nullNS);

        if (sq.isSimple()) {
            OMElement el = fac.createOMElement("simpletrigger", synNS, job);
            if (sq.getCount() == -1) {
                el.addAttribute("forever", "true", nullNS);
            } else {
                el.addAttribute("count", Integer.toString(sq.getCount()), nullNS);
            }
            el.addAttribute("interval", Long.toString(sq.getInterval()), nullNS);
        } else {
            OMElement el = fac.createOMElement("crontrigger", synNS, job);
            el.addAttribute("expression", sq.getCron(), nullNS);
        }

        Iterator it = sq.getProperties().iterator();
        while (it.hasNext()) {
            OMElement prop = (OMElement) it.next();
            job.addChild(prop.cloneOMElement());
        }

    }

}
