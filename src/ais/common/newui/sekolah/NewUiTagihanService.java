package ais.common.newui.sekolah;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.sekolah.helper.TagihanUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;

/** Port headless TagihanAction; tidak memakai komponen atau lifecycle ZK. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiTagihanService {
    public Snapshot load(Filter f, Tbmuser user) {
        Session s = HibernateUtil.openSession();
        try {
            Criteria count = criteria(s, f, user, false).setProjection(Projections.rowCount());
            Number n = (Number) count.uniqueResult();
            Snapshot out = new Snapshot();
            out.total = n == null ? 0 : n.intValue();
            List values = criteria(s, f, user, true).setFirstResult(Math.max(0, f.page) * f.size)
                    .setMaxResults(f.size).list();
            for (Object value : values) out.rows.add(new Row((Tagihan) value));
            out.options = options(s, user);
            return out;
        } finally { s.close(); }
    }

    public int autoGenerate(Long studentId, Tbmuser user) {
        if (studentId == null) throw new IllegalArgumentException("Siswa wajib dipilih.");
        Session s = HibernateUtil.openSession();
        Siswa student;
        List<PengaturanBiaya> settings;
        try {
            student = (Siswa) s.get(Siswa.class, studentId);
            requireStudentScope(student, user);
            if (student == null || student.getSekolah() == null) throw new IllegalArgumentException("Sekolah siswa belum tersedia.");
            settings = s.createCriteria(PengaturanBiaya.class).createAlias("jenisBiayaSekolah", "jbs")
                    .add(Restrictions.eq("jbs.periode", "Bulanan"))
                    .add(Restrictions.eq("sekolah", student.getSekolah()))
                    .add(Restrictions.isNotNull("bulanSampai")).list();
            for (PengaturanBiaya setting : settings) {
                setting.getJenisBiayaSekolah().getPeriode();
            }
        } finally { s.close(); }
        int generated = 0;
        for (PengaturanBiaya setting : settings) {
            try {
                int before = countFor(studentId, setting.getId());
                int last = setting.getBulanSampai().intValue();
                int month = last % 100, year = last / 100;
                if (month < 12) month++; else { month = 1; year++; }
                TagihanUtil.doGenerateTagihanBulanan(student, setting, Integer.valueOf(year), Integer.valueOf(month),
                        TagihanUtil.getBulanMulai(setting.getJenisBiayaSekolah()));
                generated += Math.max(0, countFor(studentId, setting.getId()) - before);
            } catch (RuntimeException e) {
                try { ais.common.ErrorAuditUtil.record(e, "NewUiTagihanService.autoGenerate"); } catch (Exception ignored) { }
            }
        }
        return generated;
    }

    public int synchronize(Filter f, Tbmuser user) {
        Session read = HibernateUtil.openSession();
        List<Long> ids = new ArrayList<Long>();
        try {
            List rows = criteria(read, f, user, true).setProjection(Projections.id()).list();
            for (Object id : rows) ids.add(Long.valueOf(String.valueOf(id)));
        } finally { read.close(); }
        int done = 0;
        for (Long id : ids) {
            Session s = HibernateUtil.openSession(); Transaction tx = null;
            try { tx = s.beginTransaction(); Tagihan t = (Tagihan) s.get(Tagihan.class, id); if (t != null) { s.update(t); s.flush(); done++; } tx.commit(); }
            catch (RuntimeException e) { rollback(tx); try { ais.common.ErrorAuditUtil.record(e, "NewUiTagihanService.synchronize"); } catch (Exception ignored) { } }
            finally { s.close(); }
        }
        return done;
    }

    public void toggle(Long id, boolean active, Tbmuser user) {
        requireAdmin(user);
        Session s = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = s.beginTransaction(); Tagihan t = bill(s, id); requireScope(t, user);
            if (t.getPembayaranSiswaDetail() != null || Boolean.TRUE.equals(t.getBukanTagihan()))
                throw new IllegalArgumentException("Hanya tagihan belum lunas yang dapat diaktifkan atau dinonaktifkan.");
            t.setAktif(Boolean.valueOf(active)); t.setAktifkanmanual(Boolean.valueOf(active));
            t.setNonaktifManual(Boolean.valueOf(!active)); s.update(t); s.flush(); tx.commit();
        } catch (RuntimeException e) { rollback(tx); throw e; } finally { s.close(); }
    }

    public List<MoveTarget> moveTargets(Long id, Tbmuser user) {
        Session s = HibernateUtil.openSession();
        try {
            Tagihan source = bill(s, id); requireScope(source, user);
            if (source.getPembayaranSiswaDetail() == null || Boolean.TRUE.equals(source.getBukanTagihan()))
                throw new IllegalArgumentException("Tagihan sumber belum mempunyai pembayaran yang dapat dipindahkan.");
            Criteria c = s.createCriteria(Tagihan.class);
            if (source.getCalonSiswa() != null) c.add(Restrictions.eq("calonSiswa", source.getCalonSiswa()));
            else c.add(Restrictions.eq("siswa", source.getSiswa()));
            c.add(Restrictions.ne("id", source.getId())).add(Restrictions.eq("aktif", Boolean.TRUE))
                    .add(Restrictions.or(Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
                            Restrictions.sqlRestriction("this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
                            Restrictions.isNull("pembayaranSiswaDetail"))).addOrder(Order.asc("tahunbulan"));
            List<MoveTarget> out = new ArrayList<MoveTarget>();
            for (Object value : c.list()) { Tagihan target = (Tagihan) value; if (target.getPembayaranSiswaDetail() == null) out.add(new MoveTarget(target)); }
            return out;
        } finally { s.close(); }
    }

    public void movePayment(Long sourceId, Long targetId, Tbmuser user) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) throw new IllegalArgumentException("Tujuan tagihan tidak valid.");
        Session s = HibernateUtil.openSession(); Transaction tx = null;
        try {
            tx = s.beginTransaction(); Tagihan source = bill(s, sourceId), target = bill(s, targetId);
            requireScope(source, user); requireScope(target, user);
            boolean sameOwner = source.getSiswa() != null && source.getSiswa().equals(target.getSiswa())
                    || source.getCalonSiswa() != null && source.getCalonSiswa().equals(target.getCalonSiswa());
            if (!sameOwner) throw new IllegalArgumentException("Pembayaran hanya dapat dipindahkan ke tagihan pemilik yang sama.");
            PembayaranSiswaDetail payment = source.getPembayaranSiswaDetail();
            if (payment == null || target.getPembayaranSiswaDetail() != null) throw new IllegalArgumentException("Status pembayaran sumber atau tujuan sudah berubah.");
            payment.setTagihan(target); payment.setNominalBiaya(target.getNominalBiaya());
            source.setPembayaranSiswaDetailTrue(null); source.setPembayaranSiswaDetail(null);
            target.setPembayaranSiswaDetail(payment);
            s.update(source); s.update(payment); s.update(target); s.flush(); tx.commit();
        } catch (RuntimeException e) { rollback(tx); throw e; } finally { s.close(); }
    }

    public void delete(Long id, Tbmuser user) {
        Session s = HibernateUtil.openSession(); Transaction tx = null;
        try { tx = s.beginTransaction(); Tagihan t = bill(s, id); requireScope(t, user); s.delete(t); s.flush(); tx.commit(); }
        catch (RuntimeException e) { rollback(tx); throw e; } finally { s.close(); }
    }

    public List<History> history(Long id, Tbmuser user) {
        Session s = HibernateUtil.openSession();
        try {
            Tagihan current = bill(s, id); requireScope(current, user);
            List rows = s.createSQLQuery("select a.rev,a.revtype,r.revtstmp,a.oleh,a.informasi,a.nominal,a.denda,a.aktif,a.pembayaran_siswa_detail_id "
                    + "from new_audit.tagihan__audit a left join new_audit.revinfo r on r.rev=a.rev where a.id=:id order by a.rev desc")
                    .setLong("id", id.longValue()).setMaxResults(100).list();
            List<History> out = new ArrayList<History>(); for (Object value : rows) out.add(new History((Object[]) value)); return out;
        } finally { s.close(); }
    }

    private Criteria criteria(Session s, Filter f, Tbmuser user, boolean order) {
        Criteria c = s.createCriteria(Tagihan.class, "t");
        c.add(f.activeOnly ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)) : Restrictions.sqlRestriction("1=1"));
        if (f.settingId != null) c.createAlias("nominalBiaya", "nb").add(Restrictions.eq("nb.pengaturanBiaya.id", f.settingId));
        if (f.studentId != null) c.add(Restrictions.eq("siswa.id", f.studentId));
        if (f.candidateId != null) c.add(Restrictions.eq("calonSiswa.id", f.candidateId));
        if (f.month != null) c.add(Restrictions.eq("bulan", f.month)); if (f.year != null) c.add(Restrictions.eq("tahun", f.year));
        if (clean(f.academicYear) != null) c.add(Restrictions.eq("tahunAjaran", clean(f.academicYear)));
        c.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).createAlias("calonSiswa", "calon", Criteria.LEFT_JOIN)
                .createAlias("itemBiayaSekolah", "item", Criteria.LEFT_JOIN);
        String fee = clean(f.fee); if (fee != null) c.add(Restrictions.or(Restrictions.ilike("item.kode", fee, MatchMode.ANYWHERE), Restrictions.ilike("item.nama", fee, MatchMode.ANYWHERE)));
        String q = clean(f.q); if (q != null) c.add(Restrictions.or(Restrictions.or(Restrictions.ilike("siswa.nomorInduk", q, MatchMode.ANYWHERE), Restrictions.ilike("siswa.nama", q, MatchMode.ANYWHERE)), Restrictions.or(Restrictions.ilike("calon.noRegistrasi", q, MatchMode.ANYWHERE), Restrictions.ilike("calon.nama", q, MatchMode.ANYWHERE))));
        if (f.unpaidOnly) c.add(Restrictions.or(Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"), Restrictions.sqlRestriction("this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")), Restrictions.isNull("pembayaranSiswaDetail")));
        if (f.studentId == null && f.candidateId == null) { if (f.schoolId != null) c.add(Restrictions.eq("item.sekolah.id", f.schoolId)); if (f.foundationId != null) c.add(Restrictions.eq("item.yayasan.id", f.foundationId)); }
        applyUserScope(c, user); if (order) c.addOrder(Order.asc("tahunbulan")).addOrder(Order.asc("pembayaranSiswaDetail")); return c;
    }

    private Options options(Session s, Tbmuser user) {
        Options o = new Options();
        List years = s.createCriteria(Tagihan.class).add(Restrictions.isNotNull("tahunAjaran")).setProjection(Projections.distinct(Projections.property("tahunAjaran"))).addOrder(Order.desc("tahunAjaran")).list();
        for (Object y : years) if (clean(String.valueOf(y)) != null) o.academicYears.add(String.valueOf(y));
        Criteria students = s.createCriteria(Siswa.class).addOrder(Order.asc("nama")).setMaxResults(500);
        if (user != null && user.getSiswa() != null) students.add(Restrictions.idEq(user.getSiswa().getId()));
        else if (user != null && user.getCalonSiswa() != null) students.add(Restrictions.sqlRestriction("1=0"));
        else if (childIds(user) != null) students.add(Restrictions.in("id", childIds(user)));
        for (Object value : students.list()) o.students.add(new Choice((Siswa) value));
        Criteria candidates = s.createCriteria(CalonSiswa.class).addOrder(Order.asc("nama")).setMaxResults(500);
        if (user != null && user.getCalonSiswa() != null) candidates.add(Restrictions.idEq(user.getCalonSiswa().getId()));
        else if (user != null && (user.getSiswa() != null || childIds(user) != null)) candidates.add(Restrictions.sqlRestriction("1=0"));
        for (Object value : candidates.list()) o.candidates.add(new Choice((CalonSiswa) value));
        return o;
    }

    private void applyUserScope(Criteria c, Tbmuser user) {
        if (user == null || Common.getApakahAdmin()) return;
        if (user.getSiswa() != null) { c.add(Restrictions.eq("siswa.id", user.getSiswa().getId())); return; }
        if (user.getCalonSiswa() != null) { c.add(Restrictions.eq("calonSiswa.id", user.getCalonSiswa().getId())); return; }
        List<Long> ids = childIds(user); if (ids != null) c.add(Restrictions.in("siswa.id", ids));
    }
    private void requireScope(Tagihan t, Tbmuser user) { if (t.getSiswa() != null) requireStudentScope(t.getSiswa(), user); else if (user != null && user.getCalonSiswa() != null && !user.getCalonSiswa().equals(t.getCalonSiswa())) throw new SecurityException("Tagihan di luar cakupan pengguna."); }
    private void requireStudentScope(Siswa student, Tbmuser user) { if (student == null) throw new IllegalArgumentException("Siswa tidak ditemukan."); if (user == null || Common.getApakahAdmin()) return; if (user.getSiswa() != null && !user.getSiswa().equals(student)) throw new SecurityException("Siswa di luar cakupan pengguna."); List<Long> ids=childIds(user); if(ids!=null&&!ids.contains(student.getId()))throw new SecurityException("Siswa di luar cakupan orang tua."); }
    private static List<Long> childIds(Tbmuser user) { try { if (user != null && user.getOrangTua() != null && !user.getOrangTua().ambilAnakSiswa().isEmpty()) return user.getOrangTua().ambilAnakSiswa(); } catch (Exception ignored) { } return null; }
    private static void requireAdmin(Tbmuser user) { if (user == null || !Common.getApakahAdmin()) throw new SecurityException("Aksi ini hanya tersedia untuk administrator."); }
    private static Tagihan bill(Session s, Long id) { Tagihan t = id == null ? null : (Tagihan) s.get(Tagihan.class, id); if (t == null) throw new IllegalArgumentException("Tagihan tidak ditemukan."); return t; }
    private int countFor(Long studentId, Long settingId) { Session s=HibernateUtil.openSession(); try { Number n=(Number)s.createCriteria(Tagihan.class).add(Restrictions.eq("siswa.id",studentId)).add(Restrictions.eq("pengaturanBiaya.id",settingId)).setProjection(Projections.rowCount()).uniqueResult(); return n==null?0:n.intValue(); } finally{s.close();} }
    private static String clean(String value) { return value == null || value.trim().length() == 0 ? null : value.trim(); }
    private static void rollback(Transaction tx) { if (tx != null) try { tx.rollback(); } catch (Exception ignored) { } }

    public static final class Filter { public String q,fee,academicYear; public Long studentId,candidateId,settingId,schoolId,foundationId; public Integer month,year; public boolean unpaidOnly,activeOnly=true; public int page=0,size=20; }
    public static final class Snapshot { public final List<Row> rows=new ArrayList<Row>(); public int total; public Options options; }
    public static final class Options { public final Set<String> academicYears=new LinkedHashSet<String>(); public final List<Choice> students=new ArrayList<Choice>(),candidates=new ArrayList<Choice>(); }
    public static final class Choice { public final Long id; public final String code,name; Choice(Siswa s){id=s.getId();code=s.getNomorInduk();name=s.getNama();} Choice(CalonSiswa s){id=s.getId();code=s.getNoRegistrasi();name=s.getNama();} }
    public static final class Row { public final Long id,studentId,candidateId; public final String code,name,school,item,information,academicYear,photo; public final Integer month,year,installment; public final double nominal,fine,paid; public final boolean settled,active,notBill,canToggle,canMove; Row(Tagihan t){id=t.getId();studentId=t.getSiswa()==null?null:t.getSiswa().getId();candidateId=t.getCalonSiswa()==null?null:t.getCalonSiswa().getId();code=t.getSiswa()!=null?t.getSiswa().getNomorInduk():t.getCalonSiswa()==null?"":t.getCalonSiswa().getNoRegistrasi();name=t.getSiswa()!=null?t.getSiswa().getNama():t.getCalonSiswa()==null?"":t.getCalonSiswa().getNama();school=t.getSiswa()!=null&&t.getSiswa().getSekolah()!=null?t.getSiswa().getSekolah().getNama():t.getCalonSiswa()!=null&&t.getCalonSiswa().getSekolah()!=null?t.getCalonSiswa().getSekolah().getNama():"";item=t.getItemBiayaSekolah()==null?"":t.getItemBiayaSekolah().getNama();information=t.getInformasi();academicYear=t.getTahunAjaran();month=t.getBulan();year=t.getTahun();installment=t.getBayarKe();nominal=t.getNominal()==null?0:t.getNominal();fine=t.getDenda()==null?0:t.getDenda();paid=t.getDibayar()==null?0:t.getDibayar();settled=t.getPembayaranSiswaDetail()!=null;active=Boolean.TRUE.equals(t.getAktif());notBill=Boolean.TRUE.equals(t.getBukanTagihan());canToggle=!settled&&!notBill;canMove=settled&&!notBill;String p="";try{p=CommonMedia.getUrlFotoPengguna(t.getSiswa()!=null?new Tbmuser(t.getSiswa()):new Tbmuser(t.getCalonSiswa()));}catch(Exception ignored){}photo=p; } }
    public static final class MoveTarget { public final Long id; public final String label; MoveTarget(Tagihan t){id=t.getId();label=(t.getItemBiayaSekolah()==null?"Tagihan":t.getItemBiayaSekolah().getNama())+" · "+(t.getTahunAjaran()==null?"":t.getTahunAjaran())+" · "+(t.getBulan()==null?"-":t.getBulan())+"/"+(t.getTahun()==null?"-":t.getTahun())+" · Rp "+Math.round(t.getNominal()==null?0:t.getNominal());} }
    public static final class History { public final int revision,type; public final long timestamp; public final String by,information; public final double nominal,fine; public final Boolean active; public final Long paymentId; History(Object[]x){revision=((Number)x[0]).intValue();type=x[1]==null?0:((Number)x[1]).intValue();timestamp=x[2]==null?0:((Number)x[2]).longValue();by=x[3]==null?"":String.valueOf(x[3]);information=x[4]==null?"":String.valueOf(x[4]);nominal=x[5]==null?0:((Number)x[5]).doubleValue();fine=x[6]==null?0:((Number)x[6]).doubleValue();active=x[7]==null?null:(Boolean)x[7];paymentId=x[8]==null?null:((Number)x[8]).longValue();} }
}
