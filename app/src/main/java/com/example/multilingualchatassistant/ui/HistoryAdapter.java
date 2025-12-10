package com.example.multilingualchatassistant.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.multilingualchatassistant.R;
import com.example.multilingualchatassistant.data.MessageEntity;
import com.example.multilingualchatassistant.util.LanguageUtils;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<MessageEntity> items = new ArrayList<>();
    private boolean gifOnlyMode = false;

    public void setItems(List<MessageEntity> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void setGifOnlyMode(boolean enabled) {
        this.gifOnlyMode = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {
        MessageEntity msg = items.get(position);

        // Common stuff
        String original = msg.originalText != null ? msg.originalText : "";
        String translated = msg.translatedForUserText != null ? msg.translatedForUserText : "";
        String replyStyled = msg.replyStyledUserLang != null ? msg.replyStyledUserLang : "";
        String replySend = msg.replySendText != null ? msg.replySendText : "";
        String gifUrl = msg.gifUrl;

        String origLangName = LanguageUtils.codeToName(msg.originalLang);
        String userLangName = LanguageUtils.codeToName(msg.userDisplayLang);
        String sendLangName = LanguageUtils.codeToName(msg.replySendLang);

        String tone = msg.detectedTone != null ? msg.detectedTone : "unknown";
        String intent = msg.detectedIntent != null ? msg.detectedIntent : "unknown";

        String timeStr = "";
        if (msg.timestamp > 0) {
            DateFormat df = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT);
            timeStr = df.format(new Date(msg.timestamp));
        }

        String meta = origLangName + " → " + userLangName;
        if (sendLangName != null && !sendLangName.isEmpty()) {
            meta += " → " + sendLangName;
        }
        meta += " • tone=" + tone + " • intent=" + intent;
        if (!timeStr.isEmpty()) {
            meta += " • " + timeStr;
        }

        if (gifOnlyMode) {
            // Show only GIF + minimal meta
            holder.tvOriginal.setVisibility(View.GONE);
            holder.tvTranslated.setVisibility(View.GONE);
            holder.tvReplyUserStyled.setVisibility(View.GONE);
            holder.tvReplySend.setVisibility(View.GONE);

            holder.tvMeta.setVisibility(View.VISIBLE);
            holder.tvMeta.setText(meta);

        } else {
            // Show full text info
            holder.tvOriginal.setVisibility(View.VISIBLE);
            holder.tvTranslated.setVisibility(View.VISIBLE);
            holder.tvReplyUserStyled.setVisibility(View.VISIBLE);
            holder.tvReplySend.setVisibility(View.VISIBLE);
            holder.tvMeta.setVisibility(View.VISIBLE);

            holder.tvOriginal.setText("Original (" + origLangName + "): " + original);
            holder.tvTranslated.setText("For you (" + userLangName + "): " + translated);
            holder.tvReplyUserStyled.setText("Your reply meaning (" + userLangName + "): " + replyStyled);
            holder.tvReplySend.setText("Reply to send (" + sendLangName + "): " + replySend);
            holder.tvMeta.setText(meta);
        }

        if (gifUrl != null && !gifUrl.isEmpty()) {
            holder.ivGif.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(gifUrl)
                    .into(holder.ivGif);
        } else {
            holder.ivGif.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOriginal;
        TextView tvTranslated;
        TextView tvReplyUserStyled;
        TextView tvReplySend;
        TextView tvMeta;
        ImageView ivGif;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginal = itemView.findViewById(R.id.tvOriginal);
            tvTranslated = itemView.findViewById(R.id.tvTranslated);
            tvReplyUserStyled = itemView.findViewById(R.id.tvReplyUserStyled);
            tvReplySend = itemView.findViewById(R.id.tvReplySend);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            ivGif = itemView.findViewById(R.id.ivGif);
        }
    }
}
