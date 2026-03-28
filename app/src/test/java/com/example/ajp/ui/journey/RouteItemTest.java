package com.example.ajp.ui.journey;

import com.example.ajp.api.Leg;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/**
 * Table 5.4 — RouteItemTest.
 */
public class RouteItemTest {

    private static final String[] NO_BADGES = new String[0];

    private static RouteItem make(String routeId, String duration, String transfers) {
        return new RouteItem(
                duration,
                "09:00",
                "10:00",
                RouteItem.CROWDING_LOW,
                transfers,
                NO_BADGES,
                "s",
                routeId,
                "A",
                "B",
                Collections.<Leg>emptyList(),
                false,
                null,
                null);
    }

    /** getDurationMinutesInt() — parses '45 min' to 45. */
    @Test
    public void getDurationMinutesInt() {
        RouteItem r = make("r1", "45 min", "0 changes");
        Assert.assertEquals(45, r.getDurationMinutesInt());
    }

    /** getTransfersCount() — parses '2 changes' to 2. */
    @Test
    public void getTransfersCount() {
        RouteItem r = make("r1", "45 min", "2 changes");
        Assert.assertEquals(2, r.getTransfersCount());
    }

    /** equals/hashCode by routeId — same routeId implies equal regardless of other fields. */
    @Test
    public void equals_hashCode_byRouteId() {
        RouteItem a = make("same-id", "30 min", "1 change");
        RouteItem b = make("same-id", "99 min", "9 changes");
        Assert.assertEquals(a, b);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        Assert.assertNotEquals(a, make("other-id", "30 min", "1 change"));
    }
}
