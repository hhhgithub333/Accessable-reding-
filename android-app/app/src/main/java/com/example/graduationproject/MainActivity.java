package com.example.graduationproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.graduationproject.auth.LoginActivity;
import com.example.graduationproject.auth.TokenManager;
import com.example.graduationproject.core.PermissionController;
import com.example.graduationproject.core.TTSController;
import com.example.graduationproject.fragments.*;
import com.example.graduationproject.services.TextCaptureService;
import com.example.graduationproject.utils.CenterToast;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 100;

    private DrawerLayout drawer;
    private NavigationView nav;
    private TTSController tts;
    private TokenManager token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        token = new TokenManager(this);
        if (!token.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        initViews();
        initDrawer();
        initTTS();
        updateUserInfo();

        if (savedInstanceState == null) {
            switchFragment(new HomeFragment());
            nav.setCheckedItem(R.id.nav_home);
            getSupportActionBar().setTitle("主页");
        }
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = findViewById(R.id.drawer_layout);
        nav = findViewById(R.id.nav_view);
    }

    private void initDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, findViewById(R.id.toolbar), 0, 0);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        nav.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) switchFragment(new HomeFragment());
            else if (id == R.id.nav_tts) switchFragment(new TTSFragment());
            else if (id == R.id.nav_status) switchFragment(new StatusFragment());
            else if (id == R.id.nav_tips) switchFragment(new TipsFragment());
            else if (id == R.id.nav_user_center) switchFragment(new UserCenterFragment());
            else if (id == R.id.nav_logout) showLogoutDialog();

            item.setChecked(true);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void initTTS() {
        tts = new TTSController(this);
        tts.setListener(new TTSController.TTSListener() {
            @Override
            public void onPlayStateChanged(boolean isPlaying) {
                runOnUiThread(() -> updateHomeStatus());
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> CenterToast.show(MainActivity.this, "TTS错误: " + error));
            }
        });
    }

    private void updateUserInfo() {
        View header = nav.getHeaderView(0);
        TextView tv = header.findViewById(R.id.tv_username);
        String username = token.getUsername();
        if (tv != null) tv.setText(username != null ? username : "用户");
    }

    private void updateHomeStatus() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f instanceof HomeFragment) {
            ((HomeFragment) f).updateStatus();
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // 服务控制方法
    public void startService() {
        PermissionController perm = new PermissionController(this);
        if (!perm.hasOverlayPermission()) {
            CenterToast.showLong(this, "请先开启悬浮窗权限");
            perm.requestOverlayPermission(REQUEST_OVERLAY);
            return;
        }
        if (!perm.hasAccessibilityPermission()) {
            CenterToast.show(this, "请先开启无障碍服务");
            perm.openAccessibilitySettings();
            return;
        }
        tts.startService();
        updateHomeStatus();
    }

    public void stopService() {
        tts.stopService();
        updateHomeStatus();
    }

    public boolean isServiceRunning() {
        return tts.isServiceRunning();
    }

    // 权限相关方法
    public boolean isOverlayPermissionGranted() {
        return new PermissionController(this).hasOverlayPermission();
    }

    public boolean isAccessibilityServiceGranted() {
        return new PermissionController(this).hasAccessibilityPermission();
    }

    public void openOverlayPermissionSettings() {
        new PermissionController(this).requestOverlayPermission(REQUEST_OVERLAY);
    }

    public void openAccessibilityServiceSettings() {
        new PermissionController(this).openAccessibilitySettings();
    }

    // 退出登录
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (d, w) -> performLogout())
                .setNegativeButton("取消", null)
                .show();
    }

    private void performLogout() {
        if (tts != null) {
            tts.stop();
            tts.release();
        }
        token.clear();
        startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY && new PermissionController(this).hasOverlayPermission()) {
            startService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHomeStatus();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.release();
    }
}