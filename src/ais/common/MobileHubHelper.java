package ais.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * <b>MobileHubHelper</b> &mdash; Kelas utilitas terpusat untuk membangun halaman hub modul
 * pada antarmuka mobile AIS eCampus.
 *
 * <hr>
 * <h2>Latar Belakang dan Tujuan</h2>
 * <p>
 * Sistem AIS eCampus pada awalnya dikembangkan dengan antarmuka berbasis ZK (ZUL) yang dirancang
 * untuk tampilan desktop. Seiring kebutuhan pengguna yang semakin mobile-centric, dikembangkanlah
 * lapisan antarmuka baru berbasis Bootstrap 5 dan JSP yang dapat diakses dari perangkat mobile
 * maupun desktop tanpa memerlukan perubahan pada logika bisnis inti.
 * </p>
 * <p>
 * Kelas {@code MobileHubHelper} hadir sebagai fondasi arsitektur mobile AIS, menyediakan
 * serangkaian metode statis yang dapat dipanggil dari halaman JSP untuk menghasilkan komponen
 * UI yang konsisten, responsif, dan mudah dirawat. Dengan pendekatan ini, setiap halaman hub
 * tidak perlu menduplikasi logika rendering HTML &mdash; cukup memanggil metode-metode yang
 * tersedia di kelas ini.
 * </p>
 *
 * <hr>
 * <h2>Arsitektur Halaman Hub Mobile</h2>
 * <p>
 * Setiap modul besar (Kepegawaian, Akuntansi, Aset, Koperasi, Antar Jemput, dll) memiliki
 * sebuah "halaman hub" ({@code /WEB-INF/baru/modul/[modul]/index.jsp}) yang berfungsi sebagai
 * pintu masuk navigasi ke sub-modul di dalamnya. Halaman hub ini dapat dibuka dalam dua mode:
 * </p>
 * <ul>
 *   <li><b>Mode Desktop</b>: Dibuka langsung via browser, menampilkan kartu navigasi dengan
 *       lebar penuh dan elemen dekoratif.</li>
 *   <li><b>Mode Mobile ({@code _mob=1})</b>: Dibuka di dalam overlay iframe milik mobile SPA
 *       ({@code /WEB-INF/baru/modul/mobile/index.jsp}). Dalam mode ini, klik pada kartu navigasi
 *       akan meneruskan sinyal ke parent frame melalui {@code window.parent.openOverlay()}, sehingga
 *       sub-modul terbuka dalam layer overlay baru tanpa reload halaman.</li>
 * </ul>
 *
 * <hr>
 * <h2>Fitur Utama</h2>
 * <ul>
 *   <li><b>Autentikasi terpadu</b>: Metode {@link #checkAuth} memeriksa sesi pengguna dan
 *       mengembalikan objek {@link Tbmuser} yang terautentikasi. Jika sesi tidak valid, respons
 *       akan diarahkan ke halaman login.</li>
 *   <li><b>Kartu navigasi responsif</b>: Metode {@link #buildCard} menghasilkan elemen HTML
 *       kartu Bootstrap 5 dengan ikon FontAwesome, warna latar aksen, label, dan deskripsi singkat.
 *       Kartu ini responsif: 2 kolom di mobile, 3 kolom di tablet, 4 kolom di desktop.</li>
 *   <li><b>Header modul konsisten</b>: Metode {@link #buildHeader} menghasilkan header berwarna
 *       gradien yang mencantumkan ikon modul, judul, dan subjudul. Tampilan ini seragam di
 *       seluruh halaman hub.</li>
 *   <li><b>Intersepsi klik mobile-aware</b>: Metode {@link #buildMobScript} menghasilkan blok
 *       JavaScript yang memasang event listener global. Setiap klik pada tautan berpola
 *       {@code /baru?} akan dicegat dan diteruskan ke {@code window.parent.openOverlay()},
 *       sehingga navigasi terasa mulus tanpa reload penuh.</li>
 *   <li><b>CSS sentuh untuk mobile</b>: Metode {@link #buildMobCss} menghasilkan deklarasi CSS
 *       yang memperbaiki umpan balik sentuh pada kartu (scale down saat disentuh, hapus
 *       highlight bawaan browser).</li>
 *   <li><b>Konversi URL ZUL ke Pagesmaster</b>: Metode {@link #zulToPagesmasterUrl} mengubah
 *       jalur ZUL (misal {@code /pages/master/antarjemput/panel_kendaraan.zul}) menjadi URL
 *       pagesmaster yang dapat diakses via JSP
 *       (misal {@code /baru?p=pagesmasterantarjemputpanelkendaraanzul}).</li>
 * </ul>
 *
 * <hr>
 * <h2>Panduan Penggunaan di JSP</h2>
 * <pre>{@code
 * <%@ page import="ais.common.MobileHubHelper, ais.common.Common" %>
 * <%@ page import="ais.database.model.Tbmuser" %>
 * <%
 *     Tbmuser user = MobileHubHelper.checkAuth(request, response);
 *     if (user == null) return;
 *     String ctx = request.getContextPath();
 *     boolean isMob = MobileHubHelper.isMobile(request);
 * %>
 * <% if (isMob) { %><%= MobileHubHelper.buildMobCss() %><% } %>
 * <%= MobileHubHelper.buildHeader("Modul Saya","Deskripsi singkat","fa-folder","#3b82f6,#1d4ed8") %>
 * <div class="row g-3">
 *     <%=MobileHubHelper.buildCard("Sub Modul A", ctx+"/baru?p=pagesmasterxxx","fa-file","#3b82f6","Keterangan")%>
 * </div>
 * <% if (isMob) { %><%= MobileHubHelper.buildMobScript(".fw-semibold") %><% } %>
 * }</pre>
 *
 * <hr>
 * <h2>Aturan Pengelolaan Sesi Hibernate</h2>
 * <p>
 * Kelas ini <b>tidak membuka sesi Hibernate secara langsung</b> karena tidak memerlukan akses
 * basis data. Otentikasi dilakukan melalui sesi HTTP (misal {@link Common#getCurrentUser})
 * yang sudah mengelola sesi Hibernate secara internal. Jika subkelas atau kode pemanggil
 * memerlukan sesi Hibernate, aturan berikut berlaku:
 * </p>
 * <ul>
 *   <li>Jika menggunakan {@code HibernateUtil.openSession()} atau
 *       {@code HibernateUtil.currentNativeSession()}: <b>WAJIB</b> ditutup di blok
 *       {@code finally} dengan {@code HibernateUtil.closeSessionQuietly(session)}.</li>
 *   <li>Jika menggunakan {@code HibernateUtil.currentSession()} (sesi ZK): <b>JANGAN</b>
 *       ditutup secara manual &mdash; sesi ini dikelola oleh container ZK dan akan ditutup
 *       otomatis.</li>
 * </ul>
 *
 * <hr>
 * <h2>Pertimbangan Threading dan Memori</h2>
 * <p>
 * Seluruh metode di kelas ini bersifat <b>stateless dan thread-safe</b>. Tidak ada state
 * instance yang disimpan &mdash; semua parameter masuk melalui argumen metode dan keluaran
 * berupa objek {@link String} baru yang tidak berbagi referensi antar thread. Karena
 * menggunakan {@link StringBuilder} untuk konstruksi string panjang, konsumsi memori dapat
 * diestimasi secara deterministik berdasarkan jumlah kartu yang dirender.
 * </p>
 * <p>
 * Untuk efisiensi memori, disarankan agar halaman hub tidak memuat data dari basis data
 * secara langsung. Jika diperlukan data dinamis (misalnya jumlah entitas), gunakan panggilan
 * AJAX terpisah agar render awal halaman tetap cepat.
 * </p>
 *
 * <hr>
 * <h2>Kompatibilitas</h2>
 * <p>
 * Kelas ini dikompilasi dengan target <b>Java 1.7</b> (--release 8 sumber, v52 bytecode)
 * melalui Ant build. Tidak menggunakan fitur Java 8 (lambda, stream, method reference,
 * try-with-resources, diamond operator pada tipe generik, atau default method pada interface).
 * Penggunaan generik dibatasi pada deklarasi eksplisit yang kompatibel dengan javac 1.6-source.
 * </p>
 *
 * @author   Tim Pengembang AIS eCampus
 * @version  2.0
 * @since    2026-07-15
 * @see      Common
 * @see      Tbmuser
 * @see      Tbmrole
 */
public final class MobileHubHelper {

    /** Kolom Bootstrap untuk kartu di layar mobile (xs). */
    public static final String COL_MOBILE  = "col-6";
    /** Kolom Bootstrap untuk kartu di layar tablet (md). */
    public static final String COL_TABLET  = "col-md-4";
    /** Kolom Bootstrap untuk kartu di layar desktop (lg). */
    public static final String COL_DESKTOP = "col-lg-3";

    /**
     * Kelas kolom Bootstrap default untuk kartu navigasi hub.
     * Menghasilkan tata letak 2 kolom di mobile, 3 di tablet, 4 di desktop.
     */
    public static final String COL_DEFAULT = COL_MOBILE + " " + COL_TABLET + " " + COL_DESKTOP;

    /* -------------------------------------------------------------------------
     * Utilitas Autentikasi
     * ---------------------------------------------------------------------- */

    /**
     * Memeriksa sesi HTTP dan mengembalikan pengguna yang sedang login.
     *
     * <p>Metode ini menggunakan {@link Common#getCurrentUser(HttpServletRequest)} untuk
     * mendapatkan pengguna dari sesi. Jika pengguna belum login atau sesi telah kedaluwarsa,
     * metode ini akan mengirimkan respons error 403 dan mengembalikan {@code null}.
     * Kode pemanggil <b>wajib</b> memeriksa nilai kembalian dan segera menghentikan
     * pemrosesan halaman jika {@code null} dengan menjalankan {@code return;} di JSP.</p>
     *
     * <p>Kelas ini tidak membuka sesi Hibernate. Sesi Hibernate dikelola secara internal
     * oleh {@link Common#getCurrentUser}. Pemanggil tidak perlu menutup sesi apa pun
     * setelah memanggil metode ini.</p>
     *
     * @param request  Objek {@link HttpServletRequest} dari halaman JSP.
     * @param response Objek {@link HttpServletResponse} dari halaman JSP.
     * @return Objek {@link Tbmuser} yang terautentikasi dan aktif, atau {@code null}
     *         jika sesi tidak valid (respons 403 sudah dikirimkan).
     * @throws IOException Jika terjadi kesalahan saat mengirimkan respons error.
     */
    public static Tbmuser checkAuth(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null || !Boolean.TRUE.equals(user.getAktif())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Sesi tidak valid atau pengguna tidak aktif.");
            return null;
        }
        return user;
    }

    /**
     * Mendeteksi apakah halaman sedang dibuka dalam mode mobile overlay.
     *
     * <p>Halaman hub dibuka dalam mode mobile ketika dimuat di dalam iframe overlay
     * milik mobile SPA dengan parameter {@code _mob=1}. Dalam mode ini, tampilan
     * perlu disesuaikan: menghilangkan elemen yang tidak relevan di mobile,
     * mengaktifkan intersepsi klik, dan menyesuaikan dimensi kartu.</p>
     *
     * @param request Objek {@link HttpServletRequest} dari halaman JSP.
     * @return {@code true} jika parameter {@code _mob} bernilai {@code "1"},
     *         {@code false} untuk kondisi lainnya.
     */
    public static boolean isMobile(HttpServletRequest request) {
        return "1".equals(request.getParameter("_mob"));
    }

    /* -------------------------------------------------------------------------
     * Komponen HTML: Header
     * ---------------------------------------------------------------------- */

    /**
     * Membangun HTML blok header halaman hub modul.
     *
     * <p>Header menampilkan ikon berbentuk lingkaran dengan gradien latar, nama modul sebagai
     * judul tebal, dan subjudul deskripsi singkat. Elemen ini selalu tampil di bagian atas
     * halaman hub, baik dalam mode desktop maupun mobile.</p>
     *
     * <p>Parameter {@code gradientCss} harus berupa nilai CSS yang valid untuk properti
     * {@code background}, misalnya {@code "linear-gradient(135deg,#3b82f6,#1d4ed8)"}.
     * Jika {@code null} atau kosong, gradien biru default akan digunakan.</p>
     *
     * @param title       Judul modul (contoh: "Antar Jemput").
     * @param subtitle    Subjudul deskripsi singkat (contoh: "Manajemen Transportasi & Antar Jemput").
     * @param faIcon      Kelas ikon FontAwesome tanpa prefix {@code fa-} (contoh: "bus").
     * @param gradientCss CSS gradien untuk latar lingkaran ikon (contoh: "linear-gradient(135deg,#6366f1,#4f46e5)").
     * @return String HTML blok header yang sudah siap di-render di JSP.
     */
    public static String buildHeader(String title, String subtitle, String faIcon, String gradientCss) {
        if (gradientCss == null || gradientCss.trim().length() == 0) {
            gradientCss = "linear-gradient(135deg,#3b82f6,#1d4ed8)";
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append("<div class=\"container-fluid py-3\">");
        sb.append("<div class=\"d-flex align-items-center mb-3 gap-2\">");
        sb.append("<div class=\"rounded-circle d-flex align-items-center justify-content-center\" ");
        sb.append("style=\"width:38px;height:38px;background:").append(esc(gradientCss)).append(";flex-shrink:0;\">");
        sb.append("<i class=\"fas fa-").append(esc(faIcon)).append(" text-white\" style=\"font-size:16px;\"></i>");
        sb.append("</div>");
        sb.append("<div>");
        sb.append("<h6 class=\"mb-0 fw-bold\">").append(esc(title)).append("</h6>");
        sb.append("<small class=\"text-muted\">").append(esc(subtitle)).append("</small>");
        sb.append("</div></div>");
        return sb.toString();
    }

    /* -------------------------------------------------------------------------
     * Komponen HTML: Kartu Navigasi
     * ---------------------------------------------------------------------- */

    /**
     * Membangun HTML satu kartu navigasi untuk sub-modul dalam halaman hub.
     *
     * <p>Kartu yang dihasilkan berupa elemen Bootstrap {@code .card} yang dapat diklik,
     * menampilkan ikon berwarna, label tebal, dan deskripsi singkat. Setiap kartu dibungkus
     * dalam {@code <a>} dengan {@code href} menuju URL sub-modul yang diberikan. Kelas kolom
     * default ({@link #COL_DEFAULT}) menghasilkan tata letak responsif: 2 kartu per baris di
     * mobile, 3 di tablet, 4 di desktop.</p>
     *
     * <p>Parameter {@code accentHex} adalah warna heksadesimal aksen (misal {@code "#3b82f6"})
     * yang digunakan sebagai warna ikon dan latar transparansi ({@code color + "20"}).
     * Format CSS ini kompatibel dengan semua browser modern.</p>
     *
     * @param label     Label teks kartu yang tampil sebagai nama sub-modul.
     * @param url       URL lengkap tujuan kartu (termasuk konteks path, contoh:
     *                  {@code "/ais/baru?p=pagesmasterantarjemputpanelkendaraanzul"}).
     * @param faIcon    Kelas ikon FontAwesome tanpa prefix {@code fa-} (contoh: "van-shuttle").
     * @param accentHex Warna heksadesimal CSS untuk aksen ikon (contoh: {@code "#f59e0b"}).
     * @param desc      Deskripsi singkat yang tampil di bawah label (1 kalimat pendek).
     * @return String HTML satu unit kolom berisi kartu navigasi, siap di-render dalam
     *         wadah {@code <div class="row g-3">}.
     */
    public static String buildCard(String label, String url, String faIcon, String accentHex, String desc) {
        return buildCard(label, url, faIcon, accentHex, desc, COL_DEFAULT);
    }

    /**
     * Membangun HTML satu kartu navigasi dengan kelas kolom Bootstrap yang dapat dikustomisasi.
     *
     * <p>Versi ini menerima parameter {@code colClass} sehingga pemanggil dapat menentukan
     * tata letak kolom sesuai kebutuhan (misal {@code "col-12 col-md-6"} untuk layout 2 kolom
     * di semua ukuran layar, atau {@code "col-12"} untuk kartu lebar penuh).</p>
     *
     * @param label    Label teks kartu.
     * @param url      URL lengkap tujuan kartu.
     * @param faIcon   Kelas ikon FontAwesome (tanpa prefix {@code fa-}).
     * @param accentHex Warna heksadesimal aksen.
     * @param desc     Deskripsi singkat.
     * @param colClass Kelas kolom Bootstrap (misal {@code "col-6 col-md-4 col-lg-3"}).
     * @return String HTML satu unit kolom berisi kartu.
     */
    public static String buildCard(String label, String url, String faIcon, String accentHex, String desc, String colClass) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<div class=\"").append(esc(colClass)).append("\">");
        sb.append("<a href=\"").append(esc(url)).append("\" class=\"text-decoration-none\">");
        sb.append("<div class=\"card border-0 shadow-sm h-100 rounded-3\" style=\"cursor:pointer;\">");
        sb.append("<div class=\"card-body p-3\">");
        sb.append("<div class=\"mb-2\">");
        sb.append("<span class=\"rounded-circle d-inline-flex align-items-center justify-content-center\" ");
        sb.append("style=\"width:36px;height:36px;background:").append(esc(accentHex)).append("20;\">");
        sb.append("<i class=\"fas fa-").append(esc(faIcon)).append("\" ");
        sb.append("style=\"color:").append(esc(accentHex)).append(";font-size:14px;\"></i>");
        sb.append("</span></div>");
        sb.append("<div class=\"fw-semibold text-dark\" style=\"font-size:13px;\">").append(esc(label)).append("</div>");
        sb.append("<div class=\"text-muted\" style=\"font-size:11px;line-height:1.3;\">").append(esc(desc)).append("</div>");
        sb.append("</div></div></a></div>");
        return sb.toString();
    }

    /**
     * Membangun HTML kartu navigasi lebar penuh ({@code col-12}) dengan tampilan horizontal.
     *
     * <p>Digunakan untuk kartu yang memerlukan lebih banyak konteks teks, seperti kartu
     * dashboard utama atau item navigasi dalam daftar vertikal di mobile. Ikon dan teks
     * ditampilkan berdampingan (flexbox horizontal).</p>
     *
     * @param label    Label teks kartu.
     * @param url      URL lengkap tujuan kartu.
     * @param faIcon   Kelas ikon FontAwesome (tanpa prefix {@code fa-}).
     * @param accentHex Warna heksadesimal aksen.
     * @param desc     Deskripsi singkat.
     * @param borderSide Sisi border Bootstrap {@code border-start} warna (misal "primary","success","warning").
     * @return String HTML kartu navigasi horizontal.
     */
    public static String buildCardHorizontal(String label, String url, String faIcon,
            String accentHex, String desc, String borderSide) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<div class=\"col-12 col-md-6\">");
        sb.append("<a href=\"").append(esc(url)).append("\" class=\"text-decoration-none\">");
        sb.append("<div class=\"card border-0 shadow-sm rounded-3 border-start border-").append(esc(borderSide)).append(" border-3\" style=\"cursor:pointer;\">");
        sb.append("<div class=\"card-body p-3\">");
        sb.append("<div class=\"d-flex align-items-center gap-3\">");
        sb.append("<span class=\"rounded-circle d-inline-flex align-items-center justify-content-center\" ");
        sb.append("style=\"width:42px;height:42px;background:").append(esc(accentHex)).append("20;flex-shrink:0;\">");
        sb.append("<i class=\"fas fa-").append(esc(faIcon)).append("\" ");
        sb.append("style=\"color:").append(esc(accentHex)).append(";font-size:18px;\"></i>");
        sb.append("</span>");
        sb.append("<div>");
        sb.append("<div class=\"fw-semibold text-dark\">").append(esc(label)).append("</div>");
        sb.append("<div class=\"text-muted\" style=\"font-size:12px;\">").append(esc(desc)).append("</div>");
        sb.append("</div></div></div></div></a></div>");
        return sb.toString();
    }

    /* -------------------------------------------------------------------------
     * Komponen HTML: Baris Kartu
     * ---------------------------------------------------------------------- */

    /**
     * Membangun HTML elemen pembuka {@code <div class="row g-3">} dan pembukaannya.
     *
     * <p>Digunakan bersama {@link #buildCard} dan {@link #closeRow} untuk membangun
     * sekumpulan kartu navigasi yang rapi. Contoh pola penggunaan:</p>
     * <pre>{@code
     * out.println(MobileHubHelper.openRow());
     * out.println(MobileHubHelper.buildCard(...));
     * out.println(MobileHubHelper.buildCard(...));
     * out.println(MobileHubHelper.closeRow());
     * }</pre>
     *
     * @return String HTML {@code <div class="row g-3">}.
     */
    public static String openRow() {
        return "<div class=\"row g-3\">";
    }

    /**
     * Membangun HTML penutup elemen baris ({@code </div>}).
     *
     * @return String HTML {@code </div>}.
     */
    public static String closeRow() {
        return "</div>";
    }

    /* -------------------------------------------------------------------------
     * Komponen HTML: Pesan Info / Alert
     * ---------------------------------------------------------------------- */

    /**
     * Membangun HTML blok alert informasi ({@code alert-info}) dengan ikon info.
     *
     * <p>Digunakan untuk menampilkan catatan penting, panduan penggunaan, atau
     * informasi kontekstual di bagian bawah halaman hub.</p>
     *
     * @param message Teks pesan yang ingin ditampilkan.
     * @return String HTML blok alert Bootstrap.
     */
    public static String buildInfoAlert(String message) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("<div class=\"alert alert-info mt-3 rounded-3\" style=\"font-size:12px;\">");
        sb.append("<i class=\"fas fa-info-circle me-2\"></i>");
        sb.append(esc(message));
        sb.append("</div>");
        return sb.toString();
    }

    /* -------------------------------------------------------------------------
     * Komponen JavaScript dan CSS Mobile
     * ---------------------------------------------------------------------- */

    /**
     * Membangun blok CSS khusus untuk pengalaman sentuh yang lebih baik di perangkat mobile.
     *
     * <p>CSS ini menghilangkan efek highlight bawaan browser saat kartu disentuh,
     * menambahkan animasi scale-down halus saat kartu ditekan, dan memastikan cursor
     * berubah menjadi pointer. Blok ini <b>hanya perlu disertakan</b> ketika
     * {@link #isMobile(HttpServletRequest)} mengembalikan {@code true}.</p>
     *
     * @return String HTML blok {@code <style>} CSS yang siap dimasukkan ke dalam halaman.
     */
    public static String buildMobCss() {
        return "<style>\n" +
            ".card{-webkit-tap-highlight-color:transparent;transition:transform .12s;cursor:pointer;}\n" +
            ".card:active{transform:scale(.96);}\n" +
            ".card-body{min-height:60px;}\n" +
            "</style>\n";
    }

    /**
     * Membangun blok JavaScript intersepsi klik untuk navigasi mobile.
     *
     * <p>Script ini memasang satu event listener global ({@code click}) pada {@code document}.
     * Saat pengguna mengklik elemen apa pun di halaman, script menelusuri DOM ke atas
     * mencari elemen {@code <a>} terdekat. Jika tautan tersebut mengarah ke URL pola
     * {@code /baru?}, klik dicegat dan dialihkan ke fungsi {@code window.parent.openOverlay()}.
     * Fungsi ini milik parent frame (mobile SPA) yang membuka sub-modul dalam overlay baru
     * tanpa reload halaman hub.</p>
     *
     * <p>Fallback tersedia jika halaman tidak berada dalam iframe atau parent tidak memiliki
     * fungsi {@code openOverlay}: navigasi akan dilanjutkan dengan menambahkan parameter
     * {@code _mob=1} ke URL tujuan, sehingga sub-modul mengetahui bahwa ia sedang dimuat
     * dalam konteks mobile.</p>
     *
     * <p>Parameter {@code labelSelector} menentukan selector CSS yang digunakan untuk
     * mengekstrak teks label dari tautan yang diklik (contoh: {@code ".fw-semibold"} untuk
     * mengambil teks dari elemen dengan kelas {@code fw-semibold} di dalam tautan).
     * Teks ini digunakan sebagai judul overlay di mobile SPA.</p>
     *
     * @param labelSelector Selector CSS untuk elemen teks label di dalam kartu
     *                      (contoh: {@code ".fw-semibold"} atau {@code ".fw-bold"}).
     * @return String HTML blok {@code <script>} yang siap dimasukkan ke dalam halaman.
     */
    public static String buildMobScript(String labelSelector) {
        if (labelSelector == null || labelSelector.trim().length() == 0) {
            labelSelector = ".fw-semibold";
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append("<script>\n(function() {\n");
        sb.append("    if (window.parent === window) return;\n");
        sb.append("    document.addEventListener('click', function(e) {\n");
        sb.append("        var el = e.target;\n");
        sb.append("        while (el && el.tagName !== 'A') { el = el.parentElement; }\n");
        sb.append("        if (!el || !el.href) return;\n");
        sb.append("        var href = el.getAttribute('href') || '';\n");
        sb.append("        if (href.indexOf('/baru?') < 0) return;\n");
        sb.append("        e.preventDefault();\n");
        sb.append("        var lbl = el.querySelector('").append(jsStr(labelSelector)).append("');\n");
        sb.append("        var label = lbl ? lbl.textContent.trim() : el.textContent.trim();\n");
        sb.append("        if (window.parent && typeof window.parent.openOverlay === 'function') {\n");
        sb.append("            window.parent.openOverlay(el.href, label);\n");
        sb.append("        } else {\n");
        sb.append("            var sep = el.href.indexOf('?') >= 0 ? '&' : '?';\n");
        sb.append("            window.location.href = el.href + sep + '_mob=1';\n");
        sb.append("        }\n");
        sb.append("    });\n})();\n</script>\n");
        return sb.toString();
    }

    /* -------------------------------------------------------------------------
     * Utilitas URL
     * ---------------------------------------------------------------------- */

    /**
     * Mengonversi jalur ZUL ke URL pagesmaster yang dapat diakses via JSP.
     *
     * <p>Format konversi: semua karakter non-alfanumerik dihilangkan dari path ZUL,
     * lalu ditambahkan prefix {@code "pagesmaster"} dan diakhiri dengan {@code "zul"},
     * kemudian dibentuk menjadi URL {@code [ctx]/baru?p=pagesmaster[xxx]zul}.</p>
     *
     * <p>Contoh: {@code /pages/master/antarjemput/panel_kendaraan.zul}
     * menjadi {@code [ctx]/baru?p=pagesmasterantarjemputpanelkendaraanzul}</p>
     *
     * @param ctx     Context path aplikasi (dari {@code request.getContextPath()}).
     * @param zulPath Jalur ZUL yang diawali dengan {@code /pages/} atau {@code /pages/master/}.
     * @return String URL lengkap yang dapat digunakan sebagai {@code href} kartu navigasi.
     */
    public static String zulToPagesmasterUrl(String ctx, String zulPath) {
        if (zulPath == null || zulPath.trim().length() == 0) return "#";
        String path = zulPath.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (path.startsWith("pagesmastermaster")) {
            path = path.replaceFirst("pagesmastermaster", "pagesmaster");
        } else if (!path.startsWith("pagesmaster")) {
            path = "pagesmaster" + path;
        }
        if (!path.endsWith("zul")) {
            path = path + "zul";
        }
        return ctx + "/baru?p=" + path;
    }

    /* -------------------------------------------------------------------------
     * Utilitas Escape
     * ---------------------------------------------------------------------- */

    /**
     * Mengkodekan karakter HTML khusus untuk mencegah XSS pada atribut dan konten teks.
     *
     * <p>Karakter yang diubah: {@code &} → {@code &amp;}, {@code <} → {@code &lt;},
     * {@code >} → {@code &gt;}, {@code "} → {@code &quot;}, {@code '} → {@code &#x27;}.
     * Metode ini dipanggil secara internal oleh semua metode build. Pemanggil eksternal
     * dapat menggunakannya untuk menyandikan data sebelum dimasukkan ke HTML.</p>
     *
     * @param s String yang akan dienkode. Jika {@code null}, dikembalikan string kosong.
     * @return String yang sudah aman untuk di-render dalam HTML.
     */
    public static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '&')  sb.append("&amp;");
            else if (c == '<')  sb.append("&lt;");
            else if (c == '>')  sb.append("&gt;");
            else if (c == '"')  sb.append("&quot;");
            else if (c == '\'') sb.append("&#x27;");
            else                sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Mengkodekan string agar aman digunakan sebagai nilai literal dalam kode JavaScript.
     *
     * <p>Karakter yang diubah: {@code \} → {@code \\}, {@code '} → {@code \'},
     * {@code "} → {@code \"}, newline → {@code \n}, carriage return → {@code \r}.
     * Digunakan secara internal untuk menghasilkan kode JavaScript yang mengandung
     * string dinamis.</p>
     *
     * @param s String yang akan dienkode. Jika {@code null}, dikembalikan string kosong.
     * @return String yang aman digunakan di dalam literal string JavaScript.
     */
    public static String jsStr(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '\\') sb.append("\\\\");
            else if (c == '\'') sb.append("\\'");
            else if (c == '"')  sb.append("\\\"");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else                sb.append(c);
        }
        return sb.toString();
    }

    /* -------------------------------------------------------------------------
     * Konstruktor privat — kelas utilitas tidak boleh diinstansiasi
     * ---------------------------------------------------------------------- */

    /**
     * Konstruktor privat untuk mencegah instansiasi kelas utilitas ini.
     *
     * <p>Seluruh metode bersifat {@code static} dan tidak memerlukan instance.
     * Melempar {@link UnsupportedOperationException} jika dipanggil melalui refleksi.</p>
     */
    private MobileHubHelper() {
        throw new UnsupportedOperationException("MobileHubHelper adalah kelas utilitas statis dan tidak dapat diinstansiasi.");
    }

    /* -------------------------------------------------------------------------
     * Data Transfer Object untuk kartu navigasi
     * ---------------------------------------------------------------------- */

    /**
     * Data transfer object (DTO) sederhana yang merepresentasikan satu kartu navigasi
     * pada halaman hub modul.
     *
     * <p>Kelas ini digunakan bersama metode {@link MobileHubHelper#buildCardsFromList}
     * untuk membangun daftar kartu secara programatik dari data yang tersimpan dalam
     * {@code List}. Semua field bersifat {@code public} untuk kemudahan pengisian dari kode JSP.</p>
     */
    public static class CardItem {
        /** Label teks yang tampil pada kartu. */
        public String label;
        /** URL lengkap tujuan kartu (termasuk context path). */
        public String url;
        /** Kelas ikon FontAwesome tanpa prefix {@code fa-}. */
        public String icon;
        /** Warna heksadesimal aksen (contoh: {@code "#3b82f6"}). */
        public String color;
        /** Deskripsi singkat satu kalimat. */
        public String desc;

        /**
         * Konstruktor lengkap untuk membuat objek {@link CardItem}.
         *
         * @param label Label teks kartu.
         * @param url   URL tujuan kartu.
         * @param icon  Kelas ikon FontAwesome.
         * @param color Warna heksadesimal aksen.
         * @param desc  Deskripsi singkat.
         */
        public CardItem(String label, String url, String icon, String color, String desc) {
            this.label = label;
            this.url   = url;
            this.icon  = icon;
            this.color = color;
            this.desc  = desc;
        }
    }

    /**
     * Membangun HTML seluruh baris kartu dari daftar {@link CardItem}.
     *
     * <p>Metode ini mengiterasi daftar {@link CardItem} yang diberikan dan memanggil
     * {@link #buildCard} untuk setiap item, kemudian membungkus seluruhnya dalam
     * elemen {@code <div class="row g-3">}. Merupakan cara paling efisien untuk
     * menghasilkan sejumlah besar kartu tanpa perlu menggabungkan string secara manual.</p>
     *
     * @param items Daftar objek {@link CardItem} yang akan dirender.
     * @return String HTML blok baris kartu yang lengkap.
     */
    public static String buildCardsFromList(List items) {
        StringBuilder sb = new StringBuilder(items.size() * 512);
        sb.append("<div class=\"row g-3\">");
        for (int i = 0; i < items.size(); i++) {
            CardItem item = (CardItem) items.get(i);
            sb.append(buildCard(item.label, item.url, item.icon, item.color, item.desc));
        }
        sb.append("</div>");
        return sb.toString();
    }
}
