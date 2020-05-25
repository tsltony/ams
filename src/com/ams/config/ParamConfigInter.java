package com.ams.config;

import java.util.List;

public interface ParamConfigInter {
	String getParam(String key);
	String getParamWidthDefault(String key ,String defaultvalue);
	List<String> getParamList(String key);
	List<String> getParamList(String key,String flag);
}
