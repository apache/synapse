/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.transaction;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transaction.TransactionConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.mediators.AbstractMediator;

import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * The Mediator for commit, rollback, suspend, resume jta transactions
 */
public class TransactionMediator extends AbstractMediator {

    public static final String ACTION_COMMIT = "commit";
    public static final String ACTION_ROLLBACK = "rollback";
    public static final String ACTION_SUSPEND = "suspend";
    public static final String ACTION_RESUME = "resume";
    public static final String ACTION_NEW = "new";
    public static final String ACTION_USE_EXISTING_OR_NEW = "use-existing-or-new";
    public static final String ACTION_FAULT_IF_NO_TX = "fault-if-no-tx";
    public static final String SUSPENDED_TRANSACTION = "suspendedTransaction";

    private String action = "";

    public boolean mediate(MessageContext synCtx) {

        TransactionManager transactionManager;
        final SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Transaction mediator (" + action + ")");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        transactionManager = getTransactionManager(synCtx);

        if (action.equals(ACTION_COMMIT)) {

            try {
                transactionManager.commit();
            } catch (Exception e) {
                handleException("Unable to commit transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_ROLLBACK)) {

            try {
                transactionManager.rollback();
            } catch (Exception e) {
                handleException("Unable to rollback transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_NEW)) {

            int status = Status.STATUS_UNKNOWN;
            try {
                status = transactionManager.getStatus();
            } catch (Exception e) {
                handleException("Unable to query transaction status", e, synCtx);
            }

            if (status != Status.STATUS_NO_TRANSACTION) {
                throw new SynapseException("Require to begin a new transaction, " +
                        "but a tansaction already exist");
            }

            try {
                transactionManager.begin();
            } catch (Exception e) {
                handleException("Unable to begin a new transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_USE_EXISTING_OR_NEW)) {

            int status = Status.STATUS_UNKNOWN;
            try {
                status = transactionManager.getStatus();
            } catch (Exception e) {
                handleException("Unable to query transaction status", e, synCtx);
            }

            try {
                if (status == Status.STATUS_NO_TRANSACTION) {
                    transactionManager.begin();
                }
            } catch (Exception e) {
                handleException("Unable to begin a new transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_FAULT_IF_NO_TX)) {

            int status = Status.STATUS_UNKNOWN;
            try {
                status = transactionManager.getStatus();
            } catch (Exception e) {
                handleException("Unable to query transaction status", e, synCtx);
            }

            if (status != Status.STATUS_ACTIVE)
                throw new SynapseException("No active transaction. Require an active transaction");

        } else if (action.equals(ACTION_SUSPEND)) {

            try {
                Transaction tx = transactionManager.suspend();
                synCtx.setProperty(SUSPENDED_TRANSACTION, tx);
            } catch (Exception e) {
                handleException("Unable to suspend transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_RESUME)) {

            Transaction tx = (Transaction) synCtx.getProperty(SUSPENDED_TRANSACTION);
            if (tx == null)
                handleException("Couldn't find a suspended transaction to resume", synCtx);
            try {
                transactionManager.resume(tx);
            } catch (Exception e) {
                handleException("Unable to resume transaction", e, synCtx);
            }

        } else {
            handleException("Invalid transaction mediator action : " + action, synCtx);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Transaction mediator");
        }

        return true;
    }

    private TransactionManager getTransactionManager(MessageContext synCtx) {

        TransactionManager transactionManager = null;

        try {    
            TransactionConfiguration transactionConfiguration = synCtx.getConfiguration()
                    .getAxisConfiguration().getTransactionConfiguration();

            if (transactionConfiguration != null) {
                transactionManager = transactionConfiguration.getTransactionManager();
            } else {
                handleException("TransactionConfiguration has not been found. " +
                        "Please check the axis2.xml and uncomment/enable the " +
                        "transaction configuration.", synCtx);
            }
            
        } catch (AxisFault ex) {
            handleException("Unable to get Transaction Manager", ex, synCtx);
        }

        return transactionManager;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

}
