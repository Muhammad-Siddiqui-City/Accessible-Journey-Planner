package com.example.ajp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Table 5.4 — AccessibilityPreferencesTest.
 * Method under test: table labels this isStepFreeEnabled(); the implementation uses {@link AccessibilityPreferences#isStepFree()}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AccessibilityPreferencesTest {

    @Before
    public void clearPrefs() {
        Context ctx = RuntimeEnvironment.getApplication();
        SharedPreferences p = ctx.getSharedPreferences("ajp_accessibility", Context.MODE_PRIVATE);
        p.edit().clear().commit();
    }

    /** isStepFreeEnabled() in table — behaviour of step-free toggle (implemented as {@link AccessibilityPreferences#isStepFree()}). */
    @Test
    public void isStepFreeEnabled() {
        Context ctx = RuntimeEnvironment.getApplication();
        AccessibilityPreferences prefs = new AccessibilityPreferences(ctx);

        Assert.assertFalse(prefs.isStepFree());
        prefs.setStepFree(true);
        Assert.assertTrue(prefs.isStepFree());
        prefs.setStepFree(false);
        Assert.assertFalse(prefs.isStepFree());
    }
}
