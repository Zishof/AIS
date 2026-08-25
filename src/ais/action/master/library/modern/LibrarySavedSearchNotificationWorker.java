package ais.action.master.library.modern;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.library.SearchHistory;
import ais.delivery.email.sender.MailSender;

/** Disabled-by-default dispatcher for saved-search APP/EMAIL/WHATSAPP alerts. */
public final class LibrarySavedSearchNotificationWorker {
    private static volatile ScheduledExecutorService scheduler;
    private LibrarySavedSearchNotificationWorker() { }

    public static synchronized void start() {
        if (scheduler != null || !enabled()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "library-saved-search-worker");
                thread.setDaemon(true);
                return thread;
            }
        });
        long minutes = intervalMinutes();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() { try { runOnce(); } catch (Throwable error) { audit(error); } }
        }, minutes, minutes, TimeUnit.MINUTES);
    }

    public static synchronized void stop() {
        ScheduledExecutorService value = scheduler;
        scheduler = null;
        if (value != null) value.shutdownNow();
    }

    @SuppressWarnings("unchecked")
    public static int runOnce() {
        if (!enabled()) return 0;
        Session session = null;
        int delivered = 0;
        try {
            session = HibernateUtil.openSession();
            List<SearchHistory> rows = (List<SearchHistory>) session.createCriteria(SearchHistory.class)
                    .add(Restrictions.eq("start", Integer.valueOf(1))).add(Restrictions.ge("banyak", Integer.valueOf(0)))
                    .setMaxResults(100).list();
            for (SearchHistory search : rows) {
                if (!due(search, new Date())) continue;
                Transaction tx = null;
                try {
                    tx = session.beginTransaction();
                    session.lock(search, org.hibernate.LockMode.UPGRADE);
                    if (search.getStart() == null || search.getStart().intValue() != 1 || search.getBanyak() == null
                            || search.getBanyak().intValue() < 0 || !due(search, new Date())) { tx.commit(); continue; }
                    Long libraryId = positiveLong(LibraryOperationsApi.savedSearchMeta(search.getRes(), "LIBRARY_ID", ""));
                    if (libraryId == null) { tx.commit(); continue; }
                    long current = LibraryOperationsApi.countSavedSearch(session, search.getQuer(), libraryId);
                    long baseline = search.getBanyak() == null ? 0L : search.getBanyak().longValue();
                    if (current <= baseline) { tx.commit(); continue; }
                    Tbmuser user = search.getOlehId() == null ? null : (Tbmuser) session.get(Tbmuser.class, search.getOlehId());
                    if (user == null || Boolean.FALSE.equals(user.getAktif())) { tx.commit(); continue; }
                    String channels = LibraryOperationsApi.savedSearchMeta(search.getRes(), "CHANNELS", "APP");
                    dispatch(user, search, current - baseline, channels);
                    search.setBanyak(Integer.valueOf((int) Math.min(Integer.MAX_VALUE, current)));
                    search.setTanggal_dirubah(new Date());
                    session.update(search);
                    tx.commit();
                    delivered++;
                } catch (Exception error) { if (tx != null && tx.isActive()) tx.rollback(); audit(error); }
            }
            LibraryTelemetry.record("saved-search-worker", delivered > 0 ? 200 : 204, 0L);
            return delivered;
        } catch (Exception error) {
            LibraryTelemetry.record("saved-search-worker", 500, 0L);
            audit(error);
            return delivered;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private static void dispatch(Tbmuser user, SearchHistory search, long newResults, String channels) throws Exception {
        String label = LibraryOperationsApi.savedSearchLabel(search.getRes());
        String subject = "Koleksi baru untuk pencarian " + label;
        String url = LibraryOperationsApi.savedSearchUrl(search.getQuer());
        String body = "Terdapat <b>" + newResults + "</b> hasil baru untuk pencarian <b>" + html(label)
                + "</b>.<br><a href=\"" + html(url) + "\">Buka hasil pencarian</a>";
        JSONArray users = new JSONArray().put(user.getUserId());
        boolean email = contains(channels, "EMAIL"), whatsapp = contains(channels, "WHATSAPP");
        String recipient = email && user.getEmail() != null ? user.getEmail().trim() : "";
        if (email || whatsapp) MailSender.sendMail(users, subject, body, "Perpustakaan", recipient, search, whatsapp);
        else MailSender.simpanNotif(users, "", subject, body, search);
    }

    private static boolean due(SearchHistory search, Date now) {
        String cadence = LibraryOperationsApi.savedSearchMeta(search.getRes(), "CADENCE", "NONE");
        if ("NONE".equals(cadence)) return false;
        if ("NEW".equals(cadence)) return true;
        Date changed = search.getTanggal_dirubah();
        if (changed == null) return true;
        long age = now.getTime() - changed.getTime();
        return "DAILY".equals(cadence) ? age >= TimeUnit.DAYS.toMillis(1L)
                : "WEEKLY".equals(cadence) && age >= TimeUnit.DAYS.toMillis(7L);
    }

    private static boolean contains(String csv, String value) {
        if (csv == null) return false;
        for (String token : csv.split(",")) if (value.equals(token.trim())) return true;
        return false;
    }

    private static Long positiveLong(String value) {
        try { long parsed=Long.parseLong(value);return parsed>0L?Long.valueOf(parsed):null; }
        catch (Exception ignored) { return null; }
    }

    private static boolean enabled() { return Boolean.parseBoolean(config("library.saved_search.worker.enabled", "false")); }
    private static long intervalMinutes() { try { return Math.max(1L, Math.min(1440L, Long.parseLong(config("library.saved_search.worker.interval_minutes", "15")))); } catch (Exception ignored) { return 15L; } }
    private static String config(String key, String fallback) { try { Konfigurasi value=Common.getKonfigurasi(key,fallback);return value==null||value.getNilai()==null?fallback:value.getNilai().trim(); } catch (Exception ignored) { return fallback; } }
    private static String html(String value) { return value == null ? "" : value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    private static void audit(Throwable error) { try { ais.common.ErrorAuditUtil.record(error, "library saved-search notification worker"); } catch (Throwable ignored) { } }
}
