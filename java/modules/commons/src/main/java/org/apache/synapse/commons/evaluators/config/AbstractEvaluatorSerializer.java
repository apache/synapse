package org.apache.synapse.commons.evaluators.config;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provide common methods for {@link EvaluatorSerializer} implementations
 */
public abstract class AbstractEvaluatorSerializer implements EvaluatorSerializer {
    protected static Log log;

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();

    protected static final OMNamespace nullNS = fac.createOMNamespace("", "");
    
    /**
     * A constructor that makes subclasses pick up the correct logger
     */
    protected AbstractEvaluatorSerializer() {
        log = LogFactory.getLog(this.getClass());
    }

    protected void serializeChildren(OMElement parent, Evaluator[] childEvaluators)
            throws EvaluatorException {
        for (Evaluator evaluator : childEvaluators) {
            String name = evaluator.getName();

            EvaluatorSerializer serializer = EvaluatorSerializerFinder.
                    getInstance().getSerializer(name);

            if (serializer != null) {
                serializer.serialize(parent, evaluator);
            } else {
                String msg = "Couldn't find the serializer for evaliator: " + name;
                log.error(msg);
                throw new EvaluatorException(msg);
            }
        }
    }

    protected void serializeChild(OMElement parenet, Evaluator child) throws EvaluatorException {
        EvaluatorSerializer serializer =
                EvaluatorSerializerFinder.getInstance().getSerializer(child.getName());

        if (serializer != null) {
            serializer.serialize(parenet, child);
        } else {
            String msg = "Couldn't find the serializer for evaliator: " + child.getName();
            log.error(msg);
            throw new EvaluatorException(msg);
        }
    }
}
