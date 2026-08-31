package ais.action.master.helper;

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
 * Tipe khusus untuk history status mahasiswa util. Kelas ini memberi nama dan batas tanggung jawab
 * yang eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code initDataStatusMahasiswa()}, {@code
 * initStatusHelper()}); pembacaan/pencarian ({@code getHistoryStatusMahasiswa()}, {@code
 * getHistoryStatusMahasiswa()}, {@code getJumlahSemester()}); validasi/perhitungan ({@code checkStatus()},
 * {@code cekDanUpdateCacheStatus()}, {@code cekPembayaranMahasiswa()}); mutasi data ({@code
 * updateSksBukanKonversi()}, {@code prosesNonAktifkanStatusSingkronisasi()}, {@code
 * updateViaTransactionQuietly()}, {@code simpanKeCache()}); operasi domain lain ({@code currentStatusSp()},
 * {@code currentStatus()}, {@code currentStatusSp()}, {@code currentStatus()}, {@code currentStatus()}, {@code
 * currentStatus()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class HistoryStatusMahasiswaUtil {

    // ========================================================================
    // PUBLIC METHODS (OVERLOADED CURRENT STATUS)
    // ========================================================================

    public static HistoryStatusMahasiswa currentStatusSp(Mahasiswa mahasiswa, Integer sp) {
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(),
                mahasiswa.currentTahapan(), sp));
    }

    public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa, Integer tahap) {
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(), tahap, null));
    }

    public static HistoryStatusMahasiswa currentStatusSp(Mahasiswa mahasiswa, String tahunAkademik, Integer semester, Integer sp) {
        Integer tahap = mahasiswa == null ? null : mahasiswa.currentTahapan();
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahap, sp));
    }

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

    public static HistoryStatusMahasiswa currentStatus(Mahasiswa mahasiswa) {
        return currentStatus(Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, mahasiswa.currentSemester(), mahasiswa.currentTahapan(), null));
    }

    public static HistoryStatusMahasiswa currentStatus(KrsMahasiswa krsMahasiswa) {
        return currentStatus(krsMahasiswa, false);
    }

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

    public static HistoryStatusMahasiswa getHistoryStatusMahasiswa(KrsMahasiswa krsMahasiswa) {
        return getHistoryStatusMahasiswa(krsMahasiswa, false);
    }

    // ========================================================================
    // CORE LOGIC: GET HISTORY STATUS MAHASISWA
    // ========================================================================

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

    public static void singkronisasiStatusMahasiswaNotTimer(final Mahasiswa mahasiswa) {
        HistoryStatusMahasiswaUtil.singkronisasiStatusMahasiswa(null, mahasiswa, Common.getCurrentTahunAkademik(), Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP, false);
    }

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

    private static HistoryStatusMahasiswa createDefaultStatus(KrsMahasiswa krsMahasiswa, Mahasiswa mahasiswa, Integer semester, Integer tahap) {
        HistoryStatusMahasiswa history = new HistoryStatusMahasiswa(krsMahasiswa == null ? null : krsMahasiswa.getSemesterPendek());
        history.setStatusMahasiswa(ConstantValues.AKTIF);
        history.setMahasiswa(mahasiswa);
        history.setSemester(semester);
        history.setTahap(tahap);
        return history;
    }

    private static boolean isStatusEqual(StatusMahasiswa current, StatusMahasiswa constant) {
        return current != null && constant != null && current.getId() != null && current.getId().equals(constant.getId());
    }

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

    private static HistoryStatusMahasiswa fetchHistoryFromDb(Session session, Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa, Integer semester, Integer tahap) {
        return (HistoryStatusMahasiswa) session.createCriteria(HistoryStatusMahasiswa.class)
                .add(Restrictions.eq("mahasiswa", mahasiswa))
                .add(krsMahasiswa != null && Perkuliahan.SEMESTER_PENDEK.equals(krsMahasiswa.getSemesterPendek())
                        ? Restrictions.eq("sp", krsMahasiswa.getSemesterPendek()) : Restrictions.isNull("sp"))
                .add(tahap == null || tahap.equals(0) ? Restrictions.eq("semester", semester) : Restrictions.eq("tahap", tahap))
                .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
    }

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
                    check &= (keg != null && keg.hitungPersentaseLunasAktual() >= 0.1);
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
                if (keg == null || keg.hitungPersentaseLunasAktual() < 0.1) {
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
     * semester yang sedang diperiksa. Tagihan daftar ulang mahasiswa baru hanya berlaku
     * pada semester pertama. Data lama pernah menyimpan tagihan tersebut pada Mahasiswa
     * (bukan hanya BiodataCalonMahasiswa), sehingga tanpa pagar ini mahasiswa lama dapat
     * tetap Nonaktif walaupun tagihan semester regulernya sudah lunas.
     */
    public static boolean kegiatanSyaratAktifBerlaku(Kegiatan kegiatan, Integer semester) {
        if (kegiatan == null || kegiatan.getJenisKegiatan() == null) {
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

    private static Integer getJumlahSemester(Mahasiswa mahasiswa) {
        return mahasiswa.getSemesterLulus() != null ? mahasiswa.getSemesterLulus() :
                (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getJenjang() != null ? mahasiswa.getJurusan().getJenjang().getJumlahSemester() : 99);
    }

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

    private static void updateViaTransactionQuietly(Session session, Transaction tx, GeneralValueObject entity) {
        try {
            tx = session.beginTransaction();
            Common.refreshUpdate(session, entity);
            tx.commit();
        } catch (Exception e) {
            rollbackQuietly(tx);
        }
    }

    private static void simpanKeCache(Mahasiswa mahasiswa, String key, JSONObject jsonObject, HistoryStatusMahasiswa history) {
        try {
            jsonObject.put(key, Common.convertToJsonObject(history, Mahasiswa.class.getName(), StatusMahasiswa.class.getName()));
            Common.setJSONTemporary(mahasiswa, key, jsonObject);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:599");}
    }

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

    private static void rollbackQuietly(Transaction tx) {
        if (tx != null && tx.isActive()) {
            try { tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:619");}
        }
    }

    private static void closeSession(Session session) {
        if (session != null) {
            try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:625");}
            try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:626");}
            try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/HistoryStatusMahasiswaUtil.java:627");}
        }
    }
}
