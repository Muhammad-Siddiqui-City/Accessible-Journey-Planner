package com.example.ajp.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import java.util.Locale;

/**
 * Reusable Text-to-Speech helper for spoken alerts.
 * PURPOSE: Centralises TTS logic; respects SettingsPrefs.isTtsEnabled(); used by LiveArrivals, RouteDetails, Home (disruptions).
 * WHY: Avoid duplicate TTS init/shutdown code; consistent locale and queue behaviour across screens.
 * ISSUES: Caller must call shutdown() when done (e.g. in onDestroyView) to release TTS engine.
 */
public class TtsHelper {

    public static final int QUEUE_FLUSH = TextToSpeech.QUEUE_FLUSH;
    public static final int QUEUE_ADD = TextToSpeech.QUEUE_ADD;

    private final Context context;
    private TextToSpeech tts;
    private String pendingPhrase;
    private boolean flushOnNextSpeak;

    public TtsHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Speak a phrase if TTS is enabled. Uses QUEUE_FLUSH by default (interrupts previous).
     */
    public void speak(@NonNull String phrase) {
        speak(phrase, QUEUE_FLUSH);
    }

    /**
     * Speak a phrase if TTS is enabled.
     *
     * @param phrase Text to speak.
     * @param mode   QUEUE_FLUSH to interrupt, QUEUE_ADD to append.
     */
    public void speak(@NonNull String phrase, int mode) {
        if (phrase.trim().isEmpty()) return;
        if (!SettingsPrefs.get(context).isTtsEnabled()) return;

        if (tts == null) {
            pendingPhrase = phrase;
            flushOnNextSpeak = (mode == QUEUE_FLUSH);
            tts = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    tts.setLanguage(Locale.UK);
                    if (pendingPhrase != null) {
                        int m = flushOnNextSpeak ? QUEUE_FLUSH : QUEUE_ADD;
                        tts.speak(pendingPhrase, m, null, null);
                        pendingPhrase = null;
                    }
                }
            });
        } else {
            tts.speak(phrase, mode, null, null);
        }
    }

    /**
     * Stop any current or queued speech.
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
        pendingPhrase = null;
    }

    /**
     * Shutdown the TTS engine. Call from onDestroyView or onDestroy.
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        pendingPhrase = null;
    }

    /**
     * Returns whether TTS is enabled in settings.
     */
    public boolean isTtsEnabled() {
        return SettingsPrefs.get(context).isTtsEnabled();
    }
}
