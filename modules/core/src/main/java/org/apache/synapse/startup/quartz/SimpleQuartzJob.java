package org.apache.synapse.startup.quartz;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.config.xml.PropertyHelper;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.startup.Task;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Set;

public class SimpleQuartzJob implements Job {
    public static final String SYNAPSE_ENVIRONMENT = "SynapseEnvironment";
    public static final String CLASSNAME = "ClassName";
    public static final String PROPERTIES = "Properties";

    private static final Log log = LogFactory.getLog(SimpleQuartzJob.class);

    public void execute(JobExecutionContext ctx) throws JobExecutionException {

        String jobName = ctx.getJobDetail().getFullName();
        if (log.isDebugEnabled()) {
            log.debug("Executing task : " + jobName);
        }
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
        for (Object property : properties) {
            OMElement prop = (OMElement) property;
            log.debug("Found Property : " + prop.toString());
            PropertyHelper.setStaticProperty(prop, task);
        }

        // 1. Initialize
        SynapseEnvironment se = (SynapseEnvironment) jdm.get("SynapseEnvironment");
        if (task instanceof ManagedLifecycle && se != null) {
            ((ManagedLifecycle) task).init(se);
        }

        // 2. Execute
        if (se != null && task != null && se.isInitialized()) {
            task.execute();
        }

        // 3. Destroy
        if (task instanceof ManagedLifecycle && se != null) {
            ((ManagedLifecycle) task).destroy();
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
