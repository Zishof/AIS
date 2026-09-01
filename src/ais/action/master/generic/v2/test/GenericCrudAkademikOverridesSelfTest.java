package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudAkademikOverrides;
import ais.action.master.generic.v2.GenericCrudDefinition;

/**
 * Test harness tanpa JUnit untuk lapisan keputusan CRUD cabang Akademik.
 *
 * <p>Yang dijaga di sini bukan "kodenya berjalan", melainkan tiga keputusan yang
 * mudah rusak diam-diam:</p>
 * <ol>
 *   <li>layar yang sengaja ditahan tetap READ_ONLY <b>dan</b> ketiga saklar
 *       tulisnya mati — bukan sekadar statusnya berlabel READ_ONLY;</li>
 *   <li>layar yang dinaikkan mendapat tambah/ubah;</li>
 *   <li>hapus hanya menyala bila modelnya punya penanda {@code aktif}. Inilah
 *       yang paling mudah salah: menyalakan hapus tanpa syarat menghasilkan
 *       tombol yang pasti gagal begitu ditekan.</li>
 * </ol>
 */
public final class GenericCrudAkademikOverridesSelfTest {

    private GenericCrudAkademikOverridesSelfTest() { }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    /** Definisi tiruan yang sudah "dihasilkan pabrik": mutable, hapus menyala. */
    private static GenericCrudDefinition definisi(String module, String page, Class<?> entity) {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setModuleKey(module);
        d.setPageKey(page);
        d.setEntityClass(entity);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setAdminDeleteEnabled(true);
        return d;
    }

    public static void main(String[] args) {
        // 1. Layar yang ditahan: seluruh saklar tulis harus mati, bukan hanya
        //    labelnya berubah. Definisi tiruannya sengaja datang dalam keadaan
        //    SERBA MENYALA supaya kegagalan mematikan benar-benar terlihat.
        String[][] ditahan = {
            { "root", "skripsi" },
            { "root", "penjadwalan_ujian" },
            { "root", "daftar_mahasiswa_lulus" },
            { "root", "mahasiswa_registrasi_wisuda" },
            { "penelitiandanpengabdian", "pengajuan_penelitian_dan_pengabdian" },
        };
        for (int i = 0; i < ditahan.length; i++) {
            String modul = ditahan[i][0], halaman = ditahan[i][1];
            GenericCrudDefinition d = definisi(modul, halaman, ais.database.model.Kurikulum.class);
            GenericCrudAkademikOverrides.terapkan(d);
            check(GenericCrudDefinition.READ_ONLY.equals(d.getLifecycleStatus()),
                    modul + "/" + halaman + " harus READ_ONLY");
            check(!d.isCreateEnabled(), modul + "/" + halaman + " tidak boleh bisa tambah");
            check(!d.isUpdateEnabled(), modul + "/" + halaman + " tidak boleh bisa ubah");
            check(!d.isDeleteEnabled(), modul + "/" + halaman + " tidak boleh bisa hapus");
            check(!d.isAdminDeleteEnabled(),
                    modul + "/" + halaman + " tidak boleh bisa hapus permanen");
            check(GenericCrudAkademikOverrides.alasanDitahan(modul, halaman) != null,
                    modul + "/" + halaman + " harus punya alasan tertulis");
        }

        // 2. Kurikulum dinaikkan penuh: modelnya punya `aktif`, jadi hapus lunak
        //    ikut menyala.
        GenericCrudDefinition kurikulum = definisi("root", "kurikulum",
                ais.database.model.Kurikulum.class);
        kurikulum.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
        kurikulum.setCreateEnabled(false);
        kurikulum.setUpdateEnabled(false);
        kurikulum.setDeleteEnabled(false);
        GenericCrudAkademikOverrides.terapkan(kurikulum);
        check(GenericCrudDefinition.FULL_CRUD.equals(kurikulum.getLifecycleStatus()),
                "Kurikulum harus naik menjadi FULL_CRUD");
        check(kurikulum.isCreateEnabled() && kurikulum.isUpdateEnabled(),
                "Kurikulum harus bisa tambah dan ubah");
        check(kurikulum.isDeleteEnabled(),
                "Kurikulum punya kolom aktif sehingga hapus lunak harus menyala");
        check(!kurikulum.isAdminDeleteEnabled(),
                "hapus permanen tidak boleh ikut menyala");
        check(kurikulum.getAdapter() != null, "Kurikulum harus memakai adapter validasinya");

        // 3. Bank Soal dinaikkan, TETAPI modelnya tidak punya `aktif`. Hapus
        //    harus tetap mati meski layarnya menjadi FULL_CRUD.
        GenericCrudDefinition bankSoal = definisi("root", "bank_soal",
                ais.database.model.BankSoal.class);
        GenericCrudAkademikOverrides.terapkan(bankSoal);
        check(GenericCrudDefinition.FULL_CRUD.equals(bankSoal.getLifecycleStatus()),
                "Bank Soal harus naik menjadi FULL_CRUD");
        check(bankSoal.isCreateEnabled() && bankSoal.isUpdateEnabled(),
                "Bank Soal harus bisa tambah dan ubah");
        check(!bankSoal.isDeleteEnabled(),
                "Bank Soal tanpa kolom aktif TIDAK boleh mendapat tombol hapus");

        // 4. Bagan akun dinaikkan; modelnya TANPA kolom aktif, jadi hapus harus
        //    tetap mati. Ini penjaga yang paling penting di cabang Akuntansi:
        //    akun yang pernah dipakai masih diacu setiap baris jurnal.
        GenericCrudDefinition akun = definisi("akunting", "akun",
                ais.database.model.akunting.Akun.class);
        GenericCrudAkademikOverrides.terapkan(akun);
        check(GenericCrudDefinition.FULL_CRUD.equals(akun.getLifecycleStatus()),
                "Setup Kode Akun harus naik menjadi FULL_CRUD");
        check(akun.isCreateEnabled() && akun.isUpdateEnabled(),
                "Setup Kode Akun harus bisa tambah dan ubah");
        check(!akun.isDeleteEnabled(),
                "Akun tanpa kolom aktif TIDAK boleh mendapat tombol hapus");
        check(akun.getAdapter() != null, "Setup Kode Akun harus memakai adapter validasinya");

        // 5. Layar keuangan yang menyentuh uang harus TETAP tertutup, meski
        //    definisi tiruannya datang serba menyala.
        String[][] keuangan = {
            { "akunting", "posting_transaksi_harian" },
            { "akunting", "pertangungjawaban_kas_besar" },
            { "root", "posting_cicilan_mahasiswa" },
            { "koperasi", "pembelian_anggota_koperasi" },
        };
        for (int i = 0; i < keuangan.length; i++) {
            GenericCrudDefinition d = definisi(keuangan[i][0], keuangan[i][1],
                    ais.database.model.akunting.Akun.class);
            GenericCrudAkademikOverrides.terapkan(d);
            String kunci = keuangan[i][0] + "/" + keuangan[i][1];
            check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(),
                    kunci + " menyentuh uang; seluruh saklar tulisnya harus mati");
            check(GenericCrudAkademikOverrides.alasanDitahan(keuangan[i][0], keuangan[i][1]) != null,
                    kunci + " harus punya alasan tertulis");
        }

        // 6. Default Checklist dinaikkan; modelnya juga tanpa kolom aktif.
        GenericCrudDefinition checklist = definisi("rab", "checklist_laporan_detail_default",
                ais.database.model.rab.ChecklistLaporanDetailDefault.class);
        GenericCrudAkademikOverrides.terapkan(checklist);
        check(GenericCrudDefinition.FULL_CRUD.equals(checklist.getLifecycleStatus()),
                "Default Checklist harus naik menjadi FULL_CRUD");
        check(checklist.isCreateEnabled() && checklist.isUpdateEnabled(),
                "Default Checklist harus bisa tambah dan ubah");
        check(!checklist.isDeleteEnabled(),
                "ChecklistLaporanDetailDefault tanpa kolom aktif TIDAK boleh mendapat hapus");

        // 7. Lima panel Antar Jemput naik dan mendapat hapus lunak; empat
        //    panel lain di cabang yang SAMA harus tetap tertutup. Keduanya
        //    diperiksa berdampingan supaya pemisahannya tidak diam-diam bergeser.
        String[][] ajNaik = {
            { "panel_kendaraan", "ais.database.model.antarjemput.KendaraanAntarJemput" },
            { "panel_rute", "ais.database.model.antarjemput.RuteAntarJemput" },
            { "panel_jadwal", "ais.database.model.antarjemput.JadwalAntarJemput" },
            { "panel_peserta", "ais.database.model.antarjemput.PesertaJadwalAntarJemput" },
            { "panel_kartu", "ais.database.model.antarjemput.KartuPenjemputAntarJemput" },
        };
        for (int i = 0; i < ajNaik.length; i++) {
            Class<?> ent;
            try { ent = Class.forName(ajNaik[i][1]); }
            catch (ClassNotFoundException e) { throw new IllegalStateException(ajNaik[i][1], e); }
            GenericCrudDefinition d = definisi("antarjemput", ajNaik[i][0], ent);
            d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
            d.setCreateEnabled(false); d.setUpdateEnabled(false); d.setDeleteEnabled(false);
            GenericCrudAkademikOverrides.terapkan(d);
            check(GenericCrudDefinition.FULL_CRUD.equals(d.getLifecycleStatus()),
                    "antarjemput/" + ajNaik[i][0] + " harus naik menjadi FULL_CRUD");
            check(d.isCreateEnabled() && d.isUpdateEnabled(),
                    "antarjemput/" + ajNaik[i][0] + " harus bisa tambah dan ubah");
            check(d.isDeleteEnabled(),
                    "antarjemput/" + ajNaik[i][0] + " punya kolom aktif; hapus lunak harus menyala");
            check(d.getAdapter() != null,
                    "antarjemput/" + ajNaik[i][0] + " harus memakai adapter validasinya");
        }
        String[] ajTahan = { "panel_transaksi", "panel_detail", "panel_log", "antar_jemput" };
        for (int i = 0; i < ajTahan.length; i++) {
            GenericCrudDefinition d = definisi("antarjemput", ajTahan[i],
                    ais.database.model.Kurikulum.class);
            GenericCrudAkademikOverrides.terapkan(d);
            check(!d.isCreateEnabled() && !d.isUpdateEnabled() && !d.isDeleteEnabled(),
                    "antarjemput/" + ajTahan[i] + " harus tetap tertutup");
            check(GenericCrudAkademikOverrides.alasanDitahan("antarjemput", ajTahan[i]) != null,
                    "antarjemput/" + ajTahan[i] + " harus punya alasan tertulis");
        }

        // 8. Route di luar daftar tidak boleh tersentuh sama sekali.
        GenericCrudDefinition lain = definisi("sekolah", "pembayaran_siswa",
                ais.database.model.Kurikulum.class);
        GenericCrudAkademikOverrides.terapkan(lain);
        check(GenericCrudDefinition.FULL_CRUD.equals(lain.getLifecycleStatus())
                && lain.isCreateEnabled() && lain.isUpdateEnabled() && lain.isDeleteEnabled()
                && lain.isAdminDeleteEnabled(),
                "route di luar cabang Akademik tidak boleh diubah lapisan ini");

        System.out.println("PASS Generic CRUD Akademik overrides self-test");
    }
}
