package com.example.arenafit.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.arenafit.R;
import com.example.arenafit.model.Workout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommunityFragment extends Fragment implements View.OnClickListener {

    private static final long WEEK_MS = 7L * 24L * 60L * 60L * 1000L;

    private static final String TAB_OVERALL = "overall";

    private TextView tabOverall, tabPushups, tabPlank, tabRunning;
    private LinearLayout leaderboardContainer;
    private TextView leaderboardEmpty;

    private String currentTab = TAB_OVERALL;
    private final List<UserStats> users = new ArrayList<>();

    private DatabaseReference usersRef;
    private ValueEventListener usersListener;
    private String myUid;

    public CommunityFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        tabOverall = view.findViewById(R.id.tabOverall);
        tabPushups = view.findViewById(R.id.tabPushups);
        tabPlank = view.findViewById(R.id.tabPlank);
        tabRunning = view.findViewById(R.id.tabRunning);
        leaderboardContainer = view.findViewById(R.id.leaderboardContainer);
        leaderboardEmpty = view.findViewById(R.id.leaderboardEmpty);

        View hero = view.findViewById(R.id.heroHeader);
        if (hero != null) applyTopInset(hero);

        tabOverall.setOnClickListener(this);
        tabPushups.setOnClickListener(this);
        tabPlank.setOnClickListener(this);
        tabRunning.setOnClickListener(this);

        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        myUid = me == null ? null : me.getUid();

        selectTab(TAB_OVERALL);
        attachUsersListener();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }

    private void applyTopInset(View target) {
        int basePaddingTop = target.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(target, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), basePaddingTop + bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tabOverall)      selectTab(TAB_OVERALL);
        else if (id == R.id.tabPushups) selectTab(Workout.TYPE_PUSHUPS);
        else if (id == R.id.tabPlank)   selectTab(Workout.TYPE_PLANK);
        else if (id == R.id.tabRunning) selectTab(Workout.TYPE_RUNNING);
    }

    private void selectTab(String tab) {
        currentTab = tab;
        tabOverall.setSelected(TAB_OVERALL.equals(tab));
        tabPushups.setSelected(Workout.TYPE_PUSHUPS.equals(tab));
        tabPlank.setSelected(Workout.TYPE_PLANK.equals(tab));
        tabRunning.setSelected(Workout.TYPE_RUNNING.equals(tab));
        renderLeaderboard();
    }

    private void attachUsersListener() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                long cutoff = System.currentTimeMillis() - WEEK_MS;
                users.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    UserStats stats = new UserStats();
                    stats.uid = userSnap.getKey();
                    Object nameObj = userSnap.child("username").getValue();
                    stats.username = nameObj == null ? "User" : nameObj.toString();

                    DataSnapshot workouts = userSnap.child("workouts");
                    for (DataSnapshot w : workouts.getChildren()) {
                        Workout workout = w.getValue(Workout.class);
                        if (workout == null || workout.type == null) continue;
                        if (workout.timestamp < cutoff) continue;
                        if (!workout.isCompleted()) continue;
                        switch (workout.type) {
                            case Workout.TYPE_PUSHUPS: stats.pushups += workout.userAccomplish; break;
                            case Workout.TYPE_PLANK: stats.plank += workout.userAccomplish; break;
                            case Workout.TYPE_RUNNING: stats.running += workout.userAccomplish; break;
                        }
                    }
                    stats.score = stats.pushups * Workout.weight(Workout.TYPE_PUSHUPS)
                            + stats.plank * Workout.weight(Workout.TYPE_PLANK)
                            + stats.running * Workout.weight(Workout.TYPE_RUNNING);
                    users.add(stats);
                }
                renderLeaderboard();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        usersRef.addValueEventListener(usersListener);
    }

    private void renderLeaderboard() {
        leaderboardContainer.removeAllViews();

        List<UserStats> ranked = new ArrayList<>(users);
        for (UserStats u : ranked) u.displayValue = valueFor(u, currentTab);

        Collections.sort(ranked, (a, b) -> Double.compare(b.displayValue, a.displayValue));

        boolean anyHasValue = false;
        for (UserStats u : ranked) {
            if (u.displayValue > 0) { anyHasValue = true; break; }
        }
        if (ranked.isEmpty() || !anyHasValue) {
            leaderboardEmpty.setVisibility(View.VISIBLE);
            return;
        }
        leaderboardEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int rank = 1;
        for (UserStats u : ranked) {
            if (u.displayValue <= 0) continue;
            View row = inflater.inflate(R.layout.row_leaderboard, leaderboardContainer, false);
            bindRow(row, u, rank++);
            leaderboardContainer.addView(row);
        }
    }

    private double valueFor(UserStats u, String tab) {
        if (TAB_OVERALL.equals(tab)) return u.score;
        if (Workout.TYPE_PUSHUPS.equals(tab)) return u.pushups;
        if (Workout.TYPE_PLANK.equals(tab)) return u.plank;
        if (Workout.TYPE_RUNNING.equals(tab)) return u.running;
        return 0;
    }

    private String formatValue(double v, String tab) {
        if (TAB_OVERALL.equals(tab)) {
            return Math.round(v) + " pts";
        }
        if (Workout.TYPE_PUSHUPS.equals(tab)) {
            return Math.round(v) + " reps";
        }
        if (Workout.TYPE_PLANK.equals(tab)) {
            return Math.round(v) + " sec";
        }
        if (Workout.TYPE_RUNNING.equals(tab)) {
            return stripTrailingZero(v) + " km";
        }
        return String.valueOf(v);
    }

    private String stripTrailingZero(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }

    private void bindRow(View row, UserStats u, int rank) {
        View rowRoot = row.findViewById(R.id.row_root);
        TextView rankText = row.findViewById(R.id.rank_text);
        View rankBg = row.findViewById(R.id.rank_bg);
        TextView nameView = row.findViewById(R.id.user_name);
        TextView subView = row.findViewById(R.id.user_sub);
        TextView valueView = row.findViewById(R.id.user_value);

        rankText.setText(String.valueOf(rank));

        int rankColor;
        int rankTextColor = ContextCompat.getColor(requireContext(), R.color.text_inverse);
        switch (rank) {
            case 1: rankColor = R.color.rank_gold; break;
            case 2: rankColor = R.color.rank_silver; break;
            case 3: rankColor = R.color.rank_bronze; break;
            default:
                rankColor = R.color.surface_muted;
                rankTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                break;
        }
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(ContextCompat.getColor(requireContext(), rankColor));
        rankBg.setBackground(bg);
        rankText.setTextColor(rankTextColor);

        nameView.setText(u.username);
        boolean isMe = myUid != null && myUid.equals(u.uid);
        subView.setText(isMe ? "you · this week" : "this week");

        valueView.setText(formatValue(u.displayValue, currentTab));

        if (isMe) {
            rowRoot.setBackgroundResource(R.drawable.bg_current_user_row);
        } else {
            rowRoot.setBackground(null);
        }
    }

    private static class UserStats {
        String uid;
        String username;
        double pushups;
        double plank;
        double running;
        double score;
        double displayValue;
    }
}
