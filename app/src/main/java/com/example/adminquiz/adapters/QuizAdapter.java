package com.example.adminquiz.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.databinding.ItemQuizBinding;
import com.example.adminquiz.entities.QuizEntity;

import java.util.Objects;

public class QuizAdapter extends ListAdapter<QuizEntity, QuizAdapter.VH> {

    public interface OnQuizClick {
        void onClick(QuizEntity quiz);
    }

    private static final DiffUtil.ItemCallback<QuizEntity> DIFF = new DiffUtil.ItemCallback<QuizEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull QuizEntity a, @NonNull QuizEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull QuizEntity a, @NonNull QuizEntity b) {
            return a.updatedAt == b.updatedAt && a.published == b.published
                    && a.ratingCount == b.ratingCount && a.ratingAvg == b.ratingAvg
                    && Objects.equals(a.title, b.title);
        }
    };

    private final OnQuizClick listener;

    public QuizAdapter(OnQuizClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemQuizBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        QuizEntity q = getItem(position);
        h.b.tvTitle.setText(q.title);
        String meta = q.category + " \u2022 " + q.difficulty + ("AI".equals(q.source) ? " \u2022 AI" : "");
        h.b.tvMeta.setText(meta);
        String rating = q.ratingCount > 0
                ? String.format(java.util.Locale.US, " \u2022 \u2605 %.1f (%d)", q.ratingAvg, q.ratingCount) : "";
        h.b.tvCreator.setText("by " + q.creatorName + (q.published || "AI".equals(q.source) ? "" : " \u2022 Draft") + rating);
        h.itemView.setOnClickListener(v -> listener.onClick(q));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemQuizBinding b;

        VH(ItemQuizBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
