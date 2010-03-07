package com.braveo.sydney.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.os.Handler;

public class UnzipJob implements Runnable {
	private String srcPath;
	public String getSrcPath() {
		return srcPath;
	}
	private String tmpDir;
	private String targetDir;
	private String[] targetFiles;
	private boolean withPath;
	private Handler handler;
	
	public static final int MESSAGE_FINISHED = 0x001221;
	public static final int BUFFER_SIZE = 2048;
	
	public UnzipJob(String srcPath, String tmpDir, String targetDir, String [] targetFiles, boolean withPath){
		this.srcPath = srcPath;
		this.tmpDir = tmpDir;
		if(tmpDir==null)
			throw new IllegalArgumentException("Missing temp dir for Unzip");
		
		this.targetDir = targetDir;
		if(this.targetDir==null)
			this.targetDir = tmpDir;
		
		this.targetFiles = targetFiles;
		this.withPath = withPath;
	}
	
	public void setHandler(Handler handler){
		this.handler = handler;
	}
	

	public void unzip() throws IOException {
		File tmp = new File(tmpDir);
		Set<String> unzipSet = new TreeSet<String>();
		
		if(!tmp.exists())
			tmp.mkdirs();
								
		ZipFile zipFile = new ZipFile(srcPath);
		Enumeration entries = zipFile.entries();
		while(entries.hasMoreElements()){
			ZipEntry entry = (ZipEntry)entries.nextElement();
			if(entry.isDirectory()){
				//do not create empty directory
				continue;
			}
			if(targetFiles!=null && targetFiles.length>0){
				int k = 0;
				for(k=0; k<targetFiles.length; k++){
					if(entry.getName().equals(targetFiles[k])){
						break;
					}
				}
				if(k == targetFiles.length){
					continue;
				}
			}
				
			String fname = entry.getName();
			if (!withPath) {
				int pos = fname.lastIndexOf("/");
				if (pos != -1)
					fname = fname.substring(pos + 1);
			}

			if (unzipSet.contains(fname))
				continue; // avoid conflict
			unzipSet.add(fname);

			File file = new File(tmpDir + "/" + fname);
			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			BufferedInputStream bis = new BufferedInputStream(zipFile
					.getInputStream(entry));
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos,
					BUFFER_SIZE);
			int count;
			byte data[] = new byte[BUFFER_SIZE];
			while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
				bos.write(data, 0, count);
			}
			bos.flush();
			bos.close();
			bis.close();
			
		}
		
		if(tmpDir.equals(targetDir))
			return;
		
		Iterator<String> iter = unzipSet.iterator();
		while(iter.hasNext()){
			String n = iter.next();
			if(!withPath){
				int pos = n.lastIndexOf("/");
				if(pos!=-1)
					n = n.substring(pos+1);
			}
			File tn = new File(tmpDir + "/" + n);
			File target = new File(targetDir + "/" + n);
			tn.renameTo(target);
		}
		
		if(!withPath)
			return;
		
		iter = unzipSet.iterator();
		while(iter.hasNext()){
			String n = iter.next();
			File tn = new File(tmpDir + "/" + n);
			tn = tn.getParentFile();
			if(tn.exists()){
				try{
					tn.delete(); //Just a try. If the directory is not empty, it will not be deleted
				}catch(Exception e){
					//do nothing
				}
			}
		}
		
	}
	public void run(){
		try{
			unzip();
		}catch(IOException ioe){
			throw new RuntimeException(ioe);
		}
		if(handler!=null){
			handler.sendEmptyMessage(MESSAGE_FINISHED);
		}
	}
	
}
