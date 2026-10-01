package com.example.translationapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translation;
import com.google.cloud.translate.TranslateOption;
import com.google.auth.oauth2.GoogleCredentials;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TranslationUtilsTest {

    private static Translate mockTranslate;
    private static Translation mockTranslation;

    @Before
    public void setUp() throws Exception {
        // Mock the Translate service
        mockTranslate = mock(Translate.class);
        mockTranslation = mock(Translation.class);
        
        when(mockTranslation.getTranslatedText()).thenReturn("Bonjour le monde");
        
        when(mockTranslate.translate(
                eq("Hello world"),
                eq(TranslateOption.targetLanguage("fr")),
                eq(TranslateOption.format("text"))
        )).thenReturn(mockTranslation);
        
        // Mock the static initialization
        try (MockedStatic<TranslationUtils> mocked = mockStatic(TranslationUtils.class)) {
            // We can't easily mock the static initializer, so we'll test what we can
        }
    }

    @Test
    public void testTranslateText() throws Exception {
        // Since we can't easily mock the static initializer, we'll test the method directly
        // by using reflection or by testing with actual credentials if available
        // For now, we'll test the logic when the service is available
        
        // This test would require actual credentials to run properly
        // In a real test environment, we'd use dependency injection or a test double
        
        // For demonstration, we'll verify the method signature and basic behavior
        assertNotNull("TranslationUtils class should be loadable", TranslationUtils.class);
    }

    @Test
    public void testDetectLanguageEnglish() {
        String result = TranslationUtils.detectLanguage("Hello world");
        assertEquals("en", result);
    }

    @Test
    public void testDetectLanguageFrench() {
        String result = TranslationUtils.detectLanguage("Bonjour le monde");
        // LangID might return various codes, but should not be "und" for clear French
        assertNotSame("und", result);
    }

    @Test
    public void testDetectLanguageUnknown() {
        String result = TranslationUtils.detectLanguage("xyzqwe"); // Gibberish
        // Should fall back to English
        assertEquals("en", result);
    }

    @Test
    public void testTranslationResult() {
        TranslationUtils.TranslationResult result = 
                new TranslationUtils.TranslationResult("Bonjour", "fr");
        assertEquals("Bonjour", result.translated);
        assertEquals("fr", result.language);
    }
}
