  package com.mymediaplayer.utils;

  import android.content.ContentResolver;
  import android.content.ContentValues;
  import android.content.Context;
  import android.database.Cursor;
  import android.database.SQLException;
  import android.net.ConnectivityManager;
  import android.net.NetworkInfo;
  import android.net.Uri;
  import android.net.wifi.WifiConfiguration;
  import android.net.wifi.WifiInfo;
  import android.net.wifi.WifiManager;
  import android.telephony.TelephonyManager;

  import java.util.ArrayList;
  import java.util.List;

  public class APNUtil {
      public static class APNNet {

          public static String CMWAP = "cmwap";

          public static String CMNET = "cmnet";
          // 中国联通3GWAP设置 中国联通3G因特网设置 中国联通WAP设置 中国联通因特网设置
          // 3gwap 3gnet uniwap uninet

          public static String GWAP_3 = "3gwap";

          public static String GNET_3 = "3gnet";

          public static String UNIWAP = "uniwap";

          public static String UNINET = "uninet";
      }

        public static int oldConnectType;
      public static String oldNetworkWiFi_id;

      public static APN oldAPN;

      Context context;

      ConnectivityManager conManager = (ConnectivityManager) context
              .getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo info = conManager
              .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
      String currentAPN = info.getExtraInfo();

      public static final Uri CURRENT_APN_URI = Uri
              .parse("content://telephony/carriers/preferapn");
      public static final Uri APN_LIST_URI = Uri
              .parse("content://telephony/carriers");

        public static boolean saveOldNetWorkandChageToCmwapAPN(Context context) {
          ConnectivityManager connectivityManager = (ConnectivityManager) context
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
          NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
          if(networkInfo!=null){
              if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                  String wifiResult = getCurrentWifiParamFromSetting(context);
                  oldConnectType = ConnectivityManager.TYPE_WIFI;
                  oldNetworkWiFi_id = wifiResult;
                  APN cmwap=getCmwapAPN(context);
                  if(cmwap==null){   //如果当前没有cmwap APN
                      int cmwapid=createCmWapAPN(context);
                      if(cmwapid==-1){
                          resetOldData();
                          return false;
                      }
                  }
                  //保证存在cmwap之后才关闭wifi
                  closeWIFI(context);
                } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                  oldConnectType = ConnectivityManager.TYPE_MOBILE;
                  oldAPN = getCurrentAPNFromSetting(context);
                  if (oldAPN == null) {					//如果当前用户没有开启apn就将apn  改成cmwap
                      oldAPN = getCmwapAPN(context);
                      if(oldAPN==null){   //如果当前没有cmwap APN
                          int cmwapid=createCmWapAPN(context);
                          if(cmwapid==-1){
                              resetOldData();
                              return false;
                          }
                      }
                  }else{
                      APN cmwap=getCmwapAPN(context);
                      if(cmwap==null){   //如果当前没有cmwap APN
                          int cmwapid=createCmWapAPN(context);
                          if(cmwapid==-1){
                              resetOldData();
                              return false;
                          }
                      }
                  }
              }
            }else{//如果用户在点击确定下载之前断网,就把oldAPN设置为cmwap
              oldConnectType = ConnectivityManager.TYPE_MOBILE;
              oldAPN = getCmwapAPN(context);
              if(oldAPN==null){   //如果当前没有cmwap APN
                  int cmwapid=createCmWapAPN(context);
                  if(cmwapid==-1){
                      resetOldData();
                      return false;
                  }
              }
          }
          boolean result = chageNetWorktoCmwap(context);
          if(result==false){
              backToNetWork(context);
  //			resetOldData();
          }
          return result;
      }

        public static boolean backToNetWork(Context context) {
  //		try {
  //			Thread.sleep(4000);
  //		} catch (InterruptedException e) {
  //			e.printStackTrace();
  //		}
          if (oldAPN == null&&oldNetworkWiFi_id==null&&oldConnectType==0) {
              //如果这些数据都已经被初始化了，直接返回
              return true;
          } else {
              ConnectivityManager connectivityManager = (ConnectivityManager) context
              .getSystemService(Context.CONNECTIVITY_SERVICE);
              NetworkInfo curNetworkInfo=connectivityManager.getActiveNetworkInfo();
              boolean curNetworkInfoNull_flag=true;
              int curConnectType=0;
              if(curNetworkInfo!=null){
                  curNetworkInfoNull_flag=false;
                  curConnectType=curNetworkInfo.getType();
              }

                if(oldConnectType== ConnectivityManager.TYPE_WIFI){
                  WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                  if((curNetworkInfoNull_flag==false)&&(curConnectType== ConnectivityManager.TYPE_WIFI)&&(wifiManager.getConnectionInfo().getNetworkId()== Integer.parseInt(oldNetworkWiFi_id))){
                      return true;
                  }else{
                      boolean result = reConnectWiFi(context);
  //					if(result==false){
  //						if(!reConnectWiFI0(context)){  //如果改用wifi 0 仍然连不上，则不再修改网络连接方式
  //							LogPrint.Print("console", "reconnect0 result:false");
  //							resetOldData();
  //							return true;
  //						}
  //					}
                      return true;
                  }
              }else{
                  if((curNetworkInfoNull_flag==false)&&(oldConnectType== ConnectivityManager.TYPE_MOBILE)&&(oldAPN.id.equals(getCurrentAPNFromSetting(context).id))){
                      return true;
                  }else{
                        List apnList=getAPNList(context);
                      APN curAPN = null;
                      boolean result=false;
                      boolean changeFlag=false;
                      for (int i = 0; i < apnList.size(); i++) {
                          curAPN=(APN)apnList.get(i);
                          if(curAPN.id.equals(oldAPN.id)){
                              changeFlag=true;
                          }
                      }
                      if(changeFlag){
                          result=chageAPN(context, oldAPN);  //无论是否成功变更回来，都不在变更
                          return true;
                      }else{     								//如果apn列表中已经没有了该apn则不在变更回来
                          return true;
                      }
                  }
              }
          }
      }

        public static String getCurrentWifiParamFromSetting(Context context) {
          ConnectivityManager connectivityManager = (ConnectivityManager) context
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
          if (connectivityManager.getActiveNetworkInfo().getType() != ConnectivityManager.TYPE_WIFI) {
              return null;
          } else {
              WifiManager wifiManager = (WifiManager) context
                      .getSystemService(Context.WIFI_SERVICE);
              int wifiNetWorkID = wifiManager.getConnectionInfo().getNetworkId();
              if (wifiNetWorkID != -1) {
                  return String.valueOf(wifiNetWorkID);
              } else {
                  return null;
              }
          }
      }

        /** 返回当前网络连接APN */
      public static APN getCurrentAPNFromSetting(Context context) {
          ContentResolver resolver = context.getContentResolver();
          Cursor cursor = null;
          APN apn = new APN();
          try {
              cursor = resolver.query(CURRENT_APN_URI, null, null, null, null);
              if (cursor != null && cursor.moveToFirst()) {
                  apn.id = cursor.getString(cursor.getColumnIndex("_id"));
                  apn.apn = cursor.getString(cursor.getColumnIndex("apn"));
                  apn.type = cursor.getString(cursor.getColumnIndex("type"));
                  apn.proxy = cursor.getString(cursor.getColumnIndex("proxy"));
              }
              cursor.close();
              return apn;
          } catch (SQLException e) {
              e.printStackTrace();
              return null;
          } finally {
              if (cursor != null) {
                  cursor.close();
              }
          }
      }

        public static boolean isCMWap(Context context) {

          ConnectivityManager conManager = (ConnectivityManager) context
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
          NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
          if (networkInfo != null) {
              if (networkInfo.getExtraInfo() == null) {
                  return false;
              } else {
                  return (networkInfo.getExtraInfo().equalsIgnoreCase("cmwap") ? true
                          : false);
              }
          }
          return false;
      }

      public static boolean isNetWorkInfoNull(Context context){
          ConnectivityManager conManager = (ConnectivityManager) context
                  .getSystemService(Context.CONNECTIVITY_SERVICE);
          NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
          return networkInfo==null?true:false;
      }

        private static List<APN> getAPNList(Context context) {
          // current不为空表示可以使用的APN
          String projection[] = { "_id,apn,type,proxy" };
          Cursor cr = context.getContentResolver().query(APN_LIST_URI,
                  projection, null, null, null);
          List<APN> list = new ArrayList<APN>();
          while (cr != null && cr.moveToNext()) {
              APN a = new APN();
              a.id = cr.getString(cr.getColumnIndex("_id"));
              a.apn = cr.getString(cr.getColumnIndex("apn"));
              a.type = cr.getString(cr.getColumnIndex("type"));
              a.proxy = cr.getString(cr.getColumnIndex("proxy"));
              list.add(a);
          }
          if (cr != null)
              cr.close();
          return list;
      }

        public static APN getCmwapAPN(Context context){
          List<APN> apnList = getAPNList(context);
          APN curAPN= null;
          for (int i = 0; i < apnList.size(); i++) {
              curAPN = apnList.get(i);
              if (curAPN.apn.equalsIgnoreCase("cmwap")) {
                  return curAPN;
              }
          }
          return null;
      }



      public static boolean chageNetWorktoCmwap(Context context) {
          APN cmWapAPN = getCmwapAPN(context);
          if (cmWapAPN == null) {
              int id=createCmWapAPN(context);
              if(id==-1){
                  return false;
              }else{
                  List<APN> apnList=getAPNList(context);
                  APN curAPN;
                  for (int i = 0; i < apnList.size(); i++) {
                      curAPN = apnList.get(i);
                      if (curAPN.id.equals(String.valueOf(id))) {
                          cmWapAPN=curAPN;
                      }
                  }
                  return chageAPN(context, cmWapAPN);
              }
          }else{
              return chageAPN(context, cmWapAPN);
          }

      }

        public static int createCmWapAPN(Context context) {
          String name = ""; // 接入点的名称
          String apn_addr = ""; // apn的名称-cmnet-cmwap-uninet-uniwap-3g、、、、
          int id = -1; // 创建的APN 的id值

          TelephonyManager telephonyManager = (TelephonyManager) context
                  .getSystemService(Context.TELEPHONY_SERVICE);

          String iNumeric = telephonyManager.getSimOperator();

          if (iNumeric.length() == 0) {

              return id;// fail to get IMSI information,SIM fail.
          }
          if (iNumeric.equals("46000") || iNumeric.equals("46002")) { // 中国移动
              name = "中国移动连接互联网";
              apn_addr = "CMWAP";
          } else{
              return id;
          }

            ContentResolver resolver = context.getContentResolver();
          ContentValues values = new ContentValues();// 创建APN相关数据结构
          values.put("name", name);
          values.put("apn", apn_addr);
          values.put("type", "default");
          values.put("numeric", iNumeric);
          values.put("mcc", iNumeric.substring(0, 3));
          values.put("mnc", iNumeric.substring(3, iNumeric.length()));
          values.put("proxy", "10.0.0.172");
          values.put("port", "80");
          values.put("mmsproxy", "");
          values.put("mmsport", "");
          values.put("user", "");
          values.put("server", "");
          values.put("password", "");
          values.put("mmsc", "");

          Cursor c = null;
          try {
              Uri newRow = resolver.insert(Uri
                      .parse("content://telephony/carriers"), values);
              if (newRow != null) {// 创建成功获取_id值
                  c = resolver.query(newRow, null, null, null, null);
                  int idindex = c.getColumnIndex("_id");
                  c.moveToFirst();
                  id = c.getShort(idindex);
              }
            } catch (SQLException e) {
              e.printStackTrace();
              if(e.getMessage()!=null){
                  LogPrint.Print("insert apn err", e.getMessage());
              }
              return -1;
          }
          if (c != null){
              c.close();
          }
          return id;
      }

        public static boolean chageAPN(Context context, APN apn) {
  //		ContentValues cv = new ContentValues();
  //
  //		cv.put("apn", matchAPN(apn.apn));
  //		cv.put("type", matchAPN(apn.type));
  //		int row = context.getContentResolver().update(CURRENT_APN_URI, cv,
  //				"_id=?", new String[]{apn.id});
          ContentResolver resolver = context.getContentResolver();
          if(apn.type==null||!apn.type.equals("default")){
              ContentValues updateTypeValues = new ContentValues();
              updateTypeValues.put("type", "default");
              resolver.update(APN_LIST_URI, updateTypeValues, "_id=?", new String[]{apn.id});
          }

          ContentValues values = new ContentValues();
          values.put("apn_id", apn.id);
          int row=resolver.update(CURRENT_APN_URI, values, null, null);
          if (row > 0) {
              return true;
          } else {
              return false;
          }
      }

        public static void resetOldData() {
          APNUtil.oldAPN = null;
          oldConnectType = 0;
          oldNetworkWiFi_id = null;
      }

      public static void closeWIFI(Context context) {
          WifiManager wifiManager = (WifiManager) context
                  .getSystemService(Context.WIFI_SERVICE);
          if (wifiManager.isWifiEnabled()) {
              WifiInfo temp = wifiManager.getConnectionInfo();
              if (temp != null) {
                  oldNetworkWiFi_id = String.valueOf(temp.getNetworkId());
                  wifiManager.setWifiEnabled(false);
              } else {
                  wifiManager.setWifiEnabled(false);
              }
          }
      }

        public static boolean reConnectWiFI0(Context context){
          WifiManager wifiManager = (WifiManager) context
                  .getSystemService(Context.WIFI_SERVICE);
          List<WifiConfiguration> wifiConfigurations = wifiManager
                  .getConfiguredNetworks();
  //		List<ScanResult> scanResults=wifiManager.getScanResults();
  //		scanResults.


          if (wifiConfigurations != null && wifiConfigurations.size() > 0
                  && oldNetworkWiFi_id != null) {
              WifiConfiguration curConfiguration=wifiConfigurations.get(0);
                      wifiManager.setWifiEnabled(true);
                      return wifiManager.enableNetwork(
                              curConfiguration.networkId, true);
          }else{
              return false;
          }


      }

        public static boolean reConnectWiFi(Context context) {
          WifiManager wifiManager = (WifiManager) context
                  .getSystemService(Context.WIFI_SERVICE);
          wifiManager.setWifiEnabled(true);
          List<WifiConfiguration> wifiConfigurations = wifiManager
                  .getConfiguredNetworks();
          if (wifiConfigurations != null && wifiConfigurations.size() > 0
                  && oldNetworkWiFi_id != null) {
              WifiConfiguration curConfiguration;
              for (int i = 0; i < wifiConfigurations.size(); i++) {
                  curConfiguration = wifiConfigurations.get(i);
                  if (curConfiguration.networkId == Integer.parseInt(oldNetworkWiFi_id)) {
                      return wifiManager.enableNetwork(Integer.parseInt(oldNetworkWiFi_id), true);
                  }
              }
              return false;
          } else if (oldNetworkWiFi_id == null && wifiConfigurations != null
                  && wifiConfigurations.size() > 0) {
              wifiManager.setWifiEnabled(true);
              return wifiManager
                      .enableNetwork(wifiConfigurations.get(0).networkId, true);
          }
          return false;
      }

        public static String matchAPN(String currentName) {
          if ("".equals(currentName) || null == currentName) {
              return "";
          }
          currentName = currentName.toLowerCase();
          if (currentName.startsWith(APNNet.CMNET))
              return APNNet.CMNET;
          else if (currentName.startsWith(APNNet.CMWAP))
              return APNNet.CMWAP;
          else if (currentName.startsWith(APNNet.GNET_3))
              return APNNet.GNET_3;
          else if (currentName.startsWith(APNNet.GWAP_3))
              return APNNet.GWAP_3;
          else if (currentName.startsWith(APNNet.UNINET))
              return APNNet.UNINET;
          else if (currentName.startsWith(APNNet.UNIWAP))
              return APNNet.UNIWAP;
          else if (currentName.startsWith("default"))
              return "default";
          else
              return "";
          // return currentName.substring(0, currentName.length() -
          // SUFFIX.length());

      }

        public static String getOldNetWork(){
          if(oldConnectType == ConnectivityManager.TYPE_WIFI){
              return "wifi";
          }else if((oldConnectType== ConnectivityManager.TYPE_MOBILE)&&oldAPN!=null&&oldAPN.apn.equalsIgnoreCase("cmwap")){
              return "cmwap";
          }else if((oldConnectType== ConnectivityManager.TYPE_MOBILE)&&oldAPN!=null&&oldAPN.apn.equalsIgnoreCase("cmnet")){
              return "cmnet";
          }else if((oldConnectType==0&&oldAPN == null)){
              return null;
          }else{
              return "other";
          }
      }



      public static class APN {
          String id;
          String apn;
          String type;
          String proxy;
      }
  }
