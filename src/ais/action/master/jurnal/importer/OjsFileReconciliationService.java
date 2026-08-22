package ais.action.master.jurnal.importer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import ais.action.master.jurnal.JurnalFileService;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.ImportMappingOjs;
import ais.database.model.repository.RepoBitstream;

/** Secure, resumable reconciliation from an OJS files_dir into streaming_ais. */
public final class OjsFileReconciliationService {
    public static final class Result { public long manifests,linked,alreadyLinked,missing,rejected,failed; }

    @SuppressWarnings("unchecked")
    public Result reconcile(Long sourceId,Path sourceRoot,Tbmuser actor){
        if(sourceId==null||sourceRoot==null)throw new IllegalArgumentException("Source dan files_dir OJS wajib diisi.");
        Result out=new Result();
        try{
            Path root=sourceRoot.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("files_dir OJS bukan direktori.");
            Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoBitstream where sourceClass=:sc and aktif=true order by id");q.setString("sc","OJS_IMPORT:"+sourceId+":submission_files");List<RepoBitstream> rows=q.list();out.manifests=rows.size();
            for(RepoBitstream b:rows){
                if("LINKED".equals(b.getStorageState())&&b.getContentRef()!=null){out.alreadyLinked++;continue;}
                String raw=sourcePath(s,sourceId,b.getId());if(raw==null){out.missing++;continue;}
                try{
                    String portable=raw.replace('\\','/');if(portable.startsWith("/")||portable.matches("^[A-Za-z]:.*")){out.rejected++;continue;}
                    Path candidate=root.resolve(portable).normalize();if(!candidate.startsWith(root)||!Files.isRegularFile(candidate,LinkOption.NOFOLLOW_LINKS)){out.missing++;continue;}
                    Path real=candidate.toRealPath();if(!real.startsWith(root)){out.rejected++;continue;}
                    long size=Files.size(real);if(size<1||size>JurnalFileService.MAX_UPLOAD_BYTES){out.rejected++;continue;}
                    InputStream in=Files.newInputStream(real);try{new JurnalFileService().attachImportedContent(b.getId(),in,size,actor);}finally{in.close();}out.linked++;
                }catch(SecurityException e){throw e;}catch(Exception e){out.failed++;try{ais.common.ErrorAuditUtil.record(e,"OjsFileReconciliationService:"+b.getId());}catch(Exception ignored){}}
            }
            return out;
        }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Rekonsiliasi files_dir OJS gagal.",e);}
    }

    private static String sourcePath(Session s,Long sourceId,Long bitstreamId){
        Query q=s.createQuery("from ImportMappingOjs where sourceId=:s and sourceTable='submission_files' and sourceField='file_id' and targetId=:b and aktif=true");q.setLong("s",sourceId);q.setLong("b",bitstreamId);q.setMaxResults(1);ImportMappingOjs link=(ImportMappingOjs)q.uniqueResult();if(link==null||blank(link.getRawPayload()))return null;
        q=s.createQuery("from ImportMappingOjs where sourceId=:s and sourceTable='files' and sourceField='file_id' and rawPayload=:f and aktif=true");q.setLong("s",sourceId);q.setString("f",link.getRawPayload());q.setMaxResults(1);ImportMappingOjs fileId=(ImportMappingOjs)q.uniqueResult();if(fileId==null)return null;
        q=s.createQuery("from ImportMappingOjs where sourceId=:s and sourceTable='files' and sourcePk=:p and sourceField='path' and aktif=true");q.setLong("s",sourceId);q.setString("p",fileId.getSourcePk());q.setMaxResults(1);ImportMappingOjs path=(ImportMappingOjs)q.uniqueResult();return path==null?null:path.getRawPayload();
    }
    private static boolean blank(String v){return v==null||v.trim().length()==0;}
}
