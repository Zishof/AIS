package ais.ui.util;

/**
 * Registry terpusat panduan penggunaan (Bantuan) untuk setiap panel portal
 * yang dibuat oleh PortalUiHelper. Panduan dideteksi secara otomatis
 * berdasarkan kata kunci pada judul panel; bila tidak cocok, panduan generik
 * berlaku. Setiap panduan minimal 500 kata dalam bahasa Indonesia formal.
 *
 * Dipanggil oleh PortalUiHelper.panel() saat bantuanHtml tidak disediakan.
 * Tidak perlu mengubah satupun Action file — cukup daftarkan di sini.
 *
 * Kompatibel Java 1.7 (tanpa lambda, stream, diamond, try-with-resources).
 */
public final class PanelBantuanRegistry {

    private PanelBantuanRegistry() {
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Kembalikan konten HTML panduan yang sesuai untuk panel dengan judul
     * dan deskripsi yang diberikan. Selalu mengembalikan non-null.
     */
    public static String getBantuan(String judul, String deskripsi) {
        String j = judul == null ? "" : judul.toLowerCase();
        String d = deskripsi == null ? "" : deskripsi;
        String t = judul == null ? "" : judul;

        // Utamakan berkas panduan KHUSUS per-panel di WEB-INF/bantuan/panel_<slug>.html
        // (template SAMA dengan panduan halaman utama, lihat BantuanHelper). Bila berkas
        // tersedia, gunakan isinya; bila tidak ada, jatuh ke panduan berbasis kata kunci
        // di bawah (selalu mengembalikan non-null).
        String dariBerkas = muatBerkasPanel(judul);
        if (dariBerkas != null) {
            return dariBerkas;
        }

        if (isPmb(j))         return bantuanPmb(t, d);
        if (isPenilaian(j))   return bantuanPenilaian(t, d);
        if (isAbsensi(j))     return bantuanAbsensi(t, d);
        if (isKrs(j))         return bantuanKrs(t, d);
        if (isPrestasi(j))    return bantuanPrestasi(t, d);
        if (isPelanggaran(j)) return bantuanPelanggaran(t, d);
        if (isCatatan(j))     return bantuanCatatan(t, d);
        if (isAlumni(j))      return bantuanAlumni(t, d);
        if (isLibrary(j))     return bantuanLibrary(t, d);
        if (isAsset(j))       return bantuanAsset(t, d);
        if (isSpmi(j))        return bantuanSpmi(t, d);
        if (isELearning(j))   return bantuanELearning(t, d);
        if (isPembayaran(j))  return bantuanPembayaran(t, d);
        if (isKegiatan(j))    return bantuanKegiatan(t, d);
        return bantuanGenerik(t, d);
    }

    // =========================================================================
    // BERKAS PANDUAN PER-PANEL (template WEB-INF/bantuan/panel_<slug>.html)
    // =========================================================================

    /**
     * Muat berkas panduan khusus panel berdasarkan judulnya, dari
     * {@code WEB-INF/bantuan/panel_<slug>.html}. Slug dibentuk dari judul panel
     * (huruf kecil, karakter non-alfanumerik diganti garis bawah). Mengembalikan
     * {@code null} bila berkas tidak ada, agar pemanggil memakai panduan bawaan.
     *
     * @param judul judul panel (mis. "Data Mahasiswa &amp; Tagihan")
     * @return isi HTML berkas panduan, atau {@code null} bila tidak tersedia
     */
    private static String muatBerkasPanel(String judul) {
        String slug = slugPanel(judul);
        if (slug.length() == 0) {
            return null;
        }
        return muatBerkas("panel_" + slug);
    }

    /** Bentuk slug nama berkas dari judul: huruf kecil + non-alfanumerik menjadi '_'. */
    private static String slugPanel(String judul) {
        if (judul == null) {
            return "";
        }
        String s = judul.toLowerCase().trim();
        StringBuilder sb = new StringBuilder();
        boolean underscoreTerakhir = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
                underscoreTerakhir = false;
            } else if (sb.length() > 0 && !underscoreTerakhir) {
                sb.append('_');
                underscoreTerakhir = true;
            }
        }
        int akhir = sb.length();
        while (akhir > 0 && sb.charAt(akhir - 1) == '_') {
            akhir--;
        }
        return sb.substring(0, akhir);
    }

    /** Baca berkas {@code WEB-INF/bantuan/<key>.html} (UTF-8); null bila tidak ada. */
    private static String muatBerkas(String key) {
        java.io.FileInputStream in = null;
        try {
            if (key != null && key.trim().length() > 0
                    && org.zkoss.zk.ui.Sessions.getCurrent() != null) {
                String path = org.zkoss.zk.ui.Sessions.getCurrent().getWebApp()
                        .getRealPath("/WEB-INF/bantuan/" + key.trim() + ".html");
                if (path != null) {
                    java.io.File f = new java.io.File(path);
                    if (f.exists() && f.isFile()) {
                        in = new java.io.FileInputStream(f);
                        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            bos.write(buf, 0, n);
                        }
                        return new String(bos.toByteArray(), "UTF-8");
                    }
                }
            }
        } catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/ui/util/PanelBantuanRegistry.java:126");
            // abaikan — kembalikan null agar panduan bawaan dipakai
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/ui/util/PanelBantuanRegistry.java:132");
                }
            }
        }
        return null;
    }

    // =========================================================================
    // DETECTORS
    // =========================================================================

    private static boolean isPmb(String j) {
        return (j.contains("mahasiswa baru") || j.contains("calon mahasiswa")
                || j.contains("gelombang") || j.contains("seleksi calon")
                || j.contains("jenis sekolah") || j.contains("skor seleksi")
                || j.contains("asal sma") || j.contains("info kampus")
                || j.contains("pendapatan orang tua") || j.contains("pendidikan orang tua")
                || j.contains("pekerjaan orang tua"))
            && (j.contains("rekap") || j.contains("calon") || j.contains("program studi")
                || j.contains("jurusan") || j.contains("kota") || j.contains("kecamatan")
                || j.contains("agama") || j.contains("kelulusan") || j.contains("status awal"));
    }

    private static boolean isPenilaian(String j) {
        return j.contains("nilai") || j.contains("penilaian") || j.contains("grade")
            || j.contains("ipk") || j.contains("indeks prestasi");
    }

    private static boolean isAbsensi(String j) {
        return j.contains("absensi") || j.contains("kehadiran") || j.contains("hadir");
    }

    private static boolean isKrs(String j) {
        return j.contains("krs") || j.contains("kartu rencana");
    }

    private static boolean isPrestasi(String j) {
        return j.contains("prestasi") || j.contains("apresiasi");
    }

    private static boolean isKegiatan(String j) {
        return j.contains("kegiatan");
    }

    private static boolean isPelanggaran(String j) {
        return j.contains("pelanggaran") || j.contains("sanksi") || j.contains("hukuman");
    }

    private static boolean isCatatan(String j) {
        return j.contains("catatan");
    }

    private static boolean isAlumni(String j) {
        return j.contains("alumni") || j.contains("lulusan");
    }

    private static boolean isLibrary(String j) {
        return j.contains("perpustakaan") || j.contains("stok item")
            || j.contains("koleksi") || j.contains("monitor stok");
    }

    private static boolean isAsset(String j) {
        return j.contains("aset") || j.contains("inventaris");
    }

    private static boolean isSpmi(String j) {
        return j.contains("spmi") || j.contains("penjaminan mutu");
    }

    private static boolean isELearning(String j) {
        return j.contains("e-learning") || j.contains("elearning") || j.contains("obe")
            || j.contains("tren aktivitas") || j.contains("aktivitas perkuliahan");
    }

    private static boolean isPembayaran(String j) {
        return j.contains("pembayaran") || j.contains("tagihan") || j.contains("tunggakan")
            || j.contains("cicilan") || j.contains("bayar");
    }

    // =========================================================================
    // MODULE HELP CONTENT
    // =========================================================================

    private static String bantuanPmb(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Penerimaan Mahasiswa Baru (PMB)")
            + p("Modul Penerimaan Mahasiswa Baru (PMB) adalah perangkat analitik yang menyajikan "
                + "data pendaftaran dan seleksi calon mahasiswa baru secara menyeluruh. "
                + "Panel <b>" + esc(judul) + "</b> secara khusus menampilkan: "
                + "<em>" + esc(deskripsi) + "</em> "
                + "Informasi ini diolah secara otomatis dari basis data pendaftar sehingga selalu "
                + "mencerminkan kondisi terkini tanpa perlu diperbarui secara manual.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel rekap PMB dirancang untuk mendukung tim penerimaan dan pimpinan institusi "
                + "dalam memantau progres pendaftaran, menganalisis komposisi demografis pendaftar, "
                + "dan mengevaluasi efektivitas strategi promosi serta seleksi. "
                + "Dengan tersedianya data visual berbasis grafik, pengambilan keputusan dapat "
                + "dilakukan lebih cepat dan berbasis fakta (data-driven decision making).")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Jumlah total pendaftar dikelompokkan berdasarkan kategori panel ini (gelombang, jenis sekolah, kota asal, agama, program studi, dan sebagainya)",
                "Persentase distribusi dalam bentuk grafik batang atau diagram lingkaran yang interaktif",
                "Perbandingan antar kategori untuk mengidentifikasi kelompok dominan dan kelompok minoritas",
                "Data calon mahasiswa versus mahasiswa yang diterima (bila tersedia di konfigurasi)",
                "Tren dari tahun ke tahun untuk mendukung perencanaan kapasitas dan target penerimaan"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Navigasikan ke menu Rekap PMB atau Cetak Registrasi pada sistem, kemudian klik tab yang sesuai (misalnya: Rekap Gelombang, Rekap Jenis Sekolah, Rekap Kota, dan seterusnya).",
                "Panel akan memuat data secara otomatis. Tunggu hingga grafik dan angka selesai ditampilkan dari server.",
                "Arahkan kursor (hover) ke atas elemen grafik — batang, irisan, atau titik data — untuk melihat nilai numerik detailnya.",
                "Gunakan tombol <b>Layar Penuh (&#8599;)</b> di pojok kanan atas header panel untuk memperbesar tampilan agar lebih mudah dibaca, terutama saat presentasi kepada pimpinan.",
                "Tekan tombol Pulihkan atau klik di luar area panel untuk kembali ke tampilan normal.",
                "Buka beberapa tab rekap secara bergantian (misalnya Kota, kemudian Jenis Sekolah) untuk mendapatkan gambaran demografis pendaftar dari berbagai dimensi.",
                "Ekspor atau cetak laporan melalui menu Laporan di halaman utama PMB untuk kebutuhan dokumentasi dan rapat evaluasi."
            })
            + h3("Interpretasi Data")
            + p("Batang grafik yang lebih tinggi atau irisan yang lebih besar menunjukkan kelompok "
                + "dengan jumlah pendaftar terbanyak. Persentase di setiap elemen menggambarkan "
                + "proporsi relatif terhadap total pendaftar. Perhatikan tren dari tahun ke tahun — "
                + "peningkatan jumlah pendaftar dari kota atau sekolah tertentu dapat menjadi indikator "
                + "keberhasilan program promosi di wilayah tersebut. Sebaliknya, penurunan di gelombang "
                + "tertentu perlu ditindaklanjuti dengan evaluasi waktu dan strategi pembukaan gelombang.")
            + h3("Fitur Panel Standar")
            + ul(new String[]{
                "<b>Tombol Layar Penuh (&#8599;)</b>: Memperbesar panel hingga memenuhi seluruh area layar untuk presentasi atau analisis mendalam.",
                "<b>Tombol Bantuan (?)</b>: Membuka panduan ini kapan saja Anda membutuhkan penjelasan.",
                "<b>Multi-tab Rekap</b>: Tersedia berbagai dimensi analisis (gelombang, kota, agama, program studi, dll.) yang dapat dibuka secara bergantian."
            })
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Buka rekap per gelombang setiap hari selama masa penerimaan untuk memantau progres pendaftaran secara real-time.",
                "Bandingkan rekap kota dan kecamatan untuk menentukan prioritas kunjungan sekolah atau kegiatan sosialisasi.",
                "Rekap jenis sekolah berguna untuk mengevaluasi apakah program promosi ke SMK, MA, atau pesantren sudah menjangkau target.",
                "Data skor seleksi membantu menentukan passing grade yang realistis berdasarkan sebaran kemampuan pendaftar.",
                "Rekap program studi menunjukkan prodi favorit dan prodi yang perlu didorong lebih agresif dalam promosi.",
                "Gunakan rekap orang tua (pekerjaan, pendidikan, pendapatan) untuk menyusun kebijakan beasiswa yang tepat sasaran.",
                "Pastikan data pendaftar diisi lengkap melalui formulir pendaftaran agar rekap ini akurat dan dapat diandalkan."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Mengapa jumlah pendaftar di rekap berbeda dengan angka yang terlihat di laporan cetak?",
                "Laporan cetak mungkin diambil pada waktu yang berbeda atau menggunakan filter yang berbeda. "
                + "Tampilan ini selalu menampilkan data terkini secara langsung dari basis data.")
            + qa("Apakah saya bisa mengubah data pendaftar langsung dari panel rekap ini?",
                "Tidak. Panel rekap bersifat hanya-baca (read-only). Perubahan data pendaftar hanya dapat dilakukan melalui modul entri data PMB oleh petugas yang berwenang.")
            + qa("Mengapa ada kategori yang nilainya nol atau kosong?",
                "Kemungkinan pendaftar tidak mengisi field tersebut saat mendaftar, atau data belum sinkron. Hubungi admin PMB untuk klarifikasi dan pembersihan data.")
            + qa("Kapan data ini diperbarui?",
                "Data diperbarui setiap kali panel dimuat. Untuk data paling segar, muat ulang halaman atau tutup dan buka kembali tab yang bersangkutan.")
        );
    }

    private static String bantuanPenilaian(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Penilaian Akademik")
            + p("Modul Penilaian Akademik menyajikan data hasil penilaian mahasiswa yang telah "
                + "diinput oleh dosen pengampu mata kuliah ke dalam sistem. Panel <b>" + esc(judul)
                + "</b> secara khusus menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data yang tersaji mencakup nilai mentah, konversi huruf mutu, Indeks Prestasi (IP), "
                + "serta Indeks Prestasi Kumulatif (IPK) yang dihitung secara otomatis oleh sistem.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini dirancang untuk memberikan gambaran menyeluruh tentang capaian akademik "
                + "mahasiswa dari berbagai sudut pandang: distribusi nilai per mata kuliah, "
                + "perbandingan antar angkatan, serta tren IPK dari semester ke semester. "
                + "Informasi ini berguna bagi dosen, koordinator akademik, dan pimpinan program studi "
                + "dalam mengevaluasi kualitas proses pembelajaran dan merancang intervensi akademik.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Distribusi nilai mahasiswa berdasarkan rentang angka atau huruf mutu (A, AB, B, BC, C, D, E)",
                "Nilai rata-rata, nilai tertinggi, dan nilai terendah per mata kuliah atau per semester",
                "Indeks Prestasi Kumulatif (IPK) dan distribusinya antar mahasiswa",
                "Tren perubahan nilai dari semester ke semester atau dari angkatan ke angkatan",
                "Perbandingan capaian nilai antar program studi atau antar kelas paralel",
                "Jumlah mahasiswa yang tidak lulus (nilai E atau di bawah standar) per mata kuliah"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Penilaian pada sistem, kemudian klik tab yang sesuai (Statistik Nilai, Penilaian Mahasiswa, Grade Nilai, Rekap Nilai, Data Nilai, atau IPK).",
                "Panel akan memuat data secara otomatis. Tunggu beberapa saat hingga grafik dan tabel tampil sepenuhnya.",
                "Gunakan filter tahun akademik atau semester (jika tersedia) untuk menyesuaikan rentang data yang ditampilkan.",
                "Arahkan kursor ke elemen grafik untuk melihat nilai detail per titik data.",
                "Gunakan tombol <b>Layar Penuh</b> di pojok kanan atas untuk memperbesar tampilan, terutama pada grafik distribusi yang rinci.",
                "Buka beberapa tab nilai secara bergantian untuk mendapatkan gambaran komprehensif (distribusi, tren, perbandingan angkatan).",
                "Ekspor atau cetak data melalui menu Laporan di halaman utama penilaian untuk kebutuhan rapat evaluasi atau laporan akreditasi."
            })
            + h3("Interpretasi Data")
            + p("Distribusi nilai yang sehat pada umumnya mengikuti pola kurva normal (bell curve): "
                + "sebagian besar mahasiswa mendapat nilai B atau C, dengan lebih sedikit yang mendapat A atau E. "
                + "IPK rata-rata program studi yang meningkat dari tahun ke tahun adalah indikator positif "
                + "kualitas pembelajaran. Mata kuliah dengan persentase nilai E (tidak lulus) yang tinggi "
                + "perlu dievaluasi — apakah standar penilaian terlalu ketat, materi terlalu berat, "
                + "atau ada faktor lain yang memengaruhi performa mahasiswa.")
            + h3("Fitur Panel Standar")
            + ul(new String[]{
                "<b>Layar Penuh</b>: Sangat berguna untuk melihat detail grafik distribusi nilai yang memiliki banyak kategori.",
                "<b>Bantuan</b>: Panduan ini tersedia kapan saja untuk referensi cara membaca dan menganalisis data.",
                "<b>Multi-tab</b>: Tersedia beberapa tab berbeda (Statistik, Data, Grade, IPK, Tren) untuk analisis multi-dimensi."
            })
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Bandingkan distribusi nilai antar mata kuliah untuk mengidentifikasi mana yang memiliki tingkat kesulitan tidak wajar.",
                "Perhatikan mata kuliah dengan persentase nilai E tinggi sebagai prioritas evaluasi silabus dan metode pengajaran.",
                "Data IPK per angkatan berguna untuk program beasiswa prestasi, seleksi asisten dosen, atau rekomendasi akademik.",
                "Grafik tren IPK per tahun angkatan membantu mengidentifikasi apakah ada perubahan kualitas mahasiswa masuk.",
                "Simpan tangkapan layar grafik secara berkala untuk dibandingkan antar semester dalam laporan evaluasi akademik tahunan.",
                "Nilai terendah dari satu mahasiswa saja tidak cukup untuk kesimpulan — selalu perhatikan jumlah mahasiswa pada setiap kategori."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Nilai saya sudah diinput dosen tetapi belum muncul di sini — mengapa?",
                "Mungkin dosen belum menyelesaikan proses finalisasi nilai, atau ada verifikasi yang masih menunggu. Hubungi dosen pengampu atau admin akademik.")
            + qa("Apakah IPK yang ditampilkan di sini sudah final?",
                "IPK dihitung dari semua nilai yang sudah diinput. Bila ada nilai yang masih pending atau dalam proses perbaikan, IPK dapat berubah setelah nilai difinalisasi.")
            + qa("Mengapa ada mahasiswa yang tidak muncul sama sekali dalam rekap nilai?",
                "Mahasiswa yang tidak terdaftar di matakuliah, sedang cuti, atau belum ada nilai yang diinput tidak akan muncul dalam rekap. Periksa status akademiknya.")
            + qa("Siapa yang berwenang mengubah nilai mahasiswa?",
                "Perubahan nilai hanya dapat dilakukan oleh dosen pengampu melalui prosedur resmi, biasanya memerlukan persetujuan ketua program studi atau dekan.")
        );
    }

    private static String bantuanAbsensi(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Absensi dan Kehadiran")
            + p("Modul Absensi dan Kehadiran mencatat dan merekap tingkat kehadiran mahasiswa atau siswa "
                + "pada setiap pertemuan perkuliahan atau pelajaran. Panel <b>" + esc(judul)
                + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data presensi dikumpulkan dari setiap pertemuan yang tercatat dalam sistem, "
                + "baik yang diinput manual oleh dosen maupun melalui sistem absensi digital.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini membantu dosen, wali kelas, dan admin akademik dalam memantau tingkat "
                + "kehadiran secara efisien tanpa harus memeriksa catatan absensi satu per satu. "
                + "Informasi kehadiran yang akurat sangat penting untuk menentukan kelayakan "
                + "mahasiswa mengikuti ujian akhir, memberikan intervensi akademik lebih awal, "
                + "serta memenuhi persyaratan pelaporan akademik kepada BAN-PT dan Dikti.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Persentase kehadiran tiap mahasiswa atau siswa per mata kuliah atau mata pelajaran",
                "Jumlah pertemuan yang dihadiri dibandingkan total pertemuan yang telah terlaksana",
                "Daftar mahasiswa atau siswa dengan kehadiran di bawah batas minimum yang ditetapkan institusi",
                "Rekap kehadiran dikelompokkan berdasarkan kelas, program studi, atau mata kuliah",
                "Tren perubahan kehadiran dari pertemuan ke pertemuan atau dari bulan ke bulan"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Absensi atau halaman Presensi pada sistem, kemudian klik tab rekap yang diinginkan.",
                "Panel memuat data secara otomatis dari catatan presensi yang tersimpan di basis data.",
                "Gunakan filter mata kuliah, kelas, semester, atau periode (jika tersedia) untuk mempersempit tampilan.",
                "Arahkan kursor ke grafik batang atau sel tabel untuk melihat angka kehadiran per mahasiswa secara detail.",
                "Perhatikan mahasiswa atau siswa yang ditandai warna merah atau kuning — ini menunjukkan kehadiran di bawah batas minimum.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas, terutama saat daftar mahasiswa sangat panjang.",
                "Ekspor atau cetak rekap absensi melalui menu Laporan untuk keperluan pengarsipan, rapat dosen, atau pengumuman kepada mahasiswa."
            })
            + h3("Interpretasi Data")
            + p("Persentase kehadiran di bawah 75% pada umumnya menempatkan mahasiswa dalam risiko "
                + "tidak diizinkan mengikuti ujian akhir sesuai peraturan akademik yang berlaku. "
                + "Tren kehadiran yang menurun di tengah semester perlu diinvestigasi lebih lanjut: "
                + "apakah ada jadwal bentrok, motivasi belajar yang menurun, masalah kesehatan, "
                + "atau hambatan lain yang perlu ditangani oleh dosen wali atau konselor akademik. "
                + "Kehadiran yang konsisten tinggi menunjukkan antusiasme dan kedisiplinan mahasiswa.")
            + h3("Fitur Panel Standar")
            + ul(new String[]{
                "<b>Layar Penuh</b>: Sangat berguna saat melihat daftar kehadiran kelas besar dengan banyak mahasiswa.",
                "<b>Bantuan</b>: Panduan ini tersedia kapan saja untuk membantu membaca dan menganalisis data absensi.",
                "<b>Indikator Warna</b>: Merah/kuning untuk kehadiran di bawah batas, hijau untuk aman — membantu identifikasi cepat."
            })
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Pantau rekap absensi setiap dua minggu sekali untuk mengidentifikasi mahasiswa bermasalah lebih awal.",
                "Koordinasikan dengan dosen pembimbing akademik (dosen wali) untuk menindaklanjuti mahasiswa dengan kehadiran rendah.",
                "Data absensi yang akurat dan lengkap sangat penting sebagai dasar validasi nilai akhir.",
                "Gunakan rekap per kelas untuk membandingkan tingkat kehadiran antar kelas paralel pada mata kuliah yang sama.",
                "Bila ada mahasiswa yang tidak muncul dalam daftar, periksa apakah ia sudah terdaftar di mata kuliah tersebut.",
                "Rekap absensi seluruh semester berguna sebagai lampiran laporan kinerja perkuliahan kepada program studi."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Kehadiran saya tampak kurang dari yang seharusnya — mengapa bisa terjadi?",
                "Kemungkinan ada pertemuan yang absensinya belum diinput oleh dosen, atau ada kesalahan input nama. Segera konfirmasi kepada dosen pengampu.")
            + qa("Bagaimana cara menghitung persentase kehadiran?",
                "Persentase dihitung dengan rumus: (jumlah pertemuan yang dihadiri / total pertemuan yang terlaksana) x 100%.")
            + qa("Siapa yang berwenang mengubah data absensi mahasiswa?",
                "Hanya dosen pengampu atau admin akademik yang berwenang mengubah catatan absensi melalui modul Perkuliahan, dengan prosedur dan persetujuan yang sesuai.")
            + qa("Apakah izin sakit dihitung sebagai hadir?",
                "Bergantung kebijakan institusi. Beberapa institusi menghitung izin sakit sebagai hadir dengan alasan valid, beberapa tidak. Cek aturan akademik yang berlaku.")
        );
    }

    private static String bantuanKrs(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Kartu Rencana Studi (KRS)")
            + p("Kartu Rencana Studi (KRS) adalah dokumen formal yang berisi daftar mata kuliah "
                + "yang akan diambil oleh mahasiswa pada semester yang sedang berjalan. "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Proses KRS melibatkan tiga tahap utama: pengisian oleh mahasiswa, "
                + "verifikasi beban SKS, dan persetujuan oleh dosen wali akademik.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini dirancang untuk membantu admin akademik, dosen wali, dan koordinator "
                + "program studi dalam memantau status pengisian dan persetujuan KRS secara real-time. "
                + "Pemantauan yang cermat memastikan semua mahasiswa mengisi KRS sebelum batas waktu "
                + "dan tidak ada yang terlewat sehingga mereka dapat mengikuti perkuliahan semester berjalan.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Status pengisian KRS mahasiswa: sudah mengisi, belum mengisi, atau sedang dalam proses",
                "Jumlah SKS yang diambil oleh tiap mahasiswa dan perbandingannya dengan batas maksimum yang diizinkan",
                "Status persetujuan KRS oleh dosen wali: menunggu persetujuan, sudah disetujui, atau ditolak",
                "Distribusi pengambilan mata kuliah per program studi, angkatan, dan kelas",
                "Tren harian pengisian KRS selama masa KRS berlangsung",
                "Daftar mahasiswa yang mendekati atau melebihi batas SKS yang diizinkan berdasarkan IPK"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Monitor KRS pada sistem, kemudian klik tab yang sesuai (Status KRS, Rekap Pengambilan, Statistik, Batas SKS, Persetujuan, dan lainnya).",
                "Panel memuat data secara otomatis dari basis data KRS semester yang aktif.",
                "Perhatikan angka mahasiswa yang sudah vs belum mengisi KRS untuk memantau progres pengisian.",
                "Identifikasi mahasiswa yang melewati batas SKS atau yang KRS-nya ditolak untuk ditindaklanjuti.",
                "Gunakan filter angkatan, program studi, atau status persetujuan untuk mempersempit tampilan.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas pada daftar mahasiswa yang panjang.",
                "Ekspor data atau cetak laporan untuk kebutuhan administrasi dan rapat koordinasi."
            })
            + h3("Interpretasi Data")
            + p("Persentase pengisian KRS yang rendah menjelang batas akhir adalah sinyal darurat "
                + "yang memerlukan tindakan segera — kirimkan pengumuman atau hubungi langsung mahasiswa "
                + "yang belum mengisi. Mahasiswa dengan jumlah SKS melebihi batas yang diizinkan "
                + "perlu berkonsultasi dengan dosen wali sebelum KRS-nya dapat disetujui. "
                + "Batas SKS ditentukan berdasarkan IPK semester sebelumnya sesuai peraturan akademik: "
                + "IPK tinggi memungkinkan pengambilan SKS lebih banyak.")
            + h3("Fitur Panel Standar")
            + ul(new String[]{
                "<b>Layar Penuh</b>: Gunakan untuk melihat tabel mahasiswa dengan lebih nyaman saat daftarnya panjang.",
                "<b>Bantuan</b>: Panduan ini tersedia kapan saja sebagai referensi.",
                "<b>Filter Multi-dimensi</b>: Filter berdasarkan angkatan, program studi, atau status untuk analisis spesifik."
            })
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Pantau progres KRS setiap hari selama masa pengisian KRS berlangsung.",
                "Kirimkan pengumuman ke mahasiswa yang belum mengisi KRS tiga hari sebelum batas waktu.",
                "Koordinasikan dengan dosen wali untuk mempercepat proses persetujuan KRS.",
                "Mahasiswa dengan SKS mendekati batas maksimum perlu mendapat saran akademik sebelum KRS difinalisasi.",
                "Data KRS yang final digunakan sebagai dasar pembebanan biaya semester dan penjadwalan kelas.",
                "Rekap statistik KRS berguna untuk merencanakan kapasitas kelas pada semester berikutnya."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("KRS sudah diisi tetapi masih terlihat belum disetujui — apa yang harus dilakukan?",
                "Dosen wali Anda belum melakukan persetujuan. Segera hubungi dosen wali untuk meminta persetujuan sebelum batas waktu KRS berakhir.")
            + qa("Apakah KRS bisa diubah setelah disetujui oleh dosen wali?",
                "Perubahan KRS setelah disetujui harus melalui prosedur revisi KRS yang ditetapkan institusi dan biasanya memerlukan persetujuan ulang. Hubungi admin akademik.")
            + qa("Mengapa batas SKS saya berbeda dari teman satu angkatan?",
                "Batas SKS ditentukan berdasarkan IPK semester sebelumnya sesuai ketentuan akademik. Mahasiswa dengan IPK lebih tinggi berhak mengambil lebih banyak SKS.")
            + qa("Apa yang terjadi jika melewati batas waktu KRS?",
                "Mahasiswa yang tidak mengisi KRS sampai batas waktu biasanya tidak dapat mengikuti perkuliahan semester berjalan. Segera hubungi admin akademik bila melewati batas waktu.")
        );
    }

    private static String bantuanPrestasi(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Prestasi dan Apresiasi")
            + p("Modul Prestasi dan Apresiasi mencatat dan merekap semua pencapaian, penghargaan, "
                + "dan apresiasi yang diraih oleh sivitas akademika — mahasiswa, dosen, pegawai, "
                + "maupun siswa — dalam berbagai kompetisi, lomba, dan penghargaan resmi. "
                + "Panel <b>" + esc(judul) + "</b> secara khusus menampilkan: "
                + "<em>" + esc(deskripsi) + "</em>")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel rekap prestasi dirancang untuk memudahkan pimpinan dan tim pembinaan "
                + "dalam memantau capaian institusi, mengidentifikasi area keunggulan, "
                + "dan merencanakan alokasi sumber daya pembinaan yang lebih tepat sasaran. "
                + "Data prestasi yang terorganisir juga sangat penting untuk keperluan "
                + "pelaporan akreditasi, penyusunan profil institusi, dan bahan promosi kepada "
                + "calon mahasiswa atau mitra strategis.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Sebaran prestasi berdasarkan cabang ilmu, bidang kompetisi, atau bidang keahlian",
                "Distribusi prestasi berdasarkan kategori kompetisi (akademik, seni, olahraga, teknologi, penelitian, dan lain-lain)",
                "Jumlah prestasi per tingkat: lokal, regional, nasional, dan internasional",
                "Peringkat yang dicapai (Juara 1, 2, 3, Harapan, Finalis) dan nama penyelenggara kompetisi",
                "Tren perolehan prestasi dari tahun ke tahun untuk melihat perkembangan kinerja"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Prestasi (Mahasiswa / Dosen / Pegawai / Siswa) pada sistem.",
                "Klik tab Rekap Cabang untuk melihat distribusi berdasarkan bidang, atau tab Rekap Kategori untuk distribusi berdasarkan jenis kompetisi.",
                "Panel akan memuat visualisasi secara otomatis. Tunggu hingga grafik tampil sepenuhnya.",
                "Arahkan kursor ke elemen grafik untuk melihat jumlah dan persentase detail per cabang atau kategori.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan grafik yang lebih besar, terutama saat dipresentasikan.",
                "Bandingkan antara tab Rekap Cabang dan Rekap Kategori untuk mendapatkan gambaran multi-dimensi.",
                "Ekspor atau cetak rekap untuk kebutuhan laporan tahunan, pengisian borang akreditasi, atau bahan rapat pimpinan."
            })
            + h3("Interpretasi Data")
            + p("Cabang atau kategori dengan jumlah prestasi terbanyak menunjukkan keunggulan "
                + "kompetitif institusi di bidang tersebut — ini dapat menjadi dasar penetapan "
                + "program unggulan (center of excellence) dan alokasi anggaran pembinaan. "
                + "Sebaliknya, bidang dengan prestasi rendah adalah peluang pengembangan yang "
                + "belum dioptimalkan. Prestasi internasional memiliki bobot sangat tinggi "
                + "dalam penilaian akreditasi dan peringkat institusi.")
            + h3("Fitur Panel Standar")
            + ul(new String[]{
                "<b>Layar Penuh</b>: Tampilan diperbesar sangat berguna untuk presentasi kepada pimpinan atau rapat dewan.",
                "<b>Bantuan</b>: Panduan ini membantu membaca dan menganalisis data prestasi secara tepat.",
                "<b>Tab Rekap Cabang & Kategori</b>: Dua dimensi analisis berbeda untuk gambaran menyeluruh."
            })
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Rekap prestasi sangat diperlukan untuk pengisian borang akreditasi BAN-PT — pastikan data selalu lengkap dan terkini.",
                "Identifikasi pembina atau pelatih yang paling produktif dalam menghasilkan prestasi untuk pemberian apresiasi.",
                "Data prestasi internasional menjadi nilai tambah sangat tinggi dalam berbagai penilaian dan peringkat institusi.",
                "Pastikan setiap prestasi diinput segera setelah diraih beserta dokumen pendukung (sertifikat, piala, foto).",
                "Koordinasikan dengan bagian kemahasiswaan, kepegawaian, atau kesiswaan untuk kelengkapan data."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Prestasi yang sudah diraih tidak muncul di rekap — mengapa?",
                "Kemungkinan belum diinput ke sistem atau masih dalam proses verifikasi. Hubungi admin bagian kemahasiswaan atau kepegawaian yang bertanggung jawab.")
            + qa("Apa perbedaan antara Rekap Cabang dan Rekap Kategori?",
                "Rekap Cabang mengelompokkan prestasi berdasarkan bidang ilmu atau olahraga (contoh: Matematika, Badminton). Rekap Kategori mengelompokkan berdasarkan jenis penghargaan (contoh: Olimpiade, Lomba Karya Ilmiah, Penghargaan Pemerintah).")
            + qa("Apakah data prestasi ini digunakan untuk akreditasi?",
                "Ya, data prestasi mahasiswa, dosen, dan pegawai adalah komponen penting dalam penilaian akreditasi BAN-PT. Pastikan kelengkapan dan keakuratannya selalu terjaga.")
        );
    }

    private static String bantuanKegiatan(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Kegiatan")
            + p("Modul Kegiatan mencatat semua aktivitas yang diikuti oleh sivitas akademika, "
                + "mencakup kegiatan kemahasiswaan (organisasi dan unit kegiatan mahasiswa), "
                + "kegiatan kedosenan (tridharma perguruan tinggi: pengajaran, penelitian, pengabdian), "
                + "serta kegiatan kesiswaan (ekstrakurikuler dan kegiatan sekolah). "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em>")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel rekap kegiatan memberikan gambaran menyeluruh tentang tingkat keterlibatan "
                + "sivitas dalam berbagai aktivitas institusional. Informasi ini berguna untuk "
                + "evaluasi kinerja, pelaporan BKD (Beban Kerja Dosen), laporan tahunan, "
                + "serta pengisian dokumen akreditasi yang memerlukan bukti kegiatan tridharma "
                + "dan kemahasiswaan yang aktif.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Sebaran kegiatan berdasarkan jabatan peserta (ketua, anggota, pembina, dan lainnya)",
                "Distribusi berdasarkan skala kegiatan: tingkat sekolah/lokal, kota/regional, nasional, dan internasional",
                "Rekap per kelompok atau bidang kegiatan (misalnya: pengajaran, penelitian, pengabdian untuk dosen)",
                "Rincian per sub-kelompok untuk analisis mendalam tentang jenis aktivitas spesifik",
                "Jumlah total kegiatan per periode dan tren dari waktu ke waktu"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Kegiatan (Kemahasiswaan / Kedosenan / Kesiswaan) pada sistem.",
                "Klik tab yang sesuai: Rekap per Jabatan, Rekap per Skala, Rekap per Kelompok, atau Rekap Detail Kelompok.",
                "Panel memuat visualisasi secara otomatis dari basis data kegiatan yang telah diinput.",
                "Arahkan kursor ke grafik untuk melihat angka detail per kategori.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas.",
                "Bandingkan beberapa tab untuk mendapatkan analisis multi-dimensi dari data kegiatan.",
                "Ekspor rekap untuk kebutuhan portofolio dosen, laporan BKD, laporan kemahasiswaan, atau borang akreditasi."
            })
            + h3("Interpretasi Data")
            + p("Kegiatan berskala internasional memiliki bobot tertinggi dalam penilaian kinerja dosen "
                + "dan akreditasi. Distribusi yang merata antara kegiatan pengajaran, penelitian, "
                + "dan pengabdian menunjukkan terpenuhinya kewajiban tridharma secara seimbang. "
                + "Konsentrasi kegiatan hanya pada satu bidang (misalnya hanya pengajaran tanpa penelitian) "
                + "dapat menjadi catatan dalam evaluasi kinerja. Untuk kegiatan kemahasiswaan, "
                + "distribusi yang beragam antar bidang menunjukkan ekosistem kampus yang aktif.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Gunakan rekap per jabatan untuk memetakan keterlibatan tiap level kepemimpinan dalam kegiatan.",
                "Rekap per skala sangat berguna untuk membuktikan kegiatan internasional dalam penilaian akreditasi.",
                "Pastikan setiap kegiatan diinput segera setelah selesai lengkap dengan dokumen pendukung.",
                "Laporan kegiatan bulanan atau semesteran dapat dibuat dari panel ini tanpa merekap ulang secara manual.",
                "Koordinasikan dengan bagian kemahasiswaan atau SDM untuk memastikan kelengkapan dan keakuratan data."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Kegiatan yang sudah diikuti tidak muncul di rekap — mengapa?",
                "Kemungkinan belum diinput ke sistem atau masih menunggu verifikasi oleh admin. Hubungi penanggung jawab modul kegiatan.")
            + qa("Apa perbedaan rekap per jabatan dan rekap per skala?",
                "Per Jabatan mengelompokkan berdasarkan peran peserta dalam kegiatan (ketua panitia, pembicara, peserta). Per Skala mengelompokkan berdasarkan cakupan geografis (lokal, nasional, internasional).")
            + qa("Apakah data kegiatan dosen digunakan untuk BKD?",
                "Ya, untuk modul Kegiatan Kedosenan. Data kegiatan tridharma yang terinput dengan lengkap menjadi dasar laporan BKD. Pastikan semua kegiatan terdokumentasi.")
        );
    }

    private static String bantuanPelanggaran(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Pelanggaran dan Kedisiplinan")
            + p("Modul Pelanggaran dan Kedisiplinan mencatat setiap insiden pelanggaran tata tertib "
                + "yang terjadi di lingkungan institusi, beserta sanksi atau hukuman yang diberikan. "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data ini dikelola secara ketat dengan akses yang dibatasi sesuai kewenangan "
                + "masing-masing pengguna untuk menjaga kerahasiaan dan integritas informasi.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini membantu tim Bimbingan Konseling (BK), bagian SDM/kepegawaian, "
                + "dan pimpinan dalam memantau tren pelanggaran, mengukur efektivitas program "
                + "pembinaan, dan merancang kebijakan pencegahan yang lebih tepat sasaran. "
                + "Rekap berbasis data memungkinkan identifikasi pola pelanggaran yang berulang "
                + "sehingga intervensi dapat dilakukan lebih dini dan efektif.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Jumlah total pelanggaran yang tercatat beserta distribusi per jenis pelanggaran",
                "Tren pelanggaran dari bulan ke bulan atau dari semester ke semester",
                "Distribusi sanksi yang diberikan: teguran lisan, teguran tertulis, skorsing, hingga pemutusan studi atau hubungan kerja",
                "Perbandingan insiden antar program studi, kelas, atau departemen",
                "Status penyelesaian kasus: dalam proses penanganan, sudah diselesaikan, atau diarsipkan"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Pelanggaran pada sistem dan klik tab Dasbor.",
                "Panel memuat statistik pelanggaran secara otomatis. Tunggu hingga grafik dan angka tampil.",
                "Gunakan grafik batang atau diagram untuk memahami distribusi dan tren pelanggaran.",
                "Arahkan kursor ke elemen grafik untuk melihat jumlah detail per kategori atau periode.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas saat presentasi ke dewan kehormatan.",
                "Ekspor rekap untuk kebutuhan laporan evaluasi kedisiplinan atau rapat dewan kehormatan."
            })
            + h3("Interpretasi Data")
            + p("Tren pelanggaran yang meningkat pada periode tertentu (misalnya menjelang ujian atau "
                + "pergantian semester) perlu diinvestigasi lebih lanjut penyebabnya. "
                + "Jenis pelanggaran yang paling sering muncul menjadi prioritas untuk program "
                + "pencegahan dan pembinaan berkelanjutan. "
                + "Efektivitas sanksi dapat dievaluasi dari ada-tidaknya pengulangan pelanggaran "
                + "oleh individu yang sama — bila tinggi, perlu pendekatan pembinaan yang berbeda.")
            + h3("Fitur Panel Standar")
            + ul(new String[]{
                "<b>Layar Penuh</b>: Sangat berguna untuk presentasi dalam sidang atau rapat dewan kehormatan.",
                "<b>Bantuan</b>: Panduan ini membantu membaca dan memahami data pelanggaran.",
                "<b>Kontrol Akses</b>: Data ini hanya dapat diakses oleh pengguna dengan kewenangan yang sesuai."
            })
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Gunakan rekap ini sebagai bahan evaluasi kebijakan tata tertib secara berkala — minimal satu kali per semester.",
                "Identifikasi pola pelanggaran berulang untuk merancang program pembinaan yang lebih tepat sasaran.",
                "Rekap pelanggaran bulanan berguna sebagai bahan laporan kepada pimpinan dan dewan.",
                "Pastikan setiap kasus tercatat lengkap: jenis pelanggaran, tanggal kejadian, sanksi yang diberikan, dan status penyelesaian.",
                "Jaga kerahasiaan data sesuai dengan aturan perlindungan data pribadi dan etika institusi yang berlaku."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Siapa yang berwenang mengakses data pelanggaran ini?",
                "Akses dibatasi sesuai peran pengguna. Umumnya hanya admin BK atau SDM, kepala sekolah atau pimpinan program studi, dan pejabat yang ditunjuk secara resmi.")
            + qa("Apakah siswa atau mahasiswa bisa melihat data pelanggaran mereka sendiri?",
                "Bergantung konfigurasi sistem. Secara umum, individu hanya dapat melihat riwayat pelanggaran mereka sendiri, bukan data orang lain.")
            + qa("Data pelanggaran lama tidak muncul — bagaimana mengaksesnya?",
                "Data lama kemungkinan telah diarsipkan. Gunakan filter periode yang lebih luas atau hubungi admin sistem untuk mengakses data arsip.")
        );
    }

    private static String bantuanCatatan(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Catatan dan Evaluasi Perkembangan")
            + p("Modul Catatan menyediakan fasilitas pencatatan dan pemantauan perkembangan "
                + "akademik, sikap, dan kegiatan dari mahasiswa, siswa, guru, maupun pegawai. "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Catatan yang terdokumentasi dengan baik menjadi rekam jejak yang valid "
                + "dan dapat digunakan sebagai dasar evaluasi, intervensi, dan pelaporan resmi.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel dasbor catatan memberikan gambaran kuantitatif tentang aktivitas pencatatan "
                + "yang telah dilakukan oleh pendidik atau pembina. Ini membantu memastikan bahwa "
                + "proses pembinaan berjalan aktif dan terdokumentasi — bukan hanya dilakukan secara "
                + "lisan tanpa jejak. Rekap catatan juga berguna sebagai bahan dalam rapat evaluasi "
                + "berkala atau penyusunan laporan perkembangan individu.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Jumlah catatan yang telah dibuat dalam periode tertentu",
                "Distribusi catatan berdasarkan kategori (akademik, sikap, kegiatan, kesehatan, lain-lain)",
                "Tren penambahan catatan dari waktu ke waktu — bulan ke bulan atau semester ke semester",
                "Rekap catatan per individu, kelas, atau program studi",
                "Catatan yang memerlukan tindak lanjut khusus (jika ada penandaan prioritas di sistem)"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Catatan yang sesuai (Catatan Mahasiswa, Catatan Siswa, Catatan Guru, atau Catatan Pegawai).",
                "Klik tab Dasbor untuk melihat visualisasi statistik catatan secara ringkas.",
                "Panel memuat data catatan secara otomatis dari basis data.",
                "Gunakan filter periode atau kategori (jika tersedia) untuk mempersempit analisis.",
                "Arahkan kursor ke grafik untuk melihat angka detail.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan lebih luas.",
                "Ekspor ringkasan catatan untuk kebutuhan laporan perkembangan berkala."
            })
            + h3("Interpretasi Data")
            + p("Volume catatan yang tinggi menunjukkan aktivitas pembinaan yang aktif dan terdokumentasi "
                + "dengan baik. Distribusi catatan per kategori membantu mengidentifikasi area yang "
                + "paling banyak mendapat perhatian. Penurunan volume catatan dari bulan ke bulan "
                + "dapat mengindikasikan berkurangnya intensitas interaksi antara pembina dan peserta didik "
                + "atau staf — ini adalah sinyal yang perlu dievaluasi.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Biasakan mencatat setiap interaksi bermakna segera setelah terjadi agar tidak terlupakan.",
                "Gunakan rekap catatan sebagai bahan diskusi dalam rapat evaluasi berkala.",
                "Catatan yang terdokumentasi dengan baik menjadi bukti sah proses pembinaan.",
                "Pastikan privasi catatan terjaga sesuai aturan — tidak semua pihak perlu melihat catatan sensitif.",
                "Rekap catatan tahunan berguna sebagai lampiran laporan kinerja pembinaan akademik."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Catatan yang sudah dibuat tidak tampil di dasbor — mengapa?",
                "Periksa filter periode yang aktif — mungkin catatan tersebut berada di luar rentang waktu yang ditampilkan. Perluas filter atau periksa apakah catatan sudah tersimpan.")
            + qa("Siapa yang bisa membaca catatan yang saya buat?",
                "Bergantung kebijakan privasi institusi. Umumnya catatan hanya dapat dibaca oleh pembuatnya dan pimpinan atau koordinator yang berwenang.")
        );
    }

    private static String bantuanAlumni(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Data Alumni")
            + p("Modul Data Alumni menyajikan informasi tentang mahasiswa yang telah menyelesaikan "
                + "studi dan dinyatakan lulus. Panel <b>" + esc(judul) + "</b> menampilkan: "
                + "<em>" + esc(deskripsi) + "</em> "
                + "Data alumni yang terkelola dengan baik sangat penting untuk pelaporan akreditasi, "
                + "pengembangan jaringan alumni, program tracer study, dan evaluasi kualitas lulusan.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini dirancang untuk membantu pengelola alumni, bagian akademik, dan pimpinan "
                + "institusi dalam memantau perkembangan jumlah lulusan, distribusi per program studi, "
                + "tren capaian IPK, dan data pasca-kelulusan yang diperoleh melalui survei tracer study. "
                + "Informasi ini menjadi dasar evaluasi efektivitas program pendidikan.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Jumlah alumni per program studi dan per tahun kelulusan",
                "Distribusi IPK kelulusan dan masa studi rata-rata",
                "Tren jumlah wisudawan dari tahun ke tahun",
                "Data survei tracer study: bidang kerja, waktu tunggu kerja pertama, dan kepuasan lulusan",
                "Rekap pengisian survei alumni: jumlah yang sudah merespons vs total alumni"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Data Alumni atau menu Maintenance Alumni pada sistem.",
                "Klik tab Rekap atau Grafik untuk melihat visualisasi distribusi alumni.",
                "Panel memuat data dari basis data kelulusan secara otomatis.",
                "Gunakan grafik untuk memahami distribusi alumni antar program studi dan angkatan.",
                "Klik tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas dan detail.",
                "Ekspor data rekap untuk keperluan laporan akreditasi atau evaluasi institusi."
            })
            + h3("Interpretasi Data")
            + p("Tren jumlah lulusan yang konsisten atau meningkat menunjukkan stabilitas dan kapasitas "
                + "proses pendidikan. IPK rata-rata kelulusan yang meningkat dari tahun ke tahun "
                + "adalah indikator positif peningkatan mutu akademik. Masa studi rata-rata yang sesuai "
                + "standar (4 tahun untuk S1) mencerminkan efisiensi pengelolaan akademik. "
                + "Data tracer study dengan respons rate tinggi memberikan gambaran akurat "
                + "tentang relevansi kurikulum dengan kebutuhan industri.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Data alumni sangat krusial untuk akreditasi — pastikan selalu diperbarui dan akurat.",
                "Hubungi alumni yang belum mengisi tracer study secara aktif untuk meningkatkan tingkat respons.",
                "Gunakan rekap alumni sebagai bahan presentasi kepada calon mahasiswa baru tentang prospek lulusan.",
                "Bandingkan tren antar program studi untuk evaluasi daya saing dan kualitas setiap program.",
                "Data masa studi rata-rata berguna untuk mengevaluasi efektivitas sistem perwalian akademik."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Mengapa jumlah alumni di rekap berbeda dari data wisuda?",
                "Data alumni mencakup semua yang sudah dinyatakan lulus, sementara data wisuda hanya mencakup yang mengikuti upacara wisuda resmi. Keduanya bisa berbeda.")
            + qa("Apakah data tracer study tersedia di panel ini?",
                "Ya, bila modul tracer study aktif. Gunakan tab survei atau rekap alumni yang sesuai untuk melihat data jawaban survei.")
            + qa("Bagaimana cara menghubungi alumni untuk mengisi survei tracer study?",
                "Sistem dapat mengirim notifikasi atau email ke alumni terdaftar. Koordinasikan dengan admin IT untuk pengaturan pengiriman survei massal.")
        );
    }

    private static String bantuanLibrary(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Perpustakaan")
            + p("Modul Perpustakaan menyajikan statistik pengelolaan koleksi, monitoring stok, "
                + "dan aktivitas peminjaman perpustakaan institusi secara real-time. "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data ini membantu pustakawan dan pimpinan memantau kondisi koleksi "
                + "tanpa harus melakukan pencatatan manual yang memakan waktu.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Jumlah total koleksi dan stok yang tersedia per jenis item (buku, jurnal, skripsi, dll.) atau per tipe",
                "Statistik peminjaman aktif dan pengembalian dalam periode berjalan",
                "Koleksi yang paling sering dipinjam sebagai indikator permintaan tertinggi",
                "Status stok per item: tersedia, sedang dipinjam, dalam perbaikan, atau hilang",
                "Tren aktivitas perpustakaan (kunjungan, peminjaman) per periode"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Perpustakaan atau Monitor Stok Item pada sistem.",
                "Panel memuat data stok dan aktivitas perpustakaan secara otomatis.",
                "Gunakan grafik untuk memahami distribusi koleksi per jenis atau tipe item.",
                "Identifikasi jenis koleksi yang stoknya menipis sebagai sinyal untuk pengadaan baru.",
                "Arahkan kursor ke grafik untuk melihat angka detail per kategori.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas.",
                "Ekspor atau cetak laporan stok untuk kebutuhan laporan pengadaan dan inventarisasi."
            })
            + h3("Interpretasi Data")
            + p("Stok mendekati nol pada jenis koleksi tertentu adalah sinyal prioritas untuk "
                + "segera mengajukan pengadaan. Koleksi dengan frekuensi peminjaman sangat tinggi "
                + "perlu diperbanyak jumlahnya agar ketersediaan terjaga. Sebaliknya, koleksi yang "
                + "tidak pernah dipinjam dalam periode panjang dapat dipertimbangkan untuk disortir "
                + "atau dipindahkan ke tempat penyimpanan arsip.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Pantau stok koleksi setiap awal semester untuk perencanaan pengadaan yang tepat waktu.",
                "Gunakan rekap jenis peminjaman terbanyak sebagai dasar kebijakan pengadaan berbasis kebutuhan nyata.",
                "Koordinasikan dengan bagian keuangan untuk penganggaran pengadaan berdasarkan analisis data ini.",
                "Informasikan kepada civitas akademika jika koleksi yang sering dicari sedang tidak tersedia.",
                "Lakukan stock opname minimal sekali setahun dan bandingkan hasilnya dengan data sistem."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Stok koleksi di sistem tidak sesuai dengan inventaris fisik — apa yang harus dilakukan?",
                "Kemungkinan ada transaksi yang belum diinput atau item yang hilang/rusak belum dilaporkan. Lakukan stock opname dan sinkronisasi data dengan admin perpustakaan.")
            + qa("Apakah koleksi yang sedang dipinjam bisa dipesan (reserved) sebelumnya?",
                "Bergantung fitur yang diaktifkan di sistem. Tanyakan kepada petugas perpustakaan mengenai prosedur reservasi koleksi.")
        );
    }

    private static String bantuanAsset(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Aset dan Inventaris Institusi")
            + p("Modul Aset dan Inventaris menyajikan gambaran menyeluruh mengenai aset tetap "
                + "dan inventaris bergerak milik institusi yang tercatat dalam sistem. "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data ini bersumber dari pencatatan aset yang dilakukan oleh unit pengelola "
                + "aset atau bagian sarana dan prasarana.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Jumlah total aset per kategori (tanah, bangunan, kendaraan, peralatan, furnitur, dan lain-lain)",
                "Nilai aset dan distribusinya per unit kerja, gedung, atau lokasi",
                "Status kondisi aset: baik, perlu perawatan, rusak ringan, rusak berat, atau sudah dihapus",
                "Tren penambahan dan penghapusan aset dari tahun ke tahun",
                "Aset yang mendekati akhir umur ekonomis dan perlu direncanakan penggantinya"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Aset atau Inventaris pada sistem.",
                "Panel dasbor memuat data aset secara otomatis dari basis data inventaris.",
                "Gunakan grafik untuk memahami distribusi jenis aset dan kondisinya.",
                "Identifikasi aset dengan kondisi kurang baik untuk perencanaan perawatan atau penggantian.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan yang lebih luas.",
                "Ekspor rekap aset untuk kebutuhan laporan keuangan, audit, atau evaluasi sarana-prasarana."
            })
            + h3("Interpretasi Data")
            + p("Proporsi aset dalam kondisi kurang baik atau rusak yang tinggi mengindikasikan "
                + "perlunya peningkatan anggaran perawatan atau percepatan program penggantian aset. "
                + "Nilai total aset yang besar mencerminkan kapasitas infrastruktur dan investasi institusi. "
                + "Tren pertumbuhan aset yang konsisten menunjukkan perkembangan institusi yang sehat.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Lakukan verifikasi fisik aset (stock opname) minimal satu kali per tahun dan bandingkan hasilnya dengan data sistem.",
                "Gunakan rekap ini sebagai bahan penyusunan laporan neraca keuangan tahunan.",
                "Rencanakan pengadaan aset baru berdasarkan analisis kebutuhan dari data gap aset saat ini.",
                "Pastikan setiap aset memiliki kode inventaris unik dan labelnya terpasang dengan jelas."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Aset yang baru dibeli belum muncul dalam rekap — apa penyebabnya?",
                "Kemungkinan aset tersebut belum dicatat ke dalam sistem. Hubungi penanggung jawab aset untuk segera menginput data aset baru beserta nomor inventarisnya.")
            + qa("Apakah nilai aset yang ditampilkan sudah memperhitungkan penyusutan (depresiasi)?",
                "Bergantung konfigurasi akuntansi dalam sistem. Hubungi bagian keuangan untuk memastikan metode penilaian aset yang diterapkan (nilai perolehan atau nilai buku setelah penyusutan).")
        );
    }

    private static String bantuanSpmi(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul SPMI (Sistem Penjaminan Mutu Internal)")
            + p("Sistem Penjaminan Mutu Internal (SPMI) adalah mekanisme institusi dalam menetapkan, "
                + "melaksanakan, mengevaluasi, mengendalikan, dan meningkatkan standar mutu "
                + "pendidikan secara berkelanjutan (siklus PPEPP). Panel <b>" + esc(judul)
                + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Informasi ini sangat penting sebagai bukti nyata pelaksanaan penjaminan mutu "
                + "yang menjadi komponen krusial dalam penilaian akreditasi BAN-PT.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel Dasbor SPMI memberikan ringkasan visual tentang capaian indikator mutu, "
                + "hasil temuan audit mutu internal (AMI), dan status tindak lanjut atas setiap "
                + "temuan yang masih terbuka. Ini memudahkan Lembaga Penjaminan Mutu (LPM), "
                + "Gugus Penjaminan Mutu (GPM) di tingkat program studi, dan pimpinan institusi "
                + "dalam memantau implementasi SPMI secara komprehensif.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Capaian indikator mutu per standar (Standar Pendidikan, Penelitian, Pengabdian, dan standar lainnya)",
                "Hasil temuan Audit Mutu Internal (AMI) dan status tindak lanjutnya (sudah ditindaklanjuti/belum)",
                "Persentase pemenuhan standar mutu per unit kerja atau program studi",
                "Tren capaian indikator mutu dari siklus evaluasi ke siklus evaluasi berikutnya",
                "Rencana Tindak Lanjut (RTL) atas temuan yang masih terbuka dan tenggat waktunya"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu SPMI atau Dasbor SPMI pada sistem navigasi utama.",
                "Panel menampilkan ringkasan capaian mutu secara otomatis dari data yang telah diinput.",
                "Perhatikan indikator dengan warna merah atau kuning — ini menandakan capaian di bawah standar yang ditetapkan dan memerlukan perhatian segera.",
                "Klik tombol <b>Layar Penuh</b> untuk tampilan yang lebih detail, terutama saat rapat evaluasi mutu.",
                "Koordinasikan dengan auditor mutu internal untuk menindaklanjuti setiap temuan secara sistematis.",
                "Ekspor laporan SPMI untuk pelaporan kepada BAN-PT, Kemendikbud, atau lembaga akreditasi internasional."
            })
            + h3("Interpretasi Data")
            + p("Capaian indikator mutu di atas 80% pada umumnya dianggap memuaskan. "
                + "Indikator yang capaiannya terus menurun antar siklus memerlukan program perbaikan "
                + "terstruktur yang didokumentasikan dalam Rencana Tindak Lanjut (RTL). "
                + "Dokumen SPMI yang lengkap, konsisten, dan dapat dibuktikan adalah salah satu "
                + "faktor penentu utama nilai akreditasi program studi maupun institusi secara keseluruhan.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Tinjau Dasbor SPMI secara berkala — minimal satu kali per semester — untuk memantau progres implementasi.",
                "Libatkan seluruh unit kerja dan program studi dalam pengisian data capaian indikator mutu.",
                "Gunakan temuan AMI sebagai bahan refleksi dan program peningkatan mutu berkelanjutan, bukan sekadar formalitas.",
                "Dokumentasikan setiap bukti capaian secara digital agar mudah diakses dan diverifikasi saat proses akreditasi.",
                "Pastikan Rencana Tindak Lanjut (RTL) atas setiap temuan diimplementasikan dan terdokumentasi dengan baik."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Apa yang dimaksud dengan siklus SPMI (PPEPP)?",
                "PPEPP adalah singkatan dari: Penetapan Standar, Pelaksanaan Standar, Evaluasi Pelaksanaan, Pengendalian Pelaksanaan, dan Peningkatan Standar. Kelima tahap ini membentuk siklus penjaminan mutu yang berulang.")
            + qa("Siapa yang bertanggung jawab mengisi data capaian indikator SPMI?",
                "Setiap unit kerja dan program studi bertanggung jawab mengisi data capaian indikator mutu di unit masing-masing, dikoordinasi oleh LPM di tingkat institusi dan GPM di tingkat program studi.")
            + qa("Apakah data SPMI ini langsung terhubung ke sistem BAN-PT?",
                "Tidak secara otomatis. Data SPMI perlu diekspor dan diunggah ke sistem BAN-PT (Sapto atau sistem akreditasi lainnya) secara terpisah sesuai jadwal akreditasi.")
        );
    }

    private static String bantuanELearning(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul E-Learning dan OBE (Outcome Based Education)")
            + p("Modul E-Learning dan OBE menyajikan data aktivitas pembelajaran daring (online) "
                + "serta capaian pembelajaran berbasis luaran (Outcome Based Education) mahasiswa. "
                + "Panel <b>" + esc(judul) + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data ini bersumber dari platform e-learning yang terintegrasi dengan sistem "
                + "dan dari instrumen penilaian OBE yang dikonfigurasi oleh dosen pengampu.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini dirancang untuk membantu dosen, koordinator kurikulum, dan pimpinan program "
                + "studi dalam memantau keterlibatan mahasiswa dalam pembelajaran digital dan "
                + "mengukur sejauh mana Capaian Pembelajaran Mata Kuliah (CPMK) serta "
                + "Capaian Pembelajaran Lulusan (CPL) telah tercapai. "
                + "Data OBE menjadi bukti krusial dalam proses akreditasi berbasis luaran (outcome-based accreditation).")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Tren aktivitas login dan keterlibatan mahasiswa di platform e-learning dari waktu ke waktu",
                "Capaian CPMK (Capaian Pembelajaran Mata Kuliah) dan CPL (Capaian Pembelajaran Lulusan) per mahasiswa",
                "Distribusi skor dan nilai berdasarkan instrumen penilaian OBE yang telah dikonfigurasi",
                "Persentase penyelesaian materi, tugas, dan kuis secara online",
                "Perbandingan tingkat aktivitas mahasiswa antar mata kuliah atau antar periode"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka menu Tampilan E-Learning atau menu OBE pada sistem.",
                "Klik tab Dasbor E-Learning untuk tren aktivitas, atau tab Dasbor OBE untuk data capaian pembelajaran.",
                "Panel memuat data secara otomatis dari platform e-learning yang terintegrasi.",
                "Gunakan grafik tren untuk mengidentifikasi pola aktivitas dari waktu ke waktu.",
                "Identifikasi mata kuliah dengan tingkat aktivitas rendah untuk intervensi dosen lebih lanjut.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan grafik yang lebih detail dan mudah dipresentasikan.",
                "Ekspor data OBE untuk pelaporan audit kurikulum atau pengisian dokumen akreditasi."
            })
            + h3("Interpretasi Data")
            + p("Tren aktivitas yang tinggi di awal semester kemudian menurun secara gradual adalah pola umum "
                + "dan dapat diterima. Namun, penurunan drastis di tengah semester perlu diinvestigasi "
                + "— apakah ada kendala teknis, penurunan motivasi, atau faktor lainnya. "
                + "Capaian CPL di bawah 60% mengindikasikan perlunya revisi metode pengajaran, "
                + "instrumen penilaian, atau bahkan rancangan kurikulum secara menyeluruh. "
                + "Data OBE yang baik harus menunjukkan distribusi capaian yang merata dan berkembang "
                + "dari semester ke semester.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Pantau aktivitas e-learning mingguan untuk mendorong keterlibatan aktif mahasiswa secara konsisten.",
                "Koordinasikan dengan dosen pengampu bila tingkat aktivitas mata kuliah tertentu sangat rendah.",
                "Data OBE perlu dikumpulkan dan dianalisis setiap akhir semester untuk evaluasi kurikulum.",
                "Hubungkan data OBE dengan data penilaian konvensional untuk gambaran capaian pembelajaran yang komprehensif.",
                "Pastikan semua instrumen penilaian sudah dikonfigurasi dan terhubung dengan CPMK serta CPL di sistem."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Data aktivitas e-learning tidak sinkron dengan platform LMS (Learning Management System) — mengapa?",
                "Kemungkinan proses sinkronisasi otomatis belum berjalan atau mengalami gangguan. Hubungi admin IT untuk memeriksa koneksi integrasi antara sistem AIS dan platform LMS.")
            + qa("Apa perbedaan antara CPMK dan CPL?",
                "CPMK (Capaian Pembelajaran Mata Kuliah) adalah capaian yang spesifik untuk satu mata kuliah. CPL (Capaian Pembelajaran Lulusan) adalah capaian tingkat program studi yang disusun dari gabungan CPMK berbagai mata kuliah.")
            + qa("Apakah mahasiswa dapat melihat capaian OBE mereka sendiri?",
                "Bergantung konfigurasi sistem dan kebijakan program studi. Hubungi dosen koordinator atau admin akademik untuk informasi akses mahasiswa terhadap data OBE.")
        );
    }

    private static String bantuanPembayaran(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Modul Pembayaran dan Keuangan Mahasiswa")
            + p("Modul Pembayaran menyajikan analitik transaksi pembayaran, tagihan, dan kondisi "
                + "keuangan mahasiswa secara visual dan terstruktur. Panel <b>" + esc(judul)
                + "</b> menampilkan: <em>" + esc(deskripsi) + "</em> "
                + "Data bersumber dari sistem keuangan institusi dan mencakup semua komponen "
                + "tagihan: biaya pendidikan, biaya SKS, biaya praktikum, dan item biaya lainnya "
                + "yang berlaku pada semester berjalan.")
            + h3("Fungsi dan Tujuan Panel")
            + p("Panel ini membantu admin keuangan, bagian kemahasiswaan, dan pimpinan dalam "
                + "memantau realisasi pembayaran mahasiswa secara agregat maupun individual. "
                + "Dengan visualisasi data pembayaran, identifikasi mahasiswa bermasalah keuangan "
                + "dapat dilakukan lebih awal, sehingga program intervensi (cicilan, beasiswa, "
                + "atau konseling keuangan) dapat dirancang lebih tepat sasaran.")
            + h3("Informasi yang Ditampilkan")
            + ul(new String[]{
                "Ringkasan tagihan total, jumlah yang sudah dibayar, dan sisa yang masih belum lunas",
                "Persentase pelunasan per mahasiswa atau per kelompok (prodi, angkatan)",
                "Tren pembayaran dari bulan ke bulan atau per semester akademik",
                "Distribusi cara pembayaran: tunai, transfer bank, virtual account, cicilan, dan lainnya",
                "Daftar mahasiswa dengan tunggakan atau pembayaran yang melebihi batas waktu"
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Buka halaman Pembayaran Mahasiswa pada sistem dan pilih tab Analisis atau Dasbor.",
                "Panel memuat data transaksi pembayaran secara otomatis dari basis data keuangan.",
                "Perhatikan kartu ringkasan: sudah dibayar, sisa tagihan, dan persentase pelunasan.",
                "Gunakan grafik tren untuk melihat pola pembayaran dari waktu ke waktu.",
                "Identifikasi mahasiswa dengan persentase pelunasan sangat rendah untuk program tindak lanjut.",
                "Gunakan tombol <b>Layar Penuh</b> untuk tampilan lebih luas saat membaca detail.",
                "Ekspor laporan melalui menu cetak atau ekspor di halaman utama untuk kebutuhan administrasi keuangan."
            })
            + h3("Interpretasi Data")
            + p("Persentase pelunasan mendekati 100% menunjukkan kondisi keuangan mahasiswa yang baik "
                + "dan kesadaran bayar yang tinggi. Tunggakan yang signifikan membutuhkan tindak lanjut "
                + "penagihan atau program cicilan yang lebih fleksibel. Tren pembayaran yang menumpuk "
                + "di akhir periode (deadline) menunjukkan kecenderungan menunda — ini adalah peluang "
                + "untuk menerapkan sistem reminder otomatis atau program insentif bayar lebih awal.")
            + h3("Tips Penggunaan")
            + ul(new String[]{
                "Pantau dasbor pembayaran setiap minggu untuk mengidentifikasi tunggakan lebih awal.",
                "Gunakan daftar mahasiswa bermasalah untuk program outreach atau konseling keuangan.",
                "Koordinasikan dengan bagian kemahasiswaan untuk program beasiswa atau keringanan bagi mahasiswa kurang mampu.",
                "Kirimkan reminder pembayaran otomatis kepada mahasiswa yang mendekati batas waktu.",
                "Verifikasi selalu data rekening mahasiswa agar notifikasi tagihan tersampaikan dengan benar."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Pembayaran sudah dilakukan tetapi status masih terlihat belum lunas — mengapa?",
                "Kemungkinan konfirmasi atau verifikasi pembayaran belum diproses oleh petugas keuangan. Tunjukkan bukti transfer atau kwitansi ke bagian keuangan untuk konfirmasi.")
            + qa("Apakah saya bisa melihat riwayat lengkap pembayaran di panel ini?",
                "Ya. Gunakan tab Riwayat Pembayaran atau Daftar Cicilan untuk melihat semua transaksi yang tercatat beserta tanggal, jumlah, dan metode pembayarannya.")
            + qa("Bagaimana cara mengajukan cicilan atau keringanan biaya?",
                "Hubungi bagian keuangan atau kemahasiswaan secara langsung. Bawa bukti kondisi keuangan yang diperlukan. Pengajuan cicilan atau keringanan tidak dapat dilakukan langsung dari panel ini.")
        );
    }

    private static String bantuanGenerik(String judul, String deskripsi) {
        return wrap(judul, deskripsi,
            h3("Pengantar Panel")
            + p("Panel <b>" + esc(judul) + "</b> merupakan komponen visualisasi data dalam "
                + "Sistem Informasi Akademik (SIA) institusi yang menampilkan informasi "
                + "secara ringkas dan mudah dipahami. Informasi yang tersaji telah diproses "
                + "secara otomatis dari basis data sistem sehingga selalu mencerminkan "
                + "kondisi terkini tanpa perlu diperbarui secara manual. "
                + "Secara spesifik, panel ini menampilkan: <em>" + esc(deskripsi) + "</em>")
            + h3("Cara Membaca Tampilan Panel")
            + p("Setiap panel dalam sistem ini terdiri dari beberapa bagian utama yang perlu "
                + "dipahami untuk memaksimalkan manfaat penggunaannya:")
            + ul(new String[]{
                "<b>Header Panel</b>: Bagian atas dengan strip aksen warna tema institusi, "
                  + "berisi judul panel, tombol Layar Penuh, dan tombol Bantuan.",
                "<b>Chip Deskripsi</b>: Kalimat singkat di bawah header yang menjelaskan "
                  + "isi panel dalam bahasa yang mudah dipahami oleh semua kalangan.",
                "<b>Area Konten</b>: Bagian utama yang berisi grafik, tabel, kartu angka, "
                  + "atau visualisasi data lainnya sesuai dengan jenis informasi yang disajikan."
            })
            + h3("Cara Menggunakan Panel Ini")
            + ol(new String[]{
                "Klik tab atau menu yang sesuai untuk memuat panel ini. Panel akan muncul secara otomatis.",
                "Tunggu beberapa saat hingga data selesai dimuat dari server. Ikon berputar "
                  + "(spinner) menandakan proses pemuatan sedang berlangsung.",
                "Baca angka atau visualisasi grafik yang ditampilkan. Setiap elemen dirancang "
                  + "untuk menyampaikan informasi penting secara sekilas (at a glance).",
                "Arahkan kursor (hover) ke atas elemen grafik — batang, irisan, atau titik data — "
                  + "untuk melihat nilai numerik detailnya dalam tooltip.",
                "Gunakan tombol <b>Layar Penuh (&#8599;)</b> di pojok kanan atas header untuk "
                  + "memperbesar panel agar lebih mudah dibaca, terutama pada layar kecil atau saat presentasi.",
                "Tekan tombol Pulihkan atau klik di luar area panel untuk kembali ke tampilan normal.",
                "Jika terdapat tombol filter atau pemilih periode, gunakan untuk mempersempit "
                  + "rentang data yang ditampilkan sesuai kebutuhan analisis.",
                "Untuk mencetak atau mengekspor data, gunakan menu Laporan yang tersedia "
                  + "di halaman utama modul yang sedang digunakan."
            })
            + h3("Fitur Standar Panel Portal")
            + p("Seluruh panel dalam sistem ini dilengkapi dua tombol standar di pojok kanan "
                + "atas header yang memudahkan pengalaman pengguna:")
            + ul(new String[]{
                "<b>Tombol Layar Penuh (&#8599;)</b>: Memperbesar panel hingga memenuhi seluruh "
                  + "area layar. Sangat berguna saat bekerja dengan data yang banyak, "
                  + "tampilan tabel yang panjang, atau saat mempresentasikan data kepada tim atau pimpinan.",
                "<b>Tombol Bantuan (?)</b>: Menampilkan panduan penggunaan ini kapan saja "
                  + "Anda membutuhkan penjelasan tentang fungsi dan cara membaca panel. "
                  + "Panduan dapat dibuka berkali-kali tanpa memengaruhi data yang ditampilkan."
            })
            + h3("Panduan Membaca Visualisasi Data")
            + p("Berikut panduan umum dalam membaca berbagai jenis visualisasi yang mungkin "
                + "ditampilkan pada panel ini:")
            + ul(new String[]{
                "<b>Grafik Batang (Bar Chart)</b>: Batang yang lebih tinggi menunjukkan nilai "
                  + "atau jumlah yang lebih besar pada kategori tersebut.",
                "<b>Diagram Lingkaran (Pie/Donut Chart)</b>: Irisan yang lebih besar menunjukkan "
                  + "proporsi yang lebih besar dari total keseluruhan data.",
                "<b>Grafik Garis (Line Chart)</b>: Garis yang naik menunjukkan peningkatan "
                  + "dari waktu ke waktu; garis turun menunjukkan penurunan.",
                "<b>Kartu Angka (Summary Card)</b>: Menampilkan satu nilai kunci yang paling "
                  + "penting dan perlu diketahui sekilas tanpa membaca grafik detail.",
                "<b>Tabel Data</b>: Berisi informasi rinci yang dapat dibaca baris per baris; "
                  + "beberapa tabel dapat diurutkan (sort) berdasarkan kolom tertentu.",
                "<b>Spider/Radar Chart</b>: Menampilkan perbandingan multi-dimensi; setiap sumbu "
                  + "mewakili satu dimensi analisis yang berbeda.",
                "<b>Sparkline</b>: Grafik mini yang menampilkan tren singkat tanpa label — "
                  + "berguna untuk melihat arah tren secara cepat."
            })
            + h3("Tips Penggunaan Sistem")
            + ul(new String[]{
                "Data yang ditampilkan selalu diambil dari basis data terkini secara otomatis "
                  + "setiap kali panel dimuat atau halaman diperbarui.",
                "Jika data terlihat tidak sesuai atau tidak diperbarui, muat ulang halaman "
                  + "(tekan F5 atau tombol Muat Ulang di browser) atau logout dan login kembali ke sistem.",
                "Gunakan panel ini secara reguler sebagai bahan evaluasi dan pengambilan keputusan "
                  + "berbasis data (data-driven decision making) — bukan hanya saat dibutuhkan saja.",
                "Tampilkan panel di layar besar atau proyektor saat rapat untuk memudahkan "
                  + "diskusi dan analisis bersama seluruh anggota tim.",
                "Jika menemukan ketidaksesuaian atau anomali data, segera laporkan kepada "
                  + "administrator sistem dengan menyertakan tangkapan layar dan keterangan detail."
            })
            + h3("Pertanyaan yang Sering Diajukan")
            + qa("Panel tidak menampilkan data apa pun — apa yang harus saya lakukan?",
                "Periksa terlebih dahulu koneksi internet Anda. Bila koneksi baik, coba muat ulang halaman. "
                + "Jika masih tidak muncul setelah beberapa menit, hubungi administrator sistem dan "
                + "sampaikan nama panel dan waktu terjadinya masalah.")
            + qa("Data yang ditampilkan terasa tidak akurat atau berbeda dari yang saya ketahui — mengapa?",
                "Pastikan Anda memilih filter periode, tahun akademik, atau kategori yang tepat. "
                + "Bila filter sudah benar namun data masih mencurigakan, segera laporkan kepada "
                + "administrator untuk pengecekan lebih lanjut di sisi basis data.")
            + qa("Apakah saya dapat mengubah atau mengedit data langsung dari panel ini?",
                "Tidak. Seluruh panel bersifat hanya-baca (read-only). Data hanya dapat diubah melalui modul "
                + "entri atau pengelolaan data yang sesuai oleh pengguna yang memiliki kewenangan.")
            + qa("Kepada siapa saya melapor bila ada masalah teknis pada panel ini?",
                "Hubungi administrator sistem atau tim TI institusi Anda. Cantumkan nama panel yang bermasalah, "
                + "tangkapan layar pesan error (bila ada), akun yang digunakan, dan waktu terjadinya masalah "
                + "agar penanganan dapat dilakukan dengan lebih cepat dan tepat.")
        );
    }

    // =========================================================================
    // HTML BUILDER HELPERS
    // =========================================================================

    private static String wrap(String judul, String deskripsi, String body) {
        return "<div style='font-family:\"Segoe UI\",Arial,sans-serif;"
            + "font-size:13px;line-height:1.75;color:#1e293b;'>"
            + "<h2 style='color:#007131;border-bottom:3px solid #007131;"
            + "padding-bottom:8px;margin:0 0 8px 0;font-size:15px;'>"
            + esc(judul) + "</h2>"
            + "<div style='background:#f0fdf4;border-left:4px solid #22c55e;"
            + "padding:7px 12px;border-radius:0 8px 8px 0;"
            + "font-size:12px;color:#166534;margin-bottom:14px;'>"
            + esc(deskripsi) + "</div>"
            + body
            + "<hr style='border:0;border-top:1px solid #e2e8f0;margin:18px 0 10px;'/>"
            + "<p style='font-size:11px;color:#94a3b8;margin:0;'>"
            + "Untuk bantuan lebih lanjut, hubungi administrator sistem atau tim TI institusi Anda.</p>"
            + "</div>";
    }

    private static String h3(String text) {
        return "<h3 style='color:#007131;font-size:13px;margin:16px 0 5px;"
            + "border-bottom:1px solid #dcfce7;padding-bottom:3px;'>"
            + esc(text) + "</h3>";
    }

    private static String p(String html) {
        return "<p style='margin:4px 0 10px;'>" + html + "</p>";
    }

    private static String ul(String[] items) {
        StringBuilder sb = new StringBuilder(
            "<ul style='margin:4px 0 10px;padding-left:20px;'>");
        for (int i = 0; i < items.length; i++) {
            sb.append("<li style='margin-bottom:5px;'>").append(items[i]).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private static String ol(String[] items) {
        StringBuilder sb = new StringBuilder(
            "<ol style='margin:4px 0 10px;padding-left:22px;'>");
        for (int i = 0; i < items.length; i++) {
            sb.append("<li style='margin-bottom:6px;'>").append(items[i]).append("</li>");
        }
        sb.append("</ol>");
        return sb.toString();
    }

    private static String qa(String q, String a) {
        return "<div style='background:#f8fafc;border:1px solid #e2e8f0;"
            + "border-radius:8px;padding:8px 12px;margin:6px 0;'>"
            + "<div style='font-weight:700;color:#1e293b;margin-bottom:3px;'>"
            + "&#10067; " + esc(q) + "</div>"
            + "<div style='color:#475569;'>" + esc(a) + "</div>"
            + "</div>";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
