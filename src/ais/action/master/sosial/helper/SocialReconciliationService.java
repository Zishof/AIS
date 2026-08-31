package ais.action.master.sosial.helper;
import java.math.BigDecimal; import java.util.Date; import org.hibernate.LockMode; import org.hibernate.Session; import org.hibernate.Transaction; import org.hibernate.criterion.Restrictions; import ais.database.hibernate.HibernateUtil; import ais.database.model.sosial.*;
/**
 * Layanan rekonsiliasi pembayaran donasi sosial: mencocokkan data settlement dari gateway
 * pembayaran (jumlah diterima, biaya, tanggal, referensi settlement) terhadap satu
 * {@link ais.database.model.sosial.PembayaranDonasi} yang sudah tercatat di sistem, lalu
 * menyimpan hasilnya sebagai {@link ais.database.model.sosial.SocialPaymentReconciliation}.
 * Akses dibatasi ke aktor berperan {@link SocialPrivilegeGuard#FINANCE} lewat
 * {@link SocialPrivilegeGuard#require}. Idempoten terhadap referensi settlement yang sama:
 * pemanggilan ulang dengan {@code settlementRef} identik dan payload yang sama mengembalikan
 * record rekonsiliasi yang sudah ada tanpa membuat duplikat, tetapi melempar
 * {@link IllegalStateException} bila referensi yang sama dipakai dengan payload berbeda
 * (indikasi data settlement yang tidak konsisten). Status hasil rekonsiliasi ditentukan dari
 * selisih nominal: {@code "MATCHED"} bila diterima persis sama dengan total tagihan, atau
 * {@code "EXCEPTION"} (tipe {@code "SETTLEMENT_MISMATCH"}) bila berbeda. Baris pembayaran dikunci
 * ({@link LockMode#UPGRADE}) selama transaksi untuk mencegah rekonsiliasi ganda yang bersamaan.
 */
public final class SocialReconciliationService {
	/**
	 * Mencatat hasil settlement satu pembayaran donasi (dicari via {@code tenantKey}+{@code orderId})
	 * sebagai record rekonsiliasi baru, atau mengembalikan record lama bila referensi settlement
	 * sudah pernah diproses dengan payload identik.
	 *
	 * @param c            konteks permintaan (tenant + aktor), divalidasi punya hak akses {@code FINANCE}
	 * @param orderId      id order pada gateway, dipakai mencari {@link ais.database.model.sosial.PembayaranDonasi} terkait
	 * @param settlementRef referensi settlement dari gateway; wajib diisi dan dipakai sebagai kunci idempoten
	 * @param date         tanggal settlement
	 * @param received     nominal yang diterima (wajib &gt;= 0, maks. 2 desimal)
	 * @param fee          biaya gateway (wajib &gt;= 0 bila diisi, maks. 2 desimal; {@code null} dianggap nol)
	 * @return record {@link ais.database.model.sosial.SocialPaymentReconciliation} yang baru dibuat atau yang sudah ada
	 * @throws IllegalArgumentException bila nominal/referensi tidak valid atau pembayaran tidak ditemukan
	 * @throws IllegalStateException    bila referensi settlement yang sama dipakai dengan payload berbeda
	 */
	public SocialPaymentReconciliation record(SocialRequestContext c,String orderId,String settlementRef,Date date,BigDecimal received,BigDecimal fee){new SocialPrivilegeGuard().require(c,SocialPrivilegeGuard.FINANCE);if(received==null||received.signum()<0||received.scale()>2)throw new IllegalArgumentException("Nominal settlement tidak valid.");if(fee!=null&&(fee.signum()<0||fee.scale()>2))throw new IllegalArgumentException("Biaya settlement tidak valid.");if(settlementRef==null||settlementRef.trim().isEmpty())throw new IllegalArgumentException("Referensi settlement wajib diisi.");received=received.setScale(2);fee=fee==null?BigDecimal.ZERO:fee.setScale(2);settlementRef=settlementRef.trim();Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();tx=s.beginTransaction();PembayaranDonasi p=(PembayaranDonasi)s.createCriteria(PembayaranDonasi.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("gatewayOrderId",orderId)).setMaxResults(1).uniqueResult();if(p==null)throw new IllegalArgumentException("Payment tidak ditemukan.");s.lock(p,LockMode.UPGRADE);SocialPaymentReconciliation prior=(SocialPaymentReconciliation)s.createCriteria(SocialPaymentReconciliation.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("gateway",p.getGatewayId())).add(Restrictions.eq("settlementReference",settlementRef)).setMaxResults(1).uniqueResult();if(prior!=null){if(prior.getPayment()==null||!prior.getPayment().getId().equals(p.getId())||!sameMoney(prior.getReceivedAmount(),received)||!sameMoney(prior.getFee(),fee))throw new IllegalStateException("Referensi settlement digunakan dengan payload berbeda.");tx.commit();return prior;}SocialPaymentReconciliation r=new SocialPaymentReconciliation();r.setTenantKey(c.getTenantKey());r.setPayment(p);r.setGateway(p.getGatewayId());r.setSettlementReference(settlementRef);r.setSettlementDate(date);r.setExpectedAmount(p.getTotal());r.setReceivedAmount(received);r.setFee(fee);r.setDifference(received.subtract(p.getTotal()).setScale(2));r.setStatus(r.getDifference().signum()==0?"MATCHED":"EXCEPTION");r.setExceptionType(r.getDifference().signum()==0?null:"SETTLEMENT_MISMATCH");r.setCreatedBy(c.getActorId());s.save(r);p.setReconciliationStatus(r.getStatus());p.setSettlementBatch(settlementRef);tx.commit();return r;}catch(RuntimeException e){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}throw e;}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}} /** Membandingkan dua nominal secara nilai (bukan skala), memperlakukan {@code null} sebagai nol. */
private boolean sameMoney(BigDecimal a,BigDecimal b){BigDecimal x=a==null?BigDecimal.ZERO:a;BigDecimal y=b==null?BigDecimal.ZERO:b;return x.compareTo(y)==0;} }
