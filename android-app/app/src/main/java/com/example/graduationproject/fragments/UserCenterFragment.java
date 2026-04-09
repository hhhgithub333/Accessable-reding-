package com.example.graduationproject.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.graduationproject.MainActivity;
import com.example.graduationproject.R;
import com.example.graduationproject.auth.LoginActivity;
import com.example.graduationproject.auth.TokenManager;

public class UserCenterFragment extends Fragment {

    private TextView tvUsername, tvUserId, tvCreatedAt;
    private Button btnLogout;
    private TokenManager tokenManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_center, container, false);

        tvUsername = view.findViewById(R.id.tv_username);
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvCreatedAt = view.findViewById(R.id.tv_created_at);
        btnLogout = view.findViewById(R.id.btn_logout);

        tokenManager = new TokenManager(getActivity());
        displayUserInfo();

        btnLogout.setOnClickListener(v -> logout());
        return view;
    }

    private void displayUserInfo() {
        String username = tokenManager.getUsername();
        int userId = tokenManager.getUserId();
        String createdAt = tokenManager.getCreatedAt();

        tvUsername.setText(username != null ? username : "未登录");
        tvUserId.setText(userId != -1 ? String.valueOf(userId) : "未知");
        tvCreatedAt.setText(createdAt != null ? createdAt : "暂无");
    }

    private void logout() {
        new AlertDialog.Builder(getActivity())
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> performLogout())
                .setNegativeButton("取消", null)
                .show();
    }

    private void performLogout() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).stopService();
        }
        tokenManager.clear();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}