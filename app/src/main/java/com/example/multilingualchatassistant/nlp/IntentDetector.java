package com.example.multilingualchatassistant.nlp;

import java.util.Locale;

/**
 * Simple intent detector facade.
 *
 * Public API used by:
 *  - GIF button
 *  - ReplyEngine (incoming + reply combined)
 *
 * Internally delegates to LocalNlpModel.predictIntent(...)
 * so we have a single source of truth for intent logic.
 *
 * Returns one of:
 *  - greeting
 *  - farewell
 *  - thanks
 *  - apology
 *  - love
 *  - congrats
 *  - hate
 *  - smalltalk
 *  - question
 *  - unknown
 */
public class IntentDetector {

    // ------------- Public APIs -------------

    // Used from GIF button (single message)
    public static String detectIntent(String text) {
        if (text == null) {
            return "unknown";
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "unknown";
        }

        String intent = LocalNlpModel.predictIntent(trimmed);
        return intent != null ? intent : "unknown";
    }

    // Used from ReplyEngine (incoming + reply combined)
    public static String detectIntent(String incoming, String reply) {
        StringBuilder sb = new StringBuilder();

        if (incoming != null && !incoming.trim().isEmpty()) {
            sb.append(incoming.trim()).append(" ");
        }
        if (reply != null && !reply.trim().isEmpty()) {
            sb.append(reply.trim());
        }

        String combined = sb.toString().trim();
        if (combined.isEmpty()) {
            return "unknown";
        }

        // Lowercasing here is optional because LocalNlpModel also lowers,
        // but it doesn't hurt and keeps things consistent.
        String intent = LocalNlpModel.predictIntent(combined.toLowerCase(Locale.ROOT));
        return intent != null ? intent : "unknown";
    }
}
