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
package org.apache.sandesha2.util;

/** 
 * log.isDebugEnabled() can be expensive. This class allows
 * for another, cheaper call to check if ANY trace or debug
 * logging is enabled for Sandesha2.
 * 
 * This is pluggable to allow integration with whatever trace
 * mechanism is provided by the environment Sandesha2 is
 * deployed into.
 * 
 * There is a default controller which simply returns false
 * which can be engaged by setting the Sandesha2.LoggingControl.ProhibitDebugLogging
 * system property.
 */
public class LoggingControl {

	public interface LoggingController{
		public boolean isAnyTracingEnabled();
	}
	
	private static class OffController implements LoggingController{
		public boolean isAnyTracingEnabled() {
			return false;
		}
	}
	
	private static LoggingController controller;
	
	static{
		String prop = null;
        try {
            prop = System.getProperty("Sandesha2.LoggingControl.ProhibitDebugLogging");
        } catch (SecurityException SE) {
            //do nothing
        }
        if(prop!=null){
        	controller = new OffController();
        }
	}
	
	public static void setController(LoggingController lc){
		controller = lc;
	}
	
	public static boolean isAnyTracingEnabled(){
		return (controller==null)?true:controller.isAnyTracingEnabled();
	}
}
