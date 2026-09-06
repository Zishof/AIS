package ais.action.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.servlet.api.ApiAccessGuard;
import ais.action.servlet.api.ApiHelperSupport;
import ais.action.servlet.api.ApiMobileLogger;
import ais.action.servlet.api.ApiRoute;
import ais.action.servlet.api.ApiRouteRegistry;
import ais.action.servlet.api.ApiTokenManager;
import ais.action.servlet.api.SuratApi;
import ais.common.Common;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;

/**
 * Komponen batas HTTP/servlet untuk api. Tipe ini menerima input dari luar aplikasi, meneruskannya
 * ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * HttpServlet}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code int HTTP_OK}, {@code Map tokens},
 * {@code Long randLong}, {@code Set nexts}, {@code Object PMB_SESSION_LOCK}, {@code Map ROUTES};
 * inisialisasi/lifecycle ({@code initTokens()}); pembacaan/pencarian ({@code ambil()}, {@code doGet()}, {@code
 * handleDownloadRequest()}); penghapusan/pembatalan ({@code removeToken()}); operasi domain lain ({@code
 * putToken()}, {@code createPmbSessionMarker()}, {@code writeResponse()}, {@code addCorsHeaders()}, {@code
 * doPost()}, {@code doOptions()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see HttpServlet
 */
public class Api extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** Kode status HTTP 200 (OK), dipakai sebagai status default respons JSON servlet ini. */
    private static final int HTTP_OK = 200;

    /**
     * Tetap public agar kompatibel dengan class lama yang langsung membaca Api.tokens.
     * Implementasi sebenarnya dikelola oleh ApiTokenManager agar bisa direuse.
     */
    public static Map<String, Object> tokens = ApiTokenManager.tokens;

    /** Dipertahankan untuk kompatibilitas penggunaan lama. */
    public static Long randLong = 0L;
    /** Kumpulan nilai {@link #randLong} yang pernah diterbitkan sebagai penanda sesi PMB (lihat {@link #createPmbSessionMarker}); dipakai untuk menjaga keunikan penanda tersebut selama proses berjalan. */
    public static Set<Long> nexts = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    /** Kunci sinkronisasi untuk pembuatan penanda sesi PMB di {@link #createPmbSessionMarker}, agar increment {@link #randLong} dan penulisan ke {@link #nexts} tidak bertabrakan antar-thread. */
    private static final Object PMB_SESSION_LOCK = new Object();
    /** Tabel rute {@code action} API ke implementasinya, dibentuk sekali saat class dimuat lewat {@link ApiRouteRegistry#createDefaultRoutes()}. */
    private static final Map<String, ApiRoute> ROUTES = ApiRouteRegistry.createDefaultRoutes();

    /** Konstruktor default; memastikan {@link #tokens} terisi dari {@link ApiTokenManager} sebelum servlet melayani permintaan pertamanya. */
    public Api() {
        super();
        initTokens();
    }

    /**
     * Inisialisasi token dipusatkan di ApiTokenManager agar logic login/logout/replikasi
     * tidak tersebar di servlet.
     */
    public static void initTokens() {
        ApiTokenManager.initTokens();
        tokens = ApiTokenManager.tokens;
    }

    /** Gunakan method ini saat login agar token ikut tersinkron antar-node Tomcat. */
    public static void putToken(String token, Object userObject) {
        ApiTokenManager.putToken(token, userObject);
        tokens = ApiTokenManager.tokens;
    }

    /** Gunakan method ini saat logout agar token ikut tersinkron antar-node Tomcat. */
    public static void removeToken(String token) {
        ApiTokenManager.removeToken(token);
        tokens = ApiTokenManager.tokens;
    }

    /**
     * Titik masuk utama routing API JSON: memvalidasi/mengotorisasi permintaan lewat {@link
     * ApiAccessGuard#check}, menentukan rute dari field {@code action} pada {@code jsonObject}
     * (dicocokkan tanpa membedakan besar/kecil huruf ke {@link #ROUTES}), lalu mengeksekusi rute
     * tersebut. Mendukung idempotensi mutasi offline-first: bila permintaan membawa {@code
     * clientMutationId} dan {@code action} tergolong mutasi ({@link
     * ais.action.servlet.api.MutasiIdempotenUtil#aksiMutasi}), eksekusi ulang dengan
     * {@code clientMutationId} yang sama mengembalikan respons tersimpan dari percobaan pertama,
     * bukan menjalankan operasi bisnis dua kali; respons sukses ({@code status} "00") dari eksekusi
     * baru disimpan untuk dipakai ulang.
     *
     * @param request    permintaan servlet yang sedang diproses (dipakai rute untuk cek sesi/tenant)
     * @param jsonObject payload permintaan; {@code null} atau tanpa {@code action} valid menghasilkan
     *                   respons default tanpa memproses apa pun
     * @return respons JSON hasil rute, respons default {@link ApiHelperSupport#defaultResponse()}
     *         bila tidak ada rute yang cocok, atau respons galat internal bila terjadi exception
     */
    public static JSONObject ambil(HttpServletRequest request, JSONObject jsonObject) {
        JSONObject hasil = ApiHelperSupport.defaultResponse();
        if (jsonObject == null) {
            return hasil;
        }

        try {
            JSONObject guardResponse = ApiAccessGuard.check(request, jsonObject);
            if (guardResponse != null) {
                return guardResponse;
            }

            String action = ApiHelperSupport.optString(jsonObject, "action");
            if (!ApiHelperSupport.hasText(action)) {
                return hasil;
            }

            ApiRoute route = ROUTES.get(action.trim().toLowerCase(Locale.ENGLISH));
            if (route == null) {
                return hasil;
            }

            PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi(request);

            // Idempotensi mutasi offline-first (Tahap C): retry dari outbox
            // klien membawa clientMutationId yang sama; eksekusi kedua
            // mengembalikan respons tersimpan alih-alih menjalankan operasi
            // bisnis dua kali. Hanya berlaku untuk action mutasi terdaftar.
            String clientMutationId = ApiHelperSupport.optString(jsonObject, "clientMutationId");
            boolean pakaiIdempotensi = ApiHelperSupport.hasText(clientMutationId)
                    && ais.action.servlet.api.MutasiIdempotenUtil.aksiMutasi(action);
            if (pakaiIdempotensi) {
                JSONObject tersimpan = ais.action.servlet.api.MutasiIdempotenUtil.ambil(
                        jsonObject, request, action, clientMutationId.trim());
                if (tersimpan != null) {
                    return tersimpan;
                }
            }

            JSONObject hasilTemp = route.execute(request, jsonObject, selectedPerguruanTinggi);
            if (pakaiIdempotensi && hasilTemp != null
                    && "00".equals(hasilTemp.optString("status"))) {
                ais.action.servlet.api.MutasiIdempotenUtil.simpan(
                        jsonObject, request, action, clientMutationId.trim(), hasilTemp);
            }
            return hasilTemp == null ? hasil : hasilTemp;
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/Api.java:103");
            }
            return ApiHelperSupport.errorResponse("Terjadi kesalahan internal server");
        }
    }

    /**
     * Menangani permintaan {@code GET}: memasang header CORS, lalu mencoba jalur unduhan surat
     * keluar ({@link #handleDownloadRequest}); bila bukan unduhan, menangani tiga kasus berdasar
     * parameter — {@code data_session_pmb} (buat penanda sesi PMB via {@link
     * #createPmbSessionMarker}), {@code checkLogin} ({@code "Y"}/{@code "N"} sesuai status login
     * lewat {@link Common#getCurrentUser}), atau badan permintaan JSON (diteruskan ke {@link
     * #ambil} dan dicatat lewat {@link ApiMobileLogger#save}). Galat parsing JSON/IO/lainnya
     * ditangkap dan diterjemahkan ke respons status kode singkat, tidak dilempar ke container.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCorsHeaders(response);

        if (handleDownloadRequest(request, response)) {
            return;
        }

        String body = ApiHelperSupport.defaultResponse().toString();
        int httpStatus = HTTP_OK;

        try {
            if (request.getParameter("data_session_pmb") != null) {
                body = createPmbSessionMarker(request);
            } else if (request.getParameter("checkLogin") != null) {
                Tbmuser user = Common.getCurrentUser(request);
                body = (user == null || user.getUserId() == null) ? "N" : "Y";
            } else {
                String data = ApiHelperSupport.readBody(request);
                if (ApiHelperSupport.hasText(data)) {
                    JSONObject jsonObject = new JSONObject(data);
                    JSONObject hasilApi = Api.ambil(request, jsonObject);
                    body = hasilApi == null ? body : hasilApi.toString();
                    ApiMobileLogger.save(request, jsonObject, body);
                }
            }
        } catch (org.json.JSONException e) {
            body = ApiHelperSupport.status("98", "Format JSON request tidak valid").toString();
        } catch (IOException e) {
            body = ApiHelperSupport.status("97", e.getMessage()).toString();
        } catch (Exception e) {
            body = ApiHelperSupport.errorResponse("Terjadi kesalahan internal server").toString();
        }

        writeResponse(response, httpStatus, body);
    }

    /**
     * Menangani permintaan unduhan surat keluar bila parameter {@code suratKeluar} hadir: mencetak
     * berkas lewat {@link SuratApi#cetakSurat} lalu men-streamnya via {@link
     * AmbilLampiran#doDownload}. Kegagalan (berkas tidak ada/exception) dijawab dengan respons JSON
     * status galat, bukan exception, agar {@link #doGet} tetap dapat menuliskan respons yang wajar.
     *
     * @return {@code true} bila permintaan ini ditangani sebagai unduhan (baik sukses maupun
     *         gagal) sehingga {@link #doGet} tidak perlu melanjutkan ke jalur lain; {@code false}
     *         bila parameter {@code suratKeluar} tidak ada dan permintaan bukan unduhan
     */
    private boolean handleDownloadRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request == null || response == null || request.getParameter("suratKeluar") == null) {
            return false;
        }

        try {
            File file = SuratApi.cetakSurat(request.getParameter("suratKeluar"), null);
            if (file == null || !file.exists() || !file.isFile()) {
                writeResponse(response, HTTP_OK, ApiHelperSupport.status("94", "File surat keluar tidak ditemukan").toString());
                return true;
            }
            AmbilLampiran.doDownload(request, response, file);
        } catch (Exception e) {
            writeResponse(response, HTTP_OK, ApiHelperSupport.errorResponse("Gagal mengunduh surat keluar").toString());
        }
        return true;
    }

    /**
     * Membuat penanda unik untuk sesi pendaftaran PMB: menaikkan {@link #randLong} secara
     * tersinkronisasi ({@link #PMB_SESSION_LOCK}), mencatatnya ke {@link #nexts}, lalu
     * menyimpannya sebagai atribut sesi HTTP {@code data_session_pmb} (sesi dibuat bila belum ada).
     *
     * @return representasi string dari nilai {@link #randLong} yang baru, dipakai klien sebagai
     *         token penanda sesi
     */
    private String createPmbSessionMarker(HttpServletRequest request) {
        synchronized (PMB_SESSION_LOCK) {
            randLong = Long.valueOf(randLong.longValue() + 1L);
            nexts.add(randLong);
            request.getSession(true).setAttribute("data_session_pmb", randLong);
            return randLong.toString();
        }
    }

    /**
     * Menulis {@code body} sebagai respons JSON: mengatur status HTTP, {@code Content-Type}
     * {@code application/json}, header panjang badan (kunci {@code "length"}, bukan {@code
     * Content-Length} standar — dipertahankan untuk kompatibilitas klien lama), dan header CORS
     * (lihat {@link #addCorsHeaders}), lalu menuliskan dan menutup writer.
     *
     * @param body badan respons; {@code null} diperlakukan sebagai string kosong
     */
    private void writeResponse(HttpServletResponse response, int httpStatus, String body) throws IOException {
        if (body == null) {
            body = "";
        }
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setHeader("Content-Type", "application/json");
        response.setHeader("length", String.valueOf(body.length()));
        addCorsHeaders(response);

        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(body);
            writer.flush();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Api.java:191");
                }
            }
        }
    }

    /**
     * Menambahkan header CORS permisif (asal mana pun, method GET/POST/OPTIONS, header umum yang
     * dipakai klien API) agar endpoint ini dapat diakses lintas-origin oleh klien web/mobile.
     * Tidak melakukan apa pun bila {@code response} {@code null}.
     */
    private void addCorsHeaders(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    }

    /** Menangani permintaan {@code POST} dengan perilaku identik {@link #doGet}: seluruh logika routing API menerima badan JSON lewat metode HTTP apa pun. */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /** Menangani preflight CORS {@code OPTIONS}: memasang header CORS (lihat {@link #addCorsHeaders}) lalu membalas {@link #HTTP_OK} tanpa badan. */
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCorsHeaders(response);
        response.setStatus(HTTP_OK);
    }
}
