package ais.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;

import ais.database.hibernate.AuditTimestampInterceptor;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AccessedUsers;
import ais.database.model.BankHost;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.BukuBahanAjar;
import ais.database.model.ChecklistBaruPenilaianDosenOlehMahasiswa;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailLogLogin;
import ais.database.model.Detailperkuliahan;
import ais.database.model.DiskonMahasiswa;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JamPerkuliahan;
import ais.database.model.JenisPengajuanPegawai;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanPertemuan;
import ais.database.model.Konstanta;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.LogUserActifity;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.Matakuliah;
import ais.database.model.MemoryInfo;
import ais.database.model.Menu;
import ais.database.model.OnlineUsers;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.Paket;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.PembombotanNilai;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.PendukungReport;
import ais.database.model.PengajuanIzinTidakMasukPerkuliahan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PenghargaanDosen;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.PilihanPaketPerJurusanMhsBaru;
import ais.database.model.PrestasiDosen;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.RolePrivilage;
import ais.database.model.Ruang;
import ais.database.model.SatuanKerjaPegawai;
import ais.database.model.SubReport;
import ais.database.model.TanyaJawab;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.Wilayah;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.JenisKasBesar;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.JenisUangMuka;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.JenisCutiDanIzin;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.SkorGolongan;
import ais.database.model.employ.Transport;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.kpi.SkorKpi;
import ais.database.model.kursus.BeritaKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.database.model.payroll.AsuransiPegawai;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.payroll.JatahCuti;
import ais.database.model.payroll.JenisTransaksiPegawai;
import ais.database.model.payroll.KelompokItemGaji;
import ais.database.model.payroll.KodeTunjangan;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.recruitment.GelombangPendaftaranPegawai;
import ais.database.model.recruitment.VerifikasiKelengkapanCalonPegawai;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.AsramaSiswaPunyaSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.DiskonSiswaPunyaSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KelompokMatapelajaran;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.MasaJadwalPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.SubMatapelajaran;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.TempatTidur;
import ais.database.model.sop.AktorSop;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.database.model.surat.VariableSuratKeluar;

/**
 * Lapisan akses-data ber-cache untuk entity {@link GeneralValueObject}. Class ini adalah "otak"
 * yang memutuskan apakah sebuah entity diambil dari/disimpan ke {@link MemoryCacheUtil cache MapDB},
 * dari daftar konstanta {@link ConstantValues}, atau dibaca langsung dari basis data via Hibernate.
 * Ia juga menangani pencatatan riwayat perubahan (audit) dan pemulihan data dari tabel audit Envers.
 *
 * <h3>Untuk apa class ini</h3>
 * ECAMPUS membaca entity referensi berulang-ulang pada hampir setiap halaman. Memukul DB tiap kali
 * akan menghabiskan koneksi pool dan memperlambat sistem. DataUtil menyaring entity ke dalam dua
 * kategori (lihat di bawah) lalu mengarahkan baca/tulis ke jalur tercepat yang masih koheren.
 *
 * <h3>Dua daftar kelas yang menentukan perilaku</h3>
 * <ul>
 *   <li><b>{@code CLASS_JANGAN_DIBERSIHKAN}</b> — entity referensi yang relatif statis (Tbmuser,
 *   Menu, Kurikulum, master gaji, dll.). Ditandai agar TIDAK dibuang dari cache saat pembersihan
 *   berkala. Dicek lewat {@link #merupakanJanganDibersihkan(Class)}.</li>
 *   <li><b>{@code CLASS_IZINKAN}</b> — entity transaksional yang BOLEH di-cache lewat MapDB
 *   (Pertemuan, HasilUjianMahasiswa, DetailBiaya, Kegiatan, dll.). {@link #izinkan(Class)} menjaga
 *   agar hanya kelas inilah yang masuk/keluar cache objek.</li>
 * </ul>
 * Keduanya memakai {@link HashSet} statis demi lookup O(1)—jauh lebih hemat CPU/memori daripada
 * rentetan {@code if-or} panjang.
 *
 * <h3>Konsistensi cache (PENTING)</h3>
 * {@link MemoryCacheUtil#get(String)} mengembalikan map yang nilainya bisa berupa SALINAN hasil
 * serialisasi. Maka {@link #masukkanData(Class, GeneralValueObject)} WAJIB dipanggil di setiap jalur
 * tulis (simpan/ubah) dan {@link #hapus(Class, String)} di jalur hapus—jika tidak, pembaca akan
 * mendapat data basi. Inilah akar bug "skor 0/data lama" yang pernah terjadi di modul ujian. Untuk
 * entity ber-kunci komposit (HasilUjianMahasiswa, HistoryStatusMahasiswa), {@code masukkanData}
 * menyegarkan baik map by-id maupun map by-key turunan.
 *
 * <h3>Higiene koneksi &amp; kompatibilitas</h3>
 * Semua session yang dibuka untuk pembacaan fallback ditutup rapi di blok {@code finally}
 * (disconnect + close + {@link HibernateUtil#closeSession()}). Penyalinan properti memakai
 * {@code BeanUtilsBean}. Penulisan nilai relasi divalidasi keberadaannya dulu agar tidak menulis FK
 * ke baris yang sudah hilang. Kompatibel Java 1.6/1.7 (tanpa lambda/stream; multi-catch ditulis
 * terpisah). Saat menambah entity baru yang perlu di-cache, daftarkan di {@code CLASS_IZINKAN}.
 */
public class DataUtil {

	// OPTIMASI: Menggunakan HashSet statis (O(1) lookup time) daripada rentetan
	// IF-OR untuk menghemat CPU & Memori
	private static final Set<String> CLASS_JANGAN_DIBERSIHKAN = new HashSet<String>();
	private static final Set<String> CLASS_IZINKAN = new HashSet<String>();

	static {
		// Inisialisasi daftar class yang jangan dibersihkan
		CLASS_JANGAN_DIBERSIHKAN.add(Tbmuser.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Tbmrole.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Guru.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Siswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Pegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Dosen.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Mahasiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(GajiPokok.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Insentif.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Makan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Transport.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Golongan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Kpi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Menu.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ais.database.model.employ.Golongan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(GelombangPendaftaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(MahasiswaJadiAsisten.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ParameterTambahan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KenaikanPangkat.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PengumumanAkademis.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SumberDana.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PenyediaAsset.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(VerifikasiKelengkapanCalonPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(DetailJenisPenilaian.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(DetailGrupKategoriItemPenilaianSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisItemPenilaianSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(GrupKategoriItemPenilaianSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KategoriItemPenilaianSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelasSiswaPunyaSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelompokItemGaji.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(AsuransiPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Toko.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Keluarga.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Pedagang.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(AkunPembayaranSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Matapelajaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelompokMatapelajaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KurikulumPunyaMatapelajaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KurikulumSekolah.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Kurikulum.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisCutiDanIzin.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(AsramaSiswaPunyaSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Closing.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisPengajuanPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelasLesSiswaPunyaSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelasSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(DiskonSiswaPunyaSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(MasaJadwalPelajaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisUangMuka.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisKasBesar.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(RolePrivilage.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JatahCuti.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PembombotanNilai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(MasterAsset.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(BlokirMahasiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(BankHost.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Ruang.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KodeTunjangan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(TanyaJawab.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ItemMedis.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Kamar.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(TempatTidur.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(DetailJenisShiftPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SubReport.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PendukungReport.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Paket.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(VariableSuratKeluar.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JurusanSekolahMahasiswaBaru.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisSekolahMahasiswaBaru.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PilihanPaketPerJurusanMhsBaru.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisTransaksiPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisTransaksi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PengaturanBiayaItemBiaya.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ProdukKursus.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(BeritaKursus.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(GelombangPendaftaranPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SatuanKerjaPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JenisJabatan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PengaturanBiaya.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SubMatapelajaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Pejabat.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Akun.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(GelombangPendaftaranPsb.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ChecklistPenilaianGuru.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JadwalPelajaran.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Wilayah.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelompokParameterTambahanCatatanAdministrasi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(KelompokParameterTambahanCatatanSiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(DiskonMahasiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(JamPerkuliahan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(AktorSop.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SatuanKerja.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PerguruanTinggi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Fakultas.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Jurusan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Matakuliah.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ItemKpi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(BiodataMahasiswa.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(NilaiKpi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SkorKpi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(SkorGolongan.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(Konstanta.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ItemGaji.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(ItemGajiPegawai.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PenilaianKpi.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(FormatKpiDetail.class.getName());
		CLASS_JANGAN_DIBERSIHKAN.add(PendaftaranCutiMahasiswa.class.getName());

		// Inisialisasi daftar class yang diizinkan
		CLASS_IZINKAN.add(Pertemuan.class.getName());
		CLASS_IZINKAN.add(HasilUjianMahasiswa.class.getName());
		CLASS_IZINKAN.add(HasilUjianMahasiswaDetail.class.getName());
		CLASS_IZINKAN.add(HistoryStatusMahasiswa.class.getName());
		CLASS_IZINKAN.add(DetailBiaya.class.getName());
		CLASS_IZINKAN.add(PengaturanPembayaranBulanan.class.getName());
		CLASS_IZINKAN.add(DetailKegiatan.class.getName());
		CLASS_IZINKAN.add(CicilanPembayaran.class.getName());
		CLASS_IZINKAN.add(Kegiatan.class.getName());
		CLASS_IZINKAN.add(PendaftaranWisuda.class.getName());
		CLASS_IZINKAN.add(MahasiswaJadiAsisten.class.getName());
		CLASS_IZINKAN.add(PengajuanIzinTidakMasukPerkuliahan.class.getName());
		CLASS_IZINKAN.add(MahasiswaDapatKelompokKkn.class.getName());
		CLASS_IZINKAN.add(MahasiswaDapatKelompokPkl.class.getName());
		CLASS_IZINKAN.add(KrsMahasiswa.class.getName());
		CLASS_IZINKAN.add(FormulirKegiatanPeserta.class.getName());
		CLASS_IZINKAN.add(PertemuanPunyaGrupPertemuan.class.getName());
		CLASS_IZINKAN.add(KelompokParameterTambahanPertemuan.class.getName());
		CLASS_IZINKAN.add(PertemuanPunyaDiskusi.class.getName());
		CLASS_IZINKAN.add(DiskusiPengumumanAkademis.class.getName());
		CLASS_IZINKAN.add(PertemuanFileContent.class.getName());
		CLASS_IZINKAN.add(AudioPertemuan.class.getName());
		CLASS_IZINKAN.add(VideoPertemuan.class.getName());
		CLASS_IZINKAN.add(TugasPertemuan.class.getName());
		CLASS_IZINKAN.add(TugasKelompok.class.getName());
		CLASS_IZINKAN.add(PertemuanPunyaUjian.class.getName());
		CLASS_IZINKAN.add(KegiatanKemahasiswaanPunyaMahasiswa.class.getName());
		CLASS_IZINKAN.add(OrganisasiIntraKampusPunyaMahasiswa.class.getName());
		CLASS_IZINKAN.add(PrestasiMahasiswa.class.getName());
		CLASS_IZINKAN.add(PenghargaanMahasiswa.class.getName());
		CLASS_IZINKAN.add(KegiatanKedosenanPunyaDosen.class.getName());
		CLASS_IZINKAN.add(OrganisasiDosenPunyaDosen.class.getName());
		CLASS_IZINKAN.add(PrestasiDosen.class.getName());
		CLASS_IZINKAN.add(PenghargaanDosen.class.getName());
		CLASS_IZINKAN.add(ChecklistBaruPenilaianDosenOlehMahasiswa.class.getName());
		CLASS_IZINKAN.add(PengajuanPenelitianDanPengabdian.class.getName());
		CLASS_IZINKAN.add(Artikel.class.getName());
		CLASS_IZINKAN.add(BukuBahanAjar.class.getName());
		CLASS_IZINKAN.add(ChecklistHasilPenilaianUmum.class.getName());
		CLASS_IZINKAN.add(FormatNilai.class.getName());
		CLASS_IZINKAN.add(Detailperkuliahan.class.getName());
		CLASS_IZINKAN.add(UjianPunyaSoal.class.getName());
		CLASS_IZINKAN.add(BankSoal.class.getName());
		CLASS_IZINKAN.add(BankSoalDetail.class.getName());
		CLASS_IZINKAN.add(PengaturanBiaya.class.getName());
	}

	/**
	 * Menentukan apakah sebuah kelas entity termasuk data referensi yang TIDAK boleh dibuang dari
	 * cache saat pembersihan berkala.
	 *
	 * <p><b>Tujuan.</b> Pembersih cache periodik perlu tahu entity mana yang "abadi" (master/referensi
	 * yang jarang berubah dan sering dibaca) agar tidak ikut dibuang—membuangnya hanya akan memaksa
	 * pembacaan ulang dari DB yang mahal. Method ini adalah sumber kebenaran tunggal untuk keputusan
	 * tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan {@code false} bila {@code clazz} null; selain itu mengecek
	 * keanggotaan {@code clazz.getName()} di {@link HashSet} statis {@code CLASS_JANGAN_DIBERSIHKAN}
	 * (lookup O(1)). Daftar itu diisi sekali di blok {@code static} dengan puluhan kelas master
	 * (Tbmuser, Menu, Kurikulum, master gaji/KPI/penilaian, dll.).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code clazz} = kelas entity (boleh null). Mengembalikan
	 * {@code true} bila kelas tergolong "jangan dibersihkan".</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk menjadikan entity baru tahan-pembersihan, tambahkan ke
	 * {@code CLASS_JANGAN_DIBERSIHKAN} di blok static. Perhatikan: pencocokan memakai nama kelas
	 * lengkap—pastikan menambah {@code .getName()} dari kelas yang benar.</p>
	 *
	 * @param clazz kelas entity yang diperiksa (boleh null)
	 * @return {@code true} bila tergolong data yang jangan dibersihkan dari cache
	 */
	@SuppressWarnings("rawtypes")
	public static boolean merupakanJanganDibersihkan(Class clazz) {
		if (clazz == null)
			return false;
		return CLASS_JANGAN_DIBERSIHKAN.contains(clazz.getName());
	}

	/**
	 * Mencatat perubahan/audit sebuah entity TANPA menyimpan aktivitas pengguna (overload ringkas).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Memperbarui stempel audit entity (kolom
	 * {@code tanggal_dirubah}/{@code oleh}) untuk operasi yang tidak perlu mencatat log aktivitas
	 * terpisah. Mendelegasikan ke {@link #ubahDataHistory(Serializable, Integer, boolean)} dengan
	 * {@code jenis=null} dan {@code simpanActivity=false}.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code serializable} = entity yang diubah. Pakai
	 * overload ini untuk perubahan rutin; gunakan {@link #ubahDataHistory(Serializable, Integer)}
	 * bila aktivitas pengguna perlu tercatat.</p>
	 *
	 * @param serializable entity yang stempel auditnya diperbarui
	 */
	public static void ubahDataHistory(Serializable serializable) {
		ubahDataHistory(serializable, null, false);
	}

	/**
	 * Mencatat perubahan/audit sebuah entity SEKALIGUS menyimpan aktivitas pengguna (overload dengan
	 * jenis operasi).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Sama seperti overload ringkas, tetapi juga mencatat aktivitas
	 * pengguna (siapa mengubah apa) dengan kode {@code jenis} operasi. Mendelegasikan ke
	 * {@link #ubahDataHistory(Serializable, Integer, boolean)} dengan {@code simpanActivity=true}.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code serializable} = entity yang diubah; {@code jenis}
	 * = kode jenis operasi (mis. tambah/ubah/hapus) untuk log aktivitas. Pakai overload ini bila jejak
	 * audit pengguna diperlukan.</p>
	 *
	 * @param serializable entity yang diubah
	 * @param jenis        kode jenis operasi untuk log aktivitas
	 */
	public static void ubahDataHistory(Serializable serializable, Integer jenis) {
		ubahDataHistory(serializable, jenis, true);
	}

	/**
	 * Overload internal yang melengkapi pemanggilan audit dengan pengguna saat ini.
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Mengambil {@link Tbmuser} aktif via
	 * {@code Common.getCurrentUser()} lalu meneruskannya ke
	 * {@link #ubahDataHistory(Serializable, Integer, boolean, Tbmuser)}. Memisahkan pengambilan user
	 * di sini menjaga method inti tetap dapat diuji/dipakai dengan user eksplisit bila perlu.</p>
	 *
	 * <p><b>Parameter &amp; pemeliharaan.</b> {@code serializable} = entity; {@code jenis} = kode
	 * operasi; {@code simpanActivity} = apakah aktivitas pengguna dicatat. Method privat; titik masuk
	 * publik adalah dua overload di atas.</p>
	 *
	 * @param serializable   entity yang diubah
	 * @param jenis          kode jenis operasi
	 * @param simpanActivity {@code true} untuk mencatat aktivitas pengguna
	 */
	private static void ubahDataHistory(Serializable serializable, Integer jenis, boolean simpanActivity) {
		Tbmuser tbmuser = ais.common.Common.getCurrentUser();
		ubahDataHistory(serializable, jenis, simpanActivity, tbmuser);
	}

	/**
	 * Inti pencatatan audit + (opsional) aktivitas pengguna untuk sebuah entity.
	 *
	 * <p><b>Tujuan.</b> Memberi stempel audit yang konsisten pada perubahan entity dan—bila diminta—
	 * mencatat siapa melakukan perubahan dari Action mana, tanpa membebani entity-entity teknis yang
	 * tidak relevan untuk diaudit.</p>
	 *
	 * <p><b>Cara kerja.</b> (1) Melewati (skip) entity yang tidak boleh diaudit:
	 * {@code AccessedUsers}, {@code DetailLogLogin}, {@code LogUserActifity}, {@code MemoryInfo},
	 * {@code RolePrivilage}, {@code OnlineUsers}, {@code FileFoto}, {@code LampiranLain}—ini data
	 * log/teknis/biner yang justru akan membanjiri audit. (2) Memanggil
	 * {@code AuditTimestampInterceptor.ubah(serializable)} untuk stempel waktu/pelaku. (3) Bila
	 * {@code simpanActivity}, ia menelusuri {@code Thread.currentThread().getStackTrace()} untuk
	 * menemukan kelas Action pemicu—mencari prefiks paket berurutan dari yang paling spesifik
	 * ({@code ais.action.master}) hingga paling umum ({@code ais.}). Penelusuran stack dipilih karena
	 * jauh lebih hemat daripada melempar-menangkap {@code Exception} manual untuk memperoleh konteks
	 * pemanggil. Bila kelas ditemukan, {@code CommonPrivilages.saveActivity(...)} dipanggil.</p>
	 *
	 * <p><b>Parameter.</b> {@code serializable} = entity; {@code jenis} = kode operasi;
	 * {@code simpanActivity} = apakah aktivitas dicatat; {@code tbmuser} = pengguna pelaku (sudah
	 * diisi oleh overload pemanggil).</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Bagian pencatatan aktivitas dibungkus try/catch
	 * yang hanya {@code printStackTrace}—kegagalan audit TIDAK boleh menggagalkan transaksi bisnis.
	 * Bila menambah entity teknis baru yang tak perlu diaudit, tambahkan ke daftar {@code instanceof}
	 * pengecualian. Urutan prefiks paket sengaja dari spesifik ke umum agar konteks Action yang paling
	 * tepat yang tercatat.</p>
	 *
	 * @param serializable   entity yang diubah (di-skip bila tipe teknis tertentu)
	 * @param jenis          kode jenis operasi
	 * @param simpanActivity {@code true} untuk mencatat aktivitas pengguna
	 * @param tbmuser        pengguna pelaku perubahan
	 */
	@SuppressWarnings("rawtypes")
	private static void ubahDataHistory(Serializable serializable, Integer jenis, boolean simpanActivity,
			Tbmuser tbmuser) {

		if (serializable != null && !(serializable instanceof AccessedUsers)
				&& !(serializable instanceof DetailLogLogin) && !(serializable instanceof LogUserActifity)
				&& !(serializable instanceof MemoryInfo) && !(serializable instanceof RolePrivilage)
				&& !(serializable instanceof OnlineUsers) && !(serializable instanceof FileFoto)
				&& !(serializable instanceof LampiranLain)) {

			AuditTimestampInterceptor.ubah(serializable);

			try {
				if (simpanActivity) {
					String perubahan = "";
					Class myClass = null;

					// OPTIMASI: Stack trace dicari menggunakan Thread.currentThread(),
					// Tidak perlu try-catch dan melempar throw new Exception secara manual (sangat
					// menghemat memori).
					StackTraceElement[] elements = Thread.currentThread().getStackTrace();
					String[] packagePrefixes = { "ais.action.master", "ais.action.maintenance", "ais.action",
							"ais.model", "ais.common", "ais." };

					for (String prefix : packagePrefixes) {
						for (StackTraceElement element : elements) {
							if (element.getClassName().startsWith(prefix)) {
								try {
									myClass = Class.forName(element.getClassName());
								} catch (ClassNotFoundException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:533");
									// ignore, biarkan null
								}
								break;
							}
						}
						if (myClass != null)
							break;
					}

					if (myClass != null) {
						CommonPrivilages.saveActivity(myClass, jenis, serializable, perubahan);
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:548");
			}
		}
	}

	/**
	 * Menyetel nilai sebuah properti entity secara reflektif, dengan validasi keberadaan bila nilainya
	 * adalah relasi entity.
	 *
	 * <p><b>Tujuan.</b> Menulis nilai ke properti entity lewat metadata Hibernate (bukan setter Java
	 * langsung), sembari MENCEGAH penulisan relasi (FK) ke baris yang sudah tidak ada di DB—penyebab
	 * klasik pelanggaran foreign key saat menyimpan.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila {@code value} adalah {@link GeneralValueObject} ber-id, method
	 * terlebih dulu menghitung keberadaan baris tersebut
	 * ({@code createCriteria(value.getClass()).add(idEq).setProjection(rowCount)}); properti hanya
	 * disetel bila {@code count > 0}. Untuk nilai non-entity (atau entity tanpa id), nilai langsung
	 * disetel via {@code classMetadata.setPropertyValue(..., EntityMode.POJO)}.</p>
	 *
	 * <p><b>Parameter.</b> {@code data} = entity target; {@code classMetadata} = metadata pemetaan
	 * entity; {@code property} = nama properti; {@code value} = nilai baru (boleh entity relasi);
	 * {@code session} = session untuk verifikasi keberadaan relasi.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Seluruh blok dibungkus try/catch yang sengaja
	 * "menelan" exception (mengikuti perilaku asli) agar satu kegagalan set tidak menghentikan
	 * penyalinan properti lain. Ada {@code System.out.println} diagnostik yang dipertahankan dari
	 * versi asli. Jangan mengubah validasi keberadaan relasi—itulah pelindung FK-nya.</p>
	 *
	 * @param data          entity target
	 * @param classMetadata metadata pemetaan entity
	 * @param property      nama properti yang disetel
	 * @param value         nilai baru (boleh entity relasi)
	 * @param session       session untuk verifikasi keberadaan relasi
	 */
	public static void setValueData(Serializable data, ClassMetadata classMetadata, String property, Object value,
			Session session) {
		try {
			System.out.println("Ubah data " + property + " ke " + value);

			if (value != null && value instanceof GeneralValueObject && value.getClass() != null) {
				GeneralValueObject generalValueObject = (GeneralValueObject) value;
				if (generalValueObject.getId() != null) {
					int c = ((Number) session.createCriteria(value.getClass())
							.add(Restrictions.idEq(generalValueObject.getId())).setProjection(Projections.rowCount())
							.uniqueResult()).intValue();
					if (c > 0) {
						classMetadata.setPropertyValue(data, property, value, EntityMode.POJO);
					}
				}
			} else {
				classMetadata.setPropertyValue(data, property, value, EntityMode.POJO);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:600");
			// Sengaja ditutup sesuai aslinya e.printStackTrace();
		}
	}

	/**
	 * Menaruh/menyegarkan sebuah entity di cache MapDB pada KEY yang diberikan secara langsung.
	 *
	 * <p><b>Tujuan.</b> Varian "by key eksplisit" dari penyimpanan cache, dipakai untuk entity yang
	 * dirujuk lewat kunci komposit/turunan (bukan sekadar id), mis. HasilUjianMahasiswa dan
	 * HistoryStatusMahasiswa. Memastikan pembaca yang mencari lewat key tersebut memperoleh data
	 * terkini.</p>
	 *
	 * <p><b>Cara kerja.</b> Hanya berjalan bila {@link #izinkan(Class)} ({@code clazzx} termasuk
	 * {@code CLASS_IZINKAN}). Nama map dibentuk dari prefiks kelas + {@code "_data_" + getCODE()}.
	 * Bila key sudah ada, properti objek lama DISALIN-timpa dengan {@code BeanUtilsBean.copyProperties}
	 * (mempertahankan identitas objek cache); bila gagal menyalin atau key belum ada, objek baru
	 * langsung di-{@code put}. Pola salin-timpa ini menjaga referensi yang mungkin dipegang pemanggil
	 * lain tetap menunjuk data terbaru.</p>
	 *
	 * <p><b>Parameter.</b> {@code clazzx} = kelas entity; {@code obj} = entity sumber data terbaru;
	 * {@code key} = kunci cache eksplisit.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Semua dibungkus try/catch dengan
	 * {@code printStackTrace}; kegagalan cache tidak boleh menjatuhkan alur bisnis. Pastikan key yang
	 * dipakai konsisten dengan yang dipakai pembaca ({@code ambilDataLangsung}). Jangan memakai method
	 * ini untuk entity di luar {@code CLASS_IZINKAN}—pemanggilannya akan diabaikan.</p>
	 *
	 * @param clazzx kelas entity (harus diizinkan)
	 * @param obj    entity dengan data terbaru
	 * @param key    kunci cache eksplisit
	 */
	// ====================================================================================
	// IDENTITY MAP JVM-WIDE (SoftReference) — jaminan "satu objek per (kelas, id)".
	// ------------------------------------------------------------------------------------
	// MASALAH yang diselesaikan: cache MapDB (MemoryCacheUtil) menyimpan hasil SERIALISASI,
	// sehingga get() mengembalikan SALINAN objek yang BERBEDA setiap kali. Akibatnya objek
	// dengan id sama yang diakses dari halaman berbeda bisa DIVERGEN (mis. NIM tampil beda
	// di form vs daftar). Lapisan ini menyimpan satu INSTANCE KANONIK per (kelas, id):
	//  - ambilData()/ambilDataLangsung() mengembalikan instance yang SAMA selama masih di RAM;
	//  - memanggil setter pada instance itu langsung terlihat dari halaman mana pun;
	//  - masukkanData()/masukkanDataLangsung() menyalin data terbaru ke instance kanonik
	//    (copyProperties) sehingga semua pemegang ikut ter-update.
	// AMAN MEMORI: memakai SoftReference — bila heap tertekan, entri boleh di-evict GC (tidak
	// OOM); saat diakses lagi ia dimuat ulang & menjadi kanonik baru. Kunci konsisten: SELALU
	// (kelas, id) — sehingga akses by-id (ambilData) maupun by-key (ambilDataLangsung) menuju
	// instance yang sama.
	// Cache resolusi nama-kelas -> Class (hindari Class.forName berulang di jalur panas identity map).
	@SuppressWarnings("rawtypes")
	private static final java.util.concurrent.ConcurrentHashMap<String, Class> KELAS_CACHE =
			new java.util.concurrent.ConcurrentHashMap<String, Class>();

	/**
	 * Saklar pemakaian MapDB untuk CACHE ENTITY (jalur ambilData/masukkanData/ambilDataLangsung/hapus).
	 * <b>DEFAULT ON</b> (true). Konsistensi objek per-(kelas,id) DIJAMIN identity map baik ON maupun OFF.
	 *
	 * <p><b>Mengapa default ON:</b> menonaktifkan MapDB membuat pola loop {@code ambilData(Kelas,id)}
	 * per-item — mis. pemuatan timeline e-Learning yang menyusuri RIBUAN {@code Pertemuan} satu per satu —
	 * berubah menjadi RIBUAN query DB sinkron sehingga halaman MENGGANTUNG. Karena itu OFF hanya untuk
	 * yang paham konsekuensinya.</p>
	 *
	 * <p>Saat OFF (false): cache entity mengandalkan {@link EntityIdentityMap} (RAM, WeakReference) + fallback
	 * DB, TANPA menyentuh MapDB — menghindari ketidakstabilan MapDB (ClosedChannelException → IOError),
	 * dengan biaya lebih banyak query DB pada cache miss. Di-set dari konfigurasi startup
	 * ({@code cache_entity_menggunakan_mapdb}). Hanya memengaruhi cache ENTITY di DataUtil — pemakaian
	 * {@link MemoryCacheUtil} lain (broadcast, ringkasan, flag) TIDAK terpengaruh.</p>
	 */
	private static volatile boolean PAKAI_MAPDB_ENTITY = true;

	/** Set dari startup untuk mengaktifkan kembali MapDB pada cache entity (default OFF). */
	public static void setPakaiMapDbEntity(boolean pakai) {
		PAKAI_MAPDB_ENTITY = pakai;
	}

	public static boolean isPakaiMapDbEntity() {
		return PAKAI_MAPDB_ENTITY;
	}

	// ── IDENTITY MAP TUNGGAL: delegasi ke EntityIdentityMap (JVM-wide, WeakReference, key=class#id) ──
	// Sebelumnya DataUtil punya IDENTITY_MAP sendiri (SoftReference, strategi "ganti-referensi") YANG
	// TERPISAH dari EntityIdentityMap → dua registry bisa menyimpan instance BERBEDA utk (kelas,id) yang
	// SAMA = lebih dari satu instance. Kini SEMUA jalur identity DataUtil memakai EntityIdentityMap agar
	// dijamin HANYA ADA SATU instance kanonik per (kelas,id) di seluruh JVM. Strategi EntityIdentityMap:
	// instance PERTAMA jadi kanonik; data terbaru di-MERGE (scalar) ke kanonik yang sudah ada (bukan
	// mengganti referensi) → pemegang lama pun melihat data terbaru TANPA memunculkan salinan kedua.

	/** Resolusi nama-kelas (proxy sudah dipangkas) -> Class anak GeneralValueObject; null bila bukan. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Class<? extends GeneralValueObject> resolveGvoClass(String clazzs) {
		if (clazzs == null || clazzs.trim().length() == 0) {
			return null;
		}
		Class cached = KELAS_CACHE.get(clazzs);
		if (cached == null) {
			try {
				cached = Class.forName(clazzs);
			} catch (Throwable t) {
				return null;
			}
			KELAS_CACHE.putIfAbsent(clazzs, cached);
		}
		return GeneralValueObject.class.isAssignableFrom(cached) ? (Class<? extends GeneralValueObject>) cached : null;
	}

	/** Parse id numerik dgn aman; null bila kosong / bukan angka (mis. kunci komposit → bukan identity by-id). */
	private static Long parseLongSafe(String id) {
		if (id == null) {
			return null;
		}
		String t = id.trim();
		if (t.length() == 0) {
			return null;
		}
		try {
			return Long.valueOf(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Ambil instance kanonik utk (kelas,id) dari EntityIdentityMap; null bila belum ada / id bukan numerik. */
	private static GeneralValueObject identityGet(String clazzs, String id) {
		Long idLong = parseLongSafe(id);
		if (idLong == null) {
			return null;
		}
		Class<? extends GeneralValueObject> clazz = resolveGvoClass(clazzs);
		if (clazz == null) {
			return null;
		}
		return EntityIdentityMap.get(clazz, idLong);
	}

	/** Daftarkan {@code obj} sebagai kanonik (kelas,id) di EntityIdentityMap (merge scalar bila sudah ada). */
	private static void identityPut(String clazzs, String id, GeneralValueObject obj) {
		if (obj == null || obj.getId() == null) {
			return;
		}
		EntityIdentityMap.canonical(obj);
	}

	/** Hapus instance kanonik utk (kelas,id) (dipanggil saat entity dihapus). */
	private static void identityRemove(String clazzs, String id) {
		Long idLong = parseLongSafe(id);
		Class<? extends GeneralValueObject> clazz = resolveGvoClass(clazzs);
		if (idLong != null && clazz != null) {
			EntityIdentityMap.evict(clazz, idLong);
		}
	}

	/**
	 * Jadikan {@code obj} kanonik terbaru untuk (kelas,id). Delegasi ke
	 * {@link EntityIdentityMap#canonical(GeneralValueObject)}: bila kanonik BELUM ada, {@code obj} jadi
	 * kanonik; bila SUDAH ada (instance lain, id sama), scalar field terbaru dari {@code obj} di-MERGE ke
	 * instance kanonik yang ada (tidak mengganti referensi, tidak memanggil getter lazy). Dengan begitu
	 * TIDAK pernah ada dua instance untuk (kelas,id) yang sama, dan semua pemegang referensi kanonik
	 * langsung melihat data terbaru.
	 */
	private static void identityRefresh(String clazzs, GeneralValueObject obj) {
		if (obj == null || obj.getId() == null) {
			return;
		}
		EntityIdentityMap.canonical(obj);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void masukkanDataLangsung(Class clazzx, GeneralValueObject obj, String key) {
		if (izinkan(clazzx)) {
			String clazzs = StringUtils.split(clazzx.getName(), "_")[0];
			try {
				Map<String, GeneralValueObject> dataYangTerambilBerdasarkanId = PAKAI_MAPDB_ENTITY ? MemoryCacheUtil.get(clazzs + "_data_" + MemoryCacheUtil.getCODE()) : null;

				if (dataYangTerambilBerdasarkanId != null) {
					GeneralValueObject data = dataYangTerambilBerdasarkanId.get(key);
					if (data != null) {
						try {
							BeanUtilsBean.getInstance().copyProperties(data, obj);
						} catch (Exception e) {
							try { dataYangTerambilBerdasarkanId.put(key, obj); } catch (Throwable oom) { ais.common.ErrorAuditUtil.record(oom, "auto-audit(empty-catch) src/ais/common/DataUtil.java:778"); /* MapDB mmap OOM, abaikan cache */ }
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:779");
						}
					} else {
						try { dataYangTerambilBerdasarkanId.put(key, obj); } catch (Throwable oom) { ais.common.ErrorAuditUtil.record(oom, "auto-audit(empty-catch) src/ais/common/DataUtil.java:782"); /* MapDB mmap OOM, abaikan cache */ }
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:786");
			}

			// IDENTITY MAP: perbarui instance KANONIK (kelas,id) dengan data terbaru → semua halaman
			// yang memegang objek id ini ikut melihat perubahan. (Aman: SoftReference, boleh di-evict.)
			identityRefresh(clazzs, obj);
		}
	}

	/**
	 * Mengambil entity dari cache MapDB berdasarkan KEY eksplisit (tanpa fallback ke DB).
	 *
	 * <p><b>Tujuan.</b> Pasangan baca untuk {@link #masukkanDataLangsung(Class, GeneralValueObject,
	 * String)}: mengambil entity yang disimpan di bawah kunci komposit/turunan. Berguna untuk lookup
	 * cepat hasil ujian/riwayat status yang tidak dikunci sekadar oleh id.</p>
	 *
	 * <p><b>Cara kerja.</b> Membentuk nama map dari prefiks kelas + {@code "_data_" + getCODE()},
	 * mengambil map via {@link MemoryCacheUtil#get(String)}, lalu mengembalikan {@code map.get(d)}.
	 * Bila map null (belum pernah dibuat) mengembalikan {@code null}. Tidak ada pembacaan ke DB di
	 * sini—murni cache.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code clazz} = kelas entity; {@code d} = kunci cache.
	 * Mengembalikan entity ter-cache atau {@code null} bila tidak ada.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Karena tidak ada fallback DB, pemanggil harus siap menerima {@code null}
	 * dan memutuskan sendiri apakah perlu membaca DB. Pastikan format key sama persis dengan yang
	 * dipakai saat menyimpan.</p>
	 *
	 * @param clazz kelas entity
	 * @param d     kunci cache
	 * @return entity ter-cache, atau null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GeneralValueObject ambilDataLangsung(Class clazz, String d) {
		String clazzs = StringUtils.split(clazz.getName(), "_")[0];
		Map<String, GeneralValueObject> dataYangTerambilBerdasarkanId = PAKAI_MAPDB_ENTITY ? MemoryCacheUtil.get(clazzs + "_data_" + MemoryCacheUtil.getCODE()) : null;

		GeneralValueObject data = null;
		if (dataYangTerambilBerdasarkanId != null) {
			data = dataYangTerambilBerdasarkanId.get(d);
		}
		// IDENTITY MAP: objek yang dibaca by-key (mis. HasilUjianMahasiswa via keyhasil) diarahkan ke
		// instance KANONIK (kelas,id) yang sama. canonical() mengembalikan instance kanonik (fresh jadi
		// kanonik bila belum ada, atau kanonik lama dgn scalar ter-merge) → PASTIKAN yang dikembalikan
		// SELALU instance tunggal itu, termasuk saat balapan pembacaan konkuren.
		if (data != null && data.getId() != null) {
			GeneralValueObject kanonik = EntityIdentityMap.canonical(data);
			return kanonik != null ? kanonik : data;
		}
		return data;
	}

	/**
	 * Menyimpan/menyegarkan sebuah entity ke cache yang tepat—titik masuk UTAMA jalur tulis.
	 *
	 * <p><b>Tujuan (PENTING).</b> Inilah method yang WAJIB dipanggil setiap kali sebuah entity
	 * ter-cache disimpan/diubah, agar cache tidak basi. Mengabaikannya adalah akar bug "data lama/skor
	 * 0" pada modul ujian, karena pembaca akan terus melihat salinan lama dari MapDB.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Bila kelas terdaftar di {@link ConstantValues} ({@code classExist}), perbaruan didelegasikan
	 *   ke {@code InitDataHelper.reInitDataUpdate(obj)} (jalur konstanta in-memory).</li>
	 *   <li>Selain itu, bila {@code obj} ber-id dan {@link #izinkan(Class)}: untuk
	 *   {@code HasilUjianMahasiswa} dan {@code HistoryStatusMahasiswa}, key komposit dibentuk lalu
	 *   {@link #masukkanDataLangsung(Class, GeneralValueObject, String)} dipanggil agar map by-key ikut
	 *   tersegarkan—bukan hanya map by-id.</li>
	 *   <li>Terakhir, entity disimpan ke map {@code <kelas>_data_<CODE>} dengan key = id (salin-timpa
	 *   bila sudah ada via {@code BeanUtilsBean}, atau {@code put} bila belum).</li>
	 * </ol>
	 *
	 * <p><b>Parameter.</b> {@code clazzx} = kelas entity; {@code obj} = entity dengan data terbaru
	 * (harus ber-id untuk jalur cache by-id).</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch ({@code printStackTrace});
	 * kegagalan cache tidak menggagalkan bisnis. Bila menambah entity ber-kunci komposit baru,
	 * tambahkan cabang khusus seperti HasilUjianMahasiswa/HistoryStatusMahasiswa agar map by-key
	 * konsisten dengan map by-id.</p>
	 *
	 * @param clazzx kelas entity
	 * @param obj    entity dengan data terbaru
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void masukkanData(Class clazzx, GeneralValueObject obj) {
		String clazzs = StringUtils.split(clazzx.getName(), "_")[0];
		if (ConstantValues.classExist(clazzs)) {
			InitDataHelper.reInitDataUpdate(obj);
		} else if (obj.getId() != null && izinkan(clazzx)) {

			if (clazzs.equalsIgnoreCase(HasilUjianMahasiswa.class.getName())) {
				HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) obj;
				String keyhasil = HasilUjianMahasiswa.genKey(hasilUjianMahasiswa.getPertemuanPunyaUjian(),
						hasilUjianMahasiswa.getMahasiswa(), hasilUjianMahasiswa.getBiodataCalonMahasiswa(),
						hasilUjianMahasiswa.getSiswa(), hasilUjianMahasiswa.getCalonSiswa());
				masukkanDataLangsung(clazzx, obj, keyhasil);
			} else if (clazzs.equalsIgnoreCase(HistoryStatusMahasiswa.class.getName())) {
				HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) obj;
				Mahasiswa mahasiswa = historyStatusMahasiswa.getMahasiswa();
				Integer tahap = historyStatusMahasiswa.getTahap();
				Integer semester = historyStatusMahasiswa.getSemester();
				// FIX NPE (fast-throw) jalur ringkasan-build: HistoryStatusMahasiswa hasil deserialisasi JSON
				// bisa punya mahasiswa null -> mahasiswa.getId() NPE & MEMBATALKAN seluruh masukkanData
				// (identity-map by-id ikut tak ter-update -> risiko cache basi). Bila mahasiswa/id null,
				// lewati update by-key saja; sisa method tetap jalan.
				if (mahasiswa != null && mahasiswa.getId() != null) {
					String key = HistoryStatusMahasiswa.class.getSimpleName() + "_" + mahasiswa.getId() + "_"
							+ (tahap == null || tahap.equals(0) ? semester : tahap);
					masukkanDataLangsung(clazzx, obj, key);
				}
			}

			try {
				Map<String, GeneralValueObject> dataYangTerambilBerdasarkanId = PAKAI_MAPDB_ENTITY ? MemoryCacheUtil.get(clazzs + "_data_" + MemoryCacheUtil.getCODE()) : null;

				if (dataYangTerambilBerdasarkanId != null) {
					String key = obj.getId().toString();
					GeneralValueObject data = dataYangTerambilBerdasarkanId.get(key);

					if (data != null) {
						try {
							BeanUtilsBean.getInstance().copyProperties(data, obj);
						} catch (Exception e) {
							try { dataYangTerambilBerdasarkanId.put(key, obj); } catch (Throwable oom) { ais.common.ErrorAuditUtil.record(oom, "auto-audit(empty-catch) src/ais/common/DataUtil.java:902"); /* MapDB mmap OOM, abaikan cache */ }
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:903");
						}
					} else {
						try { dataYangTerambilBerdasarkanId.put(key, obj); } catch (Throwable oom) { ais.common.ErrorAuditUtil.record(oom, "auto-audit(empty-catch) src/ais/common/DataUtil.java:906"); /* MapDB mmap OOM, abaikan cache */ }
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:910");
			}

			// IDENTITY MAP: perbarui instance KANONIK (kelas,id) dengan data terbaru → semua halaman
			// yang memegang objek id ini ikut melihat perubahan. (Aman: SoftReference, boleh di-evict.)
			identityRefresh(clazzs, obj);
		}
	}

	/**
	 * Mengambil entity berdasarkan id/key dari cache (overload tanpa fallback DB).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Bentuk ringkas dari
	 * {@link #ambilData(Class, String, boolean)} dengan {@code jikaNggakKetemucari=false}: hanya
	 * membaca dari konstanta/cache, tidak menyentuh DB bila tidak ditemukan. Dipakai pada jalur yang
	 * toleran terhadap cache-miss atau yang sudah memastikan data ada.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code clazz} = kelas entity; {@code key} = id/kunci.
	 * Mengembalikan entity atau {@code null}.</p>
	 *
	 * @param clazz kelas entity
	 * @param key   id/kunci entity
	 * @return entity dari cache/konstanta, atau null
	 */
	@SuppressWarnings("rawtypes")
	public static GeneralValueObject ambilData(Class clazz, String key) {
		return ambilData(clazz, key, false);
	}

	/**
	 * Mengambil entity berdasarkan id/key, dengan opsi membaca dari DB bila tidak ada di cache.
	 *
	 * <p><b>Tujuan.</b> Titik baca UTAMA untuk entity ter-cache. Menggabungkan tiga sumber—konstanta
	 * in-memory, cache MapDB, dan (opsional) DB—agar pemanggil cukup memanggil satu method dan
	 * memperoleh entity dengan biaya seminimal mungkin.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Bila kelas terdaftar di {@link ConstantValues} dan key numerik, ambil dari konstanta
	 *   ({@code ConstantValues.ambil}).</li>
	 *   <li>Selain itu, ambil dari map cache {@code <kelas>_data_<CODE>} by key.</li>
	 *   <li>Bila {@code jikaNggakKetemucari} true, data masih null, dan key numerik: baca dari DB lewat
	 *   {@link HibernateUtil#currentNativeSession()} ({@code idEq}), dan bila ketemu, simpan ke cache
	 *   via {@link #masukkanData(Class, GeneralValueObject)} agar pembacaan berikutnya cepat.</li>
	 * </ol>
	 *
	 * <p><b>Higiene koneksi.</b> Pada jalur baca DB, session ditutup TUNTAS di blok {@code finally}
	 * (disconnect + close + {@link HibernateUtil#closeSession()}) untuk mencegah kebocoran koneksi
	 * pool—pelajaran penting saat memakai c3p0.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code c} = kelas entity; {@code key} = id/kunci;
	 * {@code jikaNggakKetemucari} = izinkan fallback baca DB. Mengembalikan entity atau {@code null}.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Baca DB dibungkus try/catch; kegagalan di-log dan
	 * mengembalikan apa adanya (null). Jangan menghapus penutupan session di {@code finally}. Aktifkan
	 * {@code jikaNggakKetemucari} hanya bila benar-benar butuh data—itu menambah akses DB.</p>
	 *
	 * @param c                   kelas entity
	 * @param key                 id/kunci entity
	 * @param jikaNggakKetemucari {@code true} untuk fallback membaca dari DB bila cache-miss
	 * @return entity dari konstanta/cache/DB, atau null
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GeneralValueObject ambilData(Class c, String key, boolean jikaNggakKetemucari) {
		// Guard key null: cache entity berbasis ConcurrentHashMap yang melempar NullPointerException
		// pada get(null)/containsKey(null). Key null berarti tidak ada entity untuk diambil.
		if (key == null) {
			return null;
		}
		String clazzs = StringUtils.split(c.getName(), "_")[0];
		if (ConstantValues.classExist(c) && Common.isNumber(key)) {
			return ConstantValues.ambil(clazzs, Long.parseLong(key));
		} else {
			// IDENTITY MAP: bila instance KANONIK utk (kelas,id) ini masih di memori, kembalikan yang
			// SAMA (bukan salinan MapDB) → setter yang dipanggil di halaman lain langsung konsisten.
			GeneralValueObject kanonik = identityGet(clazzs, key);
			if (kanonik != null) {
				// SENGAJA TIDAK mencatat ke EntityAccessCache di sini: titik ini adalah CACHE-HIT generik
				// yang dipicu oleh SIAPA PUN/PROSES APA PUN (laporan batch, dashboard warmup, proses
				// internal lain) yang membaca ulang entity yang SUDAH di memori — termasuk entity yang
				// baru saja di-preload oleh InitDataHelper sendiri saat boot. Karena aplikasi restart
				// otomatis tiap hari, mencatat di sini akan membuat riwayat "3 hari terakhir" TAK PERNAH
				// menyusut (setiap boot me-refresh cap-akses id yang sudah pernah di-preload) — akhirnya
				// mencakup semua id yang pernah dimuat. Lihat ais.common.EntityAccessCache.
				return kanonik;
			}

			GeneralValueObject data = null;
			if (PAKAI_MAPDB_ENTITY) {
				Map<String, GeneralValueObject> dataYangTerambilBerdasarkanId = PAKAI_MAPDB_ENTITY ? MemoryCacheUtil.get(clazzs + "_data_" + MemoryCacheUtil.getCODE()) : null;
				if (dataYangTerambilBerdasarkanId != null) {
					data = dataYangTerambilBerdasarkanId.get(key);
				}
			}

			// MapDB OFF → cache entity = identity map saja; pada MISS WAJIB ambil dari DB agar data tetap
			// tampil (identity map lalu diisi di akhir). Karena itu fallback DB dipaksa saat OFF.
			if ((jikaNggakKetemucari || !PAKAI_MAPDB_ENTITY) && data == null && Common.isNumber(key)) {
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					data = (GeneralValueObject) session.createCriteria(c).add(Restrictions.idEq(Long.parseLong(key)))
							.uniqueResult();

					if (data != null) {
						masukkanData(c, data);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:1018");
				} finally {
					// OPTIMASI: Pastikan session selalu ditutup dengan rapi dan aman tanpa
					// membocorkan koneksi.
					// FIX SessionException "Session was already closed": session ini bisa sudah
					// ditutup pihak lain (mis. currentNativeSession() dipakai bersamaan/self-heal
					// di request lain pada thread yang sama) -- cek isOpen() dulu sebelum
					// clear/disconnect/close supaya tidak melempar exception yang cuma akan
					// tertangkap & jadi noise di audit log.
					if (session != null && session.isOpen()) {
						try {
							session.clear();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:1026");
							// Ignore clear error
						}
						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:1025");
							// Ignore disconnect error
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:1030");
							// Ignore close error
						}
					}
					HibernateUtil.closeSession();
				}
			}
			// Kembalikan instance KANONIK (kelas,id): fresh jadi kanonik bila belum ada, atau kanonik lama
			// dgn scalar ter-merge — sehingga TIDAK ada dua instance beredar utk id sama (juga saat balapan).
			if (data != null && data.getId() != null) {
				// SENGAJA TIDAK mencatat ke EntityAccessCache di sini juga — lihat penjelasan di titik
				// cache-hit identityGet di atas. DataUtil.ambilData dipanggil oleh proses internal
				// (laporan/batch/warmup) maupun request user asli tanpa bisa dibedakan dari sini,
				// sehingga BUKAN sinyal yang aman untuk "benar-benar diakses user".
				kanonik = EntityIdentityMap.canonical(data);
				return kanonik != null ? kanonik : data;
			}
			return data;
		}
	}

	/**
	 * Menghapus entity dari cache/konstanta berdasarkan id/key—pasangan hapus untuk jalur tulis.
	 *
	 * <p><b>Tujuan (PENTING).</b> Harus dipanggil setiap kali entity ter-cache dihapus dari DB, agar
	 * salinan basi tidak tertinggal di cache dan disajikan ke pembaca berikutnya.</p>
	 *
	 * <p><b>Cara kerja.</b> Bila kelas terdaftar di {@link ConstantValues} dan key numerik, dihapus
	 * dari konstanta ({@code ConstantValues.hapus}). Selain itu, bila {@link #izinkan(Class)}, entry
	 * dihapus dari map {@code <kelas>_data_<CODE>} via {@code map.remove(key)} (aman bila map null).</p>
	 *
	 * <p><b>Parameter.</b> {@code c} = kelas entity; {@code key} = id/kunci yang dihapus.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Untuk entity ber-kunci komposit, pertimbangkan menghapus juga map by-key
	 * turunannya bila relevan. Hanya entity {@code CLASS_IZINKAN} yang diproses di jalur cache.</p>
	 *
	 * @param c   kelas entity
	 * @param key id/kunci entity yang dihapus
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void hapus(Class c, String key) {
		String clazzs = StringUtils.split(c.getName(), "_")[0];
		if (ConstantValues.classExist(c) && Common.isNumber(key)) {
			ConstantValues.hapus(clazzs, Long.parseLong(key));
		} else if (izinkan(c)) {
			Map<String, GeneralValueObject> dataYangTerambilBerdasarkanId = PAKAI_MAPDB_ENTITY ? MemoryCacheUtil.get(clazzs + "_data_" + MemoryCacheUtil.getCODE()) : null;
			if (dataYangTerambilBerdasarkanId != null) {
				dataYangTerambilBerdasarkanId.remove(key);
			}
			// IDENTITY MAP: buang instance kanonik agar tidak ada objek "hantu" utk id yang sudah dihapus.
			identityRemove(clazzs, key);
		}
	}

	/**
	 * Menentukan apakah sebuah kelas entity BOLEH masuk cache objek MapDB.
	 *
	 * <p><b>Tujuan.</b> Gerbang tunggal yang menjaga agar hanya entity transaksional terpilih
	 * ({@code CLASS_IZINKAN}) yang di-cache—mencegah cache membengkak oleh entity sembarangan dan
	 * menjaga konsistensi perilaku masukkan/hapus.</p>
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan {@code false} bila null; selain itu mengecek apakah prefiks
	 * nama kelas (potongan sebelum {@code "_"}) ada di {@link HashSet} {@code CLASS_IZINKAN}
	 * (lookup O(1)). Pemakaian prefiks mengakomodasi nama kelas ter-mangle Hibernate
	 * (mis. {@code Foo_$$_javassist}).</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code clazzx} = kelas entity. {@code true} bila boleh
	 * di-cache.</p>
	 *
	 * <p><b>Pemeliharaan.</b> Daftarkan entity baru ke {@code CLASS_IZINKAN} di blok static untuk
	 * mengaktifkan caching-nya. Method privat; logika ini dipakai konsisten oleh masukkan/hapus.</p>
	 *
	 * @param clazzx kelas entity (boleh null)
	 * @return {@code true} bila kelas diizinkan masuk cache
	 */
	@SuppressWarnings("rawtypes")
	private static boolean izinkan(Class clazzx) {
		if (clazzx == null)
			return false;
		String clazzs = StringUtils.split(clazzx.getName(), "_")[0];
		return CLASS_IZINKAN.contains(clazzs);
	}

	/**
	 * Mengambil banyak entity sekaligus berdasarkan kumpulan key (overload tanpa refresh paksa).
	 *
	 * <p><b>Tujuan &amp; cara kerja.</b> Bentuk ringkas dari
	 * {@link #ambilDataBanyak(Class, Collection, boolean)} dengan {@code refresh=false}: memakai data
	 * cache bila ada, hanya membaca DB untuk key yang belum ter-cache. Cocok untuk memuat daftar
	 * relasi (mis. detail per baris) secara efisien dalam satu putaran.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code c} = kelas entity; {@code keys} = kumpulan id/kunci.
	 * Mengembalikan {@link List} entity (kosong bila keys kosong/null).</p>
	 *
	 * @param c    kelas entity
	 * @param keys kumpulan id/kunci
	 * @return daftar entity (gabungan cache + DB bila perlu)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static List ambilDataBanyak(Class c, Collection<? extends Serializable> keys) {
		return ambilDataBanyak(c, keys, false);
	}

	/**
	 * Mengambil banyak entity sekaligus secara efisien: pakai cache, lalu satu query batch ke DB untuk
	 * sisanya.
	 *
	 * <p><b>Tujuan.</b> Menghindari pola "N+1 query" saat memuat banyak entity sejenis. Daripada
	 * membaca satu per satu, method ini melayani sebanyak mungkin dari cache lalu mengambil sisa key
	 * dalam SATU query {@code IN (...)}.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <ol>
	 *   <li>Kembalikan list kosong bila {@code keys} null/kosong.</li>
	 *   <li>Iterasi tiap key: bila {@code refresh} false dan ada di cache, langsung pakai; jika tidak,
	 *   kumpulkan id-nya ke {@code idsNggakAda}. Ada batas pengaman ~1200 key per panggilan agar query
	 *   {@code IN} tidak terlalu besar.</li>
	 *   <li>Bila ada id yang belum ter-cache, jalankan satu {@code createCriteria(c).add(in("id",
	 *   idsNggakAda))} pada {@link HibernateUtil#currentNativeSession()}; hasilnya dimasukkan ke cache
	 *   (by id) dan ke list keluaran, memakai {@link HashSet} {@code sudah} untuk dedup O(1).</li>
	 * </ol>
	 *
	 * <p><b>Higiene koneksi.</b> Session baca ditutup TUNTAS di {@code finally} (disconnect + close +
	 * {@link HibernateUtil#closeSession()}). Koleksi sementara di-{@code clear}/null-kan untuk
	 * membantu GC pada batch besar.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code c} = kelas entity; {@code keys} = kumpulan id/kunci;
	 * {@code refresh} = bila true, abaikan cache dan paksa baca DB. Mengembalikan {@link List} entity.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Dibungkus try/catch berlapis; kegagalan di-log dan
	 * mengembalikan apa yang sudah terkumpul. Jangan menaikkan batas 1200 tanpa mempertimbangkan
	 * batas ukuran klausa {@code IN} pada database.</p>
	 *
	 * @param c       kelas entity
	 * @param keys    kumpulan id/kunci
	 * @param refresh {@code true} untuk mengabaikan cache dan membaca ulang dari DB
	 * @return daftar entity
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List ambilDataBanyak(Class c, Collection<? extends Serializable> keys, boolean refresh) {
		List<GeneralValueObject> generalValueObjects = new ArrayList<GeneralValueObject>();
		if (keys == null || keys.isEmpty()) {
			return generalValueObjects;
		}

		try {
			String clazzs = StringUtils.split(c.getName(), "_")[0];
			Map<String, GeneralValueObject> dataYangTerambilBerdasarkanId = PAKAI_MAPDB_ENTITY ? MemoryCacheUtil.get(clazzs + "_data_" + MemoryCacheUtil.getCODE()) : null;

			List<Long> idsNggakAda = new ArrayList<Long>();
			int banyak = 0;

			for (Serializable key : keys) {
				if (banyak > 1200) {
					break;
				}

				// JAMINAN "satu objek per (kelas,id)": utamakan instance KANONIK dari EntityIdentityMap;
				// bila hanya ada di cache MapDB, kanonikalisasi salinan hasil deserialisasi (MapDB.get()
				// mengembalikan objek BARU tiap kali) supaya TIDAK menjadi DUPLIKAT (kelas,id) yang beredar
				// bersama instance kanonik. (ambilData tunggal sudah melakukan ini; batch dulu belum.)
				GeneralValueObject data = null;
				if (!refresh) {
					data = identityGet(clazzs, key.toString());
					if (data == null && dataYangTerambilBerdasarkanId != null) {
						GeneralValueObject fromCache = dataYangTerambilBerdasarkanId.get(key.toString());
						if (fromCache != null) {
							GeneralValueObject kanonik = EntityIdentityMap.canonical(fromCache);
							data = kanonik != null ? kanonik : fromCache;
						}
					}
				}

				if (data == null) {
					try {
						idsNggakAda.add(key instanceof Long ? ((Long) key) : Long.parseLong(key.toString()));
						banyak++;
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:1208");
					}
				} else {
					generalValueObjects.add(data);
				}
			}

			if (!idsNggakAda.isEmpty()) {
				// System.out.println("clazz " + clazzs + " idsNggakAda -> " + idsNggakAda);

				Session session = null;
				// PERBAIKAN (session closed): currentNativeSession() thread-local, bukan
				// call-scoped -- bila pemanggil (mis. VOPembelajaran.reInitPertemuan yang
				// dipanggil dari tengah alur Perkuliahan.singkronkan) SUDAH membuka native
				// session lebih dulu di thread yang sama, panggilan di sini akan meminjam
				// (bukan membuka baru) session ancestor tsb. Tutup paksa di finally seperti
				// sebelumnya membuat ancestor melihat "Session is closed!" saat lanjut
				// memakainya. Hanya tutup bila method INI yang benar-benar membuka baru.
				boolean sessionSudahTerbukaSebelumnya = HibernateUtil.isNativeSessionOpenForCurrentThread();
				try {
					session = HibernateUtil.currentNativeSession();
					List<GeneralValueObject> generalValueObjectsData = session.createCriteria(c)
							.add(Restrictions.in("id", idsNggakAda)).list();

					// OPTIMASI: Menggunakan HashSet untuk O(1) performance lookup, dibandingkan
					// ArrayList.
					Set<Long> sudah = new HashSet<Long>();

					for (GeneralValueObject valueObject : generalValueObjectsData) {
						if (valueObject != null && !sudah.contains(valueObject.getId())) {
							// Kanonikalisasi hasil load. Interceptor (currentNativeSession) umumnya sudah
							// melakukannya saat load, tapi dilakukan eksplisit di sini agar jaminan
							// "satu objek per (kelas,id)" tidak bergantung pada keberadaan interceptor.
							GeneralValueObject kanonik = EntityIdentityMap.canonical(valueObject);
							if (kanonik == null) {
								kanonik = valueObject;
							}
							if (dataYangTerambilBerdasarkanId != null) {
								dataYangTerambilBerdasarkanId.put(kanonik.getId().toString(), kanonik);
							}
							generalValueObjects.add(kanonik);
							sudah.add(kanonik.getId());
						}
					}

					sudah.clear();
					generalValueObjectsData.clear();
					sudah = null;
					generalValueObjectsData = null;

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:1251");
				} finally {
					// OPTIMASI: Rapi menutup hibernate session dalam finally -- KECUALI bila
					// session ini dipinjam dari ancestor yang masih memakainya (lihat komentar
					// di atas); ancestor tetap bertanggung jawab menutup session miliknya sendiri.
					if (!sessionSudahTerbukaSebelumnya) {
						if (session != null) {
							try {
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:1257");
								// Ignore disconnect error
							}
							try {
								session.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:1262");
								// Ignore close error
							}
						}
						HibernateUtil.closeSession();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DataUtil.java:1270");
		}
		return generalValueObjects;
	}

	/**
	 * Memulihkan (restore) sebuah entity beserta SELURUH relasinya dari tabel audit Envers secara
	 * rekursif, bila entity itu hilang dari tabel utama.
	 *
	 * <p><b>Tujuan.</b> Ketika data terhapus/hilang dari tabel utama tetapi masih ada jejaknya di
	 * tabel audit (Hibernate Envers), method ini merekonstruksinya kembali—termasuk semua entity
	 * relasi yang ikut hilang—sehingga integritas referensi terjaga saat data di-replicate kembali.</p>
	 *
	 * <h4>Cara kerja (rekursif, depth-first pada relasi)</h4>
	 * <ol>
	 *   <li><b>Validasi &amp; anti-loop.</b> {@code id} null → berhenti. Memakai {@code processedIds}
	 *   (set berisi "{@code namaKelas-id}") untuk mencegah rekursi tak berujung pada relasi melingkar
	 *   (A→B→A).</li>
	 *   <li><b>Cek DB utama.</b> Bila {@code session.get(clazz, id)} sudah ada, kembalikan
	 *   {@code false} (tak perlu restore).</li>
	 *   <li><b>Tarik versi audit terakhir</b> via {@code AuditReader.forRevisionsOfEntity} (revisi
	 *   terbaru). Bila tak ada jejak audit, kembalikan {@code false}.</li>
	 *   <li><b>Telusuri relasi via refleksi.</b> Untuk tiap getter yang mengembalikan turunan
	 *   {@link GeneralValueObject} (tetapi BUKAN base class {@code GeneralValueObject} sendiri, dan
	 *   bukan {@code getClass}), ambil id relasinya. Id diekstrak AMAN dari {@code HibernateProxy}
	 *   (lewat {@code getHibernateLazyInitializer().getIdentifier()}) atau dari objeknya langsung; bila
	 *   Hibernate melempar {@code ObjectNotFoundException} (relasi fisik hilang), id tetap diambil dari
	 *   exception tersebut. Lalu restore relasi itu LEBIH DAHULU secara rekursif.</li>
	 *   <li><b>Replicate paksa.</b> Setelah semua child dipastikan ada, entity ini di-{@code replicate}
	 *   dengan {@code ReplicationMode.OVERWRITE}.</li>
	 * </ol>
	 *
	 * <p><b>Mengapa rumit begini.</b> Membaca relasi entity yang fisiknya sudah hilang biasanya memicu
	 * {@code ObjectNotFoundException} dari Hibernate; menelan id dari exception/proxy memungkinkan
	 * restore tetap berjalan tanpa memuat objek fisik. Mem-bypass base class {@code GeneralValueObject}
	 * penting agar Hibernate tidak mencoba meng-query kelas dasar yang tidak dipetakan.</p>
	 *
	 * <p><b>Parameter &amp; Return.</b> {@code session} = session aktif untuk get/replicate;
	 * {@code reader} = AuditReader Envers; {@code clazz} = kelas entity; {@code id} = id entity;
	 * {@code processedIds} = set pelacak anti-loop (diteruskan turun-temurun). Mengembalikan
	 * {@code true} bila data direstore/diproses, {@code false} bila sudah ada di DB utama atau tak ada
	 * jejak audit.</p>
	 *
	 * <p><b>Penanganan error &amp; pemeliharaan.</b> Ekstraksi id relasi menangani
	 * {@code InvocationTargetException}, {@code ObjectNotFoundException} (langsung &amp; sebagai cause),
	 * dan {@link Exception} lain secara terpisah (gaya Java 1.6, tanpa multi-catch). Method melempar
	 * {@code Exception}—pemanggil harus membungkusnya dalam transaksi yang bisa di-rollback bila
	 * sebagian restore gagal. Pertahankan bypass {@code GeneralValueObject} dan ekstraksi id dari
	 * proxy; keduanya esensial agar restore tidak menabrak data hilang.</p>
	 *
	 * @param session      session aktif untuk get/replicate
	 * @param reader       AuditReader Envers
	 * @param clazz        kelas entity yang dipulihkan
	 * @param id           id entity
	 * @param processedIds set pelacak anti-loop rekursi
	 * @return {@code true} bila direstore/diproses; {@code false} bila sudah ada / tak ada jejak audit
	 * @throws Exception bila proses baca audit/replicate gagal
	 */
	public static boolean deepRestoreDataDanRelasi(org.hibernate.Session session,
			org.hibernate.envers.AuditReader reader, Class<?> clazz, Long id, java.util.Set<String> processedIds)
			throws Exception {

		// 1. Validasi Dasar
		if (id == null)
			return false;

		// 2. Mencegah Infinite Loop (jika ada relasi melingkar A -> B -> A)
		String trackingKey = clazz.getName() + "-" + id;
		if (processedIds.contains(trackingKey)) {
			return false;
		}
		processedIds.add(trackingKey);

		// 3. Pengecekan Eksistensi di Database Utama
		Object mainDbObject = session.get(clazz, id);
		if (mainDbObject != null) {
			return false; // Data sudah aman ada di DB utama, skip.
		}

		// 4. Jika Tidak Ada di tabel utama, Tarik Data Terakhir dari Tabel Audit
		java.util.List<?> results = reader.createQuery().forRevisionsOfEntity(clazz, true, false)
				.add(org.hibernate.envers.query.AuditEntity.id().eq(id))
				.addOrder(org.hibernate.envers.query.AuditEntity.revisionNumber().desc()).setMaxResults(1)
				.getResultList();

		if (results == null || results.isEmpty()) {
			return false; // Data benar-benar tidak ada di histori (tidak pernah ter-audit)
		}

		Object auditObj = results.get(0);

		// 5. MENGGUNAKAN JAVA REFLECTION: Merayapi seluruh relasi Objek
		java.lang.reflect.Method[] methods = clazz.getMethods();
		for (java.lang.reflect.Method method : methods) {

			Class<?> returnType = method.getReturnType();

			// Memfilter hanya metode "get..." yang mengembalikan Class turunan
			// GeneralValueObject
			// [PENTING]: Bypass base class GeneralValueObject agar tidak di-query oleh
			// Hibernate
			if (method.getName().startsWith("get")
					&& ais.database.model.GeneralValueObject.class.isAssignableFrom(returnType)
					&& !returnType.equals(ais.database.model.GeneralValueObject.class)
					&& !method.getName().equals("getClass")) {

				Long relationId = null;

				try {
					Object relationObj = method.invoke(auditObj);

					if (relationObj != null) {
						// Ekstraksi ID secara aman dari Proxy tanpa mentrigger pemanggilan fisik ke DB
						if (relationObj instanceof org.hibernate.proxy.HibernateProxy) {
							relationId = (Long) ((org.hibernate.proxy.HibernateProxy) relationObj)
									.getHibernateLazyInitializer().getIdentifier();
						} else {
							relationId = ((ais.database.model.GeneralValueObject) relationObj).getId();
						}
					}
				} catch (java.lang.reflect.InvocationTargetException e) {
					// Menangkap error jika Hibernate terlanjur memanggil relasi fisik yang hilang
					Throwable cause = e.getCause();
					if (cause instanceof org.hibernate.ObjectNotFoundException) {
						relationId = (Long) ((org.hibernate.ObjectNotFoundException) cause).getIdentifier();
					}
				} catch (org.hibernate.ObjectNotFoundException e) {
					// Fallback antisipasi langsung dari Hibernate (standar 1.6, tanpa multi-catch)
					relationId = (Long) e.getIdentifier();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/DataUtil.java:1399");
					// Abaikan kegagalan baca spesifik lainnya
				}

				// Jika ID relasi berhasil didapatkan (meskipun fisiknya tidak ada), Restore
				// relasinya DULUAN!
				if (relationId != null) {
					deepRestoreDataDanRelasi(session, reader, returnType, relationId, processedIds);
				}
			}
		}

		// 6. Eksekusi Restore/Replicate paksa (Di tahap ini seluruh child/relasi
		// dipastikan sudah direplikasi)
		session.replicate(auditObj, org.hibernate.ReplicationMode.OVERWRITE);

		return true;
	}
}