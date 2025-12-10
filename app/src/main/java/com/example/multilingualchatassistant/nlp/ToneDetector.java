package com.example.multilingualchatassistant.nlp;

import java.util.Locale;

public class ToneDetector {

    /**
     * Returns one of:
     *  "formal", "friendly", "humorous", "empathetic",
     *  "casual", "neutral", "angry", "sad".
     */
    public static String detectTone(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "neutral";
        }

        String t = text.trim();
        String lower = t.toLowerCase(Locale.ROOT);

        boolean hasEmoji = containsEmoji(t);
        boolean hasExclaim = lower.contains("!");
        boolean hasQuestion = lower.contains("?");
        boolean hasAllCapsWord = hasAllCapsWord(t);
        boolean hasSlang = containsSlang(lower);

        int formalityScore = computeFormalityScore(lower, hasEmoji, hasSlang);

        boolean hasPositive = containsAny(lower, POSITIVE_WORDS);
        boolean hasNegative = containsAny(lower, NEGATIVE_WORDS);
        boolean hasApology = containsAny(lower, APOLOGY_WORDS);
        boolean hasSupport = containsAny(lower, SUPPORT_WORDS);

        // 1) Very formal messages
        if (formalityScore >= 6) {
            return "formal";
        }

        // 2) Strong emotions
        if (hasNegative && (hasExclaim || hasAllCapsWord)) {
            return "angry";
        }
        if (hasNegative && !hasPositive) {
            return "sad";
        }

        // 3) Empathetic / supportive
        if (hasApology || hasSupport) {
            return "empathetic";
        }

        // 4) Humorous / casual
        if (hasSlang || hasEmoji || hasExclaim) {
            if (hasEmoji && containsAny(lower, LAUGH_WORDS)) {
                return "humorous";
            }
            return "casual";
        }

        // 5) Friendly neutral
        if (hasPositive) {
            return "friendly";
        }

        if (hasQuestion && !hasNegative) {
            return "neutral";
        }

        return "neutral";
    }

    /**
     * Helper used by StyleEngine to decide if we should force "formal" styling.
     */
    public static boolean isLikelyFormal(String text) {
        if (text == null) return false;
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return false;

        boolean hasEmoji = containsEmoji(text);
        boolean hasSlang = containsSlang(t);
        int score = computeFormalityScore(t, hasEmoji, hasSlang);
        return score >= 6;
    }

    // ---------------------------------------------------------------------
    // Keywords (EN, ES, FR, TA)
    // ---------------------------------------------------------------------

    private static final String[] POSITIVE_WORDS = {
            // EN
            "great", "awesome", "amazing", "nice", "love", "lovely",
            "happy", "glad", "good job", "well done", "thank you",
            "thanks", "appreciate", "grateful", "fantastic",
            // ES
            "genial", "increíble", "me alegra", "muy bien", "muchas gracias",
            // FR
            "super", "génial", "content", "heureux", "merci beaucoup",
            // TA (rough)
            "சந்தோஷம்", "ரொம்ப நன்றி", "நன்றி"
    };

    private static final String[] NEGATIVE_WORDS = {
            // EN
            "hate", "annoying", "angry", "upset", "tired", "exhausted",
            "sucks", "worst", "useless", "can't deal", "cant deal",
            "fed up", "disappointed", "sad", "miserable", "depressed",
            // ES
            "odio", "molesto", "cansado", "harto", "decepcionado",
            // FR
            "je déteste", "énervé", "fatigué", "marre", "déçu",
            // TA
            "கோபம்", "சலிப்பு", "சோகமாக", "வேதனை"
    };

    private static final String[] APOLOGY_WORDS = {
            "sorry", "my bad", "i apologise", "i apologize",
            "lo siento", "perdón", "disculpa", "disculpe",
            "désolé", "je suis désolé", "pardon",
            "மன்னிச்சு", "மன்னிக்கவும்", "மன்னி"
    };

    private static final String[] SUPPORT_WORDS = {
            "i'm here for you", "im here for you",
            "here for you", "you can talk to me",
            "let me know if you need", "i understand",
            "i get how you feel", "take your time",
            "estoy aquí para ti", "je suis là pour toi",
            "உனக்காக இருக்கேன்", "நான் உன்னுடன் இருக்கேன்"
    };

    private static final String[] LAUGH_WORDS = {
            "lol", "lmao", "rofl", "haha", "hahaha", "funny",
            "jaja", "mdr"
    };

    private static final String[] SLANG_MARKERS = {
            "lol", "lmao", "rofl", "brb", "bro", "dude", "fam", "ngl",
            "no cap", " fr", "fr ", "lit", "vibes", "wassup", "sup",
            "gonna", "wanna", "gotta",
            "tío", "pana", "chévere",
            "wesh", "ouf", "relou",
            "machan", "da ", "dei "
    };

    private static final String[] FORMAL_MARKERS = {
            "please", "kindly", "could you", "would you",
            "i would like", "i would appreciate", "i appreciate",
            "thank you for", "regarding", "with reference to",
            "attached", "sincerely", "yours faithfully", "yours sincerely",
            "best regards", "dear sir", "dear madam",
            // ES
            "por favor", "le agradecería", "estimado señor", "estimada señora",
            // FR
            "s'il vous plaît", "je vous prie", "cordialement",
            "veuillez trouver ci-joint"
    };


    // Scoring helpers

    private static int computeFormalityScore(String lower,
                                             boolean hasEmoji,
                                             boolean hasSlang) {
        int score = 0;

        if (containsAny(lower, FORMAL_MARKERS)) {
            score += 4;
        }
        if (lower.length() > 40 && !hasSlang) {
            score += 2;
        }
        if (!hasEmoji) {
            score += 1;
        } else {
            score -= 2;
        }
        if (hasSlang) {
            score -= 3;
        }

        if (endsWithPunctuation(lower)) {
            score += 1;
        }

        if (score < 0) score = 0;
        if (score > 10) score = 10;
        return score;
    }

    private static boolean containsAny(String text, String[] words) {
        if (text == null || text.isEmpty()) return false;
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSlang(String lower) {
        return containsAny(lower, SLANG_MARKERS);
    }

    private static boolean containsEmoji(String s) {
        if (s == null) return false;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int cp = s.codePointAt(i);
            if (cp >= 0x1F300 && cp <= 0x1FAFF) { // emoji range
                return true;
            }
        }
        return false;
    }

    private static boolean hasAllCapsWord(String text) {
        if (text == null) return false;
        String[] parts = text.split("\\s+");
        for (String p : parts) {
            if (p.length() > 2 && p.equals(p.toUpperCase(Locale.ROOT))
                    && p.matches(".*[A-Z].*")) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWithPunctuation(String lower) {
        if (lower == null || lower.isEmpty()) return false;
        char c = lower.charAt(lower.length() - 1);
        return c == '.' || c == '?' || c == '!';
    }
}