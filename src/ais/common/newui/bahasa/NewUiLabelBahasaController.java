package ais.common.newui.bahasa;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudCsrf;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFacade;
import ais.action.master.generic.v2.GenericCrudHttpController;
import ais.action.master.generic.v2.GenericCrudJson;
import ais.action.master.generic.v2.GenericCrudPage;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudSubmittedValues;
import ais.action.master.helper.HapusBahasaTakBermaknaHelper;
import ais.action.master.helper.TerjemahMassalHelper;
import ais.common.AiTerjemah;
import ais.common.Common;
import ais.common.DefaultBahasaSeed;
import ais.common.MemoryDbUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.LabelBahasa;
import ais.database.model.Tbmuser;

/**
 * Adapter native Pengaturan Bahasa. Data induk tetap memakai Generic CRUD dan
 * lifecycle {@code LabelBahasaAction}; controller ini hanya menambahkan filter
 * terjemahan serta operasi massal yang tidak dapat direpresentasikan oleh CRUD
 * satu-baris.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiLabelBahasaController {
    private static final String MODULE = "root";
    private static final String PAGE = "label_bahasa";
    private static final int MAKS_BARIS_PEKERJAAN = 50000;
    private static final long UMUR_PEKERJAAN = 60L * 60L * 1000L;
    private static final Map<String, Job> JOBS = new ConcurrentHashMap<String, Job>();
    private static final String[] ENTITIES = new String[] { "LabelBahasa" };
    private static final String[] METHODS = new String[] {
        "doBeforeCompose", "doAfterCompose", "onEvent", "onResetKeDefault",
        "onHapusTakBermakna", "render", "call", "onAdd", "onSave",
        "onTerjemahkanOtomatis", "onTerjemahkanUlangAiSaja",
        "onTerjemahkanUlang", "initCriteria", "onSearchDefault",
        "checkNamaLabelBahasa"
    };

    private NewUiLabelBahasaController() { }

    public static void handle(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String action = text(request.getParameter("action"), "meta").toLowerCase();
        if (!isCustom(action)) {
            delegateGeneric(request, response, action);
            return;
        }

        String guardAction = guardAction(action);
        if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, PAGE, guardAction)) {
            write(response, 403, GenericCrudResult.error(
                    "ACTION_FORBIDDEN", "Hak akses Pengaturan Bahasa tidak tersedia."));
            return;
        }
        Tbmuser user = Common.getCurrentUser(request);
        if (user == null || user.getUserId() == null || request.getSession(false) == null) {
            write(response, 401, GenericCrudResult.error(
                    "AUTH_REQUIRED", "Sesi pengguna tidak tersedia."));
            return;
        }

        try {
            if ("meta".equals(action)) {
                metadata(request, response);
            } else if ("list".equals(action)) {
                list(request, response);
            } else if ("create".equals(action) || "update".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                save(request, response, action);
            } else if ("delete".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                delete(request, response);
            } else if ("translate_single".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                translateSingle(request, response, user);
            } else if ("translate_start".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                startTranslation(request, response, owner(request, user));
            } else if ("reset_start".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                startReset(request, response, user, owner(request, user));
            } else if ("cleanup_start".equals(action)) {
                GenericCrudCsrf.requireMutation(request);
                startCleanup(request, response, owner(request, user));
            } else if ("language_job_status".equals(action)) {
                jobStatus(request, response, owner(request, user));
            }
        } catch (GenericCrudException e) {
            write(response, e.getStatus(), GenericCrudResult.error(e.getCode(), e.getMessage()));
        } catch (IllegalArgumentException e) {
            write(response, 422, GenericCrudResult.error("VALIDATION_FAILED", e.getMessage()));
        } catch (Exception e) {
            record(e, "NewUiLabelBahasaController");
            if (!response.isCommitted()) write(response, 500, GenericCrudResult.error(
                    "INTERNAL_ERROR", "Pengaturan Bahasa gagal diproses. Detail dicatat di log server."));
        }
    }

    private static boolean isCustom(String action) {
        return "meta".equals(action) || "list".equals(action) || "delete".equals(action)
                || "create".equals(action) || "update".equals(action)
                || "translate_single".equals(action) || "translate_start".equals(action)
                || "reset_start".equals(action) || "cleanup_start".equals(action)
                || "language_job_status".equals(action);
    }

    private static String guardAction(String action) {
        if ("translate_single".equals(action) || "translate_start".equals(action)
                || "reset_start".equals(action)) return "update";
        if ("create".equals(action) || "update".equals(action)) return action;
        if ("delete".equals(action) || "cleanup_start".equals(action)) return "delete";
        return "list";
    }

    private static void delegateGeneric(HttpServletRequest request,
            HttpServletResponse response, String action) throws Exception {
        if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, PAGE, action)) {
            write(response, 403, GenericCrudResult.error(
                    "ACTION_FORBIDDEN", "Peran aktif tidak memiliki izin untuk aksi ini."));
            return;
        }
        GenericCrudDefinition definition = definition();
        if (definition == null) {
            write(response, 501, GenericCrudResult.error(
                    "ADAPTER_NOT_IMPLEMENTED", "Adapter data Label Bahasa belum tersedia."));
            return;
        }
        bind(request, definition);
        GenericCrudHttpController.handle(request, response);
    }

    private static GenericCrudDefinition definition() {
        return GenericCrudDefinitionRegistry.tryAutoRegister(
                MODULE, PAGE, ENTITIES, "ais.action.master", "LabelBahasaAction", METHODS);
    }

    private static GenericCrudRequestContext context(HttpServletRequest request) throws Exception {
        GenericCrudDefinition definition = definition();
        if (definition == null) throw new GenericCrudException(501,
                "ADAPTER_NOT_IMPLEMENTED", "Adapter data Label Bahasa belum tersedia.");
        bind(request, definition);
        return new GenericCrudFacade().context(
                request, definition.getEntityKey(), MODULE, PAGE);
    }

    private static void bind(HttpServletRequest request, GenericCrudDefinition definition) {
        request.setAttribute("genericCrudEntityKey", definition.getEntityKey());
        request.setAttribute("genericCrudModuleKey", MODULE);
        request.setAttribute("genericCrudPageKey", PAGE);
    }

    private static void metadata(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        GenericCrudFacade facade = new GenericCrudFacade();
        GenericCrudRequestContext context = context(request);
        Map data = facade.metadata(context);
        data.put("csrf", GenericCrudCsrf.token(request));
        boolean canTranslate = NewUiRouteGuard.isActionAuthorized(
                request, MODULE, PAGE, "update");
        boolean canCleanup = NewUiRouteGuard.isActionAuthorized(
                request, MODULE, PAGE, "delete");
        data.put("languageUtilities", Boolean.TRUE);
        data.put("canTranslate", Boolean.valueOf(canTranslate));
        data.put("canResetLanguage", Boolean.valueOf(canTranslate));
        data.put("canCleanupLanguage", Boolean.valueOf(canCleanup));
        // LabelBahasa tidak mempunyai kolom aktif, sehingga CRUD otomatis tidak
        // menawarkan delete. Halaman lama melakukan hard-delete dan controller
        // ini mempertahankannya dengan guard DELETE eksplisit.
        data.put("canDelete", Boolean.valueOf(canCleanup));
        List options = new ArrayList();
        options.add(option("", "Semua"));
        options.add(option("BELUM", "Belum diterjemahkan"));
        options.add(option("SUDAH", "Sudah diterjemahkan lengkap"));
        data.put("translationStatusOptions", options);
        write(response, 200, GenericCrudResult.ok("Metadata berhasil dimuat.", data));
    }

    private static Map option(String value, String label) {
        Map result = new LinkedHashMap();
        result.put("value", value);
        result.put("label", label);
        return result;
    }

    private static void list(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        GenericCrudRequestContext context = context(request);
        if (!context.isCanRead()) throw new GenericCrudException(
                403, "READ_FORBIDDEN", "Hak baca data tidak tersedia.");
        int page = integer(request.getParameter("page"), 1);
        int pageSize = integer(request.getParameter("pageSize"), 10);
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
        String query = text(request.getParameter("q"), "");
        String status = translationStatus(request.getParameter("translationStatus"));
        Session session = HibernateUtil.openSession();
        try {
            Criteria count = session.createCriteria(LabelBahasa.class);
            applyFilters(count, query, status);
            Number total = (Number) count.setProjection(Projections.rowCount()).uniqueResult();
            Criteria rowsQuery = session.createCriteria(LabelBahasa.class);
            applyFilters(rowsQuery, query, status);
            rowsQuery.addOrder(Order.asc("nama"));
            rowsQuery.setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);
            List<LabelBahasa> entities = rowsQuery.list();
            List rows = new ArrayList();
            for (LabelBahasa entity : entities) rows.add(row(entity));
            GenericCrudPage result = new GenericCrudPage();
            result.setRows(rows);
            result.setTotal(total == null ? 0L : total.longValue());
            result.setPage(page);
            result.setPageSize(pageSize);
            write(response, 200, GenericCrudResult.ok("Data berhasil dimuat.", result));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void applyFilters(Criteria criteria, String query, String status) {
        if (query != null && query.trim().length() > 0) {
            String value = query.trim();
            Disjunction any = Restrictions.disjunction();
            any.add(Restrictions.ilike("nama", value, MatchMode.ANYWHERE));
            any.add(Restrictions.ilike("indonesia", value, MatchMode.ANYWHERE));
            any.add(Restrictions.ilike("english", value, MatchMode.ANYWHERE));
            any.add(Restrictions.ilike("arab", value, MatchMode.ANYWHERE));
            any.add(Restrictions.ilike("mandarin", value, MatchMode.ANYWHERE));
            any.add(Restrictions.ilike("keterangan", value, MatchMode.ANYWHERE));
            criteria.add(any);
        }
        if ("BELUM".equals(status)) {
            criteria.add(Restrictions.or(
                    pending("english"), Restrictions.or(pending("arab"), pending("mandarin"))));
        } else if ("SUDAH".equals(status)) {
            criteria.add(complete("english"));
            criteria.add(complete("arab"));
            criteria.add(complete("mandarin"));
        }
    }

    private static org.hibernate.criterion.Criterion pending(String property) {
        return Restrictions.or(Restrictions.isNull(property),
                Restrictions.or(Restrictions.eq(property, ""),
                        Restrictions.eqProperty(property, "indonesia")));
    }

    private static org.hibernate.criterion.Criterion complete(String property) {
        return Restrictions.and(Restrictions.isNotNull(property),
                Restrictions.and(Restrictions.ne(property, ""),
                        Restrictions.neProperty(property, "indonesia")));
    }

    private static String translationStatus(String value) {
        String status = text(value, "").toUpperCase();
        if (status.length() == 0 || "BELUM".equals(status) || "SUDAH".equals(status)) return status;
        throw new IllegalArgumentException("Status terjemahan tidak valid.");
    }

    private static List<LabelBahasa> jobRows(HttpServletRequest request) {
        String query = text(request.getParameter("q"), "");
        String status = translationStatus(request.getParameter("translationStatus"));
        Session session = HibernateUtil.openSession();
        try {
            Criteria count = session.createCriteria(LabelBahasa.class);
            applyFilters(count, query, status);
            Number total = (Number) count.setProjection(Projections.rowCount()).uniqueResult();
            long size = total == null ? 0L : total.longValue();
            if (size <= 0L) throw new IllegalArgumentException(
                    "Tidak ada baris yang sesuai filter untuk diproses.");
            if (size > MAKS_BARIS_PEKERJAAN) throw new IllegalArgumentException(
                    "Filter mencakup lebih dari 50.000 baris. Persempit pencarian terlebih dahulu.");
            Criteria rows = session.createCriteria(LabelBahasa.class);
            applyFilters(rows, query, status);
            rows.addOrder(Order.asc("nama"));
            return new ArrayList<LabelBahasa>(rows.list());
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void translateSingle(HttpServletRequest request,
            HttpServletResponse response, final Tbmuser user) throws Exception {
        final Long id = positive(request.getParameter("id"), "ID label bahasa");
        Session read = HibernateUtil.openSession();
        final String source;
        try {
            LabelBahasa item = (LabelBahasa) read.get(LabelBahasa.class, id);
            if (item == null) throw new IllegalArgumentException("Label bahasa tidak ditemukan.");
            source = text(item.getIndonesia(), "");
            if (source.length() == 0) throw new IllegalArgumentException(
                    "Bahasa Indonesia wajib diisi sebelum diterjemahkan.");
        } finally {
            HibernateUtil.closeSessionQuietly(read);
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        String english;
        String arab;
        String mandarin;
        try {
            Future<String> en = executor.submit(new java.util.concurrent.Callable<String>() {
                public String call() { return AiTerjemah.terjemah(source, "english"); }
            });
            Future<String> ar = executor.submit(new java.util.concurrent.Callable<String>() {
                public String call() { return AiTerjemah.terjemah(source, "arab"); }
            });
            Future<String> zh = executor.submit(new java.util.concurrent.Callable<String>() {
                public String call() { return AiTerjemah.terjemah(source, "mandarin"); }
            });
            english = en.get();
            arab = ar.get();
            mandarin = zh.get();
        } finally {
            executor.shutdown();
        }

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        LabelBahasa item;
        try {
            tx = session.beginTransaction();
            item = (LabelBahasa) session.get(LabelBahasa.class, id);
            if (item == null) throw new IllegalArgumentException("Label bahasa tidak ditemukan.");
            if (notBlank(english)) item.setEnglish(english);
            if (notBlank(arab)) item.setArab(arab);
            if (notBlank(mandarin)) item.setMandarin(mandarin);
            item.setTanggal_dirubah(new Date());
            item.setOleh(user.getUserNama());
            item.setOlehId(Common.generateOlehId(user));
            session.update(item);
            session.flush();
            tx.commit();
            updateCache(item);
            write(response, 200, GenericCrudResult.ok(
                    "Terjemahan baris berhasil diperbarui.", row(item)));
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void startTranslation(HttpServletRequest request,
            HttpServletResponse response, String owner) throws Exception {
        purgeJobs();
        ensureNoRunning(owner);
        String mode = text(request.getParameter("mode"), "automatic").toLowerCase();
        boolean overwrite;
        boolean aiOnly;
        String title;
        if ("automatic".equals(mode)) {
            overwrite = false;
            aiOnly = false;
            title = "Terjemahkan Otomatis";
        } else if ("overwrite".equals(mode)) {
            overwrite = true;
            aiOnly = false;
            title = "Terjemahkan Ulang";
        } else if ("ai".equals(mode)) {
            overwrite = true;
            aiOnly = true;
            title = "Terjemahkan Ulang via AI";
        } else {
            throw new IllegalArgumentException("Mode terjemahan tidak valid.");
        }
        List<LabelBahasa> rows = jobRows(request);
        final TerjemahMassalHelper helper = new TerjemahMassalHelper(rows, overwrite, aiOnly);
        final Job job = new Job(owner, "translation", title);
        job.translation = helper;
        job.total = rows.size();
        JOBS.put(job.id, job);
        job.state = "RUNNING";
        job.message = "Sedang menerjemahkan label sesuai filter.";
        helper.mulai();
        monitorTranslation(job, helper);
        write(response, 200, GenericCrudResult.ok(
                "Pekerjaan terjemahan dimulai.", jobMap(job)));
    }

    private static void monitorTranslation(final Job job, final TerjemahMassalHelper helper) {
        Thread monitor = new Thread(new Runnable() {
            public void run() {
                try {
                    while (!helper.isSelesai()) Thread.sleep(200L);
                    helper.terapkanKeMemori();
                    job.changed = helper.getDiperbarui();
                    job.processed = helper.getTotal();
                    job.percent = 100;
                    job.state = "DONE";
                    job.message = "Terjemahan selesai. Periksa istilah yang belum dikenali secara manual.";
                } catch (Throwable e) {
                    fail(job, e, "NewUiLabelBahasaController.translation");
                } finally {
                    job.finishedAt = System.currentTimeMillis();
                }
            }
        }, "newui-label-bahasa-translate-" + job.id);
        monitor.setDaemon(true);
        monitor.start();
    }

    private static void startReset(HttpServletRequest request,
            HttpServletResponse response, final Tbmuser user, String owner) throws Exception {
        purgeJobs();
        ensureNoRunning(owner);
        final List<LabelBahasa> rows = jobRows(request);
        final Job job = new Job(owner, "reset", "Reset ke Bahasa Default");
        job.total = rows.size();
        JOBS.put(job.id, job);
        Thread worker = new Thread(new Runnable() {
            public void run() { runReset(job, rows, user); }
        }, "newui-label-bahasa-reset-" + job.id);
        worker.setDaemon(true);
        worker.start();
        write(response, 200, GenericCrudResult.ok(
                "Pekerjaan reset dimulai.", jobMap(job)));
    }

    private static void runReset(Job job, List<LabelBahasa> source, Tbmuser user) {
        job.state = "RUNNING";
        job.message = "Mengembalikan label ke nilai seed bawaan.";
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            final int batchSize = 250;
            for (int start = 0; start < source.size(); start += batchSize) {
                int end = Math.min(source.size(), start + batchSize);
                Transaction tx = session.beginTransaction();
                List<LabelBahasa> updated = new ArrayList<LabelBahasa>();
                try {
                    for (int i = start; i < end; i++) {
                        LabelBahasa old = source.get(i);
                        LabelBahasa item = old == null || old.getId() == null ? null
                                : (LabelBahasa) session.get(LabelBahasa.class, old.getId());
                        if (item == null || item.getNama() == null) continue;
                        String indonesia = DefaultBahasaSeed.getIndonesia(item.getNama());
                        String english = DefaultBahasaSeed.getEnglish(item.getNama());
                        String arab = DefaultBahasaSeed.getArab(item.getNama());
                        if (notBlank(indonesia)) item.setIndonesia(indonesia);
                        item.setEnglish(english == null ? "" : english);
                        item.setArab(arab == null ? "" : arab);
                        item.setMandarin("");
                        item.setTanggal_dirubah(new Date());
                        item.setOleh(user.getUserNama());
                        item.setOlehId(Common.generateOlehId(user));
                        session.update(item);
                        updated.add(item);
                        job.processed++;
                        job.recent = item.getNama();
                    }
                    session.flush();
                    tx.commit();
                    for (LabelBahasa item : updated) updateCache(item);
                    job.changed += updated.size();
                    job.percent = (int) (job.processed * 100L / Math.max(1, job.total));
                    session.clear();
                } catch (Exception e) {
                    rollback(tx);
                    throw e;
                }
            }
            job.percent = 100;
            job.state = "DONE";
            job.message = "Reset bahasa default selesai.";
        } catch (Throwable e) {
            fail(job, e, "NewUiLabelBahasaController.reset");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
            job.finishedAt = System.currentTimeMillis();
        }
    }

    private static void startCleanup(HttpServletRequest request,
            HttpServletResponse response, String owner) throws Exception {
        purgeJobs();
        ensureNoRunning(owner);
        final List<LabelBahasa> rows = jobRows(request);
        final HapusBahasaTakBermaknaHelper helper = new HapusBahasaTakBermaknaHelper(rows);
        final Job job = new Job(owner, "cleanup", "Hapus Label Tak Bermakna");
        job.cleanup = helper;
        job.total = rows.size();
        JOBS.put(job.id, job);
        job.state = "RUNNING";
        job.message = "AI sedang menilai label sesuai filter.";
        helper.mulai();
        Thread monitor = new Thread(new Runnable() {
            public void run() {
                try {
                    while (!helper.isSelesai()) Thread.sleep(200L);
                    purgeDeletedCache(rows);
                    job.processed = helper.getDiperiksa();
                    job.changed = helper.getDihapus();
                    job.percent = 100;
                    job.state = "DONE";
                    job.message = "Pembersihan selesai; label yang ragu tetap dipertahankan.";
                } catch (Throwable e) {
                    fail(job, e, "NewUiLabelBahasaController.cleanup");
                } finally {
                    job.finishedAt = System.currentTimeMillis();
                }
            }
        }, "newui-label-bahasa-cleanup-" + job.id);
        monitor.setDaemon(true);
        monitor.start();
        write(response, 200, GenericCrudResult.ok(
                "Pekerjaan pembersihan dimulai.", jobMap(job)));
    }

    private static void jobStatus(HttpServletRequest request,
            HttpServletResponse response, String owner) throws Exception {
        purgeJobs();
        Job job = JOBS.get(text(request.getParameter("jobId"), ""));
        if (job == null || !owner.equals(job.owner)) throw new IllegalArgumentException(
                "Pekerjaan bahasa tidak ditemukan atau sudah kedaluwarsa.");
        write(response, 200, GenericCrudResult.ok(
                "Status pekerjaan berhasil dimuat.", jobMap(job)));
    }

    private static Map jobMap(Job job) {
        Map data = new LinkedHashMap();
        data.put("jobId", job.id);
        data.put("type", job.type);
        data.put("title", job.title);
        data.put("state", job.state);
        data.put("message", job.message);
        int percent = job.percent;
        int processed = job.processed;
        int changed = job.changed;
        String phase = "processing";
        String recent = job.recent;
        List threads = new ArrayList();
        if (job.translation != null) {
            TerjemahMassalHelper helper = job.translation;
            percent = helper.isSelesai() ? 100 : helper.persen();
            processed = helper.isFasePersiapan() ? helper.getDiterjemah() : helper.getDiproses();
            changed = helper.getDiperbarui();
            phase = helper.isFasePersiapan() ? "translating" : "saving";
            recent = helper.getTerkiniGabung();
            addThreads(threads, helper.getStatusThread());
        } else if (job.cleanup != null) {
            HapusBahasaTakBermaknaHelper helper = job.cleanup;
            percent = helper.isSelesai() ? 100 : helper.persen();
            processed = helper.isFasePersiapan() ? helper.getDiperiksa() : helper.getDihapus();
            changed = helper.getDihapus();
            phase = helper.isFasePersiapan() ? "reviewing" : "deleting";
            recent = helper.getTerkiniGabung();
            addThreads(threads, helper.getStatusThread());
        }
        data.put("percent", Integer.valueOf(percent));
        data.put("phase", phase);
        data.put("total", Integer.valueOf(job.total));
        data.put("processed", Integer.valueOf(processed));
        data.put("changed", Integer.valueOf(changed));
        data.put("recent", recent == null ? "" : recent);
        data.put("threads", threads);
        data.put("done", Boolean.valueOf("DONE".equals(job.state)));
        data.put("failed", Boolean.valueOf("FAILED".equals(job.state)));
        return data;
    }

    private static void addThreads(List result, String[] values) {
        if (values == null) return;
        for (int i = 0; i < values.length; i++) {
            Map row = new LinkedHashMap();
            row.put("slot", Integer.valueOf(i + 1));
            row.put("status", values[i] == null ? "" : values[i]);
            result.add(row);
        }
    }

    private static void delete(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Long id = positive(request.getParameter("id"), "ID label bahasa");
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        String key = null;
        try {
            tx = session.beginTransaction();
            LabelBahasa item = (LabelBahasa) session.get(LabelBahasa.class, id);
            if (item == null) throw new IllegalArgumentException("Label bahasa tidak ditemukan.");
            key = item.getNama();
            session.delete(item);
            session.flush();
            tx.commit();
            removeCache(key);
            Map data = new LinkedHashMap();
            data.put("id", id);
            write(response, 200, GenericCrudResult.ok("Label bahasa berhasil dihapus.", data));
        } catch (Exception e) {
            rollback(tx);
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /** Simpan lewat mesin CRUD kanonik, lalu sinkronkan mirror kamus dalam JVM. */
    private static void save(HttpServletRequest request,
            HttpServletResponse response, String action) throws Exception {
        GenericCrudDefinition definition = definition();
        GenericCrudRequestContext context = context(request);
        GenericCrudFacade facade = new GenericCrudFacade();
        boolean create = "create".equals(action);
        Long id = create ? null : positive(request.getParameter("id"), "ID label bahasa");
        String previousKey = create ? null : labelKey(id);
        Map submitted = GenericCrudSubmittedValues.fromParameters(
                definition, request.getParameterMap(), create);
        GenericCrudResult result = create
                ? facade.create(context, submitted)
                : facade.update(context, id, submitted, request.getParameter("version"));
        if (result.isSuccess()) {
            if (create && result.getData() instanceof Map) {
                Object rawId = ((Map) result.getData()).get("id");
                if (rawId instanceof Number) id = Long.valueOf(((Number) rawId).longValue());
                else if (rawId != null) id = Long.valueOf(String.valueOf(rawId));
            }
            LabelBahasa saved = label(id);
            if (previousKey != null && saved != null
                    && !previousKey.equals(saved.getNama())) removeCache(previousKey);
            updateCache(saved);
        }
        write(response, 200, result);
    }

    private static String labelKey(Long id) {
        LabelBahasa item = label(id);
        return item == null ? null : item.getNama();
    }

    private static LabelBahasa label(Long id) {
        if (id == null) return null;
        Session session = HibernateUtil.openSession();
        try {
            return (LabelBahasa) session.get(LabelBahasa.class, id);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static Map row(LabelBahasa item) {
        Map row = new LinkedHashMap();
        row.put("id", item.getId());
        row.put("nama", item.getNama());
        row.put("indonesia", item.getIndonesia());
        row.put("english", item.getEnglish());
        row.put("arab", item.getArab());
        row.put("mandarin", item.getMandarin());
        row.put("keterangan", item.getKeterangan());
        row.put("tanggal_dirubah", item.getTanggal_dirubah());
        row.put("translationStatus", fullyTranslated(item) ? "SUDAH" : "BELUM");
        return row;
    }

    private static boolean fullyTranslated(LabelBahasa item) {
        String source = text(item.getIndonesia(), "");
        return translated(item.getEnglish(), source)
                && translated(item.getArab(), source)
                && translated(item.getMandarin(), source);
    }

    private static boolean translated(String value, String source) {
        return notBlank(value) && !value.trim().equalsIgnoreCase(source.trim());
    }

    private static void updateCache(LabelBahasa item) {
        if (item == null || item.getNama() == null) return;
        MemoryDbUtil.getBahasaIndonesias().put(item.getNama(), cacheValue(item.getIndonesia()));
        MemoryDbUtil.getBahasaEnglishs().put(item.getNama(), cacheValue(item.getEnglish()));
        MemoryDbUtil.getBahasaArabs().put(item.getNama(), cacheValue(item.getArab()));
        MemoryDbUtil.getBahasaMandarins().put(item.getNama(), cacheValue(item.getMandarin()));
    }

    private static String cacheValue(String value) {
        return value == null ? "" : value;
    }

    private static void removeCache(String key) {
        if (key == null) return;
        MemoryDbUtil.getBahasaIndonesias().remove(key);
        MemoryDbUtil.getBahasaEnglishs().remove(key);
        MemoryDbUtil.getBahasaArabs().remove(key);
        MemoryDbUtil.getBahasaMandarins().remove(key);
    }

    private static void purgeDeletedCache(List<LabelBahasa> source) {
        Set<Long> existing = new HashSet<Long>();
        Session session = HibernateUtil.openSession();
        try {
            final int batch = 500;
            for (int start = 0; start < source.size(); start += batch) {
                int end = Math.min(source.size(), start + batch);
                List<Long> ids = new ArrayList<Long>();
                for (int i = start; i < end; i++) {
                    LabelBahasa item = source.get(i);
                    if (item != null && item.getId() != null) ids.add(item.getId());
                }
                if (!ids.isEmpty()) existing.addAll(session.createQuery(
                        "select id from ais.database.model.LabelBahasa where id in (:ids)")
                        .setParameterList("ids", ids).list());
            }
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
        for (LabelBahasa item : source) {
            if (item != null && item.getId() != null && !existing.contains(item.getId())) {
                removeCache(item.getNama());
            }
        }
    }

    private static void ensureNoRunning(String owner) throws GenericCrudException {
        for (Job job : JOBS.values()) {
            if (owner.equals(job.owner) && !job.finished()) {
                throw new GenericCrudException(409, "LANGUAGE_JOB_RUNNING",
                        "Masih ada pekerjaan bahasa yang berjalan pada sesi ini.");
            }
        }
    }

    private static void purgeJobs() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Job> entry : JOBS.entrySet()) {
            Job job = entry.getValue();
            if (job.finishedAt > 0L && now - job.finishedAt > UMUR_PEKERJAAN) {
                JOBS.remove(entry.getKey());
            }
        }
    }

    private static String owner(HttpServletRequest request, Tbmuser user) {
        return request.getSession().getId() + ":" + String.valueOf(user.getUserId());
    }

    private static void fail(Job job, Throwable error, String source) {
        job.state = "FAILED";
        job.message = "Pekerjaan gagal. Detail dicatat di log server.";
        record(error, source);
    }

    private static void record(Throwable error, String source) {
        try {
            ais.common.ErrorAuditUtil.record(
                    error instanceof Exception ? (Exception) error : new Exception(error), source);
        } catch (Exception ignored) { }
    }

    private static int integer(String value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value); }
        catch (Exception ignored) { return fallback; }
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

    private static boolean notBlank(String value) {
        return value != null && value.trim().length() > 0;
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

    private static final class Job {
        final String id = UUID.randomUUID().toString().replace("-", "");
        final String owner;
        final String type;
        final String title;
        volatile String state = "QUEUED";
        volatile String message = "Menunggu pekerjaan dimulai.";
        volatile int percent;
        volatile int total;
        volatile int processed;
        volatile int changed;
        volatile String recent = "";
        volatile long finishedAt;
        volatile TerjemahMassalHelper translation;
        volatile HapusBahasaTakBermaknaHelper cleanup;

        Job(String owner, String type, String title) {
            this.owner = owner;
            this.type = type;
            this.title = title;
        }

        boolean finished() {
            return "DONE".equals(state) || "FAILED".equals(state);
        }
    }
}
