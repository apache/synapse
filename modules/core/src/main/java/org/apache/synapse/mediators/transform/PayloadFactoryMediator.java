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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Payload-factory mediator creates a new SOAP payload for the message, replacing the existing one.
 * <pre>printf()</pre> style formatting is used to configure the transformation performed by the
 * mediator.<p/>
 * Each argument in the mediator configuration could be a static value or an XPath expression.
 * When an expression is used, argument value is fetched at runtime by evaluating the provided XPath
 * expression against the existing SOAP message/message context.
 */
public class PayloadFactoryMediator extends AbstractMediator {

    /**
     * Stores the new payload format.
     */
    private String format;

    /**
     * Stores the argument list, argument values are computed dynamically at mediation time.
     */
    private List<Argument> argumentList = new ArrayList<Argument>();

    /**
     * Pattern object used for regex processing. This finds occurrences of $n, where n is a positive
     * number, to replace them with argument values.
     */
    private Pattern pattern = Pattern.compile("\\$(\\d)+");

    /**
     * Parser used only to decide whether an argument value is already a standalone XML fragment.
     * DTD and external entity support are off: the values tested here come from the message.
     */
    private static final XMLInputFactory XML_PROBE_FACTORY = XMLInputFactory.newInstance();

    static {
        XML_PROBE_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XML_PROBE_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    }

    /**
     * Replaces the existing payload with a new payload as defined by the format and the argument list
     * @param synCtx the current message for mediation
     * @return true if the transformation is successful, false otherwise.
     */
    @Override
    public boolean mediate(MessageContext synCtx) {

        SOAPBody soapBody = synCtx.getEnvelope().getBody();

        StringBuffer result = new StringBuffer();
        transformPayload(result, synCtx);

        OMElement resultElement;
        try {
            resultElement = AXIOMUtil.stringToOM(result.toString());
        } catch (XMLStreamException e) {
            handleException("Unable to create a valid XML payload. Invalid format/arguments are " +
                    "provided in the payloadFactory mediator configuration", synCtx);
            return false;
        }

        // replace the existing payload with the new payload
        soapBody.removeChildren();

        for (Iterator itr = resultElement.getChildElements(); itr.hasNext();) {
            OMElement child = (OMElement) itr.next();
            itr.remove();
            soapBody.addChild(child);
        }

        return true;
    }

    /**
     * Replaces occurrences of $n with argument values.
     * @param result StringBuffer that stores the result.
     * @param synCtx Current message under mediation.
     */
    private void transformPayload(StringBuffer result, MessageContext synCtx) {

        Object[] argValues = getArgValues(synCtx);
        Matcher matcher = pattern.matcher("<dummy>" + format + "</dummy>");
        while (matcher.find()) {
            String matchSeq = matcher.group();
            int argIndex = Integer.parseInt(matchSeq.substring(1));
            if (argIndex < 1 || argIndex > argValues.length) {
                handleException("Invalid argument index " + matchSeq + " in the payloadFactory " +
                        "format; " + argValues.length + " argument(s) are configured", synCtx);
            }
            // quoteReplacement keeps a literal '$' or '\' from being read as replacement syntax.
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(argValues[argIndex - 1].toString()));
        }
        matcher.appendTail(result);
    }

    /**
     * Extracts argument values from the current message context.
     * @param synCtx Current message under mediation.
     * @return Extracted argument values.
     */
    private Object[] getArgValues(MessageContext synCtx) {

        Object[] argValues = new Object[argumentList.size()];
        for (int i = 0; i < argumentList.size(); ++i) {
            Argument arg = argumentList.get(i);
            if (arg.getValue() != null) {
                argValues[i] = arg.getValue();
            } else if (arg.getExpression() != null) {
                String value = arg.getExpression().stringValueOf(synCtx);
                if (value != null) {
                    // Escape the value unless it is already a well-formed XML fragment.
                    argValues[i] = isXML(value) ? value : escapeXmlText(value);
                } else {
                    argValues[i] = "";
                }
            } else {
                handleException("Unexpected argument type detected in the payloadFactory " +
                        "mediator configuration", synCtx);
            }
        }
        return argValues;
    }

    /**
     * Reports whether the value is already a standalone, well-formed XML fragment.
     *
     * @param value argument value to test.
     * @return true if the value parses on its own as XML.
     */
    private static boolean isXML(String value) {

        String trimmed = value.trim();
        if (trimmed.length() < 4 || !trimmed.endsWith(">")) {
            return false;
        }
        try {
            XMLStreamReader reader = XML_PROBE_FACTORY.createXMLStreamReader(new StringReader(trimmed));
            while (reader.hasNext()) {
                reader.next();
            }
            reader.close();
            return true;
        } catch (Exception e) {
            // Broad on purpose: malformed markup can surface as an unchecked exception here.
            return false;
        }
    }

    /**
     * Escapes the characters that would otherwise be read as markup once the completed format is
     * parsed, and drops the characters XML 1.0 does not permit at all.
     *
     * @param value argument value to escape.
     * @return the value as XML character data.
     */
    private static String escapeXmlText(String value) {

        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    // Numeric rather than &apos;, which predates XML in some consumers.
                    escaped.append("&#39;");
                    break;
                default:
                    if (Character.isHighSurrogate(c) && i + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(i + 1))) {
                        // Keep a valid surrogate pair together so the filter below can't split it.
                        escaped.append(c).append(value.charAt(++i));
                    } else if (isValidXmlCharacter(c)) {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    /**
     * @param c character to test.
     * @return true if XML 1.0 permits the character in a document.
     */
    private static boolean isValidXmlCharacter(char c) {

        return c == 0x9 || c == 0xA || c == 0xD
                || (c >= 0x20 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void addArgument(Argument arg) {
        argumentList.add(arg);
    }

    public List<Argument> getArgumentList() {
        return argumentList;
    }

    /**
     * Represents an argument provided in the payload factory mediator configuration.
     */
    public static class Argument {

        private String value;
        private SynapseXPath expression;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public SynapseXPath getExpression() {
            return expression;
        }

        public void setExpression(SynapseXPath expression) {
            this.expression = expression;
        }
    }

}
