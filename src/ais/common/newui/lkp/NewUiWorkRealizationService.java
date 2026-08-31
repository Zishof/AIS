package ais.common.newui.lkp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.database.model.rab.SatuanKerja;

/** Port headless daftar, agregasi, detail, dan verifikasi dari RealisasiKerjaPegawaiAction. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class NewUiWorkRealizationService {
    public Snapshot load(Filter filter, Set<SatuanKerja> allowedUnits, Tbmuser user) {
        Session session = HibernateUtil.openSession();
        try {
            Snapshot out = new Snapshot();
            Criteria count = criteria(session, filter, allowedUnits, false);
            Number total = (Number) count.setProjection(Projections.rowCount()).uniqueResult(); out.total = total == null ? 0 : total.intValue();
            List values = criteria(session, filter, allowedUnits, true).setFirstResult(Math.max(0, filter.page) * filter.size).setMaxResults(filter.size).list();
            for (Object value : values) out.rows.add(row(session, (TargetKerjaPegawai) value, user));
            options(session, out, allowedUnits); return out;
        } finally { session.close(); }
    }

    public List<Detail> details(Long targetId, Set<SatuanKerja> allowedUnits, Tbmuser user) {
        Session session = HibernateUtil.openSession();
        try {
            TargetKerjaPegawai target = require(session, targetId, allowedUnits); List<Detail> out = new ArrayList<Detail>();
            for (Object value : session.createCriteria(RealisasiKerjaPegawai.class).add(Restrictions.eq("targetKerjaPegawai", target)).addOrder(Order.desc("tanggalWaktu")).addOrder(Order.desc("id")).list()) out.add(new Detail((RealisasiKerjaPegawai) value));
            return out;
        } finally { session.close(); }
    }

    public void verify(Long targetId, Double quality, boolean verified, String note, Set<SatuanKerja> allowedUnits, Tbmuser user) {
        Session session = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction(); TargetKerjaPegawai target = require(session, targetId, allowedUnits);
            if (!canAssess(session, target, user)) throw new SecurityException("Pengguna aktif bukan asesor pegawai tersebut.");
            if (quality == null || quality.doubleValue() < 0 || quality.doubleValue() > 100) throw new IllegalArgumentException("Kualitas realisasi harus 0 sampai 100.");
            target.setKualitasRealisasi(quality); target.setVerifikasi(Boolean.valueOf(verified)); target.setCatatan(clean(note));
            if (user != null) { target.setOleh(user.getUserNama()); target.setOlehId(user.getUserId()); }
            session.update(target); session.flush(); tx.commit();
        } catch (RuntimeException e) { rollback(tx); throw e; } finally { session.close(); }
    }

    private Criteria criteria(Session session, Filter filter, Set<SatuanKerja> allowedUnits, boolean ordered) {
        Criteria criteria = session.createCriteria(TargetKerjaPegawai.class).createAlias("kegiatanTugasJabatan", "activity");
        if (KegiatanTugasJabatan.BULANAN.equals(filter.period)) criteria.add(Restrictions.or(Restrictions.isNull("activity.periode"), Restrictions.eq("activity.periode", filter.period)));
        else criteria.add(Restrictions.eq("activity.periode", filter.period));
        if (allowedUnits != null && !allowedUnits.isEmpty()) criteria.add(Restrictions.in("activity.satuanKerja", allowedUnits));
        if (filter.unitId != null) criteria.add(Restrictions.eq("activity.satuanKerja.id", filter.unitId));
        if (clean(filter.query) != null) criteria.add(Restrictions.ilike("activity.nama", clean(filter.query), MatchMode.ANYWHERE));
        criteria.add(Restrictions.eq("tahun", Integer.valueOf(filter.year)));
        if (filter.month != null) criteria.add(Restrictions.eq("bulan", filter.month));
        if (filter.employeeId != null) criteria.add(Restrictions.eq("pegawai.id", filter.employeeId));
        if (ordered) criteria.addOrder(Order.asc("activity.noUrut")).addOrder(Order.desc("id")); return criteria;
    }

    private Row row(Session session, TargetKerjaPegawai target, Tbmuser user) {
        Row out = new Row(); out.id=target.getId();out.year=target.getTahun();out.month=target.getBulan();out.quantity=number(target.getKuantitas());out.quality=number(target.getKualitas());out.time=number(target.getWaktu());out.cost=number(target.getBiaya());out.realizedQuality=number(target.getKualitasRealisasi());out.verified=Boolean.TRUE.equals(target.getVerifikasi());out.note=target.getCatatan();out.canAssess=canAssess(session,target,user);
        if(target.getPegawai()!=null){out.employeeId=target.getPegawai().getId();out.employee=target.getPegawai().getNama();}
        if(target.getKegiatanTugasJabatan()!=null){KegiatanTugasJabatan activity=target.getKegiatanTugasJabatan();out.activity=activity.getNama();out.activityNote=activity.getKeterangan();out.quantityUnit=activity.getSatuanKuantitas()==null?"":activity.getSatuanKuantitas().getNama();out.timeUnit=activity.getSatuanWaktu();if(activity.getSatuanKerja()!=null){out.unitId=activity.getSatuanKerja().getId();out.unit=activity.getSatuanKerja().getNama();}}
        Number quantity=(Number)session.createCriteria(RealisasiKerjaPegawai.class).add(Restrictions.eq("targetKerjaPegawai",target)).add(Restrictions.eq("verifikasi",true)).setProjection(Projections.sum("kuantitas")).uniqueResult();Number time=(Number)session.createCriteria(RealisasiKerjaPegawai.class).add(Restrictions.eq("targetKerjaPegawai",target)).add(Restrictions.eq("verifikasi",true)).setProjection(Projections.sum("waktu")).uniqueResult();Number count=(Number)session.createCriteria(RealisasiKerjaPegawai.class).add(Restrictions.eq("targetKerjaPegawai",target)).setProjection(Projections.rowCount()).uniqueResult();out.realizedQuantity=quantity==null?0:quantity.doubleValue();out.realizedTime=time==null?0:time.doubleValue();out.detailCount=count==null?0:count.intValue();return out;
    }

    private boolean canAssess(Session session, TargetKerjaPegawai target, Tbmuser user) {
        if(user==null||target.getPegawai()==null)return false;Criteria criteria=session.createCriteria(AsesorPegawai.class).createAlias("asesor","assessor").createAlias("assessor.asesorPenunjangKinerjaDosen","support").createAlias("assessor.tbmuser","account").add(Restrictions.or(Restrictions.isNull("assessor.aktif"),Restrictions.eq("assessor.aktif",true))).add(Restrictions.eq("pegawai",target.getPegawai())).add(Restrictions.eq("support.aktif",true));
        List<Criterion> identities=new ArrayList<Criterion>();Pegawai employee=user.ambilPegawai();Dosen lecturer=user.ambilDosen();if(employee!=null)identities.add(Restrictions.eq("account.pegawai",employee));if(lecturer!=null)identities.add(Restrictions.eq("account.dosen",lecturer));if(identities.isEmpty())return false;Criterion identity=identities.get(0);for(int i=1;i<identities.size();i++)identity=Restrictions.or(identity,identities.get(i));Number count=(Number)criteria.add(identity).setProjection(Projections.rowCount()).uniqueResult();return count!=null&&count.longValue()>0;
    }

    private TargetKerjaPegawai require(Session session, Long id, Set<SatuanKerja> allowedUnits) {TargetKerjaPegawai value=id==null?null:(TargetKerjaPegawai)session.get(TargetKerjaPegawai.class,id);if(value==null)throw new IllegalArgumentException("Target kerja tidak ditemukan.");SatuanKerja unit=value.getKegiatanTugasJabatan()==null?null:value.getKegiatanTugasJabatan().getSatuanKerja();if(unit!=null&&allowedUnits!=null&&!allowedUnits.isEmpty()){boolean ok=false;for(SatuanKerja allowed:allowedUnits)if(unit.getId().equals(allowed.getId()))ok=true;if(!ok)throw new SecurityException("Target kerja berada di luar akses satuan kerja.");}return value;}
    private void options(Session session,Snapshot out,Set<SatuanKerja>units){if(units!=null)for(SatuanKerja unit:units)out.units.add(new Option(unit.getId(),unit.getNama()));for(Object value:session.createCriteria(Pegawai.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",true))).addOrder(Order.asc("nama")).setMaxResults(3000).list()){Pegawai employee=(Pegawai)value;out.employees.add(new Option(employee.getId(),employee.getNama()));}}
    private static double number(Number value){return value==null?0:value.doubleValue();}private static String clean(String value){return value==null||value.trim().length()==0?null:value.trim();}private static void rollback(Transaction tx){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}}
    /**
     * Pembawa data/helper lokal milik {@link NewUiWorkRealizationService} untuk filter. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiWorkRealizationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String query}, {@code String period},
     * {@code int year}, {@code int monthValue}, {@code int page}, {@code int size}, {@code Integer month}, {@code
     * Long employeeId}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiWorkRealizationService
     */
    public static final class Filter{public String query,period=KegiatanTugasJabatan.BULANAN;public int year,monthValue,page,size=20;public Integer month;public Long employeeId,unitId;}
    /**
     * Pembawa data/helper lokal milik {@link NewUiWorkRealizationService} untuk snapshot. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiWorkRealizationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code int total}, {@code List rows}, {@code
     * List employees}, {@code List units}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiWorkRealizationService
     */
    public static final class Snapshot{public int total;public final List<Row>rows=new ArrayList<Row>();public final List<Option>employees=new ArrayList<Option>(),units=new ArrayList<Option>();}
    /**
     * Pembawa data/helper lokal milik {@link NewUiWorkRealizationService} untuk option. Tipe ini mengelompokkan
     * nilai antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiWorkRealizationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String label}.
     * Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     *
     * @see NewUiWorkRealizationService
     */
    public static final class Option{public final Long id;public final String label;Option(Long id,String label){this.id=id;this.label=label;}}
    /**
     * Pembawa data/helper lokal milik {@link NewUiWorkRealizationService} untuk row. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiWorkRealizationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code Long employeeId},
     * {@code Long unitId}, {@code Integer year}, {@code Integer month}, {@code String employee}, {@code String
     * activity}, {@code String activityNote}. Aturan bisnis bersama tetap berada pada kelas induk atau service
     * yang dipanggilnya.</p>
     *
     * @see NewUiWorkRealizationService
     */
    public static final class Row{public Long id,employeeId,unitId;public Integer year,month;public String employee,activity,activityNote,unit,quantityUnit,timeUnit,note;public double quantity,quality,time,cost,realizedQuantity,realizedQuality,realizedTime;public int detailCount;public boolean verified,canAssess;}
    /**
     * Tipe implementasi bersarang {@link Detail} milik {@link NewUiWorkRealizationService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiWorkRealizationService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String description},
     * {@code String note}, {@code String by}, {@code double quantity}, {@code double time}, {@code double cost},
     * {@code boolean verified}. Aturan bisnis bersama tetap berada pada kelas induk atau service yang
     * dipanggilnya.</p>
     *
     * @see NewUiWorkRealizationService
     */
    public static final class Detail{public final Long id;public final String description,note,by;public final double quantity,time,cost;public final boolean verified;public final long from,to;Detail(RealisasiKerjaPegawai value){id=value.getId();description=value.getKeterangan();note=value.getCatatan();by=value.getOleh();quantity=number(value.getKuantitas());time=number(value.getWaktu());cost=number(value.getBiaya());verified=Boolean.TRUE.equals(value.getVerifikasi());from=value.getTanggalWaktu()==null?0:value.getTanggalWaktu().getTime();to=value.getTanggalWaktuSampai()==null?0:value.getTanggalWaktuSampai().getTime();}}
}
