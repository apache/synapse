package org.apache.synapse.config.xml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


import org.apache.axiom.om.OMElement;
import org.apache.synapse.startup.Startup;

public class StartupWrapperFactory {

	public static List createStartup(OMElement elem) {
		List startups = new LinkedList();
		for (Iterator iter = elem.getChildElements(); iter.hasNext();) {
			OMElement startupElement = (OMElement) iter.next();
			Startup s = StartupFinder.getInstance().getStartup(startupElement);
			if (s!=null) {
				startups.add(s);
			}
		}
		if (startups.size()>0) return startups;
		else return null;
	}


}
