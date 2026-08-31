package ais.action.master.sosial.helper;
import java.math.BigDecimal; import org.hibernate.Session; import org.json.JSONObject; import ais.database.hibernate.HibernateUtil;
/**
 * Menyusun ringkasan statistik untuk dasbor admin modul Sosial (donasi/penyaluran) satu tenant:
 * jumlah pembayaran tertunda, ketidakcocokan pembayaran, pengecualian rekonsiliasi, penyaluran
 * yang menunggu persetujuan, saldo keuangan (settled/returned/allocated/distributed/unallocated)
 * dari {@link SocialFinancialInvariantService}, dan kegagalan pengiriman bukti setor.
 * Mewajibkan hak akses {@link SocialPrivilegeGuard#VIEW} sebelum data dibaca.
 */
public final class SocialAdminDashboardService {
	/**
	 * Memuat seluruh angka ringkasan dasbor untuk tenant pada {@code c} ke dalam satu
	 * {@link JSONObject}, membuka dan menutup sesi Hibernate sendiri secara mandiri.
	 *
	 * @param c konteks permintaan (memuat kunci tenant dan identitas pemanggil untuk pemeriksaan hak akses)
	 * @return objek JSON berisi seluruh metrik dasbor
	 * @throws IllegalStateException bila terjadi kegagalan saat mengambil data (query/kalkulasi)
	 */
	public JSONObject load(SocialRequestContext c){new SocialPrivilegeGuard().require(c,SocialPrivilegeGuard.VIEW);Session s=null;try{s=HibernateUtil.openSession();JSONObject o=new JSONObject();try{SocialFinancialInvariantService.Snapshot balance=new SocialFinancialInvariantService().snapshot(s,c.getTenantKey());o.put("pendingPayment",count(s,"select count(p.id) from PembayaranDonasi p where p.tenantKey=:t and p.paymentStatus in ('CREATED','VA_ISSUED','PENDING')",c));o.put("paymentMismatch",count(s,"select count(p.id) from PembayaranDonasi p where p.tenantKey=:t and p.paymentStatus='MISMATCH'",c));o.put("reconciliationException",count(s,"select count(r.id) from SocialPaymentReconciliation r where r.tenantKey=:t and r.status='EXCEPTION'",c));o.put("distributionPending",count(s,"select count(d.id) from DetailPenyaluranDonasi d where d.tenantKey=:t and d.approvalStatus in ('SUBMITTED','IN_REVIEW','APPROVED')",c));o.put("settledDonation",balance.getSettled()).put("returnedDonation",balance.getReturned()).put("allocatedDonation",balance.getAllocated()).put("distributedDonation",balance.getDistributed()).put("unallocated",balance.getUnallocated()).put("allocationAvailable",balance.getAllocationAvailable()).put("financialException",balance.hasException());o.put("receiptFailure",count(s,"select count(r.id) from BuktiSetorSosial r where r.tenantKey=:t and r.deliveryStatus='FAILED'",c));}catch(Exception e){throw new IllegalStateException(e);}return o;}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}} private Long count(Session s,String h,SocialRequestContext c){Object x=s.createQuery(h).setString("t",c.getTenantKey()).uniqueResult();return x instanceof Long?(Long)x:Long.valueOf(0);} }
