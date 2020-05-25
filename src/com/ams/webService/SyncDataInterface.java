package com.ams.webService;



public interface SyncDataInterface {
	String syncData ( String dataXml , String libcode ,String unitsys,String appid);
	String syncDataFile ( String dataXml , String libcode ,String unitsys,String appid);
	//String businessArchiveService (String data);
}
