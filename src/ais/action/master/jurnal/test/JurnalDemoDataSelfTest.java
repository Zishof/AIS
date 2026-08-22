package ais.action.master.jurnal.test;

import java.util.HashSet;
import org.hibernate.Session;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalDemoDataService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/** Persists only idempotent demo data on the explicitly named disposable SIT clone. */
public final class JurnalDemoDataSelfTest {
    private JurnalDemoDataSelfTest() {}
    public static void main(String[] args) throws Exception {
        if (!"ais_jurnal_sit".equals(System.getenv("AIS_JURNAL_DB_NAME")))
            throw new IllegalStateException("Demo generator self-test hanya boleh memakai ais_jurnal_sit.");
        System.setProperty("javax.persistence.validation.mode","none");
        Tbmuser actor=admin();int exit=0;
        try {
            String key=args.length==0?"demo-500x100":args[0];
            JurnalDemoDataService service=new JurnalDemoDataService();
            JurnalDemoDataService.Result first=service.generate(500,100,Long.valueOf(245L),JurnalDemoDataService.DEFAULT_AUTHOR,key,JurnalDemoDataService.CONFIRMATION,actor);
            JurnalDemoDataService.Result second=service.generate(500,100,Long.valueOf(245L),JurnalDemoDataService.DEFAULT_AUTHOR,key,JurnalDemoDataService.CONFIRMATION,actor);
            Session s=HibernateUtil.currentSession();String source="AIS_JOURNAL_DEMO:"+key;
            Number journals=(Number)s.createQuery("select count(*) from RepoCollection where kode like :p and tipe='JOURNAL' and aktif=true").setString("p","demo-"+key+"-%").uniqueResult();
            Number articles=(Number)s.createQuery("select count(*) from RepoItem where sourceClass=:s and documentType='JOURNAL_SUBMISSION' and aktif=true").setString("s",source).uniqueResult();
            Number published=(Number)s.createQuery("select count(*) from RepoItem where sourceClass=:s and workflowStatus='PUBLISHED' and aktif=true").setString("s",source).uniqueResult();
            Number drafts=(Number)s.createQuery("select count(*) from RepoItem where sourceClass=:s and workflowStatus='DRAFT' and aktif=true").setString("s",source).uniqueResult();
            Number contributors=(Number)s.createQuery("select count(*) from RepoItemContributor c where exists (select 1 from RepoItem i where i.id=c.itemId and i.sourceClass=:s and i.aktif=true) and c.aktif=true").setString("s",source).uniqueResult();
            check(journals.intValue()==500,"Jumlah jurnal demo bukan 500.");check(articles.intValue()==50000,"Jumlah artikel demo bukan 50.000.");check(published.longValue()>0&&drafts.longValue()>0,"Status published/draft tidak tersedia.");check(contributors.longValue()==50000,"Kontributor artikel tidak lengkap.");check(second.journalsCreated==0&&second.articlesCreated==0&&second.contributorsCreated==0,"Rerun generator tidak idempoten.");
            System.out.println("JurnalDemoDataSelfTest OK journals=500 articles=50000 published="+published+" drafts="+drafts+" author="+first.authorName+" firstMs="+first.elapsedMillis+" idempotentMs="+second.elapsedMillis);
        } catch(Throwable e){exit=1;e.printStackTrace();} finally {HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());}
        System.exit(exit);
    }
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static Tbmuser admin()throws Exception{Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject json=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>menus=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){json.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)json.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));menus.add(m);}role.setJurnalAksesJson(json.toString());role.setMenus(menus);Tbmuser u=new Tbmuser();u.setUserId("JRN_DEMO_DATA_SELF_TEST");u.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),role);return u;}
}
