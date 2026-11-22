package com.myprofile.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;
import com.myprofile.app.databinding.ActivityHomeBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.PhoneUtils;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openWelcome();
            return;
        }
        binding.tvSignedIn.setText(getString(R.string.home_signed_in,
            PhoneUtils.maskPhone(user.getPhoneNumber())));

        binding.btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            openWelcome();
        });
    }

    private void openWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
