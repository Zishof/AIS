package ais.action.master.generic.v2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Konversi nilai request terbatas; tidak pernah menginstansiasi class dari input client. */
public final class GenericCrudValueConverter {
    private static final String[] DATE_PATTERNS = new String[] { "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd", "dd-MM-yyyy" };
    private GenericCrudValueConverter() { }

    public static Object convert(Object value, Class target) throws GenericCrudException {
        if (value == null) { return null; }
        if (target.isInstance(value)) { return value; }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) { return target == String.class ? "" : null; }
        try {
            if (target == String.class) { return text; }
            if (target == Long.class || target == Long.TYPE) { return Long.valueOf(text); }
            if (target == Integer.class || target == Integer.TYPE) { return Integer.valueOf(text); }
            if (target == Short.class || target == Short.TYPE) { return Short.valueOf(text); }
            if (target == Double.class || target == Double.TYPE) { return Double.valueOf(text); }
            if (target == Float.class || target == Float.TYPE) { return Float.valueOf(text); }
            if (target == Boolean.class || target == Boolean.TYPE) {
                return Boolean.valueOf("true".equalsIgnoreCase(text) || "1".equals(text) || "ya".equalsIgnoreCase(text));
            }
            if (Date.class.isAssignableFrom(target)) { return parseDate(text); }
        } catch (Exception e) {
            throw new GenericCrudException(400, "INVALID_VALUE", "Format nilai tidak valid.", e);
        }
        throw new GenericCrudException(400, "UNSUPPORTED_TYPE", "Tipe field belum didukung oleh Generic CRUD.");
    }

    private static Date parseDate(String text) throws ParseException {
        ParseException failure = null;
        for (int i = 0; i < DATE_PATTERNS.length; i++) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERNS[i]);
                format.setLenient(false);
                return format.parse(text);
            } catch (ParseException e) { failure = e; }
        }
        throw failure;
    }
}
