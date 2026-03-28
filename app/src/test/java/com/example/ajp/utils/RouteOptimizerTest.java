package com.example.ajp.utils;

import com.example.ajp.api.Leg;
import com.example.ajp.ui.journey.RouteItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Table 5.4 — RouteOptimizerTest: {@link RouteOptimizer#sortRoutes(java.util.List, RouteOptimizer.Strategy, boolean)}.
 */
public class RouteOptimizerTest {

    private static final String[] NO_BADGES = new String[0];

    private static RouteItem makeRoute(String routeId, String durationLabel, int transfers,
            int walkingMinutesTotal, int crowding) {
        String transfersText = transfers + (transfers == 1 ? " change" : " changes");
        List<Leg> legs = new ArrayList<>();
        if (walkingMinutesTotal > 0) {
            Leg walk = Leg.createWalkingLeg(null, null, walkingMinutesTotal * 60, null);
            legs.add(walk);
        }
        return new RouteItem(
                durationLabel,
                "09:00",
                "10:00",
                crowding,
                transfersText,
                NO_BADGES,
                "summary",
                routeId,
                "A",
                "B",
                legs,
                false,
                null,
                null);
    }

    /** sortRoutes(FASTEST) — list ordered ascending by duration minutes (same transfer count on all routes). */
    @Test
    public void sortRoutes_FASTEST() {
        RouteItem r30 = makeRoute("a", "30 min", 1, 0, RouteItem.CROWDING_LOW);
        RouteItem r60 = makeRoute("b", "60 min", 1, 0, RouteItem.CROWDING_LOW);
        RouteItem r45 = makeRoute("c", "45 min", 1, 0, RouteItem.CROWDING_LOW);
        List<RouteItem> list = new ArrayList<>(Arrays.asList(r60, r30, r45));
        RouteOptimizer.sortRoutes(list, RouteOptimizer.Strategy.FASTEST, false);
        Assert.assertEquals("a", list.get(0).getRouteId());
        Assert.assertEquals("c", list.get(1).getRouteId());
        Assert.assertEquals("b", list.get(2).getRouteId());
    }

    /** sortRoutes(FEWEST_TRANSFERS) — ordering by transfer count ascending. */
    @Test
    public void sortRoutes_FEWEST_TRANSFERS() {
        RouteItem t0 = makeRoute("a", "40 min", 0, 0, RouteItem.CROWDING_LOW);
        RouteItem t2 = makeRoute("b", "40 min", 2, 0, RouteItem.CROWDING_LOW);
        RouteItem t1 = makeRoute("c", "40 min", 1, 0, RouteItem.CROWDING_LOW);
        List<RouteItem> list = new ArrayList<>(Arrays.asList(t2, t0, t1));
        RouteOptimizer.sortRoutes(list, RouteOptimizer.Strategy.FEWEST_TRANSFERS, false);
        Assert.assertEquals("a", list.get(0).getRouteId());
        Assert.assertEquals("c", list.get(1).getRouteId());
        Assert.assertEquals("b", list.get(2).getRouteId());
    }

    /** sortRoutes(LEAST_WALKING) — ordering by total walking minutes ascending. */
    @Test
    public void sortRoutes_LEAST_WALKING() {
        RouteItem w0 = makeRoute("a", "40 min", 0, 0, RouteItem.CROWDING_LOW);
        RouteItem w30 = makeRoute("b", "40 min", 0, 30, RouteItem.CROWDING_LOW);
        RouteItem w10 = makeRoute("c", "40 min", 0, 10, RouteItem.CROWDING_LOW);
        List<RouteItem> list = new ArrayList<>(Arrays.asList(w30, w0, w10));
        RouteOptimizer.sortRoutes(list, RouteOptimizer.Strategy.LEAST_WALKING, false);
        Assert.assertEquals("a", list.get(0).getRouteId());
        Assert.assertEquals("c", list.get(1).getRouteId());
        Assert.assertEquals("b", list.get(2).getRouteId());
    }

    /** sortRoutes with avoidCrowded=true — high-crowding routes penalised and ranked lower. */
    @Test
    public void sortRoutes_avoidCrowdedTrue() {
        RouteItem high = makeRoute("h", "35 min", 1, 0, RouteItem.CROWDING_HIGH);
        RouteItem low = makeRoute("l", "35 min", 1, 0, RouteItem.CROWDING_LOW);
        List<RouteItem> list = new ArrayList<>(Arrays.asList(high, low));
        RouteOptimizer.sortRoutes(list, RouteOptimizer.Strategy.FASTEST, true);
        Assert.assertEquals("l", list.get(0).getRouteId());
        Assert.assertEquals("h", list.get(1).getRouteId());
    }
}
