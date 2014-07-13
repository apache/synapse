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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

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

/**
 * &lt;task class="org.my.synapse.Task" name="string"&gt;
 *  &lt;property name="stringProp" value="String"/&gt;
 *  &lt;property name="xmlProp"&gt;
 *   &lt;somexml&gt;config&lt;/somexml&gt;
 *  &lt;/property&gt;
 *  &lt;trigger ([[count="10"]? interval="1000"] | [cron="0 * 1 * * ?"] | [once=(true | false)])/&gt;
 * &lt;/task&gt;
 */
public class SimpleQuartzFactory implements StartupFactory {

    public final static QName TASK
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "task");

    private final static QName TRIGGER
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "trigger");

    private final static QName PROPERTY
        = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "property");

    private final static Log log = LogFactory.getLog(SimpleQuartzFactory.class);

    public Startup createStartup(OMElement el) {
        
        if (log.isDebugEnabled()) {
            log.debug("Creating SimpleQuartz Task");
        }
        
        if (el.getQName().equals(TASK)) {
            
            SimpleQuartz q = new SimpleQuartz();

            String name = el.getAttributeValue(
                new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));
            if (name != null) {
                q.setName(name);
            } else {
                handleException("Name for a task is required, missing name in the task");
            }

            // set the task class
            OMAttribute classAttr = el.getAttribute(new QName("class"));
            if (classAttr != null && classAttr.getAttributeValue() != null) {
                String classname = classAttr.getAttributeValue();
                try {
                    Class.forName(classname).newInstance();
                } catch (Exception e) {
                    handleException("Failed to load task class " + classname, e);
                }
                q.setJobClass(classname);
            } else {
                handleException("Syntax error in the Task : no task class specified");
            }
            
            // set pinned server list
            OMAttribute pinnedServers = el.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "pinnedServers"));
            if (pinnedServers != null) {
                String pinnedServersValue = pinnedServers.getAttributeValue();
                if (pinnedServersValue == null) {
                    // default to all servers
                } else {
                    StringTokenizer st = new StringTokenizer(pinnedServersValue, " ,");
                    List pinnedServersList = new ArrayList();
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if (token.length() != 0) {
                          pinnedServersList.add(token);
                        }
                    }
                    q.setPinnedServers(pinnedServersList);
                }
            }

            // next sort out the property children
            Iterator it = el.getChildrenWithName(PROPERTY);
            while (it.hasNext()) {
                OMElement prop = (OMElement) it.next();
                if (PropertyHelper.isStaticProperty(prop)) {
                    q.addProperty(prop);
                } else {
                    handleException("Tasks does not support dynamic properties");
                }
            }

            // setting the trigger to the task
            OMElement trigger = el.getFirstChildWithName(TRIGGER);
            if (trigger != null) {

                OMAttribute count = trigger.getAttribute(new QName("count"));
                if (count != null) {
                    try {
                        q.setCount(Integer.parseInt(count.getAttributeValue()));
                    } catch (Exception e) {
                        handleException("Failed to parse trigger count as an integer", e);
                    }
                }

                OMAttribute once = trigger.getAttribute(new QName("once"));
                if (once != null && Boolean.TRUE.toString().equals(once.getAttributeValue())) {
                    q.setCount(1);
                    q.setInterval(1);
                }

                OMAttribute repeatInterval = trigger.getAttribute(new QName("interval"));
                if (repeatInterval == null && q.getCount() > 1) {
                    handleException("Trigger seems to be " +
                        "a simple trigger, but no interval specified");
                } else if (repeatInterval != null && repeatInterval.getAttributeValue() != null) {
                    try {
                        long repeatIntervalInSeconds = Long.parseLong(repeatInterval.getAttributeValue());
                        long repeatIntervalInMillis = repeatIntervalInSeconds * 1000;
                        q.setInterval(repeatIntervalInMillis);
                    } catch (Exception e) {
                        handleException("Failed to parse trigger interval as a long value", e);
                    }
                }

                OMAttribute expr = trigger.getAttribute(new QName("cron"));
                if (expr == null && q.getInterval() == 0) {
                    q.setCount(1);
                    q.setInterval(1);
                } else if (expr != null && q.getInterval() > 0) {
                    handleException("Trigger syntax error : " +
                        "both cron and simple trigger attributes are present");
                } else if (expr != null && expr.getAttributeValue() != null) {
                    q.setCron(expr.getAttributeValue());
                }

            } else {
                q.setCount(1);
                q.setInterval(1);
            }

            return q;
        } else {
            handleException("Syntax error in the task : wrong QName for the task");
            return null;
        }
    }

    public Class getSerializerClass() {
        return SimpleQuartzSerializer.class;
    }

    public QName getTagQName() {
        return TASK;
    }

    private void handleException(String message, Exception e) {
        log.error(message);
        throw new SynapseException(message, e);
    }

    private void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }

}
