package com.myprofile.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.myprofile.app.R;
import com.myprofile.app.databinding.ActivityPhoneAuthBinding;
import com.myprofile.app.ui.dashboard.HomeActivity;
import com.myprofile.app.util.PhoneUtils;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    private static final String TAG = "PhoneAuth";
    private static final long RESEND_SECONDS = 30L;
    private static final int MAX_SEND_ATTEMPTS = 5;

    private ActivityPhoneAuthBinding binding;
    private FirebaseAuth auth;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String e164Number;
    private CountDownTimer resendTimer;
    private int sendAttempts = 0;

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                signIn(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "Verification failed", e);
                setLoading(false);
                showError(messageFor(e));
            }

            @Override
            public void onCodeSent(@NonNull String id,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                verificationId = id;
                resendToken = token;
                setLoading(false);
                showOtpPanel();
                startResendTimer();
            }
        };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhoneAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        binding.btnSendCode.setOnClickListener(v -> onSendClicked());
        binding.btnVerify.setOnClickListener(v -> onVerifyClicked());
        binding.btnResend.setOnClickListener(v -> requestCode(true));
        binding.btnChangeNumber.setOnClickListener(v -> showPhonePanel());
    }

    private void onSendClicked() {
        binding.tilCountry.setError(null);
        binding.tilPhone.setError(null);

        String code = PhoneUtils.normalize(binding.etCountryCode.getText() == null
            ? "" : binding.etCountryCode.getText().toString());
        String number = PhoneUtils.normalize(binding.etPhone.getText() == null
            ? "" : binding.etPhone.getText().toString());

        if (!PhoneUtils.isValidCountryCode(code)) {
            binding.tilCountry.setError(getString(R.string.err_invalid_country));
            return;
        }
        if (!PhoneUtils.isValidNumber(code, number)) {
            binding.tilPhone.setError(getString(R.string.err_invalid_mobile));
            return;
        }
        e164Number = code + number;
        requestCode(false);
    }

    private void requestCode(boolean isResend) {
        if (e164Number == null) {
            return;
        }
        if (sendAttempts >= MAX_SEND_ATTEMPTS) {
            showError(getString(R.string.err_limit_reached));
            return;
        }
        sendAttempts++;
        setLoading(true);

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164Number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks);
        if (isResend && resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }
        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    private void onVerifyClicked() {
        binding.tilOtp.setError(null);
        String code = binding.etOtp.getText() == null
            ? "" : binding.etOtp.getText().toString().trim();
        if (code.length() != 6 || verificationId == null) {
            binding.tilOtp.setError(getString(R.string.err_invalid_otp));
            return;
        }
        signIn(PhoneAuthProvider.getCredential(verificationId, code));
    }

    private void signIn(PhoneAuthCredential credential) {
        setLoading(true);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                goHome();
            } else {
                Exception e = task.getException();
                Log.e(TAG, "Sign-in failed", e);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    binding.tilOtp.setError(getString(R.string.err_incorrect_code));
                } else {
                    showError(messageFor(e));
                }
            }
        });
    }

    private String messageFor(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.err_invalid_phone_firebase);
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return getString(R.string.err_too_many);
        }
        if (e instanceof FirebaseNetworkException) {
            return getString(R.string.err_network);
        }
        return getString(R.string.err_generic);
    }

    private void showOtpPanel() {
        binding.panelPhone.setVisibility(View.GONE);
        binding.panelOtp.setVisibility(View.VISIBLE);
        binding.tvAuthTitle.setText(R.string.auth_title_otp);
        binding.tvAuthSubtitle.setVisibility(View.GONE);
        binding.tvOtpSentTo.setText(getString(R.string.otp_sent_to,
            PhoneUtils.maskPhone(e164Number)));
        binding.etOtp.setText("");
        binding.etOtp.requestFocus();
    }

    private void showPhonePanel() {
        cancelTimer();
        binding.panelOtp.setVisibility(View.GONE);
        binding.panelPhone.setVisibility(View.VISIBLE);
        binding.tvAuthTitle.setText(R.string.auth_title_phone);
        binding.tvAuthSubtitle.setVisibility(View.VISIBLE);
        verificationId = null;
        resendToken = null;
    }

    private void startResendTimer() {
        cancelTimer();
        binding.btnResend.setEnabled(false);
        resendTimer = new CountDownTimer(RESEND_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = (millisUntilFinished + 999L) / 1000L;
                binding.btnResend.setText(getString(R.string.resend_in, (int) seconds));
            }

            @Override
            public void onFinish() {
                binding.btnResend.setText(R.string.resend_code);
                binding.btnResend.setEnabled(true);
            }
        }.start();
    }

    private void cancelTimer() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSendCode.setEnabled(!loading);
        binding.btnVerify.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        cancelTimer();
        super.onDestroy();
    }
}
