package org.apache.synapse.rules;

import junit.framework.TestCase;
import org.apache.axis2.om.OMElement;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:27:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseRuleReaderTest extends TestCase {

    public void testRuleReader() throws Exception {
        SynapseRuleReader reader = new SynapseRuleReader();
        OMElement readDoc = reader.readRules();
        assertNotNull(readDoc);

        reader.populateRules();

        Iterator ite = null;

        assertNotNull(ite = reader.getRulesIterator());

        while (ite.hasNext()) {
            SynapaseRuleBean bean = (SynapaseRuleBean)ite.next();
            assertEquals("*",bean.getCondition());
            assertEquals("Log",bean.getMediator());
        }


    }
}
