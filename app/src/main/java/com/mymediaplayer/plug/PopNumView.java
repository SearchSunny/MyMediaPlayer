/**
 * 
 */
package com.mymediaplayer.plug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.mymediaplayer.R;

import com.mymediaplayer.utils.CommonUtil;
/**
 *
 */
public class PopNumView extends LinearLayout {

	private int[] res = {R.drawable.mp_num0,R.drawable.mp_num1,R.drawable.mp_num2,
						R.drawable.mp_num3,R.drawable.mp_num4,R.drawable.mp_num5,
						R.drawable.mp_num6,R.drawable.mp_num7,R.drawable.mp_num8,
						R.drawable.mp_num9,R.drawable.mp_numm};
	
	private final static int MESSAGE_HIDE = 112200;
	private int alpha;//初始透明度
	private boolean hideAnimationStart;
	private String num;
	private Bitmap bitmap;
	private int imageWidth;//单个数字的宽度
	private int imageHeight;//单个数字的高度
	private Paint paint = new Paint();
	
	public PopNumView(Context context) {
		super(context);
		num = null;
		setWillNotDraw(false);//自定义的view需要增加这句代码,否则onDraw不执行
	}
	
	public PopNumView(Context context, AttributeSet attrs) {
		super(context, attrs);
		num = null;
		setWillNotDraw(false);//自定义的view需要增加这句代码,否则onDraw不执行
	}
	
	public void setNum(String num){
		this.num = num;
		postInvalidate();
	}
	
	public void initImageWH(){
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;//仅获取图片的基本信息
			Bitmap tmp = BitmapFactory.decodeResource(getResources(), R.drawable.mp_num0,options);
			imageWidth = options.outWidth;
			imageHeight = options.outHeight;
			tmp.recycle();
		} catch (Exception e) {

		}
	}
	
	public void hide(boolean hide){
		if(hide){//隐藏
			bitmap = createBitmap();
			mHandler.sendEmptyMessage(MESSAGE_HIDE);
		}else{//显现
			alpha = 100;
			hideAnimationStart = false;
			setVisibility(VISIBLE);
		}
	}
	
	private Bitmap createBitmap(){
		if(num != null){
			Bitmap bmp = Bitmap.createBitmap(imageWidth*num.length(), imageHeight, Config.ARGB_8888);
			Canvas canvas = new Canvas();
			Paint paint = new Paint();
			canvas.setBitmap(bmp);
			int index;
			for(int i = 0;i < num.length();i ++){
				if(num.charAt(i) == ':'){
					index = 10;
				}else{
					index = Integer.parseInt(num.substring(i, i+1));
				}
				canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), res[index]), i*imageWidth, 0, paint);
			}
			canvas.save();
			return bmp;
		}
		return null;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(num != null){
			super.onDraw(canvas);
			if(hideAnimationStart){
				if(bitmap != null){
					if(alpha <= 0){
						hideAnimationStart = false;
						setVisibility(INVISIBLE);
					}else{
						canvas.drawBitmap(CommonUtil.setAlpha(bitmap, alpha), (CommonUtil.screen_height-bitmap.getWidth())/2, (CommonUtil.screen_width-imageHeight)/2+imageHeight, paint);
						mHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, 50);
					}
				}
			}else{
				bitmap = createBitmap();
				if(bitmap != null){
					canvas.drawBitmap(bitmap, (CommonUtil.screen_height-bitmap.getWidth())/2, (CommonUtil.screen_width-imageHeight)/2+imageHeight, paint);
				}
			}
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MESSAGE_HIDE:
				hideAnimationStart = true;
				alpha -= 5;
				postInvalidate();
				break;
			}
		}
	};
}
