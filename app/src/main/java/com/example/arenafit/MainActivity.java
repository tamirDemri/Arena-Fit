package com.example.arenafit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText emailInput;
    private EditText passwordInput;

    private Button signin;
    private Button signup;

    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Updated variable names (keeping your XML IDs as they are)
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.password);
        signin = findViewById(R.id.sign_in_button);
        signup = findViewById(R.id.sign_up_button);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        signin.setOnClickListener(this);
        signup.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.sign_in_button) {
            loginUser();
        } else if (id == R.id.sign_up_button) {
            startActivity(new Intent(MainActivity.this, SignUp.class));
        }
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        // STEP 1: Authenticate with EMAIL
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // STEP 2: Fetch the USERNAME from the Database using the UID
                            fetchUsernameAndProceed(user.getUid());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Login Failed: Bad Credentials", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUsernameAndProceed(String uid) {
        database.child(uid).child("username").get().addOnCompleteListener(dbTask -> {
            String displayName = "User"; // Default fallback

            if (dbTask.isSuccessful() && dbTask.getResult().exists()) {
                displayName = dbTask.getResult().getValue(String.class);
            }

            // STEP 3: Pass the USERNAME (the display name) to the next screen
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("username", displayName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}