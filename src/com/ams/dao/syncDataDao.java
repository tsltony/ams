package com.ams.dao;



import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ams.config.ParamConfigInter;
import com.ams.config.PropertiesConfig;
import com.ams.util.BaseDataUtil;


@SuppressWarnings({"rawtypes","unchecked"})
public class syncDataDao {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private JdbcTemplate amsJdbcTemplate;
	
	public JdbcTemplate getAmsJdbcTemplate() {
		return amsJdbcTemplate;
	}

	public void setAmsJdbcTemplate(JdbcTemplate amsJdbcTemplate) {
		this.amsJdbcTemplate = amsJdbcTemplate;
	}

	private String impDBType;
	
	public void init() throws SQLException{
		impDBType =  amsJdbcTemplate.getDataSource().getConnection().getMetaData().getDriverName();
	}
	
	/**导入案卷级数据
	 * @param row
	 * @param filelist
	 * @param efilelist
	 * @param libcode
	 * @param unitsys
	 * @param paramConfig
	 */
	public void importVolData(Map<String, Object> row,List<Map<String, Object>> filelist,List<Map<String, Object>> efilelist, String libcode,
					String unitsys, ParamConfigInter paramConfig) {
		String syscode = (String)row.get("DID");
//		boolean exit = testexit(syscode,libcode,"VOL",unitsys); 
//		if(exit){
//			logger.info("执行重复归档流程："+syscode);
//			Object[] args= new Object[]{syscode};
//			//String findql = "select ARCHCODE,STATUS,DID,ID from "+tableName(libcode,unitsys,"VOL")+" where DID=? and (ARCHCODE is null or ARCHCODE='' or ARCHCODE='null' or ARCHCODE='NULL')";
//			String findql = "select ARCHCODE,STATUS,DID,ID from "+tableName(libcode,unitsys,"VOL")+" where DID=? and status='0' ";//按整编库判断
//			List<Map<String, Object>> result =  queryAmsData(findql, args);
//			Map volmap = result.get(0);
//			String id=volmap.get("ID").toString();
//			//物理删除案卷、文件、附件
//			String delefile="delete from "+tableName(libcode,unitsys,"EFILE")+" where archid in (select id from "+tableName(libcode,unitsys,"FILE")+" where VOLID= ?)";
//			String delglgx = "delete from d_glgx_gd where wjid in (select id from "+tableName(libcode,unitsys,"FILE")+"  where VOLID= ?) ";
//			String delfile = "delete from "+tableName(libcode,unitsys,"FILE")+" where VOLID= ?";
//			String delvol = "delete from "+tableName(libcode,unitsys,"VOL")+" where ID= ?";
//			Object [] args1={id};
//			excuteUpdate4Ams(delefile,args1);
//			excuteUpdate4Ams(delglgx,args1);
//			excuteUpdate4Ams(delfile,args1);
//			excuteUpdate4Ams(delvol,args1);
//		}
//		String memo=testexitForMemo(syscode,libcode,"VOL",unitsys);
//		if(!"".equals(memo)){
//			row.put("MEMO", memo);
//		}
		//导入案卷级数据
		String voltablename = tableName(libcode,unitsys,"VOL");
		insertObject2Ams(BaseDataUtil.buildList(row),voltablename);
		//导入文件级数据			
		String filetablename = tableName(libcode,unitsys,"FILE");
		insertObject2Ams(filelist,filetablename);
		//导入电子文件
		String efiletablename = tableName(libcode,unitsys,"EFILE");
		insertObject2Ams(efilelist,efiletablename);
	}
	

	/**向档案系统中插入数据
	 * @param datalist
	 * @param tablename
	 */
	public void insertObject2Ams(List<Map<String, Object>> datalist, String tablename) {
		for(Map<String, Object> data:datalist){
			//添加关联文件
			try{
				if(data.get("RELATEDFILEID")!=null&&!"".equals(data.get("RELATEDFILEID"))){
					String [] tb=tablename.split("_");
					String glid=data.get("RELATEDFILEID").toString();
					String [] glarr=glid.split(",");
					String sql="INSERT INTO D_GLGX_GD VALUES(?,?,?,?,?,?)";
					String sql1="SELECT COUNT(*) FROM D_GLGX_GD WHERE WJID=? AND GLID=?";
					String [] glgxarrsql=new String[glarr.length];
//优化执行效率					
					for(int s=0;s<glarr.length;s++){
//						Object [] args1={data.get("ID"),glarr[s]};
//						if(this.amsJdbcTemplate.queryForInt(sql1, args1)==0){
//							Object [] args={UUID.randomUUID().toString().replaceAll("-", ""),data.get("ID"),
//									glarr[s],data.get("VOLID"),data.get("FONDSID"),tb[1]};
//							excuteUpdate4Ams(sql, args);
						if(!"".equals(glarr[s])){
							String sql2="INSERT INTO D_GLGX_GD (SYSCODE,WJID,GLID,AJID,UNITSYS,LIBCODE) VALUES('"+UUID.randomUUID().toString().replaceAll("-", "")+
									"','"+data.get("ID")+"','"+glarr[s]+"','"+data.get("VOLID")+"','"+data.get("FONDSID")+"','"+tb[1]+"')";
							glgxarrsql[s]=sql2;
						}
							
//						}
					}
					if(glgxarrsql.length>0){
						excuteBatchUpdate4Ams(glgxarrsql);
					}
//					for(int s=0;s<glarr.length;s++){
//						Object [] args1={glarr[s],data.get("ID")};
//						if(this.amsJdbcTemplate.queryForInt(sql1, args1)==0){
//							Object [] args={UUID.randomUUID().toString().replaceAll("-", ""),
//									glarr[s],data.get("ID"),data.get("VOLID"),data.get("FONDSID"),tb[1]};
//							excuteUpdate4Ams(sql, args);
//						}
//					}
					data.remove("RELATEDFILEID");
				}
			}catch(Exception e){
				e.printStackTrace();
				data.remove("RELATEDFILEID");
			}
			
			data.remove("RELATEDFILEID");
			List<Object> argslist = new ArrayList<Object>();
			StringBuffer sb = new StringBuffer("insert into ");
			sb.append(tablename+" (");
			for(String key : data.keySet()){
				sb.append(key.toUpperCase()+",");
				Object value = data.get(key);
				if(value!=null){
					String valuestr = BaseDataUtil.trimString(value);
					value = valuestr;
				}
				if("".equals(value)){//mysql 5.x 发现int类型字段空值不能插入，要转为null
					value = null;
				}
				if("SYSDATE".equals(value)){
					value = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
				}
				if("SYSTIME".equals(value)){
					value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				}
				argslist.add(value);
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(" ) values ( ");
			for(int i=0;i<argslist.size();i++){
				sb.append("?,");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(")");
			logger.info(sb.toString()+","+argslist);
			amsJdbcTemplate.update(sb.toString(), argslist.toArray());
		}
		
	}
	private void excuteBatchUpdate4Ams(String[] sql) {
		logger.info(Arrays.toString(sql));
		amsJdbcTemplate.batchUpdate(sql);
		
	}

	public List<Map<String, Object>> getImpDatasByNum(String sql,Object[] args,int start,int size){
		List<Map<String, Object>> result = null;
		if("Oracle JDBC driver".equals(impDBType)){
			StringBuffer sb = new StringBuffer();
			sb.append("select * from (select tt.*, ROWNUM AS rowno from ( ");
			sb.append(sql);
			sb.append(" ) tt ");
			sb.append(" where ROWNUM <= ?) table_alias where table_alias.rowno >? ");
			logger.info(sb.toString()+","+buildList(start+size,start));
			result= amsJdbcTemplate.queryForList(sb.toString(), new Object[]{start+size,start});
			if(result!=null&&result.size()>0){
				for(Map<String,Object> map : result){
					map.remove("ROWNO");
				}
			}
		}else if("Microsoft SQL Server".equals(impDBType)){
			//暂未实现
			result= null;
		}else{
			throw new RuntimeException("不支持该数据库类型："+impDBType);
		}
		if(result!=null&&result.size()>0){
			for(Map<String,Object> map : result){
				for(String key:map.keySet()){
					Object value = map.get(key);
					if(value instanceof BigDecimal){
						BigDecimal valueBD=(BigDecimal)value;
						int BigDecimal=valueBD.intValue();
						map.put(key, BigDecimal+"");
					}else if(value !=null){
						map.put(key, String.valueOf(value));
					}
				}
			}
		}
		return result;
	}
	public static List buildList(Object... args){
		return new ArrayList(Arrays.asList(args));
	}

	public List<Map<String,Object>> queryAmsData(String sql, Object[] args) {
		logger.info(sql+","+Arrays.asList(args));
		List<Map<String,Object>> result = amsJdbcTemplate.queryForList(sql, args);
		return result;
	}

	/**更新档案数据库
	 * @param updataSql
	 * @param args
	 * @return
	 */
	public int excuteUpdate4Ams(String updataSql, Object[] args) {
		logger.info(updataSql+","+Arrays.asList(args));
		return amsJdbcTemplate.update(updataSql, args);
	}
	/**判断数据的ID是否已存在
	 * @param syscode
	 * @param libcode
	 * @param arclvl
	 * @param unitsys
	 * @return
	 */
	public boolean testexit(String syscode, String libcode, String arclvl, String unitsys) {
		//String sql = "select count(*) from "+tableName(libcode,unitsys,arclvl)+" where DID=? and (ARCHCODE is null or ARCHCODE='' or ARCHCODE='null' or ARCHCODE='NULL')";//按编号判断
		String sql = "select count(*) from "+tableName(libcode,unitsys,arclvl)+" where DID=? and status='0' ";//按整编库判断
		Object[] args = new Object[]{syscode};
		logger.debug(sql+","+Arrays.asList(args));
		return amsJdbcTemplate.queryForInt(sql,args)>0;
	}
	private String testexitForMemo(String syscode, String libcode, String arclvl, String unitsys) {
		String remark="";
		String memo="";
		String bhFILED="";
		if("VOL".equals(arclvl)){
			bhFILED="CPBH";
		}else{
			bhFILED="WJBH";
		}
		
		String sql = "select ID,DID,TITLE,STATUS,CREATETIME,"+bhFILED+",EXT5 from "+tableName(libcode,unitsys,arclvl)+" where DID=? and (status='0' or status='1') ";//按整编库判断
		Object[] args = new Object[]{syscode};
		List<Map<String,Object>> result = amsJdbcTemplate.queryForList(sql, args);
		if(result!=null&&result.size()>0){
			for(int i=0;i<result.size();i++){
				Map<String,Object> m=result.get(i);
				String zt=BaseDataUtil.trimString(m.get("STATUS"));
				if("1".equals(zt)){
					zt="档案库";
				}else{
					zt="整编库";
				}
				String did=BaseDataUtil.trimString(m.get("DID"));
				String cpbh=BaseDataUtil.trimString(m.get(bhFILED));
				String title=BaseDataUtil.trimString(m.get("TITLE"));
				String sortTile=title;
				if(title.length()>20){
					sortTile=title.substring(0,20);
				}
				String createtime=BaseDataUtil.trimString(m.get("CREATETIME"));
				if(createtime.indexOf(".")>0){
					createtime=createtime.substring(0,createtime.indexOf("."));
				}
				//整编库:cp171116009,测试案卷XXXX,100000,20180621 09:52:55;
				//案卷：库类型（整编库／档案库）、产品ｉｄ、案卷标题（取前20字符）、产品编号（cpbh）、ｅｘｔ５（重复的原案卷进入紫光时间）
				//文件：库类型（整编库／档案库）、文件ｉｄ、标题（取前20字符）、文件编号（wjbh）、ｅｘｔ５（重复的原文件进入紫光时间）
				String cf=zt+"、"+did+"、"+sortTile+"、"+cpbh+"、"+createtime+";";
				String cf1=zt+"、"+did+"、"+title+"、"+cpbh+"、"+createtime+";";
				
				memo=memo+cf;
				remark=remark+cf1;
			}
		}
		if(!"".equals(remark)){
			logger.info("重复归档："+syscode);
			//logger.info(remark);
			logger.info(memo);
		}
		if(memo.length()>400){
			memo=memo.substring(0,398);
			logger.info("memo长度大于数据库长度400，对其截取...."+memo);
		}
		return memo;
	}
	public String tableName(String libcode,String unitsys,String arclvl){
		String tableName = "";
		if("FILE".equalsIgnoreCase(arclvl)){
			tableName = "f"+unitsys+"_"+libcode+"_document";
		}else if("VOL".equalsIgnoreCase(arclvl)){
			tableName = "f"+unitsys+"_"+libcode+"_volume";
		}else if("BOX".equalsIgnoreCase(arclvl)){
			tableName = "f"+unitsys+"_"+libcode+"_box";
		}else if("EFILE".equalsIgnoreCase(arclvl)){
			tableName = "e_record";
		}
		
		return tableName;
	}

	public void importFileData(Map<String, Object> fileRow,
			List<Map<String, Object>> efileList, String libcode,
			String unitsys, PropertiesConfig paramConfig) {
		String syscode = (String)fileRow.get("DID");
//		boolean exit = testexit(syscode,libcode,"FILE",unitsys); 
//		if(exit){
//			logger.info("执行重复归档流程："+syscode);
//			Object[] args= new Object[]{syscode};
//			//String findql = "select ARCHCODE,STATUS,DID,ID from "+tableName(libcode,unitsys,"FILE")+" where DID=? and (ARCHCODE is null or ARCHCODE='' or ARCHCODE='null' or ARCHCODE='NULL') or status='9'";
//			String findql = "select ARCHCODE,STATUS,DID,ID from "+tableName(libcode,unitsys,"FILE")+" where DID=? and  status='0'";
//			List<Map<String, Object>> result =  queryAmsData(findql, args);
//			Map volmap = result.get(0);
//			String id=volmap.get("ID").toString();
//			//物理删除案卷、文件、附件
//			String delefile="delete from "+tableName(libcode,unitsys,"EFILE")+" where archid in (select id from "+tableName(libcode,unitsys,"FILE")+" where ID= ?)";
//			String delglgx = "delete from d_glgx_gd where wjid in (select id from "+tableName(libcode,unitsys,"FILE")+"  where ID= ?)";
//			String delfile = "delete from "+tableName(libcode,unitsys,"FILE")+" where ID= ?";
//			Object [] args1={id};
//			excuteUpdate4Ams(delefile,args1);
//			excuteUpdate4Ams(delglgx,args1);
//			excuteUpdate4Ams(delfile,args1);
//		}
//		String memo=testexitForMemo(syscode,libcode,"FILE",unitsys);
//		if(!"".equals(memo)){
//			fileRow.put("MEMO", memo);
//		}
			//导入文件级数据	
			String filetablename = tableName(libcode,unitsys,"FILE");
			insertObject2Ams(BaseDataUtil.buildList(fileRow),filetablename);
					
			//导入电子文件
			String efiletablename = tableName(libcode,unitsys,"EFILE");
			insertObject2Ams(efileList,efiletablename);
		
	}

	public void deleteVolData(Map<String, Object> volRow,
			List<Map<String, Object>> fileList,
			List<Map<String, Object>> efileList, String libcode,
			String unitsys, PropertiesConfig paramConfig) {
		try{
			String voltablename = tableName(libcode,unitsys,"VOL");
			
			String syscode = (String)volRow.get("ID");
			String deletevol = "delete from "+voltablename+" where ID = ?";
			Object [] args={syscode};
			logger.info("deletevolSQl="+deletevol);
			logger.info("deletevolARGS="+syscode);
			excuteUpdate4Ams(deletevol,args);
			
			String filetablename = tableName(libcode,unitsys,"FILE");
			String deletefile = "delete from "+filetablename+" where ID = ? and EDITTIME=?";
			for(int i=0;i<fileList.size();i++){
				Map f=fileList.get(i);
				String id = (String)f.get("ID");
				Object [] args1={id,f.get("EDITTIME")};
				logger.info("deletefileSQl="+deletefile);
				logger.info("deletefileARGS="+id);
				excuteUpdate4Ams(deletefile,args1);
			}
		
			String efiletablename = tableName(libcode,unitsys,"EFILE");
			String deleteEfile = "delete from "+efiletablename+" where ID = ? and HANGINGTIME=?";
			for(int i=0;i<efileList.size();i++){
				Map e=efileList.get(i);
				String id = (String)e.get("ID");
				Object [] args2={id,e.get("HANGINGTIME")};
				logger.info("deleteEfileSQl="+deleteEfile);
				logger.info("deleteEfileARGS="+id);
				excuteUpdate4Ams(deleteEfile,args2);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	public void deleteFileData(Map<String, Object> fileRow,
			List<Map<String, Object>> efileList, String libcode,
			String unitsys, PropertiesConfig paramConfig) {
		String filetablename = tableName(libcode,unitsys,"FILE");
		String syscode = (String)fileRow.get("ID");
		String deletefile = "delete from "+filetablename+" where ID = ?  and EDITTIME=?";
		Object [] args={syscode,fileRow.get("EDITTIME")};
		logger.info("deletefileSQl="+deletefile);
		logger.info("deletefileARGS="+syscode);
		excuteUpdate4Ams(deletefile,args);
		
		String efiletablename = tableName(libcode,unitsys,"EFILE");
		String deleteEfile = "delete from "+efiletablename+" where ID = ?  and HANGINGTIME=?";
		for(int i=0;i<efileList.size();i++){
			Map e=efileList.get(i);
			String id = (String)e.get("ID");
			Object [] args2={id,e.get("HANGINGTIME")};
			logger.info("deleteEfileSQl="+deleteEfile);
			logger.info("deleteEfileARGS="+id);
			excuteUpdate4Ams(deleteEfile,args2);
		}
	}

}
