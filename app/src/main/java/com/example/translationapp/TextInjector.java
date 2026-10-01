package com.example.translationapp;

import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Bundle;

public class TextInjector {
    public static void injectText(AccessibilityNodeInfo node, String text) {
        if (node == null) return;
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }
}
