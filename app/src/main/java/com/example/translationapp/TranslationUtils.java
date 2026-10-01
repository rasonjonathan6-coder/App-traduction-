package com.example.translationapp;

import com.google.cloud.translate.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.android.libraries.langid.LanguageIdentifier;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;

public class TranslationUtils {
    private static final Translate translate;
    private static final LanguageIdentifier langId = LanguageIdentifier.getInstance();
    
    static {
        try {
            String home = System.getProperty("user.home");
            String credentialsPath = home + "/keys/heart-translate.json";
            FileInputStream cis = new FileInputStream(credentialsPath);
            translate = TranslateOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(cis))
                    .build()
                    .getService();
        } catch (Exception e) {
            throw new RuntimeException("Cannot init Translate API", e);
        }
    }

    public static CompletableFuture<TranslationResult> translateText(
            String source, String targetLang) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Translation translation = translate.translate(
                        source,
                        Translate.TranslateOption.targetLanguage(targetLang),
                        Translate.TranslateOption.format("text"));
                return new TranslationResult(
                        translation.getTranslatedText(),
                        targetLang);
            } catch (Exception e) {
                throw new RuntimeException("Translation failed", e);
            }
        });
    }

    public static String detectLanguage(String text) {
        String lang = langId.findLanguage(text);
        return "und".equals(lang) ? "en" : lang;   // fallback to English
    }

    public static class TranslationResult {
        public final String translated;
        public final String language;
        public TranslationResult(String t, String l) { translated = t; language = l; }
    }
}
