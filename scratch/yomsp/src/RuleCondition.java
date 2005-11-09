package org.apache.synapse;

public interface RuleCondition {

	boolean matches(SOAPMessageContext smc);

}
