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

package org.apache.synapse.mediators.bsf.convertors;

import java.io.ByteArrayInputStream;

import javax.script.ScriptEngine;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;

/**
 * OMElementConvertor for Ruby scripts
 * 
 * TODO: Right now this goes via Strings and likely isn't very fast
 * There could well be much better ways to do this :)
 */
public class RBOMElementConvertor implements OMElementConvertor {

    private static final Log log = LogFactory.getLog(RBOMElementConvertor.class);

    protected ScriptEngine bsfEngine;

    public Object toScript(OMElement omElement) {

        try {
            StringBuffer srcFragment = new StringBuffer("Document.new(<<EOF\n");
            srcFragment.append(omElement.toString());
            srcFragment.append("\nEOF\n");
            srcFragment.append(")");

            if (bsfEngine == null) {
                handleException("Cannot convert OMElement to Ruby Object as BSF Engine is not set");
            }
            return bsfEngine.eval(srcFragment.toString());

        } catch (Exception e) {
            handleException("Error converting OMElement to Ruby Object", e);
        }
        return null;
    }

    public OMElement fromScript(Object o) {
        if (o == null) {
            handleException("Cannot convert null Ruby Object to an OMElement");
        }

        try {
            byte[] xmlBytes = o.toString().getBytes();
            StAXOMBuilder builder = new StAXOMBuilder(new ByteArrayInputStream(xmlBytes));
            return builder.getDocumentElement();
        } catch (Exception e) {
            handleException("Error converting Ruby object of type : " + o.getClass().getName() +
                " to an OMElement", e);
        }
        return null;
    }

    public void setEngine(ScriptEngine e) {
        this.bsfEngine = e;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }
}
