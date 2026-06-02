package com.example.arenafit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arenafit.R;
import com.example.arenafit.model.Workout;
import com.example.arenafit.model.WorkoutEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutVH> {

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private final List<WorkoutEntry> entries = new ArrayList<>();
    private final OnItemClickListener listener;

    public WorkoutAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<WorkoutEntry> newEntries) {
        entries.clear();
        if (newEntries != null) entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    public WorkoutEntry getItem(int position) {
        if (position < 0 || position >= entries.size()) return null;
        return entries.get(position);
    }

    @NonNull
    @Override
    public WorkoutVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_workout_history, parent, false);
        return new WorkoutVH(row);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutVH holder, int position) {
        WorkoutEntry entry = entries.get(position);
        Workout w = entry.workout;

        holder.title.setText(Workout.displayName(w.type));

        String unit = Workout.unit(w.type);
        holder.targetView.setText("Target: " + formatNumber(w.userTarget) + " " + unit);

        if (w.isCompleted()) {
            holder.progressView.setText(formatNumber(w.userTarget) + " / " + formatNumber(w.userTarget)
                    + " " + unit + " · Done!");
        } else {
            holder.progressView.setText(formatNumber(w.userAccomplish) + " / " + formatNumber(w.userTarget)
                    + " " + unit + " · " + formatNumber(w.remaining()) + " " + unit + " left");
        }

        switch (w.type) {
            case Workout.TYPE_PUSHUPS:
                holder.icon.setImageResource(R.drawable.ic_pushup);
                holder.iconBg.setBackgroundResource(R.drawable.bg_workout_pushups);
                break;
            case Workout.TYPE_PLANK:
                holder.icon.setImageResource(R.drawable.ic_plank);
                holder.iconBg.setBackgroundResource(R.drawable.bg_workout_plank);
                break;
            case Workout.TYPE_RUNNING:
                holder.icon.setImageResource(R.drawable.ic_running);
                holder.iconBg.setBackgroundResource(R.drawable.bg_workout_running);
                break;
        }

        int colorRes;
        String stateText;
        if (w.isCompleted()) {
            stateText = "Finished";
            colorRes = R.color.success;
        } else if (w.isStarted()) {
            stateText = "In progress";
            colorRes = R.color.warning;
        } else {
            stateText = "Not started";
            colorRes = R.color.danger;
        }
        holder.stateLabel.setText(stateText);
        holder.stateLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(v, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class WorkoutVH extends RecyclerView.ViewHolder {
        final TextView title, targetView, progressView, stateLabel;
        final ImageView icon;
        final View iconBg;

        WorkoutVH(@NonNull View row) {
            super(row);
            title = row.findViewById(R.id.row_title);
            targetView = row.findViewById(R.id.row_target);
            progressView = row.findViewById(R.id.row_progress);
            icon = row.findViewById(R.id.row_icon);
            iconBg = row.findViewById(R.id.row_icon_bg);
            stateLabel = row.findViewById(R.id.row_state_label);
        }
    }

    private static String formatNumber(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return stripTrailingZero(v);
    }

    private static String stripTrailingZero(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }
}
