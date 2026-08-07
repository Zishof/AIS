package ais.action.master.generic.v2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.UUID;

/** Konversi nilai request terbatas; tidak pernah menginstansiasi class dari input client. */
public final class GenericCrudValueConverter {
    private static final String[] DATE_PATTERNS = new String[] { "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd", "dd-MM-yyyy" };
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
            if (target == Byte.class || target == Byte.TYPE) { return Byte.valueOf(text); }
            if (target == Double.class || target == Double.TYPE) { return Double.valueOf(text); }
            if (target == Float.class || target == Float.TYPE) { return Float.valueOf(text); }
            if (target == BigDecimal.class) { return new BigDecimal(text); }
            if (target == BigInteger.class) { return new BigInteger(text); }
            if (target == Character.class || target == Character.TYPE) {
                if (text.length() != 1) throw new IllegalArgumentException("Karakter harus tepat satu huruf.");
                return Character.valueOf(text.charAt(0));
            }
            if (target == Boolean.class || target == Boolean.TYPE) {
                return Boolean.valueOf("true".equalsIgnoreCase(text) || "1".equals(text) || "ya".equalsIgnoreCase(text));
            }
            if (target == java.sql.Date.class) { return java.sql.Date.valueOf(normalizeDate(text)); }
            if (target == java.sql.Timestamp.class) {
                return new java.sql.Timestamp(parseDate(text).getTime());
            }
            if (target == java.sql.Time.class) { return java.sql.Time.valueOf(text.length() == 5 ? text + ":00" : text); }
            if (Date.class.isAssignableFrom(target)) { return parseDate(text); }
            if (target == UUID.class) { return UUID.fromString(text); }
            if (target.isEnum()) { return Enum.valueOf(target, text); }
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

    private static String normalizeDate(String text) throws ParseException {
        Date value = parseDate(text);
        return new SimpleDateFormat("yyyy-MM-dd").format(value);
    }
}
