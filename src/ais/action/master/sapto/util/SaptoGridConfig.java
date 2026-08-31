package ais.action.master.sapto.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Tipe khusus untuk sapto grid config. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Map CONFIGS}; pembacaan/pencarian
 * ({@code getConfig()}); operasi domain lain ({@code put()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class SaptoGridConfig {

    /**
     * Tipe implementasi bersarang {@link Config} milik {@link SaptoGridConfig}. Kelas ini memberi nama pada state
     * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link SaptoGridConfig}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String description}, {@code String
     * headers}, {@code int dataStartRow}, {@code String chartType}, {@code boolean dynamic}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see SaptoGridConfig
     */
    public static class Config {
        public final String description;
        public final String[] headers;
        public final int dataStartRow;
        public final String chartType; // "bar", "stacked", "line", "pie", "radar", "none"
        /**
         * DINAMIS: kolom laporan tidak diketahui di muka (mis. "data_umum" yang
         * SQL-nya bebas). Saat true, header & grafik diambil OTOMATIS dari data
         * (baris ke-1 = nama kolom, baris ke-2 dst = isi), bukan dari {@link #headers}.
         */
        public final boolean dynamic;

        public Config(String description, String[] headers, int dataStartRow, String chartType) {
            this(description, headers, dataStartRow, chartType, false);
        }

        public Config(String description, String[] headers, int dataStartRow, String chartType, boolean dynamic) {
            this.description = description;
            this.headers = headers;
            this.dataStartRow = dataStartRow;
            this.chartType = chartType;
            this.dynamic = dynamic;
        }
    }

    private static final Map<String, Config> CONFIGS = new HashMap<String, Config>();

    static {
        put("A-2.4.6",
            "Status akreditasi seluruh program studi: berapa prodi yang Unggul, Baik Sekali, Baik, dan belum terakreditasi, dikelompokkan per jenjang pendidikan.",
            new String[]{"Status Akreditasi","S3","S2","S1","Sp-2","Sp-1","Profesi","D4","D3","D2","D1","Total"},
            9, "bar");

        put("A-3.1.1",
            "Perkembangan mahasiswa baru, aktif, dan lulusan program reguler setiap tahun akademik, lengkap dengan data pendaftar dan yang diterima.",
            new String[]{"Tahun","Daya Tampung","Pendaftar","Diterima","Mhs. Baru","Mhs. Aktif","Lulusan"},
            8, "line");

        put("A-3.1.2",
            "Perkembangan mahasiswa baru, aktif, dan lulusan program non-reguler (kelas karyawan/ekstensi) setiap tahun akademik.",
            new String[]{"Tahun","Daya Tampung","Pendaftar","Diterima","Mhs. Baru","Mhs. Aktif","Lulusan"},
            8, "line");

        put("A-3.1.4",
            "Rekap status mahasiswa (Aktif, Cuti, Tidak Aktif, Lulus, Keluar) per angkatan dan tahun akademik.",
            new String[]{"Angkatan","Aktif","Cuti","Tidak Aktif","Lulus","Keluar","Total"},
            8, "stacked");

        put("A-3.1.5",
            "Jumlah mahasiswa terdaftar di setiap program studi selama 5 tahun terakhir — memperlihatkan tren pertumbuhan atau penurunan peminat.",
            new String[]{"Program Studi","T-4","T-3","T-2","T-1","T"},
            8, "line");

        put("A-3.1.8",
            "Frekuensi dan jumlah peserta kegiatan layanan kepada mahasiswa: bimbingan konseling, beasiswa, pengembangan karir, dan lainnya.",
            new String[]{"Jenis Layanan","Frekuensi Kegiatan","Jumlah Peserta"},
            8, "bar");

        put("A-3.1.11",
            "Daftar prestasi dan penghargaan yang diraih mahasiswa di tingkat lokal, nasional, dan internasional selama 3 tahun terakhir.",
            new String[]{"No","Nama Mahasiswa","Nama Kegiatan","Tahun","Internasional","Nasional","Lokal"},
            8, "bar");

        put("A-3.2.1",
            "Rata-rata masa studi dan IPK lulusan per jenjang (S1, S2, S3, D4, D3, D2, D1) setiap tahun — mengukur kualitas dan efisiensi kelulusan.",
            new String[]{"Tahun Lulus","Jumlah Lulusan","IPK < 2.75","2.75 ≤ IPK < 3.0","3.0 ≤ IPK < 3.5","IPK ≥ 3.5","Rata-rata IPK","Lama ≤ Ideal","Lama > Ideal"},
            5, "line");

        put("A-3.2.2",
            "Rata-rata masa studi dan IPK lulusan program studi dalam beberapa tahun terakhir.",
            new String[]{"Tahun","Jumlah Lulusan","IPK Rata-rata","Masa Studi Rata-rata (bulan)"},
            8, "line");

        put("A-3.2.4",
            "Hasil pelacakan alumni (tracer study): tempat kerja pertama, lama waktu tunggu kerja, dan kesesuaian bidang kerja dengan jurusan.",
            new String[]{"Tahun Lulus","Jumlah Alumni","Bekerja","Waktu Tunggu (bln)","Sesuai Bidang"},
            8, "pie");

        put("A-4.3.1_PT",
            "Rekapitulasi dosen tetap seluruh institusi berdasarkan pendidikan tertinggi dan jabatan fungsional akademik (Guru Besar s.d. Tenaga Pengajar).",
            new String[]{"Jenjang / Jabatan","Guru Besar","Lektor Kepala","Lektor","Asisten Ahli","Tenaga Pengajar","Total"},
            11, "stacked");

        put("A-4.3.1",
            "Profil lengkap dosen tetap program studi: nama, riwayat pendidikan S1/S2/S3, bidang ilmu, dan universitas asal.",
            new String[]{"No","Nama Dosen","S1 - Bidang Ilmu","S1 - PT","S2 - Bidang Ilmu","S2 - PT","S3 - Bidang Ilmu","S3 - PT","Sertifikasi"},
            13, "none");

        put("A-4.3.2_PT",
            "Dosen tidak tetap institusi berdasarkan jenjang pendidikan tertinggi dan jabatan fungsional.",
            new String[]{"Jenjang / Jabatan","Guru Besar","Lektor Kepala","Lektor","Asisten Ahli","Tenaga Pengajar","Total"},
            11, "bar");

        put("A-4.3.2",
            "Profil dosen tidak tetap program studi: nama, riwayat pendidikan, dan bidang keahlian.",
            new String[]{"No","Nama Dosen","S1 - Bidang Ilmu","S1 - PT","S2 - Bidang Ilmu","S2 - PT","S3 - Bidang Ilmu","S3 - PT"},
            13, "none");

        put("A-4.3.3",
            "Beban kerja dosen tetap per semester: jumlah SKS pengajaran, penelitian, pengabdian, dan total keseluruhan.",
            new String[]{"No","Nama Dosen","SKS Mengajar","SKS Penelitian","SKS Pengabdian","Total SKS","Mengajar di PT Lain"},
            13, "bar");

        put("A-4.3.4",
            "Daftar mata kuliah yang diajarkan dosen sesuai bidang keahliannya, lengkap dengan jumlah pertemuan yang terlaksana.",
            new String[]{"No","Nama Dosen","Kode MK","Nama Mata Kuliah","SKS","Pertemuan Rencana","Pertemuan Aktual"},
            13, "none");

        put("A-4.3.5",
            "Daftar mata kuliah yang diajarkan dosen di luar bidang keahliannya (tidak sesuai bidang keilmuan).",
            new String[]{"No","Nama Dosen","Kode MK","Nama Mata Kuliah","SKS","Pertemuan Rencana","Pertemuan Aktual"},
            13, "none");

        put("A-4.4.1",
            "Profil dosen tidak tetap program studi: nama, pendidikan, dan bidang keahlian.",
            new String[]{"No","Nama Dosen","S1 - Bidang Ilmu","S1 - PT","S2 - Bidang Ilmu","S2 - PT","S3 - Bidang Ilmu","S3 - PT"},
            13, "none");

        put("A-4.4.2",
            "Beban mengajar dosen tidak tetap per semester: SKS pengajaran dan keterlibatan dalam kegiatan penelitian.",
            new String[]{"No","Nama Dosen","SKS Mengajar","SKS Penelitian","SKS Pengabdian","Total SKS"},
            13, "bar");

        put("A-4.4",
            "Data dosen yang sedang menempuh pendidikan lanjut (tugas belajar S2/S3) selama 3 tahun terakhir.",
            new String[]{"Jenjang Studi","T-2","T-1","T","Total"},
            8, "bar");

        put("A-4.5.1",
            "Daftar dosen tamu (narasumber dari luar) yang diundang memberikan kuliah di program studi dalam 3 tahun terakhir.",
            new String[]{"No","Nama Narasumber","Gelar/Jabatan","Topik","Tahun"},
            8, "none");

        put("A-4.5.1_PT",
            "Rekapitulasi tenaga kependidikan (pustakawan, programmer, administrasi, dan lainnya) berdasarkan jenjang pendidikan.",
            new String[]{"Jabatan/Kategori","S3","S2","S1","D4","D3","D2","D1","SMA","Total"},
            11, "stacked");

        put("A-4.5.2",
            "Daftar dosen yang sedang atau telah menempuh tugas belajar ke luar negeri maupun dalam negeri.",
            new String[]{"No","Nama Dosen","Bidang Studi","Negara Tujuan","Tahun"},
            8, "none");

        put("A-4.5.3",
            "Keterlibatan dosen dalam seminar, konferensi, atau pelatihan ilmiah sebagai narasumber maupun peserta.",
            new String[]{"No","Nama Dosen","Nama Kegiatan","Tahun","Sebagai Narasumber","Sebagai Peserta"},
            8, "bar");

        put("A-4.5.4",
            "Penghargaan dan prestasi yang diterima dosen di tingkat lokal, nasional, dan internasional.",
            new String[]{"No","Nama Dosen","Nama Penghargaan","Tahun","Internasional","Nasional","Lokal"},
            8, "bar");

        put("A-4.5.5",
            "Keterlibatan dosen dalam organisasi keilmuan atau profesi di tingkat lokal, nasional, dan internasional.",
            new String[]{"No","Nama Dosen","Nama Organisasi","Tahun","Internasional","Nasional","Lokal"},
            9, "bar");

        put("A-4.6.1",
            "Profil tenaga kependidikan berdasarkan jenis jabatan dan jenjang pendidikan tertinggi.",
            new String[]{"Jabatan","S3","S2","S1","D4","D3","D2","D1","SMA/SMK","Total"},
            9, "stacked");

        put("A-5.1.2.1",
            "Jumlah SKS yang harus ditempuh mahasiswa: berapa SKS yang wajib dan berapa yang boleh dipilih sendiri sesuai kurikulum.",
            new String[]{"Kategori SKS","Jumlah SKS"},
            10, "pie");

        put("A-5.1.2.2",
            "Daftar mata kuliah pilihan yang tersedia di program studi beserta jumlah SKS-nya.",
            new String[]{"No","Kode MK","Nama Mata Kuliah Pilihan","SKS"},
            8, "none");

        put("A-5.1.3",
            "Distribusi mata kuliah per semester: jumlah mata kuliah dan total SKS yang dijadwalkan di setiap semester.",
            new String[]{"Semester","Jumlah MK","Total SKS"},
            8, "bar");

        put("A-5.4.1",
            "Beban bimbingan akademik setiap dosen PA: jumlah mahasiswa yang dibimbing dan frekuensi pertemuan bimbingan.",
            new String[]{"No","Nama Dosen PA","Jumlah Mhs. Bimbingan","Jumlah Pertemuan"},
            8, "bar");

        put("A-5.5.1",
            "Dosen pembimbing skripsi/tugas akhir beserta jumlah mahasiswa yang dibimbing sebagai Pembimbing 1, Pembimbing 2, dan Ketua Sidang.",
            new String[]{"No","Nama Dosen","Pembimbing 1","Pembimbing 2","Ketua Sidang","Total"},
            8, "bar");

        put("A-5.5.2",
            "Rata-rata waktu yang dibutuhkan mahasiswa untuk menyelesaikan tugas akhir/skripsi, dihitung per tahun kelulusan.",
            new String[]{"Tahun","Rata-rata Penyelesaian (bulan)"},
            8, "line");

        put("A-6.1.4_PT",
            "Sumber dan jumlah dana yang diterima institusi dari berbagai pihak (mahasiswa, yayasan, pemerintah, dll.) dalam 3 tahun terakhir.",
            new String[]{"Sumber Dana","T-2 (Rp)","T-1 (Rp)","T (Rp)","Rata-rata (Rp)"},
            10, "stacked");

        put("A-6.1.5_PT",
            "Penggunaan dana institusi berdasarkan kategori (pendidikan, penelitian, pengabdian, investasi sarana, dll.) dalam 3 tahun terakhir.",
            new String[]{"Jenis Penggunaan","T-2 (Rp)","T-1 (Rp)","T (Rp)","Rata-rata (Rp)"},
            7, "stacked");

        put("A-6.1.6_PT",
            "Dana penelitian yang dikeluarkan institusi berdasarkan sumber dana dalam 3 tahun terakhir.",
            new String[]{"Sumber Dana Penelitian","T-2 (Rp)","T-1 (Rp)","T (Rp)","Rata-rata (Rp)"},
            7, "bar");

        put("A-6.1.7_PT",
            "Dana pengabdian masyarakat yang dikeluarkan institusi berdasarkan sumber dana dalam 3 tahun terakhir.",
            new String[]{"Sumber Dana Pengabdian","T-2 (Rp)","T-1 (Rp)","T (Rp)","Rata-rata (Rp)"},
            7, "bar");

        put("A-6.2.1.1",
            "Penerimaan dana program studi dari berbagai sumber (yayasan, pemerintah, mahasiswa) selama 3 tahun terakhir.",
            new String[]{"Sumber Dana","T-2 (Rp)","T-1 (Rp)","T (Rp)","Rata-rata (Rp)"},
            7, "stacked");

        put("A-6.2.1.2",
            "Penggunaan dana program studi berdasarkan kategori (pendidikan, penelitian, sarana, dll.) selama 3 tahun terakhir.",
            new String[]{"Jenis Penggunaan","T-2 (Rp)","T-1 (Rp)","T (Rp)","Rata-rata (Rp)"},
            7, "stacked");

        put("A-6.2.2",
            "Dana penelitian dosen program studi yang disetujui: judul, nama dosen, sumber dana, dan nilainya per tahun.",
            new String[]{"No","Judul Penelitian","Nama Dosen","Sumber Dana","Jumlah Dana (Rp)","Tahun"},
            8, "bar");

        put("A-6.2.3",
            "Dana pengabdian masyarakat dosen program studi yang disetujui: judul kegiatan, nama dosen, sumber, dan nilainya.",
            new String[]{"No","Judul Pengabdian","Nama Dosen","Sumber Dana","Jumlah Dana (Rp)","Tahun"},
            8, "bar");

        put("A-6.2.3A",
            "Prasarana utama kampus: jenis fasilitas, jumlah, status kepemilikan (milik/sewa/pinjam/kerjasama), kondisi, dan luas.",
            new String[]{"No","Jenis Prasarana","Jml","Milik Sendiri","Sewa","Pinjaman","Kerjasama","Terawat","Tidak Terawat","Luas (m²)"},
            11, "bar");

        put("A-6.2.3B",
            "Prasarana pendukung kampus (non-utama): jenis fasilitas, kepemilikan, kondisi, dan luas.",
            new String[]{"No","Jenis Prasarana","Jml","Milik Sendiri","Sewa","Pinjaman","Kerjasama","Terawat","Tidak Terawat","Luas (m²)"},
            11, "bar");

        put("A-6.2.4",
            "Rencana dan realisasi investasi pengembangan prasarana kampus beserta sumber dananya.",
            new String[]{"Jenis Prasarana","Nilai Investasi 3 Tahun (Rp)","Rencana Investasi (Rp)","Sumber Dana"},
            8, "bar");

        put("A-6.2.5",
            "Koleksi perpustakaan kampus: jumlah buku teks, jurnal ilmiah cetak, dan akses database digital.",
            new String[]{"Jenis Koleksi","Jumlah Judul","Jumlah Eksemplar/Akses"},
            8, "bar");

        put("A-6.2.7",
            "Ketersediaan fasilitas aksesibilitas kampus bagi mahasiswa berkebutuhan khusus (ramp, lift, toilet khusus, dll.).",
            new String[]{"Jenis Aksesibilitas","Tersedia","Tidak Tersedia"},
            8, "none");

        put("A-6.3.1",
            "Ruang kerja dosen: jumlah ruangan yang ditempati 1, 2, 3, atau 4 orang dosen, beserta total luasnya.",
            new String[]{"Kapasitas Penghuni","Jumlah Ruangan","Total Luas (m²)"},
            6, "bar");

        put("A-6.4.1.1",
            "Koleksi perpustakaan program studi: jumlah buku teks, jurnal, dan akses digital yang relevan dengan bidang studi.",
            new String[]{"Jenis Koleksi","Jumlah Judul","Jumlah Eksemplar/Akses"},
            8, "bar");

        put("A-7.1.2_PT",
            "Jumlah penelitian dosen tetap institusi yang mendapat dana dari berbagai sumber dalam 3 tahun terakhir.",
            new String[]{"Sumber Dana","T-2","T-1","T","Total"},
            7, "stacked");

        put("A-7.1.2",
            "Jumlah penelitian dosen tetap program studi yang mendapat pendanaan dari berbagai sumber.",
            new String[]{"Sumber Dana","T-2","T-1","T","Total"},
            7, "stacked");

        put("A-7.1.3",
            "Jumlah publikasi ilmiah dosen tetap di jurnal lokal, nasional terakreditasi, dan internasional selama 3 tahun terakhir.",
            new String[]{"Jenis Publikasi","T-2","T-1","T","Total"},
            8, "stacked");

        put("A-7.1.4",
            "Jumlah artikel dosen yang sudah terindeks di database internasional (Scopus, Web of Science) dan mendapat sitasi.",
            new String[]{"Tahun","Jumlah Artikel Terindeks & Tersitasi"},
            8, "line");

        put("A-7.1.5",
            "Karya dan penghargaan dosen tetap: perolehan paten, HaKI, dan penghargaan lainnya dalam 3 tahun terakhir.",
            new String[]{"No","Nama Dosen","Nama Karya/Penghargaan","Tahun","Paten","HaKI","Lainnya"},
            8, "bar");

        put("A-7.2.2",
            "Kegiatan pengabdian masyarakat dosen tetap yang mendapat dukungan dana dari berbagai sumber.",
            new String[]{"No","Judul Kegiatan","Nama Dosen","Sumber Dana","Jumlah Dana (Rp)","Tahun"},
            8, "bar");

        put("A-7.3.2",
            "Daftar kerjasama institusi dengan mitra dalam negeri: nama mitra, jenis kegiatan, durasi, dan manfaat yang diperoleh.",
            new String[]{"No","Nama Instansi Mitra","Jenis Kegiatan","Mulai","Berakhir","Manfaat"},
            8, "none");

        put("A-7.3.3",
            "Daftar kerjasama institusi dengan mitra luar negeri: nama mitra, jenis kegiatan, durasi, dan manfaat.",
            new String[]{"No","Nama Instansi Mitra","Jenis Kegiatan","Mulai","Berakhir","Manfaat"},
            8, "none");

        put("DOSEN",
            "Daftar seluruh dosen beserta riwayat pendidikan tertinggi (S1, S2, S3) dan bidang keahlian masing-masing.",
            new String[]{"No","NIP/NIDK","Nama Dosen","S1 - Bidang","S1 - PT","S2 - Bidang","S2 - PT","S3 - Bidang","S3 - PT"},
            6, "none");

        // ── LKPS (Laporan Kinerja Program Studi) — Akreditasi Unggul S1 ──────────────
        put("LKPS-1.A.2",
            "Sumber pendanaan program studi (dari PT/Yayasan, Kemendikbud, lembaga lain) selama 3 tahun terakhir (TS-2, TS-1, TS) dalam juta rupiah.",
            new String[]{"No","Sumber Dana","TS-2 (Rp juta)","TS-1 (Rp juta)","TS (Rp juta)"},
            7, "stacked");

        put("LKPS-1.A.3",
            "Penggunaan dana program studi per jenis kegiatan (pendidikan, penelitian, PkM, dll.) selama 3 tahun terakhir (TS-2, TS-1, TS) dalam juta rupiah.",
            new String[]{"No","Jenis Penggunaan","TS-2 (Rp juta)","TS-1 (Rp juta)","TS (Rp juta)"},
            6, "stacked");

        put("LKPS-2.A.1",
            "Data mahasiswa S1: daya tampung, jumlah pendaftar, mahasiswa yang diterima, dan mahasiswa aktif per tahun akademik (5 tahun terakhir).",
            new String[]{"Tahun","Daya Tampung","Pendaftar","Diterima","Mhs. Aktif"},
            6, "line");

        put("LKPS-3.A.2",
            "Jumlah penelitian dosen tetap program studi (DTPR) per sumber dana (mandiri, PT, Kemdikbud, luar negeri, dll.) selama 3 tahun terakhir.",
            new String[]{"Sumber Dana","TS-2","TS-1","TS"},
            7, "bar");

        put("LKPS-3.C.2",
            "Jumlah publikasi ilmiah dosen tetap per jenis media (jurnal nasional, jurnal internasional, prosiding, buku, dll.) selama 3 tahun terakhir.",
            new String[]{"Jenis Media Publikasi","TS-2","TS-1","TS"},
            6, "bar");

        put("LKPS-4.A.2",
            "Jumlah kegiatan Pengabdian kepada Masyarakat (PkM) dosen tetap program studi per sumber dana selama 3 tahun terakhir.",
            new String[]{"Sumber Dana","TS-2","TS-1","TS"},
            7, "bar");

        // ── DKPS 2.0 (Deskripsi Kinerja Program Studi / LAMDIK S1) ──────────────
        // Header kolom PERSIS sesuai template Excel DKPS 2.0 resmi BAN-PT (semua 4 prodi identik)
        // Kriteria 2 — Kerjasama Tridharma
        put("DKPS-2.1-1",
            "Tabel 2.1 Bagian 1 — Kerjasama bidang Pendidikan: lembaga mitra, tingkat, judul kegiatan, manfaat bagi PS, tanggal awal/akhir, durasi, dan bukti kerjasama.",
            new String[]{"No","Lembaga Mitra","Tingkat","Judul Kegiatan Kerjasama","Manfaat bagi PS yang Diakreditasi","Tanggal Awal Kerjasama","Tanggal Akhir Kerjasama","Durasi (Tahun)","Bukti Kerjasama"},
            6, "bar");

        put("DKPS-2.1-2",
            "Tabel 2.1 Bagian 2 — Kerjasama bidang Penelitian: lembaga mitra riset, judul kegiatan, manfaat, periode, dan bukti kerjasama.",
            new String[]{"No","Lembaga Mitra","Tingkat","Judul Kegiatan Kerjasama","Manfaat bagi PS yang Diakreditasi","Tanggal Awal Kerjasama","Tanggal Akhir Kerjasama","Durasi (Tahun)","Bukti Kerjasama"},
            6, "bar");

        put("DKPS-2.1-3",
            "Tabel 2.1 Bagian 3 — Kerjasama bidang Pengabdian kepada Masyarakat (PkM): lembaga mitra, judul kegiatan, manfaat, periode, dan bukti kerjasama.",
            new String[]{"No","Lembaga Mitra","Tingkat","Judul Kegiatan Kerjasama","Manfaat bagi PS yang Diakreditasi","Tanggal Awal Kerjasama","Tanggal Akhir Kerjasama","Durasi (Tahun)","Bukti Kerjasama"},
            6, "bar");

        // Kriteria 3 — Mahasiswa
        put("DKPS-3.1",
            "Tabel 3.1 Seleksi Mahasiswa Baru — daya tampung, pendaftar, lulus seleksi, mahasiswa baru (regular/transfer), dan mahasiswa aktif selama 5 tahun terakhir.",
            new String[]{"Tahun Akademik","Daya Tampung","Pendaftar","Lulus Seleksi","Mhs Baru Regular","Mhs Baru Transfer","Mhs Aktif Regular","Mhs Aktif Transfer"},
            5, "line");

        put("DKPS-3.2",
            "Tabel 3.2 Prestasi Akademik dan Non-Akademik Mahasiswa — nama kegiatan, waktu perolehan, tingkat (lokal/nasional/internasional), dan prestasi yang dicapai.",
            new String[]{"No","Nama Kegiatan","Waktu Perolehan (dd/mm/yyyy)","Lokal/Wilayah","Nasional","Internasional","Prestasi yang Dicapai"},
            7, "bar");

        put("DKPS-3.3-1",
            "Tabel 3.3 Bagian 1 — HKI Paten (Granted): NIM+nama mahasiswa, judul karya, tahun, dan nomor paten.",
            new String[]{"No","NIM dan Nama Mahasiswa","Judul Karya Inovatif/Luaran","Tahun (dd/mm/yyyy)","No. Patent (Granted)"},
            4, "none");

        put("DKPS-3.3-2",
            "Tabel 3.3 Bagian 2 — HKI Hak Cipta, Desain, Program Komputer, Alat Peraga: NIM+nama mahasiswa, judul karya, tahun, dan nomor sertifikat.",
            new String[]{"No","NIM dan Nama Mahasiswa","Judul Karya Inovatif/Luaran","Tahun (dd/mm/yyyy)","Keterangan (No. Sertifikat)"},
            4, "none");

        put("DKPS-3.3-3",
            "Tabel 3.3 Bagian 3 — Buku ber-ISBN: NIM+nama mahasiswa, judul buku/book chapter, tahun, dan nomor ISBN.",
            new String[]{"No","NIM dan Nama Mahasiswa","Judul Karya Inovatif/Luaran","Tahun (dd/mm/yyyy)","Keterangan (No. ISBN)"},
            4, "none");

        put("DKPS-3.3-4",
            "Tabel 3.3 Bagian 4 — Publikasi Ilmiah Mahasiswa: NIM+nama mahasiswa, judul artikel, keterangan jurnal, peringkat akreditasi, tanggal terbit, dan tautan artikel.",
            new String[]{"No","NIM dan Nama Mahasiswa","Judul Artikel/Publikasi Ilmiah","Keterangan (Jurnal, Volume, Tahun, Nomor, Halaman)","Peringkat Akreditasi Jurnal","Tanggal Terbit (dd/mm/yyyy)","Tautan Artikel"},
            8, "bar");

        put("DKPS-3.4",
            "Tabel 3.4 Kepuasan Mahasiswa — aspek keandalan, daya tanggap, kepastian, empati, tangible, dan rencana tindak lanjut UPPS/PS.",
            new String[]{"No","Aspek yang Diukur","Sangat Baik (%)","Baik (%)","Cukup (%)","Kurang Baik (%)","Rencana Tindak Lanjut oleh UPPS/PS"},
            5, "bar");

        // Kriteria 4 — Sumber Daya Manusia
        put("DKPS-4.1",
            "Tabel 4.1 Profil DTPS — nama, NIDN/NIDK, NUPTK, pendidikan (Magister/Doktor), bidang keahlian, kesesuaian kompetensi inti, jabatan akademik, sertifikat pendidik, MK yang diampu, dan kesesuaian bidang.",
            new String[]{"No","Nama Dosen","NIDN/NIDK","NUPTK","Pendidikan (Magister)","Pendidikan (Doktor)","Bidang Keahlian","Kesesuaian dengan Kompetensi Inti","Jabatan Akademik","Sertifikat Pendidik Profesional","MK yang Diampu di PS Diakreditasi","Kesesuaian Bidang Keahlian dengan MK","MK yang Diampu di PS Lain"},
            9, "none");

        put("DKPS-4.2",
            "Tabel 4.2 Beban Kerja DTPS — SKS mengajar (PS diakreditasi / PS lain dalam PT / PS lain luar PT), penelitian, PkM, tugas tambahan, jumlah SKS, dan rata-rata per-semester.",
            new String[]{"No","Nama DTPS","SKS Mengajar PS Diakreditasi","SKS Mengajar PS Lain (dalam PT)","SKS Mengajar PS Lain (luar PT)","SKS Penelitian","SKS PkM","SKS Tugas Tambahan","Jumlah (sks)","Rata-rata Per-Semester (sks)"},
            8, "bar");

        put("DKPS-4.3",
            "Tabel 4.3 Rekognisi Kepakaran/Prestasi DTPS — bidang keahlian, rekognisi/prestasi, jenis rekognisi (visiting lecturer, keynote speaker, editor, narasumber, penghargaan), dan tahun.",
            new String[]{"No","Nama DTPS","Bidang Keahlian","Rekognisi/Prestasi","Jenis Rekognisi","Tahun"},
            5, "bar");

        put("DKPS-4.4",
            "Tabel 4.4 Pengembangan Kompetensi DTPS — jenis kegiatan, tempat, waktu pelaksanaan, manfaat kegiatan, dan bukti kegiatan selama 3 tahun terakhir.",
            new String[]{"No","Nama DTPS","Jenis Kegiatan","Tempat","Waktu Pelaksanaan","Manfaat Kegiatan","Bukti Kegiatan"},
            5, "bar");

        put("DKPS-4.5",
            "Tabel 4.5 Tenaga Kependidikan — jenis (pustakawan, laboran/teknisi, administrasi, lainnya), jumlah per pendidikan (S3/S2/S1/D4/D3/SMA), dan unit kerja.",
            new String[]{"No","Jenis Tenaga Kependidikan","S3","S2","S1","D4","D3","SMA/SMK","Unit Kerja"},
            5, "stacked");

        put("DKPS-4.6",
            "Tabel 4.6 Pengembangan Kompetensi Tenaga Kependidikan — jenis pengembangan, tempat, waktu pelaksanaan, manfaat kegiatan, dan bukti kegiatan.",
            new String[]{"No","Nama Tendik","Jenis Pengembangan Kompetensi","Tempat","Waktu Pelaksanaan","Manfaat Kegiatan","Bukti Kegiatan"},
            5, "bar");

        // Kriteria 5 — Keuangan, Sarana, dan Prasarana
        put("DKPS-5.1",
            "Tabel 5.1 Penggunaan Dana — jenis penggunaan dengan rincian UPPS (TS-2/TS-1/TS/Rata-rata) dan PS (TS-2/TS-1/TS/Rata-rata) dalam rupiah.",
            new String[]{"No","Jenis Penggunaan","UPPS TS-2 (Rp)","UPPS TS-1 (Rp)","UPPS TS (Rp)","UPPS Rata-rata (Rp)","PS TS-2 (Rp)","PS TS-1 (Rp)","PS TS (Rp)","PS Rata-rata (Rp)"},
            5, "stacked");

        put("DKPS-5.2",
            "Tabel 5.2 Sarana Laboratorium dan Pembelajaran — nama lab/ruang, nama alat, kualitas, jumlah, kepemilikan (milik/sewa), kondisi (terawat/tidak), dan rata-rata waktu penggunaan.",
            new String[]{"No","Nama Lab/Ruang","Nama Alat/Peraga","Kualitas","Jumlah","Kepemilikan (Milik Sendiri)","Kepemilikan (Sewa)","Kondisi (Terawat)","Kondisi (Tidak Terawat)","Rata-rata Waktu Penggunaan (Jam/Minggu)"},
            10, "bar");

        put("DKPS-5.3",
            "Tabel 5.3 Prasarana Pendidikan — nama prasarana, fungsi, jumlah unit, total luas (m2), kualitas, kepemilikan (milik/sewa), dan kondisi (terawat/tidak).",
            new String[]{"No","Nama Prasarana Pendidikan","Fungsi","Jumlah Unit","Total Luas (m2)","Kualitas","Kepemilikan (Milik Sendiri)","Kepemilikan (Sewa)","Kondisi (Terawat)","Kondisi (Tidak Terawat)"},
            8, "bar");

        put("DKPS-5.4",
            "Tabel 5.4 Infrastruktur/Sistem Informasi (TIK) — nama sistem, deskripsi, jumlah, terintegrasi, mutahir, ketersediaan panduan, kepemilikan, dan kondisi.",
            new String[]{"No","Nama Infrastruktur/Sistem Informasi","Deskripsi","Jumlah","Terintegrasi","Mutahir","Ketersediaan Panduan","Kepemilikan","Kondisi"},
            7, "none");

        // Kriteria 6 — Pendidikan
        put("DKPS-6.1",
            "Tabel 6.1 Kurikulum — semester, kode MK, nama MK, MK kompetensi, bobot kredit (sks), dokumen rencana pembelajaran, asesmen pencapaian CPL, dan unit penyelenggara.",
            new String[]{"No","Semester","Kode MK","Nama MK","MK Kompetensi","Bobot Kredit (sks)","Dokumen Rencana Pembelajaran","Asesmen Pencapaian CPL","Unit Penyelenggara"},
            7, "bar");

        put("DKPS-6.2",
            "Tabel 6.2 Integrasi Penelitian dan PkM dalam Pembelajaran — nama DTPS, judul penelitian/PkM, mata kuliah, bentuk integrasi, dan tahun (TS-2/TS-1/TS).",
            new String[]{"No","Nama DTPS","Judul Penelitian/PkM","Mata Kuliah","Bentuk Integrasi","Tahun TS-2","Tahun TS-1","Tahun TS","Tahun (YYYY)"},
            11, "bar");

        put("DKPS-6.3",
            "Tabel 6.3 Pembimbingan Magang Kependidikan — nama dosen pembimbing, jumlah mhs bimbingan (TS-2/TS-1/TS), jumlah pertemuan (TS-2/TS-1/TS), dan lama pelaksanaan magang (bulan).",
            new String[]{"No","Nama Dosen Pembimbing","Jml Mhs Bimbing TS-2","Jml Mhs Bimbing TS-1","Jml Mhs Bimbing TS","Jml Pertemuan TS-2","Jml Pertemuan TS-1","Jml Pertemuan TS","Lama Magang (bulan)"},
            7, "bar");

        put("DKPS-6.4",
            "Tabel 6.4 Kegiatan Akademik di Luar Kelas — nama dan tema kegiatan, dosen pembimbing, tanggal kegiatan, dan bukti kegiatan.",
            new String[]{"No","Nama dan Tema Kegiatan","Dosen Pembimbing","Tanggal Kegiatan (dd/mm/yyyy)","Bukti Kegiatan"},
            7, "bar");

        put("DKPS-6.5",
            "Tabel 6.5 Pembimbingan Tugas Akhir/Skripsi — nama DTPS, jumlah mhs dibimbing di PS diakreditasi (TS-2/TS-1/TS), di PS lain (TS-2/TS-1/TS), jumlah pertemuan (TS-2/TS-1/TS), dan rata-rata.",
            new String[]{"No","Nama DTPS","Bimbing PS Akreditasi TS-2","Bimbing PS Akreditasi TS-1","Bimbing PS Akreditasi TS","Bimbing PS Lain TS-2","Bimbing PS Lain TS-1","Bimbing PS Lain TS","Pertemuan TS-2","Pertemuan TS-1","Pertemuan TS","Rata-rata Bimbingan"},
            8, "bar");

        put("DKPS-6.6",
            "Tabel 6.6 IPK Lulusan — tahun lulus, jumlah lulusan, dan distribusi IPK (< 2.75, 2.75-3.0, 3.0-3.5, >= 3.5) beserta rata-rata IPK.",
            new String[]{"Tahun Lulus","Jumlah Lulusan","IPK < 2.75","2.75 <= IPK < 3.0","3.0 <= IPK < 3.5","IPK >= 3.5","Rata-rata IPK"},
            7, "line");

        put("DKPS-6.7",
            "Tabel 6.7 Masa Studi Lulusan — tahun masuk, jumlah mhs diterima, jumlah mhs yang lulus pada TS-4/TS-3/TS-2/TS-1/TS, jumlah lulusan s.d. akhir TS, dan rata-rata masa studi.",
            new String[]{"Tahun Masuk","Jumlah Mahasiswa Diterima","Lulus pada TS-4","Lulus pada TS-3","Lulus pada TS-2","Lulus pada TS-1","Lulus pada TS","Jumlah Lulusan s.d. Akhir TS","Rata-rata Masa Studi"},
            7, "line");

        put("DKPS-6.8",
            "Tabel 6.8 Lulusan yang Bekerja dan Studi Lanjut — tahun lulus, jumlah lulusan, terlacak, bekerja sesuai bidang, usaha mandiri, studi lanjut S2, dan mengikuti PPG.",
            new String[]{"Tahun Lulus","Jumlah Lulusan","Jumlah Lulusan yang Terlacak","Bekerja sesuai Bidang","Usaha Mandiri","Studi Lanjut S2","Mengikuti PPG"},
            7, "stacked");

        put("DKPS-6.9",
            "Tabel 6.9 Waktu Tunggu Mendapatkan Pekerjaan — tahun lulus, jumlah lulusan, terlacak, dan distribusi waktu tunggu (WT < 6 Bulan, 6 <= WT <= 12 Bulan, WT > 12 Bulan).",
            new String[]{"Tahun Lulus","Jumlah Lulusan","Jumlah Lulusan yang Terlacak","WT < 6 Bulan","6 <= WT <= 12 Bulan","WT > 12 Bulan"},
            7, "bar");

        put("DKPS-6.10",
            "Tabel 6.10 Kesesuaian Bidang Kerja Lulusan — tahun lulus, jumlah lulusan, terlacak, dan tingkat kesesuaian bidang kerja (Rendah/Sedang/Tinggi).",
            new String[]{"Tahun Lulus","Jumlah Lulusan","Jumlah Lulusan yang Terlacak","Kesesuaian Rendah","Kesesuaian Sedang","Kesesuaian Tinggi"},
            7, "line");

        put("DKPS-6.11",
            "Tabel 6.11 Kepuasan Pengguna Lulusan — jenis kemampuan (etika, keahlian, komunikasi, dll.), persentase tingkat kepuasan (Sangat Baik/Baik/Cukup/Kurang), dan rencana tindak lanjut.",
            new String[]{"No","Jenis Kemampuan","Sangat Baik (%)","Baik (%)","Cukup (%)","Kurang (%)","Rencana Tindak Lanjut oleh UPPS/PS"},
            12, "bar");

        // Kriteria 7 — Penelitian
        put("DKPS-7.1",
            "Tabel 7.1 Penelitian DTPS — jumlah judul penelitian per sumber pembiayaan (PT/Mandiri, Dalam Negeri, Luar Negeri) pada TS-2, TS-1, TS, dan jumlah total.",
            new String[]{"No","Sumber Pembiayaan","Jumlah Judul TS-2","Jumlah Judul TS-1","Jumlah Judul TS","Jumlah"},
            5, "stacked");

        put("DKPS-7.2",
            "Tabel 7.2 Penelitian DTPS yang Melibatkan Mahasiswa — nama DTPS, judul/tema penelitian sesuai roadmap, NIM dan nama mahasiswa, judul kegiatan, dan tahun.",
            new String[]{"No","Nama DTPS","Judul/Tema Penelitian sesuai Roadmap","NIM dan Nama Mahasiswa","Judul Kegiatan","Tahun (YYYY)"},
            7, "bar");

        put("DKPS-7.3",
            "Tabel 7.3 Publikasi Ilmiah DTPS — media publikasi (jurnal nasional tidak terakreditasi, SINTA 1-6, internasional, seminar, media massa), jumlah TS-2/TS-1/TS, dan total.",
            new String[]{"No","Media Publikasi","Jumlah TS-2","Jumlah TS-1","Jumlah TS","Jumlah"},
            5, "stacked");

        put("DKPS-7.4",
            "Tabel 7.4 Publikasi DTPS pada Jurnal Nasional (min. SINTA 2) dan/atau Internasional Bereputasi — nama dosen, judul artikel, nama jurnal, tier/level, tahun, dan URL/DOI.",
            new String[]{"No","Nama DTPS","Judul Artikel","Nama Jurnal","Peringkat (SINTA/Scopus)","Tahun","URL/DOI"},
            5, "bar");

        put("DKPS-7.5",
            "Tabel 7.5 Karya Ilmiah DTPS yang Disitasi — nama DTPS, judul artikel yang disitasi (jurnal, volume, tahun, nomor, halaman), dan jumlah sitasi.",
            new String[]{"No","Nama DTPS","Judul Artikel yang Disitasi (Jurnal/Buku, Volume, Tahun, Nomor, Halaman)","Jumlah Sitasi"},
            7, "bar");

        // Kriteria 8 — Pengabdian kepada Masyarakat (PkM)
        put("DKPS-8.1",
            "Tabel 8.1 PkM DTPS — jumlah judul PkM per sumber pembiayaan (PT/Mandiri, Dalam Negeri, Luar Negeri) pada TS-2, TS-1, TS, dan jumlah total.",
            new String[]{"No","Sumber Pembiayaan","Jumlah Judul TS-2","Jumlah Judul TS-1","Jumlah Judul TS","Jumlah"},
            5, "stacked");

        put("DKPS-8.2",
            "Tabel 8.2 PkM DTPS yang Melibatkan Mahasiswa — nama DTPS, judul/tema PkM sesuai roadmap, NIM dan nama mahasiswa, judul kegiatan, dan tahun.",
            new String[]{"No","Nama DTPS","Judul/Tema PkM sesuai Roadmap","NIM dan Nama Mahasiswa","Judul Kegiatan","Tahun (YYYY)"},
            7, "bar");

        // Laporan DINAMIS (kolom mengikuti hasil SQL, mis. "Rekap Pembayaran Siswa/Calon").
        // Header tabel diambil dari nama kolom data; grafik & analisis dibuat otomatis.
        CONFIGS.put("data_umum", new Config(
            "Rincian data laporan. Kolom mengikuti hasil pencarian; grafik & analisis dibuat otomatis dari data.",
            new String[0], 2, "bar", true));
    }

    private static void put(String code, String desc, String[] headers, int startRow, String chartType) {
        CONFIGS.put(code, new Config(desc, headers, startRow, chartType));
    }

    public static Config getConfig(String sheetCode) {
        Config c = CONFIGS.get(sheetCode);
        // Sheet tak terdaftar diperlakukan DINAMIS: header & grafik diturunkan dari
        // data, bukan placeholder "Kolom 1..5" yang membingungkan.
        return c != null ? c : new Config(
            "Data laporan " + sheetCode + ".",
            new String[0], 2, "bar", true
        );
    }
}
