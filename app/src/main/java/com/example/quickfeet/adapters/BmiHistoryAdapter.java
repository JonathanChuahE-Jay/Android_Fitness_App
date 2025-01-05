package com.example.quickfeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickfeet.R;
import com.example.quickfeet.models.BmiHistory;

import java.util.List;

public class BmiHistoryAdapter extends RecyclerView.Adapter<BmiHistoryAdapter.ViewHolder> {

    private final List<BmiHistory> bmiHistoryList;
    private final OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(int bmiId);
    }

    public BmiHistoryAdapter(List<BmiHistory> bmiHistoryList, OnDeleteListener deleteListener) {
        this.bmiHistoryList = bmiHistoryList;
        this.deleteListener = deleteListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bmi_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BmiHistory bmiHistory = bmiHistoryList.get(position);
        holder.tvBmi.setText(String.format("BMI: %.2f", bmiHistory.getBmi()));
        holder.tvWeight.setText(String.format("Weight: %.1f kg", bmiHistory.getWeight()));
        holder.tvHeight.setText(String.format("Height: %.1f cm", bmiHistory.getHeight()));
        holder.tvDate.setText(bmiHistory.getDate());

        // Set delete button click listener
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(bmiHistory.getId()));
    }

    @Override
    public int getItemCount() {
        return bmiHistoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvBmi, tvWeight, tvHeight, tvDate;
        public Button btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvBmi = itemView.findViewById(R.id.tvBmi);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvHeight = itemView.findViewById(R.id.tvHeight);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

