/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sandesha2.storage.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.io.ByteArrayInputStream;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beans.RMSBean;
import org.apache.sandesha2.util.RangeString;

public class PersistentRMSBeanMgr extends PersistentBeanMgr implements RMSBeanMgr {

	public PersistentRMSBeanMgr(PersistentStorageManager pmgr)
	{
		super(pmgr);
	}

	private String requestForModel(RMSBean bean)
	{
		StringBuilder sql = new StringBuilder(" select * from wsrm_rms");
		if ( bean == null ) return sql.toString();
		String op = " where ";
		String clause = bean.getSequenceID();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" sequence_id='");
			sql.append(clause);
			sql.append("'");
		}
		EndpointReference epr = bean.getToEndpointReference();
		if ( epr != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" to_epr_addr='");
			sql.append(epr.getAddress());
			sql.append("'");
		}
		epr = bean.getReplyToEndpointReference();
		if ( epr != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" reply_to_epr_addr='");
			sql.append(epr.getAddress());
			sql.append("'");
		}
		epr = bean.getAcksToEndpointReference();
		if ( epr != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" acks_to_epr_addr='");
			sql.append(epr.getAddress());
			sql.append("'");
		}
		clause = bean.getRMVersion();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" rm_version='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getSecurityTokenData();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" security_token_data='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getCreateSeqMsgID();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" create_seq_msg_id='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getInternalSequenceID();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" internal_sequence_id='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getCreateSequenceMsgStoreKey();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" create_sequence_msg_store_key='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getReferenceMessageStoreKey();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" create_sequence_msg_store_key='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getTransportTo();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" transport_to='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getOfferedEndPoint();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" offered_endpoint='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getOfferedSequence();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" offered_sequence='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getAnonymousUUID();
		if ( clause != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" anonymous_uuid='");
			sql.append(clause);
			sql.append("'");
		}
		RangeString xrs = bean.getClientCompletedMessages();
		if ( xrs != null ) {
			sql.append(op);
			op = " and ";
			sql.append(" client_completed_messages='");
			sql.append(xrs.toString());
			sql.append("'");
		}
		if ( (bean.getRmsFlags() & RMSBean.LAST_SEND_ERROR_TIME_FLAG ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" last_send_error_timestamp=");
			sql.append(bean.getLastSendErrorTimestamp());
		}
		if ( (bean.getRmsFlags() & RMSBean.LAST_OUT_MSG_FLAG ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" last_out_message=");
			sql.append(bean.getLastOutMessage());
		}
		if ( (bean.getRmsFlags() & RMSBean.HIGHEST_OUT_MSG_FLAG ) != 0 ) {   
			sql.append(op);
			op = " and ";
			sql.append(" highest_out_message_number=");
			sql.append(bean.getHighestOutMessageNumber());
		}
		if ( (bean.getRmsFlags() & RMSBean.NEXT_MSG_NUM_FLAG ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" next_message_number=");
			sql.append(bean.getNextMessageNumber());
		}
		if ( (bean.getRmsFlags() & RMSBean.TERMINATE_ADDED_FLAG ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" terminate_added=");
			sql.append(bean.isTerminateAdded()?1:0);
		}
		if ( (bean.getRmsFlags() & RMSBean.TIMED_OUT_FLAG ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" timed_out=");
			sql.append(bean.isTimedOut()?1:0);
		}
		if ( (bean.getRmsFlags() & RMSBean.SEQ_CLOSED_CLIENT_FLAG ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" sequence_closed_client=");
			sql.append(bean.isSequenceClosedClient()?1:0);
		}
		if ( (bean.getRmsFlags() & RMSBean.TERM_PAUSER_FOR_CS ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" termination_pauser_for_cs=");
			sql.append(bean.isTerminationPauserForCS()?1:0);
		}
		if ( (bean.getRmsFlags() & RMSBean.EXPECTED_REPLIES ) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" expected_replies=");
			sql.append(bean.getExpectedReplies());
		}
		if ( (bean.getRmsFlags() & RMSBean.SOAP_VERSION_FLAG) != 0 ) { 
			sql.append(op);
			op = " and ";
			sql.append(" soap_version=");
			sql.append(bean.getSoapVersion());
		}
		
		if ( (bean.getFlags() & RMSBean.LAST_ACTIVATED_TIME_FLAG ) != 0 ) {
			sql.append(op);
			op = " and ";
			sql.append(" last_activated_time=");
			sql.append(bean.getLastActivatedTime());
		}
		if ( (bean.getFlags() & RMSBean.CLOSED_FLAG) != 0 ) {
			sql.append(op);
			op = " and ";
			sql.append(" closed=");
			sql.append(bean.isClosed() ? 1:0);
		}
		if ( (bean.getFlags() & RMSBean.TERMINATED_FLAG) != 0 ) {
			sql.append(op);
			op = " and ";
			sql.append(" terminated_flag=");
			sql.append(bean.isTerminated() ? 1:0);
		}
		if ( (bean.getFlags() & RMSBean.POLLING_MODE_FLAG) != 0 ) {
			sql.append(op);
			op = " and ";
			sql.append(" polling_mode=");
			sql.append(bean.isPollingMode() ? 1:0);
		}
		/* only for WSRM 1.0 ?
		if ( (bean.getFlags() & 0x0010000) != 0 ) {
			sql.append(op);
			op = " and ";
			sql.append(" replay_model=");
			sql.append(bean.isReplayModel() ? 1:0);
		} */
		if ( log.isDebugEnabled() ) log.debug("requestForModel " + sql.toString());
		return sql.toString();
	}
	
	private RMSBean getBean(ResultSet rs)
	  throws Exception
	{
        RMSBean bean = new RMSBean();
		bean.setSequenceID(rs.getString("sequence_id"));

		Object obj = getObject(rs,"to_epr");
		if ( obj != null ) {
			bean.setToEndpointReference((EndpointReference)obj);
		}
		obj = getObject(rs,"reply_to_epr");
		if ( obj != null ) {
			bean.setReplyToEndpointReference((EndpointReference)obj);
		}
		obj = getObject(rs,"acks_to_epr");
		if ( obj != null ) {
			bean.setAcksToEndpointReference((EndpointReference)obj);
		}
		
		bean.setRMVersion(rs.getString("rm_version"));
		bean.setServiceName(rs.getString("service_name"));
		bean.setSecurityTokenData(rs.getString("security_token_data"));
		bean.setCreateSeqMsgID(rs.getString("create_seq_msg_id"));
		bean.setTransportTo(rs.getString("transport_to"));
		bean.setOfferedEndPoint(rs.getString("offered_endpoint"));
		bean.setOfferedSequence(rs.getString("offered_sequence"));
		bean.setAnonymousUUID(rs.getString("anonymous_uuid"));
		bean.setInternalSequenceID(rs.getString("internal_sequence_id"));
		bean.setReferenceMessageStoreKey(rs.getString("reference_msg_store_key"));
		bean.setCreateSequenceMsgStoreKey(rs.getString("create_sequence_msg_store_key"));
		bean.setHighestOutRelatesTo(rs.getString("highest_out_relates_to"));

		obj = getObject(rs,"last_send_error");
		if ( obj != null ) bean.setLastSendError((Exception)obj);

		bean.setClientCompletedMessages(new RangeString(rs.getString("client_completed_messages")));
		
		bean.setLastSendErrorTimestamp(rs.getLong("last_send_error_timestamp"));
		bean.setLastOutMessage(rs.getLong("last_out_message"));
		bean.setHighestOutMessageNumber(rs.getLong("highest_out_message_number"));
		bean.setNextMessageNumber(rs.getLong("next_message_number"));
		bean.setLastActivatedTime(rs.getLong("last_activated_time"));
		bean.setExpectedReplies(rs.getLong("expected_replies"));
		
		bean.setClosed(rs.getInt("closed")!= 0 ? true:false);
		bean.setPollingMode(rs.getInt("polling_mode")!= 0 ? true:false);
		bean.setTerminated(rs.getInt("terminated_flag")!= 0 ? true:false);
		bean.setTerminateAdded(rs.getInt("terminate_added")!= 0 ? true:false);
		bean.setTimedOut(rs.getInt("timed_out")!= 0 ? true:false);
		bean.setSequenceClosedClient(rs.getInt("sequence_closed_client")!= 0 ? true:false);
		bean.setTerminationPauserForCS(rs.getInt("termination_pauser_for_cs")!= 0 ? true:false);
		bean.setAvoidAutoTermination(rs.getInt("avoid_auto_termination")!= 0 ? true:false);
		
		bean.setSoapVersion(rs.getInt("soap_version"));
		bean.setFlags(rs.getInt("flags"));
		bean.setRmsFlags(rs.getInt("rms_flags"));
		return bean;
	}


	public boolean delete(String msgId)
	  throws SandeshaStorageException
	{
        if(log.isDebugEnabled()) log.debug("delete RMSBean msgId " + msgId);
		try {
			Statement stmt = getDbConnection().createStatement();
			stmt.executeUpdate("delete from wsrm_rms where create_seq_msg_id='" + msgId + "'");
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException("Exception in RMSBeanMgr delete", ex);
		}
		return true;
	}
	
	public List find(RMSBean bean)
	  throws SandeshaStorageException
	{
        String sql = requestForModel(bean);
		ArrayList<RMSBean> lst = new ArrayList<RMSBean>();
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() ) {
				lst.add(getBean(rs));
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException("Exception in RMSBeanMgr find : " + sql, ex);
		}
		return lst;
	}

	
	public boolean insert(RMSBean bean)
	  throws SandeshaStorageException
	{
        log.debug("insert RMSBean " + bean);
		try {
		    PreparedStatement pstmt = getDbConnection().prepareStatement("insert into wsrm_rms(" +
		    		"create_seq_msg_id,sequence_id,to_epr_addr,to_epr,reply_to_epr_addr,reply_to_epr,acks_to_epr_addr,acks_to_epr,rm_version,security_token_data," +
		    		"last_activated_time,closed,terminated_flag,polling_mode,service_name," +
		    		"flags,id,internal_sequence_id,create_sequence_msg_store_key," +
		    		"reference_msg_store_key,last_send_error,highest_out_relates_to," +
		    		"client_completed_messages,transport_to,offered_endpoint,offered_sequence," +
		    		"anonymous_uuid,last_send_error_timestamp,last_out_message,highest_out_message_number," +
		    		"next_message_number,terminate_added,timed_out,sequence_closed_client," +
		    		"expected_replies,soap_version,termination_pauser_for_cs,avoid_auto_termination," +
		    		"rms_flags)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			 pstmt.setString(1,bean.getCreateSeqMsgID());
			 pstmt.setString(2,bean.getSequenceID());
			 EndpointReference epr = bean.getToEndpointReference();
			 pstmt.setString(3,epr != null ? epr.getAddress():null);
			 // Derby don't serialize object in blob !
			 // pstmt.setObject(4, epr);
			 ByteArrayInputStream bais = serialize(epr);
			 pstmt.setBinaryStream(4, bais, bais.available());
			 epr = bean.getReplyToEndpointReference();
			 pstmt.setString(5,epr != null ? epr.getAddress():null);
			 bais = serialize(epr);
			 pstmt.setBinaryStream(6, bais, bais.available());
			 epr = bean.getAcksToEndpointReference();
			 pstmt.setString(7,epr != null ? epr.getAddress():null);
			 bais = serialize(epr);
			 pstmt.setBinaryStream(8, bais, bais.available());
			 pstmt.setString(9,bean.getRMVersion());
			 pstmt.setString(10,bean.getSecurityTokenData());
			 pstmt.setLong(11,bean.getLastActivatedTime());
			 pstmt.setInt(12,bean.isClosed() ? 1 : 0);
			 pstmt.setInt(13,bean.isTerminated() ? 1 : 0);
			 pstmt.setInt(14,bean.isPollingMode() ? 1 : 0);
			 pstmt.setString(15,bean.getServiceName());
			 pstmt.setInt(16,bean.getFlags());
			 pstmt.setLong(17,bean.getId());
			 pstmt.setString(18,bean.getInternalSequenceID());
			 pstmt.setString(19,bean.getCreateSequenceMsgStoreKey());
			 pstmt.setString(20,bean.getReferenceMessageStoreKey());
			 bais = serialize(bean.getLastSendError());
			 pstmt.setBinaryStream(21, bais, bais.available());
			 pstmt.setString(22,bean.getHighestOutRelatesTo());
			 RangeString rs = bean.getClientCompletedMessages();
			 pstmt.setString(23,rs != null ? rs.toString():null);
			 pstmt.setString(24,bean.getTransportTo());
			 pstmt.setString(25,bean.getOfferedEndPoint());
			 pstmt.setString(26,bean.getOfferedSequence());
			 pstmt.setString(27,bean.getAnonymousUUID());
			 pstmt.setLong(28,bean.getLastSendErrorTimestamp());
			 pstmt.setLong(29,bean.getLastOutMessage());
			 pstmt.setLong(30,bean.getHighestOutMessageNumber());
			 pstmt.setLong(31,bean.getNextMessageNumber());
			 pstmt.setInt(32,bean.isTerminateAdded() ? 1:0);
			 pstmt.setInt(33,bean.isTimedOut() ? 1:0);
			 pstmt.setInt(34,bean.isSequenceClosedClient() ? 1:0);
			 pstmt.setLong(35,bean.getExpectedReplies());
			 pstmt.setInt(36,bean.getSoapVersion());
			 pstmt.setInt(37,bean.isTerminationPauserForCS() ? 1:0);
			 pstmt.setInt(38,bean.isAvoidAutoTermination() ? 1:0);
			 pstmt.setInt(39,bean.getRmsFlags());
			 pstmt.execute();
			 pstmt.close();
		} catch (Exception ex) {
			log.debug("Insert Exception  ", ex);
			throw new SandeshaStorageException("Exception in RMSBeanMgr insert", ex);
		}
		return true;
	}
	
	public RMSBean retrieve(String msgId)
	  throws SandeshaStorageException
	{
        log.debug("Retrieve  msdId " + msgId);
		RMSBean bean = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_rms where create_seq_msg_id='" + msgId + "'");
			if ( rs.next() ) bean = getBean(rs);
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			log.debug("Retrieve Exception  ", ex);
			throw new SandeshaStorageException("Exception in RMSBeanMgr retrieve", ex);
		}
		log.debug("Retrieve  return " + bean);
		return bean;
	}
	
	public boolean update(RMSBean bean)
	  throws SandeshaStorageException
	{
        if ( log.isDebugEnabled()) {
		  log.debug("Update bean : " + bean);
		}
			try {
		    	PreparedStatement pstmt = getDbConnection().prepareStatement("update wsrm_rms set " +
		    			"sequence_id=?,to_epr_addr=?,to_epr=?,reply_to_epr_addr=?,reply_to_epr=?,acks_to_epr_addr=?,acks_to_epr=?,rm_version=?,security_token_data=?," +
		    			"last_activated_time=?,closed=?,terminated_flag=?,polling_mode=?,service_name=?," +
		    			"flags=?,id=?,internal_sequence_id=?,create_sequence_msg_store_key=?," +
		    			"reference_msg_store_key=?,last_send_error=?,highest_out_relates_to=?," +
		    			"client_completed_messages=?,transport_to=?,offered_endpoint=?,offered_sequence=?," +
		    			"anonymous_uuid=?,last_send_error_timestamp=?,last_out_message=?,highest_out_message_number=?," +
		    			"next_message_number=?,terminate_added=?,timed_out=?,sequence_closed_client=?," +
		    			"expected_replies=?,soap_version=?,termination_pauser_for_cs=?,avoid_auto_termination=?," +
		    			"rms_flags=?" +
		    			" where create_seq_msg_id='" + bean.getCreateSeqMsgID() + "'");
			   	pstmt.setString(1,bean.getSequenceID());
			   	EndpointReference epr = bean.getToEndpointReference();
			   	pstmt.setString(2,epr != null ? epr.getAddress():null);
				 // Derby don't serialize object in blob !
				 // pstmt.setObject(3, epr);
				 ByteArrayInputStream bais = serialize(epr);
				 pstmt.setBinaryStream(3, bais, bais.available());
			   	epr = bean.getReplyToEndpointReference();
			   	pstmt.setString(4,epr != null ? epr.getAddress():null);
				 bais = serialize(epr);
				 pstmt.setBinaryStream(5, bais, bais.available());
			   	epr = bean.getAcksToEndpointReference();
			   	pstmt.setString(6,epr != null ? epr.getAddress():null);
				 bais = serialize(epr);
				 pstmt.setBinaryStream(7, bais, bais.available());
			   	pstmt.setString(8,bean.getRMVersion());
			   	pstmt.setString(9,bean.getSecurityTokenData());
			   	pstmt.setLong(10,bean.getLastActivatedTime());
			   	pstmt.setInt(11,bean.isClosed() ? 1 : 0);
			   	pstmt.setInt(12,bean.isTerminated() ? 1 : 0);
			   	pstmt.setInt(13,bean.isPollingMode() ? 1 : 0);
			   	pstmt.setString(14,bean.getServiceName());
			   	pstmt.setInt(15,bean.getFlags());
			   	pstmt.setLong(16,bean.getId());
			   	pstmt.setString(17,bean.getInternalSequenceID());
			   	pstmt.setString(18,bean.getCreateSequenceMsgStoreKey());
			   	pstmt.setString(19,bean.getReferenceMessageStoreKey());
				 bais = serialize(bean.getLastSendError());
				 pstmt.setBinaryStream(20, bais, bais.available());
			   	pstmt.setString(21,bean.getHighestOutRelatesTo());
			   	RangeString rs = bean.getClientCompletedMessages();
			   	pstmt.setString(22,rs != null ? rs.toString():null);
			   	pstmt.setString(23,bean.getTransportTo());
			   	pstmt.setString(24,bean.getOfferedEndPoint());
			   	pstmt.setString(25,bean.getOfferedSequence());
			   	pstmt.setString(26,bean.getAnonymousUUID());
			   	pstmt.setLong(27,bean.getLastSendErrorTimestamp());
			   	pstmt.setLong(28,bean.getLastOutMessage());
			   	pstmt.setLong(29,bean.getHighestOutMessageNumber());
			   	pstmt.setLong(30,bean.getNextMessageNumber());
			   	pstmt.setInt(31,bean.isTerminateAdded() ? 1:0);
			   	pstmt.setInt(32,bean.isTimedOut() ? 1:0);
			   	pstmt.setInt(33,bean.isSequenceClosedClient() ? 1:0);
			   	pstmt.setLong(34,bean.getExpectedReplies());
			   	pstmt.setInt(35,bean.getSoapVersion());
			   	pstmt.setInt(36,bean.isTerminationPauserForCS() ? 1:0);
			   	pstmt.setInt(37,bean.isAvoidAutoTermination() ? 1:0);
			   	pstmt.setInt(38,bean.getRmsFlags());
			    pstmt.execute();
			    pstmt.close();
			} catch (Exception ex) {
				log.error("Update Exception " + ex);
				throw new SandeshaStorageException("Exception in RMSBeanMgr update", ex);
			}
			return true;
		}
	
	public RMSBean findUnique(RMSBean bean)
	  throws SandeshaStorageException
	{
        String sql = requestForModel(bean);
		RMSBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() ) {
				if ( result == null ) {
					result = getBean(rs);
				} else {
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.nonUniqueResult,
							result.toString(),
							getBean(rs).toString());
					log.error(message);
					throw new SandeshaException (message);
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			log.error("Exception in findUnique " + ex);
			throw new SandeshaStorageException("Exception in PersistentRMSBeanManager::findUnique", ex);
		}		
		log.debug("FindUnique RMSBean : " + result);
		return result;
	}

    public RMSBean retrieveBySequenceID(String seqId) throws SandeshaStorageException {
        RMSBean dummyBean = new RMSBean();
        dummyBean.setSequenceID(seqId);
        String sql = requestForModel(dummyBean);
		RMSBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);

            while ( rs.next() ) {
				if ( result == null ) {
					result = getBean(rs);
				} else {
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.nonUniqueResult,
							result.toString(),
							getBean(rs).toString());
					log.error(message);
					throw new SandeshaException (message);
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			log.error("Exception in findUnique " + ex);
			throw new SandeshaStorageException("Exception in PersistentRMSBeanManager::findUnique", ex);
		}
		log.debug("FindUnique RMSBean : " + result);
		return result;
    }

    public RMSBean retrieveByInternalSequenceID(String internalSeqId) throws SandeshaStorageException {
        RMSBean dummyBean = new RMSBean();
        dummyBean.setInternalSequenceID(internalSeqId);
        String sql = requestForModel(dummyBean);
		RMSBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() ) {
				if ( result == null ) {
					result = getBean(rs);
				} else {
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.nonUniqueResult,
							result.toString(),
							getBean(rs).toString());
					log.error(message);
					throw new SandeshaException (message);
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			log.error("Exception in findUnique " + ex);
			throw new SandeshaStorageException("Exception in PersistentRMSBeanManager::findUnique", ex);
		}
		log.debug("FindUnique RMSBean : " + result);
		return result;
    }
}
