package com.example.ajp.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared utility class for CrsLookup.
 * Encapsulates reusable behavior that would otherwise be duplicated across features.
 * Centralizing this logic keeps edge-case handling consistent and easier to test.
 */

public class CrsLookup {

    private static final Map<String, String> tflToCrs = new HashMap<>();
    private static final Map<String, String> nameToCrs = new HashMap<>();

    private static final Map<String, String> hubToCrs = new HashMap<>();

    static {
        tflToCrs.put("910GWATERLOO", "WAT");
        tflToCrs.put("910GCLJ", "CLJ");

        tflToCrs.put("910GCLPHMJ1", "CLJ");
        tflToCrs.put("910GCLPHMJ2", "CLJ");
        tflToCrs.put("910GCLPHM", "CLJ");
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

        tflToCrs.put("910GLIVST", "LST");
        tflToCrs.put("910GPADTON", "PAD");
        tflToCrs.put("910GVICT", "VIC");
        tflToCrs.put("910GVICTR", "VIC");
        tflToCrs.put("910GVICTR2", "VIC");
        tflToCrs.put("910GLONBDE", "LBG");
        tflToCrs.put("910GEUSTON", "EUS");
        tflToCrs.put("910GSTRFD", "SRA");
        tflToCrs.put("910GSTRATFD", "SRA");
        tflToCrs.put("910GCANONST", "CST");
        tflToCrs.put("910GCANNON", "CST");
        tflToCrs.put("910GCHRX", "CHX");
        tflToCrs.put("910GSTPNS", "STP");
        tflToCrs.put("910GSTP", "STP");
        tflToCrs.put("910GSTPAN", "STP");
        tflToCrs.put("910GCRDON", "ECR");
        tflToCrs.put("910GECRO", "ECR");
        tflToCrs.put("910GBONDST", "BDS");
        tflToCrs.put("910GTOTCTRD", "TCR");
        tflToCrs.put("910GWHCHPL", "WHC");

        tflToCrs.put("910GFRNDXR", "ZFD");
        tflToCrs.put("910GFRNDNLT", "ZFD");
        tflToCrs.put("910GFARIND", "ZFD");
        tflToCrs.put("910GFARRING", "ZFD");

        tflToCrs.put("940GZZLUWLO", "WAT");
        tflToCrs.put("940GZZLUVIC", "VIC");
        tflToCrs.put("940GZZLULVT", "LST");
        tflToCrs.put("940GZZLULDS", "LST");
        tflToCrs.put("940GZZLUERB", "EUS");
        tflToCrs.put("940GZZLUBLG", "LBG");
        tflToCrs.put("940GZZLUKSX", "KGX");
        tflToCrs.put("940GZZLUPAC", "PAD");
        tflToCrs.put("940GZZLUPAE", "PAD");
        tflToCrs.put("940GZZLUCST", "CST");
        tflToCrs.put("940GZZLUCHX", "CHX");
        tflToCrs.put("940GZZLUSTR", "SRA");

        tflToCrs.put("940GZZLUBND", "BDS");
        tflToCrs.put("940GZZLUTCR", "TCR");
        tflToCrs.put("940GZZLUWHT", "WHC");
        tflToCrs.put("940GZZLUCRD", "ECR");
        tflToCrs.put("940GZZLUFCN", "ZFD");

        hubToCrs.put("HUBKGX", "KGX");
        hubToCrs.put("HUBLST", "LST");
        hubToCrs.put("HUBPAD", "PAD");
        hubToCrs.put("HUBVIC", "VIC");
        hubToCrs.put("HUBWAT", "WAT");
        hubToCrs.put("HUBLBG", "LBG");
        hubToCrs.put("HUBEUS", "EUS");
        hubToCrs.put("HUBCST", "CST");
        hubToCrs.put("HUBCHX", "CHX");
        hubToCrs.put("HUBSTP", "STP");
        hubToCrs.put("HUBSRA", "SRA");
        hubToCrs.put("HUBCLJ", "CLJ");
        hubToCrs.put("HUBECR", "ECR");
        hubToCrs.put("HUBBDS", "BDS");
        hubToCrs.put("HUBTCR", "TCR");
        hubToCrs.put("HUBWHC", "WHC");
        hubToCrs.put("HUBZFD", "ZFD");

        nameToCrs.put("barnes", "BNS");
        nameToCrs.put("barnes rail station", "BNS");
        nameToCrs.put("london waterloo", "WAT");
        nameToCrs.put("waterloo", "WAT");
        nameToCrs.put("clapham junction", "CLJ");
        nameToCrs.put("vauxhall", "VXH");
        nameToCrs.put("wimbledon", "WIM");
        nameToCrs.put("putney", "PUT");
        nameToCrs.put("richmond", "RMD");
        nameToCrs.put("stratford international", "SFA");
        nameToCrs.put("stratford international rail station", "SFA");
        nameToCrs.put("stratford", "SRA");
        nameToCrs.put("stratford rail station", "SRA");
        nameToCrs.put("stratford (london)", "SRA");
        nameToCrs.put("liverpool street", "LST");
        nameToCrs.put("london liverpool street", "LST");
        nameToCrs.put("paddington", "PAD");
        nameToCrs.put("london paddington", "PAD");
        nameToCrs.put("victoria", "VIC");
        nameToCrs.put("london victoria", "VIC");
        nameToCrs.put("london bridge", "LBG");
        nameToCrs.put("euston", "EUS");
        nameToCrs.put("london euston", "EUS");
        nameToCrs.put("cannon street", "CST");
        nameToCrs.put("london cannon street", "CST");
        nameToCrs.put("charing cross", "CHX");
        nameToCrs.put("london charing cross", "CHX");
        nameToCrs.put("st pancras international", "STP");
        nameToCrs.put("st pancras", "STP");
        nameToCrs.put("st. pancras", "STP");
        nameToCrs.put("pancras", "STP");
        nameToCrs.put("king's cross", "KGX");
        nameToCrs.put("kings cross", "KGX");
        nameToCrs.put("london king's cross", "KGX");
        nameToCrs.put("king's cross st pancras", "KGX");
        nameToCrs.put("kings cross st pancras", "KGX");
        nameToCrs.put("kings cross st. pancras", "KGX");
        nameToCrs.put("east croydon", "ECR");
        nameToCrs.put("bond street", "BDS");
        nameToCrs.put("bond street (elizabeth line)", "BDS");
        nameToCrs.put("tottenham court road (elizabeth line)", "TCR");
        nameToCrs.put("tottenham court road", "TCR");
        nameToCrs.put("tottenham court", "TCR");
        nameToCrs.put("whitechapel", "WHC");
        nameToCrs.put("farringdon", "ZFD");
        nameToCrs.put("london farringdon", "ZFD");
        nameToCrs.put("farringdon elizabeth line", "ZFD");
        nameToCrs.put("liverpool street (elizabeth line)", "LST");
        nameToCrs.put("london liverpool street rail station", "LST");
    }

    public static String getCrs(String tflId) {
        if (tflId == null || tflId.trim().isEmpty()) return null;
        String id = normalizeId(tflId.trim());
        String up = id.toUpperCase(Locale.UK);
        if (up.startsWith("HUB")) {
            String hub = hubToCrs.get(up);
            if (hub != null) return hub;
        }
        String crs = tflToCrs.get(id);
        if (crs != null) return crs;
        crs = tflToCrs.get(up);
        if (crs != null) return crs;
        return tflToCrs.get(id.toLowerCase(Locale.UK));
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
    public static String normalizeForCrsLookup(String commonName) {
        if (commonName == null) return "";
        String n = commonName.trim().toLowerCase(Locale.UK);
        n = n.replace('\u2019', '\'').replace('\u2018', '\'');
        n = n.replace(" underground station", "");
        n = n.replace(" - underground", "");
        n = n.replace(" (elizabeth line)", "");
        n = n.replace(" elizabeth line station", "");
        n = n.replace(" elizabeth line", "");
        n = n.replace(" dlr station", "");
        n = n.replace(" (all platforms)", "");
        n = n.replace(" (stop a)", "");
        n = n.replace(" (stop b)", "");
        return n.trim();
    }

    public static String getCrsFromName(String commonName) {
        if (commonName == null || commonName.isEmpty()) return null;
        String crs = getCrsFromNameInternal(normalizeForCrsLookup(commonName));
        if (crs != null) return crs;
        return getCrsFromNameInternal(commonName.trim().toLowerCase(Locale.UK));
    }

    private static String getCrsFromNameInternal(String key) {
        if (key == null || key.isEmpty()) return null;
        String crs = nameToCrs.get(key);
        if (crs != null) return crs;
        List<Map.Entry<String, String>> entries = new ArrayList<>(nameToCrs.entrySet());
        Collections.sort(entries, Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
        for (Map.Entry<String, String> e : entries) {
            if (key.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    // Handles a focused part of this feature flow and keeps related logic encapsulated.
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


