package ais.action.master.jurnal;

import java.security.MessageDigest;
import java.util.*;
import org.hibernate.*;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.PenugasanReviewerJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.*;

/** State machine submission sampai publikasi, memakai Repository sebagai source of truth. */
public final class JurnalWorkflowService {
    public static final String DRAFT="DRAFT",SUBMITTED="SUBMITTED",SCREENING="SCREENING",IN_REVIEW="IN_REVIEW",
        REVISION_REQUIRED="REVISION_REQUIRED",ACCEPTED="ACCEPTED",COPYEDITING="COPYEDITING",PRODUCTION="PRODUCTION",
        PROOF="PROOF",PUBLICATION_READY="PUBLICATION_READY",SCHEDULED="SCHEDULED",PUBLISHED="PUBLISHED",
        REJECTED="REJECTED",WITHDRAWN="WITHDRAWN",RETRACTED="RETRACTED";
    private static final Map<String,Set<String>> TRANSITIONS=transitions();
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

    public RepoItem createDraft(final Long collectionId,final String title,final String abstractText,
            final String language,final Tbmuser actor,final String requestId){
        auth.requireCrud(actor,"submission","create");required(title,"Judul wajib diisi.");
        return write(new Work<RepoItem>(){public RepoItem run(Session s){
            JournalContext journal=journal(s,collectionId);auth.requireJournalScope(s,actor,journal.id,null,null,false,null);RepoItem item=new RepoItem();item.setCollectionId(collectionId);
            item.setTenantKey(journal.tenant);item.setDocumentType("JOURNAL_SUBMISSION");item.setWorkflowStatus(DRAFT);
            item.setSyncStatus(DRAFT);item.setTitle(clean(title));item.setAbstractText(clean(abstractText));
            item.setLanguage(blank(language)?"id":clean(language));item.setOwnerId(actor.getUserId());item.setSubmittedAt(new Date());
            item.setAktif(Boolean.TRUE);item.setViewCount(0L);item.setDownloadCount(0L);item.setVersionNumber(1L);item.setOlehId(actor.getUserId());
            s.save(item);s.flush();event(s,item,null,DRAFT,"CREATE_DRAFT",null,actor,requestId,null);return item;
        }});
    }

    public RepoItem transition(final Long itemId,final Long expectedVersion,final String target,final String action,
            final String comment,final Tbmuser actor,final String requestId){
        String capability=capability(target);if(capability!=null)auth.requireWorkflow(actor,capability);
        else auth.requireCrud(actor,"submission","update");
        return write(new Work<RepoItem>(){public RepoItem run(Session s){RepoItem item=item(s,itemId);JournalContext journal=journal(s,item.getCollectionId());boolean ownerTransition=SUBMITTED.equals(target)||WITHDRAWN.equals(target);auth.requireJournalScope(s,actor,journal.id,item.getId(),item.getOwnerId(),ownerTransition,null);
            if(expectedVersion!=null&&!expectedVersion.equals(item.getLockVersion()))throw new IllegalStateException("Naskah telah berubah; muat ulang.");
            String from=item.getWorkflowStatus();Set<String> allowed=TRANSITIONS.get(from);if(allowed==null||!allowed.contains(target))throw new IllegalStateException("Transisi "+from+" ke "+target+" tidak diizinkan.");
            if((REJECTED.equals(target)||WITHDRAWN.equals(target)||RETRACTED.equals(target))&&blank(comment))throw new IllegalArgumentException("Alasan wajib diisi.");
            item.setWorkflowStatus(target);item.setSyncStatus(target);Date now=new Date();
            if(SUBMITTED.equals(target))item.setSubmittedAt(now);if(PUBLISHED.equals(target)){item.setPublishedAt(now);item.setIssuedAt(item.getIssuedAt()==null?now:item.getIssuedAt());item.setIsWithdrawn(Boolean.FALSE);if(blank(item.getSlug()))item.setSlug(slug(item.getTitle(),item.getId()));if(blank(item.getOaiIdentifier()))item.setOaiIdentifier("oai:ais:jurnal:"+item.getId());}
            if(WITHDRAWN.equals(target)||RETRACTED.equals(target)){item.setIsWithdrawn(Boolean.TRUE);item.setWithdrawnAt(now);item.setWithdrawalReason(clean(comment));}
            item.setOlehId(actor.getUserId());s.update(item);event(s,item,from,target,clean(action),comment,actor,requestId,null);return item;}});
    }

    public PenugasanReviewerJurnal inviteReviewer(final Long itemId,final String reviewerId,final int round,
            final String anonymity,final Date responseDue,final Date reviewDue,final String formVersion,
            final Tbmuser actor,final String requestId){
        auth.requireWorkflow(actor,"assignReviewer");required(reviewerId,"Reviewer wajib diisi.");if(round<1)throw new IllegalArgumentException("Putaran review tidak valid.");
        return write(new Work<PenugasanReviewerJurnal>(){public PenugasanReviewerJurnal run(Session s){RepoItem item=item(s,itemId);JournalContext journal=journal(s,item.getCollectionId());auth.requireJournalScope(s,actor,journal.id,item.getId(),item.getOwnerId(),false,"REVIEW");
            if(reviewerId.equals(item.getOwnerId()))throw new IllegalArgumentException("Penulis tidak boleh mereview naskahnya sendiri.");
            PenugasanReviewerJurnal a=new PenugasanReviewerJurnal();base(a,journal,actor);a.setItemId(itemId);a.setReviewerId(reviewerId);a.setRoundNumber(round);a.setStatus("INVITED");
            a.setAnonymityMode(validAnonymity(anonymity));a.setInvitedAt(new Date());a.setResponseDueAt(responseDue);a.setReviewDueAt(reviewDue);a.setFormVersionKey(clean(formVersion));s.save(a);
            String from=item.getWorkflowStatus();if(SUBMITTED.equals(from)||SCREENING.equals(from)||REVISION_REQUIRED.equals(from)){item.setWorkflowStatus(IN_REVIEW);item.setSyncStatus(IN_REVIEW);s.update(item);}
            event(s,item,from,item.getWorkflowStatus(),"INVITE_REVIEWER","Reviewer assignment created",actor,requestId,Integer.valueOf(round));return a;}});
    }

    public PenugasanReviewerJurnal respondInvitation(final Long assignmentId,final boolean accept,final String reason,
            final Tbmuser actor,final String requestId){
        return write(new Work<PenugasanReviewerJurnal>(){public PenugasanReviewerJurnal run(Session s){PenugasanReviewerJurnal a=assignment(s,assignmentId,actor);
            if(!"INVITED".equals(a.getStatus()))throw new IllegalStateException("Undangan tidak lagi aktif.");if(!accept&&blank(reason))throw new IllegalArgumentException("Alasan penolakan wajib diisi.");
            a.setStatus(accept?"ACCEPTED":"DECLINED");if(accept)a.setAcceptedAt(new Date());else a.setDeclinedAt(new Date());s.update(a);
            RepoItem item=item(s,a.getItemId());event(s,item,item.getWorkflowStatus(),item.getWorkflowStatus(),accept?"ACCEPT_REVIEW":"DECLINE_REVIEW",reason,actor,requestId,a.getRoundNumber());return a;}});
    }

    public PenugasanReviewerJurnal submitReview(final Long assignmentId,final String responseJson,
            final String recommendation,final Tbmuser actor,final String requestId){
        auth.requireCrud(actor,"prosesReview","update");
        return write(new Work<PenugasanReviewerJurnal>(){public PenugasanReviewerJurnal run(Session s){PenugasanReviewerJurnal a=assignment(s,assignmentId,actor);
            if(!"ACCEPTED".equals(a.getStatus()))throw new IllegalStateException("Penugasan belum diterima atau sudah selesai.");
            if(responseJson==null||responseJson.length()>262144)throw new IllegalArgumentException("Respons review tidak valid.");
            JSONObject response=parseObject(responseJson,"Respons review bukan JSON object yang valid.");if(response.length()==0)throw new IllegalArgumentException("Respons review kosong.");required(recommendation,"Rekomendasi wajib diisi.");
            a.setResponseJson(response.toString());a.setResponseChecksum(sha256(response.toString()));a.setRecommendation(clean(recommendation));a.setCompletedAt(new Date());a.setStatus("COMPLETED");s.update(a);
            RepoItem item=item(s,a.getItemId());event(s,item,item.getWorkflowStatus(),item.getWorkflowStatus(),"SUBMIT_REVIEW",recommendation,actor,requestId,a.getRoundNumber());return a;}});
    }

    private String capability(String target){if(PUBLISHED.equals(target)||SCHEDULED.equals(target))return "publish";if(RETRACTED.equals(target))return "retract";if(ACCEPTED.equals(target)||REJECTED.equals(target)||REVISION_REQUIRED.equals(target))return "makeFinalDecision";return null;}
    private static Map<String,Set<String>> transitions(){Map<String,Set<String>> m=new HashMap<String,Set<String>>();put(m,DRAFT,SUBMITTED,WITHDRAWN);put(m,SUBMITTED,SCREENING,IN_REVIEW,REJECTED,WITHDRAWN);put(m,SCREENING,IN_REVIEW,REVISION_REQUIRED,REJECTED,WITHDRAWN);put(m,IN_REVIEW,REVISION_REQUIRED,ACCEPTED,REJECTED,WITHDRAWN);put(m,REVISION_REQUIRED,SUBMITTED,IN_REVIEW,WITHDRAWN);put(m,ACCEPTED,COPYEDITING);put(m,COPYEDITING,PRODUCTION);put(m,PRODUCTION,PROOF);put(m,PROOF,PUBLICATION_READY,PRODUCTION);put(m,PUBLICATION_READY,SCHEDULED,PUBLISHED);put(m,SCHEDULED,PUBLICATION_READY,PUBLISHED);put(m,PUBLISHED,RETRACTED);put(m,WITHDRAWN,SUBMITTED);return Collections.unmodifiableMap(m);}
    private static void put(Map<String,Set<String>> m,String from,String...to){m.put(from,Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(to))));}
    private interface Work<T>{T run(Session s);}
    private <T>T write(Work<T>w){Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();T result=w.run(s);if(own)tx.commit();return result;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}catch(Exception e){if(own&&tx.isActive())tx.rollback();throw new IllegalStateException(e);}}
    private RepoItem item(Session s,Long id){RepoItem x=(RepoItem)s.get(RepoItem.class,id);if(x==null||!Boolean.TRUE.equals(x.getAktif())||!"JOURNAL_SUBMISSION".equals(x.getDocumentType()))throw new IllegalArgumentException("Naskah tidak ditemukan.");return x;}
    private PenugasanReviewerJurnal assignment(Session s,Long id,Tbmuser actor){PenugasanReviewerJurnal a=(PenugasanReviewerJurnal)s.get(PenugasanReviewerJurnal.class,id);if(a==null||!Boolean.TRUE.equals(a.getAktif()))throw new IllegalArgumentException("Penugasan tidak ditemukan.");if(actor==null||!a.getReviewerId().equals(actor.getUserId()))throw new SecurityException("Penugasan reviewer berada di luar scope.");return a;}
    private JournalContext journal(Session s,Long collectionId){RepoCollection c=(RepoCollection)s.get(RepoCollection.class,collectionId);if(c==null||!"JOURNAL".equalsIgnoreCase(c.getTipe()))throw new IllegalArgumentException("Jurnal tidak ditemukan.");Query q=s.createQuery("from JurnalPenelitian where repoCollectionId=:id and aktif=true");q.setLong("id",collectionId);q.setMaxResults(1);JurnalPenelitian j=(JurnalPenelitian)q.uniqueResult();if(j==null)throw new IllegalStateException("Jurnal belum ditautkan ke collection.");return new JournalContext(j.getId(),blank(c.getTenantKey())?"default":c.getTenantKey());}
    private void base(PenugasanReviewerJurnal a,JournalContext j,Tbmuser actor){a.setTenantKey(j.tenant);a.setJurnalPenelitianId(j.id);a.setCreatedBy(actor.getUserId());a.setCreatedAt(new Date());a.setUpdatedAt(new Date());a.setAktif(Boolean.TRUE);}
    private void event(Session s,RepoItem item,String from,String to,String action,String comment,Tbmuser actor,String requestId,Integer round){RepoWorkflowEvent e=new RepoWorkflowEvent();e.setItemId(item.getId());e.setFromStatus(from);e.setToStatus(to);e.setAction(action);e.setCommentText(clean(comment));e.setActorId(actor.getUserId());e.setActorName(actor.getUserId());e.setRequestId(requestId);e.setRoundNumber(round);e.setCreatedAt(new Date());s.save(e);}
    private static String validAnonymity(String v){String x=clean(v).toUpperCase();if(!"DOUBLE_ANONYMOUS".equals(x)&&!"SINGLE_ANONYMOUS".equals(x)&&!"OPEN".equals(x))throw new IllegalArgumentException("Mode anonimitas tidak valid.");return x;}
    private static String slug(String title,Long id){String s=clean(title).toLowerCase().replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");return(s.length()>100?s.substring(0,100):s)+"-"+id;}
    private static String sha256(String v){try{byte[] d=MessageDigest.getInstance("SHA-256").digest(v.getBytes("UTF-8"));StringBuilder b=new StringBuilder();for(byte x:d)b.append(String.format("%02x",x&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    private static JSONObject parseObject(String value,String message){try{return new JSONObject(value);}catch(Exception e){throw new IllegalArgumentException(message,e);}}
    private static void required(String v,String m){if(blank(v))throw new IllegalArgumentException(m);}private static boolean blank(String v){return v==null||v.trim().length()==0;}private static String clean(String v){return v==null?"":v.trim();}
    private static final class JournalContext{final Long id;final String tenant;JournalContext(Long i,String t){id=i;tenant=t;}}
}
