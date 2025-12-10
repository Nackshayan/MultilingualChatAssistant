package com.example.multilingualchatassistant.nlp;

import java.util.Locale;

/**
 * LocalNlpModel
 *
 * Single integration point for local NLP (intent + tone).
 *
 * - Today: uses strong heuristics so the app works fully offline.
 * - Future: can be wired to a tiny ONNX / Transformer model.
 *
 * In your FYP report you can describe:
 *  - this class as the ML integration point
 *  - how an ONNX model would be loaded from assets
 *  - how predictIntent / predictTone would call the model
 *    and only fall back to heuristics if the model is not confident.
 */
public class LocalNlpModel {

    // Toggle: set to true when you actually integrate ONNX runtime
    private static final boolean USE_ML_MODEL = false;

    // ------------------------------------------------------------------------
    // Public APIs
    // ------------------------------------------------------------------------

    /**
     * Predict intent from text using local ML model or heuristics.
     *
     * Possible return values (current heuristic version):
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
     *
     * Return null only if the ML model is enabled but not confident.
     * With heuristics only, this never returns null.
     */
    public static String predictIntent(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "unknown";
        }

        if (USE_ML_MODEL) {
            // TODO: ONNX runtime logic (in real ML version)
            // 1) Convert text to model input tensor
            // 2) Run inference
            // 3) Map logits/probabilities to one of the intent labels
            // 4) If confidence is too low, return null to allow fallback
        }

        // Heuristic "mini model" so everything works without ONNX
        return heuristicIntent(text);
    }

    /**
     * Predict tone from text using local ML model or heuristics.
     *
     * Tones: friendly, formal, casual, humorous, empathetic, neutral
     *
     * Return null only if the ML model is enabled but not confident.
     * With heuristics only, this never returns null.
     */
    public static String predictTone(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "neutral";
        }

        if (USE_ML_MODEL) {
            // TODO: ONNX runtime logic for tone classification
        }

        return heuristicTone(text);
    }

    // ------------------------------------------------------------------------
    // Heuristic "mini model" for INTENT (merged logic)
    // ------------------------------------------------------------------------

    private static String heuristicIntent(String text) {
        String t = text.toLowerCase(Locale.ROOT).trim();

        // ---- Greeting (EN, chat-style) ----
        if (t.matches("^(hi|hey|hello|yo|sup)[!. ]?.*")
                || t.contains("good morning")
                || t.contains("good afternoon")
                || t.contains("good evening")
                || t.contains("how are you")
                || t.contains("what's up")
                || t.contains("whats up")
                || t.contains("wyd")
                || t.contains("wya")) {
            return "greeting";
        }

        // ---- Greeting (ES / FR / TA) ----
        if (containsAny(t, new String[]{
                // Spanish
                "hola", "buenos dias", "buenas noches",
                // French
                "salut", "bonsoir", "bonjour",
                // Tamil
                "வணக்கம்", "ஹலோ", "vanakkam"
        })) {
            return "greeting";
        }

        // ---- Farewell / bye (EN chat + ES / FR / TA) ----
        if (containsAny(t, new String[]{
                // English
                "bye", "goodbye", "good night", "see you",
                "cya", "ttyl", "gtg", "g2g", "talk to you later",
                // Spanish
                "adios", "hasta luego", "hasta pronto",
                // French
                "à plus", "a plus", "au revoir",
                // Tamil
                "போறேன்", "போயிட்டேன்",
                // Variants
                "see u"
        })) {
            return "farewell";
        }

        // ---- Thanks (EN / ES / FR / TA) ----
        if (containsAny(t, new String[]{
                // English
                "thank you", "thanks", "thx", "tysm",
                "thanks a lot", "appreciate it", "big thanks",
                "much appreciated",
                // Spanish
                "gracias", "muchas gracias",
                // French
                "merci", "merci beaucoup",
                // Tamil
                "நன்றி", "ரொம்ப நன்றி"
        })) {
            return "thanks";
        }

        // ---- Apology (EN / ES / FR / TA) ----
        if (containsAny(t, new String[]{
                // English
                "sorry", "my bad", "apologise", "apologize",
                "i didn’t mean", "i didnt mean",
                // Spanish
                "lo siento", "perdón", "disculpa", "disculpe",
                // French
                "désolé", "je suis désolé", "pardon",
                // Tamil
                "மன்னிச்சு", "மன்னிக்கவும்", "மன்னி"
        })) {
            return "apology";
        }

        // ---- Congrats / celebration (EN / ES / FR / TA) ----
        if (containsAny(t, new String[]{
                // English
                "congrats", "congratulations",
                "so proud of you", "proud of you",
                "well done", "good job", "nice job",
                "gg", "you smashed it", "you nailed it",
                // Spanish
                "felicidades", "enhorabuena",
                // French
                "félicitations",
                // Tamil
                "வாழ்த்துக்கள்", "ரொம்ப சந்தோஷம்"
        })) {
            return "congrats";
        }

        // ---- Love / affection (EN / ES / FR / TA) ----
        if (containsAny(t, new String[]{
                // English
                "i love you", "love u", "love ya",
                "i love u", "i luv u",
                "miss you", "i miss u",
                "you mean a lot", "you mean the world",
                "my favorite person", "my favourite person",
                // Spanish
                "te amo", "te quiero",
                // French
                "je t'aime",
                // Tamil
                "நான் உன்னை காதலிக்கிறேன்", "லவ் யூ"
        })) {
            return "love";
        }

        // ---- Hate / anger (extra social intent) ----
        if (containsAny(t, new String[]{
                "hate you", "i hate you", "i hate u",
                "so mad at you", "angry at you", "pissed at you"
        })) {
            return "hate";
        }

        // ---- Question (multilingual) ----
        boolean looksLikeQuestion =
                t.endsWith("?") ||
                        t.contains("?") ||
                        t.startsWith("can you") ||
                        t.startsWith("could you") ||
                        t.startsWith("do you know") ||
                        t.startsWith("what is") ||
                        t.startsWith("when is") ||
                        t.startsWith("where is") ||
                        t.startsWith("why is") ||
                        t.startsWith("how do i") ||
                        t.startsWith("how can i") ||
                        // Spanish question marks / French words
                        t.contains("¿") ||
                        t.contains("quoi") ||
                        t.contains("pourquoi") ||
                        // Tamil "how" / "why"
                        t.contains(" எப்படி") ||  // ta "how"
                        t.contains(" ஏன்");       // ta "why"

        if (looksLikeQuestion) {
            return "question";
        }

        // ---- Smalltalk ----
        if (containsAny(t, new String[]{
                "how’s it going", "hows it going",
                "what are you doing", "long time no see",
                "wyd", "wya"
        })) {
            return "smalltalk";
        }

        return "unknown";
    }

    private static boolean containsAny(String text, String[] patterns) {
        if (text == null || text.isEmpty()) return false;
        for (String p : patterns) {
            if (text.contains(p)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // Heuristic "mini model" for TONE (same as before)
    // ------------------------------------------------------------------------

    private static String heuristicTone(String text) {
        String t = text.toLowerCase(Locale.ROOT);

        boolean hasLol = t.contains("lol") || t.contains("lmao") || t.contains("rofl")
                || t.contains("😂") || t.contains("🤣") || t.contains("😜") || t.contains("😅");
        boolean hasHeart = t.contains("love") || t.contains("❤️") || t.contains("💕") || t.contains("🥰");
        boolean hasSorry = t.contains("sorry") || t.contains("apolog") || t.contains("my bad") || t.contains("🙏");
        boolean hasSad = t.contains("sad") || t.contains("hurt") || t.contains("down")
                || t.contains("upset") || t.contains("😔") || t.contains("😢");
        boolean hasAnger = t.contains("hate") || t.contains("angry") || t.contains("annoyed")
                || t.contains("pissed") || t.contains("😡");

        // Very strong anger → keep tone neutral label (we treat sentiment separately)
        if (hasAnger) {
            return "neutral";
        }

        if (hasLol) {
            return "humorous";
        }

        if (hasHeart) {
            return "friendly";
        }

        if (hasSorry || hasSad) {
            return "empathetic";
        }

        // Formal patterns
        if (t.contains("dear")
                || t.contains("sincerely")
                || t.contains("regards")
                || t.contains("kind regards")
                || t.contains("apologies for the inconvenience")
                || t.contains("please find attached")) {
            return "formal";
        }

        // Casual markers
        if (t.contains("bro") || t.contains("dude") || t.contains("man")
                || t.contains("ngl") || t.contains("no cap") || t.contains("lowkey")
                || t.contains("fr") || t.contains("gonna") || t.contains("wanna")
                || t.contains("bruh") || t.contains("fam")) {
            return "casual";
        }

        return "neutral";
    }
}
