package com.example.adminquiz.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.databinding.ItemLeaderboardBinding;
import com.example.adminquiz.models.LeaderboardRow;
import com.example.adminquiz.utils.Fmt;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {
    private final List<LeaderboardRow> rows = new ArrayList<>();

    public void submit(List<LeaderboardRow> list) {
        rows.clear();
        if (list != null) {
            rows.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LeaderboardRow r = rows.get(position);
        h.b.tvRank.setText("#" + (position + 1));
        h.b.tvName.setText(r.username);
        h.b.tvScore.setText(r.totalScore + " pts");
        h.b.tvMeta.setText("Accuracy " + Fmt.pct(r.accuracy) + "%  \u2022  Ghost wins " + r.ghostWins);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemLeaderboardBinding b;

        VH(ItemLeaderboardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
