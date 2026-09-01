package ais.action.master.generic.v2;

import java.util.HashMap;
import java.util.Map;

import ais.action.master.generic.v2.adapter.BankSoalGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudScopeAdapter;
import ais.action.master.generic.v2.adapter.KurikulumGenericCrudAdapter;

/**
 * Keputusan tertulis untuk layar cabang Sistem Informasi Akademik yang
 * pendaftaran otomatisnya menghasilkan READ_ONLY.
 *
 * <h3>Mengapa berupa lapisan penimpa, bukan definisi utuh</h3>
 * <p>Definisi hasil {@link GenericCrudAutoDefinitionFactory} sudah menurunkan
 * seluruh kolom dari metadata Hibernate. Menulis ulang definisi utuh per layar
 * berarti menyalin belasan kolom yang harus dijaga tetap sama dengan modelnya —
 * dan begitu model berubah, salinan itulah yang tertinggal. Yang benar-benar
 * berbeda per layar hanyalah dua hal: adapter berisi validasinya, dan boleh
 * atau tidaknya layar itu menulis. Hanya kedua hal itu yang ditimpa di sini.</p>
 *
 * <h3>Tidak semua layar READ_ONLY pantas dijadikan CRUD</h3>
 * <p>Sepuluh layar sengaja <b>tetap</b> READ_ONLY, dan alasannya ditulis di
 * bawah, bukan dibiarkan sebagai kekurangan yang tampak terlewat. Menyalakan
 * tambah/ubah pada layar-layar itu akan menghasilkan kegagalan yang tidak
 * menimbulkan galat — data tersimpan, tetapi salah sasaran atau melewati
 * pemeriksaan hak yang ada pada layar lama.</p>
 *
 * <p>Sebagian di antaranya sudah READ_ONLY sejak pabrik definisi, sehingga
 * mencantumkannya tidak mengubah perilaku apa pun. Yang ditambahkan adalah
 * alasannya: tanpa itu, layar seperti "Cek oleh bagian Keuangan" tampak seperti
 * layar yang kelewat belum dinyalakan.</p>
 */
public final class GenericCrudAkademikOverrides {

    private GenericCrudAkademikOverrides() { }

    /** Layar yang dinaikkan menjadi CRUD penuh beserta adapter validasinya. */
    private static final Map<String, Class<?>> DINAIKKAN = new HashMap<String, Class<?>>();

    /** Layar yang sengaja tetap READ_ONLY beserta alasannya. */
    private static final Map<String, String> DITAHAN = new HashMap<String, String>();

    static {
        DINAIKKAN.put("root/kurikulum", KurikulumGenericCrudAdapter.class);
        DINAIKKAN.put("root/bank_soal", BankSoalGenericCrudAdapter.class);

        DITAHAN.put("root/skripsi",
                "SkripsiAction.onSave memeriksa kepemilikan (mahasiswa yang login hanya boleh "
                + "menyimpan tugas akhirnya sendiri) dan prasyarat lewat checkSyarat(). Kedua "
                + "pemeriksaan itu bergantung pada identitas pengguna, bukan pada isi baris, "
                + "sehingga tidak dapat dipindahkan ke validasi berbasis nilai. Menyalakan CRUD "
                + "generik di sini akan membuat siapa pun yang boleh membuka menu dapat mengubah "
                + "tugas akhir milik mahasiswa lain.");
        DITAHAN.put("root/penjadwalan_ujian",
                "Entity layar ini Perkuliahan — baris penawaran mata kuliah, bukan jadwal ujian. "
                + "Tambah lewat CRUD generik akan MEMBUAT penawaran mata kuliah baru dari layar "
                + "penjadwalan ujian. Jadwal ujian menuntut kontraknya sendiri.");
        DITAHAN.put("root/daftar_mahasiswa_lulus",
                "Entity yang terpilih adalah JenisSeleksi, yaitu master jenis seleksi masuk — "
                + "bukan pokok layar ini (verifikasi berkas dan kelulusan). Menyalakan CRUD akan "
                + "membuat layar verifikasi menyunting master jenis seleksi.");
        /*
         * Lima layar berikut sudah READ_ONLY sejak pabrik definisi, jadi
         * mencantumkannya di sini TIDAK mengubah perilaku apa pun. Yang
         * ditambahkan adalah alasannya — karena kelimanya paling mudah dikira
         * "kelewat belum dinyalakan" oleh orang berikutnya, dan menyalakannya
         * justru merusak.
         */
        DITAHAN.put("root/absens_kehadiran_dosen_harian",
                "Entity-nya Dosen — master data dosen, bukan catatan kehadiran. Menyalakan CRUD "
                + "akan membuat layar absensi harian menyunting biodata dosen.");
        DITAHAN.put("root/absens_kehadiran_pegawai_harian",
                "Entity-nya Pegawai — master data pegawai, bukan catatan kehadiran. Sama dengan "
                + "layar absensi dosen.");
        DITAHAN.put("root/pengecekan_pendaftaran_wisuda_keuangan",
                "Layar verifikasi: bagian Keuangan menandai satu syarat pada PendaftaranWisuda. "
                + "Itu mutasi alur kerja pada satu penanda, bukan CRUD atas seluruh baris "
                + "pendaftaran wisuda. CRUD generik akan memberi bagian Keuangan kuasa menyunting "
                + "seluruh isi pendaftaran, termasuk yang bukan urusannya.");
        DITAHAN.put("root/pengecekan_pendaftaran_wisuda_perpustakaan",
                "Sama dengan layar pengecekan Keuangan: yang boleh diubah hanya penanda milik "
                + "bagian Perpustakaan Utama, bukan seluruh baris pendaftaran wisuda.");
        DITAHAN.put("root/pengecekan_pendaftaran_wisuda_perpustakaan_fakultas",
                "Sama dengan layar pengecekan Perpustakaan Utama, untuk Perpustakaan Fakultas.");
        DITAHAN.put("root/perkuliahan",
                "Layar Penjadwalan Ruangan, entity-nya Perkuliahan — baris penawaran mata kuliah. "
                + "Alasan yang sama dengan root/penjadwalan_ujian: tambah lewat CRUD generik akan "
                + "membuat penawaran mata kuliah baru dari layar penjadwalan.");

        DITAHAN.put("root/mahasiswa_registrasi_wisuda",
                "Layar ini memang layar tinjau (\"Melihat Pendaftar Wisuda\"). Pendaftaran "
                + "wisudanya sendiri dikerjakan layar lain; menambah tombol simpan di sini "
                + "menciptakan dua jalan masuk untuk satu data.");
    }

    /**
     * Terapkan penimpaan pada definisi yang baru dibangkitkan.
     *
     * <p>Dipanggil sekali saat pendaftaran, sebelum definisi disimpan ke
     * registri. Route yang tidak terdaftar di sini tidak disentuh sama
     * sekali.</p>
     */
    public static void terapkan(GenericCrudDefinition definition) {
        if (definition == null) return;
        String kunci = definition.getModuleKey() + "/" + definition.getPageKey();

        String alasan = DITAHAN.get(kunci);
        if (alasan != null) {
            // Ditulis ke definisinya supaya alasannya ikut terbawa ke klien dan
            // tidak berhenti sebagai komentar yang hanya terbaca di kode.
            definition.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
            definition.setCreateEnabled(false);
            definition.setUpdateEnabled(false);
            definition.setDeleteEnabled(false);
            definition.setAdminDeleteEnabled(false);
            return;
        }

        Class<?> adapterClass = DINAIKKAN.get(kunci);
        if (adapterClass == null) return;
        try {
            Object adapter = adapterClass.newInstance();
            definition.setAdapter((ais.action.master.generic.v2.adapter.GenericCrudEntityAdapter) adapter);
            if (adapter instanceof GenericCrudScopeAdapter) {
                definition.setScopeAdapter((GenericCrudScopeAdapter) adapter);
            }
            definition.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
            definition.setCreateEnabled(true);
            definition.setUpdateEnabled(true);
            /*
             * Hapus HANYA bila modelnya memang punya penanda `aktif`, karena
             * satu-satunya penghapusan yang diizinkan di sini adalah
             * penonaktifan lunak. Menyalakannya tanpa syarat akan menampilkan
             * tombol hapus pada layar yang tidak punya cara menonaktifkan —
             * tombol yang pasti gagal begitu ditekan. Bank Soal adalah
             * contohnya: modelnya tanpa kolom `aktif`.
             */
            definition.setDeleteEnabled(punyaPenandaAktif(definition.getEntityClass()));
            definition.setAdminDeleteEnabled(false);
        } catch (Exception gagal) {
            /*
             * Bila adapternya gagal dibuat, definisinya DIBIARKAN sebagaimana
             * dihasilkan pabrik — yaitu READ_ONLY. Menyalakan tambah/ubah tanpa
             * adapter berarti menyimpan tanpa validasi apa pun.
             */
            try { ais.common.ErrorAuditUtil.record(gagal, "GenericCrudAkademikOverrides:" + kunci); }
            catch (Exception diabaikan) { }
        }
    }

    /**
     * Apakah model punya penanda {@code aktif} yang dapat dituliskan.
     *
     * <p>Diperiksa lewat setter-nya, bukan lewat nama field: field bisa saja
     * diwarisi atau dipetakan dengan nama lain, sedangkan yang benar-benar
     * dipakai penonaktifan adalah setter-nya.</p>
     */
    private static boolean punyaPenandaAktif(Class<?> entityClass) {
        if (entityClass == null) return false;
        try {
            entityClass.getMethod("setAktif", new Class[] { Boolean.class });
            return true;
        } catch (NoSuchMethodException tidakAda) {
            return false;
        }
    }

    /** Alasan sebuah route sengaja ditahan sebagai READ_ONLY; null bila tidak ditahan. */
    public static String alasanDitahan(String moduleKey, String pageKey) {
        return DITAHAN.get(moduleKey + "/" + pageKey);
    }

    /** Apakah route ini dinaikkan menjadi CRUD penuh oleh lapisan ini. */
    public static boolean dinaikkan(String moduleKey, String pageKey) {
        return DINAIKKAN.containsKey(moduleKey + "/" + pageKey);
    }
}
