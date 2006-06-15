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

import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.storage.SandeshaStorageException;
import org.apache.sandesha2.storage.beans.InvokerBean;

/**
 * Used to manage invoker beans.
 * 
 * @author Chamikara Jayalath <chamikaramj@gmail.com>
 * @author Sanka Samaranayaka <ssanka@gmail.com>
 */

public interface InvokerBeanMgr extends RMBeanManager {

	public boolean insert(InvokerBean bean) throws SandeshaStorageException;

	public boolean delete(String key) throws SandeshaStorageException;

	public InvokerBean retrieve(String key) throws SandeshaStorageException;

	public Collection find(InvokerBean bean) throws SandeshaStorageException;

	public InvokerBean findUnique (InvokerBean bean) throws SandeshaException;
	
	public boolean update(InvokerBean bean) throws SandeshaStorageException;

}
