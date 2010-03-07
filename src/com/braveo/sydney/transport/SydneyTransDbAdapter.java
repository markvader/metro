package com.braveo.sydney.transport;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SydneyTransDbAdapter {
	private Context ctx;
	//private DatabaseHelper dbHelper;
	private SQLiteDatabase db;
	
	/**
	 * Preloaded Date Ranges in a priority order.
	 */
	private List<DateRangeDefine> mDateRange = new LinkedList<DateRangeDefine>();
	
	private static final String TAG = "SydneyTransDbAdapter";
	private static final String DATABASE_NAME = "SydneyTransDataV2";
	private static final String DATATABLE_STATIONTIME = "StationTime";
	private static final String DATATABLE_TIMETABLE   = "TimeTable";
	private static final String DATATABLE_LINEDEF     = "LineDef";
	private static final int DATABASE_VERSION = 2;
	
	public static final int MESSAGE_OPEN_FINISHED = 0x234123;
	
	private static final String [] DATATABLES = { DATATABLE_STATIONTIME, DATATABLE_TIMETABLE, DATATABLE_LINEDEF };
	
	private static final String FILL_SAMPLE_DATA[] = 
	{" INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo) VALUES ('Eastern Suburbs & Illawarra', 'Waterfall or Cronulla', 'Bondi Junction');",
		" INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo) VALUES ('Eastern Suburbs & Illawarra', 'Bondi Junction', 'Waterfall or Cronulla'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Bankstown Line', 'Liverpool or Lidcombe', 'Town Hall', 'Bankstown'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Bankstown Line', 'Town Hall', 'Liverpool or Lidcombe', 'Bankstown'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Inner West Line/South', 'Campbelltown', 'Museum', 'Regents Park or Granville'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Inner West Line/South', 'Museum', 'Campbelltown', 'Regents Park or Granville'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Airport & East Hills Line', 'Macarthur', 'Town Hall', 'Airport or Sydenham'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Airport & East Hills Line', 'Town Hall', 'Macarthur', 'Airport or Sydenham'); ",
		 " INSERT INTO " + DATATABLE_LINEDEF + " (LineName, DirectFrom, DirectTo, Via) VALUES ('Airport & East Hills Line', 'Town Hall', 'Macarthur', 'Airport or Sydenham'); ",
	}
		;
		
	private static final String DATABASE_CREATE[] = 
	{"CREATE table " + DATATABLE_STATIONTIME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ " TimeTableId integer not null, StationName text not null, "
			+ " StopTimeHour byte not null, StopTimeMinute byte not null); ", 
			
			"CREATE table " + DATATABLE_TIMETABLE + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ " LineId integer not null, DayDefId integer not null, TableSeq int not null); ",
			
			"CREATE table " + DATATABLE_LINEDEF + " (_id integer primary key autoincrement, "
			+ " LineName text not null, DirectFrom text not null, DirectTo text not null, Via text); ",
			
			"CREATE TABLE DateDetailDefine (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ " Year BYTE NOT NULL, Month BYTE NOT NULL, DATE BYTE NOT NULL, WDATE BYTE NOT NULL);",
			
			/* Priority: 0 - normal, 1 - Month specific, 2 - Date specific */
			"CREATE TABLE DateDefine (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ " DateFrom INTEGER NOT NULL, DateTo INTEGER NOT NULL, Description TEXT NOT NULL, Priority BYTE NOT NULL);"
	};
	

	private static SydneyTransDbAdapter instance = null;
	
	public static synchronized SydneyTransDbAdapter getInstance(Context ctx){
		if(instance==null){
			if(!isReady())
				return null;
			instance = new SydneyTransDbAdapter(ctx);
			//instance.init();
		}
		return instance;
	}
	
	private SydneyTransDbAdapter(Context ctx)
	{
		this.ctx = ctx;
	}

	public static final String DB_DIRECTORY = "/sdcard/sydney-trans";
	public static final String DB_PATH = DB_DIRECTORY + "/SydneyTransportV2.db";
	
	public static boolean isReady() {
		File dir = new File(DB_DIRECTORY);
		
		if(!dir.exists()){
			dir.mkdirs();
			return false;
		}
		
		File path = new File(DB_PATH);
		if(!path.exists()){
			return false;
		}
		
		return true;
	}
	
	/**
	 * @deprecated
	 * 
	 * @return
	 * @throws SQLException
	 */
	private boolean init_old() throws SQLException {
		File dir = new File(DB_DIRECTORY);
		
		if(!dir.exists())
		{
			dir.mkdirs();
		}
		
		File path = new File(DB_PATH);
		if(!path.exists()){
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(path, null);
			int k;
			for(k=0; k<DATABASE_CREATE.length; k++){
				db.execSQL(DATABASE_CREATE[k]);
			}
			for(k=0; k<SydneyTransDbSql.linedef_sql.length; k++){
				db.execSQL(SydneyTransDbSql.linedef_sql[k]);
			}
			for(k=0; k<SydneyTransDbSql.linedetail_sql.length; k++){
				db.execSQL(SydneyTransDbSql.linedetail_sql[k]);
			}
			db.close();
		}
		
		return true;
		
	}
	
	private static void init() throws SQLException {
		SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
		makeTmpStationTable(db);
		db.close();
	}
	
	public synchronized SydneyTransDbAdapter open() throws SQLException
	{
		SydneyTransDbAdapter.init();
		
		//dbHelper = new DatabaseHelper(ctx);
		//db = dbHelper.getWritableDatabase();
		if(db==null){
			db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READONLY);
			loadDateRange();
		}
		return this;
	}
	
	public synchronized void close()
	{
		if(db!=null)
			db.close();
		db = null;
	}
	
	/**
	 * Create a VIEW style table, to accelerate loading the station names
	 */
	private static void makeTmpStationTable(SQLiteDatabase db){
		String sql = "";
		boolean stationNameViewExists = false;
		
		try{
			sql = "SELECT count(*) FROM TmpStationNameView";
			db.rawQuery(sql, null);
			stationNameViewExists = true;
		}catch(SQLException e){
			//do nothing.
		}
		
		if(!stationNameViewExists){
			sql = "CREATE TABLE TmpStationNameView(_id INTEGER PRIMARY KEY NOT NULL, StationName TEXT NOT NULL)";
			db.execSQL(sql);
			sql = "CREATE INDEX TmpStationNameView_StationName_index on TmpStationNameView(StationName)";
			db.execSQL(sql);
			sql = "INSERT INTO TmpStationNameView(StationName) SELECT DISTINCT StationName FROM StationTime ORDER BY StationName";
			db.execSQL(sql);
		}
	}
	
	private void loadDateRange(){
		String sql = "SELECT DateDefine._id, DateDefine.Description, ddd1.Year, ddd1.Month, ddd1.DATE, ddd1.WDATE, " +
				" ddd2.Year, ddd2.Month, ddd2.DATE, ddd2.WDATE FROM DateDefine, DateDetailDefine as ddd1, DateDetailDefine as ddd2 "
			+ " WHERE DateDefine.DateFrom = ddd1._id AND DateDefine.DateTo = ddd2._id ORDER BY DateDefine.Priority DESC";
		
		Cursor c = db.rawQuery(sql, null);
		
		c.moveToFirst();
		mDateRange = new LinkedList<DateRangeDefine>();
		
		do {
			int dateDefineId = c.getInt(0);
			String dateDefineDesc = c.getString(1);
			
			int dateFromYear = c.getInt(2);
			int dateFromMonth = c.getInt(3);
			int dateFromDay = c.getInt(4);
			int dateFromWday = c.getInt(5);
			
			int dateToYear = c.getInt(6);
			int dateToMonth = c.getInt(7);
			int dateToDay = c.getInt(8);
			int dateToWday = c.getInt(9);
			
			DateRangeDefine drd = new DateRangeDefine(dateDefineId, 
					new DateDefine(dateFromYear, dateFromMonth, dateFromDay, dateFromWday),
					new DateDefine(dateToYear, dateToMonth, dateToDay, dateToWday),
					dateDefineDesc);
			
			mDateRange.add(drd);
			
		}while(c.moveToNext());
		
		c.close();
	}
	
	private DateRangeDefine getRangeDefineByCal(Calendar cal){
		Iterator<DateRangeDefine> iter = mDateRange.iterator();
		
		while(iter.hasNext()){
			DateRangeDefine drd = iter.next();
			if(drd.inRange(cal))
				return drd;
		}
		return null;
	}
	
	public Cursor fetchAllLines()
	{
		return db.query(DATATABLE_LINEDEF, new String[] {"_id", "LineName", "DirectFrom", "DirectTo", "Via"}, 
				null, null, null, null, null);
		
	}
	
	public Cursor fetchAllStations()
	{
		//return db.query(true, DATATABLE_STATIONTIME, new String[] {"StationName"}, "", null, null, null,  "StationName",null);
		return db.rawQuery("SELECT StationName FROM TmpStationNameView ORDER BY StationName", null);
	}
	
	
	public Cursor fetchAllLinesByStation(String station, Calendar cal){
		String sql = "SELECT t1._id as _id, t1.StationName as StationName, t1.StopTimeHour as Hour, t1.StopTimeMinute as Minute, "
				+ " t2.StationName as TargetStationName, t2.StopTimeHour as TargetHour, t2.StopTimeMinute as TargetMinute, "
				+ " LineDef.LineName as LineName, LineDef.DirectFrom as DirectFrom, LineDef.DirectTo as DirectTo, LineDef.Via as Via, "
				+ " t1.TimeTableId as TimeTableId, DateDefine.Description "
				+ " FROM StationTime as t1, TimeTable, LineDef, DateDefine, StationTime as t2 "
				+ " WHERE StationTime.StationName = ? AND StationTime.TimeTableId = TimeTable._id "
				+ " AND TimeTable.LineId = LineDef._id " 
				+ " AND TimeTable.DayDefId = DateDefine._id "
				+ getOptimisedConditions(cal, getRangeDefineByCal(cal))
				+ " t1._id = t2._id"
				+ "ORDER BY t1.StopTimeHour, t1.StopTimeMinute ; "
				;
		//Log.w(TAG, sql);
		
		return db.rawQuery(sql , new String [] {station});
	}
	
	public static String getOptimisedConditions(Calendar cal, DateRangeDefine drd){
		/*if(!opt){
			return "";
		}*/
		
		//Get hour
		//java.util.Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		//int weekday = cal.get(Calendar.DAY_OF_WEEK);
		
		StringBuilder sb = new StringBuilder();
		int x1 = ( hour - 1 ) * 60 + minute;
		int x2 = ( hour + 2 ) * 60 + minute;
		if(hour>=3 && hour <= 21){
			sb.append(" AND (t1.StopTimeHour * 60 + t1.StopTimeMinute) >= " 
					+ x1 + " AND (t1.StopTimeHour * 60 + t1.StopTimeMinute) <= " + x2 + " ");
		}else if(hour<3){
			sb.append(" AND (t1.StopTimeHour * 60 + t1.StopTimeMinute) <= " + x2 + " ");
		}else {
			sb.append(" AND ( (t1.StopTimeHour * 60 + t1.StopTimeMinute) >= " + x1 
					+ " OR (t1.StopTimeHour * 60 + t1.StopTimeMinute) <= " + ( 2 * 60 )+ ") ");
		}
		
		//Log.w("Opt", "Weekday = " + weekday);
		/*if(weekday==Calendar.SATURDAY || weekday == Calendar.SUNDAY){
			sb.append(" AND TimeTable.WeekDay=0 ");
		}else{
			sb.append(" AND TimeTable.WeekDay=1 ");
		}*/
		sb.append(" AND TimeTable.DayDefId= " + drd.getRangeId() + " ");
		
		return sb.toString();
		
	}
	
	public Cursor fetchAllLinesByStation(String station, String stationTarget, Calendar cal){
		if(stationTarget==null || stationTarget.length()==0)
			return fetchAllLinesByStation(station, cal);
		
		
		String sql = "SELECT t1._id as _id, t1.StationName as StationName, t1.StopTimeHour as Hour, t1.StopTimeMinute as Minute, "
			+ " t2.StationName as TargetStationName, t2.StopTimeHour as TargetHour, t2.StopTimeMinute as TargetMinute, "
				+ " LineDef.LineName as LineName, LineDef.DirectFrom as DirectFrom, LineDef.DirectTo as DirectTo, LineDef.Via as Via, "
				+ " t1.TimeTableId as TimeTableId, DateDefine.Description "
				+ " FROM StationTime as t1, StationTime as t2, TimeTable, LineDef, DateDefine "
				+ " WHERE t1.StationName = ? AND t2.StationName = ? "
				+ " AND (t1.StopTimeHour * 60 + t1.StopTimeMinute) <= (t2.StopTimeHour * 60 + t2.StopTimeMinute) "
				+ " AND t1.TimeTableId = t2. TimeTableId "
				+ " AND t1.TimeTableId = TimeTable._id "
				+ " AND TimeTable.LineId = LineDef._id " 
				+ " AND TimeTable.DayDefId = DateDefine._id "
				+ getOptimisedConditions(cal, getRangeDefineByCal(cal))
				+"ORDER BY t1.StopTimeHour, t1.StopTimeMinute ; "
				;
		Log.w(TAG, sql + ", " + station + " : " + stationTarget);
		
		return db.rawQuery(sql , new String [] {station, stationTarget});
	}
	
	public Cursor fetchLineDetails(Long timeTableId){
		String sql = "SELECT _id, StationName, StopTimeHour as StopHour, StopTimeMinute as StopMinute "
			+ " FROM StationTime "
			+ " WHERE TimeTableId = ? ORDER BY _id ";
		
		return db.rawQuery(sql, new String[] {timeTableId.toString()});
	}
	
}
