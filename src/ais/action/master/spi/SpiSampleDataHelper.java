package ais.action.master.spi;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.spi.ChecklistAuditSPI;
import ais.database.model.spi.JenisAuditSPI;
import ais.database.model.spi.KriteriaAuditSPI;
import ais.database.model.spi.PenugasanAuditSPI;
import ais.database.model.spi.ProfilRisikoSPI;
import ais.database.model.spi.RencanaAuditTahunanSPI;
import ais.database.model.spi.TemuanAuditSPI;
import ais.database.model.spi.TimAuditSPI;
import ais.database.model.spi.TindakLanjutAuditSPI;

/**
 * <h2>SpiSampleDataHelper &mdash; Data Contoh Modul Satuan Pengawasan Internal (SPI)</h2>
 *
 * <p>
 * Kelas ini mengisi SEMBILAN tabel modul SPI (Bagian A/B/C sekaligus) dengan data contoh yang
 * realistis pada saat aplikasi dijalankan, agar staf yang membuka modul ini untuk pertama kali
 * langsung melihat tampilan yang terisi di setiap layar &mdash; bukan hanya sebagian layar terisi
 * sementara yang lain kosong. Data yang dibuat: 3 kategori audit lengkap dengan kriteria &amp;
 * checklist-nya (Bagian A: {@link JenisAuditSPI}/{@link KriteriaAuditSPI}/{@link ChecklistAuditSPI}),
 * profil risiko &amp; rencana audit tahunan untuk beberapa unit kerja (Bagian B:
 * {@link ProfilRisikoSPI}/{@link RencanaAuditTahunanSPI}), serta beberapa penugasan audit contoh
 * lengkap dengan tim, temuan dengan variasi klasifikasi, dan tindak lanjut dengan variasi status
 * (Bagian C: {@link PenugasanAuditSPI}/{@link TimAuditSPI}/{@link TemuanAuditSPI}/
 * {@link TindakLanjutAuditSPI}) &mdash; sembilan tabel tercakup semuanya.
 * </p>
 *
 * <h3>Idempoten PER BAGIAN, bukan satu gerbang tunggal untuk semuanya</h3>
 * <p>
 * Versi awal kelas ini memakai SATU pemeriksaan tunggal ("apakah {@link JenisAuditSPI} sudah ada")
 * untuk memutuskan apakah SELURUH proses seeding (Bagian A+B+C sekaligus) perlu dijalankan. Itu
 * ternyata rapuh: bila proses sempat berhenti di tengah jalan (mis. Bagian A berhasil namun Bagian
 * C gagal), pemeriksaan tunggal itu akan terus melihat "sudah ada JenisAuditSPI" pada setiap
 * restart berikutnya dan SELAMANYA melewatkan Bagian B/C yang sebenarnya masih kosong. Karena itu
 * kelas ini sekarang memeriksa &amp; men-seed TIGA bagian secara independen
 * ({@link #ensureBagianA()}, {@link #ensureBagianB()}, {@link #ensureBagianC()}), masing-masing
 * dengan gerbang idempotensi dan sesi/transaksi Hibernate-nya SENDIRI &mdash; bila satu bagian
 * gagal, bagian lain tetap berhasil, dan bagian yang gagal akan otomatis dicoba lagi pada restart
 * berikutnya tanpa mengulang bagian yang sudah selesai.
 * </p>
 *
 * <h3>Tidak terikat pada isi hardcode Bagian A sendiri</h3>
 * <p>
 * Bagian B dan C SENGAJA membaca {@link JenisAuditSPI}/{@link KriteriaAuditSPI}/
 * {@link ChecklistAuditSPI} yang ADA APA ADANYA di database (lewat query biasa, BUKAN meneruskan
 * referensi Java dari Bagian A) &mdash; baik itu hasil seeding Bagian A MAUPUN data asli yang sudah
 * diisi staf SPI secara manual sebelum seeder ini sempat berjalan. Dengan begitu, sekalipun Bagian
 * A dilewati karena staf sudah lebih dulu mengisi Jenis Audit sendiri, Bagian B/C tetap bisa
 * berjalan memakai data yang staf isikan tersebut &mdash; teks temuan pun dihasilkan secara
 * DINAMIS dari nama checklist yang benar-benar ada (lihat {@link #teksTemuan(String, String)}),
 * bukan narasi tetap yang hanya cocok untuk contoh checklist bawaan Bagian A.
 * </p>
 *
 * <h3>Kenapa query-ulang di Bagian C sekarang aman (dulu SEMPAT tidak aman)</h3>
 * <p>
 * Versi awal Bagian C sempat menyebabkan {@code SessionException: Session is closed!} karena
 * meng-query ulang baris {@link KriteriaAuditSPI}/{@link ChecklistAuditSPI} yang BARU SAJA disimpan
 * pada session/transaksi yang SAMA (masih berstatus "pending insert", belum commit) &mdash;
 * kombinasi itu bentrok dengan {@code EntityIdentityMap} (satu objek per kelas+id se-JVM) yang
 * dipakai {@code AuditTimestampInterceptor}. Sekarang karena Bagian A, B, dan C masing-masing
 * commit &amp; menutup session-nya SENDIRI sebelum bagian berikutnya mulai, query di Bagian B/C
 * SELALU membaca data yang SUDAH commit secara tuntas ke database &mdash; tidak ada lagi baris
 * "pending" yang di-query ulang pada session yang sama, sehingga masalah itu tidak dapat terulang.
 * </p>
 *
 * <h3>Auditee &amp; anggota tim diambil dari data yang SUDAH ADA, tidak membuat data palsu</h3>
 * <p>
 * Unit kerja (auditee) dan anggota tim audit pada data contoh ini SENGAJA diambil dari baris
 * {@link SatuanKerja} dan {@link Tbmuser} yang SUDAH ADA di database &mdash; bukan membuat unit
 * organisasi atau pengguna baru yang tidak sungguhan. Bila instalasi masih benar-benar kosong
 * (belum ada {@link SatuanKerja}/{@link Tbmuser}/{@link JenisAuditSPI} sama sekali), bagian yang
 * bergantung pada data tersebut otomatis dilewati dengan aman dan dicatat di log server, TANPA
 * menggagalkan startup aplikasi.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class SpiSampleDataHelper {

    private static final String PENANDA = "Sistem (Data Contoh)";

    /**
     * Titik masuk utama, dipanggil sekali dari {@code AppStartupListener} setiap kali aplikasi
     * dijalankan. Aman dipanggil berkali-kali — setiap bagian memeriksa idempotensinya sendiri
     * (lihat javadoc kelas).
     */
    public static void ensureSampleData() {
        ensureBagianA();
        ensureBagianB();
        ensureBagianC();
    }

    // =====================================================================
    // Bagian A — Jenis / Kriteria / Checklist
    // =====================================================================

    private static void ensureBagianA() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.openSession();
            if (hitung(session, JenisAuditSPI.class) > 0) {
                return;
            }

            tx = session.beginTransaction();

            JenisAuditSPI keuangan = jenis(session, "AK", "Audit Keuangan",
                    "Pemeriksaan atas pengelolaan kas, bank, dan pertanggungjawaban keuangan unit kerja.");
            kriteriaDanChecklist(session, keuangan, 1, "Kepatuhan Pengelolaan Kas Kecil",
                    "Mengacu pada SOP pengelolaan kas kecil internal lembaga.",
                    new String[]{
                            "saldo kas kecil fisik sesuai dengan catatan pembukuan",
                            "setiap pengeluaran kas kecil memiliki otorisasi berjenjang sesuai nilai nominalnya",
                            "kas kecil direkonsiliasi setiap akhir bulan"
                    });
            kriteriaDanChecklist(session, keuangan, 2, "Rekonsiliasi Bank Bulanan",
                    "Mengacu pada praktik pengendalian internal keuangan standar.",
                    new String[]{
                            "rekonsiliasi bank dilakukan tepat waktu setiap bulan",
                            "selisih rekonsiliasi ditindaklanjuti dan didokumentasikan"
                    });

            JenisAuditSPI pengadaan = jenis(session, "APBJ", "Audit Pengadaan Barang/Jasa",
                    "Pemeriksaan atas proses pengadaan barang/jasa, mulai dari perencanaan sampai serah terima.");
            kriteriaDanChecklist(session, pengadaan, 1, "Kepatuhan Prosedur Pengadaan",
                    "Mengacu pada SOP pengadaan barang/jasa internal lembaga.",
                    new String[]{
                            "pengadaan di atas nilai tertentu melalui proses tender/lelang yang benar",
                            "dokumen kontrak dan berita acara serah terima lengkap"
                    });
            kriteriaDanChecklist(session, pengadaan, 2, "Evaluasi Vendor/Penyedia",
                    "Mengacu pada kebijakan manajemen vendor lembaga.",
                    new String[]{
                            "vendor terdaftar dan terverifikasi sebelum kontrak ditandatangani",
                            "evaluasi kinerja vendor didokumentasikan setelah pekerjaan selesai"
                    });

            JenisAuditSPI sdm = jenis(session, "SDM", "Audit Sumber Daya Manusia",
                    "Pemeriksaan atas administrasi kepegawaian, penggajian, dan kehadiran.");
            kriteriaDanChecklist(session, sdm, 1, "Kepatuhan Administrasi Kepegawaian",
                    "Mengacu pada peraturan kepegawaian internal lembaga.",
                    new String[]{
                            "dokumen kepegawaian pegawai baru lengkap",
                            "pembayaran gaji sesuai dengan SK dan masa kerja pegawai"
                    });
            kriteriaDanChecklist(session, sdm, 2, "Pengelolaan Cuti dan Kehadiran",
                    "Mengacu pada SOP pengajuan cuti dan rekap kehadiran.",
                    new String[]{
                            "pengajuan cuti melalui prosedur dan persetujuan yang benar",
                            "rekap kehadiran sesuai dengan data absensi aktual"
                    });

            tx.commit();
            System.out.println("[SpiSampleDataHelper] Bagian A (Jenis/Kriteria/Checklist) berhasil dibuat.");
        } catch (Exception e) {
            gagal("Bagian A", tx, e);
        } finally {
            tutup(session);
        }
    }

    private static JenisAuditSPI jenis(Session session, String kode, String nama, String keterangan) {
        JenisAuditSPI j = new JenisAuditSPI();
        j.setOleh(PENANDA);
        j.setKode(kode);
        j.setNama(nama);
        j.setKeterangan(keterangan);
        j.setAktif(true);
        session.save(j);
        return j;
    }

    private static void kriteriaDanChecklist(Session session, JenisAuditSPI jenis, int urut,
            String namaKriteria, String keteranganKriteria, String[] checklistItems) {
        KriteriaAuditSPI k = new KriteriaAuditSPI();
        k.setOleh(PENANDA);
        k.setJenisAuditSPI(jenis);
        k.setNomorUrut(urut);
        k.setNama(namaKriteria);
        k.setKeterangan(keteranganKriteria);
        k.setAktif(true);
        session.save(k);

        int i = 1;
        for (String item : checklistItems) {
            ChecklistAuditSPI c = new ChecklistAuditSPI();
            c.setOleh(PENANDA);
            c.setKriteriaAuditSPI(k);
            c.setNomorUrut(i++);
            c.setNama("Periksa apakah " + item);
            c.setAktif(true);
            session.save(c);
        }
    }

    // =====================================================================
    // Bagian B — Profil Risiko & Rencana Audit Tahunan
    // =====================================================================

    private static void ensureBagianB() {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.openSession();
            if (hitung(session, ProfilRisikoSPI.class) > 0) {
                return;
            }

            List<SatuanKerja> unit = ambilSatuanKerjaContoh(session);
            List<JenisAuditSPI> jenisList = ambilJenisAuditSPIContoh(session);
            if (unit.isEmpty() || jenisList.isEmpty()) {
                System.out.println("[SpiSampleDataHelper] Bagian B dilewati: belum ada Unit Kerja/Jenis Audit di database.");
                return;
            }

            tx = session.beginTransaction();
            int tahun = Calendar.getInstance().get(Calendar.YEAR);

            ProfilRisikoSPI p1 = profilRisiko(session, unit.get(0), tahun, 5, 5, 4, 4, 4,
                    "Unit dengan anggaran besar dan riwayat temuan berulang — prioritas utama PKPT tahun ini.");
            ProfilRisikoSPI p2 = unit.size() > 1
                    ? profilRisiko(session, unit.get(1), tahun, 3, 3, 2, 2, 2, "Risiko sedang, perlu pemantauan rutin.")
                    : null;
            if (unit.size() > 2) {
                profilRisiko(session, unit.get(2), tahun, 1, 2, 1, 1, 1, "Riwayat kepatuhan baik, risiko rendah.");
            }

            RencanaAuditTahunanSPI r1 = new RencanaAuditTahunanSPI();
            r1.setOleh(PENANDA);
            r1.setTahun(tahun);
            r1.setSatuanKerja(unit.get(0));
            r1.setJenisAuditSPI(jenisList.get(0));
            r1.setProfilRisikoSPI(p1);
            r1.setTriwulanRencana(1);
            r1.setJenisPenugasan(RencanaAuditTahunanSPI.REGULER);
            r1.setStatusRealisasi(RencanaAuditTahunanSPI.SEDANG_BERJALAN);
            r1.setKeterangan("Prioritas tertinggi berdasarkan hasil penilaian risiko.");
            r1.setAktif(true);
            session.save(r1);

            if (p2 != null) {
                RencanaAuditTahunanSPI r2 = new RencanaAuditTahunanSPI();
                r2.setOleh(PENANDA);
                r2.setTahun(tahun);
                r2.setSatuanKerja(unit.get(1));
                r2.setJenisAuditSPI(jenisList.size() > 1 ? jenisList.get(1) : jenisList.get(0));
                r2.setProfilRisikoSPI(p2);
                r2.setTriwulanRencana(2);
                r2.setJenisPenugasan(RencanaAuditTahunanSPI.REGULER);
                r2.setStatusRealisasi(RencanaAuditTahunanSPI.BELUM_DILAKSANAKAN);
                r2.setKeterangan("Terjadwal triwulan kedua sesuai PKPT.");
                r2.setAktif(true);
                session.save(r2);
            }

            RencanaAuditTahunanSPI r3 = new RencanaAuditTahunanSPI();
            r3.setOleh(PENANDA);
            r3.setTahun(tahun);
            r3.setSatuanKerja(unit.get(0));
            r3.setJenisAuditSPI(jenisList.size() > 2 ? jenisList.get(2) : jenisList.get(0));
            r3.setTriwulanRencana(3);
            r3.setJenisPenugasan(RencanaAuditTahunanSPI.KHUSUS);
            r3.setStatusRealisasi(RencanaAuditTahunanSPI.BELUM_DILAKSANAKAN);
            r3.setKeterangan("Audit khusus atas permintaan pimpinan, di luar siklus reguler.");
            r3.setAktif(true);
            session.save(r3);

            tx.commit();
            System.out.println("[SpiSampleDataHelper] Bagian B (Profil Risiko/Rencana Audit Tahunan) berhasil dibuat.");
        } catch (Exception e) {
            gagal("Bagian B", tx, e);
        } finally {
            tutup(session);
        }
    }

    private static ProfilRisikoSPI profilRisiko(Session session, SatuanKerja unit, int tahun,
            int materialitas, int dampak, int pengendalian, int temuanSebelumnya, int lamaTidakDiaudit,
            String catatan) {
        ProfilRisikoSPI p = new ProfilRisikoSPI();
        p.setOleh(PENANDA);
        p.setSatuanKerja(unit);
        p.setTahun(tahun);
        p.setSkorMaterialitas(materialitas);
        p.setSkorDampakOperasional(dampak);
        p.setSkorKualitasPengendalian(pengendalian);
        p.setSkorTemuanSebelumnya(temuanSebelumnya);
        p.setSkorLamaTidakDiaudit(lamaTidakDiaudit);
        p.setCatatan(catatan);
        p.setAktif(true);
        session.save(p);
        return p;
    }

    // =====================================================================
    // Bagian C — Penugasan, Tim, Temuan, Tindak Lanjut
    // =====================================================================

    /**
     * Menyusun DUA penugasan contoh, masing-masing dalam sesi/transaksi Hibernate-nya SENDIRI
     * (lihat {@link #buatSatuPenugasanContoh(int, Long[], String[], String[], boolean)}).
     *
     * <p>
     * Versi awal method ini membuat KEDUA penugasan dalam SATU sesi/transaksi yang sama. Itu
     * ternyata masih memicu {@code SessionException: Session is closed!} yang sama seperti bug
     * sebelumnya (lihat javadoc kelas) &mdash; kali ini bukan karena meng-query ulang baris yang
     * baru disimpan pada bagian YANG SAMA, melainkan karena baris {@link PenugasanAuditSPI}/
     * {@link TimAuditSPI}/{@link TemuanAuditSPI}/{@link TindakLanjutAuditSPI} milik penugasan
     * PERTAMA masih berstatus "pending insert" (belum commit) di persistence context saat
     * penugasan KEDUA memicu query baru (auto-flush ikut membereskan sisa milik penugasan
     * pertama, lalu bentrok dengan {@code EntityIdentityMap} yang sama seperti sebelumnya).
     * Solusinya sama: pisahkan sepenuhnya jadi transaksi independen per penugasan, bukan hanya
     * per Bagian.
     * </p>
     */
    private static void ensureBagianC() {
        // PENTING: hanya meneruskan ID, BUKAN entity JenisAuditSPI itu sendiri. cekSession
        // ditutup di finally sebelum buatSatuPenugasanContoh dipanggil, sehingga entity yang
        // dimuat lewat cekSession jadi DETACHED. AuditTimestampInterceptor.instantiate()
        // (lihat ais/database/hibernate/AuditTimestampInterceptor.java) mengembalikan SATU
        // instance canonical per kelas+id dari EntityIdentityMap yang dipakai bersama oleh
        // SEMUA session di JVM ini -- meneruskan objek detached tsb ke session BARU yang dibuka
        // buatSatuPenugasanContoh (dipakai lewat Restrictions.eq/setter relasi many-to-one) bisa
        // membuat dirty-check/auto-flush session baru itu keliru merujuk balik ke cekSession yang
        // sudah tertutup -> org.hibernate.SessionException: "Session is closed!" saat query
        // pertama di buatPenugasanContoh. Dengan hanya membawa Long id, tiap panggilan
        // buatSatuPenugasanContoh WAJIB mengambil ulang JenisAuditSPI-nya sendiri lewat
        // session.get(...) pada session miliknya sendiri -- entity yang dipakai selalu terikat
        // ke session yang sedang aktif, tidak pernah "menyeberang" dari session yang sudah tutup.
        Long[] jenisIds;
        Session cekSession = null;
        try {
            cekSession = HibernateUtil.openSession();
            List<JenisAuditSPI> jenisList = ambilJenisAuditSPIContoh(cekSession);
            jenisIds = new Long[jenisList.size()];
            for (int i = 0; i < jenisList.size(); i++) {
                jenisIds[i] = jenisList.get(i).getId();
            }
        } finally {
            tutup(cekSession);
        }

        if (jenisIds.length == 0) {
            System.out.println("[SpiSampleDataHelper] Bagian C dilewati: belum ada Jenis Audit di database.");
            return;
        }

        // Penugasan #1 — hasil audit sudah disetujui, klasifikasi ringan (Minor/Sesuai/Observasi)
        buatSatuPenugasanContoh(0, jenisIds,
                new String[]{TemuanAuditSPI.MINOR, TemuanAuditSPI.SESUAI, TemuanAuditSPI.OBSERVASI},
                new String[]{TindakLanjutAuditSPI.SEDANG_BERJALAN, null, TindakLanjutAuditSPI.BELUM_DIMULAI},
                true);

        // Penugasan #2 — masih diajukan, klasifikasi berat (Kritis/Mayor) untuk variasi dasbor
        if (jenisIds.length > 1) {
            buatSatuPenugasanContoh(1, jenisIds,
                    new String[]{TemuanAuditSPI.KRITIS, TemuanAuditSPI.MAYOR},
                    new String[]{TindakLanjutAuditSPI.TERLAMBAT, TindakLanjutAuditSPI.SELESAI},
                    false);
        }
    }

    /**
     * Membuat SATU {@link PenugasanAuditSPI} contoh dalam sesi/transaksi Hibernate yang
     * SEPENUHNYA independen (dibuka &amp; ditutup di dalam method ini sendiri) &mdash; idempoten
     * sendiri (memeriksa apakah penugasan untuk kombinasi unit+jenis ini sudah ada) sehingga
     * gagal pada penugasan #2 tidak pernah menggagalkan/mengulang penugasan #1 yang sudah
     * berhasil, dan sebaliknya.
     */
    @SuppressWarnings("unchecked")
    private static void buatSatuPenugasanContoh(int index, Long[] jenisIds,
            String[] klasifikasiUrut, String[] statusTlUrut, boolean disetujui) {
        Session session = null;
        Transaction tx = null;
        try {
            session = HibernateUtil.openSession();

            // Ambil ulang JenisAuditSPI SEGAR lewat session ini sendiri (lihat komentar di
            // ensureBagianC) -- JANGAN pakai entity yang dimuat lewat session lain yang sudah
            // ditutup, supaya tidak ada objek "menyeberang" session yang memicu
            // SessionException: "Session is closed!" saat dirty-check/auto-flush.
            JenisAuditSPI jenis = (JenisAuditSPI) session.get(JenisAuditSPI.class,
                    jenisIds[index % jenisIds.length]);
            if (jenis == null) {
                System.out.println("[SpiSampleDataHelper] Penugasan contoh #" + (index + 1) + " dilewati: Jenis Audit SPI sudah tidak ada di database.");
                return;
            }
            List<SatuanKerja> unit = ambilSatuanKerjaContoh(session);
            List<Tbmuser> user = ambilTbmuserContoh(session);
            if (unit.isEmpty()) {
                System.out.println("[SpiSampleDataHelper] Penugasan contoh #" + (index + 1) + " dilewati: belum ada Unit Kerja di database.");
                return;
            }
            SatuanKerja satuanKerja = unit.get(index % unit.size());

            long sudahAda = hitung(session, PenugasanAuditSPI.class,
                    Restrictions.eq("satuanKerja", satuanKerja), Restrictions.eq("jenisAuditSPI", jenis));
            if (sudahAda > 0) {
                return;
            }

            tx = session.beginTransaction();
            boolean dibuat = buatPenugasanContoh(session, jenis, satuanKerja, user, index,
                    klasifikasiUrut, statusTlUrut, disetujui);
            tx.commit();

            if (dibuat) {
                System.out.println("[SpiSampleDataHelper] Penugasan contoh #" + (index + 1) + " berhasil dibuat.");
            }
        } catch (Exception e) {
            gagal("Bagian C (penugasan #" + (index + 1) + ")", tx, e);
        } finally {
            tutup(session);
        }
    }

    /**
     * Membuat satu {@link PenugasanAuditSPI} contoh lengkap: tim (bila ada pengguna tersedia),
     * dan satu {@link TemuanAuditSPI} per checklist pertama yang ditemukan di bawah {@code jenis}
     * (mengikuti urutan {@code klasifikasiUrut}), masing-masing dengan {@link TindakLanjutAuditSPI}
     * bila {@code statusTlUrut} pada indeks yang sama tidak {@code null}.
     *
     * @return {@code true} bila penugasan benar-benar dibuat, {@code false} bila dilewati karena
     *         belum ada checklist di bawah {@code jenis}
     */
    private static boolean buatPenugasanContoh(Session session, JenisAuditSPI jenis, SatuanKerja unit,
            List<Tbmuser> user, int userOffset, String[] klasifikasiUrut, String[] statusTlUrut,
            boolean disetujui) {

        List<KriteriaAuditSPI> kriteriaList = session.createCriteria(KriteriaAuditSPI.class)
                .add(Restrictions.eq("jenisAuditSPI", jenis))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nomorUrut"))
                .setMaxResults(1)
                .list();
        if (kriteriaList.isEmpty()) {
            return false;
        }

        List<ChecklistAuditSPI> checklistList = session.createCriteria(ChecklistAuditSPI.class)
                .add(Restrictions.eq("kriteriaAuditSPI", kriteriaList.get(0)))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("nomorUrut"))
                .setMaxResults(klasifikasiUrut.length)
                .list();
        if (checklistList.isEmpty()) {
            return false;
        }

        PenugasanAuditSPI penugasan = new PenugasanAuditSPI();
        penugasan.setOleh(PENANDA);
        penugasan.setJenisAuditSPI(jenis);
        penugasan.setSatuanKerja(unit);
        penugasan.setNama(jenis.getNama() + " — " + unit.getNama() + " (Contoh Penugasan)");
        penugasan.setKeterangan("Data contoh untuk memperlihatkan alur pelaksanaan audit secara lengkap.");
        penugasan.setTanggalMulai(ais.ui.util.WaktuUtil.getDate());
        Tbmuser dibuatOleh = user.isEmpty() ? null : user.get(userOffset % user.size());
        penugasan.setDibuatOleh(dibuatOleh);
        penugasan.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());
        if (disetujui && dibuatOleh != null) {
            penugasan.setDisetujuiOleh(dibuatOleh);
            penugasan.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
        }
        penugasan.setAktif(true);
        session.save(penugasan);

        if (!user.isEmpty()) {
            TimAuditSPI ketua = new TimAuditSPI(penugasan);
            ketua.setOleh(PENANDA);
            ketua.setAnggota(user.get(userOffset % user.size()));
            ketua.setPeranTim(TimAuditSPI.KETUA_TIM);
            ketua.setAktif(true);
            session.save(ketua);

            if (user.size() > 1) {
                TimAuditSPI anggota = new TimAuditSPI(penugasan);
                anggota.setOleh(PENANDA);
                anggota.setAnggota(user.get((userOffset + 1) % user.size()));
                anggota.setPeranTim(TimAuditSPI.ANGGOTA_TIM);
                anggota.setAktif(true);
                session.save(anggota);
            }
        }

        for (int i = 0; i < checklistList.size() && i < klasifikasiUrut.length; i++) {
            ChecklistAuditSPI checklist = checklistList.get(i);
            String klasifikasi = klasifikasiUrut[i];

            TemuanAuditSPI temuan = new TemuanAuditSPI(checklist, penugasan);
            temuan.setOleh(PENANDA);
            temuan.setKlasifikasi(klasifikasi);
            temuan.setKondisi(teksKondisi(klasifikasi, checklist.getNama()));
            if (!TemuanAuditSPI.SESUAI.equals(klasifikasi)) {
                temuan.setSebab("Belum ada pengawasan berjenjang yang memadai atas proses terkait.");
                temuan.setAkibat("Berpotensi menimbulkan risiko kesalahan/ketidakpatuhan bila dibiarkan.");
                temuan.setRekomendasi("Perbaiki prosedur kerja terkait dan tingkatkan pengawasan berjenjang.");
            }
            temuan.setAktif(true);
            session.save(temuan);

            String statusTl = i < statusTlUrut.length ? statusTlUrut[i] : null;
            if (statusTl != null) {
                TindakLanjutAuditSPI tl = new TindakLanjutAuditSPI(temuan);
                tl.setOleh(PENANDA);
                tl.setDeskripsi("Unit " + unit.getNama() + " menindaklanjuti rekomendasi auditor terkait temuan ini.");
                tl.setPicNama(unit.getNama());
                tl.setStatus(statusTl);
                tl.setProgressPersen(
                        TindakLanjutAuditSPI.SELESAI.equals(statusTl) ? 100
                        : TindakLanjutAuditSPI.SEDANG_BERJALAN.equals(statusTl) ? 40
                        : TindakLanjutAuditSPI.TERLAMBAT.equals(statusTl) ? 20
                        : 0);
                tl.setAktif(true);
                session.save(tl);
            }
        }
        return true;
    }

    private static String teksKondisi(String klasifikasi, String namaChecklist) {
        String inti = namaChecklist == null ? "ketentuan terkait" : namaChecklist.replaceFirst("(?i)^periksa apakah ", "");
        if (TemuanAuditSPI.KRITIS.equals(klasifikasi)) {
            return "Ditemukan pelanggaran serius: " + inti + " tidak terpenuhi sama sekali, berisiko tinggi terhadap keandalan proses.";
        } else if (TemuanAuditSPI.MAYOR.equals(klasifikasi)) {
            return "Ditemukan kelemahan signifikan: " + inti + " belum dijalankan secara konsisten.";
        } else if (TemuanAuditSPI.MINOR.equals(klasifikasi)) {
            return "Ditemukan kekurangan kecil terkait " + inti + " — perlu perbaikan namun tidak mendesak.";
        } else if (TemuanAuditSPI.OBSERVASI.equals(klasifikasi)) {
            return "Perlu perhatian terkait " + inti + ", meski belum menimbulkan dampak signifikan.";
        }
        return "Terkait " + inti + ", pelaksanaan sudah sesuai ketentuan yang berlaku.";
    }

    // =====================================================================
    // Pembantu bersama
    // =====================================================================

    @SuppressWarnings("unchecked")
    private static List<SatuanKerja> ambilSatuanKerjaContoh(Session session) {
        List<SatuanKerja> list = session.createCriteria(SatuanKerja.class)
                .add(Restrictions.isNotNull("parent"))
                .addOrder(Order.asc("id"))
                .setMaxResults(3)
                .list();
        if (list.isEmpty()) {
            list = session.createCriteria(SatuanKerja.class)
                    .addOrder(Order.asc("id"))
                    .setMaxResults(3)
                    .list();
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<Tbmuser> ambilTbmuserContoh(Session session) {
        return session.createCriteria(Tbmuser.class)
                .addOrder(Order.asc("userId"))
                .setMaxResults(3)
                .list();
    }

    @SuppressWarnings("unchecked")
    private static List<JenisAuditSPI> ambilJenisAuditSPIContoh(Session session) {
        return session.createCriteria(JenisAuditSPI.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("id"))
                .setMaxResults(3)
                .list();
    }

    private static long hitung(Session session, Class<?> clazz) {
        Number n = (Number) session.createCriteria(clazz)
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return n == null ? 0 : n.longValue();
    }

    private static long hitung(Session session, Class<?> clazz, org.hibernate.criterion.Criterion... kriteria) {
        org.hibernate.Criteria criteria = session.createCriteria(clazz);
        for (org.hibernate.criterion.Criterion k : kriteria) {
            criteria.add(k);
        }
        Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
        return n == null ? 0 : n.longValue();
    }

    private static void gagal(String tag, Transaction tx, Exception e) {
        if (tx != null && tx.isActive()) {
            try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) SpiSampleDataHelper.gagal:rollback:" + tag); }
        }
        System.err.println("[SpiSampleDataHelper] " + tag + " gagal dibuat: " + e.getMessage());
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit SpiSampleDataHelper." + tag);
    }

    /**
     * Menutup session Hibernate hasil {@link HibernateUtil#openSession()} (POLA isolasi penuh,
     * thread latar {@code AppStartupListener}) — SATU-SATUNYA jalur penutupan yang dipakai di
     * seluruh kelas ini, lewat {@link HibernateUtil#closeSessionQuietly(Session)}.
     *
     * <p>
     * Seluruh method di kelas ini kini memakai {@code openSession()} (bukan {@code currentNativeSession()})
     * agar tiap bagian (A/B/C) mendapat session yang benar-benar independen dari ThreadLocal.
     * {@code AuditTimestampInterceptor} menyimpan SATU instance kanonik per kelas+id di
     * {@code EntityIdentityMap} yang dibagi seluruh session di JVM — bila session lama ditutup
     * namun entity kanoniknya masih dirujuk session baru lewat ThreadLocal yang sama,
     * dirty-check/auto-flush akan menyentuh session yang sudah tertutup dan melempar
     * {@code SessionException: "Session is closed!"}. Dengan {@code openSession()} setiap
     * pemanggil memiliki session tersendiri yang tidak terikat ThreadLocal, sehingga
     * cross-session reference tidak dapat terjadi.
     * </p>
     */
    private static void tutup(Session session) {
        HibernateUtil.closeSessionQuietly(session);
    }
}
