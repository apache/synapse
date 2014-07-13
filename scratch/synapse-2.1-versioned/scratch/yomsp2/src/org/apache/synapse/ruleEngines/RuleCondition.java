package org.apache.synapse.ruleEngines;

import org.apache.axis2.context.SOAPMessageContext;



public interface RuleCondition {

	boolean matches(SOAPMessageContext smc);

}
