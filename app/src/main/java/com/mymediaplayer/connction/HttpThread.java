package com.mymediaplayer.connction;

import android.content.Context;

import com.mymediaplayer.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.StringTokenizer;

import com.mymediaplayer.utils.CommonUtil;
import com.mymediaplayer.utils.LogPrint;

/**
 *
 */

public class HttpThread extends Thread{

    private Context mContext;
    /** 线程开关，当线程已经被关闭时，该变量将被设置为true,以应该线程延迟关闭会写入数据的现象 */
    public boolean isStop = false;

    public static final int BUFFER = 1024;// 一次读1K

    private String host = "";
    private int port;
    private String other_url = "";

    /** 最大写入数据 */
    public final static int MAX_WRITE_LEN_WIFI = 50 * 1024;
    public final static int MAX_WRITE_LEN_WAP = 10 * 1024;
    public final static int MAX_WRITE_LEN_NET = 30 * 1024;
    private static final int MAX_RETRY = 5;

    /** 下载地址缓冲 */
    private String urlBuffer;
    /** 下载缓冲区 */
    private byte[] buffer;
    /** 接口对象 */
    private HttpListener listener;
    /** 下载类型 */
    private int type;
    /** 重试次数 */
    private int mRetry;
    private int mMaxLen_Finish = MAX_WRITE_LEN_WIFI;
    public final static int TYPE_PAGE = 0;
    public final static int TYPE_IMAGE = 1;
    public final static int TYPE_FILE = 2;
    public final static int TYPE_COVER = 3;
    public final static int TYPE_JSON = 4;
    public final static int TYPE_MESSAGE_GET = 5;  //消息轮询by lyb-3.11

    private Socket socket;

    private int threadIndex;
    private int method;
    public final static String GET = "GET";
    protected final static String POST = "POST";

    private int curPos = 0;
    private int fileOffset_Start = 0; // 当前线程开始工作前文件已经下载的长度，用于在下载失败后重新请求url的offset计算，fileOffset_start+curPos
    // 就是新Offset
    private String content_type;
    private int content_length;

    private boolean isNetWorkOpen;
    private int APN;// 0 wifi 1 cmwap 2 cmnet
    private final static String wifi = "wifi";
    private final static String cmwap = "wap";
//	private final static String cmnet = "net";

    private static String proxyAddr = "10.0.0.172";
    private final static int proxyPort = 80;

    private long startTime;//联网开始的时间

    private final static int TIMEOUT_WIFI = 3000;
    private final static int TIMEOUT_WAP = 5000;
    private final static int TIMEOUT_NET = 5000;

//  	public HttpThread(Context context, String url, int type, HttpListener listener) {
//
//	}

    public HttpThread(Context context, String url, int fileOffset, int type, HttpListener listener, int index) {
        threadIndex = index;
        fileOffset_Start = fileOffset;
        startTime = System.currentTimeMillis();
        LogPrint.Print("speed", "*******************start***************");
        LogPrint.Print("speed", "********url = "+url);
        mContext = context;
        this.type = type;
        this.listener = listener;
        curPos = 0;
        content_type = null;
        content_length = 0;
        if (url == null) {
            if(type == TYPE_PAGE){
                listener.onError(102, "请求的url为null",threadIndex,type);
            }else if(type == TYPE_IMAGE){
                listener.onImageError(102, "请求的url为null",threadIndex);
            }
            // LogPrint.Print("tag", "url is null");
            return;
        } else {
            listener.onCurDataPos(curPos,0);
            if (getUrlInfo(url)) {
                isNetWorkOpen = CommonUtil.isNetWorkOpen(mContext);

                LogPrint.Print("connect", "isNetWorkOpen = "+isNetWorkOpen);
                if (isNetWorkOpen) {
                    String temp = CommonUtil.getApnType(mContext);
                    if (temp.toLowerCase().indexOf("ct") >= 0) {
                        proxyAddr = "10.0.0.200";
                    }
                    if (temp.toLowerCase().indexOf(wifi) >= 0) {
                        APN = 0;
                        mMaxLen_Finish = MAX_WRITE_LEN_WIFI;
                        //MessageReceiveService.CHICK_TIME = 30000;
                        LogPrint.Print("connect","WIFI");
                    } else if (temp.toLowerCase().indexOf(cmwap) >= 0) {
                        APN = 1;
                        mMaxLen_Finish = MAX_WRITE_LEN_WAP;
                        //MessageReceiveService.CHICK_TIME = 600000;
                        LogPrint.Print("connect","CMWAP");
                    } else {
                        APN = 2;
                        mMaxLen_Finish = MAX_WRITE_LEN_NET;
                        //MessageReceiveService.CHICK_TIME = 600000;
                        LogPrint.Print("connect","CMNET");
                    }
                    start();
                    listener.onApnType(temp, threadIndex);
                } else {
                    if(type == TYPE_PAGE){
                        listener.onError(102, context.getString(R.string.reminder_no_net),threadIndex,type);
                    }else if(type == TYPE_IMAGE){
                        listener.onImageError(102, context.getString(R.string.reminder_no_net),threadIndex);
                    }
                    // LogPrint.Print("tag", "net not open");
                }
            }
        }
    }

    private SocketAddress isa = null;

    protected void startConnect(String requestType) {
        try {
            LogPrint.Print("connect", "startConnect");
            if (isStop)
                return;

            StringBuffer sb = new StringBuffer();
            switch (APN) {
                case 0:// wifi
                    socket = new Socket();
                    isa = new InetSocketAddress(host, port);
                    if (!socket.isConnected()) {
                        socket.connect(isa, TIMEOUT_WIFI);
                    }
                    sb.append(requestType + " " + other_url + " HTTP/1.1\r\n");
                    sb.append("Host: " + host + ":" + port + "\r\n");
                    break;
                case 1:// cmwap
                    socket = new Socket();
                    isa = new InetSocketAddress(proxyAddr, proxyPort);
                    if (!socket.isConnected()) {
                        socket.connect(isa, TIMEOUT_WAP);
                    }
                    sb.append(requestType + " " + urlBuffer + " HTTP/1.1\r\n");
                    sb.append("Host: " + proxyAddr + ":80\r\n");
                    break;
                case 2:// cmnet
                    socket = new Socket();
                    isa = new InetSocketAddress(host, port);
                    if (!socket.isConnected()) {
                        socket.connect(isa, TIMEOUT_NET);
                    }
                    sb.append(requestType + " " + other_url + " HTTP/1.1\r\n");
                    sb.append("Host: " + host + ":" + port + "\r\n");
                    break;
            }
            sb.append("Accept: */*\r\n");
            sb.append("Range: bytes="+(fileOffset_Start + curPos)+"-\r\n");
            sb.append("User-Agent: MAUI WAP Browser\r\n\r\n");
            OutputStream out = socket.getOutputStream();
            out.write(sb.toString().getBytes("UTF-8"));
//			start();
            LogPrint.Print("connect", "startConnectOver");
        } catch (Exception ex) {
            closeSocket();
            if (mRetry++ < MAX_RETRY) {
                reconnectSocket();
                return;
            }
            listener.onFinish(buffer, curPos, false, threadIndex);// 下载结束
            if(type == TYPE_PAGE){
                listener.onError(102, mContext.getString(R.string.net_busying),threadIndex,type);
            }else if(type == TYPE_IMAGE){
                listener.onImageError(102, mContext.getString(R.string.net_busying),threadIndex);
            }
            // LogPrint.Print("tag","retry > max");
            mRetry = 0;
        } finally {
        }
    }

    // 获得host,port,other
    private boolean getUrlInfo(String url) {
        int hoststart;
        int portstart;
        int otherstart;
        if (url.startsWith("http://")) {
            hoststart = 7;
        } else {
            hoststart = 0;
        }
        // 错误地址
        if (url.indexOf("xxx", hoststart) >= 0) {// http://xxxxxx
            return false;
        }
        if(url.indexOf(":",hoststart) > 0){
            int pos = url.indexOf(":",hoststart);
            int pos1 = url.indexOf("/",hoststart);
            //:间没有/才是正确的
            if(pos < pos1){
                host = url.substring(hoststart, (portstart = url.indexOf(":",hoststart)));
                String temp = url.substring(portstart+1, (otherstart = url.indexOf(File.separator, portstart)));
                if(temp==null||temp.equals("")){
                    temp="80";
                }
                port = Integer.parseInt(temp);

                other_url = url.substring(otherstart);
            }else{
                if(url.indexOf(File.separator,hoststart) > 0){
                    host = url.substring(hoststart, (portstart = url.indexOf(File.separator,hoststart)));
                    otherstart = url.indexOf(File.separator, portstart);
                    port = 80;
                    other_url = url.substring(otherstart);
                }else{
                    host = url.substring(hoststart);
                    port = 80;
                    other_url = "";
                }
            }
        }else {
            if(url.indexOf(File.separator,hoststart) > 0){
                host = url.substring(hoststart, (portstart = url.indexOf(File.separator,hoststart)));
                otherstart = url.indexOf(File.separator, portstart);
                port = 80;
                other_url = url.substring(otherstart);
            }else{
                host = url.substring(hoststart);
                port = 80;
                other_url = "";
            }
        }

//		if(!URLUtil.IS_LOCAL_NETSTATE){
//			host = "116.90.82.241";
//			port = 58080;
//		}
        LogPrint.Print("connect", "host = "+host);
        LogPrint.Print("connect", "port = "+port);
        LogPrint.Print("connect", "other_url = "+other_url);
        urlBuffer = url;
        return true;
    }

    /**
     * 判断是否推送页面(text/vnd.wap.wml)
     *
     * @param contentType
     * @return
     */
    public boolean isMobileWap(String contentType) {
        if (contentType == null)
            return false;
        if (contentType.indexOf("text/vnd.wap.wml") >= 0) {
            return true;
        }
        return false;
    }

    private void waitResponse() {
        boolean done = false;
        int l = 0;
        int i = 0;
        int t = 0;
        int count = 0;// 64K记数
        try {
            if (socket.isConnected() && !socket.isClosed()) {
                //lyb 增加链接超时判断。
                socket.setSoTimeout(10000); //	10s超时判断
                InputStream in = socket.getInputStream();
                int tmplen = in.available();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // --------------------读取Head开始---------------------
                if (in != null) {
                    while (!done) {
                        l = in.read();
                        if (l < 0)
                            break;
                        switch (l) {
                            case '\r':
                                break;
                            case '\n':
                                if (i == 0)
                                    done = true;
                                i = 0;
                                break;
                            default:
                                i++;
                                break;
                        }
                        baos.write(l);
                        if (i > 100000)
                            done = true;
                    }

                    if (l < 0) {
                        reconnectSocket();
                        return;
                    }
                    // --------------------读取Head结束---------------------

                    // --------------------解析code开始---------------------
                    StringReader stringReader = new StringReader(baos.toString());
                    StringTokenizer st = null;
                    String right = "100";
                    String left;
                    while (right.equals("100")) {
                        st = null;
                        st = getLineForStream(stringReader);
                        right = getTokenizer(st, 1);

                        if (right.equals("100")) {
                            right = "0";
                            st = getLineForStream(stringReader);
                        }
                    }

                    if(Integer.parseInt(right) > 400){
                        LogPrint.Print("connect", "error: code > 400");
                        listener.onFinish(null, 0, false, threadIndex);// 下载结束
                        if(type == TYPE_IMAGE){
                            listener.onImageError(102, mContext.getString(R.string.net_busying),threadIndex);
                        }else if(type == TYPE_PAGE){
                            listener.onError(102, mContext.getString(R.string.net_busying),threadIndex,type);
                        }
                        closeSocket();
                        return;
                    }

                    if (!"200".equals(right) && !"301".equals(right) && !"302".equals(right) && !"206".equals(right)) {
                        closeSocket();
                        if ("0".equals(right) && mRetry++ < MAX_RETRY) {
                            reconnectSocket();
                            return;
                        }
                        mRetry = 0;
                        return;
                    }
                    // --------------------解析code结束---------------------

                    // --------------------解析head开始---------------------
                    while (true) {
                        st = getLineForStream(stringReader);
                        if (st == null || "\r".equals(st.toString()))
                            break;
                        left = getTokenizer(st, 0);

                        if (left != null) {
                            if (left.compareToIgnoreCase("Content-Length:") == 0) {
                                right = getTokenizer(st, 0);
                                if (right != null) {
                                    content_length = Integer.parseInt(right);
                                    if(0==(fileOffset_Start+curPos))
                                        listener.onSetSize(content_length,threadIndex);
                                }
                            } else if (left.compareToIgnoreCase("Content-Type:") == 0) {
                                right = getTokenizer(st, 0);
                                if (right != null) {
                                    content_type = right;
                                    listener.onGetContentType(content_type,threadIndex);
                                }
                            } else if (left.compareToIgnoreCase("Connection:") == 0) {
                                right = getTokenizer(st, 0);
                            }
                        }
                    }
                    listener.onGetUrl(urlBuffer, type,threadIndex);

                    // --------------------解析Head结束---------------------

                    if (isMobileWap(content_type)) { // 遇到移动推送页 重连
                        closeSocket();
                        reconnectSocket();
                        return;
                    }

                    // --------------------读取消息实体---------------------
                    baos.reset();
                    i = content_length;
                    byte[] data = new byte[BUFFER];
                    int bytesRecv = -1;
                    int bufSize = BUFFER;
                    if (content_length != 0) { // Http头里指定了消息实体的长度
                        while (i != 0) {
                            if (isStop) {
                                closeSocket();
                                return;
                            }

                            if (i < BUFFER)
                                bufSize = i;
                            if ((bytesRecv = in.read(data, 0, bufSize)) == 0) {
                                break;
                            } else if (bytesRecv < 0) {
                                // LogPrint.Print("tag", "recv <0");
                                closeSocket();
                                if (mRetry++ < MAX_RETRY) { // 发生了超时之类的 重启连接
                                    reconnectSocket();
                                    return;
                                }
                                mRetry = 0;
                                return;
                            }

                            i -= bytesRecv;
//							LogPrint.Print("==================down Book Size================= :", "bytesRecv:" + bytesRecv);
                            baos.write(data, 0, bytesRecv);
                            curPos += bytesRecv;
                            listener.onCurDataPos(curPos, threadIndex);

                            if (type == TYPE_FILE) { // 32KB OnFinish一次
                                t = curPos / mMaxLen_Finish;
                                if (t > count) {
                                    l = baos.size();
                                    buffer = baos.toByteArray();
                                    baos.reset();
                                    listener.onCurDataPos(curPos, threadIndex);
                                    listener.onFinish(buffer, l, false, threadIndex);
                                    count = t;
                                }

                            }
                        }
                    } else { // 没有指定Content-Length 需要判断是否是chunked（我们服务器没有进行GZip
                        // 等压缩 无需要）
                        while (bytesRecv != 0) {
                            if (isStop) {
                                closeSocket();
                                return;
                            }
                            if ((bytesRecv = in.read(data, 0, bufSize)) < 0) {
                                // LogPrint.Print("tag", "recv < 0");
                                closeSocket();
                                if (mRetry++ < 2) { // 发生了超时之类的 重启连接
                                    reconnectSocket();
                                    return;
                                }
                                mRetry = 0;
                                break;
                            }
                            baos.write(data, 0, bytesRecv);
                            curPos += bytesRecv;

                            if (type == TYPE_FILE) { // 32KB OnFinish一次
                                i = curPos / mMaxLen_Finish;
                                if (i > count) {
                                    l = baos.size();
                                    buffer = baos.toByteArray();
                                    baos.reset();
                                    listener.onCurDataPos(curPos, threadIndex);
                                    listener.onFinish(buffer, l, false, threadIndex);
                                    count = i;
                                }

                            }
                        }
                    }
                    closeSocket();

                    l = baos.size();
                    if (l > 0) {
                        buffer = baos.toByteArray();
                        baos.reset();
                        if (curPos == content_length){
                            listener.onCurDataPos(curPos, threadIndex);
                            listener.onFinish(buffer, l, true, threadIndex);
                        }
                        else{
                            listener.onCurDataPos(curPos, threadIndex);
                            listener.onFinish(buffer, l, false, threadIndex);
                        }
                        count = i;
                    }
                    LogPrint.Print("connect", "downLoadOver");
                    LogPrint.Print("speed", "********download over = "+(System.currentTimeMillis()-startTime));
                    LogPrint.Print("speed", "***************over*************");
                    // if(isClose) //如果不支持长连接
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            listener.onFinish(null, 0, false, threadIndex);// 下载结束
            if(type == TYPE_PAGE){
                listener.onError(102, mContext.getString(R.string.net_busying),threadIndex,type);
            }else if(type == TYPE_IMAGE){
                listener.onImageError(102, mContext.getString(R.string.net_busying),threadIndex);
            }
            // LogPrint.Print("tag", "send error");
            closeSocket();
        } finally {
            isStop = true;
        }

    }

    /**
     * 从Stream读取一行 并构造StringTokenizer
     *
     * @param reader
     * @return
     */
    private StringTokenizer getLineForStream(Reader reader) {
        int c = 0;
        StringBuffer buf = new StringBuffer();

        try {
            while ((c = reader.read()) > 0) {
                if (c == '\n') {
                    String line = buf.toString();
                    return new StringTokenizer(line);
                }
                buf.append((char) c);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 从StringTokenizer中读取后续String
     *
     * @param st
     * @param index
     *            向后查找多少位，从0开始
     * @return 如果未找到返回null
     */
    private String getTokenizer(StringTokenizer st, int index) {
        int i = 0;
        while (st.hasMoreTokens()) {
            if (i >= index)
                return st.nextToken();
            st.nextToken();
            i++;
        }
        return null;
    }

    /**
     * 重连Socket
     */
    private void reconnectSocket() {
        startConnect(method == 0 ? GET : POST);
    }

    /**
     * 关闭Socket
     */
    public void closeSocket() {
        try {
            if (!socket.isInputShutdown() || !socket.isOutputShutdown() || !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ex) {
        }
    }

    public boolean isClosed() {
        if (socket == null) {
            return true;
        }
        return socket.isClosed();
    }

    public void run() {
        startConnect(GET);
        LogPrint.Print("speed", "********hand over = "+(System.currentTimeMillis()-startTime));
        LogPrint.Print("connect", "downLoadStart");
        waitResponse();
    }

}
