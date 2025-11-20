package com.example.ui_coen390;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PollutantAdapter extends RecyclerView.Adapter<PollutantAdapter.PollutantViewHolder> {

    private List<Pollutant> pollutants;

    public PollutantAdapter(List<Pollutant> pollutants) {
        this.pollutants = pollutants;
    }

    @NonNull
    @Override
    public PollutantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pollutant_item, parent, false);
        return new PollutantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PollutantViewHolder holder, int position) {
        Pollutant pollutant = pollutants.get(position);
        holder.pollutantNameTextView.setText(pollutant.getName());
        holder.pollutantValueTextView.setText(pollutant.getValue());
    }

    @Override
    public int getItemCount() {
        return pollutants.size();
    }

    public static class PollutantViewHolder extends RecyclerView.ViewHolder {
        TextView pollutantNameTextView;
        TextView pollutantValueTextView;

        public PollutantViewHolder(@NonNull View itemView) {
            super(itemView);
            pollutantNameTextView = itemView.findViewById(R.id.pollutantNameTextView);
            pollutantValueTextView = itemView.findViewById(R.id.pollutantValueTextView);
        }
    }
}
