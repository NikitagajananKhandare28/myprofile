package com.myprofile.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.myprofile.app.databinding.ActivitySplashBinding;
import com.myprofile.app.ui.dashboard.HomeActivity;
import com.myprofile.app.ui.welcome.WelcomeActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "MyProfileFlow";
    private static final long SPLASH_DELAY_MS = 1500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goNext = () -> {
        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        Log.d(TAG, "Splash finished, loggedIn=" + loggedIn);
        Class<?> target = loggedIn ? HomeActivity.class : WelcomeActivity.class;
        startActivity(new Intent(SplashActivity.this, target));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Splash onCreate");
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        handler.postDelayed(goNext, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(goNext);
        super.onDestroy();
    }
}
