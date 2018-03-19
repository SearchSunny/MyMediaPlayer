/**
 * 
 */
package com.mymediaplayer.plug;

import android.content.Context;

import com.mymediaplayer.R;
import com.mymediaplayer.connction.HttpThread;
import com.mymediaplayer.utils.CommonUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;

/**
 *	视频下载蚂蚁
 */
public class MediaAnts extends Thread {

	private byte[] buffer;
	private static String proxyAddr = "10.0.0.172";
	private final static int proxyPort = 80;
	private int APN;// 0 wifi 1 cmwap 2 cmnet
	private int curPos = 0;
	public static final int BUFFER = 1024;// 一次读1K
	private int content_length;
	
	private Context context;
	private String urlString;
	private MediaAntsListener listener;
	private int threadIndex;
	private int start;//下载开始点
	private int end;//下载结束点
	
	private boolean isStop;
	private boolean onlyGetLength;//是否仅获取长度
	
	public MediaAnts(Context context, String url, MediaAntsListener listener, int threadIndex, int start, int end){
		this.context = context;
		this.urlString = url;
		this.listener = listener;
		this.threadIndex = threadIndex;
		this.start = start;
		this.end = end;
		isStop = false;
		this.onlyGetLength = false;
		start();
	}
	
	public MediaAnts(Context context, String url, MediaAntsListener listener, int threadIndex){
		this.context = context;
		this.urlString = url;
		this.listener = listener;
		this.threadIndex = threadIndex;
		isStop = false;
		this.onlyGetLength = true;
		start();
	}
	
	public void stopDownLoad(){
		isStop = true;
	}
	
	/**
	 * 判断是否推送页面(text/vnd.wap.wml)
	 * 
	 * @param contentType
	 * @return
	 */
	private boolean isMobileWap(String contentType) {
		if (contentType == null)
			return false;
		if (contentType.indexOf("text/vnd.wap.wml") >= 0) {
			return true;
		}
		return false;
	}
	
	private void connect(){
		try {
			boolean isNetWorkOpen = CommonUtil.isNetWorkOpen(context);
			if(isNetWorkOpen){
				String temp = CommonUtil.getApnType(context);
				if (temp.toLowerCase().indexOf("ct") >= 0) {
					proxyAddr = "10.0.0.200";
				}
				if (temp.toLowerCase().indexOf("wifi") >= 0) {
					APN = 0;
				} else if (temp.toLowerCase().indexOf("wap") >= 0) {
					APN = 1;
				} else {
					APN = 2;
				}
			}
			URL url = new URL(urlString);
			HttpURLConnection httpConnection = null;
			Proxy proxy;
			switch (APN) {
			case 0:// wifi
			case 2:// cmnet
				httpConnection = (HttpURLConnection)url.openConnection();
				break;
			case 1:// cmwap
				proxy = new Proxy(Type.HTTP,new InetSocketAddress(proxyAddr, proxyPort));
				httpConnection = (HttpURLConnection)url.openConnection(proxy);
				break;
			}
			if(!onlyGetLength){
				httpConnection.setRequestProperty("Range", "bytes="
						+ start + "-" + end);
			}
			httpConnection.setConnectTimeout(6000);
			if (isMobileWap(httpConnection.getContentType())) {
				connect();
				return;
			}
			content_length = httpConnection.getContentLength();
			listener.onSetSize(content_length, threadIndex);
			
			if(onlyGetLength)return;//如果只获取长度，则直接退出
			
			InputStream in = httpConnection.getInputStream();
			if(in != null){
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				curPos = 0;
				byte[] data = new byte[BUFFER];
				int bytesRecv = -1;
				int bufSize = BUFFER;
				int l = 0;
				while (bytesRecv != 0) {
					if(isStop){
						listener.onFinish(null, 0, false, threadIndex,start,end);
						return;
					}
					if ((bytesRecv = in.read(data, 0, bufSize)) < 0) {
						break;
					}
					baos.write(data, 0, bytesRecv);
					curPos += bytesRecv;
					
				}
				l = baos.size();
				if (l > 0) {
					buffer = baos.toByteArray();
					baos.close();
					if (curPos >= content_length){
						listener.onCurDataPos(curPos, threadIndex);
						listener.onFinish(buffer, l, true, threadIndex,start,end);
					}
					else{
						listener.onFinish(null, 0, false, threadIndex,start,end);
					}
				}
			}
		} catch (Exception e) {
			listener.onFinish(null, 0, false, threadIndex,start,end);// 下载结束
			listener.onError(102, context.getString(R.string.net_busying),threadIndex, HttpThread.TYPE_PAGE);
		}
	}
	
	public void run(){
		connect();
	}
}
