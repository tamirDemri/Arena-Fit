package com.example.arenafit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText username;
    private EditText password;

    private Button signin;
    private Button signup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        username = findViewById(R.id.userName); // linking between var and visual
        password = findViewById(R.id.password);
        signin = findViewById(R.id.sign_in_button);
        signup = findViewById(R.id.sign_up_button);

        signin.setOnClickListener(this); // for the button to listen
        signup.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        if(view == signin)
        {
            //sign in
        }
        else if(view == signup)
        {
            //sign up
        }
    }
}