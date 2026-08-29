package ais.action.servlet.landing;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.database.model.sekolah.Sekolah;

/** Konfigurasi presentasional website sekolah; identitas dan kontak selalu berasal dari model Sekolah. */
public final class SchoolWebsiteConfig {

    public static final int SCHEMA_VERSION = 1;
    public static final int MAKS_KARAKTER = 1000000;

    private SchoolWebsiteConfig() { }

    public static JSONObject load(Sekolah sekolah, String root) {
        JSONObject defaults = defaults(root == null ? "" : root);
        String raw = sekolah == null ? null : sekolah.getWebsite();
        if (raw == null || raw.trim().length() == 0 || !raw.trim().startsWith("{")) return defaults;
        try {
            JSONObject result = merge(defaults, new JSONObject(raw.trim()));
            contextualize(result, root == null ? "" : root);
            removeModelBackedProperties(result);
            return result;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SchoolWebsiteConfig.load");
            put(defaults, "configurationWarning", "Konfigurasi website tidak valid; konten bawaan digunakan.");
            return defaults;
        }
    }

    public static String editableJson(Sekolah sekolah) {
        JSONObject result = load(sekolah, "");
        try { return result.toString(2); } catch (Exception e) { return result.toString(); }
    }

    public static void validate(String raw) throws JSONException {
        if (raw == null || raw.trim().length() == 0) throw new JSONException("Konfigurasi website wajib diisi.");
        if (raw.length() > MAKS_KARAKTER) throw new JSONException("Konfigurasi website terlalu panjang.");
        JSONObject parsed = new JSONObject(raw);
        if (parsed.optInt("schemaVersion", SCHEMA_VERSION) != SCHEMA_VERSION) {
            throw new JSONException("schemaVersion belum didukung.");
        }
        removeModelBackedProperties(parsed);
    }

    /** Validasi sekaligus membuang salinan field model sebelum JSON disimpan. */
    public static String normalize(String raw) throws JSONException {
        validate(raw);
        JSONObject parsed = new JSONObject(raw.trim());
        removeModelBackedProperties(parsed);
        return parsed.toString(2);
    }

    public static JSONObject object(JSONObject parent, String key) {
        JSONObject value = parent == null ? null : parent.optJSONObject(key);
        return value == null ? new JSONObject() : value;
    }

    public static JSONArray array(JSONObject parent, String key) {
        JSONArray value = parent == null ? null : parent.optJSONArray(key);
        return value == null ? new JSONArray() : value;
    }

    public static String text(JSONObject parent, String key, String fallback) {
        String value = parent == null ? fallback : parent.optString(key, fallback == null ? "" : fallback);
        return value == null ? "" : value.trim();
    }

    public static boolean visible(JSONObject root, String key, boolean fallback) {
        JSONObject visibility = object(root, "visibility");
        return visibility.has(key) ? visibility.optBoolean(key, fallback) : fallback;
    }

    private static JSONObject defaults(String root) {
        return obj(
                "schemaVersion", Integer.valueOf(SCHEMA_VERSION),
                "metadata", obj("language", "id"),
                "theme", obj("primary", "#1d4ed8", "secondary", "#f59e0b", "dark", "#102a43",
                        "surface", "#f5f8fc", "pattern", Boolean.TRUE),
                "announcement", obj("enabled", Boolean.FALSE, "label", "Informasi", "text", "", "url", "#berita"),
                "navigation", arr(
                        obj("label", "Profil", "url", "#profil"),
                        obj("label", "Program", "url", "#program"),
                        obj("label", "Kehidupan Sekolah", "url", "#kehidupan"),
                        obj("label", "Fasilitas", "url", "#fasilitas"),
                        obj("label", "Berita", "url", "#berita"),
                        obj("label", "Kontak", "url", "#kontak")),
                "hero", obj("eyebrow", "Sekolah yang bertumbuh bersama setiap peserta didik",
                        "primaryAction", obj("label", "Pendaftaran Siswa", "url", root + "/psb"),
                        "secondaryAction", obj("label", "Jelajahi Sekolah", "url", "#profil"),
                        "loginAction", obj("label", "Masuk eSchool", "url", root + "/login")),
                "profile", obj("eyebrow", "Tentang sekolah", "title", "Pendidikan bermutu, aman, dan berpusat pada peserta didik"),
                "valuesSection", section("Nilai utama", "Budaya sekolah yang membentuk karakter", "Nilai diterapkan dalam pembelajaran dan kehidupan sekolah."),
                "values", arr(
                        obj("title", "Integritas", "body", "Jujur, bertanggung jawab, dan konsisten dalam tindakan."),
                        obj("title", "Keunggulan", "body", "Terus belajar dan meningkatkan mutu secara terukur."),
                        obj("title", "Kepedulian", "body", "Lingkungan aman, inklusif, dan saling menghargai."),
                        obj("title", "Kemandirian", "body", "Mendorong inisiatif, kreativitas, dan kesiapan masa depan.")),
                "programsSection", section("Program pendidikan", "Pengalaman belajar yang utuh", "Program dapat disesuaikan dengan kekhasan dan jenjang sekolah."),
                "programs", arr(
                        obj("title", "Pembelajaran akademik", "body", "Kurikulum, asesmen, pengayaan, dan pendampingan belajar yang terstruktur."),
                        obj("title", "Penguatan karakter", "body", "Pembiasaan positif, kepemimpinan, literasi, dan kepedulian sosial."),
                        obj("title", "Minat dan bakat", "body", "Ekstrakurikuler dan proyek untuk mengembangkan potensi peserta didik.")),
                "studentLifeSection", section("Kehidupan sekolah", "Ruang aman untuk belajar dan bertumbuh", "Kolaborasi sekolah, peserta didik, serta orang tua menjadi bagian dari proses pendidikan."),
                "studentLife", arr(
                        obj("title", "Kesejahteraan siswa", "body", "Pendampingan, kesehatan, konseling, dan perlindungan anak."),
                        obj("title", "Kemitraan orang tua", "body", "Informasi perkembangan dan kanal komunikasi resmi yang jelas."),
                        obj("title", "Kegiatan dan prestasi", "body", "Agenda, organisasi siswa, kompetisi, serta apresiasi prestasi.")),
                "facilitiesSection", section("Fasilitas", "Lingkungan belajar yang mendukung", "Foto fasilitas dapat dikelola dari konfigurasi website tanpa menyalin identitas sekolah."),
                "gallery", arr(
                        obj("image", "", "title", "Ruang belajar", "caption", "Ruang yang nyaman dan mendukung pembelajaran aktif."),
                        obj("image", "", "title", "Kegiatan siswa", "caption", "Pengembangan karakter, minat, dan bakat."),
                        obj("image", "", "title", "Lingkungan sekolah", "caption", "Lingkungan yang aman, bersih, dan ramah anak.")),
                "newsSection", section("Kabar sekolah", "Berita dan pengumuman terbaru", "Hanya informasi aktif, publik, dan masih dalam masa tayang yang ditampilkan."),
                "cta", obj("eyebrow", "Terhubung dengan sekolah", "primaryLabel", "Daftar Sekarang",
                        "primaryUrl", root + "/psb", "secondaryLabel", "Hubungi WhatsApp"),
                "footer", obj("right", "Didukung eSchool · CV. Zishof"),
                "visibility", obj("profile", Boolean.TRUE, "values", Boolean.TRUE, "programs", Boolean.TRUE,
                        "studentLife", Boolean.TRUE, "facilities", Boolean.TRUE, "news", Boolean.TRUE,
                        "contact", Boolean.TRUE));
    }

    private static JSONObject section(String eyebrow, String title, String body) {
        return obj("eyebrow", eyebrow, "title", title, "body", body);
    }

    private static void removeModelBackedProperties(JSONObject value) {
        remove(object(value, "identity"), "name", "motto", "description", "address", "phone", "email", "whatsapp");
        remove(object(value, "seo"), "title", "description", "canonical");
        remove(object(value, "hero"), "title", "lead");
        remove(object(value, "profile"), "body");
        remove(object(value, "cta"), "title", "body");
        remove(object(value, "contact"), "address", "phone", "whatsapp", "email", "website");
        remove(object(value, "footer"), "left");
        remove(value, "name", "motto", "description", "address", "phone", "whatsapp", "email", "website");
        if (object(value, "identity").length() == 0) value.remove("identity");
        if (object(value, "contact").length() == 0) value.remove("contact");
        if (object(value, "seo").length() == 0) value.remove("seo");
    }

    private static void remove(JSONObject value, String... keys) {
        if (value == null || keys == null) return;
        for (String key : keys) value.remove(key);
    }

    private static JSONObject merge(JSONObject base, JSONObject custom) throws JSONException {
        for (Iterator it = custom.keys(); it.hasNext();) {
            String key = String.valueOf(it.next());
            Object incoming = custom.get(key);
            if (incoming instanceof JSONObject && base.optJSONObject(key) != null) {
                merge(base.getJSONObject(key), (JSONObject) incoming);
            } else {
                base.put(key, incoming);
            }
        }
        return base;
    }

    private static void contextualize(Object value, String root) throws JSONException {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (Iterator it = object.keys(); it.hasNext();) {
                String key = String.valueOf(it.next());
                Object child = object.get(key);
                if (("url".equalsIgnoreCase(key) || "image".equalsIgnoreCase(key)
                        || "logo".equalsIgnoreCase(key) || "heroImage".equalsIgnoreCase(key)) && child instanceof String) {
                    object.put(key, PesantrenWebsiteConfig.contextualizeUrl(String.valueOf(child), root));
                } else contextualize(child, root);
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) contextualize(array.get(i), root);
        }
    }

    private static JSONObject obj(Object... values) {
        JSONObject result = new JSONObject();
        for (int i = 0; i + 1 < values.length; i += 2) put(result, String.valueOf(values[i]), values[i + 1]);
        return result;
    }

    private static JSONArray arr(Object... values) {
        JSONArray result = new JSONArray();
        for (Object value : values) result.put(value);
        return result;
    }

    private static void put(JSONObject target, String key, Object value) {
        try { target.put(key, value); } catch (JSONException e) { throw new IllegalStateException(e); }
    }
}
