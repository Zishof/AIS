package ais.action.master.jurnal;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** Read model for the admin workspace, constrained to active journal assignments. */
public final class JurnalWorkspaceService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public JSONObject load(String module, Long requestedJournalId, int page, int size, Tbmuser actor) throws Exception {
        auth.requireRead(actor, module);
        int safePage = Math.max(0, page), safeSize = Math.max(1, Math.min(100, size));
        Session s = HibernateUtil.currentSession();
        Set<Long> journalIds = journalIds(s, requestedJournalId, actor);
        List<JurnalPenelitian> journals = journals(s, journalIds, auth.isAdministrator(actor) && requestedJournalId == null);
        Set<Long> collectionIds = new LinkedHashSet<Long>();
        JSONArray journalRows = new JSONArray();
        for (JurnalPenelitian journal : journals) {
            if (journal.getRepoCollectionId() == null) continue;
            RepoCollection collection = (RepoCollection) s.get(RepoCollection.class, journal.getRepoCollectionId());
            if (collection == null || !Boolean.TRUE.equals(collection.getAktif()) || !"JOURNAL".equalsIgnoreCase(collection.getTipe())) continue;
            collectionIds.add(collection.getId());
            journalRows.put(new JSONObject().put("id", journal.getId()).put("collectionId", collection.getId())
                    .put("name", collection.getNama()).put("slug", collection.getKode())
                    .put("tenant", collection.getTenantKey()));
        }
        JSONArray items = new JSONArray();
        long totalItems = 0L;
        if (!collectionIds.isEmpty()) {
            Query count = s.createQuery("select count(*) from RepoItem where collectionId in (:c) and aktif=true");
            count.setParameterList("c", collectionIds); totalItems = ((Number) count.uniqueResult()).longValue();
            Query rows = s.createQuery("from RepoItem where collectionId in (:c) and aktif=true order by id desc");
            rows.setParameterList("c", collectionIds); rows.setFirstResult(safePage * safeSize); rows.setMaxResults(safeSize);
            @SuppressWarnings("unchecked") List<RepoItem> result = rows.list();
            for (RepoItem item : result) items.put(new JSONObject().put("id", item.getId())
                    .put("collectionId", item.getCollectionId()).put("type", item.getDocumentType())
                    .put("title", item.getTitle()).put("status", item.getWorkflowStatus())
                    .put("ownerId", item.getOwnerId()).put("updatedAt", item.getTanggal_dirubah() == null ? JSONObject.NULL : item.getTanggal_dirubah().getTime()));
        }
        JSONObject out = new JSONObject();
        out.put("module", module).put("journals", journalRows).put("items", items)
                .put("page", safePage).put("size", safeSize).put("totalItems", totalItems)
                .put("generatedAt", new Date().getTime());
        return out;
    }

    private Set<Long> journalIds(Session s, Long requested, Tbmuser actor) {
        Set<Long> ids = new LinkedHashSet<Long>();
        if (requested != null) {
            auth.requireJournalScope(s, actor, requested, null, null, false, "JOURNAL");
            ids.add(requested); return ids;
        }
        if (auth.isAdministrator(actor)) return ids;
        Query q = s.createQuery("select distinct jurnalPenelitianId from PenugasanTahapJurnal "
                + "where aktif=true and status='ACTIVE' and userId=:u and startsAt<=:n and (endsAt is null or endsAt>:n)");
        q.setString("u", actor.getUserId()).setTimestamp("n", new Date());
        @SuppressWarnings("unchecked") List<Long> assigned = q.list(); ids.addAll(assigned); return ids;
    }

    private List<JurnalPenelitian> journals(Session s, Set<Long> ids, boolean all) {
        if (!all && ids.isEmpty()) return new ArrayList<JurnalPenelitian>();
        Query q = all ? s.createQuery("from JurnalPenelitian where aktif=true order by id")
                : s.createQuery("from JurnalPenelitian where aktif=true and id in (:ids) order by id");
        if (!all) q.setParameterList("ids", ids);
        @SuppressWarnings("unchecked") List<JurnalPenelitian> result = q.list(); return result;
    }
}
