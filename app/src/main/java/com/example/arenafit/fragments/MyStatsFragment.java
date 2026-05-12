package com.example.arenafit.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.arenafit.R;

public class MyStatsFragment extends Fragment {

    private Spinner workoutSpinner;
    private EditText valueInput;
    private Button submitWorkout;
    private TextView statsText;

    public MyStatsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_my_stats, container, false);

        workoutSpinner = view.findViewById(R.id.workoutSpinner);
        valueInput = view.findViewById(R.id.valueInput);
        submitWorkout = view.findViewById(R.id.submitWorkout);
        statsText = view.findViewById(R.id.statsText);

        String[] workouts = {
                "Push Ups",
                "Plank",
                "Running"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                workouts
        );

        workoutSpinner.setAdapter(adapter);

        submitWorkout.setOnClickListener(v -> submitWorkout());

        return view;
    }

    private void submitWorkout() {

        String selectedWorkout = workoutSpinner.getSelectedItem().toString();
        String value = valueInput.getText().toString().trim();

        if (TextUtils.isEmpty(value)) {
            valueInput.setError("Enter value");
            return;
        }

        String measurement = "";

        switch (selectedWorkout) {

            case "Push Ups":
                measurement = "reps";
                break;

            case "Plank":
                measurement = "seconds";
                break;

            case "Running":
                measurement = "km";
                break;
        }

        statsText.setText(
                "Last Workout:\n" +
                        selectedWorkout +
                        " - " +
                        value +
                        " " +
                        measurement
        );

        Toast.makeText(
                requireContext(),
                "Workout saved",
                Toast.LENGTH_SHORT
        ).show();
    }
}
