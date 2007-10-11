package org.apache.synapse.startup.quartz;

import java.util.Iterator;
import java.util.Set;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.synapse.core.SynapseEnvironment;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SimpleQuartzJob implements Job {
	public static final String SYNAPSEENVIRONMENT= "SynapseEnvironment", CLASSNAME="ClassName", PROPERTIES = "Properties";
	private static final Log log = LogFactory.getLog(SimpleQuartzJob.class);
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		log.debug("executing task "+ctx.getJobDetail().getFullName());
		JobDataMap jdm = ctx.getMergedJobDataMap();
		String jobClassName = (String)jdm.get(CLASSNAME);
		if (jobClassName==null) {
			throw new JobExecutionException("No "+CLASSNAME+" in JobDetails");
		}
		org.apache.synapse.startup.Task task =null;
		try {
			task = (org.apache.synapse.startup.Task)getClass().getClassLoader().loadClass(jobClassName).newInstance();
		} catch (Exception e) {
			throw new JobExecutionException("Cannot instantiate task "+jobClassName, e);
		}
		Set properties = (Set)jdm.get(PROPERTIES);
		Iterator it = properties.iterator();
		while (it.hasNext()) {
			OMElement prop = (OMElement)it.next();
			log.debug("found Property"+prop.toString());
			PropertyHelper.setStaticProperty(prop, task);
		}
		SynapseEnvironment se = (SynapseEnvironment)jdm.get("SynapseEnvironment");
		if (task instanceof ManagedLifecycle) {
			if (se!=null) {
				((ManagedLifecycle) task).init(se);
			}
		}
		task.execute();
		
	}

}
