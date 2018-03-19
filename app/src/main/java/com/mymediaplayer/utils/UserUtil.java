package com.mymediaplayer.utils;

import android.content.Context;

/**
 *
 */

public class UserUtil {

    //用户id
    public static int userid;
    //游客id
    public static String vid;
    //用户状态,0为未注册（游客）,1为已注册（正式用户）,2为无效,3为登出
    public static int userState;
    //上一次的联网方式
    public static String preNetApn;
    //是否显示价格
    public static boolean isShowPrice = false;
    //是否显示商品特色
    public static boolean isShowFeature = true;

    public static String Encryption(String str){
        StringBuffer sb = new StringBuffer();
        String[] tmps = new String[str.length()];
        String tmp;
        for(int i = 0;i < str.length();i ++){
            tmps[i] = str.substring(i, i+1);
        }
        for(int i = 0;i < tmps.length;){
            if(i == tmps.length -1)break;
            tmp = tmps[i];
            tmps[i] = tmps[i+1];
            tmps[i+1] = tmp;
            i+=2;
        }
        for(int i = 0;i < tmps.length;i++){
            sb.append(tmps[i]);
        }
        return sb.toString();
    }

    public static String Decrypt(String str){
        return Encryption(str);
    }

    //登出时清除数据
    public static void clearSharePreference(Context context, String toast){

        CommonUtil.ShowToast(context, toast);
        //清空操作方式
        CommonUtil.saveOperateHand(context, "0");
        UserUtil.isShowFeature = true;
        UserUtil.isShowPrice = false;
    }

}
