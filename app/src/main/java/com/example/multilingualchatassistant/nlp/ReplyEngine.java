package com.example.multilingualchatassistant.nlp;

import androidx.annotation.NonNull;

import com.example.multilingualchatassistant.util.LanguageUtils;

/**
 * ReplyEngine
 *
 * Central coordinator for:
 *  - intent detection (incoming + reply)
 *  - tone detection (reply, with manual override)
 *  - slang-aware styling in user language
 *  - translation to target language
 *  - slang / emoji injection in target language
 *
 * Works fully on-device using:
 *  LocalNlpModel + IntentDetector + ToneDetector + StyleEngine + SlangProcessor + TranslatorHelper.
 */
public class ReplyEngine {

    public interface Callback {
        void onReplyReady(@NonNull ReplyResult result);
        void onError(@NonNull Exception e);
    }

    /**
     * Container for everything MainActivity needs.
     */
    public static class ReplyResult {
        public String intent;              // e.g., "love", "thanks", "congrats", "unknown"
        public String tone;                // final tone actually used (auto or manual)
        public String userLangCode;        // display language (user UI language)
        public String sendLangCode;        // selected send language (where we paste reply)
        public String replyUserMeaning;    // styled reply in user language (for preview)
        public String replyToSend;         // final reply in send language (with slang / emojis)
    }

    /**
     * High-level API used from MainActivity.
     *
     * @param incomingText      original text received (possibly in foreign language)
     * @param replyUserInput    what the user typed in their UI language
     * @param userLangCode      language code for display (spinner: "You read in")
     * @param sendLangCode      language code we will send in (for you: always incoming language)
     * @param toneOverrideCode  "auto" or one of: friendly, formal, casual, humorous, empathetic, neutral
     * @param callback          async result
     */
    public static void generateReplyAsync(
            @NonNull String incomingText,
            @NonNull String replyUserInput,
            @NonNull String userLangCode,
            @NonNull String sendLangCode,
            @NonNull String toneOverrideCode,
            @NonNull Callback callback
    ) {

        try {
            // 1) Normalize slang in user reply for better detection
            String normalizedForDetection =
                    SlangProcessor.normalizeInput(userLangCode, replyUserInput);

            // 2) Detect intent & auto tone (local model + rules)
            String autoIntent = IntentDetector.detectIntent(incomingText, normalizedForDetection);
            String autoTone = ToneDetector.detectTone(normalizedForDetection);

            // 3) Apply manual tone override if user selected one
            String finalTone;
            if (toneOverrideCode != null && !"auto".equalsIgnoreCase(toneOverrideCode)) {
                finalTone = toneOverrideCode.toLowerCase();
            } else {
                finalTone = autoTone;
            }
            String intent = autoIntent;

            // 4) Style reply in user language (idioms + emoji, no target slang yet)
            String styledUserLang =
                    StyleEngine.styleReply(replyUserInput, userLangCode, finalTone, intent);

            // Prepare result object and fill common fields
            ReplyResult base = new ReplyResult();
            base.intent = intent;
            base.tone = finalTone;
            base.userLangCode = userLangCode;
            base.sendLangCode = sendLangCode;
            base.replyUserMeaning = styledUserLang; // always this in user language

            // 5) If send language == user language → just apply slang & return
            if (LanguageUtils.codesEqual(userLangCode, sendLangCode)) {
                String finalWithSlang =
                        SlangProcessor.applySlang(sendLangCode, styledUserLang, finalTone, intent);

                base.replyToSend = finalWithSlang;
                callback.onReplyReady(base);
                return;
            }

            // 6) Otherwise: translate from user language -> send language, then inject target slang.
            TranslatorHelper.translate(userLangCode, sendLangCode, styledUserLang,
                    new TranslatorHelper.TranslateCallback() {
                        @Override
                        public void onTranslated(String translatedText) {
                            try {
                                String withSlang =
                                        SlangProcessor.applySlang(sendLangCode, translatedText, finalTone, intent);

                                ReplyResult result = new ReplyResult();
                                result.intent = base.intent;
                                result.tone = base.tone;
                                result.userLangCode = base.userLangCode;
                                result.sendLangCode = sendLangCode;
                                result.replyUserMeaning = base.replyUserMeaning;
                                result.replyToSend = withSlang;

                                callback.onReplyReady(result);
                            } catch (Exception e) {
                                callback.onError(e);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            // Fallback: keep user-language styled text with user slang
                            String fallback =
                                    SlangProcessor.applySlang(userLangCode, styledUserLang, finalTone, intent);

                            ReplyResult result = new ReplyResult();
                            result.intent = base.intent;
                            result.tone = base.tone;
                            result.userLangCode = base.userLangCode;
                            result.sendLangCode = userLangCode;
                            result.replyUserMeaning = base.replyUserMeaning;
                            result.replyToSend = fallback;

                            // We still return success so the UI continues to work
                            callback.onReplyReady(result);
                        }
                    });

        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
