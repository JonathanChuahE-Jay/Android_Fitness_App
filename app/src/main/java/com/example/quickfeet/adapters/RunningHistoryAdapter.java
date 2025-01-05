package com.example.quickfeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickfeet.R;
import com.example.quickfeet.models.RunningHistoryItem;

import java.util.List;
public class RunningHistoryAdapter extends RecyclerView.Adapter<RunningHistoryAdapter.HistoryViewHolder> {

    private final List<RunningHistoryItem> historyItems;
    private final OnDeleteClickListener onDeleteClickListener;

    public RunningHistoryAdapter(List<RunningHistoryItem> historyItems, OnDeleteClickListener onDeleteClickListener) {
        this.historyItems = historyItems;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_running_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        RunningHistoryItem item = historyItems.get(position);
        holder.distanceTextView.setText(String.format("Distance: %.2f m", item.getDistance()));
        holder.speedTextView.setText(String.format("Speed: %.2f km/h", item.getSpeed()));
        holder.timeTextView.setText(String.format("Time: %s", item.getTime()));
        holder.caloriesTextView.setText(String.format("Calories: %.2f kcal", item.getCalories()));

        holder.deleteButton.setOnClickListener(v -> onDeleteClickListener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView distanceTextView, speedTextView, timeTextView, caloriesTextView;
        Button deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            distanceTextView = itemView.findViewById(R.id.historyDistance);
            speedTextView = itemView.findViewById(R.id.historySpeed);
            timeTextView = itemView.findViewById(R.id.historyTime);
            caloriesTextView = itemView.findViewById(R.id.historyCalories);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(RunningHistoryItem item);
    }
}
