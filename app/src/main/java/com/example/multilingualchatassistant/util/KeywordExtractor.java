package com.example.multilingualchatassistant.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Very small, on-device keyword extractor.
 * - Removes emojis & punctuation
 * - Drops stopwords
 * - Keeps 1–2 content words only
 */
public class KeywordExtractor {

    // Basic stopwords for EN / ES / FR / TA mixed
    private static final String[] STOPWORDS = {
            "i", "you", "the", "a", "an", "and", "or", "to", "for", "is", "are", "am",
            "in", "of", "on", "at", "that", "this", "it", "just", "now", "so", "very",
            "me", "my", "your", "our", "we", "they", "he", "she",
            // Spanish / French / random bits
            "yo", "tu", "el", "ella", "nous", "vous", "ellos", "elles", "le", "la", "les",
            "de", "des", "du", "un", "una", "uno", "y", "o", "et",
            // Tamil latin-ish filler words
            "da", "di", "la", "pa", "machan", "machi"
    };

    private static final Set<String> STOP_SET = new HashSet<>();

    static {
        for (String s : STOPWORDS) {
            STOP_SET.add(s);
        }
    }

    /**
     * Extract up to maxKeywords short keywords.
     */
    public static String extractKeywords(String text, int maxKeywords) {
        if (text == null || text.trim().isEmpty()) return "";

        String normalized = stripEmojisAndPunctuation(text).toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        List<String> keep = new ArrayList<>();

        for (String p : parts) {
            if (p.length() < 2) continue;
            if (STOP_SET.contains(p)) continue;
            keep.add(p);
            if (keep.size() >= maxKeywords) break;
        }

        if (keep.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keep.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(keep.get(i));
        }
        return sb.toString().trim();
    }

    private static String stripEmojisAndPunctuation(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder();
        int len = input.length();
        for (int i = 0; i < len; ) {
            int cp = input.codePointAt(i);
            if (isEmoji(cp)) {
                // skip
            } else if (Character.isLetterOrDigit(cp) || Character.isSpaceChar(cp)) {
                out.appendCodePoint(cp);
            } // else skip punctuation
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    private static boolean isEmoji(int codePoint) {
        // very rough: anything in emoji blocks
        return (codePoint >= 0x1F000 && codePoint <= 0x1FAFF);
    }
}
