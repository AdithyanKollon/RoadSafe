package com.example.roadsafe;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
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

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin, btnSkip;
    TextView tvRegister, tvEmailError, tvPasswordError;
    ProgressBar progressLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        showActiveUserCount();
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        btnSkip       = findViewById(R.id.btnSkip);
        tvRegister    = findViewById(R.id.tvRegister);
        tvEmailError  = findViewById(R.id.tvEmailError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        progressLogin = findViewById(R.id.progressLogin);

        // 🔹 NORMAL FIREBASE LOGIN
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Clear previous errors
                tvEmailError.setVisibility(View.GONE);
                tvPasswordError.setVisibility(View.GONE);

                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                boolean hasError = false;

                // Validate email format
                if (email.isEmpty()) {
                    tvEmailError.setText("Email is required");
                    tvEmailError.setVisibility(View.VISIBLE);
                    hasError = true;
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tvEmailError.setText("Please enter a valid email address");
                    tvEmailError.setVisibility(View.VISIBLE);
                    hasError = true;
                }

                // Validate password length
                if (password.isEmpty()) {
                    tvPasswordError.setText("Password is required");
                    tvPasswordError.setVisibility(View.VISIBLE);
                    hasError = true;
                } else if (password.length() < 6) {
                    tvPasswordError.setText("Password must be at least 6 characters");
                    tvPasswordError.setVisibility(View.VISIBLE);
                    Toast.makeText(LoginActivity.this,
                            "Password must be at least 6 characters",
                            Toast.LENGTH_SHORT).show();
                    hasError = true;
                }

                if (hasError) return;

                // Disable button and show loading
                setLoading(true);

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(
                                new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(
                                            @NonNull Task<AuthResult> task) {

                                        setLoading(false);

                                        if (task.isSuccessful()) {
                                            Toast.makeText(
                                                    LoginActivity.this,
                                                    "Login successful",
                                                    Toast.LENGTH_SHORT).show();
                                            SharedPreferences prefs =
                                                    getSharedPreferences("RoadSafePrefs", MODE_PRIVATE);


                                            String name = email.split("@")[0];
                                            prefs.edit().putString("username", name)
                                                    .putBoolean("isLoggedIn", true)
                                                    .apply();
                                            startActivity(
                                                    new Intent(
                                                            LoginActivity.this,
                                                            HomeActivity.class
                                                    )
                                            );
                                            finish();

                                        } else {
                                            Toast.makeText(
                                                    LoginActivity.this,
                                                    "Login failed: " + (task.getException() != null
                                                            ? task.getException().getMessage()
                                                            : "Unknown error"),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
            }
        });

        // 🔹 OFFLINE BYPASS LOGIN
        btnSkip.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Offline Mode - Login Skipped",
                    Toast.LENGTH_SHORT).show();

            startActivity(new Intent(
                    LoginActivity.this,
                    HomeActivity.class
            ));
            finish();
        });

        // Register click
        tvRegister.setOnClickListener(v ->
                startActivity(
                        new Intent(LoginActivity.this,
                                RegisterActivity.class)));
    }

    private void showActiveUserCount() {
        PotholeDatabaseHelper dbHelper = new PotholeDatabaseHelper(this);
        int userCount = dbHelper.getUserCount();
        // Find your existing subtitle TextView and append user count
        TextView tvSubtitle = findViewById(R.id.tvActiveUsers);
        tvSubtitle.setText(userCount + " active reporters in your area");
    }
    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "" : "Login");
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSkip.setEnabled(!loading);
    }
}
