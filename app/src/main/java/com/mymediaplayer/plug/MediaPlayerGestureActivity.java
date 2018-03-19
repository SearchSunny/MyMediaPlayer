package com.mymediaplayer.plug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.mymediaplayer.R;
import com.mymediaplayer.activity.BaseActivity;
import com.mymediaplayer.utils.ActionID;
import com.mymediaplayer.utils.LogPrint;

import java.util.ArrayList;

public class MediaPlayerGestureActivity extends BaseActivity implements OnGesturePerformedListener, OnTouchListener {


    //手势文件索引
    private final static String GESTURE_RIGHT = "ges_right";//对钩
    private final static String GESTURE_CIRCLE = "ges_circle";//圆圈
    private final static String GESTURE_TRIANGLE = "ges_triangle";//三角
    private final static String GESTURE_RIGHT_1 = "ges_right1";//反对钩
    private final static String GESTURE_CIRCLE_1 = "ges_circle1";//反圆圈
    private final static String GESTURE_TRIANGLE_1 = "ges_triangle1";//反三角
    private final static String GESTURE_TRIANGLE_2 = "ges_triangle2";//倒三角

    private final static String GESTURE_RIGHT_H = "ges_right_h";//对钩（横屏）
    private final static String GESTURE_CIRCLE_H = "ges_circle_h";//圆圈（横屏）
    private final static String GESTURE_TRIANGLE_H = "ges_triangle_h";//三角（横屏）
    private final static String GESTURE_RIGHT_1_H = "ges_right1_h";//反对钩（横屏）
    private final static String GESTURE_CIRCLE_1_H = "ges_circle1_h";//反圆圈（横屏）
    private final static String GESTURE_TRIANGLE_1_H = "ges_triangle1_h";//反三角（横屏）
    private final static String GESTURE_TRIANGLE_2_H = "ges_triangle2_h";//倒三角（横屏）

    private GestureLibrary gestureLibrary;
    //用于数据广播
    private Intent broadcastIntent;
    private boolean isRegisterBroadcast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (MediaPlayerA.pauseing) {
            finish();
            return;
        }
        setFullScreenMold();
        setContentView(R.layout.mediaplayer_gesture);

        if (loadGesture() == false) {
            Toast.makeText(this, R.string.gesture_load_false, Toast.LENGTH_LONG).show();
        }
    }

    protected void setFullScreenMold() {
        //隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏信号条(联想需要显示信号条所以以下注释掉)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * 判断加载手势文件是否成功方法
     *
     * @return
     */
    private boolean loadGesture() {
        boolean isLoadSuccess = false;
        if (gestureLibrary == null) {
            gestureLibrary = GestureLibraries.fromRawResource(this, R.raw.mygestures);
        }
        isLoadSuccess = gestureLibrary.load();
        if (isLoadSuccess) {
            GestureOverlayView gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView);
            //设置GesturePerformedListener事件
            gestureOverlayView.addOnGesturePerformedListener(this);
            gestureOverlayView.setOnTouchListener(this);
        }
        return isLoadSuccess;
    }


    @Override
    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        //获得可能匹配的手势
        ArrayList<Prediction> array = gestureLibrary.recognize(gesture);
        //有匹配的手势
        if (array.size() > 0) {
            //手势匹配记数
            int resultCount = 0;
            for (int i = 0; i < array.size(); i++) {
                Prediction prediction = array.get(i);
                //手势配置值大于1.0
                if (prediction.score > 1.0) {
                    //匹配对钩
                    if (GESTURE_RIGHT.equals(prediction.name) || GESTURE_RIGHT_1.equals(prediction.name) ||
                            GESTURE_RIGHT_H.equals(prediction.name) || GESTURE_RIGHT_1_H.equals(prediction.name)) {
                        LogPrint.Print("right");
                        //OfflineLog.writeGestureRight((byte)1);//写入离线日志
                        broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_RIGHT);
                        sendBroadcast(broadcastIntent);
                    }
                    //匹配圆圈
                    else if (GESTURE_CIRCLE.equals(prediction.name) || GESTURE_CIRCLE_1.equals(prediction.name) ||
                            GESTURE_CIRCLE_H.equals(prediction.name) || GESTURE_CIRCLE_1_H.equals(prediction.name)) {
                        LogPrint.Print("circle");
                        //OfflineLog.writeGestureCircle((byte)1);//写入离线日志
                        broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CIRCLE);
                        sendBroadcast(broadcastIntent);
                    }
                    //匹配三角
                    else if (GESTURE_TRIANGLE.equals(prediction.name) || GESTURE_TRIANGLE_1.equals(prediction.name) || GESTURE_TRIANGLE_2.equals(prediction.name) ||
                            GESTURE_TRIANGLE_H.equals(prediction.name) || GESTURE_TRIANGLE_1_H.equals(prediction.name) || GESTURE_TRIANGLE_2_H.equals(prediction.name)) {
                        LogPrint.Print("triangle");
                        //OfflineLog.writeGestureTriangle((byte)1);//写入离线日志
                        broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_TRIANGLE);
                        sendBroadcast(broadcastIntent);
                    }
                    resultCount++;
                    finish();
                    break;
                }
            }
            //无匹配到任何手势的处理
            if (resultCount == 0) {
                LogPrint.Print("no");
                broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_NO_RESULT);
                sendBroadcast(broadcastIntent);
                finish();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CLOSE);
                sendBroadcast(broadcastIntent);
                finish();
                break;

        }
        return true;
    }

    /**
     * 注册广播接收器
     */
    private final void registerReceiver() {
        isRegisterBroadcast = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionID.ACTION_BROADCAST_MEDIAPLAYER_FINISH);
        registerReceiver(closeReceiver, intentFilter);
    }

    /**
     * 卸载广播接收器
     */
    private final void unRegisterReceiver() {
        if (closeReceiver != null && isRegisterBroadcast) {
            try {
                unregisterReceiver(closeReceiver);
            } catch (Exception e) {
                // TODO: handle exception
            }
            isRegisterBroadcast = false;
        }
    }

    private BroadcastReceiver closeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ActionID.ACTION_BROADCAST_MEDIAPLAYER_FINISH.equals(action)) {
                finish();
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onDestroy() {
        LogPrint.Print("lyb", "meidiaGest onDestroy");
        super.onDestroy();
    }

}
