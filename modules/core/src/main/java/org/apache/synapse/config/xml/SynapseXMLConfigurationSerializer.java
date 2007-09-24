package org.apache.synapse.config.xml;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.EndpointAbstractSerializer;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.Startup;

public class SynapseXMLConfigurationSerializer implements ConfigurationSerializer {

    private static final Log log = LogFactory
            .getLog(XMLConfigurationSerializer.class);

    private static final OMFactory fac = OMAbstractFactory.getOMFactory();

    private static final OMNamespace synNS = fac.createOMNamespace(
            XMLConfigConstants.SYNAPSE_NAMESPACE, "syn");

    private static final OMNamespace nullNS = fac.createOMNamespace(
            XMLConfigConstants.NULL_NAMESPACE, "");

    /**
     * order of entries is irrelavant, however its nice to have some order
     *
     * @param synCfg
     * @throws XMLStreamException
     */

    public OMElement serializeConfiguration(SynapseConfiguration synCfg) {

        OMElement definitions = fac.createOMElement("definitions", synNS);

        // first process a remote registry if present
        if (synCfg.getRegistry() != null) {
            RegistrySerializer.serializeRegistry(definitions, synCfg
                    .getRegistry());
        }

        // add proxy services
        Iterator iter = synCfg.getProxyServices().iterator();
        while (iter.hasNext()) {
            ProxyService service = (ProxyService) iter.next();
            ProxyServiceSerializer.serializeProxy(definitions, service);
        }

        Map entries = new HashMap();
        Map endpoints = new HashMap();
        Map sequences = new HashMap();

        iter = synCfg.getLocalRegistry().keySet().iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            Object o = synCfg.getLocalRegistry().get(key);
            if (o instanceof Mediator) {
                sequences.put(key, o);
            } else if (o instanceof Endpoint) {
                endpoints.put(key, o);
            } else if (o instanceof Entry) {
                entries.put(key, o);
            } else {
                handleException("Unknown object : " + o.getClass()
                        + " for serialization into Synapse configuration");
            }
        }

        // process entries
        serializeEntries(definitions, entries);

        // process endpoints
        serializeEndpoints(definitions, endpoints);

        // process sequences
        serializeSequences(definitions, sequences);

        // handle startups
        serializeStartups(definitions, synCfg.getStartups());

        return definitions;
    }

    private static void serializeEntries(OMElement definitions, Map entries) {
        Iterator iter = entries.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            EntrySerializer.serializeEntry((Entry) entries.get(key),
                    definitions);
        }
    }

    private static void serializeStartups(OMElement definitions, Collection startups) {
        Iterator it = startups.iterator();
        while (it.hasNext()) {
            Startup s = (Startup) it.next();
            StartupFinder.getInstance().serializeStartup(definitions, s);
        }
    }

    private static void serializeEndpoints(OMElement definitions, Map endpoints) {
        Iterator iter = endpoints.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Object o = endpoints.get(key);
            if (o instanceof Endpoint) {
                Endpoint endpoint = (Endpoint) o;
                OMElement epElement = EndpointAbstractSerializer
                        .getEndpointSerializer(endpoint).serializeEndpoint(
                        endpoint);
                definitions.addChild(epElement);
            }

        }
    }

    private static void serializeSequences(OMElement definitions, Map sequences) {
        Iterator iter = sequences.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Mediator mediator = (Mediator) sequences.get(key);
            MediatorSerializerFinder.getInstance().getSerializer(mediator)
                    .serializeMediator(definitions, mediator);
        }
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public QName getTagQName() {
        return XMLConfigConstants.DEFINITIONS_ELT;
	}

}
