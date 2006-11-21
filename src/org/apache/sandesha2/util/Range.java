package org.apache.sandesha2.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sandesha2.SandeshaException;
import org.apache.sandesha2.i18n.SandeshaMessageHelper;
import org.apache.sandesha2.i18n.SandeshaMessageKeys;

/**
 * Data structure to represent a range of values from lowerValue->upperValue inclusive.
 */
public class Range {

	private static final Log log = LogFactory.getLog(Range.class);
	
	long lowerValue;
	long upperValue;
	
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
