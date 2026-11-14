package com.simpletoob.app;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Strict parsing shared by the search box, paste dialog and Android share target. */
public final class VideoIds {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private VideoIds() {}
    public static boolean valid(String id) { return id != null && ID.matcher(id).matches(); }
    public static String parse(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (valid(s)) return s;
        if (s.length() > 4096) return null;
        // Extract the URL from pasted embed markup without ever rendering or executing it.
        java.util.regex.Matcher embedded=Pattern.compile("\\bsrc\\s*=\\s*['\"]([^'\"]+)['\"]",Pattern.CASE_INSENSITIVE).matcher(s);
        while(embedded.find()) {
            String id=parseUrl(embedded.group(1).replace("&amp;","&"));
            if(id!=null)return id;
        }
        // Shared text can contain a caption followed by a URL.
        for (String word : s.split("\\s+")) {
            String result = parseUrl(word);
            if (result != null) return result;
        }
        return null;
    }
    private static String parseUrl(String s) {
        try {
            if (!s.contains("://")) s = "https://" + s;
            URI uri = URI.create(s);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) return null;
            if (uri.getUserInfo() != null || uri.getHost() == null) return null;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null ? "" : uri.getPath();
            String id = null;
            if (host.equals("youtu.be")) id = path.startsWith("/") ? path.substring(1) : path;
            else if (host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com")
                    || host.equals("music.youtube.com") || host.equals("www.youtube-nocookie.com")) {
                if (path.equals("/watch") && uri.getRawQuery() != null) {
                    for (String pair : uri.getRawQuery().split("&")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2 && kv[0].equals("v")) id = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                    }
                } else {
                    String[] segments = path.split("/");
                    if (segments.length == 3 && (segments[1].equals("shorts") || segments[1].equals("embed")
                            || segments[1].equals("live"))) id = segments[2];
                }
            }
            return valid(id) ? id : null;
        } catch (Exception ignored) { return null; }
    }
}
