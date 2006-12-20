package org.apache.sandesha2.wsrm;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.EndpointReferenceHelper;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

public class Endpoint implements IOMRMElement {

	private EndpointReference epr;
	
	private String rmNamespaceValue = null;
	
	private String addressingNamespaceValue = null;

	// Constructor used while parsing
	public Endpoint (String rmNamespaceValue) throws AxisFault {
		if (!isNamespaceSupported(rmNamespaceValue))
			throw new SandeshaException (SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.unknownSpec,
					rmNamespaceValue));
		
		this.rmNamespaceValue = rmNamespaceValue;
	}
	
	// Constructor used while writing
	public Endpoint (EndpointReference epr, String rmNamespaceValue, String addressingNamespaceValue) throws AxisFault {
		this (rmNamespaceValue);
		this.addressingNamespaceValue = addressingNamespaceValue;
		this.epr = epr;
	}

	public String getNamespaceValue(){
		return rmNamespaceValue;
	}

	public Object fromOMElement(OMElement endpointElement) throws OMException,AxisFault {

		epr = EndpointReferenceHelper.fromOM (endpointElement);
		if (epr==null) {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidElementFoundWithinElement,
					"EPR",
					Sandesha2Constants.WSRM_COMMON.ENDPOINT);
			throw new SandeshaException (message);
		}
		
		return this;
	}

	public OMElement toOMElement(OMElement element) throws OMException,AxisFault {

		if (epr == null)
			throw new OMException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.cannotSetEndpoint,
					null));

		OMFactory factory = element.getOMFactory();
		
		QName endpoint = new QName (rmNamespaceValue,Sandesha2Constants.WSRM_COMMON.ENDPOINT, Sandesha2Constants.WSRM_COMMON.NS_PREFIX_RM);
	    OMElement endpointElement =	EndpointReferenceHelper.toOM (factory, epr, endpoint, addressingNamespaceValue);
		
		element.addChild(endpointElement);
		return element;
	}

	public EndpointReference getEPR() {
		return epr;
	}

	public boolean isNamespaceSupported (String namespaceName) {
		if (Sandesha2Constants.SPEC_2006_08.NS_URI.equals(namespaceName))
			return true;
		
		return false;
	}
}
