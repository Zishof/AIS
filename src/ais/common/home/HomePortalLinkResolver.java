package ais.common.home;

import java.net.URI;

import ais.common.Common;

/** Normalizes tenant-configured links and rejects executable URL schemes. */
public class HomePortalLinkResolver {
    public HomePortalViewModel.LinkItem resolve(String raw, String fallback) {
        HomePortalViewModel.LinkItem result = new HomePortalViewModel.LinkItem();
        String value = raw == null || raw.trim().length() == 0 ? fallback : raw.trim();
        if (!isSafe(value)) {
            value = fallback;
        }
        if (!isSafe(value)) {
            value = "#";
        }
        String root = Common.ROOT == null ? "" : Common.ROOT;
        boolean external = isExternal(value);
        if (!external && !value.startsWith("#") && !value.startsWith("mailto:") && !value.startsWith("tel:")) {
            if (root.length() > 0 && (value.equals(root) || value.startsWith(root + "/"))) {
                result.url = value;
            } else if (value.startsWith("/")) {
                result.url = root + value;
            } else {
                result.url = root + "/" + value;
            }
        } else {
            result.url = value;
        }
        result.target = external ? "_blank" : "_self";
        result.rel = external ? "noopener noreferrer" : "";
        return result;
    }

    private boolean isExternal(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private boolean isSafe(String value) {
        if (value == null || value.trim().length() == 0) return false;
        String lower = value.trim().toLowerCase();
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) return false;
        if (lower.startsWith("/") || lower.startsWith("#") || lower.startsWith("mailto:") || lower.startsWith("tel:")) return true;
        try {
            URI uri = new URI(value);
            return uri.getScheme() == null || "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        } catch (Exception e) {
            return false;
        }
    }
}
