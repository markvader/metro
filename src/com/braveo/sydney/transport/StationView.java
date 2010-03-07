package com.braveo.sydney.transport;

import java.util.Calendar;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class StationView extends ListActivity {
	private String stationName;
	private String stationTarget;
	private Cursor stationCursor;
	//private boolean optimised = false;
	private Calendar mConditionCal;
	
	private SydneyTransDbAdapter dbHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.old_main);
    	
		dbHelper = SydneyTransDbAdapter.getInstance(this);
        dbHelper.open();
		
		Bundle extras = getIntent().getExtras();
		if(extras!=null){
			stationName = extras.getString("STATION_NAME");
			stationTarget = extras.getString("STATION_TARGET");
			//optimised = extras.getBoolean("OPT");
			mConditionCal = (Calendar)extras.get("CAL");
			
			if(stationName==null || stationName.length()==0){
				finish();
				return;
			}
			fillData();
		}
		
		registerForContextMenu(getListView());
		
	}

    private void fillData()
    {
    	
    	Cursor c = stationCursor = dbHelper.fetchAllLinesByStation(stationName, stationTarget, mConditionCal);
    	startManagingCursor(c);
    	
    	String[] from = new String[] { "StationName", "Hour", "Minute", "TargetStationName", "TargetHour", "TargetMinute","LineName", "DirectFrom", "DirectTo", "Via", "DateDefine.Description" };
    	int[] to = new int[] {R.id.station, R.id.stop_hour, R.id.stop_minute, R.id.target_station, R.id.target_stop_hour, R.id.target_stop_minute, R.id.line, R.id.from, R.id.to, R.id.via, R.id.daydescription};
    	
    	SimpleCursorAdapter lines = 
    		new SimpleCursorAdapter(this, R.layout.line_row, c, from, to);
    	//Log.w("StationView", lines.convertToString(c).toString());
    	final SimpleCursorAdapter.ViewBinder viewBinder = lines.getViewBinder();
    		SimpleCursorAdapter.ViewBinder vb2 = new SimpleCursorAdapter.ViewBinder(){
    			private SimpleCursorAdapter.ViewBinder proxy = viewBinder;
    			
    			public boolean setViewValue(View view, Cursor c, int ci){
    				int viewId = view.getId();
    				if(viewId == R.id.line){
    					int xh = c.getInt(c.getColumnIndexOrThrow("Hour"));
    					int xm = c.getInt(c.getColumnIndexOrThrow("Minute"));
    					
    					int xv = xh * 60 + xm;
    					
    					Calendar cu = mConditionCal;
    					int yh = cu.get(Calendar.HOUR_OF_DAY);
    					int ym = cu.get(Calendar.MINUTE);
    					
    					int yv = yh * 60 + ym;
    					
    					if(xv >= yv){    						
    						TextView tv = (TextView)view;
    						//tv.setBackgroundColor(0xFF558866);
    						tv.setTextColor(0xCCEE8600);
    						tv.setText(c.getString(ci));
    						tv.setHighlightColor(0xFFEE8644);
    						return true;
    					}
    					else
    					{
    						TextView tv = (TextView)view;
    						//tv.setBackgroundColor(0xFF1a328c);
    						tv.setTextColor(0xBBFFFFFF);
    						tv.setText(c.getString(ci));
    						tv.setHighlightColor(0xFFFFFFFF);
    						return true;
    					}	
    				}else if(viewId == R.id.stop_hour || viewId == R.id.stop_minute 
    						|| viewId == R.id.target_stop_hour || viewId == R.id.target_stop_minute){
    					String vStr = String.valueOf(c.getInt(ci));
    					if(vStr.length()==1)
    						vStr = "0" + vStr;
						((TextView)view).setText(vStr);
						return true;
    				}


    				if (proxy != null)
   						return proxy.setViewValue(view, c, ci);
   					else
   						return false;
    			}
    		};
    		lines.setViewBinder(vb2);
    	setListAdapter(lines);
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Cursor c = stationCursor;
		
		c.moveToPosition(position);
		
		Intent i = new Intent(this, LineDetailsView.class);
		i.putExtra("TimeTableId", c.getLong(c.getColumnIndexOrThrow("TimeTableId")));
		i.putExtra("CURR", stationName);
		i.putExtra("TARG", stationTarget);
		this.startActivity(i);
		
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	

}
