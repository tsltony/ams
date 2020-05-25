package com.ams.widget;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import com.ams.util.BaseDataUtil;
import com.ams.util.ZipFileUtil;



public class FtpEfileService implements EFileTransferServie {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private FTPClient ftpClient = new FTPClient(); 
	private String ip,port,user,pwd,charset;
	
	public FtpEfileService(String ip,String port,String user,String pwd){
		this.ip=ip;
		this.port=port;
		this.user=user;
		this.pwd=pwd;
		this.charset = "GBK";
	}
	public FtpEfileService(String ip,String port,String user,String pwd,String charset){
		this.ip=ip;
		this.port=port;
		this.user=user;
		this.pwd=pwd;
		this.charset = charset;
	}

	@Override
	public boolean testOpen() {
		ftpClient = new FTPClient();
		int time = 0;
    	try {time =ftpClient.getDefaultTimeout();
    	} catch (Exception e1) {}
    	try{
    		boolean sucess = false;
    		ftpClient.setDataTimeout(1500);
    		ftpClient.connect(ip, BaseDataUtil.parseInt(port));
    		if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){  
    			sucess = ftpClient.login(user, pwd);
    		}   
    		ftpClient.disconnect();
    		return sucess;
    	}catch(Exception e){
    		return false;
    	}finally{    		
    		try {if(time>0)ftpClient.setDataTimeout(time);
    		} catch (Exception e) {}
    	}
	}
	
	@Override
	public boolean upload(String remotepath, String localpath) {
		try{
			boolean bn = connect();
			if(!bn)throw new Exception("无法登录FTP系统");
			uploadFile(remotepath,localpath);
			return true;
		}catch(Exception e){
			showError(e.getMessage(),e);
			return false;
		}finally{
			disconnect();
		}
	}
	

	@Override
	public boolean download(String remotepath, String downldpath) {
		try{
			boolean bn = connect();
			if(!bn)throw new Exception("无法登录FTP系统");
			downloadFile(remotepath,downldpath);
			return true;
		}catch(Exception e){
			showError(e.getMessage(),e);
			return false;
		}finally{
			disconnect();
		}
	}
	
	@Override
	public String getLocalPath(String remptePath) {
		 // 此方法只对 本地服务器有效, 其他实现类返回null即可
		return null;           
	}
	
	
	@Override
	public boolean testExist(String remptePath) {
		try{
			boolean bn = connect();
			if(!bn)throw new Exception("无法登录FTP系统");
			return checkFileExit(remptePath);
		}catch(Exception e){
			showError(e.getMessage(),e);
			return false;
		}finally{
			disconnect();
		}
	}
	
	//连接ftp
	private boolean connect() throws IOException{   
		ftpClient = new FTPClient();
		ftpClient.connect(ip, BaseDataUtil.parseInt(port));
		ftpClient.setControlEncoding(charset);   
		boolean sucess = false;
		if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){  
			sucess = ftpClient.login(user, pwd);
		}   
		if(!sucess)disconnect();   
		return sucess;   
    }   
	
	//关闭ftp
	private void disconnect(){   
		try {
	    	if(ftpClient.isConnected()){   
	    		ftpClient.disconnect();
	    	}   
		} catch (IOException e) {}
    }
	
	private boolean  checkFileExit(String remotePath) throws Exception {  
		remotePath = remotePath.replaceAll("\\\\", "/");
		remotePath = new String(remotePath.getBytes(charset), "iso-8859-1");
		if(remotePath.startsWith("/"))remotePath =remotePath.substring(1);
		if(remotePath.endsWith("/"))remotePath =remotePath.substring(0,remotePath.length()-1);
		String filename = remotePath;
		if(remotePath.indexOf("/")>=0){
			int index = remotePath.lastIndexOf("/");
			filename = remotePath.substring(index+1);
			remotePath = remotePath.substring(0,index);
		}else{
			remotePath="";
		}
		boolean result = false;
		if(filename.indexOf(".")>=0){			
			filename = new String(filename.getBytes("iso-8859-1"),charset);
			FTPFile[] files = ftpClient.listFiles(remotePath);
			for (FTPFile f : files) {
				if(f.isFile()&&filename.equals(f.getName())){
					result = true;
					break;
				}
			}
		}else{
			result = ftpClient.changeWorkingDirectory(remotePath+"/"+filename);
		}
		return result;
	}
	
	private void downloadFile(String remotePath,String localPath) throws Exception {  
		localPath = localPath.replaceAll("\\\\", "/");
		ZipFileUtil.makeNewFile(localPath);

		remotePath = new String(remotePath.getBytes(charset), "iso-8859-1");
		remotePath = remotePath.replaceAll("\\\\", "/");
		if(remotePath.startsWith("/"))remotePath =remotePath.substring(1);
		if(remotePath.endsWith("/"))remotePath =remotePath.substring(0,remotePath.length()-1);
		
        BufferedOutputStream output = null;  
        try {  
            output = new BufferedOutputStream(new FileOutputStream(localPath));
            boolean bn = ftpClient.retrieveFile(remotePath, output);  
            if(!bn)throw new Exception("下载文件失败");
        } catch (Exception e) {  
            throw e;  
        } finally {  
            if (output != null){
            	try{output.close();}catch(Exception e){}
            }
        }  
    }  
	
	private void uploadFile(String remotePath,String localPath) throws Exception {  
		localPath = localPath.replaceAll("\\\\", "/");
		remotePath = remotePath.replaceAll("\\\\", "/");
		remotePath = new String(remotePath.getBytes(charset), "iso-8859-1");
		if(remotePath.startsWith("/"))remotePath =remotePath.substring(1);
		if(remotePath.endsWith("/"))remotePath =remotePath.substring(0,remotePath.length()-1);

		String filename = remotePath;
		if(remotePath.indexOf("/")>=0){
			int index = remotePath.lastIndexOf("/");
			String remotedir = remotePath.substring(0,index);
			filename = remotePath.substring(index+1);
			autoCreateFolder(remotedir);
		}
		File file = new File(localPath);
		if(!file.exists()){
			throw new Exception("原文件不存在");
		}
        InputStream is = null;  
        try {  
        	ftpClient.enterLocalPassiveMode();
        	ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);  
        	ftpClient.setBufferSize(1024); 
        	is = new FileInputStream(file);  
        	boolean bn = ftpClient.storeFile(filename, is);  
        	if(!bn)throw new Exception("上传文件失败");
        } catch (Exception e) {  
            throw e;  
        } finally {  
        	if (is != null){
            	try{is.close();}catch(Exception e){}
            }
        }  
    }  
	
	private boolean autoCreateFolder(String remotedir){
		try{
			int start = 0;
			int end = remotedir.indexOf("/", start);
			while (true) {
				String subDirectory = remotedir.substring(start, end);
				if (!ftpClient.changeWorkingDirectory(subDirectory)) {
					if (ftpClient.makeDirectory(subDirectory)) {
						ftpClient.changeWorkingDirectory(subDirectory);
					} else {
						return false;
					}
				}
				start = end + 1;
				end = remotedir.indexOf("/", start);
				if(start >= remotedir.length()){
					break;
				}
				if(end==-1){
					end = remotedir.length();
				}
			}
			return true;
		}catch(Exception e){
			showError(e.getMessage(),e);
			return false;
		}
	}

	private void showError(String message, Exception e) {
		logger.error(message, e);
	}
	
}
