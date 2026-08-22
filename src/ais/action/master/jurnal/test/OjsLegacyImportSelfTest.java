package ais.action.master.jurnal.test;

import java.util.HashSet;
import org.hibernate.Session;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.importer.OjsImportExecutionService;
import ais.action.master.jurnal.importer.OjsImportPreflightService;
import ais.action.master.jurnal.importer.OjsImportReconciliationService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.ImportJobOjs;
import ais.database.model.jurnal.ImportSumberOjs;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** End-to-end compatibility fixture for the former seven-table OJS 2.x import path. */
public final class OjsLegacyImportSelfTest {
    public static void main(String[]args)throws Exception{
        String db=System.getenv("AIS_JURNAL_DB_NAME"),jdbc=System.getenv("AIS_JURNAL_OJS_FIXTURE_JDBC"),password=System.getenv("AIS_JURNAL_OJS_FIXTURE_PASSWORD");
        if(!"ais_jurnal_import_legacy_fixture".equals(db)||jdbc==null||!jdbc.contains("ojs_jurnal_fixture_legacy")||password==null)throw new IllegalStateException("Self-test hanya boleh memakai fixture legacy disposable.");
        System.setProperty("javax.persistence.validation.mode","none");Tbmuser actor=admin();int exit=0;
        try{String suffix=String.valueOf(System.currentTimeMillis());OjsImportPreflightService.Config cfg=new OjsImportPreflightService.Config();cfg.jdbcUrl=jdbc;cfg.user=System.getenv("AIS_JURNAL_OJS_FIXTURE_USER");cfg.password=password;cfg.schema="public";
            OjsImportPreflightService.Result preflight=new OjsImportPreflightService().inspect(cfg);check("LEGACY".equals(preflight.version),"Versi legacy tidak dikenali.");check(preflight.expectedTables==7&&preflight.foundTables==7&&preflight.foundFields==37,"Inventory legacy tidak tepat.");
            JurnalPenelitian journal=new JurnalAdministrationService().create("legacy-import-"+suffix,"Legacy Import Self Test","legacy-import-"+suffix,"id_ID",actor);OjsImportExecutionService importer=new OjsImportExecutionService();ImportSumberOjs source=importer.registerSource(journal.getId(),null,"legacy-"+suffix,"Legacy OJS fixture","env:legacy",cfg,actor);ImportJobOjs first=importer.start(source.getId(),false,"legacy-execute-"+suffix,cfg,20,actor);check("CORE_TRANSFORMED".equals(first.getStatus()),"Transform legacy gagal: "+first.getStatus());
            JSONObject report=new JSONObject(first.getReportJson());check(report.getInt("sourceTablesStaged")==7,"Tidak semua tabel legacy di-stage.");check(report.getInt("domainSubmissionsCreated")==1&&report.getInt("domainIssuesCreated")==1&&report.getInt("domainIssueRelationsCreated")==1,"Root/relasi legacy tidak lengkap: "+report);
            Session s=HibernateUtil.currentSession();Number unresolved=(Number)s.createQuery("select count(*) from ImportMappingOjs where sourceId=:s and aktif=true and targetId is null").setLong("s",source.getId()).uniqueResult();check(unresolved.longValue()==0,"Mapping legacy tanpa target: "+unresolved);Object title=s.createQuery("select title from RepoItem where collectionId=:c and documentType='JOURNAL_SUBMISSION' and sourceClass like :p").setLong("c",journal.getRepoCollectionId()).setString("p","OJS_IMPORT:"+source.getId()+":articles").setMaxResults(1).uniqueResult();check("Artikel Legacy Teruji".equals(title),"Judul artikel legacy tidak terpetakan: "+title);
            ImportJobOjs again=importer.start(source.getId(),false,"legacy-execute-"+suffix,cfg,20,actor);check(first.getId().equals(again.getId()),"Rerun legacy tidak idempoten.");OjsImportReconciliationService.Result finalResult=new OjsImportReconciliationService().finalizeJob(first.getId(),actor);check(finalResult.complete&&"COMPLETED".equals(finalResult.status),"Rekonsiliasi final legacy gagal: "+finalResult.blockers);System.out.println("OjsLegacyImportSelfTest OK version=LEGACY tables=7 fields=37 roots=2 relation=1 idempotent reconciled");
        }catch(Throwable e){exit=1;e.printStackTrace();}finally{HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());}System.exit(exit);
    }
    private static void check(boolean x,String m){if(!x)throw new IllegalStateException(m);}private static Tbmuser admin()throws Exception{Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject json=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>menus=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){json.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)json.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));menus.add(m);}json.getJSONObject("workflow").put("manageImport",true);role.setJurnalAksesJson(json.toString());role.setMenus(menus);Tbmuser u=new Tbmuser();u.setUserId("JRN_OJS_LEGACY_SELF_TEST");u.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),role);return u;}
}
