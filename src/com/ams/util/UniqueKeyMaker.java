/**
 * 
 */
package com.ams.util;


/**
 * @author Harold.HH
 * @date 
 * @param
 * @return
 * @Exception
 */
public class UniqueKeyMaker {
	
	static int count=0;
	

	private synchronized static void changeCount(int increase){
		count+=increase;
	}
	//不可出现20位的bigint mysql 不承认
	public  static String getPk(){
		if(count>100000) count=0;
		String value=String.valueOf(100000+count);
		changeCount(1);
		return value;
	}

	public  static String getPk(int num){
		if(count>100000) count=0;
		String value=String.valueOf(100000+count);
		changeCount(num);
		return System.currentTimeMillis()+value;
	}
	
	public static void main(String [] arg){
		
		System.out.println( UniqueKeyMaker.getPk(3));
		System.out.println( UniqueKeyMaker.getPk(1));
		System.out.println( UniqueKeyMaker.getPk(2));

	}
}
