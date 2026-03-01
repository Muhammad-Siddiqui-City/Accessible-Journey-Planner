package com.example.ajp.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * TfL NaPTAN ID → National Rail CRS mapping. Add in Commit 9 with NationalRailApi.
 * PURPOSE: Map stop IDs (e.g. 910GBARNES) to 3-letter CRS (BNS) for GetDepartureBoard.
 * WHY: OpenLDBWS requires CRS; TfL uses NaPTAN; static map + name fallback for unknown IDs.
 * ISSUES: Handles URL format and case; add entries for more stations as needed.
 */
public class CrsLookup {

    private static final Map<String, String> tflToCrs = new HashMap<>();
    private static final Map<String, String> nameToCrs = new HashMap<>();

    static {
        tflToCrs.put("910GWATERLOO", "WAT");
        tflToCrs.put("910GCLJ", "CLJ");
        tflToCrs.put("910GVAUXHALL", "VXH");
        tflToCrs.put("910GWIMBLDN", "WIM");
        tflToCrs.put("910GBARNES", "BNS");
        tflToCrs.put("9100BARNES0", "BNS");
        tflToCrs.put("9100BARNES1", "BNS");
        tflToCrs.put("9100BARNES", "BNS");
        tflToCrs.put("910GPUTNEY", "PUT");
        tflToCrs.put("910GRCHMND", "RMD");
        tflToCrs.put("910GKINGX", "KGX");
        tflToCrs.put("910GKNGX", "KGX");
        tflToCrs.put("910GWIMBLEDON", "WIM");

        nameToCrs.put("barnes", "BNS");
        nameToCrs.put("barnes rail station", "BNS");
        nameToCrs.put("london waterloo", "WAT");
        nameToCrs.put("waterloo", "WAT");
        nameToCrs.put("clapham junction", "CLJ");
        nameToCrs.put("vauxhall", "VXH");
        nameToCrs.put("wimbledon", "WIM");
        nameToCrs.put("putney", "PUT");
        nameToCrs.put("richmond", "RMD");
    }

    /** Returns CRS code for TfL NaPTAN id, or null if not found. Handles URL format and case. */
    public static String getCrs(String tflId) {
        if (tflId == null || tflId.trim().isEmpty()) return null;
        String id = normalizeId(tflId.trim());
        String crs = tflToCrs.get(id);
        if (crs != null) return crs;
        crs = tflToCrs.get(id.toUpperCase());
        if (crs != null) return crs;
        return tflToCrs.get(id.toLowerCase());
    }

    /** CRS from station common name (e.g. "Barnes Rail Station" -> BNS). */
    public static String getCrsFromName(String commonName) {
        if (commonName == null || commonName.isEmpty()) return null;
        String key = commonName.trim().toLowerCase();
        String crs = nameToCrs.get(key);
        if (crs != null) return crs;
        for (Map.Entry<String, String> e : nameToCrs.entrySet()) {
            if (key.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    /** Extract naptan id from URL or return as-is. */
    private static String normalizeId(String id) {
        if (id.contains("/")) {
            int last = id.lastIndexOf('/');
            if (last >= 0 && last < id.length() - 1) {
                return id.substring(last + 1).trim();
            }
        }
        return id;
    }
}
