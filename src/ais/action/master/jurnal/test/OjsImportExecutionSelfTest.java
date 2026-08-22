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

/** Committed only to a disposable AIS importer clone. */
public final class OjsImportExecutionSelfTest {
    private OjsImportExecutionSelfTest() {}
    public static void main(String[] args) throws Exception {
        String db=System.getenv("AIS_JURNAL_DB_NAME"),jdbc=System.getenv("AIS_JURNAL_OJS_FIXTURE_JDBC"),password=System.getenv("AIS_JURNAL_OJS_FIXTURE_PASSWORD");
        if(!"ais_jurnal_import_fixture".equals(db)||jdbc==null||!jdbc.contains("ojs_jurnal_fixture_3505")||password==null)throw new IllegalStateException("Self-test hanya boleh memakai disposable importer clones.");
        System.setProperty("javax.persistence.validation.mode","none");Tbmuser actor=admin();
        try{
            String suffix=Long.toString(System.currentTimeMillis());
            JurnalPenelitian journal=new JurnalAdministrationService().create("import-self-test","OJS Import Self Test","ojs-import-self-test-"+suffix,"id_ID",actor);
            OjsImportPreflightService.Config cfg=new OjsImportPreflightService.Config();cfg.jdbcUrl=jdbc;cfg.user=System.getenv("AIS_JURNAL_OJS_FIXTURE_USER");cfg.password=password;cfg.schema="public";
            OjsImportExecutionService importer=new OjsImportExecutionService();
            ImportSumberOjs source=importer.registerSource(journal.getId(),"ignored","fixture-"+suffix,"OJS 3.5 fixture","env:ojs-fixture",cfg,actor);
            ImportJobOjs first=importer.start(source.getId(),true,"dry-run-"+suffix,cfg,25,actor);
            if(!"STAGING_COMPLETED".equals(first.getStatus()))throw new IllegalStateException("Status staging bukan completed.");
            JSONObject report=new JSONObject(first.getReportJson());
            if(report.getInt("sourceTablesStaged")!=134||report.getInt("sourceRowsProcessed")!=134||report.getInt("sourceFieldsPreserved")!=905||report.getInt("sourceFieldsWithExplicitOutcome")!=905||!"NOT_EXECUTED".equals(report.getString("domainTransformStatus")))throw new IllegalStateException("Report staging bukan 134/905 explicit outcomes: "+report);
            ImportJobOjs again=importer.start(source.getId(),true,"dry-run-"+suffix,cfg,25,actor);
            if(!first.getId().equals(again.getId()))throw new IllegalStateException("Idempotency key membuat job kedua.");
            Session s=HibernateUtil.currentSession();Number mappings=(Number)s.createQuery("select count(*) from ImportMappingOjs where sourceId=:s and aktif=true").setLong("s",source.getId()).uniqueResult();
            if(mappings.intValue()!=905)throw new IllegalStateException("Mapping source bukan 905 setelah rerun.");
            org.hibernate.Transaction tx=s.beginTransaction();first.setStatus("CANCELLED");s.update(first);tx.commit();ImportJobOjs resumed=importer.resume(first.getId(),cfg,25,actor);if(!first.getId().equals(resumed.getId())||!"STAGING_COMPLETED".equals(resumed.getStatus()))throw new IllegalStateException("Resume checkpoint gagal.");
            System.out.println("OjsImportExecutionSelfTest OK dry-run tables=134 rows=134 fields=905 explicit=905 idempotent resume-checkpoint");
        }finally{HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());}
        System.exit(0);
    }
    private static Tbmuser admin()throws Exception{Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject json=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>menus=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){json.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)json.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));menus.add(m);}json.getJSONObject("workflow").put("manageImport",true);role.setJurnalAksesJson(json.toString());role.setMenus(menus);Tbmuser u=new Tbmuser();u.setUserId("JRN_OJS_IMPORT_SELF_TEST");u.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),role);return u;}
}
