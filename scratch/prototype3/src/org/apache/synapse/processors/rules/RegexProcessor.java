package org.apache.synapse.processors.rules;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.synapse.HeaderType;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.processors.ListProcessor;

/**
 * @author Paul Fremantle
 * 
 */
public class RegexProcessor extends ListProcessor {
	private Pattern pattern = null;

	private Log log = LogFactory.getLog(getClass());

	private HeaderType headerType = new HeaderType();

	private String property = null;

	public void setHeaderType(String header) {
		headerType.setHeaderType(header);
	}

	public String getHeaderType() {
		return headerType.getHeaderType();
	}

	public void setPattern(String p) {
		pattern = Pattern.compile(p);
	}

	public String getPattern() {
		return pattern.toString();
	}

	public void setPropertyName(String p) {
		this.property = p;
	}

	public String getPropertyName() {
		return property;
	}

	public boolean process(SynapseEnvironment se, SynapseMessage smc) {

		if (pattern == null) {
			log.debug("trying to process with empty pattern");
			return true;
		}
		String toMatch = null;
		if (property != null) {
			toMatch = smc.getProperty(property).toString();
		} else {
			toMatch = headerType.getHeader(smc);
		}
		if (toMatch==null) return true;
		
		if (pattern.matcher(toMatch).matches()) {
			log.debug("Regex pattern " + pattern.toString() + " matched "
					+ toMatch);
			return super.process(se, smc);
		}
		return true;
	}

}
