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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Data structure to represent a range of values from lowerValue->upperValue inclusive.
 */
public class Range implements Serializable{

	private static final long serialVersionUID = 1701339894096240940L;

	private static final Log log = LogFactory.getLog(Range.class);
	
	public long lowerValue;
	public long upperValue;
	
	/**
	 * Create a range for a single number
	 * @param value
	 */
	public Range(long value){
		this(value, value);
	}
	
	/**
	 * 
	 * @param low
	 * @param high
	 * 
	 * NOTE: low and high can be equal
	 */
	public Range(long low, long high){
		if(high<low || high<0 || low<0){
			throw new IllegalArgumentException(SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidRange, ""+(low), ""+(high)));
		}
		lowerValue = low;
		upperValue = high;
	}
	
	/**
	 * Construct a range from a String object
	 * @param s a String of the form [lower, upper]
	 * NOTE: lower and upper can be equal
	 */
	public Range(String s){
		s = s.trim();

		int length = s.length();

		if (s.charAt(0) != '[' || s.charAt(length - 1) != ']') {
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidStringArray, s);
			log.debug(message);
			throw new IllegalArgumentException(message);
		}
		
		//remove the braces on either end
		String subStr = s.substring(1, length - 1);

		String[] parts = subStr.split(",");

		if(parts.length!=2){
			String message = SandeshaMessageHelper.getMessage(
					SandeshaMessageKeys.invalidStringArray, s);
			log.debug(message);
			throw new IllegalArgumentException(message);
		}
		
		lowerValue = Long.parseLong(parts[0]);
		upperValue = Long.parseLong(parts[1]);
	}

	public boolean equals(Object o){
		boolean returnValue = false;
		if(o instanceof Range){
			Range testRange = (Range)o;
			if(testRange.lowerValue== this.lowerValue && testRange.upperValue == this.upperValue){
				returnValue = true;
			}
		}
		return returnValue;
	}
	
	
	/**
	 * Value is considered to be "in range" if it is with the limits set by the
	 * upper and lower values.
	 * e.g. [x, x+n] would return true for all values in the set [x...x+n]
	 */
	public boolean rangeContainsValue(long value){
		if(value<=upperValue && value>=lowerValue){
			return true;
		}
		else return false;
	}
	
	public String toString(){
		return "[" + lowerValue + "," + upperValue + "]";
	}
	
}
