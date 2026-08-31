package ais.action.master.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoWorkflowEvent;

/** Secure bitstream storage outside the web root. */
public class RepositoryFileService {
    public static final long DEFAULT_MAX_BYTES = 100L * 1024L * 1024L;
    private static final Set<String> EXTENSIONS = new HashSet<String>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
            "txt", "csv", "rtf", "jpg", "jpeg", "png", "tif", "tiff", "zip"));
    private final RepositoryWorkflowService workflow = new RepositoryWorkflowService();

    public RepoBitstream store(Long itemId, String originalName, String declaredMime, long declaredSize,
            InputStream content, boolean primary, String accessPolicy, String description, Tbmuser actor,
            String requestId) throws Exception {
        requireLogin(actor);
        if (itemId == null || content == null) throw new IllegalArgumentException("Item dan isi berkas wajib tersedia.");
        // Authorize before creating directories, invoking scanners, or consuming the upload.
        // The item is checked again inside the write transaction to close state-change races.
        authorizeEditableItem(itemId, actor);
        final String safeName = safeFileName(originalName);
        final String extension = extension(safeName);
        if (!EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Jenis ekstensi berkas tidak diizinkan.");
        long maximum = maxBytes();
        if (declaredSize > maximum) throw new IllegalArgumentException("Ukuran berkas melebihi batas " + maximum + " byte.");

        File root = storageRoot();
        File itemDir = canonicalChild(root, String.valueOf(itemId));
        if (!itemDir.exists() && !itemDir.mkdirs()) throw new IllegalStateException("Folder penyimpanan tidak dapat dibuat.");
        File temporary = canonicalChild(itemDir, ".upload-" + UUID.randomUUID().toString() + ".tmp");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] header = new byte[16];
        int headerLength = 0;
        long total = 0L;
        InputStream in = new BufferedInputStream(content);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(temporary));
        try {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) continue;
                total += read;
                if (total > maximum) throw new IllegalArgumentException("Ukuran berkas melebihi batas.");
                if (headerLength < header.length) {
                    int copy = Math.min(read, header.length - headerLength);
                    System.arraycopy(buffer, 0, header, headerLength, copy);
                    headerLength += copy;
                }
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            temporary.delete();
            throw e;
        } finally {
            try { out.close(); } finally { in.close(); }
        }
        if (total == 0L) { temporary.delete(); throw new IllegalArgumentException("Berkas kosong tidak dapat disimpan."); }
        boolean signatureValid = validSignature(extension, header, headerLength);
        if (!signatureValid) { temporary.delete(); throw new IllegalArgumentException("Signature berkas tidak sesuai dengan ekstensi."); }
        final String checksum = hex(digest.digest());
        final long actualSize = total;
        final String mime = normalizedMime(extension, declaredMime);
        final ScanResult scan = scan(temporary);
        if ("INFECTED".equals(scan.status)) {
            quarantine(root, temporary, safeName);
            throw new SecurityException("Berkas terindikasi malware dan dipindahkan ke karantina.");
        }

        File finalFile = canonicalChild(itemDir, UUID.randomUUID().toString() + "-" + safeName);
        if (!temporary.renameTo(finalFile)) {
            copy(temporary, finalFile);
            temporary.delete();
        }
        final File stored = finalFile;
        final String extractedText = extractText(stored, extension);
        Session session = HibernateUtil.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            RepoItem item = requiredEditableItem(session, itemId, actor);
            RepoBitstream duplicate = (RepoBitstream) session.createCriteria(RepoBitstream.class)
                    .add(Restrictions.eq("itemId", itemId)).add(Restrictions.eq("checksum", checksum))
                    .add(Restrictions.eq("aktif", Boolean.TRUE)).setMaxResults(1).uniqueResult();
            if (duplicate != null) {
                transaction.rollback(); stored.delete(); return duplicate;
            }
            if (primary) {
                @SuppressWarnings("unchecked")
                List<RepoBitstream> oldPrimary = session.createCriteria(RepoBitstream.class)
                        .add(Restrictions.eq("itemId", itemId)).add(Restrictions.eq("aktif", Boolean.TRUE))
                        .add(Restrictions.eq("primaryFile", Boolean.TRUE)).list();
                for (RepoBitstream old : oldPrimary) { old.setPrimaryFile(Boolean.FALSE); session.update(old); }
            }
            Number maxVersion = (Number) session.createCriteria(RepoBitstream.class)
                    .add(Restrictions.eq("itemId", itemId)).add(Restrictions.eq("namaFile", safeName))
                    .setProjection(Projections.max("fileVersion")).uniqueResult();
            RepoBitstream bitstream = new RepoBitstream();
            bitstream.setItemId(itemId); bitstream.setNamaFile(safeName); bitstream.setMimeType(mime);
            bitstream.setPathSistem(stored.getCanonicalPath()); bitstream.setUkuranByte(Long.valueOf(actualSize));
            bitstream.setChecksum(checksum); bitstream.setBundleName("ORIGINAL");
            bitstream.setDescription(clean(description)); bitstream.setAccessPolicy(normalizeAccess(accessPolicy));
            bitstream.setPrimaryFile(Boolean.valueOf(primary)); bitstream.setSignatureValid(Boolean.TRUE);
            bitstream.setVirusScanStatus(scan.status); bitstream.setVirusScannedAt(scan.scannedAt);
            bitstream.setFileVersion(Long.valueOf(maxVersion == null ? 1L : maxVersion.longValue() + 1L));
            bitstream.setAktif(Boolean.TRUE); bitstream.setOlehId(actor.getUserId()); bitstream.setOleh(actor.toString());
            session.save(bitstream); session.flush();
            if (extractedText.length() > 0) { item.setExtractedText(extractedText); session.update(item); }
            workflowEvent(session, itemId, item.getWorkflowStatus(), "FILE_UPLOAD", safeName, actor, requestId);
            transaction.commit();
            return bitstream;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) try { transaction.rollback(); } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "RepositoryFileService.store.rollback");
            }
            stored.delete();
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    public void remove(Long bitstreamId, Tbmuser actor, String requestId) throws Exception {
        requireLogin(actor);
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        File source = null;
        File trashed = null;
        boolean movedToTrash = false;
        try {
            tx = session.beginTransaction();
            RepoBitstream file = (RepoBitstream) session.get(RepoBitstream.class, bitstreamId);
            if (file == null || !Boolean.TRUE.equals(file.getAktif())) throw new IllegalArgumentException("Berkas tidak ditemukan.");
            RepoItem item = requiredEditableItem(session, file.getItemId(), actor);
            source = resolveManagedFile(file.getPathSistem());
            if(source==null)throw new SecurityException("Path berkas berada di luar storage Repository.");
            if (source.exists() && source.isFile()) {
                File trash = canonicalChild(storageRoot(), ".trash" + File.separator + file.getItemId());
                if (!trash.exists()) trash.mkdirs();
                trashed = canonicalChild(trash, System.currentTimeMillis() + "-" + safeFileName(file.getNamaFile()));
                if (!source.renameTo(trashed)) { copy(source, trashed); if (!source.delete()) throw new IllegalStateException("Berkas sumber tidak dapat dipindahkan ke trash."); }
                movedToTrash = true;
            }
            file.setAktif(Boolean.FALSE); file.setPrimaryFile(Boolean.FALSE); session.update(file);
            workflowEvent(session, item.getId(), item.getWorkflowStatus(), "FILE_REMOVE", file.getNamaFile(), actor, requestId);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) try { tx.rollback(); } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "RepositoryFileService.remove.rollback");
            }
            if (movedToTrash && trashed != null && source != null && trashed.exists() && !source.exists()) {
                try {
                    File parent = source.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    if (!trashed.renameTo(source)) { copy(trashed, source); if (!trashed.delete()) throw new IllegalStateException("Trash tidak dapat dibersihkan setelah pemulihan."); }
                } catch (Exception restoreFailure) {
                    ais.common.ErrorAuditUtil.record(restoreFailure, "RepositoryFileService.remove.restore");
                }
            }
            throw e;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    public List<RepoBitstream> list(Long itemId, Tbmuser actor) {
        requireLogin(actor);
        Session session = HibernateUtil.openSession();
        try {
            requiredVisibleWorkspaceItem(session, itemId, actor);
            return session.createCriteria(RepoBitstream.class).add(Restrictions.eq("itemId", itemId))
                    .add(Restrictions.eq("aktif", Boolean.TRUE)).addOrder(Order.desc("primaryFile"))
                    .addOrder(Order.asc("id")).list();
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private RepoItem requiredEditableItem(Session session, Long id, Tbmuser actor) {
        RepoItem item = requiredVisibleWorkspaceItem(session, id, actor);
        if (!actor.getUserId().equals(item.getOwnerId()) && !workflow.isRepositoryManager(actor))
            throw new SecurityException("Hanya pemilik deposit atau administrator yang dapat mengubah berkas.");
        String state = item.getWorkflowStatus();
        if (!RepositoryWorkflowService.DRAFT.equals(state) && !RepositoryWorkflowService.REVISION_REQUIRED.equals(state))
            throw new IllegalStateException("Berkas hanya dapat diubah pada draft atau revisi.");
        return item;
    }

    private void authorizeEditableItem(Long itemId, Tbmuser actor) {
        Session session = HibernateUtil.openSession();
        try { requiredEditableItem(session, itemId, actor); }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private RepoItem requiredVisibleWorkspaceItem(Session session, Long id, Tbmuser actor) {
        RepoItem item = (RepoItem) session.get(RepoItem.class, id);
        if (item == null || !RepositoryTenantScope.currentKey().equals(item.getTenantKey()) || !Boolean.TRUE.equals(item.getAktif())) throw new IllegalArgumentException("Item tidak ditemukan.");
        if (!actor.getUserId().equals(item.getOwnerId()) && !workflow.isRepositoryAdmin(actor)
                && !workflow.isRepositoryManager(actor))
            throw new SecurityException("Item bukan milik pengguna aktif.");
        return item;
    }

    private ScanResult scan(File file) {
        ScanResult result = new ScanResult();
        result.status = "PENDING";
        String executable = clean(System.getProperty("ais.repository.virusScanner"));
        if (executable.length() == 0) return result;
        Process process=null;InputStream output=null;
        try {
            process = new ProcessBuilder(executable, file.getCanonicalPath()).redirectErrorStream(true).start();
            output=process.getInputStream();final InputStream scannerOutput=output;
            Thread drainer=new Thread(new Runnable(){public void run(){try{byte[] drain=new byte[4096];while(scannerOutput.read(drain)>=0){}}catch(Exception ignored){}}},"repository-virus-scanner-output");drainer.setDaemon(true);drainer.start();
            boolean finished=process.waitFor(scannerTimeoutSeconds(),TimeUnit.SECONDS);
            if(!finished){process.destroy();if(!process.waitFor(2L,TimeUnit.SECONDS))process.destroyForcibly();result.status="ERROR";result.scannedAt=new Date();ais.common.ErrorAuditUtil.record(new IllegalStateException("Antivirus melewati batas waktu."),"RepositoryFileService.scan.timeout");return result;}
            int code = process.exitValue();
            result.scannedAt = new Date();
            result.status = code == 0 ? "CLEAN" : (code == 1 ? "INFECTED" : "ERROR");
        } catch (Exception e) {
            result.status = "ERROR"; result.scannedAt = new Date();
            ais.common.ErrorAuditUtil.record(e, "RepositoryFileService.scan");
        }finally{if(output!=null)try{output.close();}catch(Exception ignored){}if(process!=null&&process.isAlive())process.destroyForcibly();}
        return result;
    }

    private int scannerTimeoutSeconds(){try{int value=Integer.parseInt(System.getProperty("ais.repository.virusScannerTimeoutSeconds","120"));return Math.max(10,Math.min(value,900));}catch(Exception e){return 120;}}

    private void workflowEvent(Session session, Long itemId, String state, String action, String comment,
            Tbmuser actor, String requestId) {
        RepoWorkflowEvent event = new RepoWorkflowEvent();
        event.setItemId(itemId); event.setFromStatus(state); event.setToStatus(state); event.setAction(action);
        event.setCommentText(comment); event.setActorId(actor.getUserId()); event.setActorName(actor.toString());
        event.setRequestId(requestId); event.setCreatedAt(new Date()); session.save(event);
    }

    private long maxBytes() {
        try {
            long configured = Long.parseLong(System.getProperty("ais.repository.maxUploadBytes", String.valueOf(DEFAULT_MAX_BYTES)));
            return configured > 0L ? Math.min(configured, 2L * 1024L * 1024L * 1024L) : DEFAULT_MAX_BYTES;
        }
        catch (Exception e) { return DEFAULT_MAX_BYTES; }
    }

    public File storageRoot() throws Exception {
        String configured = clean(System.getProperty("ais.repository.storage"));
        if (configured.length() == 0) configured = clean(System.getenv("AIS_REPOSITORY_STORAGE"));
        if (configured.length() == 0) configured = File.separatorChar == '\\' ? "C:\\opt\\AIS\\repository-files" : "/opt/AIS/repository-files";
        File root = new File(configured).getCanonicalFile();
        if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("Root repository tidak dapat dibuat.");
        if (!root.isDirectory()) throw new IllegalStateException("Root repository bukan direktori.");
        return root;
    }

    /** Resolve hanya berkas yang benar-benar berada di bawah storage Repository. */
    public File resolveManagedFile(String storedPath)throws Exception{String value=clean(storedPath);if(value.length()==0)return null;File root=storageRoot().getCanonicalFile();File file=new File(value).getCanonicalFile();String prefix=root.getPath()+File.separator;if(!file.getPath().startsWith(prefix))return null;return file;}

    private File canonicalChild(File root, String relative) throws Exception {
        File base = root.getCanonicalFile();
        File child = new File(base, relative).getCanonicalFile();
        String prefix = base.getPath() + File.separator;
        if (!child.getPath().startsWith(prefix)) throw new SecurityException("Path berkas keluar dari root repository.");
        return child;
    }

    private void quarantine(File root, File file, String name) throws Exception {
        File dir = canonicalChild(root, ".quarantine"); if (!dir.exists()) dir.mkdirs();
        File target = canonicalChild(dir, System.currentTimeMillis() + "-" + safeFileName(name));
        if (!file.renameTo(target)) { copy(file, target); file.delete(); }
    }

    private static void copy(File source, File target) throws Exception {
        InputStream in = new BufferedInputStream(new FileInputStream(source));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        try { byte[] buffer = new byte[16384]; int n; while ((n = in.read(buffer)) >= 0) if (n > 0) out.write(buffer, 0, n); }
        finally { try { out.close(); } finally { in.close(); } }
    }

    private static boolean validSignature(String ext, byte[] b, int n) {
        if ("pdf".equals(ext)) return starts(b,n,new int[]{0x25,0x50,0x44,0x46,0x2d});
        if (Arrays.asList("docx","xlsx","pptx","odt","ods","odp","zip").contains(ext)) return starts(b,n,new int[]{0x50,0x4b});
        if (Arrays.asList("doc","xls","ppt").contains(ext)) return starts(b,n,new int[]{0xd0,0xcf,0x11,0xe0});
        if ("png".equals(ext)) return starts(b,n,new int[]{0x89,0x50,0x4e,0x47});
        if ("jpg".equals(ext)||"jpeg".equals(ext)) return starts(b,n,new int[]{0xff,0xd8,0xff});
        if ("tif".equals(ext)||"tiff".equals(ext)) return starts(b,n,new int[]{0x49,0x49,0x2a,0x00})||starts(b,n,new int[]{0x4d,0x4d,0x00,0x2a});
        if (Arrays.asList("txt","csv","rtf").contains(ext)) { for(int i=0;i<n;i++) if(b[i]==0) return false; return true; }
        return false;
    }
    private static boolean starts(byte[] b,int n,int[] sig){if(n<sig.length)return false;for(int i=0;i<sig.length;i++)if((b[i]&255)!=sig[i])return false;return true;}
    private static String normalizedMime(String ext,String declared){
        if("pdf".equals(ext))return "application/pdf";if("png".equals(ext))return "image/png";if("jpg".equals(ext)||"jpeg".equals(ext))return "image/jpeg";if("tif".equals(ext)||"tiff".equals(ext))return "image/tiff";
        if("txt".equals(ext))return "text/plain";if("csv".equals(ext))return "text/csv";if("rtf".equals(ext))return "application/rtf";if("zip".equals(ext))return "application/zip";
        if("doc".equals(ext))return "application/msword";if("docx".equals(ext))return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";if("xls".equals(ext))return "application/vnd.ms-excel";if("xlsx".equals(ext))return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";if("ppt".equals(ext))return "application/vnd.ms-powerpoint";if("pptx".equals(ext))return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if("odt".equals(ext))return "application/vnd.oasis.opendocument.text";if("ods".equals(ext))return "application/vnd.oasis.opendocument.spreadsheet";if("odp".equals(ext))return "application/vnd.oasis.opendocument.presentation";return "application/octet-stream";
    }
    private static String normalizeAccess(String value){String v=clean(value).toUpperCase();return "OPEN_ACCESS".equals(v)?v:"RESTRICTED";}
    private static String safeFileName(String value){String v=clean(value).replace('\\','_').replace('/','_').replace(':','_').replaceAll("[\\p{Cntrl}]","_");if(v.length()>180)v=v.substring(v.length()-180);if(v.length()==0)throw new IllegalArgumentException("Nama berkas kosong.");return v;}
    private static String extension(String name){int p=name.lastIndexOf('.');return p<0?"":name.substring(p+1).toLowerCase();}
    private static String clean(String value){return value==null?"":value.trim();}
    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format("%02x",v&255));return b.toString();}
    private static String extractText(File file,String extension){try{String text="";if("pdf".equals(extension)){PDDocument d=PDDocument.load(file);try{text=new PDFTextStripper().getText(d);}finally{d.close();}}else if("txt".equals(extension)||"csv".equals(extension)||"rtf".equals(extension)){InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream();try{byte[]b=new byte[8192];int n,total=0;while((n=in.read(b))>=0&&total<1048576){int take=Math.min(n,1048576-total);out.write(b,0,take);total+=take;}}finally{in.close();}text=new String(out.toByteArray(),"UTF-8");}text=clean(text).replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]"," ");return text.length()>1048576?text.substring(0,1048576):text;}catch(Exception e){ais.common.ErrorAuditUtil.record(e,"RepositoryFileService.extractText");return "";}}
    private static void requireLogin(Tbmuser actor){if(actor==null||clean(actor.getUserId()).length()==0)throw new SecurityException("Login diperlukan.");}
    private static class ScanResult { String status; Date scannedAt; }
}
