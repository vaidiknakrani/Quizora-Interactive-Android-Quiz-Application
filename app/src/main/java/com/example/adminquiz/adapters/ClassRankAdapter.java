package com.example.adminquiz.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.R;
import com.example.adminquiz.analytics.LeaderboardRanker.Mode;
import com.example.adminquiz.analytics.LeaderboardRanker.RankedEntry;
import com.example.adminquiz.databinding.ItemClassRankBinding;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/** Rows of the class leaderboard. The signed-in student's own row is highlighted. */
public class ClassRankAdapter extends RecyclerView.Adapter<ClassRankAdapter.VH> {
    private final List<RankedEntry> rows = new ArrayList<>();
    private Mode mode = Mode.POINTS;

    public void submit(List<RankedEntry> list, Mode mode) {
        rows.clear();
        if (list != null) {
            rows.addAll(list);
        }
        this.mode = mode == null ? Mode.POINTS : mode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemClassRankBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Context c = h.b.getRoot().getContext();
        RankedEntry e = rows.get(position);
        String name = e.username == null ? "" : e.username;
        h.b.tvRank.setText(e.ranked ? "#" + e.rank : "-");
        h.b.tvAvatar.setText(initial(name));
        h.b.tvName.setText(e.me ? name + " (" + c.getString(R.string.lb_you) + ")" : name);
        h.b.tvMeta.setText(mode == Mode.POINTS
                ? c.getString(R.string.lb_row_meta, e.quizzes, e.accuracyPct())
                : c.getString(R.string.lb_points_value, e.points) + " · " + e.quizzes);
        h.b.tvValue.setText(mode == Mode.POINTS
                ? c.getString(R.string.lb_points_value, e.points)
                : c.getString(R.string.lb_accuracy_value, e.accuracyPct()));
        int stroke = e.me
                ? MaterialColors.getColor(h.b.card, com.google.android.material.R.attr.colorPrimary)
                : MaterialColors.getColor(h.b.card, com.google.android.material.R.attr.colorOutlineVariant);
        int fill = e.me
                ? MaterialColors.getColor(h.b.card, com.google.android.material.R.attr.colorPrimaryContainer)
                : MaterialColors.getColor(h.b.card, com.google.android.material.R.attr.colorSurface);
        h.b.card.setStrokeColor(stroke);
        h.b.card.setStrokeWidth(e.me ? 3 : 1);
        h.b.card.setCardBackgroundColor(fill);
    }

    static String initial(String name) {
        for (int i = 0; i < name.length(); i++) {
            int cp = name.codePointAt(i);
            if (Character.isLetterOrDigit(cp)) {
                return new String(Character.toChars(Character.toUpperCase(cp)));
            }
        }
        return "?";
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemClassRankBinding b;

        VH(ItemClassRankBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
