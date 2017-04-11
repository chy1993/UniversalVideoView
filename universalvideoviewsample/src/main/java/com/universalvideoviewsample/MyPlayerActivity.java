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
    private static final String SEEK_POSITION_KEY = "SEEK_POSITION_KEY";


    UniversalVideoView mVideoView;                                   //播放器
    UniversalMediaController mMediaController;                       //控制器

    Button mStartButton;                                             //开始播放按钮
    View mVideoLayout;                                               //整个播放器与控制器的父布局



    private int mSeekPosition;                                       //视频播放到的位置
    private int cachedHeight;                                        //播放视频部分的高度
    private boolean isFullscreen;                                    //是否全屏

    private String[]  files;                                          //文件名集合

    int mCurrentFilePosition;                                        //当前播放的文件在集合中的位置


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_player);

        //获取该目录下所有文件名集合
        files = getFiles("/sdcard/Download");

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
//                cachedHeight = (int) (width * 405f / 720f);
                cachedHeight = mVideoLayout.getHeight();
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
            mCurrentFilePosition = getFilePosition(files, getNameFormPath(path));

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
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
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





}
