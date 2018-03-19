package com.mymediaplayer.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

/**
 * 摇一摇倒计时
 */

public class TimerForRock {

    public final static int TIME = 90;
    private static int limitTime = TIME;//剩余时间
    private static Context mContext;

    public static void initLimitTime(int time,Context context){
        limitTime = time;
        mContext = context;
        mHandler.removeMessages(1234);
        mHandler.sendEmptyMessageDelayed(1234, 60*1000);
    }

    public static void reset(){
        limitTime = TIME;
    }

    public static int getLimitTime(){
        return limitTime;
    }

    private static Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case 1234:
                    if(UserUtil.userid != -1&&UserUtil.userState == 1){
                        limitTime --;
                        if(limitTime <= 0){
                            limitTime = 0;
                        }
                        mHandler.sendEmptyMessageDelayed(1234, 60*1000);
                        if(mContext != null){
                            mContext.sendBroadcast(new Intent(ActionID.ACTION_BROADCAST_ROCK_TIME_CHANGE));
                        }
                    }
                    break;
            }
            super.handleMessage(msg);
        }

    };

    public static boolean isNoTime(){
        if(UserUtil.userid != -1&&UserUtil.userState == 1){
            if(limitTime <= 0){
                return true;
            }
        }
        return false;
    }
}
