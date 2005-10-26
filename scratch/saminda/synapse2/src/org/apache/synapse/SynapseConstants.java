package org.apache.synapse;

/**
 * Created by IntelliJ IDEA.
 * User: saminda
 * Date: Oct 18, 2005
 * Time: 10:15:00 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SynapseConstants {
    public static final String MEDEATOT_STATE = "mediatorState";
    public static final String VALUE_TRUE="true";
    public static final String VALUE_FALSE="false";
    public static final String SYNAPSE_STATE="state";
    public static final String RULE_STATE="ruleState";
    public static final String PATH="path";

    public interface SynapseRuleReader {
        public static final String CONDITION = "condition";
        public static final String MEDIATOR = "mediator";
        public static final String NAMESPACE = "ns";
        public static final String PRIFIX = "prifix";
        public static final String URI = "uri";
    }
    public interface SynapseRuleEngine {
        public static final String CUMULATIVE_RUEL_ARRAY_LIST="cumulative";
        public static final String SYNAPSE_RECEIVER = "receiver";
        public static final String SYNAPSE_RULE_ENGINE= "synapseRuleEngine";
    }
}
