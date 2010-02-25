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

package org.apache.synapse.commons.evaluators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.UnsupportedEncodingException;

/**
 * This evaluator uses regular expressions to match a given HTTP request.
 *
 * syntax:
 * <match type="header | param | url" source="" regex=""/>
 *
 * The source is used to extract the HTTP header or URL param
 */
public class MatchEvaluator implements Evaluator {

    private Log log = LogFactory.getLog(MatchEvaluator.class);

    private int type = 3;

    private String source = null;

    private Pattern regex = null;

    public boolean evaluate(EvaluatorContext context) throws EvaluatorException {
        String sourceText = null;

        if (type == 1) {
            sourceText = context.getUrl();
        } else if (type == 2) {
            try {
                sourceText = context.getParam(source);
            } catch (UnsupportedEncodingException e) {
                handleException("Error retrieving paramter: " + source);
            }
        } else if (type == 3) {
            sourceText = context.getHeader(source);
        }

        if (sourceText == null) {
            return false;
        }

        Matcher matcher = regex.matcher(sourceText);
        return matcher.matches();
    }

    public String getName() {
        return EvaluatorConstants.MATCH;
    }

    public int getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public Pattern getRegex() {
        return regex;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    private void handleException(String message) throws EvaluatorException {
        log.error(message);
        throw new EvaluatorException(message);
    }
}
