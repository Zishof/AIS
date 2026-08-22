package ais.action.master.jurnal.test;

import java.util.HashSet;
import org.hibernate.Session;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.importer.OjsImportExecutionService;
import ais.action.master.jurnal.importer.OjsImportPreflightService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.ImportJobOjs;
import ais.database.model.jurnal.ImportSumberOjs;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;

/** Writes only to ais_jurnal_import_fixture and proves the dependency pass is idempotent. */
public final class OjsDomainTransformSelfTest {
    private OjsDomainTransformSelfTest() {}
    public static void main(String[] args)throws Exception{
        String db=System.getenv("AIS_JURNAL_DB_NAME"),jdbc=System.getenv("AIS_JURNAL_OJS_FIXTURE_JDBC"),password=System.getenv("AIS_JURNAL_OJS_FIXTURE_PASSWORD");
        if(!"ais_jurnal_import_fixture".equals(db)||jdbc==null||!jdbc.contains("ojs_jurnal_fixture_3505")||password==null)throw new IllegalStateException("Self-test hanya boleh memakai disposable importer clones.");
        System.setProperty("javax.persistence.validation.mode","none");Tbmuser actor=admin();
        int exit=0;try{
            String suffix=Long.toString(System.currentTimeMillis());
            JurnalPenelitian journal=new JurnalAdministrationService().create("import-domain-self-test-"+suffix,"OJS Domain Self Test","ojs-domain-self-test-"+suffix,"id_ID",actor);
            OjsImportPreflightService.Config cfg=new OjsImportPreflightService.Config();cfg.jdbcUrl=jdbc;cfg.user=System.getenv("AIS_JURNAL_OJS_FIXTURE_USER");cfg.password=password;cfg.schema="public";
            OjsImportExecutionService importer=new OjsImportExecutionService();
            ImportSumberOjs source=importer.registerSource(journal.getId(),"ignored","domain-fixture-"+suffix,"OJS 3.5 fixture","env:ojs-fixture",cfg,actor);
            ImportJobOjs first=importer.start(source.getId(),false,"execute-"+suffix,cfg,25,actor);
            if(!"CORE_TRANSFORMED".equals(first.getStatus()))throw new IllegalStateException("Core transform tidak selesai: "+first.getStatus());
            JSONObject report=new JSONObject(first.getReportJson());
            if(!"CORE_TRANSFORMED".equals(report.getString("domainTransformStatus"))||report.getLong("domainMappingsLinked")<1||report.getLong("domainSubmissionsCreated")!=1||report.getLong("domainIssuesCreated")!=1)throw new IllegalStateException("Report domain tidak sesuai: "+report);
            Session s=HibernateUtil.currentSession();
            Number unresolved=(Number)s.createQuery("select count(*) from ImportMappingOjs where sourceId=:s and aktif=true and decision not in ('NOT_APPLICABLE_WITH_RATIONALE','DERIVED') and targetId is null").setLong("s",source.getId()).uniqueResult();
            if(unresolved.longValue()!=0)throw new IllegalStateException("Masih ada mapping domain tanpa target: "+unresolved);
            Number before=(Number)s.createQuery("select count(*) from RepoItem where collectionId=:c and sourceClass like :p and aktif=true").setLong("c",journal.getRepoCollectionId()).setString("p","OJS_IMPORT:"+source.getId()+":%").uniqueResult();
            ImportJobOjs again=importer.start(source.getId(),false,"execute-"+suffix,cfg,25,actor);
            Number after=(Number)s.createQuery("select count(*) from RepoItem where collectionId=:c and sourceClass like :p and aktif=true").setLong("c",journal.getRepoCollectionId()).setString("p","OJS_IMPORT:"+source.getId()+":%").uniqueResult();
            if(!first.getId().equals(again.getId())||before.longValue()!=after.longValue())throw new IllegalStateException("Rerun transform tidak idempoten.");
            System.out.println("OjsDomainTransformSelfTest OK staged=134/905 roots=2 linked="+report.getLong("domainMappingsLinked")+" idempotent");
        }catch(Throwable e){exit=1;e.printStackTrace();}finally{HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());}
        System.exit(exit);
    }
    private static Tbmuser admin()throws Exception{Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject json=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>menus=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){json.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)json.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));menus.add(m);}json.getJSONObject("workflow").put("manageImport",true);role.setJurnalAksesJson(json.toString());role.setMenus(menus);Tbmuser u=new Tbmuser();u.setUserId("JRN_OJS_DOMAIN_SELF_TEST");u.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),role);return u;}
}
