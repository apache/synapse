package org.apache.sandesha2.msgprocessors;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.RMMsgContext;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.workers.SequenceEntry;
import org.apache.sandesha2.wsrm.MessagePending;

public class MessagePendingProcessor {

	private static final Log log = LogFactory.getLog(MessagePendingProcessor.class);
	
	public void processMessagePendingHeaders(RMMsgContext message) throws AxisFault {
		
		if (log.isDebugEnabled())
			log.debug("Enter: MessagePendingProcessor::processMessagePendingHeaders");

		MessagePending messagePending = (MessagePending) message.getMessagePart(Sandesha2Constants.MessageParts.MESSAGE_PENDING);
		if (messagePending!=null) {
			boolean pending = messagePending.isPending();
			if (pending) {
				SequenceEntry entry = (SequenceEntry) message.getProperty(Sandesha2Constants.MessageContextProperties.MAKECONNECTION_ENTRY);
				if(entry != null) {
					ConfigurationContext context = message.getConfigurationContext();
					StorageManager storage = SandeshaUtil.getSandeshaStorageManager(context, context.getAxisConfiguration());
					PollingManager pollingManager = storage.getPollingManager();
					if(pollingManager != null) pollingManager.schedulePollingRequest(entry.getSequenceId(), entry.isRmSource());
				}
			}
		}
			
		
		
		if (log.isDebugEnabled())
			log.debug("Exit: MessagePendingProcessor::processMessagePendingHeaders");
	}

}
