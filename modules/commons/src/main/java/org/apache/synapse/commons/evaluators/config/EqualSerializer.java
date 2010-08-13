/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.commons.evaluators.config;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.commons.evaluators.*;
import org.apache.synapse.commons.evaluators.source.SourceTextRetriever;
import org.apache.synapse.commons.evaluators.source.HeaderTextRetriever;
import org.apache.synapse.commons.evaluators.source.ParameterTextRetriever;
import org.apache.synapse.commons.evaluators.source.URLTextRetriever;

import javax.xml.namespace.QName;

/**
 * Serialize the {@link EqualEvaluator} to the XML configuration defined in
 * the {@link EqualFactory}. 
 */
public class EqualSerializer extends AbstractEvaluatorSerializer{

    public OMElement serialize(OMElement parent, Evaluator evaluator) throws EvaluatorException {

        if (!(evaluator instanceof EqualEvaluator)) {
            throw new IllegalArgumentException("Evalutor should be a EqualEvalutor");
        }

        EqualEvaluator equalEvaluator = (EqualEvaluator) evaluator;
        OMElement equalElement = fac.createOMElement(new QName(EvaluatorConstants.EQUAL));

        SourceTextRetriever textRetriever = equalEvaluator.getTextRetriever();
        if (textRetriever instanceof HeaderTextRetriever) {
            equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.HEADER));
            HeaderTextRetriever headerTextRetriever = (HeaderTextRetriever) textRetriever;
            addSourceAttribute(headerTextRetriever.getSource(), equalElement);

        } else if (textRetriever instanceof ParameterTextRetriever) {
            equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.PARAM));
            ParameterTextRetriever paramTextRetriever = (ParameterTextRetriever) textRetriever;
            addSourceAttribute(paramTextRetriever.getSource(), equalElement);

        } else {
            equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.TYPE, nullNS,
                    EvaluatorConstants.URL));
            URLTextRetriever urlTextRetriever = (URLTextRetriever) textRetriever;
            if (urlTextRetriever.getFragment() != null) {
                equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.FRAGMENT,
                        nullNS, urlTextRetriever.getFragment()));
            }
        }

        equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.VALUE, nullNS,
                equalEvaluator.getValue()));

        if (parent != null) {
            parent.addChild(equalElement);
        }

        return equalElement;
    }

    private void addSourceAttribute(String source, OMElement equalElement)
            throws EvaluatorException {

        if (source != null) {
            equalElement.addAttribute(fac.createOMAttribute(EvaluatorConstants.SOURCE, nullNS,
                    source));
        } else {
            String msg = "If type is not URL a source value should be specified for " +
                            "the equal evaluator";
            log.error(msg);
            throw new EvaluatorException(msg);
        }
    }
}
