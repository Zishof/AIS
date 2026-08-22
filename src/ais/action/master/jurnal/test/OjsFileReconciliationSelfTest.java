package ais.action.master.jurnal.test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalFileService;
import ais.action.master.jurnal.importer.OjsFileReconciliationService;
import ais.action.master.jurnal.importer.OjsImportReconciliationService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.ImportJobOjs;
import ais.database.model.repository.RepoBitstream;

/** Commits only to disposable main/streaming importer fixtures. */
public final class OjsFileReconciliationSelfTest {
    private OjsFileReconciliationSelfTest(){}
    @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
        if(!"ais_jurnal_import_fixture".equals(System.getenv("AIS_JURNAL_DB_NAME"))||!"streaming_ais_jurnal_import_fixture".equals(System.getenv("AIS_JURNAL_STREAMING_DB_NAME")))throw new IllegalStateException("Self-test hanya boleh memakai disposable importer clones.");
        System.setProperty("javax.persistence.validation.mode","none");Tbmuser actor=admin();Path root=Files.createTempDirectory("ojs-files-fixture-");int exit=0;
        try{
            Session s=HibernateUtil.currentSession();Query q=s.createQuery("from RepoBitstream where sourceClass like 'OJS_IMPORT:%:submission_files' and storageState<>'LINKED' and contentRef is null and aktif=true order by id desc");q.setMaxResults(1);RepoBitstream bitstream=(RepoBitstream)q.uniqueResult();if(bitstream==null)throw new IllegalStateException("Manifest pending/retry hasil domain transform tidak ditemukan.");
            String[] parts=bitstream.getSourceClass().split(":");Long sourceId=Long.valueOf(parts[1]);Path file=root.resolve("journals/1/articles/100/submission/article.pdf");Files.createDirectories(file.getParent());byte[] expected="%PDF-1.4\n% AIS OJS fixture\n1 0 obj<</Type/Catalog>>endobj\n%%EOF\n".getBytes(Charset.forName("UTF-8"));Files.write(file,expected);
            OjsFileReconciliationService service=new OjsFileReconciliationService();OjsFileReconciliationService.Result first=service.reconcile(sourceId,root,actor);if(first.linked!=1||first.failed!=0||first.rejected!=0)throw new IllegalStateException("Rekonsiliasi pertama gagal linked="+first.linked+" failed="+first.failed);
            OjsFileReconciliationService.Result second=service.reconcile(sourceId,root,actor);if(second.alreadyLinked!=1||second.linked!=0)throw new IllegalStateException("Rerun file tidak idempoten.");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();new JurnalFileService().stream(bitstream.getId(),actor,bytes);if(!java.util.Arrays.equals(expected,bytes.toByteArray()))throw new IllegalStateException("Byte hasil stream berbeda.");
            ByteArrayOutputStream anonymous=new ByteArrayOutputStream();new JurnalFileService().stream(bitstream.getId(),null,"127.0.0.1",anonymous);if(!java.util.Arrays.equals(expected,anonymous.toByteArray()))throw new IllegalStateException("Galley open-access anonim gagal.");
            ImportJobOjs job=(ImportJobOjs)s.createQuery("from ImportJobOjs where sourceId=:s and status='CORE_TRANSFORMED' and aktif=true order by id desc").setLong("s",sourceId).setMaxResults(1).uniqueResult();if(job==null)throw new IllegalStateException("Job core transform untuk finalisasi tidak ditemukan.");OjsImportReconciliationService.Result finalResult=new OjsImportReconciliationService().finalizeJob(job.getId(),actor);if(!finalResult.complete||finalResult.pendingFiles!=0)throw new IllegalStateException("Final reconciliation OJS 3.5 gagal: "+finalResult.blockers);JSONObject finalReport=new JSONObject(((ImportJobOjs)s.get(ImportJobOjs.class,job.getId())).getReportJson());if(!finalReport.getBoolean("complete134Table905FieldReconciliation"))throw new IllegalStateException("Flag rekonsiliasi 134/905 belum final.");
            System.out.println("OjsFileReconciliationSelfTest OK linked=1 alreadyLinked=1 bytes="+expected.length+" checksum-stream-verified anonymous-entitlement final-134/905");
        }catch(Throwable e){exit=1;e.printStackTrace();}finally{HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());try{List<Path>paths=new ArrayList<Path>();java.util.stream.Stream<Path>w=Files.walk(root);try{java.util.Iterator<Path>it=w.iterator();while(it.hasNext()){paths.add(it.next());}}finally{w.close();}Collections.sort(paths,new Comparator<Path>(){public int compare(Path a,Path b){return b.getNameCount()-a.getNameCount();}});for(Path p:paths)Files.deleteIfExists(p);}catch(Exception ignored){}}
        System.exit(exit);
    }
    private static Tbmuser admin()throws Exception{Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject json=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>menus=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){json.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)json.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));menus.add(m);}json.getJSONObject("workflow").put("manageImport",true);role.setJurnalAksesJson(json.toString());role.setMenus(menus);Tbmuser u=new Tbmuser();u.setUserId("JRN_OJS_FILE_SELF_TEST");u.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),role);return u;}
}
