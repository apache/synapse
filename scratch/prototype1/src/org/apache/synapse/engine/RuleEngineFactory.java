package org.apache.synapse.engine;

public class RuleEngineFactory {

    public RuleSelector getRuleEngine(Rule[] rules){
        return new RuleSelectorImpl(rules);
    }

}
