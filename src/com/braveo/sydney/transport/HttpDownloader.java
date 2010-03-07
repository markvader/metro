package com.braveo.sydney.transport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class HttpDownloader implements Runnable{
	private String urlStr;
	private String tmpDir;
	private String targetDir;
	private String targetFileName;
	
	public String getTargetFileName() {
		return targetFileName;
	}
	
	public String getTargetFilePath() {
		return targetDir + "/" + targetFileName;
	}

	private Handler handler = null;
	private URLConnection conn = null;
	private int downloaded = 0;
	private boolean finished = false;
	
	private boolean needBackup = false;
	
	
	public HttpDownloader(String urlStr, String tmpDir, String targetDir, String targetFileName){
		this.urlStr = urlStr;
		this.tmpDir = tmpDir;
		if(this.tmpDir==null)
			throw new IllegalArgumentException("Missing tmpDir");
		
		this.targetDir = targetDir;
		if(targetDir==null)
			this.targetDir = tmpDir;
		if(targetFileName != null)
			this.targetFileName = targetFileName;
		else {
			if(urlStr.endsWith("/")){
				this.targetFileName = "index.html";
			}else{
				int pos = urlStr.lastIndexOf('/');
				this.targetFileName = urlStr.substring(pos+1);
			}
		}
		
	}
	
	public boolean isFinished() {
		return finished;
	}

	public void setMessageHandler(Handler handler){
		this.handler = handler;
	}
	
	public boolean initConnect(){
		File dir = new File(tmpDir);
		if(!dir.exists())
			dir.mkdirs();
		dir = new File(targetDir);
		if(!dir.exists())
			dir.mkdirs();
		
		String tmpPath = tmpDir + "/" + targetFileName;
		
		File tmpFile = new File(tmpPath);
		if(tmpFile.exists()){
			tmpFile.delete();
		}
		
		try{
			URL url = new URL(urlStr);
			conn = url.openConnection();
		}catch(IOException ioe){
			return false;
		}
		
		return true;
	}
	
	public int getContentLength() {
		return conn.getContentLength();
	}
	
	public void setNeedBackup(boolean b){
		this.needBackup = b;
	}
	
 	/**
	 * Look like Java does not support continual downloading
	 * @throws IOException
	 */
	public void download() throws IOException{
		String tmpPath = tmpDir + "/" + targetFileName;
		String targetPath = targetDir + "/" + targetFileName;
		
		File tmpFile = new File(tmpPath);
		
		conn.connect();
		InputStream is = conn.getInputStream();
		FileOutputStream fos = new FileOutputStream(tmpPath);
		byte [] b = new byte[1024];
		int read;
		while((read = is.read(b))>0){
			fos.write(b,0, read);
			downloaded += read;
			if(handler!=null){
				Bundle data = new Bundle();
				data.putInt("CURR", downloaded);
				Message msg = Message.obtain(handler, MESSAGE_CURR);
				msg.setData(data);
				handler.sendMessage(msg);
			}
		}
		is.close();
		fos.close();
		
		if(targetPath.equals(tmpPath))
			return;
		
		if(needBackup){
			backup();
		}
		
		File targetFile = new File(targetPath);
		if(targetFile.exists()){
			targetFile.delete();
		}
		
		tmpFile.renameTo(targetFile);
	}
	
	public static final int MESSAGE_FINISHED = 0x1001;
	public static final int MESSAGE_CURR	 = 0x1002;
	public static final int MESSAGE_FAILED = 0x1003;
	
	public void backup(){
		String targetPath = targetDir + "/" + targetFileName;
		
		File file = new File(targetPath);
		File backFile = new File(targetPath + ".bak");
		if(file.exists()){
			file.renameTo(backFile);
		}
			
	}

	public void run() {
		try{
			download();
			finished = true;
			if(handler!=null)
			{
				Message msg = Message.obtain(handler, MESSAGE_FINISHED);
				handler.sendMessage(msg);
			}

		}catch(IOException e){
			if(handler!=null)
			{
				Message msg = Message.obtain(handler, MESSAGE_FAILED);
				Bundle b = new Bundle();
				b.putString("ERR", "Download Failed");
				handler.sendMessage(msg);
			}
		}
		
	}
	
}