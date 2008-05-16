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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.ServerManager;
import org.apache.synapse.startup.AbstractStartup;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.DirectSchedulerFactory;

/*
 * This class is instantiated by SimpleQuartzFactory (or by hand)
 * When it is initialized it creates a Quartz Scheduler with a job and a trigger
 * The class it starts is always an instance of SimpleQuartzJob
 * SimpleQuartzJob is there to set the properties and start the actual business-logic class
 * It wraps up any properties that the job needs as in the JobDetail and JDMap
 */
public class SimpleQuartz extends AbstractStartup {

    private static final Log log = LogFactory.getLog(SimpleQuartz.class);
    private static final int THREADPOOLSIZE = 5;

    static {
      try {
        DirectSchedulerFactory.getInstance().createVolatileScheduler(THREADPOOLSIZE);
      } catch (SchedulerException e) {
        throw new SynapseException("Error initializing scheduler factory", e);
      }
    }
    
    private String cron;
    private int repeatCount = -1;
    private long repeatInterval; // in milliseconds
    private String className;
    private List pinnedServers;
    private Scheduler sch;
    Set xmlProperties = new HashSet();

    public QName getTagQName() {
        return SimpleQuartzFactory.TASK;
    }

    public void destroy() {
        if (sch != null) {
            try {
                sch.shutdown();
            } catch (SchedulerException e) {
                log.warn("Error shutting down scheduler", e);
                throw new SynapseException("Error shutting down scheduler", e);
            }
        }
    }

    public void init(SynapseEnvironment synapseEnvironment) {

        // this server name given by system property SynapseServerName
        // otherwise take host-name
        // else assume localhost
        String thisServerName = ServerManager.getInstance().getServerName();
        if(thisServerName == null || thisServerName.equals("")) {
          try {
            InetAddress addr = InetAddress.getLocalHost();
            thisServerName = addr.getHostName();
  
          } catch (UnknownHostException e) {
            log.warn("Could not get local host name", e);
          }
          
          if(thisServerName == null || thisServerName.equals("")) {
            thisServerName = "localhost";
          }
        }
        log.debug("Synapse server name : " + thisServerName);
        
        // start proxy service if either,
        // pinned server name list is empty
        // or pinned server list has this server name
        List pinnedServers = getPinnedServers();
        if(pinnedServers != null && !pinnedServers.isEmpty()) {
          if(!pinnedServers.contains(thisServerName)) {
            log.info("Server name not in pinned servers list. Not starting Task : " + getName());
            return;
          }
        }
      
      
        try {
            sch = DirectSchedulerFactory.getInstance().getScheduler();
            if (sch == null) {
              DirectSchedulerFactory.getInstance().createVolatileScheduler(THREADPOOLSIZE);
              sch = DirectSchedulerFactory.getInstance().getScheduler();
            }
            
            if(sch == null) {
              throw new NullPointerException("Scheduler is null");
            }

            Trigger trigger = null;
            if (cron == null) {
                if (repeatCount >= 0) {
                    trigger = TriggerUtils.makeImmediateTrigger(repeatCount - 1, repeatInterval);
                } else {
                    trigger = TriggerUtils.makeImmediateTrigger(-1, repeatInterval);
                }

            } else {
                CronTrigger cronTrig = new CronTrigger();
                cronTrig.setCronExpression(cron);
                trigger = cronTrig;
            }

            // give the trigger a random name
            trigger.setName("Trigger" + String.valueOf((new Random()).nextLong()));
            trigger.setGroup("synapse.simple.quartz");
            trigger.setVolatility(true);
            JobDetail jobDetail = new JobDetail();

            // Give the job a name
            jobDetail.setName(name);
            jobDetail.setGroup("synapse.simple.quartz");
            jobDetail.setJobClass(SimpleQuartzJob.class);
            JobDataMap jdm = new JobDataMap();
            jdm.put(SimpleQuartzJob.SYNAPSE_ENVIRONMENT, synapseEnvironment);
            jdm.put(SimpleQuartzJob.CLASSNAME, className);
            jdm.put(SimpleQuartzJob.PROPERTIES, xmlProperties);
            jobDetail.setJobDataMap(jdm);

            sch.scheduleJob(jobDetail, trigger);
            sch.start();
            log.info("Scheduled job " + jobDetail.getFullName() + " for class " + className);

        } catch (Exception e) {
            log.fatal("Error starting up Scheduler", e);
            throw new SynapseException("Error starting up Scheduler", e);
        }

    }

    public String getJobClass() {
        return className;
    }

    public void setJobClass(String attributeValue) {
        className = attributeValue;

    }

    public void setInterval(long l) {
        repeatInterval = l;

    }

    public long getInterval() {
        return repeatInterval;
    }

    public void setCount(int i) {
        repeatCount = i;
    }

    public int getCount() {
        return repeatCount;
    }

    public void addProperty(OMElement prop) {
        xmlProperties.add(prop);
    }

    public Set getProperties() {
        return xmlProperties;
    }

    public void setCron(String attributeValue) {
        cron = attributeValue;

    }

    public String getCron() {
        return cron;
    }

    public List getPinnedServers() {
      return pinnedServers;
    }

    public void setPinnedServers(List pinnedServers) {
      this.pinnedServers = pinnedServers;
    }

}
