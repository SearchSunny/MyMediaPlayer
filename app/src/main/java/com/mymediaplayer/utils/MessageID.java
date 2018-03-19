package com.mymediaplayer.utils;

/**
 *
 */

public class MessageID {

    //mediaplayer=========================
    //初始化视频
    public static final int MESSAGE_MEDIA_INIT = 400;
    //播放
    public static final int MESSAGE_MEDIA_PLAY = 401;
    //快进
    public static final int MESSAGE_MEDIA_NEXT = 402;
    //快退
    public static final int MESSAGE_MEDIA_PRE = 403;
    //当前播放时间
    public static final int MESSAGE_MEDIA_CURRENTTIME = 404;
    //跳转播放位置
    public static final int MESSAGE_MEDIA_SEEK = 405;
    //5秒后关闭视频操作控件
    public static final int MESSAGE_MEDIA_CLOSE = 406;

    //喜欢返回的刷新
    public static final int REQUESTCODE_LIKE_FLUSH = 800;

    //视频已达到可播放的长度
    public static final int MESSAGE_MEDIAPLAYER_READY = 900;
    //视频加载错误
    public static final int MESSAGE_MEDIAPLAYER_LOAD_ERROR = 901;
    //视频播放错误
    public static final int MESSAGE_MEDIAPLAYER_PLAY_ERROR = 902;
    //更新缓存进度
    public static final int MESSAGE_MEDIAPLAYER_BUFFERUPDATE = 903;

    //============联网相关message
    //页面数据下载完成
    public static final int MESSAGE_CONNECT_DOWNLOADOVER = 6;
    //页面数据布局完成,不包含图片
    public static final int MESSAGE_CONNECT_LAYOUTOVER = 7;
    //连接开始
    public static final int MESSAGE_CONNECT_START = 8;
    //连接出错
    public static final int MESSAGE_CONNECT_ERROR = 9;
}
