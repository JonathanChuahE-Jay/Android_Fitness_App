package com.example.quickfeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quickfeet.R;
import com.example.quickfeet.models.WorkoutLog;

import java.util.List;

public class WorkoutLogAdapter extends RecyclerView.Adapter<WorkoutLogAdapter.WorkoutLogViewHolder> {

    private List<WorkoutLog> workoutLogs;
    private final OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(WorkoutLog log);
    }

    public WorkoutLogAdapter(List<WorkoutLog> workoutLogs, OnDeleteClickListener deleteListener) {
        this.workoutLogs = workoutLogs;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public WorkoutLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_log, parent, false);
        return new WorkoutLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutLogViewHolder holder, int position) {
        WorkoutLog log = workoutLogs.get(position);
        holder.tvWorkoutName.setText(log.getWorkoutName());
        holder.tvSetsReps.setText(String.format("Sets: %d, Reps: %d", log.getSets(), log.getReps()));
        holder.tvWeight.setText(String.format("Weight: %.2f kg", log.getWeight()));

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(log));
    }

    @Override
    public int getItemCount() {
        return workoutLogs.size();
    }

    static class WorkoutLogViewHolder extends RecyclerView.ViewHolder {
        TextView tvWorkoutName, tvSetsReps, tvWeight;
        Button btnDelete;

        WorkoutLogViewHolder(View itemView) {
            super(itemView);
            tvWorkoutName = itemView.findViewById(R.id.tvWorkoutName);
            tvSetsReps = itemView.findViewById(R.id.tvSetsReps);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
