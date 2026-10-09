package com.example.adminquiz.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.databinding.ItemAttemptBinding;
import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.models.Persona;
import com.example.adminquiz.utils.Fmt;

public class AttemptAdapter extends ListAdapter<AttemptEntity, AttemptAdapter.VH> {

    public interface OnAttemptClick {
        void onClick(AttemptEntity attempt);
    }

    private static final DiffUtil.ItemCallback<AttemptEntity> DIFF = new DiffUtil.ItemCallback<AttemptEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull AttemptEntity a, @NonNull AttemptEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull AttemptEntity a, @NonNull AttemptEntity b) {
            return a.id == b.id && a.score == b.score && a.finishedAt == b.finishedAt;
        }
    };

    private final OnAttemptClick listener;

    public AttemptAdapter(OnAttemptClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemAttemptBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AttemptEntity a = getItem(position);
        Persona p = Persona.fromName(a.persona);
        h.b.tvEmoji.setText(p.emoji);
        h.b.tvPersona.setText(p.title);
        h.b.tvQuiz.setText(a.quizTitle + " \u2022 " + a.mode);
        h.b.tvScore.setText(a.score + "/" + a.total + "  (" + a.overallPct + "%)");
        h.b.tvDate.setText(Fmt.date(a.finishedAt) + " \u2022 " + Fmt.time(a.totalTimeMs));
        h.itemView.setOnClickListener(v -> listener.onClick(a));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemAttemptBinding b;

        VH(ItemAttemptBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
