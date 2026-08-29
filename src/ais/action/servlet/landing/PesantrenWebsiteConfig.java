package ais.action.servlet.landing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.database.model.sekolah.Yayasan;

/**
 * Konfigurasi konten landing page ePesantren yang disimpan pada kolom
 * {@code sekolah.yayasan.website}. Kolom tersebut sebelumnya berisi URL polos;
 * Nilai JSON hanya menyimpan presentasi/konten tambahan. Identitas dan kontak
 * selalu dibaca langsung dari model agar perubahan data induk segera tampil.
 */
public final class PesantrenWebsiteConfig {

    public static final int SCHEMA_VERSION = 1;
    public static final int MAKS_KARAKTER = 1000000;

    private PesantrenWebsiteConfig() {
    }

    public static JSONObject load(Yayasan yayasan, String root) {
        JSONObject defaults = defaults(root == null ? "" : root);
        String raw = yayasan == null ? null : yayasan.getWebsite();
        if (raw == null || raw.trim().length() == 0) {
            return defaults;
        }
        raw = raw.trim();
        if (!raw.startsWith("{")) {
            return defaults;
        }
        try {
            JSONObject merged = merge(defaults, new JSONObject(raw));
            contextualizeInternalUrls(merged, root);
            removeModelBackedProperties(merged);
            return merged;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "PesantrenWebsiteConfig.load");
            put(defaults, "configurationWarning", "JSON website tidak valid; konten bawaan digunakan.");
            return defaults;
        }
    }

    public static String editableJson(Yayasan yayasan) {
        try {
            return load(yayasan, "").toString(2);
        } catch (Exception e) {
            return load(yayasan, "").toString();
        }
    }

    public static void validate(String raw) throws JSONException {
        if (raw == null || raw.trim().length() == 0) {
            throw new JSONException("Konfigurasi website JSON wajib diisi.");
        }
        if (raw.length() > MAKS_KARAKTER) {
            throw new JSONException("Konfigurasi website melebihi " + MAKS_KARAKTER + " karakter.");
        }
        JSONObject parsed = new JSONObject(raw);
        int version = parsed.optInt("schemaVersion", SCHEMA_VERSION);
        if (version != SCHEMA_VERSION) {
            throw new JSONException("schemaVersion belum didukung: " + version);
        }
    }

    /** Validasi sekaligus membuang salinan field model sebelum JSON disimpan. */
    public static String normalize(String raw) throws JSONException {
        validate(raw);
        JSONObject parsed = new JSONObject(raw.trim());
        removeModelBackedProperties(parsed);
        return parsed.toString(2);
    }

    public static JSONObject object(JSONObject parent, String key) {
        if (parent == null) return new JSONObject();
        JSONObject value = parent.optJSONObject(key);
        return value == null ? new JSONObject() : value;
    }

    public static JSONArray array(JSONObject parent, String key) {
        if (parent == null) return new JSONArray();
        JSONArray value = parent.optJSONArray(key);
        return value == null ? new JSONArray() : value;
    }

    public static String text(JSONObject parent, String key, String fallback) {
        if (parent == null) return fallback == null ? "" : fallback;
        String value = parent.optString(key, fallback == null ? "" : fallback);
        return value == null ? "" : value.trim();
    }

    public static boolean visible(JSONObject root, String key, boolean fallback) {
        JSONObject visibility = object(root, "visibility");
        return visibility.has(key) ? visibility.optBoolean(key, fallback) : fallback;
    }

    public static String websiteUrl(Yayasan yayasan) {
        return yayasan == null || yayasan.getDomain() == null ? "" : yayasan.getDomain().trim();
    }

    /** Field model Yayasan tidak boleh memiliki salinan kedua di JSON website. */
    private static void removeModelBackedProperties(JSONObject value) {
        remove(object(value, "identity"), "name", "motto", "description");
        remove(object(value, "theme"), "primary", "logo", "heroImage");
        remove(object(value, "seo"), "title", "description");
        remove(object(value, "hero"), "title", "lead");
        remove(object(value, "profile"), "body");
        remove(object(value, "cta"), "title", "body");
        remove(object(value, "contact"), "address", "phone", "whatsapp", "email", "website");
        remove(object(value, "footer"), "left");
        remove(value, "name", "motto", "description", "address", "phone", "whatsapp", "email", "website");
        if (object(value, "contact").length() == 0) value.remove("contact");
    }

    private static void remove(JSONObject value, String... keys) {
        if (value == null || keys == null) return;
        for (String key : keys) value.remove(key);
    }

    /**
     * Menambahkan context path aktif pada URL internal lama seperti /login.
     * URL absolut, protocol-relative, anchor, mailto, dan tel tidak diubah.
     */
    public static String contextualizeUrl(String value, String root) {
        String url = value == null ? "" : value.trim();
        String context = root == null ? "" : root.trim();
        if (context.endsWith("/") && context.length() > 1) context = context.substring(0, context.length() - 1);
        if (context.length() == 0 || "/".equals(context) || url.length() == 0 || !url.startsWith("/")
                || url.startsWith("//") || url.equals(context) || url.startsWith(context + "/")) {
            return url;
        }
        return context + url;
    }

    private static JSONObject defaults(String root) {
        JSONObject rootObject = obj(
                "schemaVersion", Integer.valueOf(SCHEMA_VERSION),
                "identity", obj("shortName", "ePesantren"),
                "theme", obj("secondary", "#c79a3b", "ink", "#102a2a", "dark", "#073f3d",
                        "cream", "#fbfaf5", "pattern", Boolean.TRUE),
                "seo", obj("canonical", root + "/index", "language", "id"),
                "announcement", obj("enabled", Boolean.FALSE, "label", "Informasi", "text", "",
                        "url", "#berita"),
                "navigation", arr(
                        obj("label", "Profil", "url", "#profil"),
                        obj("label", "Unit", "url", "#unit"),
                        obj("label", "Alur Harian", "url", "#alur-harian"),
                        obj("label", "Workflow", "url", "#workflow"),
                        obj("label", "Layanan", "url", "#layanan"),
                        obj("label", "Berita", "url", "#berita")),
                "hero", obj(
                        "eyebrow", "Satu Pesantren · Satu Sistem · Satu Data",
                        "primaryAction", obj("label", "Masuk ePesantren", "url", root + "/login"),
                        "secondaryAction", obj("label", "Jelajahi Layanan", "url", "#layanan"),
                        "tertiaryAction", obj("label", "Pendaftaran Santri", "url", root + "/psb")),
                "profile", obj(
                        "eyebrow", "Profil Pondok",
                        "title", "Berakar pada adab, bertumbuh dengan teknologi"),
                "stats", arr(
                        obj("value", "Dinamis", "label", "Unit pendidikan"),
                        obj("value", "24/7", "label", "Layanan digital"),
                        obj("value", "1", "label", "Identitas terpadu"),
                        obj("value", "Local-first", "label", "Tetap tangguh")),
                "unitsSection", section("Jaringan pendidikan", "Satu yayasan, seluruh unit terhubung",
                        "Sekolah, madrasah, dan perguruan tinggi tampil dari struktur Yayasan AIS."),
                "pillarsSection", section("Ekosistem ePesantren", "Delapan pilar kehidupan pondok",
                        "Bukan aplikasi terpisah: seluruh layanan memakai identitas, role, dan audit yang sama."),
                "pillars", defaultPillars(),
                "dailySection", section("Operasional sehari-hari", "Sistem mengikuti ritme pondok dari pagi hingga malam",
                        "Setiap bagian memperlihatkan siapa bekerja, apa yang dicatat, dan tindak lanjutnya."),
                "dailyTimeline", arr(
                        obj("time", "04.00–06.30", "title", "Ibadah, mutabaah, dan kesiapan hari", "description", "Musyrif mencatat aktivitas inti; santri melihat agenda pertama."),
                        obj("time", "06.30–12.00", "title", "Kelas, presensi, dan pembelajaran", "description", "Jadwal, biometrik, materi, tugas, dan kehadiran terhubung."),
                        obj("time", "12.00–16.00", "title", "Kesehatan, administrasi, dan layanan", "description", "Poskestren, perizinan, pembayaran, dan pelayanan wali."),
                        obj("time", "16.00–21.30", "title", "Pengasuhan, unit usaha, dan evaluasi", "description", "Kegiatan sore, koperasi, pembinaan, dan ringkasan pimpinan.")),
                "workflowSection", section("Workflow operasional", "Alur kerja terlihat sebelum fitur dibuka",
                        "Setiap workflow dapat diganti, diurutkan, disembunyikan, atau ditambah melalui JSON Yayasan."),
                "workflows", defaultWorkflows(),
                "diagramSection", section("Diagram ekosistem", "Satu identitas menghubungkan seluruh layanan",
                        "Diagram membantu menjelaskan hubungan unit, pengguna, perangkat, dan server."),
                "diagrams", defaultDiagrams(),
                "biometric", obj(
                        "eyebrow", "Identitas biometrik local-first",
                        "title", "Satu verifikasi untuk layanan penting santri",
                        "body", "Sidik jari dan pengenalan wajah dapat digunakan untuk presensi mandiri, izin gerbang, dan verifikasi member POS dengan fallback serta audit.",
                        "image", "",
                        "action", obj("label", "Kelola melalui akun berwenang", "url", root + "/login"),
                        "steps", arr(
                                step("Enrolmen dengan persetujuan", "Petugas berwenang mendaftarkan biometrik, perangkat, tujuan penggunaan, dan masa berlaku."),
                                step("Verifikasi cepat di perangkat", "Cache terenkripsi terbatas mempercepat layanan tanpa menjadikan data mentah sebagai identitas."),
                                step("Sinkronisasi idempoten", "Peristiwa offline dikirim ulang aman tanpa menggandakan presensi atau transaksi."),
                                step("Audit, pencabutan, dan fallback", "Admin dapat mencabut akses; kartu, PIN, dan verifikasi petugas tetap tersedia."))),
                "servicesSection", section("Fasilitas digital", "Seluruh pintu layanan dalam satu halaman",
                        "Aktifkan layanan secara bertahap sesuai kesiapan dan kebijakan pondok."),
                "serviceGroups", defaultServices(root),
                "gallerySection", section("Suasana dan fasilitas", "Tampilkan kehidupan pondok secara nyata",
                        "Gunakan foto yang memperoleh izin dan tidak membuka data pribadi santri."),
                "gallery", arr(
                        obj("image", root + "/img/pesantren/pembelajaran-default-v1.png", "title", "Kegiatan belajar", "caption", "Kelas formal, diniyah, tahfiz, dan pembelajaran proyek."),
                        obj("image", root + "/img/pesantren/hero-default-v1.png", "title", "Pengasuhan", "caption", "Asrama yang tertib, aman, dan mendukung kemandirian."),
                        obj("image", root + "/img/pesantren/ekonomi-default-v1.png", "title", "Kemandirian ekonomi", "caption", "Koperasi, kantin, BMT, dan unit usaha pesantren.")),
                "newsSection", section("Kabar pondok", "Pengumuman umum terbaru",
                        "Berita aktif dan ditujukan untuk umum diambil otomatis dari Pengumuman Akademis."),
                "cta", obj("eyebrow", "Terhubung dengan pondok", "primaryLabel", "Masuk Sistem", "primaryUrl", root + "/login",
                        "secondaryLabel", "Hubungi WhatsApp"),
                "footer", obj("right", "Didukung ePesantren · CV. Zishof"),
                "visibility", obj("units", Boolean.TRUE, "pillars", Boolean.TRUE, "dailyTimeline", Boolean.TRUE,
                        "workflows", Boolean.TRUE, "diagrams", Boolean.TRUE, "biometric", Boolean.TRUE,
                        "services", Boolean.TRUE, "gallery", Boolean.TRUE, "news", Boolean.TRUE,
                        "contact", Boolean.TRUE));
        return rootObject;
    }

    private static JSONArray defaultPillars() {
        return arr(
                obj("icon", "01", "title", "Pendidikan multi-jenjang", "description", "eSchool, eCampus, diniyah, tahfiz, kursus, LMS, penilaian, dan pelaporan."),
                obj("icon", "02", "title", "Kesantrian & asrama", "description", "Kamar, pengasuhan, izin, kunjungan, prestasi, dan komunikasi wali."),
                obj("icon", "03", "title", "Kesehatan pesantren", "description", "Poskestren, rekam medis, farmasi, pemeriksaan, rujukan, dan notifikasi."),
                obj("icon", "04", "title", "BMT & keuangan", "description", "Tagihan, tabungan, simpanan, pembiayaan, ZIS, dan rekonsiliasi."),
                obj("icon", "05", "title", "Unit usaha & POS", "description", "Koperasi, kantin, gudang, stok, QRIS, dan laporan usaha."),
                obj("icon", "06", "title", "Back-office", "description", "SDM, payroll, pengadaan, aset, surat, SOP, akuntansi, dan audit."),
                obj("icon", "07", "title", "Pustaka & repository", "description", "Buku, kitab digital baca-saja ber-watermark, dan karya ilmiah."),
                obj("icon", "08", "title", "Kanal mandiri & analitik", "description", "Android, desktop, web, kiosk, dashboard, serta AI dengan tinjauan manusia."));
    }

    private static JSONArray defaultWorkflows() {
        return arr(
                workflow("akademik", "Akademik harian", "Dari jadwal sampai tindak lanjut wali",
                        arr(step("Jadwal aktif", "Kelas dan pengajar"), step("Buka pertemuan", "Materi dan agenda"),
                                step("Presensi", "Biometrik atau fallback"), step("Tugas & nilai", "Draft lokal dan umpan balik"),
                                step("Tindak lanjut", "Wali dan pimpinan"))),
                workflow("pengasuhan", "Pengasuhan santri", "Mencatat seperlunya, mendampingi sepenuhnya",
                        arr(step("Mutabaah", "Aktivitas harian"), step("Catat kejadian", "Prestasi atau kebutuhan"),
                                step("Pembinaan", "Tindakan yang manusiawi"), step("Persetujuan", "PIC dan batas waktu"),
                                step("Pantau hasil", "Perkembangan dan histori"))),
                workflow("izin", "Izin keluar / masuk", "Izin dan identitas diperiksa sebagai satu keputusan",
                        arr(step("Ajukan", "Tujuan, alasan, pendamping"), step("Review", "Musyrif atau pimpinan"),
                                step("Izin aktif", "Waktu dan syarat"), step("Scan gerbang", "Biometrik dan liveness"),
                                step("Kembali", "Tutup izin dan notifikasi"))),
                workflow("pos", "Pembelian anggota", "Cepat di kasir, terkendali di server",
                        arr(step("Pilih anggota", "Kartu, cari, atau biometrik"), step("Pindai produk", "Harga dan stok"),
                                step("Konfirmasi", "Total dan batas belanja"), step("Struk lokal", "Tetap bekerja saat offline"),
                                step("Sinkron", "Dedup, jurnal, dan audit"))),
                workflow("pustaka", "Pustaka terlindungi", "Baca daring tanpa membuka file sumber",
                        arr(step("Cari koleksi", "Judul dan penulis"), step("Cek publikasi", "Status dan role"),
                                step("Render halaman", "Server membuat citra"), step("Watermark", "Identitas pembaca"),
                                step("Baca & audit", "Tanpa unduh file"))));
    }

    private static JSONArray defaultDiagrams() {
        return arr(
                obj("type", "hub", "title", "Ekosistem satu identitas", "center", "AIS / ePesantren Core",
                        "items", arr("Santri & wali", "Ustadz & pegawai", "Sekolah & kampus", "BMT & POS", "Pimpinan")),
                obj("type", "layers", "title", "Arsitektur local-first", "center", "Server sebagai sumber kebenaran",
                        "items", arr("Aplikasi Desktop / Android", "Cache terenkripsi", "Outbox dan retry", "API idempoten", "Audit trail")),
                obj("type", "decision", "title", "Keputusan biometrik", "center", "Verifikasi kontekstual",
                        "items", arr("Izin aktif", "Role diperbolehkan", "Lokasi benar", "Liveness lolos", "Fallback tersedia")));
    }

    private static JSONArray defaultServices(String root) {
        return arr(
                obj("title", "Pendidikan & publikasi", "items", arr(
                        link("Login ePesantren", root + "/login"), link("Penerimaan santri", root + "/psb"),
                        link("Penerimaan mahasiswa", root + "/pmb"), link("Alumni", root + "/alumni"),
                        link("Pustaka digital", root + "/pustaka"), link("Repository ilmiah", root + "/repository"))),
                obj("title", "Kesantrian & layanan mandiri", "items", arr(
                        link("Anjungan santri", root + "/anjungan"), link("Presensi siswa", root + "/welsis"),
                        link("Buku tamu", root + "/tamu"), link("Antar jemput", root + "/antarJemput"),
                        link("Kursus", root + "/krrs"), link("Lowongan & karir", root + "/karir"))),
                obj("title", "Ekonomi & operasional", "items", arr(
                        link("eKantin & koperasi", root + "/kantin"), link("Point of Sale", root + "/pos"),
                        link("Portal rekanan", root + "/vendor"), link("eMedic", "https://apps.emedik.id"),
                        link("Proposal", root + "/proposal"), link("Dokumen kerja sama", root + "/pks"))));
    }

    private static JSONObject section(String eyebrow, String title, String body) {
        return obj("eyebrow", eyebrow, "title", title, "body", body);
    }

    private static JSONObject workflow(String id, String title, String subtitle, JSONArray steps) {
        return obj("id", id, "title", title, "subtitle", subtitle, "image", "", "steps", steps);
    }

    private static JSONObject step(String title, String description) {
        return obj("title", title, "description", description);
    }

    private static JSONObject link(String label, String url) {
        return obj("label", label, "url", url);
    }

    private static JSONObject merge(JSONObject base, JSONObject custom) throws JSONException {
        java.util.Iterator<?> keys = custom.keys();
        while (keys.hasNext()) {
            String key = String.valueOf(keys.next());
            Object value = custom.get(key);
            if (value instanceof JSONObject && base.optJSONObject(key) != null) {
                base.put(key, merge(base.getJSONObject(key), (JSONObject) value));
            } else {
                base.put(key, value);
            }
        }
        return base;
    }

    private static void contextualizeInternalUrls(Object node, String root) throws JSONException {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            java.util.Iterator<?> keys = object.keys();
            while (keys.hasNext()) {
                String key = String.valueOf(keys.next());
                Object value = object.get(key);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    contextualizeInternalUrls(value, root);
                } else if (value instanceof String && ((String) value).startsWith("/")) {
                    object.put(key, contextualizeUrl((String) value, root));
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray values = (JSONArray) node;
            for (int i = 0; i < values.length(); i++) {
                Object value = values.get(i);
                if (value instanceof JSONObject || value instanceof JSONArray) {
                    contextualizeInternalUrls(value, root);
                } else if (value instanceof String && ((String) value).startsWith("/")) {
                    values.put(i, contextualizeUrl((String) value, root));
                }
            }
        }
    }

    private static JSONObject obj(Object... pairs) {
        JSONObject result = new JSONObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            put(result, String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }

    private static JSONArray arr(Object... values) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < values.length; i++) result.put(values[i]);
        return result;
    }

    private static void put(JSONObject target, String key, Object value) {
        try {
            target.put(key, value == null ? JSONObject.NULL : value);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

}
