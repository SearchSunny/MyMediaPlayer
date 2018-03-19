/**
 *
 */
package com.mymediaplayer.plug;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.mymediaplayer.R;
import com.mymediaplayer.activity.BaseActivity;
import com.mymediaplayer.utils.ActionID;
import com.mymediaplayer.utils.CommonUtil;
import com.mymediaplayer.utils.LogPrint;
import com.mymediaplayer.utils.MessageID;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class MediaPlayerA extends BaseActivity implements SurfaceHolder.Callback {

    private RelativeLayout rctrlscreen;
    private SurfaceView surfaceView;
    private TextView titlebar_titletext;
    private Button titlebar_backbutton;
    private Button titlebar_shareButton;
    private Button media_likebtn;
    private Button media_prebtn;
    private Button media_playbtn;
    private Button media_nextbtn;
    private Button media_lookbtn;
    private TextView curtime;
    private TextView maxtime;
    private SeekBar seekBar;
    private PopNumView popNumView;
    private RelativeLayout media_share;
    private ListView media_sharelist;
    private Handler screenHandler;
    private SurfaceHolder holder;
    private android.media.MediaPlayer mediaPlayer;
    private boolean isPlay;//播放true,暂停false
    private int mProgress;//进度百分比

    private String playUrl;//视频播放地址
    private String titleString;
    private String commodityInfoString;
    private String commodityImageString;
    private int commodityid;
    private String wapUrl;
    private boolean likeState;
    private int deleteId;
    private String likenum;
    private boolean isTitleAnimationFinish;
    private boolean isBottomAnimationFinish;
    private boolean isSurfaceDestory;
    private int saveCurPos;//记录当前播放位置
    public final static int[] itemImages = {R.drawable.listitem_weibo, R.drawable.listitem_qq, R.drawable.listitem_wx, R.drawable.listitem_sms, R.drawable.listitem_email};

    private ProgressBar loadingBar;
    private boolean isPrepare;//是否准备完成了
    private boolean isPlayOver;//是否播放完成
    private boolean isBufferPrepare;//缓冲是否准备好了
    // IWXAPI 是第三方app和微信通信的openapi接口
    //private IWXAPI api;
    //微信版本号
    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;
    private byte[] wxByte;
    //头部菜单布局
    private RelativeLayout relativeLayout_title;
    //播放控制器布局
    private RelativeLayout relativeLayout_bottom;
    //是否全屏
    private boolean isScreen = false;
    private boolean flag = false;
    private boolean isRegisterBroadcast = false;
    private boolean isPlayFinish = false;
    private Timer mTimer = new Timer();
    //手势提醒布局
    private RelativeLayout relativeLayout;

    private ImageView imageView;
    private TimerTask mTimerTask = new TimerTask() {

        @Override
        public void run() {
            if (mediaPlayer != null) {
                if (isPrepare && isBufferPrepare && mediaPlayer.isPlaying()) {
                    mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_CURRENTTIME);
                }
            }
        }

    };

    private boolean isOnPause;
    //手动跳转至其他界面，置为true;(分享，登录，看看)
    public static boolean pauseing;

    private MediaLoader mediaLoader;//视频预加载
    private boolean isCacheReadyToPlay;//下载是否已满足可播放标准（500k）
    private boolean isError;//播放时是否出现错误
    private int bufferPecent;//记录当前的最小缓存进度
    private boolean tempPause;//临时暂停，当跳转到未缓存区域时使用
    private Dialog dialog = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMold();
        setContentView(R.layout.mediaplayer);
        relativeLayout = (RelativeLayout) findViewById(R.id.gesture01);
        imageView = (ImageView) findViewById(R.id.gesture5);

        String prompGestures = CommonUtil.getPromptVideoGestureStatu(this);
        if (prompGestures.equals("0")) {
            relativeLayout.setVisibility(View.VISIBLE);
            imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    relativeLayout.setVisibility(View.GONE);
                    CommonUtil.savePromptVideoGestureStatu(MediaPlayerA.this, "1");
                }
            });
        } else {
            relativeLayout.setVisibility(View.GONE);
        }
        isCacheReadyToPlay = false;
        isError = false;
        bufferPecent = 0;
        tempPause = false;
        //初始化头部菜单布局
        relativeLayout_title = (RelativeLayout) findViewById(R.id.relativeLayout_title);
        //初始化播放控制器布局
        relativeLayout_bottom = (RelativeLayout) findViewById(R.id.relativeLayout_bottom);
        isSurfaceDestory = false;
        saveCurPos = 0;
        isPlayOver = false;
        isPrepare = false;
        isBufferPrepare = false;
        playUrl = getIntent().getExtras().getString("url");
        titleString = getIntent().getExtras().getString("title");
        commodityInfoString = getIntent().getExtras().getString("commodityInfoString");
        commodityImageString = getIntent().getExtras().getString("image");
        commodityid = getIntent().getExtras().getInt("commodityid");
        wapUrl = getIntent().getExtras().getString("wapurl");
        likeState = getIntent().getExtras().getBoolean("likestate");
        deleteId = getIntent().getExtras().getInt("deleteid");
        likenum = getIntent().getExtras().getString("likenum");

        rctrlscreen = (RelativeLayout) findViewById(R.id.rctrlscreen);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        loadingBar = (ProgressBar) findViewById(R.id.loading);
        addProgress();
        titlebar_titletext = (TextView) findViewById(R.id.titlebar_titletext);
        titlebar_backbutton = (Button) findViewById(R.id.titlebar_backbutton);
        titlebar_shareButton = (Button) findViewById(R.id.titlebar_menubutton);
        media_likebtn = (Button) findViewById(R.id.media_likebtn);
        if (likeState) {
            media_likebtn.setBackgroundResource(R.drawable.media_likebtn_f);
        }
        media_prebtn = (Button) findViewById(R.id.media_prebtn);
        media_playbtn = (Button) findViewById(R.id.media_playbtn);
        media_nextbtn = (Button) findViewById(R.id.media_nextbtn);
        media_lookbtn = (Button) findViewById(R.id.media_lookbtn);
        curtime = (TextView) findViewById(R.id.curtime);
        maxtime = (TextView) findViewById(R.id.maxtime);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
        popNumView = (PopNumView) findViewById(R.id.popnumview);
        popNumView.initImageWH();
        media_share = (RelativeLayout) findViewById(R.id.media_share);
        media_sharelist = (ListView) findViewById(R.id.media_sharelist);
        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.media_shareitem, new String[]{"item"}, new int[]{R.id.media_shareitemimg});
        media_sharelist.setAdapter(adapter);
        media_sharelist.setOnItemClickListener(listener);
        titlebar_titletext.setText(titleString);

        changeScreenMode(false);

        surfaceView.setOnClickListener(surfaceClickListener);
        titlebar_backbutton.setOnClickListener(backClickListener);
        titlebar_shareButton.setOnClickListener(shareClickListener);
        media_likebtn.setOnClickListener(likeClickListener);
        media_prebtn.setOnClickListener(preClickListener);
        media_playbtn.setOnClickListener(playClickListener);
        media_nextbtn.setOnClickListener(nextClickListener);
        media_lookbtn.setOnClickListener(lookClickListener);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        holder = surfaceView.getHolder();
//        holder.setFixedSize(CommonUtil.screen_height, CommonUtil.screen_width);
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (mTimer != null && mTimerTask != null) {
            mTimer.schedule(mTimerTask, 0, 500);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//		pauseing = true;
        LogPrint.Print("webview", "Meida onPause");
        if (mHandler != null) {
            mHandler.removeCallbacks(cloassRunnable);
            alertDialogClose();
        }
        Intent broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_FINISH);
        sendBroadcast(broadcastIntent);
        if (!flag) {
            isOnPause = true;
            pauseMedia();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacks(cloassRunnable);
            alertDialogClose();
        }
        Intent broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_FINISH);
        sendBroadcast(broadcastIntent);
        //销毁广播
        unRegisterReceiver();
        pauseing = true;
    }

    @Override
    protected void onResume() {
        isScreen = false;
        pauseing = false;
        animtionIn();
        super.onResume();
        //注册广播
        registerReceiver();
        isOnPause = false;


        LogPrint.Print("webview", "onResume");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MessageID.REQUESTCODE_LIKE_FLUSH) {
            likeState = data.getBooleanExtra("likestate", false);
            likenum = data.getStringExtra("likenum");
            if (likeState) {
                media_likebtn.setBackgroundResource(R.drawable.media_likebtn_f);
            } else {
                media_likebtn.setBackgroundResource(R.drawable.media_likebtn);
            }
        }
    }

    public void finish() {
        if (mediaPlayer != null) {
            mHandler.removeMessages(MessageID.MESSAGE_MEDIA_CURRENTTIME);
            if (mTimer != null) {
                mTimer.cancel();
                mTimerTask = null;
                mTimer = null;
            }
            try {
                mediaPlayer.stop();
            } catch (Exception e) {
                LogPrint.Print("exp=" + e.toString());
            }
            mediaPlayer.release();
            if (mediaLoader != null) {
                mediaLoader.stopAllAnts();//终止所有蚂蚁的下载
            }
        }
        super.finish();
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < itemImages.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("item", itemImages[i]);
            list.add(map);
        }

        return list;
    }

    private void init() {
        if (isSurfaceDestory) {
            addProgress();
        }
        try {
            mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDisplay(holder);
            mediaPlayer.setDataSource(CommonUtil.dir_media + "/" + CommonUtil.urlToNum(MediaLoader.getUrl(playUrl)) + ".mp4");
            mediaPlayer.setOnPreparedListener(onPreparedListener);
            mediaPlayer.setOnCompletionListener(onCompletionListener);
            mediaPlayer.setOnErrorListener(onErrorListener);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            closeProgress();
            changePlayRes(false);
            e.printStackTrace();
        }
    }

    private void changePlayRes(boolean b) {
        isPlay = b;
        if (isPlay) {
            media_playbtn.setBackgroundResource(R.drawable.media_pausebtn_0);
        } else {
            media_playbtn.setBackgroundResource(R.drawable.media_playbtn_0);
        }
    }

    private void changeScreenMode(boolean b) {
        if (b) {
            rctrlscreen.setVisibility(View.INVISIBLE);
        } else {
            rctrlscreen.setVisibility(View.VISIBLE);
        }
    }

    //已小时:分:秒的方式展示时间
    private String getTime(int time) {
        String result = "";
        if (time <= 0) return "00:00";

        int h = time / 1000 / 3600;
        int m = (time / 1000 / 60) % 60;
        int s = (time / 1000) % 60;
        String sh = h > 9 ? ("" + h) : ("0" + h);
        String sm = m > 9 ? ("" + m) : ("0" + m);
        String ss = s > 9 ? ("" + s) : ("0" + s);

        if (h > 0) {
            result = sh + ":" + sm + ":" + ss;
        } else {
            result = sm + ":" + ss;
        }

        return result;
    }

    private OnClickListener backClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private OnClickListener shareClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //分享出现后不消失,等用户点击后在消失
            /*if(UserUtil.userid == -1||UserUtil.userState != 1){//未登陆
                pauseMedia();
				shareLikeIfNoLogin("请您登录后分享");
				return;
			}
			alertDialogShow();*/
        }
    };

    /**
     * 显示分享对话框
     */
    public void alertDialogShow() {
        int width = CommonUtil.screen_width;
        int height = CommonUtil.screen_height;
        media_share.bringToFront();
        titlebar_shareButton.setBackgroundResource(R.drawable.media_sharebtn_f);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.media_button, null);
        Button buttonWB = (Button) view.findViewById(R.id.buttonWB);
        Button buttonQQ = (Button) view.findViewById(R.id.buttonQQ);
        Button buttonWX = (Button) view.findViewById(R.id.buttonWX);
        Button buttonEmail = (Button) view.findViewById(R.id.buttonEmail);
        Button buttonSms = (Button) view.findViewById(R.id.buttonSms);
        Button buttonWXFreindButton = (Button) view.findViewById(R.id.buttonPengyou);
        dialog = new Dialog(MediaPlayerA.this, R.style.FullScreenDialog1);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                titlebar_shareButton.setBackgroundResource(R.drawable.media_sharebtn);
                //关闭分享消失后，控制条也消失
                //mHandler.post(cloassRunnable);
            }
        });
        buttonWB.setOnClickListener(shareListener);
        buttonQQ.setOnClickListener(shareListener);
        buttonWX.setOnClickListener(shareListener);
        buttonEmail.setOnClickListener(shareListener);
        buttonSms.setOnClickListener(shareListener);
        buttonWXFreindButton.setOnClickListener(shareListener);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (width <= 240 && height <= 320) {
            lp.x = 10;
            lp.y = -20;
        } else if (width <= 320 && height <= 480) {
            lp.x = 10;
            lp.y = -20;
        } else if (width <= 480 && height <= 800) {
            lp.x = 10;
            lp.y = -40;
        } else if (width <= 540 && height <= 960) {
            lp.x = 10;
            lp.y = -50;
        } else if (width <= 640 && height <= 960) {
            lp.x = 10;
            lp.y = -50;
        } else if (width <= 720 && height <= 1280) {
            lp.x = 10;
            lp.y = -50;
        } else if (width <= 800 && height <= 1280) {
            lp.x = 10;
            lp.y = -50;
        }
        dialog.show();
    }

    /**
     * 关闭分享对话框
     */
    public void alertDialogClose() {
        if (dialog != null) {
            titlebar_shareButton.setBackgroundResource(R.drawable.media_sharebtn);
            dialog.dismiss();
        }
    }

	private void shareLikeIfNoLogin(String str) {
		// 分享的登录判断

	}

    private OnClickListener likeClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

        }
    };

    private OnClickListener preClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mHandler.removeCallbacks(cloassRunnable);
            mHandler.postDelayed(cloassRunnable, 5000);
            if (isPrepare && isBufferPrepare) {
                mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_PRE);
            } else {
                CommonUtil.ShowToast(MediaPlayerA.this, "努力加载中", false);
            }
        }
    };

    private OnClickListener playClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            mHandler.removeCallbacks(cloassRunnable);
            mHandler.postDelayed(cloassRunnable, 5000);
            if (isPrepare && isBufferPrepare) {
                mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_PLAY);
            } else {
                CommonUtil.ShowToast(MediaPlayerA.this, "努力加载中", false);
            }
        }
    };

    private OnClickListener nextClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            mHandler.removeCallbacks(cloassRunnable);
            mHandler.postDelayed(cloassRunnable, 5000);
            if (isPrepare && isBufferPrepare) {
                mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_NEXT);
            } else {
                CommonUtil.ShowToast(MediaPlayerA.this, "努力加载中", false);
            }
        }
    };

    private OnClickListener lookClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (wapUrl != null) {
                pauseMedia();
                //lyb
                pauseing = true;
                mHandler.removeCallbacks(cloassRunnable);
            }
        }
    };

    private OnClickListener surfaceClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (isTitleAnimationFinish && isBottomAnimationFinish) {
                mHandler.removeCallbacks(cloassRunnable);
                flag = true;
                isScreen = true;
                animtionOut();
                Intent intent = new Intent(MediaPlayerA.this, MediaPlayerGestureActivity.class);
                startActivity(intent);
                titlebar_shareButton.setBackgroundResource(R.drawable.media_sharebtn);
            }
        }
    };

    private OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mHandler.removeCallbacks(cloassRunnable);
            mHandler.postDelayed(cloassRunnable, 5000);
            if (isPrepare && isBufferPrepare) {
                LogPrint.Print("webview", "seekPecent|bufferPecent = " + mProgress + "|" + bufferPecent);
                if (mProgress > bufferPecent) {//跳转的进度比缓存的进度大
                    addProgress();
                    mediaPlayer.pause();
                    tempPause = true;
                } else {
                    mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_SEEK);
                }
                popNumView.hide(true);
            } else {
                CommonUtil.ShowToast(MediaPlayerA.this, "努力加载中", false);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (!isPrepare || !isBufferPrepare) return;
            popNumView.hide(false);
            popNumView.setNum(getTime(mediaPlayer.getCurrentPosition()));
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if (!isPrepare || !isBufferPrepare) return;
            mProgress = progress;
            int pos = mediaPlayer.getDuration() / 100 * mProgress;
            setCurTime(pos);
            popNumView.setNum(getTime(pos));
        }
    };

    private android.media.MediaPlayer.OnPreparedListener onPreparedListener = new android.media.MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(android.media.MediaPlayer mp) {
            if (isOnPause) return;
            closeProgress();
            LogPrint.Print("webview", "onprepared");
            isPrepare = true;
            isBufferPrepare = true;
            setScreenSize();
            if (isSurfaceDestory) {
                mediaPlayer.start();
                changePlayRes(true);
                setCurTime(saveCurPos);
                setMaxTime(mediaPlayer.getDuration());
                mediaPlayer.seekTo(saveCurPos);
                surfaceView.postInvalidate();
                mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_CURRENTTIME);
                isSurfaceDestory = false;
                isScreen = true;
                flag = true;
                mHandler.sendEmptyMessageDelayed(MessageID.MESSAGE_MEDIA_CLOSE, 5000);
            } else {
                mediaPlayer.start();
                changePlayRes(true);
                setCurTime(0);
                setMaxTime(mediaPlayer.getDuration());
                if (isPlayOver) {
                    isPlayOver = false;
                    changePlayRes(false);
                    changeScreenMode(false);
                    mediaPlayer.pause();
                }
                surfaceView.postInvalidate();
                mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_CURRENTTIME);
                isScreen = true;
                flag = true;
                if (!isPlayFinish) {
                    mHandler.postDelayed(cloassRunnable, 1000);
                }
            }
        }
    };

    private android.media.MediaPlayer.OnCompletionListener onCompletionListener = new android.media.MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(android.media.MediaPlayer mp) {
            if (!isPlayOver && isPrepare) {
                isPlayOver = true;
                LogPrint.Print("webview", "onCompletion");
                mHandler.sendEmptyMessage(100010);

            }
        }
    };

    private android.media.MediaPlayer.OnErrorListener onErrorListener = new android.media.MediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            LogPrint.Print("webview", "onError");
            isError = true;
            mediaPlayer.pause();
            addProgress();
            return false;
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MessageID.MESSAGE_CONNECT_START:
                    addProgress();
                    break;
                case MessageID.MESSAGE_CONNECT_ERROR:
                    closeProgress();
                    CommonUtil.ShowToast(MediaPlayerA.this, (String) msg.obj);
                    break;
                case MessageID.MESSAGE_CONNECT_DOWNLOADOVER:
                    closeProgress();
                    if ("text/json".equals(msg.getData().getString("content_type"))) {
                        Json((byte[]) msg.obj, msg.arg1);
                    } else {
                        //wxSend((byte[]) msg.obj);
                    }
                    break;
                case MessageID.MESSAGE_MEDIA_INIT:
                    init();
                    break;
                case MessageID.MESSAGE_MEDIA_PLAY:
                    isPlay = !isPlay;
                    changePlayRes(isPlay);
                    try {
                        if (isPlay) {
                            if (mediaPlayer != null) {
                                mediaPlayer.start();
                                mHandler.sendEmptyMessage(MessageID.MESSAGE_MEDIA_CURRENTTIME);
                            }
                        } else {
                            if (mediaPlayer != null) {
                                mediaPlayer.pause();
                                mHandler.removeMessages(MessageID.MESSAGE_MEDIA_CURRENTTIME);
                                //动画切入,暂停情况下
                                mHandler.removeCallbacks(cloassRunnable);
                            }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                        changePlayRes(false);
                    }
                    break;
                case MessageID.MESSAGE_MEDIA_PRE:
                    try {
                        int timepos = mediaPlayer.getCurrentPosition() - 5000;
                        int max = mediaPlayer.getDuration();
                        if (timepos <= 0) {
                            timepos = 0;
                        }
                        mediaPlayer.seekTo(timepos);
                        setCurTime(timepos);
                        float pec = 0;
                        if (max != 0) {
                            pec = (float) (timepos * 100 / max);
                        }
                        seekBar.setProgress((int) pec);
                        popNumView.hide(false);
                        popNumView.setNum(getTime(mediaPlayer.getCurrentPosition()));
                        popNumView.hide(true);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    break;
                case MessageID.MESSAGE_MEDIA_NEXT:
                    try {
                        int timepos = mediaPlayer.getCurrentPosition() + 5000;
                        int max = mediaPlayer.getDuration();
                        if (timepos >= max) {
                            timepos = max;
                        }
                        mediaPlayer.seekTo(timepos);
                        setCurTime(timepos);
                        float pec = 0;
                        if (max != 0) {
                            pec = (float) (timepos * 100 / max);
                        }
                        seekBar.setProgress((int) pec);
                        popNumView.hide(false);
                        popNumView.setNum(getTime(mediaPlayer.getCurrentPosition()));
                        popNumView.hide(true);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    break;
                case MessageID.MESSAGE_MEDIA_CURRENTTIME:
                    try {
                        surfaceView.postInvalidate();
                        setCurTime(mediaPlayer.getCurrentPosition());
                        int max = mediaPlayer.getDuration();
                        float pec = 0;
                        if (max != 0) {
                            pec = (float) (mediaPlayer.getCurrentPosition() * 100 / max);
                        }
                        seekBar.setProgress((int) pec);
                    } catch (Exception e) {

                    }
//				//1秒后继续获取
//				mHandler.sendEmptyMessageDelayed(MessageID.MESSAGE_MEDIA_CURRENTTIME, 1000);
                    break;
                case MessageID.MESSAGE_MEDIA_SEEK:
                    try {
                        if (tempPause) {
                            tempPause = false;
                            closeProgress();
                        }
                        int pos = mediaPlayer.getDuration() / 100 * mProgress;
                        mediaPlayer.seekTo(pos);
                        setCurTime(pos);
                        if (!mediaPlayer.isPlaying()) {
                            //mediaPlayer.start();
                            changePlayRes(false);
                            mHandler.removeCallbacks(cloassRunnable);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    break;
                case 100005://邮件分享
                    //shareEmail("翠鸟分享:" + titleString, commodityInfoString + "\n\n" + msg.getData().getString("url") + "\n\n(分享自 翠鸟客户端)\n\n" + "【客户端下载地址】" + URLUtil.URL_APPDOWNLOAD + "mail", null);
                    break;
                case 100006://短信分享
                    //shareSms("翠鸟分享:" + titleString + "\n\n去看看 " + msg.getData().getString("url") + "\n\n(分享自 翠鸟客户端)\n\n" + "【客户端下载地址】" + URLUtil.URL_APPDOWNLOAD + "love");
                    break;
                case 100007:
                    CommonUtil.ShowToast(MediaPlayerA.this, "失败了!");
                    break;
                case 100008://删除喜欢
                    likeState = false;
                    media_likebtn.setBackgroundResource(R.drawable.media_likebtn);
                    likenum = msg.getData().getString("likenum");
                    break;
                case 100009://添加喜欢
                    boolean mayAddLike = msg.getData().getBoolean("likeNumLimit");
                    //喜欢可以添加喜欢
				/*if(!mayAddLike){
					new LikeLimitDialog(MediaPlayerA.this);
				}else{
				likeState = true;
				media_likebtn.setBackgroundResource(R.drawable.media_likebtn_f);
				likenum = msg.getData().getString("likenum");
				}*/
                    break;
                case 100010://视频播放完成
                    setCurTime(mediaPlayer.getDuration());
                    seekBar.setProgress(100);
                    surfaceView.postInvalidate();
                    mHandler.sendEmptyMessageDelayed(100011, 200);
                    break;
                case 100011://视频播放完成后的处理
                    isPrepare = false;
                    isBufferPrepare = false;
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    addProgress();
                    isPlayFinish = true;
                    //用于数据广播
                    Intent broadcastIntent = new Intent(ActionID.ACTION_BROADCAST_MEDIAPLAYER_FINISH);
                    sendBroadcast(broadcastIntent);
                    init();
                    break;
                //处理视频播放5秒后关闭头部和播控控制器
                case MessageID.MESSAGE_MEDIA_CLOSE:
                    animtionOut();
                    Intent intent = new Intent(MediaPlayerA.this, MediaPlayerGestureActivity.class);
                    startActivity(intent);
                    break;
                case MessageID.MESSAGE_MEDIAPLAYER_READY://视频达到可播放条件
                    if (!isCacheReadyToPlay) {
                        isCacheReadyToPlay = true;
                        mHandler.sendEmptyMessageDelayed(MessageID.MESSAGE_MEDIA_INIT, 3000);
                    }
                    break;
                case MessageID.MESSAGE_MEDIAPLAYER_BUFFERUPDATE://更新缓存进度
                    bufferPecent = msg.arg1;
                    seekBar.setSecondaryProgress(bufferPecent);
                    if (tempPause) {
                        if (bufferPecent >= mProgress) {
                            tempPause = false;
                            int pos = mediaPlayer.getDuration() / 100 * mProgress;
                            closeProgress();
                            mediaPlayer.seekTo(pos);
                            setCurTime(pos);
                            if (!mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                                changePlayRes(true);
                            }
                        }
                    }
                    if (isError) {
                        isError = false;
                        closeProgress();
                        mediaPlayer.start();
                    }
                    break;
            }
        }

    };

    public void addProgress() {
        if (loadingBar != null) {
            loadingBar.setVisibility(View.VISIBLE);
        }
    }

    public void closeProgress() {
        if (loadingBar != null) {
            loadingBar.setVisibility(View.INVISIBLE);
        }
    }

    private OnClickListener shareListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            pauseMedia();
            mHandler.removeCallbacks(cloassRunnable);
            pauseing = true;
        }
    };


    private AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            pauseMedia();
            mHandler.removeCallbacks(cloassRunnable);
            pauseing = true;
        }

    };

    private void Json(byte[] data, int threadindex) {
        try {
            String str = new String(data, "UTF-8");
            str = CommonUtil.formUrlEncode(str);
            LogPrint.Print("json = " + str);
            JSONObject jObject = new JSONObject(str);
            String result = jObject.getString("result");
            if (result != null) {
                if (threadindex >= 4) {
                    if (result.equalsIgnoreCase("true")) {
                        Bundle bundle = new Bundle();
                        String likeNum = jObject.getString("likenum");
                        bundle.putString("likenum", likeNum);
                        Message msg = new Message();
                        if (threadindex == 4) {//删除
                            msg.setData(bundle);
                            msg.what = 100008;
                            mHandler.sendMessage(msg);
                        } else if (threadindex == 5) {//添加
                            bundle.putBoolean("likeNumLimit", jObject.getBoolean("likeNumLimit"));
                            msg.setData(bundle);
                            msg.what = 100009;
                            mHandler.sendMessage(msg);
                        }
                    } else {
                        CommonUtil.ShowToast(MediaPlayerA.this, "失败了!");
                    }
                } else {
                    if (result.equalsIgnoreCase("true")) {
                        String url = jObject.getString("url");
                        Bundle bundle = new Bundle();
                        bundle.putString("url", url);
                        Message message = new Message();
                        if (threadindex == 0) {//短信
                            message.what = 100006;
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                        } else {//邮件
                            message.what = 100005;
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                        }
                    } else {
                        mHandler.sendEmptyMessage(100007);
                    }
                }
            }
        } catch (Exception e) {
        }
    }
    public String addUrlParam(String url, int logintype) {
  		/*if(URLUtil.IsLocalUrl()){
			return url;
		}*/
        //return url+"?pid="+commodityid+"&oid="+UserUtil.userid+"&dpi="+URLUtil.dpi()+"&logintype="+logintype;
        return url;
    }

    private void pauseMedia() {
        //播放暂停
        isPlay = false;
        changePlayRes(isPlay);
        try {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                mHandler.removeMessages(MessageID.MESSAGE_MEDIA_CURRENTTIME);
            }
        } catch (Exception e) {
            changePlayRes(false);
        }
    }

    protected void setFullScreenMold() {
        //隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏信号条(联想需要显示信号条所以以下注释掉)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (playUrl != null && playUrl.length() > 0) {
            if (mediaLoader == null) {
                mediaLoader = new MediaLoader(MediaPlayerA.this, playUrl, mHandler);
                mediaLoader.start();//启动加载
            }
            if (isCacheReadyToPlay) {
                mHandler.sendEmptyMessageDelayed(MessageID.MESSAGE_MEDIA_INIT, 500);
            }
        } else {
            CommonUtil.ShowToast(this, "播放地址出错了");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            LogPrint.Print("webview", "surfaceDestroyed");
            isPrepare = false;
            isBufferPrepare = false;
            isSurfaceDestory = true;
            saveCurPos = mediaPlayer.getCurrentPosition();
            mediaPlayer.stop();
            mediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setScreenSize() {
        float usePecent;//最终使用比率
        float wPecent, hPecent;//宽高比率
        //视频源尺寸，需要在视频准备好后获取
        float srcW = mediaPlayer.getVideoWidth();
        float srcH = mediaPlayer.getVideoHeight();
        wPecent = CommonUtil.screen_height / srcW;
        hPecent = CommonUtil.screen_width / srcH;
        usePecent = Math.min(wPecent, hPecent);
        LayoutParams lp = surfaceView.getLayoutParams();
        lp.width = (int) (wPecent < hPecent ? CommonUtil.screen_height : srcW * usePecent);
        lp.height = (int) (hPecent < wPecent ? CommonUtil.screen_width : srcH * usePecent);
        surfaceView.setLayoutParams(lp);
        //视频logo
        ImageView image = (ImageView) findViewById(R.id.videoLogo);
        image.setVisibility(View.VISIBLE);
    }

    private void setCurTime(int time) {
        if (maxtime != null && !"00:00".equals(maxtime.getText().toString())) {
            curtime.setText(getTime(time));
        }
    }

    private void setMaxTime(int time) {
        maxtime.setText(getTime(time));
    }

    /**
     * @param bmp         位图对象
     * @param needRecycle 是否回收
     * @return
     */
    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    //
    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    /**
     * 头部菜单动画进入监听事件
     */
    private AnimationListener title_in_animationListener = new AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 0);
            relativeLayout_title.setLayoutParams(params);
            isTitleAnimationFinish = false;

        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            isTitleAnimationFinish = true;
        }
    };

    /**
     * 播放控制器动画进入监听事件
     */
    private AnimationListener bottom_in_animationListener = new AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 0);
            relativeLayout_bottom.setLayoutParams(params);
            isBottomAnimationFinish = false;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            isBottomAnimationFinish = true;
        }
    };

    /**
     * 头部菜单退出屏幕动画监听事件
     */
    private AnimationListener title_out_animationListener = new AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, -relativeLayout_title.getBottom(), 0, 0);
            relativeLayout_title.setLayoutParams(params);
            media_share.setVisibility(View.INVISIBLE);
        }
    };

    /**
     * 播放控制器退出屏幕动画监听事件
     */
    private AnimationListener bottom_out_animationListener = new AnimationListener() {

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, -relativeLayout_bottom.getBottom());
            relativeLayout_bottom.setLayoutParams(params);
        }
    };

    /**
     * 手势动作广播接收器
     */
    private BroadcastReceiver mGestuReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //无匹配到任何手势
            if (ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_NO_RESULT.equals(action)) {
                LogPrint.Print("broadcase: no");
                Toast.makeText(MediaPlayerA.this, "主人，您的画太抽象，小C没认出来", Toast.LENGTH_SHORT).show();
            }
            //匹配到对钩手势--//购买
			/*else if(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_RIGHT.equals(action)){
				if(wapUrl != null){
					pauseMedia();	
					pauseing = true;
					Intent i = new Intent();
					i.setClass(MediaPlayerA.this, WebviewActivity.class);
					LogPrint.Print("==============kankan = "+wapUrl);
					i.putExtra("url",wapUrl);
					i.putExtra("title", titleString);
					i.putExtra("commodityid", commodityid);
					i.putExtra("commodityImageString", commodityImageString);
					i.putExtra("commodityInfoString", commodityInfoString);
					//add by lyb for taobao's like
					i.putExtra("likestate", likeState);
					i.putExtra("deleteid", deleteId);
					i.putExtra("likenum", likenum);
					startActivityForResult(i, MessageID.REQUESTCODE_LIKE_FLUSH);
				}
				else{
					CommonUtil.ShowToast(MediaPlayerA.this, "抱歉，不能去看看！");
				}
			}*/
            //喜欢
			/*else if(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CIRCLE.equals(action)){
				// TODO Auto-generated method stub
				if(UserUtil.userid != -1&&UserUtil.userState == 1){					
					if(likeState){//删除
						new ConnectUtil(MediaPlayerA.this, mHandler,0).connect(URLUtil.URL_MYLIKE_DELETE_SINGLE+"?id="+deleteId+"&oid="+UserUtil.userid+"&cid="+commodityid, HttpThread.TYPE_PAGE, 4);
					}else{//添加
						new ConnectUtil(MediaPlayerA.this, mHandler,0).connect(URLUtil.URL_ADDLIKE+"?oid="+UserUtil.userid+"&cid="+commodityid+"&plaid="+URLUtil.plaid, HttpThread.TYPE_PAGE, 5);
					}
				}else{
					pauseMedia();
					shareLikeIfNoLogin("登录后才能喜欢");
				}
			}*/
            //分享
			/*else if(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_TRIANGLE.equals(action)){
				// TODO Auto-generated method stub
				if (UserUtil.userid != -1 && UserUtil.userState == 1) {
					alertDialogShow();
				} else {
					pauseMedia();
					shareLikeIfNoLogin("请您登录后分享");
				}
			}*/
            //关闭手势面板跳回视频Activity
            else if (ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CLOSE.equals(action)) {
                isScreen = true;
                flag = true;
                mHandler.postDelayed(cloassRunnable, 5000);
            }
        }
    };

    /**
     * 注册广播接收器
     */
    private final void registerReceiver() {
        isRegisterBroadcast = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_NO_RESULT);
        intentFilter.addAction(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_RIGHT);
        intentFilter.addAction(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CIRCLE);
        intentFilter.addAction(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_TRIANGLE);
        intentFilter.addAction(ActionID.ACTION_BROADCAST_MEDIAPLAYER_GESTURE_CLOSE);
        registerReceiver(mGestuReceiver, intentFilter);
    }

    /**
     * 卸载广播接收器
     */
    private final void unRegisterReceiver() {
        if (mGestuReceiver != null && isRegisterBroadcast) {
            try {
                unregisterReceiver(mGestuReceiver);
            } catch (Exception e) {
            }
            isRegisterBroadcast = false;
        }
    }

    /**
     * 头部和播放控制器进入动画
     */
    private void animtionIn() {
        Animation animation_title_in = AnimationUtils.loadAnimation(MediaPlayerA.this, R.anim.mediaplayer_title_in);
        relativeLayout_title.startAnimation(animation_title_in);
        animation_title_in.setAnimationListener(title_in_animationListener);
        Animation animation_bottom_in = AnimationUtils.loadAnimation(MediaPlayerA.this, R.anim.mediaplayer_bottom_in);
        relativeLayout_bottom.startAnimation(animation_bottom_in);
        animation_bottom_in.setAnimationListener(bottom_in_animationListener);
    }

    /**
     * 头部和播放控制器退出动画
     */
    private void animtionOut() {
        Animation animation_title_out = AnimationUtils.loadAnimation(MediaPlayerA.this, R.anim.mediaplayer_title_out);
        relativeLayout_title.startAnimation(animation_title_out);
        animation_title_out.setAnimationListener(title_out_animationListener);
        Animation animation_bottom_out = AnimationUtils.loadAnimation(MediaPlayerA.this, R.anim.mediaplayer_bottom_out);
        relativeLayout_bottom.startAnimation(animation_bottom_out);
        animation_bottom_out.setAnimationListener(bottom_out_animationListener);
    }

    Runnable cloassRunnable = new Runnable() {

        @Override
        public void run() {
            animtionOut();
            alertDialogClose();
            Intent intent = new Intent(MediaPlayerA.this, MediaPlayerGestureActivity.class);
            startActivity(intent);
        }
    };
}
