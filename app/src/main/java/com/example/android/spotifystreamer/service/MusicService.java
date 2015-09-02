package com.example.android.spotifystreamer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.spotifystreamer.player.PlayerAction;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by achiang on 8/26/15.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener{

    private static final String LOG_TAG = MusicService.class.getSimpleName();

    private ScheduledExecutorService schedule;
    private ScheduledFuture scheduledFuture;

    private WifiManager.WifiLock mWifiLock;
    private MediaPlayer mMediaPlayer;
    private String mPreviewUrl;
    private Handler mUiHandler;

    /**
     * Binder is an interface between service and other components.
     */
    private final IBinder mMusicBinder = new MusicBinder();

    public class MusicBinder extends Binder
    {
        public MusicService getService(){
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(LOG_TAG, "onCreate");

        mMediaPlayer = new MediaPlayer();

        // Wake lock is used to prevent the system from interfering with playback.
        // It tells the system that app is using some features that should stay available
        // even if the phone is idle.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        // Register callbacks.
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");

        return mMusicBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");

        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(LOG_TAG, "onCompletion");

        // Release the wifi lock when the music is done streaming.
        if(mWifiLock != null) {
            mWifiLock.release();
        }

        sendMsgToUiHandler(PlayerAction.DONE);
        stopCheckingPlaybackProgress();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(LOG_TAG, "onError:  " + what + " " + extra);

        mp.reset();

        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(LOG_TAG, "onSeekComplete");

        mMediaPlayer.start();

        sendMsgToUiHandler(PlayerAction.PLAY);

        stopCheckingPlaybackProgress();
        startCheckingPlaybackProgress();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(LOG_TAG, "onPrepared");

        mp.start();
        sendMsgToUiHandler(PlayerAction.PLAY);

        stopCheckingPlaybackProgress();
        startCheckingPlaybackProgress();
    }

    public void startPlayback(String previewUrl){
        Log.d(LOG_TAG, "startPlayback");

        // If the song is currently playing, do nothing.
        if(previewUrl.equals(mPreviewUrl)){
            Log.d(LOG_TAG, "in startPlayback: same track, do nothing.");
            return;
        }

        // The player should display a 'play' button before playing the song,
        // and display a 'pause' button when the song is playing.
        sendMsgToUiHandler(PlayerAction.STOP);

        // We should acquire the 'WIFI LOCK' in order to keep streaming the music
        // , and release it when we no longer need the network.
        // 'WAKE_LOCK' is also required to be declared in the manifest file.
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "theWifiLock");
        mWifiLock.acquire();

        // Sets the reference to the new song.
        mPreviewUrl = previewUrl;

        // Reset to the idle state.
        mMediaPlayer.reset();

        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            // Moves to the initialized state.
            mMediaPlayer.setDataSource(previewUrl);

            // Moves to the Prepared state.
            mMediaPlayer.prepareAsync();

        }catch(IOException e){
            Log.d(LOG_TAG, e.getMessage());
        }

    }

    public void prevPlayback(String previewUrl) {
        Log.d(LOG_TAG, "prevPlayback()");
        startPlayback(previewUrl);
    }

    public void pausePlayback() {
        Log.d(LOG_TAG, "pausePlayback");

        mMediaPlayer.pause();
        sendMsgToUiHandler(PlayerAction.PAUSE);
        stopCheckingPlaybackProgress();
    }

    public void resumePlayback(){
        Log.d(LOG_TAG, "resumePlayback");

        mMediaPlayer.start();
        sendMsgToUiHandler(PlayerAction.PLAY);
        startCheckingPlaybackProgress();
    }

    public void nextPlayback(String previewUrl){
        Log.d(LOG_TAG, "nextPlayback");
        startPlayback(previewUrl);
    }

    public void seekTo(int milliseconds){
        Log.d(LOG_TAG, "seekTo " + milliseconds);
        mMediaPlayer.seekTo(milliseconds);
    }

    public boolean musicIsPlaying(){
        return mMediaPlayer.isPlaying();
    }

    // references to the UI thread's handler for interacting with UI thread's message queue.
    public void registerHandler(Handler handler){
        if(mUiHandler != handler){
            mUiHandler = handler;
        }
    }

    private void sendMsgToUiHandler(int action) {
        if (mUiHandler != null){
            Message msg = mUiHandler.obtainMessage(action);
            mUiHandler.sendMessage(msg);
        }
    }

    private void sendMsgToUiHandler(int action, int progress, int duration){
        if (mUiHandler != null){
            Message msg = mUiHandler.obtainMessage(action, progress, duration);
            mUiHandler.sendMessage(msg);
        }
    }

    private void startCheckingPlaybackProgress(){
        schedule = Executors.newSingleThreadScheduledExecutor();

        Runnable checking = new Runnable(){
            @Override
            public void run() {
                if (mMediaPlayer.isPlaying()) {
                    int duration = mMediaPlayer.getDuration();
                    int currentPos = mMediaPlayer.getCurrentPosition();
                    sendMsgToUiHandler(PlayerAction.PROGRESS, currentPos, duration);
                    //Log.d(LOG_TAG, "Thread# " + Thread.currentThread().getId() + " current pos = " + currentPos + " , duration = " + duration);
                } else {
                    Log.d(LOG_TAG, "music is not playing yet.");
                }
            }
        };

        scheduledFuture = schedule.scheduleAtFixedRate(checking, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void stopCheckingPlaybackProgress(){
        if(schedule != null && !schedule.isShutdown()){
            //scheduledFuture.cancel(true);
            schedule.shutdownNow();
            if(schedule.isShutdown()){
                Log.d(LOG_TAG, "The ScheduledExecutorService has been shutting down");
            }else{
                Log.d(LOG_TAG, "The ScheduledExecutorService hasn't shut down");
            }
        }else{
            Log.d(LOG_TAG, "The scheduledExecutorService is null or is already shutdown");
        }
    }
}
