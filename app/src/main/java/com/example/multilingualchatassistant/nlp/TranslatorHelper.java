package com.example.multilingualchatassistant.nlp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.common.model.DownloadConditions;

/**
 * Wrapper around ML Kit on-device translation.
 * Supports only: en, es, fr, ta.
 */
public class TranslatorHelper {

    public interface TranslateCallback {
        void onTranslated(@NonNull String translatedText);
        void onError(@NonNull Exception e);
    }

    public static void translate(
            @NonNull String sourceLang,
            @NonNull String targetLang,
            @NonNull String text,
            @NonNull TranslateCallback callback
    ) {
        if (sourceLang.equalsIgnoreCase(targetLang)) {
            callback.onTranslated(text);
            return;
        }

        String src = toMlKitCode(sourceLang);
        String tgt = toMlKitCode(targetLang);

        if (src == null || tgt == null) {
            // Unsupported -> just return original
            callback.onTranslated(text);
            return;
        }

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(src)
                        .setTargetLanguage(tgt)
                        .build();
        final Translator translator = Translation.getClient(options);

        DownloadConditions conditions =
                new DownloadConditions.Builder()
                        .requireWifi()
                        .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused ->
                        translator.translate(text)
                                .addOnSuccessListener(translatedText -> {
                                    callback.onTranslated(translatedText);
                                    translator.close();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TranslatorHelper", "translate error", e);
                                    callback.onError(e);
                                    translator.close();
                                })
                )
                .addOnFailureListener(e -> {
                    Log.e("TranslatorHelper", "model download error", e);
                    callback.onError(e);
                    translator.close();
                });
    }

    private static String toMlKitCode(String code) {
        if (code == null) return null;
        String c = code.toLowerCase();

        switch (c) {
            case "en":
            case "en-us":
            case "en-gb":
                return TranslateLanguage.ENGLISH;
            case "es":
            case "es-es":
            case "es-419":
                return TranslateLanguage.SPANISH;
            case "fr":
            case "fr-fr":
                return TranslateLanguage.FRENCH;
            case "ta":
            case "ta-in":
                return TranslateLanguage.TAMIL;
            default:
                return null; // unsupported
        }
    }
}
