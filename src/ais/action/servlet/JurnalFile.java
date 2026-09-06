package ais.action.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalFileService;
import ais.action.master.jurnal.JurnalUsageEventService;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;

/**
 * Endpoint unduh (GET) dan unggah (POST) berkas jurnal ({@link RepoBitstream}) dengan otorisasi
 * dan pembatasan ukuran unggahan. GET men-stream berkas untuk pengunduhan (otorisasi dan
 * scoping visibilitas didelegasikan sepenuhnya ke {@link JurnalFileService}, yang boleh
 * mengizinkan pengunjung anonim untuk berkas yang sudah dipublikasikan -- sesuai model jurnal
 * ilmiah terbuka). POST menyimpan berkas baru atau mereko-siliasi metadata; wajib login dan
 * token CSRF valid.
 */
public final class JurnalFile extends HttpServlet {
    /** Versi serialisasi tetap 1L; servlet tidak pernah benar-benar diserialisasi ke stream. */
    private static final long serialVersionUID = 1L;
    /** Layanan penyimpanan/pengambilan berkas jurnal (otorisasi, streaming, upload) sesungguhnya. */
    private final JurnalFileService files = new JurnalFileService();

    /**
     * Menstream isi berkas jurnal untuk diunduh/ditampilkan. Otorisasi (termasuk apakah berkas
     * ini boleh diakses anonim atau perlu login) sepenuhnya diperiksa di dalam
     * {@link JurnalFileService#metadataForDownload} dan {@link JurnalFileService#stream}, yang
     * melempar {@link SecurityException} jika ditolak. Tipe konten HTML/XML/SVG dipaksa menjadi
     * unduhan ({@code attachment}) dan diberi CSP {@code sandbox} ketat untuk mencegah eksekusi
     * konten aktif yang diunggah pengguna (mitigasi stored-XSS lewat berkas jurnal).
     *
     * @param req permintaan HTTP masuk; id berkas diambil dari {@code pathInfo} ({@code /{id}})
     * @param res respons HTTP keluar
     * @throws IOException jika terjadi galat I/O saat menstream berkas atau menulis status galat
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String requestId = requestId(req);
        securityHeaders(res, requestId);
        try {
            Long id = pathId(req);
            Tbmuser actor = Common.getCurrentUser(req);
            RepoBitstream meta = files.metadataForDownload(id,actor,req.getRemoteAddr());
            String mime = safeMime(meta.getMimeType());
            res.setContentType(mime);
            boolean activeContent = mime.startsWith("text/html") || mime.indexOf("xml") >= 0 || mime.indexOf("svg") >= 0;
            res.setHeader("Content-Disposition", (activeContent ? "attachment" : "inline") + "; filename=\"" + headerFileName(meta.getNamaFile()) + "\"");
            if (activeContent) res.setHeader("Content-Security-Policy", "sandbox; default-src 'none'");
            res.setHeader("Content-Length", String.valueOf(meta.getUkuranByte()));
            files.stream(id, actor, req.getRemoteAddr(), res.getOutputStream());
            try { new JurnalUsageEventService().record(meta.getItemId(),meta.getId(),"DOWNLOAD",actor,req); }
            catch (Exception usageError) { ais.common.ErrorAuditUtil.record(usageError,"JurnalFile usage DOWNLOAD:"+requestId); }
        } catch (SecurityException e) {
            if (!res.isCommitted()) res.sendError(403, "Hak akses file jurnal tidak tersedia.");
        } catch (java.io.FileNotFoundException e) {
            if (!res.isCommitted()) res.sendError(404);
        } catch (IllegalArgumentException e) {
            if (!res.isCommitted()) res.sendError(422, e.getMessage());
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "JurnalFile.GET:" + requestId);
            if (!res.isCommitted()) res.sendError(500, "File jurnal gagal dibaca. ID: " + requestId);
        } finally { HibernateUtil.closeSession(); }
    }

    /**
     * Menangani unggah berkas baru ({@code action} default) atau rekonsiliasi metadata berkas
     * ({@code action=reconcile}). Wajib login ({@link SecurityException} 401 via 403 jika tidak)
     * dan token CSRF valid; ukuran unggahan dibatasi lewat header {@code Content-Length} (lihat
     * {@link #contentLength}). Hasil selalu berupa JSON, baik sukses maupun galat.
     *
     * @param req permintaan HTTP masuk; parameter {@code action}, {@code bitstreamId}
     *        (untuk reconcile) atau {@code itemId}/{@code fileName}/{@code mimeType}/
     *        {@code stage}/{@code genre}/{@code round} (untuk upload) dibaca di sini
     * @param res respons HTTP keluar; selalu JSON ({@code application/json})
     * @throws IOException jika terjadi galat I/O saat membaca body upload atau menulis respons JSON
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String requestId = requestId(req);
        securityHeaders(res, requestId);
        res.setContentType("application/json; charset=UTF-8");
        JSONObject out = new JSONObject();
        try {
            Tbmuser actor = Common.getCurrentUser(req);
            if (actor == null) throw new SecurityException("Login diperlukan.");
            if (!NewUiCsrfUtil.isValid(req)) throw new SecurityException("Token CSRF tidak valid.");
            RepoBitstream saved;
            if ("reconcile".equals(req.getParameter("action"))) {
                saved = files.reconcile(requiredLong(req, "bitstreamId"), actor);
            } else {
                long size = contentLength(req);
                saved = files.store(requiredLong(req, "itemId"), required(req, "fileName"),
                        required(req, "mimeType"), required(req, "stage"), required(req, "genre"),
                        optionalInteger(req, "round"), req.getInputStream(), size, actor);
            }
            out.put("ok", true).put("id", saved.getId()).put("storageState", saved.getStorageState())
                    .put("size", saved.getUkuranByte()).put("checksum", saved.getChecksum());
        } catch (SecurityException e) {
            res.setStatus(403); failure(out, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            res.setStatus(422); failure(out, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            res.setStatus(500); failure(out, "INTERNAL_ERROR", "Upload file jurnal gagal. ID: " + requestId);
            ais.common.ErrorAuditUtil.record(e, "JurnalFile.POST:" + requestId);
        } finally {
            res.getWriter().write(out.toString());
            HibernateUtil.closeSession();
        }
    }

    /**
     * Membaca dan memvalidasi header {@code Content-Length} unggahan: wajib berupa angka positif
     * dan tidak melebihi {@link JurnalFileService#MAX_UPLOAD_BYTES}, mencegah unggahan tak
     * terbatas ukurannya membebani server.
     *
     * @param req permintaan HTTP masuk
     * @return ukuran konten dalam byte, sudah tervalidasi
     * @throws IllegalArgumentException jika header tidak ada, bukan angka, atau di luar rentang yang diizinkan
     */
    private static long contentLength(HttpServletRequest req) {
        String raw = req.getHeader("Content-Length");
        long value;
        try { value = Long.parseLong(raw); }
        catch (Exception e) { throw new IllegalArgumentException("Content-Length upload wajib valid."); }
        if (value < 1 || value > JurnalFileService.MAX_UPLOAD_BYTES)
            throw new IllegalArgumentException("Ukuran upload tidak diizinkan.");
        return value;
    }

    /**
     * Mengurai id berkas dari {@code pathInfo} yang wajib berbentuk {@code /{angka positif}}.
     *
     * @param req permintaan HTTP masuk
     * @return id berkas hasil parse
     * @throws IllegalArgumentException jika {@code pathInfo} tidak ada atau tidak sesuai pola
     */
    private static Long pathId(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || !path.matches("/[1-9][0-9]*"))
            throw new IllegalArgumentException("ID file tidak valid.");
        return Long.valueOf(path.substring(1));
    }

    /**
     * Mengambil parameter wajib dan memvalidasinya sebagai {@link Long} positif (&gt;= 1).
     *
     * @param req permintaan HTTP masuk
     * @param name nama parameter yang diambil
     * @return nilai {@link Long} hasil parse
     * @throws IllegalArgumentException jika parameter tidak ada, bukan angka, atau kurang dari 1
     */
    private static Long requiredLong(HttpServletRequest req, String name) {
        String value = required(req, name);
        try { Long id = Long.valueOf(value); if (id.longValue() < 1) throw new Exception(); return id; }
        catch (Exception e) { throw new IllegalArgumentException(name + " tidak valid."); }
    }

    /**
     * Mengambil parameter opsional dan memvalidasinya sebagai bilangan bulat dalam rentang 1-999
     * (mis. nomor ronde review), mengembalikan {@code null} jika parameter tidak diisi.
     *
     * @param req permintaan HTTP masuk
     * @param name nama parameter yang diambil
     * @return nilai {@link Integer} hasil parse, atau {@code null} jika parameter kosong/tidak ada
     * @throws IllegalArgumentException jika parameter diisi tetapi bukan angka valid dalam rentang 1-999
     */
    private static Integer optionalInteger(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null || value.trim().length() == 0) return null;
        try { int result = Integer.parseInt(value); if (result < 1 || result > 999) throw new Exception(); return result; }
        catch (Exception e) { throw new IllegalArgumentException(name + " tidak valid."); }
    }

    /**
     * Mengambil parameter wajib dan memastikan tidak kosong (setelah di-trim).
     *
     * @param req permintaan HTTP masuk
     * @param name nama parameter yang diambil
     * @return nilai parameter yang sudah di-trim
     * @throws IllegalArgumentException jika parameter tidak ada atau kosong
     */
    private static String required(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null || value.trim().length() == 0)
            throw new IllegalArgumentException(name + " wajib diisi.");
        return value.trim();
    }

    /**
     * Memvalidasi nilai tipe MIME agar sesuai pola {@code jenis/subjenis}; nilai yang tidak
     * sesuai (termasuk {@code null}) diganti {@code application/octet-stream} agar tidak ada
     * nilai sembarang yang lolos ke header {@code Content-Type} respons.
     *
     * @param value nilai tipe MIME mentah dari metadata berkas; boleh {@code null}
     * @return tipe MIME yang valid; {@code application/octet-stream} jika tidak valid
     */
    private static String safeMime(String value) {
        String x = value == null ? "" : value.trim().toLowerCase();
        return x.matches("[a-z0-9.+-]+/[a-z0-9.+-]+") ? x : "application/octet-stream";
    }

    /**
     * Menyaring nama berkas agar aman disisipkan ke header {@code Content-Disposition}: karakter
     * CR/LF/kutip/backslash/garis miring diganti {@code _} (mencegah header injection dan path
     * traversal).
     *
     * @param value nama berkas asli dari metadata; {@code null} diganti {@code "file"}
     * @return nama berkas yang sudah aman dipakai di header HTTP; {@code "file"} jika kosong setelah disaring
     */
    private static String headerFileName(String value) {
        String x = value == null ? "file" : value.replaceAll("[\\r\\n\\\"\\\\/]", "_").trim();
        return x.length() == 0 ? "file" : x;
    }

    /**
     * Memasang header keamanan standar pada setiap respons: mencegah caching berkas privat,
     * MIME-sniffing, framing lintas-origin, serta menyertakan id permintaan untuk korelasi log.
     *
     * @param res respons HTTP keluar
     * @param requestId id unik permintaan (lihat {@link #requestId})
     */
    private static void securityHeaders(HttpServletResponse res, String requestId) {
        res.setHeader("Cache-Control", "private, no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "SAMEORIGIN");
        res.setHeader("X-Request-Id", requestId);
    }

    /**
     * Menyusun id unik singkat untuk satu permintaan, dipakai berkorelasi antara log audit
     * galat dan pesan yang ditampilkan ke pengguna (agar admin bisa menelusuri tanpa membocorkan
     * detail internal ke pengguna).
     *
     * @param req permintaan HTTP masuk, dipakai identity hash sebagai komponen keunikan
     * @return id permintaan dalam bentuk heksadesimal
     */
    private static String requestId(HttpServletRequest req) {
        return Long.toHexString(System.currentTimeMillis()) + Integer.toHexString(System.identityHashCode(req));
    }

    /**
     * Mengisi objek JSON respons dengan penanda kegagalan, kode galat, dan pesan untuk pengguna.
     *
     * @param out objek JSON respons yang sedang dibangun (dimutasi)
     * @param code kode galat mesin-terbaca (mis. {@code FORBIDDEN}, {@code VALIDATION_FAILED})
     * @param message pesan galat yang dapat ditampilkan ke pengguna
     */
    private static void failure(JSONObject out, String code, String message) {
        try { out.put("ok", false).put("code", code).put("message", message); }
        catch (Exception ignored) {}
    }
}
