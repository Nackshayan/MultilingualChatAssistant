package com.example.multilingualchatassistant.nlp;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.Locale;

public class LanguageDetector {

    public interface Callback {
        void onResult(@NonNull String languageCode);
        void onError(@NonNull Exception e);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void detectLanguage(@NonNull String text,
                                      @NonNull Callback callback) {
        try {
            String code = simpleGuess(text);
            MAIN.post(() -> callback.onResult(code));
        } catch (Exception e) {
            MAIN.post(() -> callback.onError(e));
        }
    }

    private static String simpleGuess(String text) {
        if (text == null || text.trim().isEmpty()) return "und";

        String t = text.trim();
        String lower = t.toLowerCase(Locale.ROOT);

        // ------------------------------
        // Tamil detection
        // ------------------------------
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c >= 0x0B80 && c <= 0x0BFF) {
                return "ta";
            }
        }

        // ------------------------------
        // Spanish detection
        // ------------------------------

        // Spanish punctuation
        if (t.contains("¿") || t.contains("¡")) return "es";

        // Spanish accents á é í ó ú ñ
        if (lower.matches(".*[áéíóúñ].*")) return "es";

        // Common Spanish keywords
        if (lower.contains("hola") ||
                lower.contains("gracias") ||
                lower.contains("bienvenido")) {
            return "es";
        }

        // ------------------------------
        // French detection
        // ------------------------------

        // French accents à â ä ç é è ê ë î ï ô œ ù û ü ÿ
        if (lower.matches(".*[àâäçéèêëîïôœùûüÿ].*")) return "fr";

        // Common French keywords
        if (lower.contains("bonjour") ||
                lower.contains("merci")) {
            return "fr";
        }

        // ------------------------------
        // If no clear match, return "und"
        // NOT "en"
        // ------------------------------
        return "und";
    }
}
