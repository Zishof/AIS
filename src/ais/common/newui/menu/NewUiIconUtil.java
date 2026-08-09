package ais.common.newui.menu;

import java.util.HashMap;
import java.util.Map;

/** Normalisasi aman icon Menu lama (FA4/FA5/FA6) ke syntax Font Awesome 7. */
public final class NewUiIconUtil {

    private static final Map<String, String> ALIASES = new HashMap<String, String>();

    static {
        ALIASES.put("fa-calendar-alt", "fa-calendar-days");
        ALIASES.put("fa-exchange-alt", "fa-right-left");
        ALIASES.put("fa-shuttle-van", "fa-van-shuttle");
        ALIASES.put("fa-user-md", "fa-user-doctor");
        ALIASES.put("fa-bus-alt", "fa-bus-simple");
        ALIASES.put("fa-map-marked-alt", "fa-map-location-dot");
        ALIASES.put("fa-file-alt", "fa-file-lines");
        ALIASES.put("fa-search", "fa-magnifying-glass");
        ALIASES.put("fa-cog", "fa-gear");
        ALIASES.put("fa-cogs", "fa-gears");
        ALIASES.put("fa-home", "fa-house");
        ALIASES.put("fa-sign-out-alt", "fa-right-from-bracket");
        ALIASES.put("fa-sign-in-alt", "fa-right-to-bracket");
        ALIASES.put("fa-edit", "fa-pen-to-square");
        ALIASES.put("fa-trash-alt", "fa-trash-can");
        ALIASES.put("fa-times", "fa-xmark");
    }

    private NewUiIconUtil() { }

    public static String classes(String raw, boolean branch) {
        String fallback = branch ? "fa-folder-tree" : "fa-file-lines";
        if (raw == null || raw.trim().length() == 0) return "fa-solid " + fallback;

        String cleaned = raw.replace('<', ' ').replace('>', ' ').replace('"', ' ')
                .replace('\'', ' ').replace('=', ' ').replace('/', ' ');
        String[] tokens = cleaned.trim().split("\\s+");
        String style = null;
        String icon = null;
        StringBuilder modifiers = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = safeToken(tokens[i]);
            if (token.length() == 0) continue;
            String mappedStyle = style(token);
            if (mappedStyle != null) {
                style = mappedStyle;
            } else if (isModifier(token)) {
                modifiers.append(' ').append(token);
            } else if (token.startsWith("fa-") && token.length() > 3) {
                String alias = ALIASES.get(token);
                icon = alias == null ? token : alias;
            }
        }
        if (style == null) style = "fa-solid";
        if (icon == null) icon = fallback;
        return style + " " + icon + modifiers.toString();
    }

    private static String style(String token) {
        if ("fas".equals(token) || "fa".equals(token) || "fa-solid".equals(token)) return "fa-solid";
        if ("far".equals(token) || "fa-regular".equals(token)) return "fa-regular";
        if ("fab".equals(token) || "fa-brands".equals(token)) return "fa-brands";
        // Style Pro lama tidak tersedia pada paket Free; gunakan Solid agar icon tetap tampil.
        if ("fal".equals(token) || "fad".equals(token) || "fat".equals(token)
                || "fa-light".equals(token) || "fa-duotone".equals(token)
                || "fa-thin".equals(token)) return "fa-solid";
        return null;
    }

    private static boolean isModifier(String token) {
        return "fa-fw".equals(token) || "fa-spin".equals(token) || "fa-pulse".equals(token)
                || token.startsWith("fa-rotate-") || token.startsWith("fa-flip-");
    }

    private static String safeToken(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                result.append(c);
            }
        }
        return result.toString();
    }
}
