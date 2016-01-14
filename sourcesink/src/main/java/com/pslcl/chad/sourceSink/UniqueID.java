package com.pslcl.chad.sourceSink;

import java.util.Date;
import java.util.Random;
/**
 * Starts at a random value between 0 and 3294967295. Increments whenever {@link #getNewID()} is called.
 * @author dethington
 *
 */
public class UniqueID {
	private static Long valueSetID = 0L;
	private static Long last = 0L;
	private static boolean incrementing = false;
	
	/**
	 * Sets the starting value of the ID and sets {@link UniqueID#setIncrementing(boolean)} to true
	 * @param startHere 
	 * @throws Exception if passed a null value or value is outside range of 0 to 3294967295.
	 */
	public static void setUniqueID(Long startHere) throws Exception{
		synchronized(valueSetID){
			if(startHere == null) throw new Exception("Invalid ValueSetID. Must be between 0 and 4294967295. Your value: null");
			if (startHere >= 0L && startHere <= 4294967295L) valueSetID = startHere;
			else throw new Exception("Invalid ValueSetID. Must be between 0 and 4294967295. Your value: " + startHere);
			incrementing = true;
		}
	}
	
	/**
	 * Start or stop incrementing return values from {@link UniqueID#getNewID()}
	 * @param incrementing (<b>boolean</b>)
	 */
	public static void setIncrementing(boolean incrementing){
		UniqueID.incrementing = incrementing;
		if(incrementing){
			valueSetID = randomLong(1000000000L);
		}
	}
	
	/**
	 * If {@link UniqueID#setIncrementing(boolean)} is set to true, returns an incrementing value.  
	 * If false, returns a random value.
	 * @return {@link Long} value between 0 and 4294967295. 
	 */
	public static Long getNewID(){
		synchronized(valueSetID){
			if(!incrementing){
				valueSetID = randomLong(0L);
				if(valueSetID == last) valueSetID += randomLong(4000000000L);
				last = valueSetID;
				return valueSetID;
			}
		
			Long id = ++valueSetID;
			if(id > 4294967295L) id = 0L;
			return id;
		}
	}
	
	private static Long randomLong(Long rangeLimit){
		Random rng = new Random(new Date().getTime());
		Long max = 4294967295L - rangeLimit;
		long randomNum, bits;
		do {
		      bits = (rng.nextLong() << 1) >>> 1;
		      randomNum = bits % max;
		} while (bits - randomNum +(max -1) < 0L);

		return randomNum;
	}
}
