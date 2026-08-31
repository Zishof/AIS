package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import ais.database.hibernate.HibernateUtil;

/**
 * Adapter konten dashboard native untuk tombol utama index.zul.
 * Definisi mengikuti MainDashboardEventHelper; angka dibaca dari entity Hibernate existing.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiModuleDashboardService {
    private static final Map DEFINITIONS;
    static {
        Map values = new LinkedHashMap();
        add(values, "emedic", "Dasbor Utama Rumah Sakit / Klinik", "DashboardSirsKomprehensif", "Ringkasan layanan rumah sakit dan klinik.",
                m("Pasien", "ais.database.model.sirs.Pasien"), m("Pendaftaran", "ais.database.model.sirs.Pendaftaran"), m("Tempat Tidur", "ais.database.model.sirs.TempatTidur"), m("Booking", "ais.database.model.sirs.BookingRegistrasi"),
                f("Okupansi tempat tidur", "Pendaftaran dan booking", "Pendapatan layanan", "Farmasi dan transaksi"));
        add(values, "elearning", "Dashboard e-Learning", "ElearningSekolah / e_learning", "Materi, kelas, tugas, ujian, dan aktivitas pembelajaran.",
                m("Pertemuan", "ais.database.model.Pertemuan"), m("Tugas", "ais.database.model.Tugas"), m("Ujian", "ais.database.model.Ujian"), m("Peserta", "ais.database.model.KrsMahasiswa"),
                f("Linimasa pembelajaran", "Materi dan media", "Tugas dan penilaian", "Ujian dan monitoring"));
        add(values, "prestasi", "Dashboard Kegiatan & Prestasi", "DashboardKegiatanKedosenan / DashboardKegiatanKemahasiswaan", "Kegiatan dan pencapaian pengguna sesuai profil aktif.",
                m("Prestasi Mahasiswa", "ais.database.model.PrestasiMahasiswa"), m("Prestasi Dosen", "ais.database.model.PrestasiDosen"), m("Prestasi Pegawai", "ais.database.model.PrestasiPegawai"), m("Kegiatan", "ais.database.model.Kegiatan"),
                f("Prestasi per periode", "Kegiatan pengguna", "Penghargaan", "Riwayat capaian"));
        add(values, "pustaka", "Dashboard Pustaka", "DashboardPustaka", "Koleksi, anggota, sirkulasi, dan pemantauan pustaka.",
                m("Koleksi", "ais.database.model.library.Item"), m("Anggota", "ais.database.model.library.Anggota"), m("Peminjaman", "ais.database.model.library.PeminjamanPengadaanItem"), m("Bahan Ajar", "ais.database.model.BukuBahanAjar"),
                f("Koleksi tersedia", "Sirkulasi peminjaman", "Anggota aktif", "Stok dan keterlambatan"));
        add(values, "workflow", "Dashboard Workflow", "DasboardSop", "Alur pekerjaan, dokumen, SLA, dan tindak lanjut.",
                m("SOP", "ais.database.model.sop.Sop"), m("Data Proses", "ais.database.model.sop.DataSop"), m("Alur", "ais.database.model.sop.AlurSop"), m("Disposisi", "ais.database.model.sop.DisposisiSop"),
                f("Antrian pekerjaan", "Status persetujuan", "Dokumen proses", "Pemantauan SLA"));
        add(values, "repository", "Dasbor Repository", "DasboardRepository", "Metadata, koleksi, dokumen, DSpace, dan kesiapan indeks.",
                m("Item", "ais.database.model.repository.RepoItem"), m("Koleksi", "ais.database.model.repository.RepoCollection"), m("Metadata", "ais.database.model.repository.RepoItemMetadata"), m("Berkas", "ais.database.model.repository.RepoBitstream"),
                f("Sinkronisasi repository", "Kelengkapan metadata", "Dokumen dan bitstream", "Kesiapan indeks"));
        add(values, "antar_jemput", "Dashboard Antar Jemput", "DasboardAntarJemput", "Armada, rute, jadwal, peserta, dan transaksi penjemputan.",
                m("Kendaraan", "ais.database.model.antarjemput.KendaraanAntarJemput"), m("Rute", "ais.database.model.antarjemput.RuteAntarJemput"), m("Jadwal", "ais.database.model.antarjemput.JadwalAntarJemput"), m("Peserta", "ais.database.model.antarjemput.PesertaJadwalAntarJemput"),
                f("Armada aktif", "Jadwal perjalanan", "Scan gerbang", "Notifikasi penjemputan"));
        add(values, "spmi", "Dasbor SPMI", "DasboardSPMI", "Siklus PPEPP, audit mutu, temuan, standar, dan tindak lanjut.",
                m("Audit AMI", "ais.database.model.spmi.HasilSPMI"), m("Temuan", "ais.database.model.spmi.HasilTemuanSPMI"), m("Tindak Lanjut", "ais.database.model.spmi.TindakLanjutTemuanSPMI"), m("Standar", "ais.database.model.spmi.StandarSPMI"),
                f("Status pengajuan AMI", "Zona kepatuhan", "Distribusi temuan", "Tren dan tindak lanjut"));
        add(values, "toko", "Dasbor Kantin / Toko", "DashboardKantin", "Penjualan, produk, produksi, dan transaksi toko.",
                m("Produk", "ais.database.model.inventory.Produk"), m("Produksi", "ais.database.model.inventory.ProduksiKantin"), m("Pengadaan", "ais.database.model.inventory.PengadaanProduk"), m("Retur", "ais.database.model.inventory.ReturPenjualan"),
                f("Ringkasan penjualan", "Produk terlaris", "Stok kritis", "Jam transaksi ramai"));
        add(values, "koperasi", "Dasbor Koperasi", "DashboardKoperasi", "Keanggotaan, simpan pinjam, transaksi, dan produk koperasi.",
                m("Anggota", "ais.database.model.koperasi.AnggotaKoperasi"), m("Produk", "ais.database.model.koperasi.ProdukKoperasi"), m("Transaksi", "ais.database.model.koperasi.TransaksiKoperasi"), m("Pembayaran", "ais.database.model.koperasi.PembayaranAnggotaKoperasi"),
                f("Keanggotaan", "Simpanan dan pinjaman", "Pembagian SHU", "Transaksi koperasi"));
        add(values, "akademik", "Dashboard Akademik", "DashboardAkademis / ProfileAction", "Ringkasan akademik disesuaikan dengan profil pengguna aktif.",
                m("Mahasiswa", "ais.database.model.Mahasiswa"), m("Dosen", "ais.database.model.Dosen"), m("KRS", "ais.database.model.KrsMahasiswa"), m("Pertemuan", "ais.database.model.Pertemuan"),
                f("Profil akademik", "Perkuliahan aktif", "KRS dan nilai", "Kehadiran dan capaian"));
        add(values, "administrasi", "Dashboard Administrasi", "DasboardSurat", "Persuratan, arsip, alur persetujuan, dan disposisi.",
                m("Surat Masuk", "ais.database.model.surat.SuratMasuk"), m("Surat Keluar", "ais.database.model.surat.SuratKeluar"), m("Alur Masuk", "ais.database.model.surat.AlurPersetujuanSuratMasuk"), m("Alur Keluar", "ais.database.model.surat.AlurPersetujuanSuratKeluar"),
                f("Persuratan", "Arsip digital", "Disposisi", "Alur persetujuan"));
        add(values, "pengadaan", "Dashboard Pengadaan", "DasboardPengadaan", "Pengajuan, pembelian, vendor, inventaris, dan realisasi.",
                m("Master Aset", "ais.database.model.asset.MasterAsset"), m("Pemesanan", "ais.database.model.asset.PemesananPengadaanMasterAsset"), m("Penerimaan", "ais.database.model.asset.PenerimaanPengadaanMasterAsset"), m("Pembayaran", "ais.database.model.asset.PembayaranPengadaanMasterAsset"),
                f("Proses pengadaan", "Rekap inventaris", "Vendor", "Realisasi anggaran"));
        add(values, "pembayaran", "Dashboard Pembayaran", "DasboardPembayaran", "Pembayaran PT/sekolah, piutang, tagihan, dan rekonsiliasi.",
                m("Pembayaran Mahasiswa", "ais.database.model.PembayaranMahasiswa"), m("Cicilan", "ais.database.model.CicilanPembayaran"), m("Bukti Bayar", "ais.database.model.BuktiPembayaran"), m("Log Pembayaran", "ais.database.model.LogPembayaran"),
                f("Pembayaran periode aktif", "Kartu piutang", "Rekonsiliasi", "Tunggakan"));
        add(values, "keuangan", "Dashboard Keuangan", "DasboardKeuangan", "Posisi keuangan, transaksi, pembayaran, dan pengendalian kas.",
                m("Transaksi", "ais.database.model.akunting.Transaksi"), m("Pembayaran", "ais.database.model.PembayaranMahasiswa"), m("Pengeluaran", "ais.database.model.PengeluaranMahasiswa"), m("Bukti", "ais.database.model.BuktiPembayaran"),
                f("Posisi keuangan", "Arus kas", "Pembayaran", "Peringatan transaksi"));
        add(values, "akuntansi", "Dashboard Akuntansi", "DasboardAkuntansi", "Jurnal, transaksi, laporan keuangan, dan posting.",
                m("Transaksi", "ais.database.model.akunting.Transaksi"), m("Posting Jurnal", "ais.database.model.akunting.PostingHistory"), m("Akun", "ais.database.model.akunting.Akun"), m("Closing Periode", "ais.database.model.akunting.Closing"),
                f("Dashboard akuntansi", "Laporan keuangan", "Posting jurnal", "Saldo akun"));
        add(values, "kepegawaian", "Dashboard Kepegawaian", "DasboardKepegawaian", "Pegawai, status, aktivitas, dan kondisi penting SDM.",
                m("Pegawai", "ais.database.model.Pegawai"), m("Biodata", "ais.database.model.BiodataPegawai"), m("Status", "ais.database.model.StatusPegawai"), m("Catatan", "ais.database.model.CatatanPegawai"),
                f("Komposisi pegawai", "Status kepegawaian", "Aktivitas SDM", "Peringatan administrasi"));
        add(values, "gaji", "Dasbor Penggajian Utama", "DasborAnalisisPenggajian", "Penggajian, transaksi pegawai, tunjangan, potongan, cuti, dan izin.",
                m("Rencana Gaji", "ais.database.model.payroll.RencanaGaji"), m("Pembayaran Gaji", "ais.database.model.payroll.PembayaranGaji"), m("Transaksi Pegawai", "ais.database.model.payroll.TransaksiPegawai"), m("Item Gaji", "ais.database.model.payroll.ItemGaji"),
                f("Ringkasan penggajian", "Tunjangan dan potongan", "Cuti dan izin", "Status pembayaran"));
        add(values, "kinerja", "Dashboard Kinerja", "DasboardKinerja", "Target, realisasi, aktivitas, dan evaluasi kinerja.",
                m("Target Kerja", "ais.database.model.lkp.TargetKerjaPegawai"), m("Realisasi", "ais.database.model.lkp.RealisasiKerjaPegawai"), m("Kegiatan", "ais.database.model.lkp.KegiatanTugasJabatan"), m("BKD", "ais.database.model.KewajibanBebanDosen"),
                f("Target kinerja", "Capaian realisasi", "Aktivitas utama", "Evaluasi periode"));
        add(values, "presensi", "Dashboard Presensi", "PresensiAction", "Kehadiran pengguna, rekap periode, dan anomali presensi.",
                m("Kehadiran Pegawai", "ais.database.model.KehadiranPegawaiBulanan"), m("Kehadiran Dosen", "ais.database.model.KehadiranDosenBulanan"), m("Absen Pegawai", "ais.database.model.payroll.AbsenPegawaiDetail"), m("Pegawai", "ais.database.model.Pegawai"),
                f("Kehadiran hari ini", "Rekap bulanan", "Keterlambatan", "Anomali presensi"));
        add(values, "kalender_akademik", "Kalender Akademik", "onInformasiKalenderAkademik", "Agenda akademik, jadwal penting, dan periode aktif.",
                m("Agenda", "ais.database.model.KalenderAkademik"), m("Konfigurasi", "ais.database.model.KonfigurasiKalenderAkademik"), m("Kegiatan Kampus", "ais.database.model.JadwalKegiatanKampus"), m("Pertemuan", "ais.database.model.Pertemuan"),
                f("Agenda hari ini", "Periode akademik", "Jadwal kegiatan", "Pengingat tenggat"));
        add(values, "info_kegiatan", "Informasi Kegiatan", "onInformasiKegiatan", "Pengumuman dan kegiatan institusi untuk pengguna aktif.",
                m("Kegiatan", "ais.database.model.Kegiatan"), m("Formulir", "ais.database.model.FormulirKegiatan"), m("Peserta", "ais.database.model.FormulirKegiatanPeserta"), m("Jadwal", "ais.database.model.JadwalKegiatanKampus"),
                f("Kegiatan terbaru", "Pendaftaran kegiatan", "Agenda institusi", "Pengumuman"));
        add(values, "feeder", "Sinkronisasi Neo Feeder PDDikti", "DasbordSinkronisasiNeoFeeder", "Status sinkronisasi data akademik dengan Neo Feeder.",
                m("Sinkronisasi", "ais.database.model.NeoFeederSync"), m("Log", "ais.database.model.FeederLog"), m("Mahasiswa", "ais.database.model.Mahasiswa"), m("Dosen", "ais.database.model.Dosen"),
                f("Status koneksi", "Antrean sinkronisasi", "Log kegagalan", "Cakupan entitas"));
        add(values, "sister", "Sinkronisasi Data SISTER", "DasbordSinkronisasiSister", "Referensi, SDM, dan Tridharma dari SISTER Kemdikbud.",
                m("Data SISTER", "ais.database.model.DataSister"), m("SDM", "ais.database.model.sister.RefSdmSister"), m("Publikasi", "ais.database.model.sister.TridPublikasiSister"), m("Pengajaran", "ais.database.model.sister.TridPengajaranSister"),
                f("Status integrasi", "Referensi SDM", "Data Tridharma", "Log sinkronisasi"));
        DEFINITIONS = Collections.unmodifiableMap(values);
    }

    private NewUiModuleDashboardService() { }

    public static Dashboard load(String key) {
        Definition definition = (Definition) DEFINITIONS.get(normalize(key));
        if (definition == null) return null;
        List metrics = new ArrayList();
        Session session = null;
        try { session = HibernateUtil.currentSession(); } catch (Exception ignored) { }
        for (int i = 0; i < definition.metrics.length; i++) {
            MetricSpec spec = definition.metrics[i]; long value = 0L; boolean available = false;
            try {
                Class entity = Class.forName(spec.entityClass);
                Criteria criteria = session.createCriteria(entity).setProjection(Projections.rowCount());
                Object result = criteria.uniqueResult(); value = result instanceof Number ? ((Number) result).longValue() : 0L; available = true;
            } catch (Exception error) {
                try { ais.common.ErrorAuditUtil.record(error, "NewUiModuleDashboardService." + definition.key + "." + spec.entityClass); } catch (Exception ignored) { }
            }
            metrics.add(new Metric(spec.label, spec.entityClass, value, available));
        }
        return new Dashboard(definition, metrics, System.currentTimeMillis());
    }

    public static boolean supports(String key) { return DEFINITIONS.containsKey(normalize(key)); }
    private static String normalize(String key) { return key == null ? "" : key.trim().toLowerCase(); }
    private static MetricSpec m(String label, String entityClass) { return new MetricSpec(label, entityClass); }
    private static String[] f(String a,String b,String c,String d) { return new String[]{a,b,c,d}; }
    private static void add(Map map,String key,String title,String source,String description,MetricSpec a,MetricSpec b,MetricSpec c,MetricSpec d,String[] features) {
        map.put(key,new Definition(key,title,source,description,new MetricSpec[]{a,b,c,d},features));
    }

    /**
     * Tipe implementasi bersarang {@link MetricSpec} milik {@link NewUiModuleDashboardService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiModuleDashboardService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code String
     * entityClass}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiModuleDashboardService
     */
    private static final class MetricSpec { final String label, entityClass; MetricSpec(String label,String entityClass){this.label=label;this.entityClass=entityClass;} }
    /**
     * Tipe implementasi bersarang {@link Definition} milik {@link NewUiModuleDashboardService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiModuleDashboardService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String key}, {@code String title},
     * {@code String sourceClass}, {@code String description}, {@code MetricSpec metrics}, {@code String features};
     * operasi lokal: {@code getKey()}, {@code getTitle()}, {@code getSourceClass()}, {@code getDescription()},
     * {@code getFeatures}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiModuleDashboardService
     */
    public static final class Definition implements Serializable {
        private static final long serialVersionUID=1L; private final String key,title,sourceClass,description; private final MetricSpec[] metrics; private final String[] features;
        Definition(String key,String title,String sourceClass,String description,MetricSpec[] metrics,String[] features){this.key=key;this.title=title;this.sourceClass=sourceClass;this.description=description;this.metrics=metrics;this.features=features;}
        public String getKey(){return key;} public String getTitle(){return title;} public String getSourceClass(){return sourceClass;} public String getDescription(){return description;} public String[] getFeatures(){return features;}
    }
    /**
     * Tipe implementasi bersarang {@link Metric} milik {@link NewUiModuleDashboardService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiModuleDashboardService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String label}, {@code String
     * entityClass}, {@code long value}, {@code boolean available}; operasi lokal: {@code getLabel()}, {@code
     * getEntityClass()}, {@code getValue()}, {@code isAvailable}(). Aturan bisnis bersama tetap berada pada kelas
     * induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiModuleDashboardService
     */
    public static final class Metric implements Serializable {
        private static final long serialVersionUID=1L; private final String label,entityClass; private final long value; private final boolean available;
        Metric(String label,String entityClass,long value,boolean available){this.label=label;this.entityClass=entityClass;this.value=value;this.available=available;}
        public String getLabel(){return label;} public String getEntityClass(){return entityClass;} public long getValue(){return value;} public boolean isAvailable(){return available;}
    }
    /**
     * Tipe implementasi bersarang {@link Dashboard} milik {@link NewUiModuleDashboardService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiModuleDashboardService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Definition definition}, {@code List
     * metrics}, {@code long generatedAt}; operasi lokal: {@code getDefinition()}, {@code getMetrics()}, {@code
     * getGeneratedAt}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiModuleDashboardService
     */
    public static final class Dashboard implements Serializable {
        private static final long serialVersionUID=1L; private final Definition definition; private final List metrics; private final long generatedAt;
        Dashboard(Definition definition,List metrics,long generatedAt){this.definition=definition;this.metrics=Collections.unmodifiableList(metrics);this.generatedAt=generatedAt;}
        public Definition getDefinition(){return definition;} public List<Metric> getMetrics(){return metrics;} public long getGeneratedAt(){return generatedAt;}
    }
}
