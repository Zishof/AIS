<%@page import="java.io.File"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.KarirConfigUtil"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%!
    private String pk(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String pbr(String s) {
        return pk(s).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br><br>");
    }
    private boolean safeKarirJsp(String path) {
        if (path == null) return false;
        path = path.trim().replace('\\', '/');
        return path.startsWith("/WEB-INF/baru/modul/karir/") && path.endsWith(".jsp") && path.indexOf("..") < 0;
    }
    private String defaultPanduanPendaftaran() { return "Panduan Portal Karir ini disusun sebagai pedoman resmi bagi setiap pelamar yang akan mengikuti proses penerimaan pegawai melalui sistem informasi institusi. Portal ini menjadi pintu masuk utama untuk melihat informasi lowongan, mengirim pendaftaran, melengkapi data diri, mengunggah dokumen persyaratan, serta memantau perkembangan seleksi. Seluruh proses dirancang agar lebih tertib, transparan, terdokumentasi, dan mudah dipantau oleh pelamar maupun panitia. Oleh karena itu, setiap pelamar diharapkan membaca panduan ini dengan saksama sebelum melakukan pendaftaran.\n\nTahap pertama yang perlu dilakukan adalah memeriksa daftar lowongan yang tersedia. Pada bagian informasi lowongan, pelamar dapat melihat nama posisi, periode pendaftaran, kuota atau kebutuhan formasi, jalur atau kelompok pendaftaran apabila tersedia, serta keterangan tambahan yang berkaitan dengan persyaratan, tugas, tanggung jawab, fasilitas, dan catatan penting dari panitia seleksi. Pelamar sebaiknya memastikan bahwa posisi yang dipilih benar-benar sesuai dengan latar belakang pendidikan, pengalaman, kompetensi, dan minat kerja. Kesalahan memilih lowongan dapat menyebabkan proses verifikasi menjadi lebih lama atau data pendaftaran tidak sesuai dengan kebutuhan seleksi. Apabila suatu lowongan belum dibuka, sudah ditutup, atau dinonaktifkan oleh panitia, tombol pendaftaran dapat tidak tersedia. Kondisi tersebut bukan merupakan gangguan sistem, melainkan mengikuti jadwal resmi yang ditetapkan oleh institusi.\n\nTahap kedua adalah melakukan pendaftaran melalui tombol daftar pada lowongan yang dipilih. Sistem akan menampilkan formulir pendaftaran calon pegawai. Pelamar wajib mengisi data diri dengan benar, lengkap, dan konsisten dengan dokumen resmi yang dimiliki. Data seperti nama lengkap, alamat email, nomor telepon, tempat lahir, tanggal lahir, jenis kelamin, agama, dan alamat tempat tinggal harus ditulis secara jelas. Alamat email harus aktif karena sistem dapat mengirimkan informasi akun, pemberitahuan, atau instruksi lanjutan melalui email tersebut. Nomor telepon juga sebaiknya merupakan nomor yang masih digunakan agar panitia dapat menghubungi pelamar apabila diperlukan. Jika terdapat perbedaan antara data yang diisi dengan dokumen yang diunggah, panitia berhak meminta klarifikasi, revisi, atau menunda proses verifikasi sampai data dianggap benar.\n\nTahap ketiga adalah mengunggah dokumen persyaratan. Dokumen yang umum diminta antara lain kartu identitas, surat keterangan pendidikan, surat pengalaman kerja apabila tersedia, surat lamaran, curriculum vitae, dan pas foto terbaru. Setiap dokumen perlu diunggah pada kategori yang tepat. File harus jelas, terbaca, tidak rusak, dan tidak berisi informasi yang tidak relevan. Untuk dokumen hasil pindai, pastikan seluruh bagian terlihat utuh, tidak terpotong, dan tidak buram. Untuk foto, gunakan gambar yang sopan dan sesuai kebutuhan administrasi. Pelamar tidak disarankan mengunggah file berukuran sangat besar apabila tidak diperlukan, karena dapat memperlambat proses unggah. Apabila sistem atau panitia menetapkan batas ukuran dan format file, pelamar wajib mengikuti ketentuan tersebut. Dokumen yang tidak sesuai kategori, tidak terbaca, atau tidak memenuhi syarat dapat diberi status revisi.\n\nSetelah pendaftaran berhasil dikirim, sistem dapat membuat akun akses Portal Karir bagi pelamar. Informasi username dan password dikirimkan melalui email yang telah didaftarkan. Pelamar wajib menyimpan informasi tersebut secara aman dan tidak membagikannya kepada pihak lain. Akun tersebut digunakan untuk masuk ke dashboard pelamar. Apabila email akses tidak ditemukan, pelamar dapat menggunakan fitur kirim ulang akses dengan memasukkan email yang sudah terdaftar. Jika email tetap tidak diterima, periksa folder spam, promosi, atau kotak masuk lain yang mungkin digunakan oleh penyedia layanan email. Pelamar juga dapat menghubungi layanan bantuan sesuai kontak resmi institusi.\n\nPada dashboard pelamar, sistem menampilkan ringkasan status seleksi, progres dokumen, informasi revisi, jadwal seleksi, ruang atau lokasi kegiatan, serta pengumuman yang berkaitan dengan proses penerimaan. Informasi ini membantu pelamar mengetahui apakah dokumen sudah diperiksa, apakah ada catatan perbaikan, apakah jadwal interview atau tes sudah tersedia, dan apakah hasil seleksi sudah ditetapkan. Dashboard bukan hanya tempat melihat pengumuman, tetapi juga menjadi ruang pemantauan pribadi yang terhubung dengan data pendaftaran. Pelamar dianjurkan login secara berkala agar tidak melewatkan informasi terbaru.\n\nApabila dokumen mendapatkan catatan revisi, pelamar perlu membaca catatan tersebut dengan cermat. Revisi dapat berarti file belum jelas, dokumen tidak sesuai kategori, data tidak sama dengan formulir, atau terdapat kekurangan informasi. Setelah memperbaiki dokumen, pelamar dapat mengunggah ulang file melalui dashboard. Proses revisi harus dilakukan sebelum batas waktu yang ditentukan oleh panitia. Keterlambatan memperbaiki dokumen dapat memengaruhi kelanjutan seleksi. Panitia dapat menyetujui dokumen setelah file dianggap memenuhi ketentuan. Status terverifikasi menunjukkan bahwa dokumen telah diperiksa dan diterima secara administrasi, tetapi tidak selalu berarti pelamar langsung dinyatakan lulus keseluruhan seleksi.\n\nSelama proses seleksi, pelamar wajib menjaga kejujuran dan keabsahan data. Setiap pernyataan, dokumen, sertifikat, pengalaman kerja, dan informasi pendidikan harus dapat dipertanggungjawabkan. Jika di kemudian hari ditemukan data yang tidak benar, dipalsukan, atau menyesatkan, institusi berhak membatalkan pendaftaran, menggugurkan pelamar dari proses seleksi, atau mengambil tindakan sesuai ketentuan yang berlaku. Pendaftaran melalui Portal Karir tidak boleh disalahgunakan untuk mencoba mengakses data pihak lain, mengunggah file berbahaya, mengirim spam, atau melakukan tindakan yang dapat mengganggu keamanan sistem.\n\nPelamar juga perlu memahami bahwa setiap informasi resmi hanya berlaku apabila diumumkan melalui kanal yang ditentukan oleh institusi, seperti Portal Karir, email resmi, pengumuman tertulis, atau kontak panitia yang sah. Hati-hati terhadap pihak yang mengatasnamakan panitia dan meminta pembayaran, imbalan, atau data rahasia di luar prosedur resmi. Pada prinsipnya, proses rekrutmen institusi harus berjalan objektif, tertib, dan sesuai aturan. Apabila terdapat informasi yang meragukan, pelamar sebaiknya menghubungi layanan bantuan sebelum mengambil tindakan.\n\nPanduan ini dapat disesuaikan oleh admin melalui menu konfigurasi. Institusi dapat menambahkan ketentuan khusus, memperbarui cara pendaftaran, mengubah instruksi dokumen, atau menyesuaikan narasi sesuai kebijakan internal. Apabila terdapat perbedaan antara panduan umum ini dengan pengumuman resmi terbaru, maka pengumuman resmi terbaru dari institusi menjadi acuan utama. Dengan membaca dan mengikuti panduan ini, pelamar diharapkan dapat menjalani proses pendaftaran secara lebih mudah, tertib, dan bertanggung jawab.\n\nDalam penggunaan sehari-hari, pelamar dianjurkan menggunakan perangkat pribadi atau perangkat yang dipercaya, terutama ketika mengisi data penting dan mengunggah dokumen. Setelah selesai menggunakan portal, khususnya pada komputer bersama, pelamar sebaiknya menekan tombol keluar agar sesi tidak digunakan oleh orang lain. Periksa kembali setiap isian sebelum menekan tombol kirim, karena data yang sudah masuk akan menjadi dasar pemeriksaan administrasi. Apabila terjadi kesalahan pengisian, segera perbaiki melalui dashboard jika fitur perbaikan tersedia, atau hubungi panitia dengan menjelaskan bagian yang perlu dikoreksi. Gunakan bahasa yang sopan dan jelas saat berkomunikasi dengan panitia. Hindari mengirim pertanyaan berulang secara berlebihan karena dapat memperlambat proses pelayanan. Simpan salinan dokumen asli dan bukti pendaftaran secara rapi sampai seluruh proses seleksi selesai. Dengan disiplin mengikuti petunjuk ini, pelamar turut membantu panitia menjaga proses rekrutmen tetap tertib, adil, profesional, dan mudah ditelusuri."; }
    private String defaultSyaratKetentuan() { return "Syarat dan ketentuan Portal Karir mengatur tata cara penggunaan layanan rekrutmen resmi institusi. Pelamar wajib memberikan data yang benar, memakai alamat email dan nomor telepon aktif, memilih lowongan sesuai minat dan kompetensi, serta mengunggah dokumen yang jelas dan dapat dipertanggungjawabkan. Panitia berwenang memeriksa, menyetujui, meminta revisi, menolak, atau membatalkan data apabila ditemukan ketidaksesuaian. Keputusan seleksi mengikuti kebijakan institusi dan informasi resmi yang diumumkan melalui portal atau kanal komunikasi yang ditentukan. Pelamar dilarang menyalahgunakan akun, mengunggah file berbahaya, menggunakan data pihak lain tanpa izin, atau melakukan tindakan yang dapat mengganggu keamanan sistem."; }
    private String defaultKebijakanPrivasi() { return "Kebijakan privasi Portal Karir menjelaskan bahwa data pelamar digunakan untuk kebutuhan administrasi penerimaan pegawai, verifikasi dokumen, penjadwalan seleksi, komunikasi resmi, dan pengambilan keputusan oleh panitia yang berwenang. Data pribadi, dokumen, dan informasi kontak diproses secara terbatas sesuai kebutuhan seleksi. Institusi berupaya menjaga kerahasiaan data dengan mekanisme akses internal dan tata kelola sistem yang tersedia. Pelamar tetap bertanggung jawab menjaga kerahasiaan username, password, dan perangkat yang digunakan untuk login."; }
    private String defaultBantuan() { return "Layanan bantuan Portal Karir disediakan untuk membantu pelamar ketika mengalami kendala saat melihat lowongan, mendaftar, menerima email akses, login, mengunggah dokumen, membaca status verifikasi, atau memahami jadwal seleksi. Sebelum menghubungi panitia, pelamar disarankan memeriksa kembali koneksi internet, ukuran file, format dokumen, folder spam email, dan kebenaran data yang dimasukkan. Jelaskan kendala secara singkat, sertakan nama lengkap, email terdaftar, nomor telepon, dan tangkapan layar apabila diperlukan agar panitia dapat melakukan pengecekan lebih cepat."; }
%>
<%
    String halaman = request.getParameter("halaman");
    if (halaman == null || halaman.trim().length() == 0) halaman = "panduan_pendaftaran";
    halaman = halaman.trim().toLowerCase().replace('-', '_');
    String judul = "Panduan Portal Karir";
    String ringkas = "Informasi resmi yang membantu pelamar memahami alur layanan Karir.";
    String textKey = "karir_panduan_pendaftaran_text";
    String titleKey = "karir_panduan_pendaftaran_judul";
    String fileKey = "karir_footer_file_panduan_pendaftaran";
    String defaultText = defaultPanduanPendaftaran();
    String icon = "fas fa-book-open";
    if ("syarat".equals(halaman) || "syarat_ketentuan".equals(halaman)) {
        titleKey = "karir_syarat_ketentuan_judul"; textKey = "karir_syarat_ketentuan_text"; fileKey = "karir_footer_file_syarat_ketentuan";
        judul = "Syarat dan Ketentuan Portal Karir"; ringkas = "Ketentuan dasar penggunaan layanan pendaftaran dan pengumuman seleksi."; defaultText = defaultSyaratKetentuan(); icon = "fas fa-file-contract";
    } else if ("privasi".equals(halaman) || "kebijakan_privasi".equals(halaman)) {
        titleKey = "karir_kebijakan_privasi_judul"; textKey = "karir_kebijakan_privasi_text"; fileKey = "karir_footer_file_kebijakan_privasi";
        judul = "Kebijakan Privasi Portal Karir"; ringkas = "Penjelasan penggunaan data pelamar dalam proses rekrutmen."; defaultText = defaultKebijakanPrivasi(); icon = "fas fa-shield-halved";
    } else if ("bantuan".equals(halaman) || "help".equals(halaman)) {
        titleKey = "karir_bantuan_judul"; textKey = "karir_bantuan_text"; fileKey = "karir_footer_file_bantuan";
        judul = "Bantuan Pelamar Portal Karir"; ringkas = "Petunjuk saat pelamar mengalami kendala pada proses pendaftaran."; defaultText = defaultBantuan(); icon = "fas fa-headset";
    }
    judul = KarirConfigUtil.text(titleKey, judul);
    String isi = KarirConfigUtil.text(textKey, defaultText);
    String customFile = KarirConfigUtil.text(fileKey, "");
    String includePage = null;
    if (safeKarirJsp(customFile)) {
        try {
            String realPath = application.getRealPath(customFile);
            if (realPath != null && new File(realPath).isFile()) includePage = customFile;
        } catch(Exception e) { includePage = null; }
    }
%>
<% if (includePage != null) { %>
    <jsp:include page="<%=includePage%>"></jsp:include>
<% } else { %>
<section class="container py-5 karir-guide-section">
    <div class="karir-guide-hero p-4 p-lg-5 mb-4">
        <div class="d-flex flex-wrap align-items-center gap-3">
            <div class="karir-guide-icon"><i class="<%=pk(icon)%>"></i></div>
            <div>
                <span class="badge karir-badge-soft rounded-pill px-3 py-2 mb-2">Informasi Resmi KARIR</span>
                <h1 class="fw-black mb-2 karir-guide-title"><%=pk(judul)%></h1>
                <p class="text-muted mb-0"><%=pk(ringkas)%></p>
            </div>
        </div>
    </div>
    <div class="row g-4">
        <div class="col-lg-8">
            <article class="karir-guide-card p-4 p-lg-5">
                <div class="karir-guide-content"><%=pbr(isi)%></div>
            </article>
        </div>
        <div class="col-lg-4">
            <aside class="karir-guide-side p-4">
                <h5 class="fw-bold mb-3"><i class="fas fa-circle-info text-warning me-2"></i>Catatan Penting</h5>
                <p class="small text-white-50 lh-lg mb-3">Jika institusi belum membuat file JSP khusus untuk halaman ini, sistem menampilkan teks default dari konfigurasi. Admin dapat mengganti teks melalui tab Karir pada Konfigurasi.</p>
                <div class="d-grid gap-2">
                    <a class="btn btn-warning rounded-pill fw-bold" href="<%=request.getContextPath()%>/karir#lowongan"><i class="fas fa-briefcase me-2"></i>Lihat Lowongan</a>
                    <a class="btn btn-outline-light rounded-pill fw-bold" href="<%=request.getContextPath()%>/karir#pendaftaran"><i class="fas fa-user-plus me-2"></i>Mulai Pendaftaran</a>
                    <a class="btn btn-outline-light rounded-pill fw-bold" href="javascript:void(0)" onclick="karirOpenLoginModal()"><i class="fas fa-right-to-bracket me-2"></i>Login Pengumuman</a>
                </div>
            </aside>
        </div>
    </div>
</section>
<% } %>
