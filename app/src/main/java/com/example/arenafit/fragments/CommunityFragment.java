package com.example.arenafit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.arenafit.R;

public class CommunityFragment extends Fragment {

    public CommunityFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_community,
                container,
                false
        );

        TextView leaderboard = view.findViewById(R.id.leaderboardText);

        leaderboard.setText(
                "Leaderboard\n\n" +
                        "1. Tom - 120 Push Ups\n" +
                        "2. Daniel - 95 Push Ups\n" +
                        "3. Ben - 80 Push Ups"
        );

        return view;
    }
}
