package ais.common.newui.akunting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;

/** Native, transactional journal workflow derived from GrupTransaksiAction. */
@SuppressWarnings({"rawtypes","unchecked","deprecation"})
public final class NewUiJournalService {
    private static final double EPS=.005d;
    public static final String ATTACHMENT_TYPE="Bukti Transaksi Jurnal Umum";

    public Snapshot load(Filter f){Session s=HibernateUtil.openSession();try{Snapshot o=new Snapshot();Criteria count=criteria(s,f,false);Number n=(Number)count.setProjection(Projections.rowCount()).uniqueResult();o.total=n==null?0:n.intValue();for(Object v:criteria(s,f,true).setFirstResult(Math.max(0,f.page)*f.size).setMaxResults(f.size).list())o.rows.add(row(s,(GrupTransaksi)v,false));Object[]sum=(Object[])criteria(s,f,false).setProjection(Projections.projectionList().add(Projections.sum("totalDebet")).add(Projections.sum("totalKredit"))).uniqueResult();if(sum!=null){o.debit=num(sum[0]);o.credit=num(sum[1]);}return o;}finally{s.close();}}

    public Row detail(Long id){Session s=HibernateUtil.openSession();try{return row(s,require(s,id),true);}finally{s.close();}}

    public Options options(){Session s=HibernateUtil.openSession();try{Options o=new Options();for(Object v:s.createCriteria(Akun.class).addOrder(Order.asc("kode")).list()){Akun a=(Akun)v;o.accounts.add(new Option(a.getId(),join(a.getKode(),a.getNama()),null));}for(Object v:s.createCriteria(JenisTransaksi.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",true))).addOrder(Order.asc("nama")).list()){JenisTransaksi x=(JenisTransaksi)v;o.types.add(new Option(x.getId(),x.getNama(),null));}for(Object v:s.createCriteria(SatuanKerja.class).addOrder(Order.asc("nama")).list()){SatuanKerja x=(SatuanKerja)v;o.workUnits.add(new Option(x.getId(),join(x.getKode(),x.getNama()),null));}for(Object v:s.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",true))).addOrder(Order.asc("nama")).setMaxResults(2000).list()){Workspace x=(Workspace)v;o.workspaces.add(new Option(x.getId(),join(x.getKode(),x.getNama()),x.getSatuanKerja()==null?null:x.getSatuanKerja().getId()));}Date d=(Date)s.createCriteria(Closing.class).setProjection(Projections.max("tanggal")).uniqueResult();o.lastClosing=d;return o;}finally{s.close();}}

    public Long save(Draft d,Tbmuser user){validateDraft(d);Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();GrupTransaksi g=d.id==null?new GrupTransaksi():require(s,d.id);guardEditable(g);guardDate(s,d.date);Number duplicate=(Number)s.createCriteria(GrupTransaksi.class).add(Restrictions.eq("kode",d.code.trim())).add(d.id==null?Restrictions.sqlRestriction("1=1"):Restrictions.ne("id",d.id)).setProjection(Projections.rowCount()).uniqueResult();if(duplicate!=null&&duplicate.intValue()>0)throw new IllegalArgumentException("Nomor jurnal sudah digunakan.");JenisTransaksi type=(JenisTransaksi)s.get(JenisTransaksi.class,d.typeId);if(type==null)throw new IllegalArgumentException("Jenis transaksi tidak ditemukan.");SatuanKerja unit=d.workUnitId==null?null:(SatuanKerja)s.get(SatuanKerja.class,d.workUnitId);Workspace workspace=d.workspaceId==null?null:(Workspace)s.get(Workspace.class,d.workspaceId);if(d.workUnitId!=null&&unit==null)throw new IllegalArgumentException("Satuan kerja tidak ditemukan.");if(d.workspaceId!=null&&workspace==null)throw new IllegalArgumentException("Workspace anggaran tidak ditemukan.");g.setKode(d.code.trim());g.setParentCode(d.code.trim());g.setTanggalTransaksi(d.date);g.setKeterangan(clean(d.description));g.setNomorTagihan(clean(d.invoice));g.setKepada(clean(d.payee));g.setJenisTransaksi(type);g.setJenisJurnal(clean(d.journalType)==null?Transaksi.JURNAL_UMUM:d.journalType.trim());g.setSatuanKerja(unit);g.setWorkspace(workspace);g.setTbmuser(user);if(user!=null)g.setPegawai(user.ambilPegawai());Calendar c=Calendar.getInstance();c.setTime(d.date);g.setBulan(c.get(Calendar.MONTH)+1);g.setTahun(c.get(Calendar.YEAR));double debit=0,credit=0;for(LineDraft x:d.lines){validateLine(x);debit+=x.debit;credit+=x.credit;}if(debit<EPS||credit<EPS||!balanced(debit,credit))throw new IllegalArgumentException("Total debet dan kredit wajib balance dan lebih dari nol.");g.setTotalDebet(debit);g.setTotalKredit(credit);s.saveOrUpdate(g);s.flush();List<Transaksi> old=s.createCriteria(Transaksi.class).add(Restrictions.eq("grupTransaksi.id",g.getId())).list();Map<Long,Transaksi> indexed=new LinkedHashMap<Long,Transaksi>();for(Transaksi x:old)indexed.put(x.getId(),x);for(LineDraft x:d.lines){Transaksi t=x.id==null?new Transaksi():indexed.remove(x.id);if(t==null)throw new IllegalArgumentException("Detail jurnal tidak ditemukan.");Akun a=(Akun)s.get(Akun.class,x.accountId);if(a==null)throw new IllegalArgumentException("Akun detail tidak ditemukan.");t.setAkun(a);t.setKeterangan(x.description.trim());t.setDebet(x.debit);t.setKredit(x.credit);t.setMerupakanDebet(Boolean.valueOf(x.debit>EPS));t.setJumlahTransaksi(Double.valueOf(x.debit>EPS?x.debit:x.credit));t.setKode(g.getKode());t.setParentCode(g.getKode());t.setJenisJurnal(g.getJenisJurnal());t.setJenisTransaksi(type);t.setGrupTransaksi(g);t.setTanggalTransaksi(d.date);t.setTanggalDimasukkan(d.date);t.setBulan(c.get(Calendar.MONTH)+1);t.setTahun(c.get(Calendar.YEAR));t.setSimpan(true);t.setStatusPosting(Transaksi.STATUS_POSTING_BELUM);s.saveOrUpdate(t);}for(Transaksi stale:indexed.values())s.delete(stale);s.flush();tx.commit();return g.getId();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void delete(Long id){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();GrupTransaksi g=require(s,id);guardEditable(g);for(Object v:s.createCriteria(Transaksi.class).add(Restrictions.eq("grupTransaksi.id",id)).list())s.delete(v);s.delete(g);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void post(Long id,Tbmuser user,Date date,String note){if(user==null)throw new SecurityException("Pengguna aktif tidak ditemukan.");if(Common.bolehKonfigurasi("file_bukti_transaksi_jurnal_wajib_diupload",Konfigurasi.TIDAK_AKTIF)&&attachment(id)==null)throw new IllegalArgumentException("File bukti transaksi wajib diunggah sebelum posting.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();GrupTransaksi g=require(s,id);if(g.getClosing()!=null)throw new IllegalStateException("Jurnal telah masuk closing.");if(g.getPostingHistory()!=null)throw new IllegalStateException("Jurnal sudah diposting.");List<Transaksi> lines=lines(s,id);Totals totals=totals(lines);if(lines.isEmpty()||totals.debit<EPS||!balanced(totals.debit,totals.credit))throw new IllegalArgumentException("Jurnal belum balance dan tidak dapat diposting.");PostingHistory p=new PostingHistory(PostingHistory.JENIS_UMUM);p.setTbmuser(user);p.setTanggal(date==null?new Date():date);p.setTanggalPosting(date==null?new Date():date);p.setKeterangan(clean(note)==null?"Posting manual oleh "+user.getUserNama():note.trim());p.setPosting(true);s.save(p);g.setTotalDebet(totals.debit);g.setTotalKredit(totals.credit);g.setPostingHistory(p);s.update(g);for(Transaksi t:lines){t.setPostingHistory(p);t.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);t.setTanggalPosting(date==null?new Date():date);s.update(t);}s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public Attachment attachment(Long id){Session s=HibernateUtil.openSession();try{require(s,id);}finally{s.close();}LampiranLain l=LampiranLain.ambil(id,ATTACHMENT_TYPE);if(l==null)return null;Attachment a=new Attachment();a.name=l.getNama();a.link=clean(l.getLink());a.file=a.link==null?l.ambilFile():null;return a;}
    public void upload(Long id,String name,InputStream in,Tbmuser user)throws Exception{if(in==null)throw new IllegalArgumentException("File bukti wajib dipilih.");Session check=HibernateUtil.openSession();try{GrupTransaksi g=require(check,id);if(g.getClosing()!=null)throw new IllegalStateException("Lampiran jurnal closing tidak dapat diubah.");}finally{check.close();}File tmp=File.createTempFile("nui-journal-proof-",".upload");FileOutputStream out=null;try{out=new FileOutputStream(tmp);byte[]b=new byte[8192];int n;while((n=in.read(b))>=0)out.write(b,0,n);}finally{if(out!=null)out.close();}Session stream=StreamingHibernateUtil.getInstance().getSessionFactory().openSession();try{FileFotoLain.createFileFotoLain(user,stream,LampiranLain.class,false,id,ATTACHMENT_TYPE,null,tmp,name);}finally{try{stream.close();}catch(Exception ignored){}tmp.delete();}}

    public void unpost(Long id){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();GrupTransaksi g=require(s,id);if(g.getClosing()!=null)throw new IllegalStateException("Posting jurnal closing tidak dapat dibatalkan.");PostingHistory p=g.getPostingHistory();if(p==null)return;if(!PostingHistory.JENIS_UMUM.equals(p.getJenis()))throw new IllegalStateException("Jurnal ini dihasilkan modul \""+p.getJenis()+"\". Batalkan postingnya dari layar modul tersebut. Pembatalan dari layar Jurnal hanya melepas cap pada jurnalnya, tidak pada dokumen sumbernya, sehingga dokumen itu akan tetap tercatat sudah diposting.");List<Transaksi> lines=lines(s,id);g.setPostingHistory(null);s.update(g);for(Transaksi t:lines){t.setPostingHistory(null);t.setStatusPosting(Transaksi.STATUS_POSTING_BELUM);t.setTanggalPosting(null);s.update(t);}s.flush();if(bolehHapusRiwayat(s,p))s.delete(p);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public void setPostingActive(Long id,boolean value){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();GrupTransaksi g=require(s,id);if(g.getPostingHistory()==null)throw new IllegalArgumentException("Jurnal belum mempunyai bukti posting.");PostingHistory p=g.getPostingHistory();p.setPosting(Boolean.valueOf(value));s.update(p);tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public BatchResult sync(Filter f){Session s=HibernateUtil.openSession();try{List<Long>ids=ids(criteria(s,f,false));BatchResult r=new BatchResult();for(Long id:ids){Session x=HibernateUtil.openSession();Transaction tx=null;try{tx=x.beginTransaction();GrupTransaksi g=require(x,id);Totals t=totals(lines(x,id));g.setTotalDebet(t.debit);g.setTotalKredit(t.credit);x.update(g);tx.commit();r.success++;}catch(Exception e){rollback(tx);r.errors.add(id+": "+e.getMessage());}finally{x.close();}}return r;}finally{s.close();}}

    public BatchResult postBatch(Filter f,Tbmuser user,Date date,String note){Session s=HibernateUtil.openSession();try{Criteria c=criteria(s,f,false).add(Restrictions.isNull("postingHistory"));List<Long>ids=ids(c);BatchResult r=new BatchResult();for(Long id:ids)try{post(id,user,date,note);r.success++;}catch(Exception e){r.errors.add(id+": "+e.getMessage());}return r;}finally{s.close();}}
    public BatchResult unpostBatch(Filter f){Session s=HibernateUtil.openSession();try{Criteria c=criteria(s,f,false).add(Restrictions.isNotNull("postingHistory"));List<Long>ids=ids(c);BatchResult r=new BatchResult();for(Long id:ids)try{unpost(id);r.success++;}catch(Exception e){r.errors.add(id+": "+e.getMessage());}return r;}finally{s.close();}}
    public BatchResult activateBatch(Filter f){Session s=HibernateUtil.openSession();try{Criteria c=criteria(s,f,false).add(Restrictions.isNotNull("postingHistory"));List<Long>ids=ids(c);BatchResult r=new BatchResult();for(Long id:ids)try{setPostingActive(id,true);r.success++;}catch(Exception e){r.errors.add(id+": "+e.getMessage());}return r;}finally{s.close();}}

    public MaintenanceResult cleanDuplicates(){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();MaintenanceResult r=new MaintenanceResult();r.groups=s.createSQLQuery("delete from akunting.grup_transaksi where id in (select min(id) from akunting.grup_transaksi group by kode having count(*)>1)").executeUpdate();r.lines=s.createSQLQuery("delete from akunting.transaksi where id in (select min(id) from akunting.transaksi group by grup_transaksi,akun,debet,kredit having count(*)>1)").executeUpdate();tx.commit();return r;}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}
    /**
     * Kosongkan seluruh jurnal: baris LEBIH DULU, baru grupnya.
     *
     * <p>Urutan itu bukan selera. {@code transaksi.grup_transaksi} menunjuk grup dan
     * kolomnya {@code nullable}, jadi menghapus grup lebih dulu tidak selalu ditolak basis
     * data -- yang tersisa adalah baris jurnal yang menunjuk grup yang sudah tidak ada.
     * Sebelumnya hanya grup yang dihapus, sehingga {@code lines} yang dilaporkan ke layar
     * selalu nol padahal barisnya memang tidak pernah disentuh.</p>
     */
    public MaintenanceResult deleteAll(){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();MaintenanceResult r=new MaintenanceResult();r.lines=s.createSQLQuery("delete from akunting.transaksi").executeUpdate();r.groups=s.createSQLQuery("delete from akunting.grup_transaksi").executeUpdate();tx.commit();return r;}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    public ImportResult importCsv(String csv,Tbmuser user){if(clean(csv)==null)throw new IllegalArgumentException("Isi CSV kosong.");String[]rows=csv.replace("\r","").split("\n");Map<String,Draft> drafts=new LinkedHashMap<String,Draft>();for(int i=0;i<rows.length;i++){if(clean(rows[i])==null)continue;List<String>c=csvRow(rows[i]);if(i==0&&c.size()>0&&"kode".equalsIgnoreCase(c.get(0).trim()))continue;if(c.size()<8)throw new IllegalArgumentException("Baris "+(i+1)+" harus berisi kode,tanggal,jenis_transaksi_id,keterangan,akun_id,keterangan_detail,debet,kredit.");String code=c.get(0).trim();Draft d=drafts.get(code);if(d==null){d=new Draft();d.code=code;d.date=parseDate(c.get(1));d.typeId=longValue(c.get(2),"jenis transaksi");d.description=c.get(3);d.lines=new ArrayList<LineDraft>();drafts.put(code,d);}LineDraft l=new LineDraft();l.accountId=longValue(c.get(4),"akun");l.description=c.get(5);l.debit=number(c.get(6));l.credit=number(c.get(7));d.lines.add(l);}ImportResult r=new ImportResult();for(Draft d:drafts.values())try{r.ids.add(save(d,user));r.success++;}catch(Exception e){r.errors.add(d.code+": "+e.getMessage());}return r;}

    public List<HistoryRow> history(Long id){Session s=HibernateUtil.openSession();try{require(s,id);AuditReader a=AuditReaderFactory.get(s);List<HistoryRow>out=new ArrayList<HistoryRow>();for(Object v:a.createQuery().forRevisionsOfEntity(GrupTransaksi.class,false,true).add(AuditEntity.id().eq(id)).addOrder(AuditEntity.revisionNumber().desc()).setMaxResults(100).getResultList()){Object[]x=(Object[])v;GrupTransaksi g=(GrupTransaksi)x[0];HistoryRow h=new HistoryRow();h.revision=((Number)revisionId(x[1])).intValue();h.date=a.getRevisionDate(h.revision);h.type=String.valueOf(x[2]);h.code=g.getKode();h.description=g.getKeterangan();h.debit=num(g.getTotalDebet());h.credit=num(g.getTotalKredit());h.postingId=g.getPostingHistory()==null?null:g.getPostingHistory().getId();out.add(h);}return out;}finally{s.close();}}

    public File receipt(Long id)throws Exception{Session s=HibernateUtil.openSession();try{GrupTransaksi g=require(s,id);List<Transaksi>txs=lines(s,id);Totals totals=totals(txs);if(!balanced(totals.debit,totals.credit))throw new IllegalArgumentException("Bukti hanya dapat dicetak untuk jurnal balance.");int dc=0,cc=0;for(Transaksi t:txs){if(t.getDebet()>EPS)dc++;if(t.getKredit()>EPS)cc++;}List<Map<String,Object>>maps=new ArrayList<Map<String,Object>>();List<Long>seen=new ArrayList<Long>();String accounts="";double amount=0;boolean debitSide=dc<=cc;for(Transaksi t:txs){boolean main=debitSide?t.getDebet()>EPS:t.getKredit()>EPS;if(main){Akun a=t.getAkun();if(a!=null&&!seen.contains(a.getId())){accounts+=(accounts.length()==0?"":", ")+join(a.getKode(),a.getNama());seen.add(a.getId());}amount+=debitSide?t.getDebet():t.getKredit();}else{Map<String,Object>m=new HashMap<String,Object>();m.put("akun",t.getAkun()==null?"":join(t.getAkun().getKode(),t.getAkun().getNama()));m.put("uraian",t.getKeterangan());m.put("nominal",t.getDebet()+t.getKredit());maps.add(m);}}Map p=ais.common.HashMapGenerator.getRand();p.put("tanggal",Common.dateFormat4.get().format(g.getTanggalTransaksi()));p.put("nomor",g.getKode());p.put("akun",accounts);p.put("atas_nama",g.getKepada());p.put("jenis",g.getJenisTransaksi()==null?null:g.getJenisTransaksi().getKeterangan());p.put("jumlah",Math.abs(amount));p.put("grup_id",g.getId());p.put("grup_ids",new Long[]{-1L});p.put("cak",g.getNomorTagihan());Connection conn=s.connection();p.put("koneksi_db",conn);p.put("terbilang",IndonesianNumberToWords.convert((long)Math.abs(amount)).toUpperCase());p.put("jumlahDebet",amount);p.put("maps",maps);return Report.generateFileReport(Report.PDF,p,"akunting/bukti_pengeluaran_kas",new Date(),maps,Common.locale);}finally{s.close();}}

    public File combinedReceipt(Filter f)throws Exception{Session s=HibernateUtil.openSession();try{List<GrupTransaksi>groups=criteria(s,f,true).list();if(groups.isEmpty())throw new IllegalArgumentException("Tidak ada jurnal pada filter untuk dicetak.");List<Long>groupIds=new ArrayList<Long>();List<String>codes=new ArrayList<String>();String payees="",types="",invoices="";for(GrupTransaksi g:groups){groupIds.add(g.getId());codes.add(g.getKode());if(clean(g.getKepada())!=null)payees+=(payees.length()==0?"":", ")+g.getKepada();if(g.getJenisTransaksi()!=null&&clean(g.getJenisTransaksi().getKeterangan())!=null)types+=(types.length()==0?"":", ")+g.getJenisTransaksi().getKeterangan();if(clean(g.getNomorTagihan())!=null)invoices+=(invoices.length()==0?"":", ")+g.getNomorTagihan();}List<Transaksi>txs=s.createCriteria(Transaksi.class).add(Restrictions.in("grupTransaksi.id",groupIds)).addOrder(Order.desc("debet")).addOrder(Order.asc("tanggalTransaksi")).list();int dc=0,cc=0;for(Transaksi t:txs){if(t.getDebet()>EPS)dc++;if(t.getKredit()>EPS)cc++;}boolean debitSide=dc<=cc;List<Map<String,Object>>maps=new ArrayList<Map<String,Object>>();List<Long>seen=new ArrayList<Long>();String accounts="";double amount=0;for(Transaksi t:txs){boolean main=debitSide?t.getDebet()>EPS:t.getKredit()>EPS;if(main){Akun a=t.getAkun();if(a!=null&&!seen.contains(a.getId())){accounts+=(accounts.length()==0?"":", ")+join(a.getKode(),a.getNama());seen.add(a.getId());}amount+=debitSide?t.getDebet():t.getKredit();}else{Map<String,Object>m=new HashMap<String,Object>();m.put("akun",t.getAkun()==null?"":join(t.getAkun().getKode(),t.getAkun().getNama()));m.put("uraian",t.getKeterangan());m.put("nominal",t.getDebet()+t.getKredit());maps.add(m);}}Map p=ais.common.HashMapGenerator.getRand();p.put("tanggal",Common.dateFormat4.get().format(new Date()));p.put("nomor",joinValues(codes));p.put("akun",accounts);p.put("atas_nama",payees);p.put("jenis",types);p.put("jumlah",Math.abs(amount));p.put("grup_id",-1L);p.put("grup_ids",groupIds.toArray(new Long[groupIds.size()]));p.put("cak",invoices);p.put("koneksi_db",s.connection());p.put("terbilang",IndonesianNumberToWords.convert((long)Math.abs(amount)).toUpperCase());p.put("jumlahDebet",amount);p.put("maps",maps);return Report.generateFileReport(Report.PDF,p,"akunting/bukti_pengeluaran_kas",new Date(),maps,Common.locale);}finally{s.close();}}

    private Criteria criteria(Session s,Filter f,boolean order){Criteria c=s.createCriteria(GrupTransaksi.class,"g");if(f.id!=null)c.add(Restrictions.idEq(f.id));if(clean(f.query)!=null)c.add(Restrictions.or(Restrictions.ilike("kode",f.query.trim(),MatchMode.ANYWHERE),Restrictions.ilike("keterangan",f.query.trim(),MatchMode.ANYWHERE)));if(f.start!=null)c.add(Restrictions.ge("tanggalTransaksi",start(f.start)));if(f.end!=null)c.add(Restrictions.lt("tanggalTransaksi",next(f.end)));if(f.typeId!=null)c.add(Restrictions.eq("jenisTransaksi.id",f.typeId));if(f.workUnitId!=null)c.add(Restrictions.eq("satuanKerja.id",f.workUnitId));if(f.workspaceId!=null)c.add(Restrictions.eq("workspace.id",f.workspaceId));if(clean(f.journalType)!=null)c.add(Restrictions.eq("jenisJurnal",f.journalType.trim()));if(f.min!=null)c.add(Restrictions.ge("totalDebet",f.min));if(f.max!=null)c.add(Restrictions.le("totalDebet",f.max));if(f.postingId!=null)c.add(Restrictions.eq("postingHistory.id",f.postingId));if(Boolean.TRUE.equals(f.posted))c.add(Restrictions.isNotNull("postingHistory"));else if(Boolean.FALSE.equals(f.posted))c.add(Restrictions.isNull("postingHistory"));if(f.unbalanced)c.add(Restrictions.sqlRestriction("coalesce(total_debet,0) <> coalesce(total_kredit,0)"));if(f.unpostedActive)c.createAlias("postingHistory","ph",Criteria.LEFT_JOIN).add(Restrictions.or(Restrictions.isNull("ph.id"),Restrictions.eq("ph.posting",false)));if(f.closingId!=null)c.add(Restrictions.eq("closing.id",f.closingId));else if(f.closingBefore!=null)c.add(Restrictions.le("tanggalTransaksi",f.closingBefore)).add(Restrictions.isNull("closing"));else if(f.closed!=null)c.add(f.closed.booleanValue()?Restrictions.isNotNull("closing"):Restrictions.isNull("closing"));if(f.postedActive!=null){if(!f.unpostedActive)c.createAlias("postingHistory","ph",Criteria.LEFT_JOIN);c.add(f.postedActive.booleanValue()?Restrictions.and(Restrictions.isNotNull("ph.id"),Restrictions.eq("ph.posting",true)):Restrictions.or(Restrictions.isNull("ph.id"),Restrictions.eq("ph.posting",false))).add(Restrictions.isNull("closing"));}if(clean(f.sourceEntity)!=null&&validSource(f.sourceEntity))c.add(Restrictions.isNotNull(f.sourceEntity));if(clean(f.ref)!=null)c.add(refRestriction(f.ref));if(clean(f.kind)!=null&&validKind(f.kind))c.add(Restrictions.sqlRestriction("jenis='"+f.kind+"'"));boolean contextual=clean(f.sourceEntity)!=null||f.closingId!=null||f.closingBefore!=null||f.closed!=null;if(!f.allJournals&&!contextual)c.add(Restrictions.or(Restrictions.eq("jenisJurnal",Transaksi.JURNAL_UMUM),Restrictions.isNotNull("postingHistory")));if(clean(f.lineQuery)!=null||f.accountIds!=null&&!f.accountIds.isEmpty()){DetachedCriteria d=DetachedCriteria.forClass(Transaksi.class,"t").setProjection(Projections.property("t.grupTransaksi.id"));if(clean(f.lineQuery)!=null)d.add(Restrictions.ilike("t.keterangan",f.lineQuery.trim(),MatchMode.ANYWHERE));if(f.accountIds!=null&&!f.accountIds.isEmpty())d.add(Restrictions.in("t.akun.id",f.accountIds));c.add(Subqueries.propertyIn("id",d));if(f.accountIds!=null&&!f.accountIds.isEmpty()&&f.posted==null)c.add(Restrictions.isNotNull("postingHistory"));}if(order)c.addOrder(Order.desc("tanggalTransaksi")).addOrder(Order.desc("id"));return c;}
    private Row row(Session s,GrupTransaksi g,boolean include){Row r=new Row();r.id=g.getId();r.code=g.getKode();r.description=g.getKeterangan();r.date=g.getTanggalTransaksi();r.debit=num(g.getTotalDebet());r.credit=num(g.getTotalKredit());r.balanced=balanced(r.debit,r.credit)&&r.debit>EPS;r.journalType=g.getJenisJurnal();r.invoice=g.getNomorTagihan();r.payee=g.getKepada();if(g.getJenisTransaksi()!=null){r.typeId=g.getJenisTransaksi().getId();r.type=g.getJenisTransaksi().getNama();}if(g.getSatuanKerja()!=null){r.workUnitId=g.getSatuanKerja().getId();r.workUnit=g.getSatuanKerja().getNama();}if(g.getWorkspace()!=null){r.workspaceId=g.getWorkspace().getId();r.workspace=g.getWorkspace().toString();}if(g.getPostingHistory()!=null){r.postingId=g.getPostingHistory().getId();r.postingActive=g.getPostingHistory().getPosting();r.postingDate=g.getPostingHistory().getTanggalPosting();}if(g.getClosing()!=null){r.closingId=g.getClosing().getId();r.closing=g.getClosing().getNama();}if(g.getTbmuser()!=null)r.user=g.getTbmuser().getUserNama();try{r.hasAttachment=LampiranLain.ambil(g.getId(),ATTACHMENT_TYPE)!=null;}catch(Exception ignored){}if(include)for(Transaksi t:lines(s,g.getId())){Line l=new Line();l.id=t.getId();l.description=t.getKeterangan();l.debit=t.getDebet();l.credit=t.getKredit();l.status=t.getStatusPosting();if(t.getAkun()!=null){l.accountId=t.getAkun().getId();l.accountCode=t.getAkun().getKode();l.account=t.getAkun().getNama();}r.lines.add(l);}return r;}
    /**
     * Riwayat posting hanya boleh DIHAPUS bila dua syarat terpenuhi sekaligus.
     *
     * <p><b>Satu:</b> jenisnya {@link PostingHistory#JENIS_UMUM}. Hanya jalur jurnal umum
     * ({@code post} di sini, {@code GrupTransaksiAction}, {@code PostingTransaksiHarianAction})
     * yang membuat riwayat berjenis itu, dan ketiganya menempelkannya HANYA ke
     * {@code GrupTransaksi} beserta baris {@code Transaksi}-nya. Riwayat berjenis lain dibuat
     * mesin posting per modul dan dipegang pula oleh dokumen sumbernya -- {@code KasKecil},
     * {@code KasBesar}, {@code UangMuka}, dan puluhan entitas lain punya kolom
     * {@code posting_history} sendiri. Menghapusnya melanggar foreign key dari sisi yang
     * TIDAK terlihat dari layar jurnal, sehingga pembatalan gagal seluruhnya.
     *
     * <p><b>Dua:</b> tidak ada lagi grup maupun baris jurnal yang merujuknya
     * ({@link #stillReferenced}).</p>
     */
    private static boolean bolehHapusRiwayat(Session s,PostingHistory p){return PostingHistory.JENIS_UMUM.equals(p.getJenis())&&!stillReferenced(s,p);}
    /**
     * Riwayat posting dapat dipakai BERSAMA: posting massal memberi satu {@link PostingHistory}
     * kepada banyak dokumen. Menghapusnya selagi masih ada perujuk melanggar foreign key, dan
     * karena pembatalan berjalan dalam satu transaksi, seluruh pembatalan dokumen itu ikut batal.
     * Panggil SESUDAH cap dilepas dan di-flush, supaya hitungannya melihat keadaan terbaru.
     */
    private static boolean stillReferenced(Session s,PostingHistory p){return refs(s,GrupTransaksi.class,p)+refs(s,Transaksi.class,p)>0;}
    private static int refs(Session s,Class<?> c,PostingHistory p){Number n=(Number)s.createCriteria(c).add(Restrictions.eq("postingHistory",p)).setProjection(Projections.rowCount()).uniqueResult();return n==null?0:n.intValue();}
    private static List<Transaksi> lines(Session s,Long id){return s.createCriteria(Transaksi.class).add(Restrictions.eq("grupTransaksi.id",id)).addOrder(Order.desc("debet")).addOrder(Order.asc("id")).list();}
    private static Totals totals(List<Transaksi>v){Totals t=new Totals();for(Transaksi x:v){t.debit+=num(x.getDebet());t.credit+=num(x.getKredit());}return t;}
    private static GrupTransaksi require(Session s,Long id){GrupTransaksi g=id==null?null:(GrupTransaksi)s.get(GrupTransaksi.class,id);if(g==null)throw new IllegalArgumentException("Jurnal tidak ditemukan.");return g;}
    private static void guardEditable(GrupTransaksi g){if(g.getClosing()!=null)throw new IllegalStateException("Jurnal telah masuk closing.");if(g.getPostingHistory()!=null)throw new IllegalStateException("Batalkan posting sebelum mengubah jurnal.");}
    private static void guardDate(Session s,Date date){Date max=(Date)s.createCriteria(Closing.class).setProjection(Projections.max("tanggal")).uniqueResult();if(max!=null&&date.before(max))throw new IllegalArgumentException("Tanggal jurnal telah melewati tanggal closing "+Common.dateFormat4.get().format(max)+".");}
    private static void validateDraft(Draft d){if(d==null)throw new IllegalArgumentException("Payload jurnal kosong.");if(clean(d.code)==null)throw new IllegalArgumentException("Nomor jurnal wajib diisi.");if(d.date==null)throw new IllegalArgumentException("Tanggal transaksi wajib diisi.");if(d.typeId==null)throw new IllegalArgumentException("Jenis transaksi wajib dipilih.");if(d.lines==null||d.lines.size()<2)throw new IllegalArgumentException("Jurnal minimal mempunyai dua baris detail.");}
    private static void validateLine(LineDraft l){if(l==null||l.accountId==null)throw new IllegalArgumentException("Akun setiap detail wajib dipilih.");if(clean(l.description)==null)throw new IllegalArgumentException("Keterangan setiap detail wajib diisi.");if(l.debit<0||l.credit<0)throw new IllegalArgumentException("Debet/kredit tidak boleh negatif.");if(l.debit>EPS&&l.credit>EPS)throw new IllegalArgumentException("Satu detail hanya boleh mempunyai nilai debet atau kredit.");if(l.debit<=EPS&&l.credit<=EPS)throw new IllegalArgumentException("Salah satu nilai debet/kredit wajib lebih dari nol.");}
    private static List<Long> ids(Criteria c){List<Long>o=new ArrayList<Long>();for(Object v:c.setProjection(Projections.property("id")).list())o.add((Long)v);return o;}
    private static Object revisionId(Object r){try{return r.getClass().getMethod("getId").invoke(r);}catch(Exception e){throw new IllegalStateException("Metadata revisi tidak valid.",e);}}
    private static boolean balanced(double d,double c){return Math.abs(d-c)<EPS;}
    private static double num(Object n){return n==null?0:((Number)n).doubleValue();}
    private static Date start(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}
    private static Date next(Date d){Calendar c=Calendar.getInstance();c.setTime(start(d));c.add(Calendar.DATE,1);return c.getTime();}
    private static void rollback(Transaction t){if(t!=null)try{t.rollback();}catch(Exception ignored){}}
    private static String clean(String s){return s==null||s.trim().length()==0?null:s.trim();}
    private static String join(String a,String b){return (clean(a)==null?"":a.trim())+(clean(a)==null||clean(b)==null?"":" - ")+(clean(b)==null?"":b.trim());}
    private static String joinValues(List<String>v){StringBuilder b=new StringBuilder();for(String x:v){if(b.length()>0)b.append(", ");b.append(x);}return b.toString();}
    private static boolean validSource(String v){String[]a={"pertangungjawaban","kasKecil","kasBesar","pajak","pemesananPengadaanMasterAsset","penerimaanPengadaanMasterAsset","saldoAwalMasterAsset","pembayaranGaji","detailKegiatan","cicilanPembayaran","deposit","pengeluaranMahasiswa","logPembayaran","tagihan","pembayaranSiswaDetail","depositSiswa","penyusutanAsset","daftarPengajuanTransfer","transitori"};for(String x:a)if(x.equals(v))return true;return false;}
    private static boolean validKind(String v){String[]a={"PIUTANG_SISWA","PEMBAYARAN_SISWA_DIBAYAR_DIMUKA","PIUTANG_DENDA_SISWA","UTANG_DISKON_SISWA"};for(String x:a)if(x.equals(v))return true;return false;}
    private static org.hibernate.criterion.Criterion refRestriction(String v){if("KOSONG".equals(v))return Restrictions.sqlRestriction("(ref is null or ref='')");if("PEKERJAAN_NON_DP".equals(v))return Restrictions.sqlRestriction("ref is not null and ref != 'DP_PEKERJAAN' and ref != 'DP_BALIK_PEKERJAAN'");if("dimuka".equals(v)||"payment_gateway".equals(v)||"DP_PEKERJAAN".equals(v)||"DP_BALIK_PEKERJAAN".equals(v))return Restrictions.eq("ref",v);return Restrictions.sqlRestriction("1=1");}
    private static Long longValue(String v,String label){try{return Long.valueOf(v.trim());}catch(Exception e){throw new IllegalArgumentException("ID "+label+" tidak valid.");}}
    private static double number(String v){try{String n=v.trim();if(n.indexOf('.')>=0&&n.indexOf(',')>=0)n=n.replace(".","").replace(',','.');else if(n.indexOf(',')>=0)n=n.replace(',','.');return Double.parseDouble(n);}catch(Exception e){throw new IllegalArgumentException("Nominal tidak valid: "+v);}}
    private static Date parseDate(String v){try{return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v.trim());}catch(Exception e){throw new IllegalArgumentException("Tanggal CSV harus yyyy-MM-dd.");}}
    private static List<String>csvRow(String row){List<String>o=new ArrayList<String>();StringBuilder b=new StringBuilder();boolean q=false;for(int i=0;i<row.length();i++){char c=row.charAt(i);if(c=='"'){if(q&&i+1<row.length()&&row.charAt(i+1)=='"'){b.append('"');i++;}else q=!q;}else if(c==','&&!q){o.add(b.toString());b.setLength(0);}else b.append(c);}o.add(b.toString());return o;}

    /**
     * Tipe implementasi bersarang {@link Totals} milik {@link NewUiJournalService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code double debit}, {@code double credit}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    private static final class Totals{double debit,credit;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiJournalService} untuk filter. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long typeId}, {@code
     * Long workUnitId}, {@code Long workspaceId}, {@code Long postingId}, {@code Long closingId}, {@code String
     * query}, {@code String lineQuery}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Filter{public Long id,typeId,workUnitId,workspaceId,postingId,closingId;public String query,lineQuery,journalType,sourceEntity,ref,kind;public Date start,end,closingBefore;public Double min,max;public Boolean posted,postedActive,closed;public boolean unbalanced,unpostedActive,allJournals;public List<Long>accountIds;public int page,size=20;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiJournalService} untuk snapshot. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code double debit},
     * {@code double credit}, {@code List rows}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Snapshot{public int total;public double debit,credit;public final List<Row>rows=new ArrayList<Row>();}
    /**
     * Pembawa data/helper lokal milik {@link NewUiJournalService} untuk row. Tipe ini mengelompokkan nilai antara
     * agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long typeId}, {@code
     * Long workUnitId}, {@code Long workspaceId}, {@code Long postingId}, {@code Long closingId}, {@code String
     * code}, {@code String description}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Row{public Long id,typeId,workUnitId,workspaceId,postingId,closingId;public String code,description,type,workUnit,workspace,journalType,invoice,payee,user,closing;public Date date,postingDate;public double debit,credit;public boolean balanced,hasAttachment;public Boolean postingActive;public final List<Line>lines=new ArrayList<Line>();}
    /**
     * Tipe implementasi bersarang {@link Line} milik {@link NewUiJournalService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long accountId},
     * {@code String accountCode}, {@code String account}, {@code String description}, {@code double debit}, {@code
     * double credit}, {@code Integer status}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Line{public Long id,accountId;public String accountCode,account,description;public double debit,credit;public Integer status;}
    /**
     * Tipe implementasi bersarang {@link Draft} milik {@link NewUiJournalService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long typeId}, {@code
     * Long workUnitId}, {@code Long workspaceId}, {@code String code}, {@code String description}, {@code String
     * journalType}, {@code String invoice}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Draft{public Long id,typeId,workUnitId,workspaceId;public String code,description,journalType,invoice,payee;public Date date;public List<LineDraft>lines=new ArrayList<LineDraft>();}
    /**
     * Tipe implementasi bersarang {@link LineDraft} milik {@link NewUiJournalService}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long accountId},
     * {@code String description}, {@code double debit}, {@code double credit}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class LineDraft{public Long id,accountId;public String description;public double debit,credit;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiJournalService} untuk option. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long parentId},
     * {@code String label}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Option{public final Long id,parentId;public final String label;Option(Long id,String label,Long parentId){this.id=id;this.label=label;this.parentId=parentId;}}
    /**
     * Pembawa data/helper lokal milik {@link NewUiJournalService} untuk options. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Date lastClosing}, {@code List
     * accounts}, {@code List types}, {@code List workUnits}, {@code List workspaces}. Aturan bisnis bersama tetap
     * berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Options{public Date lastClosing;public final List<Option>accounts=new ArrayList<Option>(),types=new ArrayList<Option>(),workUnits=new ArrayList<Option>(),workspaces=new ArrayList<Option>();}
    /**
     * Tipe implementasi bersarang {@link HistoryRow} milik {@link NewUiJournalService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int revision}, {@code Date date},
     * {@code String type}, {@code String code}, {@code String description}, {@code double debit}, {@code double
     * credit}, {@code Long postingId}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class HistoryRow{public int revision;public Date date;public String type,code,description;public double debit,credit;public Long postingId;}
    /**
     * Tipe implementasi bersarang {@link BatchResult} milik {@link NewUiJournalService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int success}, {@code List errors}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static class BatchResult{public int success;public final List<String>errors=new ArrayList<String>();}
    /**
     * Tipe implementasi bersarang {@link ImportResult} milik {@link NewUiJournalService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List ids}. Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class ImportResult extends BatchResult{public final List<Long>ids=new ArrayList<Long>();}
    /**
     * Tipe implementasi bersarang {@link Attachment} milik {@link NewUiJournalService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String name}, {@code String link},
     * {@code File file}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class Attachment{public String name,link;public File file;}
    /**
     * Tipe implementasi bersarang {@link MaintenanceResult} milik {@link NewUiJournalService}. Kelas ini memberi
     * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiJournalService}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int groups}, {@code int lines}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiJournalService
     */
    public static final class MaintenanceResult{public int groups,lines;}
}
