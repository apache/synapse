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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.storage.beans.SenderBean;

/**
 * Manages Sender Beans
 */
public class PersistentSenderBeanMgr extends PersistentBeanMgr implements SenderBeanMgr {

	public PersistentSenderBeanMgr(PersistentStorageManager pmgr) {
		super(pmgr);
	}

	private String requestForModel(SenderBean bean) {
		StringBuilder sql = new StringBuilder("select * from wsrm_sender");
		if (bean == null) return sql.toString();
		String op = " where ";
		String clause = bean.getSequenceID();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" sequence_id='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getMessageID();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" message_id='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getMessageContextRefKey();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" message_context_ref_key='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getInternalSequenceID();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" internal_sequence_id='");
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
		clause = bean.getInboundSequenceId();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" inbound_sequence_id='");
			sql.append(clause);
			sql.append("'");
		}

		if ((bean.getFlags() & SenderBean.SEND_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" send=");
			sql.append(bean.isSend() ? 1 : 0);
		}
		if ((bean.getFlags() & SenderBean.SEND_COUNT_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" sent_count=");
			sql.append(bean.getSentCount());
		}
		if ((bean.getFlags() & 0x00000100) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" message_number=");
			sql.append(bean.getMessageNumber());
		}
		if ((bean.getFlags() & 0x00001000) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" resend=");
			sql.append(bean.isReSend() ? 1 : 0);
		}
		if ((bean.getFlags() & 0x00010000) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" time_to_send<=");
			sql.append(bean.getTimeToSend());
		}
		if ((bean.getFlags() & 0x00100000) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" message_type=");
			sql.append(bean.getMessageType());
		}
		if ((bean.getFlags() & 0x01000000) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" last_message=");
			sql.append(bean.isLastMessage() ? 1 : 0);
		}
		if ((bean.getFlags() & 0x10000000) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" inbound_message_number=");
			sql.append(bean.getInboundMessageNumber());
		}
		if ((bean.getFlags() & 0x00000002) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" transport_available=");
			sql.append(bean.isTransportAvailable() ? 1 : 0);
		}
		log.debug("requestForModel " + sql.toString());
		return sql.toString();
	}

	private SenderBean getBean(ResultSet rs)
			throws Exception {
		SenderBean bean = new SenderBean();
		bean.setMessageID(rs.getString("message_id"));
		bean.setMessageContextRefKey(rs.getString("message_context_ref_key"));
		bean.setSequenceID(rs.getString("sequence_id"));
		bean.setInternalSequenceID(rs.getString("internal_sequence_id"));
		bean.setToAddress(rs.getString("to_address"));
		bean.setInboundSequenceId(rs.getString("inbound_sequence_id"));

		bean.setMessageNumber(rs.getLong("message_number"));
		bean.setTimeToSend(rs.getLong("time_to_send"));
		bean.setInboundMessageNumber(rs.getLong("inbound_message_number"));

		bean.setSend(rs.getInt("send") != 0 ? true : false);
		bean.setReSend(rs.getInt("resend") != 0 ? true : false);
		bean.setLastMessage(rs.getInt("last_message") != 0 ? true : false);
		bean.setTransportAvailable(rs.getInt("transport_available") != 0 ? true : false);

		bean.setSentCount(rs.getInt("sent_count"));
		bean.setMessageType(rs.getInt("message_type"));
		bean.setFlags(rs.getInt("flags"));
		return bean;
	}

	public boolean delete(String messageID)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("Delete MsgID " + messageID);
		try {
			Statement stmt = getDbConnection().createStatement();
			stmt.executeUpdate("delete from wsrm_sender where message_id='" + messageID + "'");
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public List<SenderBean> find(SenderBean bean)
			throws SandeshaStorageException {
		String sql = requestForModel(bean);
		ArrayList<SenderBean> lst = new ArrayList<SenderBean>();
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
		return lst;
	}


	public List<SenderBean> find(String internalSequenceId)
			throws SandeshaStorageException {
		ArrayList<SenderBean> lst = new ArrayList<SenderBean>();
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_sender where internal_sequence_id='" +
					internalSequenceId + "'");
			while (rs.next()) {
				lst.add(getBean(rs));
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return lst;
	}

	public SenderBean getNextMsgToSend(String sequenceId)
			throws SandeshaStorageException {
		SenderBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement();
			String sql = "select * from wsrm_sender where sequence_id";
			if (sequenceId == null) sql += " is null ";
			else sql += "='" + sequenceId + "' ";
			sql += "and transport_available=1 and send=1";
			log.debug("getNextMsgToSend " + sql);
			ResultSet rs = stmt.executeQuery(sql);
			long timeNow = System.currentTimeMillis();
			log.debug("getNextMsgToSend timeNow " + timeNow);
			while (rs.next()) {
				if (timeNow >= rs.getLong("time_to_send")) {
					result = getBean(rs);
					break;
				}
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			log.error("getNextMsgToSend Exception" + ex);
			throw new SandeshaStorageException(ex);
		}
		if (log.isDebugEnabled()) log.debug("Exit getNextMessageToSend " + result);
		return result;
	}

	public boolean insert(SenderBean bean)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("Insert " + bean);
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("insert into wsrm_sender(" +
					"message_id, message_context_ref_key, internal_sequence_id, sequence_id," +
					"to_address, inbound_sequence_id, send, sent_count, message_number, resend," +
					"time_to_send, message_type, last_message, inbound_message_number, transport_available," +
					"flags)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			pstmt.setString(1, bean.getMessageID());
			pstmt.setString(2, bean.getMessageContextRefKey());
			pstmt.setString(3, bean.getInternalSequenceID());
			pstmt.setString(4, bean.getSequenceID());
			pstmt.setString(5, bean.getToAddress());
			pstmt.setString(6, bean.getInboundSequenceId());
			pstmt.setInt(7, bean.isSend() ? 1 : 0);
			pstmt.setInt(8, bean.getSentCount());
			pstmt.setLong(9, bean.getMessageNumber());
			pstmt.setInt(10, bean.isReSend() ? 1 : 0);
			pstmt.setLong(11, bean.getTimeToSend());
			pstmt.setInt(12, bean.getMessageType());
			pstmt.setInt(13, bean.isLastMessage() ? 1 : 0);
			pstmt.setLong(14, bean.getInboundMessageNumber());
			pstmt.setInt(15, bean.isTransportAvailable() ? 1 : 0);
			pstmt.setInt(16, bean.getFlags());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public SenderBean retrieve(String messageID)
			throws SandeshaStorageException {
		SenderBean bean = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_sender where message_id='" + messageID + "'");
			if (! rs.next()) return bean;
			bean = getBean(rs);
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return bean;
	}

	public boolean update(SenderBean bean)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("Update " + bean);
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("update wsrm_sender set " +
					"message_context_ref_key=?, internal_sequence_id=?, sequence_id=?," +
					"to_address=?, inbound_sequence_id=?, send=?, sent_count=?, message_number=?, resend=?," +
					"time_to_send=?, message_type=?, last_message=?, inbound_message_number=?, transport_available=?," +
					"flags=? where message_id='" + bean.getMessageID() + "'");
			pstmt.setString(1, bean.getMessageContextRefKey());
			pstmt.setString(2, bean.getInternalSequenceID());
			pstmt.setString(3, bean.getSequenceID());
			pstmt.setString(4, bean.getToAddress());
			pstmt.setString(5, bean.getInboundSequenceId());
			pstmt.setInt(6, bean.isSend() ? 1 : 0);
			pstmt.setInt(7, bean.getSentCount());
			pstmt.setLong(8, bean.getMessageNumber());
			pstmt.setInt(9, bean.isReSend() ? 1 : 0);
			pstmt.setLong(10, bean.getTimeToSend());
			pstmt.setInt(11, bean.getMessageType());
			pstmt.setInt(12, bean.isLastMessage() ? 1 : 0);
			pstmt.setLong(13, bean.getInboundMessageNumber());
			pstmt.setInt(14, bean.isTransportAvailable() ? 1 : 0);
			pstmt.setInt(15, bean.getFlags());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			log.error("Update Exception " + ex);
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public SenderBean findUnique(SenderBean bean)
			throws SandeshaStorageException {
		String sql = requestForModel(bean);
		SenderBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement();
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

	public SenderBean retrieveFromMessageRefKey(String messageContextRefKey) {
		SenderBean bean = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_sender where message_context_ref_key='" + messageContextRefKey + "'");
			if (! rs.next()) return bean;
			bean = getBean(rs);
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			return null;
		}
		return bean;
	}

	public SenderBean retrieve(String sequnceId, long messageNumber) throws SandeshaStorageException {
		SenderBean bean = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_sender where sequence_id='" + sequnceId + "' " +
					" and message_number=" + messageNumber);
			if (! rs.next()) return bean;
			bean = getBean(rs);
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			return null;
		}
		return bean;
	}
}
