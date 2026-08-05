<%@page import="java.io.File"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.file.LampiranLain"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>


<%!
    private String nvl(String value, String defaultValue) {
        return value == null || value.trim().length() == 0 ? defaultValue : value.trim();
    }

    private String escapeHtml(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String paragraphHtml(String value) {
        String text = escapeHtml(value);
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] parts = text.split("\\n\\s*\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] == null ? "" : parts[i].trim();
            if (part.length() == 0) {
                continue;
            }
            sb.append("<p>").append(part.replace("\n", "<br>"));
            sb.append("</p>");
        }
        return sb.length() == 0 ? "<p>-</p>" : sb.toString();
    }

    private String defaultPanduanPendaftaranPmb() {
        return "Panduan Pendaftaran Lengkap PMB ini disusun untuk membantu calon mahasiswa memahami proses pendaftaran secara tertib, jelas, dan mudah diikuti. Pendaftaran mahasiswa baru adalah pintu awal untuk memasuki lingkungan pendidikan tinggi. Melalui proses ini, institusi mencatat identitas calon mahasiswa, pilihan program studi, jalur seleksi, kelengkapan dokumen, dan informasi pembayaran yang diperlukan untuk menentukan status pendaftaran. Karena itu, setiap calon mahasiswa perlu membaca petunjuk dengan teliti, mengisi data secara benar, dan mengikuti setiap batas waktu yang telah ditetapkan.\n\n"
            + "Sebelum melakukan pendaftaran, calon mahasiswa disarankan menyiapkan perangkat yang memadai, koneksi internet yang stabil, alamat email aktif, nomor telepon yang dapat dihubungi, dan dokumen dasar yang diperlukan. Gunakan browser yang umum dipakai dan pastikan halaman PMB yang dibuka adalah halaman resmi institusi. Hindari mengisi data melalui tautan yang tidak jelas. Jika menggunakan komputer umum, jangan lupa keluar dari akun setelah selesai. Kerahasiaan nomor registrasi, PIN, tanggal lahir, dan kata sandi harus dijaga dengan baik agar tidak disalahgunakan oleh pihak lain.\n\n"
            + "Langkah pertama adalah memilih gelombang atau jalur pendaftaran yang tersedia. Setiap gelombang dapat memiliki jadwal, kuota, biaya, dan ketentuan yang berbeda. Calon mahasiswa perlu memperhatikan tanggal pembukaan pendaftaran, batas akhir pengisian formulir, jadwal unggah berkas, jadwal seleksi, batas pembayaran, dan masa daftar ulang. Apabila terdapat beberapa jalur seleksi seperti reguler, prestasi, beasiswa, undangan, kerja sama, atau jalur khusus, pilih jalur yang paling sesuai dengan kondisi dan kemampuan. Jangan memilih jalur secara terburu-buru karena pilihan tersebut dapat memengaruhi persyaratan dokumen, proses seleksi, dan biaya yang harus dipenuhi.\n\n"
            + "Setelah memilih jalur, calon mahasiswa mengisi biodata. Data yang dimasukkan sebaiknya mengikuti dokumen resmi seperti kartu keluarga, kartu identitas, rapor, ijazah, atau surat keterangan lulus. Perhatikan penulisan nama lengkap, tempat lahir, tanggal lahir, alamat, nomor identitas, asal sekolah, data orang tua atau wali, nomor telepon, dan email. Kesalahan kecil pada nama atau tanggal lahir dapat berdampak pada kartu peserta, bukti pendaftaran, data akademik, dan dokumen lain pada tahap berikutnya. Sebelum menekan tombol simpan, baca kembali seluruh isian dan pastikan tidak ada kolom penting yang terlewat.\n\n"
            + "Tahap berikutnya adalah memilih program studi atau paket pendaftaran. Pilihan program studi sebaiknya disesuaikan dengan minat, kemampuan akademik, rencana karier, dan informasi kurikulum yang tersedia. Jika sistem menyediakan pilihan pertama dan pilihan kedua, susun pilihan sesuai prioritas. Pilihan pertama sebaiknya merupakan program studi yang paling diminati, sedangkan pilihan kedua dapat menjadi alternatif apabila hasil seleksi, kuota, atau kebijakan akademik mengarahkan calon mahasiswa pada pilihan lain. Calon mahasiswa juga dapat berdiskusi dengan orang tua, guru, atau panitia PMB sebelum menentukan pilihan akhir.\n\n"
            + "Setelah biodata dan pilihan program studi diisi, calon mahasiswa perlu mengunggah berkas. Dokumen yang umum diminta antara lain pas foto, kartu identitas, kartu keluarga, rapor, ijazah atau surat keterangan lulus, bukti prestasi, dan dokumen pendukung lain sesuai jalur pendaftaran. File harus jelas, tidak buram, tidak terpotong, dan sesuai format yang ditentukan. Jika berkas berupa foto, gunakan pencahayaan yang cukup dan posisi kamera yang lurus. Jika berkas berupa PDF, pastikan seluruh halaman dapat dibaca. Berkas yang tidak jelas dapat menyebabkan status verifikasi tertunda dan calon mahasiswa diminta mengunggah ulang.\n\n"
            + "Pembayaran pendaftaran atau daftar ulang harus dilakukan melalui kanal resmi yang diumumkan oleh institusi. Periksa nominal tagihan pada akun masing-masing karena jumlah biaya dapat berbeda berdasarkan gelombang, jalur, program studi, potongan, atau kebijakan tertentu. Simpan bukti pembayaran dengan baik. Apabila sistem meminta unggah bukti bayar, unggah bukti yang jelas dan sesuai transaksi. Jika pembayaran dilakukan melalui kanal otomatis, tunggu beberapa saat sampai status diperbarui. Apabila status belum berubah, hubungi panitia dengan menyertakan nomor registrasi, tanggal transaksi, nominal pembayaran, dan bukti transaksi.\n\n"
            + "Tahap seleksi dapat berupa ujian daring, wawancara, penilaian rapor, verifikasi dokumen, atau metode lain sesuai kebijakan institusi. Untuk ujian daring, gunakan perangkat yang siap, baterai cukup, jaringan stabil, dan lingkungan yang tenang. Bacalah instruksi sebelum mulai mengerjakan soal. Untuk wawancara, siapkan identitas diri, penjelasan mengenai minat kuliah, pengalaman belajar, prestasi, dan alasan memilih program studi. Hadir tepat waktu adalah bentuk kedisiplinan yang penting. Apabila mengalami kendala teknis, segera hubungi panitia melalui kontak resmi.\n\n"
            + "Setelah seleksi selesai, pantau pengumuman melalui akun PMB atau media resmi institusi. Calon mahasiswa yang dinyatakan lulus harus membaca instruksi daftar ulang dengan cermat. Tahap daftar ulang dapat mencakup pembayaran lanjutan, pengisian data tambahan, verifikasi dokumen asli, pencetakan bukti diterima, pengambilan nomor induk mahasiswa, atau kegiatan awal perkuliahan. Jangan menunda proses sampai mendekati batas akhir. Apabila terdapat data yang perlu diperbaiki, segera ajukan konfirmasi kepada panitia agar tidak menghambat proses administrasi.\n\n"
            + "Seluruh data yang diberikan dalam proses PMB harus benar dan dapat dipertanggungjawabkan. Institusi berhak melakukan verifikasi, meminta perbaikan, atau mengambil keputusan sesuai ketentuan apabila ditemukan ketidaksesuaian data. Calon mahasiswa diharapkan bersikap jujur, teliti, disiplin, dan aktif memantau informasi. Dengan mengikuti panduan ini, proses pendaftaran dapat berjalan lebih lancar, tertib, dan nyaman. PMB bukan hanya proses administrasi, tetapi juga langkah awal membangun sikap akademik yang bertanggung jawab dalam perjalanan pendidikan tinggi.";
    }

    private String defaultPersyaratanBerkasPmb() {
        return "Persyaratan kelengkapan berkas digunakan untuk memastikan bahwa data calon mahasiswa sesuai dengan dokumen resmi. Calon mahasiswa perlu menyiapkan pas foto terbaru, kartu identitas, kartu keluarga, rapor, ijazah atau surat keterangan lulus, serta dokumen lain sesuai jalur pendaftaran. Semua file harus jelas, tidak buram, tidak terpotong, dan memuat informasi penting secara lengkap. Nama, tempat lahir, tanggal lahir, dan asal sekolah sebaiknya sama dengan data yang diisikan pada formulir. Jika terdapat perbedaan penulisan, siapkan keterangan pendukung agar proses verifikasi dapat berjalan lancar. Berkas yang belum lengkap atau tidak terbaca dapat menyebabkan status pendaftaran tertunda. Panitia dapat meminta calon mahasiswa mengunggah ulang dokumen apabila format, ukuran, atau kualitas file belum sesuai. Simpan dokumen asli dengan baik karena dapat diminta pada saat daftar ulang atau verifikasi akhir.";
    }

    private String defaultBiayaPendidikanPmb() {
        return "Informasi biaya pendidikan membantu calon mahasiswa dan keluarga menyiapkan rencana pembiayaan sejak awal. Komponen biaya dapat meliputi biaya pendaftaran, biaya seleksi, biaya daftar ulang, biaya pengembangan, biaya perkuliahan, biaya praktikum, atau komponen lain sesuai kebijakan institusi. Nominal biaya dapat berbeda berdasarkan program studi, gelombang, jalur seleksi, sistem kuliah, potongan biaya, beasiswa, atau kerja sama tertentu. Calon mahasiswa dianjurkan membaca tagihan yang tampil pada akun masing-masing karena data tersebut biasanya sudah disesuaikan dengan pilihan pendaftaran. Pembayaran harus dilakukan melalui kanal resmi institusi. Simpan bukti pembayaran sampai status pada sistem berubah menjadi valid atau lunas. Apabila pembayaran belum terkonfirmasi, hubungi panitia dengan menyertakan nomor registrasi dan bukti transaksi.";
    }

    private String defaultFaqPmb() {
        return "Tanya jawab PMB membantu calon mahasiswa memahami kendala yang paling sering terjadi. Jika belum memiliki akun, gunakan menu pendaftaran dan ikuti langkah yang tersedia. Jika lupa nomor registrasi, periksa bukti pendaftaran, email, atau pesan resmi yang pernah diterima. Jika tidak dapat masuk ke akun, pastikan nomor registrasi, tanggal lahir, PIN, atau kata sandi sudah benar sesuai petunjuk. Jika berkas gagal diunggah, periksa format, ukuran, dan kualitas file. Jika status pembayaran belum berubah, tunggu proses validasi atau hubungi panitia melalui kontak resmi. Jika ingin mengubah pilihan program studi, perhatikan ketentuan yang berlaku karena perubahan dapat dibatasi setelah pembayaran, verifikasi, atau seleksi dilakukan. Jika dinyatakan lulus, segera baca instruksi daftar ulang dan selesaikan kewajiban sebelum batas waktu berakhir.";
    }

    private String configDefaultText(String jenisKonten) {
        String key = "pmb_footer_panduan_default_text";
        String defaultValue = defaultPanduanPendaftaranPmb();
        if ("berkas".equalsIgnoreCase(jenisKonten)) {
            key = "pmb_footer_berkas_default_text";
            defaultValue = defaultPersyaratanBerkasPmb();
        } else if ("biaya".equalsIgnoreCase(jenisKonten)) {
            key = "pmb_footer_biaya_default_text";
            defaultValue = defaultBiayaPendidikanPmb();
        } else if ("faq".equalsIgnoreCase(jenisKonten)) {
            key = "pmb_footer_faq_default_text";
            defaultValue = defaultFaqPmb();
        }
        try {
            return nvl(Common.getKonfigurasi(key, defaultValue).getNilai(), defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
%>

<%
    // =========================================================================
    // 1. INISIALISASI VARIABEL & PENANGKAPAN PARAMETER
    // =========================================================================
    String rnd = Common.getGeneratedBarCode(7);
    
    // Menangkap parameter jenis dokumen dari URL
    String jenisKonten = request.getParameter("jenis");
    if (jenisKonten == null || jenisKonten.trim().isEmpty()) {
        jenisKonten = "alur"; // Nilai bawaan
    }

    String judulHalaman = Common.getBahasaConfig("Informasi Pendaftaran");
    String src = "";
    boolean isImage = false;
    boolean isIframe = false;
    boolean redirectBlank = false;
    boolean notFound = false;
    String gdriveUrl = null;
    String docViewerUrl = null;
    String errorMessage = null;
    String defaultInfoText = null;

    Session sessionLocal = null;
    try {
        // Buka Session Database
        sessionLocal = HibernateUtil.openSession();
        LampiranLain fileLampiran = null;

        // =====================================================================
        // 2. LOGIKA PEMILIHAN DOKUMEN BERDASARKAN PARAMETER
        // =====================================================================
        if (jenisKonten.equalsIgnoreCase("panduan")) {
            judulHalaman = Common.getBahasaConfig("Panduan Pendaftaran Lengkap");
            fileLampiran = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB, "PANDUAN_PMB");
            
        } else if (jenisKonten.equalsIgnoreCase("berkas")) {
            judulHalaman = Common.getBahasaConfig("Persyaratan Kelengkapan Berkas");
            fileLampiran = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB, "BERKAS_PMB");
            
        } else if (jenisKonten.equalsIgnoreCase("biaya")) {
            judulHalaman = Common.getBahasaConfig("Informasi Biaya Pendidikan");
            fileLampiran = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB, "BIAYA_PMB");
            
        } else if (jenisKonten.equalsIgnoreCase("faq")) {
            judulHalaman = Common.getBahasaConfig("Tanya Jawab (FAQ)");
            fileLampiran = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB, "FAQ_PMB");
            
        } else {
            // Default: Alur Pendaftaran
            judulHalaman = Common.getBahasaConfig("Alur Pendaftaran Mahasiswa Baru");
            fileLampiran = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB, LampiranLain.ALUR_REGISTRASI_PMB);
        }

        // =====================================================================
        // 3. LOGIKA PENANGANAN FILE
        // =====================================================================
        if (fileLampiran == null) {
            // Jika file tidak ditemukan di database
            if (jenisKonten.equalsIgnoreCase("alur")) {
                // Backward compatibility untuk file alur lama berbentuk gambar statis
                src = Common.ROOT + "/img/Alur_SPMB_Mandiri.jpg";
                isImage = true;
            } else {
                notFound = true;
                defaultInfoText = configDefaultText(jenisKonten);
            }
        } else {
            // Jika file ditemukan, proses URL-nya
            String link = fileLampiran.getLink();
            if (link == null || link.trim().isEmpty() || !link.startsWith("http")) {
                link = fileLampiran.createLinkUri();
            }
            
            src = link;
            if (src == null || src.trim().length() == 0) {
                notFound = true;
                defaultInfoText = configDefaultText(jenisKonten);
            } else if (fileLampiran.bisaPreview()) {
                String lowerSrc = (src != null) ? src.trim().toLowerCase() : "";
                
                // Penanganan Khusus Tipe File untuk Preview yang Optimal
                if (fileLampiran.getGdrive() != null && !fileLampiran.getGdrive().trim().isEmpty()) {
                    gdriveUrl = "https://drive.google.com/file/d/" + fileLampiran.getGdrive() + "/preview";
                    isIframe = true;
                } else if (lowerSrc.endsWith(".pdf")) {
                    isIframe = true;
                } else if (fileLampiran.merupakanGambar()) {
                    isImage = true;
                } else {
                    // Penanganan dokumen Microsoft Office melalui Google Doc Viewer
                    boolean isDoc = lowerSrc.endsWith(".doc") || lowerSrc.endsWith(".docx") || 
                                    lowerSrc.endsWith(".xls") || lowerSrc.endsWith(".xlsx") || 
                                    lowerSrc.endsWith(".ppt") || lowerSrc.endsWith(".pptx");
                                    
                    if (lowerSrc.startsWith("http") && isDoc) {
                        docViewerUrl = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(src, "UTF-8");
                        isIframe = true;
                    } else {
                        isIframe = true; // Fallback iframe normal
                    }
                }
            } else {
                redirectBlank = true; // Jika format tidak didukung preview, paksa buka tab baru
            }
        }
    } catch(Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_alur_pendaftaran.jsp:194");
        errorMessage = Common.getBahasaConfig("Terjadi kesalahan sistem saat memuat dokumen informasi. Silakan coba kembali beberapa saat lagi.");
    } finally {
        if (sessionLocal != null) {
            try { sessionLocal.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_alur_pendaftaran.jsp:198");}
            try { sessionLocal.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_alur_pendaftaran.jsp:199");}
            try { sessionLocal.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_alur_pendaftaran.jsp:200");}
        }
    }
%>

<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= judulHalaman %></title>
    
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    
    <style>
        html, body { 
            margin: 0; padding: 0; width: 100%; height: 100%; 
            background-color: #f8f9fa; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            overflow: hidden;
        }
        .iframe-wrapper-<%=rnd%> { 
            width: 100%; height: 100%; border: none; 
        }
        .img-wrapper-<%=rnd%> { 
            width: 100%; height: 100%; display: flex; 
            justify-content: center; align-items: center; 
            overflow: auto; padding: 1.5rem; background-color: #e9ecef;
        }
        .img-wrapper-<%=rnd%> img { 
            max-width: 100%; height: auto; 
            box-shadow: 0 10px 25px rgba(0,0,0,0.15); 
            border-radius: 0.75rem; background-color: #fff;
        }

        .guide-wrapper-<%=rnd%> {
            height: 100%; overflow: auto; padding: 2rem 1rem; background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
        }
        .guide-card-<%=rnd%> {
            max-width: 980px; margin: 0 auto; border: 0; border-radius: 1.25rem; box-shadow: 0 18px 45px rgba(15, 23, 42, 0.12);
        }
        .guide-content-<%=rnd%> p {
            font-size: 1rem; line-height: 1.85; color: #334155; margin-bottom: 1.15rem; text-align: justify;
        }
        .guide-note-<%=rnd%> {
            background: #fff7ed; border: 1px solid #fed7aa; color: #9a3412; border-radius: 1rem;
        }
        .empty-state-icon {
            width: 90px; height: 90px;
            background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
            display: inline-flex; align-items: center; justify-content: center;
            border-radius: 50%; border: 3px solid #dee2e6;
        }
    </style>
</head>
<body>

    <% if (errorMessage != null) { %>
        <div class="d-flex h-100 align-items-center justify-content-center">
            <div class="card shadow-sm border-0 text-center p-4 rounded-4" style="max-width: 450px;">
                <div class="card-body">
                    <i class="fas fa-triangle-exclamation text-danger mb-3" style="font-size: 3.5rem;"></i>
                    <h5 class="fw-bold text-dark"><%= Common.getBahasaConfig("Gagal Memuat Dokumen") %></h5>
                    <p class="text-muted small mb-0"><%= errorMessage %></p>
                </div>
            </div>
        </div>

    <% } else if (notFound) { %>
        <div class="guide-wrapper-<%=rnd%>">
            <div class="card guide-card-<%=rnd%>">
                <div class="card-header bg-white border-0 p-4 p-md-5 pb-md-3">
                    <div class="d-flex flex-column flex-md-row align-items-md-center gap-3">
                        <div class="empty-state-icon shadow-sm flex-shrink-0">
                            <i class="fas fa-book-open text-primary" style="font-size: 2.25rem;"></i>
                        </div>
                        <div>
                            <div class="badge bg-primary-subtle text-primary rounded-pill px-3 py-2 mb-2 fw-bold">
                                <i class="fas fa-circle-info me-2"></i><%= Common.getBahasaConfig("Panduan Default") %>
                            </div>
                            <h3 class="fw-bold text-dark mb-2"><%= judulHalaman %></h3>
                            <p class="text-muted mb-0">
                                <%= Common.getBahasaConfig("Dokumen resmi belum diunggah oleh administrator. Informasi berikut ditampilkan sebagai panduan sementara dan dapat diubah melalui Konfigurasi PMB.") %>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="card-body p-4 p-md-5 pt-md-3">
                    <div class="guide-note-<%=rnd%> p-3 mb-4 small">
                        <i class="fas fa-triangle-exclamation me-2"></i>
                        <%= Common.getBahasaConfig("Gunakan informasi ini sebagai acuan awal. Jika ada perbedaan dengan pengumuman resmi panitia, ikuti pengumuman resmi terbaru dari institusi.") %>
                    </div>
                    <div class="guide-content-<%=rnd%>">
                        <%= paragraphHtml(defaultInfoText) %>
                    </div>
                    <div class="text-center mt-4">
                        <button class="btn btn-primary rounded-pill px-4 fw-bold shadow-sm" onclick="tutupModalParent()">
                            <i class="fas fa-check me-2"></i><%= Common.getBahasaConfig("Saya Mengerti") %>
                        </button>
                    </div>
                </div>
            </div>
        </div>

    <% } else if (redirectBlank) { %>
        <div class="d-flex h-100 align-items-center justify-content-center">
            <div class="card shadow-sm border-0 text-center p-4 bg-white" style="max-width: 450px; border-radius: 1rem;">
                <div class="card-body">
                    <div class="spinner-border text-primary mb-3" role="status" style="width: 3rem; height: 3rem;"></div>
                    <h5 class="fw-bold text-dark mb-2"><%= Common.getBahasaConfig("Membuka Dokumen") %></h5>
                    <p class="text-muted small mb-4"><%= Common.getBahasaConfig("Dokumen sedang dibuka pada tab peramban (browser) baru. Harap tunggu sesaat.") %></p>
                    
                    <div class="alert alert-info small border-0 bg-light rounded-3 text-start mb-4">
                        <i class="fas fa-circle-info me-2 text-info"></i><%= Common.getBahasaConfig("Jika dokumen tidak terbuka secara otomatis akibat pemblokiran popup, silakan klik tombol di bawah ini.") %>
                    </div>

                    <button class="btn btn-primary rounded-pill px-5 fw-bold" onclick="window.open('<%= src %>', '_blank'); tutupModalParent();">
                        <i class="fas fa-up-right-from-square me-2"></i><%= Common.getBahasaConfig("Buka Dokumen Manual") %>
                    </button>
                </div>
            </div>
        </div>
        
        <script>
            // Otomatis membuka tab baru lalu menutup modal
            setTimeout(() => {
                window.open('<%= src %>', '_blank');
                tutupModalParent();
            }, 800);
        </script>

    <% } else if (gdriveUrl != null) { %>
        <iframe class="iframe-wrapper-<%=rnd%>" src="<%= gdriveUrl %>" allowfullscreen></iframe>

    <% } else if (docViewerUrl != null) { %>
        <iframe class="iframe-wrapper-<%=rnd%>" src="<%= docViewerUrl %>" allowfullscreen></iframe>

    <% } else if (isImage) { %>
        <div class="img-wrapper-<%=rnd%>">
            <img src="<%= src %>" alt="<%= judulHalaman %>">
        </div>

    <% } else if (isIframe) { %>
        <iframe class="iframe-wrapper-<%=rnd%>" src="<%= src %>" allowfullscreen></iframe>
    <% } %>

    <script>
        function tutupModalParent() {
            try {
                if (window.parent && window.parent.document) {
                    // Target akurat: Mencari tombol yang memiliki atribut penutup modal pada modal yang sedang tampil
                    var closeBtn = window.parent.document.querySelector('.modal.show [data-bs-dismiss="modal"]');
                    
                    if (closeBtn) {
                        closeBtn.click();
                    } else {
                        // Fallback: Jika modal sedang animasi, cari sembarang tombol close di parent
                        var fallbackBtn = window.parent.document.querySelector('.btn-close');
                        if (fallbackBtn) fallbackBtn.click();
                    }
                }
            } catch(e) {
                console.error("Gagal menutup modal: " + e.message);
            }
        }
    </script>
</body>
</html>