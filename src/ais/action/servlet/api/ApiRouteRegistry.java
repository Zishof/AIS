package ais.action.servlet.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import ais.database.model.PerguruanTinggi;

/**
 * Pusat registrasi action API.
 *
 * Semua mapping action ditempatkan di class ini agar Api.java tidak menjadi terlalu besar,
 * dan agar penambahan action baru cukup dilakukan melalui register(...) pada group terkait.
 * Tetap memakai anonymous class agar kompatibel Java 1.7.
 */
public final class ApiRouteRegistry {

    private ApiRouteRegistry() {
    }

    public static Map<String, ApiRoute> createDefaultRoutes() {
        Map<String, ApiRoute> routes = new HashMap<String, ApiRoute>();
        registerAuthAndGeneral(routes);
        registerSurat(routes);
        registerKinerja(routes);
        registerLaporan(routes);
        registerAngket(routes);
        registerElearning(routes);
        registerKeuangan(routes);
        registerPendaftaran(routes);
        registerCatatan(routes);
        registerKantin(routes);
        registerSop(routes);
        return Collections.unmodifiableMap(routes);
    }

    private static void register(Map<String, ApiRoute> routes, String action, ApiRoute route) {
        if (routes != null && ApiHelperSupport.hasText(action) && route != null) {
            routes.put(action.trim().toLowerCase(Locale.ENGLISH), route);
        }
    }

    private static void registerAuthAndGeneral(Map<String, ApiRoute> routes) {
        register(routes, "code", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.code(json, req, pt); } });
        register(routes, "waProfile", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.waProfile(json); } });
        register(routes, "ambilCode", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.ambilCode(json, req, pt); } });
        register(routes, "loginLangsung", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.loginLangsung(json, req, pt); } });
        register(routes, "login", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.login(json, req, pt); } });
        register(routes, "loginInfo", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.loginInfo(json, req, pt); } });
        register(routes, "hakAksesSaya", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return HakAksesApi.hakAksesSaya(req, json); } });
        register(routes, "cariPenggunaResetPassword", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ResetPasswordApi.cari(req, json); } });
        register(routes, "resetPasswordPengguna", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ResetPasswordApi.reset(req, json); } });
        register(routes, "daftarPengguna", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PenggunaApi.daftar(req, json); } });
        register(routes, "detailPengguna", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PenggunaApi.detail(req, json); } });
        register(routes, "daftarRolePengguna", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PenggunaApi.daftarRole(req, json); } });
        register(routes, "simpanPengguna", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PenggunaApi.simpan(req, json); } });
        register(routes, "hapusPengguna", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PenggunaApi.hapus(req, json); } });
        register(routes, "simpanProfilSaya", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PenggunaApi.simpanProfilSaya(req, json); } });
        register(routes, "ubahPasswordMahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PasswordSayaApi.ubahMahasiswa(req, json); } });
        register(routes, "repositoryMahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return RepositoryMahasiswaApi.daftar(req, json); } });
        register(routes, "pengaduanMahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PengaduanMahasiswaApi.proses(req, json); } });
        register(routes, "kpiMahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KpiMahasiswaApi.daftar(req, json); } });
        register(routes, "pengumuman", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.pengumuman(req, json, pt); } });
        register(routes, "konfigurasi", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiUtil.konfigurasi(req, json); } });
        register(routes, "tema", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ThemeApi.tema(req, json); } });
        register(routes, "logout", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AngketUtilUmumApi.logout(req, json); } });
    }

    private static void registerSurat(Map<String, ApiRoute> routes) {
        register(routes, "klasifikasi_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.klasifikasi(req, json); } });
        register(routes, "disposisi_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.disposisi_surat_keluar(req, json); } });
        register(routes, "disposisi_surat_masuk", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.disposisi_surat_masuk(req, json); } });
        register(routes, "disposisi_simpan_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.disposisi_simpan_surat_keluar(req, json); } });
        register(routes, "disposisi_simpan_surat_masuk", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.disposisi_simpan_surat_masuk(req, json); } });
        register(routes, "parameter_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.parameter(req, json); } });
        register(routes, "update_parameter_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.updateParameter(req, json); } });
        register(routes, "info_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.info(req, json); } });
        register(routes, "cetak_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.cetak(req, json); } });
        register(routes, "lampiran_surat_keluar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.lampiranSuratKeluar(req, json); } });
        register(routes, "lampiran_surat_masuk", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SuratApi.lampiranSuratMasuk(req, json); } });
    }

    private static void registerKinerja(Map<String, ApiRoute> routes) {
        register(routes, "daftarTugasJabatan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KinerjaPegawaiApi.daftarTugasJabatan(req, json); } });
        register(routes, "lanjutTugasJabatan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KinerjaPegawaiApi.lanjutTugasJabatan(req, json); } });
        register(routes, "simpanTugasJabatan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KinerjaPegawaiApi.simpanTugasJabatan(req, json); } });
        register(routes, "daftarCatatanTugasJabatan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KinerjaPegawaiApi.daftarCatatanTugasJabatan(req, json); } });
    }

    private static void registerLaporan(Map<String, ApiRoute> routes) {
        register(routes, "laporan_absen", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.laporan_absen(req, json); } });
        register(routes, "raport_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.raport_siswa(req, json); } });
        register(routes, "catatan_guru", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.catatan_guru(req, json); } });
        register(routes, "catatan_administrasi", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.catatan_administrasi(req, json); } });
        register(routes, "catatan_kelas_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.catatan_kelas_siswa(req, json); } });
        register(routes, "catatan_pegawai", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.catatan_pegawai(req, json); } });
        register(routes, "catatan_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.catatan_siswa(req, json); } });
        register(routes, "khs", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.khs(req, json); } });
        register(routes, "krs", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.krs(req, json); } });
        register(routes, "uts", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.uts(req, json); } });
        register(routes, "uas", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.uas(req, json); } });
        register(routes, "transkrip", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.transkrip(req, json); } });
        register(routes, "ipk", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.ipk(req, json); } });
        register(routes, "struk", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.struk(req, json); } });
        register(routes, "slip", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.slip(req, json); } });
        register(routes, "sertifikatKelasLes", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LaporanApi.sertifikatKelasLes(req, json); } });
    }

    private static void registerAngket(Map<String, ApiRoute> routes) {
        register(routes, "daftarAngketUmum", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AngketUtilUmumApi.daftarAngket(req, json); } });
        register(routes, "simpanAngketUmum", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AngketUtilUmumApi.simpanAngket(req, json); } });
        register(routes, "daftarAngket", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AngketUtilApi.daftarAngket(req, json); } });
        register(routes, "simpanAngket", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AngketUtilApi.simpanAngket(req, json); } });
        register(routes, "cekKewajibanAngket", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AngketKewajibanApi.cekKewajiban(req, json); } });
    }

    private static void registerElearning(Map<String, ApiRoute> routes) {
        register(routes, "daftarAmbilKrs", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.daftarAmbilKrs(req, json); } });
        register(routes, "simpanAbsenPiket", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.simpanAbsenPiket(req, json); } });
        register(routes, "agenda", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.agenda(req, json); } });
        register(routes, "dataRinci", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.dataRinci(req, json); } });
        register(routes, "simpanDataRinci", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.simpanDataRinci(req, json); } });
        register(routes, "simpanDataBanyak", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.simpanDataBanyak(req, json); } });
        register(routes, "hapusDataRinci", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.hapusDataRinci(req, json); } });
        register(routes, "aktiftasPerkuliahanInfo", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.aktiftasPerkuliahanInfo(req, json); } });
        register(routes, "kehadiran_dosen", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.kehadiranDosen(req, json); } });
        register(routes, "syaratKrs", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.syaratKrs(req, json); } });
        register(routes, "daftar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return DaftarDataService.daftar(req, json); } });
        register(routes, "load", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return DaftarDataService.load(req, json); } });
        register(routes, "daftar_nilai_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.daftar_nilai_mahasiswa(req, json); } });
        register(routes, "daftar_absen_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.daftar_absen_mahasiswa(req, json); } });
        register(routes, "daftar_absen_dosen", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.daftar_absen_dosen(req, json); } });
        register(routes, "update_absen", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.update_absen(req, json); } });
        register(routes, "current_smt", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.current_smt(req, json); } });
        register(routes, "update_nilai_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.update_nilai_mahasiswa(req, json); } });
        register(routes, "daftar_nilai_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.daftar_nilai_siswa(req, json); } });
        register(routes, "daftar_nilai_siswa_oleh_guru", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.daftar_nilai_siswa_oleh_guru(req, json); } });
        register(routes, "daftar_nilai_untuk_input", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return NilaiSiswaApi.daftar_nilai_untuk_input(req, json); } });
        register(routes, "input_nilai_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return NilaiSiswaApi.input_nilai_siswa(req, json); } });
        register(routes, "absen", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AbsensiApiAction.absen(req, json); } });
        register(routes, "file", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.file(req, json); } });
        register(routes, "ta", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.ta(req, json); } });

        register(routes, "linimasa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.linimasa(json, req); } });
        register(routes, "daftar_ujian", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.daftar_ujian(json, req); } });
        register(routes, "daftar_tugas", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.daftar_tugas(json, req); } });
        register(routes, "daftar_materi", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.daftar_materi(json, req); } });
        register(routes, "daftar_video", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.daftar_video(json, req); } });
        register(routes, "daftar_audio", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.daftar_audio(json, req); } });
        register(routes, "masukkanInfoKePertemuan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.masukkanInfoKePertemuan(json, req); } });
        register(routes, "refreshPertemuan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return LinimasaApi.refreshPertemuan(json, req); } });

        register(routes, "ujian", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningPertemuanApi.ujian(json, req); } });
        register(routes, "simpanJawabanSoal", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningPertemuanApi.simpanJawabanSoal(json, req); } });
        register(routes, "selesaiSoal", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningPertemuanApi.selesaiSoal(json, req); } });
        register(routes, "semuaPertemuan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningPertemuanApi.semuaPertemuan(json, req); } });
        register(routes, "semuaDikusi", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningPertemuanApi.semuaDikusi(req, json); } });
        register(routes, "daftarJadwalPelajaran", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ApiBaruElearning.daftarJadwalPelajaran(req, json); } });
        register(routes, "nilai_komponen_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return ElearningApiUtil.nilai_komponen_mahasiswa(req, json); } });
    }

    private static void registerKeuangan(Map<String, ApiRoute> routes) {
        register(routes, "tagihan_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.tagihan(req, json); } });
        register(routes, "split", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.split(req, json); } });
        register(routes, "split_tagihan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.split(req, json); } });
        register(routes, "hapus_split", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.hapus_split(req, json); } });
        register(routes, "piutang_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.piutang(req, json); } });
        register(routes, "subscribeKelasLes", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.subscribeKelasLes(req, json); } });
        register(routes, "kelasLes", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.kelasLes(req, json); } });
        register(routes, "kelasLesAktif", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.kelasLesAktif(req, json); } });
        register(routes, "tabungan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.tabungan(req, json); } });
        register(routes, "pertemuanKelasLes", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.pertemuanKelasLes(json, req); } });
        register(routes, "pembayaran_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.pembayaran(req, json); } });
        register(routes, "va_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.va(json, req); } });
        register(routes, "bayar_tabungan_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanSiswa.bayarTabunganSiswa(json, req); } });

        register(routes, "tagihan_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanMahasiswa.tagihan(req, json); } });
        register(routes, "va_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanMahasiswa.va(json, req); } });
        register(routes, "daftar_va", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TagihanMahasiswa.daftar_va(req, json); } });

        register(routes, "pembelian_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TabunganSiswa.pembelian_siswa(req, json); } });
        register(routes, "tabungan_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TabunganSiswa.tabungan_siswa(req, json); } });
        register(routes, "tabungan_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TabunganSiswa.tabungan_mahasiswa(req, json); } });
        register(routes, "daftar_tabungan_siswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TabunganSiswa.daftar_tabungan_siswa(req, json); } });

        register(routes, "topup", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TopupHelper.topup(json, req); } });
        register(routes, "bayarOnline", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TopupHelper.bayarOnline(json, req); } });
        register(routes, "checkBayar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TopupHelper.checkBayar(json, req); } });
        register(routes, "riwayatPembelian", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return TopupHelper.riwayatPembelian(json, req); } });
    }

    private static void registerPendaftaran(Map<String, ApiRoute> routes) {
        register(routes, "pendaftaran_siswa_baru", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SiswaBaruApi.pendaftaran_siswa_baru(json, req); } });
        register(routes, "biodata_calon_mahasiswa", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CalonMahasiswaApiUtil.biodata_calon_mahasiswa(req, json); } });
        // Portal PSB/PPDB native mobile (publik, tanpa login) -- lihat PsbApi.
        register(routes, "psb_portal_info", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbApi.portalInfo(req, json); } });
        register(routes, "psb_pengumuman", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbApi.pengumuman(req, json); } });
        register(routes, "psb_gelombang", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbApi.gelombang(req, json); } });
        register(routes, "psb_daftar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbApi.daftar(req, json); } });
        register(routes, "psb_cek_status", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbApi.cekStatus(req, json); } });
        // Portal PMB native mobile (publik, tanpa login) -- lihat PmbApi.
        register(routes, "pmb_portal_info", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbApi.portalInfo(req, json); } });
        register(routes, "pmb_pengumuman", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbApi.pengumuman(req, json); } });
        register(routes, "pmb_prodi", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbApi.prodi(req, json); } });
        register(routes, "pmb_gelombang", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbApi.gelombang(req, json); } });
        register(routes, "pmb_daftar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbApi.daftar(req, json); } });
        register(routes, "pmb_cek_status", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbApi.cekStatus(req, json); } });
        // Login calon siswa (portal PSB) & calon mahasiswa (portal PMB) -- publik, kredensial identitas + tanggal lahir.
        register(routes, "psb_login", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbCalonApi.login(req, json); } });
        register(routes, "psb_calon_profil", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbCalonApi.profil(req, json); } });
        register(routes, "psb_update_biodata", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PsbCalonApi.updateBiodata(req, json); } });
        register(routes, "pmb_login", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbCalonApi.login(req, json); } });
        register(routes, "pmb_calon_profil", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbCalonApi.profil(req, json); } });
        register(routes, "pmb_update_biodata", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return PmbCalonApi.updateBiodata(req, json); } });
    }

    /**
     * Registrasi action modul CATATAN (pola *Catatan*Action) dan AKTIFITAS HARIAN SISWA untuk
     * aplikasi mobile. CRUD catatan siswa + definisi parameter tambahan + lampiran (CatatanApi),
     * serta aktivitas harian + pesan pembina/orang tua (AktifitasHarianSiswaApi).
     */
    private static void registerCatatan(Map<String, ApiRoute> routes) {
        // ── Catatan Siswa (representasi kanonik modul *Catatan*) ──
        register(routes, "catatan_siswa_jenis", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CatatanApi.jenisSiswa(req, json); } });
        register(routes, "catatan_siswa_parameter", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CatatanApi.parameter(req, json); } });
        register(routes, "catatan_siswa_daftar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CatatanApi.daftar(req, json); } });
        register(routes, "catatan_siswa_detail", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CatatanApi.detail(req, json); } });
        register(routes, "catatan_siswa_simpan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CatatanApi.simpan(req, json); } });
        register(routes, "catatan_siswa_hapus", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return CatatanApi.hapus(req, json); } });

        // ── Aktifitas Harian Siswa + pesan pembina/orang tua ──
        register(routes, "aktifitas_harian_siswa_daftar", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AktifitasHarianSiswaApi.daftar(req, json); } });
        register(routes, "aktifitas_harian_siswa_detail", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AktifitasHarianSiswaApi.detail(req, json); } });
        register(routes, "aktifitas_harian_siswa_simpan", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AktifitasHarianSiswaApi.simpan(req, json); } });
        register(routes, "aktifitas_harian_siswa_pesan_orang_tua", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AktifitasHarianSiswaApi.pesanOrangTua(req, json); } });
        register(routes, "aktifitas_harian_siswa_pesan_pembina", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return AktifitasHarianSiswaApi.pesanPembina(req, json); } });
    }

    /**
     * Registrasi seluruh action modul Kantin / Belanja Online untuk mobile.
     * Action tersedia: kantin_info, kantin_saldo, kantin_toko_list, kantin_cara_bayar,
     * kantin_produk_list, kantin_aturan_diskon, kantin_bayar, kantin_draft_bayar,
     * kantin_pesanan_list, kantin_pesanan_batal, kantin_transaksi_list,
     * kantin_transaksi_detail, kantin_barang_list, kantin_va_list, kantin_dashboard.
     */
    private static void registerKantin(Map<String, ApiRoute> routes) {
        register(routes, "kantin_info",             new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.info(req, json, pt); } });
        register(routes, "kantin_daftar",           new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.daftarMember(req, json, pt); } });
        register(routes, "kantin_saldo",            new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.saldo(req, json, pt); } });
        register(routes, "kantin_toko_list",        new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.tokoList(req, json, pt); } });
        register(routes, "kantin_cara_bayar",       new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.caraBayar(req, json, pt); } });
        register(routes, "kantin_produk_list",      new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.produkList(req, json, pt); } });
        register(routes, "kantin_aturan_diskon",    new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.aturanDiskon(req, json, pt); } });
        register(routes, "kantin_bayar",            new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.bayar(req, json, pt); } });
        register(routes, "kantin_draft_bayar",      new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.draftBayar(req, json, pt); } });
        register(routes, "kantin_pesanan_list",     new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pesananList(req, json, pt); } });
        register(routes, "kantin_pesanan_batal",    new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.batalPesanan(req, json, pt); } });
        register(routes, "kantin_transaksi_list",   new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.transaksiList(req, json, pt); } });
        register(routes, "kantin_transaksi_detail", new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.transaksiDetail(req, json, pt); } });
        register(routes, "kantin_barang_list",      new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.barangList(req, json, pt); } });
        register(routes, "kantin_va_list",          new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.vaList(req, json, pt); } });
        register(routes, "kantin_dashboard",               new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.dashboard(req, json, pt); } });
        register(routes, "kantin_pedagang_info",            new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangInfo(req, json, pt); } });
        register(routes, "kantin_pedagang_pesanan_list",    new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangPesananList(req, json, pt); } });
        register(routes, "kantin_pedagang_proses_pesanan",  new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangProsesPesanan(req, json, pt); } });
        register(routes, "kantin_pedagang_produk_list",     new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangProdukList(req, json, pt); } });
        register(routes, "kantin_pedagang_cara_bayar",      new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangCaraBayar(req, json, pt); } });
        register(routes, "kantin_pedagang_kasir_bayar",     new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangKasirBayar(req, json, pt); } });
        register(routes, "kantin_pedagang_riwayat",         new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangRiwayat(req, json, pt); } });
        register(routes, "kantin_pedagang_dashboard",       new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return KantinMemberApi.pedagangDashboard(req, json, pt); } });
    }

    /**
     * Registrasi action modul SOP (Standard Operational Procedure) untuk aplikasi mobile — dasbor
     * ringkasan (sop_dashboard), daftar per kategori (sop_daftar), detail + alur disposisi
     * (sop_detail), pencarian (sop_cari), aksi disposisi (sop_proses), batalkan pengajuan/tahap
     * (sop_batalkan_pengajuan/sop_batalkan_langkah), dan cetak PDF (sop_cetak).
     */
    private static void registerSop(Map<String, ApiRoute> routes) {
        register(routes, "sop_dashboard",            new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.dashboard(req, json); } });
        register(routes, "sop_daftar",                new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.daftar(req, json); } });
        register(routes, "sop_detail",                new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.detail(req, json); } });
        register(routes, "sop_cari",                  new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.cari(req, json); } });
        register(routes, "sop_proses",                new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.proses(req, json); } });
        register(routes, "sop_batalkan_pengajuan",    new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.batalkanPengajuan(req, json); } });
        register(routes, "sop_batalkan_langkah",      new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.batalkanLangkah(req, json); } });
        register(routes, "sop_cetak",                  new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.cetak(req, json); } });
        register(routes, "sop_jenis",                  new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.jenis(req, json); } });
        register(routes, "sop_mulai_info",             new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.mulaiInfo(req, json); } });
        register(routes, "sop_cari_entitas",           new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.cariEntitas(req, json); } });
        register(routes, "sop_ajukan",                 new ApiRoute() { public JSONObject execute(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception { return SopService.ajukan(req, json); } });
    }

}
