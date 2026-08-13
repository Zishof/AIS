package ais.common.newui.asset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.asset.AssetDetail;
import ais.database.model.asset.JenisAsset;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.rab.SatuanKerja;

/** Native inventory/depreciation workflow mirroring AssetDetailAction without ZK runtime. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class NewUiAssetDepreciationService {
    public Snapshot load(Filter f) {
        Session s=HibernateUtil.openSession();
        try {
            Snapshot o=new Snapshot();
            Number n=(Number)criteria(s,f,false).setProjection(Projections.rowCount()).uniqueResult();
            o.total=n==null?0:n.intValue();
            for(Object v:criteria(s,f,true).setFirstResult(Math.max(0,f.page)*f.size).setMaxResults(f.size).list()) o.rows.add(row(s,(AssetDetail)v,f.asOf));
            return o;
        } finally { s.close(); }
    }

    public List<DepreciationRow> details(Long id) {
        Session s=HibernateUtil.openSession();
        try {
            AssetDetail asset=require(s,id); List<DepreciationRow> out=new ArrayList<DepreciationRow>();
            for(Object v:s.createCriteria(PenyusutanAsset.class).add(Restrictions.eq("assetDetail",asset)).addOrder(Order.desc("perTanggal")).addOrder(Order.desc("id")).list()) out.add(depreciation((PenyusutanAsset)v));
            return out;
        } finally { s.close(); }
    }

    public Options options() {
        Session s=HibernateUtil.openSession();
        try {
            Options o=new Options();
            for(Object v:s.createCriteria(JenisAsset.class).addOrder(Order.asc("nama")).list()){JenisAsset x=(JenisAsset)v;o.assetTypes.add(new Option(x.getId(),x.getNama(),null));}
            for(Object v:s.createCriteria(Ruang.class).addOrder(Order.asc("nama")).list()){Ruang x=(Ruang)v;o.rooms.add(new Option(x.getId(),x.getNama(),null));}
            Set<SatuanKerja> allowed=ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();for(Object v:s.createCriteria(SatuanKerja.class).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).list()){SatuanKerja x=(SatuanKerja)v;if(allowed==null||allowed.isEmpty()||allowed.contains(x))o.workUnits.add(new Option(x.getId(),x.getKode()+" - "+x.getNama(),x.getParent()==null?null:x.getParent().getId()));}
            return o;
        } finally { s.close(); }
    }

    public int sync(Date target, Progress progress) {
        if(target==null) throw new IllegalArgumentException("Tanggal penyusutan wajib diisi.");
        Session s=HibernateUtil.openSession(); List<Long> ids=new ArrayList<Long>();
        try { for(Object v:s.createCriteria(AssetDetail.class).add(Restrictions.gt("hargaBeli",Double.valueOf(.1))).add(Restrictions.gt("nilaiMinimal",Double.valueOf(.1))).add(Restrictions.isNotNull("tanggalBeli")).add(Restrictions.gt("umurEkonomis",Double.valueOf(.1))).setProjection(Projections.id()).list()) ids.add((Long)v); }
        finally { s.close(); }
        progress.total=ids.size(); progress.done=0; progress.failed=0; progress.running=true;
        for(Long id:ids){try{syncOne(id,target);progress.done++;}catch(RuntimeException e){progress.failed++;try{ais.common.ErrorAuditUtil.record(e,"NewUiAssetDepreciationService.sync asset="+id);}catch(Exception ignored){}}}
        progress.running=false; return progress.done;
    }

    private void syncOne(Long id,Date target){Session s=HibernateUtil.openSession();Transaction tx=null;try{AssetDetail x=require(s,id);int months=months(x.getTanggalBeli(),target);if(months<0)return;tx=s.beginTransaction();Set<Integer> existing=new HashSet<Integer>();for(Object v:s.createCriteria(PenyusutanAsset.class).add(Restrictions.eq("assetDetail",x)).setProjection(Projections.property("tahunKe")).list())existing.add((Integer)v);for(int i=0;i<=months;i++)if(!existing.contains(Integer.valueOf(i))){PenyusutanAsset p=new PenyusutanAsset();p.setAssetDetail(x);p.setTahunKe(Integer.valueOf(i));s.save(p);}s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    private Criteria criteria(Session s,Filter f,boolean order){Criteria c=s.createCriteria(AssetDetail.class,"d").createAlias("asset","asset").createAlias("asset.masterAsset","master");Set<SatuanKerja>allowed=ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();if(allowed!=null&&!allowed.isEmpty())c.add(Restrictions.or(Restrictions.isNull("satuanKerja"),Restrictions.in("satuanKerja",allowed)));if(clean(f.query)!=null)c.add(Restrictions.or(Restrictions.ilike("nama",clean(f.query),MatchMode.ANYWHERE),Restrictions.ilike("barcode",clean(f.query),MatchMode.ANYWHERE)));if(clean(f.brand)!=null)c.add(Restrictions.ilike("master.merk",clean(f.brand),MatchMode.ANYWHERE));if(f.assetTypeId!=null)c.add(Restrictions.eq("master.jenisAsset.id",f.assetTypeId));if(f.roomId!=null)c.add(Restrictions.eq("asset.ruang.id",f.roomId));if(f.workUnitIds!=null&&!f.workUnitIds.isEmpty())c.add(Restrictions.or(Restrictions.isNull("satuanKerja"),Restrictions.in("satuanKerja.id",f.workUnitIds)));if(f.start!=null)c.add(Restrictions.ge("tanggalBeli",startOfDay(f.start)));if(f.end!=null)c.add(Restrictions.lt("tanggalBeli",nextDay(f.end)));if(order)c.addOrder(Order.desc("tanggalBeli")).addOrder(Order.desc("id"));return c;}
    private Row row(Session s,AssetDetail x,Date asOf){Row r=new Row();r.id=x.getId();r.name=x.getNama();r.barcode=x.getBarcode();r.note=x.getKeterangan();r.purchaseDate=x.getTanggalBeli();r.purchaseValue=n(x.getHargaBeli());r.minimumValue=n(x.getNilaiMinimal());r.economicLife=n(x.getUmurEkonomis());if(x.getAsset()!=null&&x.getAsset().getMasterAsset()!=null){r.masterAsset=x.getAsset().getMasterAsset().getNama();r.brand=x.getAsset().getMasterAsset().getMerk();if(x.getAsset().getMasterAsset().getJenisAsset()!=null)r.assetType=x.getAsset().getMasterAsset().getJenisAsset().getNama();}if(x.getRuang()!=null)r.room=x.getRuang().getNama();if(x.getSatuanKerja()!=null)r.workUnit=x.getSatuanKerja().getNama();PenyusutanAsset p=(PenyusutanAsset)s.createCriteria(PenyusutanAsset.class).add(Restrictions.eq("assetDetail",x)).add(Restrictions.le("perTanggal",asOf==null?new Date():asOf)).addOrder(Order.desc("perTanggal")).setMaxResults(1).uniqueResult();if(p!=null){r.month=p.getTahunKe();r.lastDate=p.getPerTanggal();r.monthlyDepreciation=n(p.getNilaiPenyusutan());r.accumulated=r.monthlyDepreciation*(r.month==null?0:r.month.intValue());r.bookValue=n(p.getNilaiBuku());}else r.bookValue=r.purchaseValue;return r;}
    private DepreciationRow depreciation(PenyusutanAsset p){DepreciationRow r=new DepreciationRow();r.id=p.getId();r.month=p.getTahunKe();r.date=p.getPerTanggal();r.purchaseValue=p.getAssetDetail()==null?0:n(p.getAssetDetail().getHargaBeli());r.monthly=n(p.getNilaiPenyusutan());r.accumulated=r.monthly*(r.month==null?0:r.month.intValue());r.bookValue=n(p.getNilaiBuku());r.note=p.getKeterangan();return r;}
    private static AssetDetail require(Session s,Long id){AssetDetail x=id==null?null:(AssetDetail)s.get(AssetDetail.class,id);if(x==null)throw new IllegalArgumentException("Detail aset tidak ditemukan.");return x;}
    private static int months(Date a,Date b){Calendar x=Calendar.getInstance(),y=Calendar.getInstance();x.setTime(a);y.setTime(b);return(y.get(Calendar.YEAR)-x.get(Calendar.YEAR))*12+y.get(Calendar.MONTH)-x.get(Calendar.MONTH);}
    private static Date startOfDay(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}private static Date nextDay(Date d){Calendar c=Calendar.getInstance();c.setTime(startOfDay(d));c.add(Calendar.DATE,1);return c.getTime();}
    private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}private static double n(Number v){return v==null?0:v.doubleValue();}private static void rollback(Transaction t){if(t!=null)try{t.rollback();}catch(Exception ignored){}}
    public static final class Filter{public String query,brand;public Long assetTypeId,roomId;public Set<Long>workUnitIds;public Date start,end,asOf=new Date();public int page,size=20;}
    public static final class Snapshot{public int total;public final List<Row>rows=new ArrayList<Row>();}
    public static final class Row{public Long id;public Integer month;public String name,barcode,note,masterAsset,brand,assetType,room,workUnit;public Date purchaseDate,lastDate;public double purchaseValue,minimumValue,economicLife,monthlyDepreciation,accumulated,bookValue;}
    public static final class DepreciationRow{public Long id;public Integer month;public Date date;public String note;public double purchaseValue,monthly,accumulated,bookValue;}
    public static final class Options{public final List<Option>assetTypes=new ArrayList<Option>(),rooms=new ArrayList<Option>(),workUnits=new ArrayList<Option>();}
    public static final class Option{public final Long id,parentId;public final String label;Option(Long i,String l,Long p){id=i;label=l;parentId=p;}}
    public static final class Progress{public volatile int total,done,failed;public volatile boolean running;}
}
