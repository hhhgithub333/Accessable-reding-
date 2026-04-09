package com.example.graduationproject;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.graduationproject.tts.TTSManager;
import com.example.graduationproject.utils.CenterToast;

public class FloatingWindowManager {

    private static final String TAG = "FloatingWindowManager";
    private static FloatingWindowManager instance;
    private WindowManager windowManager;
    private View floatingView;
    private Context context;
    private boolean isShowing = false;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    private ImageButton btnPlayPause;
    private ImageButton btnStop;
    private TextView tvStatus;
    private TextView tvCurrentSettings;

    private boolean isModelSelected = false;
    private ImageButton btnSpeed;
    private TextView tvSpeed;
    private float[] speedValues = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private int speedIndex = 2;
    private String[] speedTexts = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"};

    private FloatingWindowManager() {}

    public static FloatingWindowManager getInstance() {
        if (instance == null) {
            instance = new FloatingWindowManager();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private void showCenterToast(String message) {
        CenterToast.show(context, message);
    }

    public void show() {
        if (isShowing || windowManager == null) return;

        floatingView = LayoutInflater.from(context).inflate(R.layout.floating_window, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.TOP | Gravity.START;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        params.x = 0;
        params.y = screenHeight - 200;

        btnPlayPause = floatingView.findViewById(R.id.btn_play_pause);
        btnStop = floatingView.findViewById(R.id.btn_stop);
        tvStatus = floatingView.findViewById(R.id.tv_status);
        tvCurrentSettings = floatingView.findViewById(R.id.tv_current_settings);
        ImageButton btnClose = floatingView.findViewById(R.id.btn_close);

        btnSpeed = floatingView.findViewById(R.id.btn_speed);
        tvSpeed = floatingView.findViewById(R.id.tv_speed);
        tvSpeed.setText(speedTexts[speedIndex]);

        btnSpeed.setOnClickListener(v -> {
            speedIndex = (speedIndex + 1) % speedValues.length;
            float newSpeed = speedValues[speedIndex];
            tvSpeed.setText(speedTexts[speedIndex]);

            TTSManager ttsManager = TTSManager.getInstance();
            if (ttsManager != null) {
                ttsManager.setSpeed(newSpeed);
            }
            showCenterToast("播放速度: " + speedTexts[speedIndex]);
        });

        btnClose.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).stopService();
            } else {
                TTSManager.getInstance().stop();
                hide();
            }
        });

        tvCurrentSettings.setText("请先在侧边栏选择模型和音色");

        btnPlayPause.setOnClickListener(v -> {
            if (!isModelSelected) {
                showCenterToast("请先在侧边栏选择模型和音色");
                return;
            }

            TTSManager ttsManager = TTSManager.getInstance();

            if (ttsManager.isPlaying()) {
                ttsManager.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
                updateStatusText("已暂停");
            } else if (ttsManager.isPaused()) {
                ttsManager.resume();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                updateStatusText("播放中");
            } else {
                String currentText = com.example.graduationproject.services.TextCaptureService.getInstance() != null ?
                        com.example.graduationproject.services.TextCaptureService.getInstance().getCurrentScreenText() : "";
                if (currentText != null && !currentText.isEmpty()) {
                    ttsManager.play(currentText);
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    updateStatusText("播放中");
                } else {
                    showCenterToast("没有捕获到文字");
                }
            }
        });

        btnStop.setOnClickListener(v -> {
            TTSManager.getInstance().stop();
            btnPlayPause.setImageResource(R.drawable.ic_play);
            updateStatusText("已就绪");
        });

        View rootView = floatingView.findViewById(R.id.floating_root);
        if (rootView == null) rootView = floatingView;

        rootView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });

        try {
            windowManager.addView(floatingView, params);
            isShowing = true;
            updateStatusText("已就绪");
        } catch (Exception e) {
            Log.e(TAG, "悬浮窗显示失败", e);
        }
    }

    private void updateStatusText(String status) {
        if (tvStatus != null) {
            switch (status) {
                case "播放中":
                    tvStatus.setText("● 播放中");
                    tvStatus.setTextColor(0xFF4CAF50);
                    break;
                case "已暂停":
                    tvStatus.setText("⏸ 已暂停");
                    tvStatus.setTextColor(0xFFFF9800);
                    break;
                case "已就绪":
                    tvStatus.setText("● 已就绪");
                    tvStatus.setTextColor(0xFF4CAF50);
                    break;
            }
        }
    }

    public void updateSettings(String settings) {
        if (tvCurrentSettings != null) {
            tvCurrentSettings.setText(settings);
            tvCurrentSettings.setTextColor(0xFF9CA3AF);
            isModelSelected = true;
        }
    }

    public void reset() {
        isModelSelected = false;
        if (tvCurrentSettings != null) {
            tvCurrentSettings.setText("请先在侧边栏选择模型和音色");
            tvCurrentSettings.setTextColor(0xFFFF9800);
        }
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
        if (tvStatus != null) {
            tvStatus.setText("● 已就绪");
            tvStatus.setTextColor(0xFF4CAF50);
        }
    }

    public void updatePlayState(boolean isPlaying) {
        if (btnPlayPause != null) {
            if (isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                updateStatusText("播放中");
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play);
                updateStatusText("已就绪");
            }
        }
    }

    public void hide() {
        if (isShowing && floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败", e);
            }
            floatingView = null;
            isShowing = false;
        }
    }

    public boolean isShowing() {
        return isShowing;
    }
}