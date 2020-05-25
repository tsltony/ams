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
		logger.info("data��"+data);
		String type = "1";
		String typeWithIn = "";
		String sysid="";
		String serviceid="";
		String archid="";
		String title="";
		String filingdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String serviceType="arcSrv_LZFile";
		
		//У��ǿղ���
		if( data ==null || "".equals(data)){
			type = "0";
			typeWithIn = "���������xml����Ϊ��";
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		
		//У������ļ���������
		logger.info( "��ʼ�����ļ�����У��...");
		
		String efilesource = paramConfig.getParam("EfileSource");
		String efiletarget = paramConfig.getParam("EfileTarget");
		
		try{
			efileSourceService = getEfileTransferService(efilesource);
			efileUploadService = getEfileTransferService(efiletarget);
			if(efileSourceService==null||efileUploadService==null)return xmlReturn("0","�����ļ����������������",sysid,serviceid,archid,title,serviceType,filingdate,data);
			if(!efileSourceService.testOpen()||!efileUploadService.testOpen())return xmlReturn("0","�����ļ���ط���δ����",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return xmlReturn("0","�����ļ����������������",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		logger.info( "...�����ļ�����У�����");
		
		String libcode=paramConfig.getParam("libcode");
		if("".equals(libcode)){
			return xmlReturn("0","��������codeδ����",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		//У���ȡ����xml��ʽ���ַ���
		logger.info( "У������xml...");
		Document document = null;
		try {
			document = XmlFileUtil.StringToXML(data);
		} catch (Exception e1) {
			logger.info( "��������xml����"+e1.getMessage());
			type = "0";
			typeWithIn = "�����xml�ַ�����,����ʧ�ܡ�";
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}	
		logger.info( "...У������xml");
		
		logger.info( "��������xml...");
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
			return xmlReturn("0","��������xml����SYSTEM�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		String archiveValue="";
		if(data.indexOf("<ARCHIVE>")>-1&&data.indexOf("</ARCHIVE>")>-1){
			archiveValue=data.substring(data.indexOf("<ARCHIVE>"),data.indexOf("</ARCHIVE>")+10);
			logger.info("<ARCHIVE>��Ϣ��"+archiveValue);
		}
		if(archive==null||"".equals(archiveValue)){
			return xmlReturn("0","��������xml����ARCHIVE�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		if(digest==null){
			return xmlReturn("0","��������xml����DIGEST�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
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
			return xmlReturn("0","��������xml��DIGEST�ڵ�����ORIGIN�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String sha256=origin.getTextContent();
		String arcSha256=Sha256.getSHA256(archiveValue);
		if(!arcSha256.equals(sha256)){
			logger.info("xml-DIGEST-ORIGIN-value="+sha256);
			logger.info("����-xml-ARCHIVE-SHA256="+arcSha256);
			return xmlReturn("0","�ȶ���SHA256�㷨��[ARCHIVE]�ڵ㼰�����ݼ����ժҪ�ַ�����һ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		
		
		sysid=system.getAttribute("SYSID");
		serviceid=system.getAttribute("SERVICEID");
		String unitsys=BaseDataUtil.trimString(system.getAttribute("UNITSYS"));
		if("".equals(unitsys)){
			return xmlReturn("0","��������xml����SYSTEM�ڵ�UNITSYS����",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String fondsid=paramConfig.getParam(unitsys);
		if("".equals(fondsid)){
			return xmlReturn("0","UNITSYS="+unitsys+" ��Ӧ��������",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		try{
			//��ȡ�ֶζ�Ӧ��ϵ
			//�ļ�����Ӧ�ֶ�
			List<String> fileFlds = paramConfig.getParamList("FileFlds");
			Map<String,String> fileFldMap = buildLinkHashMap(fileFlds);
			//�ļ���Ĭ��ֵ�ֶ�
			List<String> fileDefFlds = paramConfig.getParamList("FileDefaultFlds");
			Map<String,String> fileDefFldMap = buildLinkHashMap(fileDefFlds);
			//�ļ����ǿ��ֶ�
			List<String> fileNotNullFlds = paramConfig.getParamList("FileNotNullFlds");
			
			//�����ļ�����Ӧ�ֶ�
			List<String> efileFlds = paramConfig.getParamList("EfileFlds");
			Map<String,String> efileFldMap = buildLinkHashMap(efileFlds);
			//�����ļ���Ĭ���ֶ�
			List<String> efileDefFlds = paramConfig.getParamList("EfileDefaultFlds");
			Map<String,String> efileDefFldMap = buildLinkHashMap(efileDefFlds);
			//�����ļ����ǿ��ֶ�
			List<String> efileNotNullFlds = paramConfig.getParamList("EfileNotNullFlds");
			//�����ļ����̳��ֶ�
			List<String> efileExtendsFlds = paramConfig.getParamList("EfileExtendsFlds");
			Map<String,String> efileExtendsFldMap = buildLinkHashMap(efileExtendsFlds);
			
			String copyEfile = paramConfig.getParam("CopyEFile");
			
			logger.info( "��������...");
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
				return xmlReturn("0","��������xml��ARCHIVE�ڵ���FILE",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			//��ȡ��������
			Map<String, Object> xmlFileRow =namedNodeToMap(fileNode);
			//�����ֶζ�Ӧ��ϵ��ȡ��������
			Map<String, Object>fileRow=XmlMapToArcMap(fileFldMap,xmlFileRow);
			
			archid=BaseDataUtil.trimString(fileRow.get("ID"));
			title=BaseDataUtil.trimString(fileRow.get("TITLE"));
			//У������
			String validate=notNullValidate(fileRow,fileNotNullFlds,fileFldMap,"�ļ�:"+fileRow.get("ID"));
			if(!"".equals(validate)){
				return xmlReturn("0",validate,sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			fileRow.put("FONDSID",fondsid);
			String fondcode=getFondcode(fondsid);
			if(fondcode==null || "".equals(fondcode)){
				logger.error("fondcode_δ��ȡ��");
				return xmlReturn("0","fondcode_δ��ȡ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			fileRow.put("FONDSCODE", fondcode);
			
			if(syncDao.testexit(BaseDataUtil.trimString(fileRow.get("DID")),libcode,"FILE",fondsid)){
				return xmlReturn("0","�ظ��鵵id"+BaseDataUtil.trimString(fileRow.get("DID")),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String arctypeId=getArcTypeId(libcode,fondsid);
			if(arctypeId==null || "".equals(arctypeId)){
				logger.error("arctypeIdδ��ȡ��");
				return xmlReturn("0","arctypeIdδ��ȡ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String projectid=getFileProjectId(libcode,fondsid,fondcode);
			if(projectid==null || "".equals(projectid)){
				logger.error("projectidδ��ȡ��");
				return xmlReturn("0","projectidδ��ȡ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			fileRow.put("PROJECTID", projectid);
			fileRow.put("PROARCHCODE", fprocode);
			
			//���������ļ�
			NodeList efileNodes =  fileNode.getChildNodes();
			//������ŵ����ļ������ݵļ��ϣ�һ��mapΪһ�������ļ����ݣ�key���ֶ����ƣ�value���ֶ�ֵ
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
				return xmlReturn("0","��������xml��FILE�ڵ���EFILES",sysid,serviceid,archid,title,serviceType,filingdate,data);
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
				//��ȡ�����ļ�������
				Map<String, Object> xmlEfileRow =namedNodeToMap(efileNode);
				//�����ֶζ�Ӧ��ϵ��ȡ�����ļ�������
				Map<String, Object>efileRow=XmlMapToArcMap(efileFldMap,xmlEfileRow);	
				
				//У������
				String eValidate=notNullValidate(efileRow,efileNotNullFlds,efileFldMap,"�ļ�:"+fileRow.get("ID")+"�����ļ�:"+efileRow.get("ID"));
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
					if("false".equals(copyEfile)){//����У��
						if(!filepath.startsWith("/uploads")){
							String remptePath="/uploads/company"+fondsid+"/fonds"+fondsid+"/"+arctypeId+filepath;
							bn=efileUploadService.testExist(remptePath);
						}
					}else{
						bn = efileSourceService.testExist(filepath);
					}
					
					
				}catch(Exception e){
					e.printStackTrace();
					logger.info("��������ļ��Ƿ���ڳ���------"+e.getMessage());
					return xmlReturn("0","��������ļ��Ƿ���ڳ���------"+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				if(!bn){
					logger.info("FILEPATH"+"_@_"+filepath+"_@_"+"�����ļ�������");
					return xmlReturn("0","FILEPATH"+"_@_"+filepath+"_@_"+"�����ļ�������",sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				//����ֶ�
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
				//���Ĭ��ֵ�ֶ�
				setDefaultFld(efileRow,efileDefFldMap);
				
				eList.add(efileRow);
			}
			fileRow.put("FILESNUM",elsh);
			fileRow.put("FILETITLE",ftitle);
			
			//���Ĭ��ֵ�ֶ�
			setDefaultFld(fileRow,fileDefFldMap);
			
			logger.info("�ļ���Ϣ��"+fileRow);
		
			if(!"false".equals(copyEfile)){
				//���ص����ļ�
				logger.info("��ʼ���ص����ļ�");
				for(int e=0;e<eList.size();e++){
					Map<String, Object> efileRow =(Map<String, Object>) eList.get(e);
					List<String> errorlist=transferefiles(efileRow,libcode,fondsid,efileSourceService,efileUploadService);
					if(errorlist.size()>0){
						return xmlReturn("0",errorlist.toString(),sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
				}
				logger.info("���ص����ļ�����");
			}
			logger.info("�����ļ���Ϣ:"+eList);
			logger.info( "...��������");
			//��ʼ��������
			logger.info( "��������...");
			//����Ϊ��������, mode��Ϊ��ʱ��ʹ���µ�����
			String mode = paramConfig.getParam("SyscodeCreateMode");
			if(mode!=null){// ��ԭ���������Ϲ鵵�������
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
				logger.info("�����ݿⱣ�����ݳ���-----��"+e.getMessage());
				return xmlReturn("0","�����ݿⱣ�����ݳ���-----��"+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			return xmlReturn("1","�鵵�ɹ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			e.printStackTrace();
			logger.info("�鵵�������ݳ���..."+e.getMessage());
			return xmlReturn("0","�鵵�������ݳ���..."+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
	}
	
	public String arcSrv_LZVolume(String data){
		logger.info("data��"+data);
		String type = "1";
		String typeWithIn = "";
		String sysid="";
		String serviceid="";
		String archid="";
		String title="";
		String filingdate=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String serviceType="arcSrv_LZVolume";
		
		//У��ǿղ���
		if( data ==null || "".equals(data)){
			type = "0";
			typeWithIn = "���������xml����Ϊ��";
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		//У������ļ���������
		logger.info( "��ʼ�����ļ�����У��...");
		
		String efilesource = paramConfig.getParam("EfileSource");
		String efiletarget = paramConfig.getParam("EfileTarget");
		
		try{
			efileSourceService = getEfileTransferService(efilesource);
			efileUploadService = getEfileTransferService(efiletarget);
			if(efileSourceService==null||efileUploadService==null)return xmlReturn("0","�����ļ����������������",sysid,serviceid,archid,title,serviceType,filingdate,data);
			if(!efileSourceService.testOpen()||!efileUploadService.testOpen())return xmlReturn("0","�����ļ���ط���δ����",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return xmlReturn("0","�����ļ����������������",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		logger.info( "...�����ļ�����У�����");
		
		String libcode=paramConfig.getParam("libcode");
		if("".equals(libcode)){
			return xmlReturn("0","��������codeδ����",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		//У���ȡ����xml��ʽ���ַ���
		logger.info( "У������xml...");
		Document document = null;
		try {
			document = XmlFileUtil.StringToXML(data);
		} catch (Exception e1) {
			logger.info( "��������xml����"+e1.getMessage());
			type = "0";
			typeWithIn = "�����xml�ַ�����,����ʧ�ܡ�";
			return xmlReturn(type,typeWithIn,sysid,serviceid,archid,title,serviceType,filingdate,data);
		}	
		logger.info( "...У������xml");
		
		logger.info( "��������xml...");
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
			return xmlReturn("0","��������xml����SYSTEM�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String archiveValue="";
		if(data.indexOf("<ARCHIVE>")>-1&&data.indexOf("</ARCHIVE>")>-1){
			archiveValue=data.substring(data.indexOf("<ARCHIVE>"),data.indexOf("</ARCHIVE>")+10);
			logger.info("<ARCHIVE>��Ϣ��"+archiveValue);
		}
		if(archive==null||"".equals(archiveValue)){
			return xmlReturn("0","��������xml����ARCHIVE�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		if(digest==null){
			return xmlReturn("0","��������xml����DIGEST�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
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
			return xmlReturn("0","��������xml��DIGEST�ڵ�����ORIGIN�ڵ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String sha256=origin.getTextContent();
		String arcSha256=Sha256.getSHA256(archiveValue);
		if(!arcSha256.equals(sha256)){
			logger.info("xml-DIGEST-ORIGIN-value="+sha256);
			logger.info("����-xml-ARCHIVE-SHA256="+arcSha256);
			return xmlReturn("0","�ȶ���SHA256�㷨��[ARCHIVE]�ڵ㼰�����ݼ����ժҪ�ַ�����һ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		
		sysid=system.getAttribute("SYSID");
		serviceid=system.getAttribute("SERVICEID");
		
		String unitsys=BaseDataUtil.trimString(system.getAttribute("UNITSYS"));
		if("".equals(unitsys)){
			return xmlReturn("0","��������xml����SYSTEM�ڵ�UNITSYS����",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		String fondsid=paramConfig.getParam(unitsys);
		if("".equals(fondsid)){
			return xmlReturn("0","UNITSYS="+unitsys+" ��Ӧ��������",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}
		
		try{
			//��ȡ�ֶζ�Ӧ��ϵ
			//������Ӧ�ֶ�
			List<String> volFileFlds = paramConfig.getParamList("VolFileFlds");
			Map<String,String> volFileFldMap = buildLinkHashMap(volFileFlds);
			//����Ĭ��ֵ�ֶ�
			List<String> volFileDefFlds = paramConfig.getParamList("VOlFileDefaultFlds");
			Map<String,String> volFileDefFldMap = buildLinkHashMap(volFileDefFlds);
			//�����ǿ��ֶ�
			List<String> volNotNullFlds = paramConfig.getParamList("VolNotNullFlds");
			
			//�����ļ�����Ӧ�ֶ�
			List<String> fileFlds = paramConfig.getParamList("VFileFlds");
			Map<String,String> fileFldMap = buildLinkHashMap(fileFlds);
			//�����ļ���Ĭ��ֵ�ֶ�
			List<String> fileDefFlds = paramConfig.getParamList("VFileDefaultFlds");
			Map<String,String> fileDefFldMap = buildLinkHashMap(fileDefFlds);
			//�����ļ����ǿ��ֶ�
			List<String> fileNotNullFlds = paramConfig.getParamList("VFileNotNullFlds");
			//�����ļ����̳��ֶ�
			List<String> fileExtendsFlds = paramConfig.getParamList("VFileExtendsFlds");
			Map<String,String> fileExtendsFldMap = buildLinkHashMap(fileExtendsFlds);
			
			//�����ļ�����Ӧ�ֶ�
			List<String> efileFlds = paramConfig.getParamList("EfileFlds");
			Map<String,String> efileFldMap = buildLinkHashMap(efileFlds);
			//�����ļ���Ĭ���ֶ�
			List<String> efileDefFlds = paramConfig.getParamList("EfileDefaultFlds");
			Map<String,String> efileDefFldMap = buildLinkHashMap(efileDefFlds);
			//�����ļ����ǿ��ֶ�
			List<String> efileNotNullFlds = paramConfig.getParamList("EfileNotNullFlds");
			//�����ļ����̳��ֶ�
			List<String> efileExtendsFlds = paramConfig.getParamList("EfileExtendsFlds");
			Map<String,String> efileExtendsFldMap = buildLinkHashMap(efileExtendsFlds);
			
			String copyEfile = paramConfig.getParam("CopyEFile");
			
			logger.info( "��������...");
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
				return xmlReturn("0","��������xml��ARCHIVE�ڵ���VOLUME",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			//��ȡ��������
			Map<String, Object> xmlVolRow =namedNodeToMap(volNode);
			//�����ֶζ�Ӧ��ϵ��ȡ��������
			Map<String, Object>volRow=XmlMapToArcMap(volFileFldMap,xmlVolRow);
			
			
			archid=BaseDataUtil.trimString(volRow.get("ID"));
			title=BaseDataUtil.trimString(volRow.get("TITLE"));
			//У������
			String validate=notNullValidate(volRow,volNotNullFlds,volFileFldMap,"����:"+volRow.get("ID"));
			if(!"".equals(validate)){
				return xmlReturn("0",validate,sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			volRow.put("FONDSID",fondsid);
			String fondcode=getFondcode(fondsid);
			if(fondcode==null || "".equals(fondcode)){
				logger.error("fondcode_δ��ȡ��");
				return xmlReturn("0","fondcode_δ��ȡ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			volRow.put("FONDSCODE", fondcode);
			
			if(syncDao.testexit(BaseDataUtil.trimString(volRow.get("DID")),libcode,"VOL",fondsid)){
				return xmlReturn("0","�ظ��鵵id"+BaseDataUtil.trimString(volRow.get("DID")),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String arctypeId=getArcTypeId(libcode,fondsid);
			if(arctypeId==null || "".equals(arctypeId)){
				logger.error("arctypeIdδ��ȡ��");
				return xmlReturn("0","arctypeIdδ��ȡ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			String projectid=getProjectId(libcode,fondsid,fondcode);
			if(projectid==null || "".equals(projectid)){
				logger.error("projectidδ��ȡ��");
				return xmlReturn("0","projectidδ��ȡ��",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			volRow.put("PROJECTID", projectid);
			volRow.put("PROARCHCODE", procode);
			
			//���Ĭ��ֵ�ֶ�
			setDefaultFld(volRow,volFileDefFldMap);
			
			logger.info("������Ϣ��"+volRow);
			
			//�����ļ�������
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
				return xmlReturn("0","��������xml��VOLUME�ڵ���FILES",sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			NodeList fileList = files.getChildNodes();
			//��������ļ������ݵļ��ϣ�һ��mapΪһ���ļ������ݣ�key���ֶ����ƣ�value���ֶ�ֵ
			List<Map<String, Object>> fList = new ArrayList<Map<String, Object>>();
			//������ŵ����ļ������ݵļ��ϣ�һ��mapΪһ�������ļ����ݣ�key���ֶ����ƣ�value���ֶ�ֵ
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
				
				//��ȡ�ļ�������
				Map<String, Object> xmlFileRow =namedNodeToMap(fileNode);
				//�����ֶζ�Ӧ��ϵ��ȡ��������
				Map<String, Object>fileRow=XmlMapToArcMap(fileFldMap,xmlFileRow);
				
				fileRow.put("VOLSEQUENCE", flsh);
				//���̳��ֶ�
				setExtendsFld(fileRow,volRow,fileExtendsFldMap);
				//У������
				String fValidate=notNullValidate(fileRow,fileNotNullFlds,fileFldMap,"����:"+volRow.get("ID")+"-�����ļ�:"+fileRow.get("ID"));
				if(!"".equals(fValidate)){
					return xmlReturn("0",fValidate,sysid,serviceid,archid,title,serviceType,filingdate,data);
				}
				//���������ļ�
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
					return xmlReturn("0","��������xml��FILE�ڵ���EFILES",sysid,serviceid,archid,title,serviceType,filingdate,data);
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
					//��ȡ�����ļ�������
					Map<String, Object> xmlEfileRow =namedNodeToMap(efileNode);
					//�����ֶζ�Ӧ��ϵ��ȡ�����ļ�������
					Map<String, Object>efileRow=XmlMapToArcMap(efileFldMap,xmlEfileRow);	
					
					//У������
					String eValidate=notNullValidate(efileRow,efileNotNullFlds,efileFldMap,"����:"+volRow.get("ID")+"-�����ļ�:"+fileRow.get("ID")+"�����ļ�:"+efileRow.get("ID"));
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
						if("false".equals(copyEfile)){//����У��
							if(!filepath.startsWith("/uploads")){
								String remptePath="/uploads/company"+fondsid+"/fonds"+fondsid+"/"+arctypeId+filepath;
								bn=efileUploadService.testExist(remptePath);
							}
						}else{
							bn = efileSourceService.testExist(filepath);
						}
						
						
					}catch(Exception e){
						e.printStackTrace();
						logger.info("��������ļ��Ƿ���ڳ���------"+e.getMessage());
						return xmlReturn("0","��������ļ��Ƿ���ڳ���------"+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
					if(!bn){
						logger.info("FILEPATH"+"_@_"+filepath+"_@_"+"�����ļ�������");
						return xmlReturn("0","FILEPATH"+"_@_"+filepath+"_@_"+"�����ļ�������",sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
					//����ֶ�
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
					
					//���Ĭ��ֵ�ֶ�
					setDefaultFld(efileRow,efileDefFldMap);
					
					eList.add(efileRow);
				}
				fileRow.put("FILESNUM",elsh);
				fileRow.put("FILETITLE",ftitle);
				
				//���Ĭ��ֵ�ֶ�
				setDefaultFld(fileRow,fileDefFldMap);
				
				fList.add(fileRow);
			}
			if(!"false".equals(copyEfile)){
				//���ص����ļ�
				logger.info("��ʼ���ص����ļ�");
				for(int e=0;e<eList.size();e++){
					Map<String, Object> efileRow =(Map<String, Object>) eList.get(e);
					List<String> errorlist=transferefiles(efileRow,libcode,fondsid,efileSourceService,efileUploadService);
					if(errorlist.size()>0){
						return xmlReturn("0",errorlist.toString(),sysid,serviceid,archid,title,serviceType,filingdate,data);
					}
				}
				logger.info("���ص����ļ�����");
			}
			logger.info("�ļ���Ϣ:"+fList);
			logger.info("�����ļ���Ϣ:"+eList);
			logger.info( "...��������");
			//��ʼ��������
			logger.info( "��������...");
			//����Ϊ��������, mode��Ϊ��ʱ��ʹ���µ�����
			String mode = paramConfig.getParam("SyscodeCreateMode");
			if(mode!=null){// ��ԭ���������Ϲ鵵�������
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
					String filenewsys = getCreateSyscodeByMode(psys,mode,"FILE",libcode,fondsid,filingdate);//�����ɻ�ȡ ��Ӧ�ļ�����������
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
				logger.info("�����ݿⱣ�����ݳ���-----��"+e.getMessage());
				return xmlReturn("0","�����ݿⱣ�����ݳ���-----��"+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
			}
			
			return xmlReturn("1","�鵵�ɹ�",sysid,serviceid,archid,title,serviceType,filingdate,data);
		}catch(Exception e){
			e.printStackTrace();
			logger.info("�鵵�������ݳ���..."+e.getMessage());
			return xmlReturn("0","�鵵�������ݳ���..."+e.getMessage(),sysid,serviceid,archid,title,serviceType,filingdate,data);
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
					validate=validate+type+"-"+ fileFldMap.get(fld)+" ����Ϊ��;";
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
			//ͨ��item(index)������ȡbook�ڵ��ĳһ������
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
	/**���͵����ļ�
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
				logger.info("Ŀ���ļ��Ѵ���,��ִ�е����ļ����ƣ�"+targetpath);
			}
					
			boolean hastempfile = false;
			String localpath = efileSService.getLocalPath(filepath);
			if(localpath==null){//�޷�ֱ�ӻ�ȡ�����ļ�
				hastempfile = true;
				localpath = "temp/"+UUID.randomUUID().toString()+".tmp";
				boolean bn = efileSService.download(filepath, localpath);
				if(!bn){
					errorlist.add("FILEPATH"+"_@_"+filepath+"_@_"+"���ص����ļ�ʧ��");
				}
			}
			logger.info("�����ļ�filepath="+filepath);	
			logger.info("�����ļ�localpath="+localpath);
			
			efileUService.upload(targetpath, localpath);
			logger.info("�ϴ��ļ�localpath="+localpath);
			logger.info("�ϴ��ļ�targetpath="+targetpath);
			if(hastempfile){				
				new File(localpath).delete();
			}
		}catch(Exception e){
			e.printStackTrace();
			errorlist.add("FILEPATH"+"_@_"+filepath+"_@_"+"���ص����ļ��쳣"+e.getMessage());
		}
		return errorlist;
	}
	
	/**��װ�µ�FILEPATH
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
					logger.info("δ��ȡ����Ŀ��Ϣ���Զ�������Ŀ");
					String sys = UniqueKeyMaker.getPk(1);
					String sql1="INSERT INTO "+tbname+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					Object [] args1={sys,unitsys,"�ӿڰ���鵵����",procode,null,null,"admin",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),"0",null,null,fondcode,null,null,null};
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
					Object [] args3={unitsys,"��"+procode+"���ӿڰ���鵵����",catid,catid+"-1","0","0","4",arcidmap.get(unitsys+libcode),"3","1","All",projectId};
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
					logger.info("δ��ȡ����Ŀ��Ϣ���Զ�������Ŀ");
					String sys = UniqueKeyMaker.getPk(1);
					String sql1="INSERT INTO "+tbname+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					Object [] args1={sys,unitsys,"�ӿ�ɢ�ļ��鵵����",fprocode,null,null,"admin",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),"0",null,null,fondcode,null,null,null};
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
					Object [] args3={unitsys,"��"+fprocode+"���ӿ�ɢ�ļ��鵵����",catid,catid+"-1","0","0","4",arcidmap.get(unitsys+libcode),"3","1","All",projectId};
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
		logger.info("�ӿڷ���ֵ��"+ xmlReturn);
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
		logRow.put("STATUS", "1".equals(type)?"�ɹ�":"ʧ��");
		logRow.put("DATA", data);
		logRow.put("REMARK", typeWithIn);
		try{
			syncDao.insertObject2Ams(BaseDataUtil.buildList(logRow),"D_LOG");
		}catch(Exception e){
			e.printStackTrace();
			logger.info("insert into d_log error��"+ e.getMessage());
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
		logger.info("�ӿڷ���ֵ��"+ xmlReturn);
		return xmlReturn;
	}

	/**�����µ�����
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
//		�Ż���߹鵵ִ��Ч�ʣ�ҵ���ϲ������ظ��鵵��ÿ�ι鵵�������ظ��鵵������ϵͳ���������ݡ��˴��Ż��ɣ�ֱ�ӻ�ȡ��id��������id�������ݣ����������ݿ⽻��	
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
	/**�����µ�����
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
	/**���������齨�����ļ��������
	 * @param efilesource
	 * @return
	 */
	protected EFileTransferServie getEfileTransferService(String efilesource) {
		int index1 = efilesource.indexOf(":");
		if(index1<0){
			throw new RuntimeException("�����ļ���Դ���ø�ʽ����:"+efilesource);
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
				throw new RuntimeException("�����ļ���Դ���ø�ʽ����:"+efilesource);
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
//		autovaluemap.put("DEPARTMENT", "��������");
//		autovaluemap.put("DEPARTMENTID", "100000");
		autovaluemap.put("STATUS", "0");//0������⣻1���鵵��9ɾ��
		autovaluemap.put("FILLINGNUM", "1");//�鵵����
		autovaluemap.put("CREATETIME",nowTime);
		autovaluemap.put("CREATER","SYNC");
		autovaluemap.put("KNOWNSCOPE", "2");
		autovaluemap.put("EDITTIME", nowTime);
		autovaluemap.put("EDITOR", "SYNC");
		autovaluemap.put("EXT5", nowTime);
		
		
	}
	protected Map<String,String> buildLinkHashMap(List<String> sysflds){
		Map<String,String> m = new LinkedHashMap<String, String>();//ʹ��LinkedHashMap ����ʵ��ȥ�ع��ܣ��ҿ������Ч
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
