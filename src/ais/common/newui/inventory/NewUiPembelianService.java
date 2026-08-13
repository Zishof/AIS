package ais.common.newui.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ais.action.master.sekolah.util.DepositHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.sekolah.Siswa;

/** Port headless PembelianAction dan PembelianPunyaBarangHelper, tanpa runtime ZK. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class NewUiPembelianService {
    /** Padanan request-safe Common.getCurrentToko(); helper lama bergantung pada ZK Sessions. */
    public Toko currentShop(Tbmuser user){Session s=HibernateUtil.openSession();try{if(user!=null){Pedagang merchant=(Pedagang)s.createCriteria(Pedagang.class).add(Restrictions.eq("tbmuser",user)).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE))).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();if(merchant!=null&&merchant.getToko()!=null)return merchant.getToko();}List rows=s.createCriteria(Toko.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE))).setMaxResults(2).list();return rows.size()==1?(Toko)rows.get(0):null;}finally{s.close();}}
    public Snapshot load(String query,Long shopId,Long studentId,Date start,Date end,int page,int size,Toko currentShop,Tbmuser user){
        Session s=HibernateUtil.openSession();try{Snapshot out=new Snapshot();Criteria c=criteria(s,query,shopId,studentId,start,end,currentShop,user);
            Number count=(Number)c.setProjection(Projections.rowCount()).uniqueResult();out.total=count==null?0:count.intValue();
            Criteria rows=criteria(s,query,shopId,studentId,start,end,currentShop,user).addOrder(Order.desc("id")).setFirstResult(Math.max(0,page)*size).setMaxResults(size);
            List values=rows.list();for(int i=0;i<values.size();i++)out.rows.add(new PurchaseRow((Pembelian)values.get(i)));
            Object[] sums=(Object[])criteria(s,query,shopId,studentId,start,end,currentShop,user).setProjection(Projections.projectionList().add(Projections.sum("hargaSatuan")).add(Projections.sum("hargaJual"))).uniqueResult();
            out.unitTotal=number(sums==null?null:sums[0]);out.grandTotal=number(sums==null?null:sums[1]);
            Criteria shops=s.createCriteria(Toko.class).add(Restrictions.eq("aktif",Boolean.TRUE)).addOrder(Order.asc("nama"));
            if(currentShop!=null&&currentShop.getId()!=null&&!currentShop.getBolehMelihatTokolain())shops.add(Restrictions.idEq(currentShop.getId()));
            List shopRows=shops.list();for(int i=0;i<shopRows.size();i++)out.shops.add(new Option((Toko)shopRows.get(i)));
            return out;
        }finally{s.close();}
    }

    public List<Option> findProducts(Long shopId,String query,Toko currentShop){
        Session s=HibernateUtil.openSession();try{Toko shop=shop(s,shopId,currentShop);Criteria c=s.createCriteria(Produk.class).add(Restrictions.eq("aktif",Boolean.TRUE))
                .add(Restrictions.or(Restrictions.eq("toko",shop),Restrictions.isNull("toko")));
            String q=clean(query);if(q!=null)c.add(Restrictions.or(Restrictions.ilike("kode",q,MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("barcode",q,MatchMode.ANYWHERE),Restrictions.ilike("nama",q,MatchMode.ANYWHERE))));
            List rows=c.addOrder(Order.asc("nama")).setMaxResults(50).list();List<Option> out=new ArrayList<Option>();for(int i=0;i<rows.size();i++)out.add(new Option((Produk)rows.get(i)));return out;
        }finally{s.close();}
    }

    public List<Option> findMembers(String type,String query,Tbmuser user){
        Session s=HibernateUtil.openSession();try{List<Option> out=new ArrayList<Option>();String q=clean(query);
            if("student".equals(type)){Criteria c=s.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa",""));
                if(user!=null&&user.getSiswa()!=null)c.add(Restrictions.idEq(user.getSiswa().getId()));else if(q!=null)c.add(Restrictions.or(Restrictions.ilike("namaSiswa",q,MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("nomorInduk",q,MatchMode.ANYWHERE),Restrictions.ilike("nomorIndukNasional",q,MatchMode.ANYWHERE))));
                List rows=c.addOrder(Order.asc("namaSiswa")).setMaxResults(30).list();for(int i=0;i<rows.size();i++)out.add(new Option((Siswa)rows.get(i)));
            }else{if(user!=null&&user.getSiswa()!=null)return out;Criteria c=s.createCriteria(Mahasiswa.class);if(q!=null)c.add(Restrictions.or(Restrictions.ilike("nama",q,MatchMode.ANYWHERE),Restrictions.ilike("nim",q,MatchMode.ANYWHERE)));List rows=c.addOrder(Order.asc("nama")).setMaxResults(30).list();for(int i=0;i<rows.size();i++)out.add(new Option((Mahasiswa)rows.get(i)));}
            return out;
        }finally{s.close();}
    }

    public PurchaseRow save(Long id,String invoice,Long shopId,Long studentId,Long collegeStudentId,String note,List<LineInput> lines,Toko currentShop,Tbmuser user){
        if(user!=null&&user.getSiswa()!=null)throw new IllegalArgumentException("Akun siswa hanya dapat melihat riwayat pembelian.");
        if(clean(invoice)==null)throw new IllegalArgumentException("Kode invoice wajib diisi.");if(lines==null||lines.isEmpty())throw new IllegalArgumentException("Minimal satu barang wajib dipilih.");if(studentId!=null&&collegeStudentId!=null)throw new IllegalArgumentException("Pilih siswa atau mahasiswa, bukan keduanya.");
        Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();Toko store=shop(s,shopId,currentShop);Siswa student=studentId==null?null:(Siswa)s.get(Siswa.class,studentId);Mahasiswa college=collegeStudentId==null?null:(Mahasiswa)s.get(Mahasiswa.class,collegeStudentId);
            if(studentId!=null&&student==null)throw new IllegalArgumentException("Siswa tidak ditemukan.");if(collegeStudentId!=null&&college==null)throw new IllegalArgumentException("Mahasiswa tidak ditemukan.");if(user!=null&&user.getSiswa()!=null&&(student==null||!user.getSiswa().getId().equals(student.getId())))throw new IllegalArgumentException("Akun siswa hanya boleh melihat transaksinya sendiri.");
            validateInvoiceOwner(s,clean(invoice),student,college);List<ResolvedLine> resolved=resolveLines(s,lines,store);double total=total(resolved);validateBalance(student,college,total);validateDailyMaximum(s,student,resolved,id);
            Date now=ais.ui.util.WaktuUtil.getDate();PurchaseRow first=null;for(int i=0;i<resolved.size();i++){ResolvedLine line=resolved.get(i);Pembelian value=i==0&&id!=null?(Pembelian)s.get(Pembelian.class,id):new Pembelian();if(i==0&&id!=null&&value==null)throw new IllegalArgumentException("Baris pembelian tidak ditemukan.");if(i==0&&id!=null)ensureVisible(value,currentShop,user);
                value.setKode(clean(invoice));value.setProduk(line.product);value.setQty(Double.valueOf(line.quantity));value.setHargaSatuan(line.product.getHargaJual());value.setHargaJual(Double.valueOf(line.amount));value.setSiswa(student);value.setMahasiswa(college);value.setMember(member(student,college));value.setToko(store);value.setKeterangan(clean(note));value.setWaktu(now);value.setAktif(Boolean.TRUE);stamp(value,user);s.saveOrUpdate(value);if(first==null)first=new PurchaseRow(value);}
            s.flush();tx.commit();return first;
        }catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}
    }

    public void delete(Long id,Toko currentShop,Tbmuser user){if(user!=null&&user.getSiswa()!=null)throw new IllegalArgumentException("Akun siswa hanya dapat melihat riwayat pembelian.");Session s=HibernateUtil.openSession();Transaction tx=null;try{tx=s.beginTransaction();Pembelian v=(Pembelian)s.get(Pembelian.class,id);if(v==null)throw new IllegalArgumentException("Pembelian tidak ditemukan.");ensureVisible(v,currentShop,user);s.delete(v);s.flush();tx.commit();}catch(RuntimeException e){rollback(tx);throw e;}finally{s.close();}}

    private Criteria criteria(Session s,String query,Long shopId,Long studentId,Date start,Date end,Toko currentShop,Tbmuser user){Criteria c=s.createCriteria(Pembelian.class,"p");if(user!=null&&user.getSiswa()!=null)c.add(Restrictions.eq("siswa",s.load(Siswa.class,user.getSiswa().getId())));else if(studentId!=null)c.add(Restrictions.eq("siswa",s.load(Siswa.class,studentId)));
        if(currentShop!=null&&currentShop.getId()!=null&&!currentShop.getBolehMelihatTokolain())c.add(Restrictions.eq("toko",s.load(Toko.class,currentShop.getId())));else if(shopId!=null)c.add(Restrictions.eq("toko",s.load(Toko.class,shopId)));if(start!=null)c.add(Restrictions.ge("waktu",dayStart(start)));if(end!=null)c.add(Restrictions.le("waktu",dayEnd(end)));
        String q=clean(query);if(q!=null){c.createAlias("produk","pr",Criteria.LEFT_JOIN);c.createAlias("siswa","st",Criteria.LEFT_JOIN);c.createAlias("mahasiswa","mh",Criteria.LEFT_JOIN);c.add(Restrictions.or(Restrictions.ilike("kode",q,MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("pr.nama",q,MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("pr.kode",q,MatchMode.ANYWHERE),Restrictions.or(Restrictions.ilike("st.namaSiswa",q,MatchMode.ANYWHERE),Restrictions.ilike("mh.nama",q,MatchMode.ANYWHERE))))));}return c;}
    private Toko shop(Session s,Long id,Toko current){if(id==null&&current!=null)id=current.getId();if(id==null)throw new IllegalArgumentException("Toko / penjual wajib dipilih.");Toko v=(Toko)s.get(Toko.class,id);if(v==null||!v.getAktif())throw new IllegalArgumentException("Toko tidak ditemukan atau tidak aktif.");if(current!=null&&current.getId()!=null&&!current.getBolehMelihatTokolain()&&!current.getId().equals(id))throw new IllegalArgumentException("Toko berada di luar akses pengguna.");return v;}
    private List<ResolvedLine> resolveLines(Session s,List<LineInput> inputs,Toko shop){List<ResolvedLine> out=new ArrayList<ResolvedLine>();Map<Long,ResolvedLine> unique=new HashMap<Long,ResolvedLine>();for(int i=0;i<inputs.size();i++){LineInput in=inputs.get(i);if(in.productId==null)throw new IllegalArgumentException("Produk wajib dipilih.");if(in.quantity<=0)throw new IllegalArgumentException("Qty harus lebih besar dari nol.");Produk p=(Produk)s.get(Produk.class,in.productId);if(p==null||!p.getAktif())throw new IllegalArgumentException("Produk tidak ditemukan atau tidak aktif.");if(p.getToko()!=null&&!shop.getId().equals(p.getToko().getId()))throw new IllegalArgumentException("Produk tidak tersedia pada toko terpilih.");double price=number(p.getHargaJual());ResolvedLine old=unique.get(p.getId());if(old==null){old=new ResolvedLine(p,in.quantity,price*in.quantity);unique.put(p.getId(),old);out.add(old);}else{old.quantity+=in.quantity;old.amount=old.quantity*price;}}return out;}
    private void validateInvoiceOwner(Session s,String code,Siswa student,Mahasiswa college){Criteria c=s.createCriteria(Pembelian.class).add(Restrictions.ilike("kode",code,MatchMode.EXACT));List rows=c.list();for(int i=0;i<rows.size();i++){Pembelian p=(Pembelian)rows.get(i);if(student!=null&&p.getSiswa()!=null&&!student.getId().equals(p.getSiswa().getId()))throw new IllegalArgumentException("Kode invoice sudah digunakan siswa lain.");if(college!=null&&p.getMahasiswa()!=null&&!college.getId().equals(p.getMahasiswa().getId()))throw new IllegalArgumentException("Kode invoice sudah digunakan mahasiswa lain.");if(student!=null&&p.getMahasiswa()!=null)throw new IllegalArgumentException("Kode invoice sudah digunakan mahasiswa lain.");if(college!=null&&p.getSiswa()!=null)throw new IllegalArgumentException("Kode invoice sudah digunakan siswa lain.");}}
    private void validateBalance(Siswa s,Mahasiswa m,double total){if(s!=null&&DepositHelper.hitungDeposit(s,null)<total)throw new IllegalArgumentException("Saldo tabungan siswa tidak mencukupi.");if(m!=null&&DepositHelper.hitungDeposit(m)<total)throw new IllegalArgumentException("Saldo tabungan mahasiswa tidak mencukupi.");}
    private void validateDailyMaximum(Session s,Siswa student,List<ResolvedLine> lines,Long editedId){if(student==null)return;Map<Long,Double> totals=new HashMap<Long,Double>();List old=s.createCriteria(Pembelian.class).add(Restrictions.eq("siswa",student)).add(Restrictions.sqlRestriction("date(waktu)=CURRENT_DATE")).list();for(int i=0;i<old.size();i++){Pembelian p=(Pembelian)old.get(i);if(editedId!=null&&editedId.equals(p.getId()))continue;addType(totals,p.getProduk(),number(p.getHargaJual()));}for(int i=0;i<lines.size();i++)addType(totals,lines.get(i).product,lines.get(i).amount);for(int i=0;i<lines.size();i++){JenisProduk type=lines.get(i).product.getJenisProduk();if(type!=null&&type.getId()!=null&&type.getMaksimalHarian()!=null&&totals.get(type.getId()).doubleValue()>type.getMaksimalHarian().doubleValue())throw new IllegalArgumentException("Batas pembelian harian jenis produk "+type.getNama()+" adalah "+type.getMaksimalHarian()+".");}}
    private void addType(Map<Long,Double> m,Produk p,double amount){if(p==null||p.getJenisProduk()==null||p.getJenisProduk().getId()==null)return;Long id=p.getJenisProduk().getId();Double old=m.get(id);m.put(id,Double.valueOf((old==null?0:old.doubleValue())+amount));}
    private void ensureVisible(Pembelian p,Toko shop,Tbmuser user){if(user!=null&&user.getSiswa()!=null&&(p.getSiswa()==null||!user.getSiswa().getId().equals(p.getSiswa().getId())))throw new IllegalArgumentException("Pembelian berada di luar akses siswa.");if(shop!=null&&shop.getId()!=null&&!shop.getBolehMelihatTokolain()&&(p.getToko()==null||!shop.getId().equals(p.getToko().getId())))throw new IllegalArgumentException("Pembelian berada di luar toko aktif.");}
    private static String member(Siswa s,Mahasiswa m){if(s!=null)return s.getNomorInduk()+" "+s.getNama();if(m!=null)return m.getNim()+" "+m.getNama();return "1";}private static void stamp(Pembelian p,Tbmuser u){if(u!=null){p.setOleh(u.getUserNama());p.setOlehId(u.getUserId());}}
    private static double total(List<ResolvedLine> rows){double v=0;for(int i=0;i<rows.size();i++)v+=rows.get(i).amount;return v;}private static double number(Object v){return v==null?0:((Number)v).doubleValue();}private static String clean(String v){return v==null||v.trim().length()==0?null:v.trim();}private static Date dayStart(Date d){return new Date(d.getYear(),d.getMonth(),d.getDate(),0,0,0);}private static Date dayEnd(Date d){return new Date(d.getYear(),d.getMonth(),d.getDate(),23,59,59);}private static void rollback(Transaction t){if(t!=null)try{t.rollback();}catch(Exception ignored){}}
    private static final class ResolvedLine{final Produk product;double quantity,amount;ResolvedLine(Produk p,double q,double a){product=p;quantity=q;amount=a;}}
    public static final class LineInput{public final Long productId;public final double quantity;public LineInput(Long p,double q){productId=p;quantity=q;}}
    public static final class Snapshot{public final List<PurchaseRow>rows=new ArrayList<PurchaseRow>();public final List<Option>shops=new ArrayList<Option>();public int total;public double unitTotal,grandTotal;}
    public static final class Option{public final Long id;public final String label,code,barcode;public final double price;Option(Toko v){id=v.getId();label=v.getNama();code=null;barcode=null;price=0;}Option(Produk v){id=v.getId();label=v.getNama();code=v.getKode();barcode=v.getBarcode();price=number(v.getHargaJual());}Option(Siswa v){id=v.getId();label=v.getNama();code=v.getNomorInduk();barcode=null;price=0;}Option(Mahasiswa v){id=v.getId();label=v.getNama();code=v.getNim();barcode=null;price=0;}}
    public static final class PurchaseRow{public final Long id,productId,shopId,studentId,collegeStudentId;public final String invoice,productCode,product,member,shop,note;public final double quantity,unitPrice,amount;public final Date time;PurchaseRow(Pembelian v){id=v.getId();invoice=v.getKode();productId=v.getProduk()==null?null:v.getProduk().getId();productCode=v.getProduk()==null?"":v.getProduk().getKode();product=v.getProduk()==null?"":v.getProduk().getNama();quantity=number(v.getQty());unitPrice=number(v.getHargaSatuan());amount=number(v.getHargaJual());time=v.getWaktu();member=v.getMember();shopId=v.getToko()==null?null:v.getToko().getId();shop=v.getToko()==null?"":v.getToko().getNama();studentId=v.getSiswa()==null?null:v.getSiswa().getId();collegeStudentId=v.getMahasiswa()==null?null:v.getMahasiswa().getId();note=v.getKeterangan();}}
}
