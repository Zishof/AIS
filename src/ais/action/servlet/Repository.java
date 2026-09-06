package ais.action.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.URL;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Base64;

import ais.action.master.repository.RepositoryPublicService;
import ais.action.master.repository.RepositoryPublicService.ItemCard;
import ais.action.master.repository.RepositoryPublicService.ItemDetail;
import ais.action.master.repository.RepositoryPublicService.AuthorProfile;
import ais.action.master.repository.RepositoryPublicService.CollectionView;
import ais.action.master.repository.RepositoryPublicService.Query;
import ais.action.master.repository.RepositoryPublicService.SearchResult;
import ais.action.master.repository.RepositoryPublicService.Suggestion;
import ais.action.master.repository.RepositoryWorkflowService;
import ais.common.Common;
import ais.common.security.PublicRegistrationRateLimiter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.Tbmuser;

/**
 * Portal publik Repository AIS: satu servlet yang melayani halaman katalog karya ilmiah, API baca
 * bertipe (JSON), unduhan berkas, sitasi, umpan RSS/Atom, serta antarmuka panen metadata OAI-PMH.
 *
 * <p><b>Pemetaan.</b> Didaftarkan di {@code webapp/WEB-INF/web.xml} sebagai servlet
 * {@code repository} dengan dua {@code url-pattern}: {@code /repository} dan {@code /repository/*}.
 * Aturan penutup Spring Security untuk {@code /**} adalah {@code IS_AUTHENTICATED_ANONYMOUSLY},
 * dan itu memang disengaja — repositori karya ilmiah harus dapat dibaca dan dipanen tanpa akun.
 * Karena itu seluruh pembatasan berada di dalam kelas ini dan di {@code RepositoryPublicService}.</p>
 *
 * <p><b>Rute.</b> Dua gaya rute dipakai bersamaan. Gaya lama memakai parameter {@code action} dan
 * {@code view}; gaya URL bersih membaca {@code getPathInfo()} dan memetakan {@code /search},
 * {@code /browse}, {@code /collections}, {@code /policies}, {@code /help}, {@code /ask},
 * {@code /rss/recent}, {@code /sitemap.xml}, {@code /oai/request}, serta pola bersegmen
 * {@code /item/{id}}, {@code /collection/{id}}, dan {@code /author/{nama}}. Rute {@code /oai/request}
 * hanya mengalihkan ke servlet {@code /oai} yang berdiri sendiri.</p>
 *
 * <p><b>Dua bentuk tanggapan.</b> Aksi {@code search} dan {@code suggest} membalas JSON; aksi
 * {@code download}, {@code citation}, {@code feed}, dan {@code oai} membalas berkas atau XML;
 * selebihnya mengisi atribut permintaan lalu <i>forward</i> ke satu JSP tunggal
 * ({@link #JSP}). Pemilihan bentuk galat mengikuti pola yang sama lewat {@link #isJsonRequest}.</p>
 *
 * <p><b>Mekanisme keamanan yang ada — ringkasan agar tidak diduplikasi.</b></p>
 * <ul>
 *   <li><i>Header pengeras.</i> {@link #processSafely} memasang {@code X-Content-Type-Options},
 *       {@code Referrer-Policy}, {@code X-Frame-Options: SAMEORIGIN}, dan {@code X-Request-Id}
 *       pada setiap tanggapan.</li>
 *   <li><i>CSRF.</i> Seluruh aksi yang mengubah state milik pengguna ({@code bookmark},
 *       {@code savesearch}, {@code removepreference}, {@code helpfeedback}) wajib {@code POST} dan
 *       diverifikasi {@link #verifyPublicCsrf} dengan pembandingan waktu-tetap
 *       ({@link #constantTime}).</li>
 *   <li><i>Pengalihan terbuka.</i> Tujuan {@code sendRedirect} yang berasal dari parameter
 *       disaring {@link #safeRepositoryUrl}, yang hanya meloloskan URL di bawah
 *       {@code contextPath + "/repository"}.</li>
 *   <li><i>Pembatasan laju.</i> {@code PublicRegistrationRateLimiter} per alamat IP: 300
 *       pencarian/jam, 120 unduhan/jam, 20 umpan balik panduan/jam.</li>
 *   <li><i>Naskah lengkap.</i> Bila {@code service.anonymousFullTextAllowed()} bernilai
 *       {@code false}, pengunjung anonim kehilangan daftar berkas pada halaman butir dan ditolak
 *       di {@link #download}.</li>
 *   <li><i>Token panen.</i> {@code resumptionToken} OAI-PMH ditandatangani HMAC-SHA256 dan
 *       ber-masa-berlaku, sehingga tidak dapat dipalsukan untuk melompati penomoran halaman —
 *       lihat {@link #buildOaiToken} dan {@link #decodeOaiToken}.</li>
 *   <li><i>Asal publik.</i> {@link #publicOrigin} memvalidasi skema, host, dan porta sebelum
 *       menyusun URL absolut, sehingga header {@code Host} yang dipalsukan tidak menular ke
 *       sitemap dan umpan.</li>
 *   <li><i>Nama berkas unduhan.</i> {@link #safeFileName} membuang pemisah jalur dan karakter
 *       kendali sebelum nama dipasang di {@code Content-Disposition}.</li>
 * </ul>
 *
 * <p><b>Jalur berkas.</b> Tidak ada jalur berkas yang berasal dari klien. {@link #download}
 * menerima {@code id} numerik, mengambil baris {@link RepoBitstream} dari basis data, lalu
 * meminta {@code service.resolveBitstreamFile(...)} menentukan berkas fisiknya; nama berkas dari
 * basis data hanya dipakai sebagai label unduhan. {@link #forwardRepositoryJsp} selalu
 * meneruskan ke konstanta {@link #JSP}, tidak pernah ke nilai dari permintaan.</p>
 *
 * <p><b>Session Hibernate.</b> Servlet ini berjalan di luar daur hidup <i>OpenSessionInView</i>
 * ZK. {@code RepositoryPublicService} memakai session {@code ThreadLocal} bawaan, sehingga
 * {@link #processSafely} wajib memanggil {@code HibernateUtil.closeSession()} di blok
 * {@code finally} setiap permintaan.</p>
 *
 * @see RepositoryPublicService
 * @see RepositoryWorkflowService
 */
public class Repository extends HttpServlet {
    /** Versi serialisasi servlet; tetap {@code 1L}. */
    private static final long serialVersionUID = 1L;
    /**
     * Satu-satunya template yang di-<i>forward</i> servlet ini. Nilainya konstan dan tidak pernah
     * disusun dari permintaan, sehingga tidak ada jalur <i>path traversal</i> lewat penerusan JSP.
     * Halaman mana yang dirender ditentukan atribut {@code repoView}, bukan nama berkas.
     */
    private static final String JSP = "/WEB-INF/baru/modul/repository/landing_page.jsp";
    /**
     * Nama atribut {@link HttpSession} tempat token CSRF pengunjung disimpan. Token dibangkitkan
     * sekali per sesi di {@link #process} dan diperiksa {@link #verifyPublicCsrf}.
     */
    private static final String CSRF = "repository.public.csrf";
    /**
     * Nama atribut permintaan yang menandai bahwa permintaan ini adalah percobaan ulang otomatis
     * setelah kegagalan sesaat. Penandanya dibaca {@link #canRetryHome} untuk mencegah percobaan
     * berulang tanpa henti, dan oleh cabang halaman depan di {@link #process} untuk beralih ke
     * mode terdegradasi (komponen yang gagal diganti nilai kosong alih-alih menggagalkan halaman).
     */
    private static final String TRANSIENT_RETRY = "repository.public.transientRetry";
    /**
     * Kunci HMAC untuk menandatangani {@code resumptionToken} OAI-PMH, disiapkan sekali saat kelas
     * dimuat oleh {@link #oaiTokenSecret()}.
     *
     * <p><b>Catatan operasional.</b> Bila properti sistem
     * {@code ais.repository.oaiTokenSecret} tidak disetel (atau kurang dari 32 karakter), kunci
     * dibangkitkan acak per proses. Akibatnya token panen menjadi tidak sah setelah peladen
     * dimulai ulang, dan tidak dapat dipakai lintas node bila aplikasi dijalankan berkelompok.
     * Setel properti itu pada instalasi yang dipanen secara rutin.</p>
     */
    private static final byte[] OAI_TOKEN_SECRET = oaiTokenSecret();
    /**
     * Layanan baca publik: pencarian, faset, koleksi, profil penulis, sitasi, statistik pemakaian,
     * dan pemetaan berkas. Dibuat satu kali per instance servlet dan dipakai bersama seluruh
     * permintaan, jadi implementasinya harus tetap bebas state.
     */
    private final RepositoryPublicService service = new RepositoryPublicService();
    /**
     * Layanan alur kerja deposit/telaah. Di kelas ini hanya dipakai untuk tiga pertanyaan hak
     * akses yang menentukan tombol apa yang tampil di halaman: apakah pengguna administrator
     * repositori, penelaah, dan boleh menyetor. Aksi alur kerjanya sendiri dilayani servlet
     * {@code RepositoryWorkspace}, bukan servlet publik ini.
     */
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();

    /**
     * Menangani permintaan {@code GET} dengan meneruskannya ke {@link #processSafely}.
     *
     * <p>Hampir seluruh lalu lintas portal ini berupa {@code GET}; aksi yang mengubah state
     * menolak {@code GET} secara eksplisit di {@link #process} dengan
     * {@code 405 Method Not Allowed}.</p>
     *
     * @param request  permintaan servlet
     * @param response tanggapan servlet
     * @throws ServletException bila penerusan ke JSP gagal
     * @throws IOException      bila penulisan tanggapan gagal
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processSafely(request, response);
    }

    /**
     * Menangani permintaan {@code POST} dengan meneruskannya ke {@link #processSafely}.
     *
     * <p>Dipakai aksi yang mengubah state milik pengguna ({@code bookmark}, {@code savesearch},
     * {@code removepreference}, {@code helpfeedback}); semuanya menuntut token CSRF yang sah.</p>
     *
     * @param request  permintaan servlet
     * @param response tanggapan servlet
     * @throws ServletException bila penerusan ke JSP gagal
     * @throws IOException      bila penulisan tanggapan gagal
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processSafely(request, response);
    }

    /**
     * Pembungkus setiap permintaan: memasang header pengeras, menjalankan {@link #process},
     * menerjemahkan kegagalan menjadi tanggapan yang pantas, dan menutup session Hibernate.
     *
     * <p><b>Pengenal permintaan.</b> Sebuah {@code requestId} pendek dibentuk dari cap waktu dan
     * identitas objek permintaan, dipasang sebagai header {@code X-Request-Id}, dan ikut
     * disebutkan pada setiap pesan galat — sehingga keluhan pengguna dapat dicocokkan dengan
     * catatan {@code ErrorAuditUtil} tanpa membocorkan detail internal.</p>
     *
     * <p><b>Tiga kelas kegagalan.</b></p>
     * <ul>
     *   <li>{@link SecurityException} (mis. token CSRF salah) → halaman status
     *       {@code 403}.</li>
     *   <li>{@link IllegalArgumentException} (masukan tidak valid) → {@code 400}, berbentuk JSON
     *       bila permintaannya permintaan JSON, selain itu halaman status.</li>
     *   <li>Selebihnya → dicatat sebagai kegagalan yang terlihat pengguna, lalu {@code 500}
     *       berbentuk JSON, teks biasa (khusus {@code action=citation}), atau
     *       {@code sendError}.</li>
     * </ul>
     *
     * <p><b>Percobaan ulang halaman depan.</b> Khusus kegagalan umum, bila {@link #canRetryHome}
     * mengizinkan, session Hibernate ditutup, buffer tanggapan direset, penanda
     * {@link #TRANSIENT_RETRY} dipasang, header {@code X-Repository-Retry: 1} dikirim, dan
     * {@link #process} dijalankan sekali lagi. Percobaan kedua berjalan dalam mode terdegradasi
     * sehingga komponen yang tetap gagal diganti nilai kosong, bukan menggagalkan seluruh
     * halaman. Bila percobaan ulang juga gagal, kegagalan <i>kedua</i>-lah yang dilaporkan.</p>
     *
     * <p><b>Session.</b> Blok {@code finally} selalu memanggil
     * {@code HibernateUtil.closeSession()} karena servlet ini berada di luar daur hidup
     * <i>OpenSessionInView</i> ZK dan memakai session {@code ThreadLocal} bawaan yang harus
     * dikembalikan ke kolam setiap permintaan.</p>
     *
     * @param request  permintaan servlet
     * @param response tanggapan servlet
     * @throws IOException      bila penulisan tanggapan galat gagal
     * @throws ServletException bila penerusan ke JSP gagal
     */
    private void processSafely(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String requestId = Long.toHexString(System.currentTimeMillis()) + "-"
                + Integer.toHexString(System.identityHashCode(request));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("X-Request-Id", requestId);
        try {
            request.setCharacterEncoding("UTF-8");
            process(request, response, requestId);
        } catch (SecurityException e) {
            if (!response.isCommitted()) renderState(request,response,HttpServletResponse.SC_FORBIDDEN,"Akses tidak diizinkan",e.getMessage(),requestId);
        } catch (IllegalArgumentException e) {
            if(!response.isCommitted()){if(isJsonRequest(request))writeJsonError(response,HttpServletResponse.SC_BAD_REQUEST,"INVALID_REQUEST",e.getMessage(),requestId);else renderState(request,response,HttpServletResponse.SC_BAD_REQUEST,"Permintaan tidak valid",e.getMessage(),requestId);}
        } catch (Exception e) {
            Exception visibleFailure=e;
            if(canRetryHome(request,response)){
                ais.common.ErrorAuditUtil.record(e,"Repository home transient failure; retry otomatis "+requestId);
                HibernateUtil.closeSession();
                try{
                    if(!response.isCommitted())response.resetBuffer();
                    request.setAttribute(TRANSIENT_RETRY,Boolean.TRUE);
                    response.setHeader("X-Repository-Retry","1");
                    process(request,response,requestId);
                    return;
                }catch(Exception retryFailure){visibleFailure=retryFailure;}
            }
            ais.common.ErrorAuditUtil.recordVisibleFailure(visibleFailure,
                    "Repository public servlet", request, requestId);
            if (!response.isCommitted()) {
                if (isJsonRequest(request)) {
                    writeJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "INTERNAL_ERROR", "Repository belum dapat melayani permintaan ini.", requestId);
                } else if ("citation".equalsIgnoreCase(clean(request.getParameter("action")))) {
                    writePlainError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Sitasi belum dapat dibuat. Silakan coba kembali. ID: " + requestId);
                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Repository belum dapat melayani permintaan ini. ID: " + requestId);
                }
            }
        } finally {
            // This servlet runs outside ZK's OpenSessionInView lifecycle. The
            // repository service therefore uses a native ThreadLocal session,
            // which must be returned to the pool after every request.
            HibernateUtil.closeSession();
        }
    }

    /**
     * Perutean dan pengisian model: menentukan tampilan mana yang diminta, menjalankan aksi yang
     * membalas sendiri, lalu — bila permintaannya berupa halaman — menyiapkan atribut untuk JSP
     * dan meneruskannya.
     *
     * <p><b>1. Penentuan rute.</b> Nilai awal diambil dari parameter {@code action} dan
     * {@code view} (keduanya dijadikan huruf kecil). Sesudah itu {@code getPathInfo()} diperiksa
     * dan, bila cocok, <b>menimpa</b> nilai tersebut: {@code /search} dan awalan {@code /browse}
     * menjadi tampilan pencarian; {@code /collections}, {@code /policies}, {@code /help},
     * {@code /ask} menjadi tampilan senama; {@code /rss/recent} menjadi aksi {@code feed};
     * {@code /sitemap.xml} langsung dilayani {@link #sitemap}; {@code /oai/request} dialihkan ke
     * servlet {@code /oai} beserta query string aslinya. Pola bersegmen {@code /item/{id}},
     * {@code /collection/{id}}, dan {@code /author/{nama}} mengisi {@code routeId} atau
     * {@code routeAuthor}; nama penulis di-{@code URLDecoder.decode} dan diperlakukan sebagai
     * teks pencarian, bukan sebagai jalur berkas.</p>
     *
     * <p><b>2. Pembatasan laju</b> per alamat IP, sebelum pekerjaan berat apa pun dimulai: 300
     * permintaan pencarian/saran per jam, 120 unduhan per jam, dan 20 umpan balik panduan per
     * jam. Pelanggaran dijawab {@code 429} oleh {@link #tooManyRequests}.</p>
     *
     * <p><b>3. Aksi yang membalas sendiri</b> dan langsung {@code return}: {@code search} dan
     * {@code suggest} (JSON), {@code download}, {@code citation}, {@code feed}, {@code oai},
     * serta {@code robots.txt}/{@code sitemap.xml} bila servlet dipanggil pada jalur itu.</p>
     *
     * <p><b>4. Aksi yang mengubah state.</b> {@code bookmark}, {@code savesearch}, dan
     * {@code removepreference} menuntut tiga hal berurutan: pengguna sudah masuk (jika tidak,
     * halaman status {@code 401}), metode {@code POST} (jika tidak, {@code 405}), dan token CSRF
     * yang sah ({@link #verifyPublicCsrf}). Ketiganya bekerja atas {@code actor.getUserId()} dari
     * sesi, sehingga pemanggil tidak dapat menyunting preferensi pengguna lain. Tujuan pengalihan
     * yang berasal dari parameter disaring {@link #safeRepositoryUrl}. Aksi
     * {@code helpfeedback} memakai gerbang yang sama kecuali syarat login — umpan balik panduan
     * memang boleh anonim — dan ditambah pembatas laju tersendiri.</p>
     *
     * <p><b>5. Penyiapan halaman.</b> Untuk permintaan yang berujung pada JSP, atribut yang
     * dipasang meliputi {@code repoView}, {@code repoPublicOrigin}, {@code repoCsrf} (dibangkitkan
     * sekali per sesi), {@code repoPublicUser}, {@code repoIsAdmin}, {@code repoIsReviewer},
     * {@code repoCanDeposit}, dan {@code repoAnonymousFullText}. Pengunjung anonim pada
     * {@code GET} juga menerima header penanda {@code X-Repository-Cacheable: public} untuk
     * lapisan cache di depan aplikasi.</p>
     *
     * <p><b>6. Cabang per tampilan.</b> {@code item} memuat detail butir dan, bila tidak
     * ditemukan, mencoba versi nisannya; <b>berkas disembunyikan dari pengunjung anonim</b> bila
     * {@code anonymousFullTextAllowed()} bernilai {@code false}. Kunjungan dicatat
     * {@code recordUsage} kecuali butir sudah ditarik. Tampilan {@code search}/{@code browse},
     * {@code collection}, {@code author}, {@code collections}, dan {@code ask} masing-masing
     * mengisi atribut yang dibutuhkan JSP; {@code policies} dan {@code help} tidak butuh data.
     * Selain itu jatuh ke halaman depan, yang memuat banyak komponen secara terpisah agar satu
     * komponen yang gagal tidak menjatuhkan seluruh halaman — lihat {@link #logDegraded}.</p>
     *
     * @param request   permintaan servlet
     * @param response  tanggapan servlet
     * @param requestId pengenal permintaan untuk pelacakan galat
     * @throws Exception dilempar ke {@link #processSafely} untuk diterjemahkan menjadi tanggapan
     */
    private void process(HttpServletRequest request, HttpServletResponse response, String requestId) throws Exception {
        String action = clean(request.getParameter("action")).toLowerCase();
        String view = clean(request.getParameter("view")).toLowerCase();
        Long routeId = null;
        String routeAuthor = "";
        String path = clean(request.getPathInfo());
        if (path.length() > 0 && !"/".equals(path)) {
            String[] segments = path.split("/");
            if (path.equals("/search") || path.startsWith("/browse")) view = "search";
            else if (path.equals("/collections")) view = "collections";
            else if (path.equals("/policies") || path.startsWith("/policies/")) view = "policies";
            else if (path.equals("/help")) view = "help";
            else if(path.equals("/ask"))view="ask";
            else if (path.equals("/rss/recent")) action = "feed";
            else if (path.equals("/sitemap.xml")) { sitemap(request,response); return; }
            else if (path.equals("/oai/request")) { response.sendRedirect(request.getContextPath()+"/oai"+(request.getQueryString()==null?"":"?"+request.getQueryString())); return; }
            else if (segments.length > 2 && "item".equals(segments[1])) { view="item"; routeId=parseLong(segments[2]); }
            else if (segments.length > 2 && "collection".equals(segments[1])) { view="collection"; routeId=parseLong(segments[2]); request.setAttribute("repoRouteCollectionId",routeId); }
            else if (segments.length > 2 && "author".equals(segments[1])) { view="author"; routeAuthor=URLDecoder.decode(segments[2],"UTF-8"); }
        }
        String ip = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        if (("search".equals(action) || "suggest".equals(action) || "search".equals(view) || "browse".equals(view))
                && !PublicRegistrationRateLimiter.izinkan("repository-search|" + ip, 300, 3600000L)) {
            tooManyRequests(response, requestId); return;
        }
        if ("download".equals(action)
                && !PublicRegistrationRateLimiter.izinkan("repository-download|" + ip, 120, 3600000L)) {
            tooManyRequests(response, requestId); return;
        }
        if (request.getServletPath().endsWith("robots.txt")) { robots(request, response); return; }
        if (request.getServletPath().endsWith("sitemap.xml")) { sitemap(request, response); return; }
        if ("search".equals(action)) {
            writeSearchJson(response, service.search(queryFrom(request)), requestId);
            return;
        }
        if ("suggest".equals(action)) {
            writeSuggestionJson(response, service.suggest(clean(request.getParameter("q")),
                    clean(request.getParameter("field")), 8), requestId);
            return;
        }
        if ("bookmark".equals(action) || "savesearch".equals(action) || "removepreference".equals(action)) {
            Tbmuser actor=Common.getCurrentUser(request);if(actor==null){renderState(request,response,HttpServletResponse.SC_UNAUTHORIZED,"Sesi telah berakhir","Silakan masuk kembali untuk melanjutkan.",requestId);return;}
            if(!"POST".equalsIgnoreCase(request.getMethod())){response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);return;}
            verifyPublicCsrf(request.getSession(false),request.getParameter("csrf"));
            if("bookmark".equals(action)){Long itemId=parseLong(request.getParameter("id"));service.toggleBookmark(actor.getUserId(),itemId);response.sendRedirect(request.getContextPath()+"/repository/item/"+itemId);return;}
            if("savesearch".equals(action)){service.saveSearch(actor.getUserId(),clean(request.getParameter("label")),safeRepositoryUrl(request,request.getParameter("queryValue")),"true".equalsIgnoreCase(request.getParameter("alert")));response.sendRedirect(safeRepositoryUrl(request,request.getParameter("returnTo")));return;}
            service.removePreference(actor.getUserId(),parseLong(request.getParameter("preferenceId")));response.sendRedirect(request.getContextPath()+"/repository");return;
        }
        if("helpfeedback".equals(action)){
            if(!"POST".equalsIgnoreCase(request.getMethod())){response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);return;}
            verifyPublicCsrf(request.getSession(false),request.getParameter("csrf"));
            if(!PublicRegistrationRateLimiter.izinkan("repository-help-feedback|"+ip,20,3600000L)){tooManyRequests(response,requestId);return;}
            String rating=clean(request.getParameter("helpful"));if(!"yes".equals(rating)&&!"no".equals(rating))throw new IllegalArgumentException("Pilih apakah panduan membantu.");
            Tbmuser feedbackActor=Common.getCurrentUser(request);service.recordHelpFeedback(clean(request.getParameter("contentKey")),"yes".equals(rating),request.getParameter("comment"),ip,feedbackActor==null?"":feedbackActor.getUserId());
            response.sendRedirect(request.getContextPath()+"/repository/help?feedback=thanks#help-feedback");return;
        }
        if ("download".equals(action)) {
            download(request, response);
            return;
        }
        if ("citation".equals(action)) {
            citation(request, response);
            return;
        }
        if ("feed".equals(action)) {
            feed(request, response);
            return;
        }
        if ("oai".equals(action)) {
            oai(request, response);
            return;
        }

        if (view.length() == 0) view = "home";
        request.setAttribute("repoView", view);
        request.setAttribute("repoPublicOrigin", publicOrigin(request));
        Tbmuser publicUser = Common.getCurrentUser(request);
        if(publicUser==null&&"GET".equalsIgnoreCase(request.getMethod()))response.setHeader("X-Repository-Cacheable","public");
        HttpSession publicSession=request.getSession(true);String publicCsrf=(String)publicSession.getAttribute(CSRF);if(publicCsrf==null){publicCsrf=UUID.randomUUID().toString()+UUID.randomUUID().toString();publicSession.setAttribute(CSRF,publicCsrf);}request.setAttribute("repoCsrf",publicCsrf);
        request.setAttribute("repoPublicUser", publicUser);
        request.setAttribute("repoIsAdmin",Boolean.valueOf(workflow.isRepositoryAdministrator(publicUser)));
        request.setAttribute("repoIsReviewer", Boolean.valueOf(workflow.isRepositoryAdmin(publicUser)&&!workflow.isRepositoryAdministrator(publicUser)));
        boolean canDeposit=workflow.canDeposit(publicUser);
        if(canDeposit){try{canDeposit=service.hasDepositCollection();}catch(Exception e){logDegraded(e,"deposit-availability",requestId);canDeposit=false;}}
        request.setAttribute("repoCanDeposit",Boolean.valueOf(canDeposit));
        request.setAttribute("repoAnonymousFullText",Boolean.valueOf(service.anonymousFullTextAllowed()));

        if ("item".equals(view)) {
            Long itemId=routeId==null?parseLong(request.getParameter("id")):routeId;
            ItemDetail detail = service.findPublicItem(itemId);
            if (detail == null) {
                detail = service.findTombstone(itemId);
                if (detail == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publikasi tidak ditemukan."); return; }
            }
            request.setAttribute("repoItem", detail);
            if(publicUser==null&&!service.anonymousFullTextAllowed()){detail.files.clear();detail.pdfAvailable=false;}
            if(publicUser!=null){boolean bookmarked=false;for(RepositoryPublicService.PreferenceView p:service.preferences(publicUser.getUserId(),"BOOKMARK",100))if(detail.id.equals(p.itemId)){bookmarked=true;break;}request.setAttribute("repoBookmarked",Boolean.valueOf(bookmarked));}
            if(!detail.withdrawn){service.recordUsage(detail.id, null, "VIEW", request.getRemoteAddr(), request.getHeader("User-Agent"), actorId(request),country(request),request.getHeader("Referer"));detail.viewCount++;}
        } else if ("search".equals(view) || "browse".equals(view)) {
            Query searchQuery=queryFrom(request);
            SearchResult searchResult=service.search(searchQuery);
            if(searchResult==null){searchResult=new SearchResult();searchResult.query=searchQuery;}
            if(searchResult.query==null)searchResult.query=searchQuery;
            if(searchResult.items==null)searchResult.items=Collections.emptyList();
            request.setAttribute("repoSearch",searchResult);
            if(publicUser!=null){request.setAttribute("repoSavedSearches",service.preferences(publicUser.getUserId(),"SAVED_SEARCH",20));request.setAttribute("repoSearchAlerts",service.preferences(publicUser.getUserId(),"SEARCH_ALERT",20));}
        } else if ("collection".equals(view)) {
            CollectionView collection=service.findCollection(routeId==null?parseLong(request.getParameter("id")):routeId);
            if(collection==null){response.sendError(HttpServletResponse.SC_NOT_FOUND,"Koleksi tidak ditemukan.");return;}
            request.setAttribute("repoCollection",collection); request.setAttribute("repoSearch",service.search(queryFrom(request)));
        } else if ("author".equals(view)) {
            String name=routeAuthor.length()==0?clean(request.getParameter("name")):routeAuthor;
            Integer authorPageValue=parseInteger(request.getParameter("page"));
            Integer authorSizeValue=parseInteger(request.getParameter("size"));
            int authorPage=authorPageValue==null?1:authorPageValue.intValue();
            int authorSize=authorSizeValue==null?RepositoryPublicService.DEFAULT_PAGE_SIZE:authorSizeValue.intValue();
            AuthorProfile author=service.authorProfile(name,authorPage,authorSize);if(author==null){response.sendError(HttpServletResponse.SC_NOT_FOUND,"Profil penulis tidak ditemukan.");return;}
            request.setAttribute("repoAuthor",author);
        } else if("collections".equals(view)){
            Query catalogQuery=new Query();catalogQuery.pageSize=1;SearchResult catalog=service.search(catalogQuery);
            request.setAttribute("repoCollections",service.listCollections(100));request.setAttribute("repoLatestCollections",service.latestCollections(8));
            request.setAttribute("repoLatest",service.latest(8));request.setAttribute("repoCollectionFacets",catalog);
        } else if("ask".equals(view)){
            request.setAttribute("repoAnswer",service.askRepository(clean(request.getParameter("q"))));
            Integer faqPageValue=parseInteger(request.getParameter("faqPage"));
            Integer faqSizeValue=parseInteger(request.getParameter("faqSize"));
            request.setAttribute("repoFaq",service.faqCatalog(request.getParameter("faq"),request.getParameter("faqCategory"),
                    faqPageValue==null?1:faqPageValue.intValue(),faqSizeValue==null?12:faqSizeValue.intValue()));
        } else if ("policies".equals(view) || "help".equals(view)) {
            // Rendered by the allow-listed JSP view.
        } else {
            request.setAttribute("repoView", "home");
            boolean retryMode=Boolean.TRUE.equals(request.getAttribute(TRANSIENT_RETRY));
            try{request.setAttribute("repoSummary",service.loadSummary());}catch(Exception e){if(!retryMode)throw e;logDegraded(e,"summary",requestId);request.setAttribute("repoSummary",new RepositoryPublicService.Summary());}
            try{request.setAttribute("repoCollections",service.listCollections(12));}catch(Exception e){if(!retryMode)throw e;logDegraded(e,"collections",requestId);request.setAttribute("repoCollections",Collections.emptyList());}
            Integer requestedLatestPage=parseInteger(request.getParameter("latestPage"));
            int latestPage=requestedLatestPage==null?1:requestedLatestPage.intValue();
            SearchResult latestResult;
            try{latestResult=service.latestPage(latestPage,6);}catch(Exception e){if(!retryMode)throw e;logDegraded(e,"latest",requestId);latestResult=new SearchResult();latestResult.query=new Query();latestResult.query.page=1;latestResult.query.pageSize=6;}
            if(latestResult==null){latestResult=new SearchResult();latestResult.query=new Query();latestResult.query.page=1;latestResult.query.pageSize=6;}
            if(latestResult.query==null){latestResult.query=new Query();latestResult.query.page=latestPage;latestResult.query.pageSize=6;}
            if(latestResult.items==null){latestResult.items=Collections.emptyList();}
            request.setAttribute("repoLatestPage",latestResult);
            request.setAttribute("repoLatest",latestResult.items);
            try{request.setAttribute("repoPopularCollections",service.popularCollections(6));}catch(Exception e){logDegraded(e,"popular-collections",requestId);request.setAttribute("repoPopularCollections",Collections.emptyList());}
            try{request.setAttribute("repoPopularTopics",service.popularSubjects(10));}catch(Exception e){logDegraded(e,"popular-topics",requestId);request.setAttribute("repoPopularTopics",Collections.emptyMap());}
            try{request.setAttribute("repoFeatured",service.featured(6));}catch(Exception e){logDegraded(e,"featured",requestId);request.setAttribute("repoFeatured",Collections.emptyList());}
            try{request.setAttribute("repoMostDownloaded",service.mostDownloaded(clean(request.getParameter("downloadPeriod")),6));}catch(Exception e){logDegraded(e,"most-downloaded",requestId);request.setAttribute("repoMostDownloaded",Collections.emptyList());}
            if(publicUser!=null){try{request.setAttribute("repoRecommendations",service.recommendations(publicUser.getUserId(),4));}catch(Exception e){logDegraded(e,"recommendations",requestId);request.setAttribute("repoRecommendations",Collections.emptyList());}try{request.setAttribute("repoSearchAlerts",service.preferences(publicUser.getUserId(),"SEARCH_ALERT",10));}catch(Exception e){logDegraded(e,"search-alerts",requestId);request.setAttribute("repoSearchAlerts",Collections.emptyList());}}
        }
        forwardRepositoryJsp(request,response);
    }

    /**
     * Meneruskan permintaan ke template tunggal {@link #JSP}.
     *
     * <p>Pencarian {@code RequestDispatcher} dilakukan dua kali: lewat permintaan, lalu lewat
     * {@code ServletContext} bila yang pertama {@code null} — perbedaan yang muncul ketika servlet
     * dipanggil dari konteks penerusan lain. Bila keduanya gagal, {@link ServletException}
     * dilempar dengan menyebut nama template, karena penyebabnya selalu masalah pemasangan
     * (berkas JSP tidak ikut ter-<i>deploy</i>), bukan masukan pengguna.</p>
     *
     * <p>Nama template adalah konstanta; tidak ada bagian dari permintaan yang ikut menyusunnya.</p>
     *
     * @param request  permintaan servlet yang atributnya sudah diisi {@link #process}
     * @param response tanggapan servlet
     * @throws ServletException bila template tidak ditemukan atau JSP melempar
     * @throws IOException      bila penulisan tanggapan gagal
     */
    private void forwardRepositoryJsp(HttpServletRequest request,HttpServletResponse response)
            throws ServletException,IOException {
        RequestDispatcher dispatcher=request.getRequestDispatcher(JSP);
        if(dispatcher==null)dispatcher=getServletContext().getRequestDispatcher(JSP);
        if(dispatcher==null)throw new ServletException("Template Repository tidak ditemukan: "+JSP);
        dispatcher.forward(request,response);
    }

    /**
     * Menentukan apakah sebuah kegagalan boleh dijawab dengan satu kali percobaan ulang otomatis.
     *
     * <p>Syaratnya sengaja ketat, dan semuanya harus terpenuhi:</p>
     * <ul>
     *   <li>tanggapan belum ter-<i>commit</i> (masih mungkin direset);</li>
     *   <li>permintaan ini belum merupakan percobaan ulang ({@link #TRANSIENT_RETRY} belum
     *       terpasang) — inilah yang mencegah pengulangan berantai;</li>
     *   <li>metodenya {@code GET}, sehingga pengulangan tidak dapat menggandakan efek samping;</li>
     *   <li>tidak ada parameter {@code action} — aksi yang membalas sendiri tidak diulang;</li>
     *   <li>tampilan dan jalurnya kosong atau halaman depan.</li>
     * </ul>
     * <p>Dengan kata lain hanya halaman depan anonim yang diulang, yaitu satu-satunya halaman yang
     * memuat banyak komponen sekaligus dan karena itu paling rentan terhadap kegagalan sesaat.</p>
     *
     * @param request  permintaan servlet yang sedang gagal
     * @param response tanggapan servlet, diperiksa status <i>commit</i>-nya
     * @return {@code true} bila {@link #process} boleh dijalankan sekali lagi
     */
    private boolean canRetryHome(HttpServletRequest request,HttpServletResponse response){
        if(response.isCommitted()||Boolean.TRUE.equals(request.getAttribute(TRANSIENT_RETRY))||!"GET".equalsIgnoreCase(request.getMethod()))return false;
        if(clean(request.getParameter("action")).length()>0)return false;
        String view=clean(request.getParameter("view")).toLowerCase(),path=clean(request.getPathInfo());
        return (view.length()==0||"home".equals(view))&&(path.length()==0||"/".equals(path));
    }

    /**
     * Mencatat bahwa satu komponen halaman gagal dimuat sementara halaman tetap disajikan.
     *
     * <p>Dipakai cabang halaman depan {@link #process}, yang memuat ringkasan, koleksi, terbaru,
     * koleksi populer, topik populer, pilihan redaksi, paling banyak diunduh, rekomendasi, dan
     * lansiran pencarian secara terpisah. Kegagalan salah satunya menghasilkan bagian kosong,
     * bukan halaman galat. Karena kegagalan seperti itu tidak terlihat pengunjung, catatan inilah
     * satu-satunya jejak yang tertinggal — cari nama komponen dan {@code requestId} pada catatan
     * {@code ErrorAuditUtil} bila ada bagian halaman yang dilaporkan hilang.</p>
     *
     * @param failure   kegagalan yang ditelan
     * @param component nama komponen halaman, mis. {@code "summary"} atau {@code "featured"}
     * @param requestId pengenal permintaan agar dapat dicocokkan dengan header
     *                  {@code X-Request-Id}
     */
    private void logDegraded(Exception failure,String component,String requestId){
        ais.common.ErrorAuditUtil.record(failure,"Repository home degraded component="+component+" request="+requestId);
    }

    /**
     * Menyusun objek {@link Query} dari parameter permintaan untuk seluruh jalur pencarian.
     *
     * <p>Parameter yang dibaca: {@code q} (kata kunci), {@code field} (ruas yang dicari),
     * {@code author}, {@code subject}, {@code language}, {@code identifier}, {@code program},
     * {@code scope}, {@code fullText}, {@code semantic} (boolean), tiga penyaring frasa
     * {@code exact}/{@code any}/{@code without}, {@code collection}, {@code type},
     * {@code access}, {@code year}, {@code yearFrom}, {@code yearUntil}, {@code sort}, serta
     * {@code page} dan {@code size}.</p>
     *
     * <p>Semua nilai teks dilewatkan {@link #clean} sehingga {@code null} menjadi string kosong
     * dan tidak pernah ada {@code NullPointerException} di hilir. Nilai angka dilewatkan
     * {@link #parseInteger}/{@link #parseLong} yang mengembalikan {@code null} untuk masukan tak
     * valid, bukan melempar. Halaman bawaan adalah 1 dan ukuran bawaan
     * {@code RepositoryPublicService.DEFAULT_PAGE_SIZE}.</p>
     *
     * <p>Identitas koleksi diambil lebih dulu dari atribut {@code repoRouteCollectionId} yang
     * dipasang perutean {@code /collection/{id}}; hanya bila atribut itu tidak ada barulah
     * parameter {@code collection} dipakai. Dengan begitu rute URL bersih menang atas parameter,
     * dan pengunjung tidak dapat mencampuradukkan koleksi lewat parameter tambahan pada URL
     * koleksi.</p>
     *
     * <p>Sebelum dikembalikan, objek dilewatkan {@code service.normalize(q)} — di situlah batas
     * atas ukuran halaman dan nilai pengurut yang diizinkan ditegakkan, bukan di kelas ini.</p>
     *
     * @param request permintaan servlet
     * @return objek {@link Query} yang sudah dinormalkan layanan
     */
    private Query queryFrom(HttpServletRequest request) {
        Query q = new Query();
        q.keyword = clean(request.getParameter("q"));
        q.searchField = clean(request.getParameter("field"));
        q.author = clean(request.getParameter("author"));
        q.subject = clean(request.getParameter("subject"));
        q.language = clean(request.getParameter("language"));
        q.identifier = clean(request.getParameter("identifier"));
        q.programStudy = clean(request.getParameter("program"));
        q.searchScope = clean(request.getParameter("scope"));
        q.fullText = clean(request.getParameter("fullText"));
        q.semantic = "true".equalsIgnoreCase(request.getParameter("semantic"));
        q.exactPhrase = clean(request.getParameter("exact"));
        q.anyWords = clean(request.getParameter("any"));
        q.withoutWords = clean(request.getParameter("without"));
        q.collectionId = request.getAttribute("repoRouteCollectionId") instanceof Long
                ? (Long)request.getAttribute("repoRouteCollectionId") : parseLong(request.getParameter("collection"));
        q.documentType = clean(request.getParameter("type"));
        q.accessPolicy = clean(request.getParameter("access"));
        q.year = parseInteger(request.getParameter("year"));
        q.yearFrom = parseInteger(request.getParameter("yearFrom"));
        q.yearUntil = parseInteger(request.getParameter("yearUntil"));
        q.sort = clean(request.getParameter("sort"));
        Integer page = parseInteger(request.getParameter("page"));
        Integer size = parseInteger(request.getParameter("size"));
        q.page = page == null ? 1 : page.intValue();
        q.pageSize = size == null ? RepositoryPublicService.DEFAULT_PAGE_SIZE : size.intValue();
        return service.normalize(q);
    }

    /**
     * Menuliskan hasil pencarian sebagai JSON untuk aksi {@code action=search}.
     *
     * <p>Struktur balikan: {@code status} ("OK"), {@code requestId}, metadata penomoran halaman
     * ({@code page}, {@code pageSize}, {@code total}, {@code totalPages}, {@code searchField}),
     * larik {@code items}, dan objek {@code facets}.</p>
     *
     * <p>Tiap elemen {@code items} adalah kartu ringkas — {@code id}, {@code title},
     * {@code authors}, {@code abstract}, {@code year}, {@code documentType},
     * {@code accessPolicy}, {@code collection}, {@code oaiIdentifier}, {@code programStudy},
     * {@code publicFileCount}, {@code pdfAvailable}, {@code superseded}, {@code viewCount},
     * {@code downloadCount}. Perhatikan bahwa yang disalin hanya ruas kartu; berkas dan metadata
     * lengkap tidak pernah ikut, sehingga API ini tidak dapat dipakai memanen naskah lengkap.</p>
     *
     * <p>{@code facets} memuat sepuluh peta hitungan: {@code type}, {@code access}, {@code year},
     * {@code author}, {@code subject}, {@code language}, {@code program}, {@code source},
     * {@code license}, dan {@code fullText}.</p>
     *
     * @param response  tanggapan servlet
     * @param result    hasil pencarian dari {@code RepositoryPublicService}
     * @param requestId pengenal permintaan yang ikut dikembalikan ke klien
     * @throws Exception bila penyusunan JSON atau penulisan tanggapan gagal
     */
    private void writeSearchJson(HttpServletResponse response, SearchResult result, String requestId) throws Exception {
        JSONObject root = new JSONObject();
        root.put("status", "OK");
        root.put("requestId", requestId);
        root.put("page", result.query.page);
        root.put("pageSize", result.query.pageSize);
        root.put("total", result.total);
        root.put("totalPages", result.totalPages);
        root.put("searchField", result.query.searchField);
        JSONArray items = new JSONArray();
        for (int i = 0; i < result.items.size(); i++) {
            ItemCard item = result.items.get(i);
            JSONObject row = new JSONObject();
            row.put("id", item.id);
            row.put("title", item.title);
            row.put("authors", item.authors);
            row.put("abstract", item.abstractText);
            row.put("year", item.year);
            row.put("documentType", item.documentType);
            row.put("accessPolicy", item.accessPolicy);
            row.put("collection", item.collectionName);
            row.put("oaiIdentifier", item.oaiIdentifier);
            row.put("programStudy", item.programStudy);
            row.put("publicFileCount", item.publicFileCount);
            row.put("pdfAvailable", item.pdfAvailable);
            row.put("superseded",item.superseded);
            row.put("viewCount", item.viewCount);
            row.put("downloadCount", item.downloadCount);
            items.put(row);
        }
        root.put("items", items);
        JSONObject facets = new JSONObject();
        facets.put("type", new JSONObject(result.typeFacets));
        facets.put("access", new JSONObject(result.accessFacets));
        facets.put("year", new JSONObject(result.yearFacets));
        facets.put("author",new JSONObject(result.authorFacets));facets.put("subject",new JSONObject(result.subjectFacets));
        facets.put("language",new JSONObject(result.languageFacets));facets.put("program",new JSONObject(result.programFacets));
        facets.put("source",new JSONObject(result.sourceFacets));facets.put("license",new JSONObject(result.licenseFacets));facets.put("fullText",new JSONObject(result.fullTextFacets));
        root.put("facets", facets);
        writeJson(response, root, HttpServletResponse.SC_OK);
    }

    /**
     * Menuliskan daftar saran ketik-cepat sebagai JSON untuk aksi {@code action=suggest}.
     *
     * <p>Balikan berisi {@code status} ("OK"), {@code requestId}, dan larik {@code suggestions}
     * yang tiap elemennya memuat {@code type} (jenis saran: judul, penulis, subjek, dan
     * seterusnya), {@code label} (teks yang ditampilkan), {@code detail} (keterangan pendamping),
     * serta {@code value} (nilai yang dimasukkan ke kotak pencarian bila saran dipilih).</p>
     *
     * <p>Jumlah saran dibatasi delapan oleh pemanggil di {@link #process}, dan jalur ini termasuk
     * yang dibatasi laju 300 permintaan per jam per alamat IP.</p>
     *
     * @param response    tanggapan servlet
     * @param suggestions daftar saran dari {@code RepositoryPublicService}
     * @param requestId   pengenal permintaan yang ikut dikembalikan ke klien
     * @throws Exception bila penyusunan JSON atau penulisan tanggapan gagal
     */
    private void writeSuggestionJson(HttpServletResponse response, List<Suggestion> suggestions, String requestId) throws Exception {
        JSONObject root = new JSONObject(); root.put("status", "OK"); root.put("requestId", requestId);
        JSONArray rows = new JSONArray();
        for (Suggestion suggestion : suggestions) {
            JSONObject row = new JSONObject(); row.put("type", suggestion.type); row.put("label", suggestion.label);
            row.put("detail", suggestion.detail); row.put("value", suggestion.value); rows.put(row);
        }
        root.put("suggestions", rows); writeJson(response, root, HttpServletResponse.SC_OK);
    }

    /**
     * Memastikan token CSRF yang dikirim formulir cocok dengan token yang tersimpan di sesi.
     *
     * <p>Token yang diharapkan dibaca dari atribut sesi {@link #CSRF}, yang diisi sekali per sesi
     * di {@link #process} berupa gabungan dua {@link UUID}. Pembandingan memakai
     * {@link #constantTime} agar durasi pemeriksaan tidak membocorkan seberapa banyak karakter
     * awal yang sudah benar.</p>
     *
     * <p>Sesi {@code null} — pemanggil yang belum pernah memuat halaman mana pun — diperlakukan
     * sebagai gagal, sama seperti token yang salah. Kegagalan melempar {@link SecurityException},
     * yang oleh {@link #processSafely} diterjemahkan menjadi halaman status {@code 403}.</p>
     *
     * @param session  sesi HTTP; boleh {@code null}
     * @param supplied token yang dikirim klien pada parameter {@code csrf}
     * @throws SecurityException bila token tidak sah atau sesi sudah berakhir
     */
    private void verifyPublicCsrf(HttpSession session,String supplied){String expected=session==null?null:(String)session.getAttribute(CSRF);if(!constantTime(expected,supplied))throw new SecurityException("Token keamanan tidak valid atau sesi telah berakhir.");}
    /**
     * Membandingkan dua string dengan waktu yang tidak bergantung pada posisi perbedaan pertama.
     *
     * <p>Perbedaan panjang dan perbedaan tiap karakter diakumulasikan lewat operasi XOR ke dalam
     * satu variabel, dan perulangan selalu berjalan sepanjang string terpendek — tidak ada
     * {@code return} lebih awal. Ini mencegah serangan pengukuran waktu yang menebak token
     * karakter demi karakter.</p>
     *
     * <p>Perlu dicatat bahwa perbedaan panjang tetap memengaruhi jumlah putaran, jadi jaminannya
     * berlaku untuk isi token yang panjangnya sama — cukup untuk token CSRF yang panjangnya
     * tetap. {@code null} pada salah satu sisi langsung bernilai gagal.</p>
     *
     * @param a string pembanding pertama; boleh {@code null}
     * @param b string pembanding kedua; boleh {@code null}
     * @return {@code true} hanya bila keduanya bukan {@code null} dan isinya sama persis
     */
    private boolean constantTime(String a,String b){if(a==null||b==null)return false;int diff=a.length()^b.length(),n=Math.min(a.length(),b.length());for(int i=0;i<n;i++)diff|=a.charAt(i)^b.charAt(i);return diff==0;}
    /**
     * Menyaring URL tujuan pengalihan agar tidak pernah keluar dari portal Repository —
     * penangkal <i>open redirect</i>.
     *
     * <p>Nilai diterima hanya bila sudah berupa {@code contextPath + "/repository"} persis, atau
     * berlanjut dengan {@code /} atau {@code ?}. Bentuk tanpa {@code contextPath}
     * ({@code /repository}, {@code /repository/...}, {@code /repository?...}) juga diterima dan
     * diberi awalan {@code contextPath}. <b>Semua nilai lain diganti</b> dengan halaman depan
     * Repository — termasuk URL absolut ke situs luar, jalur ke bagian aplikasi lain, dan bentuk
     * yang menyerupai jalur tetapi tidak cocok seperti {@code /repository-workspace}, yang
     * tersaring karena awalan yang diuji selalu menyertakan pemisah.</p>
     *
     * <p>Dipakai pada dua tempat: parameter {@code queryValue} saat menyimpan pencarian, dan
     * parameter {@code returnTo} saat mengembalikan pengguna setelah aksi.</p>
     *
     * @param request permintaan servlet, untuk membaca {@code contextPath}
     * @param value   URL yang diusulkan klien; boleh {@code null}
     * @return URL yang aman untuk {@code sendRedirect}, selalu di dalam portal Repository
     */
    private String safeRepositoryUrl(HttpServletRequest request,String value){String context=request.getContextPath(),v=clean(value),local=context+"/repository";if(v.equals(local)||v.startsWith(local+"/")||v.startsWith(local+"?"))return v;if(v.equals("/repository")||v.startsWith("/repository/")||v.startsWith("/repository?"))return context+v;return local;}
    /**
     * Menyajikan halaman status berbingkai portal (bukan halaman galat mentah container) untuk
     * kondisi seperti {@code 401} sesi berakhir, {@code 403} akses ditolak, dan {@code 400}
     * permintaan tidak valid.
     *
     * <p>Kode status HTTP tetap disetel sebenarnya, lalu atribut {@code repoView="state"} beserta
     * {@code repoStateCode}, {@code repoStateTitle}, {@code repoStateMessage}, dan
     * {@code repoRequestId} dipasang sebelum diteruskan ke {@link #JSP}. Dengan begitu pengunjung
     * tetap melihat tajuk dan navigasi portal, dan {@code requestId} yang tampil dapat ia sebutkan
     * saat melapor.</p>
     *
     * <p>{@code repoPublicUser} ikut diisi ulang di sini karena halaman status dapat muncul
     * sebelum {@link #process} sempat memasangnya.</p>
     *
     * @param request   permintaan servlet
     * @param response  tanggapan servlet
     * @param status    kode status HTTP yang dikirim
     * @param title     judul singkat untuk pengunjung
     * @param message   penjelasan untuk pengunjung
     * @param requestId pengenal permintaan yang ditampilkan di halaman
     * @throws ServletException bila penerusan ke JSP gagal
     * @throws IOException      bila penulisan tanggapan gagal
     */
    private void renderState(HttpServletRequest request,HttpServletResponse response,int status,String title,String message,String requestId)throws ServletException,IOException{response.setStatus(status);request.setAttribute("repoView","state");request.setAttribute("repoStateCode",Integer.valueOf(status));request.setAttribute("repoStateTitle",title);request.setAttribute("repoStateMessage",message);request.setAttribute("repoRequestId",requestId);request.setAttribute("repoPublicUser",Common.getCurrentUser(request));forwardRepositoryJsp(request,response);}

    private void citation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ItemDetail item = service.findPublicCitationItem(parseLong(request.getParameter("id")));
        if (item == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Publikasi tidak ditemukan.");
            return;
        }
        String format = clean(request.getParameter("format")).toLowerCase();
        if (!"ris".equals(format) && !"bibtex".equals(format) && !"endnote".equals(format)
                && !"csl".equals(format) && !"dcxml".equals(format) && !"apa".equals(format)
                && !"ieee".equals(format) && !"harvard".equals(format) && !"vancouver".equals(format)
                && !"chicago".equals(format) && !"text".equals(format)) format = "text";
        String body = service.citation(item, format);
        String extension = "bibtex".equals(format) ? "bib" : ("ris".equals(format) ? "ris"
                : ("endnote".equals(format) ? "enw" : ("csl".equals(format) ? "json" : ("dcxml".equals(format)?"xml":"txt"))));
        response.setContentType("csl".equals(format)
                ? "application/vnd.citationstyles.csl+json;charset=UTF-8" : ("dcxml".equals(format)?"application/xml;charset=UTF-8":"text/plain;charset=UTF-8"));
        response.setHeader("Content-Disposition", "attachment; filename=repository-" + item.id + "." + extension);
        response.getWriter().write(body);
    }

    private void writePlainError(HttpServletResponse response, int status, String message) throws IOException {
        response.resetBuffer();
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(message);
    }

    private void download(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(!service.anonymousFullTextAllowed()&&Common.getCurrentUser(request)==null){response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Silakan login menggunakan akun eCampus untuk membaca atau mengunduh naskah lengkap.");return;}
        RepoBitstream bitstream = service.findDownloadableBitstream(parseLong(request.getParameter("id")));
        if (bitstream == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Berkas tidak ditemukan atau tidak dapat diakses.");
            return;
        }
        File file = service.resolveBitstreamFile(bitstream);
        if (file == null || !file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Berkas fisik belum tersedia.");
            return;
        }
        service.recordUsage(bitstream.getItemId(), bitstream.getId(), "DOWNLOAD", request.getRemoteAddr(),
                request.getHeader("User-Agent"), actorId(request),country(request),request.getHeader("Referer"));
        String fileName = safeFileName(bitstream.getNamaFile());
        String mime = clean(bitstream.getMimeType());
        if (mime.length() == 0) mime = getServletContext().getMimeType(fileName);
        if (mime == null || mime.length() == 0) mime = "application/octet-stream";
        response.reset();
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentType(mime);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        boolean inline = "true".equalsIgnoreCase(request.getParameter("inline")) && "application/pdf".equalsIgnoreCase(mime);
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + fileName.replace("\"", "")
                + "\"; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));

        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        OutputStream output = response.getOutputStream();
        try {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            output.flush();
        } finally {
            input.close();
        }
    }

    private void oai(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String verb = clean(request.getParameter("verb"));
        String token = clean(request.getParameter("resumptionToken"));
        response.setContentType("text/xml;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        PrintWriter out = response.getWriter();
        String configuredBase = clean(System.getProperty("ais.repository.oaiBaseUrl"));
        String requestUrl = request.getRequestURL().toString();
        String base = configuredBase.length() > 0 ? configuredBase
                : (requestUrl.endsWith("/oai") ? requestUrl : requestUrl + "?action=oai");
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.print("<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">");
        out.print("<responseDate>" + xmlDate(new Date()) + "</responseDate>");
        writeOaiRequest(out, request, base, verb);

        String argumentError = oaiArgumentError(request, verb, token);
        if (argumentError.length() > 0) {
            oaiError(out, verb.length() == 0 || !isOaiVerb(verb) ? "badVerb" : "badArgument", argumentError);
            out.print("</OAI-PMH>"); out.flush(); return;
        }

        if ("Identify".equals(verb)) {
            String repositoryName = clean(System.getProperty("ais.repository.oaiRepositoryName", "Repository AIS"));
            String adminEmail = clean(System.getProperty("ais.repository.oaiAdminEmail", "repository@localhost"));
            out.print("<Identify><repositoryName>" + xml(repositoryName) + "</repositoryName><baseURL>" + xml(base)
                    + "</baseURL><protocolVersion>2.0</protocolVersion><adminEmail>" + xml(adminEmail) + "</adminEmail>"
                    + "<earliestDatestamp>1970-01-01T00:00:00Z</earliestDatestamp><deletedRecord>transient</deletedRecord>"
                    + "<granularity>YYYY-MM-DDThh:mm:ssZ</granularity></Identify>");
        } else if ("ListMetadataFormats".equals(verb)) {
            String identifier = clean(request.getParameter("identifier"));
            if (identifier.length() > 0 && service.findOaiItemByIdentifier(identifier) == null)
                oaiError(out, "idDoesNotExist", "Identifier tidak ditemukan.");
            else out.print("<ListMetadataFormats><metadataFormat><metadataPrefix>oai_dc</metadataPrefix>"
                        + "<schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>"
                        + "<metadataNamespace>http://www.openarchives.org/OAI/2.0/oai_dc/</metadataNamespace>"
                        + "</metadataFormat></ListMetadataFormats>");
        } else if ("ListSets".equals(verb)) {
            List<RepositoryPublicService.CollectionView> sets = service.listCollections(500);
            out.print("<ListSets>");
            for (int i = 0; i < sets.size(); i++) {
                RepositoryPublicService.CollectionView set = sets.get(i);
                out.print("<set><setSpec>collection:" + set.id + "</setSpec><setName>" + xml(set.nama) + "</setName></set>");
            }
            out.print("</ListSets>");
        } else if ("GetRecord".equals(verb)) {
            if (!validOaiPrefix(request.getParameter("metadataPrefix"))) {
                oaiError(out, "cannotDisseminateFormat", "Metadata prefix harus oai_dc.");
            } else {
                ItemDetail item = service.findOaiItemByIdentifier(request.getParameter("identifier"));
                if (item == null) oaiError(out, "idDoesNotExist", "Identifier tidak ditemukan.");
                else {
                    out.print("<GetRecord>");
                    writeOaiRecord(out, item, true);
                    out.print("</GetRecord>");
                }
            }
        } else if ("ListIdentifiers".equals(verb) || "ListRecords".equals(verb)) {
            if (token.length() == 0 && !validOaiPrefix(request.getParameter("metadataPrefix"))) {
                oaiError(out, "cannotDisseminateFormat", "Metadata prefix harus oai_dc.");
            } else {
                int page = parseOaiPage(token);
                if (page < 1 || (token.length() > 0 && (!validOaiToken(token) || !verb.equals(parseOaiTokenVerb(token))))) {
                    oaiError(out, "badResumptionToken", "Resumption token tidak valid.");
                } else {
                    Query q = new Query();
                    q.page = page;
                    q.pageSize = 50;
                    Long setId = token.length() == 0 ? parseSet(request.getParameter("set")) : parseOaiTokenLong(token, "s");
                    Date from = token.length() == 0 ? parseOaiDate(request.getParameter("from"), false) : parseOaiTokenDate(token, "f");
                    Date until = token.length() == 0 ? parseOaiDate(request.getParameter("until"), true) : parseOaiTokenDate(token, "u");
                    boolean invalidSet = token.length() == 0 && clean(request.getParameter("set")).length() > 0 && setId == null;
                    boolean invalidFrom = token.length() == 0 && clean(request.getParameter("from")).length() > 0 && from == null;
                    boolean invalidUntil = token.length() == 0 && clean(request.getParameter("until")).length() > 0 && until == null;
                    if (invalidSet || invalidFrom || invalidUntil || (from != null && until != null && from.after(until))) {
                        oaiError(out, "badArgument", "Parameter set/from/until tidak valid.");
                    } else {
                        q.collectionId = setId;
                        q.modifiedFrom = from;
                        q.modifiedUntil = until;
                        SearchResult result = service.searchOaiRecords(q);
                        if (token.length() > 0 && (page > result.totalPages || result.items.isEmpty())) {
                            oaiError(out, "badResumptionToken", "Resumption token sudah tidak valid.");
                            out.print("</OAI-PMH>"); out.flush(); return;
                        }
                        if (result.items.isEmpty() && page == 1) {
                            oaiError(out, "noRecordsMatch", "Tidak ada record publik yang sesuai.");
                        } else {
                            out.print("<" + verb + ">");
                            for (int i = 0; i < result.items.size(); i++) {
                                ItemDetail item = service.findOaiItem(result.items.get(i).id);
                                writeOaiRecord(out, item, "ListRecords".equals(verb));
                            }
                            String nextToken = page < result.totalPages
                                    ? buildOaiToken(page + 1, setId, from, until, verb) : "";
                            if (nextToken.length() > 0) out.print("<resumptionToken completeListSize=\"" + result.total + "\" cursor=\""
                                    + ((page - 1) * q.pageSize) + "\">" + xml(nextToken) + "</resumptionToken>");
                            out.print("</" + verb + ">");
                        }
                    }
                }
            }
        } else {
            oaiError(out, "badVerb", "Verb OAI-PMH tidak dikenal.");
        }
        out.print("</OAI-PMH>");
        out.flush();
    }

    private void writeOaiRecord(PrintWriter out, ItemDetail item, boolean includeMetadata) {
        if (item == null) return;
        if (includeMetadata) out.print("<record>");
        out.print("<header" + (item.withdrawn ? " status=\"deleted\"" : "") + "><identifier>" + xml(item.oaiIdentifier) + "</identifier><datestamp>"
                + xmlDate(item.datestamp == null ? new Date(0L) : item.datestamp) + "</datestamp>"
                + "<setSpec>collection:" + item.collectionId + "</setSpec></header>");
        if (includeMetadata && !item.withdrawn) {
            out.print("<metadata><oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
            out.print("<dc:title>" + xml(item.title) + "</dc:title>");
            String[] authors = item.authors == null ? new String[0] : item.authors.split(";");
            for (int i = 0; i < authors.length; i++) if (authors[i].trim().length() > 0) out.print("<dc:creator>" + xml(authors[i].trim()) + "</dc:creator>");
            if (item.abstractText.length() > 0) out.print("<dc:description>" + xml(item.abstractText) + "</dc:description>");
            if (item.subjects.length() > 0) out.print("<dc:subject>" + xml(item.subjects) + "</dc:subject>");
            if (item.publisher.length() > 0) out.print("<dc:publisher>" + xml(item.publisher) + "</dc:publisher>");
            if (item.year.length() > 0) out.print("<dc:date>" + xml(item.year) + "</dc:date>");
            out.print("<dc:type>" + xml(item.documentType) + "</dc:type><dc:language>" + xml(item.language)
                    + "</dc:language><dc:identifier>" + xml(item.oaiIdentifier) + "</dc:identifier>");
            if (item.dspaceHandle.length() > 0) out.print("<dc:identifier>" + xml(item.dspaceHandle) + "</dc:identifier>");
            out.print("<dc:rights>" + xml(item.accessPolicy) + "</dc:rights></oai_dc:dc></metadata>");
        }
        if (includeMetadata) out.print("</record>");
    }

    private void robots(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("text/plain;charset=UTF-8");
        String origin = publicOrigin(request);
        response.getWriter().print("User-agent: *\nAllow: " + request.getContextPath()
                + "/repository\nSitemap: " + origin + request.getContextPath() + "/sitemap.xml\n");
    }

    private void sitemap(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("application/xml;charset=UTF-8");
        String origin = publicOrigin(request) + request.getContextPath();
        PrintWriter out = response.getWriter();
        out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>"
                + xml(origin + "/repository") + "</loc></url>");
        Query q = new Query(); q.pageSize = RepositoryPublicService.MAX_PAGE_SIZE; q.page = 1;
        SearchResult page;
        do {
            page = service.search(q);
            for (ItemCard item : page.items)
                out.print("<url><loc>" + xml(origin + "/repository/item/" + item.id) + "</loc></url>");
            q.page++;
        } while (q.page <= page.totalPages);
        out.print("</urlset>");
    }

    private void feed(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean atom = "atom".equalsIgnoreCase(clean(request.getParameter("format")));
        String base = publicOrigin(request) + request.getContextPath();
        Query feedQuery=queryFrom(request);feedQuery.page=1;feedQuery.pageSize=20;feedQuery.sort="newest";
        List<ItemCard> items = service.search(feedQuery).items;
        response.setContentType((atom ? "application/atom+xml" : "application/rss+xml") + ";charset=UTF-8");
        PrintWriter out = response.getWriter();
        if (atom) {
            out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\"><title>Publikasi terbaru Repository AIS</title><id>" + xml(base + "/repository") + "</id><updated>" + xmlDate(new Date()) + "</updated>");
            for (ItemCard item : items) out.print("<entry><title>" + xml(item.title) + "</title><id>" + xml(item.oaiIdentifier) + "</id><link href=\"" + xml(base + "/repository/item/" + item.id) + "\"/><updated>" + xmlDate(item.issuedAt) + "</updated><summary>" + xml(item.abstractText) + "</summary></entry>");
            out.print("</feed>");
        } else {
            out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><title>Publikasi terbaru Repository AIS</title><link>" + xml(base + "/repository") + "</link><description>Karya ilmiah terbaru yang tersedia untuk publik.</description>");
            for (ItemCard item : items) out.print("<item><title>" + xml(item.title) + "</title><guid isPermaLink=\"false\">" + xml(item.oaiIdentifier) + "</guid><link>" + xml(base + "/repository/item/" + item.id) + "</link><description>" + xml(item.abstractText) + "</description><pubDate>" + rfc822(item.issuedAt) + "</pubDate></item>");
            out.print("</channel></rss>");
        }
    }

    private String rfc822(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date == null ? new Date(0L) : date);
    }

    private void tooManyRequests(HttpServletResponse response, String requestId) throws IOException {
        response.setHeader("Retry-After", "3600");
        writeJsonError(response, 429, "RATE_LIMITED",
                "Terlalu banyak permintaan. Silakan coba kembali beberapa saat lagi.", requestId);
    }

    private String actorId(HttpServletRequest request) {
        try { ais.database.model.Tbmuser u = Common.getCurrentUser(request); return u == null ? "" : u.getUserId(); }
        catch (Exception e) { return ""; }
    }

    private String publicOrigin(HttpServletRequest request) throws Exception {
        String configured = clean(System.getProperty("ais.repository.publicBaseUrl"));
        if (configured.length() > 0) {
            URL url = new URL(configured);
            String path = clean(url.getPath());
            if (!("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()))
                    || clean(url.getHost()).length() == 0 || url.getUserInfo() != null || url.getQuery() != null
                    || url.getRef() != null || !(path.length() == 0 || "/".equals(path)))
                throw new IllegalStateException("ais.repository.publicBaseUrl tidak valid.");
            int port = url.getPort();
            String host = url.getHost().indexOf(':') >= 0 ? "[" + url.getHost() + "]" : url.getHost();
            return url.getProtocol().toLowerCase() + "://" + host + (port < 0 ? "" : ":" + port);
        }
        String scheme = clean(request.getScheme()).toLowerCase();
        String host = clean(request.getServerName());
        int port = request.getServerPort();
        if (!("http".equals(scheme) || "https".equals(scheme)) || host.length() == 0
                || !host.matches("[A-Za-z0-9.-]+|[0-9a-fA-F:]+") || port < 1 || port > 65535)
            throw new IllegalStateException("Origin publik Repository tidak valid.");
        String authority = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + authority + (defaultPort ? "" : ":" + port);
    }
    private String country(HttpServletRequest request){String value=clean(request.getHeader("CF-IPCountry"));if(value.length()==0)value=clean(request.getHeader("X-Country-Code"));return value;}

    private void writeOaiRequest(PrintWriter out, HttpServletRequest request, String base, String verb) {
        out.print("<request");
        String[] names = new String[] { "verb", "identifier", "metadataPrefix", "from", "until", "set", "resumptionToken" };
        for (int i = 0; i < names.length; i++) {
            String value = "verb".equals(names[i]) ? verb : clean(request.getParameter(names[i]));
            if (value.length() > 0) out.print(" " + names[i] + "=\"" + xml(value) + "\"");
        }
        out.print(">" + xml(base) + "</request>");
    }

    private boolean isOaiVerb(String verb) {
        return "Identify".equals(verb) || "ListMetadataFormats".equals(verb) || "ListSets".equals(verb)
                || "GetRecord".equals(verb) || "ListIdentifiers".equals(verb) || "ListRecords".equals(verb);
    }

    private String oaiArgumentError(HttpServletRequest request, String verb, String token) {
        if (!isOaiVerb(verb)) return "Verb OAI-PMH tidak dikenal.";
        Map<?,?> parameters = request.getParameterMap();
        for (Object keyObject : parameters.keySet()) {
            String key = String.valueOf(keyObject);
            if ("action".equals(key)) continue;
            if (!("verb".equals(key) || "identifier".equals(key) || "metadataPrefix".equals(key)
                    || "from".equals(key) || "until".equals(key) || "set".equals(key)
                    || "resumptionToken".equals(key))) return "Argumen " + key + " tidak dikenal.";
            Object raw = parameters.get(keyObject);
            if (raw instanceof String[] && ((String[]) raw).length != 1) return "Argumen " + key + " tidak boleh diulang.";
        }
        if (token.length() > 0) {
            if (!("ListIdentifiers".equals(verb) || "ListRecords".equals(verb)))
                return "Resumption token hanya berlaku untuk ListIdentifiers atau ListRecords.";
            if (parameters.size() > (parameters.containsKey("action") ? 3 : 2))
                return "Resumption token harus menjadi satu-satunya argumen selain verb.";
            return "";
        }
        if ("Identify".equals(verb) || "ListSets".equals(verb))
            return hasAnyOaiArgument(request, new String[] { "identifier", "metadataPrefix", "from", "until", "set", "resumptionToken" })
                    ? "Verb ini tidak menerima argumen tambahan." : "";
        if ("ListMetadataFormats".equals(verb))
            return hasAnyOaiArgument(request, new String[] { "metadataPrefix", "from", "until", "set", "resumptionToken" })
                    ? "ListMetadataFormats hanya menerima identifier opsional." : "";
        if ("GetRecord".equals(verb)) {
            if (clean(request.getParameter("identifier")).length() == 0 || clean(request.getParameter("metadataPrefix")).length() == 0)
                return "GetRecord memerlukan identifier dan metadataPrefix.";
            return hasAnyOaiArgument(request, new String[] { "from", "until", "set", "resumptionToken" })
                    ? "GetRecord tidak menerima set/from/until." : "";
        }
        if (request.getParameter("resumptionToken") != null)
            return "Resumption token kosong tidak valid.";
        if (clean(request.getParameter("metadataPrefix")).length() == 0)
            return verb + " memerlukan metadataPrefix.";
        if (clean(request.getParameter("identifier")).length() > 0)
            return verb + " tidak menerima identifier.";
        String from = clean(request.getParameter("from")), until = clean(request.getParameter("until"));
        if (from.length() > 0 && until.length() > 0 && ((from.length() == 10) != (until.length() == 10)))
            return "from dan until harus menggunakan granularitas yang sama.";
        return "";
    }

    private boolean hasAnyOaiArgument(HttpServletRequest request, String[] names) {
        for (int i = 0; i < names.length; i++) if (request.getParameter(names[i]) != null) return true;
        return false;
    }

    private boolean validOaiPrefix(String value) {
        return "oai_dc".equals(clean(value));
    }

    private int parseOaiPage(String token) {
        if (token == null || token.length() == 0) return 1;
        String[] values=decodeOaiToken(token);if(values==null)return -1;
        try{int page=Integer.parseInt(values[0]);return page>0?page:-1;}catch(Exception e){return -1;}
    }

    private String buildOaiToken(int page, Long setId, Date from, Date until, String verb) {
        try{String payload=page+"|"+(setId==null?0L:setId.longValue())+"|"+(from==null?0L:from.getTime())+"|"+(until==null?0L:until.getTime())+"|"+("ListIdentifiers".equals(verb)?"I":"R")+"|"+System.currentTimeMillis();byte[] bytes=payload.getBytes("UTF-8");return Base64.encodeBase64URLSafeString(bytes)+"."+Base64.encodeBase64URLSafeString(hmac(bytes));}catch(Exception e){throw new IllegalStateException("Resumption token tidak dapat dibuat.",e);}
    }

    private Long parseOaiTokenLong(String token, String key) {
        String[] values=decodeOaiToken(token);if(values==null)return null;int index="s".equals(key)?1:("f".equals(key)?2:("u".equals(key)?3:-1));if(index<0)return null;try{long value=Long.parseLong(values[index]);return value<=0L?null:Long.valueOf(value);}catch(Exception e){return null;}
    }

    private Date parseOaiTokenDate(String token, String key) {
        Long value = parseOaiTokenLong(token, key);
        return value == null ? null : new Date(value.longValue());
    }

    private String parseOaiTokenVerb(String token) {
        String[] values=decodeOaiToken(token);return values==null?"":("I".equals(values[4])?"ListIdentifiers":"ListRecords");
    }

    private boolean validOaiToken(String token) {
        return decodeOaiToken(token)!=null;
    }

    private String[] decodeOaiToken(String token){try{String value=clean(token);int dot=value.indexOf('.');if(dot<=0||dot!=value.lastIndexOf('.'))return null;byte[] payload=Base64.decodeBase64(value.substring(0,dot)),signature=Base64.decodeBase64(value.substring(dot+1));if(!MessageDigest.isEqual(signature,hmac(payload)))return null;String[] parts=new String(payload,"UTF-8").split("\\|",-1);if(parts.length!=6||!("I".equals(parts[4])||"R".equals(parts[4])))return null;int page=Integer.parseInt(parts[0]);Long.parseLong(parts[1]);Long.parseLong(parts[2]);Long.parseLong(parts[3]);long issued=Long.parseLong(parts[5]),maximumAge=oaiTokenMaximumAge();if(page<1||issued>System.currentTimeMillis()+60000L||System.currentTimeMillis()-issued>maximumAge)return null;return parts;}catch(Exception e){return null;}}
    private static byte[] hmac(byte[] payload)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(OAI_TOKEN_SECRET,"HmacSHA256"));return mac.doFinal(payload);}
    private static byte[] oaiTokenSecret(){try{String configured=System.getProperty("ais.repository.oaiTokenSecret","").trim();String value=configured.length()>=32?configured:UUID.randomUUID().toString()+UUID.randomUUID().toString();return value.getBytes("UTF-8");}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private static long oaiTokenMaximumAge(){try{long seconds=Long.parseLong(System.getProperty("ais.repository.oaiTokenTtlSeconds","86400"));return Math.max(300L,Math.min(seconds,604800L))*1000L;}catch(Exception e){return 86400000L;}}

    private Date parseOaiDate(String value, boolean endOfDay) {
        String text = clean(value);
        if (text.length() == 0) return null;
        String pattern = text.length() == 10 ? "yyyy-MM-dd" : "yyyy-MM-dd'T'HH:mm:ss'Z'";
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsed = format.parse(text);
            return endOfDay && text.length() == 10 ? new Date(parsed.getTime() + 86399999L) : parsed;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseSet(String set) {
        String value = clean(set);
        return value.startsWith("collection:") ? parseLong(value.substring("collection:".length())) : null;
    }

    private void oaiError(PrintWriter out, String code, String message) {
        out.print("<error code=\"" + xml(code) + "\">" + xml(message) + "</error>");
    }

    private String xmlDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date == null ? new Date(0L) : date);
    }

    private String xml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String action=clean(request.getParameter("action"));return "search".equalsIgnoreCase(action)||"suggest".equalsIgnoreCase(action);
    }

    private void writeJsonError(HttpServletResponse response, int status, String code,
            String message, String requestId) throws IOException {
        try {
            JSONObject json = new JSONObject();
            json.put("status", "ERROR");
            json.put("code", code);
            json.put("message", message);
            json.put("requestId", requestId);
            writeJson(response, json, status);
        } catch (Exception e) {
            response.sendError(status, message);
        }
    }

    private void writeJson(HttpServletResponse response, JSONObject body, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body == null ? "{}" : body.toString());
    }

    private static Long parseLong(String value) {
        try {
            Long parsed = Long.valueOf(clean(value));
            return parsed.longValue() > 0L ? parsed : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(clean(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeFileName(String value) {
        String name = clean(value).replace('\\', '_').replace('/', '_').replace(':', '_').replaceAll("[\\p{Cntrl}]", "_");
        if(name.length()>180)name=name.substring(name.length()-180);
        return name.length() == 0 ? "repository-file" : name;
    }
}
