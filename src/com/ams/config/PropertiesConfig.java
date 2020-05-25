package com.ams.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class PropertiesConfig implements ParamConfigInter {
	private	PropertiesConfiguration config;
	
	private static	Map<String, String> transferrMapping;
	static{
		AbstractConfiguration.setDelimiter('\n');
		// 以下为转移字符的设定
		transferrMapping = new HashMap<String, String>();
		transferrMapping.put(",", "&#44;");
		transferrMapping.put(":", "&#58;");
		transferrMapping.put(";", "&#59;");
	}
	
	public PropertiesConfig(URL url){
		try {
			config = new PropertiesConfiguration(url);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getParam(String key) {
		if(config == null) return null;
		String value = config.getString(key);
		if("".equals(value)) value = null;
		return value;
	}

	@Override
	public String getParamWidthDefault(String key, String defaultvalue) {
		if(config == null) return defaultvalue;
		String value = getParam(key);
		if(value == null )value= defaultvalue;
		return value;
	}

	@Override
	public List<String> getParamList(String key) {
		return getParamList(key,null);
	}

	@Override
	public List<String> getParamList(String key, String flag) {
		if(flag==null)flag = ",";
		String values = getParam(key);
		if(values!=null){
			List<String> result = new ArrayList<String>(Arrays.asList(values.split(flag)));
			String tr = transferrMapping.get(flag);
			if(tr!=null){
				for(int i=0;i<result.size();i++){
					String value = result.get(i);
					value = value.replaceAll(tr,flag);
					result.set(i, value);
				}
			}
			return result;
		}else{
			return new ArrayList<String>();
		}
	}

}
