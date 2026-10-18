package com.sqljudge.auth.service;

import java.util.List;
import java.util.Map;

public record DeviceLabel(String browser, String os) {

    private static final List<Map.Entry<String, String>> BROWSER_MARKERS =
            List.of(
                    Map.entry("Edg/", "Edge"),
                    Map.entry("OPR/", "Opera"),
                    Map.entry("CriOS/", "Chrome"),
                    Map.entry("FxiOS/", "Firefox"),
                    Map.entry("Firefox/", "Firefox"),
                    Map.entry("Chrome/", "Chrome"),
                    Map.entry("Safari/", "Safari"));

    private static final List<Map.Entry<String, String>> OS_MARKERS =
            List.of(
                    Map.entry("iPhone", "iPhone"),
                    Map.entry("iPad", "iPad"),
                    Map.entry("Android", "Android"),
                    Map.entry("CrOS", "ChromeOS"),
                    Map.entry("Windows", "Windows"),
                    Map.entry("Mac OS X", "macOS"),
                    Map.entry("Linux", "Linux"));

    public static DeviceLabel of(String userAgent) {
        if (userAgent == null) {
            return new DeviceLabel(null, null);
        }
        return new DeviceLabel(
                firstMatch(BROWSER_MARKERS, userAgent), firstMatch(OS_MARKERS, userAgent));
    }

    private static String firstMatch(List<Map.Entry<String, String>> markers, String userAgent) {
        return markers.stream()
                .filter(marker -> userAgent.contains(marker.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
