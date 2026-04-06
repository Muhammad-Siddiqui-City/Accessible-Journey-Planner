package com.example.ajp.ui.journey;

import android.graphics.Color;

/**
 * UI-side model/helper used by RouteLineColors.
 * Encapsulates presentation-oriented behavior needed by screens in this feature package.
 * Keeping this separate helps avoid leaking API/database concerns into view code.
 */

public final class RouteLineColors {

    /** Walking legs — distinct from all tube lines and national rail brown. */
    public static final int WALKING = Color.parseColor("#3949AB");

    /** South Western Railway — black bar, matching real-world branding (same on cards and step list). */
    public static final int SOUTH_WESTERN_RAILWAY = Color.parseColor("#000000");

    /** Other National Rail when not a named tube line. */
    public static final int NATIONAL_RAIL = Color.parseColor("#5D4037");

    private RouteLineColors() { }
}


