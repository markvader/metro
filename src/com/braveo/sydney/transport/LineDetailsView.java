package com.braveo.sydney.transport;

import android.app.ListActivity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class LineDetailsView extends ListActivity {
	private Long timeTableId;
	private SydneyTransDbAdapter dbHelper;
	private String mCurrStation;
	private String mTargStation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.line_details);

		dbHelper = SydneyTransDbAdapter.getInstance(this);
        dbHelper.open();

		Bundle extras = this.getIntent().getExtras();
		
		if(extras != null)	{
			timeTableId = extras.getLong("TimeTableId");
			mCurrStation = extras.getString("CURR");
			mTargStation = extras.getString("TARG");
			if(timeTableId != null){
				fillData();
			}
		}
	}
	
	private void fillData() {		
		Log.w("DD", "" + timeTableId);
		
		Cursor c = dbHelper.fetchLineDetails(timeTableId);
		this.startManagingCursor(c);
		
		String[] from = new String [] { "StationName", "StopHour", "StopMinute" };
		int[] to   = new int[]     { R.id.station_name, R.id.stop_hour, R.id.stop_minute };
		
		SimpleCursorAdapter ad = new SimpleCursorAdapter(this, R.layout.station_row, c, from, to);
		final SimpleCursorAdapter.ViewBinder viewBinder = ad.getViewBinder();
		ad.setViewBinder(new SimpleCursorAdapter.ViewBinder(){
			private SimpleCursorAdapter.ViewBinder proxy = viewBinder;
			
			public boolean setViewValue(View view, Cursor c, int ci){
				int viewId = view.getId();
				
				if(viewId == R.id.station_name){
					if(mCurrStation!=null && mTargStation!=null){
						String x = c.getString(ci);
						TextView tv = (TextView)view;
						if(x.equals(mCurrStation) || x.equals(mTargStation)){
							tv.setBackgroundColor(0xFFD6DFF7);
							tv.setTextColor(0xFF000080);
							//view.setBackgroundColor(0x1a328c);
							tv.setText(x);
							//tv.setText(Html.fromHtml("<font color=\"#1a328c\">This is a</font> <font color=\"#0000FF\">test.</font>"));
							return true;
						}
						else
						{
							tv.setBackgroundColor(0x0);
							tv.setTextColor(0x88FFFFFF);
							tv.setText(x);
							return true;
						}
					}
				}else if(viewId == R.id.stop_hour || viewId == R.id.stop_minute){
					String x = String.valueOf(c.getInt(ci));
					if(x.length()==1){
						x = "0" + x;
					}
					((TextView)view).setText(x);
					return true;
				}
					

				if (proxy != null)
					return proxy.setViewValue(view, c, ci);
				else
					return false;
			}			
		});
		setListAdapter(ad);
		
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	

}
