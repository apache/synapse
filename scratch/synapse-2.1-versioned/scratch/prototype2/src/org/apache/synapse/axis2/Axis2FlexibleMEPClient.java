package org.apache.synapse.axis2;




import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.deployment.util.PhasesInfo;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.OutInAxisOperation;
import org.apache.axis2.engine.AxisConfigurationImpl;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.synapse.Constants;
import org.apache.synapse.SynapseException;




import javax.xml.namespace.QName;

public class Axis2FlexibleMEPClient {
     
	// wholesale cut and paste from axis2.clientapi.*
	public static MessageContext send(MessageContext smc) {
		try {
		
	     ConfigurationContext sysContext = null;
         ConfigurationContextFactory efac =
	                    new ConfigurationContextFactory();
	            sysContext = efac.buildClientConfigurationContext(null);
        QName assumedServiceName = new QName("AnonymousService");
        AxisService axisService = new AxisService(assumedServiceName);
        AxisOperation axisOperationTemplate = new   OutInAxisOperation(new QName("TemplateOperation"));
        PhasesInfo info =((AxisConfigurationImpl)sysContext.getAxisConfiguration()).getPhasesinfo();
        if(info != null){
              info.setOperationPhases(axisOperationTemplate);
        }
        axisService.addOperation(axisOperationTemplate);
        sysContext.getAxisConfiguration().addService(axisService);
	    ServiceContext serviceContext = axisService.getParent().getServiceGroupContext(sysContext).getServiceContext(
	                assumedServiceName.getLocalPart());
	    
	    MessageContext msgCtx = new MessageContext(serviceContext.getConfigurationContext());
		
	    if(smc.getSoapAction()!=null) msgCtx.setSoapAction(smc.getSoapAction());
	    if(smc.getTo()!=null) msgCtx.setTo(smc.getTo());
	    if(smc.getFrom()!=null) msgCtx.setFrom(smc.getFrom());
	    if(smc.getMessageID()!=null) msgCtx.setMessageID(smc.getMessageID()); else msgCtx.setMessageID(String.valueOf("uuid:"+ UUIDGenerator.getUUID()));
	    if(smc.getReplyTo()!=null) msgCtx.setReplyTo(smc.getReplyTo());
	    if(smc.getRelatesTo()!=null) msgCtx.setRelatesTo(smc.getRelatesTo());

	    msgCtx.setEnvelope(smc.getEnvelope());
	    if (msgCtx.getEnvelope().getHeader()==null )
	    msgCtx.getEnvelope().getBody().insertSiblingBefore(
				OMAbstractFactory.getSOAP11Factory()
						.getDefaultEnvelope().getHeader());

        
	    msgCtx.setServiceContext(serviceContext);
        
        EndpointReference epr = msgCtx.getTo();
        String transport = null;
        if (epr != null) {
            String toURL = epr.getAddress();
            int index = toURL.indexOf(':');
            if (index > 0) {
                transport = toURL.substring(0, index);
            }
        }

        if (transport != null) {
        
				msgCtx.setTransportOut(serviceContext.getConfigurationContext().getAxisConfiguration().getTransportOut(
				        new QName(transport)));
		
        } else {
            throw new SynapseException("cannotInferTransport");
        }
        //initialize and set the Operation Context
	    
	    
	    
			msgCtx.setOperationContext(axisOperationTemplate.findOperationContext(msgCtx, serviceContext));
			AxisEngine engine = new AxisEngine(sysContext);
			engine.send(msgCtx);
			
			
		
		MessageContext response =
            new MessageContext(msgCtx.getSystemContext(),
                    msgCtx.getSessionContext(),
                    msgCtx.getTransportIn(),
                    msgCtx.getTransportOut());
		response.setProperty(MessageContext.TRANSPORT_IN,
                         msgCtx.getProperty(MessageContext.TRANSPORT_IN));
		msgCtx.getAxisOperation().registerOperationContext(response,msgCtx.getOperationContext());
		response.setServerSide(false);
		response.setServiceContext(msgCtx.getServiceContext());
		response.setServiceGroupContext(msgCtx.getServiceGroupContext());

		//If request is REST we assume the response is REST, so set the variable
		response.setDoingREST(msgCtx.isDoingREST());

		SOAPEnvelope resenvelope = TransportUtils.createSOAPMessage(response, msgCtx.getEnvelope().getNamespace().getName());

		response.setEnvelope(resenvelope);
		engine = new AxisEngine(msgCtx.getSystemContext());
		engine.receive(response);
		response.setProperty(Constants.ISRESPONSE_PROPERTY, new Boolean(true));
		return response;
	    } catch (Exception e) {
			e.printStackTrace();
			throw new SynapseException(e);
		}
	      
	}
	
}
