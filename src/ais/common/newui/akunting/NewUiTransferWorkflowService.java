package ais.common.newui.akunting;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.action.master.akunting.helper.DaftarPengajuanTransferSearchHelper;
import ais.action.master.akunting.helper.SinkronDaftarPengajuanTransferHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.CaraPembayaranTransfer;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.ProsesTransfer;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DisposisiAlurSop;

/** Native transactional port of DaftarPengajuanTransferAction and ProsesTransferAction. */
@SuppressWarnings({"rawtypes","unchecked","deprecation"})
public final class NewUiTransferWorkflowService {
    public Snapshot load(Filter f){Session s=HibernateUtil.openSession();try{Set<SatuanKerja> allowed=SekolahUtil.ambilSatuanKerjas();Criteria c=criteria(s,f,true,allowed);Number n=(Number)criteria(s,f,false,allowed).setProjection(Projections.rowCount()).uniqueResult();Snapshot out=new Snapshot();out.total=n==null?0:n.intValue();List<DaftarPengajuanTransfer> values=c.setFirstResult(Math.max(0,f.page)*f.size).setMaxResults(f.size).list();for(DaftarPengajuanTransfer v:values){Row r=row(v);out.rows.add(r);count(out,r.status);out.amount+=r.amount;}Dashboard d=dashboard(s,f,allowed);out.waiting=d.waiting;out.submitted=d.submitted;out.transitory=d.transitory;out.transferred=d.transferred;out.allAmount=d.amount;return out;}finally{s.close();}}

    public ProcessSnapshot processes(Filter f){Session s=HibernateUtil.openSession();try{Criteria c=processCriteria(s,f,true);Number n=(Number)processCriteria(s,f,false).setProjection(Projections.rowCount()).uniqueResult();ProcessSnapshot out=new ProcessSnapshot();out.total=n==null?0:n.intValue();for(Object o:c.setFirstResult(Math.max(0,f.page)*f.size).setMaxResults(f.size).list()){ProsesTransfer p=(ProsesTransfer)o;ProcessRow r=processRow(s,p,true);out.rows.add(r);if("WAITING_APPROVAL".equals(r.status))out.waiting++;else if("APPROVED".equals(r.status))out.approved++;else if("REALIZED".equals(r.status))out.realized++;}return out;}finally{s.close();}}

    public Options options(){Session s=HibernateUtil.openSession();try{Options o=new Options();for(Object x:s.createCriteria(CaraPembayaranTransfer.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE))).add(Restrictions.isNotNull("akun")).addOrder(Order.asc("nama")).list()){CaraPembayaranTransfer v=(CaraPembayaranTransfer)x;o.methods.add(new Option(v.getId(),join(v.getKode(),v.getNama())));}for(Object x:s.createCriteria(Akun.class).addOrder(Order.asc("kode")).list()){Akun v=(Akun)x;o.accounts.add(new Option(v.getId(),join(v.getKode(),v.getNama())));}for(Object x:s.createCriteria(SatuanKerja.class).addOrder(Order.asc("nama")).list()){SatuanKerja v=(SatuanKerja)x;o.workUnits.add(new Option(v.getId(),join(v.getKode(),v.getNama())));}return o;}finally{s.close();}}

    public Long createProcess(ProcessDraft d,Tbmuser user){validate(d);Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();CaraPembayaranTransfer method=(CaraPembayaranTransfer)s.get(CaraPembayaranTransfer.class,d.methodId);if(method==null||!Boolean.TRUE.equals(method.getAktif()))throw new IllegalArgumentException("Cara transfer tidak valid atau tidak aktif.");List<DaftarPengajuanTransfer> items=s.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.in("id",d.itemIds)).setLockMode(org.hibernate.LockMode.UPGRADE).list();if(items.size()!=d.itemIds.size())throw new IllegalArgumentException("Sebagian pengajuan transfer tidak ditemukan.");double total=0;for(DaftarPengajuanTransfer item:items){if(item.getProsesTransfer()!=null)throw new IllegalStateException("Pengajuan "+item.getKode()+" sudah berada dalam proses transfer.");if(!Boolean.TRUE.equals(item.getAktif()))throw new IllegalStateException("Pengajuan "+item.getKode()+" tidak aktif.");if(DaftarPengajuanTransferSearchHelper.terminBelumDisetujui(s,item))throw new IllegalStateException("Termin "+item.getKode()+" belum disetujui.");total+=number(item.getNominal());}ProsesTransfer p=new ProsesTransfer();String code=uniqueProcessCode(s);p.setKode(code);p.setNama(clean(d.title));p.setKeterangan(clean(d.note));p.setCaraPembayaranTransfer(method);p.setTanggalPembuatan(d.date==null?new Date():d.date);p.setAktif(Boolean.TRUE);p.setNilai(total);stamp(p,user);s.save(p);for(DaftarPengajuanTransfer item:items){item.setProsesTransfer(p);s.update(item);}s.flush();tx.commit();return p.getId();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void updateProcess(ProcessDraft d,Tbmuser user){if(d.id==null)throw new IllegalArgumentException("ID proses wajib diisi.");if(clean(d.title)==null)throw new IllegalArgumentException("Judul transfer wajib diisi.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,d.id,true);if(p.getDisetujuiOleh()!=null)throw new IllegalStateException("Proses yang telah disetujui tidak dapat diubah.");CaraPembayaranTransfer m=(CaraPembayaranTransfer)s.get(CaraPembayaranTransfer.class,d.methodId);if(m==null)throw new IllegalArgumentException("Cara transfer tidak ditemukan.");p.setNama(clean(d.title));p.setKeterangan(clean(d.note));p.setCaraPembayaranTransfer(m);p.setTanggalPembuatan(d.date==null?p.getTanggalPembuatan():d.date);stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void addItems(Long processId,List<Long> ids,Tbmuser user){if(ids==null||ids.isEmpty())throw new IllegalArgumentException("Pilih minimal satu pengajuan.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,processId,true);guardDraft(p);double total=number(p.getNilai());List<DaftarPengajuanTransfer> items=s.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.in("id",ids)).setLockMode(org.hibernate.LockMode.UPGRADE).list();for(DaftarPengajuanTransfer d:items){if(d.getProsesTransfer()!=null&&!p.getId().equals(d.getProsesTransfer().getId()))throw new IllegalStateException("Pengajuan "+d.getKode()+" sudah diproses.");if(d.getProsesTransfer()==null){if(DaftarPengajuanTransferSearchHelper.terminBelumDisetujui(s,d))throw new IllegalStateException("Termin "+d.getKode()+" belum disetujui.");d.setProsesTransfer(p);s.update(d);total+=number(d.getNominal());}}p.setNilai(total);stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void removeItem(Long processId,Long itemId,Tbmuser user){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,processId,true);guardDraft(p);DaftarPengajuanTransfer d=requireItem(s,itemId,true);if(d.getProsesTransfer()==null||!p.getId().equals(d.getProsesTransfer().getId()))throw new IllegalStateException("Pengajuan tidak termasuk proses ini.");d.setProsesTransfer(null);d.setTransfer(null);d.setTransitori(null);s.update(d);p.setNilai(Math.max(0,number(p.getNilai())-number(d.getNominal())));stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void approve(Long id,Tbmuser user){if(user==null)throw new SecurityException("Pengguna aktif tidak ditemukan.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,id,true);if(p.getRealisasikanOleh()!=null)throw new IllegalStateException("Transfer sudah direalisasikan.");if(p.getDisetujuiOleh()!=null)throw new IllegalStateException("Transfer sudah disetujui.");if(itemCount(s,p.getId())==0)throw new IllegalStateException("Proses transfer tidak mempunyai pengajuan.");p.setDisetujuiOleh(user);p.setTanggalPersetujuan(new Date());stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void reject(Long id,Tbmuser user){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,id,true);if(p.getRealisasikanOleh()!=null)throw new IllegalStateException("Realisasi harus dibatalkan lebih dahulu.");p.setDisetujuiOleh(null);p.setTanggalPersetujuan(null);for(Object o:items(s,p.getId())){DaftarPengajuanTransfer d=(DaftarPengajuanTransfer)o;d.setProsesTransfer(null);d.setTransfer(null);d.setTransitori(null);s.update(d);}p.setNilai(Double.valueOf(0));stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void setProcessActive(Long id,boolean active,Tbmuser user){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,id,true);if(p.getRealisasikanOleh()!=null&&!active)throw new IllegalStateException("Proses terealisasi tidak dapat dinonaktifkan.");p.setAktif(Boolean.valueOf(active));stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void deleteProcess(Long id){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,id,true);if(p.getDisetujuiOleh()!=null||p.getRealisasikanOleh()!=null)throw new IllegalStateException("Proses yang sudah disetujui/direalisasi tidak dapat dihapus.");for(Object o:items(s,p.getId())){DaftarPengajuanTransfer d=(DaftarPengajuanTransfer)o;d.setProsesTransfer(null);d.setTransfer(null);d.setTransitori(null);s.update(d);}s.delete(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void realize(Long id,Date date,Tbmuser user){if(user==null)throw new SecurityException("Pengguna aktif tidak ditemukan.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,id,true);if(p.getDisetujuiOleh()==null)throw new IllegalStateException("Proses transfer belum disetujui.");if(p.getRealisasikanOleh()!=null)throw new IllegalStateException("Proses transfer sudah direalisasikan.");p.setRealisasikanOleh(user);p.setTanggalRealisasikan(date==null?new Date():date);stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void cancelRealization(Long id,Tbmuser user){if(user==null)throw new SecurityException("Pengguna aktif tidak ditemukan.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();ProsesTransfer p=requireProcess(s,id,true);if(p.getRealisasikanOleh()==null)throw new IllegalStateException("Proses belum direalisasikan.");if(p.getRealisasikanOleh().getUserId()!=null&&!p.getRealisasikanOleh().getUserId().equals(user.getUserId()))throw new SecurityException("Realisasi hanya dapat dibatalkan oleh realisator yang sama.");p.setRealisasikanOleh(null);p.setTanggalRealisasikan(null);stamp(p,user);s.update(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void setDisposition(Long itemId,boolean transfer,boolean transitory,Tbmuser user){if(transfer&&transitory)throw new IllegalArgumentException("Status transfer dan transitori tidak boleh aktif bersamaan.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();DaftarPengajuanTransfer d=requireItem(s,itemId,true);if(d.getProsesTransfer()==null||d.getProsesTransfer().getRealisasikanOleh()==null)throw new IllegalStateException("Status tujuan hanya dapat diubah setelah realisasi.");d.setTransfer(Boolean.valueOf(transfer));d.setTransitori(Boolean.valueOf(transitory));stamp(d,user);s.update(d);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void updateNote(Long id,String note,Tbmuser user){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();DaftarPengajuanTransfer d=requireItem(s,id,true);d.setKeterangan(clean(note));stamp(d,user);s.update(d);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public SyncResult sync(){SinkronDaftarPengajuanTransferHelper.HasilSinkron x=SinkronDaftarPengajuanTransferHelper.sinkronkanHeadless();SyncResult r=new SyncResult();r.total=x.total;r.success=x.berhasil;r.skipped=x.dilewati;r.failed=x.gagal;r.messages.addAll(x.pesan);return r;}
    public File receipt(Long id)throws Exception{Session s=HibernateUtil.openSession();try{ProsesTransfer p=requireProcess(s,id,false);Map parameters=ais.common.HashMapGenerator.getRand();parameters.put("id",p.getId());Common.insertProperty(ProsesTransfer.class,p,parameters,"data");if(p.getDisposisiSop()!=null)DisposisiAlurSop.parameterMap(p.getDisposisiSop(),parameters);List<Map> maps=new ArrayList<Map>();double total=0;for(Object o:items(s,p.getId())){DaftarPengajuanTransfer d=(DaftarPengajuanTransfer)o;Map m=new HashMap();Common.insertProperty(DaftarPengajuanTransfer.class,d,m,"data",2,"prosesTransfer");String approval=p.getDisetujuiOleh()==null?"Belum disetujui":"Disetujui oleh "+p.getDisetujuiOleh().getUserNama()+" pada "+(p.getTanggalPersetujuan()==null?"":Common.dateFormat51.get().format(p.getTanggalPersetujuan()));m.put("status_persetujuan",approval);m.put("perpustakaan",p.getKeterangan());m.put("tanggal_persetujuan",p.getTanggalPersetujuan());m.put("disetujui_oleh",p.getDisetujuiOleh()==null?"":p.getDisetujuiOleh().getUserNama());total+=number(d.getNominal());maps.add(m);}parameters.put("maps",maps);parameters.put("totalSemua",Double.valueOf(total));List<String>clear=new ArrayList<String>();for(Object k:parameters.keySet())if(String.valueOf(k).indexOf("disposisiSop")>=0)clear.add(String.valueOf(k));for(String k:clear)parameters.put(k,null);return Report.generateFileReport(Report.PDF,parameters,"akunting/pengajuan_cheque",p.getTanggalPembuatan(),(List)null,Common.locale);}finally{s.close();}}

    public List<PaymentMethodRow> paymentMethods(String q,boolean activeOnly){Session s=HibernateUtil.openSession();try{Criteria c=s.createCriteria(CaraPembayaranTransfer.class);if(activeOnly)c.add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)));if(clean(q)!=null)c.add(Restrictions.or(Restrictions.ilike("kode",q.trim(),MatchMode.ANYWHERE),Restrictions.ilike("nama",q.trim(),MatchMode.ANYWHERE)));c.addOrder(Order.asc("kode"));List<PaymentMethodRow> out=new ArrayList<PaymentMethodRow>();for(Object o:c.list())out.add(new PaymentMethodRow((CaraPembayaranTransfer)o));return out;}finally{s.close();}}
    public Long savePaymentMethod(PaymentMethodDraft d,Tbmuser user){if(clean(d.name)==null)throw new IllegalArgumentException("Nama cara transfer wajib diisi.");if(d.accountId==null)throw new IllegalArgumentException("Akun utama wajib dipilih.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();Criteria dup=s.createCriteria(CaraPembayaranTransfer.class).add(Restrictions.or(clean(d.code)==null?Restrictions.sqlRestriction("false"):Restrictions.eq("kode",d.code.trim()),Restrictions.eq("nama",d.name.trim())));if(d.id!=null)dup.add(Restrictions.ne("id",d.id));if(((Number)dup.setProjection(Projections.rowCount()).uniqueResult()).intValue()>0)throw new IllegalArgumentException("Kode atau nama cara transfer sudah digunakan.");CaraPembayaranTransfer v=d.id==null?new CaraPembayaranTransfer():(CaraPembayaranTransfer)s.get(CaraPembayaranTransfer.class,d.id);if(v==null)throw new IllegalArgumentException("Cara transfer tidak ditemukan.");Akun a=(Akun)s.get(Akun.class,d.accountId);if(a==null)throw new IllegalArgumentException("Akun utama tidak ditemukan.");v.setKode(clean(d.code)==null?"":d.code.trim());v.setNama(d.name.trim());v.setDeskripsi(clean(d.description));v.setAkun(a);v.setAkunTransitori(d.transitoryAccountId==null?null:(Akun)s.get(Akun.class,d.transitoryAccountId));v.setSatuanKerja(d.workUnitId==null?null:(SatuanKerja)s.get(SatuanKerja.class,d.workUnitId));v.setAktif(Boolean.valueOf(d.active));v.setDefaultPembayaran(Boolean.valueOf(d.makeDefault));stamp(v,user);s.saveOrUpdate(v);s.flush();if(d.makeDefault){for(Object o:s.createCriteria(CaraPembayaranTransfer.class).add(Restrictions.eq("defaultPembayaran",Boolean.TRUE)).list()){CaraPembayaranTransfer other=(CaraPembayaranTransfer)o;if(!other.getId().equals(v.getId())){other.setDefaultPembayaran(Boolean.FALSE);s.update(other);}}v.setDefaultPembayaran(Boolean.TRUE);s.update(v);}tx.commit();return v.getId();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}
    public void setPaymentMethodActive(Long id,boolean active,Tbmuser user){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();CaraPembayaranTransfer v=(CaraPembayaranTransfer)s.get(CaraPembayaranTransfer.class,id);if(v==null)throw new IllegalArgumentException("Cara transfer tidak ditemukan.");v.setAktif(Boolean.valueOf(active));stamp(v,user);s.update(v);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    private Criteria criteria(Session s,Filter f,boolean order,Set<SatuanKerja> allowed){Criteria c=s.createCriteria(DaftarPengajuanTransfer.class,"d").createAlias("prosesTransfer","p",Criteria.LEFT_JOIN);if(f.id!=null)c.add(Restrictions.idEq(f.id));if(f.start!=null)c.add(Restrictions.ge("waktu",start(f.start)));if(f.end!=null)c.add(Restrictions.lt("waktu",next(f.end)));if(f.activeOnly)c.add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)));c.add(scopeRestriction(f.workUnitId,allowed));if(clean(f.query)!=null)c.add(Restrictions.or(Restrictions.ilike("kode",f.query.trim(),MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("nama",f.query.trim(),MatchMode.ANYWHERE),Restrictions.ilike("keterangan",f.query.trim(),MatchMode.ANYWHERE))));applyStatus(c,f);applyKinds(c,f);if(order)c.addOrder(Order.desc("waktu")).addOrder(Order.desc("id"));return c;}
    /** Dibatasi ke satuan kerja yang berhak dilihat pengguna login, sama seperti initCriteria() pada DaftarPengajuanTransferAction (layar ZK pembandingnya) — TIDAK opsional berdasarkan workUnitId dari klien. workUnitId hanya boleh MENYEMPITKAN ke satu satuan kerja yang memang ada dalam allowed; di luar itu ditolak, bukan diabaikan. */
    private static Criterion scopeRestriction(Long workUnitId,Set<SatuanKerja> allowed){if(workUnitId!=null){if(!containsId(allowed,workUnitId))throw new IllegalArgumentException("Satuan kerja berada di luar akses pengguna.");return Restrictions.eq("satuanKerja.id",workUnitId);}if(allowed==null||allowed.isEmpty())return Restrictions.isNull("satuanKerja");return Restrictions.or(Restrictions.isNull("satuanKerja"),Restrictions.in("satuanKerja",allowed));}
    private static boolean containsId(Set<SatuanKerja> allowed,Long id){if(allowed==null)return false;for(SatuanKerja u:allowed)if(u.getId()!=null&&u.getId().equals(id))return true;return false;}
    private Criteria processCriteria(Session s,Filter f,boolean order){Criteria c=s.createCriteria(ProsesTransfer.class,"p");if(f.id!=null)c.add(Restrictions.idEq(f.id));if(f.start!=null)c.add(Restrictions.ge("tanggalPembuatan",start(f.start)));if(f.end!=null)c.add(Restrictions.lt("tanggalPembuatan",next(f.end)));if(f.activeOnly)c.add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE)));if(clean(f.query)!=null)c.add(Restrictions.or(Restrictions.ilike("kode",f.query.trim(),MatchMode.ANYWHERE),Restrictions.ilike("nama",f.query.trim(),MatchMode.ANYWHERE)));if(order)c.addOrder(Order.desc("id"));return c;}
    private static void applyStatus(Criteria c,Filter f){if(!f.waiting&&!f.submitted&&!f.transitory&&!f.transferred)return;org.hibernate.criterion.Disjunction or=Restrictions.disjunction();if(f.waiting)or.add(Restrictions.isNull("p.id"));if(f.submitted)or.add(Restrictions.and(Restrictions.isNotNull("p.id"),Restrictions.isNull("p.realisasikanOleh")));if(f.transitory)or.add(Restrictions.and(Restrictions.isNotNull("p.realisasikanOleh"),Restrictions.eq("transitori",Boolean.TRUE)));if(f.transferred)or.add(Restrictions.and(Restrictions.isNotNull("p.realisasikanOleh"),Restrictions.or(Restrictions.eq("transfer",Boolean.TRUE),Restrictions.and(Restrictions.or(Restrictions.isNull("transfer"),Restrictions.eq("transfer",Boolean.FALSE)),Restrictions.or(Restrictions.isNull("transitori"),Restrictions.eq("transitori",Boolean.FALSE))))));c.add(or);}
    private static void applyKinds(Criteria c,Filter f){List<org.hibernate.criterion.Criterion>x=new ArrayList<org.hibernate.criterion.Criterion>();if(f.advance)x.add(Restrictions.isNotNull("uangMuka"));if(f.accountability)x.add(Restrictions.or(Restrictions.isNotNull("pertangungjawaban"),Restrictions.isNotNull("pertangungjawabanKasBesar")));if(f.bigCash)x.add(Restrictions.isNotNull("kasBesar"));if(f.smallCash)x.add(Restrictions.or(Restrictions.isNotNull("jenisKasKecil"),Restrictions.isNotNull("penggantianKasKecil")));if(f.procurement)x.add(Restrictions.or(Restrictions.isNotNull("pembayaranPengadaanMasterAssetDetail"),Restrictions.isNotNull("saldoAwalMasterAsset")));if(f.termin)x.add(Restrictions.isNotNull("pembayaranTerminMasterAssetDetail"));if(f.downPayment)x.add(Restrictions.isNotNull("pembayaranDpMasterAssetDetail"));if(f.tax)x.add(Restrictions.isNotNull("pajak"));if(f.employee)x.add(Restrictions.isNotNull("pengajuanTransaksiPegawai"));if(f.cooperative)x.add(Restrictions.isNotNull("transaksiKoperasi"));if(!x.isEmpty()){org.hibernate.criterion.Disjunction or=Restrictions.disjunction();for(org.hibernate.criterion.Criterion z:x)or.add(z);c.add(or);}}
    private Dashboard dashboard(Session s,Filter f,Set<SatuanKerja> allowed){Filter x=f.copy();x.page=0;x.size=1;Dashboard d=new Dashboard();for(Object o:criteria(s,x,false,allowed).list()){Row r=row((DaftarPengajuanTransfer)o);if("WAITING".equals(r.status))d.waiting++;else if("SUBMITTED".equals(r.status))d.submitted++;else if("TRANSITORY".equals(r.status))d.transitory++;else d.transferred++;d.amount+=r.amount;}return d;}
    private Row row(DaftarPengajuanTransfer v){Row r=new Row();r.id=v.getId();r.code=v.getKode();r.name=v.getNama();r.note=v.getKeterangan();r.time=v.getWaktu();r.amount=number(v.getNominal());r.active=Boolean.TRUE.equals(v.getAktif());r.kind=kind(v);r.status=status(v);if(v.getAkun()!=null)r.account=join(v.getAkun().getKode(),v.getAkun().getNama());if(v.getBankSumber()!=null)r.bank=v.getBankSumber().getNama();r.accountName=v.getAtasNamaSumber();r.accountNumber=v.getNoRekSumber();if(v.getSatuanKerja()!=null){r.workUnitId=v.getSatuanKerja().getId();r.workUnit=v.getSatuanKerja().getNama();}if(v.getProsesTransfer()!=null){r.processId=v.getProsesTransfer().getId();r.processCode=v.getProsesTransfer().getKode();r.processTitle=v.getProsesTransfer().getNama();r.realizedBy=v.getProsesTransfer().getRealisasikanOleh()==null?null:v.getProsesTransfer().getRealisasikanOleh().getUserNama();}if(v.getDisposisiSop()!=null){r.sopId=v.getDisposisiSop().getId();r.sop=v.getDisposisiSop().getKeterangan();}return r;}
    private ProcessRow processRow(Session s,ProsesTransfer p,boolean details){ProcessRow r=new ProcessRow();r.id=p.getId();r.code=p.getKode();r.title=p.getNama();r.note=p.getKeterangan();r.date=p.getTanggalPembuatan();r.amount=number(p.getNilai());r.active=Boolean.TRUE.equals(p.getAktif());if(p.getCaraPembayaranTransfer()!=null){r.methodId=p.getCaraPembayaranTransfer().getId();r.method=p.getCaraPembayaranTransfer().getNama();}if(p.getDisetujuiOleh()!=null){r.approvedBy=p.getDisetujuiOleh().getUserNama();r.approvedAt=p.getTanggalPersetujuan();}if(p.getRealisasikanOleh()!=null){r.realizedBy=p.getRealisasikanOleh().getUserNama();r.realizedUserId=p.getRealisasikanOleh().getUserId();r.realizedAt=p.getTanggalRealisasikan();}r.status=p.getRealisasikanOleh()!=null?"REALIZED":p.getDisetujuiOleh()!=null?"APPROVED":"WAITING_APPROVAL";if(details)for(Object o:items(s,p.getId()))r.items.add(row((DaftarPengajuanTransfer)o));return r;}
    private static String status(DaftarPengajuanTransfer d){if(d.getProsesTransfer()==null)return"WAITING";if(d.getProsesTransfer().getRealisasikanOleh()==null)return"SUBMITTED";if(Boolean.TRUE.equals(d.getTransfer()))return"TRANSFERRED";if(Boolean.TRUE.equals(d.getTransitori()))return"TRANSITORY";return"REALIZED";}
    private static String kind(DaftarPengajuanTransfer d){if(d.getPajak()!=null)return"PAJAK";if(d.getPembayaranTerminMasterAssetDetail()!=null)return"TERMIN";if(d.getPembayaranDpMasterAssetDetail()!=null)return"DP";if(d.getPembayaranPengadaanMasterAssetDetail()!=null||d.getSaldoAwalMasterAsset()!=null)return"PENGADAAN";if(d.getUangMuka()!=null)return"UANG_MUKA";if(d.getPertangungjawaban()!=null||d.getPertangungjawabanKasBesar()!=null)return"LPJ";if(d.getKasBesar()!=null)return"KAS_BESAR";if(d.getJenisKasKecil()!=null||d.getPenggantianKasKecil()!=null)return"KAS_KECIL";if(d.getDanaTalangan()!=null)return"DANA_TALANGAN";if(d.getPengajuanTransaksiPegawai()!=null)return"PEGAWAI";if(d.getTransaksiKoperasi()!=null)return"KOPERASI";return"LAINNYA";}
    private static List items(Session s,Long id){return s.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("prosesTransfer.id",id)).addOrder(Order.asc("id")).list();}
    private static int itemCount(Session s,Long id){Number n=(Number)s.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("prosesTransfer.id",id)).setProjection(Projections.rowCount()).uniqueResult();return n==null?0:n.intValue();}
    private static ProsesTransfer requireProcess(Session s,Long id,boolean lock){ProsesTransfer p=(ProsesTransfer)s.get(ProsesTransfer.class,id,lock?org.hibernate.LockMode.UPGRADE:org.hibernate.LockMode.NONE);if(p==null)throw new IllegalArgumentException("Proses transfer tidak ditemukan.");return p;}
    private static DaftarPengajuanTransfer requireItem(Session s,Long id,boolean lock){DaftarPengajuanTransfer d=(DaftarPengajuanTransfer)s.get(DaftarPengajuanTransfer.class,id,lock?org.hibernate.LockMode.UPGRADE:org.hibernate.LockMode.NONE);if(d==null)throw new IllegalArgumentException("Pengajuan transfer tidak ditemukan.");return d;}
    private static void guardDraft(ProsesTransfer p){if(p.getDisetujuiOleh()!=null)throw new IllegalStateException("Daftar transfer tidak dapat diubah setelah persetujuan.");}
    private static String uniqueProcessCode(Session s){for(int i=0;i<20;i++){String code=Common.getGeneratedBarCode();Number n=(Number)s.createCriteria(ProsesTransfer.class).add(Restrictions.eq("kode",code)).setProjection(Projections.rowCount()).uniqueResult();if(n==null||n.intValue()==0)return code;}throw new IllegalStateException("Gagal membuat nomor proses transfer yang unik.");}
    private static void validate(ProcessDraft d){if(d==null)throw new IllegalArgumentException("Payload proses transfer kosong.");if(clean(d.title)==null)throw new IllegalArgumentException("Judul transfer wajib diisi.");if(d.methodId==null)throw new IllegalArgumentException("Cara transfer wajib dipilih.");if(d.itemIds==null||d.itemIds.isEmpty())throw new IllegalArgumentException("Pilih minimal satu pengajuan transfer.");}
    private static void stamp(ais.database.model.GeneralValueObject v,Tbmuser u){if(u!=null){v.setOleh(u.getUserNama());v.setOlehId(u.getUserId());v.setTanggal_dirubah(new Date());}}
    private static void count(Snapshot s,String status){if("WAITING".equals(status))s.waiting++;else if("SUBMITTED".equals(status))s.submitted++;else if("TRANSITORY".equals(status))s.transitory++;else s.transferred++;}
    private static Date start(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}
    private static Date next(Date d){Calendar c=Calendar.getInstance();c.setTime(start(d));c.add(Calendar.DATE,1);return c.getTime();}
    private static double number(Double n){return n==null?0:n.doubleValue();}
    private static String clean(String s){return s==null||s.trim().length()==0?null:s.trim();}
    private static String join(String a,String b){return(clean(a)==null?"":a.trim())+(clean(a)==null||clean(b)==null?"":" - ")+(clean(b)==null?"":b.trim());}
    private static void rollback(Transaction t){if(t!=null)try{t.rollback();}catch(Exception ignored){}}

    /**
     * Tipe implementasi bersarang {@link Dashboard} milik {@link NewUiTransferWorkflowService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int waiting}, {@code int submitted},
     * {@code int transitory}, {@code int transferred}, {@code double amount}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    private static final class Dashboard{int waiting,submitted,transitory,transferred;double amount;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiTransferWorkflowService} untuk filter. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long workUnitId},
     * {@code String query}, {@code Date start}, {@code Date end}, {@code boolean activeOnly}, {@code boolean
     * waiting}, {@code boolean submitted}; operasi lokal: {@code copy}(). Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class Filter{public Long id,workUnitId;public String query;public Date start,end;public boolean activeOnly=true,waiting,submitted,transitory,transferred,advance,accountability,bigCash,smallCash,procurement,termin,downPayment,tax,employee,cooperative;public int page,size=20;Filter copy(){Filter x=new Filter();x.id=id;x.workUnitId=workUnitId;x.query=query;x.start=start;x.end=end;x.activeOnly=activeOnly;x.waiting=waiting;x.submitted=submitted;x.transitory=transitory;x.transferred=transferred;x.advance=advance;x.accountability=accountability;x.bigCash=bigCash;x.smallCash=smallCash;x.procurement=procurement;x.termin=termin;x.downPayment=downPayment;x.tax=tax;x.employee=employee;x.cooperative=cooperative;return x;}}
    /**
     * Pembawa data/helper lokal milik {@link NewUiTransferWorkflowService} untuk snapshot. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code int waiting},
     * {@code int submitted}, {@code int transitory}, {@code int transferred}, {@code double amount}, {@code double
     * allAmount}, {@code List rows}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class Snapshot{public int total,waiting,submitted,transitory,transferred;public double amount,allAmount;public final List<Row>rows=new ArrayList<Row>();}
    /**
     * Pembawa data/helper lokal milik {@link NewUiTransferWorkflowService} untuk row. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long processId},
     * {@code Long workUnitId}, {@code Long sopId}, {@code String code}, {@code String name}, {@code String note},
     * {@code String kind}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class Row{public Long id,processId,workUnitId,sopId;public String code,name,note,kind,status,account,bank,accountName,accountNumber,processCode,processTitle,realizedBy,workUnit,sop;public Date time;public double amount;public boolean active;}
    /**
     * Tipe implementasi bersarang {@link ProcessSnapshot} milik {@link NewUiTransferWorkflowService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code int waiting},
     * {@code int approved}, {@code int realized}, {@code List rows}. Aturan bisnis bersama tetap berada pada kelas
     * induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class ProcessSnapshot{public int total,waiting,approved,realized;public final List<ProcessRow>rows=new ArrayList<ProcessRow>();}
    /**
     * Tipe implementasi bersarang {@link ProcessRow} milik {@link NewUiTransferWorkflowService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long methodId},
     * {@code String code}, {@code String title}, {@code String note}, {@code String method}, {@code String
     * status}, {@code String approvedBy}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class ProcessRow{public Long id,methodId;public String code,title,note,method,status,approvedBy,realizedBy,realizedUserId;public Date date,approvedAt,realizedAt;public double amount;public boolean active;public final List<Row>items=new ArrayList<Row>();}
    /**
     * Tipe implementasi bersarang {@link ProcessDraft} milik {@link NewUiTransferWorkflowService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long methodId},
     * {@code String title}, {@code String note}, {@code Date date}, {@code List itemIds}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class ProcessDraft{public Long id,methodId;public String title,note;public Date date;public List<Long>itemIds=new ArrayList<Long>();}
    /**
     * Pembawa data/helper lokal milik {@link NewUiTransferWorkflowService} untuk option. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String label}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class Option{public final Long id;public final String label;Option(Long id,String label){this.id=id;this.label=label;}}
    /**
     * Pembawa data/helper lokal milik {@link NewUiTransferWorkflowService} untuk options. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List methods}, {@code List accounts},
     * {@code List workUnits}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class Options{public final List<Option>methods=new ArrayList<Option>(),accounts=new ArrayList<Option>(),workUnits=new ArrayList<Option>();}
    /**
     * Tipe implementasi bersarang {@link SyncResult} milik {@link NewUiTransferWorkflowService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code int success},
     * {@code int skipped}, {@code int failed}, {@code List messages}. Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class SyncResult{public int total,success,skipped,failed;public final List<String>messages=new ArrayList<String>();}
    /**
     * Tipe implementasi bersarang {@link PaymentMethodRow} milik {@link NewUiTransferWorkflowService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long accountId},
     * {@code Long transitoryAccountId}, {@code Long workUnitId}, {@code String code}, {@code String name}, {@code
     * String description}, {@code String account}. Aturan bisnis bersama tetap berada pada kelas induk atau
     * service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class PaymentMethodRow{public Long id,accountId,transitoryAccountId,workUnitId;public String code,name,description,account,transitoryAccount,workUnit;public boolean active,defaultPayment;PaymentMethodRow(CaraPembayaranTransfer v){id=v.getId();code=v.getKode();name=v.getNama();description=v.getDeskripsi();active=Boolean.TRUE.equals(v.getAktif());defaultPayment=Boolean.TRUE.equals(v.getDefaultPembayaran());if(v.getAkun()!=null){accountId=v.getAkun().getId();account=join(v.getAkun().getKode(),v.getAkun().getNama());}if(v.getAkunTransitori()!=null){transitoryAccountId=v.getAkunTransitori().getId();transitoryAccount=join(v.getAkunTransitori().getKode(),v.getAkunTransitori().getNama());}if(v.getSatuanKerja()!=null){workUnitId=v.getSatuanKerja().getId();workUnit=v.getSatuanKerja().getNama();}}}
    /**
     * Tipe implementasi bersarang {@link PaymentMethodDraft} milik {@link NewUiTransferWorkflowService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiTransferWorkflowService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long accountId},
     * {@code Long transitoryAccountId}, {@code Long workUnitId}, {@code String code}, {@code String name}, {@code
     * String description}, {@code boolean active}. Aturan bisnis bersama tetap berada pada kelas induk atau
     * service yang dipanggilnya.</p>
     *
     * @see NewUiTransferWorkflowService
     */
    public static final class PaymentMethodDraft{public Long id,accountId,transitoryAccountId,workUnitId;public String code,name,description;public boolean active=true,makeDefault;}
}
