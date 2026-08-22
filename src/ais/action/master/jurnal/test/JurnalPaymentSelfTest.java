package ais.action.master.jurnal.test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.action.master.jurnal.JurnalPaymentService;
import ais.action.master.jurnal.JurnalPaymentCallbackService;
import ais.common.JurnalAksesKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.LogPembayaran;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.LanggananJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoCollection;

/** Rollback-only payment reconciliation and idempotency gate. */
public final class JurnalPaymentSelfTest {
    private JurnalPaymentSelfTest() {}

    public static void main(String[] args) throws Exception {
        String target = System.getenv("AIS_JURNAL_DB_NAME");
        if (target == null || target.trim().length() == 0 || "ais".equalsIgnoreCase(target.trim()))
            throw new IllegalStateException("Test wajib diarahkan ke clone main SIT/UAT.");
        System.setProperty("javax.persistence.validation.mode", "none");
        final Tbmuser actor = admin();
        Session session = HibernateUtil.currentSession();
        Transaction tx = session.beginTransaction();
        try {
            JurnalPenelitian journal = (JurnalPenelitian) session.createQuery(
                    "from JurnalPenelitian where aktif=true order by id").setMaxResults(1).uniqueResult();
            if (journal == null) throw new IllegalStateException("Fixture jurnal existing tidak tersedia.");
            RepoCollection collection = collection(actor);
            session.save(collection); session.flush();
            journal.setRepoCollectionId(collection.getId()); journal.setTenantKey("self-test"); session.update(journal);
            final LanggananJurnal subscription = subscription(journal, collection, actor);
            session.save(subscription); session.flush();

            final JurnalPaymentService payments = new JurnalPaymentService();
            final String external = "JRN-PAYMENT-SELF-TEST";
            final String providerRef = "PROVIDER-PAYMENT-SELF-TEST";
            payments.prepare(subscription.getId(), external, actor);
            expectDenied(new Runnable() { public void run() {
                payments.settle(subscription.getId(), "WRONG-REFERENCE", new BigDecimal("125000.00"),
                        "IDR", "SANDBOX", providerRef, actor);
            }});
            expectInvalid(new Runnable() { public void run() {
                payments.settle(subscription.getId(), external, new BigDecimal("124999.00"),
                        "IDR", "SANDBOX", providerRef, actor);
            }});
            LogPembayaran first = payments.settle(subscription.getId(), external, new BigDecimal("125000.00"),
                    "IDR", "SANDBOX", providerRef, actor);
            LogPembayaran second = payments.settle(subscription.getId(), external, new BigDecimal("125000.00"),
                    "IDR", "SANDBOX", providerRef, actor);
            if (first.getId() == null || !first.getId().equals(second.getId())
                    || !"ACTIVE".equals(subscription.getStatus()) || subscription.getPaymentId() == null)
                throw new IllegalStateException("Settlement idempoten tidak konsisten.");
            expectInvalid(new Runnable() { public void run() {
                payments.markFailed(subscription.getId(), "late failure", actor);
            }});

            final LanggananJurnal callbackSubscription = subscription(journal, collection, actor);
            session.save(callbackSubscription); session.flush();
            final String callbackExternal="JRN-PAYMENT-CALLBACK-SELF-TEST";
            final String callbackReference="PROVIDER-CALLBACK-SELF-TEST";
            payments.prepare(callbackSubscription.getId(), callbackExternal, actor);
            final String secret="journal-payment-self-test-secret-32-bytes-minimum";
            JurnalPaymentCallbackService.SecretResolver resolver=new JurnalPaymentCallbackService.SecretResolver(){
                public String get(String provider){return secret;}
                public String allowedProviders(){return "SANDBOX";}
            };
            final JurnalPaymentCallbackService callbacks=new JurnalPaymentCallbackService(resolver,300000L);
            final long now=System.currentTimeMillis();
            final String signature=JurnalPaymentCallbackService.signForTest(secret,now,"SANDBOX",callbackSubscription.getId(),
                    callbackExternal,new BigDecimal("125000.00"),"IDR",callbackReference);
            expectDenied(new Runnable(){public void run(){callbacks.settle(now,"00"+signature.substring(2),callbackSubscription.getId(),
                    callbackExternal,new BigDecimal("125000.00"),"IDR","SANDBOX",callbackReference);}});
            final long stale=now-600000L;
            final String staleSignature=JurnalPaymentCallbackService.signForTest(secret,stale,"SANDBOX",callbackSubscription.getId(),
                    callbackExternal,new BigDecimal("125000.00"),"IDR",callbackReference);
            expectDenied(new Runnable(){public void run(){callbacks.settle(stale,staleSignature,callbackSubscription.getId(),
                    callbackExternal,new BigDecimal("125000.00"),"IDR","SANDBOX",callbackReference);}});
            LogPembayaran callbackFirst=callbacks.settle(now,signature,callbackSubscription.getId(),callbackExternal,
                    new BigDecimal("125000.00"),"IDR","SANDBOX",callbackReference);
            LogPembayaran callbackSecond=callbacks.settle(now,signature,callbackSubscription.getId(),callbackExternal,
                    new BigDecimal("125000.00"),"IDR","SANDBOX",callbackReference);
            if(callbackFirst.getId()==null||!callbackFirst.getId().equals(callbackSecond.getId())
                    ||!"ACTIVE".equals(callbackSubscription.getStatus()))throw new IllegalStateException("Callback idempoten tidak konsisten.");
            System.out.println("JurnalPaymentSelfTest OK mismatch-denied settlement-idempotent HMAC stale-replay-denied callback-idempotent rollback");
        } finally {
            if (tx.isActive()) tx.rollback();
            HibernateUtil.closeSession();
            Tbmuser.getUserRoleYgDipakai.remove(actor.getUserId());
        }
        System.exit(0);
    }

    private static RepoCollection collection(Tbmuser actor) {
        RepoCollection c = new RepoCollection(); c.setTenantKey("self-test"); c.setKode("payment-self-test");
        c.setNama("Payment Self Test"); c.setTipe("JOURNAL"); c.setSourceSystem("AIS");
        c.setMetadataProfileJson("{\"schemaVersion\":1}"); c.setWorkflowProfileJson("{\"schemaVersion\":1,\"reviewForms\":[]}");
        c.setAccessPolicyJson("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"paid\",\"active\":true,\"format\":\"SUBSCRIPTION\",\"price\":125000.00,\"currency\":\"IDR\"}]}");
        c.setDepositEnabled(Boolean.TRUE); c.setAktif(Boolean.TRUE); c.setOlehId(actor.getUserId()); return c;
    }

    private static LanggananJurnal subscription(JurnalPenelitian j, RepoCollection c, Tbmuser actor) {
        LanggananJurnal x = new LanggananJurnal(); x.setTenantKey("self-test"); x.setJurnalPenelitianId(j.getId());
        x.setCollectionId(c.getId()); x.setPolicyKey("paid");
        x.setPolicySnapshotJson("{\"policyKey\":\"paid\",\"active\":true,\"format\":\"SUBSCRIPTION\",\"price\":125000.00,\"currency\":\"IDR\"}");
        x.setUserId("payment-self-test-user"); x.setStartsAt(new Date(System.currentTimeMillis() - 60000L));
        x.setEndsAt(new Date(System.currentTimeMillis() + 86400000L)); x.setStatus("DRAFT");
        x.setCreatedBy(actor.getUserId()); x.setCreatedAt(new Date()); x.setUpdatedAt(new Date()); x.setAktif(Boolean.TRUE);
        return x;
    }

    private static Tbmuser admin() throws Exception {
        Tbmrole role = new Tbmrole(); role.setRoleId(Tbmrole.ADMINISTRATOR);
        JSONObject access = JurnalAksesKatalog.modelUntukEditor(null);
        access.getJSONObject("menu").put("pembayaran", true);
        access.getJSONObject("workflow").put("managePayment", true);
        role.setJurnalAksesJson(access.toString());
        Menu menu = new Menu(); menu.setId(Long.valueOf(2000460518L));
        HashSet<Menu> menus = new HashSet<Menu>(); menus.add(menu); role.setMenus(menus);
        Tbmuser user = new Tbmuser(); user.setUserId("JRN_PAYMENT_SELF_TEST"); user.setUserRole(role);
        Tbmuser.getUserRoleYgDipakai.put(user.getUserId(), role); return user;
    }

    private static void expectDenied(Runnable action) {
        boolean denied = false; try { action.run(); } catch (SecurityException expected) { denied = true; }
        if (!denied) throw new IllegalStateException("Security mismatch seharusnya ditolak.");
    }

    private static void expectInvalid(Runnable action) {
        boolean denied = false; try { action.run(); } catch (IllegalArgumentException expected) { denied = true; }
        catch (IllegalStateException expected) { denied = true; }
        if (!denied) throw new IllegalStateException("Kondisi pembayaran tidak valid seharusnya ditolak.");
    }
}
