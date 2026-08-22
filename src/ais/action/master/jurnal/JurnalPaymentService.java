package ais.action.master.jurnal;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.LogPembayaran;
import ais.database.model.Tbmuser;
import ais.database.model.jurnal.LanggananJurnal;

/**
 * Journal payment linkage on existing LogPembayaran. Gateway-specific request
 * and signature validation remain in the existing provider adapters; this
 * service owns idempotent journal reconciliation and subscription activation.
 */
public final class JurnalPaymentService {
    private final JurnalAuthorizationService auth = new JurnalAuthorizationService();

    public LanggananJurnal prepare(Long subscriptionId, String requestedReference, Tbmuser actor) {
        auth.requireWorkflow(actor, "managePayment");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); LanggananJurnal subscription = subscription(s, subscriptionId);
            auth.requireJournalScope(s, actor, subscription.getJurnalPenelitianId(), null, null, false, "SUBSCRIPTION");
            if (subscription.getPaymentId() != null || "ACTIVE".equals(subscription.getStatus()))
                throw new IllegalStateException("Langganan sudah dibayar atau aktif.");
            String reference = blank(requestedReference) ? "JRN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ENGLISH) : reference(requestedReference);
            Number duplicate = (Number) s.createQuery("select count(*) from LanggananJurnal where externalReference=:r and id<>:i and aktif=true")
                    .setString("r", reference).setLong("i", subscriptionId).uniqueResult();
            if (duplicate.longValue() > 0) throw new IllegalArgumentException("Referensi pembayaran sudah dipakai.");
            subscription.setExternalReference(reference); subscription.setStatus("PENDING_PAYMENT");
            subscription.setUpdatedAt(new Date()); s.update(subscription); if (own) tx.commit(); return subscription;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public LogPembayaran settle(Long subscriptionId, String externalReference, BigDecimal amount,
            String currency, String provider, String providerReference, Tbmuser actor) {
        auth.requireWorkflow(actor, "managePayment");
        return settleInternal(subscriptionId, externalReference, amount, currency, provider,
                providerReference, actor.getUserId(), actor, true);
    }

    /** Entry point reserved for a callback that has already passed provider authentication. */
    LogPembayaran settleVerified(Long subscriptionId, String externalReference, BigDecimal amount,
            String currency, String provider, String providerReference, String callbackAuditActor) {
        return settleInternal(subscriptionId, externalReference, amount, currency, provider,
                providerReference, callbackAuditActor, null, false);
    }

    private LogPembayaran settleInternal(Long subscriptionId, String externalReference, BigDecimal amount,
            String currency, String provider, String providerReference, String auditActor,
            Tbmuser actor, boolean requireHumanAuthorization) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2) throw new IllegalArgumentException("Nominal pembayaran tidak valid.");
        if (blank(auditActor)) throw new SecurityException("Identitas audit pembayaran tidak valid.");
        String normalizedCurrency = currency(currency); String normalizedProvider = token(provider, 80, "Provider pembayaran tidak valid.");
        String normalizedProviderReference = token(providerReference, 255, "Referensi provider tidak valid.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); LanggananJurnal subscription = subscription(s, subscriptionId);
            if (requireHumanAuthorization)
                auth.requireJournalScope(s, actor, subscription.getJurnalPenelitianId(), null, null, false, "SUBSCRIPTION");
            String expectedReference = reference(externalReference);
            if (!expectedReference.equals(subscription.getExternalReference())) throw new SecurityException("Referensi pembayaran tidak sesuai.");
            if (subscription.getPaymentId() != null) {
                LogPembayaran old = (LogPembayaran) s.get(LogPembayaran.class, subscription.getPaymentId());
                if (old != null && money(old.getNominal()).compareTo(amount) == 0) { if (own) tx.commit(); return old; }
                throw new IllegalStateException("Pembayaran sebelumnya tidak konsisten.");
            }
            JSONObject policy = object(subscription.getPolicySnapshotJson());
            BigDecimal expectedAmount = decimal(policy.opt("price"));
            String expectedCurrency = currency(policy.optString("currency", normalizedCurrency));
            if (expectedAmount.compareTo(amount) != 0 || !expectedCurrency.equals(normalizedCurrency))
                throw new IllegalArgumentException("Nominal atau mata uang tidak sesuai snapshot policy.");
            Query duplicate = s.createQuery("from LogPembayaran where validator=:v and keterangan like :marker");
            duplicate.setString("v", normalizedProvider); duplicate.setString("marker", "%\"providerReference\":\"" + escapeLike(normalizedProviderReference) + "\"%"); duplicate.setMaxResults(1);
            LogPembayaran payment = (LogPembayaran) duplicate.uniqueResult();
            if (payment == null) {
                payment = new LogPembayaran(); payment.setNominal(amount.doubleValue()); payment.setValidator(normalizedProvider);
                payment.setKeterangan(evidence(subscription, expectedReference, normalizedProviderReference, normalizedCurrency)); payment.setTanggal(new Date()); payment.setTanggal_dirubah(new Date());
                payment.setOlehId(auditActor); payment.setOleh(auditActor); s.save(payment); s.flush();
            } else if (money(payment.getNominal()).compareTo(amount) != 0) throw new IllegalStateException("Referensi provider mempunyai nominal berbeda.");
            subscription.setPaymentId(payment.getId()); subscription.setStatus("ACTIVE"); subscription.setUpdatedAt(new Date()); s.update(subscription);
            if (own) tx.commit(); return payment;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    public LanggananJurnal markFailed(Long subscriptionId, String reason, Tbmuser actor) {
        auth.requireWorkflow(actor, "managePayment"); if (blank(reason)) throw new IllegalArgumentException("Alasan kegagalan wajib diisi.");
        Session s = HibernateUtil.currentSession(); Transaction tx = s.getTransaction(); boolean own = !tx.isActive();
        try { if (own) tx.begin(); LanggananJurnal subscription = subscription(s, subscriptionId);
            auth.requireJournalScope(s, actor, subscription.getJurnalPenelitianId(), null, null, false, "SUBSCRIPTION");
            if (subscription.getPaymentId() != null) throw new IllegalStateException("Pembayaran yang sudah settle tidak dapat digagalkan.");
            subscription.setStatus("PAYMENT_FAILED"); subscription.setUpdatedAt(new Date()); s.update(subscription);
            if (own) tx.commit(); return subscription;
        } catch (RuntimeException e) { if (own && tx.isActive()) tx.rollback(); throw e; }
    }

    private static LanggananJurnal subscription(Session s, Long id) { LanggananJurnal x = (LanggananJurnal) s.get(LanggananJurnal.class, id); if (x == null || !Boolean.TRUE.equals(x.getAktif())) throw new IllegalArgumentException("Langganan tidak ditemukan."); return x; }
    private static JSONObject object(String raw) { try { return new JSONObject(raw); } catch (Exception e) { throw new IllegalArgumentException("Snapshot policy pembayaran tidak valid.", e); } }
    private static String evidence(LanggananJurnal subscription, String external, String provider, String currency) { try { JSONObject x = new JSONObject(); x.put("schemaVersion", 1); x.put("type", "JOURNAL_SUBSCRIPTION"); x.put("subscriptionId", subscription.getId()); x.put("journalId", subscription.getJurnalPenelitianId()); x.put("externalReference", external); x.put("providerReference", provider); x.put("currency", currency); x.put("status", "SETTLED"); return x.toString(); } catch (Exception e) { throw new IllegalStateException("Evidence pembayaran gagal dibentuk.", e); } }
    private static BigDecimal decimal(Object value) { try { return new BigDecimal(String.valueOf(value == null ? "0" : value)).setScale(2); } catch (Exception e) { throw new IllegalArgumentException("Harga policy tidak valid."); } }
    private static BigDecimal money(Double value) { return BigDecimal.valueOf(value == null ? 0D : value.doubleValue()).setScale(2); }
    private static String currency(String value) { String x = clean(value).toUpperCase(Locale.ENGLISH); if (!x.matches("[A-Z]{3}")) throw new IllegalArgumentException("Mata uang tidak valid."); return x; }
    private static String reference(String value) { return token(value, 255, "Referensi pembayaran tidak valid."); }
    private static String token(String value, int max, String message) { String x = clean(value); if (x.length() < 2 || x.length() > max || !x.matches("[A-Za-z0-9._:/-]+")) throw new IllegalArgumentException(message); return x; }
    private static String escapeLike(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
    private static boolean blank(String value) { return clean(value).length() == 0; }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
