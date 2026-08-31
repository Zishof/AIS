package ais.action.master.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoWorkflowEvent;
import ais.database.model.repository.RepoItemMetadata;
import ais.database.model.repository.RepoAuthorAuthority;
import ais.database.model.repository.RepoItemContributor;
import ais.database.model.repository.RepoIntegrationEvent;
import ais.database.model.repository.RepoUserPreference;
import ais.database.model.repository.RepoHelpFeedback;

/** Typed repository administration, reporting, import validation, and preservation checks. */
public class RepositoryAdminService {
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();
    private final RepositoryFileService fileService = new RepositoryFileService();

    /**
     * Tipe implementasi bersarang {@link Health} milik {@link RepositoryAdminService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * RepositoryAdminService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
     * diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code long collections}, {@code long
     * items}, {@code long publicItems}, {@code long missingOai}, {@code long duplicateOai}, {@code long
     * failedSync}, {@code long bitstreams}, {@code long missingChecksum}. Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     *
     * @see RepositoryAdminService
     */
    public static class Health {
        public long collections, items, publicItems, missingOai, duplicateOai, failedSync;
        public long bitstreams, missingChecksum, pendingScan, infected, turnitinSubmitted;
        public long humanViews,uniqueViews,botViews,humanDownloads,uniqueDownloads;
        public long activeSearchAlerts,alertFailures,overdueReviews,integrationFailures24h,storageFreeBytes,helpfulFeedback,unhelpfulFeedback;
        public String storagePath="",lastSync="";
        public boolean storageWritable,antivirusConfigured,analyticsSaltConfigured;
        public final List<RepoHelpFeedback> recentHelpFeedback = new ArrayList<RepoHelpFeedback>();
        public final Map<String, Long> workflow = new LinkedHashMap<String, Long>();
        public final Map<String, Integer> qualityPercent = new LinkedHashMap<String, Integer>();
        public final Map<String, Long> qualityMissing = new LinkedHashMap<String, Long>();
        public final Map<String, Long> countries = new LinkedHashMap<String, Long>();
        public final Map<String, Long> referrers = new LinkedHashMap<String, Long>();
        public final Map<String, Long> dailyTrend = new LinkedHashMap<String, Long>();
    }
    /**
     * Tipe implementasi bersarang {@link ImportResult} milik {@link RepositoryAdminService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * RepositoryAdminService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
     * diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int rows}, {@code int validRows},
     * {@code List errors}; operasi lokal: {@code isValid}(). Aturan bisnis bersama tetap berada pada kelas induk
     * atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see RepositoryAdminService
     */
    public static class ImportResult {
        public int rows, validRows;
        public final List<String> errors = new ArrayList<String>();
        public boolean isValid() { return errors.isEmpty(); }
    }
    /**
     * Tipe implementasi bersarang {@link FixityResult} milik {@link RepositoryAdminService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * RepositoryAdminService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan
     * diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int checked}, {@code int ok}, {@code
     * int missing}, {@code int mismatch}, {@code List errors}. Aturan bisnis bersama tetap berada pada kelas induk
     * atau service yang dipanggilnya.</p>
     *
     * @see RepositoryAdminService
     */
    public static class FixityResult {
        public int checked, ok, missing, mismatch;
        public final List<String> errors = new ArrayList<String>();
    }

    @SuppressWarnings("unchecked")
    public List<RepoCollection> collections(Tbmuser actor) {
        requireAdmin(actor); Session session = HibernateUtil.openSession();
        try { return session.createCriteria(RepoCollection.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("aktif", Boolean.TRUE))
                .addOrder(Order.asc("sortOrder")).addOrder(Order.asc("nama")).list(); }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }
    @SuppressWarnings("unchecked") public List<RepoAuthorAuthority> authorities(Tbmuser actor,int maximum){requireAdmin(actor);Session session=HibernateUtil.openSession();try{return session.createCriteria(RepoAuthorAuthority.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("aktif",Boolean.TRUE)).addOrder(Order.asc("canonicalName")).setMaxResults(Math.max(1,Math.min(maximum,1000))).list();}finally{HibernateUtil.closeSessionQuietly(session);}}
    @SuppressWarnings("unchecked") public void mergeAuthorities(Long sourceId,Long targetId,Tbmuser actor,String requestId){
        requireAdmin(actor);if(sourceId==null||targetId==null||sourceId.equals(targetId))throw new IllegalArgumentException("Pilih dua authority berbeda.");
        Session session=HibernateUtil.openSession();Transaction tx=null;
        try{
            tx=session.beginTransaction();RepoAuthorAuthority source=(RepoAuthorAuthority)session.get(RepoAuthorAuthority.class,sourceId),target=(RepoAuthorAuthority)session.get(RepoAuthorAuthority.class,targetId);
            if(source==null||target==null||!RepositoryTenantScope.currentKey().equals(source.getTenantKey())||!RepositoryTenantScope.currentKey().equals(target.getTenantKey()))throw new IllegalArgumentException("Authority tidak ditemukan.");
            String before="sourceId="+sourceId+"; sourceName="+clean(source.getCanonicalName())+"; targetId="+targetId+"; targetName="+clean(target.getCanonicalName());
            List<RepoItemContributor> links=session.createCriteria(RepoItemContributor.class).add(Restrictions.eq("authorityId",sourceId)).list();int moved=0,duplicates=0;
            for(RepoItemContributor link:links){RepoItemContributor duplicate=(RepoItemContributor)session.createCriteria(RepoItemContributor.class).add(Restrictions.eq("itemId",link.getItemId())).add(Restrictions.eq("authorityId",targetId)).add(Restrictions.eq("contributorRole",link.getContributorRole())).setMaxResults(1).uniqueResult();if(duplicate==null){link.setAuthorityId(targetId);session.update(link);moved++;}else{link.setAktif(Boolean.FALSE);session.update(link);duplicates++;}}
            target.setNameVariants(target.getNameVariants()+"\n"+source.getCanonicalName()+"\n"+source.getNameVariants());if(target.getOrcid().length()==0)target.setOrcid(source.getOrcid());if(target.getRorId().length()==0)target.setRorId(source.getRorId());target.setUpdatedAt(new Date());source.setAktif(Boolean.FALSE);source.setUpdatedAt(new Date());session.update(target);session.update(source);
            String after="activeAuthorityId="+targetId+"; linksMoved="+moved+"; duplicateLinksDisabled="+duplicates;
            RepoIntegrationEvent audit=new RepoIntegrationEvent();audit.setTenantKey(RepositoryTenantScope.currentKey());audit.setServiceName("AUTHORITY");audit.setActionName("MERGE");audit.setStatus("SUCCESS");audit.setActorId(actor==null?"":clean(actor.getUserId()));audit.setRequestId(clean(requestId));audit.setRequestPayload(before);audit.setResponsePayload(after);audit.setErrorMessage("");audit.setCreatedAt(new Date());session.save(audit);
            tx.commit();
        }catch(RuntimeException e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    @SuppressWarnings("unchecked")
    public Health health(Tbmuser actor) {
        requireAdmin(actor); Session session = HibernateUtil.openSession();
        try {
            Health h = new Health();
            List<RepoCollection> cs = session.createCriteria(RepoCollection.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("aktif", Boolean.TRUE)).list();
            List<RepoItem> items = session.createCriteria(RepoItem.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("aktif", Boolean.TRUE)).list();
            List<RepoBitstream> files = session.createQuery("from RepoBitstream b where b.aktif=true and b.itemId in (select i.id from RepoItem i where i.tenantKey=:tenant and i.aktif=true)")
                    .setString("tenant",RepositoryTenantScope.currentKey()).list();
            h.collections = cs.size(); h.items = items.size(); h.bitstreams = files.size();
            Map<String, Integer> oai = new LinkedHashMap<String, Integer>();
            for (RepoItem item : items) {
                String state = clean(item.getWorkflowStatus()); Long n = h.workflow.get(state);
                h.workflow.put(state, Long.valueOf(n == null ? 1L : n.longValue() + 1L));
                if (RepositoryWorkflowService.PUBLISHED.equals(state)) {
                    h.publicItems++; String id = clean(item.getOaiIdentifier());
                    if (id.length() == 0) h.missingOai++; else oai.put(id, Integer.valueOf(oai.containsKey(id) ? oai.get(id).intValue() + 1 : 1));
                }
                if ("FAILED".equalsIgnoreCase(item.getSyncStatus()) || "ERROR".equalsIgnoreCase(item.getSyncStatus())) h.failedSync++;
                if ((RepositoryWorkflowService.SUBMITTED.equals(state)||RepositoryWorkflowService.IN_REVIEW.equals(state))&&item.getSubmittedAt()!=null&&item.getSubmittedAt().before(new Date(System.currentTimeMillis()-7L*86400000L)))h.overdueReviews++;
            }
            for (Integer count : oai.values()) if (count.intValue() > 1) h.duplicateOai += count.intValue() - 1;
            for (RepoBitstream file : files) {
                if (clean(file.getChecksum()).length() == 0) h.missingChecksum++;
                if ("PENDING".equalsIgnoreCase(file.getVirusScanStatus()) || "ERROR".equalsIgnoreCase(file.getVirusScanStatus())) h.pendingScan++;
                if ("INFECTED".equalsIgnoreCase(file.getVirusScanStatus())) h.infected++;
                if (Boolean.TRUE.equals(file.getTurnitinSubmitted())) h.turnitinSubmitted++;
            }
            quality(session,h,items,files);
            Object[] usage=(Object[])session.createSQLQuery("select count(*) filter(where e.event_type='VIEW' and e.user_agent_class<>'BOT'),count(distinct e.visitor_hash) filter(where e.event_type='VIEW' and e.user_agent_class<>'BOT'),count(*) filter(where e.event_type='VIEW' and e.user_agent_class='BOT'),count(*) filter(where e.event_type='DOWNLOAD' and e.user_agent_class<>'BOT'),count(distinct e.visitor_hash) filter(where e.event_type='DOWNLOAD' and e.user_agent_class<>'BOT') from repo_usage_event e join repo_item i on i.id=e.item_id where i.tenant_key=:tenant").setString("tenant",RepositoryTenantScope.currentKey()).uniqueResult();
            if(usage!=null){h.humanViews=number(usage[0]);h.uniqueViews=number(usage[1]);h.botViews=number(usage[2]);h.humanDownloads=number(usage[3]);h.uniqueDownloads=number(usage[4]);}
            fillUsageMap(session,h.countries,"select coalesce(nullif(e.country_code,''),'Tidak diketahui'),count(*) from repo_usage_event e join repo_item i on i.id=e.item_id where i.tenant_key=:tenant and e.user_agent_class<>'BOT' group by 1 order by 2 desc limit 15");
            fillUsageMap(session,h.referrers,"select coalesce(nullif(e.referrer_host,''),'Langsung'),count(*) from repo_usage_event e join repo_item i on i.id=e.item_id where i.tenant_key=:tenant and e.user_agent_class<>'BOT' group by 1 order by 2 desc limit 15");
            fillUsageMap(session,h.dailyTrend,"select to_char(e.occurred_at,'YYYY-MM-DD'),count(*) from repo_usage_event e join repo_item i on i.id=e.item_id where i.tenant_key=:tenant and e.user_agent_class<>'BOT' and e.occurred_at>=current_timestamp-interval '30 days' group by 1 order by 1");
            Number activeAlerts=(Number)session.createCriteria(RepoUserPreference.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("preferenceType","SEARCH_ALERT")).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE))).setProjection(Projections.rowCount()).uniqueResult();h.activeSearchAlerts=activeAlerts==null?0L:activeAlerts.longValue();
            Number failedAlerts=(Number)session.createCriteria(RepoUserPreference.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("preferenceType","SEARCH_ALERT")).add(Restrictions.gt("failureCount",Integer.valueOf(0))).setProjection(Projections.rowCount()).uniqueResult();h.alertFailures=failedAlerts==null?0L:failedAlerts.longValue();
            Number integrationFailures=(Number)session.createCriteria(RepoIntegrationEvent.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.in("status",new String[]{"FAILED","ERROR"})).add(Restrictions.ge("createdAt",new Date(System.currentTimeMillis()-86400000L))).setProjection(Projections.rowCount()).uniqueResult();h.integrationFailures24h=integrationFailures==null?0L:integrationFailures.longValue();
            Number helpful=(Number)session.createCriteria(RepoHelpFeedback.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("helpful",Boolean.TRUE)).setProjection(Projections.rowCount()).uniqueResult();h.helpfulFeedback=helpful==null?0L:helpful.longValue();
            Number unhelpful=(Number)session.createCriteria(RepoHelpFeedback.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.eq("helpful",Boolean.FALSE)).setProjection(Projections.rowCount()).uniqueResult();h.unhelpfulFeedback=unhelpful==null?0L:unhelpful.longValue();
            h.recentHelpFeedback.addAll(session.createCriteria(RepoHelpFeedback.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).addOrder(Order.desc("createdAt")).setMaxResults(20).list());
            Date lastSync=(Date)session.createCriteria(RepoItem.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).setProjection(Projections.max("lastSyncAt")).uniqueResult();h.lastSync=lastSync==null?"Belum ada":new java.text.SimpleDateFormat("dd MMM yyyy HH:mm").format(lastSync);
            h.storagePath=clean(System.getProperty("ais.repository.storage",System.getenv("AIS_REPOSITORY_STORAGE")));File storage=h.storagePath.length()==0?null:new File(h.storagePath);h.storageWritable=storage!=null&&storage.exists()&&storage.isDirectory()&&storage.canWrite();h.storageFreeBytes=storage==null?0L:storage.getUsableSpace();
            String scanner=clean(System.getProperty("ais.repository.virusScanner"));h.antivirusConfigured=scanner.length()>0&&new File(scanner).isFile()&&new File(scanner).canExecute();String salt=clean(System.getProperty("ais.repository.analyticsSalt"));h.analyticsSaltConfigured=salt.length()>=24&&!"AIS-REPOSITORY".equals(salt);
            return h;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    public RepoCollection saveCollection(Long id, String kode, String nama, String description, Long parentId,
            boolean depositEnabled, String defaultLicense, String metadataJson, String workflowJson,
            String accessJson, Tbmuser actor) {
        requireAdmin(actor); if (clean(nama).length() == 0) throw new IllegalArgumentException("Nama koleksi wajib diisi.");
        validateJson(metadataJson, "profil metadata"); validateJson(workflowJson, "profil workflow"); validateJson(accessJson, "kebijakan akses");
        Session session = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction(); RepoCollection c = id == null ? new RepoCollection() : (RepoCollection) session.get(RepoCollection.class, id);
            if(id==null)c.setTenantKey(RepositoryTenantScope.currentKey());
            if(id!=null&&(c==null||!RepositoryTenantScope.currentKey().equals(c.getTenantKey())))throw new SecurityException("Koleksi bukan milik tenant aktif.");
            if (c == null) throw new IllegalArgumentException("Koleksi tidak ditemukan.");
            if (parentId != null) { if (parentId.equals(id)) throw new IllegalArgumentException("Koleksi tidak boleh menjadi induknya sendiri."); ensureNoCycle(session, id, parentId); }
            c.setKode(clean(kode)); c.setNama(clean(nama)); c.setDeskripsi(clean(description)); c.setParentId(parentId);
            c.setTipe("COLLECTION"); c.setSourceSystem("AIS"); c.setDepositEnabled(Boolean.valueOf(depositEnabled));
            c.setDefaultLicenseUri(clean(defaultLicense)); c.setMetadataProfileJson(jsonOrEmpty(metadataJson));
            c.setWorkflowProfileJson(jsonOrEmpty(workflowJson)); c.setAccessPolicyJson(jsonOrEmpty(accessJson));
            c.setAktif(Boolean.TRUE); c.setOlehId(actor.getUserId()); c.setOleh(actor.toString()); c.setTanggal_dirubah(new Date());
            if (id == null) session.save(c); else session.update(c); tx.commit(); return c;
        } catch (RuntimeException e) { rollback(tx); throw e; }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    public void exportXlsx(OutputStream out, Tbmuser actor) throws Exception {
        requireAdmin(actor); Session session = HibernateUtil.openSession(); XSSFWorkbook wb = new XSSFWorkbook();
        try {
            List<RepoItem> items = session.createCriteria(RepoItem.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).addOrder(Order.asc("id")).list();
            Sheet sheet = wb.createSheet("Items"); row(sheet, 0, new Object[] {"ID","OAI Identifier","Judul","Penulis","Jenis","Status","Akses","DOI","Collection ID","Updated"});
            int i = 1; for (RepoItem item : items) row(sheet, i++, new Object[] {item.getId(),item.getOaiIdentifier(),item.getTitle(),item.getAuthors(),item.getDocumentType(),item.getWorkflowStatus(),item.getAccessPolicy(),item.getDoi(),item.getCollectionId(),item.getTanggal_dirubah()});
            List<RepoWorkflowEvent> events = session.createQuery("from RepoWorkflowEvent e where e.itemId in (select i.id from RepoItem i where i.tenantKey=:tenant) order by e.id asc").setString("tenant",RepositoryTenantScope.currentKey()).list();
            Sheet audit = wb.createSheet("Workflow Audit"); row(audit, 0, new Object[] {"ID","Item ID","Action","From","To","Actor","Comment","Created"});
            i = 1; for (RepoWorkflowEvent e : events) row(audit, i++, new Object[] {e.getId(),e.getItemId(),e.getAction(),e.getFromStatus(),e.getToStatus(),e.getActorName(),e.getCommentText(),e.getCreatedAt()});
            for (int c=0;c<10;c++) sheet.autoSizeColumn(c); for (int c=0;c<8;c++) audit.autoSizeColumn(c);
            wb.write(out);
        } finally { try { wb.close(); } finally { HibernateUtil.closeSessionQuietly(session); } }
    }

    public ImportResult dryRunXlsx(InputStream in, Tbmuser actor) throws Exception {
        requireAdmin(actor); ImportResult result = new ImportResult(); XSSFWorkbook wb = new XSSFWorkbook(in);
        try {
            if (wb.getNumberOfSheets() == 0) { result.errors.add("Workbook tidak mempunyai sheet."); return result; }
            Sheet s = wb.getSheetAt(0); Row header = s.getRow(0);
            if (header == null || !"title".equalsIgnoreCase(text(header.getCell(0))) || !"authors".equalsIgnoreCase(text(header.getCell(1)))) {
                result.errors.add("Header wajib dimulai dengan: title, authors, collection_id, document_type, access_policy."); return result;
            }
            for (int n=1;n<=s.getLastRowNum();n++) {
                Row r=s.getRow(n); if(r==null || allBlank(r)) continue; result.rows++; List<String> rowErrors=new ArrayList<String>();
                if(text(r.getCell(0)).length()==0)rowErrors.add("title"); if(text(r.getCell(1)).length()==0)rowErrors.add("authors");
                if(text(r.getCell(2)).length()==0)rowErrors.add("collection_id"); if(text(r.getCell(4)).length()==0)rowErrors.add("access_policy");
                if(rowErrors.isEmpty())result.validRows++; else result.errors.add("Baris "+(n+1)+" tidak valid: "+rowErrors);
            }
            return result;
        } finally { wb.close(); }
    }

    @SuppressWarnings("unchecked")
    public FixityResult verifyFixity(Tbmuser actor) {
        requireAdmin(actor); Session session = HibernateUtil.openSession();
        try {
            List<RepoBitstream> files = session.createQuery("from RepoBitstream b where b.aktif=true and b.itemId in (select i.id from RepoItem i where i.tenantKey=:tenant and i.aktif=true)").setString("tenant",RepositoryTenantScope.currentKey()).list(); FixityResult r = new FixityResult();
            for (RepoBitstream bit : files) { r.checked++; try { File f = fileService.resolveManagedFile(bit.getPathSistem());
                if(f==null){r.mismatch++;r.errors.add("Path #"+bit.getId()+" berada di luar storage: "+bit.getNamaFile());continue;}
                if (!f.isFile()) { r.missing++; r.errors.add("Berkas #"+bit.getId()+" hilang: "+bit.getNamaFile()); continue; }
                String actual = sha256(f); if (actual.equalsIgnoreCase(clean(bit.getChecksum()))) r.ok++; else { r.mismatch++; r.errors.add("Checksum #"+bit.getId()+" tidak cocok: "+bit.getNamaFile()); }
            } catch(Exception e) { r.mismatch++; r.errors.add("Berkas #"+bit.getId()+": "+e.getMessage()); } }
            return r;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    public int retryFailedSync(Tbmuser actor) {
        requireAdmin(actor); Session session=HibernateUtil.openSession();Transaction tx=null;
        try{tx=session.beginTransaction();List<RepoItem> rows=session.createCriteria(RepoItem.class).add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey()))
                .add(Restrictions.in("syncStatus",new String[]{"FAILED","ERROR"})).list();
            for(RepoItem item:rows){item.setSyncStatus("PENDING");item.setSyncMessage("Manual retry queued by "+actor.getUserId());item.setOlehId(actor.getUserId());item.setTanggal_dirubah(new Date());session.update(item);}tx.commit();return rows.size();
        }catch(RuntimeException e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    public int bulkRepairMetadata(String field,Tbmuser actor){
        requireAdmin(actor);String selected=clean(field);String property,value;
        if("language".equals(selected)){property="language";value="id";}else if("documentType".equals(selected)){property="documentType";value="Other";}
        else if("accessPolicy".equals(selected)){property="accessPolicy";value="METADATA_ONLY";}else throw new IllegalArgumentException("Field bulk repair tidak diizinkan.");
        Session session=HibernateUtil.openSession();Transaction tx=null;try{tx=session.beginTransaction();List<RepoItem> rows=session.createCriteria(RepoItem.class)
                .add(Restrictions.eq("tenantKey",RepositoryTenantScope.currentKey())).add(Restrictions.or(Restrictions.isNull(property),Restrictions.eq(property,""))).list();
            for(RepoItem row:rows){if("language".equals(property))row.setLanguage(value);else if("documentType".equals(property))row.setDocumentType(value);else row.setAccessPolicy(value);row.setTanggal_dirubah(new Date());row.setOlehId(actor.getUserId());session.update(row);}tx.commit();return rows.size();
        }catch(RuntimeException e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }
    public void toggleFeatured(Long itemId,Tbmuser actor){requireAdmin(actor);Session session=HibernateUtil.openSession();Transaction tx=null;try{tx=session.beginTransaction();RepoItem item=(RepoItem)session.get(RepoItem.class,itemId);if(item==null||!RepositoryTenantScope.currentKey().equals(item.getTenantKey()))throw new IllegalArgumentException("Item tidak ditemukan.");boolean next=!Boolean.TRUE.equals(item.getFeatured());item.setFeatured(Boolean.valueOf(next));item.setFeaturedAt(next?new Date():null);item.setOlehId(actor.getUserId());item.setTanggal_dirubah(new Date());session.update(item);tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}}

    @SuppressWarnings("unchecked")
    private void quality(Session session,Health h,List<RepoItem> items,List<RepoBitstream> files){
        List<RepoItem> published=new ArrayList<RepoItem>();for(RepoItem item:items)if(Boolean.FALSE.equals(item.getIsWithdrawn())||item.getIsWithdrawn()==null)if("PUBLISHED".equals(item.getWorkflowStatus())||"SYNCED".equals(item.getSyncStatus())||"APPROVED".equals(item.getSyncStatus()))published.add(item);
        Map<Long,Boolean> primary=new LinkedHashMap<Long,Boolean>();for(RepoBitstream f:files)if(Boolean.TRUE.equals(f.getPrimaryFile()))primary.put(f.getItemId(),Boolean.TRUE);
        List<Long> programIds=new ArrayList<Long>(),orcidIds=new ArrayList<Long>();List<RepoItemMetadata> metas=session.createQuery("from RepoItemMetadata m where m.aktif=true and m.metadataField in (:fields) and m.itemId in (select i.id from RepoItem i where i.tenantKey=:tenant and i.aktif=true)").setParameterList("fields",new String[]{"repository.programStudy","repository.author.orcid"}).setString("tenant",RepositoryTenantScope.currentKey()).list();
        for(RepoItemMetadata m:metas){if("repository.programStudy".equals(m.getMetadataField())&&!programIds.contains(m.getItemId()))programIds.add(m.getItemId());if("repository.author.orcid".equals(m.getMetadataField())&&!orcidIds.contains(m.getItemId()))orcidIds.add(m.getItemId());}
        int total=published.size();qualityMetric(h,"Judul",missing(published,"title",null),total);qualityMetric(h,"Penulis terstruktur",missing(published,"authors",null),total);qualityMetric(h,"Abstrak",missing(published,"abstract",null),total);qualityMetric(h,"Kata kunci",missing(published,"subjects",null),total);qualityMetric(h,"Program studi",missing(published,"ids",programIds),total);qualityMetric(h,"Lisensi",missing(published,"license",null),total);qualityMetric(h,"ORCID",missing(published,"ids",orcidIds),total);qualityMetric(h,"File utama",missing(published,"ids",new ArrayList<Long>(primary.keySet())),total);qualityMetric(h,"DOI",missing(published,"doi",null),total);
    }
    private long missing(List<RepoItem> items,String field,List<Long> ids){long n=0;for(RepoItem i:items){boolean ok="ids".equals(field)?ids.contains(i.getId()):("title".equals(field)?clean(i.getTitle()).length()>0:"authors".equals(field)?clean(i.getAuthors()).length()>0:"abstract".equals(field)?clean(i.getAbstractText()).length()>0:"subjects".equals(field)?clean(i.getSubjects()).length()>0:"license".equals(field)?clean(i.getLicenseUri()).length()>0:clean(i.getDoi()).length()>0);if(!ok)n++;}return n;}
    private void qualityMetric(Health h,String label,long missing,int total){h.qualityMissing.put(label,Long.valueOf(missing));h.qualityPercent.put(label,Integer.valueOf(total==0?0:(int)Math.round(100.0d*(total-missing)/total)));}
    @SuppressWarnings("unchecked") private void fillUsageMap(Session session,Map<String,Long> target,String sql){List<Object[]> rows=session.createSQLQuery(sql).setString("tenant",RepositoryTenantScope.currentKey()).list();for(Object[] row:rows)target.put(String.valueOf(row[0]),Long.valueOf(number(row[1])));}
    private static long number(Object value){return value instanceof Number?((Number)value).longValue():0L;}

    private void requireAdmin(Tbmuser actor) { if (!workflow.isRepositoryAdministrator(actor)) throw new SecurityException("Hak administrator repository diperlukan."); }
    private void ensureNoCycle(Session session, Long id, Long parent) { Long p=parent; int guard=0; while(p!=null&&guard++<100){if(p.equals(id))throw new IllegalArgumentException("Hierarchy koleksi membentuk siklus.");RepoCollection c=(RepoCollection)session.get(RepoCollection.class,p);if(c==null||!RepositoryTenantScope.currentKey().equals(c.getTenantKey()))throw new IllegalArgumentException("Induk koleksi tidak ditemukan.");p=c.getParentId();} }
    private static void validateJson(String value,String label){try{new JSONObject(jsonOrEmpty(value));}catch(Exception e){throw new IllegalArgumentException("JSON "+label+" tidak valid.");}}
    private static String jsonOrEmpty(String value){return clean(value).length()==0?"{}":clean(value);}
    private static void row(Sheet s,int n,Object[] values){Row r=s.createRow(n);for(int i=0;i<values.length;i++){Cell c=r.createCell(i);Object v=values[i];if(v instanceof Number)c.setCellValue(((Number)v).doubleValue());else if(v instanceof Date)c.setCellValue((Date)v);else c.setCellValue(v==null?"":String.valueOf(v));}}
    private static String text(Cell c){if(c==null)return "";try{c.setCellType(Cell.CELL_TYPE_STRING);return clean(c.getStringCellValue());}catch(Exception e){return "";}}
    private static boolean allBlank(Row r){for(int i=0;i<5;i++)if(text(r.getCell(i)).length()>0)return false;return true;}
    private static String sha256(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");DigestInputStream in=new DigestInputStream(new FileInputStream(f),d);try{byte[]b=new byte[16384];while(in.read(b)>=0){}}finally{in.close();}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format("%02x",b&255));return s.toString();}
    private static void rollback(Transaction tx){if(tx!=null&&tx.isActive())try{tx.rollback();}catch(Exception ignored){ais.common.ErrorAuditUtil.record(ignored,"RepositoryAdminService.rollback");}}
    private static String clean(String v){return v==null?"":v.trim();}
}
