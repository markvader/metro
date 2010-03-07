package com.braveo.sydney.transport;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TimePicker;

public class SydneyTransport extends Activity {

    public static final String CONDITION = "Source Station";
	
	private SydneyTransDbAdapter dbHelper;
	
	private static final int MENU_QUIT_ID = Menu.FIRST;
	private static final int MENU_SHOW_LINE_ID = Menu.FIRST + 1;
	
	private static List<String> stationList = new ArrayList<String> ();
	private VirtualKeyboard mKeyboard;
	
	private static final int DIALOG_DATABASE_NOT_READY = 0x0010001;

	private HttpDownloader mDownloader = null;
	private ProgressDialog mProgressDialog = null;
	private Handler mProgressHandler = null;
	private int mContentLength = -1;
	private int mDialogId = 0;
	
	private Handler mUnzipHandler = null;
	//private UnzipJob mUnzipJob = null;
	
	private Handler mGeneralHandler = null;
	
	private String mProgressTitle = "";

	private static final int DIALOG_DOWNLOAD_INIT_FAILED = 0x10021;
	private static final int DIALOG_DOWNLOAD_PROGRESS_BAR = 0x10022;
	private static final int DIALOG_DOWNLOAD_PROGRESS_CIRCLE = 0x10023;
	private static final int DIALOG_DATABASE_DOWNLOAD_FAILED = 0x10024;
	private static final int DIALOG_DATE_DIALOG			= 0x10025;
	private static final int DIALOG_TIME_DIALOG			= 0x10026;
	
	private static final int ACITIVITY_DOWNLOAD_FAILED_HELP = 0x11001;
	private static final int ACITIVITY_DATABASE_NOT_READY_HELP = 0x11002;
	
	private static final int ACTIVITY_HISTORY = 0x10001;
	private static final int ACTIVITY_DOWN_CHOOSE = 0x10002;
	
	private static final int MENU_DOWNLOAD_NEW_DATABASE = Menu.FIRST;
	private static final int MENU_HELP = Menu.FIRST + 1;
	private static final int MENU_ABOUT = Menu.FIRST + 2;
	
	public static final String DB_DOWNLOAD_BASE = "http://www.markvader.com/files";
	private String mDbDownUrl = null;
	
	
	private int mCustomisedYear = 0;
	private int mCustomisedMonth = 0;
	private int mCustomisedDay = 0;
	
	private int mCustomisedHour = 0;
	private int mCustomisedMinute = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mKeyboard = new VirtualKeyboard(this);
        mKeyboard.setVisibility(false);
        
        OnFocusChangeListener textViewListener = new OnFocusChangeListener() {
        	public void onFocusChange(View v, boolean hasFocus){
    			mKeyboard.setInputTarget(v);
        		if(v.getId() == R.id.current_station && hasFocus == true){
        			((RadioButton)findViewById(R.id.radio_current)).setChecked(true);
        		}
        		else if(v.getId() == R.id.target_station && hasFocus == true)
        			((RadioButton)findViewById(R.id.radio_target)).setChecked(true);
        		
        	}
        };
        
        AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.current_station);
        
        textView.setOnFocusChangeListener(textViewListener);
        textView.requestFocus();

        textView = (AutoCompleteTextView)findViewById(R.id.target_station);
        textView.setOnFocusChangeListener(textViewListener);
        
        RadioButton radio = (RadioButton)findViewById(R.id.radio_current);
        radio.setChecked(true);
        
        ((CheckBox)findViewById(R.id.checkbox_optimised)).setChecked(true);
        
        OnClickListener radio_listener = new OnClickListener() {
        	public void onClick(View v){
        		if(v.getId()==R.id.radio_current)
        			findViewById(R.id.current_station).requestFocus();
        		else
        			findViewById(R.id.target_station).requestFocus();
        	}
        };
        
        radio.setOnClickListener(radio_listener);
        
        ((RadioButton)findViewById(R.id.radio_target)).setOnClickListener(radio_listener);
    
        Button searchButton = (Button)findViewById(R.id.BSearch);
        searchButton.setOnClickListener(new View.OnClickListener(){
        	public void onClick(View view){
    			AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.current_station);
    			String current_station = textView.getText().toString().trim();
    			
    			textView = (AutoCompleteTextView)findViewById(R.id.target_station);
    			String target_station = textView.getText().toString().trim();
    			
    			if(current_station.length()==0)
    				return;
    			
    			CheckBox cbOpt = (CheckBox)findViewById(R.id.checkbox_optimised);
    			if(cbOpt.isChecked()){
    				doSearch(Calendar.getInstance());
    			}
    			else{
    				SydneyTransport.this.initCustomisedDateTime();
    				SydneyTransport.this.showDialog(SydneyTransport.DIALOG_DATE_DIALOG);
    			}
        	}
        });
        
        ((Button)findViewById(R.id.BHist)).setOnClickListener(new View.OnClickListener(){
        	public void onClick(View view){
        		Intent x = new Intent(SydneyTransport.this, SydneyTransHistoryView.class);
        		SydneyTransport.this.startActivityForResult(x, ACTIVITY_HISTORY);
        	}
        });
        
        if(SydneyTransDbAdapter.isReady()){
        	this.setupDatabase();
        }else{
        	showDialog(DIALOG_DATABASE_NOT_READY);
        }

    }
    
    protected void doSearch(Calendar cal){
		AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.current_station);
		String current_station = textView.getText().toString().trim();
		
		textView = (AutoCompleteTextView)findViewById(R.id.target_station);
		String target_station = textView.getText().toString().trim();
		
		if(current_station.length()==0)
			return;
		
        Intent i = new Intent(SydneyTransport.this, StationView.class);
        i.putExtra("STATION_NAME", current_station);
        i.putExtra("STATION_TARGET", target_station);
        i.putExtra("CAL", cal);
		HistDbAdaptor.getInstance(SydneyTransport.this).addHistory(current_station, target_station);
        startActivity(i);   	
    }
    
    @Override
	protected Dialog onCreateDialog(int id) {
    	switch(id) {
    	case DIALOG_DATABASE_DOWNLOAD_FAILED:
    		return new AlertDialog.Builder(SydneyTransport.this)
    			.setTitle("Download failed")
    			.setMessage("Download failed due to the network problem. "
    					+ "Please download the timetable later." 
    					+ "Or write to the programmer about the problem."
    					+ "\n\n"
    					+ "Software Website:\nhttp://www.markvader.com\n\n"
    					+ "Enquiry E-mail:\nmarkjbreen@gmail.com\n\n"
    					)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
			    					if(!SydneyTransDbAdapter.isReady())
			    						System.exit(0);
								}
							})
					.setNeutralButton("Help",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Intent x = new Intent(SydneyTransport.this, HelpView.class);
									SydneyTransport.this.startActivityForResult(x, ACITIVITY_DOWNLOAD_FAILED_HELP);
								}
							})
							.setCancelable(false)
							.create();
    	case DIALOG_DATABASE_NOT_READY:
    		return new AlertDialog.Builder(SydneyTransport.this)
    			.setTitle("Download timetable right now?")
    			.setMessage("A timetable database is required to run Sydtrans. "
    					+ "You can choose to download right now, or later. " 
    					+ "You can also download the timetable manually."
    					+ "\n\n"
    					+ "You're recommended to download with WiFi network.\n\n"
    					+ "Software Website:\nhttp://www.markvader.com\n\n"
    					+ "Enquiry E-mail:\nmarkjbreen@gmail.com\n\n"
    					)
    			.setPositiveButton("List Databases", new DialogInterface.OnClickListener(){
    				public void onClick(DialogInterface dialog, int whichButton){
    					SydneyTransport.this.prepareRetrieveDatabase();
    				}
    			})
    			.setNegativeButton("Later", new DialogInterface.OnClickListener(){
    				public void onClick(DialogInterface dialog, int whichButton){
    					if(!SydneyTransDbAdapter.isReady())
    						System.exit(0);
    				}
    			})
				.setNeutralButton("Help",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								Intent x = new Intent(SydneyTransport.this, HelpView.class);
								SydneyTransport.this.startActivityForResult(x, ACITIVITY_DATABASE_NOT_READY_HELP);
							}
						})
    			.setCancelable(false)
    			.create();
		case DIALOG_DOWNLOAD_INIT_FAILED:
			return new AlertDialog.Builder(this).setTitle("Downloading Error")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
			    					if(!SydneyTransDbAdapter.isReady())
			    						System.exit(0);
								}
							})
					.setNeutralButton("Help",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Intent x = new Intent(SydneyTransport.this, HelpView.class);
									SydneyTransport.this.startActivityForResult(x, ACITIVITY_DOWNLOAD_FAILED_HELP);
								}
							})
							.setCancelable(false)
							.create();
		case DIALOG_DOWNLOAD_PROGRESS_BAR:
			Log.w("DOWN", "BAR");
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setTitle(mProgressTitle);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMax(mContentLength);
			mProgressDialog.setProgress(0);
			mProgressDialog.setCancelable(false);
			return mProgressDialog;
		case DIALOG_DOWNLOAD_PROGRESS_CIRCLE:
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setTitle(mProgressTitle);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setCancelable(false);
			return mProgressDialog;    	
		case DIALOG_DATE_DIALOG:
			return new DatePickerDialog(this, mDateSetListener, 
					mCustomisedYear, mCustomisedMonth, mCustomisedDay);
		case DIALOG_TIME_DIALOG:
			return new TimePickerDialog(this, mTimeSetListener, 
					mCustomisedHour, mCustomisedMinute, false);
    	}
    	return null;
    }
    
    private void initCustomisedDateTime(){
    	if(mCustomisedYear == 0){
    		Calendar cal = Calendar.getInstance();
    		mCustomisedYear = cal.get(cal.YEAR);
    		mCustomisedMonth = cal.get(cal.MONTH);
    		mCustomisedDay = cal.get(cal.DAY_OF_MONTH);
    		mCustomisedHour = cal.get(cal.HOUR_OF_DAY);
    		mCustomisedMinute = cal.get(cal.MINUTE);
    	}
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu){
    	super.onCreateOptionsMenu(menu);
    	
    	//Build the menus that are shown when searching.
    	menu.add(0, MENU_DOWNLOAD_NEW_DATABASE, 0, "D/L other database");
    	menu.add(1, MENU_HELP, 0, "Help...");
    	menu.add(2, MENU_ABOUT, 0, "About...");
    	//menu.add(0, MENU_QUIT_ID, 0, "Quit");
    	//menu.add(1, MENU_SHOW_LINE_ID, 0, "Show Line");
    	
    	return true;
    }
    
	private DatePickerDialog.OnDateSetListener mDateSetListener = 
		new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth){
			mCustomisedYear = year;
			mCustomisedMonth = monthOfYear;
			mCustomisedDay = dayOfMonth;
			SydneyTransport.this.showDialog(DIALOG_TIME_DIALOG);
		}
	};
	
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = 
		new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute){
				mCustomisedHour = hourOfDay;
				mCustomisedMinute  = minute;
				Calendar cal = Calendar.getInstance();
				cal.set(mCustomisedYear, 
						mCustomisedMonth, mCustomisedDay, mCustomisedHour, mCustomisedMinute);
				doSearch(cal);
			}
	};
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
    	Intent x = null;
    	super.onOptionsItemSelected(item);
    	switch(item.getItemId()){
    	case MENU_DOWNLOAD_NEW_DATABASE:
    		this.prepareRetrieveDatabase();
    		return true;
    	case MENU_HELP:
    		x = new Intent(this, HelpView.class);
    		this.startActivity(x);
    		return true;
    	case MENU_ABOUT:
    		x = new Intent(this, AboutView.class);
    		this.startActivity(x);
    		return true;
    	}
    	return true;
    }
    
    /**
     * @deprecated
     */
    private void fillData()
    {
    	Cursor c = dbHelper.fetchAllLines();
    	startManagingCursor(c);
    	
    	String[] from = new String[] { "LineName", "DirectFrom", "DirectTo", "Via" };
    	int[] to = new int[] {R.id.line, R.id.from, R.id.to, R.id.via};
    	
    	SimpleCursorAdapter lines = 
    		new SimpleCursorAdapter(this, R.layout.line_row, c, from, to);
    	//setListAdapter(lines);
    }
    
    private void fillStationList()
    {
    	Cursor c = dbHelper.fetchAllStations();
    	stationList.clear();
    	
    	if(c.moveToFirst())
    	{
    		do{
    			stationList.add(c.getString(0));
    		}while(c.moveToNext());
    	}

    	c.close();
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if(newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO){
			mKeyboard.setVisibility(false);
		}else{
			//mKeyboard.setVisibility(true);
			mKeyboard.setVisibility(false);
		}

		super.onConfigurationChanged(newConfig);
	}

	public void retrieveDatabase(){
		mProgressDialog = null;
		mProgressHandler = null;

		mDownloader = new HttpDownloader(
				this.mDbDownUrl,
				"/sdcard/tmp", null, null);

		if (mDownloader.initConnect() == false) {
			this.showDialog(DIALOG_DOWNLOAD_INIT_FAILED);
			return;
		}

		mContentLength = mDownloader.getContentLength();

		
		if (mContentLength > 0) {
			mDialogId = DIALOG_DOWNLOAD_PROGRESS_BAR;
		}else{
			mDialogId = DIALOG_DOWNLOAD_PROGRESS_CIRCLE;
		}
		
		mProgressTitle = "Downloading...";
		this.showDialog(mDialogId);

		// mProgressDialog.setMax(mContentLength);
		mProgressHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					if (mProgressDialog == null)
						return;

					if (msg.what == HttpDownloader.MESSAGE_CURR) {
						if(mContentLength>0){
							Bundle data = msg.getData();
							int curr = data.getInt("CURR");
							mProgressDialog.setProgress(curr);
						}
					} else if (msg.what == HttpDownloader.MESSAGE_FINISHED) {
						SydneyTransport.this.removeDialog(mDialogId);
						//mProgressDialog.dismiss();
						mProgressHandler = null;
						mProgressDialog = null;
						//mDownloader = null;
						mContentLength = -1;
						mDialogId = 0;
						
						mDialogId = DIALOG_DOWNLOAD_PROGRESS_CIRCLE;
						mProgressTitle = "Extracting....";
						showDialog(mDialogId);
						//UnzipHandler
						mUnzipHandler = new Handler(){
							public void handleMessage(Message msg){
								if(msg.what == UnzipJob.MESSAGE_FINISHED){
									SydneyTransport.this.removeDialog(mDialogId);
									new File(mDownloader.getTargetFilePath()).delete();
									mProgressDialog = null;
									mDialogId = 0;
									setupDatabase();
								}
							}
						};
						UnzipJob unzipJob = new UnzipJob(mDownloader.getTargetFilePath(), "/sdcard/tmp", "/sdcard/sydney-trans/", null, false);
						unzipJob.setHandler(mUnzipHandler);
						new Thread(unzipJob).start();
						//mDownloader = null;
					} else if(msg.what == HttpDownloader.MESSAGE_FAILED){
						SydneyTransport.this.removeDialog(mDialogId);
						//mProgressDialog.dismiss();
						mProgressHandler = null;
						mProgressDialog = null;
						//mDownloader = null;
						mContentLength = -1;
						mDialogId = 0;
						
						showDialog(SydneyTransport.DIALOG_DATABASE_DOWNLOAD_FAILED);
						return;
						
					}
			}
		};
		mDownloader.setMessageHandler(mProgressHandler);
		
		new Thread(mDownloader).start();		
	}

	private Handler mXHandler;
	
	protected void setupDatabase(){
		mDialogId = DIALOG_DOWNLOAD_PROGRESS_CIRCLE;
		mProgressTitle = "Initialising Timetable....";
		showDialog(mDialogId);
		
       dbHelper = SydneyTransDbAdapter.getInstance(this);
        if(dbHelper==null){
        	showDialog(DIALOG_DATABASE_NOT_READY);
        	return;
        }
        dbHelper.close();
        mXHandler = new Handler(){
        	public void handleMessage(Message msg){
        		if(msg.what == SydneyTransDbAdapter.MESSAGE_OPEN_FINISHED){
        			realSetupDatabase();
        		}
        	}
        };
        
		new Thread(new Runnable(){
			public void run(){
				Looper.prepare();
		        dbHelper.open();
				//realSetupDatabase();
				SydneyTransport.this.removeDialog(mDialogId);
				mProgressDialog = null;
				mDialogId = 0;		
				Message msg = Message.obtain(mXHandler, SydneyTransDbAdapter.MESSAGE_OPEN_FINISHED);
				mXHandler.sendMessage(msg);

			}
		}).start();//debug
	}

	protected boolean realSetupDatabase(){	        
	        //dbHelper.init();
	        fillStationList();
	        //fillData(); deprecated

	        //AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.current_station);
	        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stationList);

	        AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.current_station);
	        textView.setAdapter(adapter);
	        textView = (AutoCompleteTextView)findViewById(R.id.target_station);
	        textView.setAdapter(adapter);
	        return true;
	}
	
	protected void prepareRetrieveDatabase(){
		Intent x = new Intent(this, DownloadListView.class);
		startActivityForResult(x, ACTIVITY_DOWN_CHOOSE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVITY_HISTORY: {
			if(data==null)
				return;
			
			Bundle extras = data.getExtras();
			String curr = extras.getString(SydneyTransHistoryView.BUNDLE_CURR);
			String targ = extras.getString(SydneyTransHistoryView.BUNDLE_TARG);
			if (curr == null || targ == null)
				break;
			AutoCompleteTextView textView = (AutoCompleteTextView)findViewById(R.id.current_station);
			textView.setText(curr);
			
			textView = (AutoCompleteTextView)findViewById(R.id.target_station);
			textView.setText(targ);

			CheckBox cbOpt = (CheckBox) findViewById(R.id.checkbox_optimised);
			if (cbOpt.isChecked())
				doSearch(Calendar.getInstance());
			else{
				SydneyTransport.this.initCustomisedDateTime();
				showDialog(SydneyTransport.DIALOG_DATE_DIALOG);
			}
			break;
		}
		case ACTIVITY_DOWN_CHOOSE:
			if(data != null){
				Bundle extras = data.getExtras();
				String dbFile = extras.getString(DownloadListView.BUNDLE_DB);
				this.mDbDownUrl = SydneyTransport.DB_DOWNLOAD_BASE + "/" + dbFile;
				this.retrieveDatabase();
				break;
			}
			this.setupDatabase();
			break;
		case ACITIVITY_DATABASE_NOT_READY_HELP:
		case ACITIVITY_DOWNLOAD_FAILED_HELP:
			if(!SydneyTransDbAdapter.isReady())
				System.exit(0);
			break;
		}
	}

}