package com.example.multilingualchatassistant.nlp;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class SlangProcessor {

    private static final Random RANDOM = new Random();

    /**
     * Replace known slang within a text with its more standard meaning.
     * Useful when analysing / translating incoming messages.
     */
    public static String normalizeInput(String langCode, String text) {
        if (text == null || text.isEmpty()) return text;

        String result = text;
        List<SlangDictionary.Entry> entries = SlangDictionary.getEntries(langCode);

        for (SlangDictionary.Entry e : entries) {
            String slang = e.slang;
            if (slang == null || slang.isEmpty()) continue;

            // word-boundary match, case-insensitive
            String regex = "\\b" + Pattern.quote(slang) + "\\b";
            result = result.replaceAll("(?i)" + regex, e.meaning);
        }
        return result;
    }

    /**
     * Add local slang flavour to a reply while keeping it readable.
     * We keep slang light and tone-aware.
     */
    public static String applySlang(String langCode,
                                    String text,
                                    String tone,
                                    String intent) {

        if (text == null || text.isEmpty()) return text;

        String tTone = tone == null ? "" : tone.toLowerCase(Locale.ROOT);
        String tIntent = intent == null ? "" : intent.toLowerCase(Locale.ROOT);

        // For very formal tone we skip slang
        if (tTone.contains("formal")) {
            return text;
        }

        String lang = (langCode == null ? "en" : langCode.toLowerCase(Locale.ROOT));

        switch (lang) {
            case "es":
                return applySpanishSlang(text, tTone, tIntent);
            case "fr":
                return applyFrenchSlang(text, tTone, tIntent);
            case "de":
                return applyGermanSlang(text, tTone, tIntent);
            case "ta":
                return applyTamilSlang(text, tTone, tIntent);
            case "en":
            default:
                return applyEnglishSlang(text, tTone, tIntent);
        }
    }

    // ---------- ENGLISH ----------
    private static String applyEnglishSlang(String text, String tone, String intent) {
        String result = text;
        String lower = text.toLowerCase(Locale.ROOT);

        // 50–70% of the time add a small slang touch for casual/friendly/humorous
        boolean casualTone = tone.contains("casual")
                || tone.contains("friendly")
                || tone.contains("humorous");

        // "bro context" – we want to be extra slangy here
        boolean hasBroContext =
                lower.contains("bro") ||
                        lower.contains("bruh") ||
                        lower.contains("dude") ||
                        lower.contains("fam") ||
                        lower.contains("bestie");

        if (intent.contains("love")) {
            if (hasBroContext) {
                // ALWAYS add one of these when it's love + bro-type message
                result = maybeAppend(result,
                        randomOf("fr", "for real", "no cap", "fr fr"));
            } else if (casualTone && chance(0.6f)) {
                // normal love case without "bro"
                result = maybeAppend(result, "fr");
            }
        } else if (intent.contains("thanks")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "ngl you’re a real one 🙌");
            }
        } else if (intent.contains("congrats")) {
            if (casualTone && chance(0.7f)) {
                result = maybeAppend(result,
                        randomOf("you’re killing it 🔥", "big glow up fr ✨"));
            }
        } else if (intent.contains("apology")) {
            if (tone.contains("empathetic") && chance(0.5f)) {
                result = maybeAppend(result, "for real, my bad 🙏");
            }
        } else {
            if (casualTone && chance(0.4f)) {
                result = maybeAppend(result,
                        randomOf("ngl", "no cap", "lowkey", "fr"));
            }
        }

        return result;
    }

    // ---------- SPANISH ----------
    private static String applySpanishSlang(String text, String tone, String intent) {
        String result = text;
        boolean casualTone = tone.contains("casual") || tone.contains("friendly") || tone.contains("humorous");

        if (intent.contains("love")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "de verdad 💕");
            }
        } else if (intent.contains("congrats")) {
            if (casualTone && chance(0.7f)) {
                result = maybeAppend(result,
                        randomOf("eres un crack 🔥", "full orgullo por ti 💪"));
            }
        } else if (intent.contains("thanks")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "de pana 🙏");
            }
        } else {
            if (casualTone && chance(0.5f)) {
                result = maybeAppend(result,
                        randomOf("qué buena onda 😄", "está full bien 😌", "todo chill 😎"));
            }
        }
        return result;
    }

    // ---------- FRENCH ----------
    private static String applyFrenchSlang(String text, String tone, String intent) {
        String result = text;
        boolean casualTone = tone.contains("casual") || tone.contains("friendly") || tone.contains("humorous");

        if (intent.contains("love")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "grave 💕");
            }
        } else if (intent.contains("congrats")) {
            if (casualTone && chance(0.7f)) {
                result = maybeAppend(result,
                        randomOf("ça déchire 🔥", "t’es trop chaud(e) 😎"));
            }
        } else if (intent.contains("thanks")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "cimer poto 🙏");
            }
        } else if (intent.contains("apology")) {
            if (tone.contains("empathetic") && chance(0.5f)) {
                result = maybeAppend(result, "j’avoue c’était pas ouf 😅");
            }
        } else {
            if (casualTone && chance(0.5f)) {
                result = maybeAppend(result,
                        randomOf("tkt c’est chill 😌", "grave stylé 👌"));
            }
        }
        return result;
    }

    // ---------- GERMAN ----------
    private static String applyGermanSlang(String text, String tone, String intent) {
        String result = text;
        boolean casualTone = tone.contains("casual") || tone.contains("friendly") || tone.contains("humorous");

        if (intent.contains("congrats")) {
            if (casualTone && chance(0.7f)) {
                result = maybeAppend(result,
                        randomOf("richtig stabil 🔥", "läuft bei dir 😎"));
            }
        } else if (intent.contains("love")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "so krass 💕");
            }
        } else if (intent.contains("thanks")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "ehrenmann 🙏");
            }
        } else {
            if (casualTone && chance(0.5f)) {
                result = maybeAppend(result,
                        randomOf("alles easy 😌", "läuft schon 😄"));
            }
        }
        return result;
    }

    // ---------- TAMIL ----------
    private static String applyTamilSlang(String text, String tone, String intent) {
        String result = text;
        boolean casualTone = tone.contains("casual") || tone.contains("friendly") || tone.contains("humorous");

        if (intent.contains("love")) {
            if (casualTone && chance(0.7f)) {
                result = maybeAppend(result, "machan level 💕");
            }
        } else if (intent.contains("congrats")) {
            if (casualTone && chance(0.7f)) {
                result = maybeAppend(result,
                        randomOf("vera level da 🔥", "semma massu 😎"));
            }
        } else if (intent.contains("thanks")) {
            if (casualTone && chance(0.6f)) {
                result = maybeAppend(result, "romba thanks da 🙏");
            }
        } else if (intent.contains("apology")) {
            if (tone.contains("empathetic") && chance(0.5f)) {
                result = maybeAppend(result, "light ah eduthuko da 😔");
            }
        } else {
            if (casualTone && chance(0.5f)) {
                result = maybeAppend(result,
                        randomOf("jolly ah irukku 😄", "scene illa da 😌"));
            }
        }
        return result;
    }

    // ---------- Helpers ----------
    private static boolean chance(float p) {
        return RANDOM.nextFloat() < p;
    }

    private static String randomOf(String... options) {
        if (options == null || options.length == 0) return "";
        int idx = RANDOM.nextInt(options.length);
        return options[idx];
    }

    private static String maybeAppend(String base, String addition) {
        if (addition == null || addition.isEmpty()) return base;
        if (base.endsWith("!") || base.endsWith("?") || base.endsWith(".")) {
            return base + " " + addition;
        } else {
            return base + " " + addition;
        }
    }
}
