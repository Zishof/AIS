package ais.action.master.jurnal;

import java.io.*;
import java.security.MessageDigest;
import java.sql.Blob;
import java.util.Date;
import org.hibernate.*;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.repository.RepoBitstream;
import ais.database.model.repository.RepoItem;

/** Metadata di main DB; byte BLOB di SessionFactory streaming, tanpa relasi ORM lintas DB. */
public final class JurnalFileService {
    public static final long MAX_UPLOAD_BYTES=200L*1024L*1024L;
    private final JurnalAuthorizationService auth=new JurnalAuthorizationService();

    public RepoBitstream store(Long itemId,String fileName,String mimeType,String stage,String genre,Integer round,
            InputStream content,long declaredSize,Tbmuser actor){
        if(content==null||declaredSize<1||declaredSize>MAX_UPLOAD_BYTES)throw new IllegalArgumentException("Ukuran file tidak diizinkan.");
        auth.requireCrud(actor,"submission","update");RepoBitstream meta=createPending(itemId,fileName,mimeType,stage,genre,round,declaredSize,actor);
        StreamingHibernateUtil streaming=StreamingHibernateUtil.getInstance();Session blobSession=null;Transaction blobTx=null;
        try{
            blobSession=streaming.currentSession();blobTx=blobSession.beginTransaction();
            DigestInputStream bounded=new DigestInputStream(content,MAX_UPLOAD_BYTES);
            LampiranLain lampiran=new LampiranLain();lampiran.setRef(meta.getId());lampiran.setJenis(LampiranLain.JURNAL_REPO_BITSTREAM);
            lampiran.setNama(safeName(fileName));lampiran.setKeterangan("RepoBitstream:"+meta.getId());lampiran.setOlehId(actor.getUserId());lampiran.setOleh(actor.getUserId());lampiran.setTanggal_dirubah(new Date());
            lampiran.setFoto(org.hibernate.Hibernate.createBlob(bounded));blobSession.save(lampiran);blobSession.flush();blobTx.commit();
            return markLinked(meta.getId(),lampiran.getId(),bounded.hex(),bounded.count,actor);
        }catch(Exception e){if(blobTx!=null&&blobTx.isActive())blobTx.rollback();markFailed(meta.getId(),actor);throw new IllegalStateException("Penyimpanan file jurnal gagal.",e);}
        finally{try{streaming.closeSession();}catch(Exception ignored){}}
    }

    public void stream(Long bitstreamId,Tbmuser actor,OutputStream output)throws Exception{
        RepoBitstream b=(RepoBitstream)HibernateUtil.currentSession().get(RepoBitstream.class,bitstreamId);if(b==null||!"LINKED".equals(b.getStorageState()))throw new FileNotFoundException();
        RepoItem item=(RepoItem)HibernateUtil.currentSession().get(RepoItem.class,b.getItemId());boolean publicFile=item!=null&&"PUBLISHED".equals(item.getWorkflowStatus())&&!Boolean.TRUE.equals(item.getIsWithdrawn())&&"OPEN_ACCESS".equals(b.getAccessPolicy());
        if(!publicFile){if(actor==null)throw new SecurityException("Login diperlukan.");boolean owner=item!=null&&actor.getUserId().equals(item.getOwnerId());if(!owner&&!auth.canRead(actor,"submission")&&!auth.canRead(actor,"produksiGalley"))throw new SecurityException("File berada di luar scope.");}
        StreamingHibernateUtil streaming=StreamingHibernateUtil.getInstance();try{Session s=streaming.currentSession();Query q=s.createQuery("from LampiranLain where ref=:ref and jenis=:jenis order by id desc");q.setLong("ref",bitstreamId);q.setString("jenis",LampiranLain.JURNAL_REPO_BITSTREAM);q.setMaxResults(1);LampiranLain l=(LampiranLain)q.uniqueResult();if(l==null||l.getFoto()==null)throw new FileNotFoundException();Blob blob=l.getFoto();InputStream in=blob.getBinaryStream();try{byte[] buf=new byte[65536];int n;while((n=in.read(buf))>=0)output.write(buf,0,n);}finally{in.close();}}finally{streaming.closeSession();}
    }

    private RepoBitstream createPending(Long itemId,String fileName,String mime,String stage,String genre,Integer round,long size,Tbmuser actor){Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();RepoItem item=(RepoItem)s.get(RepoItem.class,itemId);if(item==null||!"JOURNAL_SUBMISSION".equals(item.getDocumentType()))throw new IllegalArgumentException("Naskah tidak ditemukan.");if(!actor.getUserId().equals(item.getOwnerId())&&!auth.canCrud(actor,"produksiGalley","create"))throw new SecurityException("Naskah berada di luar scope.");RepoBitstream b=new RepoBitstream();b.setItemId(itemId);b.setNamaFile(safeName(fileName));b.setMimeType(clean(mime));b.setPathSistem("streaming:pending");b.setUkuranByte(size);b.setJournalStage(clean(stage));b.setJournalGenre(clean(genre));b.setReviewRound(round);b.setStorageState("PENDING_CONTENT");b.setAccessPolicy("RESTRICTED");b.setFileVersion(1L);b.setAktif(Boolean.TRUE);b.setOlehId(actor.getUserId());s.save(b);if(own)tx.commit();return b;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    private RepoBitstream markLinked(Long id,Long ref,String checksum,long actual,Tbmuser actor){Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();try{if(own)tx.begin();RepoBitstream b=(RepoBitstream)s.get(RepoBitstream.class,id);b.setContentRef(ref);b.setChecksum(checksum);b.setUkuranByte(actual);b.setPathSistem("streaming:"+ref);b.setStorageState("CONTENT_VERIFIED");s.flush();b.setStorageState("LINKED");b.setOlehId(actor.getUserId());s.update(b);if(own)tx.commit();return b;}catch(RuntimeException e){if(own&&tx.isActive())tx.rollback();throw e;}}
    private void markFailed(Long id,Tbmuser actor){try{Session s=HibernateUtil.currentSession();Transaction tx=s.getTransaction();boolean own=!tx.isActive();if(own)tx.begin();RepoBitstream b=(RepoBitstream)s.get(RepoBitstream.class,id);if(b!=null){b.setStorageState("FAILED");b.setOlehId(actor.getUserId());s.update(b);}if(own)tx.commit();}catch(Exception ignored){try{ais.common.ErrorAuditUtil.record(ignored,"JurnalFileService.markFailed");}catch(Exception ignored2){}}}
    private static String safeName(String v){String n=clean(v).replace('\\','/');if(n.indexOf('/')>=0)n=n.substring(n.lastIndexOf('/')+1);n=n.replaceAll("[\\r\\n\\u0000]","");if(n.length()==0||n.length()>255)throw new IllegalArgumentException("Nama file tidak valid.");return n;}
    private static String clean(String v){return v==null?"":v.trim();}
    private static final class DigestInputStream extends FilterInputStream{final MessageDigest digest;final long max;long count;DigestInputStream(InputStream in,long m)throws Exception{super(in);max=m;digest=MessageDigest.getInstance("SHA-256");}public int read()throws IOException{int b=super.read();if(b>=0){count++;guard();digest.update((byte)b);}return b;}public int read(byte[]b,int o,int l)throws IOException{int n=super.read(b,o,l);if(n>0){count+=n;guard();digest.update(b,o,n);}return n;}void guard()throws IOException{if(count>max)throw new IOException("File terlalu besar");}String hex(){StringBuilder b=new StringBuilder();for(byte x:digest.digest())b.append(String.format("%02x",x&255));return b.toString();}}
}
