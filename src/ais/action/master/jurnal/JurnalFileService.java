package ais.action.master.jurnal;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.sql.Blob;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoItem;

/** Metadata di main DB; byte BLOB di SessionFactory streaming, tanpa relasi ORM lintas DB. */
public final class JurnalFileService {
    public static final long MAX_UPLOAD_BYTES = 200L * 1024L * 1024L;
    private static final Set<String> STAGES = new HashSet<String>(Arrays.asList(
            "SUBMISSION", "REVIEW", "COPYEDITING", "PRODUCTION", "PROOF", "PUBLICATION"));
    private static final Set<String> MIME_TYPES = new HashSet<String>(Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.text", "application/rtf", "text/plain",
            "application/epub+zip", "application/xml", "text/xml",
            "image/png", "image/jpeg", "image/tiff"));
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public RepoBitstream store(Long itemId, String fileName, String mimeType, String stage, String genre,
            Integer round, InputStream content, long declaredSize, Tbmuser actor) {
        if (content == null || declaredSize < 1 || declaredSize > MAX_UPLOAD_BYTES)
            throw new IllegalArgumentException("Ukuran file tidak diizinkan.");
        String normalizedStage = validStage(stage);
        String normalizedMime = validMime(mimeType);
        if ("PRODUCTION".equals(normalizedStage) || "PROOF".equals(normalizedStage)
                || "PUBLICATION".equals(normalizedStage)) auth.requireCrud(actor, "produksiGalley", "create");
        else auth.requireCrud(actor, "submission", "update");
        RepoBitstream meta = createPending(itemId, fileName, normalizedMime, normalizedStage,
                validGenre(genre), round, declaredSize, actor);
        StreamingHibernateUtil streaming = StreamingHibernateUtil.getInstance();
        Session blobSession = null; Transaction blobTx = null;
        try {
            blobSession = streaming.currentSession(); blobTx = blobSession.beginTransaction();
            DigestInputStream bounded = new DigestInputStream(content, MAX_UPLOAD_BYTES);
            LampiranLain lampiran = new LampiranLain();
            lampiran.setRef(meta.getId()); lampiran.setJenis(LampiranLain.JURNAL_REPO_BITSTREAM);
            lampiran.setNama(safeName(fileName)); lampiran.setKeterangan("RepoBitstream:" + meta.getId());
            lampiran.setOlehId(actor.getUserId()); lampiran.setOleh(actor.getUserId()); lampiran.setTanggal_dirubah(new Date());
            lampiran.setFoto(Hibernate.createBlob(bounded)); blobSession.save(lampiran); blobSession.flush();
            if (bounded.count != declaredSize)
                throw new IOException("Ukuran aktual file tidak sesuai Content-Length.");
            blobTx.commit();
            return markLinked(meta.getId(), lampiran.getId(), bounded.hex(), bounded.count, actor);
        } catch (Exception e) {
            if (blobTx != null && blobTx.isActive()) blobTx.rollback();
            markFailed(meta.getId(), actor);
            throw new IllegalStateException("Penyimpanan file jurnal gagal.", e);
        } finally { try { streaming.closeSession(); } catch (Exception ignored) {} }
    }

    /** Attach bytes to a manifest created by the OJS importer, without creating duplicate metadata. */
    public RepoBitstream attachImportedContent(Long bitstreamId, InputStream content, long declaredSize, Tbmuser actor) {
        auth.requireWorkflow(actor, "manageImport");
        if (content == null || declaredSize < 1 || declaredSize > MAX_UPLOAD_BYTES)
            throw new IllegalArgumentException("Ukuran file import tidak diizinkan.");
        Session main = HibernateUtil.currentSession();
        RepoBitstream meta = (RepoBitstream) main.get(RepoBitstream.class, bitstreamId);
        if (meta == null || meta.getSourceClass() == null || !meta.getSourceClass().startsWith("OJS_IMPORT:"))
            throw new IllegalArgumentException("Manifest file OJS tidak ditemukan.");
        RepoItem item = (RepoItem) main.get(RepoItem.class, meta.getItemId());
        auth.requireItemScope(main, actor, item, false, meta.getJournalStage());
        if ("LINKED".equals(meta.getStorageState()) && meta.getContentRef() != null) return meta;
        if (!("PENDING_CONTENT".equals(meta.getStorageState()) || "FAILED".equals(meta.getStorageState())))
            throw new IllegalStateException("State manifest tidak dapat menerima content.");
        StreamingHibernateUtil streaming = StreamingHibernateUtil.getInstance(); Session blobSession = null; Transaction blobTx = null;
        try {
            blobSession = streaming.currentSession(); blobTx = blobSession.beginTransaction();
            Query existing = blobSession.createQuery("from LampiranLain where ref=:r and jenis=:j");
            existing.setLong("r", bitstreamId); existing.setString("j", LampiranLain.JURNAL_REPO_BITSTREAM); existing.setMaxResults(2);
            if (!existing.list().isEmpty()) throw new IllegalStateException("Content import sudah ada atau ambigu; jalankan rekonsiliasi.");
            DigestInputStream bounded = new DigestInputStream(content, MAX_UPLOAD_BYTES);
            LampiranLain lampiran = new LampiranLain(); lampiran.setRef(bitstreamId); lampiran.setJenis(LampiranLain.JURNAL_REPO_BITSTREAM);
            lampiran.setNama(meta.getNamaFile()); lampiran.setKeterangan("OJS Import RepoBitstream:"+bitstreamId);
            lampiran.setOlehId(actor.getUserId()); lampiran.setOleh(actor.getUserId()); lampiran.setTanggal_dirubah(new Date());
            lampiran.setFoto(Hibernate.createBlob(bounded)); blobSession.save(lampiran); blobSession.flush();
            if (bounded.count != declaredSize) throw new IOException("Ukuran aktual file import tidak sesuai manifest.");
            blobTx.commit(); return markLinked(bitstreamId,lampiran.getId(),bounded.hex(),bounded.count,actor);
        } catch(Exception e) {
            if(blobTx!=null&&blobTx.isActive())blobTx.rollback(); markFailed(bitstreamId,actor);
            throw new IllegalStateException("Penyimpanan content import OJS gagal.",e);
        } finally { try{streaming.closeSession();}catch(Exception ignored){} }
    }

    public void stream(Long bitstreamId, Tbmuser actor, OutputStream output) throws Exception { stream(bitstreamId,actor,null,output); }

    /** Authorizes before HTTP metadata headers are exposed. */
    public RepoBitstream metadataForDownload(Long bitstreamId,Tbmuser actor,String remoteIp)throws FileNotFoundException{
        RepoBitstream bitstream=(RepoBitstream)HibernateUtil.currentSession().get(RepoBitstream.class,bitstreamId);
        if(bitstream==null||!"LINKED".equals(bitstream.getStorageState())||bitstream.getContentRef()==null)throw new FileNotFoundException();
        requireDownloadAccess(bitstream,actor,remoteIp);return bitstream;
    }

    public void stream(Long bitstreamId, Tbmuser actor, String remoteIp, OutputStream output) throws Exception {
        if (output == null) throw new IllegalArgumentException("Output stream wajib tersedia.");
        RepoBitstream bitstream = metadataForDownload(bitstreamId,actor,remoteIp);
        StreamingHibernateUtil streaming = StreamingHibernateUtil.getInstance();
        Transaction streamingTx = null;
        try {
            Session session = streaming.currentSession();
            streamingTx = session.beginTransaction();
            LampiranLain lampiran = (LampiranLain) session.get(LampiranLain.class, bitstream.getContentRef());
            if (lampiran == null || !bitstreamId.equals(lampiran.getRef())
                    || !LampiranLain.JURNAL_REPO_BITSTREAM.equals(lampiran.getJenis()) || lampiran.getFoto() == null)
                throw new FileNotFoundException();
            Blob blob = lampiran.getFoto();
            if (blob.length() < 1 || blob.length() > MAX_UPLOAD_BYTES || blob.length() != bitstream.getUkuranByte().longValue())
                throw new IOException("Ukuran file jurnal tidak konsisten.");
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); InputStream in = blob.getBinaryStream();
            try { byte[] buffer = new byte[65536]; int n; while ((n = in.read(buffer)) >= 0) { digest.update(buffer, 0, n); output.write(buffer, 0, n); } }
            finally { in.close(); }
            if (!hex(digest.digest()).equalsIgnoreCase(clean(bitstream.getChecksum())))
                throw new IOException("Checksum file jurnal tidak konsisten.");
            streamingTx.commit();
        } finally {
            if (streamingTx != null && streamingTx.isActive()) streamingTx.rollback();
            streaming.closeSession();
        }
    }

    private void requireDownloadAccess(RepoBitstream bitstream,Tbmuser actor,String remoteIp){
        RepoItem item = (RepoItem) HibernateUtil.currentSession().get(RepoItem.class, bitstream.getItemId());
        boolean published = item != null && "PUBLISHED".equals(item.getWorkflowStatus()) && !Boolean.TRUE.equals(item.getIsWithdrawn());
        boolean distributable = "PUBLICATION".equals(bitstream.getJournalStage()) || ("GALLEY".equals(bitstream.getJournalGenre()) && Boolean.TRUE.equals(bitstream.getPrimaryFile()));
        boolean entitled = published && distributable && new JurnalAccessService().evaluate(item,actor==null?null:actor.getUserId(),null,remoteIp,new Date()).allowed;
        if (!entitled) {
            if (actor == null) throw new SecurityException("Login diperlukan.");
            if (!auth.canRead(actor, "submission") && !auth.canRead(actor, "produksiGalley"))
                throw new SecurityException("Hak baca file tidak tersedia.");
            auth.requireItemScope(HibernateUtil.currentSession(), actor, item, true, bitstream.getJournalStage());
        }
    }

    /** Repairs a metadata/blob link after a cross-database partial failure. */
    public RepoBitstream reconcile(Long bitstreamId, Tbmuser actor) {
        auth.requireCrud(actor, "produksiGalley", "update");
        Session main = HibernateUtil.currentSession();
        RepoBitstream bitstream = (RepoBitstream) main.get(RepoBitstream.class, bitstreamId);
        if (bitstream == null) throw new IllegalArgumentException("Metadata file tidak ditemukan.");
        RepoItem item = (RepoItem) main.get(RepoItem.class, bitstream.getItemId());
        auth.requireItemScope(main, actor, item, false, bitstream.getJournalStage());
        if ("LINKED".equals(bitstream.getStorageState()) && bitstream.getContentRef() != null) return bitstream;
        StreamingHibernateUtil streaming = StreamingHibernateUtil.getInstance();
        Transaction streamingTx = null;
        try {
            Session blobSession = streaming.currentSession();
            streamingTx = blobSession.beginTransaction();
            Query q = blobSession.createQuery("from LampiranLain where ref=:r and jenis=:j order by id desc");
            q.setLong("r", bitstreamId);
            q.setString("j", LampiranLain.JURNAL_REPO_BITSTREAM);
            q.setMaxResults(2);
            @SuppressWarnings("unchecked") java.util.List<LampiranLain> candidates = q.list();
            if (candidates.size() != 1 || candidates.get(0).getFoto() == null)
                throw new IllegalStateException(candidates.isEmpty()
                        ? "BLOB pasangan tidak ditemukan." : "BLOB pasangan ambigu; perlu rekonsiliasi manual.");
            LampiranLain lampiran = candidates.get(0);
            Blob blob = lampiran.getFoto();
            long size = blob.length();
            if (size < 1 || size > MAX_UPLOAD_BYTES)
                throw new IllegalStateException("Ukuran BLOB tidak valid.");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream in = blob.getBinaryStream();
            try { byte[] buffer = new byte[65536]; int n; while ((n = in.read(buffer)) >= 0) digest.update(buffer, 0, n); }
            finally { in.close(); }
            String checksum = hex(digest.digest());
            Long contentRef = lampiran.getId();
            streamingTx.commit();
            return markLinked(bitstreamId, contentRef, checksum, size, actor);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Rekonsiliasi file jurnal gagal.", e);
        } finally {
            if (streamingTx != null && streamingTx.isActive()) streamingTx.rollback();
            streaming.closeSession();
        }
    }

    private RepoBitstream createPending(Long itemId, String fileName, String mime, String stage, String genre,
            Integer round, long size, Tbmuser actor) {
        Session session = HibernateUtil.currentSession(); Transaction tx = session.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); RepoItem item = (RepoItem) session.get(RepoItem.class, itemId);
            if (item == null || !"JOURNAL_SUBMISSION".equals(item.getDocumentType())) throw new IllegalArgumentException("Naskah tidak ditemukan.");
            boolean ownerAllowed = "SUBMISSION".equals(stage) || "REVIEW".equals(stage);
            auth.requireItemScope(session, actor, item, ownerAllowed, stage);
            RepoBitstream b = new RepoBitstream(); b.setItemId(itemId); b.setNamaFile(safeName(fileName)); b.setMimeType(mime);
            b.setPathSistem("streaming:pending"); b.setUkuranByte(size); b.setJournalStage(stage); b.setJournalGenre(genre);
            b.setReviewRound(round); b.setStorageState("PENDING_CONTENT"); b.setAccessPolicy("RESTRICTED"); b.setFileVersion(1L);
            b.setAktif(Boolean.TRUE); b.setOlehId(actor.getUserId()); session.save(b); if (own) tx.commit(); return b;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    private RepoBitstream markLinked(Long id, Long ref, String checksum, long actual, Tbmuser actor) {
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); RepoBitstream b = (RepoBitstream) s.get(RepoBitstream.class, id);
            b.setContentRef(ref); b.setChecksum(checksum); b.setUkuranByte(actual); b.setPathSistem("streaming:" + ref);
            b.setStorageState("CONTENT_VERIFIED"); s.flush(); b.setStorageState("LINKED"); b.setOlehId(actor.getUserId()); s.update(b);
            if (own) tx.commit(); return b;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    private void markFailed(Long id, Tbmuser actor) {
        try { Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive(); if (own) tx.begin();
            RepoBitstream b = (RepoBitstream) s.get(RepoBitstream.class, id); if (b != null) { b.setStorageState("FAILED"); b.setOlehId(actor.getUserId()); s.update(b); }
            if (own) tx.commit();
        } catch (Exception ignored) { try { ais.common.ErrorAuditUtil.record(ignored, "JurnalFileService.markFailed"); } catch (Exception ignored2) {} }
    }

    private static String validStage(String value) { String x = clean(value).toUpperCase(Locale.ENGLISH); if (!STAGES.contains(x)) throw new IllegalArgumentException("Tahap file jurnal tidak valid."); return x; }
    private static String validMime(String value) { String x = clean(value).toLowerCase(Locale.ENGLISH); int semicolon = x.indexOf(';'); if (semicolon >= 0) x = x.substring(0, semicolon).trim(); if (!MIME_TYPES.contains(x)) throw new IllegalArgumentException("Tipe file jurnal tidak diizinkan."); return x; }
    private static String validGenre(String value) { String x = clean(value).toUpperCase(Locale.ENGLISH); if (!x.matches("[A-Z0-9_\\-]{1,80}")) throw new IllegalArgumentException("Genre file jurnal tidak valid."); return x; }
    private static String safeName(String value) { String n = clean(value).replace('\\', '/'); if (n.indexOf('/') >= 0) n = n.substring(n.lastIndexOf('/') + 1); n = n.replaceAll("[\\r\\n\\u0000]", ""); if (n.length() == 0 || n.length() > 255) throw new IllegalArgumentException("Nama file tidak valid."); return n; }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String hex(byte[] bytes) { StringBuilder b = new StringBuilder(); for (byte x : bytes) b.append(String.format("%02x", x & 255)); return b.toString(); }

    private static final class DigestInputStream extends FilterInputStream {
        final MessageDigest digest; final long max; long count;
        DigestInputStream(InputStream in, long maximum) throws Exception { super(in); max = maximum; digest = MessageDigest.getInstance("SHA-256"); }
        public int read() throws IOException { int b = super.read(); if (b >= 0) { count++; guard(); digest.update((byte) b); } return b; }
        public int read(byte[] b, int o, int l) throws IOException { int n = super.read(b, o, l); if (n > 0) { count += n; guard(); digest.update(b, o, n); } return n; }
        void guard() throws IOException { if (count > max) throw new IOException("File terlalu besar"); }
        String hex() { return JurnalFileService.hex(digest.digest()); }
    }
}
