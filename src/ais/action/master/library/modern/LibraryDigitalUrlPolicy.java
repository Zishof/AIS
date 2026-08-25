package ais.action.master.library.modern;

/** Central allow-list for digital collection URLs exposed to the portal. */
public final class LibraryDigitalUrlPolicy {
    private LibraryDigitalUrlPolicy() { }

    public static String safe(String value) {
        if (value == null) return null;
        value = value.trim();
        if (value.length() == 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\\') >= 0) return null;
        if (value.startsWith("https://") || value.startsWith("http://")) return value;
        String lower = value.toLowerCase(java.util.Locale.ENGLISH);
        if (value.startsWith("/") && !value.startsWith("//") && !value.contains("/../") && !value.endsWith("/..")
                && !lower.contains("%2e") && !lower.contains("%5c")) return value;
        return null;
    }
}
