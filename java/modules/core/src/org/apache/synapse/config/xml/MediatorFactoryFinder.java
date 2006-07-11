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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.MediatorFactory;
import org.apache.synapse.api.Mediator;
import org.apache.axiom.om.OMElement;
import org.apache.ws.commons.schema.*;

import sun.misc.Service;

/**
 *
 * 
 * This class is based on J2SE Service Provider model
 * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
 */

public class MediatorFactoryFinder {

	private static final Log log = LogFactory.getLog(MediatorFactoryFinder.class);

	private static final Class[] mediatorFactories = {
        SequenceMediatorFactory.class,
        LogMediatorFactory.class,
        SendMediatorFactory.class,
        FilterMediatorFactory.class,
        SynapseMediatorFactory.class,
        DropMediatorFactory.class,
        HeaderMediatorFactory.class,
        FaultMediatorFactory.class,
        PropertyMediatorFactory.class,
        SwitchMediatorFactory.class,
        SwitchCaseMediatorFactory.class,
        SwitchCaseDefaultMediatorFactory.class,
        InMediatorFactory.class,
        OutMediatorFactory.class,
        RMMediatorFactory.class
      };

    private static final String STR_SCHEMA =
        Constants.SCHEMA_PROLOG +
        "\t<xs:complexType name=\"mediator_type\">\n" +
        "\t\t<xs:sequence maxOccurs=\"unbounded\">\n" +
        "\t\t\t<xs:choice>\n" +
        "\t\t\t</xs:choice>\n" +
        "\t\t</xs:sequence>\n" +
        "\t</xs:complexType>\n" +
        "\t<xs:complexType name=\"property_type\">\n" +
        "\t\t<xs:attribute name=\"name\" type=\"xs:string\" use=\"required\"/>\n" +
        "\t\t<xs:attribute name=\"value\" type=\"xs:string\"/>\n" +
        "\t\t<xs:attribute name=\"expression\" type=\"xs:string\"/>\n" +
        "\t</xs:complexType>" +
        Constants.SCHEMA_EPILOG;

    private static MediatorFactoryFinder instance = null;

    /**
     * A map of mediator QNames to implementation class
     */
    private static Map factoryMap = new HashMap();

    /**
     * A holder to construct the Synapse.XSD for known mediators
     */
    private XmlSchema schema = null;

    /**
     * Used to define the "mediator_type" element for all known and
     * dynamically registered mediators
     */
    private XmlSchema mediators = null;

    /**
     * Load schemas only if Xalan (a dependency for XmlSchema) is available
     * i.e. if someone wants to access schema's this is a runtime requirement
     */
    private boolean loadSchemas = false;

    public static synchronized MediatorFactoryFinder getInstance() {
        if (instance == null) {
            instance = new MediatorFactoryFinder();
        }
        return instance;
    }

    /**
     * Force re initialization next time
     */
    public synchronized void reset() {
        factoryMap.clear();
        instance = null;
    }

    private MediatorFactoryFinder() {

        try {
            TransformerFactory.newInstance();
            loadSchemas = true;

            schema = new XmlSchema(Constants.SYNAPSE_NAMESPACE, null);
            try {
                mediators = new XmlSchemaCollection().read(new StreamSource(
                    new StringReader(STR_SCHEMA)), null);
            } catch (XmlSchemaException e) {
                handleException("Error defining mediator_types " +
                    "elemement for the configuration language schema", e);
            }

        } catch(TransformerFactoryConfigurationError e) {
            log.warn("Xalan unavailable. Mediator schemas will not be available");
        }

        factoryMap = new HashMap();

        for (int i = 0; i < mediatorFactories.length; i++) {
			Class c = mediatorFactories[i];
			try {
                MediatorFactory fac = (MediatorFactory) c.newInstance();
                factoryMap.put(fac.getTagQName(), c);
                if (loadSchemas) {
                    mergeSchema(fac.getTagSchema());
                    addMediatorType(mediators,  fac);
                }
            } catch (Exception e) {
				throw new SynapseException("Error instantiating " + c.getName(), e);
			}
		}
        // now iterate through the available pluggable mediator factories
        registerExtensions(mediators);

        // add registers mediators as extensions
        if (loadSchemas) {
            mergeSchema(mediators);
            if (log.isDebugEnabled()) {
                System.out.println("Mediator Schema : ");
                schema.write(System.out);
            }
        }
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    /**
     * Merge given schema into our master schema
     * @param child the sub schema / fragment to be merged
     */
    private void mergeSchema(XmlSchema child) {
        if (child != null) {
            XmlSchemaObjectTable schemaTypes = child.getSchemaTypes();
            Iterator iter = schemaTypes.getNames();
            while (iter.hasNext()) {
                QName name = (QName) iter.next();
                schema.getItems().add(schemaTypes.getItem(name));
            }
        }
    }

    /**
     * Register pluggable mediator factories from the classpath
     *
     * This looks for JAR files containing a META-INF/services that adheres to the following
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#Service%20Provider
     */
    private void registerExtensions(XmlSchema mediators) {

        log.debug("Registering mediator extensions found in the classpath : " + System.getProperty("java.class.path"));

        // register MediatorFactory extensions
        Iterator it = Service.providers(MediatorFactory.class);
        while (it.hasNext()) {
            MediatorFactory mf = (MediatorFactory) it.next();
            QName tag = mf.getTagQName();
            factoryMap.put(tag, mf.getClass());
            mergeSchema(mf.getTagSchema());
            addMediatorType(mediators,  mf);

            log.debug("Added MediatorFactory " + mf.getClass() + " to handle " + tag);
        }
    }

    /**
     * Include the mediator type element into the available "mediator_types"
     * @param mediators the parent schema which holds the "mediator_types"
     * @param mf the mediator factory which will provide the QName of the type
     */
    private void addMediatorType(XmlSchema mediators, MediatorFactory mf) {

        XmlSchemaComplexType cmplx =
            (XmlSchemaComplexType) mediators.getItems().getItem(0);
        XmlSchemaSequence seq  = (XmlSchemaSequence) cmplx.getParticle();
        XmlSchemaChoice choice = (XmlSchemaChoice) seq.getItems().getItem(0);

        XmlSchemaElement ele = new XmlSchemaElement();
        ele.setName(mf.getTagQName().getLocalPart());
        if (mf.getTagSchemaType() != null && mf.getTagSchema() != null) {
            ele.setSchemaTypeName(mf.getTagSchemaType());
            choice.getItems().add(ele);
        }
    }

    /**
	 * This method returns a Processor given an OMElement. This will be used
	 * recursively by the elements which contain processor elements themselves
	 * (e.g. rules)
	 * 
	 * @param element
     * @return Processor
	 */
	public Mediator getMediator(OMElement element) {

		QName qName = new QName(element.getNamespace().getName(), element.getLocalName());
        log.debug("getMediator(" + qName + ")");
        Class cls = (Class) factoryMap.get(qName);

        if (cls == null) {
            String msg = "Unknown mediator referenced by configuration element : " + qName;
            log.error(msg);
            throw new SynapseException(msg);
        }

        try {
			MediatorFactory mf = (MediatorFactory) cls.newInstance();
			return mf.createMediator(element);

        } catch (InstantiationException e) {
            String msg = "Error initializing mediator factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);

        } catch (IllegalAccessException e) {
            String msg = "Error initializing mediator factory : " + cls;
            log.error(msg);
            throw new SynapseException(msg, e);
		}
	}
    /*
    This method exposes all the MediatorFactories and its Extensions 
    */
    public Map getFactoryMap() {
        return factoryMap;
    }

    /**
     * Returns the XML schema for the known mediators if available - or null
     * @return the XmlSchema for the mediator configuration, if avialable. This
     * will load only if the mediator factory has properly met the requirements
     * for this feature, and if Xalan is available to the runtime (this is a
     * requiement for the XmlSchema package used underneath)
     */
    public XmlSchema getSchema() {
        return schema;
    }
}
