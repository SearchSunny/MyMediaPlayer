package com.mymediaplayer.utils;

/**
 *
 */

public class ActionID {

    //摇一摇倒计时改变
    public final static String ACTION_BROADCAST_ROCK_TIME_CHANGE = "com.cmmobi.broadcast.rock.time.change";

    //==================MediaPlayer手势广播动作
    //视频播放完成
    public final static String ACTION_BROADCAST_MEDIAPLAYER_FINISH = "com.cmmobi.broadcast.MediaPlayerFinish";
    //关闭手势面板
    public final static String ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CLOSE = "com.cmmobi.broadcast.MediaPlayerGestureClose";
    //无匹配到任何手势
    public final static String ACTION_BROADCAST_MEDIAPLAYER_GESTURE_NO_RESULT = "com.cmmobi.broadcast.MediaPlayerGestureNoResult";
    //匹配到对钩手势
    public final static String ACTION_BROADCAST_MEDIAPLAYER_GESTURE_RIGHT = "com.cmmobi.broadcast.MediaPlayerGestureRight";
    //匹配到圆圈手势
    public final static String ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CIRCLE = "com.cmmobi.broadcast.MediaPlayerGestureCircle";
    //匹配到三角手势
    public final static String ACTION_BROADCAST_MEDIAPLAYER_GESTURE_TRIANGLE = "com.cmmobi.broadcast.MediaPlayerGestureTriangle";
}
