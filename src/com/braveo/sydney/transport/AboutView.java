package com.braveo.sydney.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about);
		
		fillData();
	}
	
	protected void fillData(){
		BufferedReader br = null;
		try{
			InputStream is = getResources().openRawResource(R.raw.about);
			br = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer sb = new StringBuffer();
			
			while((line = br.readLine())!=null){
				sb.append(line);
			}
			
			br.close();
			
			WebView wv = (WebView)this.findViewById(R.id.about_text);
			wv.loadData(sb.toString(), "text/html", "utf8");
		}catch(IOException ioe){
			
		}finally{
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
	}
	

}
