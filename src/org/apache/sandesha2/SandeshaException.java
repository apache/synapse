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

package org.apache.sandesha2;

import org.apache.axis2.AxisFault;

/**
 * Exception class of Sandesa2.
 */

public class SandeshaException extends AxisFault  {

	private static final long serialVersionUID = 730653663339985226L;

	public SandeshaException (String message) {
		super (message);
	}
	
	public SandeshaException (Exception e) {
		super (e);
	}

	public SandeshaException (String message,Exception e) {
		super (message,e);
	}
	
	
	
}
