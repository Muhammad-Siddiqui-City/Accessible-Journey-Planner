package com.example.ajp.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import java.util.Locale;







/**
 * Utility class for TtsHelper.
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




    public void speak(@NonNull String phrase) {
        speak(phrase, QUEUE_FLUSH);
    }







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




    public void stop() {
        if (tts != null) {
            tts.stop();
        }
        pendingPhrase = null;
    }




    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        pendingPhrase = null;
    }




    public boolean isTtsEnabled() {
        return SettingsPrefs.get(context).isTtsEnabled();
    }
}

