package com.example.rook;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rvRecentActivity = findViewById(R.id.rvRecentActivity);
        // We use setNestedScrollingEnabled(false) in XML, so a normal LinearLayoutManager is fine
        rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        
        // Setup dummy data mirroring the HTML design
        List<ApiActivity> activities = new ArrayList<>();
        activities.add(new ApiActivity("POST /v1/auth/login", "2 mins ago", "200 OK", "#a8e6cf", "#2c6957", "#2c6956"));
        activities.add(new ApiActivity("GET /v1/user/profile", "15 mins ago", "404 NOT FOUND", "#ffdad6", "#93000a", "#ba1a1a"));
        activities.add(new ApiActivity("PUT /v1/settings", "45 mins ago", "201 CREATED", "#fdd1b4", "#785841", "#785741"));
        
        RecentActivityAdapter adapter = new RecentActivityAdapter(activities);
        rvRecentActivity.setAdapter(adapter);
    }

    // Model class for the database later
    public static class ApiActivity {
        String endpoint;
        String time;
        String status;
        String badgeColor;
        String textColor;
        String dotColor;

        public ApiActivity(String endpoint, String time, String status, String badgeColor, String textColor, String dotColor) {
            this.endpoint = endpoint;
            this.time = time;
            this.status = status;
            this.badgeColor = badgeColor;
            this.textColor = textColor;
            this.dotColor = dotColor;
        }
    }

    // Adapter class for RecyclerView
    public static class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

        private final List<ApiActivity> items;

        public RecentActivityAdapter(List<ApiActivity> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ApiActivity item = items.get(position);
            holder.tvEndpoint.setText(item.endpoint);
            holder.tvTime.setText(item.time);
            holder.tvStatusCode.setText(item.status);
            
            holder.tvStatusCode.setTextColor(Color.parseColor(item.textColor));
            holder.vStatusIndicator.setBackgroundColor(Color.parseColor(item.badgeColor));
            holder.cvStatusBadge.setCardBackgroundColor(Color.parseColor(item.badgeColor));
            holder.cvStatusDot.setCardBackgroundColor(Color.parseColor(item.dotColor));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEndpoint, tvTime, tvStatusCode;
            View vStatusIndicator;
            MaterialCardView cvStatusBadge, cvStatusDot;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEndpoint = itemView.findViewById(R.id.tvEndpoint);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvStatusCode = itemView.findViewById(R.id.tvStatusCode);
                vStatusIndicator = itemView.findViewById(R.id.vStatusIndicator);
                cvStatusBadge = itemView.findViewById(R.id.cvStatusBadge);
                cvStatusDot = itemView.findViewById(R.id.cvStatusDot);
            }
        }
    }
}