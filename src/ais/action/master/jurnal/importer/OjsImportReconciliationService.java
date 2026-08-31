package ais.action.master.jurnal.importer;

import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.ImportJobOjs;
import ais.database.model.jurnal.ImportSumberOjs;

/** Final fail-closed data/file reconciliation gate for an executed import job. */
public final class OjsImportReconciliationService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    /**
     * Pembawa data/helper lokal milik {@link OjsImportReconciliationService} untuk result. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * OjsImportReconciliationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long jobId}, {@code String status},
     * {@code long mappings}, {@code long linked}, {@code long notApplicable}, {@code long derived}, {@code long
     * failedFields}, {@code long pendingFiles}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see OjsImportReconciliationService
     */
    public static final class Result{public Long jobId;public String status;public long mappings,linked,notApplicable,derived,failedFields,pendingFiles;public final JSONArray blockers=new JSONArray();public boolean complete;}
    public Result finalizeJob(Long jobId,Tbmuser actor){
        auth.requireWorkflow(actor,"manageImport");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();
        try{if(own)tx.begin();ImportJobOjs job=(ImportJobOjs)s.get(ImportJobOjs.class,jobId);if(job==null||!Boolean.TRUE.equals(job.getAktif()))throw new IllegalArgumentException("Job import tidak ditemukan.");ImportSumberOjs source=(ImportSumberOjs)s.get(ImportSumberOjs.class,job.getSourceId());if(source==null)throw new IllegalStateException("Source import tidak ditemukan.");auth.requireJournalScope(s,actor,job.getJurnalPenelitianId(),null,null,false,"JOURNAL");if(Boolean.TRUE.equals(job.getDryRun()))throw new IllegalStateException("Dry-run tidak dapat difinalisasi sebagai import data.");if(!("CORE_TRANSFORMED".equals(job.getStatus())||"RECONCILIATION_FAILED".equals(job.getStatus())))throw new IllegalStateException("Transform domain belum siap direkonsiliasi.");
            Result r=new Result();r.jobId=jobId;r.mappings=count(s,"select count(*) from ImportMappingOjs where sourceId=:id and aktif=true",source.getId());r.notApplicable=count(s,"select count(*) from ImportMappingOjs where sourceId=:id and aktif=true and decision='NOT_APPLICABLE_WITH_RATIONALE'",source.getId());r.derived=count(s,"select count(*) from ImportMappingOjs where sourceId=:id and aktif=true and decision='DERIVED'",source.getId());r.linked=count(s,"select count(*) from ImportMappingOjs where sourceId=:id and aktif=true and targetId is not null",source.getId());r.failedFields=count(s,"select coalesce(sum(failedCount),0) from ImportCheckpointOjs where jobId=:id and aktif=true",jobId);r.pendingFiles=count(s,"select count(*) from RepoBitstream where sourceClass like :sourceClass and aktif=true and storageState<>'LINKED'",null,"sourceClass","OJS_IMPORT:"+source.getId()+":%");long unresolved=count(s,"select count(*) from ImportMappingOjs where sourceId=:id and aktif=true and decision not in ('NOT_APPLICABLE_WITH_RATIONALE','DERIVED') and targetId is null",source.getId());
            if(r.mappings==0)r.blockers.put("NO_SOURCE_ROWS");if(unresolved>0)r.blockers.put("UNRESOLVED_DOMAIN_MAPPINGS:"+unresolved);if(r.failedFields>0)r.blockers.put("FAILED_FIELDS:"+r.failedFields);if(r.pendingFiles>0)r.blockers.put("FILES_NOT_LINKED:"+r.pendingFiles);r.complete=r.blockers.length()==0;r.status=r.complete?"COMPLETED":"RECONCILIATION_FAILED";
            JSONObject report=job.getReportJson()==null?new JSONObject():new JSONObject(job.getReportJson());JSONObject reconciliation=new JSONObject().put("checkedAt",new Date().getTime()).put("sourceVersion",source.getOjsVersion()).put("mappings",r.mappings).put("linked",r.linked).put("notApplicable",r.notApplicable).put("derived",r.derived).put("failedFields",r.failedFields).put("pendingFiles",r.pendingFiles).put("blockers",r.blockers).put("complete",r.complete);report.put("reconciliation",reconciliation).put("complete134Table905FieldReconciliation",r.complete&&"3.5.0-5".equals(source.getOjsVersion()));job.setReportJson(report.toString());job.setStatus(r.status);job.setErrorSummary(r.complete?null:r.blockers.toString());job.setFinishedAt(new Date());job.setUpdatedAt(new Date());s.update(job);if(own)tx.commit();return r;
        }catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalStateException("Rekonsiliasi import gagal.",e);}
    }
    private static long count(Session s,String hql,Long id){return count(s,hql,id,"id",null);}private static long count(Session s,String hql,Long id,String parameter,String text){org.hibernate.Query q=s.createQuery(hql);if(id!=null)q.setLong(parameter,id);else q.setString(parameter,text);Object n=q.uniqueResult();return n==null?0:((Number)n).longValue();}
}
