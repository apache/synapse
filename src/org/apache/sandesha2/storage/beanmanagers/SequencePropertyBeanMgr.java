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

import java.util.Collection;
import java.util.List;

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beans.SequencePropertyBean;

/**
 * Used to manage Sequence Property beans.
 */

public interface SequencePropertyBeanMgr extends RMBeanManager {

	public boolean delete(String sequenceId, String name) throws SandeshaStorageException;

	public SequencePropertyBean retrieve(String sequenceId, String name) throws SandeshaStorageException;

	public boolean insert(SequencePropertyBean bean) throws SandeshaStorageException;

	public List find(SequencePropertyBean bean) throws SandeshaStorageException;
	
	public SequencePropertyBean findUnique (SequencePropertyBean bean) throws SandeshaException;

	public boolean update(SequencePropertyBean bean) throws SandeshaStorageException;
	
	/**
	 * @deprecated
	 * 
	 * @param bean
	 * @return
	 * @throws SandeshaStorageException
	 */
	public boolean updateOrInsert(SequencePropertyBean bean) throws SandeshaStorageException;

	public Collection retrieveAll () throws SandeshaStorageException;
}
