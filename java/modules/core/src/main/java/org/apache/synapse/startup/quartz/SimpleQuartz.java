package org.apache.synapse.startup.quartz;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseException;
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
public class SimpleQuartz implements Startup {
	private static final Log log = LogFactory.getLog(SimpleQuartz.class);
	private static final int THREADPOOLSIZE = 5;

	private String cron;

	private int repeatCount;

	private long repeatInterval;

	private boolean simple; // true means use repeat, false means use cron

	private String className;

//	private SynapseEnvironment synapseEnvironment;

	private Scheduler sch;

	Set xmlProperties = new HashSet();

	public QName getTagQName() {
		return SimpleQuartzFactory.JOB;
	}

	public void destroy() {
		if (sch != null) {
			try {
				sch.shutdown();
			} catch (SchedulerException e) {
				throw new SynapseException(e);
			}
		}

	}

	public void init(SynapseEnvironment synapseEnvironment) {
		
		//this.synapseEnvironment = synapseEnvironment;
		try {
			DirectSchedulerFactory.getInstance().createVolatileScheduler(
					THREADPOOLSIZE);
			sch = DirectSchedulerFactory.getInstance().getScheduler();
			Trigger trigger = null;
			if (simple) {
				
				trigger = TriggerUtils.makeImmediateTrigger(repeatCount, repeatInterval);
			} else {
				CronTrigger cronTrig = new CronTrigger();
				cronTrig.setCronExpression(cron);
				trigger = cronTrig;
			}
			// give the trigger a random name
			trigger.setName("Trigger"+String.valueOf((new Random()).nextLong()));
			trigger.setGroup("synapse.simple.quartz");
			trigger.setVolatility(true);
			JobDetail jobDetail = new JobDetail();
			// Give the job a random name 
			jobDetail.setName("Job"+String.valueOf((new Random()).nextLong()));
			jobDetail.setGroup("synapse.simple.quartz");
			jobDetail.setJobClass(SimpleQuartzJob.class);
			JobDataMap jdm = new JobDataMap();
			jdm.put(SimpleQuartzJob.SYNAPSEENVIRONMENT, synapseEnvironment);
			jdm.put(SimpleQuartzJob.CLASSNAME, className);
			jdm.put(SimpleQuartzJob.PROPERTIES, xmlProperties);
			jobDetail.setJobDataMap(jdm);
			sch.scheduleJob(jobDetail, trigger);
			sch.start();
			log.info("Scheduled job "+jobDetail.getFullName()+" for class "+className);

		} catch (Exception e) {
			throw new SynapseException("Problem with startup of Scheduler ", e);
		}

	}

	public String getJobClass() {
		return className;
	}
	
	public void setJobClass(String attributeValue) {
		className = attributeValue;

	}

	public void setSimple(boolean b) {
		simple = b;
	}
	
	public boolean isSimple() {
		return simple;
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

}
