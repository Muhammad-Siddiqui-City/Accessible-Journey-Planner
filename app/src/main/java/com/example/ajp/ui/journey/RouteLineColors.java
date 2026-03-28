package com.example.ajp.ui.journey;

import android.graphics.Color;

/**
 * Shared colours for line badges (journey cards) and step indicators so the same mode looks the same everywhere.
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
