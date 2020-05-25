package com.ams.util;


import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.rpc.client.RPCServiceClient;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.ams.util.XmlFileUtil;

public class SyncImpTest {
	private static Logger logger = Logger.getLogger(SyncImpTest.class.getName());
	public static void main(String [] a){
		String xml="";
		try {
			Document xmldoc=XmlFileUtil.LocalFiletoXml(new File("C:/temp/user.xml"));//12345…Ûº∆£ª1∫œÕ¨£ª20ÀﬂÀœ
			xml = XmlFileUtil.XMLtoString(xmldoc);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String groupUserXml="";
		if(!"".equals(xml)){
			RPCServiceClient client;
//			String address = "http://192.168.3.23:7001/ams/services/SyncData";
			String address = "http://127.0.0.1:8080/ams/services/SyncData";
//			String address = "http://172.11.10.18:7001/ams/services/SyncData";
			try {
			    client = new RPCServiceClient();
			    Options options = client.getOptions();
			    String defualNameSpace = "http://service.platform.com";
			    EndpointReference epr = new EndpointReference(address);
			    options.setTo(epr);
			    QName qname = new QName(defualNameSpace,"syncData");
			    Object[] result = client.invokeBlocking(qname, new Object[] {xml,"1","0001"}, new Class[] {String.class,String.class,String.class });
			    groupUserXml = (String) result[0];
			    logger.info(groupUserXml);
			  
			} catch (AxisFault e) { 
			    e.printStackTrace();
			    groupUserXml="";
			}
		}
		  System.out.println(groupUserXml);
	}
	

}