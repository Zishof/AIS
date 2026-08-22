package ais.action.master.jurnal;

import java.util.Date;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/** Rebuildable, idempotent daily usage projection from existing RepoUsageEvent. */
public final class JurnalUsageAggregationService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    /** Site-wide rebuild is deliberately administrator-only. */
    public int rebuildDaily(Date fromInclusive, Date toExclusive, Tbmuser actor) {
        auth.requireCrud(actor, "statistics", "update");
        auth.requireAdministrator(actor);
        return rebuild(null, fromInclusive, toExclusive, actor);
    }

    /** Journal-scoped rebuild may be delegated through an active assignment. */
    public int rebuildDaily(Long journalId, Date fromInclusive, Date toExclusive, Tbmuser actor) {
        auth.requireCrud(actor, "statistics", "update");
        if (journalId == null) throw new IllegalArgumentException("Jurnal wajib diisi.");
        return rebuild(journalId, fromInclusive, toExclusive, actor);
    }

    private int rebuild(Long journalId, Date from, Date to, Tbmuser actor) {
        if (from == null || to == null || !to.after(from))
            throw new IllegalArgumentException("Rentang agregasi tidak valid.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try {
            if (own) tx.begin();
            if (journalId != null) auth.requireJournalScope(s, actor, journalId, null, null, false, "JOURNAL");
            String journalFilter = journalId == null ? "" : " and jurnal_penelitian_id=:j";
            SQLQuery delete = s.createSQLQuery(
                    "delete from penelitiandanpengabdian.agregat_penggunaan_jurnal "
                    + "where bucket_type='DAY' and bucket_start>=date_trunc('day',cast(:f as timestamp)) "
                    + "and bucket_start<cast(:t as timestamp)" + journalFilter);
            bind(delete, from, to, journalId);
            delete.executeUpdate();
            int changed = insert(s, from, to, journalId, "'TOTAL'", "'ALL'", "")
                    + insert(s, from, to, journalId, "'COUNTRY'",
                            "coalesce(nullif(upper(u.country_code),''),'UNKNOWN')", ",u.country_code")
                    + insert(s, from, to, journalId, "'REFERRER'",
                            "left(coalesce(nullif(lower(u.referrer_host),''),'DIRECT'),255)", ",u.referrer_host");
            if (own) tx.commit();
            return changed;
        } catch (RuntimeException e) {
            if (own && tx.isActive()) tx.rollback();
            throw e;
        }
    }

    private static int insert(Session s, Date from, Date to, Long journalId,
            String dimensionType, String dimensionKey, String extraGroup) {
        String scoped = journalId == null ? "" : " and j.id=:j";
        String sql = "insert into penelitiandanpengabdian.agregat_penggunaan_jurnal "
                + "(tenant_key,jurnal_penelitian_id,bucket_start,bucket_type,metric_key,dimension_type,dimension_key,metric_value,counter_report,lock_version,created_at,updated_at,aktif) "
                + "select i.tenant_key,j.id,date_trunc('day',u.occurred_at),'DAY',"
                + "case when u.event_type='DOWNLOAD' then 'DOWNLOAD' else 'VIEW' end,"
                + dimensionType + "," + dimensionKey + ",cast(count(*) as numeric),"
                + "case when u.event_type='DOWNLOAD' then 'PR' else 'IR' end,0,now(),now(),true "
                + "from public.repo_usage_event u join public.repo_item i on i.id=u.item_id "
                + "join penelitiandanpengabdian.jurnal_penelitian j on j.repo_collection_id=i.collection_id and j.aktif=true "
                + "where u.occurred_at>=:f and u.occurred_at<:t "
                + "and upper(coalesce(u.user_agent_class,'')) not in ('BOT','ROBOT','SPIDER','CRAWLER') "
                + "and u.event_type in ('VIEW','DOWNLOAD') "
                + "and i.document_type in ('JOURNAL_SUBMISSION','JOURNAL_ISSUE')" + scoped + " "
                + "group by i.tenant_key,j.id,date_trunc('day',u.occurred_at),u.event_type" + extraGroup;
        SQLQuery query = s.createSQLQuery(sql);
        bind(query, from, to, journalId);
        return query.executeUpdate();
    }

    private static void bind(SQLQuery query, Date from, Date to, Long journalId) {
        query.setTimestamp("f", from).setTimestamp("t", to);
        if (journalId != null) query.setLong("j", journalId.longValue());
    }
}
