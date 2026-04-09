package com.example.graduationproject.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;

public class AudioPlayer {
    private static final String TAG = "AudioPlayer";
    private MediaPlayer mediaPlayer;
    private AudioTrack audioTrack;
    private File tempFile;
    private Context context;
    private boolean isPlaying = false;
    private int pausePosition = 0;
    private float currentSpeed = 1.0f;
    private OnCompletionListener onCompletionListener;

    public interface OnCompletionListener {
        void onComplete();
    }

    public void init(Context context) {
        this.context = context;
        Log.d(TAG, "AudioPlayer 初始化完成");
    }

    // 设置倍速
    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        Log.d(TAG, "设置倍速: " + speed + "x");

        // MediaPlayer 实时倍速
        if (mediaPlayer != null && isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                Log.d(TAG, "MediaPlayer 实时调整倍速成功: " + speed + "x");
            } catch (Exception e) {
                Log.e(TAG, "MediaPlayer 实时调整倍速失败", e);
            }
        }
    }

    public float getSpeed() {
        return currentSpeed;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public void play(byte[] data) {
        try {
            stop();

            // 检查是否是 WAV 格式
            boolean isWav = data.length > 12 &&
                    data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';

            if (isWav) {
                playWavWithAudioTrack(data);
            } else {
                playWithMediaPlayer(data);
            }

        } catch (Exception e) {
            Log.e(TAG, "播放失败", e);
            if (onCompletionListener != null) {
                onCompletionListener.onComplete();
            }
        }
    }

    // MediaPlayer 播放 MP3
    private void playWithMediaPlayer(byte[] data) {
        try {
            tempFile = new File(context.getCacheDir(), "tts_" + System.currentTimeMillis() + ".mp3");
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(data);
            fos.close();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(currentSpeed));
                Log.d(TAG, "设置播放倍速: " + currentSpeed + "x");
            }

            if (pausePosition > 0) {
                mediaPlayer.seekTo(pausePosition);
                pausePosition = 0;
            }

            mediaPlayer.start();
            isPlaying = true;

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
                Log.d(TAG, "MediaPlayer 播放完成");
                if (onCompletionListener != null) {
                    onCompletionListener.onComplete();
                }
            });

            Log.d(TAG, "MediaPlayer 开始播放，倍速: " + currentSpeed + "x");

        } catch (Exception e) {
            Log.e(TAG, "MediaPlayer 播放失败", e);
        }
    }

    // AudioTrack 播放 WAV（支持倍速重采样）
    private void playWavWithAudioTrack(byte[] wavData) {
        try {
            // 解析 WAV 参数
            int sampleRate = 24000;
            int channels = 1;
            int dataOffset = 44;

            // 查找 data 块位置
            for (int i = 0; i < wavData.length - 8; i++) {
                if (wavData[i] == 'd' && wavData[i+1] == 'a' &&
                        wavData[i+2] == 't' && wavData[i+3] == 'a') {
                    dataOffset = i + 8;
                    break;
                }
            }

            // 解析采样率 (字节 24-27)
            if (wavData.length > 28) {
                sampleRate = ((wavData[24] & 0xFF) |
                        ((wavData[25] & 0xFF) << 8) |
                        ((wavData[26] & 0xFF) << 16) |
                        ((wavData[27] & 0xFF) << 24));
            }

            // 解析声道数 (字节 22-23)
            if (wavData.length > 24) {
                channels = ((wavData[22] & 0xFF) | ((wavData[23] & 0xFF) << 8));
            }

            // 提取 PCM 数据
            byte[] pcmData = new byte[wavData.length - dataOffset];
            System.arraycopy(wavData, dataOffset, pcmData, 0, pcmData.length);

            Log.d(TAG, "WAV 参数: 采样率=" + sampleRate + ", 声道=" + channels + ", PCM大小=" + pcmData.length);

            // 倍速重采样
            int finalSampleRate = sampleRate;
            if (currentSpeed != 1.0f) {
                int newSampleRate = (int)(sampleRate * currentSpeed);
                pcmData = resamplePcm(pcmData, sampleRate, newSampleRate);
                finalSampleRate = newSampleRate;
                Log.d(TAG, "倍速重采样: " + sampleRate + "Hz -> " + finalSampleRate + "Hz");
            }

            // 设置音频格式
            int channelConfig = (channels == 1) ?
                    AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;

            int bufferSize = AudioTrack.getMinBufferSize(finalSampleRate, channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT);

            if (bufferSize < pcmData.length) {
                bufferSize = pcmData.length;
            }

            // 创建 AudioTrack
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(finalSampleRate)
                            .setChannelMask(channelConfig)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();

            audioTrack.write(pcmData, 0, pcmData.length);
            audioTrack.play();
            isPlaying = true;

            // 计算播放时长
            int bytesPerSample = 2;
            int playTimeMs = (int) ((double) pcmData.length / (finalSampleRate * channels * bytesPerSample) * 1000);

            final AudioTrack track = audioTrack;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (track != null) {
                        track.stop();
                        track.release();
                    }
                    isPlaying = false;
                    Log.d(TAG, "AudioTrack 播放完成");
                    if (onCompletionListener != null) {
                        onCompletionListener.onComplete();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "停止播放失败", e);
                }
            }, playTimeMs);

            Log.d(TAG, "AudioTrack 开始播放，倍速: " + currentSpeed + "x, 时长: " + playTimeMs + "ms");

        } catch (Exception e) {
            Log.e(TAG, "WAV 播放失败", e);
            if (onCompletionListener != null) {
                onCompletionListener.onComplete();
            }
        }
    }

    // PCM 重采样（线性插值）
    private byte[] resamplePcm(byte[] input, int oldRate, int newRate) {
        if (oldRate == newRate) return input;

        float ratio = (float) oldRate / newRate;
        int inputSamples = input.length / 2;
        int outputSamples = (int) (inputSamples / ratio);
        byte[] output = new byte[outputSamples * 2];

        for (int i = 0; i < outputSamples; i++) {
            float srcPos = i * ratio;
            int srcIndex = (int) srcPos;
            float frac = srcPos - srcIndex;

            if (srcIndex + 1 < inputSamples) {
                // 线性插值
                short s1 = (short) ((input[srcIndex * 2] & 0xFF) | (input[srcIndex * 2 + 1] << 8));
                short s2 = (short) ((input[(srcIndex + 1) * 2] & 0xFF) | (input[(srcIndex + 1) * 2 + 1] << 8));
                short result = (short) (s1 + frac * (s2 - s1));

                output[i * 2] = (byte) (result & 0xFF);
                output[i * 2 + 1] = (byte) ((result >> 8) & 0xFF);
            } else if (srcIndex < inputSamples) {
                // 边界处理
                output[i * 2] = input[srcIndex * 2];
                output[i * 2 + 1] = input[srcIndex * 2 + 1];
            }
        }

        return output;
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pausePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            isPlaying = false;
            Log.d(TAG, "暂停播放，位置: " + pausePosition);
        } else if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.pause();
            isPlaying = false;
            Log.d(TAG, "AudioTrack 暂停");
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && pausePosition > 0) {
            mediaPlayer.seekTo(pausePosition);
            pausePosition = 0;
            mediaPlayer.start();
            isPlaying = true;
            Log.d(TAG, "MediaPlayer 继续播放");
        } else if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
            audioTrack.play();
            isPlaying = true;
            Log.d(TAG, "AudioTrack 继续播放");
        }
    }

    public void stop() {
        Log.d(TAG, "stop() 被调用");

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 MediaPlayer 失败", e);
            }
            mediaPlayer = null;
        }

        if (audioTrack != null) {
            try {
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop();
                }
                audioTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "释放 AudioTrack 失败", e);
            }
            audioTrack = null;
        }

        isPlaying = false;
        pausePosition = 0;

        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
            tempFile = null;
        }

        Log.d(TAG, "停止播放完成");
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void release() {
        stop();
    }
}