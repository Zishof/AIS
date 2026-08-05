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

public class Api extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final int HTTP_OK = 200;

    /**
     * Tetap public agar kompatibel dengan class lama yang langsung membaca Api.tokens.
     * Implementasi sebenarnya dikelola oleh ApiTokenManager agar bisa direuse.
     */
    public static Map<String, Object> tokens = ApiTokenManager.tokens;

    /** Dipertahankan untuk kompatibilitas penggunaan lama. */
    public static Long randLong = 0L;
    public static Set<Long> nexts = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    private static final Object PMB_SESSION_LOCK = new Object();
    private static final Map<String, ApiRoute> ROUTES = ApiRouteRegistry.createDefaultRoutes();

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
            JSONObject hasilTemp = route.execute(request, jsonObject, selectedPerguruanTinggi);
            return hasilTemp == null ? hasil : hasilTemp;
        } catch (Exception e) {
            try {
                Common.tampilErrorJikaAdmin(e);
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/Api.java:103");
            }
            return ApiHelperSupport.errorResponse("Terjadi kesalahan internal server");
        }
    }

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

    private String createPmbSessionMarker(HttpServletRequest request) {
        synchronized (PMB_SESSION_LOCK) {
            randLong = Long.valueOf(randLong.longValue() + 1L);
            nexts.add(randLong);
            request.getSession(true).setAttribute("data_session_pmb", randLong);
            return randLong.toString();
        }
    }

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

    private void addCorsHeaders(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        addCorsHeaders(response);
        response.setStatus(HTTP_OK);
    }
}
