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

package org.apache.sandesha2.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Helper class.
 * Enables conversion to from a list of Range objects to a String representation
 * of that list.
 * Also performs task such as aggregation of ranges
 *
 */
public class RangeString {

	/**
	 * Each entry in this map is a range
	 * The key to each range entry is range.lowerValue
	 */
	private Map rangeMap;
	
	/**
	 * Expects a String of the form
	 * [x1,y1][x2,y2]...[xn,yn]
	 * @param s
	 */
	public RangeString(String s){

		rangeMap = Collections.synchronizedMap(new HashMap());

		if(s!=null && !s.equals("")){
			//Walk the string building range objects as we go, and
			//put them in the map
			Pattern p = Pattern.compile("\\[(.+?),(.+?)\\]");
			Matcher m = p.matcher(s);
			while(m.find()){
				String token = m.group();
				addRange(new Range(token));
			}			
		}
		
	}
	
	
	private Range getNextRangeBelow(long msgNumber){
		//start at the specified index and work down the list of ranges
		//util we find one
		
		for(long i = msgNumber; i>=0; i--){
			Range r = (Range)rangeMap.get(new Long(i));
			if(r!=null){
				//this is the next range below
				return r;
			}
		}
		//no range below this one
		return null; 
	}
	
	private Range getRangeImmediatelyAbove(long msgNumber){
		//see if there is a range that starts imemdiately
		//above the specified number
		long targetRange = msgNumber + 1;
		return (Range)rangeMap.get(new Long(targetRange));
	}
	
	
	public boolean isMessageNumberInRanges(long messageNumber){
		Range below = getNextRangeBelow(messageNumber);
		if(below!=null){
			if(below.rangeContainsValue(messageNumber)){
				//this range contains our value
				return true;
			}
		}
		
		//if we made it here then we are not in any ranges
		return false;
	}
	
	/**
	 * Returns a String representation of the ranges contained in this object
	 * @return a String of the form [x1,y1][x2,y2]...[xn,yn]
	 */
	public String toString(){
		//iterate the rangeList creating an on-going string
		Set keySet = rangeMap.keySet();
		//sort the set
		List sortedList = new LinkedList(keySet);
		Collections.sort(sortedList);
		String returnString = "";
		for(int i=0; i<sortedList.size(); i++){
			returnString = returnString + (rangeMap.get(sortedList.get(i))).toString();
		}
		
		return returnString;
	}
	
	/**
	 * Returns a list string of the form
	 * [x1,x2,x3....xn] listing each discrete number contained in all of the ranges
	 * in order
	 */
	public String getContainedElementsAsListString(){
		//iterate the rangeList creating an on-going string
		Set keySet = rangeMap.keySet();
		//sort the set
		List sortedList = new LinkedList(keySet);
		Collections.sort(sortedList);
		String returnString = "[";
		for(int i=0; i<sortedList.size(); i++){
			Range r = (Range)rangeMap.get(sortedList.get(i));
			for(long l=r.lowerValue; l<=r.upperValue;l++){
				if(i==0 && l==r.lowerValue){
					//first time does not need leading ','
					returnString += l;						
				}
				else{
					returnString += "," + l;						
				}
			}
		}
		
		return returnString + "]";		
	}
	
	public void addRange(Range r){
		//first we try to aggregate existing ranges
		boolean rangeAdded = false;
		long indexKey = r.lowerValue;
		//see if there is a range below that we can extend up
		Range below = getNextRangeBelow(indexKey);
		if(below!=null){
			if(below.upperValue == (r.lowerValue -1)){
				//we can extend this range up
				below.upperValue = r.upperValue;
				//we do not quit yet, as maybe this has plugged a gap between
				//an upper range. But we should mark the range as added.
				rangeAdded = true;
			}
		}
		
		//see if we can extend another range down
		Range above = getRangeImmediatelyAbove(r.upperValue);
		if(above!=null){
			//we can extend this down
			//first remove it. Then we will either add it under its new key or 
			//keep it removed
			rangeMap.remove(new Long(above.lowerValue));
			above.lowerValue = r.lowerValue; //extend down
			if(rangeAdded){
				//we extend down and up - join two ranges together
				//Sicne we have removed the upper, we simply do not add it again and set the
				//lower range to encompass both of them
				below.upperValue = above.upperValue;
			}
			else{
				//we did extend up but we did not extend down. Add the upper range back under its new key
				rangeAdded = true;				
				rangeMap.put(new Long(above.lowerValue), above);
			}

		}
		
		if(!rangeAdded){
			//if we got here and did not add a range then we need to 
			//genuinely add a new range object
			rangeMap.put(new Long(r.lowerValue), r);			
		}

		
	}
	

}
