package com.example.adminquiz.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.databinding.ItemGhostBinding;
import com.example.adminquiz.models.GhostItem;
import com.example.adminquiz.utils.Fmt;

import java.util.ArrayList;
import java.util.List;

public class GhostAdapter extends RecyclerView.Adapter<GhostAdapter.VH> {

    public interface Actions {
        void onShare(GhostItem item);

        void onPlay(GhostItem item);

        void onReject(GhostItem item);
    }

    private final List<GhostItem> items = new ArrayList<>();
    private final Actions actions;

    public GhostAdapter(Actions actions) {
        this.actions = actions;
    }

    public void submit(List<GhostItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemGhostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GhostItem g = items.get(position);
        h.b.tvTitle.setText(g.quizTitle);
        h.b.tvCode.setText(g.code + "  \u2022  Ghost by " + g.ownerName);
        String ghostLine = "Ghost: " + g.ghostScore + "/" + g.total + " in " + Fmt.seconds(g.ghostTimeMs);
        h.b.btnSecondary.setVisibility(View.GONE);

        if ("OWNED".equals(g.status)) {
            h.b.tvScores.setText(ghostLine);
            h.b.btnPrimary.setText("Share");
            h.b.btnPrimary.setOnClickListener(v -> actions.onShare(g));
        } else if ("PENDING".equals(g.status)) {
            h.b.tvScores.setText(ghostLine);
            h.b.btnPrimary.setText("Accept");
            h.b.btnPrimary.setOnClickListener(v -> actions.onPlay(g));
            h.b.btnSecondary.setVisibility(View.VISIBLE);
            h.b.btnSecondary.setText("Reject");
            h.b.btnSecondary.setOnClickListener(v -> actions.onReject(g));
        } else {
            String result = g.won > 0 ? "YOU WON" : (g.won == 0 ? "DRAW" : "YOU LOST");
            h.b.tvScores.setText(result + "\nYou: " + g.playerScore + "/" + g.total + " in "
                    + Fmt.seconds(g.playerTimeMs) + "\n" + ghostLine);
            h.b.btnPrimary.setText("Replay");
            h.b.btnPrimary.setOnClickListener(v -> actions.onPlay(g));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemGhostBinding b;

        VH(ItemGhostBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
