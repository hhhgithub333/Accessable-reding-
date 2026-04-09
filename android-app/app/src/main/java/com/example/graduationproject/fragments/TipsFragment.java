package com.example.graduationproject.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.example.graduationproject.R;

public class TipsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 布局文件中已经包含了所有帮助文本，直接加载即可
        return inflater.inflate(R.layout.fragment_tips, container, false);
    }
}