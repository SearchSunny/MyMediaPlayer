/**
 * 
 */
package com.mymediaplayer.plug;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.mymediaplayer.utils.LogPrint;

/**
 * 视频数据库
 *id,url(md5),over
 */
public class MediaDataBase extends SQLiteOpenHelper {

	public final static String DB_NAME = "media.db";
	public final static int DB_VERSION = 1;//数据库版本
	
	public MediaDataBase(Context context, String name, CursorFactory factory,
                         int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createDB(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
	
	private void createDB(SQLiteDatabase db){
		LogPrint.Print("media","MediaDataBase:createDB");
		String sql = "create table if not exists t_media(" +
//		"id integer primary key," +
		"url varchar(128) not null on conflict fail," +
		"over integer)";
		db.execSQL(sql);
	}
	
	//插入一条数据
	public void insertDB(String url){
		LogPrint.Print("media","MediaDataBase:insertDB");
		String sql = "insert into t_media(url,over) values('"+url+"',0)";
		SQLiteDatabase db = getWritableDatabase();
		if(db != null){
			db.execSQL(sql);
			db.close();
		}
	}
	
	//数据库中是否有这条数据
	public boolean isInDB(String url){
		LogPrint.Print("media","MediaDataBase:isInDB");
		boolean result = false;
		try {
			SQLiteDatabase db = getWritableDatabase();
			if(db != null){
				// 第一个参数String：表名  
				// 第二个参数String[]:要查询的列名  
				// 第三个参数String：查询条件  
				// 第四个参数String[]：查询条件的参数  
				// 第五个参数String:对查询的结果进行分组  
				// 第六个参数String：对分组的结果进行限制  
				// 第七个参数String：对查询的结果进行排序
				Cursor cursor = db.query("t_media", new String[]{"url"}, "url = ?", new String[]{url}, null, null, null);
				if(cursor != null){
					LogPrint.Print("media","count = "+cursor.getCount());
					if(cursor.getCount() > 0){
						cursor.moveToFirst();
						int index = cursor.getColumnIndex("url");
						String str = cursor.getString(index);
						if(str != null&&str.length() > 0){
							result = true;
						}
					}
					cursor.close();
				}
				db.close();
			}
		} catch (Exception e) {
			LogPrint.Print("media","error = "+e.toString());
		}
		return result;
	}
	
	//文件是否是下载完成的
	public boolean isDownLoadOver(String url){
		LogPrint.Print("media","MediaDataBase:isDownLoadOver");
		boolean result = false;
		try {
			SQLiteDatabase db = getWritableDatabase();
			if(db != null){
				// 第一个参数String：表名  
				// 第二个参数String[]:要查询的列名  
				// 第三个参数String：查询条件  
				// 第四个参数String[]：查询条件的参数  
				// 第五个参数String:对查询的结果进行分组  
				// 第六个参数String：对分组的结果进行限制  
				// 第七个参数String：对查询的结果进行排序
				Cursor cursor = db.query("t_media", new String[]{"url","over"}, "url = ?", new String[]{url}, null, null, null);
				if(cursor != null){
					if(cursor.getCount() > 0){
						cursor.moveToFirst();
						int index = cursor.getColumnIndex("over");
						if(index < 0){
							cursor.close();
							db.close();
							return false;
						}
						String str = cursor.getString(index);
						if(str != null&&str.length() > 0){
							result = Integer.parseInt(str)==0?false:true;
						}
					}
					cursor.close();
				}
				db.close();
			}
		} catch (Exception e) {
		}
		return result;
	}
	
	//更新下载完成状态
	public void upDateDownLoadResult(String url){
		LogPrint.Print("media","MediaDataBase:upDateDownLoadResult");
		SQLiteDatabase db = getWritableDatabase();
		if(db != null){
			ContentValues values = new ContentValues();
            values.put("over", 1);  
            // 调用update方法  
            // 第一个参数String：表名  
            // 第二个参数ContentValues：ContentValues对象  
            // 第三个参数String：where字句，相当于sql语句where后面的语句，？号是占位符  
            // 第四个参数String[]：占位符的值  
            db.update("t_media", values, "url = ?", new String[] {url});
            db.close();
		}
	}
	
	//删除数据库条目
	public void deleteDB(String url){
		LogPrint.Print("media","MediaDataBase:deleteDB");
		SQLiteDatabase db = getWritableDatabase();
		if(db != null){
			//第一个参数String：表名  
            //第二个参数String：条件语句  
            //第三个参数String[]：条件值 
			db.delete("t_media", "url = ?", new String[]{url});
			db.close();
		}
	}

}
