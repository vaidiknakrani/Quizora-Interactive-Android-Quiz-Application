package com.example.adminquiz.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.databinding.ItemAnswerBinding;
import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.utils.Fmt;

import java.util.ArrayList;
import java.util.List;

public class AnswerReviewAdapter extends RecyclerView.Adapter<AnswerReviewAdapter.VH> {
    private final List<AnswerEntity> items = new ArrayList<>();

    public void submit(List<AnswerEntity> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAnswerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AnswerEntity a = items.get(position);
        boolean ok = a.selectedIndex == a.correctIndex;
        h.b.tvQuestion.setText("Q" + (position + 1) + ". " + a.question);
        String yours = a.selectedIndex < 0 ? "Not answered" : a.option(a.selectedIndex);
        h.b.tvYourAnswer.setText("Your Answer:  " + yours + (ok ? "  \u2713" : "  \u274C"));
        h.b.tvCorrectAnswer.setText("Correct Answer:  " + a.option(a.correctIndex) + "  \u2713");
        h.b.tvExplanation.setText("Explanation:  " + (a.explanation == null ? "-" : a.explanation));
        h.b.tvTime.setText("Time: " + Fmt.seconds(a.timeMs));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemAnswerBinding b;

        VH(ItemAnswerBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
