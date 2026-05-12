package com.example.arenafit;

import android.content.Intent;
import android.os.Bundle;
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

public class SignUp extends AppCompatActivity implements View.OnClickListener {

    private EditText username;
    private EditText password;
    private EditText email;

    private FirebaseAuth mAuth;

    private Button go_back;
    private Button signin;

    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        username = findViewById(R.id.userName);
        password = findViewById(R.id.password);
        email = findViewById(R.id.email);

        go_back = findViewById(R.id.go_back);
        signin = findViewById(R.id.sign_up_button);

        go_back.setOnClickListener(this);
        signin.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == signin) {

            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString().trim();
            String usernameText = username.getText().toString().trim();

            if (emailText.isEmpty()) {
                email.setError("Email required");
                return;
            }
            if (passwordText.isEmpty()) {
                password.setError("Password required");
                return;
            }
            if (usernameText.isEmpty()) {
                username.setError("Username required");
                return;
            }

            mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser(); // safer than mAuth.getCurrentUser()
                            if (user != null) {
                                // Save username in DB under user's UID
                                database.child(user.getUid()).child("username").setValue(usernameText)
                                        .addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                // Now move to FeedActivity
                                                Intent intent = new Intent(SignUp.this, FeedActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                intent.putExtra("username", usernameText);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(SignUp.this, "DB write failed: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(SignUp.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } else if (view == go_back) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}