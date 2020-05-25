package com.ams.webService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import com.ams.config.PropertiesConfig;
import com.ams.dao.syncDataDao;
import com.ams.util.UniqueKeyMaker;
import com.ams.util.XmlFileUtil;
import com.ams.widget.EFileTransferServie;
import com.ams.widget.FtpEfileService;
import com.ams.widget.LocalEfileService;

public class SyncDataImp {
	//private String logTableName = "d_webSerData"; //记录同步接口传入的xml数据表，优化归档效率，取消存入日志表，可通过日志文件查看日志
	private static Logger logger = Logger.getLogger(SyncDataImp.class.getName());
	private static Map<String,String>arcidmap = new HashMap<String,String>();
	private static Map<String,String>arcnamemap = new HashMap<String,String>();
	private static Map<String,String>fcodemap = new HashMap<String,String>();
	private static Map<String,String>proidmap = new HashMap<String,String>();
	private static Map<String,String>newSysmap = new HashMap<String,String>();
	private String procode = "0"; //
	private String fprocode = "00"; //
	protected syncDataDao syncDao;
	private static PropertiesConfig paramConfig = new PropertiesConfig(SyncDataImp.class.getClassLoader().getResource("import.properties"));
	public String syncData(String dataXml, String libcode, String unitsys,String appid) {
		logger.error("appId："+appid);
		logger.error("unitsys："+unitsys);
		logger.error("libcode："+libcode);
//		logger.error("同步xml："+dataXml);
		dataXml=dataXml.replaceAll("&", "&amp;");
		logger.error("xml："+dataXml);
		
		String type = "1";
		String typeWithIn = "";
		//校验非空参数
		if( libcode ==null || "".equals(libcode)){
			type = "0";
			typeWithIn = "传入的档案门类libcode不能为空";
			return xmlErrorReturn(type,typeWithIn);
		}
		
		if( unitsys ==null || "".equals(unitsys)){
			type = "0";
			typeWithIn = "传入的全宗编号unitsys不能为空";
			return xmlErrorReturn(type,typeWithIn);
		}
		//校验获取到的xml格式的字符串，之存储到数据库。d_webSerData 存储信息有：syscode(唯一ID)，xml内容，接口调用的时间
		Document document = null;
		try {
			document = XmlFileUtil.StringToXML(dataXml);
		} catch (Exception e1) {
			type = "0";
			typeWithIn = "传入的xml字符有误,解析失败。";
			return xmlErrorReturn(type,typeWithIn);
		}	
		String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String cjrq = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		/**
		 * 优化归档效率，取消存入日志表，可通过日志文件查看日志
		 
		Object[] setValues = new Object[3];
		setValues[0] = UUID.randomUUID().toString().replaceAll("-","");
		setValues[1] = dataXml;
		setValues[2] = nowTime;
		String webSerSql = "INSERT INTO "+logTableName+"(XMLID,XMLDATA,TIME) VALUES(?,?,?)";
		try {
			int count = syncDao.excuteUpdate4Ams(webSerSql, setValues);
			if(count < 1){
				logger.error("同步xml时未成功插入");
			}
		} catch (Exception e1) {
			type = "0";
			typeWithIn = "保存数据xml出错";
			logger.error("保存数据xml出错："+e1.getMessage());
			//return xmlErrorReturn(type,typeWithIn);
		}
		*/
		//校验电子文件服务配置
		logger.info( "开始电子文件服务校验");
		EFileTransferServie efileSourceService = null, efileUploadService = null;
		String efilesource = paramConfig.getParam("EfileSource");
		String efiletarget = paramConfig.getParam("EfileTarget");
		String copyEfile = paramConfig.getParam("CopyEFile");
		try{
			efileSourceService = getEfileTransferService(efilesource);
			efileUploadService = getEfileTransferService(efiletarget);
			if(efileSourceService==null||efileUploadService==null)return xmlErrorReturn("0","电子文件传输相关配置有误");
			if(!efileSourceService.testOpen()||!efileUploadService.testOpen())return xmlErrorReturn("0","电子文件相关服务未开启");
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return xmlErrorReturn("0","电子文件传输相关配置有误");
		}
		logger.info( "电子文件服务校验结束");
		
		//创建存放案卷ID的集合
		List<String> volsyslist= new ArrayList<String>();
		//创建存放案卷文件级数量的map，key：案卷ID；value：文件级数量
		Map<String,String> vMap = new HashMap<String,String>();
		//创建存放案卷级错误信息的map，key：案卷ID；value：案卷级错误信息集合
		Map<String,List<String>> verrorMap = new HashMap<String,List<String>>();
		//创建存放案卷内文件级错误信息的map，key：案卷ID；value：map（文件级错误信息的map，key：文件级ID；value：文件错误信息集合）
		Map<String,Map<String,List<String>>> vferrorMap = new HashMap<String,Map<String,List<String>>>();
		
		//创建系统重要字段设置
		Map<String,String> autovaluemap = new HashMap<String,String>();
		setAutovaluemap(autovaluemap,nowTime);
		
		//解析xml数据
		Element root=document.getDocumentElement();
		NodeList nodeList = root.getChildNodes();	
		for(int j=0;j<nodeList.getLength();j++){
			Element volNode = null;
			if(nodeList.item(j) instanceof Element){
				volNode = (Element) nodeList.item(j);
			}else{
				continue;
			}
			//创建存放案卷级错误信息的集合
			List<String> verrorList = new ArrayList<String>();
			//获取案卷级数据
			NamedNodeMap volMap = volNode.getAttributes();
			Map<String, Object> volRow =namedNodeMapToMap(volMap);
			
			//校验数据
			String volSys = volNode.getAttribute("ID");
			volsyslist.add(volSys);
			if(volSys==null || "".equals(volSys)){
				logger.error("ID_案卷的主键不能为空");
				verrorList.add("ID"+"_@_"+volSys+"_@_"+"案卷的主键不能为空");
				verrorMap.put(volSys, verrorList);
				continue;
			}
			try{
				autovaluemap.put("FONDSID", unitsys);
				String fondcode=getFondcode(unitsys);
				if(fondcode==null || "".equals(fondcode)){
					logger.error("fondcode_未获取到");
					verrorList.add("unitsys"+"_@_"+unitsys+"_@_"+"未获取到对应fondcode");
					verrorMap.put(volSys, verrorList);
					continue;
				}
				autovaluemap.put("FONDSCODE", fondcode);
				String arctypeId=getArcTypeId(libcode,unitsys);
				if(arctypeId==null || "".equals(arctypeId)){
					logger.error("arctypeId未获取到");
					verrorList.add("arctypeId"+"_@_libcode="+libcode+"-unitsys="+unitsys+"_@_"+"未获取到档案类型id");
					verrorMap.put(volSys, verrorList);
					continue;
				}
				String projectid=getProjectId(libcode,unitsys,fondcode);
				if(projectid==null || "".equals(projectid)){
					logger.error("projectid未获取到");
					verrorList.add("projectid"+"_@_"+procode+"_@_"+"未获取到项目id");
					verrorMap.put(volSys, verrorList);
					continue;
				}
				autovaluemap.put("PROJECTID", projectid);
				autovaluemap.put("PROARCHCODE", procode);
				volRow.putAll(autovaluemap);
				volRow.remove("YXMID");
				logger.info("DZGDRQ---》"+"EXT4");
				String dzgdrq=volRow.get("DZGDRQ")==null?"":volRow.get("DZGDRQ").toString();
				volRow.put("EXT4", dzgdrq);//电子归档日期字段修改名
				volRow.remove("DZGDRQ");
				volRow.put("DID", volSys);//记录理正id字段
				logger.info("案卷信息："+volRow);
				
				//解析文件级数据
				NodeList fileNodes =  volNode.getChildNodes();
				//创建存放文件级错误的map，key文件级ID；value：错误信息
				Map<String,List<String>> ferrormap= new HashMap<String,List<String>>();
				//创建存放文件级数据的集合，一个map为一条文件级数据，key：字段名称；value：字段值
				List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
				//创建存放电子文件级数据的集合，一个map为一条电子文件数据，key：字段名称；value：字段值
				List<Map<String, Object>> efileList = new ArrayList<Map<String, Object>>();
				int flsh=0;
				for(int i=0;i<fileNodes.getLength();i++){
					Element fileNode =null;
					if(fileNodes.item(i) instanceof Element){
						fileNode = (Element) fileNodes.item(i);
						flsh++;
					}else{
						continue;
					}
					//创建存放文件级错误信息的集合
					List<String> errorList = new ArrayList<String>();
					//获取文件级数据
					NamedNodeMap fileMap = fileNode.getAttributes();
					Map<String, Object> fileRow =namedNodeMapToMap(fileMap);
					String fileSys = fileNode.getAttribute("ID");
					if(fileSys==null || "".equals(fileSys)){
						logger.error("ID_文件的主键不能为空");
						errorList.add("ID"+"_@_"+fileSys+"_@_"+"文件的主键不能为空");
						ferrormap.put(fileSys, errorList);
						continue;
					}
					//填充重要字段
					fileRow.putAll(autovaluemap);
					fileRow.put("VOLID", volSys);
					fileRow.put("HASATTACH", "0");
					fileRow.put("VOLSEQUENCE", flsh);
					fileRow.put("CJRQ", cjrq);
					fileRow.put("DID", fileSys);
					fileRow.put("PID", volSys);
					fileRow.put("YXMMC", volRow.get("YXMMC"));
					fileRow.put("YXMBH", volRow.get("YXMBH"));
					fileRow.put("SECURITY", volRow.get("SECURITY"));
					fileRow.put("RETENTIONPERIOD", volRow.get("RETENTIONPERIOD"));
					
					//解析电子文件
					NodeList efileNodes =  fileNode.getChildNodes();
					int elsh=0;
					String ftitle="";
					for(int l=0;l<efileNodes.getLength();l++){
						Element efileNode =null;
						if(efileNodes.item(l) instanceof Element){
							efileNode = (Element) efileNodes.item(l);
							elsh++;
						}else{
							continue;
						}
						//获取电子文件数据
						NamedNodeMap efileMap = efileNode.getAttributes();
						Map<String, Object> efileRow =namedNodeMapToMap(efileMap);
						if(efileRow.get("FILENAME")==null || "".equals(efileRow.get("FILENAME"))){
							errorList.add("FILENAME"+"_@_"+efileRow.get("FILENAME")+"_@_"+"电子文件名称不能为空");
						}
						if(efileRow.get("FILEPATH")==null || "".equals(efileRow.get("FILEPATH"))){
							errorList.add("FILEPATH"+"_@_"+efileRow.get("FILEPATH")+"_@_"+"电子文件路劲不能为空");
						}
						
						String title=(String) efileRow.get("FILENAME");
						if("".equals(ftitle)){
							ftitle=title;
						}
						
						String filepath=(String) efileRow.get("FILEPATH");
						filepath = filepath.replaceAll("\\\\", "/");
						boolean bn = false;
						try{
							if("false".equals(copyEfile)){//本地校验
								if(!filepath.startsWith("/uploads")){
									String remptePath="/uploads/company1/fonds1/1503560297506601211"+filepath;
									bn=efileUploadService.testExist(remptePath);
								}
							}else{
								bn = efileSourceService.testExist(filepath);
							}
							
							
						}catch(Exception e){
							e.printStackTrace();
							logger.info("检验电子文件是否存在出错------"+e.getMessage());
							errorList.add("FILEPATH"+"_@_"+filepath+"_@_"+"校验电子文件是否存在出错"+e.getMessage());
						}
						if(!bn){
							errorList.add("FILEPATH"+"_@_"+filepath+"_@_"+"电子文件不存在");
						}
						if(errorList.size()==0){
							//填充字段
							efileRow.put("FONDSID", unitsys);
							efileRow.put("TABLEID", "f"+unitsys+"_"+libcode+"_"+"document");
							efileRow.put("ARCHID", fileSys);
							efileRow.put("HANGINGTIME", nowTime);
							efileRow.put("EXTENSION", filepath.substring(filepath.lastIndexOf(".")+1));
							efileRow.put("ARCHTYPEID", arctypeId);
							efileRow.put("TITLE", title);
							efileRow.put("DID", efileRow.get("ID"));
							efileRow.put("PID", fileSys);
							efileRow.remove("FILENAME");
							if("false".equals(copyEfile)){
								if(!filepath.startsWith("/uploads")){
									filepath="/uploads/company1/fonds1/1503560297506601211"+filepath;
								}

								efileRow.put("FILEPATH", filepath.substring(0,filepath.lastIndexOf("/")+1));
								efileRow.put("SAVEFILENAME",filepath.substring(filepath.lastIndexOf("/")+1));
							}
							
						}
						
						efileList.add(efileRow);
					}
					fileRow.put("FILESNUM",elsh);
					fileRow.put("FILETITLE",ftitle);
					
					if(errorList.size()>0){
						ferrormap.put(fileSys, errorList);
					}
					fileList.add(fileRow);
				}
				vMap.put(volSys, String.valueOf(flsh));
				//保存数据
				if(ferrormap.size()==0&&verrorList.size()==0){
					if(!"false".equals(copyEfile)){
						//下载电子文件
						logger.info("开始下载电子文件");
						for(int e=0;e<efileList.size();e++){
							Map<String, Object> efileRow =(Map<String, Object>) efileList.get(e);
							List<String> errorlist=transferefiles(efileRow,libcode,unitsys,efileSourceService,efileUploadService);
							if(errorlist.size()>0){
								ferrormap.put((String)efileRow.get("ARCHID"), errorlist);
							}
						}
						logger.info("下载电子文件结束");
					}
					
					
					//开始保存数据
					//开始保存数据
					if(ferrormap.size()==0){
						//以下为主键策略, mode不为空时，使用新的主键
						String mode = paramConfig.getParam("SyscodeCreateMode");
						if(mode!=null){// 在原主键不符合归档的情况下
							String newvolsys =  getCreateSyscodeByMode(volSys,mode,"VOL",libcode,unitsys,nowTime);
							volRow.put("ID", newvolsys);
							for(Map <String,Object> m :fileList){
								m.put("VOLID", newvolsys);
								String filesys = (String)m.get("ID");
								String newfilesys =  getCreateSyscodeByMode(filesys,mode,"FILE",libcode,unitsys,nowTime);
								m.put("ID", newfilesys);
								if(m.get("RELATEDFILEID")!=null&&!"".equals(m.get("RELATEDFILEID"))){
									String glid=m.get("RELATEDFILEID").toString();
									String [] glarr=glid.split(",");
									String newglid="";
									for(int s=0;s<glarr.length;s++){
										if(!"".equals(glarr[s])){
											String newglid1 =  getCreateSyscodeByMode(glarr[s],mode,"FILE",libcode,unitsys,nowTime);
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
							for(Map <String,Object> m :efileList){
								String psys = (String)m.get("ARCHID");
								String filenewsys =  getCreateSyscodeByMode(psys,mode,"FILE",libcode,unitsys,nowTime);//此语句可获取 对应文件级的新主键
								m.put("ARCHID", filenewsys);
								
								String efilesys = (String)m.get("ID");
								String newefilesys =  getCreateSyscodeByMode(efilesys,mode,"EFILE",libcode,unitsys,nowTime);
								m.put("ID", newefilesys);
							}
						}
						logger.debug( "开始向数据库保存数据");
						try{
							syncDao.importVolData(volRow,fileList,efileList,libcode,unitsys,paramConfig);
						}catch(Exception e){
							e.printStackTrace();
							logger.info("向数据库保存数据出错-----："+e.getMessage());
					//		syncDao.deleteVolData(volRow,fileList,efileList,libcode,unitsys,paramConfig);
							verrorList.add("ID"+"_@_"+volSys+"_@_"+"向数据库保存数据出错"+e.getMessage());
							verrorMap.put(volSys, verrorList);
						}
						
						logger.debug( "向数据库保存数据结束");
					}
					
				}
				if(verrorList.size()>0){
					verrorMap.put(volSys, verrorList);
				}
				if(ferrormap.size()>0){
					vferrorMap.put(volSys, ferrormap);
				}
			}catch(Exception e){
				e.printStackTrace();
				verrorList.add("ID"+"_@_"+volSys+"_@_"+"保存失败异常信息"+e.getMessage());
				verrorMap.put(volSys, verrorList);
			}
			
		}
		//封装归档成败返回值
		String xmlReturn = "";
		
		Document xmldoc = XmlFileUtil.createNewDocumen();
		Element xmlRoot = xmldoc.createElement("DOC");
		Element xmlInfo = xmldoc.createElement("INFO");
		xmlRoot.appendChild(xmlInfo);
		boolean cg=false;
		if(verrorMap.size()>0||vferrorMap.size()>0){
			xmlInfo.setAttribute("TYPE", "0");
		}else{
			xmlInfo.setAttribute("TYPE", "1");
			cg=true;
		}
		
		xmlInfo.setAttribute("MESSAGE", "本次XML文件中解析出"+volsyslist.size()+"条案卷(VOL)数据,失败："+(verrorMap.size()+vferrorMap.size())+";成功："+(volsyslist.size()-verrorMap.size()-vferrorMap.size()));
		int count=0;
		if(volsyslist.size()>0){
			Element xmlFiles = xmldoc.createElement("FILES");
			xmlRoot.appendChild(xmlFiles);
			for(int f=0;f<volsyslist.size();f++){
				Element volFile = xmldoc.createElement("VOL");
				xmlFiles.appendChild(volFile);
				String vSys=volsyslist.get(f);
				String fcount=vMap.get(vSys);
				int fc=0;
				try{
					fc=Integer.parseInt(fcount);
				}catch(Exception e){
				}
				count=count+fc;
				List<String> vlist=verrorMap.get(vSys);
				Map<String,List<String>> fmap=vferrorMap.get(vSys);
				if(vlist==null&&fmap==null){
					volFile.setAttribute("ID", vSys);
					volFile.setAttribute("STATUS", "1");
				}else{
					volFile.setAttribute("ID", vSys);
					volFile.setAttribute("STATUS", "0");
					if(vlist!=null&&vlist.size()>0){
						Element volErrorMsg = xmldoc.createElement("ERRORMSG");
						volFile.appendChild(volErrorMsg);
						for(int v=0;v<vlist.size();v++){
							String value=(String) vlist.get(v);
							String [] valuearr =value.split("_@_");
							Element COLUMN = xmldoc.createElement("COLUMN");
							volErrorMsg.appendChild(COLUMN);
							COLUMN.setAttribute("NAME", valuearr[0]);
							COLUMN.setAttribute("VALUE", valuearr[1]);
							COLUMN.setAttribute("MESSAGE", valuearr[2]);
						}
					}
					if(fmap!=null&&fmap.size()>0){
						for (String key : fmap.keySet()) {
							Element File = xmldoc.createElement("FILE");
							File.setAttribute("ID", key);
							File.setAttribute("STATUS", "0");
							volFile.appendChild(File);
							Element fileErrorMsg = xmldoc.createElement("ERRORMSG");
							File.appendChild(fileErrorMsg);
							List<String> flist=fmap.get(key);
							for(int fr=0;fr<flist.size();fr++){
								String value=(String) flist.get(fr);
								String [] valuearr =value.split("_@_");
								Element COLUMN = xmldoc.createElement("COLUMN");
								fileErrorMsg.appendChild(COLUMN);
								COLUMN.setAttribute("NAME", valuearr[0]);
								COLUMN.setAttribute("VALUE", valuearr[1]);
								COLUMN.setAttribute("MESSAGE", valuearr[2]);
							}
						}
					}
				}
				
			}
		}
		if(cg){
//			xmlInfo.setTextContent( "本次XML文件中解析出"+volsyslist.size()+"条案卷数据,"+count+"条文件数据,成功归档");
		}
		xmldoc.appendChild(xmlRoot);
		xmldoc.setXmlStandalone(true);
	
		xmlReturn = XmlFileUtil.XMLtoString(xmldoc,"UTF-8");
		/**
		 * 优化归档效率，取消存入日志表，可通过日志文件查看日志
		 
		try{
			//报存导入日志
			String importLog = "UPDATE "+logTableName+" SET LOG=? WHERE XMLID=?";
			syncDao.excuteUpdate4Ams(importLog, new Object[]{xmlReturn,setValues[0]});
		}catch(Exception e){
			logger.error(e.getMessage());
		}
		*/
		logger.info("接口返回值："+ xmlReturn);
		return xmlReturn;	
				
	}
	public String syncDataFile(String dataXml, String libcode, String unitsys,String appid) {
		logger.error("appId："+appid);
		logger.error("unitsys："+unitsys);
		logger.error("libcode："+libcode);
//		logger.error("同步xml："+dataXml);
		dataXml=dataXml.replaceAll("&", "&amp;");
		logger.error("xml："+dataXml);
		
		String type = "1";
		String typeWithIn = "";
		//校验非空参数
		if( libcode ==null || "".equals(libcode)){
			type = "0";
			typeWithIn = "传入的档案门类libcode不能为空";
			return xmlErrorReturn(type,typeWithIn);
		}
		
		if( unitsys ==null || "".equals(unitsys)){
			type = "0";
			typeWithIn = "传入的全宗编号unitsys不能为空";
			return xmlErrorReturn(type,typeWithIn);
		}
		//校验获取到的xml格式的字符串，之存储到数据库。d_webSerData 存储信息有：syscode(唯一ID)，xml内容，接口调用的时间
		Document document = null;
		try {
			document = XmlFileUtil.StringToXML(dataXml);
		} catch (Exception e1) {
			type = "0";
			typeWithIn = "传入的xml字符有误,解析失败。";
			return xmlErrorReturn(type,typeWithIn);
		}	
		String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String cjrq = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		/**
		 * 优化归档效率，取消存入日志表，可通过日志文件查看日志
		 * 
		Object[] setValues = new Object[3];
		setValues[0] = UUID.randomUUID().toString().replaceAll("-","");
		setValues[1] = dataXml;
		setValues[2] = nowTime;
		String webSerSql = "INSERT INTO "+logTableName+"(XMLID,XMLDATA,TIME) VALUES(?,?,?)";
				
		try {
			
			int count = syncDao.excuteUpdate4Ams(webSerSql, setValues);
			if(count < 1){
				logger.error("同步xml时未成功插入");
			}
		} catch (Exception e1) {
			type = "0";
			typeWithIn = "保存数据xml出错";
			logger.error("保存数据xml出错："+e1.getMessage());
			//return xmlErrorReturn(type,typeWithIn);
		}
		*/
		//校验电子文件服务配置
		logger.info( "开始电子文件服务校验");
		EFileTransferServie efileSourceService = null, efileUploadService = null;
		String efilesource = paramConfig.getParam("EfileSource");
		String efiletarget = paramConfig.getParam("EfileTarget");
		String copyEfile = paramConfig.getParam("CopyEFile");
		try{
			efileSourceService = getEfileTransferService(efilesource);
			efileUploadService = getEfileTransferService(efiletarget);
			if(efileSourceService==null||efileUploadService==null)return xmlErrorReturn("0","电子文件传输相关配置有误");
			if(!efileSourceService.testOpen()||!efileUploadService.testOpen())return xmlErrorReturn("0","电子文件相关服务未开启");
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			return xmlErrorReturn("0","电子文件传输相关配置有误");
		}
		logger.info( "电子文件服务校验结束");
		
		
		
		//创建系统重要字段设置
		Map<String,String> autovaluemap = new HashMap<String,String>();
		setAutovaluemap(autovaluemap,nowTime);
		//创建存放文件ID的集合
		List<String> filesyslist= new ArrayList<String>();
		//创建存放文件级错误的map，key文件级ID；value：错误信息
		Map<String,List<String>> ferrormap= new HashMap<String,List<String>>();
		
		//解析xml数据
		Element root=document.getDocumentElement();
		//解析文件级数据
		NodeList fileNodes =  root.getChildNodes();	
		
		for(int i=0;i<fileNodes.getLength();i++){
			Element fileNode =null;
			if(fileNodes.item(i) instanceof Element){
				fileNode = (Element) fileNodes.item(i);
			}else{
				continue;
			}
			//创建存放文件级错误信息的集合
			List<String> errorList = new ArrayList<String>();
			//创建存放电子文件级数据的集合，一个map为一条电子文件数据，key：字段名称；value：字段值
			List<Map<String, Object>> efileList = new ArrayList<Map<String, Object>>();
			//获取文件级数据
			NamedNodeMap fileMap = fileNode.getAttributes();
			Map<String, Object> fileRow =namedNodeMapToMap(fileMap);
			String fileSys = fileNode.getAttribute("ID");
			//校验数据
			filesyslist.add(fileSys);
			if(fileSys==null || "".equals(fileSys)){
				logger.error("ID_文件的主键不能为空");
				errorList.add("ID"+"_@_"+fileSys+"_@_"+"文件的主键不能为空");
				ferrormap.put(fileSys, errorList);
				continue;
			}
				
			try{
				autovaluemap.put("FONDSID", unitsys);
				String fondcode=getFondcode(unitsys);
				if(fondcode==null || "".equals(fondcode)){
					logger.error("fondcode_未获取到");
					errorList.add("unitsys"+"_@_"+unitsys+"_@_"+"未获取到对应fondcode");
					ferrormap.put(fileSys, errorList);
					continue;
				}
				autovaluemap.put("FONDSCODE", fondcode);
				String arctypeId=getArcTypeId(libcode,unitsys);
				if(arctypeId==null || "".equals(arctypeId)){
					logger.error("arctypeId未获取到");
					errorList.add("arctypeId"+"_@_libcode="+libcode+"-unitsys="+unitsys+"_@_"+"未获取到档案类型id");
					ferrormap.put(fileSys, errorList);
					continue;
				}
				String projectid=getFileProjectId(libcode,unitsys,fondcode);
				if(projectid==null || "".equals(projectid)){
					logger.error("projectid未获取到");
					errorList.add("projectid"+"_@_"+fprocode+"_@_"+"未获取到项目id");
					ferrormap.put(fileSys, errorList);
					continue;
				}
				autovaluemap.put("PROJECTID", projectid);
				autovaluemap.put("PROARCHCODE", fprocode);
				//填充重要字段
				fileRow.putAll(autovaluemap);
				fileRow.put("HASATTACH", "0");
				fileRow.put("VOLID", "0");
				fileRow.put("CJRQ", cjrq);
				fileRow.put("DID", fileSys);
				fileRow.remove("YXMID");
				logger.info("文件信息："+fileRow);
				//解析电子文件
				NodeList efileNodes =  fileNode.getChildNodes();
				int elsh=0;
				String ftitle="";
				for(int l=0;l<efileNodes.getLength();l++){
					Element efileNode =null;
					if(efileNodes.item(l) instanceof Element){
						efileNode = (Element) efileNodes.item(l);
						elsh++;
					}else{
						continue;
					}
					//获取电子文件数据
					NamedNodeMap efileMap = efileNode.getAttributes();
					Map<String, Object> efileRow =namedNodeMapToMap(efileMap);
					if(efileRow.get("FILENAME")==null || "".equals(efileRow.get("FILENAME"))){
						errorList.add("FILENAME"+"_@_"+efileRow.get("FILENAME")+"_@_"+"电子文件名称不能为空");
					}
					if(efileRow.get("FILEPATH")==null || "".equals(efileRow.get("FILEPATH"))){
						errorList.add("FILEPATH"+"_@_"+efileRow.get("FILEPATH")+"_@_"+"电子文件路劲不能为空");
					}
					
					String title=(String) efileRow.get("FILENAME");
					if("".equals(ftitle)){
						ftitle=title;
					}
					
					String filepath=(String) efileRow.get("FILEPATH");
					filepath = filepath.replaceAll("\\\\", "/");
					boolean bn = false;
					try{
						if("false".equals(copyEfile)){//本地校验
							if(!filepath.startsWith("/uploads")){
								String remptePath="/uploads/company1/fonds1/1503560297506601211"+filepath;
								bn=efileUploadService.testExist(remptePath);
							}
						}else{
							bn = efileSourceService.testExist(filepath);
						}
						
					}catch(Exception e){
						e.printStackTrace();
						logger.info("检验电子文件是否存在出错------"+e.getMessage());
						errorList.add("FILEPATH"+"_@_"+filepath+"_@_"+"校验电子文件是否存在出错"+e.getMessage());
					}
					if(!bn){
						errorList.add("FILEPATH"+"_@_"+filepath+"_@_"+"电子文件不存在");
					}
					if(errorList.size()==0){
						//填充字段
						efileRow.put("FONDSID", unitsys);
						efileRow.put("TABLEID", "f"+unitsys+"_"+libcode+"_"+"document");
						efileRow.put("ARCHID", fileSys);
						efileRow.put("HANGINGTIME", nowTime);
						efileRow.put("EXTENSION", filepath.substring(filepath.lastIndexOf(".")+1));
						efileRow.put("ARCHTYPEID", arctypeId);
						efileRow.put("TITLE", title);
						efileRow.remove("FILENAME");
						efileRow.put("DID", efileRow.get("ID"));
						efileRow.put("PID", fileSys);
						if("false".equals(copyEfile)){
							if(!filepath.startsWith("/uploads")){
								filepath="/uploads/company1/fonds1/1503560297506601211"+filepath;
							}
							efileRow.put("FILEPATH", filepath.substring(0,filepath.lastIndexOf("/")+1));
							efileRow.put("SAVEFILENAME",filepath.substring(filepath.lastIndexOf("/")+1));
						}
					}
					
					efileList.add(efileRow);
				}
				fileRow.put("FILESNUM",elsh);
				fileRow.put("FILETITLE",ftitle);
				
				//保存数据
				if(errorList.size()==0){
					if(!"false".equals(copyEfile)){//不转移电子文件直接使用
						//下载电子文件
						logger.info("开始下载电子文件");
						for(int e=0;e<efileList.size();e++){
							Map<String, Object> efileRow =(Map<String, Object>) efileList.get(e);
							errorList=transferefiles(efileRow,libcode,unitsys,efileSourceService,efileUploadService);
						}
						logger.info("下载电子文件结束");
					}
					//开始保存数据
					if(errorList.size()==0){
						//以下为主键策略, mode不为空时，使用新的主键
						String mode = paramConfig.getParam("SyscodeCreateMode");
						if(mode!=null){// 在原主键不符合归档的情况下
							String newfilesys =  getCreateSyscodeByMode(fileSys,mode,"File",libcode,unitsys,nowTime);
								fileRow.put("ID",newfilesys);
								if(fileRow.get("RELATEDFILEID")!=null&&!"".equals(fileRow.get("RELATEDFILEID"))){
									String glid=fileRow.get("RELATEDFILEID").toString();
									String [] glarr=glid.split(",");
									String newglid="";
									for(int s=0;s<glarr.length;s++){
										if(!"".equals(glarr[s])){
											String newglid1 =  getCreateSyscodeByMode(glarr[s],mode,"FILE",libcode,unitsys,nowTime);
											if("".equals(newglid)){
												newglid=newglid1;
											}else{
												newglid=newglid+","+newglid1;
											}
										}
										
									}
									fileRow.put("RELATEDFILEID", newglid);
								}
							for(Map <String,Object> m :efileList){
								m.put("ARCHID", newfilesys);
								
								String efilesys = (String)m.get("ID");
								String newefilesys =  getCreateSyscodeByMode(efilesys,mode,"EFILE",libcode,unitsys,nowTime);
								m.put("ID", newefilesys);
							}
						}
						logger.debug( "开始向数据库保存数据");
						try{
							syncDao.importFileData(fileRow,efileList,libcode,unitsys,paramConfig);
						}catch(Exception e){
							e.printStackTrace();
							logger.info("向数据库保存数据出错-----："+e.getMessage());
					//		syncDao.deleteFileData(fileRow,efileList,libcode,unitsys,paramConfig);
							errorList.add("ID"+"_@_"+fileSys+"_@_"+"向数据库保存数据出错"+e.getMessage());
						}
						
						logger.debug( "向数据库保存数据结束");
					}
					
				}
				if(errorList.size()>0){
					ferrormap.put(fileSys, errorList);
				}
			}catch(Exception e){
				e.printStackTrace();
				errorList.add("ID"+"_@_"+fileSys+"_@_"+"保存失败异常信息"+e.getMessage());
				ferrormap.put(fileSys, errorList);
			}
		}	
		//封装归档成败返回值
		String xmlReturn = "";
		
		Document xmldoc = XmlFileUtil.createNewDocumen();
		Element xmlRoot = xmldoc.createElement("DOC");
		Element xmlInfo = xmldoc.createElement("INFO");
		xmlRoot.appendChild(xmlInfo);
		if(ferrormap.size()>0){
			xmlInfo.setAttribute("TYPE", "0");
		}else{
			xmlInfo.setAttribute("TYPE", "1");
		}
		
		xmlInfo.setAttribute("MESSAGE", "本次XML文件中解析出"+filesyslist.size()+"条文件数据,失败："+(ferrormap.size())+";成功："+(filesyslist.size()-ferrormap.size()));
		if(filesyslist.size()>0){
			Element xmlFiles = xmldoc.createElement("FILES");
			xmlRoot.appendChild(xmlFiles);
			for(int f=0;f<filesyslist.size();f++){
				Element fileFile = xmldoc.createElement("FILE");
				xmlFiles.appendChild(fileFile);
				String fSys=filesyslist.get(f);
				List<String> flist=ferrormap.get(fSys);
				if(flist==null){
					fileFile.setAttribute("ID", fSys);
					fileFile.setAttribute("STATUS", "1");
				}else{
					fileFile.setAttribute("ID", fSys);
					fileFile.setAttribute("STATUS", "0");
					if(flist!=null&&flist.size()>0){
						Element volErrorMsg = xmldoc.createElement("ERRORMSG");
						fileFile.appendChild(volErrorMsg);
						for(int v=0;v<flist.size();v++){
							String value=(String) flist.get(v);
							String [] valuearr =value.split("_@_");
							Element COLUMN = xmldoc.createElement("COLUMN");
							volErrorMsg.appendChild(COLUMN);
							COLUMN.setAttribute("NAME", valuearr[0]);
							COLUMN.setAttribute("VALUE", valuearr[1]);
							COLUMN.setAttribute("MESSAGE", valuearr[2]);
						}
					}
				}
				
			}
		}
		xmldoc.appendChild(xmlRoot);
		xmldoc.setXmlStandalone(true);
	
		xmlReturn = XmlFileUtil.XMLtoString(xmldoc,"UTF-8");
		/**
		 * 优化归档效率，取消存入日志表，可通过日志文件查看日志
		 * 
		try{
			//报存导入日志
			String importLog = "UPDATE "+logTableName+" SET LOG=? WHERE XMLID=?";
			syncDao.excuteUpdate4Ams(importLog, new Object[]{xmlReturn,setValues[0]});
		}catch(Exception e){
			logger.error(e.getMessage());
		}
		*/
		logger.info("接口返回值："+ xmlReturn);
		return xmlReturn;
		
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

	private String xmlErrorReturn(String type, String typeWithIn){
		Document xmldoc = XmlFileUtil.createNewDocumen();
		Element xmlRoot = xmldoc.createElement("DOC");
		Element xmlInfo = xmldoc.createElement("INFO");
		xmlInfo.setAttribute("TYPE", type);
		xmlInfo.appendChild(xmldoc.createTextNode(typeWithIn));
		xmlRoot.appendChild(xmlInfo);
		xmldoc.appendChild(xmlRoot);
		String xmlReturn = "";
		xmldoc.setXmlStandalone(true);
		xmlReturn = XmlFileUtil.XMLtoString(xmldoc,"UTF-8");
		logger.info("接口返回值："+ xmlReturn);
		return xmlReturn;
	}
	private Map<String, Object> namedNodeMapToMap(NamedNodeMap Map) {
		Map<String, Object> Row =new HashMap<String, Object>();
		for (int j = 0; j < Map.getLength(); j++) {
			//通过item(index)方法获取book节点的某一个属性
			Node attr = Map.item(j);
			Row.put(attr.getNodeName(),attr.getNodeValue());
		}
		return Row;
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
