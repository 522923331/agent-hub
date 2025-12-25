package com.agenthub.knowledge.extract;

import org.springframework.stereotype.Component;

@Component
public class LanguageDetector {

    public String detect(String text) {
        if (text == null || text.isBlank()) return "unknown";
        int zh = 0;
        int latin = 0;
        int len = Math.min(text.length(), 4000);
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (isCjk(c)) zh++;
            else if (isLatinLetter(c)) latin++;
        }
        if (zh == 0 && latin == 0) return "unknown";
        if (zh >= latin) return "zh";
        return "en";
    }

    private boolean isLatinLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isCjk(char c) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(c);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }
}


