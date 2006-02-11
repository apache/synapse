package org.apache.synapse.processors;

import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;
import org.apache.synapse.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 */
public class RefDefineProcessor extends AbstractProcessor{
    private Log log = LogFactory.getLog(getClass());
	private String refDefine = null;

    public boolean process(SynapseEnvironment se, SynapseMessage sm) {
        log.debug("RefDfineProcessor-Process");
        Processor p = se.lookupProcessor(getRefDefine());
		if (p==null) log.debug("processor with name "+this.getRefDefine()+" not found");
		else if (p instanceof DefineProcessor) {
            DefineProcessor defp = (DefineProcessor)p;
            return defp.processRef(se,sm);
        }
		return true;
    }

    public void setRefDefine(String refDefine) {
        this.refDefine = refDefine;
    }

    public String getRefDefine() {
        return this.refDefine;
    }

}
