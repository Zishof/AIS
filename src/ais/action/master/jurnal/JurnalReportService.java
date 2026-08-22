package ais.action.master.jurnal;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** Bounded journal reports using existing Repository/workflow/subscription/aggregate tables. */
public final class JurnalReportService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    public void exportCsv(Long journalId,String report,Date from,Date to,Writer writer,Tbmuser actor)throws IOException{
        auth.requireRead(actor,"laporan");Session s=HibernateUtil.currentSession();JurnalPenelitian j=journal(s,journalId);auth.requireJournalScope(s,actor,journalId,null,null,false,"JOURNAL");String kind=report==null?"":report.trim().toUpperCase();Query q;
        if("ARTICLES".equals(kind)){writer.write("id,title,status,published_at,doi,authors\n");q=s.createQuery("select i.id,i.title,i.workflowStatus,i.publishedAt,i.doi,i.authors from RepoItem i where i.collectionId=:c and i.documentType='JOURNAL_SUBMISSION' and i.aktif=true order by i.id").setLong("c",j.getRepoCollectionId());}
        else if("REVIEWS".equals(kind)){writer.write("id,item_id,reviewer_id,round,status,invited_at,completed_at,recommendation\n");q=s.createQuery("select r.id,r.itemId,r.reviewerId,r.roundNumber,r.status,r.invitedAt,r.completedAt,r.recommendation from PenugasanReviewerJurnal r where r.jurnalPenelitianId=:j and r.aktif=true order by r.id").setLong("j",journalId);}
        else if("SUBSCRIPTIONS".equals(kind)){writer.write("id,policy_key,user_id,institution_type,institution_id,starts_at,ends_at,status,external_reference\n");q=s.createQuery("select l.id,l.policyKey,l.userId,l.institutionType,l.institutionId,l.startsAt,l.endsAt,l.status,l.externalReference from LanggananJurnal l where l.jurnalPenelitianId=:j and l.aktif=true order by l.id").setLong("j",journalId);}
        else throw new IllegalArgumentException("Jenis laporan tidak didukung.");q.setFetchSize(200);int offset=0;for(;;){q.setFirstResult(offset);q.setMaxResults(200);@SuppressWarnings("unchecked")List<Object[]> rows=q.list();if(rows.isEmpty())break;for(Object[]values:rows){for(int i=0;i<values.length;i++){if(i>0)writer.write(',');writer.write(csv(values[i]));}writer.write('\n');}writer.flush();offset+=rows.size();if(rows.size()<200)break;}
    }
    @SuppressWarnings("unchecked") public JSONObject counter5(Long journalId,Date from,Date to,Tbmuser actor){auth.requireRead(actor,"statistik");if(from==null||to==null||!to.after(from))throw new IllegalArgumentException("Rentang COUNTER tidak valid.");Session s=HibernateUtil.currentSession();JurnalPenelitian journal=journal(s,journalId);auth.requireJournalScope(s,actor,journalId,null,null,false,"JOURNAL");List<Object[]> rows=s.createQuery("select bucketStart,metricKey,sum(metricValue) from AgregatPenggunaanJurnal where jurnalPenelitianId=:j and aktif=true and dimensionType='TOTAL' and bucketStart>=:f and bucketStart<:t group by bucketStart,metricKey order by bucketStart,metricKey").setLong("j",journalId).setTimestamp("f",dayFloor(from)).setTimestamp("t",dayCeiling(to)).list();try{JSONArray performance=new JSONArray();for(Object[]r:rows)performance.put(new JSONObject().put("Date",iso((Date)r[0])).put("Metric_Type","DOWNLOAD".equals(r[1])?"Total_Item_Requests":"Total_Item_Investigations").put("Count",((Number)r[2]).longValue()));JSONObject header=new JSONObject().put("Report_Name","Platform Master Report").put("Report_ID","PR").put("Release","5").put("Created",iso(new Date())).put("Institution_Name",journal.getJudul()).put("Exceptions",new JSONArray());return new JSONObject().put("Report_Header",header).put("Report_Items",performance);}catch(Exception e){throw new IllegalStateException("COUNTER report gagal.",e);}}
    private static JurnalPenelitian journal(Session s,Long id){JurnalPenelitian j=(JurnalPenelitian)s.get(JurnalPenelitian.class,id);if(j==null||!Boolean.TRUE.equals(j.getAktif())||j.getRepoCollectionId()==null)throw new IllegalArgumentException("Jurnal tidak ditemukan.");return j;}
    private static String csv(Object value){String x=value==null?"":(value instanceof Date?iso((Date)value):String.valueOf(value));if(x.matches("^[=+\\-@].*"))x="'"+x;return'"'+x.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+'"';}private static String iso(Date d){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(d);}private static Date dayFloor(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}private static Date dayCeiling(Date d){Date floor=dayFloor(d);return floor.equals(d)?d:new Date(floor.getTime()+86400000L);}
}
