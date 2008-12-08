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

import java.io.*;
import java.util.HashMap;
import java.sql.*;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ModuleConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.policy.SandeshaPolicyBean;
import org.apache.sandesha2.polling.PollingManager;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.StorageManager;
import org.apache.sandesha2.storage.Transaction;
import org.apache.sandesha2.storage.beanmanagers.InvokerBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMDBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.RMSBeanMgr;
import org.apache.sandesha2.storage.beanmanagers.SenderBeanMgr;
import org.apache.sandesha2.util.SandeshaUtil;
import org.apache.sandesha2.Sandesha2Constants;
import org.apache.sandesha2.workers.Invoker;
import org.apache.sandesha2.workers.SandeshaThread;
import org.apache.sandesha2.workers.Sender;

/**
 * A Storage Manager implementation for managing Sandesha2 beans.
 * <p/>
 * Needs this parameter in module.xml or axis2.xml :
 * <p/>
 * db.driver				JDBC Driver class name
 * db.connectionstring	JDBC connection string
 * db.user				Data Base user name
 * db.password			Data Base user password
 * <p/>
 * Transactions are supposed to be attached to a thread (see inMemoryStorageManager)
 * hence the ThreadLocal threadTransaction variable (instead of the transactions HashMap
 * used by inMemoryStorageManager).
 * <p/>
 * MessageContexts are stored in a HashMap, as in inMemoryStorageManager, AND in DataBase
 * as backup in case of failure.
 */

public class PersistentStorageManager extends StorageManager {

	private Connection dbConnection = null;
	private String dbConnectionString = null;
	private String dbDriver = null;
	private String dbUser = null;
	private String dbPassword = null;
	private PersistentRMSBeanMgr pRMSBeanMgr = null;
	private PersistentRMDBeanMgr pRMDBeanMgr = null;
	private PersistentSenderBeanMgr pSenderBeanMgr = null;
	private PersistentInvokerBeanMgr pInvokerBeanMgr = null;
	private Sender sender = null;
	private Invoker invoker = null;
	private PollingManager pollingManager = null;
	private boolean useSerialization = false;
	private HashMap<String, MessageContext> storageMap = null;
	private static ThreadLocal<Transaction> threadTransaction = null;
	private static final Log log = LogFactory.getLog(PersistentStorageManager.class);

	private TransactionLock transactionLock;

	public SandeshaThread getInvoker() {
		return invoker;
	}

	public PollingManager getPollingManager() {
		return pollingManager;
	}

	public SandeshaThread getSender() {
		return sender;
	}


	public PersistentStorageManager(ConfigurationContext context)
			throws SandeshaException {
		super(context);
		log.info("create PersistentStorageManager");
		storageMap = new HashMap<String, MessageContext>();
		threadTransaction = new ThreadLocal<Transaction>();
		pRMSBeanMgr = new PersistentRMSBeanMgr(this);
		pRMDBeanMgr = new PersistentRMDBeanMgr(this);
		pSenderBeanMgr = new PersistentSenderBeanMgr(this);
		pInvokerBeanMgr = new PersistentInvokerBeanMgr(this);
		sender = new Sender();
		// Note that while inOrder is a global property we can decide if we need the
		// invoker thread at this point. If we change this to be a sequence-level
		// property then we'll need to revisit this.
		SandeshaPolicyBean policy = SandeshaUtil.getPropertyBean(context.getAxisConfiguration());
		useSerialization = policy.isUseMessageSerialization();
		if (policy.isInOrder()) invoker = new Invoker();
		if (policy.isEnableMakeConnection()) pollingManager = new PollingManager();
		ModuleConfiguration mc = context.getAxisConfiguration().getModuleConfig("sandesha2");
        if (mc != null) {
            Parameter param = mc.getParameter("db.connectionstring");
            if (param != null) {
                dbConnectionString = (String) param.getValue();
                log.debug(param.getName() + "=" + dbConnectionString);
            }
            param = mc.getParameter("db.driver");
            if (param != null) {
                dbDriver = (String) param.getValue();
                log.debug(param.getName() + "=" + dbDriver);
            }
            param = mc.getParameter("db.user");
            if (param != null) {
                dbUser = (String) param.getValue();
                log.debug(param.getName() + "=" + dbUser);
            }
            param = mc.getParameter("db.password");
            if (param != null) {
                dbPassword = (String) param.getValue();
                log.debug(param.getName() + "=" + dbPassword);
            }
        }

		this.transactionLock = new TransactionLock();
	}

	public void shutdown() {
		if (dbConnection != null) {
			try {
				dbConnection.close();
				dbConnection = null;
			} catch (Exception ex) {
			}
		}
		super.shutdown();
	}

	public InvokerBeanMgr getInvokerBeanMgr() {
		return pInvokerBeanMgr;
	}

	public RMDBeanMgr getRMDBeanMgr() {
		return pRMDBeanMgr;
	}

	public RMSBeanMgr getRMSBeanMgr() {
		return pRMSBeanMgr;
	}

	public SenderBeanMgr getSenderBeanMgr() {
		return pSenderBeanMgr;
	}

	public boolean requiresMessageSerialization() {
		return useSerialization;
	}

	public boolean hasUserTransaction(MessageContext msg) {
		// Answer to : Is there a user transaction in play ?
		// but what is a 'user transaction' ?
		return false;
	}

	public Transaction getTransaction() {

		Transaction transaction = (Transaction) threadTransaction.get();
		if (transaction == null) {
			this.acquireTransactionLock();
			transaction = new JDBCTransaction(this);
			threadTransaction.set(transaction);
		} else {
			// We don't want to overwrite or return an existing transaction, as someone
			// else should decide if we commit it or not. If we get here then we probably
			// have a bug.
			if (log.isDebugEnabled()) log.debug("Possible re-used transaction: ");
			transaction = null;
		}
		return transaction;
	}

	public void removeTransaction() {
		threadTransaction.set(null);
	}

	/**
	 * Returns the connection attached to the current transaction if exists
	 * or the "common" connection.
	 *
	 * @return Data Base Connection
	 */
	public Connection getDbConnection() {
		JDBCTransaction transaction = (JDBCTransaction) threadTransaction.get();
		if (transaction == null) return dbConnection;
		return transaction.getDbConnection();
	}


	public void initStorage(AxisModule moduleDesc)
			throws SandeshaStorageException {
		log.info("init PersistentStorageManager");
        if (dbConnectionString == null){
            if (moduleDesc.getParameter("db.connectionstring") != null){
               dbConnectionString = (String) moduleDesc.getParameter("db.connectionstring").getValue();
            }
        }

        if (dbDriver == null){
            if (moduleDesc.getParameter("db.driver") != null){
               dbDriver = (String) moduleDesc.getParameter("db.driver").getValue();
            }
        }

        if (dbUser == null){
            if (moduleDesc.getParameter("db.user") != null){
               dbUser = (String) moduleDesc.getParameter("db.user").getValue();
            }
        }

        if (dbPassword == null){
            if (moduleDesc.getParameter("db.password") != null){
               dbPassword = (String) moduleDesc.getParameter("db.password").getValue(); 
            }
        }
        if (dbConnectionString == null || dbDriver == null)
			throw new SandeshaStorageException("Can't proceed. Needed properties are not set.");
		
		dbConnection = dbConnect();
	}

	public Connection dbConnect()
			throws SandeshaStorageException {
		try {
			Class.forName(dbDriver);
			return DriverManager.getConnection(dbConnectionString, dbUser, dbPassword);
		} catch (Exception ex) {
			log.error("Unable to create DB connection ", ex);
			throw new SandeshaStorageException(ex);
		}
	}

	public MessageContext retrieveMessageContext(String key, ConfigurationContext configContext)
			throws SandeshaStorageException {
		log.debug("Enter retrieveMessageContext for key " + key);
		 /**/
		if (storageMap.containsKey(key)) {
			log.debug("retrieveMessageContext get from cache");
			return (MessageContext) storageMap.get(key);
		}
		 /**/
		try {
			Statement stmt = getDbConnection().createStatement();
			 /**/
			ResultSet rs = stmt.executeQuery("select * from wsrm_msgctx where ctx_key='" +
					key + "'");
			rs.next();
			MessageContext msgCtx = new MessageContext();
			msgCtx.readExternal(new ObjectInputStream(rs.getBinaryStream("ctx")));
			msgCtx.activate(configContext);
			msgCtx.setProperty(Sandesha2Constants.POST_FAILURE_MESSAGE, Sandesha2Constants.VALUE_TRUE);
			rs.close();
			stmt.close();
			log.debug("RetrieveMessageContext get from DB");
			return msgCtx;
		} catch (Exception ex) {
			log.error("RetrieveMessageContext exception " + ex);
			throw new SandeshaStorageException(ex);
		}
	}

	synchronized public void storeMessageContext(String key, MessageContext msgContext)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("Enter storeMessageContext for key " + key + " context " + msgContext);
		storageMap.put(key, msgContext);
		try {
			PreparedStatement pstmt = getDbConnection().prepareStatement("insert into wsrm_msgctx(ctx_key,ctx)values(?,?)");
			pstmt.setString(1, key);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			msgContext.writeExternal(oos);
			oos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			pstmt.setBinaryStream(2, bais, bais.available());
			pstmt.execute();
			pstmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException(ex);
		}
	}

	synchronized public void updateMessageContext(String key, MessageContext msgContext)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("updateMessageContext key : " + key);
		storageMap.put(key, msgContext);
		PreparedStatement pstmt = null;
		try {
			pstmt = getDbConnection().prepareStatement("update wsrm_msgctx set ctx=?" +
					"where ctx_key='" + key + "'");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			msgContext.writeExternal(oos);
			oos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			pstmt.setBinaryStream(1, bais, bais.available());
			pstmt.executeQuery();
			pstmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException("Exception in updateMessageContext", ex);
		}
	}

	public void removeMessageContext(String key)
			throws SandeshaStorageException {
		if (log.isDebugEnabled()) log.debug("removeMessageContext key : " + key);
		try {
			Statement stmt = getDbConnection().createStatement();
			MessageContext messageInCache = (MessageContext) storageMap.get(key);
			if (messageInCache != null) storageMap.remove(key);
			stmt.executeUpdate("delete from wsrm_msgctx where ctx_key='" + key + "'");
			stmt.close();
		} catch (Exception ex) {
			throw new SandeshaStorageException("Exception in removeMessageContext", ex);
		}
	}

	public void acquireTransactionLock() {
		synchronized (this.transactionLock) {
			if (this.transactionLock.isAcquried()) {
				try {
					this.transactionLock.wait();
				} catch (InterruptedException e) {
				}
				// keep on waiting until acquireTransactionLock gain
				this.acquireTransactionLock();
			} else {
				this.transactionLock.lock();
			}
		}
	}

	public void releaseTransactionLock(){
		synchronized(this.transactionLock){
			this.transactionLock.release();
		}
	}

}
