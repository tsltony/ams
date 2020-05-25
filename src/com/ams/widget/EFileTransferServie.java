package com.ams.widget;

public interface EFileTransferServie {
	
	/**判断服务是否开启
	 * @return
	 */
	boolean testOpen();
	
	/**判断远程文件是否存在
	 * @param filepath
	 * @return
	 */
	boolean testExist(String remptePath);
	
	/**直接获取文件地址， 目前只对本地文件有效
	 * @param remptePath
	 * @return
	 */
	String getLocalPath(String remptePath);
	
	/**下载文件
	 * @param remotePath
	 * @param downldpath
	 * @return
	 */
	boolean download(String remotePath,String downldpath);
	
	/**上传文件
	 * @param remotePath
	 * @param localpath
	 * @return
	 */
	boolean upload(String remotePath,String localpath);
	
}
