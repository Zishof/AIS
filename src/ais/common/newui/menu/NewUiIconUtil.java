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

    /**
     * Memilih icon berdasarkan makna label terlebih dahulu. Icon dari tabel
     * Menu tetap menjadi fallback agar konfigurasi khusus milik pelanggan
     * tidak hilang ketika label tidak termasuk kategori umum.
     */
    public static String classes(String raw, String label, boolean branch) {
        String semantic = semanticIcon(label);
        return semantic == null ? classes(raw, branch) : "fa-solid " + semantic;
    }

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

    private static String semanticIcon(String label) {
        if (label == null) return null;
        String value = label.trim().toLowerCase();
        if (value.length() == 0) return null;

        if (contains(value, "pengaturan pengguna", "manajemen pengguna", "hak akses", "role")) return "fa-users-gear";
        if (contains(value, "sistem informasi akademik", "akademik")) return "fa-graduation-cap";
        if (contains(value, "sistem sekolah", "sekolah")) return "fa-school";
        if (contains(value, "koperasi")) return "fa-handshake";
        if (contains(value, "perpustakaan", "pustaka", "buku")) return "fa-book-open";
        if (contains(value, "aset dan pengadaan", "pengadaan", "inventaris", "inventory")) return "fa-boxes-stacked";
        if (contains(value, "kepegawaian", "pegawai")) return "fa-id-card";
        if (contains(value, "tata kelola surat", "surat menyurat", "persuratan")) return "fa-envelopes-bulk";
        if (contains(value, "akuntansi")) return "fa-calculator";
        if (contains(value, "anggaran belanja", "realisasi anggaran")) return "fa-chart-pie";
        if (contains(value, "keuangan", "pembayaran", "tagihan")) return "fa-wallet";
        if (contains(value, "antar jemput", "transportasi", "kendaraan")) return "fa-bus-simple";
        if (contains(value, "rumah sakit", "kesehatan", "medis")) return "fa-hospital";
        if (contains(value, "penggajian", "payroll", "gaji")) return "fa-money-bill-wave";
        if (contains(value, "repository", "arsip")) return "fa-box-archive";
        if (contains(value, "akreditasi")) return "fa-award";
        if (contains(value, "pengajuan", "workflow", "alur kerja")) return "fa-diagram-project";
        if (contains(value, "dokter")) return "fa-user-doctor";
        if (contains(value, "pengawasan internal", "satuan pengawasan", "spi")) return "fa-user-shield";
        if (contains(value, "konfigurasi", "utility", "utilitas")) return "fa-gears";
        if (contains(value, "mahasiswa")) return "fa-user-graduate";
        if (contains(value, "dosen", "guru", "pengajar")) return "fa-chalkboard-user";
        if (contains(value, "kurikulum")) return "fa-book-atlas";
        if (contains(value, "mata kuliah", "matakuliah", "perkuliahan")) return "fa-book-open-reader";
        if (contains(value, "jadwal", "kalender")) return "fa-calendar-days";
        if (contains(value, "presensi", "kehadiran", "absen")) return "fa-calendar-check";
        if (contains(value, "nilai", "ujian", "penilaian")) return "fa-square-poll-vertical";
        if (contains(value, "penelitian", "pengabdian")) return "fa-flask";
        if (contains(value, "wisuda")) return "fa-graduation-cap";
        if (contains(value, "alumni")) return "fa-user-group";
        if (contains(value, "beasiswa")) return "fa-hand-holding-dollar";
        if (contains(value, "kkn")) return "fa-people-roof";
        if (contains(value, "pkl", "magang")) return "fa-briefcase";
        if (contains(value, "feeder", "pddikti")) return "fa-arrows-rotate";
        if (contains(value, "spmi", "penjaminan mutu")) return "fa-shield-halved";
        if (contains(value, "prestasi")) return "fa-trophy";
        if (contains(value, "laporan")) return "fa-chart-column";
        if (contains(value, "dokumen")) return "fa-file-lines";
        if (contains(value, "beranda")) return "fa-house";
        if (contains(value, "menu lainnya")) return "fa-ellipsis";
        return null;
    }

    private static boolean contains(String value, String... candidates) {
        for (int i = 0; i < candidates.length; i++) {
            if (value.indexOf(candidates[i]) >= 0) return true;
        }
        return false;
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
