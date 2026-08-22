package ais.action.servlet;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalFileService;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;

/** Bounded raw-body upload and authorized streaming endpoint for journal files. */
public final class JurnalFile extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final JurnalFileService files = new JurnalFileService();

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String requestId = requestId(req);
        securityHeaders(res, requestId);
        try {
            Long id = pathId(req);
            Tbmuser actor = Common.getCurrentUser(req);
            RepoBitstream meta = (RepoBitstream) HibernateUtil.currentSession().get(RepoBitstream.class, id);
            if (meta == null) { res.sendError(404); return; }
            res.setContentType(safeMime(meta.getMimeType()));
            res.setHeader("Content-Disposition", "inline; filename=\"" + headerFileName(meta.getNamaFile()) + "\"");
            res.setHeader("Content-Length", String.valueOf(meta.getUkuranByte()));
            files.stream(id, actor, res.getOutputStream());
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

    private static long contentLength(HttpServletRequest req) {
        String raw = req.getHeader("Content-Length");
        long value;
        try { value = Long.parseLong(raw); }
        catch (Exception e) { throw new IllegalArgumentException("Content-Length upload wajib valid."); }
        if (value < 1 || value > JurnalFileService.MAX_UPLOAD_BYTES)
            throw new IllegalArgumentException("Ukuran upload tidak diizinkan.");
        return value;
    }

    private static Long pathId(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || !path.matches("/[1-9][0-9]*"))
            throw new IllegalArgumentException("ID file tidak valid.");
        return Long.valueOf(path.substring(1));
    }

    private static Long requiredLong(HttpServletRequest req, String name) {
        String value = required(req, name);
        try { Long id = Long.valueOf(value); if (id.longValue() < 1) throw new Exception(); return id; }
        catch (Exception e) { throw new IllegalArgumentException(name + " tidak valid."); }
    }

    private static Integer optionalInteger(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null || value.trim().length() == 0) return null;
        try { int result = Integer.parseInt(value); if (result < 1 || result > 999) throw new Exception(); return result; }
        catch (Exception e) { throw new IllegalArgumentException(name + " tidak valid."); }
    }

    private static String required(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null || value.trim().length() == 0)
            throw new IllegalArgumentException(name + " wajib diisi.");
        return value.trim();
    }

    private static String safeMime(String value) {
        String x = value == null ? "" : value.trim().toLowerCase();
        return x.matches("[a-z0-9.+-]+/[a-z0-9.+-]+") ? x : "application/octet-stream";
    }

    private static String headerFileName(String value) {
        String x = value == null ? "file" : value.replaceAll("[\\r\\n\\\"\\\\/]", "_").trim();
        return x.length() == 0 ? "file" : x;
    }

    private static void securityHeaders(HttpServletResponse res, String requestId) {
        res.setHeader("Cache-Control", "private, no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "SAMEORIGIN");
        res.setHeader("X-Request-Id", requestId);
    }

    private static String requestId(HttpServletRequest req) {
        return Long.toHexString(System.currentTimeMillis()) + Integer.toHexString(System.identityHashCode(req));
    }

    private static void failure(JSONObject out, String code, String message) {
        try { out.put("ok", false).put("code", code).put("message", message); }
        catch (Exception ignored) {}
    }
}
