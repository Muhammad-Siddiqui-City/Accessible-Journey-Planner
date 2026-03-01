package com.example.ajp.api;

import android.os.Handler;
import android.os.Looper;
import com.example.ajp.ui.arrivals.Arrival;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * National Rail OpenLDBWS SOAP client. Add in Commit 9 with CrsLookup.
 * PURPOSE: Live train departures when TfL StopPoint/Arrivals returns empty (e.g. Barnes, SWR).
 * WHY: OkHttp POST with SOAP XML; GetDepartureBoard on ldb10.asmx. Do not use GetDepBoardWithDetails
 *      (ldb11) for platform – it caused SWR times to disappear; reverted to GetDepartureBoard.
 * ISSUES: 500 if SOAPAction/namespace mismatch (use 2012-01-13 for GetDepartureBoard); "Invalid crs code"
 *      if filterCrs sent with empty or non-3-char – only add filterCrs/filterType when toCrs valid.
 */
public class NationalRailApi {

    /* --- BLOCK: SOAP endpoint and token ---
     * PURPOSE: NRE OpenLDBWS URL and token; token also in ApiKeyManager for validation.
     * WHY: ldb10.asmx for GetDepartureBoard; SOAPAction must match exactly.
     * ISSUES: Duplicate token here and in ApiKeyManager; consider using ApiKeyManager.getRailToken().
     */
    private static final String TOKEN = "5d4bfb7e-5ed4-4f6d-afc9-4d32de042094";
    private static final String SOAP_URL = "https://lite.realtime.nationalrail.co.uk/OpenLDBWS/ldb10.asmx";
    private static final MediaType SOAP_XML = MediaType.parse("text/xml; charset=utf-8");

    /* --- BLOCK: Build SOAP GetDepartureBoard request body ---
     * PURPOSE: XML body with numRows, crs (fromCrs), optional filterCrs/filterType, timeWindow.
     * WHY: ldb namespace 2017-02-02 in body; only add filterCrs when toCrs is non-null and 3-char to
     *      avoid "Invalid crs code supplied" from API.
     * ISSUES: Wrong namespace or SOAPAction caused 500; fixed by aligning with NRE docs.
     */
    private static String buildSoapBody(String fromCrs, String toCrs) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.append("xmlns:typ=\"http://thalesgroup.com/RTTI/2013-11-28/Token/types\" ");
        xml.append("xmlns:ldb=\"http://thalesgroup.com/RTTI/2017-02-02/ldb/\">");
        xml.append("<SOAP-ENV:Header>");
        xml.append("<typ:AccessToken><typ:TokenValue>").append(TOKEN).append("</typ:TokenValue></typ:AccessToken>");
        xml.append("</SOAP-ENV:Header>");
        xml.append("<SOAP-ENV:Body>");
        xml.append("<ldb:GetDepartureBoardRequest>");
        xml.append("<ldb:numRows>10</ldb:numRows>");
        xml.append("<ldb:crs>").append(fromCrs.toUpperCase().trim()).append("</ldb:crs>");
        if (toCrs != null && !toCrs.trim().isEmpty() && toCrs.trim().length() == 3) {
            xml.append("<ldb:filterCrs>").append(toCrs.trim().toUpperCase()).append("</ldb:filterCrs>");
            xml.append("<ldb:filterType>to</ldb:filterType>");
        }
        xml.append("<ldb:timeOffset>0</ldb:timeOffset>");
        xml.append("<ldb:timeWindow>120</ldb:timeWindow>");
        xml.append("</ldb:GetDepartureBoardRequest>");
        xml.append("</SOAP-ENV:Body></SOAP-ENV:Envelope>");
        return xml.toString();
    }

    /* --- BLOCK: OkHttp and main-thread callback ---
     * PURPOSE: Single client for SOAP calls; post results on main thread for UI.
     * WHY: Callback runs on mainHandler so StopsViewModel can post to LiveData safely.
     * ISSUES: None.
     */
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DepartureBoardCallback {
        void onDepartures(List<Arrival> arrivals);
        void onError(String message);
    }

    /* --- BLOCK: getDepartureBoard – request and enqueue ---
     * PURPOSE: POST SOAP request with SOAPAction header; parse response and callback on main thread.
     * WHY: SOAPAction "http://thalesgroup.com/RTTI/2012-01-13/ldb/GetDepartureBoard" required by NRE.
     * ISSUES: Content-Type text/xml; charset=utf-8. Check response.isSuccessful() and extractSoapFault first.
     */
    public void getDepartureBoard(String fromCrs, String toCrs, DepartureBoardCallback callback) {
        if (fromCrs == null || fromCrs.trim().length() != 3) {
            if (callback != null) mainHandler.post(() -> callback.onError("Invalid CRS: " + fromCrs));
            return;
        }
        String body = buildSoapBody(fromCrs.trim(), toCrs);

        Request request = new Request.Builder()
                .url(SOAP_URL)
                .post(RequestBody.create(body, SOAP_XML))
                .addHeader("Content-Type", "text/xml; charset=utf-8")
                .addHeader("SOAPAction", "\"http://thalesgroup.com/RTTI/2012-01-13/ldb/GetDepartureBoard\"")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Network error"));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                android.util.Log.d("DEBUG_SWR", "Raw XML Response length: " + responseBody.length());
                android.util.Log.d("DEBUG_SWR", "Raw XML Response: " + (responseBody.length() > 2000 ? responseBody.substring(0, 2000) + "...[truncated]" : responseBody));

                if (!response.isSuccessful()) {
                    android.util.Log.e("DEBUG_SWR", "Request Failed Code: " + response.code());
                    android.util.Log.e("DEBUG_SWR", "Error Body: " + responseBody);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    }
                    return;
                }
                String fault = extractSoapFault(responseBody);
                if (fault != null) {
                    android.util.Log.e("DEBUG_SWR", "SOAP Fault: " + fault);
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(fault));
                    }
                    return;
                }
                List<Arrival> arrivals = parseDepartureBoard(responseBody);
                android.util.Log.d("DEBUG_SWR", "Parsed arrivals count: " + (arrivals != null ? arrivals.size() : 0));
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (arrivals != null && !arrivals.isEmpty()) {
                            callback.onDepartures(arrivals);
                        } else {
                            android.util.Log.w("DEBUG_SWR", "No departures found in response");
                            callback.onError("No departures found");
                        }
                    });
                }
            }
        });
    }

    /* --- BLOCK: Extract SOAP fault message ---
     * PURPOSE: Parse faultstring from SOAP Fault for user-facing error.
     * WHY: API returns 200 with Fault in body for bad CRS etc.; need to detect and report.
     * ISSUES: Handles both <faultstring> and <soap:faultstring>.
     */
    private static String extractSoapFault(String xml) {
        if (xml == null || !xml.contains("Fault")) return null;
        int start = xml.indexOf("<faultstring>");
        if (start < 0) start = xml.indexOf("<soap:faultstring>");
        if (start < 0) return null;
        start = xml.indexOf(">", start) + 1;
        int end = xml.indexOf("</faultstring>", start);
        if (end < 0) end = xml.indexOf("</soap:faultstring>", start);
        return end > start ? xml.substring(start, end).trim() : null;
    }

    /* --- BLOCK: Parse GetDepartureBoardResponse to List<Arrival> ---
     * PURPOSE: Extract each service (std, etd, destination, platform, operator) into Arrival.
     * WHY: Strip XML namespaces (lt7:etd -> etd) so regex/indexOf work; fallback to <etd> regex if
     *      no <service> blocks found. computeSecondsToDeparture for timeToStationSeconds.
     * ISSUES: Response structure can vary; fallback handles alternate formats.
     */
    private static List<Arrival> parseDepartureBoard(String xml) {
        List<Arrival> list = new ArrayList<>();
        if (xml == null || xml.isEmpty()) return list;

        // Strip namespaces (lt7:etd -> etd, ns2:std -> std) so we can parse reliably
        String cleanXml = xml.replaceAll("<\\w+:", "<").replaceAll("</\\w+:", "</");
        if (!cleanXml.equals(xml)) {
            android.util.Log.d("DEBUG_SWR", "Stripped namespaces from XML for parsing");
        }
        xml = cleanXml;

        // Find each <service> block - structure varies; look for service tags
        int idx = 0;
        while (true) {
            int serviceStart = xml.indexOf("<service>", idx);
            if (serviceStart < 0) serviceStart = xml.indexOf("<ns2:service>", idx);
            if (serviceStart < 0) serviceStart = xml.indexOf("<ns1:service>", idx);
            if (serviceStart < 0) break;

            int serviceEnd = xml.indexOf("</service>", serviceStart);
            if (serviceEnd < 0) serviceEnd = xml.indexOf("</ns2:service>", serviceStart);
            if (serviceEnd < 0) serviceEnd = xml.indexOf("</ns1:service>", serviceStart);
            if (serviceEnd < 0) break;

            String block = xml.substring(serviceStart, serviceEnd);
            Arrival a = parseServiceBlock(block);
            if (a != null) list.add(a);

            idx = serviceEnd + 1;
        }

        // Fallback: use regex to find <etd>...</etd> (handles any namespace-stripped format)
        if (list.isEmpty()) {
            Pattern etdPattern = Pattern.compile("<etd>(.*?)</etd>", Pattern.DOTALL);
            Matcher etdMatcher = etdPattern.matcher(xml);
            while (etdMatcher.find()) {
                String etdVal = etdMatcher.group(1).trim();
                android.util.Log.d("DEBUG_SWR", "Found ETD: " + etdVal);

                int etdStart = etdMatcher.start();
                Pattern stdPattern = Pattern.compile("<std>(.*?)</std>", Pattern.DOTALL);
                String blockBefore = xml.substring(Math.max(0, etdStart - 1500), etdStart);
                Matcher stdMatcher = stdPattern.matcher(blockBefore);
                String std = "00:00";
                while (stdMatcher.find()) std = stdMatcher.group(1).trim();

                String dest = extractTag(xml, etdStart, "destination", "locationName");
                if (dest.isEmpty()) dest = extractTag(xml, etdStart, "destination", "crs");
                String platform = extractTag(xml, etdStart, "platform", "");
                String operator = extractTag(xml, etdStart, "operator", "");

                int sec = computeSecondsToDeparture("On time".equalsIgnoreCase(etdVal) ? std : etdVal);
                list.add(new Arrival(
                        operator.isEmpty() ? "National Rail" : operator,
                        dest.isEmpty() ? "—" : dest,
                        platform,
                        Math.max(0, sec),
                        "national-rail"));
            }
            if (list.isEmpty()) {
                android.util.Log.w("DEBUG_SWR", "No ETD tag found in response");
            }
        }

        return list;
    }

    /* --- BLOCK: Parse one <service> block to Arrival ---
     * PURPOSE: Read std, etd, destination/locationName, platform, operator; use "On time" → std.
     * WHY: Single place to build Arrival from service XML; mode "national-rail" for UI.
     * ISSUES: None.
     */
    private static Arrival parseServiceBlock(String block) {
        String std = extractTag(block, 0, "std", "");
        String etd = extractTag(block, 0, "etd", "");
        if (std.isEmpty() && etd.isEmpty()) return null;

        String dest = extractTag(block, 0, "destination", "locationName");
        if (dest.isEmpty()) dest = extractTag(block, 0, "destination", "crs");
        String platform = extractTag(block, 0, "platform", "");
        String operator = extractTag(block, 0, "operator", "");

        String timeToUse = ("On time".equalsIgnoreCase(etd) || etd.isEmpty()) ? std : etd;
        int sec = computeSecondsToDeparture(timeToUse);
        if (sec < 0 && !timeToUse.isEmpty()) sec = 0; // Past departure, show as due

        return new Arrival(
                operator.isEmpty() ? "South Western Railway" : operator,
                dest.isEmpty() ? "—" : dest,
                platform,
                Math.max(0, sec),
                "national-rail");
    }

    /* --- BLOCK: Extract tag value from XML (handles namespaces) ---
     * PURPOSE: Find <parent><child>value</child></parent> or <parent>value</parent>.
     * WHY: SOAP response uses ns1:/ns2: prefixes; search for both.
     * ISSUES: searchEnd limits scan to 1000 chars to avoid runaway.
     */
    private static String extractTag(String xml, int fromIndex, String parent, String child) {
        int start = xml.indexOf("<" + parent + ">", fromIndex);
        if (start < 0) start = xml.indexOf("<ns2:" + parent + ">", fromIndex);
        if (start < 0) start = xml.indexOf("<ns1:" + parent + ">", fromIndex);
        if (start < 0) start = xml.indexOf("<" + parent + " ", fromIndex);
        if (start < 0) return "";

        int searchEnd = Math.min(start + 1000, xml.length());
        String block = xml.substring(start, searchEnd);
        if (!child.isEmpty()) {
            int cStart = block.indexOf("<" + child + ">");
            if (cStart < 0) cStart = block.indexOf("<ns2:" + child + ">");
            if (cStart < 0) cStart = block.indexOf("<ns1:" + child + ">");
            if (cStart >= 0) {
                int contentStart = block.indexOf(">", cStart) + 1;
                int cEnd = block.indexOf("</" + child + ">", contentStart);
                if (cEnd < 0) cEnd = block.indexOf("</ns2:" + child + ">", contentStart);
                if (cEnd < 0) cEnd = block.indexOf("</ns1:" + child + ">", contentStart);
                if (cEnd > 0) return block.substring(contentStart, cEnd).trim();
            }
        } else {
            int contentStart = block.indexOf(">") + 1;
            int end = block.indexOf("</" + parent + ">", contentStart);
            if (end < 0) end = block.indexOf("</ns2:" + parent + ">", contentStart);
            if (end > 0) return block.substring(contentStart, end).trim();
        }
        return "";
    }

    /* --- BLOCK: "HH:mm" to seconds from now ---
     * PURPOSE: Convert scheduled/estimated time to timeToStationSeconds for Arrival.
     * WHY: If past midnight (diffMin < 0) add 24*60 for next day.
     * ISSUES: "On time"/"Delayed"/"Cancelled" treated as 0 (due).
     */
    private static int computeSecondsToDeparture(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1;
        timeStr = timeStr.trim();
        if ("On time".equalsIgnoreCase(timeStr) || "Delayed".equalsIgnoreCase(timeStr)
                || "Cancelled".equalsIgnoreCase(timeStr)) return 0;

        String[] parts = timeStr.split(":");
        if (parts.length < 2) return -1;
        int hour, min;
        try {
            hour = Integer.parseInt(parts[0].trim());
            min = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
        Calendar now = Calendar.getInstance();
        int nowMinOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int depMinOfDay = hour * 60 + min;
        int diffMin = depMinOfDay - nowMinOfDay;
        if (diffMin < 0) diffMin += 24 * 60; // Next day
        return diffMin * 60;
    }
}
