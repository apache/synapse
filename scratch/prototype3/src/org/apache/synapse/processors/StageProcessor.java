package org.apache.synapse.processors;

import javax.xml.namespace.QName;

import org.apache.synapse.Constants;

/**
 * @author Paul Fremantle A stage is really just an alias for &ltall&gt
 * 
 */
public class StageProcessor extends AllProcessor {
	private static final QName STAGE_Q = new QName(Constants.SYNAPSE_NAMESPACE,
			"stage");

	public QName getTagQName() {
		return STAGE_Q;
	}

}
