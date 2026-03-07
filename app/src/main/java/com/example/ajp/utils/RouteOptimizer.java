package com.example.ajp.utils;

import com.example.ajp.ui.journey.RouteItem;
import java.util.Collections;
import java.util.List;

/**
 * Sorts journey options by strategy. Add in Commit 11.
 * PURPOSE: Rank TfL journey results by FASTEST, FEWEST_TRANSFERS, LEAST_WALKING, LEAST_CROWDED; lower score = better.
 * WHY: Does not recalculate paths; only reorders; first item gets "BEST" badge in RouteAdapter.
 * ISSUES: None.
 */
public class RouteOptimizer {

    public enum Strategy {
        FASTEST,
        FEWEST_TRANSFERS,
        LEAST_WALKING,
        LEAST_CROWDED
    }

    /**
     * Calculates a "Pain Score" for a route (lower is better).
     * Base score is journey time; strategy adds weighted penalties.
     * When avoidCrowded is true, crowded routes get a large penalty so they sort to the bottom.
     */
    private static double calculateScore(RouteItem route, Strategy strategy, boolean avoidCrowded) {
        double score = route.getDurationMinutesInt();

        switch (strategy) {
            case FASTEST:
                score += (route.getTransfersCount() * 2);
                break;

            case FEWEST_TRANSFERS:
                score += (route.getTransfersCount() * 15);
                break;

            case LEAST_WALKING:
                score += (route.getTotalWalkingMinutes() * 3.0);
                break;

            case LEAST_CROWDED:
                if (route.getCrowdingLevel() == RouteItem.CROWDING_HIGH) score += 30;
                else if (route.getCrowdingLevel() == RouteItem.CROWDING_MEDIUM) score += 10;
                break;
        }

        if (avoidCrowded) {
            if (route.getCrowdingLevel() == RouteItem.CROWDING_HIGH) score += 1000.0;
            else if (route.getCrowdingLevel() == RouteItem.CROWDING_MEDIUM) score += 300.0;
        }

        if (route.hasLiftDisruption()) {
            score += 1000.0;
        }

        return score;
    }

    /**
     * Sorts routes in place by the given strategy. First item becomes "best".
     * When avoidCrowded is true, crowded routes are placed at the bottom of the list.
     */
    public static void sortRoutes(List<RouteItem> routes, Strategy strategy) {
        sortRoutes(routes, strategy, false);
    }

    /**
     * Sorts routes in place. When avoidCrowded is true, high/medium crowding routes sort to the bottom.
     */
    public static void sortRoutes(List<RouteItem> routes, Strategy strategy, boolean avoidCrowded) {
        if (routes == null || routes.isEmpty()) return;

        Collections.sort(routes, (r1, r2) -> {
            double s1 = calculateScore(r1, strategy, avoidCrowded);
            double s2 = calculateScore(r2, strategy, avoidCrowded);
            return Double.compare(s1, s2);
        });
    }
}
