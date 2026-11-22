package com.myprofile.app.ui.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.myprofile.app.databinding.ActivityWelcomeBinding;
import com.myprofile.app.ui.auth.PhoneAuthActivity;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "MyProfileFlow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Welcome onCreate");
        ActivityWelcomeBinding binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCreateProfile.setOnClickListener(v -> openPhoneAuth());
        binding.btnLogin.setOnClickListener(v -> openPhoneAuth());
    }

    private void openPhoneAuth() {
        startActivity(new Intent(this, PhoneAuthActivity.class));
    }
}
