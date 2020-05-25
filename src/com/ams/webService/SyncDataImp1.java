package com.ams.webService;

import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ams.config.PropertiesConfig;
import com.ams.dao.syncDataDao;
import com.ams.util.BaseDataUtil;
import com.ams.util.Sha256;
import com.ams.util.UniqueKeyMaker;
import com.ams.util.XmlFileUtil;
import com.ams.widget.EFileTransferServie;
import com.ams.widget.FtpEfileService;
import com.ams.widget.LocalEfileService;

public class SyncDataImp1 {
	private static Logger logger = Logger.getLogger(SyncDataImp1.class.getName());
	private static Map<String,String>arcidmap = new HashMap<String,String>();
	private static Map<String,String>arcnamemap = new HashMap<String,String>();
	private static Map<String,String>fcodemap = new HashMap<String,String>();
	private static Map<String,String>proidmap = new HashMap<String,String>();
	private static Map<String,String>newSysmap = new HashMap<String,String>();
	protected syncDataDao syncDao;
	private EFileTransferServie efileSourceService = null, efileUploadService = null;
	private static PropertiesConfig paramConfig = new PropertiesConfig(SyncDataImp1.class.getClassLoader().getResource("import.properties"));
	private String procode = paramConfig.getParam("procode"); //
	private String fprocode = paramConfig.getParam("fprocode");
	public String arcSrv_LZFile(String data){
		logger.info("data："+data);
		String type = "1";
		String typeWithIn = "";
		String sysid="";
		String serviceid="";
		String archid="";
		String title="";
		String filingdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String serviceType="arcSrv_LZFile";
		
		//校验非空参数
		if( data ==null || "".equals(data)){
			type = "0";
			typeWithIn = "传入的数据xml不能为空";
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		
		//校验电子文件服务配置
		logger.info( "开始电子文件服务校验...");
		
		String efilesource = paramConfig.getParam("EfileSource");
		String efiletarget = paramConfig.getParam("EfileTarget");
		
		try{
			efileSourceService = getEfileTransferService(efilesource);
			efileUploadService = getEfileTransferService(efiletarget);
			if(efileSourceService==null||efileUploadService==null)return xmlReturn("0","电子文件传输相关配置有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
			if(!efileSourceService.testOpen()||!efileUploadService.testOpen())return xmlReturn("0","电子文件相关服务未开启",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return xmlReturn("0","电子文件传输相关配置有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		logger.info( "...电子文件服务校验结束");
		
		String libcode=paramConfig.getParam("libcode");
		if("".equals(libcode)){
			return xmlReturn("0","档案门类code未配置",sysid,serviceid,archid,title,serviceType,filingdate,data);
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
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}	
		logger.info( "...校验数据xml");
		
		logger.info( "解析数据xml...");
		Element root=document.getDocumentElement();
		NodeList nodeList = root.getChildNodes();
		Element system = null;
		Element archive = null;
		Element digest = null;
		for(int j=0;j<nodeList.getLength();j++){
			if(nodeList.item(j) instanceof Element && "SYSTEM".equals(nodeList.item(j).getNodeName())){
				system = (Element) nodeList.item(j);
			}else if(nodeList.item(j) instanceof Element && "ARCHIVE".equals(nodeList.item(j).getNodeName())){
				archive = (Element) nodeList.item(j);
			}else if(nodeList.item(j) instanceof Element && "DIGEST".equals(nodeList.item(j).getNodeName())){
				digest = (Element) nodeList.item(j);
			}else{
				continue;
			}
		}
		if(system==null){
			return xmlReturn("0","解析数据xml中无SYSTEM节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		String archiveValue="";
		if(data.indexOf("<ARCHIVE>")>-1&&data.indexOf("</ARCHIVE>")>-1){
			archiveValue=data.substring(data.indexOf("<ARCHIVE>"),data.indexOf("</ARCHIVE>")+10);
			logger.info("<ARCHIVE>信息："+archiveValue);
		}
		if(archive==null||"".equals(archiveValue)){
			return xmlReturn("0","解析数据xml中无ARCHIVE节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		if(digest==null){
			return xmlReturn("0","解析数据xml中无DIGEST节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		NodeList dnodeList = digest.getChildNodes();
		Element origin = null;
		for(int j=0;j<dnodeList.getLength();j++){
			if(dnodeList.item(j) instanceof Element && "ORIGIN".equals(dnodeList.item(j).getNodeName())){
				origin = (Element) dnodeList.item(j);
				break;
			}else{
				continue;
			}
		}
		if(origin==null){
			return xmlReturn("0","解析数据xml中DIGEST节点下无ORIGIN节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String sha256=origin.getTextContent();
		String arcSha256=Sha256.getSHA256(archiveValue);
		if(!arcSha256.equals(sha256)){
			logger.info("xml-DIGEST-ORIGIN-value="+sha256);
			logger.info("计算-xml-ARCHIVE-SHA256="+arcSha256);
			return xmlReturn("0","比对用SHA256算法对[ARCHIVE]节点及其内容计算的摘要字符串不一致",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		
		
		sysid=system.getAttribute("SYSID");
		serviceid=system.getAttribute("SERVICEID");
		String unitsys=BaseDataUtil.trimString(system.getAttribute("UNITSYS"));
		if("".equals(unitsys)){
			return xmlReturn("0","解析数据xml中无SYSTEM节点UNITSYS有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String fondsid=paramConfig.getParam(unitsys);
		if("".equals(fondsid)){
			return xmlReturn("0","UNITSYS="+unitsys+" 对应配置有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		try{
			//获取字段对应关系
			//文件级对应字段
			List<String> fileFlds = paramConfig.getParamList("FileFlds");
			Map<String,String> fileFldMap = buildLinkHashMap(fileFlds);
			//文件级默认值字段
			List<String> fileDefFlds = paramConfig.getParamList("FileDefaultFlds");
			Map<String,String> fileDefFldMap = buildLinkHashMap(fileDefFlds);
			//文件级非空字段
			List<String> fileNotNullFlds = paramConfig.getParamList("FileNotNullFlds");
			
			//电子文件级对应字段
			List<String> efileFlds = paramConfig.getParamList("EfileFlds");
			Map<String,String> efileFldMap = buildLinkHashMap(efileFlds);
			//电子文件级默认字段
			List<String> efileDefFlds = paramConfig.getParamList("EfileDefaultFlds");
			Map<String,String> efileDefFldMap = buildLinkHashMap(efileDefFlds);
			//电子文件级非空字段
			List<String> efileNotNullFlds = paramConfig.getParamList("EfileNotNullFlds");
			//电子文件级继承字段
			List<String> efileExtendsFlds = paramConfig.getParamList("EfileExtendsFlds");
			Map<String,String> efileExtendsFldMap = buildLinkHashMap(efileExtendsFlds);
			
			String copyEfile = paramConfig.getParam("CopyEFile");
			
			logger.info( "解析数据...");
			NodeList fnodeList = archive.getChildNodes();
			Element fileNode = null;
			for(int j=0;j<fnodeList.getLength();j++){
				if(fnodeList.item(j) instanceof Element && "FILE".equals(fnodeList.item(j).getNodeName())){
					fileNode = (Element) fnodeList.item(j);
					break;
				}else{
					continue;
				}
			}
			if(fileNode==null){
				return xmlReturn("0","解析数据xml中ARCHIVE节点无FILE",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			//获取案卷级数据
			Map<String, Object> xmlFileRow =namedNodeToMap(fileNode);
			//根据字段对应关系获取案卷级数据
			Map<String, Object>fileRow=XmlMapToArcMap(fileFldMap,xmlFileRow);
			
			archid=BaseDataUtil.trimString(fileRow.get("ID"));
			title=BaseDataUtil.trimString(fileRow.get("TITLE"));
			//校验数据
			String validate=notNullValidate(fileRow,fileNotNullFlds,fileFldMap,"文件:"+fileRow.get("ID"));
			if(!"".equals(validate)){
				return xmlReturn("0",validate,sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			fileRow.put("FONDSID",fondsid);
			String fondcode=getFondcode(fondsid);
			if(fondcode==null || "".equals(fondcode)){
				logger.error("fondcode_未获取到");
				return xmlReturn("0","fondcode_未获取到",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			fileRow.put("FONDSCODE", fondcode);
			
			if(syncDao.testexit(BaseDataUtil.trimString(fileRow.get("DID")),libcode,"FILE",fondsid)){
				return xmlReturn("0","重复归档id"+BaseDataUtil.trimString(fileRow.get("DID")),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String arctypeId=getArcTypeId(libcode,fondsid);
			if(arctypeId==null || "".equals(arctypeId)){
				logger.error("arctypeId未获取到");
				return xmlReturn("0","arctypeId未获取到",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String projectid=getFileProjectId(libcode,fondsid,fondcode);
			if(projectid==null || "".equals(projectid)){
				logger.error("projectid未获取到");
				return xmlReturn("0","projectid未获取到",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			fileRow.put("PROJECTID", projectid);
			fileRow.put("PROARCHCODE", fprocode);
			
			//解析电子文件
			NodeList efileNodes =  fileNode.getChildNodes();
			//创建存放电子文件级数据的集合，一个map为一条电子文件数据，key：字段名称；value：字段值
			List<Map<String, Object>> eList = new ArrayList<Map<String, Object>>();
			Element efiles = null;
			for(int e=0;e<efileNodes.getLength();e++){
				
				if(efileNodes.item(e) instanceof Element && "EFILES".equals(efileNodes.item(e).getNodeName())){
					efiles = (Element) efileNodes.item(e);
					break;
				}else{
					continue;
				}
			}
			if(efiles==null){
				return xmlReturn("0","解析数据xml中FILE节点无EFILES",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			NodeList efileList = efiles.getChildNodes();
			int elsh=0;
			String ftitle="";
			for(int y=0;y<efileList.getLength();y++){
				Element efileNode = null;
				if(efileList.item(y) instanceof Element && "EFILE".equals(efileList.item(y).getNodeName())){
					efileNode = (Element) efileList.item(y);
					elsh++;
				}else{
					continue;
				}
				//获取电子文件级数据
				Map<String, Object> xmlEfileRow =namedNodeToMap(efileNode);
				//根据字段对应关系获取电子文件级数据
				Map<String, Object>efileRow=XmlMapToArcMap(efileFldMap,xmlEfileRow);	
				
				//校验数据
				String eValidate=notNullValidate(efileRow,efileNotNullFlds,efileFldMap,"文件:"+fileRow.get("ID")+"电子文件:"+efileRow.get("ID"));
				if(!"".equals(eValidate)){
					return xmlReturn("0",eValidate,sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				if("".equals(ftitle)){
					ftitle=BaseDataUtil.trimString(efileRow.get("TITLE"));
				}
				String filepath=BaseDataUtil.trimString(efileRow.get("FILEPATH"));
				filepath = filepath.replaceAll("\\\\", "/");
				boolean bn = false;
				try{
					if("false".equals(copyEfile)){//本地校验
						if(!filepath.startsWith("/uploads")){
							String remptePath="/uploads/company"+fondsid+"/fonds"+fondsid+"/"+arctypeId+filepath;
							bn=efileUploadService.testExist(remptePath);
						}
					}else{
						bn = efileSourceService.testExist(filepath);
					}
					
					
				}catch(Exception e){
					e.printStackTrace();
					logger.info("检验电子文件是否存在出错------"+e.getMessage());
					return xmlReturn("0","检验电子文件是否存在出错------"+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				if(!bn){
					logger.info("FILEPATH"+"_@_"+filepath+"_@_"+"电子文件不存在");
					return xmlReturn("0","FILEPATH"+"_@_"+filepath+"_@_"+"电子文件不存在",sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				//填充字段
				efileRow.putAll(efileDefFldMap);
				setExtendsFld(efileRow,fileRow,efileExtendsFldMap);
				efileRow.put("TABLEID", "f"+fondsid+"_"+libcode+"_"+"document");
				efileRow.put("EXTENSION", filepath.substring(filepath.lastIndexOf(".")+1));
				efileRow.put("ARCHTYPEID", arctypeId);
				if("false".equals(copyEfile)){
					if(!filepath.startsWith("/uploads")){
						filepath="/uploads/company"+fondsid+"/fonds"+fondsid+"/"+arctypeId+filepath;
					}

					efileRow.put("FILEPATH", filepath.substring(0,filepath.lastIndexOf("/")+1));
					efileRow.put("SAVEFILENAME",filepath.substring(filepath.lastIndexOf("/")+1));
				}
				//填充默认值字段
				setDefaultFld(efileRow,efileDefFldMap);
				
				eList.add(efileRow);
			}
			fileRow.put("FILESNUM",elsh);
			fileRow.put("FILETITLE",ftitle);
			
			//填充默认值字段
			setDefaultFld(fileRow,fileDefFldMap);
			
			logger.info("文件信息："+fileRow);
		
			if(!"false".equals(copyEfile)){
				//下载电子文件
				logger.info("开始下载电子文件");
				for(int e=0;e<eList.size();e++){
					Map<String, Object> efileRow =(Map<String, Object>) eList.get(e);
					List<String> errorlist=transferefiles(efileRow,libcode,fondsid,efileSourceService,efileUploadService);
					if(errorlist.size()>0){
						return xmlReturn("0",errorlist.toString(),sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
				}
				logger.info("下载电子文件结束");
			}
			logger.info("电子文件信息:"+eList);
			logger.info( "...解析数据");
			//开始保存数据
			logger.info( "保存数据...");
			//以下为主键策略, mode不为空时，使用新的主键
			String mode = paramConfig.getParam("SyscodeCreateMode");
			if(mode!=null){// 在原主键不符合归档的情况下
				String newfilesys = getCreateSyscodeByMode(BaseDataUtil.trimString(fileRow.get("ID")),mode,"FILE",libcode,fondsid,filingdate);
				fileRow.put("ID",newfilesys);
				if(fileRow.get("RELATEDFILEID")!=null&&!"".equals(fileRow.get("RELATEDFILEID"))){
					String glid=fileRow.get("RELATEDFILEID").toString();
					String [] glarr=glid.split(",");
					String newglid="";
					for(int s=0;s<glarr.length;s++){
						if(!"".equals(glarr[s])){
							String newglid1 =  getCreateSyscodeByMode(glarr[s],mode,"FILE",libcode,fondsid,filingdate);
							if("".equals(newglid)){
								newglid=newglid1;
							}else{
								newglid=newglid+","+newglid1;
							}
						}
						
					}
					fileRow.put("RELATEDFILEID", newglid);
				}
				for(Map <String,Object> m :eList){
					m.put("ARCHID", newfilesys);
					
					String efilesys = (String)m.get("ID");
					String newefilesys =  getCreateSyscodeByMode(efilesys,mode,"EFILE",libcode,fondsid,filingdate);
					m.put("ID", newefilesys);
				}
		
			}
			try{
				syncDao.importFileData(fileRow,eList,libcode,fondsid,paramConfig);
			}catch(Exception e){
				e.printStackTrace();
				logger.info("向数据库保存数据出错-----："+e.getMessage());
				return xmlReturn("0","向数据库保存数据出错-----："+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			return xmlReturn("1","归档成功",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			e.printStackTrace();
			logger.info("归档案卷数据出错..."+e.getMessage());
			return xmlReturn("0","归档案卷数据出错..."+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
	}
	
	public String arcSrv_LZVolume(String data){
		logger.info("data："+data);
		String type = "1";
		String typeWithIn = "";
		String sysid="";
		String serviceid="";
		String archid="";
		String title="";
		String filingdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String serviceType="arcSrv_LZVolume";
		
		//校验非空参数
		if( data ==null || "".equals(data)){
			type = "0";
			typeWithIn = "传入的数据xml不能为空";
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		//校验电子文件服务配置
		logger.info( "开始电子文件服务校验...");
		
		String efilesource = paramConfig.getParam("EfileSource");
		String efiletarget = paramConfig.getParam("EfileTarget");
		
		try{
			efileSourceService = getEfileTransferService(efilesource);
			efileUploadService = getEfileTransferService(efiletarget);
			if(efileSourceService==null||efileUploadService==null)return xmlReturn("0","电子文件传输相关配置有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
			if(!efileSourceService.testOpen()||!efileUploadService.testOpen())return xmlReturn("0","电子文件相关服务未开启",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return xmlReturn("0","电子文件传输相关配置有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		logger.info( "...电子文件服务校验结束");
		
		String libcode=paramConfig.getParam("libcode");
		if("".equals(libcode)){
			return xmlReturn("0","档案门类code未配置",sysid,serviceid,archid,title,serviceType,filingdate,data);
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
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}	
		logger.info( "...校验数据xml");
		
		logger.info( "解析数据xml...");
		Element root=document.getDocumentElement();
		NodeList nodeList = root.getChildNodes();
		Element system = null;
		Element archive = null;
		Element digest = null;
		
		for(int j=0;j<nodeList.getLength();j++){
			if(nodeList.item(j) instanceof Element && "SYSTEM".equals(nodeList.item(j).getNodeName())){
				system = (Element) nodeList.item(j);
			}else if(nodeList.item(j) instanceof Element && "ARCHIVE".equals(nodeList.item(j).getNodeName())){
				archive = (Element) nodeList.item(j);
			}else if(nodeList.item(j) instanceof Element && "DIGEST".equals(nodeList.item(j).getNodeName())){
				digest = (Element) nodeList.item(j);
			}else{
				continue;
			}
		}
		if(system==null){
			return xmlReturn("0","解析数据xml中无SYSTEM节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String archiveValue="";
		if(data.indexOf("<ARCHIVE>")>-1&&data.indexOf("</ARCHIVE>")>-1){
			archiveValue=data.substring(data.indexOf("<ARCHIVE>"),data.indexOf("</ARCHIVE>")+10);
			logger.info("<ARCHIVE>信息："+archiveValue);
		}
		if(archive==null||"".equals(archiveValue)){
			return xmlReturn("0","解析数据xml中无ARCHIVE节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		if(digest==null){
			return xmlReturn("0","解析数据xml中无DIGEST节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		NodeList dnodeList = digest.getChildNodes();
		Element origin = null;
		for(int j=0;j<dnodeList.getLength();j++){
			if(dnodeList.item(j) instanceof Element && "ORIGIN".equals(dnodeList.item(j).getNodeName())){
				origin = (Element) dnodeList.item(j);
				break;
			}else{
				continue;
			}
		}
		if(origin==null){
			return xmlReturn("0","解析数据xml中DIGEST节点下无ORIGIN节点",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String sha256=origin.getTextContent();
		String arcSha256=Sha256.getSHA256(archiveValue);
		if(!arcSha256.equals(sha256)){
			logger.info("xml-DIGEST-ORIGIN-value="+sha256);
			logger.info("计算-xml-ARCHIVE-SHA256="+arcSha256);
			return xmlReturn("0","比对用SHA256算法对[ARCHIVE]节点及其内容计算的摘要字符串不一致",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		
		sysid=system.getAttribute("SYSID");
		serviceid=system.getAttribute("SERVICEID");
		
		String unitsys=BaseDataUtil.trimString(system.getAttribute("UNITSYS"));
		if("".equals(unitsys)){
			return xmlReturn("0","解析数据xml中无SYSTEM节点UNITSYS有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String fondsid=paramConfig.getParam(unitsys);
		if("".equals(fondsid)){
			return xmlReturn("0","UNITSYS="+unitsys+" 对应配置有误",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		try{
			//获取字段对应关系
			//案卷级对应字段
			List<String> volFileFlds = paramConfig.getParamList("VolFileFlds");
			Map<String,String> volFileFldMap = buildLinkHashMap(volFileFlds);
			//案卷级默认值字段
			List<String> volFileDefFlds = paramConfig.getParamList("VOlFileDefaultFlds");
			Map<String,String> volFileDefFldMap = buildLinkHashMap(volFileDefFlds);
			//案卷级非空字段
			List<String> volNotNullFlds = paramConfig.getParamList("VolNotNullFlds");
			
			//卷内文件级对应字段
			List<String> fileFlds = paramConfig.getParamList("VFileFlds");
			Map<String,String> fileFldMap = buildLinkHashMap(fileFlds);
			//卷内文件级默认值字段
			List<String> fileDefFlds = paramConfig.getParamList("VFileDefaultFlds");
			Map<String,String> fileDefFldMap = buildLinkHashMap(fileDefFlds);
			//卷内文件级非空字段
			List<String> fileNotNullFlds = paramConfig.getParamList("VFileNotNullFlds");
			//卷内文件级继承字段
			List<String> fileExtendsFlds = paramConfig.getParamList("VFileExtendsFlds");
			Map<String,String> fileExtendsFldMap = buildLinkHashMap(fileExtendsFlds);
			
			//电子文件级对应字段
			List<String> efileFlds = paramConfig.getParamList("EfileFlds");
			Map<String,String> efileFldMap = buildLinkHashMap(efileFlds);
			//电子文件级默认字段
			List<String> efileDefFlds = paramConfig.getParamList("EfileDefaultFlds");
			Map<String,String> efileDefFldMap = buildLinkHashMap(efileDefFlds);
			//电子文件级非空字段
			List<String> efileNotNullFlds = paramConfig.getParamList("EfileNotNullFlds");
			//电子文件级继承字段
			List<String> efileExtendsFlds = paramConfig.getParamList("EfileExtendsFlds");
			Map<String,String> efileExtendsFldMap = buildLinkHashMap(efileExtendsFlds);
			
			String copyEfile = paramConfig.getParam("CopyEFile");
			
			logger.info( "解析数据...");
			NodeList vnodeList = archive.getChildNodes();
			Element volNode = null;
			for(int j=0;j<vnodeList.getLength();j++){
				if(vnodeList.item(j) instanceof Element && "VOLUME".equals(vnodeList.item(j).getNodeName())){
					volNode = (Element) vnodeList.item(j);
					break;
				}else{
					continue;
				}
			}
			if(volNode==null){
				return xmlReturn("0","解析数据xml中ARCHIVE节点无VOLUME",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			//获取案卷级数据
			Map<String, Object> xmlVolRow =namedNodeToMap(volNode);
			//根据字段对应关系获取案卷级数据
			Map<String, Object>volRow=XmlMapToArcMap(volFileFldMap,xmlVolRow);
			
			
			archid=BaseDataUtil.trimString(volRow.get("ID"));
			title=BaseDataUtil.trimString(volRow.get("TITLE"));
			//校验数据
			String validate=notNullValidate(volRow,volNotNullFlds,volFileFldMap,"案卷:"+volRow.get("ID"));
			if(!"".equals(validate)){
				return xmlReturn("0",validate,sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			volRow.put("FONDSID",fondsid);
			String fondcode=getFondcode(fondsid);
			if(fondcode==null || "".equals(fondcode)){
				logger.error("fondcode_未获取到");
				return xmlReturn("0","fondcode_未获取到",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			volRow.put("FONDSCODE", fondcode);
			
			if(syncDao.testexit(BaseDataUtil.trimString(volRow.get("DID")),libcode,"VOL",fondsid)){
				return xmlReturn("0","重复归档id"+BaseDataUtil.trimString(volRow.get("DID")),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String arctypeId=getArcTypeId(libcode,fondsid);
			if(arctypeId==null || "".equals(arctypeId)){
				logger.error("arctypeId未获取到");
				return xmlReturn("0","arctypeId未获取到",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String projectid=getProjectId(libcode,fondsid,fondcode);
			if(projectid==null || "".equals(projectid)){
				logger.error("projectid未获取到");
				return xmlReturn("0","projectid未获取到",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			volRow.put("PROJECTID", projectid);
			volRow.put("PROARCHCODE", procode);
			
			//填充默认值字段
			setDefaultFld(volRow,volFileDefFldMap);
			
			logger.info("案卷信息："+volRow);
			
			//解析文件级数据
			NodeList volList = volNode.getChildNodes();
			Element files = null;
			for(int j=0;j<volList.getLength();j++){
				
				if(volList.item(j) instanceof Element && "FILES".equals(volList.item(j).getNodeName())){
					files = (Element) volList.item(j);
					break;
				}else{
					continue;
				}
			}
			if(files==null){
				return xmlReturn("0","解析数据xml中VOLUME节点无FILES",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			NodeList fileList = files.getChildNodes();
			//创建存放文件级数据的集合，一个map为一条文件级数据，key：字段名称；value：字段值
			List<Map<String, Object>> fList = new ArrayList<Map<String, Object>>();
			//创建存放电子文件级数据的集合，一个map为一条电子文件数据，key：字段名称；value：字段值
			List<Map<String, Object>> eList = new ArrayList<Map<String, Object>>();
			int flsh=0;
			for(int j=0;j<fileList.getLength();j++){
				Element fileNode = null;
				if(fileList.item(j) instanceof Element && "FILE".equals(fileList.item(j).getNodeName())){
					fileNode = (Element) fileList.item(j);
					flsh++;
				}else{
					continue;
				}
				
				//获取文件级数据
				Map<String, Object> xmlFileRow =namedNodeToMap(fileNode);
				//根据字段对应关系获取案卷级数据
				Map<String, Object>fileRow=XmlMapToArcMap(fileFldMap,xmlFileRow);
				
				fileRow.put("VOLSEQUENCE", flsh);
				//填充继承字段
				setExtendsFld(fileRow,volRow,fileExtendsFldMap);
				//校验数据
				String fValidate=notNullValidate(fileRow,fileNotNullFlds,fileFldMap,"案卷:"+volRow.get("ID")+"-卷内文件:"+fileRow.get("ID"));
				if(!"".equals(fValidate)){
					return xmlReturn("0",fValidate,sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				//解析电子文件
				NodeList efileNodes =  fileNode.getChildNodes();
				Element efiles = null;
				for(int e=0;e<efileNodes.getLength();e++){
					
					if(efileNodes.item(e) instanceof Element && "EFILES".equals(efileNodes.item(e).getNodeName())){
						efiles = (Element) efileNodes.item(e);
						break;
					}else{
						continue;
					}
				}
				if(efiles==null){
					return xmlReturn("0","解析数据xml中FILE节点无EFILES",sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				NodeList efileList = efiles.getChildNodes();
				int elsh=0;
				String ftitle="";
				for(int y=0;y<efileList.getLength();y++){
					Element efileNode = null;
					if(efileList.item(y) instanceof Element && "EFILE".equals(efileList.item(y).getNodeName())){
						efileNode = (Element) efileList.item(y);
						elsh++;
					}else{
						continue;
					}
					//获取电子文件级数据
					Map<String, Object> xmlEfileRow =namedNodeToMap(efileNode);
					//根据字段对应关系获取电子文件级数据
					Map<String, Object>efileRow=XmlMapToArcMap(efileFldMap,xmlEfileRow);	
					
					//校验数据
					String eValidate=notNullValidate(efileRow,efileNotNullFlds,efileFldMap,"案卷:"+volRow.get("ID")+"-卷内文件:"+fileRow.get("ID")+"电子文件:"+efileRow.get("ID"));
					if(!"".equals(eValidate)){
						return xmlReturn("0",eValidate,sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
					if("".equals(ftitle)){
						ftitle=BaseDataUtil.trimString(efileRow.get("TITLE"));
					}
					String filepath=BaseDataUtil.trimString(efileRow.get("FILEPATH"));
					filepath = filepath.replaceAll("\\\\", "/");
					boolean bn = false;
					try{
						if("false".equals(copyEfile)){//本地校验
							if(!filepath.startsWith("/uploads")){
								String remptePath="/uploads/company"+fondsid+"/fonds"+fondsid+"/"+arctypeId+filepath;
								bn=efileUploadService.testExist(remptePath);
							}
						}else{
							bn = efileSourceService.testExist(filepath);
						}
						
						
					}catch(Exception e){
						e.printStackTrace();
						logger.info("检验电子文件是否存在出错------"+e.getMessage());
						return xmlReturn("0","检验电子文件是否存在出错------"+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
					if(!bn){
						logger.info("FILEPATH"+"_@_"+filepath+"_@_"+"电子文件不存在");
						return xmlReturn("0","FILEPATH"+"_@_"+filepath+"_@_"+"电子文件不存在",sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
					//填充字段
					efileRow.putAll(efileDefFldMap);
					setExtendsFld(efileRow,fileRow,efileExtendsFldMap);
					efileRow.put("TABLEID", "f"+fondsid+"_"+libcode+"_"+"document");
					efileRow.put("EXTENSION", filepath.substring(filepath.lastIndexOf(".")+1));
					efileRow.put("ARCHTYPEID", arctypeId);
					if("false".equals(copyEfile)){
						if(!filepath.startsWith("/uploads")){
							filepath="/uploads/company"+fondsid+"/fonds"+fondsid+"/"+arctypeId+filepath;
						}

						efileRow.put("FILEPATH", filepath.substring(0,filepath.lastIndexOf("/")+1));
						efileRow.put("SAVEFILENAME",filepath.substring(filepath.lastIndexOf("/")+1));
					}
					
					//填充默认值字段
					setDefaultFld(efileRow,efileDefFldMap);
					
					eList.add(efileRow);
				}
				fileRow.put("FILESNUM",elsh);
				fileRow.put("FILETITLE",ftitle);
				
				//填充默认值字段
				setDefaultFld(fileRow,fileDefFldMap);
				
				fList.add(fileRow);
			}
			if(!"false".equals(copyEfile)){
				//下载电子文件
				logger.info("开始下载电子文件");
				for(int e=0;e<eList.size();e++){
					Map<String, Object> efileRow =(Map<String, Object>) eList.get(e);
					List<String> errorlist=transferefiles(efileRow,libcode,fondsid,efileSourceService,efileUploadService);
					if(errorlist.size()>0){
						return xmlReturn("0",errorlist.toString(),sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
				}
				logger.info("下载电子文件结束");
			}
			logger.info("文件信息:"+fList);
			logger.info("电子文件信息:"+eList);
			logger.info( "...解析数据");
			//开始保存数据
			logger.info( "保存数据...");
			//以下为主键策略, mode不为空时，使用新的主键
			String mode = paramConfig.getParam("SyscodeCreateMode");
			if(mode!=null){// 在原主键不符合归档的情况下
				String newvolsys = getCreateSyscodeByMode(BaseDataUtil.trimString(volRow.get("ID")),mode,"VOL",libcode,fondsid,filingdate);
				volRow.put("ID", newvolsys);
				for(Map <String,Object> m :fList){
					m.put("VOLID", newvolsys);
					String filesys = (String)m.get("ID");
					String newfilesys = getCreateSyscodeByMode(filesys,mode,"FILE",libcode,fondsid,filingdate);
					m.put("ID", newfilesys);
					if(m.get("RELATEDFILEID")!=null&&!"".equals(m.get("RELATEDFILEID"))){
						String glid=m.get("RELATEDFILEID").toString();
						String [] glarr=glid.split(",");
						String newglid="";
						for(int s=0;s<glarr.length;s++){
							if(!"".equals(glarr[s])){
								String newglid1 = getCreateSyscodeByMode(glarr[s],mode,"FILE",libcode,fondsid,filingdate);
								if("".equals(newglid)){
									newglid=newglid1;
								}else{
									newglid=newglid+","+newglid1;
								}
							}
						}
						m.put("RELATEDFILEID", newglid);
					}
					
				}
				for(Map <String,Object> m :eList){
					String psys = (String)m.get("ARCHID");
					String filenewsys = getCreateSyscodeByMode(psys,mode,"FILE",libcode,fondsid,filingdate);//此语句可获取 对应文件级的新主键
					m.put("ARCHID", filenewsys);
					
					String efilesys = (String)m.get("ID");
					String newefilesys =  getCreateSyscodeByMode(efilesys,mode,"EFILE",libcode,fondsid,filingdate);
					m.put("ID", newefilesys);
				}
			}
			try{
				syncDao.importVolData(volRow,fList,eList,libcode,fondsid,paramConfig);
			}catch(Exception e){
				e.printStackTrace();
				logger.info("向数据库保存数据出错-----："+e.getMessage());
				return xmlReturn("0","向数据库保存数据出错-----："+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			return xmlReturn("1","归档成功",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			e.printStackTrace();
			logger.info("归档案卷数据出错..."+e.getMessage());
			return xmlReturn("0","归档案卷数据出错..."+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
	}

	private void setDefaultFld(Map<String, Object> Row,
			Map<String, String> FileDefFldMap) {
		for(String key : FileDefFldMap.keySet()){
			String fie = FileDefFldMap.get(key);
			if("".equals(BaseDataUtil.trimString(Row.get(key)))){
				Row.put(key,fie);
			}
		}
	}

	private void setExtendsFld(Map<String, Object> fileRow,
			Map<String, Object> volRow, Map<String, String> fileExtendsFldMap) {
		for(String key : fileExtendsFldMap.keySet()){
			String fie = fileExtendsFldMap.get(key);
			fileRow.put(key, BaseDataUtil.trimString(volRow.get(fie)));
		}
		
	}
	private String notNullValidate(Map<String, Object> volRow,
			List<String> notNullFlds, Map<String, String> fileFldMap,String type) {
		String validate="";
		if(notNullFlds!=null&&notNullFlds.size()>0){
			for(int i=0;i<notNullFlds.size();i++){
				String fld=notNullFlds.get(i);
				if(volRow.get(fld)==null||"".equals(volRow.get(fld))){
					validate=validate+type+"-"+ fileFldMap.get(fld)+" 不能为空;";
				}
			}
		}
		
		return validate;
	}
	private Map<String, Object> XmlMapToArcMap(
			Map<String, String> filefieldmap, Map<String, Object> fileRow) {
		if(filefieldmap==null||filefieldmap.size()==0){
			return fileRow;
		}
		Map<String, Object> arcfileRow=new LinkedHashMap<String, Object>();
		for(String key : filefieldmap.keySet()){
			String fie = filefieldmap.get(key);
			arcfileRow.put(key, fileRow.get(fie)==null?"":fileRow.get(fie));
		}
		return arcfileRow;
	}
	private Map<String, Object> namedNodeToMap(Element Node) {
		Map<String, Object> Row =new HashMap<String, Object>();
		NamedNodeMap Map = Node.getAttributes();
		for (int j = 0; j < Map.getLength(); j++) {
			//通过item(index)方法获取book节点的某一个属性
			Node attr = Map.item(j);
			Row.put(attr.getNodeName(),attr.getNodeValue());
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
	
	private String getArcTypeId(String libcode, String unitsys) {
		String arcid="";
		String arcname="";
		try{
			if(arcidmap.get(unitsys+libcode)==null){
				String sql="SELECT ID,NAME FROM S_ARCHIVE_TYPE WHERE FONDSID=? and ARCHINDEX=?";
				Object [] args={unitsys,libcode};
				List<Map<String,Object>> list=syncDao.queryAmsData(sql, args);
				if(list!=null&&list.size()>0){
					Map<String,Object> map=list.get(0);
					arcid=map.get("ID")==null?"":map.get("ID").toString();
					arcname=map.get("NAME")==null?"":map.get("NAME").toString();
					arcidmap.put(unitsys+libcode, arcid);
					arcnamemap.put(unitsys+libcode, arcname);
				}
			}else{
				arcid=arcidmap.get(unitsys+libcode);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return arcid;
	}
	/**传送电子文件
	 * @param efilelist
	 * @param row
	 * @param libcode
	 * @param unitsys
	 * @param efileSService
	 * @param efileUService
	 */
	protected List<String> transferefiles(Map<String, Object> row, String libcode, String unitsys, EFileTransferServie efileSService, EFileTransferServie efileUService) {
		List<String> errorlist = new ArrayList<String>();
		String filepath="";
		try{
			filepath = (String)row.get("FILEPATH");
			String targetpath = filepath;
			String newfilepath = buildNewFilePath(row,libcode,unitsys);
			row.put("FILEPATH", newfilepath.substring(0,newfilepath.lastIndexOf("/")+1));
			targetpath = newfilepath;
			if(efileUService.testExist(targetpath)){
				logger.info("目标文件已存在,不执行电子文件复制："+targetpath);
			}
					
			boolean hastempfile = false;
			String localpath = efileSService.getLocalPath(filepath);
			if(localpath==null){//无法直接获取本地文件
				hastempfile = true;
				localpath = "temp/"+UUID.randomUUID().toString()+".tmp";
				boolean bn = efileSService.download(filepath, localpath);
				if(!bn){
					errorlist.add("FILEPATH"+"_@_"+filepath+"_@_"+"下载电子文件失败");
				}
			}
			logger.info("下载文件filepath="+filepath);	
			logger.info("下载文件localpath="+localpath);
			
			efileUService.upload(targetpath, localpath);
			logger.info("上传文件localpath="+localpath);
			logger.info("上传文件targetpath="+targetpath);
			if(hastempfile){				
				new File(localpath).delete();
			}
		}catch(Exception e){
			e.printStackTrace();
			errorlist.add("FILEPATH"+"_@_"+filepath+"_@_"+"下载电子文件异常"+e.getMessage());
		}
		return errorlist;
	}
	
	/**组装新的FILEPATH
	 * @param efileinfo
	 * @param fileinfo
	 * @param libcode
	 * @param unitsys
	 * @return
	 */
	protected String buildNewFilePath(Map<String, Object> efileinfo,String libcode,String unitsys){
		StringBuffer sb = new StringBuffer();//"/uploads/company1/fonds1/1503560297506601211/2017/20170905/"
		sb.append("/uploads/");
		sb.append("company"+unitsys+"/");
		sb.append("fonds"+unitsys+"/");
		sb.append(efileinfo.get("ARCHTYPEID")+"/");
		sb.append(new SimpleDateFormat("yyyy").format(new Date())+"/");
		sb.append(new SimpleDateFormat("yyyyMMdd").format(new Date())+"/");
		String filename = (String)efileinfo.get("TITLE");
		String ext = filename.substring(filename.lastIndexOf(".")+1);
		String savefilename="UNIS-"+UUID.randomUUID().toString().replaceAll("-", "").substring(0,25)+"."+ext.toLowerCase();
		efileinfo.put("savefilename",savefilename);
		sb.append(savefilename);
		return sb.toString();
	}
	private String getProjectId(String libcode,String unitsys,String fondcode) {
		String projectId="";
		try{
			if(proidmap.get(procode)==null){
				String tbname="F"+unitsys+"_"+libcode+"_"+"PROJECT";
				String sql="SELECT ID FROM "+tbname+" WHERE PROCODE=? ";
				Object [] args={procode};
				List<Map<String,Object>> list=syncDao.queryAmsData(sql, args);
				if(list!=null&&list.size()>0){
					Map<String,Object> map=list.get(0);
					projectId=map.get("ID")==null?"":map.get("ID").toString();
					proidmap.put(procode, projectId);
				}else{
					logger.info("未获取到项目信息，自动创建项目");
					String sys = UniqueKeyMaker.getPk(1);
					String sql1="INSERT INTO "+tbname+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					Object [] args1={sys,unitsys,"接口案卷归档数据",procode,null,null,"admin",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),"0",null,null,fondcode,null,null,null};
					syncDao.excuteUpdate4Ams(sql1, args1);
					proidmap.put(procode, sys);
					projectId=sys;
					
					String catid="";
					String sql2="SELECT CATID FROM  CATALOG WHERE CATNAME=? AND FONDSID=? ";
					Object [] args2={arcnamemap.get(unitsys+libcode),unitsys};
					List<Map<String,Object>> list1=syncDao.queryAmsData(sql2, args2);
					if(list1!=null&&list1.size()>0){
						Map<String,Object> map=list1.get(0);
						catid=map.get("CATID")==null?"":map.get("CATID").toString();
					}
					String sql3="insert into catalog (fondsid,catname,parentid,arrparentid,haschild,listorder,cattype,archtypeid,archlistlayout,addtype,datatype,archid_pro) " +
							"values(?,?,?,?,?,?,?,?,?,?,?,?)";
					Object [] args3={unitsys,"【"+procode+"】接口案卷归档数据",catid,catid+"-1","0","0","4",arcidmap.get(unitsys+libcode),"3","1","All",projectId};
					syncDao.excuteUpdate4Ams(sql3, args3);
				}
			}else{
				projectId=proidmap.get(procode);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return projectId;
	}
	private String getFileProjectId(String libcode,String unitsys,String fondcode) {
		String projectId="";
		try{
			if(proidmap.get(fprocode)==null){
				String tbname="F"+unitsys+"_"+libcode+"_"+"PROJECT";
				String sql="SELECT ID FROM "+tbname+" WHERE PROCODE=? ";
				Object [] args={fprocode};
				List<Map<String,Object>> list=syncDao.queryAmsData(sql, args);
				if(list!=null&&list.size()>0){
					Map<String,Object> map=list.get(0);
					projectId=map.get("ID")==null?"":map.get("ID").toString();
					proidmap.put(fprocode, projectId);
				}else{
					logger.info("未获取到项目信息，自动创建项目");
					String sys = UniqueKeyMaker.getPk(1);
					String sql1="INSERT INTO "+tbname+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					Object [] args1={sys,unitsys,"接口散文件归档数据",fprocode,null,null,"admin",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),"0",null,null,fondcode,null,null,null};
					syncDao.excuteUpdate4Ams(sql1, args1);
					proidmap.put(fprocode, sys);
					projectId=sys;
					
					String catid="";
					String sql2="SELECT CATID FROM  CATALOG WHERE CATNAME=? AND FONDSID=? ";
					Object [] args2={arcnamemap.get(unitsys+libcode),unitsys};
					List<Map<String,Object>> list1=syncDao.queryAmsData(sql2, args2);
					if(list1!=null&&list1.size()>0){
						Map<String,Object> map=list1.get(0);
						catid=map.get("CATID")==null?"":map.get("CATID").toString();
					}
					String sql3="insert into catalog (fondsid,catname,parentid,arrparentid,haschild,listorder,cattype,archtypeid,archlistlayout,addtype,datatype,archid_pro) " +
							"values(?,?,?,?,?,?,?,?,?,?,?,?)";
					Object [] args3={unitsys,"【"+fprocode+"】接口散文件归档数据",catid,catid+"-1","0","0","4",arcidmap.get(unitsys+libcode),"3","1","All",projectId};
					syncDao.excuteUpdate4Ams(sql3, args3);
				}
			}else{
				projectId=proidmap.get(fprocode);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return projectId;
	}

	private String getFondcode(String unitsys) {
		String fondcode="";
		try{
			if(fcodemap.get(unitsys)==null){
				String sql="SELECT CODE FROM S_FONDS WHERE ID=? ";
				Object [] args={unitsys};
				List<Map<String,Object>> list=syncDao.queryAmsData(sql, args);
				if(list!=null&&list.size()>0){
					Map<String,Object> map=list.get(0);
					fondcode=map.get("CODE")==null?"":map.get("CODE").toString();
					fcodemap.put(unitsys, fondcode);
				}
			}else{
				fondcode=fcodemap.get(unitsys);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return fondcode;
	}

	private String xmlReturn(String type, String typeWithIn){
		Document xmldoc = XmlFileUtil.createNewDocumen();
		Element xmlRoot = xmldoc.createElement("root");
		Element result = xmldoc.createElement("Result");
		Element memo = xmldoc.createElement("Memo");
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
	private String xmlReturn(String type, String typeWithIn, String sysid,
			String serviceid, String archid, String title, String serviceType,
			String filingdate,String data) {
		Map<String, Object> logRow=new LinkedHashMap<String, Object>();
		logRow.put("ID", UUID.randomUUID().toString().replaceAll("-", ""));
		logRow.put("SYSID", sysid);
		logRow.put("SERVICEID", serviceid);
		logRow.put("ARCHID", archid);
		logRow.put("TITLE", title);
		logRow.put("CLASS", serviceType);
		logRow.put("FILINGDATE", filingdate);
		logRow.put("STATUS", "1".equals(type)?"成功":"失败");
		logRow.put("DATA", data);
		logRow.put("REMARK", typeWithIn);
		try{
			syncDao.insertObject2Ams(BaseDataUtil.buildList(logRow),"D_LOG");
		}catch(Exception e){
			e.printStackTrace();
			logger.info("insert into d_log error："+ e.getMessage());
		}
		
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

	/**生成新的主键
	 * @param syscode
	 * @param mode
	 * @param arclvl
	 * @param libcode
	 * @param unitsys
	 * @return
	 */
	protected String getCreateSyscodeByMode(String syscode, String mode, String arclvl,String libcode,String unitsys,String nowtime) {
		if("newuuid".equalsIgnoreCase(mode)){
			String key=syscode+mode+arclvl+libcode+unitsys+nowtime;
			if(newSysmap.get(key)!=null){
				return newSysmap.get(key);
			}else{
//		优化提高归档执行效率，业务上不存在重复归档，每次归档（包括重复归档）档案系统都新增数据。此处优化成：直接获取新id，不将新id存入数据，减少与数据库交互	
//				String sql = "select SYSCODE from D_NEWSYS where OLDSYS=? and ARCLVL=? and LIBCODE=? and UNITSYS=? and NOWTIME=?";
//				Object[] args = new Object[]{syscode,arclvl,libcode,unitsys,nowtime};
//				List<Map<String,Object>> result = syncDao.queryAmsData(sql, args);
//				if(result!=null&&result.size()>0){
//					String sys=(String)result.get(0).get("SYSCODE");
//					newSysmap.put(key, sys);
//					return sys;
//				}else{
					String newsys = UniqueKeyMaker.getPk(1);
//					String newsys=getNewsys();
//					String insertsql = "insert into D_NEWSYS (SYSCODE,OLDSYS,ARCLVL,LIBCODE,UNITSYS,NOWTIME) values (?,?,?,?,?,?)";
//					Object[] args2 = new Object[]{newsys,syscode,arclvl,libcode,unitsys,nowtime};
//					syncDao.excuteUpdate4Ams(insertsql, args2);
					newSysmap.put(key, newsys);
					return newsys;
//				}
			}
			
		}else if("guid36".equalsIgnoreCase(mode)){
			return  syscode.replaceAll("-", "");
		}else{
			return syscode;
		}
	}
	private String getNewsys() {
		String sys="";
		while(true){
			String newsys = UniqueKeyMaker.getPk(1);
			String sql = "select SYSCODE from D_NEWSYS where syscode=? ";
			Object[] args = new Object[]{newsys};
			List<Map<String,Object>> result = syncDao.queryAmsData(sql, args);
			if(result==null||result.size()==0){
				sys=newsys;
				break;
			}
		}
		
		return sys;
	}
	/**生成新的主键
	 * @param syscode
	 * @param mode
	 * @param arclvl
	 * @param libcode
	 * @param unitsys
	 * @return
	 */
	protected String getCreateSyscodeByMode(String syscode, String mode, String arclvl,String libcode,String unitsys) {
		if("newuuid".equalsIgnoreCase(mode)){
			String sql = "select SYSCODE from D_NEWSYS where OLDSYS=? and ARCLVL=? and LIBCODE=? and UNITSYS=?";
			Object[] args = new Object[]{syscode,arclvl,libcode,unitsys};
			List<Map<String,Object>> result = syncDao.queryAmsData(sql, args);
			if(result!=null&&result.size()>0){
				return (String)result.get(0).get("SYSCODE");
			}else{
				String newsys = UniqueKeyMaker.getPk(1);
				String insertsql = "insert into D_NEWSYS (SYSCODE,OLDSYS,ARCLVL,LIBCODE,UNITSYS) values (?,?,?,?,?)";
				Object[] args2 = new Object[]{newsys,syscode,arclvl,libcode,unitsys};
				syncDao.excuteUpdate4Ams(insertsql, args2);
				return newsys;
			}
		}else if("guid36".equalsIgnoreCase(mode)){
			return  syscode.replaceAll("-", "");
		}else{
			return syscode;
		}
	}
	/**根据配置组建电子文件传输对象
	 * @param efilesource
	 * @return
	 */
	protected EFileTransferServie getEfileTransferService(String efilesource) {
		int index1 = efilesource.indexOf(":");
		if(index1<0){
			throw new RuntimeException("电子文件来源配置格式有误:"+efilesource);
		}
		String type = efilesource.substring(0,index1);
		if("ftp".equals(type)){
			String[] args = efilesource.substring(index1+1).split(":");
			if(args.length==4){
				FtpEfileService  fs = new FtpEfileService(args[0], args[1], args[2], args[3]);
				return fs;
			}else if(args.length==5){
				FtpEfileService  fs = new FtpEfileService(args[0], args[1], args[2], args[3],args[4]);
				return fs;
			}else{
				throw new RuntimeException("电子文件来源配置格式有误:"+efilesource);
			}
		}else if("local".equals(type)){
			LocalEfileService ls = new LocalEfileService(efilesource.substring(index1+1));
			return ls;
		}else{			
			return null;
		}
	}
	private void setAutovaluemap(Map<String, String> autovaluemap,String nowTime) {
		autovaluemap.put("MANAGESTATUS", "2");
//		autovaluemap.put("DEPARTMENT", "档案部门");
//		autovaluemap.put("DEPARTMENTID", "100000");
		autovaluemap.put("STATUS", "0");//0：整编库；1：归档；9删除
		autovaluemap.put("FILLINGNUM", "1");//归档份数
		autovaluemap.put("CREATETIME",nowTime);
		autovaluemap.put("CREATER","SYNC");
		autovaluemap.put("KNOWNSCOPE", "2");
		autovaluemap.put("EDITTIME", nowTime);
		autovaluemap.put("EDITOR", "SYNC");
		autovaluemap.put("EXT5", nowTime);
		
		
	}
	protected Map<String,String> buildLinkHashMap(List<String> sysflds){
		Map<String,String> m = new LinkedHashMap<String, String>();//使用LinkedHashMap 可以实现去重功能，且靠后的生效
		for(String fie : sysflds){
			fie = fie.trim();
			int index = fie.indexOf(":");
			if(index>=0){
				String key = fie.substring(0,index).toUpperCase();
				m.put(key, fie.substring(index+1));
			}else{
				m.put(fie.toUpperCase(), fie.toUpperCase());
			}
		}
		return m;
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
