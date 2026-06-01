package com.example.arenafit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class FeedActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private ImageButton logoutButton;
    private TextView feedTitle;
    private MaterialButton getStartedButton;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        feedTitle = findViewById(R.id.feed_title);
        logoutButton = findViewById(R.id.logout_button);
        getStartedButton = findViewById(R.id.get_started_button);

        username = getIntent().getStringExtra("username");
        feedTitle.setText("Hello, " + (username == null ? "athlete" : username));

        logoutButton.setOnClickListener(this);
        getStartedButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.logout_button) {
            mAuth.signOut();
            Intent intent = new Intent(FeedActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if (id == R.id.get_started_button) {
            Intent intent = new Intent(FeedActivity.this, HomeActivity.class);
            intent.putExtra("username", username);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
