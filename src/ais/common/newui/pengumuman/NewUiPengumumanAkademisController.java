package ais.common.newui.pengumuman;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudCsrf;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudHttpController;
import ais.action.master.generic.v2.GenericCrudJson;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.common.newui.NewUiUnggahRequest;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPengumumanAkademis;

/**
 * Jembatan Pengumuman Akademis untuk Flutter desktop dan Android.
 *
 * <p>CRUD data induk tetap dikerjakan kerangka Generic CRUD agar seluruh field,
 * validasi metadata, audit, scope institusi, dan optimistic locking yang sudah
 * dipakai halaman native tidak disalin di sini. Controller ini hanya menangani
 * relasi yang tidak dapat direpresentasikan oleh form CRUD datar: lampiran
 * pengumuman.</p>
 */
public final class NewUiPengumumanAkademisController {

    private static final String MODULE = "root";
    private static final String PAGE = "pengumuman_akademis";
    private static final String KIND_ATTACHMENT = "attachment";
    private static final String[] ENTITIES = {
        "PerguruanTinggi", "KategoriPengumuman", "PengumumanAkademis",
        "LampiranLain", "Jurusan", "Pertemuan", "Statusabsensi",
        "Siswa", "Mahasiswa"
    };
    private static final String[] METHODS = {
        "onPengumumanPerkuliahan", "onPenumumanWebsite", "onKategoriPengumuman",
        "onTeksBerjalan", "doBeforeCompose", "doAfterCompose", "onEvent",
        "render", "init", "onAdd", "tampilkanPolling",
        "tandaiKehadiranHomeDitampilkan", "isKehadiranHomeDitampilkan",
        "tampilkanKehadiranDosen", "tampilkanKehadiranGuru",
        "tampilPengumuanLangsungTampil", "tampilPengumuman",
        "tampilPengumumanLain", "onSave", "initCriteria",
        "onSearchDefault", "initIsiPolling"
    };

    private NewUiPengumumanAkademisController() {
    }

    public static void handle(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String action = text(request.getParameter("action"), "meta").toLowerCase();
        String kind = text(request.getParameter("kind"), "").toLowerCase();
        boolean attachment = "detail".equals(action) || "upload".equals(action)
                || "export_attachment".equals(action)
                || (KIND_ATTACHMENT.equals(kind)
                        && ("update".equals(action) || "delete".equals(action)));
        if (!attachment) {
            delegateGeneric(request, response, action);
            return;
        }

        if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, PAGE, action)) {
            write(response, 403, GenericCrudResult.error(
                    "ACTION_FORBIDDEN", "Hak akses lampiran pengumuman tidak tersedia."));
            return;
        }
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null || user.getUserId() == null) {
            write(response, 401, GenericCrudResult.error(
                    "AUTH_REQUIRED", "Sesi pengguna tidak tersedia."));
            return;
        }

        try {
            if ("detail".equals(action)) {
                detail(request, response);
            } else if ("upload".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                upload(request, response, user);
            } else if ("update".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                updateAttachment(request, response, user);
            } else if ("delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                deleteAttachment(request, response);
            } else if ("export_attachment".equals(action)) {
                download(request, response);
            }
        } catch (GenericCrudException e) {
            write(response, e.getStatus(), GenericCrudResult.error(e.getCode(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            write(response, 422, GenericCrudResult.error("VALIDATION_FAILED", e.getMessage()));
        } catch (Exception e) {
            try { ais.common.ErrorAuditUtil.record(e, "NewUiPengumumanAkademisController"); }
            catch (Exception ignored) { }
            if (!response.isCommitted()) write(response, 500, GenericCrudResult.error(
                    "INTERNAL_ERROR", "Lampiran pengumuman gagal diproses. Detail dicatat di log server."));
        }
    }

    private static void delegateGeneric(HttpServletRequest request,
            HttpServletResponse response, String action) throws Exception {
        if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, PAGE, action)) {
            write(response, 403, GenericCrudResult.error(
                    "ACTION_FORBIDDEN", "Peran aktif tidak memiliki izin untuk aksi ini."));
            return;
        }
        GenericCrudDefinition definition = GenericCrudDefinitionRegistry.tryAutoRegister(
                MODULE, PAGE, ENTITIES, "ais.action.master",
                "PengumumanAkademisAction", METHODS);
        if (definition == null) {
            write(response, 501, GenericCrudResult.error(
                    "ADAPTER_NOT_IMPLEMENTED", "Adapter data Pengumuman Akademis belum tersedia."));
            return;
        }
        request.setAttribute("genericCrudEntityKey", definition.getEntityKey());
        request.setAttribute("genericCrudModuleKey", MODULE);
        request.setAttribute("genericCrudPageKey", PAGE);
        GenericCrudHttpController.handle(request, response);
    }

    @SuppressWarnings("unchecked")
    private static void detail(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Long announcementId = positive(request.getParameter("id"), "ID pengumuman");
        Session session = HibernateUtil.openSession();
        try {
            PengumumanAkademis parent = (PengumumanAkademis) session.get(
                    PengumumanAkademis.class, announcementId);
            if (parent == null) throw new IllegalArgumentException("Pengumuman tidak ditemukan.");
            List<LampiranPengumumanAkademis> list = session
                    .createCriteria(LampiranPengumumanAkademis.class)
                    .add(Restrictions.eq("pengumumanAkademis", parent))
                    .addOrder(Order.desc("id")).setMaxResults(100).list();
            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            for (LampiranPengumumanAkademis item : list) rows.add(attachmentRow(item));
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("announcementId", announcementId);
            data.put("title", parent.getJudul());
            data.put("rows", rows);
            data.put("total", Integer.valueOf(rows.size()));
            data.put("truncated", Boolean.valueOf(rows.size() >= 100));
            data.put("maxUploadBytes", Long.valueOf(maxUploadBytes()));
            write(response, 200, GenericCrudResult.ok("Lampiran berhasil dimuat.", data));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void upload(HttpServletRequest request,
            HttpServletResponse response, Tbmuser user) throws Exception {
        if (!(request instanceof NewUiUnggahRequest)) {
            throw new IllegalArgumentException("Permintaan upload tidak membawa berkas.");
        }
        NewUiUnggahRequest upload = (NewUiUnggahRequest) request;
        File file = upload.getBerkas();
        try {
            if (file == null || !file.exists() || file.length() <= 0L) {
                throw new IllegalArgumentException("Berkas kosong atau tidak dapat dibaca.");
            }
            if (file.length() > maxUploadBytes()) {
                throw new IllegalArgumentException("Ukuran berkas melampaui batas upload institusi.");
            }
            Long announcementId = positive(request.getParameter("id"), "ID pengumuman");
            byte[] bytes;
            InputStream input = new FileInputStream(file);
            try { bytes = IOUtils.toByteArray(input); }
            finally { input.close(); }

            Session session = HibernateUtil.openSession();
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                PengumumanAkademis parent = (PengumumanAkademis) session.get(
                        PengumumanAkademis.class, announcementId);
                if (parent == null) throw new IllegalArgumentException("Pengumuman tidak ditemukan.");
                LampiranPengumumanAkademis item = new LampiranPengumumanAkademis();
                item.setFoto(Hibernate.createBlob(bytes));
                item.setMimeType(safeMime(upload.getMimeType()));
                item.setNama(safeFileName(upload.getNamaBerkas()));
                item.setKeterangan(limited(request.getParameter("keterangan"), 1000));
                item.setPengumumanAkademis(parent);
                item.setUploadDate(new Date());
                item.setDitampilkan(Boolean.valueOf(bool(request.getParameter("ditampilkan"), true)));
                item.setOleh(user.getUserNama());
                item.setOlehId(Common.generateOlehId(user));
                session.save(item);
                session.flush();
                tx.commit();
                write(response, 200, GenericCrudResult.ok(
                        "Lampiran pengumuman berhasil diunggah.", attachmentRow(item)));
            } catch (Exception e) {
                rollback(tx);
                throw e;
            } finally {
                HibernateUtil.closeSessionQuietly(session);
            }
        } finally {
            try { if (file != null && file.exists()) file.delete(); }
            catch (Exception ignored) { }
        }
    }

    private static void updateAttachment(HttpServletRequest request,
            HttpServletResponse response, Tbmuser user) throws Exception {
        Long announcementId = positive(request.getParameter("announcementId"), "ID pengumuman");
        Long id = positive(request.getParameter("id"), "ID lampiran");
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            LampiranPengumumanAkademis item = ownedAttachment(session, id, announcementId);
            item.setKeterangan(limited(request.getParameter("keterangan"), 1000));
            item.setDitampilkan(Boolean.valueOf(bool(request.getParameter("ditampilkan"), true)));
            item.setTanggal_dirubah(new Date());
            item.setOleh(user.getUserNama());
            item.setOlehId(Common.generateOlehId(user));
            session.update(item);
            session.flush();
            tx.commit();
            write(response, 200, GenericCrudResult.ok(
                    "Pengaturan lampiran berhasil disimpan.", attachmentRow(item)));
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void deleteAttachment(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Long announcementId = positive(request.getParameter("announcementId"), "ID pengumuman");
        Long id = positive(request.getParameter("id"), "ID lampiran");
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            LampiranPengumumanAkademis item = ownedAttachment(session, id, announcementId);
            session.delete(item);
            session.flush();
            tx.commit();
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("id", id);
            data.put("announcementId", announcementId);
            write(response, 200, GenericCrudResult.ok("Lampiran berhasil dihapus.", data));
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void download(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Long announcementId = positive(request.getParameter("announcementId"), "ID pengumuman");
        Long id = positive(request.getParameter("id"), "ID lampiran");
        Session session = HibernateUtil.openSession();
        File source;
        String name;
        String mime;
        try {
            LampiranPengumumanAkademis item = ownedAttachment(session, id, announcementId);
            name = safeFileName(item.getNama());
            mime = safeMime(item.getMimeType());
            source = item.ambilFile();
            if (source == null || !source.exists() || source.length() <= 0L) {
                throw new IllegalArgumentException("Isi lampiran tidak tersedia di penyimpanan.");
            }
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
        response.setContentType(mime);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + headerFileName(name) + "\"");
        if (source.length() <= Integer.MAX_VALUE) response.setContentLength((int) source.length());
        InputStream input = new FileInputStream(source);
        OutputStream output = response.getOutputStream();
        try {
            IOUtils.copy(input, output);
            output.flush();
        } finally {
            input.close();
        }
    }

    private static LampiranPengumumanAkademis ownedAttachment(Session session,
            Long id, Long announcementId) {
        LampiranPengumumanAkademis item = (LampiranPengumumanAkademis) session
                .createCriteria(LampiranPengumumanAkademis.class)
                .add(Restrictions.idEq(id)).setMaxResults(1).uniqueResult();
        if (item == null || item.getPengumumanAkademis() == null
                || !announcementId.equals(item.getPengumumanAkademis().getId())) {
            throw new IllegalArgumentException("Lampiran tidak ditemukan pada pengumuman ini.");
        }
        return item;
    }

    private static Map<String, Object> attachmentRow(LampiranPengumumanAkademis item) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", item.getId());
        row.put("name", item.getNama());
        row.put("mimeType", safeMime(item.getMimeType()));
        row.put("description", item.getKeterangan() == null ? "" : item.getKeterangan());
        row.put("uploadedAt", item.getUploadDate());
        row.put("visible", item.getDitampilkan());
        row.put("uploadedBy", item.getOleh() == null ? "" : item.getOleh());
        return row;
    }

    private static long maxUploadBytes() {
        long kb = 1024L;
        try {
            kb = Long.parseLong(Common.getKonfigurasi(
                    "ukuran_maksimal_file_diupload", "1024").getNilai());
        } catch (Exception ignored) { }
        if (kb < 1L) kb = 1024L;
        return Math.min(NewUiUnggahRequest.BATAS_UKURAN, kb * 1024L);
    }

    private static String safeFileName(String value) {
        String name = text(value, "lampiran").replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replace('\r', '_').replace('\n', '_').replace('"', '_').trim();
        if (name.length() > 255) name = name.substring(name.length() - 255);
        return name.length() == 0 ? "lampiran" : name;
    }

    private static String headerFileName(String value) {
        String name = safeFileName(value);
        return name.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    private static String safeMime(String value) {
        String mime = text(value, "application/octet-stream").toLowerCase();
        if (!mime.matches("[a-z0-9.+-]+/[a-z0-9.+-]+")) return "application/octet-stream";
        return mime;
    }

    private static String limited(String value, int max) {
        String result = value == null ? "" : value.trim();
        return result.length() <= max ? result : result.substring(0, max);
    }

    private static boolean bool(String value, boolean fallback) {
        if (value == null || value.trim().length() == 0) return fallback;
        return "true".equalsIgnoreCase(value) || "1".equals(value)
                || "yes".equalsIgnoreCase(value) || "ya".equalsIgnoreCase(value);
    }

    private static Long positive(String value, String label) {
        try {
            Long id = Long.valueOf(text(value, ""));
            if (id.longValue() <= 0L) throw new Exception();
            return id;
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " wajib valid.");
        }
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    private static void rollback(Transaction tx) {
        try { if (tx != null && tx.isActive()) tx.rollback(); }
        catch (Exception ignored) { }
    }

    private static void write(HttpServletResponse response, int status,
            GenericCrudResult result) throws Exception {
        if (response.isCommitted()) return;
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(GenericCrudJson.toJson(result));
    }
}
