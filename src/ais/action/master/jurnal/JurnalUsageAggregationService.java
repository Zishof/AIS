package ais.action.master.jurnal;

import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/** Rebuildable daily usage projection from normalized RepoUsageEvent. */
public final class JurnalUsageAggregationService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

    public int rebuildDaily(Date fromInclusive,Date toExclusive,Tbmuser actor){
        auth.requireCrud(actor,"statistik","update");
        if(fromInclusive==null||toExclusive==null||!toExclusive.after(fromInclusive))throw new IllegalArgumentException("Rentang agregasi tidak valid.");
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();
        try{if(own)tx.begin();String sql=
            "insert into penelitiandanpengabdian.agregat_penggunaan_jurnal " +
            "(tenant_key,jurnal_penelitian_id,bucket_start,bucket_type,metric_key,dimension_type,dimension_key,metric_value,counter_report,lock_version,created_at,updated_at,aktif) " +
            "select i.tenant_key,j.id,date_trunc('day',u.occurred_at),'DAY'," +
            "case when u.event_type='BITSTREAM_DOWNLOAD' then 'DOWNLOAD' else 'VIEW' end,'TOTAL','ALL',count(*)::numeric," +
            "case when u.event_type='BITSTREAM_DOWNLOAD' then 'PR' else 'IR' end,0,now(),now(),true " +
            "from public.repo_usage_event u join public.repo_item i on i.id=u.item_id " +
            "join penelitiandanpengabdian.jurnal_penelitian j on j.repo_collection_id=i.collection_id and j.aktif=true " +
            "where u.occurred_at>=:f and u.occurred_at<:t and coalesce(u.user_agent_class,'')<>'BOT' " +
            "and u.event_type in ('ITEM_VIEW','BITSTREAM_DOWNLOAD') and i.document_type in ('JOURNAL_SUBMISSION','JOURNAL_ISSUE') " +
            "group by i.tenant_key,j.id,date_trunc('day',u.occurred_at),u.event_type " +
            "on conflict (tenant_key,jurnal_penelitian_id,bucket_start,bucket_type,metric_key,dimension_type,dimension_key) " +
            "do update set metric_value=excluded.metric_value,counter_report=excluded.counter_report,updated_at=now(),aktif=true";
            int changed=s.createSQLQuery(sql).setTimestamp("f",fromInclusive).setTimestamp("t",toExclusive).executeUpdate();if(own)tx.commit();return changed;
        }catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
    }
}
