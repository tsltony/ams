package com.ams.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256 {

	/**
	* ����javaԭ������ʵ��SHA256����
	* @param str ���ܺ�ı���
	* @return
	*/
	public static String getSHA256(String str){
		MessageDigest messageDigest;
		String encodestr = "";
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(str.getBytes("UTF-8"));
			encodestr = byte2Hex(messageDigest.digest());
		 } catch (NoSuchAlgorithmException e) {
			 e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		 return encodestr;
	}
	/**
	* ��byteתΪ16����
	* @param bytes
	* @return
	*/
	private static String byte2Hex(byte[] bytes){
		StringBuffer stringBuffer = new StringBuffer();
		String temp = null;
		for (int i=0;i<bytes.length;i++){
			temp = Integer.toHexString(bytes[i] & 0xFF);
			if (temp.length()==1){
			//1�õ�һλ�Ľ��в�0����
				stringBuffer.append("0");
			}
			stringBuffer.append(temp);
		}
		return stringBuffer.toString();
	}
}