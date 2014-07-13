package org.apache.synapse.startup.quartz;

import java.util.Iterator;
import java.util.Set;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.startup.Task;
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.synapse.core.SynapseEnvironment;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SimpleQuartzJob implements Job {
    public static final String
        SYNAPSE_ENVIRONMENT = "SynapseEnvironment",
        CLASSNAME = "ClassName",
        PROPERTIES = "Properties";
    private static final Log log = LogFactory.getLog(SimpleQuartzJob.class);

    public void execute(JobExecutionContext ctx) throws JobExecutionException {

        log.debug("Executing task : " + ctx.getJobDetail().getFullName());
        JobDataMap jdm = ctx.getMergedJobDataMap();
        String jobClassName = (String) jdm.get(CLASSNAME);
        if (jobClassName == null) {
            handleException("No " + CLASSNAME + " in JobDetails");
        }

        Task task = null;
        try {
            task = (Task) getClass().getClassLoader().loadClass(jobClassName).newInstance();
        } catch (Exception e) {
            handleException("Cannot instantiate task : " + jobClassName, e);
        }

        Set properties = (Set) jdm.get(PROPERTIES);
        Iterator it = properties.iterator();
        while (it.hasNext()) {
            OMElement prop = (OMElement) it.next();
            log.debug("Found Property : " + prop.toString());
            PropertyHelper.setStaticProperty(prop, task);
        }

        SynapseEnvironment se = (SynapseEnvironment) jdm.get("SynapseEnvironment");
        if (task instanceof ManagedLifecycle) {
            if (se != null) {
                ((ManagedLifecycle) task).init(se);
            }
        }

        if (se.isInitialized()) {
            task.execute();
        }
    }

    private void handleException(String msg) throws JobExecutionException {
        log.error(msg);
        throw new JobExecutionException(msg);
    }

    private void handleException(String msg, Exception e) throws JobExecutionException {
        log.error(msg, e);
        throw new JobExecutionException(msg, e);
    }

}
