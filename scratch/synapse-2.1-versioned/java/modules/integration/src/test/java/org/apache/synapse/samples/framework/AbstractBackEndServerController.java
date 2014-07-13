package org.apache.synapse.samples.framework;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

public abstract class AbstractBackEndServerController implements ProcessController {

    protected String serverName;

    public AbstractBackEndServerController(OMElement element) {
        serverName = SynapseTestUtils.getAttribute(element,
                SampleConfigConstants.ATTR_SERVER_ID, SampleConfigConstants.DEFAULT_SERVER_ID);
    }

    public String getServerName() {
        return serverName;
    }
}
