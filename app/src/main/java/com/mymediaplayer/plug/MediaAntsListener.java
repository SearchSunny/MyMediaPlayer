/**
 * 
 */
package com.mymediaplayer.plug;

/**
 *
 */
public interface MediaAntsListener {

	/**获得数据大小*/
    public void onSetSize(int size, int index);

    /**数据下载完成后的处理*/
    public void onFinish(byte[] data, int size, boolean isOver, int index, int start, int end);

    /**当前进度*/
    public void onCurDataPos(int dataPos, int index);

    /**错误异常*/
    public void onError(int code, String message, int index, int type);

    /**获得URL*/
    public void onGetUrl(String url, int type, int index);

    /**获得Content-type*/
    public void onGetContentType(String contentType, int index);
}
