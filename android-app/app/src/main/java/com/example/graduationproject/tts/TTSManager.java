package com.example.graduationproject.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.graduationproject.FloatingWindowManager;
import com.example.graduationproject.services.TextCaptureService;
import com.example.graduationproject.utils.AudioPlayer;
import com.example.graduationproject.utils.CenterToast;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TTSManager {
    private static final String TAG = "TTSManager";
    private static TTSManager instance;
    private Context context;
    private TTSEngine currentEngine;
    private String currentEngineName;
    private Queue<String> textQueue = new LinkedList<>();
    private AtomicBoolean isProcessing = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private Thread workThread;
    private volatile boolean workThreadRunning = false;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private AudioPlayer audioPlayer;
    private String currentVoice = "Cherry";
    private TTSListener listener;

    public interface TTSListener {
        void onStart(); void onProgress(String text); void onComplete();
        void onError(String error); void onPaused(); void onResumed();
    }

    private TTSManager() {}
    public static TTSManager getInstance() {
        if (instance == null) instance = new TTSManager();
        return instance;
    }

    public void init(Context context, String apiKey) {
        this.context = context;
        this.audioPlayer = new AudioPlayer();
        this.audioPlayer.init(context);
        this.audioPlayer.setOnCompletionListener(() -> {
            isPlaying.set(false);
            mainHandler.post(() -> {
                if (listener != null) listener.onComplete();
                FloatingWindowManager.getInstance().updatePlayState(false);
            });
        });
        Log.d(TAG, "TTSManager 初始化完成");
    }

    public void setEngine(TTSEngine engine, String engineName) {
        if (currentEngine != null) currentEngine.release();
        this.currentEngine = engine;
        this.currentEngineName = engineName;
        this.currentEngine.init(context, "");
        Log.d(TAG, "切换到引擎: " + engineName);
    }

    public void setVoice(String voice) { this.currentVoice = voice; }
    public void setSpeed(float speed) { if (audioPlayer != null) audioPlayer.setSpeed(speed); }
    public void setListener(TTSListener listener) { this.listener = listener; }

    private boolean isEngineAvailable() { return currentEngine != null; }

    public void play(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!isEngineAvailable()) {
            Log.e(TAG, "引擎不可用");
            if (listener != null) listener.onError("引擎未初始化");
            return;
        }
        Log.d(TAG, "播放: " + text);
        if (workThread != null && workThread.isAlive()) {
            try { workThread.join(500); } catch (InterruptedException e) { workThread.interrupt(); }
        }
        stop();
        isPlaying.set(false); isPaused.set(false); isProcessing.set(false);
        synchronized (textQueue) { textQueue.clear(); textQueue.offer(text); }
        startWorkThread();
    }

    public void pause() {
        if (isPaused.get()) return;
        Log.d(TAG, "暂停");
        isPaused.set(true);
        if (audioPlayer != null) audioPlayer.pause();
        if (currentEngine != null) currentEngine.stop();
        isPlaying.set(false);
        mainHandler.post(() -> { if (listener != null) listener.onPaused(); });
    }

    public void resume() {
        if (!isPaused.get()) return;
        Log.d(TAG, "继续");
        isPaused.set(false);
        if (audioPlayer != null) { audioPlayer.resume(); isPlaying.set(true); }
        mainHandler.post(() -> { if (listener != null) listener.onResumed(); });
    }

    public void togglePlayPause() {
        if (isPlaying.get()) pause();
        else if (isPaused.get()) resume();
        else {
            String text = TextCaptureService.getInstance() != null ?
                    TextCaptureService.getInstance().getCurrentScreenText() : "";
            if (text != null && !text.isEmpty()) play(text);
            else if (context != null) CenterToast.show(context, "没有捕获到文字");
        }
    }

    public void stop() {
        Log.d(TAG, "停止");
        isPaused.set(false); isPlaying.set(false);
        synchronized (textQueue) { textQueue.clear(); }
        if (currentEngine != null) currentEngine.stop();
        if (audioPlayer != null) audioPlayer.stop();
        isProcessing.set(false);
    }

    private synchronized void startWorkThread() {
        if (workThread != null && workThread.isAlive()) {
            try { workThread.join(500); } catch (InterruptedException e) { workThread.interrupt(); }
        }
        workThreadRunning = true;
        workThread = new Thread(() -> { processQueue(); workThreadRunning = false; workThread = null; });
        workThread.start();
    }

    private void processQueue() {
        while (workThreadRunning) {
            while (isPaused.get() && workThreadRunning) {
                try { Thread.sleep(100); } catch (InterruptedException e) { return; }
            }
            if (!workThreadRunning) break;
            String text;
            synchronized (textQueue) { text = textQueue.poll(); }
            if (text == null) break;
            if (!isEngineAvailable()) break;

            final Object taskLock = new Object();
            final AtomicBoolean completed = new AtomicBoolean(false);
            mainHandler.post(() -> { if (listener != null) listener.onStart(); });
            Log.d(TAG, "开始合成: " + text);

            try {
                currentEngine.synthesize(text, currentVoice, new TTSEngine.Callback() {
                    @Override public void onStart() {}
                    @Override public void onAudioData(byte[] data) {
                        if (isPaused.get()) return;
                        if (audioPlayer != null && data != null && data.length > 0) {
                            audioPlayer.play(data);
                            isPlaying.set(true);
                        }
                    }
                    @Override public void onComplete() {
                        synchronized (taskLock) { completed.set(true); taskLock.notify(); }
                        mainHandler.post(() -> { if (listener != null) listener.onProgress(text); });
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "错误: " + error);
                        synchronized (taskLock) { completed.set(true); taskLock.notify(); }
                        mainHandler.post(() -> { if (listener != null) listener.onError(error); });
                    }
                });
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "引擎任务被拒绝", e);
                mainHandler.post(() -> { if (listener != null) listener.onError("引擎不可用"); });
                break;
            }
            try {
                synchronized (taskLock) {
                    while (!completed.get() && workThreadRunning) taskLock.wait();
                }
            } catch (InterruptedException e) { break; }
        }
        isProcessing.set(false); workThreadRunning = false;
        Log.d(TAG, "队列处理完成，线程退出");
    }

    public boolean isPlaying() { return isPlaying.get(); }
    public boolean isPaused() { return isPaused.get(); }
    public void release() { stop(); if (currentEngine != null) currentEngine.release(); if (audioPlayer != null) audioPlayer.release(); }
}