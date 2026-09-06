package ais.action.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;
import ais.database.model.file.LampiranLain;

/**
 * Servlet Portal Dokumen Manajemen Sistem (DMS) — penjelajah katalog dokumen berbasis
 * {@link Akreditasi} sebagai ruang arsip dan {@link DokumenAkreditasi} sebagai pohon isinya.
 *
 * <h3>Sumber katalog</h3>
 * <ul>
 *   <li>{@link Akreditasi} — kategori atau ruang dokumen tingkat teratas;</li>
 *   <li>{@link DokumenAkreditasi} — struktur isi bertingkat; simpul yang punya anak diperlakukan
 *       sebagai "Sub Ruang", simpul tanpa anak sebagai "Dokumen";</li>
 *   <li>{@link LampiranLain} — berkas yang dapat diunduh, ditautkan ke dokumen lewat pasangan
 *       (id pemilik, nama kelas pemilik).</li>
 * </ul>
 *
 * <h3>Tiga mode permintaan</h3>
 * <ol>
 *   <li>{@code action=download&id=<id>} — menstrim isi lampiran; <b>mewajibkan pengguna sudah
 *       login</b>;</li>
 *   <li>{@code service=1}, {@code service=true}, atau {@code action=list} — menyertakan
 *       ({@code include}) potongan JSP layanan untuk pemuatan AJAX;</li>
 *   <li>tanpa keduanya — meneruskan ({@code forward}) ke halaman utuh portal.</li>
 * </ol>
 *
 * <h3>Batas akses — fakta yang perlu dipahami</h3>
 * <p>Aturan {@code intercept-url pattern="/**"} pada {@code applicationContext-security.xml}
 * bernilai {@code IS_AUTHENTICATED_ANONYMOUSLY}, sehingga endpoint ini <b>terbuka untuk pengunjung
 * anonim</b>. Pembatasan yang nyata dilakukan di dalam kelas ini sendiri, berlapis:</p>
 * <ul>
 *   <li><b>Penjelajahan katalog</b> boleh dilakukan anonim. Yang tampak disaring oleh
 *       {@link #addAkreditasiRoleCriterion(Criteria, Object)}: pengunjung tanpa role hanya melihat
 *       ruang arsip yang {@code kodeGrupPengguna}-nya kosong atau {@code null}. Judul, kode,
 *       keterangan, dan jumlah isi ruang publik karenanya memang dapat dibaca tanpa login — itu
 *       perilaku yang disengaja untuk portal ini.</li>
 *   <li><b>Pengunduhan berkas</b> ditolak dengan HTTP 401 bila
 *       {@link #getLoggedUser(HttpServletRequest)} mengembalikan {@code null}, lalu diperiksa
 *       ulang lewat {@link #isDokumenAktif(DokumenAkreditasi)} dan
 *       {@link #isAkreditasiVisible(Akreditasi, Object)} sehingga id dokumen yang ditebak tidak
 *       menembus batas ruang arsip.</li>
 * </ul>
 * <p>Perlu dicatat bahwa {@link #isSatuanKerjaVisible(Object)} bersifat <b>fail-open</b>: bila
 * refleksi gagal atau kolom satuan kerja kosong, isi dianggap boleh tampil. Demikian pula
 * {@link #addSatuanKerjaCriterion(Criteria)} yang melewatkan penyaringan ketika daftar satuan
 * kerja kosong. Keduanya didokumentasikan apa adanya sebagai perilaku yang berlaku sekarang.</p>
 *
 * <h3>Tidak ada penjelajahan direktori</h3>
 * <p>Servlet ini tidak pernah menyusun jalur berkas dari masukan pengguna. Parameter
 * {@code id}, {@code akreditasi}, dan {@code induk} diurai sebagai {@link Long} lewat
 * {@link #parseLong(String)} — masukan non-numerik menjadi {@code null}. Isi berkas diperoleh dari
 * {@link LampiranLain#ambilFile()}, dan nama berkas untuk header {@code Content-Disposition}
 * dibersihkan oleh {@link #safeDownloadName(String)} sehingga garis miring maupun karakter baris
 * baru tidak dapat menyuntik header.</p>
 *
 * <h3>Catatan session Hibernate</h3>
 * <ul>
 *   <li>{@link #buildDmsContentData(HttpServletRequest)} membuka session sendiri lewat
 *       {@code HibernateUtil.getSessionFactory().openSession()} dan menutupnya di {@code finally}
 *       (clear, disconnect, close).</li>
 *   <li>{@link #downloadDocument(HttpServletRequest, HttpServletResponse)} memakai
 *       {@code HibernateUtil.currentSession()}. Di konteks servlet (non-ZK), pemanggilan itu bisa
 *       mengembalikan sesi ZK sisa thread pool yang sudah ditutup; karena itu bila sesi
 *       {@code null} atau sudah tertutup, sesi native diambil ulang lewat
 *       {@code currentNativeSession()} yang dijamin terbuka. Ini mencegah galat
 *       "Session is closed!".</li>
 *   <li>Sesi hasil {@code currentSession()} tidak ditutup manual karena dikelola lifecycle
 *       aplikasi — penutupan terpusat dilakukan di {@link FilterJSP}.</li>
 *   <li>Kelas ini tidak mengatur character encoding langsung pada object response.</li>
 * </ul>
 *
 * @see Akreditasi
 * @see DokumenAkreditasi
 * @see LampiranLain
 */
public class Document extends HttpServlet {
    /** Versi serial standar {@link java.io.Serializable} untuk kontrak servlet. */
    private static final long serialVersionUID = 1L;

    /**
     * Halaman utuh portal DMS yang dipakai lewat {@code forward} ketika permintaan bukan
     * permintaan layanan maupun unduhan. Berada di bawah {@code /WEB-INF/} sehingga tidak dapat
     * dijangkau langsung lewat URL dan hanya terakses melalui servlet ini.
     */
    private static final String JSP_LANDING = "/WEB-INF/baru/modul/dms/landing_page.jsp";

    /**
     * Potongan JSP layanan yang dipakai lewat {@code include} untuk permintaan AJAX daftar isi
     * ({@code service=1}/{@code service=true}/{@code action=list}). Menghasilkan penggalan HTML,
     * bukan halaman penuh.
     */
    private static final String JSP_SERVICE = "/WEB-INF/baru/modul/dms/_dms_service.jsp";

    /**
     * Potongan JSP isi katalog. Tidak pernah dipanggil langsung oleh servlet, melainkan diberikan
     * ke halaman lewat atribut {@code DMS_CONTENT_JSP} agar JSP induk yang menyertakannya.
     */
    private static final String JSP_CONTENT = "/WEB-INF/baru/modul/dms/_dms_content.jsp";

    /**
     * Batas keras jumlah baris yang diambil per tampilan katalog, berlaku untuk daftar
     * {@link Akreditasi} maupun {@link DokumenAkreditasi}.
     *
     * <p>Tidak ada penomoran halaman, sehingga isi di atas batas ini tidak akan pernah tampil.
     * Batas ini juga menjadi pelindung sederhana agar satu permintaan tidak menarik seluruh
     * tabel.</p>
     */
    private static final int MAX_LIST_ITEM = 300;

    /** Ukuran penyangga penyalinan saat menstrim isi lampiran ke klien, dalam byte. */
    private static final int STREAM_BUFFER_SIZE = 16 * 1024;

    /**
     * Konstruktor default yang dibutuhkan container servlet; tidak melakukan inisialisasi apa pun
     * selain memanggil konstruktor {@link HttpServlet}. Seluruh state bersifat per-permintaan.
     */
    public Document() {
        super();
    }

    /**
     * Menerima permintaan HTTP GET — metode utama portal ini — dan meneruskannya ke
     * {@link #process(HttpServletRequest, HttpServletResponse)}.
     *
     * <p>Kegagalan apa pun ditangkap lalu diserahkan ke
     * {@link #handleFatalError(HttpServletResponse, Exception)} yang menuliskan halaman galat
     * ramah, bukan halaman error bawaan container.</p>
     *
     * @param request  permintaan dari peramban
     * @param response respons yang akan diisi halaman, penggalan HTML, atau isi berkas
     * @throws ServletException bila container melaporkan kegagalan servlet
     * @throws IOException      bila penulisan respons gagal
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(response, e);
        }
    }

    /**
     * Menerima permintaan HTTP POST dan meneruskannya ke
     * {@link #process(HttpServletRequest, HttpServletResponse)}.
     *
     * <p>Perilakunya identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)};
     * kelas ini tidak membedakan metode HTTP sama sekali, karena mode ditentukan sepenuhnya oleh
     * parameter {@code action} dan {@code service}.</p>
     *
     * @param request  permintaan dari peramban
     * @param response respons yang akan diisi halaman, penggalan HTML, atau isi berkas
     * @throws ServletException bila container melaporkan kegagalan servlet
     * @throws IOException      bila penulisan respons gagal
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(response, e);
        }
    }

    /**
     * Pengarah permintaan: menentukan mode portal lalu memilih antara menstrim berkas,
     * menyertakan potongan JSP layanan, atau meneruskan ke halaman utuh.
     *
     * <h4>Urutan kerja</h4>
     * <ol>
     *   <li>Memasang header anti-cache lewat {@link #setNoCache(HttpServletResponse)} dan menyetel
     *       encoding permintaan ke UTF-8 (kegagalannya diabaikan karena sebagian container sudah
     *       mengunci encoding sebelum titik ini);</li>
     *   <li>menyiapkan atribut umum lewat
     *       {@link #prepareCommonAttributes(HttpServletRequest)} — termasuk identitas pengguna,
     *       sehingga JSP tahu apakah tombol unduh perlu ditampilkan;</li>
     *   <li>bila {@code action=download}, menyerahkan seluruh sisa penanganan ke
     *       {@link #downloadDocument(HttpServletRequest, HttpServletResponse)} dan berhenti —
     *       katalog tidak dibangun sama sekali untuk jalur ini;</li>
     *   <li>selain itu membangun isi katalog lewat
     *       {@link #prepareDmsContentAttributes(HttpServletRequest)}, lalu menyertakan
     *       ({@code include}) {@link #JSP_SERVICE} untuk permintaan AJAX
     *       ({@code service=1}/{@code service=true}/{@code action=list}) atau meneruskan
     *       ({@code forward}) ke {@link #JSP_LANDING} untuk permintaan halaman biasa.</li>
     * </ol>
     *
     * <p>Perbedaan {@code include} dan {@code forward} bersifat penting: mode layanan hanya
     * menyumbang penggalan HTML ke respons yang sedang berjalan, sedangkan mode halaman
     * menyerahkan kendali respons sepenuhnya ke JSP tujuan.</p>
     *
     * @param request  permintaan dari peramban
     * @param response respons yang akan diisi
     * @throws Exception bila JSP tujuan tidak ditemukan atau penanganan unduhan gagal
     */
    private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        setNoCache(response);
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:96");
        }

        String action = trim(request.getParameter("action"));
        String service = trim(request.getParameter("service"));

        prepareCommonAttributes(request);

        if ("download".equalsIgnoreCase(action)) {
            downloadDocument(request, response);
            return;
        }

        prepareDmsContentAttributes(request);

        if ("1".equals(service) || "true".equalsIgnoreCase(service) || "list".equalsIgnoreCase(action)) {
            response.setContentType("text/html; charset=UTF-8");
            RequestDispatcher rd = request.getRequestDispatcher(JSP_SERVICE);
            if (rd == null) {
                throw new ServletException("Service DMS tidak ditemukan: " + JSP_SERVICE);
            }
            rd.include(request, response);
            return;
        }

        RequestDispatcher rd = request.getRequestDispatcher(JSP_LANDING);
        if (rd == null) {
            throw new ServletException("Halaman DMS tidak ditemukan: " + JSP_LANDING);
        }
        rd.forward(request, response);
    }

    /**
     * Menyiapkan atribut permintaan yang dibutuhkan semua tampilan portal, terlepas dari mode.
     *
     * <p>Atribut yang dipasang: {@code DMS_BASE_URL} (gabungan context path dan servlet path,
     * dengan cadangan {@code "/document"} bila servlet path kosong), {@code DMS_LOGGED_IN},
     * {@code DMS_USER_OBJECT}, {@code DMS_USER_DISPLAY} (nama tampil pengguna atau
     * {@code "Pengunjung"} bila anonim), dan {@code DMS_CONTENT_JSP}.</p>
     *
     * <p>{@code DMS_BASE_URL} dipakai sebagai dasar penyusunan seluruh tautan buka dan unduh pada
     * {@link #toAkreditasiEntry(Session, Akreditasi, String)} serta
     * {@link #toDokumenEntry(Session, DokumenAkreditasi, String)}.</p>
     *
     * @param request permintaan yang atributnya akan diisi
     */
    private void prepareCommonAttributes(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }
        String servletPath = request.getServletPath();
        if (servletPath == null || servletPath.trim().length() == 0) {
            servletPath = "/document";
        }

        Object user = getLoggedUser(request);
        boolean loggedIn = user != null;

        request.setAttribute("DMS_BASE_URL", contextPath + servletPath);
        request.setAttribute("DMS_LOGGED_IN", Boolean.valueOf(loggedIn));
        request.setAttribute("DMS_USER_OBJECT", user);
        request.setAttribute("DMS_USER_DISPLAY", loggedIn ? getUserDisplayName(user) : "Pengunjung");
        request.setAttribute("DMS_CONTENT_JSP", JSP_CONTENT);
    }

    /**
     * Membangun isi katalog lewat {@link #buildDmsContentData(HttpServletRequest)} lalu
     * memindahkannya ke atribut permintaan agar dapat dibaca JSP.
     *
     * <p>Atribut yang dipasang mencakup mode tampilan ({@code DMS_MODE} bernilai {@code "root"}
     * atau {@code "dokumen"}), posisi penjelajahan ({@code DMS_AKREDITASI_ID},
     * {@code DMS_INDUK_ID}), kata kunci pencarian, daftar baris ({@code DMS_ENTRIES}), remah roti
     * ({@code DMS_BREADCRUMBS}), sejumlah pencacah ringkasan, dan pesan galat.</p>
     *
     * <p>{@code DMS_SERVICE_ALLOWED} selalu diisi {@link Boolean#TRUE}: penyaringan hak akses
     * sudah dilakukan di tingkat query dan tingkat baris, bukan lewat bendera ini.</p>
     *
     * @param request permintaan yang atributnya akan diisi
     */
    private void prepareDmsContentAttributes(HttpServletRequest request) {
        DmsContentData data = buildDmsContentData(request);

        request.setAttribute("DMS_SERVICE_ALLOWED", Boolean.TRUE);
        request.setAttribute("DMS_MODE", data.mode);
        request.setAttribute("DMS_AKREDITASI_ID", toStringOrEmpty(data.akreditasiId));
        request.setAttribute("DMS_INDUK_ID", toStringOrEmpty(data.indukId));
        request.setAttribute("DMS_KEYWORD", data.keyword);
        request.setAttribute("DMS_ENTRIES", data.entries);
        request.setAttribute("DMS_BREADCRUMBS", data.breadcrumbs);
        request.setAttribute("DMS_TOTAL_AKREDITASI", Integer.valueOf(data.totalAkreditasi));
        request.setAttribute("DMS_TOTAL_FOLDER", Integer.valueOf(data.totalFolder));
        request.setAttribute("DMS_TOTAL_FILE", Integer.valueOf(data.totalFile));
        request.setAttribute("DMS_TOTAL_LAMPIRAN", Integer.valueOf(data.totalLampiran));
        request.setAttribute("DMS_TOTAL_ITEM", Integer.valueOf(data.entries == null ? 0 : data.entries.size()));
        request.setAttribute("DMS_ERROR_MESSAGE", data.errorMessage);
    }

    /**
     * Menyusun seluruh isi katalog untuk satu permintaan: daftar baris, remah roti, pencacah
     * ringkasan, dan pesan galat bila ada.
     *
     * <h4>Dua mode</h4>
     * <ul>
     *   <li><b>Mode {@code root}</b> — dipilih ketika parameter {@code akreditasi} tidak ada atau
     *       tidak dapat diurai sebagai angka. Menampilkan daftar ruang arsip hasil
     *       {@link #buildAkreditasiCriteria(Session, Object, String, boolean)}.</li>
     *   <li><b>Mode {@code dokumen}</b> — menampilkan isi satu ruang arsip. Bila {@code induk}
     *       diberikan, isi yang ditampilkan adalah anak dari simpul itu; bila tidak, yang
     *       ditampilkan adalah simpul akar ruang tersebut.</li>
     * </ul>
     *
     * <h4>Pemeriksaan yang dilakukan</h4>
     * <p>Ruang arsip yang diminta harus lolos {@link #isAkreditasiVisible(Akreditasi, Object)};
     * bila tidak, tampilan dikembalikan ke mode {@code root} disertai pesan netral tanpa
     * membocorkan apakah baris itu sebenarnya ada. Simpul {@code induk} diverifikasi harus benar
     * benar milik ruang arsip yang sama ({@code induk.getAkreditasi().getId()} dibandingkan
     * dengan id ruang) sekaligus masih aktif — penjagaan inilah yang mencegah id induk dari ruang
     * lain dipakai untuk mengintip isi ruang yang tidak boleh dilihat.</p>
     *
     * <h4>Session dan penanganan galat</h4>
     * <p>Method membuka session Hibernate sendiri dan menutupnya di {@code finally} dengan urutan
     * clear, disconnect, close. Query hanya membaca; tidak ada transaksi tulis. Exception apa pun
     * ditangkap dan diubah menjadi {@code errorMessage} pada objek hasil sehingga halaman tetap
     * tampil separuh jalan alih-alih memunculkan galat container. Pesan itu memuat teks
     * {@link Exception#getMessage()} apa adanya, yang berarti detail teknis dapat terbaca oleh
     * pengunjung — perilaku yang berlaku sekarang dan dicatat di sini apa adanya.</p>
     *
     * @param request permintaan yang memuat parameter {@code q}, {@code akreditasi}, {@code induk}
     * @return objek data isi katalog yang tidak pernah {@code null}
     */
    @SuppressWarnings("unchecked")
    private DmsContentData buildDmsContentData(HttpServletRequest request) {
        DmsContentData data = new DmsContentData();
        data.keyword = trim(request.getParameter("q"));
        data.akreditasiId = parseLong(request.getParameter("akreditasi"));
        data.indukId = parseLong(request.getParameter("induk"));

        Object user = getLoggedUser(request);
        String baseUrl = String.valueOf(request.getAttribute("DMS_BASE_URL"));

        Session session = null;
        try {
            // Servlet (non-ZK): currentSession() bisa mengembalikan sesi ZK SISA dari thread
            // pool yang sudah ditutup → "Session is closed!" saat query. Re-acquire sesi native
            // yang dijamin open (currentNativeSession membuka ulang bila sesi lama stale/closed).
            session = HibernateUtil.getSessionFactory().openSession();

            if (data.akreditasiId == null) {
                data.mode = "root";
                data.breadcrumbs = buildRootBreadcrumbs();

                Criteria criteria = buildAkreditasiCriteria(session, user, data.keyword, true);
                List<Akreditasi> rows = criteria.setMaxResults(MAX_LIST_ITEM).list();
                if (rows == null) {
                    return data;
                }

                for (int i = 0; i < rows.size(); i++) {
                    Akreditasi akreditasi = rows.get(i);
                    if (akreditasi == null || akreditasi.getId() == null) {
                        continue;
                    }
                    Map<String, Object> row = toAkreditasiEntry(session, akreditasi, baseUrl);
                    data.entries.add(row);
                    data.totalAkreditasi++;
                    data.totalFolder++;
                }
                return data;
            }

            Akreditasi akreditasi = (Akreditasi) session.get(Akreditasi.class, data.akreditasiId);
            if (akreditasi == null || !isAkreditasiVisible(akreditasi, user)) {
                data.mode = "root";
                data.akreditasiId = null;
                data.indukId = null;
                data.breadcrumbs = buildRootBreadcrumbs();
                data.errorMessage = "Ruang dokumen tidak ditemukan atau tidak dapat ditampilkan.";
                return data;
            }

            DokumenAkreditasi induk = null;
            if (data.indukId != null) {
                induk = (DokumenAkreditasi) session.get(DokumenAkreditasi.class, data.indukId);
                if (induk == null || induk.getAkreditasi() == null || induk.getAkreditasi().getId() == null
                        || !induk.getAkreditasi().getId().equals(akreditasi.getId()) || !isDokumenAktif(induk)) {
                    data.indukId = null;
                    induk = null;
                    data.errorMessage = "Sub ruang dokumen tidak ditemukan atau tidak aktif.";
                }
            }

            data.mode = "dokumen";
            data.breadcrumbs = buildDokumenBreadcrumbs(akreditasi, induk);

            Criteria criteria = buildDokumenCriteria(session, akreditasi, induk, data.keyword, true);
            List<DokumenAkreditasi> rows = criteria.setMaxResults(MAX_LIST_ITEM).list();
            if (rows == null) {
                return data;
            }

            for (int i = 0; i < rows.size(); i++) {
                DokumenAkreditasi dokumen = rows.get(i);
                if (dokumen == null || dokumen.getId() == null) {
                    continue;
                }
                Map<String, Object> row = toDokumenEntry(session, dokumen, baseUrl);
                data.entries.add(row);

                boolean hasChildren = Boolean.TRUE.equals(row.get("hasChildren"));
                boolean hasAttachment = Boolean.TRUE.equals(row.get("hasAttachment"));
                if (hasChildren) {
                    data.totalFolder++;
                } else {
                    data.totalFile++;
                }
                if (hasAttachment) {
                    data.totalLampiran++;
                }
            }
        } catch (Exception e) {
            data.errorMessage = "Katalog dokumen belum dapat dimuat. " + safe(e.getMessage());
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:261");
            }
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:buildDmsContentData-clear");}
                try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:buildDmsContentData-disconnect");}
                try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:buildDmsContentData-close");}
            }
        }

        return data;
    }

    /**
     * Merakit kriteria pencarian daftar ruang arsip {@link Akreditasi} untuk mode {@code root}.
     *
     * <p>Empat penyaring dipasang berurutan: status aktif
     * ({@link #addAktifCriterion(Criteria)}), pembatasan jenis dokumen yang boleh muncul di portal
     * ({@link #addAkreditasiJenisCriterion(Criteria)}), pembatasan menurut role pengguna
     * ({@link #addAkreditasiRoleCriterion(Criteria, Object)}), dan pembatasan satuan kerja
     * ({@link #addSatuanKerjaCriterion(Criteria)}).</p>
     *
     * <p>Kata kunci pencarian dicocokkan dengan {@code ilike} bermodus
     * {@link MatchMode#ANYWHERE} pada kolom {@code nama}, {@code keterangan}, {@code lembaga},
     * dan {@code jenis}. Nilai kata kunci diteruskan sebagai parameter terikat sehingga tidak
     * membentuk SQL; karakter jokerLIKE ({@code %} dan {@code _}) di dalamnya tetap berlaku
     * sebagai joker dan hanya dapat memperluas hasil di dalam batas yang sudah dipagari keempat
     * penyaring di atas.</p>
     *
     * @param session session Hibernate yang sedang terbuka
     * @param user    pengguna saat ini; boleh {@code null} untuk pengunjung anonim
     * @param keyword kata kunci pencarian; boleh {@code null} atau kosong
     * @param order   bila {@code true}, hasil diurutkan menurut jenis, tahun menurun, lalu nama
     * @return kriteria siap pakai yang belum dieksekusi
     */
    private Criteria buildAkreditasiCriteria(Session session, Object user, String keyword, boolean order) {
        Criteria criteria = session.createCriteria(Akreditasi.class);
        addAktifCriterion(criteria);
        addAkreditasiJenisCriterion(criteria);
        addAkreditasiRoleCriterion(criteria, user);
        addSatuanKerjaCriterion(criteria);

        if (keyword != null && keyword.trim().length() > 0) {
            String q = keyword.trim();
            criteria.add(Restrictions.or(Restrictions.ilike("nama", q, MatchMode.ANYWHERE),
                    Restrictions.or(Restrictions.ilike("keterangan", q, MatchMode.ANYWHERE),
                            Restrictions.or(Restrictions.ilike("lembaga", q, MatchMode.ANYWHERE),
                                    Restrictions.ilike("jenis", q, MatchMode.ANYWHERE)))));
        }

        if (order) {
            criteria.addOrder(Order.asc("jenis"));
            criteria.addOrder(Order.desc("tahun"));
            criteria.addOrder(Order.asc("nama"));
        }
        return criteria;
    }

    /**
     * Merakit kriteria pencarian isi satu ruang arsip untuk mode {@code dokumen}.
     *
     * <p>Baris dibatasi pada {@code akreditasi} yang diberikan — inilah pagar utama yang menjaga
     * agar isi ruang lain tidak ikut terbawa. Ketika {@code induk} bernilai {@code null}, yang
     * diambil adalah simpul akar ({@code induk is null}); selain itu diambil anak langsung dari
     * simpul tersebut, sehingga penjelajahan berlangsung satu tingkat per permintaan.</p>
     *
     * <p>Penyaring status aktif dan satuan kerja ikut dipasang. Kata kunci dicocokkan dengan
     * {@code ilike} bermodus {@link MatchMode#ANYWHERE} pada kolom {@code nama}, {@code kode},
     * dan {@code keterangan}, dengan sifat yang sama seperti pada
     * {@link #buildAkreditasiCriteria(Session, Object, String, boolean)}.</p>
     *
     * <p>Perhatikan bahwa penyaring role <b>tidak</b> dipasang di sini; pembatasan role sudah
     * ditegakkan lebih dulu pada tingkat ruang arsip oleh pemanggilnya
     * ({@link #buildDmsContentData(HttpServletRequest)} memverifikasi
     * {@link #isAkreditasiVisible(Akreditasi, Object)} sebelum memanggil method ini).</p>
     *
     * @param akreditasi ruang arsip yang isinya ditampilkan; wajib sudah diverifikasi boleh
     *                   dilihat oleh pengguna saat ini
     * @param induk      simpul induk; {@code null} berarti tingkat akar
     * @param session    session Hibernate yang sedang terbuka
     * @param keyword    kata kunci pencarian; boleh {@code null} atau kosong
     * @param order      bila {@code true}, hasil diurutkan menurut nomor urut, kode, lalu nama
     * @return kriteria siap pakai yang belum dieksekusi
     */
    private Criteria buildDokumenCriteria(Session session, Akreditasi akreditasi, DokumenAkreditasi induk,
            String keyword, boolean order) {
        Criteria criteria = session.createCriteria(DokumenAkreditasi.class);
        criteria.add(Restrictions.eq("akreditasi", akreditasi));

        if (induk == null) {
            criteria.add(Restrictions.isNull("induk"));
        } else {
            criteria.add(Restrictions.eq("induk", induk));
        }

        addAktifCriterion(criteria);
        addSatuanKerjaCriterion(criteria);

        if (keyword != null && keyword.trim().length() > 0) {
            String q = keyword.trim();
            criteria.add(Restrictions.or(Restrictions.ilike("nama", q, MatchMode.ANYWHERE),
                    Restrictions.or(Restrictions.ilike("kode", q, MatchMode.ANYWHERE),
                            Restrictions.ilike("keterangan", q, MatchMode.ANYWHERE))));
        }

        if (order) {
            criteria.addOrder(Order.asc("nomorUrut"));
            criteria.addOrder(Order.asc("kode"));
            criteria.addOrder(Order.asc("nama"));
        }
        return criteria;
    }

    /**
     * Membatasi kriteria pada baris yang aktif.
     *
     * <p>Kolom {@code aktif} bernilai {@code null} diperlakukan sebagai aktif, mengikuti kebiasaan
     * data lama yang belum pernah mengisi kolom tersebut. Dengan kata lain hanya nilai
     * {@link Boolean#FALSE} yang benar-benar menyembunyikan baris.</p>
     *
     * @param criteria kriteria yang akan ditambahi penyaring
     */
    private void addAktifCriterion(Criteria criteria) {
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
    }

    /**
     * Membatasi ruang arsip yang boleh tampil di portal menurut kolom {@code jenis}.
     *
     * <p>Daftar jenis yang diizinkan diambil lebih dulu lewat refleksi terhadap method statis
     * {@code Akreditasi.jenisDokumenDms()} bila method itu tersedia; kegagalan refleksi diabaikan
     * dan tidak menghentikan proses. Setelah itu empat jenis bawaan selalu ditambahkan lewat
     * {@link #addUnique(List, String)}: {@code "Dokumen"},
     * {@code "Sertifikasi/Akreditasi Eksternal"},
     * {@code "Akreditasi Internasional Program Studi"}, dan
     * {@code "Audit Eksternal Keuangan"}.</p>
     *
     * <p>Baris dengan {@code jenis} kosong atau {@code null} tetap diloloskan, sehingga penyaring
     * ini bersifat memangkas jenis yang tidak dikenal, bukan mewajibkan jenis tertentu.</p>
     *
     * @param criteria kriteria yang akan ditambahi penyaring
     */
    @SuppressWarnings("unchecked")
    private void addAkreditasiJenisCriterion(Criteria criteria) {
        List<String> jenis = new ArrayList<String>();
        try {
            Method method = Akreditasi.class.getMethod("jenisDokumenDms", new Class[0]);
            Object value = method.invoke(null, new Object[0]);
            if (value instanceof List) {
                jenis.addAll((List<String>) value);
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:333");
        }

        addUnique(jenis, "Dokumen");
        addUnique(jenis, "Sertifikasi/Akreditasi Eksternal");
        addUnique(jenis, "Akreditasi Internasional Program Studi");
        addUnique(jenis, "Audit Eksternal Keuangan");

        criteria.add(Restrictions.or(Restrictions.isNull("jenis"),
                Restrictions.or(Restrictions.eq("jenis", ""), Restrictions.in("jenis", jenis))));
    }

    /**
     * Menambahkan satu nilai ke daftar bila belum ada padanannya, dengan perbandingan tanpa
     * memperhatikan besar kecil huruf.
     *
     * <p>Nilai {@code null}, kosong, atau yang hanya berisi spasi diabaikan. Nilai yang ditambahkan
     * sudah dipangkas spasi tepinya.</p>
     *
     * @param data  daftar tujuan yang akan dimutasi di tempat
     * @param value nilai yang hendak ditambahkan
     */
    private void addUnique(List<String> data, String value) {
        if (value == null || value.trim().length() == 0) {
            return;
        }
        String clean = value.trim();
        for (int i = 0; i < data.size(); i++) {
            if (clean.equalsIgnoreCase(String.valueOf(data.get(i)))) {
                return;
            }
        }
        data.add(clean);
    }

    /**
     * Membatasi baris pada satuan kerja yang boleh dilihat pengguna saat ini.
     *
     * <p>Daftar satuan kerja diambil dari {@code SekolahUtil.ambilSatuanKerjas()}. Baris dengan
     * {@code satuanKerja} bernilai {@code null} selalu diloloskan sebagai isi lintas satuan
     * kerja.</p>
     *
     * <p><b>Sifat fail-open yang perlu diketahui:</b> bila daftar satuan kerja kosong atau
     * {@code null} — termasuk ketika pemanggilan melempar exception dan ditelan blok
     * {@code catch} — penyaring ini <b>tidak dipasang sama sekali</b>, sehingga seluruh satuan
     * kerja ikut tampil. Perilaku ini didokumentasikan apa adanya sebagai keadaan yang berlaku
     * sekarang.</p>
     *
     * @param criteria kriteria yang akan ditambahi penyaring
     */
    @SuppressWarnings("unchecked")
    private void addSatuanKerjaCriterion(Criteria criteria) {
        try {
            Set satuanKerjas = SekolahUtil.ambilSatuanKerjas();
            if (satuanKerjas != null && !satuanKerjas.isEmpty()) {
                criteria.add(Restrictions.or(Restrictions.isNull("satuanKerja"), Restrictions.in("satuanKerja", satuanKerjas)));
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:365");
        }
    }

    /**
     * Membatasi ruang arsip menurut role pengguna, memakai kolom {@code kodeGrupPengguna}.
     *
     * <p>Kolom itu menyimpan daftar kode role yang dipisah koma. Pencocokan dilakukan terhadap
     * pola bertanda batas {@code ",<role>,"} dengan {@code ilike} bermodus
     * {@link MatchMode#ANYWHERE}, sehingga role {@code "adm"} tidak ikut cocok dengan entri
     * {@code "admfak"}. Nilai role berasal dari basis data lewat {@link #getRoleId(Object)},
     * bukan dari masukan pengguna.</p>
     *
     * <p>Aturan yang berlaku:</p>
     * <ul>
     *   <li>pengguna administratif ({@link #isAdminUser(Object)}) tidak dibatasi sama sekali;</li>
     *   <li>pengguna tanpa role — termasuk pengunjung anonim — hanya melihat ruang arsip yang
     *       {@code kodeGrupPengguna}-nya {@code null} atau kosong, yaitu ruang publik;</li>
     *   <li>pengguna ber-role melihat ruang publik ditambah ruang yang mencantumkan role
     *       tersebut.</li>
     * </ul>
     *
     * <p>Blok {@code catch} bersifat <b>fail-closed</b>: bila penentuan role gagal, yang dipasang
     * adalah penyaring paling ketat, yakni hanya ruang publik.</p>
     *
     * @param criteria kriteria yang akan ditambahi penyaring
     * @param user     pengguna saat ini; boleh {@code null}
     */
    private void addAkreditasiRoleCriterion(Criteria criteria, Object user) {
        try {
            if (isAdminUser(user)) {
                return;
            }
            String role = getRoleId(user);
            if (role == null || role.trim().length() == 0) {
                criteria.add(Restrictions.or(Restrictions.isNull("kodeGrupPengguna"), Restrictions.eq("kodeGrupPengguna", "")));
            } else {
                String token = "," + role.trim() + ",";
                criteria.add(Restrictions.or(Restrictions.isNull("kodeGrupPengguna"),
                        Restrictions.or(Restrictions.eq("kodeGrupPengguna", ""),
                                Restrictions.ilike("kodeGrupPengguna", token, MatchMode.ANYWHERE))));
            }
        } catch (Exception e) {
            criteria.add(Restrictions.or(Restrictions.isNull("kodeGrupPengguna"), Restrictions.eq("kodeGrupPengguna", "")));
        }
    }

    /**
     * Mengubah satu {@link Akreditasi} menjadi baris tampilan berbentuk peta kunci-nilai yang siap
     * dibaca JSP.
     *
     * <p>Kunci yang dihasilkan mencakup identitas ({@code type} bernilai {@code "akreditasi"},
     * {@code id}, {@code akreditasiId}), teks tampilan ({@code name}, {@code description},
     * {@code jenis}, {@code lembaga}, {@code tingkat}, {@code lingkup}, {@code tahun},
     * {@code periode}), petunjuk tampilan ({@code iconClass}, {@code typeLabel},
     * {@code sizeLabel}), dan navigasi ({@code openUrl}).</p>
     *
     * <p>Jumlah isi dihitung lewat {@link #countRootDokumen(Session, Akreditasi)}. Ruang arsip
     * selalu {@code canOpen} dan tidak pernah punya lampiran langsung, sehingga
     * {@code downloadUrl} selalu kosong.</p>
     *
     * @param session    session Hibernate yang sedang terbuka
     * @param akreditasi ruang arsip yang sudah lolos penyaringan hak akses
     * @param baseUrl    dasar URL portal untuk menyusun tautan
     * @return peta baris tampilan
     */
    private Map<String, Object> toAkreditasiEntry(Session session, Akreditasi akreditasi, String baseUrl) {
        Map<String, Object> row = new HashMap<String, Object>();
        int childCount = countRootDokumen(session, akreditasi);

        row.put("type", "akreditasi");
        row.put("typeLabel", "Ruang Arsip");
        row.put("id", akreditasi.getId());
        row.put("akreditasiId", akreditasi.getId());
        row.put("name", safe(akreditasi.getNama()));
        row.put("code", "");
        row.put("description", safe(akreditasi.getKeterangan()));
        row.put("jenis", safe(akreditasi.getJenis()));
        row.put("lembaga", safe(akreditasi.getLembaga()));
        row.put("tingkat", safe(akreditasi.getTingkat()));
        row.put("lingkup", safe(akreditasi.getLingkup()));
        row.put("tahun", akreditasi.getTahun() == null ? "" : akreditasi.getTahun().toString());
        row.put("periode", buildPeriode(akreditasi));
        row.put("dateLabel", buildPeriode(akreditasi));
        row.put("iconClass", "fa fa-folder-tree text-warning");
        row.put("hasChildren", Boolean.valueOf(childCount > 0));
        row.put("canOpen", Boolean.TRUE);
        row.put("hasAttachment", Boolean.FALSE);
        row.put("childCount", Integer.valueOf(childCount));
        row.put("downloadUrl", "");
        row.put("openUrl", baseUrl + "?akreditasi=" + akreditasi.getId() + "#dokumen");
        row.put("sizeLabel", childCount > 0 ? childCount + " isi dokumen" : "Ruang masih kosong");
        return row;
    }

    /**
     * Mengubah satu {@link DokumenAkreditasi} menjadi baris tampilan berbentuk peta kunci-nilai.
     *
     * <p>Simpul yang punya anak diberi label {@code "Sub Ruang"} dan dapat dibuka
     * ({@code canOpen}); simpul tanpa anak diberi label {@code "Dokumen"} dan hanya dapat diunduh
     * bila lampirannya ada. Ikon dipilih {@link #iconClassByLampiran(LampiranLain)} berdasarkan
     * akhiran nama berkas.</p>
     *
     * <p>{@code downloadUrl} hanya diisi ketika lampiran benar-benar ada. Perlu diingat bahwa
     * tautan itu semata petunjuk tampilan: pemeriksaan hak akses yang sesungguhnya dilakukan
     * ulang di {@link #downloadDocument(HttpServletRequest, HttpServletResponse)}, sehingga
     * menyusun URL unduh secara manual tidak melewati pemeriksaan apa pun.</p>
     *
     * @param session session Hibernate yang sedang terbuka
     * @param dokumen simpul dokumen yang sudah lolos penyaringan kriteria
     * @param baseUrl dasar URL portal untuk menyusun tautan
     * @return peta baris tampilan
     */
    private Map<String, Object> toDokumenEntry(Session session, DokumenAkreditasi dokumen, String baseUrl) {
        Map<String, Object> row = new HashMap<String, Object>();
        int childCount = countChildDokumen(session, dokumen);
        LampiranLain lampiran = getLampiran(dokumen);
        boolean hasAttachment = lampiran != null;
        boolean hasChildren = childCount > 0;
        Long akreditasiId = dokumen.getAkreditasi() == null ? null : dokumen.getAkreditasi().getId();

        row.put("type", "dokumen");
        row.put("typeLabel", hasChildren ? "Sub Ruang" : "Dokumen");
        row.put("id", dokumen.getId());
        row.put("akreditasiId", akreditasiId);
        row.put("name", safe(dokumen.getNama()));
        row.put("code", safe(dokumen.getKode()));
        row.put("description", safe(dokumen.getKeterangan()));
        row.put("jenis", dokumen.getAkreditasi() == null ? "" : safe(dokumen.getAkreditasi().getJenis()));
        row.put("lembaga", dokumen.getAkreditasi() == null ? "" : safe(dokumen.getAkreditasi().getLembaga()));
        row.put("tingkat", "");
        row.put("lingkup", "");
        row.put("tahun", "");
        row.put("periode", "");
        row.put("dateLabel", formatDate(dokumen.getTanggalDokumen()));
        row.put("iconClass", hasChildren ? "fa fa-folder-open text-warning" : iconClassByLampiran(lampiran));
        row.put("hasChildren", Boolean.valueOf(hasChildren));
        row.put("canOpen", Boolean.valueOf(hasChildren));
        row.put("hasAttachment", Boolean.valueOf(hasAttachment));
        row.put("childCount", Integer.valueOf(childCount));
        row.put("downloadUrl", hasAttachment ? baseUrl + "?action=download&id=" + dokumen.getId() : "");
        row.put("openUrl", baseUrl + "?akreditasi=" + akreditasiId + "&induk=" + dokumen.getId() + "#dokumen");
        row.put("sizeLabel", hasChildren ? childCount + " isi dokumen" : (hasAttachment ? "Lampiran tersedia" : "Belum ada lampiran"));
        return row;
    }

    /**
     * Menghitung jumlah simpul akar (yang {@code induk}-nya {@code null}) pada satu ruang arsip.
     *
     * <p>Penyaring aktif dan satuan kerja ikut diterapkan agar angka yang tampil sesuai dengan
     * isi yang benar-benar dapat dilihat. Kegagalan query dikembalikan sebagai {@code 0} sehingga
     * satu baris bermasalah tidak menggagalkan seluruh halaman.</p>
     *
     * @param session    session Hibernate yang sedang terbuka
     * @param akreditasi ruang arsip yang dihitung isinya
     * @return jumlah simpul akar, atau {@code 0} bila query gagal
     */
    private int countRootDokumen(Session session, Akreditasi akreditasi) {
        try {
            Criteria criteria = session.createCriteria(DokumenAkreditasi.class)
                    .add(Restrictions.eq("akreditasi", akreditasi)).add(Restrictions.isNull("induk"));
            addAktifCriterion(criteria);
            addSatuanKerjaCriterion(criteria);
            Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Menghitung jumlah anak langsung dari satu simpul dokumen.
     *
     * <p>Dipakai untuk menentukan apakah simpul ditampilkan sebagai "Sub Ruang" atau "Dokumen".
     * Penyaring aktif dan satuan kerja ikut diterapkan, dan kegagalan query dikembalikan sebagai
     * {@code 0}.</p>
     *
     * @param session session Hibernate yang sedang terbuka
     * @param induk   simpul yang dihitung anaknya
     * @return jumlah anak langsung, atau {@code 0} bila query gagal
     */
    private int countChildDokumen(Session session, DokumenAkreditasi induk) {
        try {
            Criteria criteria = session.createCriteria(DokumenAkreditasi.class).add(Restrictions.eq("induk", induk));
            addAktifCriterion(criteria);
            addSatuanKerjaCriterion(criteria);
            Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Membentuk remah roti untuk mode {@code root}, yang hanya berisi satu simpul
     * {@code "Beranda Dokumen"} dengan id kosong.
     *
     * @return daftar remah roti berisi tepat satu elemen
     */
    private List<Map<String, Object>> buildRootBreadcrumbs() {
        List<Map<String, Object>> breadcrumbs = new ArrayList<Map<String, Object>>();
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("label", "Beranda Dokumen");
        root.put("akreditasiId", "");
        root.put("indukId", "");
        breadcrumbs.add(root);
        return breadcrumbs;
    }

    /**
     * Membentuk remah roti untuk mode {@code dokumen}: beranda, nama ruang arsip, lalu seluruh
     * simpul induk dari yang terluar hingga simpul yang sedang dibuka.
     *
     * <p>Rantai induk ditelusuri ke atas lewat {@code getInduk()}. Penelusuran dijaga
     * {@link HashSet} berisi id yang sudah dikunjungi sehingga data yang terlanjur membentuk
     * lingkaran ({@code A} induk {@code B}, {@code B} induk {@code A}) berhenti dengan sendirinya
     * alih-alih menyebabkan pengulangan tanpa akhir.</p>
     *
     * @param akreditasi ruang arsip yang sedang dibuka
     * @param induk      simpul yang sedang dibuka; {@code null} berarti tingkat akar
     * @return daftar remah roti terurut dari terluar ke terdalam
     */
    private List<Map<String, Object>> buildDokumenBreadcrumbs(Akreditasi akreditasi, DokumenAkreditasi induk) {
        List<Map<String, Object>> breadcrumbs = buildRootBreadcrumbs();
        Map<String, Object> grup = new HashMap<String, Object>();
        grup.put("label", safe(akreditasi.getNama()));
        grup.put("akreditasiId", akreditasi.getId());
        grup.put("indukId", "");
        breadcrumbs.add(grup);

        if (induk == null) {
            return breadcrumbs;
        }

        List<DokumenAkreditasi> parents = new ArrayList<DokumenAkreditasi>();
        DokumenAkreditasi cursor = induk;
        Set<Long> visited = new HashSet<Long>();
        while (cursor != null && cursor.getId() != null && !visited.contains(cursor.getId())) {
            visited.add(cursor.getId());
            parents.add(0, cursor);
            cursor = cursor.getInduk();
        }

        for (int i = 0; i < parents.size(); i++) {
            DokumenAkreditasi d = parents.get(i);
            Map<String, Object> crumb = new HashMap<String, Object>();
            crumb.put("label", safe(d.getNama()));
            crumb.put("akreditasiId", akreditasi.getId());
            crumb.put("indukId", d.getId());
            breadcrumbs.add(crumb);
        }
        return breadcrumbs;
    }

    /**
     * Menstrim isi satu lampiran dokumen ke klien — satu-satunya jalur di kelas ini yang
     * mewajibkan pengguna sudah login.
     *
     * <h4>Gerbang berlapis</h4>
     * <ol>
     *   <li>Pengguna wajib terdeteksi login lewat {@link #getLoggedUser(HttpServletRequest)};
     *       bila tidak, dikembalikan HTTP 401.</li>
     *   <li>Parameter {@code id} wajib berupa angka; bila tidak, dikembalikan HTTP 400.</li>
     *   <li>Dokumen wajib ada, aktif ({@link #isDokumenAktif(DokumenAkreditasi)}), punya ruang
     *       arsip, dan ruang arsip itu wajib boleh dilihat pengguna tersebut
     *       ({@link #isAkreditasiVisible(Akreditasi, Object)}). Kegagalan mana pun dijawab HTTP
     *       404 dengan pesan seragam, sehingga id yang ditebak tidak dapat dipakai membedakan
     *       "tidak ada" dari "tidak boleh diakses".</li>
     *   <li>Lampiran wajib ada dan isinya wajib dapat diambil.</li>
     * </ol>
     * <p>Pemeriksaan ini berdiri sendiri dan tidak bergantung pada tautan yang dihasilkan
     * {@link #toDokumenEntry(Session, DokumenAkreditasi, String)}, sehingga URL unduh yang
     * disusun manual tetap melewati seluruh gerbang di atas.</p>
     *
     * <h4>Penulisan respons</h4>
     * <p>Nama berkas diambil dari keterangan lampiran, lalu nama dokumen, lalu cadangan
     * {@code "dokumen-<id>"}. Header {@code X-Content-Type-Options: nosniff} dipasang agar
     * peramban tidak menebak tipe isi, dan {@code Content-Disposition} dirakit
     * {@link #addContentDisposition(HttpServletResponse, String)} dengan nama yang sudah
     * dibersihkan.</p>
     * <p>{@link LampiranLain#ambilFile()} dapat mengembalikan tiga bentuk yang ditangani
     * terpisah: {@link File} (tipe MIME ditanyakan ke container, isi disalin lewat
     * {@link #streamFile(File, HttpServletResponse)}), {@code byte[]}, dan {@link InputStream}.
     * Bentuk lain dijawab HTTP 500. Untuk {@link File} dan {@code byte[]} panjang isi diketahui
     * sehingga {@code Content-Length} dapat dipasang; untuk {@link InputStream} tidak.</p>
     *
     * <h4>Session</h4>
     * <p>Berbeda dengan {@link #buildDmsContentData(HttpServletRequest)}, method ini memakai
     * {@code HibernateUtil.currentSession()} dan tidak menutupnya sendiri — penutupan dilakukan
     * terpusat di {@link FilterJSP}. Bila sesi itu {@code null} atau sudah tertutup, sesi native
     * diambil ulang lebih dulu.</p>
     *
     * @param request  permintaan yang memuat parameter {@code id}
     * @param response respons yang akan diisi isi berkas atau kode galat
     * @throws Exception bila pengambilan atau penyalinan isi berkas gagal
     */
    private void downloadDocument(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Object user = getLoggedUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Silakan login terlebih dahulu melalui popup Portal Dokumen.");
            return;
        }

        Long id = parseLong(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID dokumen tidak valid.");
            return;
        }

        // Re-acquire sesi native bila currentSession() mengembalikan sesi ZK sisa thread pool
        // yang sudah tertutup (lihat catatan di buildDmsContentData) → cegah "Session is closed!".
        Session session = HibernateUtil.currentSession();
        if (session == null || !session.isOpen()) {
            session = HibernateUtil.currentNativeSession();
        }
        DokumenAkreditasi dokumen = (DokumenAkreditasi) session.get(DokumenAkreditasi.class, id);
        if (dokumen == null || !isDokumenAktif(dokumen) || dokumen.getAkreditasi() == null
                || !isAkreditasiVisible(dokumen.getAkreditasi(), user)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Dokumen tidak ditemukan atau tidak dapat diakses.");
            return;
        }

        LampiranLain lampiran = getLampiran(dokumen);
        if (lampiran == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Lampiran dokumen belum tersedia.");
            return;
        }

        String fileName = safe(lampiran.getKeterangan());
        if (fileName.length() == 0) {
            fileName = safe(dokumen.getNama());
        }
        if (fileName.length() == 0) {
            fileName = "dokumen-" + id;
        }

        Object fileObject = lampiran.ambilFile();
        if (fileObject == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File lampiran tidak ditemukan.");
            return;
        }

        response.reset();
        response.setHeader("X-Content-Type-Options", "nosniff");
        addContentDisposition(response, fileName);

        if (fileObject instanceof File) {
            File file = (File) fileObject;
            if (!file.exists() || !file.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File lampiran tidak ditemukan di server.");
                return;
            }
            String mimeType = getServletContext().getMimeType(file.getName());
            response.setContentType(isBlank(mimeType) ? "application/octet-stream" : mimeType);
            response.setHeader("Content-Length", String.valueOf(file.length()));
            streamFile(file, response);
            return;
        }

        if (fileObject instanceof byte[]) {
            byte[] bytes = (byte[]) fileObject;
            response.setContentType(resolveMimeByName(fileName));
            response.setHeader("Content-Length", String.valueOf(bytes.length));
            streamInput(new ByteArrayInputStream(bytes), response.getOutputStream(), true);
            return;
        }

        if (fileObject instanceof InputStream) {
            response.setContentType(resolveMimeByName(fileName));
            InputStream input = null;
            try {
                input = (InputStream) fileObject;
                streamInput(input, response.getOutputStream(), false);
            } finally {
                closeQuietly(input);
            }
            return;
        }

        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Tipe file lampiran belum didukung oleh servlet Document.");
    }

    /**
     * Menyalin isi sebuah berkas di cakram ke aliran keluaran respons.
     *
     * <p>Berkas dibungkus {@link BufferedInputStream} lalu diserahkan ke
     * {@link #streamInput(InputStream, OutputStream, boolean)}. Aliran masukan selalu ditutup di
     * blok {@code finally}, termasuk ketika penyalinan gagal di tengah jalan.</p>
     *
     * @param file     berkas yang isinya dikirim; sudah dipastikan ada oleh pemanggil
     * @param response respons tujuan
     * @throws IOException bila pembacaan berkas atau penulisan respons gagal
     */
    private void streamFile(File file, HttpServletResponse response) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            streamInput(input, response.getOutputStream(), false);
        } finally {
            closeQuietly(input);
        }
    }

    /**
     * Menyalin seluruh isi aliran masukan ke aliran keluaran memakai penyangga sebesar
     * {@link #STREAM_BUFFER_SIZE}, lalu melakukan {@code flush}.
     *
     * <p>Kepemilikan aliran masukan sengaja dibuat eksplisit lewat {@code closeInput} karena
     * pemanggil berbeda punya tanggung jawab berbeda: penyalinan dari {@code byte[]} menutup
     * aliran sementaranya sendiri, sedangkan penyalinan dari berkas atau dari
     * {@link InputStream} milik lampiran ditutup oleh pemanggilnya.</p>
     *
     * @param input      aliran sumber
     * @param output     aliran tujuan; tidak pernah ditutup oleh method ini
     * @param closeInput bila {@code true}, aliran sumber ditutup di blok {@code finally}
     * @throws IOException bila pembacaan atau penulisan gagal
     */
    private void streamInput(InputStream input, OutputStream output, boolean closeInput) throws IOException {
        try {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } finally {
            if (closeInput) {
                closeQuietly(input);
            }
        }
    }

    /**
     * Mengambil lampiran milik satu dokumen lewat
     * {@link LampiranLain#ambil(Long, String)}, dengan pasangan kunci berupa id dokumen dan nama
     * kelas {@link DokumenAkreditasi}.
     *
     * <p>Penyertaan nama kelas pemilik penting: {@link LampiranLain} dipakai bersama oleh banyak
     * entity, sehingga id saja tidak cukup untuk mengenali pemilik yang benar.</p>
     *
     * <p>Kegagalan apa pun dikembalikan sebagai {@code null} sehingga baris tetap dapat tampil
     * tanpa tombol unduh alih-alih menggagalkan seluruh halaman.</p>
     *
     * @param dokumen dokumen pemilik lampiran; boleh {@code null}
     * @return lampiran yang ditemukan, atau {@code null} bila tidak ada atau terjadi galat
     */
    private LampiranLain getLampiran(DokumenAkreditasi dokumen) {
        try {
            if (dokumen != null && dokumen.getId() != null) {
                return LampiranLain.ambil(dokumen.getId(), DokumenAkreditasi.class.getName());
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:635");
        }
        return null;
    }

    /**
     * Menentukan apakah satu ruang arsip boleh dilihat pengguna tertentu — padanan per-baris dari
     * penyaring query {@link #addAkreditasiRoleCriterion(Criteria, Object)}.
     *
     * <p>Dipakai untuk memeriksa ulang baris yang diambil lewat id langsung, yaitu di
     * {@link #buildDmsContentData(HttpServletRequest)} dan
     * {@link #downloadDocument(HttpServletRequest, HttpServletResponse)}, sehingga id yang ditebak
     * tetap terbentur pemeriksaan yang sama seperti daftar biasa.</p>
     *
     * <p>Urutan pemeriksaan: ruang wajib ada dan aktif, wajib lolos
     * {@link #isSatuanKerjaVisible(Object)}, lalu pengguna administratif langsung diloloskan.
     * Selain itu, ruang tanpa {@code kodeGrupPengguna} dianggap publik, sedangkan ruang ber-kode
     * hanya terbuka bagi pengguna yang role-nya tercantum. Pencocokan memakai pola bertanda batas
     * {@code ",<role>,"} dengan perbandingan huruf kecil, sama seperti pada penyaring query.</p>
     *
     * @param akreditasi ruang arsip yang diperiksa; boleh {@code null}
     * @param user       pengguna saat ini; boleh {@code null} untuk pengunjung anonim
     * @return {@code true} bila ruang boleh ditampilkan
     */
    private boolean isAkreditasiVisible(Akreditasi akreditasi, Object user) {
        if (akreditasi == null || !isAktif(akreditasi.getAktif())) {
            return false;
        }
        if (!isSatuanKerjaVisible(akreditasi)) {
            return false;
        }
        if (isAdminUser(user)) {
            return true;
        }

        String role = getRoleId(user);
        String kode = safe(akreditasi.getKodeGrupPengguna());
        if (kode.length() == 0) {
            return true;
        }

        return role != null && role.trim().length() > 0 && kode.toLowerCase(Locale.ENGLISH)
                .indexOf(("," + role.trim() + ",").toLowerCase(Locale.ENGLISH)) >= 0;
    }

    /**
     * Menentukan apakah satu simpul dokumen masih aktif sekaligus berada dalam cakupan satuan
     * kerja yang boleh dilihat.
     *
     * @param dokumen simpul yang diperiksa; boleh {@code null}
     * @return {@code true} bila simpul ada, aktif, dan satuan kerjanya terlihat
     */
    private boolean isDokumenAktif(DokumenAkreditasi dokumen) {
        return dokumen != null && isAktif(dokumen.getAktif()) && isSatuanKerjaVisible(dokumen);
    }

    /**
     * Memeriksa apakah satuan kerja pemilik sebuah baris termasuk yang boleh dilihat pengguna
     * saat ini, memakai refleksi terhadap method {@code getSatuanKerja()}.
     *
     * <p>Refleksi dipakai agar satu method dapat melayani {@link Akreditasi} maupun
     * {@link DokumenAkreditasi} tanpa antarmuka bersama.</p>
     *
     * <p><b>Sifat fail-open yang perlu diketahui:</b> method mengembalikan {@code true} pada tiga
     * keadaan berbeda — object {@code null}, kolom satuan kerja {@code null} (isi lintas satuan
     * kerja), dan <b>kegagalan refleksi apa pun</b>, termasuk ketika method
     * {@code getSatuanKerja()} tidak ada. Daftar satuan kerja yang kosong juga meloloskan semua.
     * Perilaku ini sejalan dengan {@link #addSatuanKerjaCriterion(Criteria)} dan dicatat apa
     * adanya sebagai keadaan yang berlaku sekarang.</p>
     *
     * @param obj baris yang diperiksa; boleh {@code null}
     * @return {@code true} bila baris boleh ditampilkan
     */
    @SuppressWarnings("unchecked")
    private boolean isSatuanKerjaVisible(Object obj) {
        if (obj == null) {
            return true;
        }
        try {
            Method getter = obj.getClass().getMethod("getSatuanKerja", new Class[0]);
            Object satker = getter.invoke(obj, new Object[0]);
            if (satker == null) {
                return true;
            }
            Set allowed = SekolahUtil.ambilSatuanKerjas();
            return allowed == null || allowed.isEmpty() || allowed.contains(satker);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Menafsirkan kolom {@code aktif} bertipe {@link Boolean} yang boleh {@code null}.
     *
     * <p>Nilai {@code null} diperlakukan sebagai aktif, mengikuti kebiasaan data lama yang belum
     * pernah mengisi kolom tersebut. Penafsiran ini sama dengan yang dipakai
     * {@link #addAktifCriterion(Criteria)} di tingkat query.</p>
     *
     * @param aktif nilai kolom; boleh {@code null}
     * @return {@code true} kecuali nilainya benar-benar {@link Boolean#FALSE}
     */
    private boolean isAktif(Boolean aktif) {
        return aktif == null || aktif.booleanValue();
    }

    /**
     * Mendeteksi pengguna yang sedang login pada permintaan servlet biasa (non-ZK).
     *
     * <p><b>Latar masalah:</b> servlet {@code /document} bukan eksekusi ZK.
     * {@code Common.getCurrentUser()} tanpa argumen bersandar pada
     * {@code ExecutionsCtrl}/{@code RequestContext} milik ZK yang tidak tersedia di sini, sehingga
     * ia mengembalikan {@code null} walau pengguna sudah login — akibatnya tombol unduh
     * disembunyikan dan endpoint unduh menolak dengan HTTP 401. Karena itu dipakai varian
     * ber-{@code request} yang membaca langsung atribut sesi.</p>
     *
     * <p>Tiga jalur dicoba berurutan: {@code Common.getCurrentUser(request)},
     * {@code Common.getCurrentUser()}, lalu penelusuran langsung atribut {@link HttpSession}.
     * Pada jalur ketiga, kunci {@code "mytbmuser"} dan {@code "usersTemp"} didahulukan karena
     * itulah kunci login yang sesungguhnya disetel saat otentikasi (lihat {@code MainAction} dan
     * {@code CommonCurrentSessionHelper}); kunci-kunci berikutnya hanyalah jaring pengaman untuk
     * kompatibilitas dengan alur lama.</p>
     *
     * <p>Sesi tidak pernah dibuat di sini: {@code request.getSession(false)} memastikan pengunjung
     * anonim tidak memicu pembuatan sesi baru.</p>
     *
     * @param request permintaan yang sedang dilayani
     * @return object pengguna dalam bentuk apa pun yang tersimpan di sesi, atau {@code null} bila
     *         tidak ada yang login
     */
    private Object getLoggedUser(HttpServletRequest request) {
        // PENTING: servlet /document BUKAN eksekusi ZK. Common.getCurrentUser() tanpa argumen
        // mengandalkan ExecutionsCtrl/RequestContext ZK yang TIDAK tersedia di request servlet biasa,
        // sehingga mengembalikan null walau user SUDAH login → tombol download disembunyikan &
        // endpoint download menolak (401). Solusi: pakai versi ber-request yang membaca langsung
        // session.getAttribute("mytbmuser")/"usersTemp" — kunci sesungguhnya yang di-set saat login.
        try {
            Object current = Common.getCurrentUser(request);
            if (current != null) {
                return current;
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:698");
        }
        try {
            Object current = Common.getCurrentUser();
            if (current != null) {
                return current;
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:705");
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        // "mytbmuser" & "usersTemp" didahulukan: itulah kunci login yang sebenarnya (lihat
        // MainAction/CommonCurrentSessionHelper). Sisanya jaring pengaman untuk kompatibilitas.
        String[] keys = new String[] { "mytbmuser", "usersTemp", "tbmuser", "TBMUSER", "tbmUser",
                "currentUser", "CURRENT_USER", "CURRENT_TBMUSER", "user", "USER", "userLogin",
                "USER_LOGIN", "pengguna", "PENGGUNA", "mahasiswa", "siswa", "pegawai", "dosen" };
        for (int i = 0; i < keys.length; i++) {
            Object value = session.getAttribute(keys[i]);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Menentukan nama yang ditampilkan untuk pengguna, mencoba tiga tingkat berurutan.
     *
     * <ol>
     *   <li><b>Nama langsung</b> pada object pengguna ({@code getNama}, {@code getNamaLengkap},
     *       {@code getName}) — berlaku untuk tipe seperti Mahasiswa, Pegawai, dan Siswa yang
     *       memang punya nama sendiri.</li>
     *   <li><b>Nama lewat relasi</b>, untuk akun {@code Tbmuser} dasar yang tidak punya nama
     *       sendiri: dicoba {@code getBiodataCalonMahasiswa}, {@code getMahasiswa},
     *       {@code getPegawai}, {@code getSiswa}, {@code getDosen}, {@code getGuru},
     *       {@code getCalonSiswa}, {@code getCalonPegawai}, {@code getPenduduk}.</li>
     *   <li><b>Identitas akun</b> sebagai cadangan terakhir ({@code getUserNama},
     *       {@code getUserId}, {@code getUsername}, {@code getEmail}) — ini identitas akun, bukan
     *       nama asli.</li>
     * </ol>
     *
     * <p>{@link NoSuchMethodException} pada jalur relasi adalah keadaan <b>wajar</b>, bukan bug:
     * tidak semua tipe pengguna punya semua relasi yang dicoba. Karena itu setiap percobaan
     * dibungkus {@code try-catch} sendiri agar kegagalan satu relasi tidak menghentikan percobaan
     * berikutnya. Urutan dan isi tingkat pertama sengaja dipertahankan apa adanya.</p>
     *
     * @param user object pengguna; boleh {@code null}
     * @return nama tampilan, atau {@code "Pengguna"} bila tidak ada yang dapat ditentukan
     */
    private String getUserDisplayName(Object user) {
        if (user == null) {
            return "Pengguna";
        }

        // 1) Nama asli langsung di object user (berlaku utk Mahasiswa/Pegawai/Siswa/dst
        // yang memang punya getNama()). JANGAN ubah urutan/isi bagian ini.
        String[] namaMethods = new String[] { "getNama", "getNamaLengkap", "getName" };
        String direct = invokeFirstNonEmpty(user, namaMethods);
        if (direct != null) {
            return direct;
        }

        // 2) user berupa Tbmuser dasar (mis. akun "Calon Mahasiswa") yang TIDAK punya
        // getNama()/getNamaLengkap() sendiri — namanya ada di entitas terkait via relasi.
        // NoSuchMethodException di sini WAJAR (bukan bug) utk tipe user yang relasinya
        // tidak berlaku baginya; setiap percobaan dibungkus try-catch sendiri.
        String[] relationGetters = new String[] { "getBiodataCalonMahasiswa", "getMahasiswa", "getPegawai",
                "getSiswa", "getDosen", "getGuru", "getCalonSiswa", "getCalonPegawai", "getPenduduk" };
        for (int i = 0; i < relationGetters.length; i++) {
            try {
                Method rel = user.getClass().getMethod(relationGetters[i], new Class[0]);
                Object related = rel.invoke(user, new Object[0]);
                if (related != null) {
                    String namaRelasi = invokeFirstNonEmpty(related, namaMethods);
                    if (namaRelasi != null) {
                        return namaRelasi;
                    }
                }
            } catch (NoSuchMethodException ignored) {
                // Normal: tidak semua tipe user punya seluruh relasi yang dicoba.
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:740");
            }
        }

        // 3) Fallback terakhir: identitas akun (username/id/email), bukan nama asli.
        String[] akunMethods = new String[] { "getUserNama", "getUserId", "getUsername", "getEmail" };
        String akun = invokeFirstNonEmpty(user, akunMethods);
        if (akun != null) {
            return akun;
        }
        return "Pengguna";
    }

    /**
     * Coba panggil satu-per-satu method (tanpa argumen) via refleksi pada {@code target}
     * dan kembalikan nilai String pertama yang non-null & non-kosong. NoSuchMethodException
     * WAJAR terjadi di sini karena tidak semua tipe entity punya semua method yang dicoba —
     * setiap percobaan dibungkus try-catch sendiri agar tidak menghentikan percobaan berikutnya.
     */
    private String invokeFirstNonEmpty(Object target, String[] methodNames) {
        if (target == null) {
            return null;
        }
        for (int i = 0; i < methodNames.length; i++) {
            try {
                Method m = target.getClass().getMethod(methodNames[i], new Class[0]);
                Object value = m.invoke(target, new Object[0]);
                if (value != null && trim(String.valueOf(value)).length() > 0) {
                    return String.valueOf(value);
                }
            } catch (NoSuchMethodException ignored) {
                // Normal: method fallback ini memang belum tentu ada di semua entity.
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:740");
            }
        }
        return null;
    }

    /**
     * Mengambil kode role pengguna.
     *
     * <p>AKAR MASALAH (NoSuchMethodException Tbmuser.getRoleId): ketika
     * {@code hakAkses()} mengembalikan null -- normal untuk pengguna tanpa role,
     * mis. Calon Mahasiswa -- blok pertama selesai TANPA return, lalu jatuh ke
     * blok kedua yang merefleksi {@code getRoleId()} pada Tbmuser. Method itu
     * memang tidak pernah ada, sehingga setiap pemuatan halaman meninggalkan satu
     * catatan error palsu di audit. Perbaikannya: pakai API bertipe untuk Tbmuser
     * (tanpa refleksi sama sekali), dan pada jalur refleksi untuk tipe lain,
     * NoSuchMethodException diperlakukan sebagai keadaan WAJAR -- bukan error --
     * sejalan dengan idiom yang sudah dipakai di kelas ini.
     */
    private String getRoleId(Object user) {
        if (user == null) {
            return "";
        }
        // Jalur bertipe: Tbmuser tanpa role bukan kesalahan, cukup kembalikan kosong.
        if (user instanceof ais.database.model.Tbmuser) {
            try {
                ais.database.model.Tbmrole role = ((ais.database.model.Tbmuser) user).hakAkses();
                if (role == null || role.getRoleId() == null) {
                    return "";
                }
                return role.getRoleId().trim();
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "Document.getRoleId(Tbmuser)");
                return "";
            }
        }
        try {
            Method hakAkses = user.getClass().getMethod("hakAkses", new Class[0]);
            Object role = hakAkses.invoke(user, new Object[0]);
            if (role != null) {
                Method getRoleId = role.getClass().getMethod("getRoleId", new Class[0]);
                Object value = getRoleId.invoke(role, new Object[0]);
                return value == null ? "" : String.valueOf(value).trim();
            }
        } catch (NoSuchMethodException wajar) {
            // Tipe ini memang tidak punya method tsb -- keadaan wajar, bukan error.
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:758");
        }
        try {
            Method getRoleId = user.getClass().getMethod("getRoleId", new Class[0]);
            Object value = getRoleId.invoke(user, new Object[0]);
            return value == null ? "" : String.valueOf(value).trim();
        } catch (NoSuchMethodException wajar) {
            // Tipe ini memang tidak punya method tsb -- keadaan wajar, bukan error.
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:764");
        }
        return "";
    }

    /**
     * Menentukan apakah pengguna berperan administratif sehingga tidak dibatasi penyaring role.
     *
     * <p>Dua sumber diperiksa berurutan: bendera {@code Common.getApakahAdmin()}, lalu daftar kode
     * role yang dianggap administratif ({@code admin}, {@code am}, {@code adm}, {@code admfak},
     * {@code admprd}, {@code akademik}, {@code superadmin}) dengan perbandingan tanpa memperhatikan
     * besar kecil huruf.</p>
     *
     * <p>Perlu dicatat bahwa {@code Common.getApakahAdmin()} bersandar pada konteks eksekusi ZK
     * yang tidak tersedia pada permintaan servlet biasa; pada jalur portal ini penentuan karena
     * itu praktis bertumpu pada kode role. Kegagalan pemanggilan bendera tersebut ditelan dan
     * pemeriksaan dilanjutkan ke kode role.</p>
     *
     * @param user pengguna saat ini; boleh {@code null}
     * @return {@code true} bila pengguna dianggap administratif
     */
    private boolean isAdminUser(Object user) {
        try {
            if (Common.getApakahAdmin()) {
                return true;
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:774");
        }
        String role = getRoleId(user);
        return "admin".equalsIgnoreCase(role) || "am".equalsIgnoreCase(role) || "adm".equalsIgnoreCase(role)
                || "admfak".equalsIgnoreCase(role) || "admprd".equalsIgnoreCase(role)
                || "akademik".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
    }

    private String buildPeriode(Akreditasi akreditasi) {
        if (akreditasi == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (akreditasi.getMulai() != null) {
            sb.append(formatDateOnly(akreditasi.getMulai()));
        }
        if (akreditasi.getSampai() != null) {
            if (sb.length() > 0) {
                sb.append(" s.d ");
            }
            sb.append(formatDateOnly(akreditasi.getSampai()));
        }
        if (sb.length() == 0 && akreditasi.getTahun() != null) {
            sb.append(akreditasi.getTahun());
        }
        return sb.toString();
    }

    private String iconClassByLampiran(LampiranLain lampiran) {
        if (lampiran == null) {
            return "fa fa-file-lines text-secondary";
        }
        String name = safe(lampiran.getKeterangan()).toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".pdf")) return "fa fa-file-pdf text-danger";
        if (name.endsWith(".doc") || name.endsWith(".docx")) return "fa fa-file-word text-primary";
        if (name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".csv")) return "fa fa-file-excel text-success";
        if (name.endsWith(".ppt") || name.endsWith(".pptx")) return "fa fa-file-powerpoint text-warning";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp")) return "fa fa-file-image text-info";
        if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")) return "fa fa-file-zipper text-secondary";
        return "fa fa-file-lines text-secondary";
    }

    private String resolveMimeByName(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ENGLISH);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        try {
            return new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(date);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDateOnly(Date date) {
        if (date == null) {
            return "";
        }
        try {
            return new SimpleDateFormat("dd/MM/yyyy", new Locale("id", "ID")).format(date);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Mengurai parameter permintaan menjadi {@link Long} secara aman.
     *
     * <p>Nilai {@code null}, kosong, maupun bukan angka dikembalikan sebagai {@code null} alih
     * alih melempar exception. Inilah pintu tunggal yang membuat parameter {@code id},
     * {@code akreditasi}, dan {@code induk} tidak pernah dipakai sebagai teks bebas di mana pun,
     * sehingga tidak ada jalan untuk menyusun jalur berkas maupun potongan query dari masukan
     * pengguna.</p>
     *
     * @param value teks parameter; boleh {@code null}
     * @return nilai numerik, atau {@code null} bila tidak dapat diurai
     */
    private Long parseLong(String value) {
        try {
            String s = trim(value);
            if (s.length() == 0) {
                return null;
            }
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Memeriksa apakah sebuah teks kosong: {@code null}, panjang nol, atau hanya berisi spasi.
     *
     * @param value teks yang diperiksa; boleh {@code null}
     * @return {@code true} bila teks dianggap kosong
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * Memangkas spasi tepi sebuah teks dan mengubah {@code null} menjadi teks kosong, sehingga
     * pemanggil tidak perlu memeriksa {@code null} lagi.
     *
     * @param value teks masukan; boleh {@code null}
     * @return teks terpangkas yang tidak pernah {@code null}
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String toStringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safeDownloadName(String value) {
        String name = value == null ? "dokumen" : value.trim();
        if (name.length() == 0) {
            name = "dokumen";
        }
        return name.replace("\\", "_").replace("/", "_").replace("\r", "_").replace("\n", "_").replace("\"", "'");
    }

    private void addContentDisposition(HttpServletResponse response, String fileName) {
        String safeName = safeDownloadName(fileName);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + safeName + "\"");
        try {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''"
                    + URLEncoder.encode(safeName, "UTF-8").replace("+", "%20"));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:896");
        }
    }

    private void setNoCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private void handleFatalError(HttpServletResponse response, Exception e) throws IOException {
        try {
            Common.tampilErrorJikaAdmin(e);
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:909");
        }
        if (response.isCommitted()) {
            return;
        }
        response.reset();
        response.setContentType("text/html; charset=UTF-8");
        OutputStream writer = response.getOutputStream();
        String html = "<!doctype html><html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>Portal Dokumen</title>"
                + "<link href='https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css' rel='stylesheet'>"
                + "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css'>"
                + "</head><body class='bg-light'>"
                + "<div class='container py-5'><div class='card border-0 shadow rounded-4'><div class='card-body p-4 p-lg-5'>"
                + "<div class='d-flex align-items-center gap-3 mb-3'><div class='rounded-circle bg-warning-subtle text-warning p-3'>"
                + "<i class='fa fa-triangle-exclamation fa-2x'></i></div><div><h3 class='fw-bold mb-1'>Portal Dokumen belum dapat ditampilkan</h3>"
                + "<div class='text-muted'>Terjadi kendala saat mengambil data dokumen dari tabel Akreditasi/DokumenAkreditasi.</div></div></div>"
                + "<pre class='bg-light border rounded-3 p-3 small text-danger'>" + escapeHtml(e.getMessage()) + "</pre>"
                + "</div></div></div></body></html>";
        writer.write(html.getBytes("UTF-8"));
        writer.flush();
    }

    private String escapeHtml(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:946");
            }
        }
    }

    /**
     * Tipe implementasi bersarang {@link DmsContentData} milik {@link Document}. Kelas ini memberi nama pada state
     * atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link Document}. Dependensi
     * yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini merupakan
     * detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String mode}, {@code Long
     * akreditasiId}, {@code Long indukId}, {@code String keyword}, {@code String errorMessage}, {@code int
     * totalAkreditasi}, {@code int totalFolder}, {@code int totalFile}. Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     *
     * @see Document
     */
    private static class DmsContentData {
        String mode = "root";
        Long akreditasiId;
        Long indukId;
        String keyword = "";
        String errorMessage = "";
        int totalAkreditasi;
        int totalFolder;
        int totalFile;
        int totalLampiran;
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> breadcrumbs = new ArrayList<Map<String, Object>>();
    }
}
