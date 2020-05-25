package com.ams.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BaseDataUtil {

	public static int parseInt(Object o){
		int rtnvalue = 0;
		if(o!=null){
			try{
				rtnvalue = Integer.parseInt(o.toString());
			}catch(Exception e){
			}
		}
		return rtnvalue;
	}
	
	public static String trimString(Object o){
		if(o==null)return "";
		else return o.toString().trim();
	}
	
	public static boolean isEmpty(Object o){
		if(o==null)return true;
		else return o.toString().trim().equals("");
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map buildMap(Object... args){
		Map map = new LinkedHashMap();
		int count = args.length/2;
		for(int i=0;i<count;i++){
			map.put(args[i*2], args[i*2+1]);
		}
		return map;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List buildList(Object... args){
		return new ArrayList(Arrays.asList(args));
	}
}
