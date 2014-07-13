package org.apache.synapse.ruleEngines;

import org.apache.synapse.api.SOAPMessageContext;





public interface RuleCondition {

	boolean matches(SOAPMessageContext smc);

}
