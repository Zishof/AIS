package ais.action.master.jurnal;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.BufferedInputStream;
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
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranJurnal;
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
                || "PUBLICATION".equals(normalizedStage)) auth.requireCrud(actor, "production", "create");
        else auth.requireCrud(actor, "submissions", "update");
        RepoBitstream meta = createPending(itemId, fileName, normalizedMime, normalizedStage,
                validGenre(genre), round, declaredSize, actor);
        StreamingHibernateUtil streaming = StreamingHibernateUtil.getInstance();
        Session blobSession = null; Transaction blobTx = null;
        try {
            blobSession = streaming.currentSession(); blobTx = blobSession.beginTransaction();
            BufferedInputStream buffered = new BufferedInputStream(content, 8192);
            String detectedMime = sniffMime(buffered, normalizedMime);
            requireSniffCompatible(normalizedMime, detectedMime);
            DigestInputStream bounded = new DigestInputStream(buffered, MAX_UPLOAD_BYTES);
            LampiranJurnal lampiran = newLampiran(meta, safeName(fileName), normalizedMime,
                    detectedMime, normalizedStage, declaredSize, actor, "UPLOAD");
            lampiran.setContent(org.hibernate.Hibernate.createBlob(bounded));
            blobSession.save(lampiran); blobSession.flush();
            lampiran.setStorageState("CONTENT_STORED");
            if (bounded.count != declaredSize)
                throw new IOException("Ukuran aktual file tidak sesuai Content-Length.");
            lampiran.setActualSize(Long.valueOf(bounded.count));
            lampiran.setChecksumSha256(bounded.hex());
            lampiran.setStorageState("VERIFIED");
            lampiran.setUpdatedAt(new Date());
            blobSession.update(lampiran);
            blobTx.commit();
            RepoBitstream linked = markLinked(meta.getId(), lampiran.getId(), lampiran.getChecksumSha256(), bounded.count, actor);
            markAvailable(lampiran.getId(), actor);
            return linked;
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
            Query existing = blobSession.createQuery("from LampiranJurnal where repoBitstreamId=:r");
            existing.setLong("r", bitstreamId); existing.setMaxResults(2);
            if (!existing.list().isEmpty()) throw new IllegalStateException("Content import sudah ada atau ambigu; jalankan rekonsiliasi.");
            BufferedInputStream buffered = new BufferedInputStream(content, 8192);
            String declaredMime = validMime(meta.getMimeType());
            DigestInputStream bounded = new DigestInputStream(buffered, MAX_UPLOAD_BYTES);
            String detectedMime=sniffMime(buffered,declaredMime);requireSniffCompatible(declaredMime,detectedMime);
            LampiranJurnal lampiran = newLampiran(meta, safeName(meta.getNamaFile()), declaredMime,
                    detectedMime, validStage(meta.getJournalStage()), declaredSize, actor, "OJS_IMPORT");
            lampiran.setContent(org.hibernate.Hibernate.createBlob(bounded)); blobSession.save(lampiran); blobSession.flush();
            lampiran.setStorageState("CONTENT_STORED");
            if (bounded.count != declaredSize) throw new IOException("Ukuran aktual file import tidak sesuai manifest.");
            lampiran.setActualSize(Long.valueOf(bounded.count)); lampiran.setChecksumSha256(bounded.hex());
            lampiran.setStorageState("VERIFIED"); lampiran.setUpdatedAt(new Date()); blobSession.update(lampiran);
            blobTx.commit(); RepoBitstream linked=markLinked(bitstreamId,lampiran.getId(),lampiran.getChecksumSha256(),bounded.count,actor);
            markAvailable(lampiran.getId(),actor); return linked;
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
            LampiranJurnal lampiran = (LampiranJurnal) session.get(LampiranJurnal.class, bitstream.getContentRef());
            if (lampiran == null || !bitstreamId.equals(lampiran.getRepoBitstreamId())
                    || !("VERIFIED".equals(lampiran.getStorageState()) || "LINKED".equals(lampiran.getStorageState())
                    || "AVAILABLE".equals(lampiran.getStorageState())) || lampiran.getContent() == null)
                throw new FileNotFoundException();
            Blob blob = lampiran.getContent();
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
        auth.requireCrud(actor, "production", "update");
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
            Query q = blobSession.createQuery("from LampiranJurnal where repoBitstreamId=:r order by id desc");
            q.setLong("r", bitstreamId);
            q.setMaxResults(2);
            @SuppressWarnings("unchecked") java.util.List<LampiranJurnal> candidates = q.list();
            if (candidates.size() != 1 || candidates.get(0).getContent() == null)
                throw new IllegalStateException(candidates.isEmpty()
                        ? "BLOB pasangan tidak ditemukan." : "BLOB pasangan ambigu; perlu rekonsiliasi manual.");
            LampiranJurnal lampiran = candidates.get(0);
            Blob blob = lampiran.getContent();
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
            RepoBitstream linked=markLinked(bitstreamId, contentRef, checksum, size, actor);
            markAvailable(contentRef,actor); return linked;
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

    private static LampiranJurnal newLampiran(RepoBitstream meta, String fileName, String declaredMime,
            String detectedMime, String stage, long declaredSize, Tbmuser actor, String source) {
        Date now = new Date();
        LampiranJurnal value = new LampiranJurnal();
        value.setRepoBitstreamId(meta.getId());
        value.setOriginalFileName(fileName);
        value.setDeclaredMimeType(declaredMime);
        value.setDetectedMimeType(detectedMime);
        value.setDeclaredSize(Long.valueOf(declaredSize));
        value.setJournalStage(stage);
        value.setFileVersion(meta.getFileVersion());
        value.setStorageState("PENDING_CONTENT");
        value.setScanState("NOT_CONFIGURED");
        value.setQuarantineState("RELEASED_BY_POLICY");
        value.setIdempotencyKey(source + ":REPO_BITSTREAM:" + meta.getId());
        value.setCreatedBy(actor.getUserId());
        value.setUpdatedBy(actor.getUserId());
        value.setCreatedAt(now);
        value.setUpdatedAt(now);
        return value;
    }

    private static void markAvailable(Long lampiranId, Tbmuser actor) {
        StreamingHibernateUtil streaming = StreamingHibernateUtil.getInstance();
        Session session = null; Transaction tx = null;
        try {
            session = streaming.currentSession(); tx = session.beginTransaction();
            LampiranJurnal value = (LampiranJurnal) session.get(LampiranJurnal.class, lampiranId);
            if (value == null || !("VERIFIED".equals(value.getStorageState()) || "LINKED".equals(value.getStorageState())))
                throw new IllegalStateException("Konten streaming belum terverifikasi.");
            value.setStorageState("LINKED"); session.flush();
            value.setStorageState("AVAILABLE"); value.setUpdatedBy(actor.getUserId()); value.setUpdatedAt(new Date());
            session.update(value); tx.commit();
        } finally {
            if (tx != null && tx.isActive()) tx.rollback();
            try { streaming.closeSession(); } catch (Exception ignored) {}
        }
    }

    /** Minimal magic-byte sniffing; hasil dicatat terpisah dari MIME yang dideklarasikan. */
    private static String sniffMime(BufferedInputStream input, String declaredMime) throws IOException {
        input.mark(32);
        byte[] head = new byte[16];
        int count = input.read(head);
        input.reset();
        if (count >= 5 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F' && head[4] == '-')
            return "application/pdf";
        if (count >= 8 && (head[0] & 255) == 137 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G')
            return "image/png";
        if (count >= 3 && (head[0] & 255) == 255 && (head[1] & 255) == 216 && (head[2] & 255) == 255)
            return "image/jpeg";
        if (count >= 4 && ((head[0] == 'I' && head[1] == 'I' && head[2] == 42 && head[3] == 0)
                || (head[0] == 'M' && head[1] == 'M' && head[2] == 0 && head[3] == 42)))
            return "image/tiff";
        if (count >= 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4)
            return declaredMime;
        if (count >= 8 && (head[0]&255)==208 && (head[1]&255)==207 && (head[2]&255)==17 && (head[3]&255)==224
                && (head[4]&255)==161 && (head[5]&255)==177 && (head[6]&255)==26 && (head[7]&255)==225)
            return "application/msword";
        if (count >= 5 && head[0] == '{' && head[1] == '\\' && head[2] == 'r' && head[3] == 't' && head[4] == 'f')
            return "application/rtf";
        return declaredMime.startsWith("text/") || declaredMime.indexOf("xml") >= 0
                ? declaredMime : "application/octet-stream";
    }
    private static void requireSniffCompatible(String declared,String detected){
        if(!declared.equals(detected))throw new IllegalArgumentException("Isi file tidak sesuai MIME yang dideklarasikan.");
    }

    private static String validStage(String value) { String x = clean(value).toUpperCase(Locale.ENGLISH); if (!STAGES.contains(x)) throw new IllegalArgumentException("Tahap file jurnal tidak valid."); return x; }
    private static String validMime(String value) { String x = clean(value).toLowerCase(Locale.ENGLISH); int semicolon = x.indexOf(';'); if (semicolon >= 0) x = x.substring(0, semicolon).trim(); if (!MIME_TYPES.contains(x)) throw new IllegalArgumentException("Tipe file jurnal tidak diizinkan."); return x; }
    private static String validGenre(String value) { String x = clean(value).toUpperCase(Locale.ENGLISH); if (!x.matches("[A-Z0-9_\\-]{1,80}")) throw new IllegalArgumentException("Genre file jurnal tidak valid."); return x; }
    private static String safeName(String value) { String n = clean(value).replace('\\', '/'); if (n.indexOf('/') >= 0) n = n.substring(n.lastIndexOf('/') + 1); n = n.replaceAll("[\\r\\n\\u0000]", ""); if (n.length() == 0 || n.length() > 255) throw new IllegalArgumentException("Nama file tidak valid."); return n; }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String hex(byte[] bytes) { StringBuilder b = new StringBuilder(); for (byte x : bytes) b.append(String.format("%02x", x & 255)); return b.toString(); }

    /**
     * Tipe implementasi bersarang {@link DigestInputStream} milik {@link JurnalFileService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalFileService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code MessageDigest digest}, {@code long
     * max}, {@code long count}; operasi lokal: {@code read()}, {@code read()}, {@code guard()}, {@code hex}().
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see JurnalFileService
     */
    private static final class DigestInputStream extends FilterInputStream {
        final MessageDigest digest; final long max; long count;
        DigestInputStream(InputStream in, long maximum) throws Exception { super(in); max = maximum; digest = MessageDigest.getInstance("SHA-256"); }
        public int read() throws IOException { int b = super.read(); if (b >= 0) { count++; guard(); digest.update((byte) b); } return b; }
        public int read(byte[] b, int o, int l) throws IOException { int n = super.read(b, o, l); if (n > 0) { count += n; guard(); digest.update(b, o, n); } return n; }
        void guard() throws IOException { if (count > max) throw new IOException("File terlalu besar"); }
        String hex() { return JurnalFileService.hex(digest.digest()); }
    }
}
