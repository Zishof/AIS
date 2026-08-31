package ais.action.master.sosial.helper;

import java.math.BigDecimal; import java.util.ArrayList; import java.util.List; import org.hibernate.Session; import org.hibernate.criterion.Order; import org.hibernate.criterion.Restrictions; import org.json.JSONObject;
import ais.database.hibernate.HibernateUtil; import ais.database.model.sosial.*;

/**
 * Layanan baca (read-only) untuk halaman publik Portal Sosial: menyajikan daftar program donasi
 * yang dipublikasikan, jenis dana/zakat aktif, ringkasan transparansi keuangan, riwayat donasi
 * donatur yang sedang login, status pembayaran satu transaksi, dan verifikasi bukti setor lewat
 * token. Semua query di-scope per tenant ({@code SocialRequestContext#getTenantKey()}) sehingga
 * data antar-institusi/cabang tidak tercampur.
 *
 * <p>
 * Setiap method membuka {@link Session} Hibernate baru sendiri dan menutupnya di blok
 * {@code finally} lewat {@link #close(Session)} — tidak menggunakan sesi thread-local bersama,
 * sehingga aman dipanggil dari konteks di luar siklus request-response ZK biasa (mis. servlet
 * API publik).
 * </p>
 */
public final class SocialPortalService {
 /** Mengembalikan hingga {@code limit} (dibatasi 1-100) program donasi terpublikasi milik tenant, diurutkan unggulan lalu terbaru. */
 @SuppressWarnings("unchecked") public List<SocialProgramView> programs(SocialRequestContext c,int limit){Session s=null;try{s=HibernateUtil.openSession();List<SocialProgramExtension> rows=s.createCriteria(SocialProgramExtension.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("publicStatus","PUBLISHED")).addOrder(Order.desc("featured")).addOrder(Order.desc("publishedAt")).setMaxResults(Math.min(Math.max(limit,1),100)).list();List<SocialProgramView> out=new ArrayList<SocialProgramView>();for(SocialProgramExtension e:rows)out.add(view(s,e));return out;}finally{close(s);}}
 /** Mengembalikan jenis dana sosial yang aktif untuk tampilan publik milik tenant, terurut nama. */
 @SuppressWarnings("unchecked") public List<JenisDanaSosial> funds(SocialRequestContext c){Session s=null;try{s=HibernateUtil.openSession();return s.createCriteria(JenisDanaSosial.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.or(Restrictions.isNull("publicActive"),Restrictions.eq("publicActive",Boolean.TRUE))).addOrder(Order.asc("nama")).list();}finally{close(s);}}
 /** Mengembalikan jenis zakat yang aktif untuk tampilan publik milik tenant, terurut sesuai {@code sortOrder}. */
 @SuppressWarnings("unchecked") public List<JenisZakat> zakatTypes(SocialRequestContext c){Session s=null;try{s=HibernateUtil.openSession();return s.createCriteria(JenisZakat.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.or(Restrictions.isNull("active"),Restrictions.eq("active",Boolean.TRUE))).addOrder(Order.asc("sortOrder")).list();}finally{close(s);}}
 /** Mengembalikan detail satu program donasi terpublikasi berdasarkan {@code slug}-nya, atau {@code null} bila tidak ditemukan. */
 public SocialProgramView program(SocialRequestContext c,String slug){Session s=null;try{s=HibernateUtil.openSession();SocialProgramExtension e=(SocialProgramExtension)s.createCriteria(SocialProgramExtension.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("slug",slug)).add(Restrictions.eq("publicStatus","PUBLISHED")).setMaxResults(1).uniqueResult();return e==null?null:view(s,e);}finally{close(s);}}
 /**
  * Menyusun ringkasan transparansi keuangan Portal Sosial tenant: total dana diterima
  * (settled), dialokasikan, disalurkan, sisa saldo, alokasi belum tersalurkan, dan jumlah
  * program yang sedang terpublikasi. Saldo dihitung lewat
  * {@link SocialFinancialInvariantService#snapshot(Session, String)} agar konsisten dengan
  * mekanisme pengecekan invarian keuangan modul Sosial.
  */
 public JSONObject transparency(SocialRequestContext c){Session s=null;try{s=HibernateUtil.openSession();SocialFinancialInvariantService.Snapshot balance=new SocialFinancialInvariantService().snapshot(s,c.getTenantKey());Long programs=(Long)s.createQuery("select count(e.id) from SocialProgramExtension e where e.tenantKey=:tenant and e.publicStatus='PUBLISHED'").setString("tenant",c.getTenantKey()).uniqueResult();JSONObject o=new JSONObject();try{o.put("received",balance.getNetSettled()).put("allocated",balance.getAllocated()).put("distributed",balance.getDistributed()).put("balance",balance.getAllocationAvailable()).put("unallocated",balance.getUnallocated()).put("programs",programs==null?0:programs.longValue());}catch(Exception ignored){}return o;}finally{close(s);}}
 /**
  * Mengembalikan hingga {@code limit} (dibatasi 1-100) riwayat transaksi donasi milik donatur
  * yang sedang login, terurut terbaru dulu.
  *
  * @throws SecurityException bila {@code c} belum terautentikasi
  */
 @SuppressWarnings("unchecked") public List<TransaksiDonasi> history(SocialRequestContext c,int limit){if(!c.isAuthenticated())throw new SecurityException("Login diperlukan.");Session s=null;try{s=HibernateUtil.openSession();SocialDonorIdentity i=(SocialDonorIdentity)s.createCriteria(SocialDonorIdentity.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("tbmuser",c.getUser())).setMaxResults(1).uniqueResult();if(i==null)return new ArrayList<TransaksiDonasi>();return s.createCriteria(TransaksiDonasi.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("donorIdentity",i)).addOrder(Order.desc("createdAt")).setMaxResults(Math.min(Math.max(limit,1),100)).list();}finally{close(s);}}
 /**
  * Mengembalikan pembayaran terbaru dari satu transaksi donasi berdasarkan nomor transaksinya.
  *
  * @throws SecurityException bila transaksi memiliki donatur teridentifikasi dan pemanggil
  *                            bukan pemilik transaksi tersebut
  */
 public PembayaranDonasi payment(SocialRequestContext c,String transactionNumber){Session s=null;try{s=HibernateUtil.openSession();TransaksiDonasi d=(TransaksiDonasi)s.createCriteria(TransaksiDonasi.class).add(Restrictions.eq("tenantKey",c.getTenantKey())).add(Restrictions.eq("transactionNumber",transactionNumber)).setMaxResults(1).uniqueResult();if(d==null)return null;if(d.getDonorIdentity()!=null&&(!c.isAuthenticated()||!c.getUser().getUserId().equals(d.getDonorIdentity().getTbmuser().getUserId())))throw new SecurityException("Transaksi bukan milik pengguna.");return (PembayaranDonasi)s.createCriteria(PembayaranDonasi.class).add(Restrictions.eq("transaction",d)).addOrder(Order.desc("createdAt")).setMaxResults(1).uniqueResult();}finally{close(s);}}
 /** Mencari bukti setor sosial berdasarkan token verifikasinya (mis. dari tautan/kode QR pada bukti cetak), atau {@code null} bila tidak ditemukan. */
 public BuktiSetorSosial verifyReceipt(String token){Session s=null;try{s=HibernateUtil.openSession();return (BuktiSetorSosial)s.createCriteria(BuktiSetorSosial.class).add(Restrictions.eq("verificationToken",token)).setMaxResults(1).uniqueResult();}finally{close(s);}}
 /** Membangun {@link SocialProgramView} tampilan publik dari entitas {@code e}, termasuk menghitung total terkumpul dan tersalurkan. */
 private SocialProgramView view(Session s,SocialProgramExtension e){SocialProgramView v=new SocialProgramView();v.id=e.getId();v.fundTypeId=e.getFundType().getId();v.slug=e.getSlug();v.name=e.getProgram().getNama();v.summary=e.getShortDescription();v.story=e.getLongStory();v.cover=e.getCoverUrl();v.fundType=e.getFundType().getNama();v.target=e.getTargetAmount();v.minimum=e.getMinimumDonation();v.featured=e.getFeatured();v.restricted=e.getRestricted();v.location=e.getTargetLocation();v.beneficiaries=e.getTargetBeneficiaries();try{v.unit=e.getProgram().getSatuanKerja()==null?null:e.getProgram().getSatuanKerja().getNama();}catch(Exception ignored){}v.collected=sumProgram(s,"select sum(a.amount) from AlokasiDonasi a where a.program=:program and a.tenantKey=:tenant and a.status='POSTED'",e);v.distributed=sumProgram(s,"select sum(d.amount) from DetailPenyaluranDonasi d where d.program=:program and d.tenantKey=:tenant and d.status='POSTED'",e);v.balance=v.collected.subtract(v.distributed);return v;}
 /** Menjalankan {@code hql} ber-parameter {@code :tenant} dan mengembalikan hasilnya sebagai {@link BigDecimal}, atau nol bila hasilnya bukan angka. */
 private BigDecimal sum(Session s,String hql,SocialRequestContext c){Object x=s.createQuery(hql).setString("tenant",c.getTenantKey()).uniqueResult();return x instanceof BigDecimal?(BigDecimal)x:BigDecimal.ZERO;}
 /** Menjalankan {@code hql} ber-parameter {@code :program} dan {@code :tenant} untuk satu program, mengembalikan hasil sebagai {@link BigDecimal} atau nol. */
 private BigDecimal sumProgram(Session s,String hql,SocialProgramExtension e){Object x=s.createQuery(hql).setEntity("program",e.getProgram()).setString("tenant",e.getTenantKey()).uniqueResult();return x instanceof BigDecimal?(BigDecimal)x:BigDecimal.ZERO;}
 /** Menutup {@code s} dengan aman, mengabaikan galat penutupan bila {@code s} tidak {@code null}. */
 private void close(Session s){if(s!=null)try{s.close();}catch(Exception ignored){}}
}
