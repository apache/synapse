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

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beans.RMDBean;
import org.apache.sandesha2.util.RangeString;

public class PersistentRMDBeanMgr extends PersistentBeanMgr implements RMDBeanMgr {

	public PersistentRMDBeanMgr(PersistentStorageManager pmgr) {
		super(pmgr);
	}

	private String requestForModel(RMDBean bean) {
		StringBuilder sql = new StringBuilder(" select * from wsrm_rmd");
		if (bean == null) return sql.toString();
		String op = " where";
		String clause = bean.getSequenceID();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" sequence_id='");
			sql.append(clause);
			sql.append("'");
		}
		EndpointReference epr = bean.getToEndpointReference();
		if (epr != null) {
			sql.append(op);
			op = " and ";
			sql.append(" to_epr_addr='");
			sql.append(epr.getAddress());
			sql.append("'");
		}
		epr = bean.getReplyToEndpointReference();
		if (epr != null) {
			sql.append(op);
			op = " and ";
			sql.append(" reply_to_epr_addr='");
			sql.append(epr.getAddress());
			sql.append("'");
		}
		epr = bean.getAcksToEndpointReference();
		if (epr != null) {
			sql.append(op);
			op = " and ";
			sql.append(" acks_to_epr_addr='");
			sql.append(epr.getAddress());
			sql.append("'");
		}
		clause = bean.getRMVersion();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" rm_version='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getSecurityTokenData();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" security_token_data='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getReferenceMessageKey();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" reference_message_key='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getHighestInMessageId();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" highest_in_message_id='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getLastInMessageId();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" last_in_message_id='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getToAddress();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" to_address='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getOutboundInternalSequence();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" outbound_internal_sequence='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getOutboundInternalSequence();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" outbound_internal_sequence='");
			sql.append(clause);
			sql.append("'");
		}
		RangeString rs = bean.getServerCompletedMessages();
		if (rs != null) {
			sql.append(op);
			op = " and ";
			sql.append(" server_completed_messages='");
			sql.append(rs.toString());
			sql.append("'");
		}
		rs = bean.getOutOfOrderRanges();
		if (rs != null) {
			sql.append(op);
			op = " and ";
			sql.append(" outof_order_ranges='");
			sql.append(rs.toString());
			sql.append("'");
		}
		if ((bean.getRmdFlags() & RMDBean.NEXT_MSG_NO_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" next_msgno_to_process=");
			sql.append(bean.getNextMsgNoToProcess());
		}
		if ((bean.getRmdFlags() & RMDBean.HIGHEST_IN_MSG_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" highest_in_message_number=");
			sql.append(bean.getHighestInMessageNumber());
		}
		if ((bean.getFlags() & RMDBean.LAST_ACTIVATED_TIME_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" last_activated_time=");
			sql.append(bean.getLastActivatedTime());
		}
		if ((bean.getFlags() & RMDBean.CLOSED_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" closed=");
			sql.append(bean.isClosed() ? 1 : 0);
		}
		if ((bean.getFlags() & RMDBean.TERMINATED_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" terminated_flag=");
			sql.append(bean.isTerminated() ? 1 : 0);
		}
		if ((bean.getFlags() & RMDBean.POLLING_MODE_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" polling_mode=");
			sql.append(bean.isPollingMode() ? 1 : 0);
		}
		if (log.isDebugEnabled()) log.debug("requestForModel " + sql.toString());
		return sql.toString();
	}

	private RMDBean getBean(ResultSet rs)
			throws Exception {
		RMDBean bean = new RMDBean();
		bean.setSequenceID(rs.getString("sequence_id"));

		Object obj = getObject(rs, "to_epr");
		if (obj != null) {
			bean.setToEndpointReference((EndpointReference) obj);
		}
		obj = getObject(rs, "reply_to_epr");
		if (obj != null) {
			bean.setReplyToEndpointReference((EndpointReference) obj);
		}
		obj = getObject(rs, "acks_to_epr");
		if (obj != null) {
			bean.setAcksToEndpointReference((EndpointReference) obj);
		}

		bean.setRMVersion(rs.getString("rm_version"));
		bean.setServiceName(rs.getString("service_name"));
		bean.setSecurityTokenData(rs.getString("security_token_data"));
		bean.setReferenceMessageKey(rs.getString("reference_message_key"));
		bean.setHighestInMessageId(rs.getString("highest_in_message_id"));
		bean.setLastInMessageId(rs.getString("last_in_message_id"));
		bean.setToAddress(rs.getString("to_address"));
		bean.setOutboundInternalSequence(rs.getString("outbound_internal_sequence"));

		bean.setOutOfOrderRanges(new RangeString(rs.getString("outof_order_ranges")));
		bean.setServerCompletedMessages(new RangeString(rs.getString("server_completed_messages")));

		bean.setHighestInMessageNumber(rs.getLong("highest_in_message_number"));
		bean.setNextMsgNoToProcess(rs.getLong("next_msgno_to_process"));
		bean.setLastActivatedTime(rs.getLong("last_activated_time"));

		bean.setClosed(rs.getInt("closed") != 0 ? true : false);
		bean.setPollingMode(rs.getInt("polling_mode") != 0 ? true : false);
		bean.setTerminated(rs.getInt("terminated_flag") != 0 ? true : false);

		bean.setFlags(rs.getInt("flags"));
		bean.setRmdFlags(rs.getInt("rmd_flags"));
		return bean;
	}

	public boolean delete(String sequenceID)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("delete RMSBean sequenceID " + sequenceID);
		try {
			Statement stmt = getDbConnection().createStatement();
			stmt.executeUpdate("delete from wsrm_rmd where sequence_id='" + sequenceID + "'");
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public List find(RMDBean bean)
			throws SandeshaStorageException {
		String sql = requestForModel(bean);
		ArrayList<RMDBean> lst = new ArrayList<RMDBean>();
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				lst.add(getBean(rs));
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		if (log.isDebugEnabled()) log.debug("Exit find lst.size(): " + lst.size());
		return lst;
	}

	public boolean insert(RMDBean bean)
			throws SandeshaStorageException {
		log.debug("insert " + bean);
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("insert into wsrm_rmd(" +
					"sequence_id,to_epr_addr,to_epr,reply_to_epr_addr,reply_to_epr,acks_to_epr_addr," +
					"acks_to_epr,rm_version,security_token_data," +
					"last_activated_time,closed,terminated_flag,polling_mode,service_name," +
					"flags,reference_message_key,highest_in_message_id,last_in_message_id," +
					"server_completed_messages,outof_order_ranges,to_address," +
					"outbound_internal_sequence,next_msgno_to_process,highest_in_message_number,rmd_flags" +
					")values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			pstmt.setString(1, bean.getSequenceID());
			EndpointReference epr = bean.getToEndpointReference();
			pstmt.setString(2, epr != null ? epr.getAddress() : null);
			// Derby don't serialize object in blob !
			// pstmt.setObject(3, epr);
			ByteArrayInputStream bais = serialize(epr);
			pstmt.setBinaryStream(3, bais, bais.available());
			epr = bean.getReplyToEndpointReference();
			pstmt.setString(4, epr != null ? epr.getAddress() : null);
			bais = serialize(epr);
			pstmt.setBinaryStream(5, bais, bais.available());
			epr = bean.getAcksToEndpointReference();
			pstmt.setString(6, epr != null ? epr.getAddress() : null);
			bais = serialize(epr);
			pstmt.setBinaryStream(7, bais, bais.available());
			pstmt.setString(8, bean.getRMVersion());
			pstmt.setString(9, bean.getSecurityTokenData());
			pstmt.setLong(10, bean.getLastActivatedTime());
			pstmt.setInt(11, bean.isClosed() ? 1 : 0);
			pstmt.setInt(12, bean.isTerminated() ? 1 : 0);
			pstmt.setInt(13, bean.isPollingMode() ? 1 : 0);
			pstmt.setString(14, bean.getServiceName());
			pstmt.setInt(15, bean.getFlags());
			pstmt.setString(16, bean.getReferenceMessageKey());
			pstmt.setString(17, bean.getHighestInMessageId());
			pstmt.setString(18, bean.getLastInMessageId());
			RangeString trs = bean.getServerCompletedMessages();
			pstmt.setString(19, trs == null ? null : trs.toString());
			trs = bean.getOutOfOrderRanges();
			pstmt.setString(20, trs == null ? null : trs.toString());
			pstmt.setString(21, bean.getToAddress());
			pstmt.setString(22, bean.getOutboundInternalSequence());
			pstmt.setLong(23, bean.getNextMsgNoToProcess());
			pstmt.setLong(24, bean.getHighestInMessageNumber());
			pstmt.setInt(25, bean.getRmdFlags());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			log.debug("Insert Exception  ", ex);
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public RMDBean retrieve(String sequenceID)
			throws SandeshaStorageException {
		RMDBean bean = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_rmd where sequence_id='" + sequenceID + "'");
			if (! rs.next()) return bean;
			bean = getBean(rs);
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return bean;
	}

	public Collection retrieveAll()
			throws SandeshaStorageException {
		return find(null);
	}

	public boolean update(RMDBean bean)
			throws SandeshaStorageException {
		log.debug("update " + bean);
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("update wsrm_rmd set " +
					"to_epr_addr=?,to_epr=?,reply_to_epr_addr=?,reply_to_epr=?,acks_to_epr_addr=?," +
					"acks_to_epr=?,rm_version=?,security_token_data=?," +
					"last_activated_time=?,closed=?,terminated_flag=?,polling_mode=?,service_name=?," +
					"flags=?,reference_message_key=?,highest_in_message_id=?,last_in_message_id=?," +
					"server_completed_messages=?,outof_order_ranges=?,to_address=?," +
					"outbound_internal_sequence=?,next_msgno_to_process=?,highest_in_message_number=?,rmd_flags=?" +
					" where sequence_id='" + bean.getSequenceID() + "'");
			EndpointReference epr = bean.getToEndpointReference();
			pstmt.setString(1, epr != null ? epr.getAddress() : null);
			// Derby don't serialize object in blob !
			// pstmt.setObject(2, epr);
			ByteArrayInputStream bais = serialize(epr);
			pstmt.setBinaryStream(2, bais, bais.available());
			epr = bean.getReplyToEndpointReference();
			pstmt.setString(3, epr != null ? epr.getAddress() : null);
			bais = serialize(epr);
			pstmt.setBinaryStream(4, bais, bais.available());
			epr = bean.getAcksToEndpointReference();
			pstmt.setString(5, epr != null ? epr.getAddress() : null);
			bais = serialize(epr);
			pstmt.setBinaryStream(6, bais, bais.available());
			pstmt.setString(7, bean.getRMVersion());
			pstmt.setString(8, bean.getSecurityTokenData());
			pstmt.setLong(9, bean.getLastActivatedTime());
			pstmt.setInt(10, bean.isClosed() ? 1 : 0);
			pstmt.setInt(11, bean.isTerminated() ? 1 : 0);
			pstmt.setInt(12, bean.isPollingMode() ? 1 : 0);
			pstmt.setString(13, bean.getServiceName());
			pstmt.setInt(14, bean.getFlags());
			pstmt.setString(15, bean.getReferenceMessageKey());
			pstmt.setString(16, bean.getHighestInMessageId());
			pstmt.setString(17, bean.getLastInMessageId());
			RangeString rs = bean.getServerCompletedMessages();
			pstmt.setString(18, rs != null ? rs.toString() : null);
			rs = bean.getOutOfOrderRanges();
			pstmt.setString(19, rs != null ? rs.toString() : null);
			pstmt.setString(20, bean.getToAddress());
			pstmt.setString(21, bean.getOutboundInternalSequence());
			pstmt.setLong(22, bean.getNextMsgNoToProcess());
			pstmt.setLong(23, bean.getHighestInMessageNumber());
			pstmt.setInt(24, bean.getRmdFlags());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public RMDBean findUnique(RMDBean bean)
			throws SandeshaStorageException {
		String sql = requestForModel(bean);
		RMDBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (result == null) {
					result = getBean(rs);
				} else {
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.nonUniqueResult,
							result.toString(),
							getBean(rs).toString());
					log.error(message);
					throw new SandeshaException(message);
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return result;
	}
}
