package com.example.ajp.utils;

import android.content.Context;
import com.example.ajp.api.StopPoint;
import com.example.ajp.api.TflApi;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import retrofit2.Call;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Table 5.4 — LiftDisruptionCheckerTest: {@link LiftDisruptionChecker#hasLiftIssues(String, TflApi)}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LiftDisruptionCheckerTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        RouteMonitorPrefs.get(context).setSimulatedDisruptedStopIds(Collections.<String>emptySet());
    }

    /** hasLiftIssues — simulated: simulation pref set → true without API call. */
    @Test
    public void hasLiftIssues_simulated() {
        Set<String> ids = new HashSet<>();
        ids.add("SIM_STOP");
        RouteMonitorPrefs.get(context).setSimulatedDisruptedStopIds(ids);

        TflApi api = mock(TflApi.class);
        LiftDisruptionChecker checker = new LiftDisruptionChecker(context);

        Assert.assertTrue(checker.hasLiftIssues("SIM_STOP", api));

        verify(api, never()).getStopPointDisruptions(anyString());
        verify(api, never()).getStopPoint(anyString());
    }

    /** hasLiftIssues — cache hit: second call for same stopId uses cache (same result, no extra API). */
    @Test
    @SuppressWarnings("unchecked")
    public void hasLiftIssues_cacheHit() throws Exception {
        TflApi api = mock(TflApi.class);

        Call<List<Object>> disruptionCall = mock(Call.class);
        when(disruptionCall.execute()).thenReturn(Response.success(Collections.emptyList()));
        when(api.getStopPointDisruptions(eq("STOP1"))).thenReturn(disruptionCall);

        Call<StopPoint> stopCall = mock(Call.class);
        StopPoint stopBody = new Gson().fromJson(
                "{\"id\":\"STOP1\",\"commonName\":\"Test Station\"}", StopPoint.class);
        when(stopCall.execute()).thenReturn(Response.success(stopBody));
        when(api.getStopPoint(eq("STOP1"))).thenReturn(stopCall);

        LiftDisruptionChecker checker = new LiftDisruptionChecker(context);

        Assert.assertFalse(checker.hasLiftIssues("STOP1", api));
        Assert.assertFalse(checker.hasLiftIssues("STOP1", api));

        verify(api, times(1)).getStopPointDisruptions(eq("STOP1"));
        verify(api, times(1)).getStopPoint(eq("STOP1"));
    }
}
