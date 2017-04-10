package com.universalvideoviewsample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.universalvideoview.UniversalMediaController;
import com.universalvideoview.UniversalVideoView;

import java.io.File;

public class MyPlayerActivity extends AppCompatActivity implements UniversalVideoView.VideoViewCallback,
        UniversalMediaController.PlayPrevNextListener{
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;            //运行时权限
    private static final String VIDEO_LOCAL_URL = "/sdcard/Download/1.mp4";        //默认播放的视频路径


    UniversalVideoView mVideoView;                                   //播放器
    UniversalMediaController mMediaController;                       //控制器

    Button mStartButton;                                             //开始播放按钮
    View mVideoLayout;                                               //整个播放器与控制器的父布局



    private int mSeekPosition;                                       //视频播放到的位置
    private int cachedHeight;                                        //播放视频部分的高度
    private boolean isFullscreen;                                    //是否全屏

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_player);

        initVideoView();

        setVideoAreaSize();
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
     * 设置视频区域大小
     */
    private void setVideoAreaSize() {
        mVideoLayout.post(new Runnable() {
            @Override
            public void run() {
                int width = mVideoLayout.getWidth();
                cachedHeight = (int) (width * 405f / 720f);
                ViewGroup.LayoutParams videoLayoutParams = mVideoLayout.getLayoutParams();
                videoLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
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
            mVideoView.requestFocus();
        }
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
    public int findFileName(String[] files ,String fileName){
        for (int i=0; i<files.length; i++){
            if (fileName.equals(files[i])){
                return i;
            }
        }
        return -1;
    }


    @Override
    public void onScaleChange(boolean isFullscreen) {

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

    }

    @Override
    public void next() {

    }

    @Override
    public void rePlay() {

    }
}
