package org.apache.synapse.commons.evaluators.config;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.synapse.commons.evaluators.MatchEvaluator;
import org.apache.synapse.commons.evaluators.EvaluatorConstants;
import org.apache.synapse.commons.evaluators.source.SourceTextRetriever;
import org.apache.synapse.commons.evaluators.source.HeaderTextRetriever;
import org.apache.synapse.commons.evaluators.source.ParameterTextRetriever;

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

        SourceTextRetriever textRetriever = matchEvaluator.getTextRetriever();
        if (textRetriever instanceof HeaderTextRetriever) {
            matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.HEADER));
            HeaderTextRetriever headerTextRetriever = (HeaderTextRetriever) textRetriever;
            addSourceAttribute(headerTextRetriever.getSource(), matchElement);

        } else if (textRetriever instanceof ParameterTextRetriever) {
            matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.PARAM));
            ParameterTextRetriever paramTextRetriever = (ParameterTextRetriever) textRetriever;
            addSourceAttribute(paramTextRetriever.getSource(), matchElement);

        } else {
            matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.URL));
        }

        matchElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.REGEX, nullNS,
                matchEvaluator.getRegex().toString()));

        if (parent != null) {
            parent.addChild(matchElement);
        }

        return matchElement;
    }

    private void addSourceAttribute(String source, OMElement equalElement)
            throws EvaluatorException {

        if (source != null) {
            equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.SOURCE, nullNS,
                    source));
        } else {
            String msg = "If type is not URL a source value should be specified for " +
                            "the match evaluator";
            log.error(msg);
            throw new EvaluatorException(msg);
        }
    }
}
