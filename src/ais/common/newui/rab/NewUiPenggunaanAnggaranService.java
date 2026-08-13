package ais.common.newui.rab;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.Transaksi;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.rab.Workspace;

/** Headless parity untuk daftar dan proses ulang PenggunaanAnggaranAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiPenggunaanAnggaranService {
    public static final String JURNAL = "jurnal";
    public static final String PENGADAAN = "pengadaan";
    public static final String UANG_MUKA = "uang_muka";
    public static final String PERTANGGUNGJAWABAN = "pertanggungjawaban";
    public static final String SALDO_AWAL = "saldo_awal";
    public static final String GAJI = "gaji";
    public static final String KAS_KECIL = "kas_kecil";
    public static final String KAS_BESAR = "kas_besar";

    public Snapshot load(Filter f) {
        normalize(f);
        Session session = HibernateUtil.openSession();
        try {
            Snapshot out = new Snapshot();
            Number count=(Number)criteria(session,f,false).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();out.total=count==null?0:count.intValue();
            Number amount=(Number)criteria(session,f,false).setProjection(org.hibernate.criterion.Projections.sum("nilai")).uniqueResult();out.totalAmount=amount==null?0:amount.doubleValue();
            if(f.activeOnly){out.active=out.total;out.inactive=0;}else{Number active=(Number)criteria(session,f,false).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",true))).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();out.active=active==null?0:active.intValue();out.inactive=out.total-out.active;}
            String[][] facets={{JURNAL,"Jurnal Umum","grupTransaksi"},{PENGADAAN,"Pengadaan","permintaanPengadaanMasterAssetDetail"},{UANG_MUKA,"Uang Muka","uangMuka"},{PERTANGGUNGJAWABAN,"Pertanggungjawaban","pertangungjawaban"},{SALDO_AWAL,"Saldo Awal Asset","saldoAwalMasterAssetDetail"},{GAJI,"Pembayaran Gaji","pembayaranGaji"},{KAS_KECIL,"Kas Kecil","kasKecil"},{KAS_BESAR,"Kas Besar","kasBesar"}};
            for(int i=0;i<facets.length;i++)if(f.enabled(facets[i][0])){Number n=(Number)criteria(session,f,false).add(Restrictions.isNotNull(facets[i][2])).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();if(n!=null&&n.intValue()>0)out.sourceCounts.put(facets[i][1],Integer.valueOf(n.intValue()));}
            List<PenggunaanAnggaran> pageRows=criteria(session,f,true).setFirstResult(f.page*f.size).setMaxResults(f.size).list();
            for(PenggunaanAnggaran value:pageRows){Row row=toRow(value);out.rows.add(row);if(row.transferStatus!=null&&row.transferStatus.length()>0)increment(out.transferCounts,row.transferStatus);}
            out.workspaces.addAll(workspaceOptions(session));
            return out;
        } finally {
            session.close();
        }
    }

    /** Memanggil rule domain PenggunaanAnggaran.prosesSimpan untuk delapan sumber existing. */
    public ReprocessResult reprocess(Filter f, Progress progress) {
        normalize(f);
        if (f.start == null || f.end == null) throw new IllegalArgumentException("Tanggal mulai dan akhir wajib diisi.");
        ReprocessResult result = new ReprocessResult();
        Session session = HibernateUtil.openSession();
        try {
            List<SourceSpec> specs = sourceSpecs();
            int selected = 0;
            for (SourceSpec spec : specs) if (f.enabled(spec.key)) selected++;
            if (selected == 0) throw new IllegalArgumentException("Pilih minimal satu sumber anggaran.");
            int done = 0;
            for (SourceSpec spec : specs) {
                if (!f.enabled(spec.key)) continue;
                progress.update(percent(done, selected), "Memproses " + spec.label + "...");
                List values = sourceCriteria(session, spec, f).list();
                for (int i = 0; i < values.size(); i++) {
                    PenggunaanAnggaran.prosesSimpan((Serializable) values.get(i), session);
                    result.processed++;
                }
                result.perSource.put(spec.label, Integer.valueOf(values.size()));
                values.clear();
                try { session.clear(); } catch (Exception ignored) { }
                done++;
            }
            progress.update(95, "Membersihkan referensi duplikat...");
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                result.duplicatesRemoved = session.createSQLQuery(
                    "delete from rab.penggunaan_anggaran where id in "
                    + "(select id from (select id,row_number() over(partition by ref order by id) rn "
                    + "from rab.penggunaan_anggaran where ref is not null) d where rn>1)").executeUpdate();
                tx.commit();
            } catch (RuntimeException e) {
                if (tx != null) try { tx.rollback(); } catch (Exception ignored) { }
                throw e;
            }
            progress.update(100, "Proses ulang selesai.");
            return result;
        } finally {
            session.close();
        }
    }

    /** Read-only validation atas seluruh criteria sumber sebelum proses mutasi dijalankan. */
    public Map<String,Integer> previewSources(Filter f) {
        normalize(f);if(f.start==null||f.end==null)throw new IllegalArgumentException("Tanggal mulai dan akhir wajib diisi.");
        Session s=HibernateUtil.openSession();try{Map<String,Integer>out=new LinkedHashMap<String,Integer>();for(SourceSpec spec:sourceSpecs())if(f.enabled(spec.key)){Number n=(Number)sourceCriteria(s,spec,f).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();out.put(spec.label,Integer.valueOf(n==null?0:n.intValue()));}return out;}finally{s.close();}
    }

    /** Riwayat immutable Envers, setara tautan RevisiHelper pada renderer lama. */
    public List<History> history(Long id) {
        if (id == null) throw new IllegalArgumentException("ID penggunaan anggaran wajib diisi.");
        Session s=HibernateUtil.openSession();try{
            List<Object[]> values=s.createSQLQuery("select a.rev,a.revtype,r.revtstmp,a.oleh,a.kode,a.nama,a.nilai,a.aktif,a.keterangan "
                +"from new_audit.penggunaan_anggaran__audit a left join new_audit.revinfo r on r.rev=a.rev "
                +"where a.id=:id order by a.rev desc").setLong("id",id.longValue()).list();
            List<History> out=new ArrayList<History>();for(Object[]x:values)out.add(new History(x));return out;
        }finally{s.close();}
    }

    private Criteria criteria(Session s, Filter f, boolean order) {
        Criteria c = s.createCriteria(PenggunaanAnggaran.class);
        if (f.workspaceId != null) {
            List<Long> ids = Workspace.getAllWorkspaceIds(s, f.workspaceId);
            if (ids.isEmpty()) c.add(Restrictions.eq("workspace.id", f.workspaceId));
            else c.add(Restrictions.in("workspace.id", ids));
        }
        if (f.activeOnly) c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
        if (f.start != null) c.add(Restrictions.ge("waktu", startOfDay(f.start)));
        if (f.end != null) c.add(Restrictions.le("waktu", endOfDay(f.end)));
        String q = clean(f.q);
        if (q != null) c.add(Restrictions.or(Restrictions.ilike("kode", q, MatchMode.ANYWHERE),
                Restrictions.ilike("nama", q, MatchMode.ANYWHERE)));
        List source = new ArrayList();
        if (f.enabled(JURNAL)) source.add(Restrictions.isNotNull("grupTransaksi"));
        if (f.enabled(PENGADAAN)) source.add(Restrictions.isNotNull("permintaanPengadaanMasterAssetDetail"));
        if (f.enabled(UANG_MUKA)) source.add(Restrictions.isNotNull("uangMuka"));
        if (f.enabled(PERTANGGUNGJAWABAN)) source.add(Restrictions.isNotNull("pertangungjawaban"));
        if (f.enabled(SALDO_AWAL)) source.add(Restrictions.isNotNull("saldoAwalMasterAssetDetail"));
        if (f.enabled(GAJI)) source.add(Restrictions.isNotNull("pembayaranGaji"));
        if (f.enabled(KAS_KECIL)) source.add(Restrictions.isNotNull("kasKecil"));
        if (f.enabled(KAS_BESAR)) source.add(Restrictions.isNotNull("kasBesar"));
        if (source.size() < 8) {
            if (source.isEmpty()) c.add(Restrictions.sqlRestriction("false"));
            else { org.hibernate.criterion.Criterion joined=(org.hibernate.criterion.Criterion)source.get(0);for(int i=1;i<source.size();i++)joined=Restrictions.or(joined,(org.hibernate.criterion.Criterion)source.get(i));c.add(joined); }
        }
        if (order) c.addOrder(Order.desc("waktu")).addOrder(Order.desc("id"));
        return c;
    }

    private Criteria sourceCriteria(Session s, SourceSpec spec, Filter f) {
        Criteria c = s.createCriteria(spec.type);
        if (JURNAL.equals(spec.key)) {
            c.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM)).add(Restrictions.isNotNull("workspace"));
            between(c, "tanggalTransaksi", f);
        } else if (PENGADAAN.equals(spec.key)) {
            c.createAlias("permintaanPengadaanMasterAsset", "master").add(Restrictions.isNotNull("master.workspace"));
            between(c, "tanggalPembuatan", f);
            if (f.activeOnly) c.add(Restrictions.or(Restrictions.isNull("master.aktif"), Restrictions.eq("master.aktif", true)));
        } else if (UANG_MUKA.equals(spec.key)) {
            c.add(Restrictions.isNotNull("workspace")); between(c, "tanggalPembuatan", f); active(c, f);
        } else if (PERTANGGUNGJAWABAN.equals(spec.key)) {
            c.createAlias("uangMuka", "um").add(Restrictions.isNotNull("um.workspace"));
            between(c, "tanggalPersetujuan", f); active(c, f);
        } else if (SALDO_AWAL.equals(spec.key)) {
            c.createAlias("saldoAwal", "saldo").add(Restrictions.isNotNull("workspace"));
            c.add(Restrictions.sqlRestriction("date(tanggal_pembuatan) between ? and ?",
                new Object[]{startOfDay(f.start),endOfDay(f.end)},
                new org.hibernate.type.Type[]{org.hibernate.Hibernate.DATE,org.hibernate.Hibernate.DATE}));
            if (f.activeOnly) c.add(Restrictions.or(Restrictions.isNull("saldo.aktif"), Restrictions.eq("saldo.aktif", true)));
        } else if (GAJI.equals(spec.key)) {
            c.add(Restrictions.isNotNull("workspace")); between(c, "tanggalPembuatan", f); active(c, f);
        } else {
            between(c, "tanggalPembuatan", f); active(c, f);
        }
        return c;
    }

    private static void between(Criteria c, String field, Filter f) {
        c.add(Restrictions.ge(field, startOfDay(f.start))).add(Restrictions.le(field, endOfDay(f.end)));
    }
    private static void active(Criteria c, Filter f) {
        if (f.activeOnly) c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
    }
    private static Date startOfDay(Date d) { Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime(); }
    private static Date endOfDay(Date d) { Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,23);c.set(Calendar.MINUTE,59);c.set(Calendar.SECOND,59);c.set(Calendar.MILLISECOND,999);return c.getTime(); }

    private List<WorkspaceOption> workspaceOptions(Session s) {
        List<Workspace> values = s.createCriteria(Workspace.class)
            .add(Restrictions.or(Restrictions.eq("carryOver", true),
                Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
            .addOrder(Order.desc("tahunWorkspace")).addOrder(Order.asc("nama")).list();
        List<WorkspaceOption> out = new ArrayList<WorkspaceOption>();
        for (Workspace w : values) out.add(new WorkspaceOption(w));
        return out;
    }

    private Row toRow(PenggunaanAnggaran v) {
        Row r = new Row(); r.id=v.getId(); r.code=v.getKode(); r.name=v.getNama(); r.note=v.getKeterangan();
        r.time=v.getWaktu(); r.amount=value(v.getNilai()); r.active=Boolean.TRUE.equals(v.getAktif()); r.ref=v.getRef();
        Workspace w=v.getWorkspace(); if(w!=null){r.workspaceId=w.getId();r.workspace=w.getNama();r.budget=value(w.getHargaTotal());}
        r.source=source(v); if(v.getDisposisiSop()!=null){r.sopId=v.getDisposisiSop().getId();r.sop=v.getDisposisiSop().getKeterangan();if(v.getDisposisiSop().getSop()!=null)r.sopName=v.getDisposisiSop().getSop().getNama();}
        r.transferStatus=transferStatus(transfer(v)); return r;
    }
    private static DaftarPengajuanTransfer transfer(PenggunaanAnggaran v){
        if(v.getPertangungjawaban()!=null)return v.getPertangungjawaban().getDaftarPengajuanTransfer();
        if(v.getUangMuka()!=null)return v.getUangMuka().getDaftarPengajuanTransfer();
        if(v.getKasBesar()!=null)return v.getKasBesar().getDaftarPengajuanTransfer();
        if(v.getKasKecil()!=null&&v.getKasKecil().getPenggantianKasKecil()!=null)return v.getKasKecil().getPenggantianKasKecil().getDaftarPengajuanTransfer();
        return null;
    }
    private static String transferStatus(DaftarPengajuanTransfer d){
        if(d==null)return ""; if(d.getProsesTransfer()==null)return "Menunggu proses transfer";
        if(Boolean.TRUE.equals(d.getTransfer())&&d.getProsesTransfer().getCaraPembayaranTransfer()!=null)
            return "Ditransfer via "+d.getProsesTransfer().getCaraPembayaranTransfer().getNama();
        return "Dalam proses transfer";
    }
    static String source(PenggunaanAnggaran v){if(v.getGrupTransaksi()!=null)return"Jurnal Umum";if(v.getPermintaanPengadaanMasterAssetDetail()!=null)return"Pengadaan";if(v.getUangMuka()!=null)return"Uang Muka";if(v.getPertangungjawaban()!=null)return"Pertanggungjawaban";if(v.getSaldoAwalMasterAssetDetail()!=null)return"Saldo Awal Asset";if(v.getPembayaranGaji()!=null)return"Pembayaran Gaji";if(v.getKasKecil()!=null)return"Kas Kecil";if(v.getKasBesar()!=null)return"Kas Besar";return"Tidak diketahui";}
    private static List<SourceSpec> sourceSpecs(){List<SourceSpec>x=new ArrayList<SourceSpec>();x.add(new SourceSpec(UANG_MUKA,"Uang Muka",UangMuka.class));x.add(new SourceSpec(PENGADAAN,"Pengadaan Master Asset",PermintaanPengadaanMasterAssetDetail.class));x.add(new SourceSpec(GAJI,"Pembayaran Gaji",PembayaranGaji.class));x.add(new SourceSpec(KAS_KECIL,"Kas Kecil",KasKecil.class));x.add(new SourceSpec(KAS_BESAR,"Kas Besar",KasBesar.class));x.add(new SourceSpec(JURNAL,"Jurnal Umum",GrupTransaksi.class));x.add(new SourceSpec(SALDO_AWAL,"Saldo Awal Asset",SaldoAwalMasterAssetDetail.class));x.add(new SourceSpec(PERTANGGUNGJAWABAN,"Pertanggungjawaban",Pertangungjawaban.class));return x;}
    private static int percent(int done,int total){return total==0?0:Math.min(90,done*90/total);}private static void increment(Map<String,Integer>m,String k){Integer v=m.get(k);m.put(k,Integer.valueOf(v==null?1:v.intValue()+1));}private static double value(Double v){return v==null?0:v.doubleValue();}private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}private static void normalize(Filter f){if(f==null)throw new IllegalArgumentException("Filter wajib tersedia.");f.page=Math.max(0,f.page);f.size=Math.max(10,Math.min(100,f.size));}

    public interface Progress { void update(int percent,String message); }
    public static final class Filter { public String q;public Long workspaceId;public Date start,end;public boolean activeOnly=true;public int page,size=20;public final Map<String,Boolean> sources=new LinkedHashMap<String,Boolean>();public boolean enabled(String key){Boolean v=sources.get(key);return v==null||v.booleanValue();} }
    public static final class Snapshot { public final List<Row>rows=new ArrayList<Row>();public final List<WorkspaceOption>workspaces=new ArrayList<WorkspaceOption>();public final Map<String,Integer>sourceCounts=new LinkedHashMap<String,Integer>(),transferCounts=new LinkedHashMap<String,Integer>();public int total,active,inactive;public double totalAmount; }
    public static final class Row { public Long id,workspaceId,sopId;public String code,name,note,ref,workspace,source,sop,sopName,transferStatus;public Date time;public double amount,budget;public boolean active; }
    public static final class WorkspaceOption { public final Long id;public final String label;WorkspaceOption(Workspace w){id=w.getId();label=(w.getTahunWorkspace()==null?"":w.getTahunWorkspace()+" · ")+w.getNama();} }
    public static final class ReprocessResult { public int processed,duplicatesRemoved;public final Map<String,Integer>perSource=new LinkedHashMap<String,Integer>(); }
    public static final class History { public final int revision,type;public final long timestamp;public final String by,code,name,note;public final double amount;public final Boolean active;History(Object[]x){revision=((Number)x[0]).intValue();type=x[1]==null?0:((Number)x[1]).intValue();timestamp=x[2]==null?0:((Number)x[2]).longValue();by=x[3]==null?"":String.valueOf(x[3]);code=x[4]==null?"":String.valueOf(x[4]);name=x[5]==null?"":String.valueOf(x[5]);amount=x[6]==null?0:((Number)x[6]).doubleValue();active=x[7]==null?null:(Boolean)x[7];note=x[8]==null?"":String.valueOf(x[8]);} }
    private static final class SourceSpec { final String key,label;final Class type;SourceSpec(String k,String l,Class t){key=k;label=l;type=t;} }
}
