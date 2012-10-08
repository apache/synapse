package org.apache.synapse.libraries.eip;

import junit.framework.TestCase;
import org.apache.synapse.libraries.imports.SynapseImport;
import org.apache.synapse.libraries.model.Library;
import org.apache.synapse.libraries.util.LibDeployerUtils;
import org.apache.synapse.mediators.eip.AbstractSplitMediatorTestCase;

import java.io.File;
import java.net.URISyntaxException;

public abstract class AbstractEipLibTestCase extends TestCase {
    public String path = null;

    protected String getResourcePath() {
        try {
            if (path == null) {
                path = new File("./target/test_repos/synapse/synapse-libraries/synapse-eiptest-lib.zip").getAbsolutePath();
            }
        } catch (Exception e) {
            return null;
        }
        return path;

    }
}
