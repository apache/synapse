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

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.sql.*;

import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beans.InvokerBean;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

public class PersistentInvokerBeanMgr extends PersistentBeanMgr implements InvokerBeanMgr {

	public PersistentInvokerBeanMgr(PersistentStorageManager pmgr) {
		super(pmgr);
	}

	private String requestForModel(InvokerBean bean) {
		StringBuilder sql = new StringBuilder(" select * from wsrm_invoker");
		if (bean == null) return sql.toString();
		String op = " where";
		String clause = bean.getMessageContextRefKey();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" message_context_ref_key='");
			sql.append(clause);
			sql.append("'");
		}
		clause = bean.getSequenceID();
		if (clause != null) {
			sql.append(op);
			op = " and ";
			sql.append(" sequence_id='");
			sql.append(clause);
			sql.append("'");
		}
		if ((bean.getFlags() & InvokerBean.MSG_NO_FLAG) != 0) {
			sql.append(op);
			op = " and ";
			sql.append(" msg_no=");
			sql.append(bean.getMsgNo());
		}
		return sql.toString();
	}

	private InvokerBean getInvokerBean(ResultSet rs)
			throws Exception {
		InvokerBean invokerBean = new InvokerBean();
		invokerBean.setMessageContextRefKey(rs.getString("message_context_ref_key"));
		invokerBean.setSequenceID(rs.getString("sequence_id"));
		invokerBean.setMsgNo(rs.getLong("msg_no"));
		invokerBean.setFlags(rs.getInt("flags"));
		invokerBean.setContext((Serializable) getObject(rs, "context"));
		return invokerBean;
	}

	public boolean delete(String key)
			throws SandeshaStorageException {
		try {
			Statement stmt = getDbConnection().createStatement();
			stmt.executeUpdate("delete from wsrm_invoker where message_context_ref_key='" + key + "'");
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public List find(InvokerBean bean)
			throws SandeshaStorageException {
		String sql = requestForModel(bean);
		ArrayList<InvokerBean> lst = new ArrayList<InvokerBean>();
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				lst.add(getInvokerBean(rs));
			}
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return lst;
	}

	public boolean insert(InvokerBean bean)
			throws SandeshaStorageException {
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("insert into wsrm_invoker(message_context_ref_key," +
					"sequence_id,context,msg_no,flags)values(?,?,?,?,?)");
			pstmt.setString(1, bean.getMessageContextRefKey());
			pstmt.setString(2, bean.getSequenceID());
			pstmt.setLong(4, bean.getMsgNo());
			pstmt.setInt(5, bean.getFlags());
			// Derby ne serialise pas avec setObject
			ByteArrayInputStream bais = serialize(bean.getContext());
			pstmt.setBinaryStream(3, bais, bais.available());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public InvokerBean retrieve(String key)
			throws SandeshaStorageException {
		InvokerBean invokerBean = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery("select * from wsrm_invoker where message_context_ref_key='" + key + "'");
			if (! rs.next()) return invokerBean;
			invokerBean = getInvokerBean(rs);
			rs.close();
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return invokerBean;
	}

	public boolean update(InvokerBean bean)
			throws SandeshaStorageException {
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("update wsrm_invoker set " +
					"sequence_id=?,context=?,msg_no=?,flags=? where message_context_ref_key='" + bean.getMessageContextRefKey() + "'");
			pstmt.setString(1, bean.getSequenceID());
			pstmt.setLong(3, bean.getMsgNo());
			pstmt.setInt(4, bean.getFlags());
			// Derby ne serialise pas avec setObject
			ByteArrayInputStream bais = serialize(bean.getContext());
			pstmt.setBinaryStream(2, bais, bais.available());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
		return true;
	}

	public InvokerBean findUnique(InvokerBean bean)
			throws SandeshaException {
		String sql = requestForModel(bean);
		InvokerBean result = null;
		try {
			Statement stmt = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (result == null) {
					result = getInvokerBean(rs);
				} else {
					String message = SandeshaMessageHelper.getMessage(
							SandeshaMessageKeys.nonUniqueResult,
							result.toString(),
							getInvokerBean(rs).toString());
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
