package org.apache.synapse.rules;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.AxisFault;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 10, 2005
 * Time: 4:54:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseRuleEngine {

    private String operationName = "receiver";

    private SynapseRuleReader ruleReader;

    private ArrayList ruleList;

    public SynapseRuleEngine() {
        ruleReader = new SynapseRuleReader();
        ruleList = new ArrayList();
    }

    public void ruleConfiguration(MessageContext msgCtx) throws AxisFault {
        AxisConfiguration registry = msgCtx.getSystemContext()
                .getAxisConfiguration();

        HashMap serviceMap = registry.getServices();

        ruleReader.populateRules(registry);

        Iterator ite = serviceMap.entrySet().iterator();
        Map.Entry entry = null;
        String key = null;
        while (ite.hasNext()) {
            entry = (Map.Entry) ite.next();
            key = (String) entry.getKey();

            Iterator iterator = ruleReader.getRulesIterator();

            while (iterator.hasNext()) {
                RuleBean bean = (RuleBean) iterator.next();
                /**
                 * Rulling logic goes here
                 */

                if (bean.getMediatation().equalsIgnoreCase(key)) {
                    this.ruleList.add(key);
                }
            }

        }
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }    

    public ArrayList getArrayList() {
        return ruleList;
    }
}
