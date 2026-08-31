package ais.action.master.generic.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Utilitas serialisasi JSON bersama untuk kerangka kerja CRUD generik ({@code generic/v2}).
 * Membungkus satu instance {@link Gson} yang dikonfigurasi seragam: format tanggal
 * {@code yyyy-MM-dd'T'HH:mm:ss} dan escaping HTML dimatikan (agar karakter seperti {@code <}/{@code >}
 * dalam data tidak diubah menjadi entity unicode saat diserialisasi ke JSON). Kelas ini murni
 * statis (tidak dapat diinstansiasi) agar seluruh bagian modul generik memakai pengaturan Gson
 * yang sama persis.
 */
public final class GenericCrudJson {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").disableHtmlEscaping().create();
    private GenericCrudJson() { }
    /** Menyerialisasi {@code value} menjadi string JSON memakai konfigurasi Gson bersama. */
    public static String toJson(Object value) { return GSON.toJson(value); }
    /** Mendeserialisasi string JSON {@code value} menjadi instance {@code type}. */
    public static Object fromJson(String value, Class type) { return GSON.fromJson(value, type); }
}
