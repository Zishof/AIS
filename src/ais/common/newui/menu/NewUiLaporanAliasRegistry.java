package ais.common.newui.menu;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Alias menu laporan: nama laporan pada kolom {@code url} menu, dipetakan ke
 * nama kelas {@code *Window} yang melayaninya.
 *
 * <h3>Mengapa lapisan ini ada</h3>
 * <p>Sebagian menu laporan tidak menyimpan path maupun nama kelas pada
 * kolom {@code url}, melainkan sebuah nama laporan seperti
 * {@code laporanTranskripAkademik}. Menu semacam itu dilayani rantai
 * {@code else if} raksasa di {@code Common.launchMenu}, yang mencocokkan nama
 * tersebut lalu membuat kelas {@code *Window}-nya sendiri.</p>
 *
 * <p>{@link NewUiNativeJspResolver} tidak dapat melihatnya: pencarian menurut
 * nama URL tidak menemukan apa-apa, dan fallback composer hanya berlaku bagi
 * URL yang benar-benar menunjuk berkas ZUL. Akibatnya menu-menu itu dilaporkan
 * "adaptor belum dikonfigurasi" — padahal <b>halaman native-nya sudah ada</b>,
 * hanya dinamai menurut kelas Window-nya. Yang hilang semata-mata pemetaan
 * namanya.</p>
 *
 * <h3>Tabel ini dibangkitkan, bukan diketik</h3>
 * <p>Isinya diekstraksi dari {@code Common.launchMenu}. Mengetiknya dengan
 * tangan menjamin tabel ini lambat laun menyimpang dari rantai {@code else if}
 * yang sesungguhnya menentukan perilaku, tanpa gejala apa pun.
 * {@code NewUiLaporanAliasRegistrySelfTest} mengekstraksi ulang dari sumber
 * dan gagal bila keduanya berbeda.</p>
 */
public final class NewUiLaporanAliasRegistry {

    private static final Map<String, String> ALIAS;

    static {
        Map<String, String> m = new HashMap<String, String>();
        ALIAS = Collections.unmodifiableMap(isi(m));
    }

    private NewUiLaporanAliasRegistry() { }

    private static Map<String, String> isi(final Map<String, String> m) {
        Pengisi p = new Pengisi(m);
        p.semua();
        return m;
    }

    /** Pengisi tabel; dipisah agar daftar panjang tidak membanjiri static block. */
    private static final class Pengisi {
        private final Map<String, String> m;
        Pengisi(Map<String, String> m) { this.m = m; }
        private void petakan(String alias, String window) { m.put(alias, window); }
        void semua() {
        petakan("LaporanRekapPerPembayarandgnPenguranganWindow", "LaporanRekapPerPembayarandgnPenguranganWindow");
        petakan("cetakAlbumWisudaAdmin", "CetakAlbumWisudaAdminWindow");
        petakan("cetakUndanganWisuda", "GenerateUndanganWisudaWindow");
        petakan("daftarHadirDosen", "LaporanDaftarHadirDosenHarianWindow");
        petakan("generateNoKursi", "GenerateNoKursiWindow");
        petakan("generateValidasiLaporanWindow", "GenerateValidasiLaporanWindow");
        petakan("kurikulum", "LaporanKurikulumWindow");
        petakan("laporanAbsensi", "LaporanAbsensiWindow");
        petakan("laporanAbsensiUjian", "LaporanAbsensiUjianWindow");
        petakan("laporanBeritaAcaraSkripsi", "LaporanBeritaAcaraSkripsiWindow");
        petakan("laporanCoverAbsensi", "LaporanCoverAbsensiWindow");
        petakan("laporanDaftarHadirDosenSemua", "LaporanDaftarHadirDosenWindow");
        petakan("laporanDaftarHadirUjianSidang", "LaporanDaftarHadirUjianSidangWindow");
        petakan("laporanDaftarHadirWindow", "LaporanDaftarHadirWindow");
        petakan("laporanDaftarNilaiWindow", "LaporanDaftarNilaiWindow");
        petakan("laporanDaftarPegawaiNamaAlamat", "LaporanDataPegawaiNamaAlamatWindow");
        petakan("laporanDaftarPrestasiBelajarWindow", "LaporanDaftarPrestasiBelajarWindow");
        petakan("laporanDataMahasiswaWindow", "LaporanDataMahasiswaWindow");
        petakan("laporanJadwalUAS", "LaporanJadwalUasWindow");
        petakan("laporanKHS", "LaporanKHSWindow");
        petakan("laporanKHSSemesterPendek", "LaporanKHSSemesterPendekWindow");
        petakan("laporanKartuHasilStudiMahasiswaWindow", "LaporanKartuHasilStudiMahasiswaWindow");
        petakan("laporanPerkuliahan", "LaporanJadwalPerkuliahanWindow");
        petakan("laporanRegistrasiWisuda", "GenerateNoKursiDanNoRegistrasiWindow");
        petakan("laporanRekapHostToHostWindow", "LaporanRekapHostToHostWindow");
        petakan("laporanRekapMahasiswaBelumBayarWindow", "LaporanRekapMahasiswaBelumBayarWindow");
        petakan("laporanRekapMahasiswaSudahBayarWindow", "LaporanRekapMahasiswaSudahBayarWindow");
        petakan("laporanRekapPenilaianMahasiswaWindow", "LaporanRekapPenilaianMahasiswaWindow");
        petakan("laporanRekapPerJenisBiayaWindow", "LaporanRekapPerJenisBiayaWindow");
        petakan("laporanRekapPerPembayaranWindow", "LaporanRekapPerPembayaranWindow");
        petakan("laporanRekapPerProdiDenganPenguranganPerValidatorWindow", "LaporanRekapPerProdiDenganPenguranganPerValidatorWindow");
        petakan("laporanRekapPerProdiDenganPenguranganWindow", "LaporanRekapPerProdiDenganPenguranganWindow");
        petakan("laporanRekapPerProdiWindow", "LaporanRekapPerProdiWindow");
        petakan("laporanSKSDosenWindow", "LaporanSKSDosenWindow");
        petakan("laporanTranskripAkademik", "LaporanTranskipAkademikWindow");
        petakan("laporanTranskripAkademikKonversi", "LaporanTranskipAkademikKonversiWindow");
        petakan("laporanUjianSidangSkripsi", "LaporanNilaiUjianSidangSkripsiWindow");
        petakan("rekapAngketDosenPerDosen", "LaporanAngketDosenPerDosenWindow");
        petakan("rekapDataPmdk", "LaporanRekapitulasiPMDKWindow");
        petakan("rekapDosenPa", "LaporanRekapitulasiPAWindow");
        petakan("rekapDosenPendidikan", "LaporanRekapitulasiDosenPerPendidikanWindow");
        petakan("rekap_jumlah_dosen_semua", "LaporanRekapitulasiDosenWindow");
        petakan("rekap_jumlah_mahasiswa_fakultas", "LaporanRekapJumlahMhsFakWindow");
        petakan("rekapitulasiAlumniJurusan", "LaporanRekapitulasiAlumniJurusanWindow");
        petakan("rekapitulasiDataMahasiswa", "LaporanRekapitulasiMahasiswaWindow");
        petakan("rekapitulasiItemBiaya", "LaporanRekapitulasiItemBiayaWindow");
        petakan("rekapitulasiValidasiKeuangan", "LaporanRekapitulasiValidasiKeuanganWindow");
        petakan("rubah_password", "ChangePasswordWindow");
        }
    }

    /**
     * Nama kelas {@code *Window} untuk sebuah alias laporan.
     *
     * @param url isi kolom {@code url} milik menu
     * @return nama sederhana kelas Window, atau {@code null} bila bukan alias
     */
    public static String windowUntuk(String url) {
        if (url == null) return null;
        return ALIAS.get(url.trim());
    }

    /** Jumlah alias yang dikenal; dipakai uji mandiri. */
    public static int jumlah() {
        return ALIAS.size();
    }

    /** Salinan tabel untuk keperluan uji. */
    public static Map<String, String> semuaAlias() {
        return ALIAS;
    }
}
