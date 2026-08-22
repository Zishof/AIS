package ais.action.master.jurnal;

import java.util.Date;
import java.util.Locale;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoAuthorAuthority;
import ais.database.model.repository.RepoCollection;

/** Admin-only, explicitly enabled generator for disposable demo/SIT/UAT data. */
public final class JurnalDemoDataService {
    public static final String CONFIRMATION = "GENERATE-DEMO-500X100";
    public static final String DEFAULT_AUTHOR = "Prof. Dr. ASROFI RIDHO S.AG., M.SI., M.H, M.Pd, M.Psi";
    private static final int MIN_JOURNALS = 500, MAX_JOURNALS = 500;
    private static final int MIN_ARTICLES = 100, MAX_ARTICLES = 200;
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public static final class Result {
        public String key, authorName, authorReference;
        public int journalsRequested, journalsCreated, articlesPerJournal, articlesCreated, contributorsCreated;
        public long elapsedMillis;
    }

    public Result generate(int journalCount, int articlesPerJournal, Long authorId, String fallbackAuthor,
            String idempotencyKey, String confirmation, Tbmuser actor) {
        auth.requireCrud(actor, "journals", "create");
        auth.requireAdministrator(actor);
        requireEnabled();
        if (!CONFIRMATION.equals(confirmation)) throw new IllegalArgumentException("Konfirmasi generator demo tidak sesuai.");
        if (journalCount < MIN_JOURNALS || journalCount > MAX_JOURNALS)
            throw new IllegalArgumentException("Jumlah jurnal demo harus tepat 500.");
        if (articlesPerJournal < MIN_ARTICLES || articlesPerJournal > MAX_ARTICLES)
            throw new IllegalArgumentException("Artikel per jurnal harus 100 sampai 200.");
        String key = key(idempotencyKey);
        String fallback = clean(fallbackAuthor, DEFAULT_AUTHOR);
        long started = System.currentTimeMillis();
        Session session = HibernateUtil.currentSession();
        Author author = author(session, authorId, fallback, actor);
        Result out = new Result(); out.key=key; out.authorName=author.name; out.authorReference=author.reference;
        out.journalsRequested=journalCount; out.articlesPerJournal=articlesPerJournal;
        String sourceClass = "AIS_JOURNAL_DEMO:" + key;
        for (int ordinal=1; ordinal<=journalCount; ordinal++) {
            Transaction tx=session.getTransaction();
            try {
                tx.begin();
                String slug="demo-"+key+"-"+ordinal;
                Query existing=session.createQuery("from RepoCollection where kode=:k and tipe='JOURNAL' and aktif=true");
                existing.setString("k",slug); existing.setMaxResults(1);
                RepoCollection collection=(RepoCollection)existing.uniqueResult();
                if(collection==null){
                    JurnalPenelitian journal=new JurnalAdministrationService().create("demo","Jurnal Demo "+ordinal,slug,"id_ID",actor);
                    collection=(RepoCollection)session.get(RepoCollection.class,journal.getRepoCollectionId());
                    out.journalsCreated++;
                }
                SQLQuery insert=session.createSQLQuery(
                    "insert into public.repo_item (collection_id,title,abstract_text,authors,subjects,publisher,language,document_type,access_policy,sync_status,submitted_at,issued_at,published_at,workflow_status,owner_id,slug,version_number,view_count,download_count,tenant_key,featured,doi_state,source_class,source_id,source_label,is_withdrawn,aktif,olehid,tanggal_dirubah,lock_version) " +
                    "select :collectionId,'Artikel Demo '||:o1||'-'||g,'Artikel contoh untuk demonstrasi workflow jurnal AIS.',:authorName,'Pendidikan; Teknologi; Penelitian','AIS Demo Publisher','id','JOURNAL_SUBMISSION','OPEN_ACCESS',case when ((g-1)%9)=8 then 'PUBLISHED' else 'DRAFT' end,current_timestamp-(g*interval '1 day'),case when ((g-1)%9)=8 then current_timestamp-(g*interval '1 day') else null end,case when ((g-1)%9)=8 then current_timestamp-(g*interval '1 day') else null end,case ((g-1)%9) when 0 then 'DRAFT' when 1 then 'SUBMITTED' when 2 then 'SCREENING' when 3 then 'REVIEW' when 4 then 'COPYEDITING' when 5 then 'PRODUCTION' when 6 then 'PROOF' when 7 then 'SCHEDULED' else 'PUBLISHED' end,:owner,'artikel-demo-'||:key1||'-'||:o2||'-'||g,1,case when ((g-1)%9)=8 then g*10 else 0 end,case when ((g-1)%9)=8 then g else 0 end,'demo',false,case when ((g-1)%9)=8 then 'REGISTERED' else 'DRAFT' end,:sc1,:o3,:o4||':'||g,false,true,:actor,current_timestamp,0 " +
                    "from generate_series(1,:articleCount) g where not exists (select 1 from public.repo_item x where x.source_class=:sc2 and x.source_label=:o5||':'||g and x.aktif=true)");
                insert.setLong("collectionId",collection.getId()).setInteger("o1",ordinal).setInteger("o2",ordinal)
                    .setInteger("o3",ordinal).setInteger("o4",ordinal).setInteger("o5",ordinal)
                    .setString("authorName",author.name).setString("owner",author.reference).setString("key1",key)
                    .setString("sc1",sourceClass).setString("sc2",sourceClass).setString("actor",actor.getUserId())
                    .setInteger("articleCount",articlesPerJournal);
                out.articlesCreated+=insert.executeUpdate();
                session.createSQLQuery("update public.repo_item set owner_id=:owner,authors=:name where collection_id=:collectionId and source_class=:sourceClass and aktif=true and (owner_id<>:oldOwner or authors<>:oldName)")
                    .setString("owner",author.reference).setString("name",author.name).setLong("collectionId",collection.getId())
                    .setString("sourceClass",sourceClass).setString("oldOwner",author.reference).setString("oldName",author.name).executeUpdate();
                SQLQuery contributors=session.createSQLQuery(
                    "insert into public.repo_item_contributor (item_id,authority_id,contributor_role,display_name,sequence_number,corresponding,aktif,created_at) " +
                    "select i.id,:authority1,'AUTHOR',:name,0,true,true,current_timestamp from public.repo_item i " +
                    "where i.collection_id=:collectionId and i.source_class=:sourceClass and i.aktif=true and not exists " +
                    "(select 1 from public.repo_item_contributor c where c.item_id=i.id and c.authority_id=:authority2 and c.contributor_role='AUTHOR')");
                contributors.setLong("authority1",author.authorityId).setLong("authority2",author.authorityId).setString("name",author.name)
                    .setLong("collectionId",collection.getId()).setString("sourceClass",sourceClass);
                out.contributorsCreated+=contributors.executeUpdate();
                tx.commit(); session.clear();
            } catch(RuntimeException e){if(tx.isActive())tx.rollback();throw e;}
        }
        out.elapsedMillis=System.currentTimeMillis()-started;
        return out;
    }

    private static Author author(Session session,Long requestedId,String fallback,Tbmuser actor){
        Long id=requestedId==null?Long.valueOf(245L):requestedId;String name=null;boolean existingPerson=false;
        try{Object value=session.createSQLQuery("select nama from public.pegawai where id=:id").setLong("id",id).setMaxResults(1).uniqueResult();if(value!=null){name=String.valueOf(value).trim();existingPerson=name.length()>0;}}catch(Exception ignored){}
        if(name==null||name.length()==0)name=fallback;
        String normalized=normalize(name);String tenant="demo";String reference=existingPerson?"PEGAWAI:"+id:limit("DEMO-AUTHOR:"+normalized,255);Transaction tx=session.getTransaction();boolean own=!tx.isActive();
        try{if(own)tx.begin();Query q=session.createQuery("from RepoAuthorAuthority where tenantKey=:t and normalizedName=:n and aktif=true");q.setString("t",tenant).setString("n",normalized).setMaxResults(1);RepoAuthorAuthority a=(RepoAuthorAuthority)q.uniqueResult();if(a==null){a=new RepoAuthorAuthority();Date now=new Date();a.setTenantKey(tenant);a.setCanonicalName(name);a.setNormalizedName(normalized);a.setNameVariants("[]");a.setUserRefId(reference);a.setAffiliation("AIS Demo");a.setVerified(Boolean.valueOf(existingPerson));a.setAktif(Boolean.TRUE);a.setCreatedAt(now);a.setUpdatedAt(now);session.save(a);session.flush();}else if(!reference.equals(a.getUserRefId())){a.setUserRefId(reference);a.setVerified(Boolean.valueOf(existingPerson));a.setUpdatedAt(new Date());session.update(a);}if(own)tx.commit();return new Author(a.getId(),name,reference);}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}
    }
    private static void requireEnabled(){String db=System.getenv("AIS_JURNAL_DB_NAME"),flag=System.getenv("AIS_JURNAL_DEMO_GENERATOR_ENABLED");String x=db==null?"":db.toLowerCase(Locale.ENGLISH);if(!"true".equalsIgnoreCase(flag)&&!(x.contains("_sit")||x.contains("_uat")||x.contains("demo")||x.contains("fixture")))throw new SecurityException("Generator demo nonaktif. Set AIS_JURNAL_DEMO_GENERATOR_ENABLED=true pada deployment demo.");}
    private static String key(String v){String x=clean(v,"").toLowerCase(Locale.ENGLISH);if(!x.matches("[a-z0-9][a-z0-9-]{7,39}"))throw new IllegalArgumentException("Idempotency key harus 8-40 karakter huruf kecil/angka/tanda hubung.");return x;}
    private static String clean(String v,String d){return v==null||v.trim().length()==0?d:v.trim();}
    private static String normalize(String v){String x=v.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+"," ").trim();return x.length()>255?x.substring(0,255):x;}
    private static String limit(String v,int n){return v.length()<=n?v:v.substring(0,n);}
    private static final class Author{final Long authorityId;final String name,reference;Author(Long id,String n,String r){authorityId=id;name=n;reference=r;}}
}
