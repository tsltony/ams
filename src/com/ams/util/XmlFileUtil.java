package com.ams.util;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
public class XmlFileUtil {
	private static Transformer transformer=null;
	private static DocumentBuilder builder=null;
	private static Logger logger = Logger.getLogger(XmlFileUtil.class.getName());
	static{
		try {
			logger.info("xmlfileutil.class start");
			TransformerFactory tf = TransformerFactory.newInstance();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			transformer = tf.newTransformer();
			transformer.setOutputProperty("encoding", "GBK");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	//展示XML内容
	public static String XMLtoString(Document doc){
		try{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(doc), new StreamResult(bos));
		return bos.toString();
		}catch(Exception e){return "";}
	}
	//XML本地化
	public static void XMLtoLocalFile(Document doc,String targetpath) throws TransformerFactoryConfigurationError, Exception{
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		DOMSource source = new DOMSource(doc);
		try {
			StreamResult result;
			result = new StreamResult(new FileOutputStream(targetpath));
			transformer.transform(source,result);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		}
	}
	//读取本地XML
	public static Document LocalFiletoXml(File file) throws IllegalArgumentException, SAXException, IOException{		
		Document doc = builder.parse (file);
		doc.getDocumentElement().normalize ();
		return doc;
	}
	
	public static Document StringToXML(String word) throws Exception{
		try {
			InputSource is=new InputSource(new StringReader(word));
			return builder.parse(is);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e.getMessage());
		} 
	}
	public synchronized static Document createNewDocumen(){
		
		DocumentBuilderFactory factory = DocumentBuilderFactory
		.newInstance();
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = builder.newDocument();
		return doc;
	}
	public static String XMLtoString(Document xmldoc, String charset) {
		try{
			//ByteArrayOutputStream bos = new ByteArrayOutputStream();
			//bos UTF-8格式，中文乱码，改使用writer
			StringWriter writer = new StringWriter();
			transformer.setOutputProperty("encoding", charset);
			transformer.transform(new DOMSource(xmldoc), new StreamResult(writer));
		//return bos.toString();
		return writer.toString();
		}catch(Exception e){return "";}
	}
}
