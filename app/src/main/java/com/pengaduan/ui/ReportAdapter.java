package com.pengaduan.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.pengaduan.R;
import com.pengaduan.api.ApiClient;
import com.pengaduan.data.Report;
import com.pengaduan.databinding.ItemReportBinding;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<Report> reports;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Report report);
    }

    public ReportAdapter(List<Report> reports, OnItemClickListener listener) {
        this.reports = reports;
        this.listener = listener;
    }

    public void updateData(List<Report> newReports) {
        this.reports = newReports;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportBinding binding = ItemReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reports.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private ItemReportBinding binding;

        public ReportViewHolder(ItemReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Report report, OnItemClickListener listener) {
            binding.tvCategory.setText(report.getCategory());
            binding.tvDescription.setText(report.getDescription());
            
            String status = report.getStatus();
            binding.tvStatus.setText(formatStatus(status));

            int statusColor;
            switch (status) {
                case "new": statusColor = R.color.status_new; break;
                case "processing": statusColor = R.color.status_processing; break;
                case "completed": statusColor = R.color.status_completed; break;
                default: statusColor = R.color.primary; break;
            }
            binding.tvStatus.setBackgroundResource(statusColor);

            // FIX: photo_url dari backend berupa path relatif ("uploads/xxx.jpg"),
            // harus digabung base URL dulu sebelum di-load Glide, kalau tidak gambar gak akan muncul
            String fullPhotoUrl = ApiClient.getImageUrl(itemView.getContext(), report.getPhotoUrl());
            Glide.with(itemView.getContext())
                    .load(fullPhotoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivReport);

            itemView.setOnClickListener(v -> listener.onItemClick(report));
        }

        private String formatStatus(String status) {
            switch (status) {
                case "new": return "Baru";
                case "processing": return "Diproses";
                case "completed": return "Selesai";
                default: return status;
            }
        }
    }
}
