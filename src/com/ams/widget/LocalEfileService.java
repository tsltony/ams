package com.ams.widget;

import java.io.File;

import org.apache.log4j.Logger;

import com.ams.util.ZipFileUtil;

public class LocalEfileService implements EFileTransferServie {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private String baseDir;
	private boolean open;


	public  LocalEfileService(String baseDir){
		if(baseDir==null)baseDir="";
		baseDir = baseDir.replaceAll("\\\\", "/");
		if(!baseDir.equals("")&&!baseDir.endsWith("/")){
			baseDir = baseDir+"/";
		}
		this.baseDir = baseDir;
		try{
			ZipFileUtil.makeDirectory(baseDir);
			open = true;
		}catch(Exception e){
			showError(e.getMessage(),e);
			open = false;
		}
	}
	
	@Override
	public boolean testOpen() {
		return open;
	}

	@Override
	public boolean testExist(String remotePath) {
		remotePath = remotePath.replaceAll("\\\\", "/");
		String realpath = baseDir+remotePath;
		return new File(realpath).exists();
	}

	@Override
	public boolean download(String remotePath, String downldpath) {
		remotePath = remotePath.replaceAll("\\\\", "/");
		String realpath = baseDir+remotePath;
		try {
			ZipFileUtil.copyfile(realpath, downldpath);
			return true;
		} catch (Exception e) {
			showError(e.getMessage(),e);
			return false;
		}
	}

	@Override
	public boolean upload(String remotePath, String localpath) {
		remotePath = remotePath.replaceAll("\\\\", "/");
		String realpath = baseDir+remotePath;
		try {
			ZipFileUtil.copyfile(localpath, realpath);
			return true;
		} catch (Exception e) {
			showError(e.getMessage(),e);
			return false;
		}
	}

	@Override
	public String getLocalPath(String remptePath) {
		remptePath = remptePath.replaceAll("\\\\", "/");
		String realpath = baseDir+remptePath;
		return realpath;
	}

	private void showError(String message, Exception e) {
		logger.error( message,e);
	}
}
