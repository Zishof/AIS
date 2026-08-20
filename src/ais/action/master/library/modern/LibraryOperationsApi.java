package ais.action.master.library.modern;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.library.Item;
import ais.database.model.library.ItemKomentar;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.DetailTransaksi;
import ais.database.model.library.KodeTransaksi;
import ais.database.model.library.KoreksiItem;
import ais.database.model.library.KoreksiItemDetail;
import ais.database.model.library.KembaliPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.SearchHistory;
import ais.database.model.library.Serial;

/** Audited operational workspace built on the existing library schema. */
public final class LibraryOperationsApi {
    private LibraryOperationsApi() { }

    public static JSONObject handle(HttpServletRequest request) throws Exception {
        Tbmuser user=Common.getCurrentUser(request);if(user==null)return error("Silakan masuk terlebih dahulu.");
        String action=text(request.getParameter("action"),40);boolean admin=Common.getApakahAdmin();
        boolean mutation="serial_claim".equals(action)||"serial_resolve".equals(action)
                ||"inventory_create".equals(action)||"inventory_scan".equals(action)||"inventory_close".equals(action)
                ||"comment_add".equals(action)||"comment_moderate".equals(action)
                ||"search_save".equals(action)||"search_remove".equals(action)
                ||"fine_payment".equals(action)||"fine_waive".equals(action)||"fine_reverse".equals(action);
        if(mutation&&(!"POST".equalsIgnoreCase(request.getMethod())||!NewUiCsrfUtil.isValid(request)))return error("Permintaan mutasi atau token keamanan tidak valid.");
        JSONObject result;
        if("overview".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=overview();}
        else if("serial_list".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=serialList();}
        else if("serial_claim".equals(action)||"serial_resolve".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=serialEvent(request,user,"serial_resolve".equals(action));}
        else if("inventory_list".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=inventoryList();}
        else if("inventory_create".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=inventoryCreate(request,user);}
        else if("inventory_scan".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=inventoryScan(request);}
        else if("inventory_close".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=inventoryClose(request,user);}
        else if("fine_list".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=fineList();}
        else if("fine_payment".equals(action)||"fine_waive".equals(action)||"fine_reverse".equals(action)){if(!admin)return error("Hak petugas diperlukan.");result=fineAction(request,user,action);}
        else if("search_list".equals(action))result=searchList(user);
        else if("search_save".equals(action))result=searchSave(request,user);
        else if("search_remove".equals(action))result=searchRemove(request,user);
        else if("comment_list".equals(action))result=commentList(request,user,admin);
        else if("comment_add".equals(action))result=commentAdd(request,user);
        else if("comment_moderate".equals(action)){if(!admin)return error("Hak moderator diperlukan.");result=commentModerate(request);}
        else result=error("Operasi tidak dikenal.");
        result.put("csrf",NewUiCsrfUtil.getToken(request.getSession()));return result;
    }

    private static JSONObject overview()throws Exception{Session s=null;try{s=HibernateUtil.openSession();JSONObject c=new JSONObject();
        c.put("overdue",number(s.createSQLQuery("select count(d.id) from library.peminjaman_pengadaan_item_detail d where d.kembali_pengadaan_item_detail is null and d.batas_waktu_pengembalian<current_timestamp").uniqueResult()));
        c.put("openHolds",number(s.createSQLQuery("select count(id) from library.pesanan_anggota where lower(coalesce(status,'')) not in ('batal','selesai','diambil')").uniqueResult()));
        c.put("pendingComments",number(s.createSQLQuery("select count(id) from library.item_komentar where coalesce(kontak,'PENDING')='PENDING'").uniqueResult()));
        c.put("openInventories",number(s.createSQLQuery("select count(id) from library.koreksi_item where tanggal_persetujuan is null and coalesce(keterangan,'') like '[STOCKTAKE]%'").uniqueResult()));
        c.put("activeSerials",number(s.createSQLQuery("select count(id) from library.serial where mulai<=current_date and (selesai is null or selesai>=current_date)").uniqueResult()));
        return ok().put("counts",c).put("definitions",new JSONObject().put("loans","Transaksi peminjaman disetujui pada periode laporan (ISO 2789).") .put("visits","Kunjungan fisik yang tercatat pada lokasi perpustakaan.").put("titles","Record bibliografis aktif; bukan jumlah eksemplar.").put("copies","Eksemplar/barcode fisik yang tercatat."));
    }finally{HibernateUtil.closeSessionQuietly(s);}}

    @SuppressWarnings("unchecked") private static JSONObject serialList()throws Exception{Session s=null;try{s=HibernateUtil.openSession();JSONArray a=new JSONArray();for(Serial x:(List<Serial>)s.createCriteria(Serial.class).addOrder(Order.desc("mulai")).setMaxResults(100).list()){Calendar next=Calendar.getInstance();next.setTime(x.getMulai()==null?new Date():x.getMulai());while(next.getTime().before(new Date()))advance(next,x.getPeriode());a.put(new JSONObject().put("id",x.getId()).put("title",x.getItem()==null?"-":safe(x.getItem().getNama())).put("period",safe(x.getPeriode())).put("nextExpected",Common.dateFormat1.get().format(next.getTime())).put("note",safe(x.getKeterangan())));}return ok().put("data",a);}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject serialEvent(HttpServletRequest r,Tbmuser u,boolean resolve)throws Exception{Long id=positiveLong(r.getParameter("id"));String reason=text(r.getParameter("reason"),240);if(id==null||reason==null)return error("Serial dan alasan wajib diisi.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();Serial x=(Serial)s.get(Serial.class,id);if(x==null)return error("Serial tidak ditemukan.");String event="[CLAIM date="+date()+" status="+(resolve?"RESOLVED":"OPEN")+" actor="+safe(u.getUserId())+"] "+reason;tx=s.beginTransaction();x.setKeterangan(append(x.getKeterangan(),event,3900));s.update(x);tx.commit();return ok().put("message",resolve?"Klaim diselesaikan.":"Klaim penerbit dicatat.");}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    @SuppressWarnings("unchecked") private static JSONObject inventoryList()throws Exception{Session s=null;try{s=HibernateUtil.openSession();JSONArray a=new JSONArray();for(KoreksiItem x:(List<KoreksiItem>)s.createCriteria(KoreksiItem.class).add(Restrictions.like("keterangan","[STOCKTAKE]%")).addOrder(Order.desc("tanggalPembuatan")).setMaxResults(50).list()){long scanned=number(s.createSQLQuery("select count(id) from library.koreksi_item_detail where koreksi_item=:id").setLong("id",x.getId()).uniqueResult());a.put(new JSONObject().put("id",x.getId()).put("code",safe(x.getKode())).put("note",safe(x.getKeterangan())).put("scanned",scanned).put("closed",x.getTanggalPersetujuan()!=null));}return ok().put("data",a);}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject inventoryCreate(HttpServletRequest r,Tbmuser u)throws Exception{String note=text(r.getParameter("note"),180);Perpustakaan library=Common.getCurrentPerpustakaan();if(library==null)return error("Perpustakaan aktif belum dipilih.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();KoreksiItem x=new KoreksiItem();x.setKode("STK-"+System.currentTimeMillis());x.setKeterangan("[STOCKTAKE] "+safe(note));x.setTanggalPembuatan(new Date());x.setDibuatOleh(u);x.setPerpustakaan((Perpustakaan)s.get(Perpustakaan.class,library.getId()));tx=s.beginTransaction();s.save(x);tx.commit();return ok().put("message","Sesi stocktake dibuat.").put("id",x.getId()).put("code",x.getKode());}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject inventoryScan(HttpServletRequest r)throws Exception{
        Long sessionId=positiveLong(r.getParameter("id"));String barcode=text(r.getParameter("barcode"),100);
        if(sessionId==null||barcode==null)return error("Sesi dan barcode wajib diisi.");
        Session s=null;Transaction tx=null;
        try{
            s=HibernateUtil.openSession();KoreksiItem k=(KoreksiItem)s.get(KoreksiItem.class,sessionId);
            if(k==null||k.getTanggalPersetujuan()!=null)return error("Sesi stocktake tidak aktif.");
            ItemPunyaBarcode copy=(ItemPunyaBarcode)s.createCriteria(ItemPunyaBarcode.class).add(Restrictions.eq("barcode",barcode)).setMaxResults(1).uniqueResult();
            Item item=copy==null?null:copy.getItem();
            if(item==null)item=(Item)s.createQuery("from Item where kode=:code or isbn=:code or temporaryBarcode=:code").setString("code",barcode).setMaxResults(1).uniqueResult();
            if(item==null)return error("Item/barcode tidak ditemukan.");
            KoreksiItemDetail d=(KoreksiItemDetail)s.createCriteria(KoreksiItemDetail.class).add(Restrictions.eq("koreksiItem.id",sessionId)).add(Restrictions.eq("item.id",item.getId())).setMaxResults(1).uniqueResult();
            String marker="["+barcode+"]";if(d!=null&&safe(d.getKeterangan()).contains(marker))return error("Barcode sudah dipindai pada sesi ini.");
            if(k.getPerpustakaan()==null)return error("Lokasi stocktake tidak valid.");
            double stock=decimal(s.createSQLQuery("select coalesce(sum((coalesce(d.qty,0)+coalesce(d.qtybonus,0))*t.jenis),0) from library.detail_transaksi d join library.kode_transaksi t on t.id=d.kode_transaksi where d.item=:i and d.perpustakaan=:p").setLong("i",item.getId()).setLong("p",k.getPerpustakaan().getId()).uniqueResult());
            tx=s.beginTransaction();
            if(d==null){d=new KoreksiItemDetail();d.setKoreksiItem(k);d.setItem(item);d.setStok(stock);d.setStokmenjadi(1D);d.setKeterangan("STOCKTAKE "+marker);}
            else{double physical=(d.getStokmenjadi()==null?0D:d.getStokmenjadi())+1D;d.setStokmenjadi(physical);d.setKeterangan(append(d.getKeterangan(),marker,240));}
            double difference=d.getStokmenjadi()-stock;d.setJumlah(Math.abs(difference));String adjustment=difference<0?"ADK":"ADT";KodeTransaksi code=(KodeTransaksi)s.createCriteria(KodeTransaksi.class).add(Restrictions.eq("kode",adjustment)).setMaxResults(1).uniqueResult();if(code==null){rollback(tx);return error("Kode transaksi koreksi "+adjustment+" belum dikonfigurasi.");}d.setKodeTransaksi(code);if(d.getId()==null)s.save(d);else s.update(d);
            tx.commit();return ok().put("message","Eksemplar dipindai.").put("item",new JSONObject().put("id",item.getId()).put("title",safe(item.getNama())).put("systemStock",stock).put("physicalCount",d.getStokmenjadi()));
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}
    }
    @SuppressWarnings("unchecked") private static JSONObject inventoryClose(HttpServletRequest r,Tbmuser u)throws Exception{
        Long id=positiveLong(r.getParameter("id"));if(id==null)return error("Sesi tidak valid.");Session s=null;Transaction tx=null;
        try{s=HibernateUtil.openSession();KoreksiItem k=(KoreksiItem)s.get(KoreksiItem.class,id);if(k==null||k.getTanggalPersetujuan()!=null)return error("Sesi tidak aktif.");if(k.getPerpustakaan()==null)return error("Lokasi stocktake tidak valid.");
            List<KoreksiItemDetail> details=(List<KoreksiItemDetail>)s.createCriteria(KoreksiItemDetail.class).add(Restrictions.eq("koreksiItem.id",id)).list();if(details.isEmpty())return error("Sesi belum memiliki hasil scan.");
            long posted=number(s.createSQLQuery("select count(t.id) from library.detail_transaksi t join library.koreksi_item_detail d on d.id=t.koreksi_item_detail where d.koreksi_item=:id").setLong("id",id).uniqueResult());if(posted>0)return error("Ledger stocktake sudah pernah diposting.");
            for(KoreksiItemDetail d:details)if(d.getKodeTransaksi()==null)return error("Jenis koreksi pada detail belum lengkap.");Date now=new Date();tx=s.beginTransaction();for(KoreksiItemDetail d:details){DetailTransaksi ledger=new DetailTransaksi();ledger.setKoreksiItemDetail(d);ledger.setQtyBonus(0D);ledger.setItem(d.getItem());ledger.setKeterangan("Stocktake "+safe(d.getKodeTransaksi().getNama()));ledger.setKodeTransaksi(d.getKodeTransaksi());ledger.setPerpustakaan(k.getPerpustakaan());ledger.setQty(d.getJumlah());ledger.setTanggal(now);ledger.setTanggalDanWaktu(now);s.save(ledger);}k.setTanggalPersetujuan(now);k.setDisetujuiOleh(u);k.setKeterangan(append(k.getKeterangan(),"[CLOSED "+date()+"]",240));s.update(k);tx.commit();return ok().put("message","Sesi stocktake ditutup dan ledger persediaan diposting.").put("posted",details.size());
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    @SuppressWarnings("unchecked") private static JSONObject fineList()throws Exception{Session s=null;try{s=HibernateUtil.openSession();JSONArray a=new JSONArray();for(KembaliPengadaanItemDetail x:(List<KembaliPengadaanItemDetail>)s.createCriteria(KembaliPengadaanItemDetail.class).add(Restrictions.gt("denda",0D)).addOrder(Order.desc("tanggal")).setMaxResults(100).list()){String member="-";try{member=x.getPeminjamanPengadaanItemDetail().getPeminjamanPengadaanItem().getAnggota().getNama();}catch(Exception ignored){}a.put(new JSONObject().put("id",x.getId()).put("member",safe(member)).put("item",x.getItem()==null?"-":safe(x.getItem().getNama())).put("assessed",x.getDenda()).put("paid",x.getDibayarSejumlah()).put("settled",x.getTelahDibayar()).put("note",safe(x.getKetDenda())));}return ok().put("data",a);}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject fineAction(HttpServletRequest r,Tbmuser u,String action)throws Exception{Long id=positiveLong(r.getParameter("id"));String reason=text(r.getParameter("reason"),240);if(id==null||reason==null)return error("Denda dan alasan wajib diisi.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();KembaliPengadaanItemDetail x=(KembaliPengadaanItemDetail)s.get(KembaliPengadaanItemDetail.class,id);if(x==null||x.getDenda()<=0)return error("Denda tidak ditemukan.");double amount=decimalValue(r.getParameter("amount"));String event;if("fine_payment".equals(action)){if(x.getTelahDibayar())return error("Denda sudah diselesaikan.");double remaining=x.getDenda()-x.getDibayarSejumlah();if(amount<=0||amount>remaining)return error("Nominal pembayaran melebihi sisa denda.");double paid=Math.min(x.getDenda(),x.getDibayarSejumlah()+amount);x.setDibayarSejumlah(paid);x.setTelahDibayar(paid>=x.getDenda());event="PAYMENT amount="+amount;}else if("fine_waive".equals(action)){if(x.getTelahDibayar())return error("Denda sudah diselesaikan.");x.setDibayarSejumlah(x.getDenda());x.setTelahDibayar(true);event="WAIVER amount="+x.getDenda();}else{x.setDibayarSejumlah(0D);x.setTelahDibayar(false);event="REVERSAL";}event="["+event+" date="+date()+" actor="+safe(u.getUserId())+"] "+reason;tx=s.beginTransaction();x.setKetDenda(append(x.getKetDenda(),event,3900));s.update(x);tx.commit();return ok().put("message","Ledger denda diperbarui.");}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    @SuppressWarnings("unchecked") private static JSONObject searchList(Tbmuser u)throws Exception{Session s=null;try{s=HibernateUtil.openSession();JSONArray a=new JSONArray();for(SearchHistory x:(List<SearchHistory>)s.createCriteria(SearchHistory.class).add(Restrictions.eq("olehId",u.getUserId())).add(Restrictions.ge("banyak",0)).addOrder(Order.desc("tanggal_dirubah")).setMaxResults(30).list())a.put(new JSONObject().put("id",x.getId()).put("label",safe(x.getRes())).put("query",safe(x.getQuer())));return ok().put("data",a);}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject searchSave(HttpServletRequest r,Tbmuser u)throws Exception{String label=text(r.getParameter("label"),100),query=text(r.getParameter("query"),500);if(label==null||query==null)return error("Nama dan query pencarian wajib diisi.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();SearchHistory x=new SearchHistory();x.setOlehId(u.getUserId());x.setOleh(u.getUserNama());x.setRes(label);x.setQuer(query);x.setStart(0);x.setBanyak(0);tx=s.beginTransaction();s.save(x);tx.commit();return ok().put("message","Pencarian disimpan.").put("id",x.getId());}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject searchRemove(HttpServletRequest r,Tbmuser u)throws Exception{Long id=positiveLong(r.getParameter("id"));Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();SearchHistory x=id==null?null:(SearchHistory)s.get(SearchHistory.class,id);if(x==null||!safe(u.getUserId()).equals(safe(x.getOlehId())))return error("Pencarian tidak ditemukan.");tx=s.beginTransaction();x.setBanyak(-1);x.setRes("[REMOVED] "+safe(x.getRes()));s.update(x);tx.commit();return ok().put("message","Pencarian dihapus.");}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    @SuppressWarnings("unchecked") private static JSONObject commentList(HttpServletRequest r,Tbmuser u,boolean admin)throws Exception{Session s=null;try{s=HibernateUtil.openSession();Long itemId=positiveLong(r.getParameter("itemId"));org.hibernate.Criteria c=s.createCriteria(ItemKomentar.class);if(itemId!=null)c.add(Restrictions.eq("item.id",itemId));if(!admin)c.add(Restrictions.eq("kontak","APPROVED"));c.addOrder(Order.desc("tanggal_dirubah")).setMaxResults(100);JSONArray a=new JSONArray();for(ItemKomentar x:(List<ItemKomentar>)c.list())a.put(new JSONObject().put("id",x.getId()).put("itemId",x.getItem()==null?0:x.getItem().getId()).put("item",x.getItem()==null?"-":safe(x.getItem().getNama())).put("author",safe(x.getNama())).put("comment",safe(x.getAlamat())).put("status",safe(x.getKontak())));return ok().put("data",a);}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject commentAdd(HttpServletRequest r,Tbmuser u)throws Exception{Long itemId=positiveLong(r.getParameter("itemId"));String body=text(r.getParameter("comment"),240);if(itemId==null||body==null)return error("Item dan komentar wajib diisi.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();Item item=(Item)s.get(Item.class,itemId);if(item==null)return error("Item tidak ditemukan.");ItemKomentar x=new ItemKomentar();x.setItem(item);String author=text(u.getUserNama(),200);x.setNama(author==null?safe(u.getUserId()):author);x.setEmail(text(u.getEmail(),200));x.setAlamat(body);x.setKontak("PENDING");x.setOlehId(u.getUserId());x.setOleh(author==null?safe(u.getUserId()):author);tx=s.beginTransaction();s.save(x);tx.commit();return ok().put("message","Komentar menunggu moderasi.");}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}
    private static JSONObject commentModerate(HttpServletRequest r)throws Exception{Long id=positiveLong(r.getParameter("id"));String status=text(r.getParameter("status"),20);if(!"APPROVED".equals(status)&&!"REJECTED".equals(status))return error("Status moderasi tidak valid.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();ItemKomentar x=id==null?null:(ItemKomentar)s.get(ItemKomentar.class,id);if(x==null)return error("Komentar tidak ditemukan.");tx=s.beginTransaction();x.setKontak(status);s.update(x);tx.commit();return ok().put("message","Moderasi disimpan.");}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    private static void advance(Calendar c,String p){String v=safe(p).toLowerCase();if(v.contains("hari")||v.contains("daily"))c.add(Calendar.DATE,1);else if(v.contains("minggu")||v.contains("week"))c.add(Calendar.DATE,7);else if(v.contains("triwulan")||v.contains("quarter"))c.add(Calendar.MONTH,3);else if(v.contains("semester")||v.contains("semi"))c.add(Calendar.MONTH,6);else if(v.contains("tahun")||v.contains("annual"))c.add(Calendar.YEAR,1);else c.add(Calendar.MONTH,1);}
    private static String append(String old,String event,int max){String v=(safe(old).trim()+"\n"+event).trim();return v.length()>max?v.substring(v.length()-max):v;}
    private static String date(){return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());}
    private static String text(String v,int max){if(v==null)return null;v=v.trim().replaceAll("[\\r\\n]+"," ");return v.length()==0?null:(v.length()>max?v.substring(0,max):v);}
    private static String safe(Object v){return v==null?"":String.valueOf(v);}private static Long positiveLong(String v){try{long n=Long.parseLong(v);return n>0?n:null;}catch(Exception e){return null;}}
    private static long number(Object v){return v instanceof Number?((Number)v).longValue():0;}private static double decimal(Object v){return v instanceof Number?((Number)v).doubleValue():0D;}
    private static double decimalValue(String v){try{return Double.parseDouble(v);}catch(Exception e){return 0D;}}
    private static JSONObject ok()throws Exception{return new JSONObject().put("ok",true).put("status","success");}private static JSONObject error(String m)throws Exception{return new JSONObject().put("ok",false).put("status","error").put("error",m).put("message",m);}
    private static void rollback(Transaction tx){try{if(tx!=null&&tx.isActive())tx.rollback();}catch(Exception ignored){}}
}
