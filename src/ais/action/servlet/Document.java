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
 * Servlet publik Portal Dokumen Manajemen Sistem.
 *
 * Sumber katalog:
 * - Akreditasi sebagai kategori/ruang dokumen utama.
 * - DokumenAkreditasi sebagai struktur isi dokumen bertingkat.
 * - LampiranLain sebagai file download.
 *
 * Catatan session Hibernate:
 * - Servlet ini memakai HibernateUtil.currentSession(), TETAPI di konteks servlet (non-ZK)
 *   currentSession() bisa mengembalikan sesi ZK sisa thread pool yang sudah ditutup. Karena itu
 *   setiap pemakaian dijaga: bila sesi null/closed, re-acquire HibernateUtil.currentNativeSession()
 *   yang dijamin open (membuka ulang bila stale). Mencegah "Session is closed!".
 * - currentSession() tidak ditutup manual karena dikelola lifecycle aplikasi.
 * - Tidak memakai pengaturan character encoding langsung pada object response.
 */
public class Document extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String JSP_LANDING = "/WEB-INF/baru/modul/dms/landing_page.jsp";
    private static final String JSP_SERVICE = "/WEB-INF/baru/modul/dms/_dms_service.jsp";
    private static final String JSP_CONTENT = "/WEB-INF/baru/modul/dms/_dms_content.jsp";

    private static final int MAX_LIST_ITEM = 300;
    private static final int STREAM_BUFFER_SIZE = 16 * 1024;

    public Document() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(response, e);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            process(request, response);
        } catch (Exception e) {
            handleFatalError(response, e);
        }
    }

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

    private void addAktifCriterion(Criteria criteria) {
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
    }

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

    private List<Map<String, Object>> buildRootBreadcrumbs() {
        List<Map<String, Object>> breadcrumbs = new ArrayList<Map<String, Object>>();
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("label", "Beranda Dokumen");
        root.put("akreditasiId", "");
        root.put("indukId", "");
        breadcrumbs.add(root);
        return breadcrumbs;
    }

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

    private void streamFile(File file, HttpServletResponse response) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            streamInput(input, response.getOutputStream(), false);
        } finally {
            closeQuietly(input);
        }
    }

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

    private LampiranLain getLampiran(DokumenAkreditasi dokumen) {
        try {
            if (dokumen != null && dokumen.getId() != null) {
                return LampiranLain.ambil(dokumen.getId(), DokumenAkreditasi.class.getName());
            }
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/Document.java:635");
        }
        return null;
    }

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

    private boolean isDokumenAktif(DokumenAkreditasi dokumen) {
        return dokumen != null && isAktif(dokumen.getAktif()) && isSatuanKerjaVisible(dokumen);
    }

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

    private boolean isAktif(Boolean aktif) {
        return aktif == null || aktif.booleanValue();
    }

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

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

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
