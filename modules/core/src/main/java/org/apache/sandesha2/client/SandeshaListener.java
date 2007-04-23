/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *  
 */

package org.apache.sandesha2.client;

import org.apache.axis2.AxisFault;

/**
 * By implementing this interface and registering an object with
 * Sandesha2, users will be invoked in some events.
 */

public interface SandeshaListener {

	/**
	 * This sill be invoked when Sandesha2 receive a fault message
	 * in response to a RM control message that was sent by it.
	 */
	public void onError(AxisFault fault);
	
	/**
	 * This will be invoked when a specific sequence time out.
	 * The timing out method depends on policies.
	 */
	public void onTimeOut(SequenceReport report);
	
}
