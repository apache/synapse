/*
 * Copyright 2007 The Apache Software Foundation.
 * Copyright 2007 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sandesha2.context;

import java.io.Serializable;

/**
 * This interface allows the context surrounding an invocation to be saved and
 * restored. This is useful when we are doing inOrder processing, as the service
 * will be dispatched by the InvokerWorker (not the original transport thread),
 * and context may have been lost by that switch.
 * 
 * Each ContextManager implementation should have a constructor that takes a
 * Axis ConfigurationContext object.
 */

public interface ContextManager {

	/**
	 * Store the current threads execution context. When embedded in an appserver,
	 * this provides a hook point to store classloaders, security context, JNDI, etc.
	 */
	public Serializable storeContext();
	
	/**
	 * Wrap the provided runnable with execution context that was previously stored.
	 * We expect the resulting runnable to be dispatched by a thread pool or other
	 * worker, and the wrapper ensures that the correct execution context will be
	 * applied.
	 */
	public Runnable wrapWithContext(Runnable work, Serializable context);
	
}
