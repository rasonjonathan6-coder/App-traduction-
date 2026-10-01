package com.example.translationapp;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.AccessibilityEvent;
import android.view.AccessibilityNodeInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Accessibility service that captures text changes and button clicks,
 * then shows a floating overlay for translation.
 */
public class TranslationAccessibilityService extends AccessibilityService {

    private static final String TAG = "TranslationAccessibilityService";
    private WindowManager windowManager;
    private View overlayView;
    private EditText sourceTextView;
    private EditText translationTextView;
    private Button sendButton;
    private FrameLayout overlayContainer;
    
    // For drag functionality
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private WindowManager.LayoutParams params;
    
    // For storing last translated text
    private String lastTranslatedText = "";
    private String lastSourceText = "";
    private String detectedLanguage = "en"; // Default to English
    
    @Override
    public void onCreate() {
        super.onCreate();
        loadLastTranslatedText();
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            
            // Get the source of the event
            AccessibilityNodeInfo source = event.getSource();
            if (source == null) {
                return;
            }
            
            // Check if it's a Send/Send button
            if (isSendButton(source)) {
                // Capture the text from the associated input field
                captureAndTranslateText(source);
            }
            
            // Recycle the source
            source.recycle();
        }
    }
    
    private boolean isSendButton(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return false;
        
        // Check if it's a button
        if (!"android.widget.Button".equals(nodeInfo.getClassName())) {
            return false;
        }
        
        // Check text or content description for send-related terms
        CharSequence text = nodeInfo.getText();
        CharSequence desc = nodeInfo.getContentDescription();
        
        String textStr = text != null ? text.toString().toLowerCase(Locale.ROOT) : "";
        String descStr = desc != null ? desc.toString().toLowerCase(Locale.ROOT) : "";
        
        return textStr.contains("send") || 
               textStr.contains("envoyer") ||
               descStr.contains("send") ||
               descStr.contains("envoyer") ||
               nodeInfo.isClickable();
    }
    
    private void captureAndTranslateText(AccessibilityNodeInfo buttonNode) {
        if (buttonNode == null) return;
        
        // Find the associated input field (usually sibling or parent's child)
        AccessibilityNodeInfo inputField = findAssociatedInputField(buttonNode);
        if (inputField == null) {
            // Try to get text from clipboard as fallback
            String clipboardText = getClipboardText();
            if (!TextUtils.isEmpty(clipboardText)) {
                processTextForTranslation(clipboardText);
            }
            return;
        }
        
        // Get the text from the input field
        CharSequence inputText = inputField.getText();
        if (inputText != null && !TextUtils.isEmpty(inputText)) {
            processTextForTranslation(inputText.toString());
        }
        
        // Recycle
        inputField.recycle();
    }
    
    private AccessibilityNodeInfo findAssociatedInputField(AccessibilityNodeInfo buttonNode) {
        if (buttonNode == null) return null;
        
        // Get parent and look for EditText siblings
        AccessibilityNodeInfo parent = buttonNode.getParent();
        if (parent == null) return null;
        
        // Look for EditText in parent's children
        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child != null && 
                "android.widget.EditText".equals(child.getClassName()) &&
                child.isEnabled() && 
                child.isFocused()) {
                return child;
            }
            if (child != null) {
                child.recycle();
            }
        }
        
        // Recycle parent
        parent.recycle();
        return null;
    }
    
    private String getClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                ClipData.Item item = clip.getItemAt(0);
                if (item != null) {
                    return item.getText().toString();
                }
            }
        }
        return "";
    }
    
    private void processTextForTranslation(String text) {
        if (TextUtils.isEmpty(text)) return;
        
        // Store original text
        lastSourceText = text;
        
        // Detect language and translate to French
        new Thread(() -> {
            try {
                // Detect language using TranslationUtils
                detectedLanguage = TranslationUtils.detectLanguage(text);
                
                // Translate to French
                TranslationUtils.TranslationResult result = TranslationUtils.translateText(text, "fr").get();
                
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    lastTranslatedText = result.translated;
                    showOverlay(result.translated);
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(TranslationAccessibilityService.this,
                            "Translation error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(TranslationAccessibilityService.this,
                            "Translation error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void showOverlay(String frenchText) {
        if (overlayView != null) {
            // Update existing overlay
            sourceTextView.setText(lastSourceText);
            translationTextView.setText(frenchText);
            return;
        }
        
        // Create new overlay
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);
        
        // Find views
        sourceTextView = overlayView.findViewById(R.id.sourceTextView);
        translationTextView = overlayView.findViewById(R.id.translationTextView);
        sendButton = overlayView.findViewById(R.id.sendButton);
        overlayContainer = overlayView.findViewById(R.id.overlayContainer);
        
        // Set initial text
        sourceTextView.setText(lastSourceText);
        translationTextView.setText(frenchText);
        
        // Make translation text editable
        translationTextView.setFocusableInTouchMode(true);
        translationTextView.setFocusable(true);
        
        // Set up send button click listener
        sendButton.setOnClickListener(v -> {
            String responseText = translationTextView.getText().toString();
            if (!TextUtils.isEmpty(responseText)) {
                // Translate response back to original language
                new Thread(() -> {
                    try {
                        TranslationUtils.TranslationResult result = TranslationUtils.translateText(responseText, detectedLanguage).get();
                        
                        // Send the translated response back to the original app
                        sendTranslationResult(result.translated);
                        
                        // Clear the response field and hide overlay
                        new Handler(Looper.getMainLooper()).post(() -> {
                            translationTextView.setText("");
                            saveLastTranslatedText(responseText);
                            hideOverlay();
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(TranslationAccessibilityService.this,
                                    "Translation error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(TranslationAccessibilityService.this,
                                    "Translation error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }
        });
        
        // Set up drag functionality
        setupDragListener();
        
        // Set up double-tap to re-center
        setupDoubleTapToCenter();
        
        // Configure window parameters
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;
        params.alpha = 0.9f; // Semi-transparent
        
        windowManager.addView(overlayView, params);
    }
    
    private void setupDragListener() {
        overlayContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });
    }
    
    private void setupDoubleTapToCenter() {
        overlayContainer.setOnTouchListener(new View.OnTouchListener() {
            private long lastTapTime = 0;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long currentTime = System.currentTimeMillis();
                    long tapInterval = currentTime - lastTapTime;
                    
                    if (tapInterval < 300 && tapInterval > 0) {
                        // Double tap detected
                        centerOverlay();
                        lastTapTime = 0;
                        return true;
                    }
                    lastTapTime = currentTime;
                }
                return false;
            }
        });
    }
    
    private void centerOverlay() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        
        params.x = (width - overlayView.getWidth()) / 2;
        params.y = (height - overlayView.getHeight()) / 2;
        wm.updateViewLayout(overlayView, params);
    }
    
    private void sendTranslationResult(String translatedText) {
        // For now, we'll put it in clipboard as a fallback
        // In a more advanced implementation, we'd use AccessibilityNodeInfo to inject text
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("translated_text", translatedText);
        clipboard.setPrimaryClip(clip);
        
        // Show toast to indicate success
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(TranslationAccessibilityService.this,
                    "Translation copied to clipboard", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void hideOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }
    
    private void loadLastTranslatedText() {
        lastTranslatedText = getSharedPreferences("translation_prefs", MODE_PRIVATE)
                .getString("last_translated_text", "");
        lastSourceText = getSharedPreferences("translation_prefs", MODE_PRIVATE)
                .getString("last_source_text", "");
    }
    
    private void saveLastTranslatedText(String text) {
        getSharedPreferences("translation_prefs", MODE_PRIVATE)
                .edit()
                .putString("last_translated_text", text)
                .apply();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
    }
    
    @Override
    public void onInterrupt() {
        // Handle interruption
    }
}
