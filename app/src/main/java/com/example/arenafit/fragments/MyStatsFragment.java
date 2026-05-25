package com.example.arenafit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.arenafit.MainActivity;
import com.example.arenafit.R;
import com.example.arenafit.model.Workout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyStatsFragment extends Fragment {

    private static final long WEEK_MS = 7L * 24L * 60L * 60L * 1000L;

    private TextView chipPushups, chipPlank, chipRunning;
    private TextInputLayout valueInputLayout;
    private TextInputEditText valueInput;
    private MaterialButton submitWorkout;
    private ImageButton logoutButton;

    private TextView heroScore, heroPushups, heroPlank, heroRunning;
    private TextView historyEmpty;
    private LinearLayout historyContainer;
    private LinearLayout heroHeader;

    private String selectedType = Workout.TYPE_PUSHUPS;

    private DatabaseReference workoutsRef;
    private ValueEventListener workoutsListener;

    public MyStatsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_my_stats, container, false);

        heroHeader = view.findViewById(R.id.heroHeader);
        chipPushups = view.findViewById(R.id.chipPushups);
        chipPlank = view.findViewById(R.id.chipPlank);
        chipRunning = view.findViewById(R.id.chipRunning);
        valueInputLayout = view.findViewById(R.id.valueInputLayout);
        valueInput = view.findViewById(R.id.valueInput);
        submitWorkout = view.findViewById(R.id.submitWorkout);
        logoutButton = view.findViewById(R.id.logoutButton);

        heroScore = view.findViewById(R.id.heroScore);
        heroPushups = view.findViewById(R.id.heroPushups);
        heroPlank = view.findViewById(R.id.heroPlank);
        heroRunning = view.findViewById(R.id.heroRunning);
        historyEmpty = view.findViewById(R.id.historyEmpty);
        historyContainer = view.findViewById(R.id.historyContainer);

        applyTopInset(heroHeader);

        chipPushups.setOnClickListener(v -> selectType(Workout.TYPE_PUSHUPS));
        chipPlank.setOnClickListener(v -> selectType(Workout.TYPE_PLANK));
        chipRunning.setOnClickListener(v -> selectType(Workout.TYPE_RUNNING));

        submitWorkout.setOnClickListener(v -> submitWorkout());
        logoutButton.setOnClickListener(v -> logout());

        selectType(Workout.TYPE_PUSHUPS);
        attachWorkoutsListener();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (workoutsRef != null && workoutsListener != null) {
            workoutsRef.removeEventListener(workoutsListener);
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

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void selectType(String type) {
        selectedType = type;
        chipPushups.setSelected(type.equals(Workout.TYPE_PUSHUPS));
        chipPlank.setSelected(type.equals(Workout.TYPE_PLANK));
        chipRunning.setSelected(type.equals(Workout.TYPE_RUNNING));

        switch (type) {
            case Workout.TYPE_PUSHUPS:
                valueInputLayout.setHint("Reps");
                valueInputLayout.setSuffixText("reps");
                break;
            case Workout.TYPE_PLANK:
                valueInputLayout.setHint("Seconds");
                valueInputLayout.setSuffixText("sec");
                break;
            case Workout.TYPE_RUNNING:
                valueInputLayout.setHint("Distance");
                valueInputLayout.setSuffixText("km");
                break;
        }
    }

    private void submitWorkout() {
        String raw = valueInput.getText() == null ? "" : valueInput.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) {
            valueInputLayout.setError("Enter a value");
            return;
        }

        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            valueInputLayout.setError("Enter a valid number");
            return;
        }
        if (value <= 0) {
            valueInputLayout.setError("Must be greater than 0");
            return;
        }
        valueInputLayout.setError(null);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("workouts")
                .push();

        Workout w = new Workout(selectedType, value, System.currentTimeMillis());
        ref.setValue(w).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                valueInput.setText("");
                Toast.makeText(requireContext(), "Workout saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachWorkoutsListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        workoutsRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("workouts");

        Query query = workoutsRef.orderByChild("timestamp");

        workoutsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                List<Workout> all = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Workout w = child.getValue(Workout.class);
                    if (w != null && w.type != null) all.add(w);
                }
                renderHistory(all);
                renderWeeklySummary(all);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        query.addValueEventListener(workoutsListener);
    }

    private void renderHistory(List<Workout> workouts) {
        Collections.sort(workouts, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        historyContainer.removeAllViews();
        if (workouts.isEmpty()) {
            historyEmpty.setVisibility(View.VISIBLE);
            return;
        }
        historyEmpty.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Workout w : workouts) {
            View row = inflater.inflate(R.layout.row_workout_history, historyContainer, false);
            bindRow(row, w);
            historyContainer.addView(row);
        }
    }

    private void bindRow(View row, Workout w) {
        TextView title = row.findViewById(R.id.row_title);
        TextView time = row.findViewById(R.id.row_time);
        TextView valueView = row.findViewById(R.id.row_value);
        ImageView icon = row.findViewById(R.id.row_icon);
        View iconBg = row.findViewById(R.id.row_icon_bg);

        title.setText(Workout.displayName(w.type));
        time.setText(DateUtils.getRelativeTimeSpanString(
                w.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS));
        valueView.setText(formatValue(w) + " " + Workout.unit(w.type));

        switch (w.type) {
            case Workout.TYPE_PUSHUPS:
                icon.setImageResource(R.drawable.ic_pushup);
                iconBg.setBackgroundResource(R.drawable.bg_workout_pushups);
                break;
            case Workout.TYPE_PLANK:
                icon.setImageResource(R.drawable.ic_plank);
                iconBg.setBackgroundResource(R.drawable.bg_workout_plank);
                break;
            case Workout.TYPE_RUNNING:
                icon.setImageResource(R.drawable.ic_running);
                iconBg.setBackgroundResource(R.drawable.bg_workout_running);
                break;
        }
    }

    private String formatValue(Workout w) {
        if (Workout.TYPE_RUNNING.equals(w.type)) {
            return stripTrailingZero(w.value);
        }
        if (w.value == Math.floor(w.value)) {
            return String.valueOf((long) w.value);
        }
        return stripTrailingZero(w.value);
    }

    private String stripTrailingZero(double v) {
        String s = String.format(java.util.Locale.US, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }

    private void renderWeeklySummary(List<Workout> all) {
        long cutoff = System.currentTimeMillis() - WEEK_MS;
        double pushups = 0, plank = 0, running = 0;
        for (Workout w : all) {
            if (w.timestamp < cutoff) continue;
            if (Workout.TYPE_PUSHUPS.equals(w.type)) pushups += w.value;
            else if (Workout.TYPE_PLANK.equals(w.type)) plank += w.value;
            else if (Workout.TYPE_RUNNING.equals(w.type)) running += w.value;
        }
        double score = pushups * Workout.weight(Workout.TYPE_PUSHUPS)
                + plank * Workout.weight(Workout.TYPE_PLANK)
                + running * Workout.weight(Workout.TYPE_RUNNING);

        heroScore.setText(String.valueOf((long) Math.round(score)));
        heroPushups.setText(String.valueOf((long) pushups));
        heroPlank.setText(((long) plank) + "s");
        heroRunning.setText(stripTrailingZero(running) + "km");
    }
}
