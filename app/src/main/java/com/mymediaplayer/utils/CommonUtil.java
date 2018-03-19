/**
 *
 */
package com.mymediaplayer.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类
 */
public class CommonUtil {

    public static final String PATH = "MyMediaPlayer";

    public static String dir_friends = getExtendsCardPath() + PATH + "/friend/";
    //cache目录,保存缓存数据
    public static String dir_cache = getExtendsCardPath() + PATH + "/cache";
    //cache中的首页缓存目录
    public static String dir_cache_page = dir_cache + "/page";
    //下载地址
    public static String dir_download = getExtendsCardPath() + PATH + "/download";
    //user目录,保存用户数据,如消息
    public static String dir_user = getExtendsCardPath() + PATH + "/user";
    //视频文件保存地址
    public static String dir_media = getExtendsCardPath() + PATH + "/media";
    //nomedia文件,避免图片被系统扫描到
    public final static String NOMEDIA = ".nomedia";
    //用户信息文件的名称
    public static String dir_message_user = dir_user + "/message_user";
    //拓展名
    public static String endName = ".dat";
    //屏幕的宽度
    public static int screen_width;
    //屏幕的高度
    public static int screen_height;
    //是否是在信息管理或私信界面
    public static boolean isInMessageScreen;
    //是否是正常进入应用
    public static boolean isNormalInToApp;
    public static boolean isConnecting;
    private static int[] randoms = new int[]{-30, -20, -10, 10, 20, 30};

    //软件是否在前台（home）
    public static boolean isAppOnForeground;

    /**
     * 网络是否是打开的(WIFI\cmwap\cmnet)
     *
     * @param context
     * @return
     */
    public static boolean isNetWorkOpen(Context context) {
        boolean isOpen = false;
        try {
            ConnectivityManager cwjManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            isOpen = cwjManager.getActiveNetworkInfo().isAvailable();
        } catch (Exception ex) {
            LogPrint.Print("isNetWorkOpen", ex.toString());
            //如果出异常，那么就是电信3G卡
            isOpen = false;
        }

        return isOpen;
    }

    /**
     * 获取连接类型
     *
     * @param context
     * @return
     */
    public static String getApnType(Context context) {
        String result = null;
        String proxy = null;
        try {
            if (isNetWorkOpen(context)) {
                ConnectivityManager mag = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                String type = mag.getActiveNetworkInfo().getTypeName();
                if (type.toLowerCase().equals("wifi")) {
                    result = "wifi";
                } else {
                    proxy = APNUtil.getCurrentAPNFromSetting(context).proxy;
                    NetworkInfo mobInfo = mag.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    result = mobInfo.getExtraInfo();
                }
            } else {
                return UserUtil.preNetApn == null ? "wifi" : UserUtil.preNetApn;
            }
        } catch (Exception e) {
            LogPrint.Print("tag", "CommonUtil:getApnType apn Error");
        } finally {
            //如果出异常或者为null，则是电信3G卡
            if (result == null || result.indexOf("#") >= 0) {

                result = UserUtil.preNetApn == null ? "wifi" : UserUtil.preNetApn;
            }
        }
        return result.toLowerCase();
    }

    //获得操作系统版本
    public static String getOs_Version() {
        if (null != android.os.Build.VERSION.RELEASE) {
            return android.os.Build.VERSION.RELEASE;
        }
        return "";
    }

    //获取imei
    public static String getIMEI(Context activity) {
        try {
            TelephonyManager tm = (TelephonyManager) activity.getSystemService(activity.TELEPHONY_SERVICE);
            String mImei = tm.getDeviceId();
            if (mImei != null) {
                mImei = mImei.trim();
                return mImei;
            }
        } catch (Exception e) {
        }
        return "";
    }

    //获取imsi
    public static String getIMSI(Context activity) {
        try {
            TelephonyManager tm = (TelephonyManager) activity.getSystemService(activity.TELEPHONY_SERVICE);
            String mImsi = tm.getSimSerialNumber();// sim卡后面的20位唯一标市
            if (mImsi != null) {
                mImsi = mImsi.trim();
                return mImsi;
            }
        } catch (Exception e) {
        }
        return "";
    }

    //获取sim卡类型
    public static String getSimType(Context activity) {
        try {
            String mImsi = getIMSI(activity);
            if (mImsi.length() >= 6) {
                return mImsi.substring(4, 6);
            }
        } catch (Exception e) {
        }
        return "-1";
    }

    //获得设备名称
    public static String getDeviceName() {
        if (null != android.os.Build.MODEL) {
            return android.os.Build.MODEL;
        }
        return "";
    }

    public static boolean getSDCardState() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else if (new File("/flash/").exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static String getExtendsCardPath() {
        if (getSDCardState()) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return Environment.getExternalStorageDirectory().getPath() + "/";
            } else {
                return "/flash/";
            }
        } else {
            return "/sdcard/";
        }
    }

    /**
     * 获取剩余空间,单位为M
     *
     * @return
     */
    public static long getAviableSpaceOfSdcard() {
        if (getSDCardState() == false) {
            return 0;
        } else {

            StatFs stat = new StatFs(getExtendsCardPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize / 1024 / 1024;
        }
    }

    public static boolean isNullOrEmpty(String s) {
        if (s == null || s.equals("")) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * 创建文件夹
     */
    public static boolean createDirs(String dir, boolean bcreateNoMedia, Context context) {
        boolean result = false;
        if (getSDCardState() == false) {
            if (context != null) {
                ShowToast(context, "SD卡未找到，小C无法安家。");
            }
            return false;
        }
        try {
            if (!isNullOrEmpty(dir)) {
                File file = new File(dir);
                if (!file.exists()) {
                    result = file.mkdirs();
                    if (bcreateNoMedia) {
                        File _file = new File(dir + "/" + NOMEDIA);
                        if (!_file.exists()) {
                            _file.createNewFile();
                        }
                    }
                } else {
                    result = true;
                }
            }
        } catch (Exception e) {
        }

        return result;
    }

    /**
     * 文件是否存在
     */
    public static boolean exists(String path) {
        if (isNullOrEmpty(path)) return false;

        File file = new File(path);
        return file.exists();
    }

    /**
     * 将文本转化为md5码，用于保存链接地址，文件路径等
     */
    public static String urlToNum(String str) {
        if (str == null) return null;
        String result = null;

        MD5 md5 = new MD5();
        result = md5.getMD5ofStr(str);

        return result;
    }

    /**
     * 读文件，返回byte流
     */
    public static byte[] getSDCardFileByteArray(String strPath) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream in;
        byte[] result = null;
        byte[] data = new byte[2 * 1024];
        int iDataLen;
        try {
            in = new FileInputStream(strPath);
            while ((iDataLen = in.read(data)) != -1) {
                bos.write(data, 0, iDataLen);
            }
            result = bos.toByteArray();
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * 读文件，返回byte流
     */
    public static byte[] getSDCardFileByteArray(File f) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream in;
        byte[] result = null;
        byte[] data = new byte[2 * 1024];
        int iDataLen;
        try {
            in = new FileInputStream(f);
            while ((iDataLen = in.read(data)) != -1) {
                bos.write(data, 0, iDataLen);
            }
            result = bos.toByteArray();
            in.close();
        } catch (Exception ex) {

        }
        return result;
    }

    public static Bitmap resizeImageBy2G(Bitmap src) {
        if (src == null) {
            return null;
        }
        float scaleWidth = 1;
        float scaleHeight = 1;
        scaleWidth = ((float) src.getWidth() / 2) / (float) src.getWidth();
        scaleHeight = ((float) src.getHeight() / 2) / (float) src.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        return resizeBmp;
    }

    /**
     * 2G下缩放大图片
     *
     * @param src
     * @param backgroundWidth
     * @param backgroundHeight
     * @return
     */
    public static Bitmap resizeBigImageBy2G(Bitmap src, int backgroundWidth, int backgroundHeight) {
        if (src == null) {
            return null;
        }
        float scaleWidth = 1;
        float scaleHeight = 1;
        scaleWidth = ((float) backgroundWidth / 4) / (float) src.getWidth();
        scaleHeight = ((float) backgroundHeight / 4) / (float) src.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        return resizeBmp;
    }

    /**
     * 2G下缩放小图片
     *
     * @param src
     * @param backgroundWidth
     * @param backgroundHeight
     * @return
     */
    public static Bitmap resizeImageBy2G(Bitmap src, int backgroundWidth, int backgroundHeight) {
        if (src == null) {
            return null;
        }
        float scaleWidth = 1;
        float scaleHeight = 1;
        scaleWidth = ((float) backgroundWidth / 2) / (float) src.getWidth();
        scaleHeight = ((float) backgroundHeight / 2) / (float) src.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        return resizeBmp;
    }


    /**
     * 缩放图片
     */
    public static Bitmap resizeImage(Bitmap src, int destW, int destH) {
        if (src == null) return null;
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        float scaleWidth = 1;
        float scaleHeight = 1;
        if (srcW == destW && srcH == destH) {
            return src;
        }
        //计算出这次要缩小的比例
        scaleWidth = (float) destW / (float) srcW;
        scaleHeight = (float) destH / (float) srcH;
        //产生resize后的Bitmap对象
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizeBmp = Bitmap.createBitmap(src, 0, 0, srcW, srcH, matrix, true);

        return resizeBmp;
    }

    /**
     * 写文件
     */
    public static void writeToFile(String dir, byte[] data) {
        File file = null;
        if (getSDCardState() == false) return;
        try {
            CommonUtil.createDirs(dir_cache, true, null);
            CommonUtil.createDirs(dir_cache_page, false, null);
            file = new File(dir);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (Exception ex) {
            if (file.exists()) {
                file.delete();
            }
            LogPrint.Print("writeFileError: " + ex.toString());
            ex.printStackTrace();
        }
    }

    //向文件末尾写数据
    public static void writeToFileFromEnd(String dir, byte[] data, boolean isDelete) {
        File file = null;
        if (getSDCardState() == false) return;
        try {
            file = new File(dir);
            if (file.exists()) {
                if (isDelete) {
                    file.delete();
                }
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            RandomAccessFile rAccessFile = new RandomAccessFile(file, "rw");
            rAccessFile.seek(rAccessFile.length());
            rAccessFile.write(data);
            rAccessFile.close();
        } catch (Exception e) {
            LogPrint.Print("writeFileFromEndError: " + e.toString());
            e.printStackTrace();
        }
    }

    public static Bitmap mergerTwoBitmap(Bitmap backBitmap, Bitmap topBitmap, int x, int y) {
        Bitmap bmp = Bitmap.createBitmap(backBitmap.getWidth(), backBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        canvas.setBitmap(bmp);
        canvas.drawBitmap(backBitmap, 0, 0, paint);
        canvas.drawBitmap(topBitmap, x, y, paint);
        canvas.save();
        return bmp;
    }

    /**
     * 2G下单品页商品图片
     *
     * @param backBitmap
     * @param topBitmap
     * @return
     */
    public static Bitmap mergerTwoBitmapBy2G(Bitmap backBitmap, Bitmap topBitmap, int x, int y) {
        Bitmap bmp = Bitmap.createBitmap(backBitmap.getWidth(), backBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        canvas.setBitmap(bmp);
        int centerX = x / 2 - backBitmap.getWidth() / 2;
        int centerY = y / 2 - backBitmap.getHeight() / 2;
        canvas.drawBitmap(backBitmap, 200, 80, paint);
        canvas.drawBitmap(topBitmap, x, y, paint);
        canvas.save();
        return bmp;
    }

    private static int getRandom() {
        int index = new Random().nextInt(5);
        return randoms[index];
    }

    /**
     * 处理2G下首页图片,根据不同随机数生成旋转角度
     *
     * @param context
     * @param src
     * @param backgroundWidth
     * @param backgroundHeight
     * @return
     */
    public static Bitmap mergerBitmapForMainPageBy2g(Context context, Bitmap src, int backgroundWidth, int backgroundHeight, Bitmap playicon, String price, String feature) {
        Bitmap bitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        Matrix matrix = new Matrix();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Style.FILL);
        //绘制白色底部
        canvas.drawRect(0, 0, backgroundWidth, backgroundHeight, paint);
        int rotate = getRandom();
        //判断图片是否超过图片框
        if (src.getWidth() >= backgroundWidth || src.getHeight() >= backgroundHeight) {
            matrix.postTranslate((bitmap.getWidth() / 2 - src.getWidth() / 2), (bitmap.getHeight() / 2 - src.getHeight() / 2));
        } else {
            float width = (src.getWidth()) / 2;
            float height = (src.getHeight()) / 2;
            matrix.preRotate(rotate, width, height);
            matrix.postTranslate((backgroundWidth - src.getWidth()) / 2, (backgroundHeight - src.getHeight()) / 2);
        }
        canvas.drawBitmap(src, matrix, paint);
        //绘制播放图标
        if (playicon != null) {
            canvas.drawBitmap(playicon, backgroundWidth - playicon.getWidth(), 0, paint);
        }
        //绘制价格
        if (UserUtil.isShowPrice) {
            if (price != null && price.length() > 0) {
                price = price.substring(0, price.indexOf("."));
                price = "￥" + price;
                paint.setTextSize(dip2px(context, 10));
                float limitWidth = paint.measureText(price) > backgroundWidth - 2 * 2 ? backgroundWidth - 2 * 2 : paint.measureText(price);
                Bitmap bitmap_price = getRoundRectImage((int) limitWidth + 2 * 2, (int) paint.getTextSize(), 0x7f000000, 0xffffffff, price, 2);
                int y = 0;
                if (UserUtil.isShowPrice && UserUtil.isShowFeature) {
                    //    				y = src.getHeight()-(bitmap_price.getHeight()+2)*2+1;
                    y = bitmap.getHeight() - (bitmap_price.getHeight() + 2) * 2 + 1;
                } else {
                    //    				y = src.getHeight()-(bitmap_price.getHeight()+2);
                    y = bitmap.getHeight() - (bitmap_price.getHeight() + 2);
                }
                //    			canvas.drawBitmap(bitmap_price, src.getWidth()-bitmap_price.getWidth()-2, y, paint);
                canvas.drawBitmap(bitmap_price, bitmap.getWidth() - bitmap_price.getWidth() - 2, y, paint);
            }
        }
        //绘制商品特色
        if (UserUtil.isShowFeature) {
            if (feature != null && feature.length() > 0) {
                paint.setTextSize(dip2px(context, 10));
                float limitWidth = paint.measureText(feature) > backgroundWidth - 2 * 2 ? backgroundWidth - 2 * 2 : paint.measureText(feature);
                Bitmap bitmap_price = getRoundRectImage((int) limitWidth + 2 * 2, (int) paint.getTextSize(), 0x7f000000, 0xffffffff, feature, 2);
                //    			int y = src.getHeight()-(bitmap_price.getHeight()+2);
                int y = bitmap.getHeight() - (bitmap_price.getHeight() + 2);
                //    			canvas.drawBitmap(bitmap_price, src.getWidth()-bitmap_price.getWidth()-2, y, paint);
                canvas.drawBitmap(bitmap_price, bitmap.getWidth() - bitmap_price.getWidth() - 2, y, paint);
            }
        }
        return bitmap;
    }

    public static Bitmap addBoxLine(Bitmap bitmap) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        //绘制边框
        paint.setColor(0xffacacac);
        paint.setStyle(Style.STROKE);
        canvas.drawRect(0, 0, bitmap.getWidth() - 1, bitmap.getHeight() - 1, paint);
        canvas.save();
        return bitmap;
    }

    /**
     * 处理消息未读时出现未读图片
     */
    public static Bitmap mergerBitmapForRead(Bitmap src, Bitmap readicon) {
        Bitmap bmp = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        canvas.setBitmap(bmp);
        canvas.drawBitmap(src, 0, 0, null);
        canvas.drawBitmap(readicon, src.getWidth() - readicon.getWidth(), 0, paint);
        return bmp;
    }

    /**
     * 处理首页上的图片,添加三角,播放图片和边框
     */
    public static Bitmap mergerBitmapForMainPage(Context context, Bitmap src, int color, Bitmap playicon, String price, String feature) {
        Bitmap bmp = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        canvas.setBitmap(bmp);
        canvas.drawBitmap(src, 0, 0, null);
        //绘制播放图标
        if (playicon != null) {
            canvas.drawBitmap(playicon, src.getWidth() - playicon.getWidth(), 0, paint);
        }
        //绘制边框
        paint.setColor(0xffacacac);
        paint.setStyle(Style.STROKE);
        canvas.drawRect(0, 0, src.getWidth() - 1, src.getHeight() - 1, paint);
        //绘制价格
        if (UserUtil.isShowPrice) {
            if (price != null && price.length() > 0) {
                price = price.substring(0, price.indexOf("."));
                price = "￥" + price;
                paint.setTextSize(dip2px(context, 10));
                float limitWidth = paint.measureText(price) > src.getWidth() - 2 * 2 ? src.getWidth() - 2 * 2 : paint.measureText(price);
                Bitmap bitmap_price = getRoundRectImage((int) limitWidth + 2 * 2, (int) paint.getTextSize(), 0x7f000000, 0xffffffff, price, 2);
                int y = 0;
                if (UserUtil.isShowPrice && UserUtil.isShowFeature) {
                    y = src.getHeight() - (bitmap_price.getHeight() + 2) * 2 + 1;
                } else {
                    y = src.getHeight() - (bitmap_price.getHeight() + 2);
                }
                canvas.drawBitmap(bitmap_price, src.getWidth() - bitmap_price.getWidth() - 2, y, paint);
            }
        }
        //绘制商品特色
        if (UserUtil.isShowFeature) {
            if (feature != null && feature.length() > 0) {
                paint.setTextSize(dip2px(context, 10));
                float limitWidth = paint.measureText(feature) > src.getWidth() - 2 * 2 ? src.getWidth() - 2 * 2 : paint.measureText(feature);
                Bitmap bitmap_price = getRoundRectImage((int) limitWidth + 2 * 2, (int) paint.getTextSize(), 0x7f000000, 0xffffffff, feature, 2);
                int y = src.getHeight() - (bitmap_price.getHeight() + 2);
                canvas.drawBitmap(bitmap_price, src.getWidth() - bitmap_price.getWidth() - 2, y, paint);
            }
        }
        canvas.save();
        return bmp;
    }

    /**
     * 获得圆角底图的文本
     *
     * @param width     生成图片宽度
     * @param size      字体大小
     * @param backColor 背景颜色
     * @param textColor 文本颜色
     * @param text      文本
     * @param r         圆角半径
     */
    public static Bitmap getRoundRectImage(int width, int size, int backColor, int textColor, String text, int r) {
        Paint paint = new Paint();
        paint.setTextSize(size);
        int height = (int) paint.getTextSize() + 4;
        Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bmp);

        paint.setColor(backColor);
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
        RectF rf = new RectF(0, 0, width, height);
        canvas.drawRoundRect(rf, r, r, paint);
        paint.setColor(textColor);
        paint.setStyle(Style.STROKE);
        canvas.drawText(text, 2, paint.getTextSize(), paint);

        canvas.save();
        return bmp;
    }

    /**
     * 个人主页的播放图标
     */
    public static Bitmap mergerBitmapForUser(Context context, Bitmap src, int color, Bitmap playicon) {
        Bitmap bmp = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        canvas.setBitmap(bmp);
        canvas.drawBitmap(src, 0, 0, null);
        if (playicon != null) {
            canvas.drawBitmap(playicon, src.getWidth() - playicon.getWidth(), 0, paint);
        }
        return bmp;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 处理圆角的图标
     */
    public static Bitmap mergerIcon(Bitmap background, Bitmap src, int r) {
        Bitmap output = Bitmap.createBitmap(background.getWidth(), background.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff000000;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, background.getWidth(), background.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = r;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(background, rect, rect, paint);
        canvas.drawBitmap(src, rect, rect, paint);
        canvas.save();

        return output;
    }

    /**
     * 处理圆角的图标
     */
    public static Bitmap mergerIcon(Bitmap background, Bitmap src, int r, int x, int y) {
        Bitmap output = Bitmap.createBitmap(background.getWidth(), background.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff000000;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, background.getWidth(), background.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = r;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(background, rect, rect, paint);
        //        canvas.drawBitmap(src, rect, rect, paint);
        canvas.drawBitmap(src, x, y, paint);
        canvas.save();

        return output;
    }

    public static Bitmap createScreenCache(Bitmap src, int startDrawX) {
        if (src == null) {
            return Bitmap.createBitmap(startDrawX, screen_height, Config.ARGB_8888);
        }
        Bitmap bmp = Bitmap.createBitmap(startDrawX, src.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bmp);
        canvas.drawBitmap(src, -(src.getWidth() - startDrawX), 0, null);

        canvas.save();
        return bmp;
    }

    //转码
    public static String toUrlEncode(String str) {
        try {
            if (str == null) return null;
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
        }
        return null;
    }

    //解码
    public static String formUrlEncode(String urlEncodeStr) {
        try {
            if (urlEncodeStr == null) return null;
            return URLDecoder.decode(urlEncodeStr, "UTF-8");
        } catch (Exception e) {
        }
        return null;
    }

    public static void ShowToast(Context context, String text) {
        try {
            if (isAppOnForeground == false) {//home时不提示
                return;
            }
            if (text == null || text.length() <= 0) {
                return;
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
        }
    }

    public static void ShowToast(Context context, String text, boolean islong) {
        try {
            if (isAppOnForeground == false) {//home时不提示
                return;
            }
            if (text == null || text.length() <= 0) {
                return;
            }
            Toast.makeText(context, text, islong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
        }
    }

    //计算摇一摇剩余时间
    public static int getLimitTime(long createTime, long serviceTime) {
        long time = serviceTime - createTime;
        if (time <= 0) return TimerForRock.TIME;
        time = TimerForRock.TIME - time / 60000;
        if (time <= 0) {
            time = 0;
        }

        return (int) time;
    }

    public static void writeMessage(String dir, String from, String from_name, String to, String to_name, String msg, String time) {
        if (from == null) return;
        if (from_name == null) return;
        if (to == null) return;
        if (to_name == null) return;
        if (msg == null) return;
        if (time == null) return;

        try {
            JSONObject jObject = new JSONObject();
            jObject.put("from", from);
            jObject.put("from_name", from_name);
            jObject.put("to", to);
            jObject.put("to_name", to_name);
            jObject.put("msg", msg);
            jObject.put("time", time);
            writeToFileFromEnd(dir, (jObject.toString() + "\r\n").getBytes("utf-8"), false);
        } catch (Exception e) {
        }
    }

    public static void deleteAll(File path) {
        if (path == null) return;
        if (!path.exists()) return;
        try {
            if (path.isDirectory()) {
                File[] child = path.listFiles();
                if (child != null && child.length != 0) {
                    for (int i = 0; i < child.length; i++) {
                        deleteAll(child[i]);
                        child[i].delete();
                    }
                }
            }
            path.delete();
        } catch (Exception e) {
        }
    }

    public static void saveCreateIcon(Context context, boolean iscreate) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.firstrun", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("create", iscreate);
            editor.commit();
        } catch (Exception e) {
        }
    }

    public static boolean getCreateIcon(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.firstrun", Activity.MODE_PRIVATE);
            return sharedPreferences.getBoolean("create", false);
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * <p>
     * 执行一个HTTP POST请求，返回请求响应的HTML
     * </p>
     *
     * @param url    请求的URL地址
     * @param params 请求的查询参数,可以为null
     * @return 返回请求响应的HTML
     * @throws UnsupportedEncodingException
     */
    public static String httpPost(String url, List<NameValuePair> params) {

        HttpPost httpRequest = new HttpPost(url);
        String strResult = "";
        try {
            LogPrint.Print("connect", "httpPost url = " + url);
            LogPrint.Print("connect", "httpPost params = " + params.toString());
            //httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                strResult = EntityUtils.toString(httpResponse.getEntity());
            } else {
                strResult = null;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return strResult;
    }

    //删除某一页面的缓存
    public static void deleteCacheFile(String url, Context context) {
        try {
            String dir = chickResForApn(url, getApnType(context), context);
            File file = new File(dir);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
        }
    }

    //检查是否有高清的资源
    public static String chickResForApn(String url, String apn, Context context) {
        String dir = "";
        if (apn == null) apn = "wifi";

        //拼接deviceid
        if (url.indexOf("?") >= 0) {
            if (url.indexOf("deviceid=") < 0) {
                url += "&deviceid=" + CommonUtil.getIMEI(context);
            }
        } else {
            if (url.indexOf("deviceid=") < 0) {
                url += "?deviceid=" + CommonUtil.getIMEI(context);
            }
        }
        //拼接oid参数
        if (url.indexOf("?") >= 0) {
            if (url.indexOf("oid=") < 0) {
                url += "&oid=" + UserUtil.userid;
            }
        } else {
            if (url.indexOf("oid=") < 0) {
                url += "?oid=" + UserUtil.userid;
            }
        }
        //拼接vid参数
        if (url.indexOf("?") >= 0) {
            if (url.indexOf("vid=") < 0) {
                url += "&vid=" + UserUtil.vid;
            }
        } else {
            if (url.indexOf("vid=") < 0) {
                url += "?vid=" + UserUtil.vid;
            }
        }
        //拼接ver参数
        if (url.indexOf("?") >= 0) {
            if (url.indexOf("ver=") < 0) {
                url += "&ver=" + URLUtil.version;
            }
        } else {
            if (url.indexOf("ver=") < 0) {
                url += "?ver=" + URLUtil.version;
            }
        }
        if (apn.toLowerCase().indexOf("wifi") < 0) {
            String saveUrlString = url;
            for (int i = 0; i < 2; i++) {
                String tmpApn;
                if (i == 0) {
                    tmpApn = "wifi";
                } else {
                    tmpApn = apn;
                }
                //拼接network_type参数
                if (url.indexOf("?") >= 0) {
                    if (url.indexOf("network_type=") < 0) {
                        url += "&network_type=" + tmpApn;
                    }
                } else {
                    if (url.indexOf("network_type=") < 0) {
                        url += "?network_type=" + tmpApn;
                    }
                }
                dir = CommonUtil.dir_cache_page + "/" + CommonUtil.urlToNum(url);
                if (CommonUtil.exists(dir)) {
                    return dir;
                } else {
                    url = saveUrlString;
                }
            }
        }

        //拼接network_type参数
        if (url.indexOf("?") >= 0) {
            if (url.indexOf("network_type=") < 0) {
                url += "&network_type=" + apn;
            }
        } else {
            if (url.indexOf("network_type=") < 0) {
                url += "?network_type=" + apn;
            }
        }
        dir = CommonUtil.dir_cache_page + "/" + CommonUtil.urlToNum(url);

        return dir;
    }

    //调整图片透明度0--100，100是不透明
    public static Bitmap setAlpha(Bitmap sourceImg, int number) {
        int[] argb = new int[sourceImg.getWidth() * sourceImg.getHeight()];
        sourceImg.getPixels(argb, 0, sourceImg.getWidth(), 0, 0, sourceImg
                .getWidth(), sourceImg.getHeight());// 获得图片的ARGB值
        number = number * 255 / 100;
        for (int i = 0; i < argb.length; i++) {
            if ((argb[i] & 0xff) != 0) {
                argb[i] = (number << 24) | (argb[i] & 0x00FFFFFF);// [/i][i]修改最高2[/i][i]位的值
            }
        }
        sourceImg = Bitmap.createBitmap(argb, sourceImg.getWidth(), sourceImg
                .getHeight(), Config.ARGB_8888);
        return sourceImg;
    }

    /**
     * 隐藏键盘
     *
     * @param v
     * @param context
     */
    public static void hideSoftInput(View v, Context context) {
        final InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public static InputStream getNetInputStream(String urlStr) {
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            return is;
        } catch (Exception e) {
            e.printStackTrace();
            LogPrint.Print("exp", "down e =" + e.getMessage());
        }
        return null;
    }

    public static byte[] readStream(InputStream ism) {
        try {
            ByteArrayOutputStream outstream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4 * 1024];
            int len = -1;
            while ((len = ism.read(buffer)) != -1) {
                outstream.write(buffer, 0, len);
            }
            outstream.close();
            return outstream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * byte[] to bitmap
     *
     * @param data
     * @return
     */
    public static Bitmap bytes2Bitmap(byte[] data) {
        if (data != null && data.length > 0) {
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        return null;
    }

    public static Drawable bitmap2Drawable(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        BitmapDrawable bd = new BitmapDrawable(context.getResources(), bitmap);
        return bd;
    }

    public static Bitmap drawable2Bitmap(Context context, Drawable d) {
        BitmapDrawable bd = (BitmapDrawable) d;
        Bitmap bm = bd.getBitmap();
        return bm;
    }

    //转换十六进制
    public static String toHexString(long i, int size) {

        String value = Long.toHexString(i);

        while (value.length() < size) {
            value = "0" + value;
        }

        return value;
    }

    public static String loadHtmlError(Context context) {
        try {
            AssetManager am = null;
            am = context.getAssets();
            InputStream is = am.open("err.html");
            int len = is.available();
            byte[] data = new byte[len];
            is.read(data);
            String errorHtml = new String(data);
            return errorHtml;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    //获取两个字符串中间的字符
    public static String getSubString(String origin, String begin, String end) {
        int startIdx = origin.indexOf(begin) + begin.length();
        int endIdx = origin.indexOf(end);
        if ((startIdx >= 0 && startIdx <= origin.length() - 1) && (endIdx >= 0 && endIdx <= origin.length() - 1)
                && (endIdx > startIdx)) {
            return origin.substring(startIdx, endIdx);
        }
        return "";
    }

    //获取两个字符串中间的字符
    public static String getSubString(String origin, String begin) {
        int startIdx = origin.indexOf(begin) + begin.length();
        if ((startIdx >= 0 && startIdx <= origin.length() - 1)) {
            return origin.substring(startIdx);
        }
        return "";
    }

    /**
     * 生成毫秒级系统时间
     *
     * @return
     */
    public static String createCommentTime() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String strNow = sdf.format(now);
        return strNow;
    }

    /**
     * 验证邮箱
     *
     * @param email
     * @return
     */
    public static boolean checkEmail(String email) {
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$");
        Matcher matcher = pattern.matcher(email);
        return matcher.find();
    }

    public static int parserUserid(String strInteger) {
        int integer = -1;
        try {
            integer = Integer.parseInt(strInteger);
        } catch (Exception e) {
        }
        return integer;
    }

    /**
     * 保存操作手状态
     * 1-左手
     * 2-右手
     *
     * @param context
     * @param hand
     */
    public static void saveOperateHand(Context context, String hand) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.operatehand", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("hand", hand);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得操作手状态
     *
     * @param context
     * @return
     */
    public static String getOperateHand(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.operatehand", Activity.MODE_PRIVATE);
            return sharedPreferences.getString("hand", "0"); //默认righthand
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存视频提示状态
     *
     * @param context
     * @param statu
     */
    public static void savePromptVideoStatu(Context context, String statu) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.promptVideoStatu", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("promptVideo", statu);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取视频提示状态
     *
     * @param context
     * @return
     */
    public static String getPromptVideoStatu(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.promptVideoStatu", Activity.MODE_PRIVATE);
            return sharedPreferences.getString("promptVideo", "0");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存视频手势提示状态
     *
     * @param context
     * @param statu
     */
    public static void savePromptVideoGestureStatu(Context context, String statu) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.promptVideoGestureStatu", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("promptVideoGesture", statu);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取视频手势提示状态
     *
     * @param context
     * @return
     */
    public static String getPromptVideoGestureStatu(Context context) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.cmmobi.promptVideoGestureStatu", Activity.MODE_PRIVATE);
            return sharedPreferences.getString("promptVideoGesture", "0");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //检测service是否运行中
    public static boolean isServiceRunning(Context context, String className) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (RunningServiceInfo service : manager.getRunningServices(1000)) {
                if (className.equals(service.service.getClassName())) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    //检测软件进程是否运行中
    public static boolean isAppRunning(Context context, String packageName) {
        try {
            ActivityManager mActivityManager = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningAppProcessInfo> run = mActivityManager
                    .getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo pro : run) {
                if (pro.processName.equals(packageName)) {
                    return true;
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断当前app是否在前台
     *
     * @param context
     * @return
     */
    public static boolean isAppOnForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = context.getPackageName();
        List<RecentTaskInfo> appTask = manager.getRecentTasks(Integer.MAX_VALUE, 1);
        if (appTask != null) {
            boolean result = appTask.get(0).baseIntent.toString().contains(packageName);
            LogPrint.Print("media", "isAppOnForeground = " + result);
            return result;
        }
        LogPrint.Print("media", "isAppOnForeground = false");
        return false;
    }

    public static byte bool2Byte(boolean bool) {
        if (bool) {
            return 1;
        }
        return 0;
    }

    public static boolean byte2Bool(byte bt) {
        if (bt == 1) {
            return true;
        }
        return false;
    }

}
