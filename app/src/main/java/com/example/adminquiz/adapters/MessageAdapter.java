package com.example.adminquiz.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adminquiz.databinding.ItemMessageBinding;
import com.example.adminquiz.entities.MessageEntity;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    public interface OnReply {
        void onReply(MessageEntity message);
    }

    private final List<MessageEntity> items = new ArrayList<>();
    private final OnReply listener;
    private boolean owner;

    public MessageAdapter(OnReply listener) {
        this.listener = listener;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
        notifyDataSetChanged();
    }

    public void submit(List<MessageEntity> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MessageEntity m = items.get(position);
        h.b.tvSender.setText(m.senderName + " asked:");
        h.b.tvText.setText(m.text);
        boolean hasReply = m.reply != null && !m.reply.isEmpty();
        h.b.tvReply.setVisibility(hasReply ? View.VISIBLE : View.GONE);
        h.b.tvReply.setText("Creator: " + m.reply);
        h.b.btnReply.setVisibility(owner && !hasReply ? View.VISIBLE : View.GONE);
        h.b.btnReply.setOnClickListener(v -> listener.onReply(m));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemMessageBinding b;

        VH(ItemMessageBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
