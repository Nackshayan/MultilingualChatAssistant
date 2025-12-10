package com.example.multilingualchatassistant.util;

import java.util.Locale;

public class LanguageUtils {

    public static String nameToCode(String name) {
        if (name == null) return "en";
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("spanish")) return "es";
        if (n.contains("french")) return "fr";
        if (n.contains("tamil")) return "ta";
        return "en";
    }

    public static String codeToName(String code) {
        if (code == null) return "English";
        switch (code) {
            case "es":
                return "Spanish";
            case "fr":
                return "French";
            case "ta":
                return "Tamil";
            default:
                return "English";
        }
    }

    public static boolean codesEqual(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
