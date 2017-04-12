package com.universalvideoviewsample;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.universalvideoview.UniversalMediaController;
import com.universalvideoview.UniversalVideoView;

import java.io.File;

public class MyPlayerActivity extends AppCompatActivity implements UniversalVideoView.VideoViewCallback,
        UniversalMediaController.PlayPrevNextListener{
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;            //运行时权限
    private static final String VIDEO_LOCAL_URL = "/sdcard/Download/1.mp4";        //默认播放的视频路径
    private static final String SEEK_POSITION_KEY = "SEEK_POSITION_KEY";


    UniversalVideoView mVideoView;                                   //播放器
    UniversalMediaController mMediaController;                       //控制器

    Button mStartButton;                                             //开始播放按钮
    View mVideoLayout;                                               //整个播放器与控制器的父布局



    private int mSeekPosition;                                       //视频播放到的位置
    private int cachedHeight;                                        //播放视频部分的高度
    private int cacheWidth;                                          //播放视频部分的宽度
    private boolean isFullscreen;                                    //是否全屏

    private String[]  files;                                         //文件名集合

    int mCurrentFilePosition;                                        //当前播放的文件在集合中的位置

    boolean mDragging = false;                                       //进度条是否拖动

    private int maxVolume, currentVolume;           //音量最大值与当前值


    ImageButton mAPrevButton;
    ImageButton mATurnButton;
    ImageButton mANextButton;
    SeekBar     mAPlaySeekBar;
    ImageButton mAPeplayButton;
    ImageButton mAStopButton;
    SeekBar     mAVolumeSeekBar;
    ImageButton mAScaleButton;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_player);

        //获取该目录下所有文件名集合
        files = getFiles("/sdcard/Download");

        initVideoView();
        initController();
        setVideoAreaSize();

    }

    /**
     * 初始化非全屏状态的控制器
     */
    private void initController(){
        mAPrevButton = (ImageButton) findViewById(R.id.aprev);
        mATurnButton = (ImageButton) findViewById(R.id.aturn_button);
        mANextButton = (ImageButton) findViewById(R.id.anext);
        mAPlaySeekBar = (SeekBar) findViewById(R.id.aseekbar);
        mAPeplayButton = (ImageButton) findViewById(R.id.areplay);
        mAStopButton = (ImageButton) findViewById(R.id.astop);
        mAVolumeSeekBar = (SeekBar) findViewById(R.id.asbVolumeSlider);
        mAScaleButton = (ImageButton) findViewById(R.id.ascale_button);


        //播放前一首
        mAPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaController.mPrevButton.performClick();
            }
        });


        //播放暂停的按钮监听
        mATurnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaController.mTurnButton.performClick();

                //再点击按钮的时候做判断 如果是播放发消息更新
                if (mMediaController.mPlayer.isPlaying()){
                    mHandler.sendEmptyMessage(UniversalMediaController.SHOW_PROGRESS);
                }
            }
         });

        //播放下一首
        mANextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaController.mNextButton.performClick();
            }
        });

        //重新播放
        mAPeplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rePlay();
            }
        });


        //全屏按钮的监听
        mAScaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaController.mScaleButton.performClick();
            }
        });

        //播放进度的seekbar
        mAPlaySeekBar.setMax(1000);
        mAPlaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int newPosition = 0;
            boolean change = false;

            public void onStartTrackingTouch(SeekBar bar) {
                if (mMediaController.mPlayer == null) {
                    return;
                }
                mDragging = true;
            }

            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                if (mMediaController.mPlayer == null || !fromuser) {
                    return;
                }

                long duration = mMediaController.mPlayer.getDuration();
                long newposition = (duration * progress) / 1000L;
                newPosition = (int) newposition;
                change = true;
            }

            public void onStopTrackingTouch(SeekBar bar) {
                if (mMediaController.mPlayer == null) {
                    return;
                }
                if (change) {
                    mMediaController.mPlayer.seekTo(newPosition);
//                    if (mMediaController.mCurrentTime != null) {
//                        mMediaController.mCurrentTime.setText(mMediaController.stringForTime(newPosition));
//                    }
                }
                mDragging = false;

                setPlayProgress();

                mMediaController.updatePausePlay();

                mHandler.sendEmptyMessage(UniversalMediaController.SHOW_PROGRESS);
            }
        });


        //音量控制的seekbar
        mAVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                //系统音量和媒体音量同时更新
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, progress, 0);
                audioManager.setStreamVolume(3, progress, 0);//  3 代表  AudioManager.STREAM_MUSIC
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    /**
     * 初始化视频播放器
     */
    private void initVideoView(){
        mVideoLayout = findViewById(R.id.video_layout);
        mVideoView = (UniversalVideoView) findViewById(R.id.videoView);
        mMediaController = (UniversalMediaController) findViewById(R.id.media_controller);
        mStartButton = (Button) findViewById(R.id.btStart);

        mMediaController.setPlayPrevNextListener(this);
        mVideoView.setVideoViewCallback(this);

        mVideoView.setMediaController(mMediaController);
        setVideoPath(VIDEO_LOCAL_URL);

        //播放默认视频的按钮点击事件
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSeekPosition > 0) {
                    mVideoView.seekTo(mSeekPosition);
                }
                mVideoView.start();
                mMediaController.setTitle("1.mp4");
                mHandler.sendEmptyMessage(UniversalMediaController.SHOW_PROGRESS);
            }
        });


//        //视频播放完成时的回调
//        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//            }
//        });
    }


    /**
     * 初始化音量控制
     * @param seekBar
     */
    private void initVolume(SeekBar seekBar){
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);//获取媒体系统服务
        seekBar.setMax(100); //设置最大音量
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);  //获取当前值
        seekBar.setProgress(currentVolume);// 当前的媒体音量
        myRegisterReceiver();//注册同步更新的广播
    }

    /**
     * 设置视频区域大小
     */
    private void setVideoAreaSize() {
        mVideoLayout.post(new Runnable() {
            @Override
            public void run() {
                 cacheWidth = mVideoLayout.getWidth();
//                cachedHeight = (int) (width * 405f / 720f);
                cachedHeight = mVideoLayout.getHeight();
                ViewGroup.LayoutParams videoLayoutParams = mVideoLayout.getLayoutParams();
//                videoLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                videoLayoutParams.width = cacheWidth;
                videoLayoutParams.height = cachedHeight;
                mVideoLayout.setLayoutParams(videoLayoutParams);
            }
        });
    }

    /**
     * 设置播放路径
     */
    void setVideoPath(String path){
        //关于权限
        if (ContextCompat.checkSelfPermission(MyPlayerActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MyPlayerActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        } else {
            mVideoView.setVideoPath(path);
            mCurrentFilePosition = getFilePosition(files, getNameFormPath(path));

            mVideoView.requestFocus();
        }

    }


    /**
     *  根据视频播放进度设置mAPlaySeekBar的进度
     * @return
     */
    public int setPlayProgress() {
        if (mMediaController.mPlayer == null || mDragging) {
            return 0;
        }
        int position = mMediaController.mPlayer.getCurrentPosition();
        int duration = mMediaController.mPlayer.getDuration();
        if (mAPlaySeekBar != null) {
            //TODO 删了duration的>0保护
//            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mAPlaySeekBar.setProgress((int) pos);
//            }
            int percent = mMediaController.mPlayer.getBufferPercentage();
            mAPlaySeekBar.setSecondaryProgress(percent * 10);
        }

//        if (mMediaController.mEndTime != null)
//            mMediaController.mEndTime.setText(mMediaController.stringForTime(duration));
//        if (mMediaController.mCurrentTime != null)
//            mMediaController.mCurrentTime.setText(mMediaController.stringForTime(position));

        return position;
    }

    /**
     * 获取某一目录下文件集合的方法
     * @param path
     * @return
     */
    public String[] getFiles(String path){
        File f = new File(path);
        return f.list();
    }

    /**
     * 在String[]中 查找某一文件名 若找到返回其位置，否则返回-1
     */
    public int getFilePosition(String[] files ,String fileName){
        for (int i=0; i<files.length; i++){
            if (fileName.equals(files[i])){
                return i;
            }
        }
        return -1;
    }

    /**
     * 根据文件在集合中的位置 获取文件名
     * @param files
     * @param position
     * @return
     */
    public String getFileName(String[] files ,int position){
        if (files == null || files.length == 0){
            return null;
        }

        if (position < 0){
            return null;
        }else {
            if (position < files.length){
                return files[position];
            }else {
                return null;
            }
        }

    }

    /**
     * 根据路径得到文件名
     * 这里路径名就是“/sdcard/Download/*”
     */
    public String getNameFormPath(String path){
        String[] a  =   path.split("/");
        return a[a.length-1];
    }

    /**
     * 根据文件名得到播放路径
     * 这里路径名就是“/sdcard/Download/*”
     */
    public String getPlayPath(String fileName){
        return "/sdcard/Download/" + fileName;
    }

    /**
     * titleBar切换
     * @param show
     */
    private void switchTitleBar(boolean show) {
        android.support.v7.app.ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            if (show) {
                supportActionBar.show();
            } else {
                supportActionBar.hide();
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SEEK_POSITION_KEY, mSeekPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle outState) {
        super.onRestoreInstanceState(outState);
        mSeekPosition = outState.getInt(SEEK_POSITION_KEY);
    }


    @Override
    public void onScaleChange(boolean isFullscreen) {
        this.isFullscreen = isFullscreen;
        if (isFullscreen) {
            ViewGroup.LayoutParams layoutParams = mVideoLayout.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mVideoLayout.setLayoutParams(layoutParams);
            mStartButton.setVisibility(View.GONE);

        } else {
            ViewGroup.LayoutParams layoutParams = mVideoLayout.getLayoutParams();
            layoutParams.width = this.cacheWidth;
            layoutParams.height = this.cachedHeight;
            mVideoLayout.setLayoutParams(layoutParams);
            mStartButton.setVisibility(View.VISIBLE);
        }

        switchTitleBar(!isFullscreen);
    }

    @Override
    public void onPause(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onStart(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onBufferingStart(MediaPlayer mediaPlayer) {

    }

    @Override
    public void onBufferingEnd(MediaPlayer mediaPlayer) {

    }


    @Override
    public void prev() {
        String path;

        if (mCurrentFilePosition > 0){
            mCurrentFilePosition = mCurrentFilePosition-1;

            path = getPlayPath(getFileName(files,mCurrentFilePosition));
            setVideoPath(path);
            mVideoView.start();
            mMediaController.setTitle(getFileName(files,mCurrentFilePosition));

        }

    }

    @Override
    public void next() {
        String path;
        if (mCurrentFilePosition < files.length-1){
            mCurrentFilePosition = mCurrentFilePosition +1;
            path = getPlayPath(getFileName(files,mCurrentFilePosition));
            setVideoPath(path);
            mVideoView.start();
            mMediaController.setTitle(getFileName(files,mCurrentFilePosition));
        }

    }

    @Override
    public void rePlay() {
        String path = getPlayPath(getFileName(files,mCurrentFilePosition));
        setVideoPath(path);
        mVideoView.start();
        mMediaController.setTitle(getFileName(files,mCurrentFilePosition));

    }


    /**
     * 失去焦点时记录当前播放的位置
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            mSeekPosition = mVideoView.getCurrentPosition();
            mVideoView.pause();
        }
    }

    /**
     * 返回键的作用
     */
    @Override
    public void onBackPressed() {
        if (this.isFullscreen) {
            mVideoView.setFullscreen(false);
        } else {
            super.onBackPressed();
        }
    }


    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case UniversalMediaController.SHOW_PROGRESS:
                    pos = setPlayProgress();
                    if (!mDragging && mMediaController.mPlayer != null && mMediaController.mPlayer.isPlaying()) {
                        msg = obtainMessage(UniversalMediaController.SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };


    /**
     * 注册当音量发生变化时接收的广播
     */
    private void myRegisterReceiver(){
        MyVolumeReceiver mVolumeReceiver = new MyVolumeReceiver() ;
        IntentFilter filter = new IntentFilter() ;
        filter.addAction("android.media.VOLUME_CHANGED_ACTION") ;
        registerReceiver(mVolumeReceiver, filter) ;
    }

    /**
     * 处理音量变化时的界面显示
     * @author long
     */
    private class MyVolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //如果音量发生变化则更改seekbar的位置
            if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) ;// 当前的媒体音量
                mAVolumeSeekBar.setProgress(currVolume);
            }
        }
    }

}
