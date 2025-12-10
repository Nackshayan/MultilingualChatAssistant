package com.example.multilingualchatassistant.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Incoming message
    public String originalText;
    public String originalLang;
    public String userDisplayLang;          // language user understands
    public String translatedForUserText;    // original → user language

    // Reply
    public String replyUserInput;           // what user typed/spoke
    public String replyStyledUserLang;      // with idioms/emojis in user language
    public String replySendLang;            // language to send in
    public String replySendText;            // final text to paste in chat

    // Extras
    public String detectedTone;
    public String detectedIntent;
    public String gifUrl;

    public long timestamp;                  // System.currentTimeMillis()
}
