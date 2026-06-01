package com.example.arenafit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arenafit.MainActivity;
import com.example.arenafit.R;
import com.example.arenafit.adapters.WorkoutAdapter;
import com.example.arenafit.model.Workout;
import com.example.arenafit.model.WorkoutEntry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

public class MyStatsFragment extends Fragment
        implements WorkoutAdapter.OnItemClickListener, View.OnClickListener {

    private static final long WEEK_MS = 7L * 24L * 60L * 60L * 1000L;

    private TextView chipPushups, chipPlank, chipRunning;
    private TextInputLayout valueInputLayout;
    private TextInputEditText valueInput;
    private MaterialButton submitWorkout;
    private ImageButton logoutButton;

    private TextView heroScore, heroPushups, heroPlank, heroRunning;
    private TextView historyEmpty;
    private RecyclerView historyRecycler;
    private LinearLayout heroHeader;

    private WorkoutAdapter adapter;

    private String selectedType = Workout.TYPE_PUSHUPS;

    private DatabaseReference workoutsRef;
    private ValueEventListener workoutsListener;

    private CountDownTimer activeTimer;
    private AlertDialog activeDialog;

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
        historyRecycler = view.findViewById(R.id.historyRecycler);

        adapter = new WorkoutAdapter(this);
        historyRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyRecycler.setAdapter(adapter);

        applyTopInset(heroHeader);

        chipPushups.setOnClickListener(this);
        chipPlank.setOnClickListener(this);
        chipRunning.setOnClickListener(this);
        submitWorkout.setOnClickListener(this);
        logoutButton.setOnClickListener(this);

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
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.chipPushups)        selectType(Workout.TYPE_PUSHUPS);
        else if (id == R.id.chipPlank)     selectType(Workout.TYPE_PLANK);
        else if (id == R.id.chipRunning)   selectType(Workout.TYPE_RUNNING);
        else if (id == R.id.submitWorkout) submitWorkout();
        else if (id == R.id.logoutButton)  logout();
    }

    @Override
    public void onItemClick(View view, int position) {
        WorkoutEntry entry = adapter.getItem(position);
        if (entry != null) showActionDialog(entry);
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
                valueInputLayout.setHint("Target reps");
                valueInputLayout.setSuffixText("reps");
                break;
            case Workout.TYPE_PLANK:
                valueInputLayout.setHint("Target seconds");
                valueInputLayout.setSuffixText("sec");
                break;
            case Workout.TYPE_RUNNING:
                valueInputLayout.setHint("Target distance");
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

        double target;
        try {
            target = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            valueInputLayout.setError("Enter a valid number");
            return;
        }
        if (target <= 0) {
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

        Workout w = new Workout(selectedType, target, 0.0, System.currentTimeMillis());
        ref.setValue(w).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                valueInput.setText("");
                Toast.makeText(requireContext(), "Workout added", Toast.LENGTH_SHORT).show();
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

        workoutsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                List<WorkoutEntry> all = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Workout w = child.getValue(Workout.class);
                    if (w != null && w.type != null && w.userTarget > 0) {
                        all.add(new WorkoutEntry(child.getKey(), w));
                    }
                }
                renderHistory(all);
                renderWeeklySummary(all);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        workoutsRef.addValueEventListener(workoutsListener);
    }

    private void renderHistory(List<WorkoutEntry> entries) {
        Collections.sort(entries, (a, b) -> Long.compare(b.workout.timestamp, a.workout.timestamp));

        if (entries.isEmpty()) {
            historyEmpty.setVisibility(View.VISIBLE);
            historyRecycler.setVisibility(View.GONE);
        } else {
            historyEmpty.setVisibility(View.GONE);
            historyRecycler.setVisibility(View.VISIBLE);
        }
        adapter.submit(entries);
    }

    private void showActionDialog(WorkoutEntry entry) {
        Workout w = entry.workout;
        String typeName = Workout.displayName(w.type);
        String unit = Workout.unit(w.type);

        String title;
        String message;
        String confirmLabel;

        if (w.isCompleted()) {
            title = "Restart this workout?";
            message = "A new " + typeName.toLowerCase(Locale.US)
                    + " entry will be created — old one stays.";
            confirmLabel = "Restart";
        } else if (w.isStarted()) {
            title = "Resume " + typeName + " workout?";
            message = formatNumber(w.userAccomplish) + " of " + formatNumber(w.userTarget)
                    + " " + unit + " completed\n"
                    + formatNumber(w.remaining()) + " " + unit + " remaining";
            confirmLabel = "Resume";
        } else {
            title = "Start " + typeName + " workout?";
            message = "Target: " + formatNumber(w.userTarget) + " " + unit;
            confirmLabel = "Start";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(confirmLabel, (d, which) -> performAction(entry))
                .show();
    }

    private void performAction(WorkoutEntry entry) {
        Workout w = entry.workout;

        if (w.isCompleted()) {
            if (Workout.TYPE_PLANK.equals(w.type)) {
                DatabaseReference newRef = workoutsRef.push();
                String newKey = newRef.getKey();
                Workout copy = new Workout(w.type, w.userTarget, 0.0, System.currentTimeMillis());
                newRef.setValue(copy).addOnSuccessListener(unused -> {
                    if (isAdded() && newKey != null) openPlankTimer(newKey, copy);
                });
            } else {
                Toast.makeText(requireContext(), "Under construction", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (Workout.TYPE_PLANK.equals(w.type)) {
            openPlankTimer(entry.key, w);
        } else {
            Toast.makeText(requireContext(), "Under construction", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPlankTimer(String key, Workout w) {
        double remainingSec = w.userTarget - w.userAccomplish;
        if (remainingSec <= 0) return;
        long totalMs = (long) Math.ceil(remainingSec * 1000.0);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View content = inflater.inflate(R.layout.dialog_plank_timer, null, false);
        TextView titleText = content.findViewById(R.id.timerTitle);
        TextView timerText = content.findViewById(R.id.timerText);
        MaterialButton stopButton = content.findViewById(R.id.timerStopButton);

        titleText.setText("Plank · " + formatNumber(w.userTarget) + " sec target");
        timerText.setText(formatMmSs(totalMs));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(content)
                .setCancelable(false)
                .create();

        final long[] msLeft = {totalMs};
        final double startAccomplish = w.userAccomplish;
        final double target = w.userTarget;

        CountDownTimer timer = new CountDownTimer(totalMs, 100) {
            @Override
            public void onTick(long ms) {
                msLeft[0] = ms;
                timerText.setText(formatMmSs(ms));
            }

            @Override
            public void onFinish() {
                msLeft[0] = 0;
                workoutsRef.child(key).child("userAccomplish").setValue(target);
                if (dialog.isShowing()) dialog.dismiss();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Workout complete!", Toast.LENGTH_SHORT).show();
                }
                activeTimer = null;
                activeDialog = null;
            }
        };

        stopButton.setOnClickListener(v -> {
            timer.cancel();
            long elapsedMs = totalMs - msLeft[0];
            double elapsedSec = elapsedMs / 1000.0;
            double newAccomplish = Math.min(target, startAccomplish + elapsedSec);
            workoutsRef.child(key).child("userAccomplish").setValue(newAccomplish);
            dialog.dismiss();
            activeTimer = null;
            activeDialog = null;
        });

        activeTimer = timer;
        activeDialog = dialog;
        dialog.show();
        timer.start();
    }

    private String formatMmSs(long ms) {
        long totalSec = (ms + 999) / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    private String formatNumber(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return stripTrailingZero(v);
    }

    private String stripTrailingZero(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }

    private void renderWeeklySummary(List<WorkoutEntry> all) {
        long cutoff = System.currentTimeMillis() - WEEK_MS;
        double pushups = 0, plank = 0, running = 0;
        for (WorkoutEntry e : all) {
            Workout w = e.workout;
            if (w.timestamp < cutoff) continue;
            if (!w.isCompleted()) continue;
            if (Workout.TYPE_PUSHUPS.equals(w.type)) pushups += w.userAccomplish;
            else if (Workout.TYPE_PLANK.equals(w.type)) plank += w.userAccomplish;
            else if (Workout.TYPE_RUNNING.equals(w.type)) running += w.userAccomplish;
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
