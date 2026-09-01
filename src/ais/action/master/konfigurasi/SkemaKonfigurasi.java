package ais.action.master.konfigurasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Skema layar konfigurasi: kunci, label, tipe, dan nilai bawaannya.
 *
 * <p><b>Mengapa harus satu sumber.</b> {@code Common.getKonfigurasi(nama,
 * bawaan)} bukan pembacaan murni — bila barisnya belum ada, ia MEMBUAT dan
 * MENYIMPAN baris itu dengan nilai bawaan yang diberikan pemanggil. Artinya
 * bila layar ZK dan kontrak native menyebut nilai bawaan yang berbeda untuk
 * kunci yang sama, siapa pun yang membuka layarnya lebih dulu akan menetapkan
 * nilai itu secara permanen. Dua daftar terpisah karena itu bukan sekadar
 * duplikasi yang membosankan, melainkan sumber perbedaan konfigurasi yang sulit
 * dilacak. Kelas ini menjadi satu-satunya tempat kunci, label, dan bawaannya
 * dideklarasikan; layar ZK maupun kontrak native membacanya dari sini.</p>
 *
 * <p>Sebagian bawaan tidak konstan — ada yang menyusun kalimat dari konfigurasi
 * lain atau dari alamat host. Perhitungannya ikut ditaruh di sini
 * ({@link Butir#bawaan()}) agar tetap satu tempat.</p>
 */
public final class SkemaKonfigurasi {

    /** Teks satu baris. */
    public static final String TEKS = "teks";
    /** Teks banyak baris; {@link Butir#baris} menyatakan tinggi kotaknya. */
    public static final String TEKS_PANJANG = "teks_panjang";
    /** Saklar Aktif / Tidak Aktif. */
    public static final String SAKLAR = "saklar";

    /** Satu butir konfigurasi. */
    public static final class Butir {
        public final String kunci;
        public final String label;
        public final String tipe;
        /** Kelompok tampilan, sama dengan nama tab pada layar ZK. */
        public final String kelompok;
        /** Tinggi kotak teks panjang; 1 untuk tipe lain. */
        public final int baris;

        private final String bawaanTetap;
        private final String bawaanDinamis;

        private Butir(String kunci, String label, String tipe, String kelompok, int baris,
                String bawaanTetap, String bawaanDinamis) {
            this.kunci = kunci;
            this.label = label;
            this.tipe = tipe;
            this.kelompok = kelompok;
            this.baris = baris;
            this.bawaanTetap = bawaanTetap;
            this.bawaanDinamis = bawaanDinamis;
        }

        static Butir teks(String kunci, String label, String kelompok, String bawaan) {
            return new Butir(kunci, label, TEKS, kelompok, 1, bawaan, null);
        }

        static Butir panjang(String kunci, String label, String kelompok, int baris, String bawaan) {
            return new Butir(kunci, label, TEKS_PANJANG, kelompok, baris, bawaan, null);
        }

        static Butir panjangDinamis(String kunci, String label, String kelompok, int baris, String penanda) {
            return new Butir(kunci, label, TEKS_PANJANG, kelompok, baris, null, penanda);
        }

        static Butir saklar(String kunci, String label, String kelompok, String bawaan) {
            return new Butir(kunci, label, SAKLAR, kelompok, 1, bawaan, null);
        }

        /**
         * Nilai bawaan butir ini.
         *
         * <p>Harus dipakai baik oleh layar ZK maupun kontrak native saat
         * memanggil {@code Common.getKonfigurasi}, karena nilai inilah yang
         * tersimpan bila barisnya belum pernah dibuat.</p>
         */
        public String bawaan() {
            if (bawaanDinamis == null) {
                return bawaanTetap;
            }
            if ("info_banner_psb".equals(bawaanDinamis)) {
                return "Kegiatan seleksi penerimaan siswa baru merupakan kegiatan yang bertujuan mendapatkan calon siswa yang berkualitas dan memiliki kompetensi dasar yang baik sesuai dengan standar yang ditetapkan. Kegiatan ini merupaka kegiatan rutin bagi "
                        + Common.getKonfigurasi("label_universitas", "").getNilai()
                        + ", karena itu penyelenggaraannya harus profesional, terjamin, terukur dan efesien.";
            }
            if ("tata_tertib_kartu_siswa".equals(bawaanDinamis)) {
                return "1. Kartu ini ditertibkan oleh ....... Segala penggunaan kartu oleh ....... sesuai ketentuan dan syarat yang berlaku.\n"
                        + "2. Kartu ini harus dibawa sebagai identitas siswa.\n"
                        + "3. Kartu ini hanya berlaku bagi pemilik dan tidak untuk orang lain.\n"
                        + "4. Siswa harus mematuhi semua tata tertib .......\n"
                        + "5. Bila menemukan kartu ini mohon mengembalikan ke .......\n" + "\n\n\n" + " .......\n"
                        + "website : " + Common.getRequestHostWithProtocol();
            }
            return "";
        }

        /** Nilai tersimpan; membuat barisnya dengan bawaan bila belum ada. */
        public String nilai() {
            Konfigurasi k = Common.getKonfigurasi(kunci, bawaan());
            return k == null || k.getNilai() == null ? "" : k.getNilai();
        }
    }

    // ------------------------------------------------------------------ SKP

    /**
     * Pengaturan Sasaran Kerja Pegawai — tiga bobot penilaian.
     *
     * <p>Ketiganya persentase yang, pada pemakaian normal, berjumlah 100.</p>
     */
    public static final List<Butir> SKP = daftar(
            Butir.teks("prosentasi_nilai_skp_kuantitas",
                    "Prosentasi nilai Sasaran Kerja Pegawai pada bidang kuantitas",
                    "Pengaturan Sasaran Kerja Pegawai", "70"),
            Butir.teks("prosentasi_nilai_skp_kualitas",
                    "Prosentasi nilai Sasaran Kerja Pegawai pada bidang kualitas",
                    "Pengaturan Sasaran Kerja Pegawai", "10"),
            Butir.teks("prosentasi_nilai_skp_waktu",
                    "Prosentasi nilai Sasaran Kerja Pegawai pada bidang waktu",
                    "Pengaturan Sasaran Kerja Pegawai", "20"));

    // -------------------------------------------------------------- Sekolah

    private static final String T1 = "Konfigurasi Sekolah";
    private static final String T2 = "PSB";
    private static final String T3 = "Kartu Siswa";

    /** Pengaturan bernilai teks/saklar pada layar Konfigurasi Sekolah. */
    public static final List<Butir> SEKOLAH = daftar(
            Butir.saklar("apakah_aktifkan_modul_sekolah", "Apakah modul sekolah / pesanren diaktifkan ?",
                    T1, Konfigurasi.TIDAK_AKTIF),
            Butir.saklar("apakah_aktifkan_modul_perguruan_tinggi", "Apakah modul perguruan tinggi diaktifkan ?",
                    T1, Konfigurasi.AKTIF),
            Butir.teks("label_instansi_sekolah", "Label Instansi / Yayasan", T1, "Instansi / Yayasan"),
            Butir.teks("alamat_instansi_sekolah", "Label Alamat Instansi / Yayasan", T1,
                    "Alamat Instansi / Yayasan"),
            Butir.teks("label_telp_instansi_sekolah", "Telp. Instansi / Yayasan", T1, "Telp. "),
            Butir.saklar("siswa_boleh_mengubah_foto_profile", "Siswa boleh mengganti foto profile sendiri",
                    T1, Konfigurasi.AKTIF),
            Butir.saklar("guru_boleh_mengubah_foto_profile", "Guru boleh mengganti foto profile sendiri",
                    T1, Konfigurasi.AKTIF),

            Butir.teks("info_dari_mana_ppdb", "Apa saja info pertanyaan yang ditampilkan ?", T2,
                    "Website,Teman,Radio,Koran,Lain-lain"),
            Butir.teks("no_whatsapp_operator", "Nomor Whatsapp yang bisa dihubungi", T2, ""),
            Butir.teks("tanya_whatsapp_psb", "Tanya Whatsapp", T2,
                    "Salamat Datang, apa yang bisa kami bantu?"),
            Butir.teks("jawab_whatsapp_psb", "Jawab Whatsapp", T2,
                    "Saya ingin menanyakan tentang informasi penerimaan siswa baru, apakah Anda bisa membantu?"),
            Butir.saklar("tampilkan_psb_di_banner", "Tampilkan Tulisan Teks penerimaan siswa baru di banner",
                    T2, Konfigurasi.AKTIF),
            Butir.panjangDinamis("info_banner_psb", "Informasi yang muncul di banner penerimaan siswa baru",
                    T2, 5, "info_banner_psb"),
            Butir.teks("tinggi_banner_psb", "Tinggi banner penerimaan siswa baru", T2, ""),
            Butir.teks("tinggi_halaman_utama_psb", "Tinggi halaman utama penerimaan siswa baru", T2, "850"),
            Butir.panjang("label_psb_sekolah", "Informasi header", T2, 5,
                    "Penerimaan Peserta Didik Baru (PPDB) Tahun Pelajaran 2022-2023"),
            Butir.panjang("informasi_kelulusan_sekolah", "Informasi yang muncul di kelulusan siswa baru", T2, 5,
                    "NIS Anda [nis], nis ini bisa Anda gunakan untuk login ke http://ecampus dengan username NIS password NIS."),
            Butir.panjang("informasi_kelulusan_tambahan_sekolah",
                    "Informasi tambahan yang muncul di kelulusan siswa baru", T2, 5,
                    "Jika Anda belum melakukan pembayaran, silahkan lakukan pembayaran di ....(tanya ke akademik);Kode pembayaran dapat dilihat di ....(tanya ke akademik)"),

            Butir.panjangDinamis("tata_tertib_kartu_siswa", "Tata Tertib Kartu Siswa", T3, 15,
                    "tata_tertib_kartu_siswa"),
            Butir.teks("label_jabatan_kartu_siswa", "Label Jabatan Kartu Siswa", T3, "Rektor"),
            Butir.teks("label_ttd_kartu_siswa", "Label TTD Kartu Siswa", T3, "...................."),
            Butir.teks("nip_ttd_kartu_siswa", "NIP Kartu Siswa", T3, "...................."),
            Butir.teks("masa_berlaku_kartu_siswa", "Masa berlaku kartu siswa", T3, "4"),
            Butir.saklar("apakah_tampilan_cr_code", "Tamilkan CR Code di belakang kartu", T3,
                    Konfigurasi.AKTIF));

    /**
     * Berkas lampiran pada layar Konfigurasi Sekolah — logo, banner, tanda
     * tangan, stempel, dan alur PDF.
     *
     * <p>Bukan pasangan kunci-nilai melainkan unggahan berkas, sehingga tidak
     * dapat disunting lewat kontrak konfigurasi. Daftarnya tetap diumumkan agar
     * klien native dapat MENYEBUTKAN keberadaannya alih-alih membiarkan
     * pengguna mengira seluruh pengaturan sudah tampil di layar barunya.</p>
     */
    public static final List<String> LAMPIRAN_SEKOLAH = Collections.unmodifiableList(senarai(
            "Alur Pendaftaran Penerimaan Siswa Baru (PDF)",
            "Background Depan Pesantren",
            "Logo Depan Pesantren",
            "Logo Depan PSB",
            "Banner Depan PSB",
            "Tanda Tangan Untuk Kartu Siswa (PNG)",
            "Stempel Untuk Kartu Siswa (PNG)",
            "Background Depan kartu Siswa",
            "Background Belakang kartu Siswa"));

    private SkemaKonfigurasi() { }

    /**
     * Simpan nilai satu butir konfigurasi.
     *
     * <p>Meniru persis langkah yang dilakukan pendengar {@code onChange} pada
     * layar ZK: perbarui barisnya lewat session native, lalu <b>segarkan
     * cache</b>. Langkah kedua itu yang mudah terlupa dan diam-diam merusak:
     * seluruh pembacaan konfigurasi melewati cache, sehingga tanpa penyegaran
     * nilai baru tersimpan di basis data namun tidak pernah terbaca sampai
     * aplikasi dimuat ulang — persoalan yang tampak seperti "simpan tidak
     * berfungsi" padahal datanya benar.</p>
     */
    public static void simpan(Butir butir, String nilai) {
        Konfigurasi konfigurasi = Common.getKonfigurasi(butir.kunci, butir.bawaan());
        konfigurasi.setNilai(nilai == null ? "" : nilai.trim());
        org.hibernate.Session session = ais.database.hibernate.HibernateUtil.currentNativeSession();
        session.getTransaction().begin();
        session.update(konfigurasi);
        session.getTransaction().commit();
        ais.common.KarirConfigUtil.closeNativeSession(session);
        ais.common.MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
    }

    /** Butir dengan kunci tertentu, atau null bila tidak dideklarasikan. */
    public static Butir cari(List<Butir> skema, String kunci) {
        if (kunci == null) {
            return null;
        }
        for (int i = 0; i < skema.size(); i++) {
            if (kunci.equals(skema.get(i).kunci)) {
                return skema.get(i);
            }
        }
        return null;
    }

    private static List<Butir> daftar(Butir... butir) {
        List<Butir> hasil = new ArrayList<Butir>();
        for (int i = 0; i < butir.length; i++) {
            hasil.add(butir[i]);
        }
        return Collections.unmodifiableList(hasil);
    }

    private static List<String> senarai(String... nilai) {
        List<String> hasil = new ArrayList<String>();
        for (int i = 0; i < nilai.length; i++) {
            hasil.add(nilai[i]);
        }
        return hasil;
    }
}
