package ais.action.master.jurnal;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.LanggananJurnal;
import ais.database.model.jurnal.RentangIpLanggananJurnal;
import ais.database.model.penelitiandanpengabdian.JurnalPenelitian;
import ais.database.model.repository.RepoCollection;
import ais.database.model.repository.RepoItem;

/** Evaluator open/embargo/subscription/institution-IP dengan policy snapshot immutable. */
public final class JurnalAccessService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();
    /**
     * Tipe implementasi bersarang {@link Decision} milik {@link JurnalAccessService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link JurnalAccessService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code boolean allowed}, {@code String
     * reason}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see JurnalAccessService
     */
    public static final class Decision { public final boolean allowed; public final String reason; Decision(boolean a, String r) { allowed = a; reason = r; } }

    public LanggananJurnal activate(Long journalId, Long collectionId, String policyKey, String userId,
            String institutionType, Long institutionId, Date starts, Date ends, Long paymentId,
            String ignoredTenant, Tbmuser actor) {
        auth.requireWorkflow(actor, "manageSubscription");
        if (starts == null || ends == null || !ends.after(starts)) throw new IllegalArgumentException("Periode langganan tidak valid.");
        boolean userTarget = !blank(userId); boolean institutionTarget = institutionId != null;
        if (userTarget == institutionTarget) throw new IllegalArgumentException("Langganan harus ditujukan tepat ke satu pengguna atau institusi.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin();
            RepoCollection collection = (RepoCollection) s.get(RepoCollection.class, collectionId);
            JurnalPenelitian journal = (JurnalPenelitian) s.get(JurnalPenelitian.class, journalId);
            if (collection == null || journal == null || !"JOURNAL".equalsIgnoreCase(collection.getTipe())
                    || journal.getRepoCollectionId() == null || !journal.getRepoCollectionId().equals(collectionId))
                throw new IllegalArgumentException("Jurnal tidak ditemukan atau scope tidak konsisten.");
            auth.requireJournalScope(s, actor, journalId, null, null, false, "SUBSCRIPTION");
            JSONObject policy = findPolicy(collection.getAccessPolicyJson(), policyKey);
            LanggananJurnal subscription = new LanggananJurnal();
            subscription.setTenantKey(blank(journal.getTenantKey()) ? collection.getTenantKey() : journal.getTenantKey());
            subscription.setJurnalPenelitianId(journalId); subscription.setCollectionId(collectionId);
            subscription.setPolicyKey(clean(policyKey)); subscription.setPolicySnapshotJson(policy.toString());
            subscription.setUserId(userTarget ? clean(userId) : null);
            subscription.setInstitutionType(institutionTarget ? cleanNull(institutionType) : null);
            subscription.setInstitutionId(institutionId); subscription.setStartsAt(starts); subscription.setEndsAt(ends);
            subscription.setPaymentId(paymentId); subscription.setStatus(paymentRequired(policy) && paymentId == null ? "PENDING_PAYMENT" : "ACTIVE");
            subscription.setCreatedBy(actor.getUserId()); subscription.setCreatedAt(new Date()); subscription.setUpdatedAt(new Date()); subscription.setAktif(Boolean.TRUE);
            s.save(subscription); if (own) tx.commit(); return subscription;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
        catch (Exception e) { if (own && tx.isActive()) tx.rollback(); throw new IllegalArgumentException("Policy langganan tidak valid.", e); }
    }

    public Decision evaluate(RepoItem item, String userId, Long institutionId, String remoteIp, Date now) {
        if (item == null) return new Decision(false, "ITEM_NOT_FOUND"); Date at = now == null ? new Date() : now;
        if ("OPEN_ACCESS".equals(item.getAccessPolicy()) && (item.getEmbargoUntil() == null || !item.getEmbargoUntil().after(at)))
            return new Decision(true, "OPEN");
        Session s = HibernateUtil.currentSession();
        Query q = s.createQuery("from LanggananJurnal where collectionId=:c and status='ACTIVE' and aktif=true and startsAt<=:at and endsAt>:at and ((userId is not null and userId=:u) or (institutionId is not null and institutionId=:i))");
        q.setLong("c", item.getCollectionId()); q.setTimestamp("at", at); q.setString("u", clean(userId));
        q.setLong("i", institutionId == null ? -1L : institutionId.longValue()); q.setMaxResults(1);
        LanggananJurnal subscription = (LanggananJurnal) q.uniqueResult();
        if (subscription != null) return new Decision(true, subscription.getUserId() != null ? "USER_SUBSCRIPTION" : "INSTITUTION_SUBSCRIPTION");
        if (!blank(remoteIp)) {
            Query ipq = s.createQuery("select r from RentangIpLanggananJurnal r,LanggananJurnal l where r.langgananId=l.id and l.collectionId=:c and l.status='ACTIVE' and l.aktif=true and r.aktif=true and l.startsAt<=:at and l.endsAt>:at");
            ipq.setLong("c", item.getCollectionId()); ipq.setTimestamp("at", at);
            @SuppressWarnings("unchecked") List<RentangIpLanggananJurnal> ranges = ipq.list();
            for (RentangIpLanggananJurnal range : ranges)
                if (inRange(remoteIp, range.getStartAddress(), range.getEndAddress(), range.getAddressFamily())) return new Decision(true, "INSTITUTION_IP");
        }
        return new Decision(false, item.getEmbargoUntil() != null && item.getEmbargoUntil().after(at) ? "EMBARGO" : "SUBSCRIPTION_REQUIRED");
    }

    public RentangIpLanggananJurnal addRange(Long subscriptionId, String start, String end, String label, Tbmuser actor) {
        auth.requireWorkflow(actor, "manageSubscription");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); InetAddress first = literalAddress(start); InetAddress last = literalAddress(end);
            if (first.getAddress().length != last.getAddress().length || unsigned(first).compareTo(unsigned(last)) > 0)
                throw new IllegalArgumentException("Rentang IP tidak valid.");
            LanggananJurnal subscription = (LanggananJurnal) s.get(LanggananJurnal.class, subscriptionId);
            if (subscription == null || !Boolean.TRUE.equals(subscription.getAktif())) throw new IllegalArgumentException("Langganan tidak ditemukan.");
            auth.requireJournalScope(s, actor, subscription.getJurnalPenelitianId(), null, null, false, "SUBSCRIPTION");
            int family = first.getAddress().length == 4 ? 4 : 6;
            @SuppressWarnings("unchecked") List<RentangIpLanggananJurnal> existing = s.createQuery(
                    "from RentangIpLanggananJurnal where jurnalPenelitianId=:j and addressFamily=:f and aktif=true")
                    .setLong("j", subscription.getJurnalPenelitianId()).setInteger("f", family).list();
            BigInteger newStart = unsigned(first), newEnd = unsigned(last);
            for (RentangIpLanggananJurnal range : existing) {
                BigInteger oldStart = unsigned(literalAddress(range.getStartAddress()));
                BigInteger oldEnd = unsigned(literalAddress(range.getEndAddress()));
                if (newStart.compareTo(oldEnd) <= 0 && newEnd.compareTo(oldStart) >= 0)
                    throw new IllegalArgumentException("Rentang IP bertumpang tindih dengan rentang aktif jurnal.");
            }
            RentangIpLanggananJurnal range = new RentangIpLanggananJurnal(); range.setTenantKey(subscription.getTenantKey());
            range.setJurnalPenelitianId(subscription.getJurnalPenelitianId()); range.setLanggananId(subscriptionId);
            range.setAddressFamily(family); range.setStartAddress(first.getHostAddress()); range.setEndAddress(last.getHostAddress());
            range.setLabel(clean(label)); range.setCreatedBy(actor.getUserId()); range.setCreatedAt(new Date()); range.setUpdatedAt(new Date()); range.setAktif(Boolean.TRUE);
            s.save(range); if (own) tx.commit(); return range;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
        catch (Exception e) { if (own && tx.isActive()) tx.rollback(); throw new IllegalArgumentException("Rentang IP tidak valid.", e); }
    }

    private static JSONObject findPolicy(String raw, String key) throws Exception { JSONObject root = new JSONObject(raw); if (root.optInt("schemaVersion", 0) != 1) throw new Exception(); JSONArray policies = root.getJSONArray("policies"); for (int i = 0; i < policies.length(); i++) { JSONObject p = policies.getJSONObject(i); if (key.equals(p.optString("policyKey")) && p.optBoolean("active", false)) return new JSONObject(p.toString()); } throw new IllegalArgumentException("Policy langganan tidak ditemukan."); }
    private static boolean paymentRequired(JSONObject p) { return p.optDouble("price", 0) > 0; }
    private static boolean inRange(String ip, String start, String end, Integer family) { try { InetAddress a = literalAddress(ip); if ((a.getAddress().length == 4 ? 4 : 6) != (family == null ? 0 : family.intValue())) return false; BigInteger v = unsigned(a); return v.compareTo(unsigned(literalAddress(start))) >= 0 && v.compareTo(unsigned(literalAddress(end))) <= 0; } catch (Exception e) { return false; } }
    private static InetAddress literalAddress(String value) throws Exception { String x=clean(value); if(x.length()==0||x.indexOf('%')>=0||!(x.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")||x.matches("[0-9A-Fa-f:]+"))) throw new IllegalArgumentException("Alamat IP literal tidak valid."); InetAddress a=InetAddress.getByName(x); if(a.getHostAddress()==null)throw new IllegalArgumentException("Alamat IP tidak valid."); return a; }
    private static BigInteger unsigned(InetAddress a) { return new BigInteger(1, a.getAddress()); }
    private static boolean blank(String v) { return clean(v).length() == 0; }
    private static String cleanNull(String v) { return blank(v) ? null : clean(v); }
    private static String clean(String v) { return v == null ? "" : v.trim(); }
}
