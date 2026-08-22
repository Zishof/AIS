package ais.action.master.jurnal.importer;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAuthorizationService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.*;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/**
 * Version-aware staging importer. Source access is JDBC read-only; this class never
 * loads OjsHibernateUtil nor executes PHP/job/filter/session payloads. Every source
 * field is preserved as provenance before a domain transformer may link it.
 */
public final class OjsImportExecutionService {
    private static final int DEFAULT_BATCH = 250;
    private static final int MAX_VALUE = 1024 * 1024;
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public ImportSumberOjs registerSource(Long journalId,String tenant,String sourceKey,String displayName,
            String connectionReference,OjsImportPreflightService.Config config,Tbmuser actor) throws Exception {
        auth.requireWorkflow(actor,"manageImport");
        OjsImportPreflightService.Result p = new OjsImportPreflightService().inspect(config);
        if (p.foundTables == 0) throw new IllegalArgumentException("Tidak ada tabel OJS yang dikenali.");
        Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();
        try { if(own)tx.begin();
            auth.requireJournalScope(s,actor,journalId,null,null,false,"JOURNAL");
            JurnalPenelitian journal=(JurnalPenelitian)s.get(JurnalPenelitian.class,journalId);
            if(journal==null||!Boolean.TRUE.equals(journal.getAktif()))throw new IllegalArgumentException("Jurnal tidak ditemukan.");
            String scopedTenant=blank(journal.getTenantKey())?"default":clean(journal.getTenantKey());
            Query q=s.createQuery("from ImportSumberOjs where tenantKey=:t and sourceKey=:k and aktif=true");
            q.setString("t",scopedTenant);q.setString("k",clean(sourceKey));q.setMaxResults(1);
            ImportSumberOjs x=(ImportSumberOjs)q.uniqueResult();if(x==null){x=new ImportSumberOjs();base(x,journalId,scopedTenant,actor);x.setSourceKey(clean(sourceKey));}
            x.setDisplayName(clean(displayName));x.setConnectionReference(clean(connectionReference));x.setDialect(p.dialect);
            x.setOjsVersion(p.version);x.setSchemaSignature(p.schemaSignature);x.setStatus(p.missing.isEmpty()?"READY":"READY_WITH_GAPS");
            if(x.getId()==null)s.save(x);else s.update(x);if(own)tx.commit();return x;
        } catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
    }

    public ImportJobOjs start(Long sourceId,boolean dryRun,String idempotencyKey,
            OjsImportPreflightService.Config config,int requestedBatch,Tbmuser actor) throws Exception {
        auth.requireWorkflow(actor,"manageImport");
        final int batch=requestedBatch<1?DEFAULT_BATCH:Math.min(requestedBatch,1000);
        ImportSumberOjs source=loadSource(sourceId);auth.requireJournalScope(HibernateUtil.currentSession(),actor,source.getJurnalPenelitianId(),null,null,false,"JOURNAL");ImportJobOjs job=createJob(source,dryRun,idempotencyKey,actor);
        if(!"RUNNING".equals(job.getStatus()))return job;
        return execute(source,job,dryRun,config,batch,actor);
    }

    /** Resume a cancelled/failed job from committed table checkpoints. */
    public ImportJobOjs resume(Long jobId,OjsImportPreflightService.Config config,int requestedBatch,Tbmuser actor)throws Exception{
        auth.requireWorkflow(actor,"manageImport");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();ImportJobOjs job;ImportSumberOjs source;
        try{if(own)tx.begin();job=(ImportJobOjs)s.get(ImportJobOjs.class,jobId);if(job==null||!Boolean.TRUE.equals(job.getAktif()))throw new IllegalArgumentException("Job import tidak ditemukan.");source=(ImportSumberOjs)s.get(ImportSumberOjs.class,job.getSourceId());if(source==null)throw new IllegalArgumentException("Source import tidak ditemukan.");auth.requireJournalScope(s,actor,job.getJurnalPenelitianId(),null,null,false,"JOURNAL");if(!("FAILED".equals(job.getStatus())||"CANCELLED".equals(job.getStatus())||"CANCEL_REQUESTED".equals(job.getStatus())))throw new IllegalStateException("Job tidak berada pada status yang dapat dilanjutkan.");job.setStatus("RUNNING");job.setErrorSummary(null);job.setFinishedAt(null);job.setUpdatedAt(new Date());s.update(job);if(own)tx.commit();}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
        return execute(source,job,Boolean.TRUE.equals(job.getDryRun()),config,requestedBatch<1?DEFAULT_BATCH:Math.min(requestedBatch,1000),actor);
    }

    private ImportJobOjs execute(ImportSumberOjs source,ImportJobOjs job,boolean dryRun,OjsImportPreflightService.Config config,int batch,Tbmuser actor)throws Exception{
        Connection external=null;
        try {
            external=openReadOnly(config);
            List<String> sourceTables="LEGACY".equalsIgnoreCase(source.getOjsVersion())?OjsLegacySourceCatalog.TABLES:OjsSourceCatalog.TABLES;
            for(String table:sourceTables){
                if(isCancelled(job.getId())) break;
                importTable(external,config,source,job,table,batch,actor);
            }
            if(isCancelled(job.getId()))finish(job.getId(),"CANCELLED",null,null);
            else if(dryRun)finish(job.getId(),"STAGING_COMPLETED",null,null);
            else {OjsDomainTransformService.Result transformed=new OjsDomainTransformService().transform(job.getId(),actor);finish(job.getId(),"CORE_TRANSFORMED",null,transformed);}
        } catch(Exception e){finish(job.getId(),"FAILED",safe(e.getMessage(),2000),null);throw e;}
        finally {if(external!=null)try{external.rollback();}catch(Exception ignored){}if(external!=null)try{external.close();}catch(Exception ignored){}}
        return reloadJob(job.getId());
    }

    public void cancel(Long jobId,Tbmuser actor){auth.requireWorkflow(actor,"manageImport");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();ImportJobOjs j=(ImportJobOjs)s.get(ImportJobOjs.class,jobId);if(j==null)throw new IllegalArgumentException("Job tidak ditemukan.");auth.requireJournalScope(s,actor,j.getJurnalPenelitianId(),null,null,false,"JOURNAL");if("RUNNING".equals(j.getStatus())){j.setStatus("CANCEL_REQUESTED");j.setUpdatedAt(new Date());s.update(j);}if(own)tx.commit();}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}

    private void importTable(Connection c,OjsImportPreflightService.Config cfg,ImportSumberOjs source,
            ImportJobOjs job,String table,int batch,Tbmuser actor)throws Exception{
        TableShape shape=shape(c,cfg,table);if(shape.columns.isEmpty())return;
        long offset=completedRows(job.getId(),table);int batchNo=completedBatches(job.getId(),table);
        for(;;){if(isCancelled(job.getId()))return;String sql="select "+joinQuoted(shape.columns,source.getDialect())+" from "+qualified(cfg.schema,table,source.getDialect())+order(shape,source.getDialect())+" limit "+batch+" offset "+offset;
            Statement st=c.createStatement();st.setFetchSize(batch);st.setQueryTimeout(Math.max(1,Math.min(300,cfg.queryTimeoutSeconds)));ResultSet rs=null;List<Row> rows=new ArrayList<Row>();
            try{rs=st.executeQuery(sql);while(rs.next())rows.add(read(rs,shape));}finally{if(rs!=null)rs.close();st.close();}
            if(rows.isEmpty())break;persistBatch(source,job,table,rows,++batchNo,actor);offset+=rows.size();if(rows.size()<batch)break;
        }
    }

    private void persistBatch(ImportSumberOjs source,ImportJobOjs job,String table,List<Row> rows,int batchNo,Tbmuser actor){
        Session s=HibernateUtil.openSession();Transaction tx=null;long accepted=0,failed=0;try{tx=s.beginTransaction();
            for(Row row:rows){for(Map.Entry<String,String> f:row.values.entrySet()){Query q=s.createQuery("from ImportMappingOjs where sourceId=:s and sourceTable=:t and sourcePk=:p and sourceField=:f and aktif=true");q.setLong("s",source.getId());q.setString("t",table);q.setString("p",row.pk);q.setString("f",f.getKey());q.setMaxResults(1);ImportMappingOjs m=(ImportMappingOjs)q.uniqueResult();OjsTransformCatalog.Outcome outcome=OjsLegacySourceCatalog.TABLE_SET.contains(table)?OjsLegacySourceCatalog.outcome(table,f.getKey()):OjsTransformCatalog.outcome(table,f.getKey());String checksum=sha256(f.getValue());if(m==null){m=new ImportMappingOjs();base(m,source.getJurnalPenelitianId(),source.getTenantKey(),actor);m.setSourceId(source.getId());m.setJobId(job.getId());m.setSourceTable(table);m.setSourcePk(row.pk);m.setSourceField(f.getKey());m.setRawPayload(safe(f.getValue(),MAX_VALUE));m.setSourceChecksum(checksum);m.setDecision(outcome.disposition);m.setTargetType(outcome.targetType);m.setTargetField(outcome.targetField);s.save(m);}else{if(!checksum.equals(m.getSourceChecksum()))throw new IllegalStateException("Sumber berubah setelah staging; buat source revision baru.");if(m.getTargetType()==null||m.getTargetField()==null){m.setDecision(outcome.disposition);m.setTargetType(outcome.targetType);m.setTargetField(outcome.targetField);s.update(m);}}accepted++;}
            }
            ImportCheckpointOjs cp=new ImportCheckpointOjs();base(cp,source.getJurnalPenelitianId(),source.getTenantKey(),actor);cp.setJobId(job.getId());cp.setSourceTable(table);cp.setBatchNumber(batchNo);cp.setCursorValue(rows.get(rows.size()-1).pk);cp.setProcessedCount(Long.valueOf(rows.size()));cp.setAcceptedCount(Long.valueOf(accepted));cp.setFailedCount(Long.valueOf(failed));cp.setStatus(failed==0?"COMPLETED":"COMPLETED_WITH_ERRORS");s.save(cp);tx.commit();
        }catch(RuntimeException e){if(tx!=null&&tx.isActive())tx.rollback();throw e;}finally{HibernateUtil.closeSessionQuietly(s);}
    }

    private ImportJobOjs createJob(ImportSumberOjs source,boolean dry,String key,Tbmuser actor){String idempotency=token(key);Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();Query q=s.createQuery("from ImportJobOjs where tenantKey=:t and idempotencyKey=:k and aktif=true");q.setString("t",source.getTenantKey());q.setString("k",idempotency);q.setMaxResults(1);ImportJobOjs old=(ImportJobOjs)q.uniqueResult();if(old!=null)return old;ImportJobOjs j=new ImportJobOjs();base(j,source.getJurnalPenelitianId(),source.getTenantKey(),actor);j.setSourceId(source.getId());j.setDryRun(Boolean.valueOf(dry));j.setIdempotencyKey(idempotency);j.setStatus("RUNNING");j.setStartedAt(new Date());j.setReportJson("{\"schemaVersion\":1,\"stage\":\"RUNNING\"}");s.save(j);if(own)tx.commit();return j;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    private void finish(Long id,String status,String error,OjsDomainTransformService.Result transformed){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ImportJobOjs j=(ImportJobOjs)s.get(ImportJobOjs.class,id);if(j!=null){Number rows=(Number)s.createQuery("select sum(processedCount) from ImportCheckpointOjs where jobId=:j and aktif=true").setLong("j",id).uniqueResult();Number fields=(Number)s.createQuery("select count(*) from ImportMappingOjs where sourceId=:s and aktif=true").setLong("s",j.getSourceId()).uniqueResult();Number explicit=(Number)s.createQuery("select count(*) from ImportMappingOjs where sourceId=:s and aktif=true and targetType is not null and targetField is not null and decision is not null").setLong("s",j.getSourceId()).uniqueResult();Number tables=(Number)s.createQuery("select count(distinct sourceTable) from ImportMappingOjs where sourceId=:s and aktif=true").setLong("s",j.getSourceId()).uniqueResult();Number failures=(Number)s.createQuery("select sum(failedCount) from ImportCheckpointOjs where jobId=:j and aktif=true").setLong("j",id).uniqueResult();JSONObject report=new JSONObject();report.put("schemaVersion",1);report.put("stage",status);report.put("sourceTablesStaged",tables==null?0:tables.longValue());report.put("sourceRowsProcessed",rows==null?0:rows.longValue());report.put("sourceFieldsPreserved",fields==null?0:fields.longValue());report.put("sourceFieldsWithExplicitOutcome",explicit==null?0:explicit.longValue());report.put("failedFields",failures==null?0:failures.longValue());report.put("domainTransformStatus",transformed==null?"NOT_EXECUTED":transformed.status);report.put("domainMappingsLinked",transformed==null?0:transformed.linkedMappings);report.put("domainSubmissionsCreated",transformed==null?0:transformed.submissionsCreated);report.put("domainIssuesCreated",transformed==null?0:transformed.issuesCreated);report.put("domainMetadataCreated",transformed==null?0:transformed.metadataCreated);report.put("domainEmailTemplatesCreated",transformed==null?0:transformed.emailTemplatesCreated);report.put("domainSubscriptionsCreated",transformed==null?0:transformed.subscriptionsCreated);report.put("domainInvitationsCreated",transformed==null?0:transformed.invitationsCreated);report.put("domainStageAssignmentsCreated",transformed==null?0:transformed.stageAssignmentsCreated);report.put("domainReviewAssignmentsCreated",transformed==null?0:transformed.reviewAssignmentsCreated);report.put("domainIssueRelationsCreated",transformed==null?0:transformed.issueRelationsCreated);report.put("domainWorkflowEventsCreated",transformed==null?0:transformed.workflowEventsCreated);report.put("domainDiscussionsCreated",transformed==null?0:transformed.discussionsCreated);report.put("domainParticipantsCreated",transformed==null?0:transformed.participantsCreated);report.put("domainContributorsCreated",transformed==null?0:transformed.contributorsCreated);report.put("domainBitstreamsCreated",transformed==null?0:transformed.bitstreamsCreated);report.put("complete134Table905FieldReconciliation",false);j.setReportJson(report.toString());j.setStatus(status);j.setErrorSummary(error);j.setFinishedAt(new Date());j.setUpdatedAt(new Date());s.update(j);}tx.commit();}catch(Exception e){if(tx!=null&&tx.isActive())tx.rollback();try{ais.common.ErrorAuditUtil.record(e,"OjsImportExecutionService.finish");}catch(Exception ignored){}}finally{HibernateUtil.closeSessionQuietly(s);}}
    private boolean isCancelled(Long id){Session s=HibernateUtil.openSession();try{ImportJobOjs j=(ImportJobOjs)s.get(ImportJobOjs.class,id);return j==null||"CANCEL_REQUESTED".equals(j.getStatus())||"CANCELLED".equals(j.getStatus());}finally{HibernateUtil.closeSessionQuietly(s);}}
    private long completedRows(Long job,String table){Session s=HibernateUtil.openSession();try{Number n=(Number)s.createQuery("select sum(processedCount) from ImportCheckpointOjs where jobId=:j and sourceTable=:t and aktif=true").setLong("j",job).setString("t",table).uniqueResult();return n==null?0:n.longValue();}finally{HibernateUtil.closeSessionQuietly(s);}}
    private int completedBatches(Long job,String table){Session s=HibernateUtil.openSession();try{Number n=(Number)s.createQuery("select max(batchNumber) from ImportCheckpointOjs where jobId=:j and sourceTable=:t and aktif=true").setLong("j",job).setString("t",table).uniqueResult();return n==null?0:n.intValue();}finally{HibernateUtil.closeSessionQuietly(s);}}
    private ImportSumberOjs loadSource(Long id){Session s=HibernateUtil.currentSession();ImportSumberOjs x=(ImportSumberOjs)s.get(ImportSumberOjs.class,id);if(x==null||!Boolean.TRUE.equals(x.getAktif()))throw new IllegalArgumentException("Sumber import tidak ditemukan.");return x;}
    private ImportJobOjs reloadJob(Long id){Session s=HibernateUtil.currentSession();ImportJobOjs job=(ImportJobOjs)s.get(ImportJobOjs.class,id);if(job!=null)s.refresh(job);return job;}
    private static Connection openReadOnly(OjsImportPreflightService.Config cfg)throws Exception{Properties p=new Properties();p.setProperty("user",cfg.user);p.setProperty("password",cfg.password);Connection c=DriverManager.getConnection(cfg.jdbcUrl,p);c.setReadOnly(true);c.setAutoCommit(false);return c;}
    private static TableShape shape(Connection c,OjsImportPreflightService.Config cfg,String table)throws Exception{TableShape x=new TableShape();DatabaseMetaData m=c.getMetaData();ResultSet cols=m.getColumns(c.getCatalog(),blank(cfg.schema)?null:cfg.schema,table,"%");try{while(cols.next())x.columns.add(cols.getString("COLUMN_NAME"));}finally{cols.close();}ResultSet keys=m.getPrimaryKeys(c.getCatalog(),blank(cfg.schema)?null:cfg.schema,table);TreeMap<Short,String> ordered=new TreeMap<Short,String>();try{while(keys.next())ordered.put(Short.valueOf(keys.getShort("KEY_SEQ")),keys.getString("COLUMN_NAME"));}finally{keys.close();}x.pk.addAll(ordered.values());if(x.pk.isEmpty())x.pk.addAll(x.columns);return x;}
    private static Row read(ResultSet rs,TableShape s)throws Exception{Row r=new Row();StringBuilder pk=new StringBuilder();for(String c:s.columns){Object v=rs.getObject(c);String value=v==null?null:String.valueOf(v);r.values.put(c,value);}for(String c:s.pk){if(pk.length()>0)pk.append('|');pk.append(c).append('=').append(safe(r.values.get(c),500));}r.pk=safe(pk.toString(),500);return r;}
    private static String order(TableShape s,String d){return s.pk.isEmpty()?"":" order by "+joinQuoted(s.pk,d);}
    private static String qualified(String schema,String table,String d){return blank(schema)?quote(table,d):quote(schema,d)+"."+quote(table,d);}
    private static String joinQuoted(List<String> names,String d){StringBuilder b=new StringBuilder();for(String n:names){if(b.length()>0)b.append(',');b.append(quote(n,d));}return b.toString();}
    private static String quote(String v,String d){if(!v.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("Identifier sumber tidak valid.");return "POSTGRESQL".equals(d)?"\""+v+"\"":"`"+v+"`";}
    private static void base(JurnalEntityBase e,Long journal,String tenant,Tbmuser actor){e.setTenantKey(clean(tenant));e.setJurnalPenelitianId(journal);e.setCreatedBy(actor.getUserId());e.setCreatedAt(new Date());e.setUpdatedAt(new Date());e.setAktif(Boolean.TRUE);}
    private static String sha256(String v){try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] h=d.digest((v==null?"<NULL>":v).getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format("%02x",x&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    private static String safe(String v,int max){if(v==null)return null;return v.length()<=max?v:v.substring(0,max);}
    private static String token(String v){String x=clean(v);if(x.length()<2||x.length()>160||!x.matches("[A-Za-z0-9._:/-]+"))throw new IllegalArgumentException("Idempotency key import tidak valid.");return x;}
    private static String clean(String v){return v==null?"":v.trim();}private static boolean blank(String v){return clean(v).length()==0;}
    private static final class TableShape{final List<String> columns=new ArrayList<String>();final List<String> pk=new ArrayList<String>();}
    private static final class Row{String pk;final LinkedHashMap<String,String> values=new LinkedHashMap<String,String>();}
}
