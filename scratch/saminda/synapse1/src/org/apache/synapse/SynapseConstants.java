package org.apache.synapse;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 12, 2005
 * Time: 2:27:49 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SynapseConstants {

    public static final String MEDEATOT_STATE = "mediatorState";
    public static final String VALUE_TRUE="true";
    public static final String VALUE_FALSE="false";
    public static final String SYNAPSE_STATE="state";
    public static final String RULE_STATE="ruleState";

    public interface SynapseRuleReader {
        public static final String CONDITION = "condition";
        public static final String MEDIATOR = "mediator";
    }
    public interface SynapseRuleEngine {
        public static final String GENERAT_RULE_ARRAY_LIST = "generalRules";
        public static final String XPATH_RULE_ARRAY_LIST= "xpathRules";
        public static final String SYNAPSE_RECEIVER = "receiver";
        public static final String SYNAPSE_RULE_ENGINE= "synapseRuleEngine";
    }
}
