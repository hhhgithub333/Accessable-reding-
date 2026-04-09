package com.example.graduationproject.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.graduationproject.FloatingWindowManager;
import com.example.graduationproject.R;
import com.example.graduationproject.tts.*;

public class TTSFragment extends Fragment {
    private Spinner spinnerModel, spinnerVoice;
    private TextView tvStatus;
    private TTSEngine currentEngine;
    private String currentVoice = "Cherry";
    private int currentModelIndex = 0;
    private int currentVoiceIndex = 0;

    private static class ModelItem {
        String name; String[] voices; String[] voiceNames; TTSEngine engine;
        ModelItem(String name, String[] voices, String[] voiceNames, TTSEngine engine) {
            this.name = name; this.voices = voices; this.voiceNames = voiceNames; this.engine = engine;
        }
    }

    private ModelItem[] models = {
            new ModelItem("千问 TTS",
                    new String[]{"Cherry", "Serena", "Ethan", "Chelsie", "Momo", "Vivian", "Moon", "Maia", "Kai", "Nofish", "Bella", "Jennifer", "Ryan", "Katerina", "Aiden", "Eldric Sage", "Mia", "Mochi", "Bellona", "Vincent", "Bunny", "Neil", "Elias", "Arthur"},
                    new String[]{"🍒 Cherry - 芊悦", "⭐ Serena - 苏瑶", "👨 Ethan - 晨煦", "🌸 Chelsie - 千雪", "🐰 Momo - 茉兔", "💜 Vivian - 十三", "🌙 Moon - 月白", "📚 Maia - 四月", "🎧 Kai - 凯", "🐟 Nofish - 不吃鱼", "👶 Bella - 萌宝", "🎬 Jennifer - 詹妮弗", "🎭 Ryan - 甜茶", "👑 Katerina - 卡捷琳娜", "🍳 Aiden - 艾登", "🧓 Eldric Sage - 沧明子", "👧 Mia - 乖小妹", "🧒 Mochi - 沙小弥", "⚔️ Bellona - 燕铮莺", "🎸 Vincent - 田叔", "🐰 Bunny - 萌小姬", "📺 Neil - 阿闻", "🎓 Elias - 墨讲师", "👴 Arthur - 徐大爷"},
                    new QwenTTSEngine()),
            new ModelItem("CosyVoice",
                    new String[]{"longanyang", "longanhuan", "longhuhu_v3", "longpaopao_v3", "longjielidou_v3", "longxian_v3", "longling_v3", "longshanshan_v3", "longniuniu_v3", "longjiaxin_v3", "longjiayi_v3", "longanyue_v3", "longlaotie_v3", "longshange_v3", "longanmin_v3", "loongkyong_v3", "loongriko_v3", "loongtomoka_v3", "longfei_v3"},
                    new String[]{"🦁 龙安洋", "🦊 龙安欢", "🧒 龙呼呼", "🐉 龙泡泡", "😄 龙杰力豆", "✨ 龙仙", "🎵 龙铃", "🌟 龙闪闪", "💪 龙牛牛", "🗣️ 龙嘉欣", "🎤 龙嘉怡", "🎭 龙安粤", "🎯 龙老铁", "🏔️ 龙陕哥", "🌸 龙安闽", "🇰🇷 loongkyong", "🇯🇵 Riko", "🇯🇵 loongtomoka", "📖 龙飞"},
                    new CosyVoiceTTSEngine()),
            new ModelItem("Sambert",
                    new String[]{"zhinan", "zhiqi", "zhichu", "zhide", "zhijia", "zhiru", "zhiqian", "zhixiang", "zhiwei", "zhihao", "zhijing", "zhiming", "zhimo", "zhina", "zhishu", "zhistella", "zhiting", "zhixiao", "zhiya", "zhiye", "zhiying", "zhiyuan", "zhiyue", "zhigui", "zhishuo", "zhimiao-emo", "zhimao"},
                    new String[]{"👨 知楠", "👩 知琪", "👨 知厨", "👨 知德", "👩 知佳", "👩 知茹", "👩 知倩", "👨 知祥", "👩 知薇", "👨 知浩", "👩 知婧", "👨 知茗", "👨 知墨", "👩 知娜", "👨 知树", "👩 知莎", "👩 知婷", "👩 知笑", "👩 知雅", "👨 知晔", "👩 知颖", "👩 知媛", "👩 知悦", "👩 知柜", "👨 知硕", "👩 知妙", "👩 知猫"},
                    new SambertTTSEngine()),
            new ModelItem("XTTS-v2",
                    new String[]{"default"},
                    new String[]{"🎤 默认声音（需要参考音频）"},
                    new XTTSv2Engine()),
            new ModelItem("VoxCPM",
                    new String[]{"default"},
                    new String[]{"🎤 默认声音"},
                    new VoxCPMEngine()),
            new ModelItem("ChatterBox",
                    new String[]{"default"},
                    new String[]{"🎤 默认声音"},
                    new ChatterBoxEngine()),
            new ModelItem("VibeVoice",
                    new String[]{"en-Mike_man", "en-Emma_woman", "en-Carter_man", "en-Davis_man", "en-Frank_man", "en-Grace_woman"},
                    new String[]{"🇺🇸 Mike - 英文男声", "🇺🇸 Emma - 英文女声", "🇺🇸 Carter - 英文男声", "🇺🇸 Davis - 英文男声", "🇺🇸 Frank - 英文男声", "🇺🇸 Grace - 英文女声"},
                    new VibeVoiceEngine()),
            new ModelItem("IndexTTS",
                    new String[]{"default"},
                    new String[]{"🎤 默认声音"},
                    new IndexTTSEngine()),
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_test_tts, container, false);
        spinnerModel = view.findViewById(R.id.spinner_model);
        spinnerVoice = view.findViewById(R.id.spinner_voice);
        tvStatus = view.findViewById(R.id.tv_tts_status);
        for (ModelItem model : models) model.engine.init(getActivity(), "");
        currentEngine = models[0].engine;
        String[] modelNames = new String[models.length];
        for (int i = 0; i < models.length; i++) modelNames[i] = models[i].name;
        spinnerModel.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, modelNames));
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentModelIndex = position; currentEngine = models[position].engine; updateVoiceSpinner(); applySettings();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        updateVoiceSpinner();
        spinnerVoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentVoiceIndex = position; currentVoice = models[currentModelIndex].voices[position]; applySettings();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        return view;
    }

    private void updateVoiceSpinner() {
        ModelItem model = models[currentModelIndex];
        if (currentVoiceIndex >= model.voiceNames.length) currentVoiceIndex = 0;
        spinnerVoice.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.spinner_dropdown_item, model.voiceNames));
    }

    private void applySettings() {
        TTSManager ttsManager = TTSManager.getInstance();
        ttsManager.setEngine(currentEngine, models[currentModelIndex].name);
        String currentVoiceName = currentVoiceIndex < models[currentModelIndex].voices.length ? models[currentModelIndex].voices[currentVoiceIndex] : "default";
        ttsManager.setVoice(currentVoiceName);
        String settingsText = models[currentModelIndex].name + " · " + models[currentModelIndex].voiceNames[currentVoiceIndex];
        FloatingWindowManager.getInstance().updateSettings(settingsText);
        tvStatus.setText("✅ 已应用: " + settingsText);
    }

    @Override public void onDestroy() { super.onDestroy(); for (ModelItem model : models) model.engine.release(); }
}