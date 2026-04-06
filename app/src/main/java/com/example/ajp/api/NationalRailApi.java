package com.example.ajp.api;

import android.os.Handler;
import android.os.Looper;
import com.example.ajp.ui.arrivals.Arrival;
import com.example.ajp.utils.ApiKeyManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

/** SOAP client for National Rail OpenLDBWS (live departure boards). */
public class NationalRailApi {

    private static final String SOAP_URL = "https://lite.realtime.nationalrail.co.uk/OpenLDBWS/ldb10.asmx";
    private static final MediaType SOAP_XML = MediaType.parse("text/xml; charset=utf-8");

    private static String buildSoapBody(String fromCrs, String toCrs) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.append("xmlns:typ=\"http://thalesgroup.com/RTTI/2013-11-28/Token/types\" ");
        xml.append("xmlns:ldb=\"http://thalesgroup.com/RTTI/2017-02-02/ldb/\">");
        xml.append("<SOAP-ENV:Header>");
        xml.append("<typ:AccessToken><typ:TokenValue>").append(ApiKeyManager.getRailToken()).append("</typ:TokenValue></typ:AccessToken>");
        xml.append("</SOAP-ENV:Header>");
        xml.append("<SOAP-ENV:Body>");
        xml.append("<ldb:GetDepartureBoardRequest>");
        xml.append("<ldb:numRows>5</ldb:numRows>");
        xml.append("<ldb:crs>").append(fromCrs.toUpperCase().trim()).append("</ldb:crs>");
        // filterCrs must be omitted when invalid; sending blank values causes SOAP faults.
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

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DepartureBoardCallback {
        void onDepartures(List<Arrival> arrivals);
        void onError(String message);
    }

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
                            android.util.Log.w("DEBUG_SWR", "No departures parsed from response");
                            callback.onDepartures(Collections.emptyList());
                        }
                    });
                }
            }
        });
    }

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

    private static int indexOfIgnoreCase(String haystack, String needle, int from) {
        if (haystack == null || needle == null) return -1;
        int nlen = needle.length();
        if (haystack.length() - from < nlen) return -1;
        for (int i = from; i <= haystack.length() - nlen; i++) {
            if (haystack.regionMatches(true, i, needle, 0, nlen)) return i;
        }
        return -1;
    }

    private static List<Arrival> parseDepartureBoard(String xml) {
        List<Arrival> list = new ArrayList<>();
        if (xml == null || xml.isEmpty()) return list;

        // Strip namespace prefixes so simple tag scanners work across provider variants.
        String cleanXml = xml.replaceAll("<\\w+:", "<").replaceAll("</\\w+:", "</");
        if (!cleanXml.equals(xml)) {
            android.util.Log.d("DEBUG_SWR", "Stripped namespaces from XML for parsing");
        }
        xml = cleanXml;

        int idx = 0;
        while (true) {
            int serviceStart = indexOfIgnoreCase(xml, "<service", idx);
            if (serviceStart < 0) break;
            int openEnd = xml.indexOf('>', serviceStart);
            if (openEnd < 0) break;

            int serviceEnd = indexOfIgnoreCase(xml, "</service>", openEnd);
            if (serviceEnd < 0) break;

            String block = xml.substring(openEnd + 1, serviceEnd);
            Arrival a = parseServiceBlock(block);
            if (a != null) list.add(a);

            idx = serviceEnd + 1;
        }

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

                // Some feeds use textual ETD values; convert them to a safe countdown.
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
        if (sec < 0 && !timeToUse.isEmpty()) sec = 0;

        return new Arrival(
                operator.isEmpty() ? "South Western Railway" : operator,
                dest.isEmpty() ? "—" : dest,
                platform,
                Math.max(0, sec),
                "national-rail");
    }

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
        // Treat past times as next-day departures to keep countdowns non-negative.
        if (diffMin < 0) diffMin += 24 * 60;
        return diffMin * 60;
    }
}


