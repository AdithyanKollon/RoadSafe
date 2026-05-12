package com.example.roadsafe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    EditText etRegName, etRegEmail, etRegPassword, etRegConfirmPassword;
    Button btnRegister;
    TextView tvNameError, tvRegEmailError, tvRegPasswordError, tvConfirmPasswordError, tvPasswordStrength;
    ProgressBar progressRegister;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etRegName            = findViewById(R.id.etRegName);
        etRegEmail           = findViewById(R.id.etRegEmail);
        etRegPassword        = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister          = findViewById(R.id.btnRegister);
        tvNameError          = findViewById(R.id.tvNameError);
        tvRegEmailError      = findViewById(R.id.tvRegEmailError);
        tvRegPasswordError   = findViewById(R.id.tvRegPasswordError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvPasswordStrength   = findViewById(R.id.tvPasswordStrength);
        progressRegister     = findViewById(R.id.progressRegister);

        // Password strength indicator (real-time)
        etRegPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String pwd = s.toString();
                if (pwd.isEmpty()) {
                    tvPasswordStrength.setVisibility(View.GONE);
                    return;
                }
                tvPasswordStrength.setVisibility(View.VISIBLE);
                if (pwd.length() < 6) {
                    tvPasswordStrength.setText("Weak");
                    tvPasswordStrength.setTextColor(0xFFC62828); // red
                } else if (pwd.length() < 10) {
                    tvPasswordStrength.setText("Medium");
                    tvPasswordStrength.setTextColor(0xFFE65100); // orange
                } else {
                    tvPasswordStrength.setText("Strong");
                    tvPasswordStrength.setTextColor(0xFF2E7D32); // green
                }
            }
        });

        btnRegister.setOnClickListener(v -> {
            // Clear errors
            tvNameError.setVisibility(View.GONE);
            tvRegEmailError.setVisibility(View.GONE);
            tvRegPasswordError.setVisibility(View.GONE);
            tvConfirmPasswordError.setVisibility(View.GONE);

            String name     = etRegName.getText().toString().trim();
            String email    = etRegEmail.getText().toString().trim();
            String password = etRegPassword.getText().toString().trim();
            String confirm  = etRegConfirmPassword.getText().toString().trim();

            boolean hasError = false;

            // Validate name
            if (name.isEmpty()) {
                tvNameError.setText("Name is required");
                tvNameError.setVisibility(View.VISIBLE);
                hasError = true;
            }

            // Validate email
            if (email.isEmpty()) {
                tvRegEmailError.setText("Email is required");
                tvRegEmailError.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvRegEmailError.setText("Please enter a valid email address");
                tvRegEmailError.setVisibility(View.VISIBLE);
                hasError = true;
            }

            // Validate password
            if (password.isEmpty()) {
                tvRegPasswordError.setText("Password is required");
                tvRegPasswordError.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (password.length() < 6) {
                tvRegPasswordError.setText("Password must be at least 6 characters");
                tvRegPasswordError.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Password must be at least 6 characters",
                        Toast.LENGTH_SHORT).show();
                hasError = true;
            }

            // Validate confirm password
            if (!password.equals(confirm)) {
                tvConfirmPasswordError.setText("Passwords do not match");
                tvConfirmPasswordError.setVisibility(View.VISIBLE);
                hasError = true;
            }

            if (hasError) return;

            // Show loading
            setLoading(true);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            setLoading(false);
                            if (task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this,
                                        "Registration successful",
                                        Toast.LENGTH_SHORT).show();
                                finish(); // go back to Login
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Error: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "" : "Register");
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
