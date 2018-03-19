/**
 * 
 */
package com.mymediaplayer.plug;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.mymediaplayer.utils.CommonUtil;
import com.mymediaplayer.utils.LogPrint;
import com.mymediaplayer.utils.MessageID;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

/**
 *用于视频文件的加载相关逻辑
 *实现逻辑：一个主文件，多个蚂蚁，当蚂蚁满足下载量时写入主文件。播放时使用主文件
 */
public class MediaLoader implements MediaAntsListener{

	private Context context;
	
	private String url;
	private Handler handler;
	
//	private final static int READY_SIZE = 500*1024;//允许播放的大小
	public final static int CACHE_SIZE = 200*1024;//缓冲大小，满足缓冲大小时写入主文件
	private final static int ANTS_NUM = 5;//蚂蚁数量
	
	private MediaDataBase dataBase;
	
	private RandomAccessFile rAccess;//主文件
	private Ants[] ants;//蚂蚁
	private PartFileInfo[] infos;//片段文件信息
	
	public MediaLoader(Context context, String url, Handler handler){
		this.url = url;
		this.handler = handler;
		this.context = context;
		//创建视频目录
		CommonUtil.createDirs(CommonUtil.dir_media, true,context);
		dataBase = new MediaDataBase(context, MediaDataBase.DB_NAME, null, MediaDataBase.DB_VERSION);
		LogPrint.Print("media","MediaLoader create");
	}
	
	public void start(){
		try {
			File file = new File(CommonUtil.dir_media+"/"+CommonUtil.urlToNum(getUrl(url))+".mp4");
			//检查视频文件是否存在
			if(file.exists()){//存在
				if(dataBase.isDownLoadOver(CommonUtil.urlToNum(getUrl(url)))){//文件是否已经下载完成
					LogPrint.Print("media","file complete ready play");
					handler.sendEmptyMessage(MessageID.MESSAGE_MEDIAPLAYER_READY);//发送允许播放通知
					Message message = new Message();
					message.what = MessageID.MESSAGE_MEDIAPLAYER_BUFFERUPDATE;
					message.arg1 = 100;
					handler.sendMessage(message);//发送缓存进度
				}else{
					LogPrint.Print("media","file uncomplete");
					String mediaInfo = getMediaInfo(context, CommonUtil.urlToNum(getUrl(url)));
					//判断片段信息是否存在
					if(mediaInfo.length() > 0){
						readPartFileInfo((int)file.length(), mediaInfo);
						boolean isReady = true;
						for(int i = 0;infos != null&&i < 3;i ++){
							if(infos[i].isDownLoadOver != 2){
								isReady = false;
								break;
							}
						}
						int count = 0;
						for(int i = 0;infos != null&&i < infos.length;i ++){
							if(infos[i].isDownLoadOver == 2){
								count ++;//最小缓存进度
							}else{
								break;
							}
						}
						Message message = new Message();
						message.what = MessageID.MESSAGE_MEDIAPLAYER_BUFFERUPDATE;
						message.arg1 = (count*1000)/(infos.length*10);
						handler.sendMessage(message);//发送缓存进度
						if(isReady&&infos!=null){
							LogPrint.Print("media","file download 500k ready play");
							handler.sendEmptyMessage(MessageID.MESSAGE_MEDIAPLAYER_READY);//发送允许播放通知
						}
						findAntsToWork();
					}else{
						createPartFileInfo((int)file.length());
						findAntsToWork();
					}
					saveMediaInfo(context, CommonUtil.urlToNum(getUrl(url)), buildMediaInfoString());
				}
			}else{//不存在
				LogPrint.Print("media","create empty file");
				new MediaAnts(context, url, this, 101010);//获取数据长度
				//后续逻辑在onSetSize里执行
			}
		} catch (Exception e) {
			handler.sendEmptyMessage(MessageID.MESSAGE_MEDIAPLAYER_LOAD_ERROR);//加载错误发送通知
		}
	}
	
	//进行一个可用的下载
	private void findAntsToWork(){
		if(ants == null){
			ants = new Ants[ANTS_NUM];
		}
		boolean isfind = false;
		for(int i = 0;infos != null&&i < infos.length;i ++){
			isfind = false;
			if(infos[i].isDownLoadOver == 0){
				for(int j = 0;j < ANTS_NUM;j ++){
					if(ants[j] == null||!ants[j].isUsed){//空闲时
						ants[j] = new Ants(context, url, this, i, infos[i].start, infos[i].end);
						isfind = true;
						infos[i].isDownLoadOver = 1;
						LogPrint.Print("media","findAntsToWord:ants["+j+"]("+i+") start");
						break;
					}
				}
				if(isfind == false)break;//没有找到任何空闲蚂蚁的时候直接结束
			}
		}
	}
	
	//创建partfileinfo
	private void createPartFileInfo(int length){
		int size = length%CACHE_SIZE==0?length/CACHE_SIZE:length/CACHE_SIZE+1;
		infos = new PartFileInfo[size];
		
		for (int i = 0; i < infos.length; i++) {
			infos[i] = new PartFileInfo();
			infos[i].start = i*CACHE_SIZE+1;
			infos[i].end = (i+1)*CACHE_SIZE;
			//特殊处理第一个
			if(infos[i].start == 1)infos[i].start = 0;
			//特殊处理最后一个
			if(infos[i].end >= length)infos[i].end = length;
			infos[i].isDownLoadOver = 0;
			LogPrint.Print("media","createPartFileInfo:infos["+i+"] = "+infos[i].start+"|"+infos[i].end+"|"+infos[i].isDownLoadOver);
		}
	}
	
	//从存储中读取partfileinfo
	private void readPartFileInfo(int length,String str){
		String[] tmp = str.split("#");
		if(tmp.length > 0){
			infos = new PartFileInfo[tmp.length];
			
			for (int i = 0; i < infos.length; i++) {
				infos[i] = new PartFileInfo();
				infos[i].start = i*CACHE_SIZE+1;
				infos[i].end = (i+1)*CACHE_SIZE;
				//特殊处理第一个
				if(infos[i].start == 1)infos[i].start = 0;
				//特殊处理最后一个
				if(infos[i].end >= length)infos[i].end = length;
				infos[i].isDownLoadOver = Integer.parseInt(tmp[i]);
				LogPrint.Print("media","readPartFileInfo:infos["+i+"] = "+infos[i].start+"|"+infos[i].end+"|"+infos[i].isDownLoadOver);
			}
		}
	}
	
	//拼接
	private String buildMediaInfoString(){
		String result = "";
		for(int i = 0;infos != null&&i < infos.length;i ++){
			if(i == infos.length-1){
				result += infos[i].isDownLoadOver;
			}else{
				result += infos[i].isDownLoadOver+"#";
			}
		}
		return result;
	}
	
	//保存每个蚂蚁下载的文件是否下载完成
	public static void saveMediaInfo(Context context, String url, String str){
    	try {
			SharedPreferences sharedPreferences = context.getSharedPreferences("com.mediainfo", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			LogPrint.Print("media","saveMediaInfo:"+str);
			editor.putString(url, str);
			editor.commit();
		} catch (Exception e) {
		}
    }
	
	public static String getMediaInfo(Context context, String url){
    	try {
    		SharedPreferences sharedPreferences = context.getSharedPreferences("com.mediainfo", Activity.MODE_PRIVATE);
    		String result = sharedPreferences.getString(url, "");
    		LogPrint.Print("media","getMediaInfo:"+result);
    		return result;
		} catch (Exception e) {
		}
		return "";
    }
	
	public static void removeMediaInfo(Context context, String url){
		SharedPreferences sharedPreferences = context.getSharedPreferences("com.mediainfo", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.remove(url);
	}
	
	public void stopAllAnts(){
		for(int i = 0;ants != null&&i < ants.length;i ++){
			if(ants[i]!=null&&ants[i].isUsed){
				ants[i].mediaAnts.stopDownLoad();
				LogPrint.Print("media","ants["+i+"] stop");
			}
		}
	}
	
	//检查sd卡容量，最大50m，超过的删除部分，按时间顺序删
	public void chickSdCard(int length){
		try {
			int curLen = 0;
			int maxLen = 50*1024*1024;//50M
			
			File file = new File(CommonUtil.dir_media);
			File[] listFiles = file.listFiles();
			if(listFiles != null&&listFiles.length > 0){
				for(int i = 0;i < listFiles.length;i ++){
					curLen += listFiles[i].length();
				}
				
				if(curLen+length >= maxLen){
					//冒泡排序
					for(int i = 0;i < listFiles.length-1;i ++){
						for(int j = 0 ;j < listFiles.length - i - 1; j++){
							if(listFiles[j].lastModified() > listFiles[j+1].lastModified()){
								File tFile = listFiles[j];
								listFiles[j] = listFiles[j+1];
								listFiles[j+1] = tFile;
							}
						}
					}
					//清除空间，提供可用容量
					int index = 0;
					while(curLen+length >= maxLen){
						curLen -= listFiles[index].length();
						listFiles[index].delete();
						index ++;
						if(index >= listFiles.length){
							index = 0;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
		}
	}
	
	public static String getUrl(String url){
		if(url == null)return null;
		if(url.length() <= 0)return "";
		if(url.indexOf("?") > 0){
			return url.substring(0,url.indexOf("?"));
		}else{
			return url;
		}
	}
	
	@Override
	public void onCurDataPos(int dataPos, int index) {

	}

	@Override
	public void onError(int code, String message, int index, int type) {

	}

	@Override
	public void onFinish(byte[] data, int size, boolean isOver, int index,
			int start, int end) {
		synchronized(this){
			Message message = new Message();
			message.what = 222222;
			Bundle bundle = new Bundle();
			bundle.putByteArray("data", data);
			bundle.putInt("size", size);
			bundle.putBoolean("isOver", isOver);
			bundle.putInt("index", index);
			bundle.putInt("start", start);
			bundle.putInt("end", end);
			message.setData(bundle);
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onGetContentType(String contentType, int index) {

	}

	@Override
	public void onGetUrl(String url, int type, int index) {

	}

	@Override
	public void onSetSize(int size, int index) {
		Message message = new Message();
		message.what = 111111;
		message.arg1 = size;
		message.arg2 = index;
		mHandler.sendMessage(message);
	}
	
	//蚂蚁，用于分段下载
	public class Ants{
		boolean isUsed;//是否使用中
		MediaAnts mediaAnts;
		int index;
		
		public Ants(Context context, String url, MediaAntsListener listener, int threadIndex, int start, int end){
			isUsed = true;
			index = threadIndex;
			mediaAnts = new MediaAnts(context, url, listener, threadIndex, start, end);
		}
	}
	
	//用于记录拆分的文件的信息
	public class PartFileInfo{
		int start;//记录切分数据的开始点
		int end;//记录切分数据的结束点
		int isDownLoadOver;//记录当前片段是否下载完成,0:未完成,1:下载中,2:下载完成
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 111111://onsetsize
				try {
					int size = msg.arg1;
					int index = msg.arg2;
					//文件长度
					if(size > 0&&index == 101010){
						LogPrint.Print("media","Content-Length = "+size);
						chickSdCard(size);
						//创建一个等长的空文件，便于蚂蚁片段写入
						byte[] data = new byte[1024*100];
						File file= new File(CommonUtil.dir_media+"/"+CommonUtil.urlToNum(getUrl(url))+".mp4");
						if(!file.exists()){
							file.createNewFile();
						}
						FileOutputStream fos = new FileOutputStream(file);
						boolean runing = true;
						int len = size;
						while(runing){
							if(len < 0)break;
							if(len - data.length > 0){
								fos.write(data, 0, data.length);
								len -= data.length;
							}else{
								fos.write(data, 0, len);
								runing = false;
							}
						}
						fos.close();
						//写入数据库
						if(dataBase.isInDB(CommonUtil.urlToNum(getUrl(url)))){
							dataBase.deleteDB(CommonUtil.urlToNum(getUrl(url)));
						}
						dataBase.insertDB(CommonUtil.urlToNum(getUrl(url)));
						//下载
						createPartFileInfo(size);
						findAntsToWork();
						saveMediaInfo(context, CommonUtil.urlToNum(getUrl(url)), buildMediaInfoString());
					}
				} catch (Exception e) {
					LogPrint.Print("media","onSetSize: error = "+e.toString());
					handler.sendEmptyMessage(MessageID.MESSAGE_MEDIAPLAYER_LOAD_ERROR);//加载错误发送通知
				}
				break;
			case 222222://onfinish
				byte[] data = msg.getData().getByteArray("data");
				int size = msg.getData().getInt("size");
				boolean isOver = msg.getData().getBoolean("isOver");
				int index = msg.getData().getInt("index");
				int start = msg.getData().getInt("start");
				int end = msg.getData().getInt("end");
				LogPrint.Print("media","onFinish("+index+") "+start+"-"+end+" len = "+(data==null?0:data.length));
				if(data == null||size <= 0){
					//出问题时需要重置参数
					if(infos != null){
						infos[index].isDownLoadOver = 0;
						for(int i = 0;i < ants.length;i ++){
							if(ants[i].index == index){
								ants[i].isUsed = false;
								break;
							}
						}
						saveMediaInfo(context, CommonUtil.urlToNum(getUrl(url)), buildMediaInfoString());
					}
					return;
				}
				try {
					if(rAccess == null){
						File file= new File(CommonUtil.dir_media+"/"+CommonUtil.urlToNum(getUrl(url))+".mp4");
						rAccess = new RandomAccessFile(file, "rwd");
					}
					//写入文件
					rAccess.seek(start);
					rAccess.write(data, 0, data.length);
					//下载完修改状态
					infos[index].isDownLoadOver = 2;
					for(int i = 0;i < ants.length;i ++){
						if(ants[i].index == index){
							ants[i].isUsed = false;
							break;
						}
					}
					//保存infos值
					saveMediaInfo(context, CommonUtil.urlToNum(getUrl(url)), buildMediaInfoString());
					//判断是否可播放
					boolean isReady = true;
					for(int i = 0;infos != null&&i < 3;i ++){
						if(infos[i].isDownLoadOver != 2){
							isReady = false;
							break;
						}
					}
					int count = 0;
					for(int i = 0;infos != null&&i < infos.length;i ++){
						if(infos[i].isDownLoadOver == 2){
							count ++;//最小缓存进度
						}else{
							break;
						}
					}
					Message message = new Message();
					message.what = MessageID.MESSAGE_MEDIAPLAYER_BUFFERUPDATE;
					message.arg1 = (count*1000)/(infos.length*10);
					handler.sendMessage(message);//发送缓存进度
					if(isReady&&infos!=null){
						LogPrint.Print("media","file download 500k ready play");
						handler.sendEmptyMessage(MessageID.MESSAGE_MEDIAPLAYER_READY);//发送允许播放通知
					}
					//判断是否全部下载完成
					boolean allover = true;
					for(int i = 0;i < infos.length;i ++){
						if(infos[i].isDownLoadOver != 2){
							allover = false;
							break;
						}
					}
					if(allover){
						dataBase.upDateDownLoadResult(CommonUtil.urlToNum(getUrl(url)));//全下完了更新数据库
						handler.sendEmptyMessage(MessageID.MESSAGE_MEDIAPLAYER_READY);//发送允许播放通知
					}else{
						findAntsToWork();
					}
				} catch (Exception e) {
					LogPrint.Print("media","onfinish error = "+e.toString());
				}
				break;
			}
		}
		
	};
}
