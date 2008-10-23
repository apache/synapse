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

import java.sql.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.storage.Transaction;
/*
 * Each transaction has its own DataBase connection with autocommit set to false
 * 
 * As there is no method to "release" a transaction, connections are closed on
 * commit or rollback.
 */
public class JDBCTransaction implements Transaction {
	private PersistentStorageManager pmgr = null;
	private Connection dbConnection = null;
	private boolean active = false;
	private Log log = LogFactory.getLog(getClass());

	public JDBCTransaction (PersistentStorageManager pmgr)
	{
		log.debug("new JDBCTransaction");
		try {
		  this.pmgr = pmgr;
		  dbConnection = pmgr.dbConnect();
		  dbConnection.setAutoCommit(false);
		  active = true;
		} catch (Exception ex) {}
	}

	public Connection getDbConnection()
	{
		return dbConnection;
	}

	private void freeTransaction()
	{
		try {
			dbConnection.close();
			pmgr.removeTransaction();
		} catch (Exception ex) {}

	}

	public void commit()
	{
		log.debug("commit JDBCTransaction");
		try {
		  dbConnection.commit();
		  freeTransaction();
		} catch (Exception ex) {}
		active = false;
	}

	public void rollback()
	{
		log.debug("rollback JDBCTransaction");
		try {
			  dbConnection.rollback();
			  freeTransaction();
			} catch (Exception ex) {}
		active = false;
	}
		
	public boolean isActive ()
	{
		return active;
	}
	
}
