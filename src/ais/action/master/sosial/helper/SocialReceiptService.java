package ais.action.master.sosial.helper;
import java.util.Date; import org.hibernate.Session; import org.hibernate.criterion.Restrictions; import ais.database.model.sosial.*;
/**
 * Helper modul sosial (donasi) untuk memastikan setiap transaksi donasi ({@link TransaksiDonasi})
 * memiliki tepat satu bukti setor ({@link BuktiSetorSosial}), dibuat lazy (hanya saat dibutuhkan)
 * dan idempoten.
 */
public final class SocialReceiptService {
	/**
	 * Mengembalikan bukti setor yang sudah ada untuk transaksi donasi {@code d}, atau membuat baru
	 * bila belum ada (dicari lewat {@code Restrictions.eq("transaction", d)}, sehingga aman
	 * dipanggil berulang tanpa membuat bukti setor dobel). Bukti setor baru diberi nomor referensi
	 * dan token verifikasi acak dari {@code SocialSecurity}, versi template {@code "SOCIAL-V1"},
	 * status {@code "VALID"} dan status pengiriman {@code "AVAILABLE"}; setelah disimpan, status
	 * bukti setor pada transaksi induk ({@code d.setReceiptStatus("AVAILABLE")}) turut diperbarui.
	 * Penyimpanan dilakukan lewat {@code Session} yang diberikan pemanggil — transaksi Hibernate
	 * (begin/commit) menjadi tanggung jawab pemanggil.
	 *
	 * @param s sesi Hibernate aktif milik pemanggil
	 * @param c konteks permintaan sosial (menyediakan {@code tenantKey} dan id aktor pembuat)
	 * @param d transaksi donasi yang bukti setornya dicari/dibuat
	 * @return bukti setor ({@link BuktiSetorSosial}) yang sudah ada atau baru dibuat, tidak pernah {@code null}
	 */
	public BuktiSetorSosial createIfMissing(Session s,SocialRequestContext c,TransaksiDonasi d){BuktiSetorSosial r=(BuktiSetorSosial)s.createCriteria(BuktiSetorSosial.class).add(Restrictions.eq("transaction",d)).setMaxResults(1).uniqueResult();if(r!=null)return r;r=new BuktiSetorSosial();r.setTenantKey(c.getTenantKey());r.setTransaction(d);r.setReceiptNumber(SocialSecurity.reference("BS"));r.setReceiptType(d.getFundType().getReceiptType());r.setTemplateVersion("SOCIAL-V1");r.setVerificationToken(SocialSecurity.token(32));r.setGeneratedAt(new Date());r.setDeliveryStatus("AVAILABLE");r.setVoided(Boolean.FALSE);r.setStatus("VALID");r.setCreatedBy(c.getActorId());s.save(r);d.setReceiptStatus("AVAILABLE");return r;} }
