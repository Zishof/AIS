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
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoWorkflowEvent;

/** Typed repository administration, reporting, import validation, and preservation checks. */
public class RepositoryAdminService {
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();

    public static class Health {
        public long collections, items, publicItems, missingOai, duplicateOai, failedSync;
        public long bitstreams, missingChecksum, pendingScan, infected, turnitinSubmitted;
        public final Map<String, Long> workflow = new LinkedHashMap<String, Long>();
    }
    public static class ImportResult {
        public int rows, validRows;
        public final List<String> errors = new ArrayList<String>();
        public boolean isValid() { return errors.isEmpty(); }
    }
    public static class FixityResult {
        public int checked, ok, missing, mismatch;
        public final List<String> errors = new ArrayList<String>();
    }

    @SuppressWarnings("unchecked")
    public List<RepoCollection> collections(Tbmuser actor) {
        requireAdmin(actor); Session session = HibernateUtil.openSession();
        try { return session.createCriteria(RepoCollection.class).add(Restrictions.eq("aktif", Boolean.TRUE))
                .addOrder(Order.asc("sortOrder")).addOrder(Order.asc("nama")).list(); }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    public Health health(Tbmuser actor) {
        requireAdmin(actor); Session session = HibernateUtil.openSession();
        try {
            Health h = new Health();
            List<RepoCollection> cs = session.createCriteria(RepoCollection.class).add(Restrictions.eq("aktif", Boolean.TRUE)).list();
            List<RepoItem> items = session.createCriteria(RepoItem.class).add(Restrictions.eq("aktif", Boolean.TRUE)).list();
            List<RepoBitstream> files = session.createCriteria(RepoBitstream.class).add(Restrictions.eq("aktif", Boolean.TRUE)).list();
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
            }
            for (Integer count : oai.values()) if (count.intValue() > 1) h.duplicateOai += count.intValue() - 1;
            for (RepoBitstream file : files) {
                if (clean(file.getChecksum()).length() == 0) h.missingChecksum++;
                if ("PENDING".equalsIgnoreCase(file.getVirusScanStatus()) || "ERROR".equalsIgnoreCase(file.getVirusScanStatus())) h.pendingScan++;
                if ("INFECTED".equalsIgnoreCase(file.getVirusScanStatus())) h.infected++;
                if (Boolean.TRUE.equals(file.getTurnitinSubmitted())) h.turnitinSubmitted++;
            }
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
            List<RepoItem> items = session.createCriteria(RepoItem.class).addOrder(Order.asc("id")).list();
            Sheet sheet = wb.createSheet("Items"); row(sheet, 0, new Object[] {"ID","OAI Identifier","Judul","Penulis","Jenis","Status","Akses","DOI","Collection ID","Updated"});
            int i = 1; for (RepoItem item : items) row(sheet, i++, new Object[] {item.getId(),item.getOaiIdentifier(),item.getTitle(),item.getAuthors(),item.getDocumentType(),item.getWorkflowStatus(),item.getAccessPolicy(),item.getDoi(),item.getCollectionId(),item.getTanggal_dirubah()});
            List<RepoWorkflowEvent> events = session.createCriteria(RepoWorkflowEvent.class).addOrder(Order.asc("id")).list();
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
            List<RepoBitstream> files = session.createCriteria(RepoBitstream.class).add(Restrictions.eq("aktif", Boolean.TRUE)).list(); FixityResult r = new FixityResult();
            for (RepoBitstream bit : files) { r.checked++; try { File f = new File(bit.getPathSistem()).getCanonicalFile();
                if (!f.isFile()) { r.missing++; r.errors.add("Berkas #"+bit.getId()+" hilang: "+bit.getNamaFile()); continue; }
                String actual = sha256(f); if (actual.equalsIgnoreCase(clean(bit.getChecksum()))) r.ok++; else { r.mismatch++; r.errors.add("Checksum #"+bit.getId()+" tidak cocok: "+bit.getNamaFile()); }
            } catch(Exception e) { r.mismatch++; r.errors.add("Berkas #"+bit.getId()+": "+e.getMessage()); } }
            return r;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private void requireAdmin(Tbmuser actor) { if (!workflow.isRepositoryAdmin(actor)) throw new SecurityException("Hak administrator repository diperlukan."); }
    private void ensureNoCycle(Session session, Long id, Long parent) { Long p=parent; int guard=0; while(p!=null&&guard++<100){if(p.equals(id))throw new IllegalArgumentException("Hierarchy koleksi membentuk siklus.");RepoCollection c=(RepoCollection)session.get(RepoCollection.class,p);if(c==null)throw new IllegalArgumentException("Induk koleksi tidak ditemukan.");p=c.getParentId();} }
    private static void validateJson(String value,String label){try{new JSONObject(jsonOrEmpty(value));}catch(Exception e){throw new IllegalArgumentException("JSON "+label+" tidak valid.");}}
    private static String jsonOrEmpty(String value){return clean(value).length()==0?"{}":clean(value);}
    private static void row(Sheet s,int n,Object[] values){Row r=s.createRow(n);for(int i=0;i<values.length;i++){Cell c=r.createCell(i);Object v=values[i];if(v instanceof Number)c.setCellValue(((Number)v).doubleValue());else if(v instanceof Date)c.setCellValue((Date)v);else c.setCellValue(v==null?"":String.valueOf(v));}}
    private static String text(Cell c){if(c==null)return "";try{c.setCellType(Cell.CELL_TYPE_STRING);return clean(c.getStringCellValue());}catch(Exception e){return "";}}
    private static boolean allBlank(Row r){for(int i=0;i<5;i++)if(text(r.getCell(i)).length()>0)return false;return true;}
    private static String sha256(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");DigestInputStream in=new DigestInputStream(new FileInputStream(f),d);try{byte[]b=new byte[16384];while(in.read(b)>=0){}}finally{in.close();}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format("%02x",b&255));return s.toString();}
    private static void rollback(Transaction tx){if(tx!=null&&tx.isActive())try{tx.rollback();}catch(Exception ignored){ais.common.ErrorAuditUtil.record(ignored,"RepositoryAdminService.rollback");}}
    private static String clean(String v){return v==null?"":v.trim();}
}
