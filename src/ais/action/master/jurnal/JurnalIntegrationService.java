package ais.action.master.jurnal;

import java.util.Date;
import java.util.Locale;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoIntegrationEvent;
import ais.database.model.repository.RepoItem;

/** Audited, idempotent attempt ledger for DOI/deposit/export/payment integrations. */
public final class JurnalIntegrationService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public RepoIntegrationEvent begin(Long itemId, String ignoredTenant, String service, String action,
            String requestId, String sanitizedPayload, Tbmuser actor) {
        authorize(actor, service, action); required(requestId, "Request ID wajib diisi.");
        String normalizedService = upper(service); String normalizedAction = upper(action);
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); RepoItem item = item(s, itemId);
            auth.requireItemScope(s, actor, item, false, stage(normalizedService));
            Query q = s.createQuery("from RepoIntegrationEvent where tenantKey=:t and serviceName=:s and actionName=:a and requestId=:r order by id desc");
            q.setString("t", item.getTenantKey()); q.setString("s", normalizedService); q.setString("a", normalizedAction); q.setString("r", clean(requestId)); q.setMaxResults(1);
            RepoIntegrationEvent existing = (RepoIntegrationEvent) q.uniqueResult();
            if (existing != null) { if (own) tx.commit(); return existing; }
            RepoIntegrationEvent event = new RepoIntegrationEvent(); event.setItemId(itemId); event.setTenantKey(item.getTenantKey());
            event.setServiceName(normalizedService); event.setActionName(normalizedAction); event.setStatus("PENDING");
            event.setActorId(actor.getUserId()); event.setRequestId(clean(requestId)); event.setRequestPayload(limit(sanitizedPayload, 262144));
            event.setCreatedAt(new Date()); s.save(event); if (own) tx.commit(); return event;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public RepoIntegrationEvent finish(Long eventId, boolean success, String sanitizedResponse, String error, Tbmuser actor) {
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); RepoIntegrationEvent event = (RepoIntegrationEvent) s.get(RepoIntegrationEvent.class, eventId);
            if (event == null) throw new IllegalArgumentException("Attempt integrasi tidak ditemukan."); authorize(actor, event.getServiceName(), event.getActionName());
            RepoItem item = item(s, event.getItemId()); auth.requireItemScope(s, actor, item, false, stage(event.getServiceName()));
            if (!"PENDING".equals(event.getStatus()) && !"RETRYING".equals(event.getStatus())) { if (own) tx.commit(); return event; }
            event.setStatus(success ? "SUCCEEDED" : "FAILED"); event.setResponsePayload(limit(sanitizedResponse, 262144));
            event.setErrorMessage(limit(error, 4000)); s.update(event); if (own) tx.commit(); return event;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public RepoIntegrationEvent retry(Long failedEventId, String newRequestId, Tbmuser actor) {
        Session s = HibernateUtil.currentSession(); RepoIntegrationEvent failed = (RepoIntegrationEvent) s.get(RepoIntegrationEvent.class, failedEventId);
        if (failed == null || !"FAILED".equals(failed.getStatus())) throw new IllegalArgumentException("Attempt gagal tidak ditemukan.");
        return begin(failed.getItemId(), failed.getTenantKey(), failed.getServiceName(), failed.getActionName(), newRequestId, failed.getRequestPayload(), actor);
    }

    private void authorize(Tbmuser actor, String service, String action) { String s = upper(service); if ("CROSSREF".equals(s) || "DATACITE".equals(s) || "DOI".equals(s) || "URN".equals(s)) auth.requireWorkflow(actor, "manageIdentifier"); else auth.requireCrud(actor, "integrations", "update"); }
    private static RepoItem item(Session s, Long id) { RepoItem item = (RepoItem) s.get(RepoItem.class, id); if (item == null || !Boolean.TRUE.equals(item.getAktif()) || !("JOURNAL_SUBMISSION".equals(item.getDocumentType()) || "JOURNAL_ISSUE".equals(item.getDocumentType()))) throw new IllegalArgumentException("Item jurnal tidak ditemukan."); return item; }
    private static String stage(String service) { String s = upper(service); return ("CROSSREF".equals(s) || "DATACITE".equals(s) || "DOI".equals(s) || "URN".equals(s)) ? "PUBLICATION" : "JOURNAL"; }
    private static String upper(String v) { String x = clean(v).toUpperCase(Locale.ENGLISH); if (!x.matches("[A-Z0-9_.-]{2,80}")) throw new IllegalArgumentException("Nama integrasi tidak valid."); return x; }
    private static String clean(String v) { return v == null ? "" : v.trim(); }
    private static String limit(String v, int n) { return v == null ? null : (v.length() <= n ? v : v.substring(0, n)); }
    private static void required(String v, String m) { if (clean(v).length() == 0) throw new IllegalArgumentException(m); }
}
