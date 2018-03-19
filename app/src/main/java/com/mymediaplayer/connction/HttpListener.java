package com.mymediaplayer.connction;

/**
 *
 */

public interface HttpListener {

    /**获得数据大小*/
    public void onSetSize(int size,int index);

    /**数据下载完成后的处理*/
    public void onFinish(byte[] data,int size,boolean isOver,int index);

    /**当前进度*/
    public void onCurDataPos(int dataPos,int index);

    /**错误异常*/
    public void onError(int code, String message,int index,int type);

    /**获得URL*/
    public void onGetUrl(String url,int type,int index);

    /**获得Content-type*/
    public void onGetContentType(String contentType,int index);

    /**获得apntype*/
    public void onApnType(String apn,int index);

    /**图片错误提醒*/
    public void onImageError(int code,String message,int index);
}
