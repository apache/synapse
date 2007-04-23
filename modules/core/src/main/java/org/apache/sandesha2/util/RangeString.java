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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
public class RangeString implements Serializable{

	private static final long serialVersionUID = -3487094584241136861L;
	/**
	 * Each entry in this map is a range
	 * The key to each range entry is range.lowerValue
	 */
	private final Map rangeMap;
	
	/**
	 * Creates an empty range string
	 */
	public RangeString(){
		this(null);
	}
	
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
		Iterator iterator = rangeMap.keySet().iterator();
		
		long cachedKey = -1;
		while (iterator.hasNext()) {
			long key = ((Long)iterator.next()).longValue();
			
			if (key > cachedKey && key <= msgNumber) {
				cachedKey = key;
			}
		}
		
		if (cachedKey != -1) {
			//this is the next range below
			Range r = (Range)rangeMap.get(new Long(cachedKey));
			return r;
		}
		
		//no range below this one
		return null; 
	}
	
	/**
	 * If the passed in evelopeRange encompasses several ranges, these are 
	 * removed from the map 
	 * @param currentRange
	 * @return
	 */
	private void cleanUpRangesEnveloped(Range envelopeRange){
		//see if there are any ranges that start at some point between 
		//immediately above the start of the envelope range up to 
		//its end 
		long startOfRangeLookup = envelopeRange.lowerValue + 1;
		long endOfRangeLookup = envelopeRange.upperValue;
		// Iterator over the available ranges.
		Iterator ranges = rangeMap.keySet().iterator();
		while (ranges.hasNext()) {
			// Get the key
			long key = ((Long)ranges.next()).longValue();
			if (key >= startOfRangeLookup && key <= endOfRangeLookup) {
				Range removedRange = (Range)rangeMap.get(new Long(key));
				
				if(removedRange!=null && removedRange.upperValue>envelopeRange.upperValue){
					//this range started in our envelope but stretched out beyond it so we
					//can absorb its upper value
					envelopeRange.upperValue = removedRange.upperValue;
				}
				// Remove the current range from the HashMap.
				ranges.remove();
			}
		}
	}
	
	/**
	 * Looks to see if there is a range that starts immediately above
	 * this one. If so, that range is returned 
	 * @param targetRange
	 * @return
	 */
	private Range getRangeImmediatelyAbove(Range targetRange){
		return (Range)rangeMap.get(new Long(targetRange.upperValue + 1));		
	}
	
	
	public boolean isMessageNumberInRanges(long messageNumber){
		if(getRangeForMessageNumber(messageNumber)!=null){
			return true;
		}
		
		return false;
	}
	
	public Range getRangeForMessageNumber(long messageNumber){
		Range below = getNextRangeBelow(messageNumber);
		if(below!=null){
			if(below.rangeContainsValue(messageNumber)){
				//this range contains our value
				return below;
			}
		}
		
		//if we made it here then we are not in any ranges
		return null;		
	}
	
	/**
	 * Returns true if the numbers are contained in a single range
	 * @param interestedRange
	 * @return
	 */
	public boolean isRangeCompleted(Range interestedRange){
		Range containingRange = getNextRangeBelow(interestedRange.lowerValue);
		if(containingRange!=null){
			//so we know there is a range below us, check to see if it
			//stretches to us or over above us
			if(containingRange.upperValue>=interestedRange.upperValue){
				//it does, so this range is contained
				return true;
			}
		}
		//either their was no range at all or it did not reach high enough 
		return false;
	}
	
	/**
	 * Returns a String representation of the ranges contained in this object
	 * @return a String of the form [x1,y1][x2,y2]...[xn,yn]
	 */
	public String toString(){
		List sortedList = getSortedKeyList();
		String returnString = "";
		for(int i=0; i<sortedList.size(); i++){
			returnString = returnString + (rangeMap.get(sortedList.get(i))).toString();
		}
		
		return returnString;
	}
	
	/**
	 * @return ordered array of each range object in the string 
	 */
	public Range[] getRanges(){
		List sortedKeyList = getSortedKeyList();
		Range[] ranges = new Range[sortedKeyList.size()];
		for(int i=0; i<ranges.length; i++){
			ranges[i] = (Range)rangeMap.get(sortedKeyList.get(i));
		}
		return ranges;
	}
	
	
	private List getSortedKeyList(){
		Set keySet = rangeMap.keySet();
		//sort the set
		List sortedList = new LinkedList(keySet);
		Collections.sort(sortedList);
		return sortedList;
	}
	
	/**
	 * Returns a List of the form
	 * [x1,x2,x3....xn] listing each discrete number contained in all of the ranges
	 * in order
	 * NOTE: inefficient, should be avoided
	 */
	public List getContainedElementsAsNumbersList(){
		List returnList = new LinkedList();
		Range[] ranges = getRanges();
		for(int i=0; i<ranges.length; i++){
			for(long current = ranges[i].lowerValue; current<=ranges[i].upperValue; current++){
				returnList.add(new Long(current));
			}
		}
		return returnList;
	}
	
	public void addRange(Range r){
		
		Range finalRange = r; //we use this to keep track of the final range
		//as we might aggregate this new range with existing ranges
		
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
				finalRange = below; //as below now encompasses both ranges agrregated together 
			}
			else if(below.upperValue > r.lowerValue){
				//the range below extends over this one - this range
				//is already complete, so we do not need to add it at all.
				return;
			}
		}
		
		//see if we can extend another range down
		Range above = getRangeImmediatelyAbove(r);
		if(above!=null){
			//we can extend this down
			//first remove it. Then we will either add it under its new key or 
			//keep it removed
			rangeMap.remove(new Long(above.lowerValue));
			above.lowerValue = r.lowerValue; //extend down
			if(rangeAdded){
				//we extend down and up - join two ranges together
				//Sicne we have removed the upper, we simply do not add it again and set the
				//below range to encompass both of them
				below.upperValue = above.upperValue;
				//NOTE: finalRange has already been set when extending up
			}
			else{
				//we did not extend up but we can extend down. 
				//Add the upper range back under its new key
				rangeAdded = true;				
				rangeMap.put(new Long(above.lowerValue), above);
				finalRange = above;
			}

		}
		
		if(!rangeAdded){
			//if we got here and did not add a range then we need to 
			//genuinely add a new range object
			rangeMap.put(new Long(r.lowerValue), r);
		}
		
		//finally, we go through the new range we have added to make sure it
		//does not now encompass any smaller ranges that were there before (but
		//that could not be extended up or down)
		cleanUpRangesEnveloped(finalRange);
		
	}
	
	

}
