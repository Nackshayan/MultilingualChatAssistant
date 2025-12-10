package com.example.multilingualchatassistant.nlp;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * StyleEngine
 *
 * Turns the raw reply the user typed into a nicer sentence:
 *  - Uses intent (greeting / thanks / apology / congrats / love / unknown)
 *  - Uses tone (friendly, formal, casual, humorous, empathetic, neutral)
 *  - Has language-specific templates for EN / ES / FR
 *  - For other languages, keeps the text and adds emojis so we don't break grammar.
 *
 * This is like a tiny on-device "reply library".
 */
public class StyleEngine {

    @NonNull
    public static String styleReply(@NonNull String rawText,
                                    @NonNull String langCode,
                                    @NonNull String tone,
                                    @NonNull String intent) {

        String text = rawText.trim();
        if (text.isEmpty()) return rawText;

        String lcLang = langCode.toLowerCase(Locale.ROOT);
        String lcTone = tone == null ? "neutral" : tone.toLowerCase(Locale.ROOT);
        String lcIntent = intent == null ? "unknown" : intent.toLowerCase(Locale.ROOT);
        String lower = text.toLowerCase(Locale.ROOT);

        boolean richLanguage =
                "en".equals(lcLang) || "es".equals(lcLang) || "fr".equals(lcLang);

        // If intent is unknown but message looks like a simple greeting, treat it as greeting
        if ("unknown".equals(lcIntent)) {
            if (looksLikeGreeting(lower, lcLang)) {
                lcIntent = "greeting";
            }
        }

        // 1) Full templates for EN / ES / FR
        if (richLanguage) {
            switch (lcIntent) {
                case "greeting":
                    return buildGreeting(lcLang, lcTone);
                case "thanks":
                    return buildThanks(lcLang, lcTone);
                case "apology":
                    return buildApology(lcLang, lcTone);
                case "congrats":
                case "congratulations":
                    return buildCongrats(lcLang, lcTone);
                case "love":
                    return buildLove(lcLang, lcTone);
                default:
                    // other intents → lightly decorate
                    return styleGeneric(text, lcLang, lcTone, lcIntent);
            }
        }

        // 2) Other languages → keep text but decorate with emojis based on intent/tone
        return addEmojiFallback(text, lcTone, lcIntent);
    }

    // --------------------------------------------------------------------
    // Greeting detector (very small, just to upgrade "hello"/"hola"/"bonjour")
    // --------------------------------------------------------------------
    private static boolean looksLikeGreeting(String lower, String lang) {
        switch (lang) {
            case "es":
                return lower.matches("^(hola|buenos dias|buenos días|buenas tardes|buenas noches)[!.]*$");
            case "fr":
                return lower.matches("^(salut|bonjour|bonsoir)[!.]*$");
            case "en":
            default:
                return lower.matches("^(hi|hello|hey|hey there|hi there)[!.]*$");
        }
    }

    // --------------------------------------------------------------------
    // EN / ES / FR templates
    // --------------------------------------------------------------------
    private static String buildGreeting(String lang, String tone) {
        switch (lang) {
            case "es": // Spanish
                switch (tone) {
                    case "formal":
                        return "Hola, muchas gracias por tu mensaje. ¿En qué puedo ayudarte hoy? 😊";
                    case "humorous":
                        return "¡Hey! Has entrado en mi chat, ¿qué se cuenta? 😄";
                    case "empathetic":
                        return "Hola, me alegra mucho saber de ti. ¿Cómo estás? ❤️";
                    case "casual":
                    case "friendly":
                        return "¡Hey! Gracias por escribir 😊 ¿Qué tal todo?";
                    case "neutral":
                    default:
                        return "Hola, gracias por tu mensaje. ¿En qué puedo ayudarte?";
                }
            case "fr": // French
                switch (tone) {
                    case "formal":
                        return "Bonjour, merci beaucoup pour votre message. Comment puis-je vous aider aujourd’hui ? 😊";
                    case "humorous":
                        return "Salut ! Tu es officiellement dans mon chat 😄 Quoi de neuf ?";
                    case "empathetic":
                        return "Salut, ça me fait vraiment plaisir d’avoir de tes nouvelles. Comment tu vas ? ❤️";
                    case "casual":
                    case "friendly":
                        return "Salut ! Merci pour ton message 😊 Ça va sinon ?";
                    case "neutral":
                    default:
                        return "Bonjour, merci pour votre message. Comment puis-je aider ?";
                }
            case "en": // English
            default:
                switch (tone) {
                    case "formal":
                        return "Hello, whats'up 😊";
                    case "humorous":
                        return "Hey hey! You’ve officially entered my chat zone 😄 What’s up?";
                    case "empathetic":
                        return "Hey, it’s really nice to hear from you. How are you doing? ❤️";
                    case "casual":
                    case "friendly":
                        return "Hey! Thanks for reaching out 😊 How’s everything going?";
                    case "neutral":
                    default:
                        return "Hello! Thanks for your message. How can I help?";
                }
        }
    }

    private static String buildThanks(String lang, String tone) {
        switch (lang) {
            case "es":
                switch (tone) {
                    case "formal":
                        return "Muchas gracias, de verdad aprecio tu ayuda y tu tiempo. 🙏";
                    case "humorous":
                        return "¡Muchísimas gracias, eres un(a) crack! 😄🙏";
                    case "empathetic":
                        return "De verdad, muchas gracias, significa mucho para mí. ❤️🙏";
                    case "casual":
                    case "friendly":
                        return "¡Gracias, de verdad lo aprecio mucho! 😊🙏";
                    case "neutral":
                    default:
                        return "Muchas gracias, lo aprecio mucho. 🙏";
                }
            case "fr":
                switch (tone) {
                    case "formal":
                        return "Merci beaucoup, j’apprécie vraiment votre aide et votre temps. 🙏";
                    case "humorous":
                        return "Un grand merci, tu gères grave 😄🙏";
                    case "empathetic":
                        return "Merci beaucoup, ça compte vraiment pour moi. ❤️🙏";
                    case "casual":
                    case "friendly":
                        return "Merci beaucoup, j’apprécie vraiment 😊🙏";
                    case "neutral":
                    default:
                        return "Merci beaucoup, j’apprécie vraiment. 🙏";
                }
            case "en":
            default:
                switch (tone) {
                    case "formal":
                        return "Thank you, I genuinely appreciate your help and time. 🙏";
                    case "humorous":
                        return "Huge thanks, you’re a lifesaver 😄🙏";
                    case "empathetic":
                        return "Thank you so much, it really means a lot to me. ❤️🙏";
                    case "casual":
                    case "friendly":
                        return "Thanks a ton, I really appreciate it 😊🙏";
                    case "neutral":
                    default:
                        return "Thank you, I really appreciate it. 🙏";
                }
        }
    }

    private static String buildApology(String lang, String tone) {
        switch (lang) {
            case "es":
                switch (tone) {
                    case "formal":
                        return "Lamento sinceramente las molestias y haré todo lo posible para que no vuelva a ocurrir. 🙇‍♂️";
                    case "empathetic":
                        return "Lo siento mucho, entiendo que esto puede ser muy frustrante. ❤️";
                    case "humorous":
                        return "Ups, ahí metí la pata 😅 Lo siento de verdad.";
                    case "casual":
                    case "friendly":
                        return "Lo siento muchísimo, intentaré arreglarlo lo antes posible. 🙏";
                    case "neutral":
                    default:
                        return "Lo siento por las molestias. 🙏";
                }
            case "fr":
                switch (tone) {
                    case "formal":
                        return "Je vous prie de m’excuser pour ce désagrément, je ferai en sorte que cela ne se reproduise plus. 🙇‍♂️";
                    case "empathetic":
                        return "Je suis vraiment désolé, je comprends que ce soit frustrant. ❤️";
                    case "humorous":
                        return "Oups, là j’ai un peu foiré 😅 Désolé !";
                    case "casual":
                    case "friendly":
                        return "Je suis vraiment désolé, je vais essayer de régler ça au plus vite. 🙏";
                    case "neutral":
                    default:
                        return "Je suis désolé pour le dérangement. 🙏";
                }
            case "en":
            default:
                switch (tone) {
                    case "formal":
                        return "I sincerely apologise for the inconvenience and I’ll make sure it doesn’t happen again. 🙇‍♂️";
                    case "empathetic":
                        return "I’m really sorry about this, I understand how frustrating it must be. ❤️";
                    case "humorous":
                        return "I definitely messed up there 😅 I’m really sorry about that.";
                    case "casual":
                    case "friendly":
                        return "I’m really sorry about that, I’ll try to fix it as soon as possible. 🙏";
                    case "neutral":
                    default:
                        return "I’m sorry for the inconvenience. 🙏";
                }
        }
    }

    private static String buildCongrats(String lang, String tone) {
        switch (lang) {
            case "es":
                switch (tone) {
                    case "formal":
                        return "Muchas felicidades por tu logro, te lo mereces de verdad. 🎉";
                    case "humorous":
                        return "¡Enhorabuena! Estás a otro nivel 😄🎉🔥";
                    case "empathetic":
                        return "Muchísimas felicidades, me alegro un montón por ti 🥹❤️🎉";
                    case "casual":
                    case "friendly":
                        return "¡Felicidades, es una noticia increíble! 🎉";
                    case "neutral":
                    default:
                        return "Felicidades, es una gran noticia. 🎉";
                }
            case "fr":
                switch (tone) {
                    case "formal":
                        return "Félicitations pour cette réussite, vous le méritez vraiment. 🎉";
                    case "humorous":
                        return "Félicitations ! Tu déchires totalement 😄🎉🔥";
                    case "empathetic":
                        return "Un grand bravo, je suis vraiment heureux(se) pour toi 🥹❤️🎉";
                    case "casual":
                    case "friendly":
                        return "Félicitations, c’est une super nouvelle ! 🎉";
                    case "neutral":
                    default:
                        return "Félicitations, c’est une excellente nouvelle. 🎉";
                }
            case "en":
            default:
                switch (tone) {
                    case "formal":
                        return "Congratulations on your achievement, you truly deserve it. 🎉";
                    case "humorous":
                        return "Congrats! You’re absolutely smashing it 😄🎉🔥";
                    case "empathetic":
                        return "Big congratulations, I’m genuinely happy for you 🥹❤️🎉";
                    case "casual":
                    case "friendly":
                        return "Congrats, that’s awesome news! 🎉";
                    case "neutral":
                    default:
                        return "Congratulations, that’s great news. 🎉";
                }
        }
    }

    private static String buildLove(String lang, String tone) {
        switch (lang) {
            case "es":
                switch (tone) {
                    case "formal":
                        return "Te aprecio muchísimo y valoro de verdad tenerte en mi vida. ❤️";
                    case "humorous":
                        return "Eres oficialmente mi persona favorita 😌❤️";
                    case "empathetic":
                        return "Te quiero mucho y siempre voy a estar aquí para ti. ❤️";
                    case "casual":
                    case "friendly":
                        return "Te quiero muchísimo y me encanta pasar tiempo contigo ❤️";
                    case "neutral":
                    default:
                        return "Te quiero y te aprecio de verdad. ❤️";
                }
            case "fr":
                switch (tone) {
                    case "formal":
                        return "Je tiens énormément à toi et je suis vraiment reconnaissant(e) de t’avoir dans ma vie. ❤️";
                    case "humorous":
                        return "Tu es officiellement ma personne préférée 😌❤️";
                    case "empathetic":
                        return "Je tiens beaucoup à toi et je serai toujours là pour toi. ❤️";
                    case "casual":
                    case "friendly":
                        return "Je t’aime énormément et j’adore passer du temps avec toi ❤️";
                    case "neutral":
                    default:
                        return "Je tiens beaucoup à toi. ❤️";
                }
            case "en":
            default:
                switch (tone) {
                    case "formal":
                        return "I care about you deeply and truly appreciate having you in my life. ❤️";
                    case "humorous":
                        return "You’re my favourite human, no contest 😌❤️";
                    case "empathetic":
                        return "I really care about you, and I’m always here for you. ❤️";
                    case "casual":
                    case "friendly":
                        return "I really like you a lot, and I love spending time with you ❤️";
                    case "neutral":
                    default:
                        return "I care about you very much. ❤️";
                }
        }
    }

    // --------------------------------------------------------------------
    // Generic styling for other intents in EN / ES / FR
    // --------------------------------------------------------------------
    private static String styleGeneric(String text, String lang, String tone, String intent) {
        // If long text, don't touch much
        if (text.length() > 80) return text;

        String emojiTail = "";
        switch (intent) {
            case "thanks":
                emojiTail = " 🙏";
                break;
            case "love":
                emojiTail = " ❤️";
                break;
            case "congrats":
            case "congratulations":
                emojiTail = " 🎉";
                break;
            case "apology":
                emojiTail = " 🙏";
                break;
            default:
                // use tone
                switch (tone) {
                    case "humorous":
                        emojiTail = " 😄";
                        break;
                    case "friendly":
                    case "casual":
                        emojiTail = " 😊";
                        break;
                    case "empathetic":
                        emojiTail = " ❤️";
                        break;
                    default:
                        emojiTail = "";
                }
        }

        // Add punctuation depending on tone
        String result = text;
        if (!result.endsWith("!") && !result.endsWith("?") && !result.endsWith(".")) {
            if ("humorous".equals(tone) || "casual".equals(tone) || "friendly".equals(tone)) {
                result += "!";
            } else {
                result += ".";
            }
        }
        return result + emojiTail;
    }

    // --------------------------------------------------------------------
    // Fallback for ALL other languages – keep text, just add emoji
    // --------------------------------------------------------------------
    private static String addEmojiFallback(String text, String tone, String intent) {
        String emoji = "";

        switch (intent) {
            case "thanks":
                emoji = " 🙏";
                break;
            case "love":
                emoji = " ❤️";
                break;
            case "congrats":
            case "congratulations":
                emoji = " 🎉";
                break;
            case "apology":
                emoji = " 🙏";
                break;
            case "greeting":
                emoji = " 😊";
                break;
            default:
                switch (tone) {
                    case "humorous":
                        emoji = " 😄";
                        break;
                    case "friendly":
                    case "casual":
                        emoji = " 😊";
                        break;
                    case "empathetic":
                        emoji = " ❤️";
                        break;
                    default:
                        emoji = "";
                }
        }

        if (emoji.isEmpty()) return text;
        return text + emoji;
    }
}
