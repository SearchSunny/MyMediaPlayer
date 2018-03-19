  /**
 * 
 */
package com.mymediaplayer.utils;

import android.util.Log;

  /**
   * 用于调试时的数据输出
   */
  public class LogPrint {

      public final static String LOG_TAG = "console";

      //是否打印log信息的开关，debug模式打印log信息  release模式不打印log信息
      private static boolean mIsDebugMode=true;

      public static void isPrintLogMsg(Boolean isPrint){
          mIsDebugMode = isPrint;
      }

      public static void Print(String msg){
          if(mIsDebugMode){
              Log.i(LOG_TAG, msg);
          }
      }

      public static void Print(String tag, String msg) {
          if(mIsDebugMode){
              Log.i(tag, msg);
          }
      }

      public static boolean IsDebugMode(){
          return mIsDebugMode;
      }
  }
