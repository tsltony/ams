package com.ams.webService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ams.dao.syncDataDao;
import com.ams.util.BaseDataUtil;
import com.ams.util.UniqueKeyMaker;
import com.ams.util.XmlFileUtil;

public class ArchiveCheckServiceImp {
	private static Logger logger = Logger.getLogger(ArchiveCheckServiceImp.class.getName());
	private static Map<String,Map<String, Object>>checkRowMap = new HashMap<String,Map<String, Object>>();
	protected syncDataDao syncDao;
	public String archiveCheckService (String data, String sysID, String serviceID){
		logger.info("data："+data);
		logger.info("sysID："+sysID);
		logger.info("serviceID："+serviceID);
		String type = "1";
		String typeWithIn = "";
		//校验非空参数
		if( data ==null || "".equals(data)){
			type = "0";
			typeWithIn = "传入的数据data不能为空";
			return xmlReturn(type,typeWithIn);
		}
		if( sysID ==null || "".equals(sysID)){
			type = "0";
			typeWithIn = "传入的数据sysID不能为空";
			return xmlReturn(type,typeWithIn);
		}
		if( serviceID ==null || "".equals(serviceID)){
			type = "0";
			typeWithIn = "传入的数据serviceID不能为空";
			return xmlReturn(type,typeWithIn);
		}
		
		//校验获取到的xml格式的字符串
		logger.info( "校验数据xml...");
		Document document = null;
		try {
			document = XmlFileUtil.StringToXML(data);
		} catch (Exception e1) {
			logger.info( "解析数据xml出错："+e1.getMessage());
			type = "0";
			typeWithIn = "传入的xml字符有误,解析失败。";
			return xmlReturn(type,typeWithIn);
		}	
		logger.info( "...校验数据xml");
		
		logger.info( "解析数据xml...");
		Element root=document.getDocumentElement();
		NodeList nodeList = root.getChildNodes();
		Element archive = null;
		for(int j=0;j<nodeList.getLength();j++){
			if(nodeList.item(j) instanceof Element && "SYSTEM".equals(nodeList.item(j).getNodeName())){
				archive = (Element) nodeList.item(j);
			}else{
				continue;
			}
		}
		
		if(archive==null){
			return xmlReturn("0","解析数据xml中无SYSTEM节点");
		}
		
		try{
			//获取xml档案数据
			Map<String, Object> xmlFileRow =namedNodeToMap(archive);
			String archid=BaseDataUtil.trimString(xmlFileRow.get("CHECKID"));
			if("".equals(archid)){
				return xmlReturn("0","解析数据xml中无SYSTEM节点CHECKID属性有误");
			}
			String unitsys=BaseDataUtil.trimString(xmlFileRow.get("UNITSYS"));
			if("".equals(unitsys)){
				return xmlReturn("0","解析数据xml中无SYSTEM节点UNITSYS属性有误");
			}
			Map<String, Object> checkRowMap=getCheckRowMap(sysID,serviceID,unitsys);
			if(checkRowMap==null){
				logger.error("查重表d_check未找到业务系统的相关配置：SYSID="+sysID+",SERVICEID="+serviceID+",UNITSYS="+unitsys);
				return xmlReturn("0","查重表d_check未找到业务系统的相关配置：SYSID="+sysID+",SERVICEID="+serviceID+",UNITSYS="+unitsys);
			}
			logger.info("查重表获取到业务系统的相关配置："+checkRowMap);
			
			String srctable=BaseDataUtil.trimString(checkRowMap.get("SRCTABLE"));
			if(srctable==null || "".equals(srctable)){
				logger.error("srctable未获取到");
				return xmlReturn("0","srctable未获取到");
			}
			
			String srcfield=BaseDataUtil.trimString(checkRowMap.get("SRCFIELD"));
			if(srcfield==null || "".equals(srcfield)){
				logger.error("srcfield未获取到");
				return xmlReturn("0","srcfield未获取到");
			}
			
			String memo=getCheckdata(srctable,srcfield,archid);
			if(memo==null || "".equals(memo)){
				return xmlReturn("1","不重复");
			}else{
				return xmlReturn("0",memo);
			}
			
		}catch(Exception e){
			e.printStackTrace();
			logger.info("查重出错..."+e.getMessage());
			return xmlReturn("0","查重出错..."+e.getMessage());
		}
		
	}
	
	private String getCheckdata(String srctable, String srcfield, String archid) {
		String memo="";
		try{
			String sql="SELECT TITLE,CREATETIME,ID,DID FROM "+srctable+" WHERE "+srcfield+"=?";
			Object [] args={archid};
			List<Map<String,Object>> list=syncDao.queryAmsData(sql, args);
			if(list!=null&&list.size()>0){
				Map<String,Object> map=list.get(0);
				memo="重复："+archid+"、"+BaseDataUtil.trimString(map.get("TITLE")+"、"+BaseDataUtil.trimString(map.get("CREATETIME")));
			}
			
		}catch(Exception e){
			e.printStackTrace();
			logger.info("getCheckdata 出错："+e.getMessage());
			memo=e.getMessage();
		}
		return memo;
	}
	private Map<String, Object> getCheckRowMap(String sysID, String serviceID,String unitsys) {
		Map<String, Object>cmap=null;
		try{
			if(checkRowMap.get(sysID+serviceID+unitsys)==null){
				String sql="SELECT SRCTABLE,SRCFIELD,SYSID,SERVICEID FROM D_CHECK WHERE SYSID=? and SERVICEID=? and UNITSYS=?";
				Object [] args={sysID,serviceID,unitsys};
				List<Map<String,Object>> list=syncDao.queryAmsData(sql, args);
				if(list!=null&&list.size()>0){
					cmap=list.get(0);
					checkRowMap.put(sysID+serviceID, cmap);
				}
			}else{
				cmap=checkRowMap.get(sysID+serviceID+unitsys);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return cmap;
	}
	

	private Map<String, Object> namedNodeToMap(Element Node) {
		Map<String, Object> Row =new HashMap<String, Object>();
		NamedNodeMap Map = Node.getAttributes();
		for (int j = 0; j < Map.getLength(); j++) {
			//通过item(index)方法获取book节点的某一个属性
			Node attr = Map.item(j);
			Row.put(attr.getNodeName().toUpperCase(),attr.getNodeValue());
		}
		Element metadatas = null;
		NodeList list = Node.getChildNodes();
		for(int j=0;j<list.getLength();j++){
			
			if(list.item(j) instanceof Element && "METADATAS".equals(list.item(j).getNodeName())){
				metadatas = (Element) list.item(j);
				break;
			}else{
				continue;
			}
		}
		if(metadatas!=null){
			NodeList metadataList = metadatas.getChildNodes();
			for(int j=0;j<metadataList.getLength();j++){
				
				if(metadataList.item(j) instanceof Element){
					Element metadata = (Element) metadataList.item(j);
					Row.put(metadata.getNodeName(),metadata.getTextContent());
				}else{
					continue;
				}
			}
		}
		
		return Row;
	}
	
	

	private String xmlReturn(String type, String typeWithIn){
		Document xmldoc = XmlFileUtil.createNewDocumen();
		Element xmlRoot = xmldoc.createElement("ROOT");
		Element result = xmldoc.createElement("RESULT");
		Element memo = xmldoc.createElement("MEMO");
		result.setTextContent(type);
		memo.setTextContent(typeWithIn);
		xmlRoot.appendChild(result);
		xmlRoot.appendChild(memo);
		xmldoc.appendChild(xmlRoot);
		String xmlReturn = "";
		xmldoc.setXmlStandalone(true);
		xmlReturn = XmlFileUtil.XMLtoString(xmldoc,"UTF-8");
		logger.info("接口返回值："+ xmlReturn);
		return xmlReturn;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sys = UniqueKeyMaker.getPk(1);
		System.out.println(sys);
		System.out.println(sys.length());
		String id =UUID.randomUUID().toString().replaceAll("-", "");
		System.out.println(id);
		String s="UNIS-DNu6AKHh4Y6SLFErUsfdayeHK";
		System.out.println(s.length());
		String str="UNIS-"+UUID.randomUUID().toString().replaceAll("-", "").substring(0,25).toUpperCase();
		System.out.println(str.length());
		System.out.println(str);

	}
	
	public syncDataDao getSyncDao() {
		return syncDao;
	}
	
	public void setSyncDao(syncDataDao syncDao) {
		this.syncDao = syncDao;
	}
}
