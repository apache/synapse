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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
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
	private final SortedMap rangeMap;
	
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

		rangeMap = new TreeMap();

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
		
		long cachedKey = -1;
		//see if we get lucky on a first hit
		if(rangeMap.containsKey(new Long(msgNumber))){
			cachedKey = msgNumber;
		}
		else{
			//start at the specified index and work down the list of ranges
			//utill we find one
			Iterator iterator = rangeMap.keySet().iterator();
			
			while (iterator.hasNext()) {
				long key = ((Long)iterator.next()).longValue();
				
				if (key > cachedKey && key <= msgNumber) {
					cachedKey = key;
				}
				else if(key > msgNumber){
					//we have gone beyond the required point, return with what we have
					break;
				}
			}//end while			
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
	 * If the passed in evelopeRange encompasses several existing ranges between the start and end lookup points
	 * then these are removed from the map. All other points are added to the ongoing newRangesAdded RangeString 
	 * @param currentRange
	 * @return
	 */
	private void cleanUpRangesEnvelopedAndDiscoverNewRangesAdded(Range envelopeRange, long startOfRangeLookup, long endOfRangeLookup, RangeString newRangesAdded){
		
		boolean checkRequired = !rangeMap.isEmpty();
		if(checkRequired){
			for(long index = startOfRangeLookup; index<=endOfRangeLookup && index>0; index++ )
			{
				Long currentKey = new Long(index);
				Range existingRange = (Range)rangeMap.get(currentKey);
				if(existingRange!=null){
					if( existingRange.upperValue>envelopeRange.upperValue){
						//this range started in our envelope but stretched out beyond it so we
						//can absorb its upper value
						envelopeRange.upperValue = existingRange.upperValue;
						//we are guaranteed that there are no other ranges present underneath this existing range
						//as they would have been removed when it was first added. Therefore we can now jump our
						//pointer so that the next range we look at is after the existing range
						index = existingRange.upperValue; 
					}
					//Remove the current range from the HashMap.
					rangeMap.remove(currentKey);
				}
				else{
					//This range has not been enveloped, and therefore this is a new Range added
					if(newRangesAdded!=null){
						newRangesAdded.addRange(new Range(index, index),
																		false); //every range added will be new so there is no need for this
					}
				}		
			}			
		}
		else{
			//no check required - this must be a new range
			if(newRangesAdded!=null){
				newRangesAdded.addRange(new Range(startOfRangeLookup, endOfRangeLookup),
																false); //every range added will be new so there is no need for this
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
	
	public String toString(){
//		List sortedList = getSortedKeyList();
		String returnString = "";
//		for(int i=0; i<sortedList.size(); i++){
//			returnString = returnString + (rangeMap.get(sortedList.get(i))).toString();
//		}
		for(Iterator iter = rangeMap.entrySet().iterator();iter.hasNext();){
			Entry e = (Entry)iter.next();
			returnString = returnString + e.getValue();
		}
		
		return returnString;
	}
	
	/**
	 * @return ordered array of each range object in the string 
	 */
	public Range[] getRanges(){
		Set entrySet = rangeMap.entrySet();
		Range[] ranges = new Range[entrySet.size()];
		int i=0;
		for(Iterator iter = entrySet.iterator();iter.hasNext();){
			ranges[i] = (Range)((Entry)iter.next()).getValue();
			i++;
		}
		return ranges;
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
	
	/**
	 * Adds the Range into the existing RangeString
	 * Any existing Ranges that are encompassed in this new Range are removed.
	 * Any existing Ranges that are on either side of this Range (i.e. if this Range plugs a gap) are joined.
	 * The method returns a RangeString consisting of all the Ranges that were added that were not present previously
	 * i.e. all the new Ranges
	 * @param r
	 * @return
	 */
	public RangeString addRange(Range r){
		return addRange(r, true);
	}

	/**
	 * Adds the Range into the existing RangeString
	 * Any existing Ranges that are encompassed in this new Range are removed.
	 * Any existing Ranges that are on either side of this Range (i.e. if this Range plugs a gap) are joined.
	 * If newRangeProcessingRequired is set, the method returns a RangeString 
	 * consisting of all the Ranges that were added that were not present previously
	 * i.e. all the new Ranges
	 * @param r
	 * @return
	 */
	private RangeString addRange(Range r, boolean newRangeProcessingRequired){
		
		Range finalRange = r; //we use this to keep track of the final range
		//as we might aggregate this new range with existing ranges
		
		RangeString newRangesAdded = null;
		if(newRangeProcessingRequired){
			newRangesAdded = new RangeString(); //keep track of the ranges that have been newly filled
		}
		
		long envelopCheckingStartPoint = r.lowerValue; //used to help remove existing ranges that have been enveloped 
		
		//first we try to aggregate existing ranges
		boolean rangeAdded = false;
		long indexKey = r.lowerValue;
		//see if there is a range below that we can extend up
		Range below = getNextRangeBelow(indexKey);
		if(below!=null){
			if(below.equals(r)){
				//nothing to do
				return newRangesAdded;
			}
			if(below.upperValue<r.upperValue && below.upperValue >= (r.lowerValue -1)){
				long startingRange = below.upperValue + 1;
				//we can extend this lower range up
				below.upperValue = r.upperValue;

				//we do not quit yet, as maybe this has plugged a gap between
				//an upper range. But we should mark the range as added.
				rangeAdded = true;
				finalRange = below; //as below now encompasses both ranges agrregated together 
				
				//this action might have caused some existing ranges to be enveloped
				//so cleanup anything existing between startingRange to r.upper
				//add every other number to the newRanges string
				cleanUpRangesEnvelopedAndDiscoverNewRangesAdded(finalRange, startingRange, r.upperValue, newRangesAdded);
				envelopCheckingStartPoint = r.upperValue + 1;					
				
			}
			else if(below.upperValue >= r.upperValue){
				//the range below already covers this one - this range
				//is already complete, so we do not need to add it at all.
				return newRangesAdded;
			}
		}
		
		//see if we can extend another range down
		Range above = getRangeImmediatelyAbove(finalRange);
		if(above!=null){
			//we can extend down. Since the lower ranges take precedence, the upper range will eventually be removed.
			//Before that we might add it under a new, lower key
			Long removeKey = new Long(above.lowerValue);
			if(rangeAdded){
				//this means we extend up before. Now we are extending down to - join two ranges together.
				
				//Since we will later remove the upper, we simply set the below range to encompass both of them
				finalRange.upperValue = above.upperValue;
				//NOTE: finalRange is still what was 'below' when extending up
				
				//we need to check that there are no existing ranges from envelopCheckingStartPoint to above.lower
				//Any non-existing ranges get added to the newRangesAdded string
				cleanUpRangesEnvelopedAndDiscoverNewRangesAdded(finalRange, envelopCheckingStartPoint, above.lowerValue, newRangesAdded);
			}
			else{
				//we did not extend up but we can extend down. 
				//Add the upper range under its new key NOTE: we will remove it from under the old key later
				rangeAdded = true;				
				//we need to check that there are no existing ranges from r.lower to above.lower
				//Any non-existing ranges get added to the newRangesAdded string
				cleanUpRangesEnvelopedAndDiscoverNewRangesAdded(above, r.lowerValue, r.upperValue, newRangesAdded);
				
				above.lowerValue = r.lowerValue; //extend down
				rangeMap.put(new Long(above.lowerValue), above);
				finalRange = above;
			}
			//finally we do the remove of the above range under its old key
			rangeMap.remove(removeKey);
		}
		
		if(!rangeAdded){
			//A simple add.
			//First cleanup and add from r.lower to r.upper
			cleanUpRangesEnvelopedAndDiscoverNewRangesAdded(r, r.lowerValue, r.upperValue, newRangesAdded);
			//if we got here and did not add a range then we need to 
			//genuinely add a new range object
			rangeMap.put(new Long(r.lowerValue), r);				
		}
		
		return newRangesAdded;
		
	}
	
	

}
