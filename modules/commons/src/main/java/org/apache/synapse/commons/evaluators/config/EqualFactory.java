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

import org.apache.synapse.commons.evaluators.Evaluator;
import org.apache.synapse.commons.evaluators.EvaluatorException;
import org.apache.synapse.commons.evaluators.EqualEvaluator;
import org.apache.synapse.commons.evaluators.EvaluatorConstants;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * <p> This factory creates a {@link EqualEvaluator}. This factory expects the
 * following XML configuration.</p>
 * <pre>
 * &lt;equal type=&quot;header | param | url&quot; source=&quot;&quot; value=&quot;&quot;/&gt;
 * </pre>
 */
public class EqualFactory implements EvaluatorFactory {
    private Log log = LogFactory.getLog(EqualFactory.class);

    public Evaluator create(OMElement e) throws EvaluatorException {
        EqualEvaluator equal = new EqualEvaluator();

        OMAttribute typeAttr = e.getAttribute(new QName(EvaluatorConstants.TYPE));

        int type = 3;

        if (typeAttr != null) {
            String value = typeAttr.getAttributeValue();
            if (value.equals(EvaluatorConstants.HEADER)) {
                type = 3;
            } else if (value.equals(EvaluatorConstants.PARAM)) {
                type = 2;
            } else if (value.equals(EvaluatorConstants.URL)) {
                type = 1;
            }
        }

        equal.setType(type);

        OMAttribute sourceAttr = e.getAttribute(new QName(EvaluatorConstants.SOURCE));
        if (type != 1 && sourceAttr == null) {
            handleException(EvaluatorConstants.SOURCE + " attribute is required");
            return null;
        }

        equal.setSource(sourceAttr.getAttributeValue());

        OMAttribute valueAttr = e.getAttribute(new QName(EvaluatorConstants.VALUE));

        if (valueAttr == null) {
            handleException(EvaluatorConstants.VALUE + " attribute is required");
            return null;
        }

        equal.setValue(valueAttr.getAttributeValue());
        return equal;
    }

    private void handleException(String message) throws EvaluatorException {
        log.error(message);
        throw new EvaluatorException(message);
    }
}
