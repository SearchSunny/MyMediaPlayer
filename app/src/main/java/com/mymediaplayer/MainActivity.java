package com.mymediaplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.mymediaplayer.activity.BaseActivity;
import com.mymediaplayer.plug.MediaPlayerA;
import com.mymediaplayer.utils.CommonUtil;
import com.mymediaplayer.utils.MessageID;

public class MainActivity extends BaseActivity {

    private Button button;
    //https://cloud.video.taobao.com/play/u/112083600/p/1/e/6/t/1/50017306860.mp4
    //https://cloud.video.taobao.com/play/u/1669198343/p/1/e/6/t/1/31093847.mp4
    //http://vodcdn.video.taobao.com/oss/ali-video/213b4b43750fc40c1be7bc6bb8e0d4df.mp4
    //https://tbm.alicdn.com/dxmrcxlQ733nSRoIvjm/d4kORCHifUsYm7c8vXx%40%40sd.mp4
    String videoPath = "http://vodcdn.video.taobao.com/oss/ali-video/213b4b43750fc40c1be7bc6bb8e0d4df.mp4";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setFullScreenMold();
        setContentView(R.layout.activity_main);
        button = (Button)findViewById(R.id.btn);
        button.setOnClickListener(btn_player);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        CommonUtil.screen_width = dm.widthPixels;
        CommonUtil.screen_height = dm.heightPixels;
    }

    public View.OnClickListener btn_player = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent1 = new Intent();
            intent1.setClass(mContext, MediaPlayerA.class);
            intent1.putExtra("url", videoPath);
            mContext.startActivityForResult(intent1, MessageID.REQUESTCODE_LIKE_FLUSH);
        }
    };

    protected void setFullScreenMold() {
        //隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏信号条(联想需要显示信号条所以以下注释掉)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
