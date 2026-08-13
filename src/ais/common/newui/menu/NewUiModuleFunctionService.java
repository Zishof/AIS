package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.common.Common;
import ais.database.model.Tbmuser;

/**
 * Kontrak fungsi dashboard native yang bersumber dari toolbar {@code index.zul}
 * dan handler {@code MainDashboardEventHelper}. Setiap fungsi tetap fail-closed:
 * target operasional hanya diisi dari snapshot menu yang sudah lolos RBAC.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiModuleFunctionService {

    private static final String ALL = "ALL";
    private static final String PT = "PT";
    private static final String SCHOOL = "SCHOOL";
    private static final String PERSONAL = "PERSONAL";
    private static final String ADMIN = "ADMIN";
    private static final String ADMIN_PT = "ADMIN_PT";
    private static final String ADMIN_SCHOOL = "ADMIN_SCHOOL";
    private static final Map DEFINITIONS;

    static {
        Map values = new LinkedHashMap();
        add(values, "emedic",
                f("ringkasan", "Dasbor Utama eMedic", "Ringkasan komprehensif layanan rumah sakit dan klinik.", "ais.action.master.dashboard.sirs.DashboardSirsKomprehensif", ALL, a("sistem informasi rumah sakit", "emedic", "dashboard sirs")),
                f("okupansi", "Okupansi Tempat Tidur", "Pantau ketersediaan dan keterisian tempat tidur.", "ais.action.master.sirs.chart.OkupansiTempatTidurDashboardAction", ALL, a("okupansi tempat tidur", "tempat tidur")),
                f("pendaftaran", "Pendaftaran & Booking", "Ringkasan pendaftaran, booking, dan layanan pasien.", "ais.action.master.sirs.chart.PendaftaranOverviewDashboardAction", ALL, a("pendaftaran", "booking", "registrasi pasien")),
                f("rawat_jalan_mingguan", "Rawat Jalan Mingguan", "Tren kunjungan rawat jalan per minggu.", "ais.action.master.sirs.chart.RawatJalanMingguanDashboardAction", ALL, a("rawat jalan mingguan", "rawat jalan")),
                f("rawat_jalan_bulanan", "Rawat Jalan Bulanan", "Tren kunjungan rawat jalan per bulan.", "ais.action.master.sirs.chart.RawatJalanBulananDashboardAction", ALL, a("rawat jalan bulanan", "rawat jalan")),
                f("pendapatan", "Pendapatan Layanan", "Pendapatan rumah sakit atau klinik per unit layanan.", "ais.action.master.sirs.chart.PendapatanDashboardAction", ALL, a("pendapatan", "pembayaran pasien")),
                f("diagnosa", "Diagnosa Terbanyak", "Distribusi diagnosa untuk pemantauan layanan.", "ais.action.master.sirs.chart.DiagnosaTerbanyakDashboardAction", ALL, a("diagnosa", "rekam medis")),
                f("farmasi", "Kedaluwarsa Farmasi", "Peringatan stok farmasi yang mendekati kedaluwarsa.", "ais.action.master.sirs.chart.KadaluarsaFarmasiDashboardAction", ALL, a("farmasi", "obat", "kadaluarsa")));
        add(values, "elearning",
                f("linimasa", "Linimasa Pembelajaran", "Pertemuan, materi, tugas, ujian, diskusi, dan presensi.", "ais.action.master.dashboard.admin.DashboardTimelinePertemuan", ALL, a("linimasa", "pertemuan", "perkuliahan")),
                f("materi", "Materi & Media", "Materi, dokumen, video, dan audio pembelajaran.", "ais.action.master.sekolah.helper.ElearningSekolah", ALL, a("materi", "bahan ajar", "media pembelajaran")),
                f("tugas", "Tugas & Penilaian", "Pemberian, pengumpulan, koreksi, dan nilai tugas.", "ais.action.master.sekolah.helper.ElearningSekolah", ALL, a("tugas", "penilaian")),
                f("ujian", "Ujian & Monitoring", "Ujian, kuis, peserta, waktu, dan hasil.", "ais.action.master.sekolah.helper.ElearningSekolah", ALL, a("ujian", "kuis")),
                f("diskusi", "Diskusi & Kolaborasi", "Diskusi kelas dan balasan peserta.", "ais.action.master.dashboard.admin.DashboardTimelinePertemuan", ALL, a("diskusi", "forum")),
                f("kalender", "Kalender & Presensi", "Agenda pembelajaran dan kehadiran peserta.", "ais.action.master.dashboard.admin.DashboardTimelinePertemuan", ALL, a("kalender", "presensi", "kehadiran")),
                f("gradebook", "Gradebook & Analitik", "Nilai, capaian, OBE, dan peserta berisiko.", "ais.action.master.dashboard.admin.DashboardGradePenilaianMahasiswa", PT, a("gradebook", "nilai mahasiswa", "obe")));
        add(values, "prestasi",
                f("mahasiswa", "Prestasi Mahasiswa", "Aktivitas, prestasi, karya, penghargaan, dan organisasi mahasiswa.", "ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaan", PT, a("prestasi mahasiswa", "kegiatan mahasiswa")),
                f("dosen", "Prestasi Dosen", "Aktivitas, prestasi, karya, penelitian, dan pengabdian dosen.", "ais.action.master.dashboard.admin.DashboardKegiatanKedosenan", PT, a("prestasi dosen", "kegiatan dosen")),
                f("siswa", "Prestasi Siswa", "Aktivitas, prestasi, karya, dan organisasi siswa.", "ais.action.master.dashboard.admin.DashboardKegiatanKesiswaan", SCHOOL, a("prestasi siswa", "kegiatan siswa")),
                f("mahasiswa_admin", "Analitik Prestasi Mahasiswa", "Dashboard pembinaan aktivitas dan prestasi mahasiswa untuk pengelola.", "ais.action.master.dashboard.admin.DashboardKegiatanKemahasiswaanAdmin", ADMIN_PT, a("prestasi mahasiswa", "kegiatan mahasiswa")),
                f("dosen_admin", "Analitik Prestasi Dosen", "Dashboard pembinaan aktivitas dan prestasi dosen untuk pengelola.", "ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin", ADMIN_PT, a("prestasi dosen", "kegiatan dosen")),
                f("siswa_admin", "Analitik Prestasi Siswa", "Dashboard pembinaan aktivitas dan prestasi siswa untuk pengelola.", "ais.action.master.dashboard.sekolah.DashboardKegiatanKesiswaanAdmin", ADMIN_SCHOOL, a("prestasi siswa", "kegiatan siswa")),
                f("pegawai", "Prestasi Pegawai", "Aktivitas dan prestasi pegawai.", "ais.action.master.prestasi.DasbordPrestasi", ALL, a("prestasi pegawai", "apresiasi pegawai")),
                f("riwayat", "Riwayat Capaian", "Riwayat prestasi sesuai profil pengguna aktif.", "ais.action.master.prestasi.DasbordKegiatanKedosenan", PERSONAL, a("riwayat prestasi", "capaian")));
        add(values, "pustaka",
                f("koleksi", "Koleksi Pustaka", "Pencarian dan ketersediaan koleksi.", "ais.action.master.dashboard.utama.DashboardPustaka", ALL, a("koleksi", "katalog", "item pustaka")),
                f("peminjaman", "Peminjaman", "Sirkulasi peminjaman anggota.", "ais.action.master.dashboard.library.DashboardStatistikPeminjamanAnggota", ALL, a("peminjaman", "sirkulasi")),
                f("pengembalian", "Pengembalian", "Pengembalian, keterlambatan, dan denda.", "ais.action.master.dashboard.library.DashboardStatistikPengembalianAnggota", ALL, a("pengembalian", "keterlambatan")),
                f("kunjungan", "Kunjungan Anggota", "Statistik kunjungan pengguna pustaka.", "ais.action.master.dashboard.library.DashboardStatistikKunjunganAnggota", ALL, a("kunjungan pustaka", "kunjungan anggota")),
                f("stok", "Stok & Bahan Ajar", "Stok koleksi dan buku bahan ajar.", "ais.action.master.dashboard.utama.DashboardPustaka", ALL, a("stok pustaka", "buku bahan ajar")));
        add(values, "workflow",
                f("antrian", "Antrian Pekerjaan", "Pekerjaan dan pengajuan yang menunggu tindakan.", "ais.action.master.sop.helper.DasboardSop", ALL, a("antrian", "pengajuan", "workflow")),
                f("persetujuan", "Status Persetujuan", "Tahap persetujuan dan pihak yang harus menindaklanjuti.", "ais.action.master.sop.helper.DasboardSop", ALL, a("persetujuan", "approval")),
                f("dokumen", "Dokumen Proses", "Dokumen dan lampiran di setiap alur.", "ais.action.master.sop.helper.DasboardSop", ALL, a("dokumen proses", "dokumen pendukung")),
                f("sla", "Pemantauan SLA", "Tenggat, keterlambatan, dan eskalasi proses.", "ais.action.master.sop.helper.DasboardSop", ALL, a("sla", "tenggat", "keterlambatan")));
        add(values, "repository",
                f("sinkronisasi", "Sinkronisasi Repository", "Sinkronisasi koleksi dan item repository.", "ais.action.master.repository.DasboardRepository", ALL, a("sinkronisasi repository", "repository")),
                f("metadata", "Kelengkapan Metadata", "Validasi metadata item dan koleksi.", "ais.action.master.repository.DasboardRepository", ALL, a("metadata", "repository")),
                f("dokumen", "Dokumen & Bitstream", "Berkas, format, ukuran, dan akses dokumen.", "ais.action.master.repository.DasboardRepository", ALL, a("bitstream", "dokumen repository")),
                f("dspace", "DSpace & Indeks", "Kesiapan DSpace, indeks, dan Turnitin.", "ais.action.master.repository.DasboardRepository", ALL, a("dspace", "turnitin", "indeks repository")));
        add(values, "antar_jemput",
                f("kendaraan", "Kendaraan", "Armada dan status kendaraan antar jemput.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("kendaraan", "armada")),
                f("rute", "Rute", "Rute, titik jemput, dan tujuan.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("rute", "titik jemput")),
                f("jadwal", "Jadwal", "Jadwal perjalanan dan petugas.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("jadwal antar jemput", "jadwal")),
                f("peserta", "Peserta Jadwal", "Peserta pada setiap jadwal perjalanan.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("peserta jadwal", "peserta antar jemput")),
                f("kartu", "Kartu Penjemput", "Kartu dan otorisasi penjemput.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("kartu penjemput", "penjemput")),
                f("transaksi", "Transaksi Penjemputan", "Scan gerbang dan detail transaksi penjemputan.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("transaksi penjemputan", "scan gerbang")),
                f("notifikasi", "Panggilan & Notifikasi", "Panggilan kelas dan log notifikasi.", "ais.action.master.antarjemput.DasboardAntarJemput", ALL, a("notifikasi antar jemput", "panggilan")));
        add(values, "spmi",
                f("ami", "Audit Mutu Internal", "Pengajuan dan pelaksanaan AMI.", "ais.action.master.spmi.DasboardSPMI", ALL, a("audit mutu internal", "hasil spmi", "ami")),
                f("temuan", "Temuan Audit", "Distribusi dan status temuan audit.", "ais.action.master.spmi.DasboardSPMI", ALL, a("temuan spmi", "temuan audit")),
                f("tindak_lanjut", "Tindak Lanjut", "Tindakan koreksi dan verifikasi penyelesaian.", "ais.action.master.spmi.DasboardSPMI", ALL, a("tindak lanjut temuan", "tindak lanjut")),
                f("standar", "Standar & Kepatuhan", "Standar mutu, indikator, dan zona kepatuhan.", "ais.action.master.spmi.DasboardSPMI", ALL, a("standar spmi", "kepatuhan")),
                f("tren", "Tren Kualitas", "Tren hasil audit dan peningkatan mutu.", "ais.action.master.spmi.DasboardSPMI", ALL, a("tren spmi", "peningkatan mutu")));
        add(values, "toko",
                f("penjualan", "Ringkasan Penjualan", "Transaksi dan pemasukan kantin atau toko.", "ais.action.master.koperasi.DashboardKantinAction", ALL, a("penjualan", "transaksi toko")),
                f("produk", "Produk Terlaris", "Produk, kategori, dan penjualan tertinggi.", "ais.action.master.koperasi.DashboardKantinAction", ALL, a("produk", "produk terlaris")),
                f("stok", "Stok Kritis", "Stok produk dan peringatan kebutuhan pengadaan.", "ais.action.master.inventory.DashboardStokKantinAction", ALL, a("stok kantin", "stok produk")),
                f("jam", "Jam Transaksi Ramai", "Distribusi transaksi berdasarkan waktu.", "ais.action.master.koperasi.DashboardKantinAction", ALL, a("jam transaksi", "transaksi")),
                f("pelanggan", "Pelanggan & Member", "Pelanggan setia dan aktivitas member.", "ais.action.master.koperasi.DashboardMemberKantinAction", ALL, a("member kantin", "pelanggan")));
        add(values, "koperasi",
                f("anggota", "Keanggotaan", "Anggota dan status keanggotaan koperasi.", "ais.action.master.koperasi.DashboardKoperasiAction", ALL, a("anggota koperasi", "keanggotaan")),
                f("simpan_pinjam", "Simpan Pinjam", "Simpanan, pinjaman, angsuran, dan tunggakan.", "ais.action.master.koperasi.DashboardSimpanPinjamAction", ALL, a("simpan pinjam", "simpanan", "pinjaman")),
                f("laporan", "Laporan Koperasi", "Ringkasan transaksi dan laporan koperasi.", "ais.action.master.koperasi.DashboardKoperasiAction", ALL, a("laporan koperasi", "transaksi koperasi")),
                f("shu", "Pembagian SHU", "Perhitungan dan pembagian sisa hasil usaha.", "ais.action.master.koperasi.DashboardKoperasiAction", ALL, a("pembagian shu", "shu")),
                f("kantin", "Kantin Koperasi", "Penjualan dan produk koperasi.", "ais.action.master.koperasi.DashboardKantinAction", ALL, a("kantin", "produk koperasi")));
        add(values, "akademik",
                f("profil", "Profil Akademik", "Dashboard pribadi mahasiswa, dosen, guru, atau siswa.", "ais.action.maintenance.ProfileAction", PERSONAL, a("profil akademik", "profil")),
                f("pt", "Akademik Perguruan Tinggi", "Ringkasan akademik kampus untuk pengelola.", "ais.action.master.dashboard.utama.DashboardData", PT, a("sistem informasi akademik", "akademik")),
                f("sekolah", "Akademik Sekolah", "Ringkasan akademik sekolah untuk pengelola.", "ais.action.master.dashboard.utama.DashboardDataSekolah", SCHOOL, a("sistem sekolah", "akademik sekolah")),
                f("peserta", "Mahasiswa & Siswa", "Status dan aktivitas peserta didik.", "ais.action.master.dashboard.admin.DashboardMahasiswa", ADMIN, a("mahasiswa", "siswa")),
                f("pengajar", "Dosen & Guru", "Status dan aktivitas tenaga pengajar.", "ais.action.master.dashboard.admin.DashboardDosen", ADMIN, a("dosen", "guru")),
                f("krs", "KRS & Nilai", "Pengambilan KRS, persetujuan, nilai, dan capaian.", "ais.action.master.dashboard.admin.DashboardKrsMahasiswa", PT, a("krs", "nilai mahasiswa")),
                f("perkuliahan", "Perkuliahan & Kehadiran", "Jadwal, pertemuan, dan kehadiran.", "ais.action.master.dashboard.admin.DashboardPerkuliahan", ALL, a("perkuliahan", "kehadiran")));
        add(values, "administrasi",
                f("persuratan", "Dashboard Persuratan", "Surat masuk, surat keluar, disposisi, dan pekerjaan tertunda.", "ais.action.master.surat.helper.DasboardSurat", ALL, a("surat masuk", "surat keluar", "persuratan")),
                f("alur", "Alur Persetujuan", "Posisi surat dan tahapan persetujuan.", "ais.action.master.surat.helper.DasboardAlurSurat", ALL, a("alur persetujuan surat", "persetujuan surat")),
                f("arsip", "Arsip Digital", "Arsip, klasifikasi, dan pencarian dokumen surat.", "ais.action.master.dashboard.surat.DasboardArsip", ALL, a("arsip surat", "arsip digital")),
                f("disposisi", "Disposisi", "Disposisi dan tindak lanjut surat.", "ais.action.master.surat.helper.DasboardSurat", ALL, a("disposisi surat", "disposisi")));
        add(values, "pengadaan",
                f("proses", "Proses Pengadaan", "Pengajuan, pembelian, penerimaan, dan pembayaran.", "ais.action.master.asset.helper.DasboardPengadaan", ALL, a("proses pengadaan", "pengadaan")),
                f("inventaris", "Rekap Inventaris", "Jumlah, kondisi, lokasi, dan sebaran aset.", "ais.action.master.dashboard.helper.DashboardRekapAset", ALL, a("inventaris", "master aset", "aset")),
                f("vendor", "Analisis Vendor", "Aktivitas, harga, kualitas, dan evaluasi vendor.", "ais.action.master.asset.helper.DasboardAnalisisVendor", ALL, a("analisis vendor", "vendor", "penyedia")),
                f("realisasi", "Realisasi Anggaran", "Nilai pengadaan dan realisasi pembayaran.", "ais.action.master.asset.helper.DasboardPengadaan", ALL, a("realisasi pengadaan", "pembayaran pengadaan")));
        add(values, "pembayaran",
                f("dashboard_pt", "Dashboard Pembayaran PT", "Penerimaan dan kondisi transaksi mahasiswa.", "ais.action.master.akunting.helper.DasboardPembayaranPerguruanTinggi", PT, a("pembayaran mahasiswa", "dashboard pembayaran")),
                f("piutang_pt", "Kartu Piutang PT", "Posisi tagihan mahasiswa.", "ais.action.master.dashboard.keuangan.DasboardPiutang", PT, a("kartu piutang", "piutang mahasiswa")),
                f("piutang_rinci", "Kartu Piutang Rinci", "Rincian tagihan dan pembayaran per mahasiswa.", "ais.action.master.dashboard.keuangan.DasboardPiutangRInci", PT, a("piutang rinci", "rincian piutang")),
                f("piutang_excel", "Kartu Piutang Rinci Excel", "Pratinjau dan ekspor rincian piutang.", "ais.action.master.dashboard.keuangan.DasboardPiutangRInciExcel", PT, a("piutang excel", "laporan piutang")),
                f("dashboard_sekolah", "Dashboard Pembayaran Sekolah", "Penerimaan, tunggakan, dan transaksi siswa.", "ais.action.master.akunting.helper.DasboardPembayaranSekolah", SCHOOL, a("pembayaran siswa", "dashboard pembayaran sekolah")),
                f("piutang_sekolah", "Kartu Piutang Sekolah", "Posisi tagihan siswa.", "ais.action.master.dashboard.keuangan.DasboardPiutangRinciSekolah", SCHOOL, a("piutang siswa", "kartu piutang sekolah")),
                f("rincian_sekolah", "Dashboard Rincian Pembayaran", "Rincian pembayaran siswa yang dapat dicari dan diekspor.", "ais.action.report.format1.sekolah.LaporanRincianPembayaranSiswaGrid", SCHOOL, a("rincian pembayaran siswa", "laporan pembayaran siswa")));
        add(values, "keuangan",
                f("posisi", "Posisi Keuangan", "Ringkasan saldo dan kondisi keuangan.", "ais.action.master.akunting.helper.DasboardKeuangan", ALL, a("posisi keuangan", "keuangan")),
                f("arus_kas", "Arus Kas", "Penerimaan, pengeluaran, dan pergerakan kas.", "ais.action.master.akunting.helper.DasboardKeuangan", ALL, a("arus kas", "kas besar", "kas kecil")),
                f("pembayaran", "Pembayaran", "Transaksi pembayaran dan bukti penerimaan.", "ais.action.master.akunting.helper.DasboardKeuangan", ALL, a("pembayaran", "bukti pembayaran")),
                f("peringatan", "Peringatan Transaksi", "Transaksi belum selesai dan kondisi yang perlu diperiksa.", "ais.action.master.akunting.helper.DasboardKeuangan", ALL, a("monitor transaksi", "peringatan keuangan")));
        add(values, "akuntansi",
                f("dashboard", "Dashboard Akuntansi", "Kondisi akuntansi dan transaksi penting.", "ais.action.master.akunting.helper.DasboardAkuntansi", ALL, a("dashboard akuntansi", "akuntansi")),
                f("laporan", "Laporan Keuangan", "Laporan keuangan utama.", "ais.action.master.akunting.helper.DasboardAkunting", ALL, a("laporan keuangan", "laporan akuntansi")),
                f("buku_besar", "Buku Besar", "Pergerakan akun dan rincian transaksi.", "ais.action.master.dashboard.akunting.DasboardBukuBesar", ALL, a("buku besar", "akun")),
                f("neraca", "Neraca Lajur", "Saldo akun dan penyusunan laporan akhir.", "ais.action.master.dashboard.akunting.DasboardNeracaLajur", ALL, a("neraca lajur", "neraca")),
                f("posting", "Posting Jurnal", "Posting, pembatalan, dan riwayat jurnal.", "ais.action.master.akunting.helper.DasboardAkuntansi", ALL, a("posting jurnal", "jurnal")));
        add(values, "kepegawaian",
                f("pegawai", "Data Pegawai", "Ringkasan dan status data pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaian", ALL, a("pegawai", "kepegawaian")),
                f("status", "Status Kepegawaian", "Distribusi status pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaianStatusKepegawaian", ALL, a("status kepegawaian", "status pegawai")),
                f("unit", "Unit Kerja", "Sebaran pegawai berdasarkan unit kerja.", "ais.action.master.dashboard.admin.DasboardKepegawaianUnitKerja", ALL, a("unit kerja", "satuan kerja")),
                f("jabatan", "Jabatan", "Sebaran jenis jabatan pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaianJenisJabatan", ALL, a("jenis jabatan", "jabatan pegawai")),
                f("masa_kerja", "Masa Kerja", "Distribusi masa kerja pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaianMasaKerja", ALL, a("masa kerja", "pegawai")),
                f("pendidikan", "Pendidikan", "Pendidikan terakhir pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaianPendidikan", ALL, a("pendidikan pegawai", "pendidikan")),
                f("usia", "Usia", "Distribusi usia pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaianUsia", ALL, a("usia pegawai", "tanggal lahir")),
                f("asuransi", "Asuransi & Ikatan Kerja", "Asuransi dan ikatan kerja pegawai.", "ais.action.master.dashboard.admin.DasboardKepegawaianAsuransi", ALL, a("asuransi pegawai", "ikatan kerja")));
        add(values, "gaji",
                f("ringkasan", "Ringkasan Penggajian", "Rencana, proses, dan pembayaran gaji.", "ais.action.master.payroll.helper.DasborAnalisisPenggajian", ALL, a("penggajian", "rencana gaji")),
                f("detail", "Gaji Pegawai", "Rincian komponen dan pembayaran gaji pegawai.", "ais.action.master.payroll.helper.DasborGajiPegawai", ALL, a("gaji pegawai", "pembayaran gaji")),
                f("transaksi", "Tunjangan & Potongan", "Transaksi pegawai, tunjangan, dan potongan.", "ais.action.master.payroll.helper.DasborTransaksiPegawai", ALL, a("transaksi pegawai", "tunjangan", "potongan")),
                f("cuti", "Cuti & Izin", "Pengajuan, persetujuan, dan saldo cuti atau izin.", "ais.action.master.payroll.helper.DasborCutiDanIzin", ALL, a("cuti", "izin pegawai")),
                f("status", "Status Pembayaran", "Status proses dan pembayaran payroll.", "ais.action.master.payroll.helper.DasborAnalisisPenggajian", ALL, a("status pembayaran gaji", "payroll")));
        add(values, "kinerja",
                f("target", "Target Kinerja", "Target kerja dan indikator pegawai.", "ais.action.master.dashboard.employ.DasboardKinerja", ALL, a("target kerja", "target kinerja")),
                f("realisasi", "Capaian Realisasi", "Realisasi dan persentase capaian target.", "ais.action.master.dashboard.employ.DasboardKinerja", ALL, a("realisasi kerja", "capaian kinerja")),
                f("aktivitas", "Aktivitas Utama", "Kegiatan tugas jabatan dan bukti aktivitas.", "ais.action.master.dashboard.employ.DasboardKinerja", ALL, a("kegiatan tugas jabatan", "aktivitas kinerja")),
                f("evaluasi", "Evaluasi Periode", "Evaluasi kinerja dan tindak lanjut.", "ais.action.master.dashboard.employ.DasboardKinerja", ALL, a("evaluasi kinerja", "penilaian kinerja")),
                f("bkd", "Beban Kinerja Dosen", "Rencana, realisasi, dan verifikasi BKD.", "ais.action.master.dashboard.employ.DasboardKinerja", PT, a("beban kinerja dosen", "bkd")));
        add(values, "presensi",
                f("hari_ini", "Kehadiran Hari Ini", "Presensi masuk, pulang, dan status hari ini.", "ais.action.master.payroll.KehadiranPegawaiAction", ALL, a("presensi", "kehadiran hari ini")),
                f("bulanan", "Rekap Bulanan", "Rekap kehadiran per periode.", "ais.action.master.payroll.KehadiranPegawaiAction", ALL, a("rekap presensi", "kehadiran bulanan")),
                f("keterlambatan", "Keterlambatan", "Keterlambatan dan pulang sebelum waktunya.", "ais.action.master.payroll.KehadiranPegawaiAction", ALL, a("keterlambatan", "absen pegawai")),
                f("anomali", "Anomali Presensi", "Data tidak lengkap dan kondisi yang perlu ditindaklanjuti.", "ais.action.master.payroll.KehadiranPegawaiAction", ALL, a("anomali presensi", "presensi tidak lengkap")));
        add(values, "kalender_akademik",
                f("agenda", "Agenda Akademik", "Seluruh kegiatan akademik dan status pelaksanaannya.", "ais.action.master.kalender.DasbordKalenderAkademik", ALL, a("kalender akademik", "agenda akademik")),
                f("periode", "Periode Akademik", "Tahun akademik, semester, dan rentang kegiatan.", "ais.action.master.kalender.DasbordKalenderAkademik", ALL, a("periode akademik", "tahun akademik")),
                f("jadwal", "Jadwal Kegiatan", "Waktu, durasi, dan sebaran kegiatan.", "ais.action.master.kalender.DasbordKalenderAkademik", ALL, a("jadwal kegiatan kampus", "kegiatan akademik")),
                f("pengingat", "Pengingat Tenggat", "Kegiatan mendatang dan tenggat penting.", "ais.action.master.kalender.DasbordKalenderAkademik", ALL, a("pengingat", "tenggat")));
        add(values, "info_kegiatan",
                f("terbaru", "Kegiatan Terbaru", "Daftar kegiatan terbaru sesuai institusi.", "ais.action.master.kalender.DasbordInfoKegiatan", ALL, a("info kegiatan", "kegiatan")),
                f("pendaftaran", "Pendaftaran Kegiatan", "Formulir dan status pendaftaran peserta.", "ais.action.master.kalender.DasbordInfoKegiatan", ALL, a("pendaftaran kegiatan", "formulir kegiatan")),
                f("agenda", "Agenda Institusi", "Waktu dan detail pelaksanaan kegiatan.", "ais.action.master.kalender.DasbordInfoKegiatan", ALL, a("agenda kegiatan", "jadwal kegiatan")),
                f("pengumuman", "Pengumuman", "Informasi dan pengumuman untuk pengguna aktif.", "ais.action.master.kalender.DasbordInfoKegiatan", ALL, a("pengumuman", "informasi kegiatan")));
        add(values, "feeder",
                f("koneksi", "Status Koneksi", "Koneksi dan token Neo Feeder.", "ais.action.master.feeder.DasbordSinkronisasiNeoFeeder", ALL, a("koneksi feeder", "konfigurasi feeder")),
                f("antrean", "Antrean Sinkronisasi", "Data yang menunggu dikirim atau diperbarui.", "ais.action.master.feeder.DasbordSinkronisasiNeoFeeder", ALL, a("sinkronisasi feeder", "antrean feeder")),
                f("log", "Log Kegagalan", "Kesalahan, retry, dan hasil sinkronisasi.", "ais.action.master.feeder.DasbordSinkronisasiNeoFeeder", ALL, a("log feeder", "error feeder")),
                f("mahasiswa", "Mahasiswa", "Sinkronisasi biodata dan aktivitas mahasiswa.", "ais.action.master.feeder.DasbordSinkronisasiNeoFeeder", PT, a("mahasiswa feeder", "feeder mahasiswa")),
                f("dosen", "Dosen", "Sinkronisasi dosen dan penugasan.", "ais.action.master.feeder.DasbordSinkronisasiNeoFeeder", PT, a("dosen feeder", "feeder dosen")),
                f("akademik", "Mata Kuliah, Kelas & Nilai", "Sinkronisasi mata kuliah, kurikulum, kelas, KRS, dan nilai.", "ais.action.master.feeder.DasbordSinkronisasiNeoFeeder", PT, a("kelas feeder", "nilai feeder", "mata kuliah feeder")));
        add(values, "sister",
                f("koneksi", "Status Integrasi", "Koneksi, autentikasi, dan kesehatan layanan SISTER.", "ais.action.master.sister.DasbordSinkronisasiSister", ALL, a("integrasi sister", "koneksi sister")),
                f("referensi", "Referensi", "Referensi data dari SISTER Kemdikbud.", "ais.action.master.sister.DasbordSinkronisasiSister", ALL, a("referensi sister", "sister")),
                f("sdm", "Dosen & SDM", "Sinkronisasi identitas dan data SDM.", "ais.action.master.sister.DasbordSinkronisasiSister", PT, a("sdm sister", "dosen sister")),
                f("pengajaran", "Pengajaran", "Data pengajaran dan pendidikan.", "ais.action.master.sister.DasbordSinkronisasiSister", PT, a("pengajaran sister", "tridharma")),
                f("penelitian", "Penelitian & Publikasi", "Penelitian, publikasi, dan kekayaan intelektual.", "ais.action.master.sister.DasbordSinkronisasiSister", PT, a("publikasi sister", "penelitian sister")),
                f("pengabdian", "Pengabdian", "Kegiatan pengabdian kepada masyarakat.", "ais.action.master.sister.DasbordSinkronisasiSister", PT, a("pengabdian sister", "tridharma")));
        DEFINITIONS = Collections.unmodifiableMap(values);
    }

    private NewUiModuleFunctionService() { }

    public static List<Function> build(String moduleKey, Tbmuser user,
            List<NewUiHybridMenuNode> authorizedNodes) {
        List specs = (List) DEFINITIONS.get(normalize(moduleKey));
        List<Function> result = new ArrayList<Function>();
        if (specs == null) return result;
        for (int i = 0; i < specs.size(); i++) {
            Spec spec = (Spec) specs.get(i);
            if (!applies(spec.audience, user)) continue;
            NewUiHybridMenuNode target = NewUiModuleShortcutService.findBest(
                    authorizedNodes, spec.aliases, null);
            result.add(new Function(spec.key, spec.label, spec.description,
                    spec.sourceClass, i + 1, target));
        }
        return Collections.unmodifiableList(result);
    }

    public static Function find(String moduleKey, String functionKey, Tbmuser user,
            List<NewUiHybridMenuNode> authorizedNodes) {
        List<Function> functions = build(moduleKey, user, authorizedNodes);
        for (int i = 0; i < functions.size(); i++) {
            Function value = functions.get(i);
            if (value.getKey().equalsIgnoreCase(normalize(functionKey))) return value;
        }
        return null;
    }

    public static boolean supports(String key) {
        return DEFINITIONS.containsKey(normalize(key));
    }

    public static int definitionCount(String key) {
        List values = (List) DEFINITIONS.get(normalize(key));
        return values == null ? 0 : values.size();
    }

    /** Snapshot immutable untuk audit otomatis kontrak toolbar index.zul. */
    public static List<String> moduleKeys() {
        return Collections.unmodifiableList(new ArrayList<String>(DEFINITIONS.keySet()));
    }

    /** Daftar key fungsi tanpa evaluasi user/konteks agar dapat diuji secara deterministik. */
    public static List<String> definitionKeys(String key) {
        List values = (List) DEFINITIONS.get(normalize(key));
        List<String> result = new ArrayList<String>();
        if (values == null) return Collections.unmodifiableList(result);
        for (int i = 0; i < values.size(); i++) result.add(((Spec) values.get(i)).key);
        return Collections.unmodifiableList(result);
    }

    /** Class Action/dashboard existing yang menjadi sumber kebenaran tiap fungsi. */
    public static List<String> definitionSourceClasses(String key) {
        List values = (List) DEFINITIONS.get(normalize(key));
        List<String> result = new ArrayList<String>();
        if (values == null) return Collections.unmodifiableList(result);
        for (int i = 0; i < values.size(); i++) {
            String source = ((Spec) values.get(i)).sourceClass;
            if (!result.contains(source)) result.add(source);
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean applies(String audience, Tbmuser user) {
        if (ALL.equals(audience)) return true;
        boolean personal = false;
        try { personal = user != null && (user.getMahasiswa() != null || user.getSiswa() != null
                || user.ambilDosen() != null || user.ambilGuru() != null); }
        catch (Exception ignored) { }
        if (PERSONAL.equals(audience)) return personal;
        if (ADMIN.equals(audience)) return !personal;
        boolean[] context = null;
        try { context = Common.chekPtAtauSekolah(); } catch (Exception ignored) { }
        if (ADMIN_PT.equals(audience)) return !personal
                && (context == null || context.length == 0 || context[0]);
        if (ADMIN_SCHOOL.equals(audience)) return !personal
                && (context == null || context.length < 2 || context[1]);
        if (PT.equals(audience)) return context == null || context.length == 0 || context[0];
        if (SCHOOL.equals(audience)) return context == null || context.length < 2 || context[1];
        return true;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String[] a(String... values) { return values; }
    private static Spec f(String key, String label, String description,
            String sourceClass, String audience, String[] aliases) {
        return new Spec(key, label, description, sourceClass, audience, aliases);
    }
    private static void add(Map map, String key, Spec... functions) {
        List values = new ArrayList();
        for (int i = 0; i < functions.length; i++) values.add(functions[i]);
        map.put(key, Collections.unmodifiableList(values));
    }

    private static final class Spec {
        final String key, label, description, sourceClass, audience;
        final String[] aliases;
        Spec(String key, String label, String description, String sourceClass,
                String audience, String[] aliases) {
            this.key = key; this.label = label; this.description = description;
            this.sourceClass = sourceClass; this.audience = audience; this.aliases = aliases;
        }
    }

    public static final class Function implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String key, label, description, sourceClass;
        private final int order;
        private final NewUiHybridMenuNode target;
        Function(String key, String label, String description, String sourceClass,
                int order, NewUiHybridMenuNode target) {
            this.key = key; this.label = label; this.description = description;
            this.sourceClass = sourceClass; this.order = order; this.target = target;
        }
        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
        public String getSourceClass() { return sourceClass; }
        public int getOrder() { return order; }
        public NewUiHybridMenuNode getTarget() { return target; }
        public boolean isOperationalRouteAvailable() { return target != null && target.getMenuId() != null; }
    }
}
