package com.ams.widget;

public interface EFileTransferServie {
	
	/**�жϷ����Ƿ���
	 * @return
	 */
	boolean testOpen();
	
	/**�ж�Զ���ļ��Ƿ����
	 * @param filepath
	 * @return
	 */
	boolean testExist(String remptePath);
	
	/**ֱ�ӻ�ȡ�ļ���ַ�� Ŀǰֻ�Ա����ļ���Ч
	 * @param remptePath
	 * @return
	 */
	String getLocalPath(String remptePath);
	
	/**�����ļ�
	 * @param remotePath
	 * @param downldpath
	 * @return
	 */
	boolean download(String remotePath,String downldpath);
	
	/**�ϴ��ļ�
	 * @param remotePath
	 * @param localpath
	 * @return
	 */
	boolean upload(String remotePath,String localpath);
	
}
