/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.policy;

import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.neethi.Assertion;
import org.apache.neethi.Constants;
import org.apache.neethi.PolicyComponent;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.util.PropertyManager;

/**
 * Used to hold peoperties loaded from sandesha2.properties file or
 * Sandesha2Constants.
 */

public class SandeshaPolicyBean implements Assertion {

	private SandeshaPolicyBean parent = null;
	
    // String storageManagerClass = null;
    boolean inOrder = true;
    private boolean inOrderSet = false;
    
    ArrayList msgTypesToDrop = null;

    private String inMemoryStorageManagerClass = null;

    private String permanentStorageManagerClass = null;

    private String securityManagerClass = null;
    
    private String contextManagerClass = null;

    private long inactiveTimeoutValue;
    private boolean inactiveTimeoutValueSet = false;
    
    private String inactivityTimeoutMeasure;

    private long inactivityTimeoutInterval = -1;
    private boolean inactivityTimeoutIntervalSet = false;
 
    private long sequenceRemovalTimeoutValue;
    private String sequenceRemovalTimeoutMeasure;

    private long sequenceRemovalTimeoutInterval = -1;
    private boolean sequenceRemovalTimeoutIntervalSet = false;
    
    private long acknowledgementInterval;
    private boolean acknowledgementIntervalSet = false;
    
    private long retransmissionInterval;
    private boolean retransmissionIntervalSet = false;
    
    private boolean exponentialBackoff;
    private boolean exponentialBackoffSet = false;
    
    private int maximumRetransmissionCount;
    private boolean maximumRetransmissionCountSet = false;
    
    private boolean enableMakeConnection;
    private boolean enableMakeConnectionSet = false;
    
    private boolean enableRMAnonURI;
    private boolean enableRMAnonURISet = false;
    
    private boolean useMessageSerialization;
    private boolean useMessageSerializationSet = false;
    
    private boolean enforceRM;
    private boolean enforceRMSet = false;
    
    public SandeshaPolicyBean () {
    	//we always set a PolicyBean from Constants as the default parent.
		PropertyManager.loadPropertiesFromDefaultValues(this);

    }
    
	public void setInactiveTimeoutInterval(long value, String measure) {
		
        long timeOut = -1;

        if (measure == null) {
            this.inactivityTimeoutInterval = value;
        } else if ("seconds".equals(measure)) {
            timeOut = value * 1000;
        } else if ("minutes".equals(measure)) {
            timeOut = value * 60 * 1000;
        } else if ("hours".equals(measure)) {
            timeOut = value * 60 * 60 * 1000;
        } else if ("days".equals(measure)) {
            timeOut = value * 24 * 60 * 60 * 1000;
        }

        this.inactivityTimeoutInterval = timeOut;

    }

	public void setSequenceRemovalTimeoutInterval(long value, String measure) {
    long timeOut = 0;

    if (measure == null) {
        this.sequenceRemovalTimeoutInterval = value;
    } else if ("seconds".equals(measure)) {
        timeOut = value * 1000;
    } else if ("minutes".equals(measure)) {
        timeOut = value * 60 * 1000;
    } else if ("hours".equals(measure)) {
        timeOut = value * 60 * 60 * 1000;
    } else if ("days".equals(measure)) {
        timeOut = value * 24 * 60 * 60 * 1000;
    }

    this.sequenceRemovalTimeoutInterval = timeOut;

	}
	
    public void setAcknowledgementInterval(long acknowledgementInterval) {
        this.acknowledgementInterval = acknowledgementInterval;
        setAcknowledgementIntervalSet(true);
    }

    public String getInMemoryStorageManagerClass() {
        return inMemoryStorageManagerClass;
    }

    public void setInMemoryStorageManagerClass(
            String inMemoryStorageManagerClass) {
        this.inMemoryStorageManagerClass = inMemoryStorageManagerClass;
    }

    public String getPermanentStorageManagerClass() {
        return permanentStorageManagerClass;
    }

    public void setPermanentStorageManagerClass(
            String permanentStorageManagerClass) {
        this.permanentStorageManagerClass = permanentStorageManagerClass;
    }

    public boolean isInOrder() {
        return inOrder;
    }

    public void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
        setInOrderSet(true);
    }

    public ArrayList getMsgTypesToDrop() {
        return msgTypesToDrop;
    }

    public void setMsgTypesToDrop(ArrayList msgTypesToDrop) {
        this.msgTypesToDrop = msgTypesToDrop;
    }

    public void addMsgTypeToDrop(Integer typeNo) {

        if (typeNo != null) {
            if (msgTypesToDrop == null)
                msgTypesToDrop = new ArrayList();

            msgTypesToDrop.add(typeNo);
        }
    }

    public int getMaximumRetransmissionCount() throws SandeshaException {
    	
    	if (isMaximumRetransmissionCountSet()) {
    		return maximumRetransmissionCount;
    	} else if (parent!=null) {
    		return parent.getMaximumRetransmissionCount();
    	} else {
    		String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyHasNotBeenSet, Sandesha2Constants.Assertions.ELEM_MAX_RETRANS_COUNT);
    		throw new SandeshaException (message);
    	}
    	
    }

    public void setMaximumRetransmissionCount(int maximumRetransmissionCount) {
        this.maximumRetransmissionCount = maximumRetransmissionCount;
        setMaximumRetransmissionCountSet(true);
    }

    public String getSecurityManagerClass() {
        return securityManagerClass;
    }

    public void setSecurityManagerClass(String className) {
        this.securityManagerClass = className;
    }
    
    public String getContextManagerClass() {
    	return contextManagerClass;
    }

    public void setContextManagerClass(String className) {
    	this.contextManagerClass = className;
    }
    
    public QName getName() {
        return Sandesha2Constants.Assertions.Q_ELEM__RMBEAN;
    }

    public boolean isOptional() {
        return false;
    }

    public PolicyComponent normalize() {
        return this;
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        try {
			String localName = Sandesha2Constants.Assertions.Q_ELEM_RMASSERTION
			        .getLocalPart();
			String namespaceURI = Sandesha2Constants.Assertions.Q_ELEM_RMASSERTION
			        .getNamespaceURI();

			String prefix = writer.getPrefix(namespaceURI);
			if (prefix == null) {
			    prefix = Sandesha2Constants.Assertions.Q_ELEM_RMASSERTION
			            .getPrefix();
			    writer.setPrefix(prefix, namespaceURI);
			}

			// <wsrm:RMAssertion>
			writer.writeStartElement(prefix, localName, namespaceURI);
			// xmlns:wsrm=".."
			writer.writeNamespace(prefix, namespaceURI);

			String policyLocalName = Sandesha2Constants.Assertions.Q_ELEM_POLICY
			        .getLocalPart();
			String policyNamespaceURI = Sandesha2Constants.Assertions.Q_ELEM_POLICY
			        .getNamespaceURI();
			String wspPrefix = writer.getPrefix(policyNamespaceURI);

			if (wspPrefix == null) {
			    wspPrefix = Sandesha2Constants.Assertions.Q_ELEM_POLICY.getPrefix();
			    writer.writeNamespace(wspPrefix, policyNamespaceURI);
			}

			// <wsp:Policy>
			writer.writeStartElement(wspPrefix, policyLocalName, policyNamespaceURI);

			// <wsrm:AcknowledgementInterval />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_ACK_INTERVAL.getLocalPart(), namespaceURI);
			writer.writeCharacters(Long.toString(getAcknowledgementInterval()));
			writer.writeEndElement();
			
			// <wsrm:RetransmissionInterval />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_RETRANS_INTERVAL.getLocalPart(), namespaceURI);
			writer.writeCharacters(Long.toString(getRetransmissionInterval()));
			writer.writeEndElement();

			// <wsrm:MaximumRetransmissionCount />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_MAX_RETRANS_COUNT.getLocalPart(), namespaceURI);
			writer.writeCharacters(Long.toString(getMaximumRetransmissionCount()));
			writer.writeEndElement();
			
			// <wsrm:ExponentialBackoff />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_EXP_BACKOFF.getLocalPart(), namespaceURI);
			writer.writeCharacters(Boolean.toString(isExponentialBackoff()));
			writer.writeEndElement();
				
			// <wsrm:InactivityTimeout />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_INACTIVITY_TIMEOUT.getLocalPart(), namespaceURI);
			writer.writeCharacters(Long.toString(getInactivityTimeoutInterval()));
			writer.writeEndElement();
			
			// <wsrm:InactivityTimeoutMeasure />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_INACTIVITY_TIMEOUT_MEASURES.getLocalPart(), namespaceURI);
			writer.writeCharacters(inactivityTimeoutMeasure);
			writer.writeEndElement();

			// <wsrm:SequenceRemovalTimeout />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_SEQUENCE_REMOVAL_TIMEOUT.getLocalPart(), namespaceURI);
			writer.writeCharacters(Long.toString(getSequenceRemovalTimeoutInterval()));
			writer.writeEndElement();
			
			// <wsrm:SequenceRemovalTimeoutMeasure />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_SEQUENCE_REMOVAL_TIMEOUT_MEASURES.getLocalPart(), namespaceURI);
			writer.writeCharacters(sequenceRemovalTimeoutMeasure);
			writer.writeEndElement();

			// <wsrm:InvokeInOrder />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_INVOKE_INORDER.getLocalPart(), namespaceURI);
			writer.writeCharacters(Boolean.toString(isInOrder()));
			writer.writeEndElement();
			
			// <wsrm:MessageTypesToDrop />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_MSG_TYPES_TO_DROP.getLocalPart(), namespaceURI);
			writer.writeCharacters("none"); // FIXME
			writer.writeEndElement();
			
			// <wsrm:StorageManagers>
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_STORAGE_MGR.getLocalPart(), namespaceURI);
			
			// <wsrm:InMemoryStorageManager />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_INMEMORY_STORAGE_MGR.getLocalPart(), namespaceURI);
			writer.writeCharacters(getInMemoryStorageManagerClass());
			writer.writeEndElement();
			
			// <wsrm:PermanentStorageManager />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_PERMANENT_STORAGE_MGR.getLocalPart(), namespaceURI);
			writer.writeCharacters(getPermanentStorageManagerClass());
			writer.writeEndElement();        
			
			// </wsrm:StorageManager>
			writer.writeEndElement();
			
			// <wsrm:SecurityManager />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_SEC_MGR.getLocalPart(), namespaceURI);
			writer.writeCharacters(getSecurityManagerClass());
			writer.writeEndElement();
			
			//<wsrm:ContextManager />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_CONTEXT_MGR.getLocalPart(), namespaceURI);
			writer.writeCharacters(getContextManagerClass());
			writer.writeEndElement();

			// <wsrm:MakeConnection>
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_MAKE_CONNECTION.getLocalPart(), namespaceURI);
			
			// <wsrm:Enabled />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_ENABLED.getLocalPart(), namespaceURI);
			writer.writeCharacters(Boolean.toString(isEnableMakeConnection()));
			writer.writeEndElement();
			
			// <wsrm:UseRMAnonURI />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_USE_RM_ANON_URI.getLocalPart(), namespaceURI);
			writer.writeCharacters(Boolean.toString(isEnableRMAnonURI()));
			writer.writeEndElement();
			
			// </wsrm:MakeConnection>
			writer.writeEndElement();
			
			// <wsrm:UseMessageSerialization />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_USE_SERIALIZATION.getLocalPart(), namespaceURI);
			writer.writeCharacters(Boolean.toString(isUseMessageSerialization()));
			writer.writeEndElement();

			// <wsrm:EnforceRM />
			writer.writeStartElement(prefix, Sandesha2Constants.Assertions.Q_ELEM_ENFORCE_RM.getLocalPart(), namespaceURI);
			writer.writeCharacters(Boolean.toString(isEnforceRM()));
			writer.writeEndElement();
			
			// </wsp:Policy>
			writer.writeEndElement();

			// </wsrm:RMAssertion>
			writer.writeEndElement();
		} catch (SandeshaException e) {
			throw new XMLStreamException (e);
		}

    }

    public short getType() {
        return Constants.TYPE_ASSERTION;
    }

    public boolean isExponentialBackoff() throws SandeshaException {
    	if (isExponentialBackoffSet()) {
    		return exponentialBackoff;
    	} else if (parent!=null) {
    		return parent.isExponentialBackoff ();
    	} else {
    		String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyHasNotBeenSet, Sandesha2Constants.Assertions.ELEM_EXP_BACKOFF);
    		throw new SandeshaException (message);
    	}
    }

    public void setExponentialBackoff(boolean exponentialBackoff) {
        this.exponentialBackoff = exponentialBackoff;
        setExponentialBackoffSet(true);
    }

    public long getRetransmissionInterval() throws SandeshaException {
    	if (isRetransmissionIntervalSet()) {
    		return retransmissionInterval;
    	} else if (parent!=null) {
    		return parent.getRetransmissionInterval();
    	} else {
    		String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyHasNotBeenSet, Sandesha2Constants.Assertions.ELEM_RETRANS_INTERVAL);
    		throw new SandeshaException (message);
    	}
    }

    public void setRetransmissionInterval(long retransmissionInterval) {
        this.retransmissionInterval = retransmissionInterval;
        setRetransmissionIntervalSet(true);
    }

    public long getAcknowledgementInterval() throws SandeshaException {
    	if (isAcknowledgementIntervalSet()) {
    		return acknowledgementInterval;
    	} else if (parent!=null) {
    		return parent.getAcknowledgementInterval();
    	} else {
    		String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyHasNotBeenSet, Sandesha2Constants.Assertions.ELEM_ACK_INTERVAL);
    		throw new SandeshaException (message);
    	}
    }

    public long getInactivityTimeoutInterval() {
        if (inactivityTimeoutInterval < 0)
            setInactiveTimeoutInterval(inactiveTimeoutValue,
                    inactivityTimeoutMeasure);

        return inactivityTimeoutInterval;
    }

    public long getSequenceRemovalTimeoutInterval() {
      if (sequenceRemovalTimeoutInterval < 0)
          setSequenceRemovalTimeoutInterval(sequenceRemovalTimeoutValue,
                  sequenceRemovalTimeoutMeasure);

      return sequenceRemovalTimeoutInterval;
  }

    public void setInactiveTimeoutValue(long inactiveTimeoutValue) {
        this.inactiveTimeoutValue = inactiveTimeoutValue;
        setInactiveTimeoutValueSet(true);
    }

    public void setInactivityTimeoutMeasure(String inactivityTimeoutMeasure) {
        this.inactivityTimeoutMeasure = inactivityTimeoutMeasure;
    }
    
  	public void setSequenceRemovalTimeoutValue(long sequenceRemovalTimeoutValue) {
  	  this.sequenceRemovalTimeoutValue = sequenceRemovalTimeoutValue;
  	  setSequenceRemovalTimeoutValueSet(true);  	  
    }

		public void setSequenceRemovalTimeoutMeasure(String sequenceRemovalTimeoutMeasure) {
  	  this.sequenceRemovalTimeoutMeasure = sequenceRemovalTimeoutMeasure; 	  
    }

    public boolean isEnableMakeConnection() {
		return enableMakeConnection;
	}

	public void setEnableMakeConnection(boolean enableMakeConnection) {
		this.enableMakeConnection = enableMakeConnection;
		setEnableMakeConnectionSet(true);
	}

	public boolean isEnableRMAnonURI() {
		return enableRMAnonURI;
	}

	public void setEnableRMAnonURI(boolean enableRMAnonURI) {
		this.enableRMAnonURI = enableRMAnonURI;
		setEnableRMAnonURISet(true);
	}

	public boolean isUseMessageSerialization() throws SandeshaException {
    	if (isUseMessageSerializationSet ()) {
    		return useMessageSerialization;
    	} else if (parent!=null) {
    		return parent.isUseMessageSerialization();
    	} else {
    		String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyHasNotBeenSet, Sandesha2Constants.Assertions.ELEM_USE_SERIALIZATION);
    		throw new SandeshaException (message);
    	}
	}

	public void setUseMessageSerialization(boolean useMessageSerialization) {
		this.useMessageSerialization = useMessageSerialization;
		setUseMessageSerializationSet(true);
	}    

	public boolean equal(PolicyComponent policyComponent) {
        // TODO
        return false;
    }

    public boolean isEnforceRM() throws SandeshaException {
    	if (isEnforceRMSet ()) {
    		return enforceRM;
    	} else if (parent!=null) {
    		return parent.isEnforceRM();
    	} else {
    		String message = SandeshaMessageHelper.getMessage(SandeshaMessageKeys.policyHasNotBeenSet, Sandesha2Constants.Assertions.ELEM_ENFORCE_RM);
    		throw new SandeshaException (message);
    	}
	}

	public void setEnforceRM(boolean enforceRM) {
		this.enforceRM = enforceRM;
		setEnforceRMSet(true);
	}

	protected boolean isAcknowledgementIntervalSet() {
		return acknowledgementIntervalSet;
	}

	protected void setAcknowledgementIntervalSet(boolean acknowledgementIntervalSet) {
		this.acknowledgementIntervalSet = acknowledgementIntervalSet;
	}

	protected boolean isEnableMakeConnectionSet() {
		return enableMakeConnectionSet;
	}

	protected void setEnableMakeConnectionSet(boolean enableMakeConnectionSet) {
		this.enableMakeConnectionSet = enableMakeConnectionSet;
	}

	protected boolean isEnableRMAnonURISet() {
		return enableRMAnonURISet;
	}

	protected void setEnableRMAnonURISet(boolean enableRMAnonURISet) {
		this.enableRMAnonURISet = enableRMAnonURISet;
	}

	protected boolean isEnforceRMSet() {
		return enforceRMSet;
	}

	protected void setEnforceRMSet(boolean enforceRMSet) {
		this.enforceRMSet = enforceRMSet;
	}

	protected boolean isExponentialBackoffSet() {
		return exponentialBackoffSet;
	}

	protected void setExponentialBackoffSet(boolean exponentialBackoffSet) {
		this.exponentialBackoffSet = exponentialBackoffSet;
	}

	protected boolean isInactiveTimeoutValueSet() {
		return inactiveTimeoutValueSet;
	}

	protected void setInactiveTimeoutValueSet(boolean inactiveTimeoutValueSet) {
		this.inactiveTimeoutValueSet = inactiveTimeoutValueSet;
	}

	protected void setSequenceRemovalTimeoutValueSet(boolean sequenceRemovalTimeoutIntervalSet) {
		this.sequenceRemovalTimeoutIntervalSet = sequenceRemovalTimeoutIntervalSet;
  }

	protected boolean isSequenceRemovalTimeoutValueSet() {
		return sequenceRemovalTimeoutIntervalSet;
	}

	protected boolean isInactivityTimeoutIntervalSet() {
		return inactivityTimeoutIntervalSet;
	}

	protected void setInactivityTimeoutIntervalSet(
			boolean inactivityTimeoutIntervalSet) {
		this.inactivityTimeoutIntervalSet = inactivityTimeoutIntervalSet;
	}

	protected boolean isInOrderSet() {
		return inOrderSet;
	}

	protected void setInOrderSet(boolean inOrderSet) {
		this.inOrderSet = inOrderSet;
	}

	protected boolean isMaximumRetransmissionCountSet() {
		return maximumRetransmissionCountSet;
	}

	protected void setMaximumRetransmissionCountSet(
			boolean maximumRetransmissionCountSet) {
		this.maximumRetransmissionCountSet = maximumRetransmissionCountSet;
	}

	protected boolean isRetransmissionIntervalSet() {
		return retransmissionIntervalSet;
	}

	protected void setRetransmissionIntervalSet(boolean retransmissionIntervalSet) {
		this.retransmissionIntervalSet = retransmissionIntervalSet;
	}

	protected boolean isUseMessageSerializationSet() {
		return useMessageSerializationSet;
	}

	protected void setUseMessageSerializationSet(boolean useMessageSerializationSet) {
		this.useMessageSerializationSet = useMessageSerializationSet;
	}

	public SandeshaPolicyBean getParent() {
		return parent;
	}

	public void setParent(SandeshaPolicyBean parent) {
		this.parent = parent;
	}
	
}
