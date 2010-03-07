package com.braveo.sydney.transport;

import java.util.Calendar;

/**
 * DateRangeDefine represents the Date Definition in 
 * the database.
 * 
 * Note that Week Days:
 * 
 * Monday = 1
 * Tuesday = 2
 * ...
 * Saturday = 6
 * Sunday = 7
 * 
 * are different from Calendar.DAY_OF_WEEK
 * 
 * @author Braveo Huang
 *
 */
public class DateRangeDefine {
	private int rangeId;
	private DateDefine dateFrom;
	private DateDefine dateTo;
	private String desc;
	
	public DateRangeDefine(int id, DateDefine from, DateDefine to, String desc){
		this.rangeId = id;
		this.dateFrom = from;
		this.dateTo = to;
		this.desc = desc;
	}
	
	public boolean inRange(Calendar cal){
		DateDefine dd = new DateDefine(cal);
		
		if(dateFrom.compareTo(dd) == 1)
			return false;
		
		if(dateTo.compareTo(dd) == -1)
			return false;
		
		return true;
	}
	
	public int getRangeId(){
		return rangeId;
	}
}
