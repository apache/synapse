/*
* Copyright 2004,2005 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.synapse.config.xml;

import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import javax.xml.transform.stream.StreamSource;
import javax.xml.namespace.QName;
import java.io.StringReader;

public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    public static XmlSchema getSchema(String str, QName qName) {
        try {
            return new XmlSchemaCollection().read(
                new InputSource(new StringReader(str)), null);
        } catch (XmlSchemaException e) {
            log.warn("Couldnt load schema for mediator with QName " + qName, e);
            return null;
        }
    }
}
