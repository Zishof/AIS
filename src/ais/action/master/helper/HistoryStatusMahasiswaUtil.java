package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.common.CommonHelperClass;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyMessageboxConfig;

/**
 * Utilitas statis (tanpa state instance) yang menghitung, memvalidasi, dan menyimpan status
 * kemahasiswaan per semester (entity {@link HistoryStatusMahasiswa}, tabel
 * {@code history_status_mahasiswa}) untuk seorang {@link Mahasiswa}. Status yang dikelola berasal
 * dari {@link ConstantValues}: {@code AKTIF}, {@code CUTI}, {@code TIDAK_AKTIF} (Nonaktif),
 * {@code LULUS}, {@code DROP_OUT}, {@code KELUAR}. Kelas ini adalah "otak" di balik pertanyaan
 * "status mahasiswa X di semester Y itu apa?" dan dipanggil dari sangat banyak tempat: layar
 * Daftar Ulang, KRS, Proses Tagihan ({@code KegiatanProsesHeper}), laporan EPSBED/PDDIKTI, dsb.
 *
 * <p><b>Model data.</b> Satu baris {@code HistoryStatusMahasiswa} merepresentasikan status
 * mahasiswa pada satu {@code semester} (atau {@code tahap}, bila
 * {@link ConstantValues#aktifkanTahapan} aktif) dalam satu {@code KrsMahasiswa}. Status Semester
 * Pendek (SP) disimpan TERPISAH dari status reguler lewat kolom {@code sp} — mahasiswa boleh
 * Nonaktif di semester reguler namun tetap Aktif di SP-nya (lihat
 * {@link #fetchHistoryFromDb} dan {@link #singkronisasiStatusMahasiswa}).</p>
 *
 * <p><b>Caching berlapis.</b> Karena dipanggil sangat sering (tiap render KRS/laporan), status
 * di-cache dua lapis: (1) cache cepat in-memory lewat
 * {@link GeneralValueObject#ambilDataLangsung}/{@code masukkanDataLangsung} (key
 * {@code "HistoryStatusMahasiswa_<idMahasiswa>_<tahap-atau-semester>"}), dan (2) cache JSON
 * per-mahasiswa lewat {@code Common.getJSONTemporary}/{@code setJSONTemporary} (bertahan lebih
 * lama, mis. antar request). {@link #getHistoryStatusMahasiswa(KrsMahasiswa, boolean)} dengan
 * parameter {@code true} (atau semester yang termasuk
 * {@link Mahasiswa#getPaksaAktifSemester()}) SENGAJA melewati kedua cache ini agar aturan status
 * terbaru (termasuk aturan "paksa aktif" dan "syarat aktif belum bayar") selalu dievaluasi ulang
 * dari database, bukan membaca hasil lama yang sudah usang.</p>
 *
 * <p><b>Mesin aturan status ({@link #kalkulasiStatusLogikaLanjutan}).</b> Setiap kali status
 * dihitung ulang dari DB, beberapa aturan bisnis diterapkan berurutan (lihat komentar inline di
 * method itu untuk riwayat permintaan user per tanggal): (a) keterlambatan bayar konfigurasi
 * {@code mhs_all_lambat_bayar_langsung_tidak_aktif} men-nonaktifkan mahasiswa hanya di tahun
 * akademik berjalan; (b) kegiatan bersyarat-aktif (checkbox {@code JenisKegiatan}) yang belum
 * dibayar sama sekali WAJIB men-nonaktifkan (searah Aktif→Nonaktif, via
 * {@link #cekPembayaranMahasiswa}), dan begitu lunas WAJIB langsung mengaktifkan kembali (searah
 * Nonaktif→Aktif, via {@link #adaKegiatanSyaratAktifLunasSemua} — sengaja pakai helper berbeda
 * karena semantik "tak ada bukti bayar" harus dibaca berbeda tergantung arah transisi); (c) status
 * Lulus/Keluar/Drop Out ditentukan retroaktif dari {@link Mahasiswa#getStatusKeluar()} begitu
 * semester mencapai {@link #getJumlahSemester}; (d) field admin
 * {@link Mahasiswa#getPaksaAktifSemester()} (daftar semester dipisah koma/titik-koma/spasi/pipe,
 * lihat {@link #semesterAdaDalamDaftar}) memaksa status AKTIF kecuali status sudah terminal
 * (Lulus/DO/Keluar).</p>
 *
 * <p><b>Bukan bagian tanggung jawab kelas ini:</b> UI form KRS/Daftar Ulang itu sendiri (ada di
 * Action terkait), maupun query pembayaran/tagihan detail (didelegasikan ke
 * {@link Mahasiswa#ambilKegiatans} dan {@link Kegiatan#hitungPersentaseLunasAktual()}). Pemanggil
 * baru sebaiknya memakai {@link #currentStatus} atau {@link #getHistoryStatusMahasiswa}, bukan
 * menyalin query Hibernate Criteria di atas ke Action lain — supaya cache dan aturan status tetap
 * konsisten satu sumber kebenaran.</p>
 */
public class HistoryStatusMahasiswaUtil {

    // ========================================================================
    // PUBLIC METHODS (OVERLOADED CURRENT STATUS)
    // ========================================================================

    /**
     * Status Semester Pendek (SP) mahasiswa pada semester/tahap berjalannya sendiri
     * ({@link Mahasiswa#currentSemester()}/{@link Mahasiswa#currentTahapan()}). Mengambil
     * {@code KrsMahasiswa} yang sudah ada TANPA memicu sinkronisasi (tidak membuat baris KRS
     * baru bila belum ada), lalu mendelegasikan ke {@link #currentStatus(KrsMahasiswa)}.
     *
     * @param mahasiswa mahasiswa yang dicek
     * @param sp        flag/kode Semester Pendek yang dicari (lihat {@code Perkuliahan.SEMESTER_PENDEK})
     * @return status SP saat ini, atau status default (lihat {@link #createDefaultStatus}) bila belum ada baris
     */
    public static HistoryStatusMahasiswa currentStatusSp(Mahasiswa mahasiswa, Integer sp) {
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(),
                mahasiswa.currentTahapan(), sp));
    }

    /**
     * Status reguler mahasiswa pada semester berjalannya sendiri, tapi dengan {@code tahap}
     * eksplisit (dipakai bila {@link ConstantValues#aktifkanTahapan} aktif — mis. jenjang dengan
     * penomoran tahap berbeda dari semester murni). Tanpa sinkronisasi KRS.
     *
     * @param mahasiswa mahasiswa yang dicek
     * @param tahap     tahap yang dicari (bisa berbeda dari {@link Mahasiswa#currentTahapan()})
     * @return status saat ini, atau status default bila belum ada baris KRS/history
     */
    public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, Integer tahap) {
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(), tahap, null));
    }

    /**
     * Status Semester Pendek mahasiswa pada {@code semester} eksplisit (bukan semester berjalan
     * mahasiswa). Parameter {@code tahunAkademik} TIDAK dipakai untuk query (diabaikan; tahap
     * diambil dari {@link Mahasiswa#currentTahapan()}) — dipertahankan hanya untuk kompatibilitas
     * signature overload historis pemanggil. Tanpa sinkronisasi KRS.
     *
     * @param mahasiswa     mahasiswa yang dicek
     * @param tahunAkademik tidak dipakai dalam query, lihat catatan di atas
     * @param semester      nomor semester eksplisit yang dicari
     * @param sp            flag/kode Semester Pendek
     * @return status SP pada semester tsb, atau status default bila belum ada baris
     */
    public static HistoryStatusMahasiswa currentStatusSp(Mahasiswa mahasiswa, String tahunAkademik, Integer semester, Integer sp) {
        Integer tahap = mahasiswa == null ? null : mahasiswa.currentTahapan();
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahap, sp));
    }

    /**
     * Status reguler mahasiswa pada {@code semester} eksplisit (bukan semester berjalan
     * mahasiswa). Sama seperti {@link #currentStatusSp(Mahasiswa, String, Integer, Integer)},
     * parameter {@code tahunAkademik} diabaikan dalam query. Tanpa sinkronisasi KRS (tidak
     * memaksa evaluasi ulang aturan status terbaru — lihat varian {@code refresh} di bawah untuk
     * itu).
     *
     * @param mahasiswa     mahasiswa yang dicek
     * @param tahunAkademik tidak dipakai dalam query, lihat catatan di atas
     * @param semester      nomor semester eksplisit yang dicari
     * @return status pada semester tsb, atau status default bila belum ada baris
     */
    public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, String tahunAkademik, Integer semester) {
        Integer tahap = mahasiswa == null ? null : mahasiswa.currentTahapan();
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahap, null));
    }

    /**
     * Varian dgn {@code refresh}: bila {@code true}, memaksa lewati cache in-memory/JSON
     * ({@link #getHistoryStatusMahasiswa(KrsMahasiswa, boolean)}) supaya aturan status baru
     * (mis. Non-Aktif kegiatan bersyarat-aktif yg belum dibayar sama sekali di
     * {@link #kalkulasiStatusLogikaLanjutan}) benar-benar dievaluasi ulang, bukan sekadar
     * membaca status lama yg sudah ter-cache. Dipakai "Proses Tagihan" (KegiatanProsesHeper)
     * agar hasil batch benar-benar mengubah status, bukan cuma kegiatan/tagihannya saja.
     */
    public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, String tahunAkademik, Integer semester, boolean refresh) {
        Integer tahap = mahasiswa == null ? null : mahasiswa.currentTahapan();
        KrsMahasiswa krsMahasiswa = refresh
                ? Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahap, null, true)
                : Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahap, null);
        return currentStatus(krsMahasiswa, refresh);
    }

    /**
     * Status reguler mahasiswa pada semester dan tahap berjalannya sendiri. Titik masuk paling
     * umum dipakai bila pemanggil hanya punya objek {@link Mahasiswa} (bukan {@code KrsMahasiswa}
     * yang sudah di-resolve).
     *
     * @param mahasiswa mahasiswa yang dicek
     * @return status saat ini, atau status default bila belum ada baris
     */
    public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa) {
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(), mahasiswa.currentTahapan(), null));
    }

    /**
     * Sinonim {@code currentStatus(krsMahasiswa, false)} — tidak memaksa refresh, boleh
     * mengembalikan hasil dari cache RAM/JSON bila masih ada.
     *
     * @param krsMahasiswa baris KRS mahasiswa (menentukan mahasiswa+semester/tahap+SP yang dicari)
     * @return status untuk KRS tsb, atau status default bila belum ada baris
     */
    public static HistoryStatusMahasiswa currentStatus(KrsMahasiswa krsMahasiswa) {
        return currentStatus(krsMahasiswa, false);
    }

    /**
     * Titik masuk inti "status saat ini": bila {@code krsMahasiswa} tidak merujuk mahasiswa
     * tersimpan (mahasiswa {@code null} atau belum punya id — mis. objek sementara di form
     * pendaftaran), TIDAK menyentuh database sama sekali dan langsung mengembalikan status
     * default transient AKTIF ({@link #createDefaultStatus}). Selain itu mendelegasikan ke
     * {@link #getHistoryStatusMahasiswa(KrsMahasiswa, boolean)}; bila hasilnya {@code null}
     * (kasus jarang, mis. exception tak tertangani di sana), tetap jatuh kembali ke status
     * default alih-alih mengembalikan {@code null} ke pemanggil.
     *
     * @param krsMahasiswa baris KRS mahasiswa yang jadi acuan
     * @param refresh      {@code true} untuk memaksa evaluasi ulang aturan status dari DB (lewati cache)
     * @return status (tersimpan atau default), tidak pernah {@code null}
     */
    public static HistoryStatusMahasiswa currentStatus(KrsMahasiswa krsMahasiswa, boolean refresh) {
        Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
        Integer semester = krsMahasiswa.getSemester();
        Integer tahap = krsMahasiswa.getTahapan();

        if (mahasiswa == null || mahasiswa.getId() == null) {
            return createDefaultStatus(krsMahasiswa, mahasiswa, semester, tahap);
        }

        HistoryStatusMahasiswa history = getHistoryStatusMahasiswa(krsMahasiswa, refresh);
        return history != null ? history : createDefaultStatus(krsMahasiswa, mahasiswa, semester, tahap);
    }

    /**
     * Sinonim {@code getHistoryStatusMahasiswa(krsMahasiswa, false)}: boleh membaca dari cache
     * cepat bila tersedia dan semester ini tidak termasuk daftar "paksa aktif".
     *
     * @param krsMahasiswa baris KRS mahasiswa yang jadi acuan
     * @return baris {@link HistoryStatusMahasiswa} (baru dibuat & disimpan bila belum ada), atau {@code null} bila terjadi exception yang tak tertangani secara internal
     */
    public static HistoryStatusMahasiswa getHistoryStatusMahasiswa(KrsMahasiswa krsMahasiswa) {
        return getHistoryStatusMahasiswa(krsMahasiswa, false);
    }

    // ========================================================================
    // CORE LOGIC: GET HISTORY STATUS MAHASISWA
    // ========================================================================

    /**
     * Method inti kelas ini: mengambil (atau menghitung ulang & menyimpan) status kemahasiswaan
     * untuk satu {@code krsMahasiswa}. Alur:
     * <ol>
     * <li><b>Cache.</b> Bila {@code tetapDiprosesWalaupunSudahAda} bernilai {@code false} DAN
     * semester ini tidak termasuk {@link Mahasiswa#getPaksaAktifSemester()}, coba ambil dari cache
     * lewat {@link #cekDanUpdateCacheStatus} lebih dulu — bila hit, langsung kembali tanpa
     * menyentuh aturan bisnis.</li>
     * <li><b>Query DB</b> lewat {@link #fetchHistoryFromDb} untuk baris terbaru yang cocok
     * mahasiswa+SP+semester/tahap.</li>
     * <li>Bila hasil query bukan {@code null} DAN semester yang diminta BUKAN semester berjalan
     * mahasiswa (mis. dipanggil untuk menampilkan riwayat semester lama), baris lama langsung
     * dipakai apa adanya (di-cache lalu dikembalikan) — aturan bisnis TIDAK dievaluasi ulang untuk
     * semester yang sudah lewat.</li>
     * <li>Bila belum ada baris sama sekali, {@link #inisialisasiDataBaru} membuat baris baru
     * (status awal diwariskan dari semester sebelumnya bila ada).</li>
     * <li><b>Evaluasi aturan bisnis</b> (hanya untuk semester berjalan atau saat dipaksa
     * refresh): tahun akademik disinkronkan; mahasiswa pindah ({@code getNimBaruPindah()} terisi)
     * langsung dipaksa {@code TIDAK_AKTIF} tanpa melalui {@link #kalkulasiStatusLogikaLanjutan};
     * selain itu {@link #kalkulasiStatusLogikaLanjutan} yang menentukan transisi status.</li>
     * <li>Pengecekan retroaktif tambahan: bila status saat ini AKTIF tapi semester sudah
     * {@code >=} {@link #getJumlahSemester}, dan ternyata SUDAH ADA baris LULUS di semester yang
     * sama-atau-lebih-awal (query terpisah), status dipaksa LULUS — menangani kasus lulus
     * "ditemukan belakangan" agar tidak terus tampil Aktif.</li>
     * <li>SKS dari {@code krsMahasiswa.getSksBukanKonversi()} disinkronkan ke kolom
     * {@code sks} bila berbeda.</li>
     * <li>Bila baris baru dibuat atau ada perubahan, disimpan dalam transaksi
     * ({@code saveOrUpdate}); kegagalan di-rollback diam-diam dan dicatat ke
     * {@code ErrorAuditUtil}.</li>
     * <li>Hasil akhir (berhasil maupun gagal di tengah jalan) selalu dicoba disimpan ke cache
     * cepat {@link GeneralValueObject#masukkanDataLangsung}.</li>
     * </ol>
     * Session Hibernate dibuka dan ditutup secara lokal dalam method ini (tidak menggunakan
     * session request-scoped) — aman dipanggil dari luar konteks web/transaksi Action.
     *
     * @param krsMahasiswa                     baris KRS mahasiswa yang menjadi acuan mahasiswa+semester/tahap+SP
     * @param tetapDiprosesWalaupunSudahAda     {@code true} untuk melewati kedua lapis cache dan selalu mengevaluasi ulang dari DB
     * @return baris {@link HistoryStatusMahasiswa} (tersimpan), atau {@code null} bila DB/inisialisasi gagal total sebelum objek sempat dibuat
     */
    public static HistoryStatusMahasiswa getHistoryStatusMahasiswa(KrsMahasiswa krsMahasiswa, boolean tetapDiprosesWalaupunSudahAda) {
        Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
        Integer semester = krsMahasiswa.getSemester();
        Integer tahap = krsMahasiswa.getTahapan();
        String tahunAjaran = krsMahasiswa.getTahunAkademik();

        String key = HistoryStatusMahasiswa.class.getSimpleName() + "_" + mahasiswa.getId() + "_" + (tahap == null || tahap.equals(0) ? semester : tahap);

        JSONObject jsonObject = Common.getJSONTemporary(mahasiswa, key);
        if (jsonObject == null) jsonObject = new JSONObject();
		boolean semesterDipaksaAktif = semesterAdaDalamDaftar(mahasiswa.getPaksaAktifSemester(), semester);

        // 1. Cek dari Cache / RAM (Jika tidak dipaksa proses)
		// Status semester yang dipaksa aktif tidak boleh berhenti pada cache lama (mis. masih
		// Nonaktif sebelum kolom paksa diisi). Lewati cache agar aturan paksa dievaluasi dan
		// hasilnya ikut disimpan ke HistoryStatusMahasiswa.
        if (!tetapDiprosesWalaupunSudahAda && !semesterDipaksaAktif) {
            HistoryStatusMahasiswa s = cekDanUpdateCacheStatus(krsMahasiswa, mahasiswa, key, jsonObject);
            if (s != null) return s;
        }

        if (!ConstantValues.aktifkanTahapan) tahap = null;

        Session session = null;
        Transaction tx = null;
        HistoryStatusMahasiswa historyStatusMahasiswa = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // 2. Query ke Database
            historyStatusMahasiswa = fetchHistoryFromDb(session, mahasiswa, krsMahasiswa, semester, tahap);

			if (!tetapDiprosesWalaupunSudahAda && !semesterDipaksaAktif && historyStatusMahasiswa != null) {
                if (mahasiswa.currentSemester() != null && !mahasiswa.currentSemester().equals(semester)) {
                    simpanKeCache(mahasiswa, key, jsonObject, historyStatusMahasiswa);
                    return historyStatusMahasiswa;
                }
            }

            boolean isNewData = false;
            boolean isDataModified = false;

            // 3. Inisialisasi Data Baru jika belum ada
            if (historyStatusMahasiswa == null) {
                historyStatusMahasiswa = inisialisasiDataBaru(session, mahasiswa, krsMahasiswa, semester, tahap, tahunAjaran);
                isNewData = true;
            }

            // 4. Proses Logika Bisnis (Perubahan Data)
            if (historyStatusMahasiswa.getTahunAkademik() == null || !historyStatusMahasiswa.getTahunAkademik().equals(tahunAjaran)) {
                historyStatusMahasiswa.setTahunAkademik(tahunAjaran);
                isDataModified = true;
            }

            if (mahasiswa.getNimBaruPindah() != null && !mahasiswa.getNimBaruPindah().trim().isEmpty()
                    && !isStatusEqual(historyStatusMahasiswa.getStatusMahasiswa(), ConstantValues.TIDAK_AKTIF)) {
                historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.TIDAK_AKTIF);
                isDataModified = true;
            } else {
                isDataModified |= kalkulasiStatusLogikaLanjutan(session, mahasiswa, historyStatusMahasiswa, semester, tahap);
            }

            // Pengecekan retroaktif status Lulus jika saat ini Aktif
            if (isStatusEqual(historyStatusMahasiswa.getStatusMahasiswa(), ConstantValues.AKTIF)) {
                Integer jumlah_semester = getJumlahSemester(mahasiswa);
                if (semester != null && semester >= jumlah_semester && ConstantValues.LULUS != null) {
                    int apakahAdaLulus = ((Number) session.createCriteria(HistoryStatusMahasiswa.class)
                            .add(Restrictions.eq("statusMahasiswa", ConstantValues.LULUS))
                            .add(Restrictions.le("semester", semester))
                            .add(Restrictions.eq("mahasiswa", mahasiswa))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

                    if (apakahAdaLulus > 0) {
                        historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.LULUS);
                        isDataModified = true;
                    }
                }
            }

            if (krsMahasiswa.getSksBukanKonversi() != null && !krsMahasiswa.getSksBukanKonversi().equals(historyStatusMahasiswa.getSks())) {
                historyStatusMahasiswa.setSks(krsMahasiswa.getSksBukanKonversi());
                isDataModified = true;
            }

            // 5. Simpan ke Database jika ada perubahan
            if (isNewData || isDataModified) {
                tx = session.beginTransaction();
                session.saveOrUpdate(historyStatusMahasiswa);
                tx.commit();
            }

            simpanKeCache(mahasiswa, key, jsonObject, historyStatusMahasiswa);

        } catch (Exception e) {
            rollbackQuietly(tx);
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:177");
        } finally {
            closeSession(session);
        }

        try { GeneralValueObject.masukkanDataLangsung(HistoryStatusMahasiswa.class, historyStatusMahasiswa, key); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:182");}

        return historyStatusMahasiswa;
    }

    /**
     * Validasi UI sebelum sebuah perubahan status disimpan secara manual (dipanggil dari layar
     * yang mengizinkan admin mengubah {@code statusMahasiswa} langsung, bukan dari mesin aturan
     * {@link #kalkulasiStatusLogikaLanjutan}). Menampilkan {@link MyMessageboxConfig} peringatan
     * dan mengembalikan {@code false} bila salah satu aturan berikut dilanggar (tidak melempar
     * exception untuk kasus ini — hanya untuk kegagalan tak terduga):
     * <ul>
     * <li>Status CUTI harus konsisten dengan ada/tidaknya {@link PendaftaranCutiMahasiswa} yang
     * DISETUJUI ({@code getPersetujuan() == true}) pada semester tsb — status Cuti tanpa
     * pengajuan disetujui, atau pengajuan disetujui tanpa status Cuti, sama-sama ditolak.</li>
     * <li>Status LULUS (untuk mahasiswa yang bukan alih jenjang/pindahan — dicek dari
     * {@link Mahasiswa#getStatusAwalMahasiswa()}): ditolak bila masih ada
     * {@link Detailperkuliahan} di semester setelah semester yang diusulkan, atau bila semester
     * yang diusulkan kurang dari {@link #getJumlahSemester} (minimal semester kelulusan
     * jenjangnya).</li>
     * <li>Status AKTIF (bila bukan dari Cuti): digerbangi konfigurasi
     * {@code mhs_all_lambat_bayar_langsung_tidak_aktif} dan ambang tahun mulai
     * {@code tahun_mulai_auto_not_activating_mhs_belum_bayar} (default 2014) — bila berlaku dan
     * mahasiswa belum melunasi kegiatan bersyarat-aktif ({@link #cekPembayaranMahasiswa}) untuk
     * semester {@code > 1}, pengaktifan ditolak dan {@link KegiatanHelper#updateBatasStudiMahasiswa}
     * dipanggil untuk mencatat batas studi.</li>
     * </ul>
     * <b>Efek samping:</b> memanggil {@code historyStatusMahasiswa.put(...)} untuk mencatat hasil
     * cek pembayaran ke dalam objek (dipakai UI/laporan), dan bisa memicu
     * {@code CommonHelperClass.reloadJenisKegiatans()} bila cache jenis kegiatan syarat-aktif
     * belum dimuat. TIDAK melakukan {@code save}/{@code update} ke {@code historyStatusMahasiswa}
     * itu sendiri — hanya memvalidasi dan menampilkan pesan; penyimpanan tetap tanggung jawab
     * pemanggil.
     *
     * @param mahasiswa             mahasiswa yang statusnya akan diubah
     * @param statusMahasiswa       status baru yang diusulkan
     * @param semester              semester yang diusulkan
     * @param tahap                 tahap terkait (dipakai untuk cek pembayaran)
     * @param historyStatusMahasiswa baris history yang sedang diedit (menerima efek samping {@code put})
     * @param sp                    {@code true} bila konteksnya Semester Pendek (memengaruhi pencarian cuti)
     * @return {@code true} bila perubahan status boleh dilanjutkan; {@code false} bila ditolak (pesan sudah ditampilkan ke user)
     * @throws Exception dilempar bila terjadi kegagalan tak terduga (bukan pelanggaran aturan bisnis di atas)
     */
    public static boolean checkStatus(Mahasiswa mahasiswa, StatusMahasiswa statusMahasiswa, Integer semester, Integer tahap, HistoryStatusMahasiswa historyStatusMahasiswa, boolean sp) throws Exception {
        historyStatusMahasiswa.put("", "checkStatusPembayaranMahasiswa");

        PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap, sp);
        int countCuti = (pendaftaranCutiMahasiswa != null && pendaftaranCutiMahasiswa.getPersetujuan() != null && pendaftaranCutiMahasiswa.getPersetujuan()) ? 1 : 0;

        if (countCuti > 0 && !isStatusEqual(statusMahasiswa, ConstantValues.CUTI)) {
            MyMessageboxConfig.show("Mahasiswa " + mahasiswa + " telah terdaftar sebagai mahasiswa cuti di semester " + semester, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }

        if (countCuti == 0 && isStatusEqual(statusMahasiswa, ConstantValues.CUTI)) {
            MyMessageboxConfig.show("Mahasiswa " + mahasiswa + " belum terdaftar sebagai mahasiswa cuti di semester " + semester, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }

        if (mahasiswa.getStatusAwalMahasiswa() != null && mahasiswa.getStatusAwalMahasiswa().getNama() != null) {
            String statusAwal = mahasiswa.getStatusAwalMahasiswa().getNama().toLowerCase().trim();
            if (!statusAwal.contains("alih") && !statusAwal.contains("pindah")) {
                if (isStatusEqual(statusMahasiswa, ConstantValues.LULUS)) {
                    Integer maxSemester = 0;
                    List<Long> detailIds = mahasiswa.ambilDetailperkuliahan();
                    if (detailIds != null) {
                        for (Long detailperkuliahanid : detailIds) {
                            Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
                            if (detail != null && detail.getSemester() != null && maxSemester < detail.getSemester()) {
                                maxSemester = detail.getSemester();
                            }
                        }
                    }

                    if (maxSemester != null && maxSemester > semester && !semester.equals(0)) {
                        MyMessageboxConfig.show("Mahasiswa " + mahasiswa + " tidak bisa diupdate lulus di semester " + semester + ", karena masih ada perkuliahan di semester " + maxSemester, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                        return false;
                    }

                    Integer jumlah_semester = getJumlahSemester(mahasiswa);
                    String namaJenjang = (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getJenjang() != null) ? mahasiswa.getJurusan().getJenjang().getNama() : "";

                    if (semester < jumlah_semester) {
                        MyMessageboxConfig.show("Mahasiswa " + mahasiswa + " tidak bisa diupdate lulus di semester " + semester + ", karena minimal kelulusan di jenjang " + namaJenjang + " adalah " + jumlah_semester, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                        return false;
                    }
                }
            }
        }

        if (countCuti == 0 && isStatusEqual(statusMahasiswa, ConstantValues.AKTIF)) {
            Konfigurasi k = Common.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa());
            if (k != null && Konfigurasi.AKTIF.equals(k.getNilai())) {
                int tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(), mahasiswa.getSemesterMulai());
                int tahunMulai = 2014;
                try {
                    Konfigurasi kmulai = Common.getKonfigurasi("tahun_mulai_auto_not_activating_mhs_belum_bayar", "2014");
                    if (kmulai != null && kmulai.getNilai() != null) tahunMulai = Integer.parseInt(kmulai.getNilai().trim());
                } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }

                if (tahunAkademikMulai >= tahunMulai) {
                    if (CommonHelperClass.jenisKegiatansUntukSyaratAktif == null) {
                        try { CommonHelperClass.reloadJenisKegiatans(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:246");}
                    }

                    boolean checkStatusPembayaranMahasiswa = cekPembayaranMahasiswa(semester, tahap, mahasiswa);
                    
                    if (!checkStatusPembayaranMahasiswa && semester > 1) {
                        KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, null, semester, checkStatusPembayaranMahasiswa);
                        historyStatusMahasiswa.put(String.valueOf(checkStatusPembayaranMahasiswa), "checkStatusPembayaranMahasiswa");

                        StringBuilder nameBuilder = new StringBuilder();
                        if (CommonHelperClass.jenisKegiatansUntukSyaratAktif != null) {
                            boolean isFirst = true;
                            for (JenisKegiatan s : CommonHelperClass.jenisKegiatansUntukSyaratAktif) {
                                if (s != null && s.getNamaKegiatan() != null) {
                                    if (!isFirst) nameBuilder.append(", atau ");
                                    nameBuilder.append(s.getNamaKegiatan());
                                    isFirst = false;
                                }
                            }
                        }
                        MyMessageboxConfig.show("Mahasiswa " + mahasiswa + " belum melakukan pembayaran " + nameBuilder.toString() + " di semester " + semester + ", sehingga status-nya tidak bisa di-aktif-kan", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                        return false;
                    } else {
                        historyStatusMahasiswa.put("true", "checkStatusPembayaranMahasiswa");
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sinkronisasi status kemahasiswaan (HistoryStatusMahasiswa) seorang mahasiswa untuk
     * rentang semester 1..semester berjalan, dengan saringan tahun akademik dan jenis
     * semester. Nilai {@code jenisSemester}: {@code Perkuliahan.GANJIL} / {@code GENAP}
     * (hanya semester ganjil/genap), {@link Perkuliahan#SP} (mode SEMESTER PENDEK:
     * membuat/menyegarkan KRS + status ber-flag {@code sp} sehingga mahasiswa dapat
     * mengambil KRS Semester Antara), atau {@code null} = Semua (semester reguler DAN,
     * bila mahasiswa sudah punya baris KRS SP, status SP-nya ikut disegarkan — baris SP
     * baru TIDAK diciptakan pada mode Semua agar data KRS tidak terpolusi).
     * <p>
     * Status SP disimpan terpisah dari status reguler (kolom {@code sp} pada
     * history_status_mahasiswa; lihat {@code fetchHistoryFromDb}) — mahasiswa yang
     * Nonaktif di semester reguler (mis. belum daftar ulang tahun akademik baru) tetap
     * bisa Aktif pada Semester Pendek di antara dua tahun akademik.
     */
    public static void singkronisasiStatusMahasiswa(Label label, Mahasiswa mahasiswa, String tahunAkademikParam, String jenisSemester, boolean nonAktifkan) {
        if (mahasiswa == null) return;

        final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
        String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
        int cursemester = Common.getSemester(tahunAngkatanMhs, semesterMulai, mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
        int mulai = mahasiswa.getPindahKeKampusIniMasukSemester() != null ? mahasiswa.getPindahKeKampusIniMasukSemester() : 1;
        if (mulai == 0) mulai = 1;

        boolean modeSemesterPendek = Perkuliahan.SP.equalsIgnoreCase(jenisSemester)
                || "SP".equalsIgnoreCase(jenisSemester);

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            for (int semester = mulai; semester <= cursemester; semester++) {
                try {
                    Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
                    String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

                    boolean masuk = (tahunAkademikParam == null || tahunAkademikParam.equals(tahunAkademik))
                            && (jenisSemester == null || modeSemesterPendek
                                    || semester % 2 == (jenisSemester.equals(Perkuliahan.GANJIL) ? 1 : 0));

                    if (masuk) {
                        if (!modeSemesterPendek) {
                            KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);
                            HistoryStatusMahasiswa historyStatusMahasiswa = getHistoryStatusMahasiswa(krsMahasiswa);

                            if (nonAktifkan && historyStatusMahasiswa != null && historyStatusMahasiswa.getId() != null) {
                                prosesNonAktifkanStatusSingkronisasi(session, mahasiswa, semester, krsMahasiswa, historyStatusMahasiswa);
                            }
                        }

                        // SEMESTER PENDEK: mode SP eksplisit → selalu buat/segarkan KRS SP +
                        // status SP (agar mahasiswa boleh ambil KRS Semester Antara). Mode
                        // lain (Semua) → hanya segarkan bila baris KRS SP sudah ada; jangan
                        // menciptakan baris SP baru untuk semester yang tak pernah ber-SP.
                        try {
                            boolean prosesSp = modeSemesterPendek;
                            if (!prosesSp && jenisSemester == null) {
                                Number cekSp = (Number) session.createCriteria(KrsMahasiswa.class)
                                        .add(Restrictions.eq("mahasiswa", mahasiswa))
                                        .add(Restrictions.eq("semester", Integer.valueOf(semester)))
                                        .add(Restrictions.eq("semesterPendek", Perkuliahan.SEMESTER_PENDEK))
                                        .setProjection(Projections.rowCount()).uniqueResult();
                                prosesSp = cekSp != null && cekSp.intValue() > 0;
                            }
                            if (prosesSp) {
                                KrsMahasiswa krsSp = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null,
                                        Perkuliahan.SEMESTER_PENDEK);
                                if (krsSp != null) {
                                    getHistoryStatusMahasiswa(krsSp);
                                }
                            }
                        } catch (Exception eSp) {
                            ais.common.ErrorAuditUtil.record(eSp,
                                    "singkronisasiStatusMahasiswa: gagal sinkron status Semester Pendek; mhs="
                                            + mahasiswa.getNim() + ", smt=" + semester);
                        }
                    }
                    session.flush();
                    session.clear();
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
            }
        } finally {
            closeSession(session);
        }
    }

    /**
     * Menjadwalkan {@link #singkronisasiStatusMahasiswaNotTimer} untuk dijalankan ~1 detik
     * kemudian lewat {@link Timer} ZK sekali-tembak, dipasang ke root halaman ZK aktif saat ini
     * ({@code ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()}). Dipakai agar
     * sinkronisasi status (yang bisa memakan waktu — loop banyak semester + query DB) tidak
     * memblokir response awal halaman; timer men-detach dirinya sendiri setelah sekali berjalan.
     * HANYA valid dipanggil dari dalam konteks event/desktop ZK yang sedang aktif.
     *
     * @param mahasiswa mahasiswa yang akan disinkronkan statusnya secara asinkron
     */
    public static void singkronisasiStatusMahasiswaTimer(final Mahasiswa mahasiswa) {
        final Timer timer = new Timer(1000);
        timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        timer.addEventListener("onTimer", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                singkronisasiStatusMahasiswaNotTimer(mahasiswa);
                timer.detach();
            }
        });
        timer.start();
    }

    /**
     * Wrapper sinkron (tanpa {@link Timer}) dari {@link #singkronisasiStatusMahasiswa}: memakai
     * tahun akademik berjalan ({@code Common.getCurrentTahunAkademik()}) dan jenis semester
     * berjalan (Ganjil/Genap dari {@code Common.isNowSemensterGanjil()}), tanpa label progres,
     * dan {@code nonAktifkan=false} (tidak memicu proses batas-studi otomatis).
     *
     * @param mahasiswa mahasiswa yang statusnya disinkronkan untuk tahun akademik berjalan
     */
    public static void singkronisasiStatusMahasiswaNotTimer(final Mahasiswa mahasiswa) {
        HistoryStatusMahasiswaUtil.singkronisasiStatusMahasiswa(null, mahasiswa, Common.getCurrentTahunAkademik(), Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP, false);
    }

    /**
     * Seeding/upsert lima baris {@link StatusMahasiswa} kanonik (Aktif/Cuti/Nonaktif/Lulus/Out)
     * lewat {@link #initStatusHelper}, satu transaksi untuk seluruh operasi. Dipanggil saat
     * inisialisasi data master (mis. setup instalasi baru atau perbaikan data) — aman dipanggil
     * berulang karena {@link #initStatusHelper} melakukan {@code saveOrUpdate} berbasis
     * pencarian nama, bukan insert buta. Kegagalan di tengah jalan (mis. satu status gagal
     * dicari/disimpan) membatalkan seluruh transaksi (rollback) dan dicatat ke
     * {@code ErrorAuditUtil} — bukan sebagian tersimpan.
     */
    public static void initDataStatusMahasiswa() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            initStatusHelper(session, "Aktif", "A");
            initStatusHelper(session, "Cuti", "C");
            initStatusHelper(session, "Nonaktif", "N", "NON-AKTIF");
            initStatusHelper(session, "Lulus", "L");
            initStatusHelper(session, "Out", "K");

            tx.commit();
        } catch (Exception e) {
            rollbackQuietly(tx);
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:349");
        } finally {
            closeSession(session);
        }
    }

    // ========================================================================
    // PRIVATE HELPER METHODS (DIBUAT UNTUK MEMAKSIMALKAN REUSE & CLEAN CODE)
    // ========================================================================

    /**
     * Membuat objek {@link HistoryStatusMahasiswa} TRANSIENT (tidak disimpan ke DB) berstatus
     * AKTIF, dipakai sebagai fallback ketika mahasiswa belum tersimpan (id {@code null}) atau
     * ketika query DB tidak menemukan baris apa pun — memastikan pemanggil {@link #currentStatus}
     * selalu menerima objek non-null yang aman dipakai UI tanpa cek null tambahan.
     *
     * @param krsMahasiswa sumber flag Semester Pendek (boleh {@code null})
     * @param mahasiswa    mahasiswa terkait (boleh belum tersimpan/{@code null})
     * @param semester     semester yang diminta
     * @param tahap        tahap yang diminta
     * @return objek {@link HistoryStatusMahasiswa} baru, status AKTIF, belum pernah di-persist
     */
    private static HistoryStatusMahasiswa createDefaultStatus(KrsMahasiswa krsMahasiswa, Mahasiswa mahasiswa, Integer semester, Integer tahap) {
        HistoryStatusMahasiswa history = new HistoryStatusMahasiswa(krsMahasiswa == null ? null : krsMahasiswa.getSemesterPendek());
        history.setStatusMahasiswa(ConstantValues.AKTIF);
        history.setMahasiswa(mahasiswa);
        history.setSemester(semester);
        history.setTahap(tahap);
        return history;
    }

    /**
     * Perbandingan status null-safe berbasis id entity (bukan {@code equals()} default/referensi
     * objek) — dipakai di seluruh kelas ini karena instance {@link StatusMahasiswa} yang
     * dibandingkan sering berasal dari sumber berbeda (cache JSON hasil deserialisasi vs. konstanta
     * {@link ConstantValues} vs. hasil query DB) sehingga identitas objek Java tidak bisa
     * diandalkan, hanya id barisnya.
     *
     * @param current  status yang sedang diperiksa (boleh {@code null})
     * @param constant status pembanding, biasanya salah satu konstanta {@link ConstantValues}
     * @return {@code true} hanya bila keduanya non-null dan id-nya sama
     */
    private static boolean isStatusEqual(StatusMahasiswa current, StatusMahasiswa constant) {
        return current != null && constant != null && current.getId() != null && current.getId().equals(constant.getId());
    }

    /**
     * Menyinkronkan kolom {@code sks} sebuah {@link HistoryStatusMahasiswa} yang SUDAH TERSIMPAN
     * bila nilainya berbeda dari {@code newSks} — dipanggil dari jalur cache
     * ({@link #cekDanUpdateCacheStatus}) supaya SKS yang ditampilkan tetap akurat walau status
     * itu sendiri dibaca dari cache (bukan dihitung ulang penuh). Membuka session &amp; transaksi
     * sendiri; memanggil {@code session.refresh(s)} lebih dulu untuk memastikan entity yang
     * dimutasi adalah versi terbaru dari DB (menghindari overwrite field lain yang mungkin sudah
     * berubah). Kegagalan di-rollback diam-diam tanpa dilempar ke pemanggil.
     *
     * @param s      baris history yang sudah tersimpan (harus punya id)
     * @param newSks nilai SKS baru; tidak melakukan apa pun bila {@code null} atau sama dengan nilai lama
     */
    private static void updateSksBukanKonversi(HistoryStatusMahasiswa s, Integer newSks) {
        if (newSks != null && !newSks.equals(s.getSks())) {
            Session session = null;
            Transaction tx = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                session.refresh(s);
                s.setSks(newSks);
                tx = session.beginTransaction();
                session.update(s);
                tx.commit();
            } catch (Exception e) {
                rollbackQuietly(tx);
            } finally {
                closeSession(session);
            }
        }
    }

    /**
     * Implementasi dua lapis cache baca yang dijelaskan di Javadoc class-level: lapis 1 (paling
     * cepat) {@link GeneralValueObject#ambilDataLangsung} — bila hit dan valid (punya id), SKS-nya
     * disinkronkan lewat {@link #updateSksBukanKonversi} lalu langsung dikembalikan. Bila lapis 1
     * kosong, coba lapis 2: cache JSON per-mahasiswa (parameter {@code jsonObject}, sumbernya
     * {@code Common.getJSONTemporary}) — hasil deserialisasi HANYA dipercaya bila punya id DAN
     * statusnya BUKAN {@code TIDAK_AKTIF} (status Nonaktif dari cache lama sengaja tidak dipercaya
     * mentah-mentah, sebab kasus nyata: cache bisa basi sementara pembayaran sudah lunas — lebih
     * aman memaksa evaluasi ulang untuk kasus Nonaktif daripada terus menampilkan Nonaktif basi).
     * Bila lapis 2 valid, mahasiswa &amp; SKS disegarkan pada objek hasil deserialisasi lalu
     * ditulis balik ke lapis 1 sebelum dikembalikan. Exception saat deserialisasi JSON ditelan
     * (dicatat ke {@code ErrorAuditUtil}) dan dianggap cache-miss.
     *
     * @param krsMahasiswa sumber SKS terbaru untuk disinkronkan bila cache hit
     * @param mahasiswa    mahasiswa pemilik cache; bila {@code null}/belum tersimpan, lapis 2 dilewati
     * @param key          kunci cache gabungan kelas+id mahasiswa+semester/tahap
     * @param jsonObject   objek JSON cache mahasiswa (lapis 2), dimutasi bila terjadi refresh dari lapis 1
     * @return baris {@link HistoryStatusMahasiswa} dari cache bila valid dan dipercaya, atau {@code null} bila cache-miss (pemanggil wajib lanjut ke DB)
     */
    private static HistoryStatusMahasiswa cekDanUpdateCacheStatus(KrsMahasiswa krsMahasiswa, Mahasiswa mahasiswa, String key, JSONObject jsonObject) {
        HistoryStatusMahasiswa s = (HistoryStatusMahasiswa) GeneralValueObject.ambilDataLangsung(HistoryStatusMahasiswa.class, key);
        if (s != null && s.getId() != null) {
            updateSksBukanKonversi(s, krsMahasiswa.getSksBukanKonversi());
            return s;
        }
        try {
            if (mahasiswa != null && mahasiswa.getId() != null && jsonObject.has(key) && !jsonObject.isNull(key)) {
                s = (HistoryStatusMahasiswa) Common.convertToObject(jsonObject.getJSONObject(key), HistoryStatusMahasiswa.class);
                if (s != null && s.getId() != null) {
                    updateSksBukanKonversi(s, krsMahasiswa.getSksBukanKonversi());
                    if (!isStatusEqual(s.getStatusMahasiswa(), ConstantValues.TIDAK_AKTIF)) {
                        s.setMahasiswa(mahasiswa);
                        s.setSks(krsMahasiswa.getSksBukanKonversi());
                        GeneralValueObject.masukkanDataLangsung(HistoryStatusMahasiswa.class, s, key);
                        return s;
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:410");}
        return null;
    }

    /**
     * Query Hibernate Criteria yang mengambil baris {@link HistoryStatusMahasiswa} TERBARU
     * (diurutkan {@code id} descending, {@code setMaxResults(1)}) yang cocok mahasiswa, flag SP,
     * dan semester-atau-tahap. Filter SP: bila {@code krsMahasiswa} bertanda
     * {@code Perkuliahan.SEMESTER_PENDEK}, dicari baris dengan {@code sp} yang sama persis;
     * selain itu dicari baris dengan {@code sp IS NULL} (status reguler) — inilah mekanisme
     * pemisahan status reguler vs. SP yang dijelaskan di Javadoc class-level. Filter
     * semester/tahap: bila {@code tahap} kosong/0, dicocokkan ke kolom {@code semester}; selain
     * itu ke kolom {@code tahap} (mode {@link ConstantValues#aktifkanTahapan}).
     *
     * @param session      session Hibernate aktif untuk menjalankan query
     * @param mahasiswa    mahasiswa yang dicari
     * @param krsMahasiswa sumber flag Semester Pendek (boleh {@code null} — diperlakukan sebagai reguler)
     * @param semester     nilai pencocokan bila tahap tidak dipakai
     * @param tahap        nilai pencocokan bila diisi &amp; bukan 0
     * @return baris terbaru yang cocok, atau {@code null} bila belum ada
     */
    private static HistoryStatusMahasiswa fetchHistoryFromDb(Session session, Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa, Integer semester, Integer tahap) {
        return (HistoryStatusMahasiswa) session.createCriteria(HistoryStatusMahasiswa.class)
                .add(Restrictions.eq("mahasiswa", mahasiswa))
                .add(krsMahasiswa != null && Perkuliahan.SEMESTER_PENDEK.equals(krsMahasiswa.getSemesterPendek())
                        ? Restrictions.eq("sp", krsMahasiswa.getSemesterPendek()) : Restrictions.isNull("sp"))
                .add(tahap == null || tahap.equals(0) ? Restrictions.eq("semester", semester) : Restrictions.eq("tahap", tahap))
                .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
    }

    /**
     * Membuat baris {@link HistoryStatusMahasiswa} BARU (belum disimpan — {@code saveOrUpdate}
     * dilakukan oleh pemanggil {@link #getHistoryStatusMahasiswa(KrsMahasiswa, boolean)}) untuk
     * kombinasi mahasiswa+semester/tahap+SP yang belum pernah punya baris. Status awal default
     * AKTIF, TAPI bila {@code semester > 1}, status DIWARISKAN dari baris semester sebelumnya
     * ({@code semester - 1}) jika ditemukan — query proyeksi khusus (hanya mengambil kolom
     * {@code statusMahasiswa}, bukan entity penuh) dengan filter {@code tahap} yang lebih longgar
     * (mencocokkan tahap ATAU {@code tahap IS NULL}, karena baris semester sebelumnya mungkin
     * dibuat sebelum mode tahap diaktifkan). Ini penting agar mahasiswa yang statusnya sudah
     * Nonaktif/Cuti di semester lalu tidak "direset" jadi Aktif begitu saja saat baris semester
     * barunya pertama kali dibuat.
     *
     * @param session      session Hibernate aktif
     * @param mahasiswa    pemilik baris baru
     * @param krsMahasiswa sumber SKS awal dan flag SP
     * @param semester     semester baris baru
     * @param tahap        tahap baris baru (boleh {@code null})
     * @param tahunAjaran  tahun akademik yang dicatat pada baris baru
     * @return objek {@link HistoryStatusMahasiswa} baru, TRANSIENT (belum di-persist)
     */
    private static HistoryStatusMahasiswa inisialisasiDataBaru(Session session, Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa, Integer semester, Integer tahap, String tahunAjaran) {
        StatusMahasiswa statusMahasiswa = ConstantValues.AKTIF;
        if (semester != null && semester > 1) {
            StatusMahasiswa status = (StatusMahasiswa) session.createCriteria(HistoryStatusMahasiswa.class)
                    .setProjection(Projections.property("statusMahasiswa"))
                    .add(krsMahasiswa != null && Perkuliahan.SEMESTER_PENDEK.equals(krsMahasiswa.getSemesterPendek())
                            ? Restrictions.eq("sp", krsMahasiswa.getSemesterPendek()) : Restrictions.isNull("sp"))
                    .add(Restrictions.eq("mahasiswa", mahasiswa))
                    .add(tahap == null || tahap.equals(0) ? Restrictions.sqlRestriction("true")
                            : Restrictions.or(Restrictions.isNull("tahap"), Restrictions.eq("tahap", tahap)))
                    .add(Restrictions.eq("semester", semester - 1)).setMaxResults(1).uniqueResult();
            if (status != null) statusMahasiswa = status;
        }

        HistoryStatusMahasiswa history = new HistoryStatusMahasiswa(tahunAjaran, krsMahasiswa.getSksBukanKonversi(), krsMahasiswa == null ? null : krsMahasiswa.getSemesterPendek());
        history.setMahasiswa(mahasiswa);
        history.setSemester(semester);
        history.setStatusMahasiswa(statusMahasiswa);
        history.setTahap(tahap);
        return history;
    }

    /**
     * Mesin aturan bisnis status kemahasiswaan — dipanggil hanya untuk semester berjalan/saat
     * refresh dipaksa dari {@link #getHistoryStatusMahasiswa(KrsMahasiswa, boolean)}. Menerapkan
     * berurutan (tiap aturan bisa saling menimpa hasil aturan sebelumnya dalam satu pemanggilan):
     * <ol>
     * <li><b>Keterlambatan bayar konfigurasi</b> ({@code mhs_all_lambat_bayar_langsung_tidak_aktif}):
     * hanya berlaku bila tahun akademik mulai semester ini SAMA DENGAN atau SATU TAHUN SEBELUM
     * tahun kalender berjalan (mencegah aturan ini menghukum retroaktif data semester lampau) dan
     * {@code semester > 1}. Bila berlaku, {@link #cekPembayaranMahasiswa} menentukan Aktif↔Nonaktif
     * dua arah, dan {@link KegiatanHelper#updateBatasStudiMahasiswa} dipanggil untuk mencatat
     * status batas studi.</li>
     * <li><b>Semester 1 selalu Aktif</b>: bila status saat ini Nonaktif tapi semester adalah 1,
     * dipaksa Aktif (mahasiswa baru tidak boleh langsung Nonaktif tanpa proses tagihan berjalan).</li>
     * <li><b>Aturan syarat-aktif kegiatan</b> (permintaan user 2026-08-02 &amp; 2026-08-05, kasus
     * KIP-K/UKT UBT dan UIN Mahmud Yunus Batusangkar): berlaku independen dari konfigurasi di atas
     * (checkbox per-{@code JenisKegiatan} sudah jadi opt-in eksplisit admin) untuk {@code semester >
     * 1}. Aktif→Nonaktif via {@link #cekPembayaranMahasiswa}; Nonaktif→Aktif via
     * {@link #adaKegiatanSyaratAktifLunasSemua} — method BERBEDA sengaja dipakai untuk tiap arah
     * karena semantik "belum ada kegiatan sama sekali" harus dibaca berbeda (lihat Javadoc kedua
     * method itu).</li>
     * <li><b>Status terminal retroaktif</b>: begitu {@code semester >= }{@link #getJumlahSemester},
     * {@link Mahasiswa#getStatusKeluar()} dicocokkan (case-insensitive, substring) ke
     * "lulus"→LULUS, "keluar"→DROP_OUT, "mengundurkan"/"putus"→KELUAR. Sebaliknya, status LULUS
     * yang semesternya ternyata masih di bawah {@link #getJumlahSemester} dikembalikan ke AKTIF
     * (data kelulusan yang salah/prematur dikoreksi otomatis).</li>
     * <li><b>Paksa Aktif admin</b> ({@link Mahasiswa#getPaksaAktifSemester()}, lihat
     * {@link #semesterAdaDalamDaftar}): menang atas semua aturan di atas KECUALI status sudah
     * terminal (Lulus/Drop Out/Keluar) — tidak pernah membatalkan status kelulusan/DO/keluar.</li>
     * </ol>
     * Setiap blok dibungkus try-catch tersendiri (exception dicatat ke {@code ErrorAuditUtil},
     * tidak menghentikan blok berikutnya) sehingga satu aturan yang gagal tidak menggagalkan
     * seluruh evaluasi status.
     *
     * @param session   session Hibernate aktif (diteruskan ke {@link KegiatanHelper#updateBatasStudiMahasiswa})
     * @param mahasiswa mahasiswa yang dievaluasi
     * @param history   baris {@link HistoryStatusMahasiswa} yang DIMUTASI langsung oleh method ini bila status berubah
     * @param semester  semester yang dievaluasi
     * @param tahap     tahap terkait (diteruskan ke pengecekan pembayaran)
     * @return {@code true} bila status pada {@code history} berubah (pemanggil perlu menyimpan), {@code false} bila tidak ada perubahan
     */
    private static boolean kalkulasiStatusLogikaLanjutan(Session session, Mahasiswa mahasiswa, HistoryStatusMahasiswa history, Integer semester, Integer tahap) {
        boolean isDataModified = false;
        Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
        int tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, mahasiswa.getSemesterMulai());
        int tahunMulai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);

        Konfigurasi konfLambat = Common.getKonfigurasi("mhs_all_lambat_bayar_langsung_tidak_aktif", "", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(), mahasiswa.getStatusAwalMahasiswa());
        boolean berubah = (konfLambat != null && Konfigurasi.AKTIF.equals(konfLambat.getNilai())) && (tahunAkademikMulai == tahunMulai || (tahunAkademikMulai + 1) == tahunMulai);

        if (berubah && semester != null && semester > 1) {
            history.put("", "checkStatusPembayaranMahasiswa");
            if (CommonHelperClass.jenisKegiatansUntukSyaratAktif == null) {
                try { CommonHelperClass.reloadJenisKegiatans(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:457");}
            }

            boolean checkStatusPembayaran = cekPembayaranMahasiswa(semester, tahap, mahasiswa);
            history.put(String.valueOf(checkStatusPembayaran), "checkStatusPembayaranMahasiswa");

            if (mahasiswa != null) KegiatanHelper.updateBatasStudiMahasiswa(mahasiswa, session, semester, checkStatusPembayaran);

            if (isStatusEqual(history.getStatusMahasiswa(), ConstantValues.AKTIF) && !checkStatusPembayaran) {
                history.setStatusMahasiswa(ConstantValues.TIDAK_AKTIF);
                isDataModified = true;
            } else if (isStatusEqual(history.getStatusMahasiswa(), ConstantValues.TIDAK_AKTIF) && checkStatusPembayaran) {
                history.setStatusMahasiswa(ConstantValues.AKTIF);
                isDataModified = true;
            }
        } else if (semester != null && semester == 1 && isStatusEqual(history.getStatusMahasiswa(), ConstantValues.TIDAK_AKTIF)) {
            history.setStatusMahasiswa(ConstantValues.AKTIF);
            isDataModified = true;
        }

        // ATURAN WAJIB (permintaan user 2026-08-02, KIP-K/UKT UBT): kalau mahasiswa berstatus
        // Aktif tapi punya kegiatan bersyarat-aktif (JenisKegiatan dgn checkbox "Digunakan
        // sebagai syarat untuk mengaktifkan status mahasiswa" tercentang) yang BELUM DIBAYAR
        // SAMA SEKALI utk semester ini, WAJIB jadi Non-Aktif -- berlaku baik dipanggil dari
        // "Proses Tagihan" (KegiatanProsesHeper) maupun dari layar DaftarUlangMahasiswaLamaAction
        // (keduanya lewat currentStatus -> method ini). SENGAJA TIDAK digerbangi Konfigurasi
        // "mhs_all_lambat_bayar_langsung_tidak_aktif" seperti blok "berubah" di atas -- checkbox
        // per-JenisKegiatan ITU SENDIRI sudah jadi opt-in eksplisit admin per jenis kegiatan;
        // reuse cekPembayaranMahasiswa (baypass-aware, refresh=true) agar konsisten dgn semantik
        // yg sudah ada, bukan ambang baru.
        try {
            if (semester != null && semester > 1) {
                if (CommonHelperClass.jenisKegiatansUntukSyaratAktif == null) {
                    try { CommonHelperClass.reloadJenisKegiatans(); } catch (Exception eReload) { ais.common.ErrorAuditUtil.record(eReload, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:syaratAktifBelumBayar"); }
                }
                if (isStatusEqual(history.getStatusMahasiswa(), ConstantValues.AKTIF)
                        && !cekPembayaranMahasiswa(semester, tahap, mahasiswa)) {
                    history.setStatusMahasiswa(ConstantValues.TIDAK_AKTIF);
                    isDataModified = true;
                } else if (isStatusEqual(history.getStatusMahasiswa(), ConstantValues.TIDAK_AKTIF)
                        // ATURAN WAJIB tambahan (permintaan user 2026-08-05, UIN Mahmud Yunus
                        // Batusangkar, kasus Vira Adya Putri): sisi sebaliknya -- begitu kegiatan
                        // bersyarat-aktif SUDAH lunas, status WAJIB langsung Aktif, JANGAN menunggu
                        // admin sinkron manual per-mahasiswa. Pakai helper KHUSUS
                        // (adaKegiatanSyaratAktifLunasSemua), BUKAN "!cekPembayaranMahasiswa(...)==false"
                        // dibalik -- cekPembayaranMahasiswa() default TRUE bila kegiatan belum ada sama
                        // sekali (aman utk arah Aktif->NonAktif: tak ada bukti = jangan hukum), tapi kalau
                        // dipakai terbalik utk NonAktif->Aktif, "tak ada bukti" akan SALAH dibaca sbg
                        // "sudah lunas" dan mempromosikan mahasiswa yg belum py tagihan sama sekali.
                        && adaKegiatanSyaratAktifLunasSemua(semester, tahap, mahasiswa)) {
                    history.setStatusMahasiswa(ConstantValues.AKTIF);
                    isDataModified = true;
                }
            }
        } catch (Exception eSyaratAktif) {
            ais.common.ErrorAuditUtil.record(eSyaratAktif, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:syaratAktifBelumBayar-outer");
        }

        Integer jumlah_semester = getJumlahSemester(mahasiswa);
        String statusKeluarStr = (mahasiswa.getStatusKeluar() != null && mahasiswa.getStatusKeluar().getNama() != null) ? mahasiswa.getStatusKeluar().getNama().trim().toLowerCase() : "";

        try {
            if (isStatusEqual(history.getStatusMahasiswa(), ConstantValues.LULUS) && semester != null && semester < jumlah_semester) {
                history.setStatusMahasiswa(ConstantValues.AKTIF);
                isDataModified = true;
            } else if (semester != null && semester >= jumlah_semester) {
                if (statusKeluarStr.equalsIgnoreCase("lulus") && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.LULUS)) {
                    history.setStatusMahasiswa(ConstantValues.LULUS);
                    isDataModified = true;
                } else if (statusKeluarStr.contains("keluar") && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.DROP_OUT)) {
                    history.setStatusMahasiswa(ConstantValues.DROP_OUT);
                    isDataModified = true;
                } else if ((statusKeluarStr.contains("mengundurkan") || statusKeluarStr.contains("putus")) && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.KELUAR)) {
                    history.setStatusMahasiswa(ConstantValues.KELUAR);
                    isDataModified = true;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:496");}

        // PAKSA AKTIF (fitur "Memaksa Aktif di semester mana saja?"): bila admin mengisi daftar semester
        // di Mahasiswa.paksaAktifSemester dan semester ini termasuk, paksa status = AKTIF (menang atas
        // logika pembayaran/otomatis di atas). TIDAK menimpa status terminal (LULUS/DROP_OUT/KELUAR)
        // agar tak membatalkan kelulusan/DO/keluar. Field kosong (default mhs lama) = tanpa efek.
        try {
            if (semester != null && semesterAdaDalamDaftar(mahasiswa.getPaksaAktifSemester(), semester)
                    && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.AKTIF)
                    && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.LULUS)
                    && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.DROP_OUT)
                    && !isStatusEqual(history.getStatusMahasiswa(), ConstantValues.KELUAR)) {
                history.setStatusMahasiswa(ConstantValues.AKTIF);
                isDataModified = true;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:511");}

        return isDataModified;
    }

    /**
     * True bila {@code semester} termuat dalam daftar semester dipisah koma {@code daftar}
     * (mis. "3,4,5"). Null/empty daftar atau null semester -> false.
     */
    private static boolean semesterAdaDalamDaftar(String daftar, Integer semester) {
        if (daftar == null || daftar.trim().isEmpty() || semester == null) {
            return false;
        }
		// Selain koma (format yang dianjurkan UI), toleransi titik koma, tanda pipa, dan
		// spasi. Data lama sering diisi "7|8" atau "7 8" sehingga sebelumnya tidak pernah
		// cocok dan status tetap Nonaktif meskipun secara maksud sudah dipaksa aktif.
		for (String s : daftar.split("[,;|\\s]+")) {
            if (s != null && s.trim().equalsIgnoreCase(String.valueOf(semester))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Menentukan apakah mahasiswa "boleh dianggap sudah bayar" untuk tetap/menjadi Aktif pada
     * semester tsb. Default {@code true} (aman untuk arah Aktif→Nonaktif: tak ada kegiatan
     * bersyarat-aktif berarti tidak ada yang menghukum) — <b>lihat kebalikannya</b>
     * {@link #adaKegiatanSyaratAktifLunasSemua} yang punya semantik default berbeda untuk arah
     * sebaliknya. Bila {@code Common.checkBaypassStatusPembayaranMahasiswa(...)} mengizinkan
     * bypass (mis. mahasiswa dikecualikan lewat konfigurasi lain), langsung {@code true} tanpa
     * query lebih lanjut. Selain itu, mengambil semua {@link Kegiatan} yang harus dibayar
     * ({@code Mahasiswa.ambilKegiatans(..., refresh=true)} — SENGAJA refresh agar tidak membaca
     * entity {@code Kegiatan} basi dari cache JVM, lihat komentar inline "GERBANG STATUS WAJIB
     * DATA SEGAR"), menyaring yang benar-benar berlaku untuk semester ini lewat
     * {@link #kegiatanSyaratAktifBerlaku}, lalu mensyaratkan SEMUA kegiatan berlaku sudah
     * lunas {@code >= 10%}. Nilai pembayaran dibaca langsung dari database lewat
     * {@link KegiatanPersistenceHelper#hitungPersentaseLunasAktualDariDatabase(Kegiatan)}
     * agar tidak menunggu rekap asynchronous pada {@code Kegiatan.bulans}.
     *
     * @param semester  semester yang dicek
     * @param tahap     tahap terkait (diteruskan ke pengecekan bypass)
     * @param mahasiswa mahasiswa yang dicek
     * @return {@code true} bila tidak ada kegiatan bersyarat-aktif berlaku yang belum dibayar sama sekali (atau bypass aktif)
     */
    private static boolean cekPembayaranMahasiswa(Integer semester, Integer tahap, Mahasiswa mahasiswa) {
        boolean check = true;
        if (!Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, CommonHelperClass.jenisKegiatansUntukSyaratAktif)) {
            // GERBANG STATUS WAJIB DATA SEGAR (refresh=true): tanpa ini persentase dibaca dari
            // entity Kegiatan yang di-cache di memori JVM. Kasus nyata (UBT, KIP-Kuliah):
            // pembayaran KIP baru saja di-upload (lunas di DB), tapi cek ini masih membaca
            // kegiatan basi ber-persentase 0 -> status mahasiswa DIBALIK ke Tidak Aktif dan
            // tersimpan, padahal sudah membayar.
            List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, CommonHelperClass.jenisKegiatansUntukSyaratAktif, true);
            if (kegiatanDibayars != null) {
                for (Kegiatan keg : kegiatanDibayars) {
                    if (!kegiatanSyaratAktifBerlaku(keg, semester)) {
                        continue;
                    }
                    check &= (keg != null && KegiatanPersistenceHelper
                            .hitungPersentaseLunasAktualDariDatabase(keg) >= 0.1);
                }
            }
        }
        return check;
    }

    /**
     * Kebalikan {@link #cekPembayaranMahasiswa}: dipakai KHUSUS utk promosi Non-Aktif->Aktif,
     * BUKAN utk pertanyaan "boleh tetap Aktif?". Beda krusial: {@code cekPembayaranMahasiswa}
     * default TRUE bila kegiatan bersyarat-aktif belum ada SAMA SEKALI (aman dipakai searah
     * Aktif->NonAktif -- tak ada bukti = jangan hukum). Di sini kebalikannya WAJIB default
     * FALSE bila belum ada kegiatan sama sekali -- kalau tidak, mahasiswa yang belum py tagihan
     * semester ini (blm diproses "Proses Tagihan") akan salah dipromosikan ke Aktif tanpa bukti
     * bayar apa pun. Refresh=true (data segar) sama seperti {@link #cekPembayaranMahasiswa}.
     */
    private static boolean adaKegiatanSyaratAktifLunasSemua(Integer semester, Integer tahap, Mahasiswa mahasiswa) {
        try {
            if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, CommonHelperClass.jenisKegiatansUntukSyaratAktif)) {
                return true;
            }
            List<Kegiatan> kegiatanDibayars = mahasiswa.ambilKegiatans(semester, CommonHelperClass.jenisKegiatansUntukSyaratAktif, true);
            if (kegiatanDibayars == null || kegiatanDibayars.isEmpty()) {
                return false;
            }
            boolean adaTagihanYangBerlaku = false;
            for (Kegiatan keg : kegiatanDibayars) {
                if (!kegiatanSyaratAktifBerlaku(keg, semester)) {
                    continue;
                }
                adaTagihanYangBerlaku = true;
                if (keg == null || KegiatanPersistenceHelper
                        .hitungPersentaseLunasAktualDariDatabase(keg) < 0.1) {
                    return false;
                }
            }
            return adaTagihanYangBerlaku;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e,
                    "auto-audit(empty-catch) HistoryStatusMahasiswaUtil.adaKegiatanSyaratAktifLunasSemua");
            return false;
        }
    }

    /**
     * Menentukan apakah sebuah tagihan memang boleh memengaruhi status mahasiswa pada
     * semester yang sedang diperiksa.
     *
     * <p>Ada dua pagar yang wajib dilewati. Pertama, jenis kegiatan harus benar-benar
     * menyatakan {@link JenisKegiatan#getDigunakanSyaratKeaktifan()} = {@code true} menurut
     * aturan domain. Pemeriksaan ini tidak boleh hanya mempercayai keanggotaan pada cache
     * {@code CommonHelperClass.jenisKegiatansUntukSyaratAktif}. Data legacy banyak memiliki
     * nilai database {@code NULL}; {@code NULL} berarti tidak ikut menjadi syarat untuk hampir
     * semua jenis, bukan nilai {@code true}. Pagar di sini adalah pertahanan lapis kedua bila
     * cache lama, cache dari node aplikasi lain, atau proses hot-deploy masih membawa anggota
     * yang tidak valid.</p>
     *
     * <p>Kedua, tagihan daftar ulang mahasiswa baru hanya berlaku pada semester pertama.
     * Data lama pernah menyimpan tagihan tersebut pada Mahasiswa, bukan hanya pada
     * BiodataCalonMahasiswa. Tanpa pagar semester, mahasiswa lama dapat tetap Nonaktif walaupun
     * tagihan semester regulernya sudah lunas.</p>
     *
     * @param kegiatan kegiatan/tagihan yang akan dinilai
     * @param semester semester status yang sedang dihitung
     * @return {@code true} hanya bila kegiatan memang sah menjadi syarat aktif pada semester itu
     */
    public static boolean kegiatanSyaratAktifBerlaku(Kegiatan kegiatan, Integer semester) {
        if (kegiatan == null || kegiatan.getJenisKegiatan() == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(kegiatan.getJenisKegiatan().getDigunakanSyaratKeaktifan())) {
            return false;
        }
        if (semester != null && semester.intValue() > 1
                && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
                && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId() != null
                && kegiatan.getJenisKegiatan().getId() != null
                && ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
                        .equals(kegiatan.getJenisKegiatan().getId())) {
            return false;
        }
        return true;
    }

    /**
     * Menjelaskan penyebab paling mungkin dan dapat dibuktikan ketika status mahasiswa masih
     * Nonaktif. Method ini adalah pasangan diagnostik dari mesin keputusan
     * {@link #kalkulasiStatusLogikaLanjutan}: ia memakai daftar jenis kegiatan, pagar semester,
     * serta sumber pembayaran committed yang sama dengan kalkulasi status. Karena itu teks yang
     * ditampilkan UI bukan dugaan berdasarkan nama status saja.
     *
     * <p>Urutan analisis dibuat mengikuti kekuatan buktinya. NIM lama yang sudah dipindahkan
     * diperiksa pertama karena aturan tersebut langsung memaksa Nonaktif. Berikutnya seluruh
     * kegiatan syarat aktif semester ini diperiksa dan pembayaran aktualnya dibaca dari database.
     * Bila ada kegiatan dengan persentase di bawah {@code 0.1}, nama kegiatan disebut sebagai
     * penghambat. Bila tidak ada kegiatan syarat aktif, sistem menjelaskan bahwa tagihan pemicu
     * aktivasi belum tersedia. Bila pembayaran sudah memenuhi aturan tetapi KRS masih nol, KRS/SKS
     * disebut sebagai penyebab akademik yang perlu diperiksa. Terakhir, bila seluruh bukti
     * pembayaran dan KRS sudah baik tetapi status masih Nonaktif, hasil secara jujur menyatakan
     * bahwa status history belum tersinkron atau berasal dari penetapan akademik/manual.</p>
     *
     * <p>Method ini read-only. Ia hanya dipanggil untuk satu mahasiswa yang sedang ditampilkan,
     * bukan untuk tabel massal. Pembacaan cicilan segar sengaja dipertahankan agar keterangan tidak
     * menyebut "belum bayar" beberapa detik setelah pembayaran baru selesai di-commit.</p>
     *
     * @param krsMahasiswa konteks mahasiswa, semester, tahap, dan jumlah SKS yang dianalisis
     * @return alasan singkat tanpa tanda kurung; selalu aman ditampilkan setelah teks status
     */
    public static String analisisPenyebabNonaktif(KrsMahasiswa krsMahasiswa) {
        if (krsMahasiswa == null || krsMahasiswa.getMahasiswa() == null) {
            return "data mahasiswa atau semester belum lengkap";
        }

        Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
        Integer semester = krsMahasiswa.getSemester();
        try {
            String nimBaru = mahasiswa.getNimBaruPindah();
            if (nimBaru != null && !nimBaru.trim().isEmpty()) {
                return "data telah dipindahkan ke NIM baru " + nimBaru.trim();
            }

            if (CommonHelperClass.jenisKegiatansUntukSyaratAktif == null) {
                CommonHelperClass.reloadJenisKegiatans();
            }

            boolean adaTagihanSyaratAktif = false;
            List<String> tagihanBelumMemenuhi = new ArrayList<String>();
            List<Kegiatan> kegiatans = CommonHelperClass.jenisKegiatansUntukSyaratAktif == null
                    || CommonHelperClass.jenisKegiatansUntukSyaratAktif.isEmpty()
                            ? null
                            : mahasiswa.ambilKegiatans(semester,
                                    CommonHelperClass.jenisKegiatansUntukSyaratAktif, true);
            if (kegiatans != null) {
                for (Kegiatan kegiatan : kegiatans) {
                    if (!kegiatanSyaratAktifBerlaku(kegiatan, semester)) {
                        continue;
                    }
                    adaTagihanSyaratAktif = true;
                    double persen = KegiatanPersistenceHelper
                            .hitungPersentaseLunasAktualDariDatabase(kegiatan).doubleValue();
                    if (persen < 0.1) {
                        String nama = kegiatan.getJenisKegiatan().getNamaKegiatan();
                        if (nama == null || nama.trim().isEmpty()) {
                            nama = "tagihan syarat aktif";
                        }
                        if (!tagihanBelumMemenuhi.contains(nama)) {
                            tagihanBelumMemenuhi.add(nama);
                        }
                    }
                }
            }

            if (!tagihanBelumMemenuhi.isEmpty()) {
                return "belum ada pembayaran yang diakui untuk " + gabungkanAlasan(tagihanBelumMemenuhi);
            }

            boolean belumAdaKrs = krsMahasiswa.getSksBukanKonversi() == null
                    || krsMahasiswa.getSksBukanKonversi().intValue() <= 0;
            if (!adaTagihanSyaratAktif) {
                return belumAdaKrs
                        ? "belum ada tagihan syarat aktif dan belum ada pengambilan KRS/SKS semester ini"
                        : "belum ada tagihan syarat aktif yang dapat menjadi bukti aktivasi semester ini";
            }
            if (belumAdaKrs) {
                return "pembayaran wajib sudah memenuhi ketentuan, tetapi belum ada pengambilan KRS/SKS semester ini";
            }
            return "pembayaran wajib dan KRS sudah memenuhi ketentuan; status history belum tersinkron atau ditetapkan dari proses akademik/manual";
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e,
                    "auto-audit HistoryStatusMahasiswaUtil.analisisPenyebabNonaktif mahasiswa="
                            + mahasiswa.getId() + ", semester=" + semester);
            return "penyebab rinci belum dapat dibaca; periksa history status dan gunakan Refresh";
        }
    }

    private static String gabungkanAlasan(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(value);
        }
        return result.toString();
    }

    /**
     * Ambang semester minimal untuk dianggap "sudah waktunya lulus" bagi seorang mahasiswa:
     * memakai {@link Mahasiswa#getSemesterLulus()} bila diisi manual (override per-mahasiswa),
     * jika tidak jatuh ke {@code Jurusan.getJenjang().getJumlahSemester()} (standar jenjang), atau
     * {@code 99} (efektif "tidak pernah otomatis lulus/DO berdasarkan semester") bila jurusan/
     * jenjang tidak diketahui.
     *
     * @param mahasiswa mahasiswa yang dicek
     * @return jumlah semester minimal sebelum status terminal (Lulus/DO/Keluar) dievaluasi
     */
    private static Integer getJumlahSemester(Mahasiswa mahasiswa) {
        return mahasiswa.getSemesterLulus() != null ? mahasiswa.getSemesterLulus() :
                (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getJenjang() != null ? mahasiswa.getJurusan().getJenjang().getJumlahSemester() : 99);
    }

    /**
     * Dipanggil dari {@link #singkronisasiStatusMahasiswa} (jalur {@code nonAktifkan=true}) untuk
     * menyinkronkan status Nonaktif berbasis SKS yang benar-benar diambil pada satu semester,
     * sekaligus memelihara daftar {@link Mahasiswa#getBatasStudi()} (daftar semester "batas
     * studi" dipisah koma). Dua cabang: bila SKS yang diambil {@code <= 0} (mahasiswa tidak
     * mengambil KRS sama sekali semester itu), semester ditambahkan ke {@code batasStudi} (bila
     * belum ada) dan status history dipaksa {@code TIDAK_AKTIF}; sebaliknya bila SKS diambil
     * {@code > 0}, semester tsb DIHAPUS dari {@code batasStudi} (mahasiswa aktif kembali) dan
     * status history dipaksa {@code AKTIF} — tanpa melalui {@link #kalkulasiStatusLogikaLanjutan}
     * (jalur ini murni berbasis kehadiran SKS, bukan status pembayaran).
     * <p><b>Kuirk:</b> parameter {@code tx} dideklarasikan lokal ({@code Transaction tx = null;})
     * dan diteruskan ke {@link #updateViaTransactionQuietly}, tapi method itu SELALU membuat
     * transaksi barunya sendiri ({@code tx = session.beginTransaction()} pada parameter lokalnya)
     * — variabel {@code tx} di sini tidak pernah benar-benar dipakai/dibaca ulang. Bukan bug yang
     * mempengaruhi hasil (tiap update tetap dalam transaksi commit-nya sendiri), hanya kode mati/
     * membingungkan yang dipertahankan apa adanya (bukan diubah — lingkup tugas ini Javadoc saja).</p>
     *
     * @param session                 session Hibernate aktif dari loop pemanggil
     * @param mahasiswa               mahasiswa yang batas studinya dimutasi
     * @param semester                semester yang dievaluasi
     * @param krsMahasiswa            baris KRS semester tsb (sumber {@code getSksYangDiambil()})
     * @param historyStatusMahasiswa  baris history semester tsb yang statusnya dimutasi
     */
    private static void prosesNonAktifkanStatusSingkronisasi(Session session, Mahasiswa mahasiswa, int semester, KrsMahasiswa krsMahasiswa, HistoryStatusMahasiswa historyStatusMahasiswa) {
        Transaction tx = null;
        if (krsMahasiswa != null && krsMahasiswa.getSksYangDiambil() != null && krsMahasiswa.getSksYangDiambil() <= 0) {
            boolean ada = false;
            if (mahasiswa.getBatasStudi() != null) {
                for (String s : mahasiswa.getBatasStudi().split(",")) {
                    if (s.equalsIgnoreCase(String.valueOf(semester))) { ada = true; break; }
                }
            }
            if (!ada) {
                String currentBatas = mahasiswa.getBatasStudi() == null ? "" : mahasiswa.getBatasStudi();
                mahasiswa.setBatasStudi(currentBatas.isEmpty() ? String.valueOf(semester) : currentBatas + "," + semester);
                updateViaTransactionQuietly(session, tx, mahasiswa);
            }
            historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.TIDAK_AKTIF);
            updateViaTransactionQuietly(session, tx, historyStatusMahasiswa);
        } else {
            StringBuilder batasStudiBaru = new StringBuilder();
            if (mahasiswa.getBatasStudi() != null) {
                for (String s : mahasiswa.getBatasStudi().split(",")) {
                    if (!s.trim().isEmpty() && !s.equalsIgnoreCase(String.valueOf(semester))) {
                        if (batasStudiBaru.length() > 0) batasStudiBaru.append(",");
                        batasStudiBaru.append(s);
                    }
                }
            }
            if (mahasiswa.getBatasStudi() == null || !mahasiswa.getBatasStudi().equalsIgnoreCase(batasStudiBaru.toString())) {
                mahasiswa.setBatasStudi(batasStudiBaru.toString());
                updateViaTransactionQuietly(session, tx, mahasiswa);
            }
            historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.AKTIF);
            updateViaTransactionQuietly(session, tx, historyStatusMahasiswa);
        }
    }

    /**
     * Update satu entity dalam transaksi mandiri, menelan (bukan melempar) exception dan
     * melakukan rollback diam-diam bila gagal — dipakai untuk mutasi "best-effort" yang tidak
     * boleh menggagalkan proses pemanggil yang lebih besar (mis. loop sinkronisasi banyak
     * semester di {@link #singkronisasiStatusMahasiswa}). Parameter {@code tx} yang diterima dari
     * pemanggil DIABAIKAN isinya dan ditimpa dengan transaksi baru milik method ini sendiri —
     * lihat catatan kuirk di {@link #prosesNonAktifkanStatusSingkronisasi}.
     *
     * @param session session Hibernate aktif
     * @param tx      diterima tapi selalu ditimpa transaksi baru lokal (lihat catatan kuirk di atas)
     * @param entity  entity yang akan di-refresh lalu diupdate ({@code Common.refreshUpdate})
     */
    private static void updateViaTransactionQuietly(Session session, Transaction tx, GeneralValueObject entity) {
        try {
            tx = session.beginTransaction();
            Common.refreshUpdate(session, entity);
            tx.commit();
        } catch (Exception e) {
            rollbackQuietly(tx);
        }
    }

    /**
     * Sisi TULIS dari cache JSON lapis-2 yang dijelaskan di Javadoc class-level: menyerialisasi
     * {@code history} ke JSON (mengecualikan properti bertipe {@link Mahasiswa} dan
     * {@link StatusMahasiswa} dari deep-serialization — lihat parameter exclude
     * {@code Common.convertToJsonObject}, mencegah cache membengkak dengan graph entity penuh),
     * menyimpannya ke {@code jsonObject} di bawah {@code key}, lalu mempersistenkannya per-
     * mahasiswa lewat {@code Common.setJSONTemporary}. Kegagalan (mis. properti tak bisa
     * diserialisasi) ditelan dan dicatat ke {@code ErrorAuditUtil} — cache yang gagal ditulis
     * tidak menggagalkan alur utama pemanggil.
     *
     * @param mahasiswa  pemilik cache
     * @param key        kunci entri dalam {@code jsonObject}
     * @param jsonObject objek JSON cache milik mahasiswa ini, dimutasi langsung
     * @param history    baris status yang akan diserialisasi ke cache
     */
    private static void simpanKeCache(Mahasiswa mahasiswa, String key, JSONObject jsonObject, HistoryStatusMahasiswa history) {
        try {
            jsonObject.put(key, Common.convertToJsonObject(history, Mahasiswa.class.getName(), StatusMahasiswa.class.getName()));
            Common.setJSONTemporary(mahasiswa, key, jsonObject);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:599");}
    }

    /**
     * Upsert satu baris {@link StatusMahasiswa} kanonik berdasarkan pencarian nama (bukan id
     * tetap) — dicari dengan {@code ilike} awalan ({@link MatchMode#START}) pada {@code
     * namaLengkap}, opsional di-OR dengan {@code orNames[0]} (dipakai untuk "Nonaktif" yang juga
     * harus mencocokkan data lama berlabel "NON-AKTIF"), diambil satu yang id-nya terkecil
     * ({@code Order.asc("id")}). Bila tidak ditemukan, baris baru dibuat. {@code kodeEpsbed} HANYA
     * ditulis bila berbeda dari nilai saat ini (menghindari dirty-update tanpa perubahan nyata);
     * {@code nama} selalu ditimpa ke {@code namaLengkap} kanonik.
     *
     * @param session    session Hibernate aktif (dan transaksi dari pemanggil {@link #initDataStatusMahasiswa})
     * @param namaLengkap nama kanonik yang disimpan/dicari (mis. "Aktif")
     * @param kodeEpsbed  kode EPSBED satu huruf (mis. "A") yang disinkronkan bila berbeda
     * @param orNames     nama alternatif opsional untuk turut dicocokkan saat pencarian (hanya elemen pertama dipakai)
     */
    private static void initStatusHelper(Session session, String namaLengkap, String kodeEpsbed, String... orNames) {
        org.hibernate.criterion.Criterion nameCrit = Restrictions.ilike("nama", namaLengkap, MatchMode.START);
        if (orNames.length > 0) {
            nameCrit = Restrictions.or(nameCrit, Restrictions.ilike("nama", orNames[0], MatchMode.START));
        }

        StatusMahasiswa status = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
                .add(nameCrit).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();

        if (status == null) status = new StatusMahasiswa();
        if (!kodeEpsbed.equals(status.getKodeEpsbed())) status.setKodeEpsbed(kodeEpsbed);
        status.setNama(namaLengkap);
        session.saveOrUpdate(status);
    }

    /** Rollback aman: hanya rollback bila transaksi non-null dan masih aktif; kegagalan rollback itu sendiri ditelan dan dicatat, tidak dilempar ulang. */
    private static void rollbackQuietly(Transaction tx) {
        if (tx != null && tx.isActive()) {
            try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:619");}
        }
    }

    /**
     * Penutupan session Hibernate tiga langkah, masing-masing dibungkus try-catch terpisah
     * (kegagalan satu langkah tidak menghalangi langkah berikutnya): {@code clear()} lalu
     * {@code disconnect()} lalu {@code close()}, masing-masing dijaga cek {@code isOpen()} bila
     * relevan. Urutan {@code disconnect()} sebelum {@code close()} adalah pola lama untuk
     * melepaskan koneksi JDBC ke connection pool lebih awal; pada versi Hibernate yang dipakai di
     * sini {@code close()} sendiri sudah cukup, jadi {@code disconnect()} eksplisit sedikit
     * redundan tapi tidak berbahaya — dipertahankan apa adanya (bukan bagian tugas Javadoc ini
     * untuk mengubah perilaku).
     *
     * @param session session yang akan ditutup; aman dipanggil dengan {@code null}
     */
    private static void closeSession(Session session) {
        if (session != null) {
            try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:625");}
            try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:626");}
            try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:627");}
        }
    }
}
