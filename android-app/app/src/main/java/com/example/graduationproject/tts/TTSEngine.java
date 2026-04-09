package com.example.graduationproject.tts;

import android.content.Context;

public interface TTSEngine {
    void init(Context context, String apiKey);
    void synthesize(String text, String voice, Callback callback);
    void stop();
    void release();

    interface Callback {
        void onStart();
        void onAudioData(byte[] data);
        void onComplete();
        void onError(String error);
    }
}