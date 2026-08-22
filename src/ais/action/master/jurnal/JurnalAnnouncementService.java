package ais.action.master.jurnal;

import java.util.Date;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** Journal announcements reuse RepoItem; no OJS announcement table is duplicated. */
public final class JurnalAnnouncementService {
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();
    public RepoItem publish(Long collectionId,String title,String body,Tbmuser actor){auth.requireCrud(actor,"pengumuman","update");if(blank(title)||title.length()>1000||body==null||body.length()>262144)throw new IllegalArgumentException("Pengumuman jurnal tidak valid.");Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();RepoCollection c=(RepoCollection)s.get(RepoCollection.class,collectionId);if(c==null||!Boolean.TRUE.equals(c.getAktif())||!"JOURNAL".equalsIgnoreCase(c.getTipe()))throw new IllegalArgumentException("Koleksi jurnal tidak ditemukan.");Query jq=s.createQuery("from JurnalPenelitian where repoCollectionId=:c and aktif=true");jq.setLong("c",collectionId);jq.setMaxResults(1);JurnalPenelitian j=(JurnalPenelitian)jq.uniqueResult();if(j==null)throw new IllegalArgumentException("Jurnal tidak ditemukan.");auth.requireJournalScope(s,actor,j.getId(),null,null,false,"JOURNAL");Date now=new Date();RepoItem x=new RepoItem();x.setCollectionId(collectionId);x.setTenantKey(c.getTenantKey());x.setDocumentType("JOURNAL_ANNOUNCEMENT");x.setWorkflowStatus("PUBLISHED");x.setSyncStatus("PUBLISHED");x.setTitle(title.trim());x.setAbstractText(body.trim());x.setLanguage("id");x.setOwnerId(actor.getUserId());x.setSubmittedAt(now);x.setIssuedAt(now);x.setPublishedAt(now);x.setIsWithdrawn(Boolean.FALSE);x.setAktif(Boolean.TRUE);x.setViewCount(0L);x.setDownloadCount(0L);x.setVersionNumber(1L);x.setOlehId(actor.getUserId());s.save(x);s.flush();x.setSlug("announcement-"+x.getId());x.setOaiIdentifier("oai:ais:jurnal:announcement:"+x.getId());s.update(x);if(own)tx.commit();return x;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    private static boolean blank(String v){return v==null||v.trim().length()==0;}
}
