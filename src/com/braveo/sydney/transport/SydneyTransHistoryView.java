package com.braveo.sydney.transport;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SydneyTransHistoryView extends ListActivity {
	public static final String BUNDLE_CURR = "CURR";
	public static final String BUNDLE_TARG = "TARG";
	
	private Cursor mHistCursor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.line_details);

		fillData();
	}

	private void fillData() {
		Cursor c = mHistCursor = HistDbAdaptor.getInstance(this).getAllSearchCondition();
		this.startManagingCursor(c);

		String[] from = new String[] { "CurrentStation", "TargetStation"};
		int[] to = new int[] { R.id.current_station, R.id.target_station };

		SimpleCursorAdapter ad = new SimpleCursorAdapter(this,
				R.layout.history_row, c, from, to);
		setListAdapter(ad);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Cursor c = mHistCursor;
		
		c.moveToPosition(position);
		
		Intent i = new Intent();
		i.putExtra(BUNDLE_CURR, c.getString(c.getColumnIndexOrThrow("CurrentStation")));
		i.putExtra(BUNDLE_TARG, c.getString(c.getColumnIndexOrThrow("TargetStation")));
		setResult(RESULT_OK, i);
		finish();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

}
