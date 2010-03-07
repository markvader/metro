package com.braveo.sydney.transport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DownloadListView extends ListActivity {
	public static final String BUNDLE_DB = "DB";
	public static final int MESSAGE_DOWN_DB_DESC = 0x34123;
	public static final String DESC_URL = "http://www.markvader.com/files/db.txt";
	
	private HttpDownloader mDownloader;
	
	public class DownDesc {
		public String mFileName;
		public String mDesc;
		
		public DownDesc(String f, String d){
			mFileName = f;
			mDesc = d;
		}
		
		public String toString(){
			//<table border="0"><tr><td width="30%"><font color="#D6DFF7">mFileName</font></td><td width="70%">mDesc</td></tr></table>
			return "<font color=\"#D6DFF7\" size=\"8pt\">" +
					mFileName +
					"</font><br /><font size=\"6pt\">" +
					mDesc + "</font><br /><br />";
		}
	};
	
	private List<DownDesc> mDownList = new ArrayList<DownDesc>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.line_details);
		
		fillData();
	}
	
	private void finishDownload() {
		String fname = mDownloader.getTargetFilePath();
		File file = new File(fname);
		if (!file.exists()) {
			DownloadListView.this.setResult(Activity.RESULT_CANCELED);
			DownloadListView.this.finish();
		} else {

			mDownList.clear();
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					int pos1 = line.indexOf(' ');
					int pos2 = line.indexOf('\t');
					if (pos1 == -1)
						pos1 = pos2;
					if (pos2 != -1 && pos1 > pos2)
						pos1 = pos2;
					if (pos1 == -1)
						continue;
					String dbName = line.substring(0, pos1).trim();
					String desc = line.substring(pos1 + 1).trim();
					mDownList.add(new DownDesc(dbName, desc));
				}
			} catch (IOException e) {
				// do nothing
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						// do nothing
					}
				}
			}
		}
	}
	
	public class MyArrayAdapter extends ArrayAdapter<DownDesc> {
		public MyArrayAdapter(Context context, int textViewResourceId, List<DownDesc> objects){
			super(context, textViewResourceId, objects);
		}
		
		public View	 getView(int position, View convertView, ViewGroup parent){
    		final LayoutInflater inflater = LayoutInflater.from(super.getContext());
    		final TextView view = (TextView)inflater.inflate(R.layout.download_choose_row, parent, false);
    		view.setText(Html.fromHtml(mDownList.get(position).toString()));
    		return view;
		}
	}

	private void fillData() {
		//HttpDownloader(String urlStr, String tmpDir, String targetDir, String targetFileName)
		mDownloader = new HttpDownloader(DESC_URL, "/sdcard/tmp", null, null);
		if (mDownloader.initConnect() != false) {
			mDownloader.run();
			finishDownload();
			mDownloader = null;
		}

		ArrayAdapter<DownDesc> ad = new MyArrayAdapter(this, R.layout.download_choose_row, mDownList);
		setListAdapter(ad);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		Intent i = new Intent();
		i.putExtra(BUNDLE_DB, mDownList.get(position).mFileName);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
}
