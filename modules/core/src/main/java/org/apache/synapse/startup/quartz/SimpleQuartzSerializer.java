package org.apache.synapse.startup.quartz;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.StartupSerializer;
import org.apache.synapse.Startup;
import org.apache.synapse.SynapseException;

public class SimpleQuartzSerializer implements StartupSerializer {
	
	public void serializeStartup(OMElement parent, Startup s) {
		if (!(s instanceof SimpleQuartz)) throw new SynapseException("called SimpleQuartzSerializer on some other kind of startup"+s.getClass().getName());
		SimpleQuartz sq = (SimpleQuartz)s;
		OMFactory fac = parent.getOMFactory();
		OMNamespace nullNS = fac.createOMNamespace("", "");
		OMElement job = fac.createOMElement(SimpleQuartzFactory.JOB,  parent);
		job.addAttribute("class", sq.getJobClass(), nullNS);
		if (sq.isSimple()) {
			OMElement el = fac.createOMElement(new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,"simpletrigger"), job);
			if (sq.getCount()==-1) {
				el.addAttribute("forever","true",nullNS);
			} else {
				el.addAttribute("count",Integer.toString(sq.getCount()),nullNS);
			}
			el.addAttribute("interval", Long.toString(sq.getInterval()), nullNS);
		} else {
			OMElement el = fac.createOMElement(new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,"crontrigger"), job);
			el.addAttribute("expression", sq.getCron(), nullNS);
		}
		Iterator it = sq.getProperties().iterator();
		while (it.hasNext()) {
			OMElement prop = (OMElement)it.next();
			job.addChild(prop.cloneOMElement());
		}
		
	}

}
