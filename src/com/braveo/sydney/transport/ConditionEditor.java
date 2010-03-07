package com.braveo.sydney.transport;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConditionEditor extends Activity implements OnClickListener {
	
	public static final String EDIT_CONDITION_ACTION = "com.braveo.sydney.transport.action.EDIT_CONDITION";

	private static final String[] PROJECTION = new String[] {
		SydneyTransport.CONDITION, // 0
	};
	
	private Cursor mCursor;
	private EditText mText;
	private Uri mUri;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.search_line);
		mUri = getIntent().getData();
		mCursor = this.managedQuery(mUri, PROJECTION, null, null, null);
		
		Button b = (Button) findViewById(R.id.ok);
		b.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(mCursor != null){
			mCursor.moveToFirst();
			mText.setText("Hello World");
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if(mCursor != null) {
			ContentValues values = new ContentValues();
			values.put(SydneyTransport.CONDITION, mText.getText().toString());
			getContentResolver().update(mUri, values, null, null);
		}
			
	}
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		finish();
	}

}
