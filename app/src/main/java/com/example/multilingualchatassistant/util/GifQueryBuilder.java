package com.example.multilingualchatassistant.util;

import java.util.Locale;

/**
 * Builds short, privacy-friendly GIF search queries.
 *
 * Rules:
 *  - Never send the full user message.
 *  - Only send 1–3 sanitized keyword words (no emojis, no punctuation).
 *  - If we have no keywords, fall back to tiny phrases based on intent/tone
 *    like "hello wave", "thank you", "sad reaction", etc.
 */
public class GifQueryBuilder {

    /**
     * Main query builder used by MainActivity.
     *
     * @param intent   detected intent (e.g. greeting, thanks, love, apology, congrats, unknown)
     * @param tone     detected tone (friendly, sad, angry, humorous, etc.)
     * @param keywords short keyword string from KeywordExtractor (may be empty)
     */
    public static String buildQuery(String intent, String tone, String keywords) {
        // 1) Use sanitized keywords if we have them
        String clean = sanitizeKeywords(keywords);
        if (!clean.isEmpty()) {
            // already only 1–3 words, safe to send
            return clean;
        }

        // 2) Otherwise fall back to something tiny from intent/tone
        return buildFromIntentTone(intent, tone);
    }

    /**
     * Fallback used by MainActivity when the first query returns no GIFs.
     * Signature matches the call: buildFallbackQuery(String, String)
     */
    public static String buildFallbackQuery(String intent, String tone) {
        return buildFromIntentTone(intent, tone);
    }

    // -------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------

    private static String buildFromIntentTone(String intent, String tone) {
        String i = intent == null ? "" : intent.toLowerCase(Locale.ROOT);
        String t = tone == null ? "" : tone.toLowerCase(Locale.ROOT);

        // Intent-based phrases
        if (i.contains("greeting"))  return "hello wave";
        if (i.contains("thanks"))    return "thank you";
        if (i.contains("love"))      return "love heart";
        if (i.contains("apology") || i.contains("sorry")) return "sorry";
        if (i.contains("congrats"))  return "congratulations";

        // Tone-based fallbacks
        if (t.contains("humor"))     return "funny reaction";
        if (t.contains("sad"))       return "sad reaction";
        if (t.contains("angry"))     return "angry reaction";
        if (t.contains("friendly") || t.contains("happy"))
            return "happy reaction";

        // Final generic fallback
        return "reaction";
    }

    /**
     * Keep only letters + spaces, trim, and limit to 3 words max.
     * This guarantees we never leak the full message text.
     */
    private static String sanitizeKeywords(String raw) {
        if (raw == null) return "";

        String t = raw.toLowerCase(Locale.ROOT).trim();

        // Letters (including basic accents) + spaces only
        t = t.replaceAll("[^a-zñáéíóúüçàèìòùâêîôûäëïöü ]", " ");
        t = t.replaceAll("\\s+", " ").trim();

        if (t.isEmpty()) return "";

        String[] parts = t.split(" ");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (count > 0) sb.append(' ');
            sb.append(p);
            count++;
            if (count >= 3) break; // max 3 words
        }
        return sb.toString().trim();
    }
}
