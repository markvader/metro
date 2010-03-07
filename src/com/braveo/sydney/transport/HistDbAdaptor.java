package com.braveo.sydney.transport;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Use a separated  database to store the history.
 * Because usually, people download the database from the Internet,
 * and leave their own data on there phone
 * @author Braveo Huang
 *
 */
public class HistDbAdaptor {
	private Context ctx;
	private static final String DB_NAME = "SydneyTransHistory.db";
	private static final String TABLE_NAME = "history";
	private static final int DB_VERSION = 1;
	
	private SQLiteDatabase db;
	
	/**
	 * When StopHour = -1, It will only be a selection condition
	 * When StopHour <> -1, It will be a specific line the user mark.
	 */
	private static final String[] DB_CREATE = {
		"CREATE TABLE history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"CurrentStation TEXT NOT NULL, TargetStation TEXT NOT NULL, StopHour INTEGER NOT NULL, "
			+ " StopMinute INTEGER NOT NULL, StationTimeId INTEGER NOT NULL, HistTime LONG NOT NULL);",
		"CREATE INDEX hist_time_index ON history (HistTime);",
	};

	private static final String [] EXTRA_DB_CREATE = {
	"CREATE TABLE android_metadata (locale TEXT);",
	"INSERT INTO android_metadata(locale) VALUES('en_US');"
	};

	/**
	 * Do nothing in the contructor
	 */
	private HistDbAdaptor(Context ctx){
		this.ctx = ctx;
	}
	
	private static HistDbAdaptor instance = null;
	
	public static synchronized HistDbAdaptor getInstance(Context ctx){
		if(instance==null){
			instance = new HistDbAdaptor(ctx);
		}
		instance.open();
		return instance;
	}
	
	public void update_database(){
		boolean table_exists = false;
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + "_old;");
		
		try{
			db.rawQuery("SELECT count(*) FROM " + TABLE_NAME + ";", null);
			table_exists = true;
			db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO " + TABLE_NAME + "_old;");
		}catch(SQLException e){
			//do nothing
		}

		db.execSQL("DROP INDEX IF EXISTS hist_time_index;");
		create_database();
		
		if(table_exists){
			db.execSQL("INSERT INTO " + TABLE_NAME + "(_id, CurrentStation, TargetStation, StopHour, StopMinute, StationTimeId, HistTime)" +
					" SELECT _id, CurrentStation, TargetStation, StopHour, StopMinute, StationTimeId, HistTime FROM " + TABLE_NAME +"_old;");
			db.execSQL("DROP TABLE " + TABLE_NAME +"_old;");
		}
	}
	
	public void create_database(){
		for(int k=0; k<DB_CREATE.length; k++){
			db.execSQL(DB_CREATE[k]);
		}
		db.setVersion(DB_VERSION);
	}
	
	private synchronized boolean open() throws SQLException {
		if(db==null){
			db = SQLiteDatabase.openDatabase(SydneyTransDbAdapter.DB_DIRECTORY + "/" + DB_NAME, 
					null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.OPEN_READWRITE);
			if(db.needUpgrade(DB_VERSION)){
				update_database();
			}
		}
		return true;
	}
	
	public synchronized void close() throws SQLException {
		if(db!=null)
			db.close();
		db = null;
	}

	public Cursor getAllSearchCondition(){
		return db.query(TABLE_NAME, new String[] { "_id", 
				"CurrentStation", "TargetStation"}, "StopHour = -1", 
				null, null, null, "HistTime DESC");
	}
	
	public Cursor getAllSpecificLog(){
		return db.query(TABLE_NAME, new String[] { "_id", 
				"CurrentStation", "TargetStation", "StopHour", "StopMinute", "StationTimeId"},
				"StopHour > -1", null, null, null,"HistTime DESC");
	}
	
	public boolean addHistory(String curr, String target){
		if(curr==null || target==null)
			return false;
		curr = curr.trim();
		target = target.trim();
		if(curr.length()==0 || target.length()==0)
			return false;
		
		String sql = "DELETE FROM " + TABLE_NAME + " WHERE CurrentStation=? AND TargetStation=?;";
		db.execSQL(sql, new Object[] {curr, target});
		
		sql = "INSERT INTO " + TABLE_NAME + 
			" (CurrentStation, TargetStation, StopHour, StopMinute, StationTimeId, HistTime) " +
			" VALUES(?, ?, ?, ?, ?, ?);";
		long t = java.util.Calendar.getInstance().getTimeInMillis() / 1000;
		db.execSQL(sql, new Object[] {curr, target, -1, -1, -1, t});
		return true;
	}
}
