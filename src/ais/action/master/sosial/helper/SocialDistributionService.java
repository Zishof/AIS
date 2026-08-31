package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.util.Date; import org.hibernate.LockMode; import org.hibernate.Session; import org.hibernate.Transaction; import ais.database.hibernate.HibernateUtil; import ais.database.model.sosial.*;

/**
 * Layanan posting (pembukuan final) satu baris penyaluran donasi ({@link DetailPenyaluranDonasi})
 * pada modul sosial/donasi. Method {@link #post(SocialRequestContext, Long)} adalah satu-satunya
 * operasi publik: mengubah status penyaluran dari draft/disetujui menjadi {@code POSTED} setelah
 * lolos serangkaian validasi saldo dan kompatibilitas dana, sehingga nominal tersebut resmi
 * mengurangi saldo alokasi sumber ({@link AlokasiDonasi}) dan tidak dapat diubah lagi. Akses
 * dibatasi ke privilese {@code FINANCE} lewat {@link SocialPrivilegeGuard}.
 */
public final class SocialDistributionService {
 /**
  * Memposting satu penyaluran donasi. Alur: (1) memastikan aktor memiliki privilese
  * {@code FINANCE}; (2) memuat {@link DetailPenyaluranDonasi} dan mengunci barisnya
  * ({@link LockMode#UPGRADE}) serta memastikan tenant cocok; (3) bila status sudah
  * {@code POSTED} pada kedua kolom status, dianggap idempoten dan langsung dikembalikan tanpa
  * perubahan; (4) validasi transisi status lewat {@link SocialStateMachine#requireDistribution};
  * (5) memastikan referensi alokasi sumber, jenis dana, dan kategori penerima manfaat lengkap,
  * lalu mengunci baris alokasi sumber dan memastikan tenant serta jenis dana cocok dan alokasi
  * sumber sudah {@code POSTED}; (6) menghitung total nominal yang sudah terpakai
  * ({@code POSTED}) dari alokasi yang sama, memastikan nominal penyaluran ini positif dan tidak
  * membuat total terpakai melebihi saldo alokasi; (7) bila jenis dana bersifat
  * {@code restricted}, memastikan kode dana termasuk dalam daftar kode dana yang kompatibel
  * dengan kategori penerima manfaat ({@code compatibleFundCodes}, dipisah koma); (8) menandai
  * status dan status persetujuan menjadi {@code POSTED}, mencatat {@code updatedBy}, dan mengisi
  * tanggal penyaluran dengan waktu sekarang bila belum diisi. Seluruh langkah berjalan dalam satu
  * transaksi Hibernate; kegagalan apa pun (validasi maupun runtime) memicu rollback.
  *
  * @param c        konteks permintaan (aktor, tenant) untuk pengecekan privilese dan isolasi tenant
  * @param detailId id baris {@link DetailPenyaluranDonasi} yang akan diposting
  * @return baris penyaluran yang sudah berstatus {@code POSTED}
  * @throws IllegalArgumentException bila baris tidak ditemukan atau tenant tidak cocok
  * @throws IllegalStateException    bila referensi tidak lengkap, alokasi sumber belum posted,
  *                                   jenis dana tidak sama, nominal tidak valid, saldo alokasi
  *                                   terlampaui, atau kategori penerima tidak kompatibel dengan
  *                                   dana restricted
  * @throws SecurityException        bila tenant alokasi sumber tidak cocok dengan konteks
  */
 public DetailPenyaluranDonasi post(SocialRequestContext c,Long detailId){new SocialPrivilegeGuard().require(c,SocialPrivilegeGuard.FINANCE);Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();tx=s.beginTransaction();DetailPenyaluranDonasi d=(DetailPenyaluranDonasi)s.get(DetailPenyaluranDonasi.class,detailId);if(d==null||!c.getTenantKey().equals(d.getTenantKey()))throw new IllegalArgumentException("Penyaluran tidak ditemukan.");s.lock(d,LockMode.UPGRADE);if("POSTED".equals(d.getStatus())&&"POSTED".equals(d.getApprovalStatus())){tx.commit();return d;}SocialStateMachine.requireDistribution(d.getApprovalStatus(),"POSTED");AlokasiDonasi a=d.getSourceAllocation();if(a==null||d.getFundType()==null||d.getBeneficiaryCategory()==null)throw new IllegalStateException("Referensi penyaluran belum lengkap.");s.lock(a,LockMode.UPGRADE);if(!c.getTenantKey().equals(a.getTenantKey()))throw new SecurityException("Tenant alokasi tidak cocok.");if(!"POSTED".equals(a.getStatus()))throw new IllegalStateException("Sumber alokasi belum posted.");if(a.getFundType()==null||a.getFundType().getId()==null||!a.getFundType().getId().equals(d.getFundType().getId()))throw new IllegalStateException("Jenis dana penyaluran tidak sama dengan sumber alokasi.");BigDecimal used=(BigDecimal)s.createQuery("select sum(x.amount) from DetailPenyaluranDonasi x where x.sourceAllocation=:a and x.status='POSTED'").setEntity("a",a).uniqueResult();if(used==null)used=BigDecimal.ZERO;if(d.getAmount()==null||d.getAmount().signum()<=0)throw new IllegalStateException("Nominal penyaluran tidak valid.");if(used.add(d.getAmount()).compareTo(a.getAmount())>0)throw new IllegalStateException("Penyaluran melebihi saldo alokasi.");String compatible=d.getBeneficiaryCategory().getCompatibleFundCodes();if(Boolean.TRUE.equals(d.getFundType().getRestricted())&&!containsFundCode(compatible,d.getFundType().getKode()))throw new IllegalStateException("Kategori penerima tidak kompatibel dengan dana restricted.");d.setStatus("POSTED");d.setApprovalStatus("POSTED");d.setUpdatedBy(c.getActorId());if(d.getDistributionDate()==null)d.setDistributionDate(new Date());tx.commit();return d;}catch(RuntimeException e){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}throw e;}finally{if(s!=null)try{s.close();}catch(Exception ignored){}}}
 /** Mengecek apakah {@code code} (tanpa membedakan besar/kecil huruf) ada di antara nilai-nilai {@code csv} yang dipisah koma. */
 private boolean containsFundCode(String csv,String code){if(csv==null||code==null)return false;for(String value:csv.split(","))if(code.trim().equalsIgnoreCase(value.trim()))return true;return false;}
}
