/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.sandesha2.storage.beanmanagers;

import java.util.List;

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beans.CreateSeqBean;

/**
 * This is used to manage CreateSequence beans.
 */


public interface CreateSeqBeanMgr extends RMBeanManager {

	public boolean insert(CreateSeqBean bean) throws SandeshaStorageException;

	public boolean delete(String msgId) throws SandeshaStorageException;

	public CreateSeqBean retrieve(String msgId) throws SandeshaStorageException;

	public boolean update(CreateSeqBean bean) throws SandeshaStorageException;

	public List find(CreateSeqBean bean) throws SandeshaStorageException;
	
	public CreateSeqBean findUnique (CreateSeqBean bean) throws SandeshaException;

}