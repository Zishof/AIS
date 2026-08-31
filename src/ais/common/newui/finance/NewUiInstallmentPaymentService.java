package ais.common.newui.finance;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import ais.action.report.CommonReportHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;

/** Native read/report/audit/reversal workflow copied from CicilanPembayaranAction. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class NewUiInstallmentPaymentService {
    public Snapshot load(Filter f){Session s=HibernateUtil.openSession();try{Snapshot o=new Snapshot();Number n=(Number)criteria(s,f,false).setProjection(Projections.rowCount()).uniqueResult();o.total=n==null?0:n.intValue();for(Object v:criteria(s,f,true).setFirstResult(Math.max(0,f.page)*f.size).setMaxResults(f.size).list())o.rows.add(row((CicilanPembayaran)v));Object[]sum=(Object[])criteria(s,f,false).setProjection(Projections.projectionList().add(Projections.sum("nilai")).add(Projections.sum("deposit")).add(Projections.sum("denda"))).uniqueResult();if(sum!=null){o.amount=num(sum[0]);o.deposit=num(sum[1]);o.penalty=num(sum[2]);}return o;}finally{s.close();}}
    public Options options(){Session s=HibernateUtil.openSession();try{Options o=new Options();for(Object v:s.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya","i").add(Restrictions.or(Restrictions.isNull("i.aktif"),Restrictions.eq("i.aktif",true))).setProjection(Projections.projectionList().add(Projections.groupProperty("i.id")).add(Projections.groupProperty("i.nama"))).addOrder(Order.asc("i.nama")).list()){Object[]x=(Object[])v;o.items.add(new Option((Long)x[0],String.valueOf(x[1]),null));}for(Object v:s.createCriteria(Fakultas.class).addOrder(Order.asc("nama")).list()){Fakultas x=(Fakultas)v;o.faculties.add(new Option(x.getId(),x.getNama(),null));}for(Object v:s.createCriteria(Jurusan.class).addOrder(Order.asc("nama")).list()){Jurusan x=(Jurusan)v;o.studyPrograms.add(new Option(x.getId(),x.getNama(),x.getFakultas()==null?null:x.getFakultas().getId()));}return o;}finally{s.close();}}
    public List<HistoryRow> history(Long id,Long studentId,Long candidateId){Session s=HibernateUtil.openSession();try{require(s,id,studentId,candidateId);AuditReader r=AuditReaderFactory.get(s);List<HistoryRow>out=new ArrayList<HistoryRow>();for(Object v:r.createQuery().forRevisionsOfEntity(CicilanPembayaran.class,false,true).add(AuditEntity.id().eq(id)).addOrder(AuditEntity.revisionNumber().desc()).setMaxResults(100).getResultList()){Object[]x=(Object[])v;CicilanPembayaran c=(CicilanPembayaran)x[0];HistoryRow h=new HistoryRow();h.revision=((Number)getRevisionId(x[1])).intValue();h.date=r.getRevisionDate(h.revision);h.amount=num(c.getNilai());h.deposit=num(c.getDeposit());h.note=c.getKeterangan();h.user=c.getOleh();h.type=String.valueOf(x[2]);out.add(h);}return out;}finally{s.close();}}
    private Object getRevisionId(Object rev){try{return rev.getClass().getMethod("getId").invoke(rev);}catch(Exception e){throw new IllegalStateException("Metadata revisi tidak valid.",e);}}
    public File receipt(Long id,Long studentId,Long candidateId){Session s=HibernateUtil.openSession();try{CicilanPembayaran c=require(s,id,studentId,candidateId);Kegiatan k=c.getKegiatan();if(k==null)throw new IllegalArgumentException("Transaksi pembayaran tidak mempunyai kegiatan.");return k.getMahasiswa()!=null?CommonReportHelper.cetakBuktipembayaranMahasiswa(k,false):CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(k,false);}finally{s.close();}}
    public void reverse(Long id,Long studentId,Long candidateId){Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();CicilanPembayaran c=require(s,id,studentId,candidateId);s.delete(c);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}
    private Criteria criteria(Session s,Filter f,boolean order){Criteria c=s.createCriteria(CicilanPembayaran.class,"p").createAlias("kegiatan","k").createAlias("k.mahasiswa","m",Criteria.LEFT_JOIN).createAlias("k.calonMahasiswa","cm",Criteria.LEFT_JOIN).createAlias("m.jurusan","j",Criteria.LEFT_JOIN).createAlias("cm.prodiLulus","cp",Criteria.LEFT_JOIN).add(Restrictions.or(Restrictions.gt("nilai",Double.valueOf(.01)),Restrictions.lt("nilai",Double.valueOf(-.01))));if(f.studentId!=null)c.add(Restrictions.eq("m.id",f.studentId));else if(f.candidateId!=null)c.add(Restrictions.eq("cm.id",f.candidateId));if(clean(f.query)!=null)c.add(Restrictions.or(Restrictions.or(Restrictions.ilike("m.nim",clean(f.query),MatchMode.ANYWHERE),Restrictions.ilike("m.nama",clean(f.query),MatchMode.ANYWHERE)),Restrictions.or(Restrictions.ilike("cm.nama",clean(f.query),MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("cm.noRegistrasi",clean(f.query),MatchMode.ANYWHERE),Restrictions.ilike("cm.noUjian",clean(f.query),MatchMode.ANYWHERE)))));if(clean(f.academicYear)!=null)c.add(Restrictions.eq("k.tahunAkademik",clean(f.academicYear)));if(f.cohort!=null)c.add(Restrictions.or(Restrictions.eq("m.tahunangkatan",f.cohort),Restrictions.eq("cm.tahun",f.cohort)));if(f.facultyId!=null)c.add(Restrictions.or(Restrictions.eq("j.fakultas.id",f.facultyId),Restrictions.eq("cp.fakultas.id",f.facultyId)));if(f.studyProgramId!=null)c.add(Restrictions.or(Restrictions.eq("j.id",f.studyProgramId),Restrictions.eq("cp.id",f.studyProgramId)));if(f.semesterOdd!=null)c.add(Restrictions.in("k.semster",f.semesterOdd.booleanValue()?new Integer[]{1,3,5,7,9,11,13,15}:new Integer[]{2,4,6,8,10,12,14,16}));if(f.start!=null)c.add(Restrictions.ge("tanggal",start(f.start)));if(f.end!=null)c.add(Restrictions.lt("tanggal",next(f.end)));if(f.depositOnly)c.add(Restrictions.ge("deposit",Double.valueOf(.1)));if(f.itemIds!=null&&!f.itemIds.isEmpty())c.add(Restrictions.in("itemBiaya.id",f.itemIds));if(order)c.addOrder(Order.desc("id"));return c;}
    private Row row(CicilanPembayaran c){Row r=new Row();r.id=c.getId();r.amount=num(c.getNilai());r.deposit=num(c.getDeposit());r.penalty=num(c.getDenda());r.date=c.getTanggal();r.receiptDate=c.getTanggalKwitansi();r.note=c.getKeterangan();r.stage=c.getTahap();if(c.getJenisPembayaran()!=null)r.paymentType=c.getJenisPembayaran().getNama();if(c.getItemBiaya()!=null)r.item=c.getItemBiaya().getNama();Kegiatan k=c.getKegiatan();if(k!=null){r.activityId=k.getId();r.academicYear=k.getTahunAkademik();r.semester=k.getSemster();r.billed=num(k.getTagihan());r.activityPaid=num(k.getAmount());r.remaining=num(k.getAmountTerhutang());if(k.getJenisKegiatan()!=null)r.activity=k.getJenisKegiatan().getNamaKegiatan();Mahasiswa m=k.getMahasiswa();BiodataCalonMahasiswa b=k.getCalonMahasiswa();if(m!=null){r.studentId=m.getId();r.identifier=m.getNim();r.name=m.getNama();if(m.getJurusan()!=null){r.studyProgram=m.getJurusan().getNama();if(m.getJurusan().getFakultas()!=null)r.faculty=m.getJurusan().getFakultas().getNama();}}else if(b!=null){r.candidateId=b.getId();r.identifier=b.getNoRegistrasi();r.name=b.getNama();Jurusan jp=b.getProdiLulus()==null?b.getProdi1():b.getProdiLulus();if(jp!=null){r.studyProgram=jp.getNama();if(jp.getFakultas()!=null)r.faculty=jp.getFakultas().getNama();}}}return r;}
    private static CicilanPembayaran require(Session s,Long id,Long studentId,Long candidateId){CicilanPembayaran c=id==null?null:(CicilanPembayaran)s.get(CicilanPembayaran.class,id);if(c==null)throw new IllegalArgumentException("Cicilan pembayaran tidak ditemukan.");Kegiatan k=c.getKegiatan();if(studentId!=null&&(k==null||k.getMahasiswa()==null||!studentId.equals(k.getMahasiswa().getId())))throw new SecurityException("Pembayaran bukan milik pengguna aktif.");if(candidateId!=null&&(k==null||k.getCalonMahasiswa()==null||!candidateId.equals(k.getCalonMahasiswa().getId())))throw new SecurityException("Pembayaran bukan milik pengguna aktif.");return c;}
    private static Date start(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}private static Date next(Date d){Calendar c=Calendar.getInstance();c.setTime(start(d));c.add(Calendar.DATE,1);return c.getTime();}private static double num(Object n){return n==null?0:((Number)n).doubleValue();}private static String clean(String s){return s==null||s.trim().length()==0?null:s.trim();}private static void rollback(Transaction t){if(t!=null)try{t.rollback();}catch(Exception ignored){}}
    /**
     * Pembawa data/helper lokal milik {@link NewUiInstallmentPaymentService} untuk filter. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiInstallmentPaymentService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String query}, {@code String
     * academicYear}, {@code Integer cohort}, {@code Long studentId}, {@code Long candidateId}, {@code Long
     * facultyId}, {@code Long studyProgramId}, {@code Boolean semesterOdd}. Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiInstallmentPaymentService
     */
    public static final class Filter{public String query,academicYear;public Integer cohort;public Long studentId,candidateId,facultyId,studyProgramId;public Boolean semesterOdd;public Date start,end;public boolean depositOnly;public List<Long>itemIds;public int page,size=20;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiInstallmentPaymentService} untuk snapshot. Tipe ini
     * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
     * jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiInstallmentPaymentService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code double amount},
     * {@code double deposit}, {@code double penalty}, {@code List rows}. Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiInstallmentPaymentService
     */
    public static final class Snapshot{public int total;public double amount,deposit,penalty;public final List<Row>rows=new ArrayList<Row>();}
    /**
     * Pembawa data/helper lokal milik {@link NewUiInstallmentPaymentService} untuk row. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiInstallmentPaymentService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long activityId},
     * {@code Long studentId}, {@code Long candidateId}, {@code Integer semester}, {@code Integer stage}, {@code
     * String identifier}, {@code String name}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see NewUiInstallmentPaymentService
     */
    public static final class Row{public Long id,activityId,studentId,candidateId;public Integer semester,stage;public String identifier,name,faculty,studyProgram,academicYear,activity,paymentType,item,note;public Date date,receiptDate;public double amount,deposit,penalty,billed,activityPaid,remaining;}
    /**
     * Tipe implementasi bersarang {@link HistoryRow} milik {@link NewUiInstallmentPaymentService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiInstallmentPaymentService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int revision}, {@code Date date},
     * {@code String user}, {@code String type}, {@code String note}, {@code double amount}, {@code double
     * deposit}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiInstallmentPaymentService
     */
    public static final class HistoryRow{public int revision;public Date date;public String user,type,note;public double amount,deposit;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiInstallmentPaymentService} untuk options. Tipe ini
     * mengelompokkan nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang
     * jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiInstallmentPaymentService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List items}, {@code List faculties},
     * {@code List studyPrograms}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiInstallmentPaymentService
     */
    /** Satu pilihan filter cicilan dengan id, label, dan id induk opsional. */
    public static final class Options{public final List<Option>items=new ArrayList<Option>(),faculties=new ArrayList<Option>(),studyPrograms=new ArrayList<Option>();}/** Satu pilihan filter cicilan dengan id, label, dan id induk opsional. */
    public static final class Option{public final Long id,parentId;public final String label;Option(Long i,String l,Long p){id=i;label=l;parentId=p;}}
}
