package ais.common.newui.menu;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Mengubah shortcut header legacy menjadi katalog beranda New UI.
 *
 * <p>Fail-closed: flag pada {@link Tbmrole} hanya mengaktifkan kandidat. Kartu
 * baru dikirim ke browser bila targetnya juga ada pada snapshot menu visible
 * (assignment job_has_menu, scope, status aktif, dan READ sudah tervalidasi).</p>
 */
public final class NewUiModuleShortcutService {

    private NewUiModuleShortcutService() { }

    public static List<NewUiModuleShortcut> build(Tbmrole role,
            NewUiHybridMenuSnapshot snapshot) {
        return build(null, role, snapshot);
    }

    public static List<NewUiModuleShortcut> build(Tbmuser user,
            NewUiHybridMenuSnapshot snapshot) {
        Tbmrole role = null;
        try { role = user == null ? null : user.hakAkses(); } catch (Exception ignored) { }
        return build(user, role, snapshot);
    }

    private static List<NewUiModuleShortcut> build(Tbmuser user, Tbmrole role,
            NewUiHybridMenuSnapshot snapshot) {
        List<NewUiModuleShortcut> result = new ArrayList<NewUiModuleShortcut>();
        if (role == null || snapshot == null) return result;

        List<Spec> specs = specifications(user, role);
        Set<Long> used = new HashSet<Long>();
        for (int i = 0; i < specs.size(); i++) {
            Spec spec = specs.get(i);
            if (!spec.enabled) continue;
            NewUiHybridMenuNode target = findBest(snapshot.getSearchableNodes(),
                    spec.aliases, used);
            if (target == null || target.getMenuId() == null) continue;
            used.add(target.getMenuId());
            result.add(new NewUiModuleShortcut(spec.key, spec.label,
                    spec.description, spec.icon, i + 1, target));
        }
        return result;
    }

    static NewUiHybridMenuNode findBest(List<NewUiHybridMenuNode> nodes,
            String[] aliases, Set<Long> used) {
        NewUiHybridMenuNode best = null;
        int bestScore = Integer.MIN_VALUE;
        if (nodes == null || aliases == null) return null;
        for (int i = 0; i < nodes.size(); i++) {
            NewUiHybridMenuNode node = nodes.get(i);
            if (node == null || !node.isVisible() || node.getMenuId() == null
                    || (used != null && used.contains(node.getMenuId()))) continue;
            String label = normalize(node.getLabel());
            String fullPath = normalize(node.getFullPath());
            int score = Integer.MIN_VALUE;
            for (int j = 0; j < aliases.length; j++) {
                String alias = normalize(aliases[j]);
                if (alias.length() == 0) continue;
                int candidate = Integer.MIN_VALUE;
                if (label.equals(alias)) candidate = 10000 - (j * 20);
                else if (label.startsWith(alias + " ") || label.endsWith(" " + alias)) candidate = 8200 - (j * 20);
                else if (label.indexOf(alias) >= 0) candidate = 7200 - (j * 20);
                else if (fullPath.indexOf(alias) >= 0) candidate = 4200 - (j * 20);
                if (candidate > score) score = candidate;
            }
            if (score == Integer.MIN_VALUE) continue;
            if (node.isBranch()) score += 900;
            score -= pathDepth(node.getFullPath()) * 15;
            if (score > bestScore) { best = node; bestScore = score; }
        }
        return best;
    }

    private static int pathDepth(String path) {
        if (path == null || path.length() == 0) return 0;
        int depth = 0;
        for (int i = 0; i < path.length(); i++) if (path.charAt(i) == '/') depth++;
        return depth;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String text = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase();
        return text.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static boolean yes(Boolean value) { return Boolean.TRUE.equals(value); }

    private static List<Spec> specifications(Tbmuser user, Tbmrole role) {
        List<Spec> list = new ArrayList<Spec>();
        list.add(new Spec("emedic", "eMedic", "Layanan kesehatan dan sistem informasi rumah sakit.", "fa-solid fa-heart-pulse", yes(role.getEmedic()), new String[] {"emedic", "sistem informasi rumah sakit", "rumah sakit", "dokter"}));
        list.add(new Spec("elearning", "e-Learning", "Kelas, materi, tugas, ujian, dan aktivitas pembelajaran.", "fa-solid fa-book-open-reader", yes(role.getElearning()), new String[] {"e learning", "elearning", "pembelajaran", "perkuliahan"}));
        list.add(new Spec("prestasi", "Prestasi", "Prestasi, apresiasi, dan kegiatan pengguna.", "fa-solid fa-trophy", yes(role.getKegiatanDanPrestasi()), new String[] {"prestasi", "apresiasi", "kegiatan dan prestasi"}));
        list.add(new Spec("pustaka", "Pustaka", "Koleksi dan layanan perpustakaan.", "fa-solid fa-book", yes(role.getPustaka()), new String[] {"pustaka", "perpustakaan"}));
        list.add(new Spec("workflow", "Pengajuan Anda", "Pengajuan, persetujuan, dan alur kerja.", "fa-solid fa-file-circle-check", yes(role.getWorkflow()), new String[] {"pengajuan anda", "pengajuan dan workflow", "workflow", "pengajuan"}));
        boolean extraShortcuts = user != null && extraShortcuts();
        list.add(new Spec("repository", "Repository", "Dokumen, arsip, dan repository institusi.", "fa-solid fa-folder-tree", extraShortcuts && yes(role.getDasborRepository()), new String[] {"repository", "dokumen pendukung", "dokumen", "arsip"}));
        list.add(new Spec("antar_jemput", "Antar Jemput", "Rute, kendaraan, jadwal, dan peserta antar jemput.", "fa-solid fa-bus-simple", extraShortcuts && yes(role.getDasboardAntarJemput()), new String[] {"antar jemput", "antarjemput", "transportasi"}));
        list.add(new Spec("spmi", "SPMI", "Penjaminan mutu internal dan dokumen mutu.", "fa-solid fa-shield-halved", yes(role.getTampilkanSpmi()), new String[] {"spmi", "sistem penjaminan mutu", "penjaminan mutu"}));
        list.add(new Spec("toko", "Toko", "Penjualan, kantin, dan transaksi toko.", "fa-solid fa-store", yes(role.getKantin()), new String[] {"toko", "kantin"}));
        list.add(new Spec("koperasi", "Koperasi", "Keanggotaan dan layanan sistem informasi koperasi.", "fa-solid fa-people-group", yes(role.getDashboardKoperasi()), new String[] {"koperasi", "sistem informasi koperasi"}));
        list.add(new Spec("akademik", "Akademik", "Data dan layanan sistem informasi akademik.", "fa-solid fa-graduation-cap", yes(role.getDashboard()), new String[] {"akademik", "sistem informasi akademik"}));
        list.add(new Spec("administrasi", "Administrasi", "Administrasi, persuratan, arsip, dan tata kelola surat.", "fa-solid fa-envelopes-bulk", yes(role.getAdministrasi()), new String[] {"administrasi", "surat menyurat", "tata kelola surat"}));
        list.add(new Spec("pengadaan", "Pengadaan", "Aset, kebutuhan, dan proses pengadaan.", "fa-solid fa-boxes-stacked", yes(role.getPengadaan()), new String[] {"pengadaan", "aset dan pengadaan"}));
        list.add(new Spec("pembayaran", "Pembayaran", "Tagihan dan transaksi pembayaran.", "fa-solid fa-money-check-dollar", yes(role.getPembayaran()), new String[] {"pembayaran", "tagihan"}));
        list.add(new Spec("keuangan", "Keuangan", "Anggaran, realisasi, dan layanan keuangan.", "fa-solid fa-wallet", yes(role.getKeuangan()), new String[] {"keuangan", "anggaran belanja dan realisasi"}));
        list.add(new Spec("akuntansi", "Akuntansi", "Jurnal, buku besar, dan laporan akuntansi.", "fa-solid fa-calculator", yes(role.getAkunting()) && !isStudent(user), new String[] {"akuntansi", "akunting"}));
        list.add(new Spec("kepegawaian", "Kepegawaian", "Data pegawai dan layanan sumber daya manusia.", "fa-solid fa-id-card", yes(role.getKepegawaian()), new String[] {"kepegawaian", "pegawai"}));
        list.add(new Spec("gaji", "Gaji", "Penggajian, payroll, dan komponen pendapatan.", "fa-solid fa-money-bill-wave", yes(role.getTampilkanGaji()), new String[] {"gaji", "penggajian", "payroll"}));
        list.add(new Spec("kinerja", "Kinerja", "Target, aktivitas, dan penilaian kinerja.", "fa-solid fa-chart-line", yes(role.getKinerja()), new String[] {"kinerja", "beban kinerja dosen"}));
        list.add(new Spec("presensi", "Presensi", "Kehadiran dan rekap presensi pengguna.", "fa-solid fa-calendar-check", yes(role.getPresensiKehadiran()), new String[] {"presensi", "kehadiran"}));
        list.add(new Spec("kalender_akademik", "Kalender Akademik", "Agenda dan kalender kegiatan akademik.", "fa-solid fa-calendar-days", yes(role.getKalenderAkademik()), new String[] {"kalender akademik", "kalender", "agenda akademik"}));
        list.add(new Spec("info_kegiatan", "Info Kegiatan", "Pengumuman dan informasi kegiatan institusi.", "fa-solid fa-circle-info", yes(role.getInfoKegiatan()), new String[] {"info kegiatan", "informasi kegiatan", "kegiatan"}));
        list.add(new Spec("feeder", "Neo Feeder", "Integrasi dan pelaporan data PDDIKTI Feeder.", "fa-solid fa-arrows-rotate", canFeeder(user, role), new String[] {"neo feeder", "feeder", "pddikti"}));
        list.add(new Spec("sister", "Sister", "Integrasi layanan SISTER.", "fa-solid fa-building-columns", canSister(user, role), new String[] {"sister"}));
        return list;
    }

    private static boolean isStudent(Tbmuser user) {
        try { return user != null && (user.getSiswa() != null || user.getMahasiswa() != null); }
        catch (Exception ignored) { return false; }
    }

    private static boolean extraShortcuts() {
        try { return Common.bolehKonfigurasi("tampilkan_shortcut_repository_antar_jemput_di_header", Konfigurasi.TIDAK_AKTIF); }
        catch (Exception ignored) { return false; }
    }

    private static boolean canFeeder(Tbmuser user, Tbmrole role) {
        if (user == null) return yes(role.getBolehAksesFeeder());
        try { return Common.getApakahAdminBolehAksesFeeder(); }
        catch (Exception ignored) { return yes(role.getBolehAksesFeeder()); }
    }

    private static boolean canSister(Tbmuser user, Tbmrole role) {
        if (user == null) return yes(role.getBolehAksesSister());
        try { return Common.getApakahAdminBolehAksesSister(); }
        catch (Exception ignored) { return yes(role.getBolehAksesSister()); }
    }

    /**
     * Tipe implementasi bersarang {@link Spec} milik {@link NewUiModuleShortcutService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiModuleShortcutService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String key}, {@code String label},
     * {@code String description}, {@code String icon}, {@code boolean enabled}, {@code String aliases}. Aturan
     * bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiModuleShortcutService
     */
    private static final class Spec {
        private final String key;
        private final String label;
        private final String description;
        private final String icon;
        private final boolean enabled;
        private final String[] aliases;

        private Spec(String key, String label, String description, String icon,
                boolean enabled, String[] aliases) {
            this.key = key; this.label = label; this.description = description;
            this.icon = icon; this.enabled = enabled; this.aliases = aliases;
        }
    }
}
