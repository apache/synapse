package org.apache.synapse.commons.evaluators.config;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.synapse.commons.evaluators.MatchEvaluator;
import org.apache.synapse.commons.evaluators.EvaluatorConstants;

import javax.xml.namespace.QName;

/**
 * Serialize the {@link MatchEvaluator} to the XML configuration defined in
 * the {@link MatchFactory}. 
 */
public class MatchSerializer extends AbstractEvaluatorSerializer {

    public OMElement serialize(OMElement parent, Evaluator evaluator) throws EvaluatorException {
        if (!(evaluator instanceof MatchEvaluator)) {
            throw new IllegalArgumentException("Evalutor should be a NotEvalutor");
        }

        MatchEvaluator matchEvaluator = (MatchEvaluator) evaluator;
        OMElement matchElement = fac.createOMElement(new QName(EvaluatorConstants.MATCH));

        if (matchEvaluator.getType() != EvaluatorConstants.TYPE_URL) {
            if (matchEvaluator.getSource() != null) {
                matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.SOURCE, nullNS,
                        matchEvaluator.getSource()));
            } else {
                String msg = "If type is not URL a source value should be specified for " +
                        "the match evaluator";
                log.error(msg);
                throw new EvaluatorException(msg);
            }
        }

        if (matchEvaluator.getType() == EvaluatorConstants.TYPE_URL) {
            matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.URL));
        } else if (matchEvaluator.getType() == EvaluatorConstants.TYPE_PARAM) {
            matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.PARAM));
        } else if (matchEvaluator.getType() == EvaluatorConstants.TYPE_HEADER) {
            matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.HEADER));
        } else {
            String msg = "Unsupported type value: " + matchEvaluator.getType();
            log.error(msg);
            throw new EvaluatorException(msg);
        }

        matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.REGEX, nullNS,
                matchEvaluator.getRegex().toString()));

        if (parent != null) {
            parent.addChild(matchElement);
        }

        return matchElement;
    }
}
