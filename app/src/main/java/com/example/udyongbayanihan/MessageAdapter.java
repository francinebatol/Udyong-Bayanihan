package com.example.udyongbayanihan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<MessageModel> messages;
    private String currentUserId;
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public MessageAdapter(List<MessageModel> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        sortMessages(); // Sort messages when adapter is created
    }

    // Add new method to update messages
    public void updateMessages(List<MessageModel> newMessages) {
        this.messages = newMessages;
        sortMessages();
        notifyDataSetChanged();
    }

    // Add sorting method
    private void sortMessages() {
        Collections.sort(messages, new Comparator<MessageModel>() {
            @Override
            public int compare(MessageModel m1, MessageModel m2) {
                if (m1.getTimestamp() == null || m2.getTimestamp() == null) {
                    return 0;
                }
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            }
        });
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_right, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_left, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = messages.get(position);
        holder.bindMessage(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        int viewType;

        MessageViewHolder(View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;

            if (viewType == VIEW_TYPE_SENT) {
                messageText = itemView.findViewById(R.id.textRightChat);
            } else {
                messageText = itemView.findViewById(R.id.textLeftChat);
            }
        }

        void bindMessage(MessageModel message) {
            if (messageText != null) {
                // Apply profanity filter when displaying messages
                // This ensures messages sent before the filter was implemented are also filtered
                String filteredMessage = ProfanityFilter.filterProfanity(message.getMessage());
                messageText.setText(filteredMessage);
            }
        }
    }
}