package org.apache.synapse.rules;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axis2.om.xpath.AXIOMXPath;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.SynapseConstants;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.SimpleNamespaceContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:12:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class SynapseRuleConfiguration {
    /**
     * this is the reference parameter to the MessageReceiver that
     * should be used
     */
    private String operationName = "receiver";

    private SynapseRuleReader ruleReader;

    private ArrayList mediatorList;

    private ArrayList generalRuleList; // contains the beans of Rules
    private ArrayList xpathRuleList;   //contains the beans of Rules

    public SynapseRuleConfiguration() {
        ruleReader = new SynapseRuleReader();
        mediatorList = new ArrayList();
        generalRuleList = new ArrayList();
        xpathRuleList = new ArrayList();
    }

    public void ruleConfiguration(MessageContext msgCtx) throws AxisFault {
        AxisConfiguration registry = msgCtx.getSystemContext()
                .getAxisConfiguration();

        HashMap serviceMap = registry.getServices();

        ruleReader.populateRules();

        Iterator ite = serviceMap.entrySet().iterator();
        Map.Entry entry = null;
        String key = null;
        while (ite.hasNext()) {
            entry = (Map.Entry) ite.next();
            key = (String) entry.getKey();

            Iterator iterator = ruleReader.getRulesIterator();

            while (iterator.hasNext()) {
                SynapaseRuleBean bean = (SynapaseRuleBean) iterator.next();

                if (bean.getMediator().equalsIgnoreCase(key)) {
                    this.mediatorList.add(key);
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

    public ArrayList getArrayofMediatorList() {
        return mediatorList;
    }

    public ArrayList getArrayofGeneralRuleList() {
        return generalRuleList;
    }

    public ArrayList getArrayofXpathRuleList() {
        return xpathRuleList;
    }

    public void validateXpath(MessageContext messageContext)
            throws JaxenException {
        Iterator ruleIte = ruleReader.getRulesIterator();

        while (ruleIte.hasNext()) {
            // genertal rule handling with "*"
            SynapaseRuleBean bean = (SynapaseRuleBean) ruleIte.next();

            if (bean.getCondition().equals("*")) {
                // this could be more than this.
                this.generalRuleList.add(bean);
            } else {
                // which deal with the xpath of the message
                String xpathExpression = bean.getCondition();
                XPath xpath = new AXIOMXPath(xpathExpression);

                //settingup the namespace which needed to be dealt with care
                SimpleNamespaceContext nameSpace = new SimpleNamespaceContext();
                //add the relevent name spaces
                xpath.setNamespaceContext(nameSpace);

                boolean xpathBool = xpath.booleanValueOf(nameSpace);

                if (xpathBool) {
                    this.xpathRuleList.add(bean);
                }
            }
        }
        messageContext.setProperty(
                SynapseConstants.SynapseRuleEngine.GENERAT_RULE_ARRAY_LIST,
                generalRuleList);
        messageContext.setProperty(
                SynapseConstants.SynapseRuleEngine.XPATH_RULE_ARRAY_LIST,
                xpathRuleList);
    }

}
