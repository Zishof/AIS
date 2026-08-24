package ais.action.master.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ais.action.master.repository.RepositoryPublicService.FaqItem;

/** Katalog bantuan publik yang tidak bergantung pada database atau sesi pengguna. */
final class RepositoryFaqCatalog {
    private static final List<FaqItem> ITEMS=build();

    private RepositoryFaqCatalog() {}

    static List<FaqItem> all(){return ITEMS;}

    private static List<FaqItem> build(){
        List<FaqItem> items=new ArrayList<FaqItem>(300);

        add(items,"Akun dan autentikasi","Bagaimana menangani %s?",
                "Untuk menangani %s, pastikan identitas akun eCampus yang digunakan benar dan sesi login masih aktif. Coba keluar lalu masuk kembali, periksa pesan pada layar, dan jangan membagikan kata sandi atau kode verifikasi. Jika tetap gagal, catat waktu kejadian, nama menu, dan pesan galat lalu hubungi pengelola akun; pengelola Repository tidak perlu meminta kata sandi Anda.",new String[]{
            "lupa kata sandi eCampus","akun terkunci setelah beberapa percobaan login","sesi login yang tiba-tiba berakhir","akun mahasiswa belum dapat membuka Repository","akun dosen belum dikenali sebagai depositor",
            "perubahan nomor induk atau identitas pengguna","login berhasil tetapi kembali ke halaman awal","dua akun dengan alamat email yang sama","kode verifikasi yang tidak diterima","akses dari perangkat baru yang ditolak",
            "akun alumni yang sudah tidak aktif","akun pegawai setelah perpindahan unit","nama pengguna yang tidak dikenali","pesan bahwa pengguna tidak memiliki hak akses","kecurigaan bahwa akun digunakan orang lain"
        });

        add(items,"Pengajuan karya","Apa yang harus dilakukan untuk %s?",
                "Untuk %s, buka workspace Repository setelah login dan pilih pengajuan yang sesuai. Lengkapi setiap tahap secara berurutan, simpan draf setelah perubahan penting, lalu periksa ringkasan sebelum mengirim. Pastikan judul, penulis, program studi, tahun, abstrak, kata kunci, serta berkas sesuai dokumen final. Setelah dikirim, perubahan tertentu menunggu pengembalian atau tindakan reviewer.",new String[]{
            "memulai deposit karya baru","menyimpan pengajuan sebagai draf","melanjutkan draf dari perangkat lain","mengajukan skripsi atau tugas akhir","mengajukan tesis atau disertasi",
            "mengajukan artikel jurnal","mengajukan buku atau bab buku","mengajukan prosiding konferensi","mengajukan laporan penelitian","mengajukan bahan ajar",
            "memilih jenis dokumen yang tepat","memilih program studi pemilik karya","menambahkan lebih dari satu penulis","memeriksa ringkasan sebelum pengiriman","membatalkan draf yang belum dikirim"
        });

        add(items,"Metadata bibliografis","Bagaimana cara %s?",
                "Cara %s adalah menyalin informasi dari halaman judul atau sumber resmi, bukan menebak atau memakai singkatan yang tidak dijelaskan. Pertahankan ejaan nama, kapitalisasi, urutan penulis, dan tahun sebagaimana dokumen final. Gunakan abstrak serta kata kunci yang informatif, hindari data pribadi sensitif, kemudian cocokkan kembali pratinjau record sebelum dikirim untuk review.",new String[]{
            "menulis judul karya secara konsisten","mengisi judul alternatif atau judul terjemahan","menuliskan nama penulis tanpa membalik urutan","menambahkan pembimbing dan penguji","menentukan tahun terbit karya",
            "menulis abstrak bahasa Indonesia","menambahkan abstrak bahasa Inggris","memilih kata kunci yang relevan","menentukan bahasa utama dokumen","mengisi nama penerbit atau institusi",
            "mencatat nomor halaman atau jumlah halaman","mengisi identifier eksternal","menentukan bidang ilmu atau subjek","menulis afiliasi penulis","memeriksa metadata sebelum publikasi"
        });

        add(items,"Berkas dan unggahan","Bagaimana menyelesaikan %s?",
                "Untuk menyelesaikan %s, gunakan berkas final yang dapat dibuka, tidak rusak, dan sesuai batas format serta ukuran yang tampil pada formulir. Beri nama berkas yang jelas tanpa karakter aneh, tunggu unggahan selesai, lalu periksa status pemindaian dan pratinjau. Jangan mengunggah data rahasia; jika unggahan gagal, simpan draf dan catat nama, jenis, serta ukuran berkas.",new String[]{
            "unggahan PDF utama","unggahan lampiran penelitian","unggahan berkas berukuran besar","unggahan yang berhenti sebelum selesai","PDF yang tidak dapat dipratinjau",
            "berkas yang salah setelah diunggah","penggantian berkas sebelum pengiriman","penambahan lebih dari satu lampiran","penamaan berkas yang ditolak","PDF yang dilindungi kata sandi",
            "berkas hasil pemindaian yang terlalu besar","dokumen yang belum memiliki watermark","unggahan format selain PDF","berkas yang terdeteksi tidak aman","penghapusan lampiran dari draf"
        });

        add(items,"Proses review","Apa arti dan tindakan untuk %s?",
                "Pada kondisi %s, baca status dan catatan reviewer pada riwayat pengajuan terlebih dahulu. Jika diminta revisi, perbaiki hanya bagian yang disebutkan, jelaskan perubahan secara singkat, simpan, lalu kirim ulang. Hindari membuat pengajuan duplikat. Bila catatan tidak jelas atau status tidak berubah dalam waktu layanan yang berlaku, gunakan kanal bantuan dengan menyertakan nomor pengajuan, bukan berkas rahasia.",new String[]{
            "status menunggu review","pengajuan sedang diperiksa reviewer","permintaan revisi metadata","permintaan penggantian berkas","catatan reviewer yang belum dipahami",
            "pengajuan dikembalikan kepada depositor","pengajuan dikirim ulang setelah revisi","pengajuan disetujui reviewer","pengajuan ditolak reviewer","review yang tampak lebih lama dari biasanya",
            "dua reviewer memberi catatan berbeda","riwayat review yang perlu ditelusuri","perubahan setelah pengajuan dikirim","pengajuan yang tidak muncul di antrean","notifikasi review yang tidak diterima"
        });

        add(items,"Publikasi dan visibilitas","Mengapa atau bagaimana menangani %s?",
                "Untuk memahami %s, periksa status publikasi, kebijakan akses, koleksi, dan waktu pembaruan pada record. Record baru dapat memerlukan persetujuan dan sinkronisasi sebelum terlihat dalam pencarian. Metadata dapat tampil sementara berkas tetap dibatasi. Jika status sudah disetujui tetapi record belum terlihat setelah proses normal, sampaikan identifier atau URL record kepada administrator untuk pemeriksaan indeks.",new String[]{
            "record belum muncul setelah disetujui","metadata tampil tetapi berkas tidak terlihat","record hanya muncul melalui tautan langsung","record tidak muncul pada koleksi","record tidak ditemukan melalui pencarian judul",
            "tahun terbit berbeda pada daftar","jumlah karya pada profil penulis belum berubah","publikasi berstatus metadata saja","badge akses berbeda dari yang diharapkan","record muncul dua kali dalam hasil",
            "perubahan metadata belum terlihat publik","gambar atau pratinjau belum tersedia","publikasi lama belum masuk Repository","record terlihat pada koleksi yang keliru","tanggal publikasi berbeda dari tanggal karya"
        });

        add(items,"Pencarian dan penelusuran","Bagaimana menggunakan Repository untuk %s?",
                "Untuk %s, mulai dengan kata kunci inti lalu pilih bidang pencarian yang sesuai, misalnya judul, penulis, subjek, abstrak, program studi, atau identifier. Gunakan filter untuk mempersempit tahun, koleksi, jenis dokumen, bahasa, dan akses; hapus filter yang terlalu ketat bila hasil kosong. Buka detail record untuk memverifikasi kecocokan karena cuplikan hasil bukan pengganti pembacaan metadata lengkap.",new String[]{
            "mencari judul tertentu","mencari karya seorang penulis","menelusuri topik penelitian","mencari berdasarkan program studi","mencari berdasarkan tahun terbit",
            "mencari DOI atau Handle","mencari frasa yang tepat","menggabungkan beberapa kata kunci","mengecualikan istilah tertentu","menampilkan hanya naskah lengkap",
            "mengurutkan hasil terbaru","mengurutkan judul secara alfabetis","menelusuri karya berbahasa Inggris","memperbaiki pencarian tanpa hasil","membandingkan hasil dari beberapa filter"
        });

        add(items,"Koleksi dan klasifikasi","Bagaimana memahami atau memperbaiki %s?",
                "Untuk %s, lihat nama, deskripsi, tipe, dan unit pemilik koleksi pada halaman koleksi. Koleksi mengelompokkan record untuk penelusuran dan tata kelola; koleksi bukan selalu sama dengan jenis dokumen atau program studi. Depositor memilih opsi yang diizinkan, sedangkan perubahan koleksi pada record terbit perlu diperiksa administrator agar statistik, izin, dan identifier terkait tetap konsisten.",new String[]{
            "perbedaan koleksi dan jenis dokumen","pemilihan koleksi saat deposit","koleksi yang tidak tersedia pada formulir","record berada pada koleksi yang salah","koleksi otomatis untuk tugas akhir",
            "koleksi untuk artikel dosen","koleksi lintas program studi","deskripsi koleksi yang belum jelas","jumlah item pada koleksi","urutan publikasi dalam koleksi",
            "pencarian hanya dalam satu koleksi","pemindahan record antarkoleksi","koleksi yang dinonaktifkan","koleksi baru untuk unit kerja","hubungan koleksi dengan kebijakan akses"
        });

        add(items,"Akses naskah lengkap","Bagaimana menangani %s?",
                "Untuk menangani %s, periksa badge akses dan daftar berkas pada halaman detail. Metadata serta abstrak dapat terbuka sementara naskah lengkap membutuhkan login, izin institusi, berakhirnya embargo, atau persetujuan lain. Masuk dengan akun yang sah dan jangan mencoba melewati pembatasan. Jika Anda seharusnya berhak tetapi tombol tetap tidak ada, kirim URL record dan jenis akses yang diharapkan kepada pengelola.",new String[]{
            "tombol unduh yang tidak muncul","permintaan login saat membuka PDF","berkas yang hanya dapat diakses sivitas akademika","naskah berstatus terbatas","record yang hanya menyediakan metadata",
            "PDF yang tersedia tetapi tidak dapat dibuka","akses dari luar jaringan kampus","akses oleh alumni","akses oleh peneliti eksternal","permintaan salinan kepada penulis",
            "batas unduhan yang berlaku","lampiran dengan hak akses berbeda","naskah yang sedang embargo","akses setelah masa embargo berakhir","pelaporan tautan unduhan yang rusak"
        });

        add(items,"Embargo, hak cipta, dan lisensi","Apa yang perlu diketahui tentang %s?",
                "Tentang %s, pastikan keputusan mengikuti persetujuan penulis, kontrak penerbit, kebijakan institusi, dan hak pihak ketiga. Embargo menunda akses berkas tanpa harus menyembunyikan metadata; lisensi menjelaskan pemanfaatan yang diizinkan dan tidak memindahkan kepemilikan hak cipta. Jangan memilih lisensi yang haknya tidak Anda miliki. Mintalah pemeriksaan pengelola bila dokumen memuat materi berizin.",new String[]{
            "penentuan tanggal akhir embargo","perbedaan embargo dan akses terbatas","pemilihan lisensi publik","penggunaan lisensi Creative Commons","hak cipta skripsi atau tesis",
            "hak penerbit atas artikel jurnal","pengunggahan versi manuskrip penulis","materi pihak ketiga di dalam karya","gambar atau instrumen berhak cipta","pencabutan izin publikasi",
            "perubahan lisensi setelah terbit","metadata selama masa embargo","akses khusus untuk penguji","permintaan penggunaan ulang dokumen","pelaporan dugaan pelanggaran hak cipta"
        });

        add(items,"Penulis, ORCID, dan afiliasi","Bagaimana mengelola %s?",
                "Untuk mengelola %s, gunakan bentuk nama resmi dan hubungkan identitas hanya melalui proses verifikasi yang tersedia. ORCID membantu membedakan penulis dengan nama serupa, sedangkan afiliasi dan ROR membantu mengenali institusi. Jangan menyalin ORCID orang lain. Bila karya terpecah karena variasi nama atau justru tergabung dengan penulis lain, kirim contoh URL record agar administrator dapat meninjau authority penulis.",new String[]{
            "variasi ejaan nama penulis","gelar akademik pada nama penulis","nama penulis yang berubah","dua penulis dengan nama sama","karya yang belum masuk profil penulis",
            "karya orang lain pada profil penulis","penautan akun ORCID","ORCID yang salah pada record","pemutusan tautan ORCID","pencantuman afiliasi utama",
            "afiliasi lebih dari satu institusi","identifier ROR institusi","urutan penulis pada karya","penulis korespondensi","verifikasi identitas penulis oleh administrator"
        });

        add(items,"DOI, Handle, dan OAI","Apa fungsi atau solusi untuk %s?",
                "Untuk %s, bedakan identifier lokal, Handle, DOI, dan identifier OAI. Identifier harus stabil dan digunakan untuk merujuk record, sedangkan URL tampilan dapat berubah mengikuti konfigurasi sistem. Jangan membuat atau mengganti DOI secara manual. Jika identifier tidak dapat diresolusikan, salin nilainya persis, periksa status publikasi, lalu laporkan kepada administrator atau layanan penerbit identifier terkait.",new String[]{
            "identifier lokal Repository","tautan Handle permanen","DOI pada publikasi","DOI yang belum aktif","DOI yang mengarah ke halaman keliru",
            "identifier OAI-PMH","perbedaan DOI dan Handle","pencarian menggunakan identifier","penyalinan tautan permanen","identifier pada versi karya",
            "identifier record yang ditarik","metadata DOI yang belum diperbarui","set koleksi pada OAI-PMH","alamat endpoint OAI-PMH","pelaporan identifier duplikat"
        });

        add(items,"Sitasi dan ekspor referensi","Bagaimana melakukan %s?",
                "Untuk melakukan %s, buka halaman detail dan gunakan menu sitasi atau ekspor yang tersedia. Pilih gaya atau format yang didukung, kemudian cocokkan judul, penulis, tahun, dan identifier dengan record asli sebelum dipakai. Sitasi otomatis merupakan bantuan awal dan mungkin perlu disesuaikan dengan pedoman jurnal, kampus, atau pengelola referensi. Laporkan metadata salah sebelum menyebarkan sitasi.",new String[]{
            "penyalinan sitasi APA","penyalinan sitasi MLA","penyalinan sitasi Chicago","ekspor BibTeX","ekspor RIS",
            "impor ke Zotero","impor ke Mendeley","sitasi menggunakan DOI","sitasi menggunakan Handle","sitasi karya tanpa tahun",
            "sitasi karya dengan banyak penulis","sitasi versi terbaru suatu karya","pemeriksaan kapitalisasi judul","perbaikan sitasi otomatis","pengunduhan metadata untuk daftar pustaka"
        });

        add(items,"Versi, koreksi, dan penarikan","Bagaimana menangani %s?",
                "Untuk menangani %s, jangan menimpa riwayat publik tanpa pemeriksaan. Koreksi kecil dapat memperbarui metadata, sedangkan perubahan substantif mungkin memerlukan versi baru yang tetap terhubung dengan versi sebelumnya. Penarikan mempertahankan tombstone dan identifier agar jejak ilmiah tidak hilang. Sertakan alasan, bukti, URL record, serta persetujuan yang relevan ketika mengajukan tindakan kepada administrator.",new String[]{
            "salah ketik pada metadata terbit","nama penulis yang keliru","abstrak yang perlu diperbaiki","berkas final yang salah","penerbitan versi revisi",
            "hubungan antara dua versi karya","record duplikat","permintaan penarikan karya","record yang sudah ditarik","alasan penarikan yang ditampilkan",
            "pemulihan record yang ditarik","koreksi setelah DOI terbit","perubahan koleksi setelah publikasi","penggantian lampiran publik","pelaporan masalah integritas karya"
        });

        add(items,"Notifikasi, bookmark, dan alert","Bagaimana menggunakan atau memperbaiki %s?",
                "Untuk %s, pastikan Anda login lalu gunakan tombol simpan, bookmark, RSS, atau alert pada halaman yang relevan. Beri nama pencarian tersimpan secara jelas dan tinjau filter sebelum mengaktifkan notifikasi agar hasilnya tidak terlalu luas. Jika notifikasi tidak diterima, periksa status preferensi dan alamat akun; hapus preferensi lama yang tidak diperlukan tanpa memengaruhi record publik.",new String[]{
            "bookmark sebuah publikasi","penghapusan bookmark","pencarian tersimpan","alert untuk topik tertentu","alert untuk penulis tertentu",
            "alert dengan filter koleksi","perubahan nama pencarian tersimpan","penonaktifan alert","notifikasi pengajuan","notifikasi permintaan revisi",
            "notifikasi persetujuan karya","RSS publikasi terbaru","RSS karya seorang penulis","feed Atom Repository","notifikasi yang tidak diterima"
        });

        add(items,"Kendala teknis","Apa langkah pemecahan untuk %s?",
                "Langkah pemecahan untuk %s adalah menyimpan pekerjaan, memuat ulang satu kali, lalu mencoba peramban terbaru dengan koneksi stabil. Hindari mengirim formulir berulang kali karena dapat membuat duplikasi. Jika masalah berlanjut, catat URL, waktu, langkah sebelum gagal, pesan galat, peramban, dan tangkapan layar tanpa data sensitif. Informasi tersebut membantu tim teknis menelusuri log secara tepat.",new String[]{
            "halaman Repository kosong","halaman memuat sangat lama","pesan galat 404","pesan galat 403","pesan galat 500",
            "tombol yang tidak merespons","formulir tidak dapat disimpan","data formulir kembali kosong","unggahan berhenti","pratinjau PDF gagal",
            "hasil pencarian tidak berubah","tampilan rusak pada ponsel","menu tertutup atau terpotong","peramban lama yang tidak didukung","gangguan setelah koneksi internet terputus"
        });

        add(items,"Privasi dan keamanan","Apa ketentuan untuk %s?",
                "Ketentuan untuk %s adalah menggunakan Repository hanya untuk tujuan yang sah dan membagikan data seminimal mungkin. Jangan mengunggah kata sandi, nomor identitas sensitif, tanda tangan, data kesehatan, atau data responden yang belum dianonimkan. Hak akses diperiksa di server dan aktivitas penting dapat dicatat untuk keamanan. Laporkan dugaan kebocoran segera melalui kanal resmi tanpa menyebarkan data lebih lanjut.",new String[]{
            "data pribadi di dalam dokumen","data responden penelitian","dokumen yang memuat tanda tangan","nomor identitas pada lampiran","informasi kontak pribadi",
            "penggunaan akun bersama","pembagian tautan berkas terbatas","percobaan akses tanpa izin","pelaporan dugaan kebocoran data","penghapusan data sensitif dari draf",
            "log aktivitas Repository","alamat IP dalam pencatatan keamanan","persetujuan subjek penelitian","anonimisasi data sebelum unggah","permintaan akses terhadap data pribadi"
        });

        add(items,"Aksesibilitas","Bagaimana memperoleh atau melaporkan %s?",
                "Untuk memperoleh atau melaporkan %s, gunakan struktur judul, label formulir, navigasi papan ketik, dan teks tautan yang tersedia. Perbesar tampilan melalui peramban tanpa mengubah isi halaman dan gunakan pembaca layar dengan mode standar. Untuk dokumen, PDF bertag dan teks yang dapat dipilih lebih mudah diakses. Laporkan komponen, URL, perangkat bantu, dan hambatan spesifik kepada pengelola.",new String[]{
            "navigasi Repository dengan papan ketik","penggunaan Repository dengan pembaca layar","fokus tombol yang tidak terlihat","label formulir yang tidak terbaca","kontras teks yang kurang jelas",
            "pembesaran tampilan halaman","akses Repository melalui ponsel","PDF yang hanya berupa gambar","PDF tanpa urutan baca","teks alternatif untuk gambar",
            "transkrip untuk materi audio","teks untuk materi video","bahasa dokumen yang tidak terdeteksi","tautan dengan nama yang tidak jelas","permintaan format dokumen yang lebih aksesibel"
        });

        add(items,"Data penelitian dan lampiran","Bagaimana mempersiapkan %s?",
                "Untuk mempersiapkan %s, pastikan data boleh dibagikan, telah dianonimkan, didokumentasikan, dan menggunakan format yang dapat dipahami kembali. Sertakan README, metode pengumpulan, definisi variabel, versi perangkat lunak, serta lisensi bila relevan. Pisahkan data terbuka dari data terbatas dan jangan mengunggah data sensitif hanya karena menjadi lampiran karya. Konsultasikan kebijakan unit sebelum publikasi.",new String[]{
            "dataset pendukung penelitian","README untuk dataset","kamus data atau codebook","instrumen penelitian","kuesioner penelitian",
            "transkrip wawancara","kode sumber analisis","berkas statistik","gambar beresolusi tinggi","materi audio penelitian",
            "materi video penelitian","data dengan ukuran sangat besar","data dengan pembatasan akses","lisensi dataset","versi baru dataset"
        });

        add(items,"Integrasi, kebijakan, dan bantuan","Di mana memperoleh penjelasan tentang %s?",
                "Penjelasan tentang %s tersedia pada halaman kebijakan, bantuan, atau endpoint publik yang relevan. Gunakan dokumentasi resmi dan jangan menganggap tampilan antarmuka sebagai izin untuk integrasi otomatis. Untuk kebutuhan sistem lain, sebutkan tujuan, format, frekuensi, volume, dan penanggung jawab. Pengelola akan menilai autentikasi, batas akses, lisensi metadata, dampak layanan, dan kontak teknis.",new String[]{
            "kebijakan deposit institusi","kebijakan akses publik","kebijakan preservasi digital","ketentuan penghapusan record","standar metadata Repository",
            "pemanenan metadata melalui OAI-PMH","penggunaan RSS atau Atom","integrasi dengan sistem perpustakaan","integrasi dengan profil peneliti","integrasi dengan pengelola referensi",
            "permintaan ekspor metadata","pelaporan metadata keliru","pelaporan gangguan layanan","permintaan pelatihan Repository","kontak administrator Repository"
        });

        if(items.size()!=300)throw new IllegalStateException("Katalog FAQ wajib berisi tepat 300 entri, aktual: "+items.size());
        return Collections.unmodifiableList(items);
    }

    private static void add(List<FaqItem> items,String category,String questionPattern,String answerPattern,String[] topics){
        if(topics.length!=15)throw new IllegalArgumentException("Setiap kategori FAQ wajib memiliki 15 topik: "+category);
        for(String topic:topics){
            FaqItem item=new FaqItem();item.id=items.size()+1;item.category=category;
            item.question=String.format(questionPattern,topic);
            item.answer=String.format(answerPattern,topic);
            item.keywords=category+" "+topic;
            items.add(item);
        }
    }
}
