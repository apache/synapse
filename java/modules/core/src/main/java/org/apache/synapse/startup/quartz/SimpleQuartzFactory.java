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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.synapse.config.xml.StartupFactory;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseException;

/*
 * Namespace = synapse namespace
 * 
 *  &lt;task class="org.my.synapse.Task">
 *  &lt;property name="stringProp" value="String"/>
 *  &lt;property name="xmlProp">
 *  %lt;somexml>config</somexml>
 *  &lt;/property>
 *  &lt;simpletrigger forever="true" count="10" interval="1000"/> 
 *  &lt;!-- forever or count not both -->
 *  &lt;crontrigger expression="0 * 1 * * ?" />
 *  &lt;/task>
 * 
 */

public class SimpleQuartzFactory implements StartupFactory {

    public final static QName TASK = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "task");

    private final static QName SIMPLE
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "simpletrigger");

    private final static QName CRON
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "crontrigger");

    private final static QName PROPERTY
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");

    private final static Log log = LogFactory.getLog(SimpleQuartzFactory.class);

    public Startup createStartup(OMElement el) {
        if (log.isDebugEnabled())
            log.debug("Creating SimpleQuartz startup");
        if (el.getQName().equals(TASK)) {
            SimpleQuartz q = new SimpleQuartz();
            OMAttribute classAttr = el.getAttribute(new QName("class"));
            if (classAttr == null) {
                log.error("No class attribute on element. It is required");
                throw new SynapseException(
                        "Cannot create Quartz Startup - no class attribute");
            }
            // test if we can create the task?
            String classname = classAttr.getAttributeValue();

            // if no package specified then prepend "org.apache.synapse.startup.tasks"
            if (classname.indexOf('.') == -1) {
                classname = "org.apache.synapse.startup.tasks." + classname;
            }
            try {
                getClass().getClassLoader().loadClass(classname).newInstance();
            }
            catch (Exception e) {
                throw new SynapseException("Failed to load task class " + classname, e);
            }
            q.setJobClass(classname);
            // next sort out the property children

            Iterator it = el.getChildrenWithName(PROPERTY);
            while (it.hasNext()) {
                // simply store the properties for now -
                // they will be loaded when the job is actually run
                OMElement prop = (OMElement) it.next();
                if (PropertyHelper.isStaticProperty(prop)) {
                    q.addProperty(prop);

                } else {
                    throw new SynapseException(
                            "Task does not support dynamic properties");
                }
            }

            /* try to handle the simpletrigger approach */
            OMElement trigger = el.getFirstChildWithName(SIMPLE);
            if (trigger != null) {
                q.setSimple(true);
                OMAttribute repeatInterval = trigger.getAttribute(new QName(
                        "interval"));
                if (repeatInterval == null) {
                    log.error("interval attribute must be specified");
                    throw new SynapseException(
                            "No interval attribute specified");
                }
                try {
                    q.setInterval(Long.parseLong(repeatInterval
                            .getAttributeValue()));
                } catch (Exception e) {
                    throw new SynapseException(
                            "Failed to parse interval as long");
                }
                OMAttribute count = trigger.getAttribute(new QName("count"));
                if (count == null) {
                    // if no count set then forever must be set and set to true
                    OMAttribute forever = trigger.getAttribute(new QName(
                            "forever"));

                    if (forever != null) {
                        String fValue = forever.getAttributeValue();
                        if (fValue.toLowerCase().charAt(0) == 't'
                                || fValue.toLowerCase().charAt(0) == '1') {
                            q.setCount(-1);
                        } else {
                            throw new SynapseException(
                                    "count must be set or forever='true'");
                        }
                    } else {
                        throw new SynapseException(
                                "count must be set or forever='true'");
                    }
                } // else count is set
                else {
                    try {
                        q.setCount(Integer.parseInt(count.getAttributeValue()));
                    } catch (Exception e) {
                        throw new SynapseException(
                                "Failed to parse count as integer");
                    }
                }

            } else // should be cron trigger
            {
                trigger = el.getFirstChildWithName(CRON);
                if (trigger == null) {
                    log.error("neither cron nor simpletrigger are set");
                    throw new SynapseException(
                            "neither crontrigger nor simpletrigger child elements exist");
                }
                q.setSimple(false); // cron trigger
                OMAttribute expr = trigger.getAttribute(new QName("expression"));
                if (expr == null) {
                    log.error("crontrigger element must have expression attribute");
                    throw new SynapseException(
                            "crontrigger element must have expression attribute");
                }
                q.setCron(expr.getAttributeValue());
            }

            return q;
        } else {
            log.error("wrong QName!");
            return null;
        }

    }

    public Class getSerializerClass() {
        return SimpleQuartzSerializer.class;
    }

    public QName getTagQName() {
        return TASK;
    }

}
