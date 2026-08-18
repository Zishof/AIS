package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Messagebox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.ui.util.MyMessageboxConfig;

public class NilaiValidator {

    // 1. Entry Point Utama (Paling Sederhana)
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa) {
        return (mahasiswa == null) ? true : checkBolehLihatNilai(mahasiswa, mahasiswa.currentSemester());
    }

    // 2. Entry Point dengan Semester
    // FIX (blank KHS): sebelumnya nilai config "mhs_yg_belum_bayar_tidak_bisa_lihat_nilai" --
    // yang seharusnya jadi SAKLAR UTAMA on/off untuk seluruh pengecekan pembayaran -- malah
    // dioper sebagai argumen tampilWarning (kendali munculnya messagebox saja). Akibatnya
    // pengecekan pembayaran di master method SELALU berjalan tanpa peduli config ini aktif atau
    // tidak (lihat bagian B di master method, kini digerbangi langsung oleh config tsb), sehingga
    // mahasiswa bisa terblokir melihat/mencetak nilai (mis. KHS tampak kosong tanpa pesan) walau
    // admin sudah menonaktifkan fitur ini. tampilWarning di sini selalu true karena entry point
    // ini dipakai jalur UI interaktif; pemanggil non-UI (mis. LaporanApi) sudah eksplisit memakai
    // overload 4-argumen dengan tampilWarning=false.
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester) {
        return checkBolehLihatNilai(mahasiswa, semester, true);
    }

    // 3. Entry Point dengan Warning Toggle
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester, boolean tampilWarning) {
        return checkBolehLihatNilai(mahasiswa, semester, tampilWarning, null);
    }

    // 4. MASTER METHOD (Satu-satunya tempat logika berada)
    @SuppressWarnings("unchecked")
    public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester, boolean tampilWarning, List<String> warnings) {
        if (mahasiswa == null) return true;

        // Jika sudah lulus, skip semua pengecekan
        if (mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester) {
            return true;
        }

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // --- A. Pengecekan Blokir ---
            List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
                    .add(Restrictions.isNotNull("keterangan"))
                    .add(Restrictions.ne("keterangan", ""))
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("nilai", true))
                    .setProjection(Projections.property("keterangan"))
                    .list();

            if (!alasans.isEmpty()) {
                String alas = Common.join(alasans, "\n\n");
                handleWarning(alas, "Informasi Lihat Nilai", tampilWarning, warnings);
                return false;
            }

            // --- B. Pengecekan Pembayaran (hanya bila fitur ini diaktifkan admin) ---
            // FIX: sebelumnya dipanggil TANPA syarat (lihat catatan di entry point 2-argumen di
            // atas) -- kini benar-benar digerbangi oleh config "mhs_yg_belum_bayar_tidak_bisa_lihat_nilai"
            // sesuai maksud aslinya (default AKTIF, lihat KonfigurasiNewAction).
            if (Common.bolehKonfigurasi("mhs_yg_belum_bayar_tidak_bisa_lihat_nilai")) {
                return validatePembayaran(session, mahasiswa, semester, tampilWarning, warnings);
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/NilaiValidator.java:70");
            // FIX (blank KHS): sebelumnya kegagalan (mis. query Hibernate bermasalah) balik false
            // TANPA pesan apa pun -- di sisi pemanggil (mis. LaporanKHS.onKHS) ini tampak sebagai
            // layar kosong tanpa penjelasan sama sekali. Beri tahu pengguna agar tidak bingung.
            handleWarning(
                    "Terjadi kendala saat memeriksa kelayakan melihat nilai mahasiswa ini. Laporan/nilai untuk "
                            + "sementara tidak dapat ditampilkan. Silakan coba lagi, atau hubungi admin bila berlanjut.",
                    "Peringatan", tampilWarning, warnings);
            return false;
        } finally {
            // Memastikan Session ditutup untuk mencegah memory leak
            if (session != null) {
                try { session.clear(); session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/NilaiValidator.java:75");}
            }
        }
    }

    // --- Helper Method untuk Pembayaran ---
    private static boolean validatePembayaran(Session session, Mahasiswa mahasiswa, int semester, boolean tampilWarning, List<String> warnings) {
        Integer tahap = mahasiswa.currentTahapan(semester);
        boolean termasukSmt1 = Common.bolehKonfigurasi("syarat_melihat_nilai_tidak_termasuk_smt_1");
        
        double harusLunasLalu = parseDoubleConfig("batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_melihat_nilai", 90.0);
        double harusLunasSaatIni = parseDoubleConfig("batas_terendah_persen_pembayaran_semester_saat_ini_boleh_melihat_nilai", 0.0);

        // Validasi Smt Lalu
        if (harusLunasLalu > 0.1 && !Common.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester, tahap, mahasiswa, harusLunasLalu, termasukSmt1)) {
            String msg = constructMsg(harusLunasLalu, (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && (tahap == null || tahap == 0)) ? "semester " + (semester - 1) : "tahap " + (tahap - 1));
            handleWarning(msg, "Peringatan", tampilWarning, warnings);
            return false;
        }

        // Validasi Smt Saat Ini
        if (harusLunasSaatIni > 0.1 && !Common.checkStatusPembayaranMahasiswaSebelumnyaUntukPenilaian(semester + 1, tahap, mahasiswa, harusLunasSaatIni, termasukSmt1)) {
            String msg = constructMsg(harusLunasSaatIni, (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && (tahap == null || tahap == 0)) ? "semester " + semester : "tahap " + tahap);
            handleWarning(msg, "Peringatan", tampilWarning, warnings);
            return false;
        }

        return true;
    }

    // --- Private Utilities untuk Kerapihan ---
    private static void handleWarning(String msg, String title, boolean tampilWarning, List<String> warnings) {
        if (tampilWarning) {
            try { MyMessageboxConfig.show(msg, title, MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/NilaiValidator.java:108");}
        }
        if (warnings != null) warnings.add(msg);
    }

    private static String constructMsg(double persen, String periode) {
        return "Untuk melihat nilai, Mahasiswa ini harus melunasi " + persen + "% biaya perkuliahan di " + periode + ". Harap hubungi bagian keuangan.";
    }

    private static double parseDoubleConfig(String key, double defaultValue) {
        try {
            return Double.parseDouble(Common.getKonfigurasi(key, String.valueOf(defaultValue)).getNilai().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}