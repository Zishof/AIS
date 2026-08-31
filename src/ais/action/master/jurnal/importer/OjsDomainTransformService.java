package ais.action.master.jurnal.importer;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Diskusi;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.*;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.*;

/**
 * Dependency-ordered second pass for an OJS import.
 *
 * <p>The staging pass deliberately has no dependency assumptions. This pass
 * first creates the two aggregate roots that are already native to AIS
 * (submission and issue), then resolves every other row against those roots.
 * It never executes source payloads and never opens a second Hibernate factory.</p>
 */
public final class OjsDomainTransformService {
    /**
     * Pembawa data/helper lokal milik {@link OjsDomainTransformService} untuk result. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * OjsDomainTransformService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long mappings}, {@code long
     * linkedMappings}, {@code long notApplicableMappings}, {@code long derivedMappings}, {@code long
     * submissionsCreated}, {@code long issuesCreated}, {@code long metadataCreated}, {@code long
     * emailTemplatesCreated}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see OjsDomainTransformService
     */
    public static final class Result {
        public long mappings, linkedMappings, notApplicableMappings, derivedMappings;
        public long submissionsCreated, issuesCreated, metadataCreated;
        public long emailTemplatesCreated, subscriptionsCreated, invitationsCreated;
        public long stageAssignmentsCreated, reviewAssignmentsCreated;
        public long issueRelationsCreated, workflowEventsCreated, discussionsCreated, participantsCreated;
        public long contributorsCreated, bitstreamsCreated;
        public String status = "CORE_TRANSFORMED";
    }

    @SuppressWarnings("unchecked")
    public Result transform(Long jobId, Tbmuser actor) {
        if (jobId == null || actor == null) throw new IllegalArgumentException("Job dan actor import wajib diisi.");
        Session s = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = s.beginTransaction();
            ImportJobOjs job = (ImportJobOjs) s.get(ImportJobOjs.class, jobId);
            if (job == null || !Boolean.TRUE.equals(job.getAktif())) throw new IllegalArgumentException("Job import tidak ditemukan.");
            if (Boolean.TRUE.equals(job.getDryRun())) throw new IllegalStateException("Dry-run tidak boleh menulis domain.");
            ImportSumberOjs source = (ImportSumberOjs) s.get(ImportSumberOjs.class, job.getSourceId());
            JurnalPenelitian journal = (JurnalPenelitian) s.get(JurnalPenelitian.class, job.getJurnalPenelitianId());
            if (source == null || journal == null || journal.getRepoCollectionId() == null) throw new IllegalStateException("Source/jurnal/collection import tidak lengkap.");
            RepoCollection collection = (RepoCollection) s.get(RepoCollection.class, journal.getRepoCollectionId());
            if (collection == null || !"JOURNAL".equalsIgnoreCase(collection.getTipe())) throw new IllegalStateException("Collection target bukan jurnal.");

            Query q = s.createQuery("from ImportMappingOjs where sourceId=:source and aktif=true order by sourceTable,sourcePk,id");
            q.setLong("source", source.getId());
            List<ImportMappingOjs> mappings = q.list();
            LinkedHashMap<String,Row> rows = rows(mappings);
            Result out = new Result(); out.mappings = mappings.size();
            Map<String,RepoItem> submissions = new HashMap<String,RepoItem>();
            Map<String,RepoItem> issues = new HashMap<String,RepoItem>();
            Map<String,RepoItem> publications = new HashMap<String,RepoItem>();
            Map<String,Diskusi> discussions = new HashMap<String,Diskusi>();
            Map<String,RepoBitstream> submissionFiles = new HashMap<String,RepoBitstream>();
            Map<String,Row> sourceFiles = new HashMap<String,Row>();

            /* Aggregate roots first. */
            for (Row row : rows.values()) if ("submissions".equals(row.table)) {
                RepoItem item = item(s, collection, source, row, "JOURNAL_SUBMISSION", actor, out);
                submissions.put(value(row,"submission_id"), item); row.target = item;
                applySubmission(item,row); s.update(item);
            }
            /* OJS 2.x/legacy names the submission aggregate `articles`. */
            for (Row row : rows.values()) if ("articles".equals(row.table)) {
                RepoItem item = item(s, collection, source, row, "JOURNAL_SUBMISSION", actor, out);
                submissions.put(value(row,"article_id"), item); row.target = item;
                applyLegacyArticle(item,row); s.update(item);
            }
            for (Row row : rows.values()) if ("issues".equals(row.table)) {
                RepoItem item = item(s, collection, source, row, "JOURNAL_ISSUE", actor, out);
                issues.put(value(row,"issue_id"), item); row.target = item;
                applyIssue(item,row); s.update(item);
            }

            /* Publications resolve only after all submissions exist. */
            for (Row row : rows.values()) if ("publications".equals(row.table)) {
                RepoItem item = submissions.get(value(row,"submission_id"));
                if (item != null) {
                    publications.put(value(row,"publication_id"),item); row.target=item;
                    applyPublication(item,row); s.update(item);
                }
            }
            for (Row row : rows.values()) if ("published_articles".equals(row.table)) {
                RepoItem item = submissions.get(value(row,"article_id"));
                if (item != null) {
                    publications.put(value(row,"article_id"),item); row.target=item;
                    applyLegacyPublication(item,row); s.update(item);
                }
            }
            for(Row row:rows.values())if("files".equals(row.table))sourceFiles.put(value(row,"file_id"),row);

            /* Resolve settings and all remaining table families to a real aggregate. */
            for (Row row : rows.values()) {
                if (row.target == null) {
                    RepoItem target = first(submissions.get(value(row,"submission_id")),
                        submissions.get(value(row,"article_id")), publications.get(value(row,"publication_id")),
                        publications.get(value(row,"article_id")),issues.get(value(row,"issue_id")));
                    row.target = target == null ? collection : target;
                }
                if (row.target instanceof RepoItem && row.table.endsWith("_settings"))
                    out.metadataCreated += setting(s,(RepoItem)row.target,row,actor);
            }

            /* Six consolidated journal models are populated only for their OJS source families. */
            for(Row row:rows.values()){
                if("email_templates".equals(row.table)||"email_templates_default_data".equals(row.table)){TemplateEmailJurnal x=emailTemplate(s,journal,source,row,actor,out);row.target=x;}
                else if("subscriptions".equals(row.table)){LanggananJurnal x=subscription(s,journal,collection,source,row,actor,out);row.target=x;}
                else if("invitations".equals(row.table)){UndanganPeranJurnal x=invitation(s,journal,source,row,actor,out);row.target=x;}
                else if("stage_assignments".equals(row.table)||"subeditor_submission_group".equals(row.table)){RepoItem item=submissions.get(value(row,"submission_id"));PenugasanTahapJurnal x=stageAssignment(s,journal,source,row,item,actor,out);row.target=x;}
                else if("review_assignments".equals(row.table)){RepoItem item=submissions.get(value(row,"submission_id"));if(item!=null){PenugasanReviewerJurnal x=reviewAssignment(s,journal,source,row,item,actor,out);row.target=x;}}
            }
            for(Row row:rows.values()){
                if("publications".equals(row.table)){RepoItem article=publications.get(value(row,"publication_id")),issue=issues.get(value(row,"issue_id"));if(article!=null&&issue!=null){RepoItemRelation x=issueRelation(s,issue,article,actor,out);row.target=x;}}
                else if("published_articles".equals(row.table)){RepoItem article=publications.get(value(row,"article_id")),issue=issues.get(value(row,"issue_id"));if(article!=null&&issue!=null){RepoItemRelation x=issueRelation(s,issue,article,actor,out);row.target=x;}}
                else if("review_rounds".equals(row.table)){RepoItem item=submissions.get(value(row,"submission_id"));if(item!=null){RepoWorkflowEvent x=reviewRound(s,source,row,item,actor,out);row.target=x;}}
                else if("queries".equals(row.table)){RepoItem item=submissions.get(value(row,"assoc_id"));Diskusi x=discussion(s,journal,source,row,item,actor,out);discussions.put(value(row,"query_id"),x);row.target=x;}
                else if("authors".equals(row.table)){RepoItem item=publications.get(value(row,"publication_id"));if(item!=null){RepoItemContributor x=contributor(s,source,row,item,out);row.target=x;}}
                else if("submission_files".equals(row.table)){RepoItem item=submissions.get(value(row,"submission_id"));if(item!=null){RepoBitstream x=bitstream(s,source,row,sourceFiles.get(value(row,"file_id")),item,actor,out);submissionFiles.put(value(row,"submission_file_id"),x);row.target=x;}}
            }
            for(Row row:rows.values()){
                if("query_participants".equals(row.table)){Diskusi d=discussions.get(value(row,"query_id"));if(d!=null){PesertaDiskusiJurnal x=participant(s,journal,source,row,d,actor,out);row.target=x;}}
                else if("publication_galleys".equals(row.table)){RepoBitstream b=submissionFiles.get(value(row,"submission_file_id"));if(b!=null){b.setJournalStage("PUBLICATION");b.setJournalGenre("GALLEY");b.setPrimaryFile(truth(value(row,"is_approved")));s.update(b);row.target=b;}}
            }

            for (ImportMappingOjs m : mappings) {
                if ("NOT_APPLICABLE_WITH_RATIONALE".equals(m.getDecision())) { out.notApplicableMappings++; continue; }
                if ("DERIVED".equals(m.getDecision())) { out.derivedMappings++; continue; }
                Row row = rows.get(key(m.getSourceTable(),m.getSourcePk()));
                if (row == null || row.target == null) throw new IllegalStateException("Target dependency tidak terselesaikan: "+m.getSourceTable()+"/"+m.getSourcePk());
                Long targetId=targetId(row.target);
                if (!targetId.equals(m.getTargetId())) { m.setTargetId(targetId); m.setUpdatedAt(new Date()); s.update(m); }
                out.linkedMappings++;
            }
            tx.commit(); return out;
        } catch (RuntimeException e) { if (tx != null && tx.isActive()) tx.rollback(); throw e; }
        finally { HibernateUtil.closeSessionQuietly(s); }
    }

    private static LinkedHashMap<String,Row> rows(List<ImportMappingOjs> mappings) {
        LinkedHashMap<String,Row> out=new LinkedHashMap<String,Row>();
        for(ImportMappingOjs m:mappings){String k=key(m.getSourceTable(),m.getSourcePk());Row r=out.get(k);if(r==null){r=new Row(m.getSourceTable(),m.getSourcePk());out.put(k,r);}r.fields.put(m.getSourceField(),m.getRawPayload());}
        return out;
    }

    private static RepoItem item(Session s,RepoCollection c,ImportSumberOjs source,Row row,String type,Tbmuser actor,Result out){
        long external=stableId(row.table,row.pk);String sourceClass="OJS_IMPORT:"+source.getId()+":"+row.table;
        Query q=s.createQuery("from RepoItem where collectionId=:c and sourceClass=:sc and sourceId=:sid and aktif=true");q.setLong("c",c.getId());q.setString("sc",sourceClass);q.setLong("sid",external);q.setMaxResults(1);
        RepoItem x=(RepoItem)q.uniqueResult();if(x!=null)return x;
        x=new RepoItem();x.setCollectionId(c.getId());x.setTenantKey(c.getTenantKey());x.setDocumentType(type);x.setSourceClass(sourceClass);x.setSourceId(external);x.setSourceLabel(limit(row.pk,255));x.setTitle(("JOURNAL_ISSUE".equals(type)?"Edisi OJS ":"Naskah OJS ")+external);x.setLanguage("id");x.setWorkflowStatus("DRAFT");x.setSyncStatus("IMPORTED");x.setOwnerId(actor.getUserId());x.setIsWithdrawn(Boolean.FALSE);x.setVersionNumber(1L);x.setViewCount(0L);x.setDownloadCount(0L);x.setAktif(Boolean.TRUE);x.setOlehId(actor.getUserId());s.save(x);s.flush();if("JOURNAL_ISSUE".equals(type))out.issuesCreated++;else out.submissionsCreated++;return x;
    }

    private static void applySubmission(RepoItem x,Row r){x.setLanguage(clean(value(r,"locale"),"id"));x.setSubmittedAt(date(value(r,"date_submitted")));String st=value(r,"status");if("3".equals(st))x.setWorkflowStatus("PUBLISHED");else if("4".equals(st))x.setWorkflowStatus("DECLINED");else x.setWorkflowStatus("DRAFT");}
    private static void applyLegacyArticle(RepoItem x,Row r){x.setLanguage(clean(value(r,"locale"),"id"));x.setSubmittedAt(date(value(r,"date_submitted")));String st=value(r,"status");if("3".equals(st))x.setWorkflowStatus("PUBLISHED");else if("4".equals(st))x.setWorkflowStatus("DECLINED");else x.setWorkflowStatus("DRAFT");}
    private static void applyLegacyPublication(RepoItem x,Row r){Date published=date(value(r,"date_published"));if(published==null)published=date(value(r,"date_published_original"));x.setPublishedAt(published==null?new Date(0):published);x.setIssuedAt(x.getPublishedAt());x.setWorkflowStatus("PUBLISHED");x.setSyncStatus("PUBLISHED");}
    private static void applyIssue(RepoItem x,Row r){String title="Vol. "+clean(value(r,"volume"),"-")+" No. "+clean(value(r,"number"),"-")+" ("+clean(value(r,"year"),"-")+")";x.setTitle(title);x.setSlug(clean(value(r,"url_path"),null));x.setPublishedAt(date(value(r,"date_published")));if(truth(value(r,"published"))){x.setWorkflowStatus("PUBLISHED");x.setSyncStatus("PUBLISHED");}}
    private static void applyPublication(RepoItem x,Row r){Long v=number(value(r,"version"));if(v!=null&&v.longValue()>0)x.setVersionNumber(v);String slug=clean(value(r,"url_path"),null);if(slug!=null)x.setSlug(slug);Date published=date(value(r,"date_published"));if(published!=null){x.setPublishedAt(published);x.setIssuedAt(published);x.setWorkflowStatus("PUBLISHED");x.setSyncStatus("PUBLISHED");}}

    private static long setting(Session s,RepoItem item,Row row,Tbmuser actor){String name=clean(value(row,"setting_name"),null),val=value(row,"setting_value");if(name==null||val==null)return 0;String locale=clean(value(row,"locale"),null);String field=limit("ojs."+row.table+"."+name,100);Query q=s.createQuery("from RepoItemMetadata where itemId=:i and metadataField=:f and language "+(locale==null?"is null":"=:l")+" and aktif=true");q.setLong("i",item.getId());q.setString("f",field);if(locale!=null)q.setString("l",limit(locale,10));q.setMaxResults(1);RepoItemMetadata m=(RepoItemMetadata)q.uniqueResult();if(m==null){m=new RepoItemMetadata();m.setItemId(item.getId());m.setMetadataField(field);m.setMetadataValue(val);m.setLanguage(locale==null?null:limit(locale,10));m.setPlace(0);m.setAuthority("OJS_IMPORT");m.setConfidence(100);m.setAktif(Boolean.TRUE);m.setOlehId(actor.getUserId());s.save(m);if("title".equalsIgnoreCase(name))item.setTitle(val);return 1;}if(!val.equals(m.getMetadataValue()))throw new IllegalStateException("Nilai setting staged berubah untuk target yang sudah diimport.");return 0;}

    private static TemplateEmailJurnal emailTemplate(Session s,JurnalPenelitian j,ImportSumberOjs source,Row r,Tbmuser actor,Result out){String key=clean(value(r,"email_key"),"OJS_"+stableId(r.table,r.pk));String locale=clean(value(r,"locale"),j.getDefaultLocale());Query q=s.createQuery("from TemplateEmailJurnal where jurnalPenelitianId=:j and templateKey=:k and locale=:l and versionNumber=1 and aktif=true");q.setLong("j",j.getId());q.setString("k",limit(key,160));q.setString("l",limit(locale,20));q.setMaxResults(1);TemplateEmailJurnal x=(TemplateEmailJurnal)q.uniqueResult();if(x!=null)return x;x=new TemplateEmailJurnal();base(x,j,actor);x.setTemplateKey(limit(key,160));x.setLocale(limit(locale,20));x.setSubjectTemplate(clean(value(r,"subject"),"[Imported OJS] "+key));x.setBodyTemplate(clean(value(r,"body"),"Template imported from OJS source "+source.getSourceKey()));x.setVariablePolicyJson("{\"schemaVersion\":1,\"source\":\"OJS_IMPORT\",\"variables\":[]}");x.setVersionNumber(1);s.save(x);s.flush();out.emailTemplatesCreated++;return x;}
    private static LanggananJurnal subscription(Session s,JurnalPenelitian j,RepoCollection c,ImportSumberOjs source,Row r,Tbmuser actor,Result out){String ref="OJS:"+source.getId()+":"+limit(r.pk,220);Query q=s.createQuery("from LanggananJurnal where jurnalPenelitianId=:j and externalReference=:r and aktif=true");q.setLong("j",j.getId());q.setString("r",limit(ref,255));q.setMaxResults(1);LanggananJurnal x=(LanggananJurnal)q.uniqueResult();if(x!=null)return x;Date start=date(value(r,"date_start"));if(start==null)start=new Date(0);Date end=date(value(r,"date_end"));if(end==null||!end.after(start))end=new Date(start.getTime()+365L*24L*60L*60L*1000L);x=new LanggananJurnal();base(x,j,actor);x.setCollectionId(c.getId());x.setPolicyKey(limit(clean(value(r,"type_id"),"ojs-imported"),120));x.setPolicySnapshotJson("{\"schemaVersion\":1,\"source\":\"OJS_IMPORT\",\"sourcePk\":\""+json(limit(r.pk,180))+"\"}");x.setUserId(limit("OJS:"+source.getId()+":"+clean(value(r,"user_id"),"external"),255));x.setStartsAt(start);x.setEndsAt(end);x.setStatus(truth(value(r,"status"))?"ACTIVE":"IMPORTED");x.setExternalReference(limit(ref,255));s.save(x);s.flush();out.subscriptionsCreated++;return x;}
    private static UndanganPeranJurnal invitation(Session s,JurnalPenelitian j,ImportSumberOjs source,Row r,Tbmuser actor,Result out){String token=sha256("OJS:"+source.getId()+":"+r.pk+":"+clean(value(r,"key_hash"),"missing"));Query q=s.createQuery("from UndanganPeranJurnal where tokenHash=:h and aktif=true");q.setString("h",token);q.setMaxResults(1);UndanganPeranJurnal x=(UndanganPeranJurnal)q.uniqueResult();if(x!=null)return x;x=new UndanganPeranJurnal();base(x,j,actor);x.setEmail(limit(clean(value(r,"email"),"unknown@invalid.local"),320));x.setRoleKey(limit(clean(value(r,"type"),"OJS_ROLE"),80));x.setScopeType("JOURNAL");x.setScopeKey(String.valueOf(j.getId()));x.setTokenHash(token);x.setStatus(limit(clean(value(r,"status"),"IMPORTED"),30));x.setInvitedUserId(limit(clean(value(r,"user_id"),null),255));Date expires=date(value(r,"expiry_date"));x.setExpiresAt(expires==null?new Date(System.currentTimeMillis()+24L*60L*60L*1000L):expires);s.save(x);s.flush();out.invitationsCreated++;return x;}
    private static PenugasanTahapJurnal stageAssignment(Session s,JurnalPenelitian j,ImportSumberOjs source,Row r,RepoItem item,Tbmuser actor,Result out){String user=limit("OJS:"+source.getId()+":"+clean(value(r,"user_id"),"external"),255);String stage=limit(clean(value(r,"stage_id"),"ALL"),80);Query q=s.createQuery("from PenugasanTahapJurnal where jurnalPenelitianId=:j and userId=:u and stageKey=:st and "+(item==null?"itemId is null":"itemId=:i")+" and aktif=true");q.setLong("j",j.getId());q.setString("u",user);q.setString("st",stage);if(item!=null)q.setLong("i",item.getId());q.setMaxResults(1);PenugasanTahapJurnal x=(PenugasanTahapJurnal)q.uniqueResult();if(x!=null)return x;x=new PenugasanTahapJurnal();base(x,j,actor);x.setItemId(item==null?null:item.getId());x.setUserId(user);x.setRoleKey("OJS_GROUP_"+limit(clean(value(r,"user_group_id"),"UNKNOWN"),60));x.setStageKey(stage);x.setStatus("ACTIVE");x.setStartsAt(date(value(r,"date_assigned"))==null?new Date(0):date(value(r,"date_assigned")));x.setProvenanceJson("{\"schemaVersion\":1,\"sourceId\":"+source.getId()+",\"sourcePk\":\""+json(limit(r.pk,180))+"\"}");s.save(x);s.flush();out.stageAssignmentsCreated++;return x;}
    private static PenugasanReviewerJurnal reviewAssignment(Session s,JurnalPenelitian j,ImportSumberOjs source,Row r,RepoItem item,Tbmuser actor,Result out){String reviewer=limit("OJS:"+source.getId()+":"+clean(value(r,"reviewer_id"),"external"),255);int round=intValue(value(r,"round"),1);Query q=s.createQuery("from PenugasanReviewerJurnal where itemId=:i and reviewerId=:r and roundNumber=:n and aktif=true");q.setLong("i",item.getId());q.setString("r",reviewer);q.setInteger("n",round);q.setMaxResults(1);PenugasanReviewerJurnal x=(PenugasanReviewerJurnal)q.uniqueResult();if(x!=null)return x;x=new PenugasanReviewerJurnal();base(x,j,actor);x.setItemId(item.getId());x.setReviewerId(reviewer);x.setRoundNumber(round);x.setAnonymityMode("DOUBLE_ANONYMOUS");String status=truth(value(r,"cancelled"))?"CANCELLED":truth(value(r,"declined"))?"DECLINED":date(value(r,"date_completed"))!=null?"COMPLETED":date(value(r,"date_confirmed"))!=null?"ACCEPTED":"INVITED";x.setStatus(status);x.setRecommendation(limit(clean(value(r,"recommendation"),null),80));x.setConflictJson("{\"source\":\"OJS_IMPORT\",\"declared\":\""+json(clean(value(r,"competing_interests"),""))+"\"}");x.setInvitedAt(date(value(r,"date_assigned")));x.setResponseDueAt(date(value(r,"date_response_due")));x.setReviewDueAt(date(value(r,"date_due")));x.setCompletedAt(date(value(r,"date_completed")));x.setCancelledAt(date(value(r,"date_cancelled")));s.save(x);s.flush();out.reviewAssignmentsCreated++;return x;}
    private static RepoItemRelation issueRelation(Session s,RepoItem issue,RepoItem article,Tbmuser actor,Result out){Query q=s.createQuery("from RepoItemRelation where itemId=:i and relatedItemId=:a and relationType='ISSUE_CONTAINS' and aktif=true");q.setLong("i",issue.getId());q.setLong("a",article.getId());q.setMaxResults(1);RepoItemRelation x=(RepoItemRelation)q.uniqueResult();if(x!=null)return x;x=new RepoItemRelation();x.setItemId(issue.getId());x.setRelatedItemId(article.getId());x.setRelationType("ISSUE_CONTAINS");x.setSortOrder(Integer.valueOf(0));x.setActorId(actor.getUserId());x.setCreatedAt(new Date());x.setAktif(Boolean.TRUE);s.save(x);s.flush();out.issueRelationsCreated++;return x;}
    private static RepoWorkflowEvent reviewRound(Session s,ImportSumberOjs source,Row r,RepoItem item,Tbmuser actor,Result out){String request=limit("OJS:"+source.getId()+":ROUND:"+stableId(r.table,r.pk),100);Query q=s.createQuery("from RepoWorkflowEvent where itemId=:i and requestId=:r");q.setLong("i",item.getId());q.setString("r",request);q.setMaxResults(1);RepoWorkflowEvent x=(RepoWorkflowEvent)q.uniqueResult();if(x!=null)return x;x=new RepoWorkflowEvent();x.setItemId(item.getId());x.setFromStatus(item.getWorkflowStatus());x.setToStatus(item.getWorkflowStatus());x.setAction("IMPORT_REVIEW_ROUND");x.setCommentText("Imported OJS review round "+clean(value(r,"review_round_id"),r.pk));x.setActorId(actor.getUserId());x.setActorName(actor.getUserId());x.setRequestId(request);x.setRoundNumber(intValue(value(r,"round"),1));x.setCreatedAt(date(value(r,"date_posted"))==null?new Date():date(value(r,"date_posted")));s.save(x);s.flush();out.workflowEventsCreated++;return x;}
    private static Diskusi discussion(Session s,JurnalPenelitian j,ImportSumberOjs source,Row r,RepoItem item,Tbmuser actor,Result out){String marker=limit("OJS query "+source.getId()+":"+clean(value(r,"query_id"),r.pk),255);Query q=s.createQuery("from Diskusi where jurnalPenelitianId=:j and nama=:n");q.setLong("j",j.getId());q.setString("n",marker);q.setMaxResults(1);Diskusi x=(Diskusi)q.uniqueResult();if(x!=null)return x;x=new Diskusi();x.setJurnalPenelitianId(j.getId());x.setRepoItemId(item==null?null:item.getId());x.setStageKey(limit(clean(value(r,"stage_id"),"EDITORIAL"),80));x.setVisibility("PARTICIPANTS");x.setAnonymityMode("DOUBLE_ANONYMOUS");x.setNama(marker);x.setKeterangan("Imported from OJS query provenance; source row is retained in ImportMappingOjs.");x.setTanggal(date(value(r,"date_posted"))==null?new Date():date(value(r,"date_posted")));x.setOlehId(actor.getUserId());s.save(x);s.flush();out.discussionsCreated++;return x;}
    private static PesertaDiskusiJurnal participant(Session s,JurnalPenelitian j,ImportSumberOjs source,Row r,Diskusi d,Tbmuser actor,Result out){String user=limit("OJS:"+source.getId()+":"+clean(value(r,"user_id"),"external"),255);Query q=s.createQuery("from PesertaDiskusiJurnal where diskusiId=:d and userId=:u and aktif=true");q.setLong("d",d.getId());q.setString("u",user);q.setMaxResults(1);PesertaDiskusiJurnal x=(PesertaDiskusiJurnal)q.uniqueResult();if(x!=null)return x;x=new PesertaDiskusiJurnal();base(x,j,actor);x.setDiskusiId(d.getId());x.setUserId(user);x.setParticipantRole("PARTICIPANT");x.setJoinedAt(new Date());s.save(x);s.flush();out.participantsCreated++;return x;}
    private static RepoItemContributor contributor(Session s,ImportSumberOjs source,Row r,RepoItem item,Result out){String external=clean(value(r,"author_id"),r.pk),normalized=limit(("ojs "+source.getId()+" author "+external).toLowerCase().replaceAll("[^a-z0-9]+"," ").trim(),255),email=limit(clean(value(r,"email"),""),255);Query q=s.createQuery("from RepoAuthorAuthority where tenantKey=:t and normalizedName=:n and aktif=true");q.setString("t",item.getTenantKey());q.setString("n",normalized);q.setMaxResults(1);RepoAuthorAuthority a=(RepoAuthorAuthority)q.uniqueResult();if(a==null&&!blank(email)){q=s.createQuery("from RepoAuthorAuthority where tenantKey=:t and lower(institutionalEmail)=:e and aktif=true");q.setString("t",item.getTenantKey());q.setString("e",email.toLowerCase());q.setMaxResults(1);if(q.uniqueResult()!=null)throw new IllegalStateException("Collision email authority memerlukan keputusan link/skip manual: "+email);}if(a==null){a=new RepoAuthorAuthority();a.setTenantKey(item.getTenantKey());a.setCanonicalName(limit("OJS Author "+external,255));a.setNormalizedName(normalized);a.setNameVariants("[]");a.setInstitutionalEmail(email);a.setVerified(Boolean.FALSE);a.setCreatedAt(new Date());a.setUpdatedAt(new Date());a.setAktif(Boolean.TRUE);s.save(a);s.flush();}q=s.createQuery("from RepoItemContributor where itemId=:i and authorityId=:a and contributorRole='AUTHOR' and aktif=true");q.setLong("i",item.getId());q.setLong("a",a.getId());q.setMaxResults(1);RepoItemContributor x=(RepoItemContributor)q.uniqueResult();if(x!=null)return x;x=new RepoItemContributor();x.setItemId(item.getId());x.setAuthorityId(a.getId());x.setContributorRole("AUTHOR");x.setDisplayName(a.getCanonicalName());x.setSequenceNumber(intValue(value(r,"seq"),0));x.setCorresponding(Boolean.FALSE);x.setCreatedAt(new Date());x.setAktif(Boolean.TRUE);s.save(x);s.flush();out.contributorsCreated++;return x;}
    private static RepoBitstream bitstream(Session s,ImportSumberOjs source,Row r,Row file,RepoItem item,Tbmuser actor,Result out){long external=stableId(r.table,r.pk);String sourceClass="OJS_IMPORT:"+source.getId()+":submission_files";Query q=s.createQuery("from RepoBitstream where itemId=:i and sourceClass=:s and sourceId=:x and aktif=true");q.setLong("i",item.getId());q.setString("s",sourceClass);q.setLong("x",external);q.setMaxResults(1);RepoBitstream b=(RepoBitstream)q.uniqueResult();if(b!=null)return b;String raw=file==null?null:value(file,"path"),name=raw==null?"ojs-file-"+external:raw.replace('\\','/');if(name.indexOf('/')>=0)name=name.substring(name.lastIndexOf('/')+1);name=limit(name.replaceAll("[^A-Za-z0-9._-]","_"),255);if(name.length()==0)name="ojs-file-"+external;b=new RepoBitstream();b.setItemId(item.getId());b.setNamaFile(name);b.setMimeType(limit(file==null?"application/octet-stream":clean(value(file,"mimetype"),"application/octet-stream"),100));b.setPathSistem("ojs-import://"+source.getId()+"/"+clean(value(r,"file_id"),String.valueOf(external)));b.setUkuranByte(0L);b.setChecksum(sha256((raw==null?"":raw)+"|"+r.pk));b.setBundleName("ORIGINAL");b.setDescription("OJS file manifest; content pending streaming reconciliation");b.setAccessPolicy("RESTRICTED");b.setSourceClass(sourceClass);b.setSourceId(external);b.setPrimaryFile(Boolean.FALSE);b.setFileVersion(1L);b.setJournalStage(limit(clean(value(r,"file_stage"),"SUBMISSION"),60));b.setJournalGenre(limit(clean(value(r,"genre_id"),"OTHER"),80));b.setStorageState("PENDING_CONTENT");b.setAktif(Boolean.TRUE);b.setOlehId(actor.getUserId());s.save(b);s.flush();out.bitstreamsCreated++;return b;}
    private static void base(JurnalEntityBase x,JurnalPenelitian j,Tbmuser actor){x.setTenantKey(clean(j.getTenantKey(),"default"));x.setJurnalPenelitianId(j.getId());x.setCreatedBy(actor.getUserId());x.setCreatedAt(new Date());x.setUpdatedAt(new Date());x.setAktif(Boolean.TRUE);}
    private static Long targetId(Object x){if(x instanceof RepoItem)return((RepoItem)x).getId();if(x instanceof RepoCollection)return((RepoCollection)x).getId();if(x instanceof JurnalEntityBase)return((JurnalEntityBase)x).getId();if(x instanceof RepoItemRelation)return((RepoItemRelation)x).getId();if(x instanceof RepoWorkflowEvent)return((RepoWorkflowEvent)x).getId();if(x instanceof Diskusi)return((Diskusi)x).getId();if(x instanceof RepoItemContributor)return((RepoItemContributor)x).getId();if(x instanceof RepoBitstream)return((RepoBitstream)x).getId();throw new IllegalStateException("Jenis target importer tidak didukung: "+x.getClass().getName());}

    private static String key(String t,String p){return t+'\n'+p;}private static String value(Row r,String f){String v=r.fields.get(f);return v==null?null:v.trim();}
    private static RepoItem first(RepoItem... values){for(RepoItem x:values)if(x!=null)return x;return null;}
    private static boolean truth(String v){return "1".equals(v)||"true".equalsIgnoreCase(v)||"yes".equalsIgnoreCase(v);}
    private static Long number(String v){try{return v==null?null:Long.valueOf(v.trim());}catch(Exception e){return null;}}private static int intValue(String v,int d){Long n=number(v);return n==null||n.longValue()<1||n.longValue()>Integer.MAX_VALUE?d:n.intValue();}
    private static Date date(String v){if(v==null||v.length()==0)return null;String[] formats={"yyyy-MM-dd HH:mm:ss","yyyy-MM-dd'T'HH:mm:ss","yyyy-MM-dd"};for(String f:formats)try{SimpleDateFormat x=new SimpleDateFormat(f);x.setLenient(false);return x.parse(v);}catch(ParseException ignored){}return null;}
    private static String clean(String v,String d){return v==null||v.trim().length()==0?d:v.trim();}private static boolean blank(String v){return v==null||v.trim().length()==0;}private static String limit(String v,int n){return v==null?null:(v.length()<=n?v:v.substring(0,n));}
    private static String json(String v){return v==null?"":v.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");}
    private static String sha256(String v){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(v.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format("%02x",x&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    private static long stableId(String table,String pk){try{byte[] h=MessageDigest.getInstance("SHA-256").digest((table+'\n'+pk).getBytes("UTF-8"));long x=0;for(int i=0;i<8;i++)x=(x<<8)|(h[i]&255L);return x==Long.MIN_VALUE?0L:Math.abs(x);}catch(Exception e){throw new IllegalStateException(e);}}
    /**
     * Pembawa data/helper lokal milik {@link OjsDomainTransformService} untuk row. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * OjsDomainTransformService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String table}, {@code String pk},
     * {@code Map fields}, {@code Object target}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see OjsDomainTransformService
     */
    private static final class Row{final String table,pk;final Map<String,String>fields=new HashMap<String,String>();Object target;Row(String t,String p){table=t;pk=p;}}
}
