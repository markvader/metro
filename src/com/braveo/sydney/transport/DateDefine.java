package com.braveo.sydney.transport;

import java.util.Calendar;

public class DateDefine implements Comparable<DateDefine>{
	private int year;
	private int month;
	private int day;
	private int wday;
	
	public DateDefine(int year, int month, int day, int wday){
		this.year = year;
		this.month = month;
		this.day = day;
		this.wday = wday;
	}
	
	/**
	 * Create a date by Calendar
	 * @param cal
	 */
	public DateDefine(Calendar cal){
		this(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_WEEK));

		wday -= 1;
		if(wday == 0)
			wday = 7;
		
	}
	
	public int compareTo(DateDefine dd) {
		
		if(this.year != -1 && dd.year != -1){
			if(this.year < dd.year)
				return -1;
			else if(this.year > dd.year)
				return 1;
		}
		
		if(this.month != -1 && dd.month != -1){
			if(this.month < dd.month)
				return -1;
			else if(this.month > dd.month)
				return 1;
		}
		
		/* day has a higher priority than wday */
		if(this.day != -1 && dd.day != -1){
			if(this.day < dd.day)
				return -1;
			else if(this.day > dd.day)
				return 1;
		}
		
		if(this.wday != -1 && dd.wday != -1){
			if(this.wday < dd.wday)
				return -1;
			else if(this.wday > dd.wday)
				return 1;
		}
			
		return 0;
	}
}