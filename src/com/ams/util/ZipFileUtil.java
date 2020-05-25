package com.ams.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipFileUtil {
	//ɾ���ļ���
	public static void deleteDirectory(String dirpath){
		File file=new File(dirpath);
		deleteFile(file);
	} 
	public static void deleteFile(File file){
		if(file.exists()){                    //�ж��ļ��Ƿ����
			if(file.isFile()){                    //�ж��Ƿ����ļ�
				file.delete();                       //delete()���� ��Ӧ��֪�� ��ɾ������˼;
			}else if(file.isDirectory()){              //�����������һ��Ŀ¼
				File files[] = file.listFiles();               //����Ŀ¼�����е��ļ� files[];
				for(int i=0;i<files.length;i++){            //����Ŀ¼�����е��ļ�
					deleteFile(files[i]);             //��ÿ���ļ� ������������е���
				}
				file.delete(); 
			} 
		}
	}
	
	
	//�����ļ�
	public static void copyfile(String sourcepath,String tagartpath) throws Exception{
		FileInputStream fileInputStream=new FileInputStream(sourcepath);
		BufferedInputStream bis=new BufferedInputStream(fileInputStream);
		FileOutputStream bos=null;
		try{
			String filePath = tagartpath.substring(0, tagartpath.lastIndexOf("/"));
			makeDirectory(filePath);
			File file=new File(tagartpath);
			file.createNewFile();
			bos=new FileOutputStream(file);
			bis = new BufferedInputStream(fileInputStream);
			byte[] buff = new byte[2048];
			int bytesRead;
			while(-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
				bos.write(buff, 0, bytesRead);
			}
		}catch(Exception e){
			throw e;
		}finally{
			try{bis.close();
			}catch(Exception e){}
			try{bos.close();
			}catch(Exception e){}
		}
	}
	
	//copy��ĳһ�ļ���
	public static void copyfileordir(String sourcepath,String tagartpath) throws Exception{
		File file=new File(sourcepath);
		if(!file.exists())return;
		if(file.isDirectory()){
			tagartpath=tagartpath+"/"+file.getName();
			makeDirectory(tagartpath);
			File[] files=file.listFiles();
			if(files!=null){
				for(File file2:files){
					copyfileordir(file2.getAbsolutePath(),tagartpath);
				}
			}
		}else{
			tagartpath=tagartpath+"/"+file.getName();
			copyfile(file.getAbsolutePath(),tagartpath);
		}
	}
	
	//ѹ���ļ���
	public static void zipdata(String directory,String targetpath) throws Exception{
		ZipOutputStream out=null;
		try{
			makeNewFile(targetpath);
			out=new ZipOutputStream(new FileOutputStream(targetpath));
			zip(out,new File(directory),"");
		}catch(Exception e){
			throw e;
		}finally{
			try{out.close();}catch(Exception e){}
		}
		
	}
	
	public static void zipdata(String directory,String targetpath,int level) throws Exception{
		ZipOutputStream out=null;
		try{
			makeNewFile(targetpath);
			out=new ZipOutputStream(new FileOutputStream(targetpath));
			out.setLevel(level);
			zip(out,new File(directory),"");
		}catch(Exception e){
			throw e;
		}finally{
			try{out.close();}catch(Exception e){}
		}
		
	}
	
	public static void zipdata(File[] files,String targetpath) throws Exception{
		ZipOutputStream out=null;
		try{
			makeNewFile(targetpath);
			out=new ZipOutputStream(new FileOutputStream(targetpath));
			for(File file:files){
				zip(out,file,file.getName());
			}
		}catch(Exception e){
			throw e;
		}finally{
			try{out.close();}catch(Exception e){}
		}
		
	}
	
	public static void zipdata(File[] files,String targetpath,int level) throws Exception{
		ZipOutputStream out=null;
		try{
			makeNewFile(targetpath);
			out=new ZipOutputStream(new FileOutputStream(targetpath));
			out.setLevel(level);
			for(File file:files){
				zip(out,file,file.getName());
			}
		}catch(Exception e){
			throw e;
		}finally{
			try{out.close();}catch(Exception e){}
		}
		
	}
	
	private  static void zip(ZipOutputStream out, File f, String base) throws Exception {   
        if (f.isDirectory()) {  //�ж��Ƿ�ΪĿ¼   
            File[] fl = f.listFiles();   
            out.putNextEntry(new ZipEntry(base + "/"));   
            base = base.length() == 0 ? "" : base + "/";   
            for (int i = 0; i < fl.length; i++) {   
                zip(out, fl[i], base + fl[i].getName());   
            }   
        } else {                //ѹ��Ŀ¼�е������ļ�   
            out.putNextEntry(new ZipEntry(base));   
            FileInputStream in = new FileInputStream(f);   
            int b;   
//            System.out.println(base);   
            while ((b = in.read()) != -1) {   
                out.write(b);   
            }   
            in.close();   
        }   
    }
	
	//��ѹ���ļ���
	public static void extZip(String zipfile, String destDir) throws Exception{
		destDir = destDir.endsWith("\\") ? destDir : destDir + "\\";
		byte b[] = new byte[512];
		int length;     
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(new File(zipfile));
			Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
			ZipEntry zipEntry = null;
			while (enumeration.hasMoreElements()) {
				zipEntry = (ZipEntry) enumeration.nextElement();
				File loadFile = new File(destDir + zipEntry.getName());
				if (zipEntry.isDirectory()) {
					loadFile.mkdirs();
				} else {
					if (!loadFile.getParentFile().exists())loadFile.getParentFile().mkdirs();
					if(loadFile.exists())loadFile.delete();
					loadFile.createNewFile();
					OutputStream outputStream = new FileOutputStream(loadFile);
					InputStream inputStream = zipFile.getInputStream(zipEntry);
					while ((length = inputStream.read(b)) > 0){
						outputStream.write(b,0,length);
					}
					inputStream.close();
					outputStream.close();
				}
			}
			zipFile.close();
		} catch (IOException e) {
			throw new Exception("��ѹ�ļ�ʧ�ܣ�",e);
		}
	}
	
	//�����ļ���
	public static void  makeDirectory(String dirpath) throws Exception{
		File directorypath=new File(dirpath);
		if(!directorypath.exists()){
			if(!directorypath.mkdirs()){
				throw new Exception("�޷������ļ���");
			}
		}
		else if(!directorypath.isDirectory()){
			directorypath.delete();
			if(!directorypath.mkdirs()){
				throw new Exception("�޷������ļ���");
			}
		}
	}
	//�������ļ�
	public static void makeNewFile(String filepath) throws Exception{
		File file=new File(filepath);
		if(!file.exists()){
			int index=filepath.lastIndexOf("/");
			if(index!=-1){
				String dir=filepath.substring(0,index);
				makeDirectory(dir);
			}
			file.createNewFile();
		}else{
			file.delete();
			file.createNewFile();
		}
	}
}
