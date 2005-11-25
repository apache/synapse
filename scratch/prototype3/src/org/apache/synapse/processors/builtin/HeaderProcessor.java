package org.apache.synapse.processors.builtin;

import org.apache.synapse.HeaderType;
import org.apache.synapse.SynapseEnvironment;
import org.apache.synapse.SynapseMessage;

import org.apache.synapse.processors.AbstractProcessor;

/**
 * @author Paul Fremantle
 *         <p>
 *        
 * 
 */
public class HeaderProcessor extends AbstractProcessor {
	
	private HeaderType headerType = new HeaderType();

	private String value = null;

	public void setHeaderType(String ht) {
		headerType.setHeaderType(ht);
	}
	
	public String getHeaderType() {
		return headerType.getHeaderType();
	}
	
	public boolean process(SynapseEnvironment se, SynapseMessage sm) {

		headerType.setHeader(sm, getValue());
		return true;
	}

	

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
