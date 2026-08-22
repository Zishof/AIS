package ais.action.master.jurnal.test;

import java.util.Date;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalAdministrationService;
import ais.action.master.jurnal.JurnalUsageAggregationService;
import ais.action.master.jurnal.JurnalReportService;
import ais.action.master.jurnal.JurnalUsageEventService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoUsageEvent;

/** Rollback-only total/country/referrer aggregation and bot filtering gate. */
public final class JurnalUsageAggregationSelfTest {
    private JurnalUsageAggregationSelfTest() {}

    public static void main(String[] args) throws Exception {
        String target=System.getenv("AIS_JURNAL_DB_NAME");
        if(target==null||target.trim().length()==0||"ais".equalsIgnoreCase(target.trim()))throw new IllegalStateException("Test wajib diarahkan ke clone.");
        System.setProperty("javax.persistence.validation.mode","none");
        Tbmuser actor=admin();Session s=HibernateUtil.currentSession();Transaction tx=s.beginTransaction();
        try{
            JurnalPenelitian journal=new JurnalAdministrationService().create("self-test","Usage Self Test","usage-self-test","id_ID",actor);
            RepoCollection collection=(RepoCollection)s.get(RepoCollection.class,journal.getRepoCollectionId());
            RepoItem item=item(collection,actor);s.save(item);s.flush();
            long now=System.currentTimeMillis();
            JurnalUsageEventService capture=new JurnalUsageEventService();
            if(!capture.recordInSession(s,item.getId(),null,"VIEW",actor,"192.0.2.1","Mozilla/5.0","https://example.org/path?private=1",new Date(now-60000L)))throw new IllegalStateException("Capture VIEW gagal.");
            if(capture.recordInSession(s,item.getId(),null,"VIEW",actor,"192.0.2.1","Mozilla/5.0","https://example.org/other",new Date(now-50000L)))throw new IllegalStateException("Dedupe VIEW gagal.");
            if(!capture.recordInSession(s,item.getId(),null,"DOWNLOAD",actor,"192.0.2.1","Mozilla/5.0","https://example.org/path",new Date(now-30000L)))throw new IllegalStateException("Capture DOWNLOAD gagal.");
            java.util.List captured=s.createQuery("from RepoUsageEvent where itemId=:i").setLong("i",item.getId()).list();for(Object row:captured)((RepoUsageEvent)row).setCountryCode("ID");
            event(s,item,"VIEW","BOT","US","crawler.invalid",new Date(now-20000L));
            event(s,item,"VIEW","HUMAN","ID","example.org",new Date(now-172800000L));
            s.flush();Date from=new Date(now-3600000L),to=new Date(now+3600000L);
            JurnalUsageAggregationService usage=new JurnalUsageAggregationService();usage.rebuildDaily(journal.getId(),from,to,actor);s.flush();
            Number rows=(Number)s.createQuery("select count(*) from AgregatPenggunaanJurnal where jurnalPenelitianId=:j").setLong("j",journal.getId()).uniqueResult();
            Number total=(Number)s.createQuery("select sum(metricValue) from AgregatPenggunaanJurnal where jurnalPenelitianId=:j and dimensionType='TOTAL'").setLong("j",journal.getId()).uniqueResult();
            if(rows.longValue()!=6L||total.intValue()!=2)throw new IllegalStateException("Agregat/deteksi bot tidak konsisten: rows="+rows+" total="+total);
            usage.rebuildDaily(journal.getId(),from,to,actor);s.flush();
            Number rerun=(Number)s.createQuery("select count(*) from AgregatPenggunaanJurnal where jurnalPenelitianId=:j").setLong("j",journal.getId()).uniqueResult();
            if(rerun.longValue()!=6L)throw new IllegalStateException("Rebuild agregat tidak idempoten.");
            JSONObject counter=new JurnalReportService().counter5(journal.getId(),from,to,actor);if(!"5".equals(counter.getJSONObject("Report_Header").getString("Release"))||counter.getJSONArray("Report_Items").length()!=2)throw new IllegalStateException("COUNTER 5 output tidak konsisten: "+counter);
            System.out.println("JurnalUsageAggregationSelfTest OK capture-dedupe total referrer bot-filter idempotent COUNTER5 rollback");
        }finally{if(tx.isActive())tx.rollback();HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());}
        System.exit(0);
    }
    private static void event(Session s,RepoItem item,String type,String agent,String country,String referrer,Date at){RepoUsageEvent e=new RepoUsageEvent();e.setItemId(item.getId());e.setEventType(type);e.setUserAgentClass(agent);e.setCountryCode(country);e.setReferrerHost(referrer);e.setOccurredAt(at);s.save(e);}
    private static RepoItem item(RepoCollection c,Tbmuser actor){RepoItem i=new RepoItem();i.setCollectionId(c.getId());i.setTenantKey(c.getTenantKey());i.setDocumentType("JOURNAL_SUBMISSION");i.setWorkflowStatus("PUBLISHED");i.setSyncStatus("PUBLISHED");i.setTitle("Usage article");i.setLanguage("id");i.setOwnerId(actor.getUserId());i.setAktif(Boolean.TRUE);i.setViewCount(0L);i.setDownloadCount(0L);i.setVersionNumber(1L);i.setOlehId(actor.getUserId());return i;}
    private static Tbmuser admin()throws Exception{Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject json=JurnalAksesKatalog.modelUntukEditor(null);HashSet<Menu>menus=new HashSet<Menu>();for(JurnalAksesKatalog.Entri e:JurnalAksesKatalog.DAFTAR){json.getJSONObject("menu").put(e.kunci,true);for(String a:JurnalAksesKatalog.AKSI_CRUD)json.getJSONObject("crud").getJSONObject(e.kunci).put(a,true);Menu m=new Menu();m.setId(Long.valueOf(2000000000L+e.child));menus.add(m);}role.setJurnalAksesJson(json.toString());role.setMenus(menus);Tbmuser u=new Tbmuser();u.setUserId("JRN_USAGE_SELF_TEST");u.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(u.getUserId(),role);return u;}
}
