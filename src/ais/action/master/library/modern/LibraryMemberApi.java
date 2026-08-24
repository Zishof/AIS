package ais.action.master.library.modern;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.library.Anggota;
import ais.database.model.library.Item;
import ais.database.model.library.ItemFavoritAnggota;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.PesananAnggota;
import ais.database.model.library.SearchHistory;

/** Typed, member-scoped API for the self-service library portal. */
public final class LibraryMemberApi {
    private LibraryMemberApi() { }

    /** Shared gate for member-only views such as the digital reader. */
    public static boolean isActiveMember(HttpServletRequest request) {
        try {
            MemberContext context = member(request);
            return context != null && !Boolean.FALSE.equals(context.member.getAktif());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static JSONObject handle(HttpServletRequest request) throws JSONException {
        JSONObject result;
        try {
            MemberContext context = member(request);
            if (context == null) return error("Silakan masuk sebagai anggota perpustakaan.");
            String action = value(request.getParameter("action"));
            boolean mutation = "renew".equals(action) || "hold".equals(action)
                    || "hold_cancel".equals(action) || "favorite_toggle".equals(action);
            if (mutation && !"POST".equalsIgnoreCase(request.getMethod())) return error("Mutasi hanya diizinkan melalui POST.");
            if (mutation && !NewUiCsrfUtil.isValid(request)) return error("Token keamanan tidak valid. Muat ulang halaman lalu coba lagi.");

            if ("summary".equals(action)) result = summary(context);
            else if ("loans".equals(action) || "history_pinjam".equals(action)) result = loans(context, request);
            else if ("holds".equals(action) || "history_pesan".equals(action)) result = holds(context, request);
            else if ("visits".equals(action) || "history_kunjung".equals(action)) result = visits(context, request);
            else if ("favorites".equals(action)) result = favorites(context, request);
            else if ("renew".equals(action)) result = renew(context, positiveLong(request.getParameter("detailId")));
            else if ("hold".equals(action)) result = hold(context, positiveLong(request.getParameter("itemId")), positiveLong(request.getParameter("libraryId")));
            else if ("hold_cancel".equals(action)) result = cancelHold(context, positiveLong(request.getParameter("holdId")));
            else if ("favorite_toggle".equals(action)) result = toggleFavorite(context, positiveLong(request.getParameter("itemId")));
            else result = error("Operasi portal anggota tidak dikenal.");
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            result = error("Layanan anggota belum dapat memproses permintaan.");
        }
        result.put("csrf", NewUiCsrfUtil.getToken(request.getSession()));
        return result;
    }

    private static JSONObject summary(MemberContext context) throws JSONException {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            long active = count(session, "select count(d.id) from library.peminjaman_pengadaan_item_detail d join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item where p.anggota=:member and d.kembali_pengadaan_item_detail is null", context.id);
            long dueSoon = count(session, "select count(d.id) from library.peminjaman_pengadaan_item_detail d join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item where p.anggota=:member and d.kembali_pengadaan_item_detail is null and d.batas_waktu_pengembalian between current_timestamp and current_timestamp + interval '3 days'", context.id);
            long overdue = count(session, "select count(d.id) from library.peminjaman_pengadaan_item_detail d join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item where p.anggota=:member and d.kembali_pengadaan_item_detail is null and d.batas_waktu_pengembalian < current_timestamp", context.id);
            long holds = count(session, "select count(id) from library.pesanan_anggota where anggota=:member and status=:status and kadaluarsa>=current_timestamp", context.id, PesananAnggota.PESAN);
            long favorites = count(session, "select count(id) from library.item_favorit_anggota where anggota=:member", context.id);
            long visits = count(session, "select count(id) from library.kunjungan_anggota where anggota=:member", context.id);
            long history = count(session, "select count(id) from library.peminjaman_pengadaan_item where anggota=:member", context.id);
            long searchAlerts = savedSearchAlerts(session,context.member);
            double assessedFine = decimal(session.createSQLQuery("select coalesce(sum(coalesce(r.denda,0)),0) from library.kembali_pengadaan_item_detail r join library.peminjaman_pengadaan_item_detail d on d.id=r.peminjaman_pengadaan_item_detail join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item where p.anggota=:member").setLong("member", context.id).uniqueResult());
            JSONObject data = new JSONObject().put("activeLoans", active).put("dueSoon", dueSoon).put("overdue", overdue)
                    .put("activeHolds", holds).put("favorites", favorites).put("visits", visits).put("loanHistory", history)
                    .put("assessedFine", assessedFine).put("savedSearchAlerts",searchAlerts).put("notifications", dueSoon + overdue + holds + searchAlerts)
                    .put("pinjam", history).put("kembali", history - active).put("pesan", holds).put("kunjung", visits);
            data.put("member", new JSONObject().put("id", context.id).put("code", safe(context.member.getKode()))
                    .put("name", safe(context.member.getNama())).put("active", !Boolean.FALSE.equals(context.member.getAktif())));
            return ok().put("data", data);
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject loans(MemberContext context, HttpServletRequest request) throws JSONException {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Page page = page(request);
            String keyword = keyword(request);
            String from = " from library.peminjaman_pengadaan_item_detail d join library.peminjaman_pengadaan_item p on p.id=d.peminjaman_pengadaan_item join library.item i on i.id=d.item where p.anggota=:member";
            if (keyword != null) from += " and (lower(coalesce(i.nama,'')) like :keyword or lower(coalesce(i.isbn,'')) like :keyword or lower(coalesce(p.kode,'')) like :keyword)";
            Query cq = session.createSQLQuery("select count(d.id)" + from); bind(cq, context.id, keyword);
            long total = number(cq.uniqueResult());
            Query q = session.createSQLQuery("select d.id,coalesce(p.kode,'-'),i.id,coalesce(i.nama,'-'),coalesce(i.pengarangs,'-'),to_char(p.tanggal_pembuatan,'DD-MM-YYYY HH24:MI'),to_char(d.batas_waktu_pengembalian,'DD-MM-YYYY'),d.jumlahperpanjangan,d.jumlahmaxperpanjangan,d.kembali_pengadaan_item_detail" + from + " order by p.tanggal_pembuatan desc,d.id desc");
            bind(q, context.id, keyword); q.setFirstResult(page.offset()).setMaxResults(page.size);
            JSONArray data = new JSONArray();
            for (Object[] row : (List<Object[]>) q.list()) {
                boolean returned = row[9] != null;
                data.put(new JSONObject().put("detailId", number(row[0])).put("kode", safe(row[1])).put("itemId", number(row[2]))
                        .put("title", safe(row[3])).put("authors", safe(row[4])).put("tanggal_pinjam", safe(row[5]))
                        .put("dueDate", safe(row[6])).put("renewals", number(row[7])).put("maxRenewals", number(row[8]))
                        .put("returned", returned).put("status", returned ? "Telah Dikembalikan" : "Sedang Dipinjam").put("tanggal_kembali", "-"));
            }
            return paged(data, total, page);
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject holds(MemberContext context, HttpServletRequest request) throws JSONException {
        Session session = null;
        try {
            session = HibernateUtil.openSession(); Page page=page(request); String keyword=keyword(request);
            String from=" from library.pesanan_anggota h join library.item i on i.id=h.item left join library.perpustakaan p on p.id=h.perpustakaan where h.anggota=:member";
            if(keyword!=null) from+=" and (lower(coalesce(i.nama,'')) like :keyword or lower(coalesce(i.isbn,'')) like :keyword or lower(coalesce(h.kode,'')) like :keyword)";
            Query cq=session.createSQLQuery("select count(h.id)"+from); bind(cq,context.id,keyword); long total=number(cq.uniqueResult());
            Query q=session.createSQLQuery("select h.id,coalesce(h.kode,'-'),i.id,coalesce(i.nama,'-'),coalesce(p.nama,'-'),to_char(h.tanggal,'DD-MM-YYYY HH24:MI'),to_char(h.kadaluarsa,'DD-MM-YYYY HH24:MI'),coalesce(h.status,'-')"+from+" order by h.tanggal desc");
            bind(q,context.id,keyword); q.setFirstResult(page.offset()).setMaxResults(page.size); JSONArray data=new JSONArray();
            for(Object[] row:(List<Object[]>)q.list()) data.put(new JSONObject().put("holdId",number(row[0])).put("kode",safe(row[1])).put("itemId",number(row[2])).put("buku",safe(row[3])).put("library",safe(row[4])).put("tanggal",safe(row[5])).put("expires",safe(row[6])).put("status",safe(row[7])));
            return paged(data,total,page);
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject visits(MemberContext context,HttpServletRequest request)throws JSONException{
        Session session=null; try{session=HibernateUtil.openSession();Page page=page(request);String keyword=keyword(request);
            String from=" from library.kunjungan_anggota k left join library.perpustakaan p on p.id=k.perpustakaan where k.anggota=:member";
            if(keyword!=null)from+=" and (lower(coalesce(k.keterangan,'')) like :keyword or lower(coalesce(p.nama,'')) like :keyword)";
            Query cq=session.createSQLQuery("select count(k.id)"+from);bind(cq,context.id,keyword);long total=number(cq.uniqueResult());
            Query q=session.createSQLQuery("select to_char(k.tanggal,'DD-MM-YYYY HH24:MI'),coalesce(p.nama,'-'),coalesce(k.keterangan,'-')"+from+" order by k.tanggal desc");bind(q,context.id,keyword);q.setFirstResult(page.offset()).setMaxResults(page.size);JSONArray data=new JSONArray();
            for(Object[] row:(List<Object[]>)q.list())data.put(new JSONObject().put("tanggal",safe(row[0])).put("perpustakaan",safe(row[1])).put("keterangan",safe(row[2])));
            return paged(data,total,page);
        }finally{HibernateUtil.closeSessionQuietly(session);}
    }

    @SuppressWarnings("unchecked")
    private static JSONObject favorites(MemberContext context,HttpServletRequest request)throws JSONException{
        Session session=null;try{session=HibernateUtil.openSession();Page page=page(request);
            Query cq=session.createSQLQuery("select count(id) from library.item_favorit_anggota where anggota=:member").setLong("member",context.id);long total=number(cq.uniqueResult());
            Query q=session.createSQLQuery("select f.id,i.id,coalesce(i.nama,'-'),coalesce(i.pengarangs,'-'),to_char(f.tanggal,'DD-MM-YYYY HH24:MI') from library.item_favorit_anggota f join library.item i on i.id=f.item where f.anggota=:member order by f.tanggal desc").setLong("member",context.id);q.setFirstResult(page.offset()).setMaxResults(page.size);JSONArray data=new JSONArray();
            for(Object[] row:(List<Object[]>)q.list())data.put(new JSONObject().put("favoriteId",number(row[0])).put("itemId",number(row[1])).put("title",safe(row[2])).put("authors",safe(row[3])).put("date",safe(row[4])));
            return paged(data,total,page);
        }finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static JSONObject renew(MemberContext context,Long detailId)throws Exception{
        if(detailId==null)return error("Peminjaman tidak valid.");Session session=null;Transaction tx=null;
        try{session=HibernateUtil.openSession();PeminjamanPengadaanItemDetail d=(PeminjamanPengadaanItemDetail)session.get(PeminjamanPengadaanItemDetail.class,detailId);
            if(d==null||d.getPeminjamanPengadaanItem()==null||d.getPeminjamanPengadaanItem().getAnggota()==null||!context.id.equals(d.getPeminjamanPengadaanItem().getAnggota().getId()))return error("Peminjaman tidak ditemukan.");
            if(d.getKembaliPengadaanItemDetail()!=null)return error("Buku sudah dikembalikan.");
            long queue=count(session,"select count(id) from library.pesanan_anggota where item=:item and anggota<>:member and status=:status and kadaluarsa>=current_timestamp",d.getItem().getId(),context.id,PesananAnggota.PESAN);
            if(queue>0)return error("Perpanjangan ditolak karena ada antrean reservasi.");
            int max=LibraryUtil.getJumlahMaksimalPerpanjanganPeminjaman(d.getPeminjamanPengadaanItem());int used=d.getJumlahPerpanjangan()==null?0:d.getJumlahPerpanjangan();
            if(used>=max)return error("Batas maksimal perpanjangan sudah tercapai.");
            tx=session.beginTransaction();d.setJumlahMaxPerpanjangan(max);d.setJumlahPerpanjangan(used+1);session.update(d);tx.commit();
            return ok().put("message","Peminjaman berhasil diperpanjang.").put("renewals",used+1).put("maxRenewals",max).put("dueDate",d.getBatasWaktupengembalian()==null?"":Common.dateFormat1.get().format(d.getBatasWaktupengembalian()));
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static JSONObject hold(MemberContext context,Long itemId,Long libraryId)throws Exception{
        if(itemId==null||libraryId==null)return error("Koleksi dan perpustakaan wajib dipilih.");if(Boolean.FALSE.equals(context.member.getAktif()))return error("Keanggotaan tidak aktif.");
        Session session=null;Transaction tx=null;try{session=HibernateUtil.openSession();Item item=(Item)session.get(Item.class,itemId);Perpustakaan library=(Perpustakaan)session.get(Perpustakaan.class,libraryId);
            if(item==null||Boolean.FALSE.equals(item.getAktif())||!isPublic(item))return error("Koleksi tidak ditemukan atau tidak lagi diterbitkan.");if(library==null||Boolean.FALSE.equals(library.getAktif()))return error("Perpustakaan tidak ditemukan.");
            List<Long> allowedLibraries=LibraryScopeResolver.allowedLibraryIds(session);if(allowedLibraries!=null&&!allowedLibraries.contains(libraryId))return error("Perpustakaan berada di luar scope institusi aktif.");
            long readOnly=number(session.createSQLQuery("select count(id) from library.item_has_status where item=:item and (perpustakaan=:library or perpustakaan is null) and lower(coalesce(status,'')) like '%tidak boleh%' and current_date between coalesce(mulai,current_date) and coalesce(sampai,current_date)").setLong("item",itemId).setLong("library",libraryId).uniqueResult());if(readOnly>0)return error("Koleksi ini hanya dapat dibaca di tempat dan tidak dapat direservasi.");
            long stock=number(session.createSQLQuery("select coalesce(sum((coalesce(d.qty,0)+coalesce(d.qtybonus,0))*k.jenis),0) from library.detail_transaksi d join library.kode_transaksi k on k.id=d.kode_transaksi where d.item=:item and d.perpustakaan=:library").setLong("item",itemId).setLong("library",libraryId).uniqueResult());
            if(stock<1)return error("Koleksi tidak tersedia pada perpustakaan yang dipilih.");
            long duplicate=count(session,"select count(id) from library.pesanan_anggota where anggota=:member and item=:item and perpustakaan=:library and status=:status and kadaluarsa>=current_timestamp",context.id,itemId,libraryId,PesananAnggota.PESAN);if(duplicate>0)return error("Koleksi ini sudah Anda reservasi.");
            Konfigurasi cfg=Common.getKonfigurasi("kadaluarsa.pemesanan.item","24");int hours=24;try{hours=Integer.parseInt(cfg.getNilai().trim());}catch(Exception ignored){hours=24;}Calendar cal=Calendar.getInstance();cal.add(Calendar.HOUR_OF_DAY,hours);
            PesananAnggota h=new PesananAnggota();h.setAnggota((Anggota)session.get(Anggota.class,context.id));h.setItem(item);h.setPerpustakaan(library);h.setTanggal(new Date());h.setKadaluarsa(cal.getTime());h.setKeterangan("Pemesanan online");h.setStatus(PesananAnggota.PESAN);
            tx=session.beginTransaction();session.save(h);tx.commit();return ok().put("message","Reservasi berhasil dibuat.").put("holdId",h.getId());
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static JSONObject cancelHold(MemberContext context,Long holdId)throws Exception{
        if(holdId==null)return error("Reservasi tidak valid.");Session session=null;Transaction tx=null;try{session=HibernateUtil.openSession();PesananAnggota h=(PesananAnggota)session.get(PesananAnggota.class,holdId);
            if(h==null||h.getAnggota()==null||!context.id.equals(h.getAnggota().getId()))return error("Reservasi tidak ditemukan.");if(!PesananAnggota.PESAN.equals(h.getStatus()))return error("Reservasi ini tidak dapat dibatalkan.");
            tx=session.beginTransaction();h.setStatus(PesananAnggota.BATAL);h.setKeterangan("Dibatalkan anggota melalui portal mandiri");session.update(h);tx.commit();return ok().put("message","Reservasi dibatalkan.");
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static JSONObject toggleFavorite(MemberContext context,Long itemId)throws Exception{
        if(itemId==null)return error("Koleksi tidak valid.");Session session=null;Transaction tx=null;try{session=HibernateUtil.openSession();Item item=(Item)session.get(Item.class,itemId);if(item==null||Boolean.FALSE.equals(item.getAktif())||!isPublic(item)||!inScope(session,itemId))return error("Koleksi tidak ditemukan atau tidak lagi diterbitkan.");
            ItemFavoritAnggota f=(ItemFavoritAnggota)session.createCriteria(ItemFavoritAnggota.class).add(Restrictions.eq("anggota.id",context.id)).add(Restrictions.eq("item.id",itemId)).setMaxResults(1).uniqueResult();tx=session.beginTransaction();boolean favorite;
            if(f==null){f=new ItemFavoritAnggota();f.setAnggota((Anggota)session.get(Anggota.class,context.id));f.setItem(item);f.setTanggal(new Date());f.setTgl(new Date());f.setKeterangan("Favorit portal anggota");session.save(f);favorite=true;}else{session.delete(f);favorite=false;}tx.commit();return ok().put("message",favorite?"Ditambahkan ke favorit.":"Dihapus dari favorit.").put("favorite",favorite);
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static MemberContext member(HttpServletRequest request){Tbmuser user=Common.getCurrentUser(request);if(user==null)return null;Anggota a=Anggota.buatAtauAmbilAnggota(user,false);return a==null||a.getId()==null?null:new MemberContext(a);}
    private static boolean isPublic(Item item){if(item==null||item.getStatusTerbitItem()==null||item.getStatusTerbitItem().getNama()==null)return false;String status=item.getStatusTerbitItem().getNama().trim().toLowerCase();return "terbit".equals(status)||"publish".equals(status)||"published".equals(status);}
    @SuppressWarnings("unchecked") private static long savedSearchAlerts(Session session,Anggota member){try{if(member==null||member.getTbmuser()==null)return 0;String userId=member.getTbmuser().getUserId();long alerts=0;for(SearchHistory x:(List<SearchHistory>)session.createCriteria(SearchHistory.class).add(Restrictions.eq("olehId",userId)).add(Restrictions.eq("start",1)).add(Restrictions.ge("banyak",0)).setMaxResults(30).list()){LibraryCatalogSearchRequest request=new LibraryCatalogSearchRequest();if(!"__ALL__".equals(x.getQuer()))request.setQuery(x.getQuer());long current=number(new LibraryCatalogSearchService().createCriteria(session,request).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult());if(current>(x.getBanyak()==null?0:x.getBanyak()))alerts++;}return alerts;}catch(Exception ignored){return 0;}}
    private static boolean inScope(Session session,Long itemId){LibraryCatalogSearchRequest request=new LibraryCatalogSearchRequest();return number(new LibraryCatalogSearchService().createCriteria(session,request).add(Restrictions.eq("id",itemId)).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult())>0;}
    private static JSONObject ok()throws JSONException{return new JSONObject().put("ok",true).put("status","success");}
    private static JSONObject error(String message)throws JSONException{return new JSONObject().put("ok",false).put("status","error").put("error",message).put("message",message);}
    private static JSONObject paged(JSONArray data,long total,Page page)throws JSONException{return ok().put("data",data).put("total",total).put("total_data",total).put("page",page.number).put("pageSize",page.size);}
    private static Page page(HttpServletRequest r){return new Page(bounded(r.getParameter("page"),1,100000,1),bounded(first(r.getParameter("pageSize"),r.getParameter("limit")),1,50,10));}
    private static String keyword(HttpServletRequest r){String k=value(r.getParameter("keyword"));return k==null?null:"%"+k.toLowerCase()+"%";}
    private static void bind(Query q,long member,String keyword){q.setLong("member",member);if(keyword!=null)q.setString("keyword",keyword);}
    private static long count(Session s,String sql,long member){return number(s.createSQLQuery(sql).setLong("member",member).uniqueResult());}
    private static long count(Session s,String sql,long member,String status){return number(s.createSQLQuery(sql).setLong("member",member).setString("status",status).uniqueResult());}
    private static long count(Session s,String sql,long item,long member,String status){return number(s.createSQLQuery(sql).setLong("item",item).setLong("member",member).setString("status",status).uniqueResult());}
    private static long count(Session s,String sql,long member,long item,long library,String status){return number(s.createSQLQuery(sql).setLong("member",member).setLong("item",item).setLong("library",library).setString("status",status).uniqueResult());}
    private static Long positiveLong(String raw){try{long v=Long.parseLong(raw);return v>0?Long.valueOf(v):null;}catch(Exception e){return null;}}
    private static int bounded(String raw,int min,int max,int fallback){try{int v=Integer.parseInt(raw);return v>=min&&v<=max?v:fallback;}catch(Exception e){return fallback;}}
    private static String value(String v){if(v==null||v.trim().length()==0)return null;v=v.trim();return v.length()>120?v.substring(0,120):v;}
    private static String first(String a,String b){return a==null?b:a;}
    private static String safe(Object value){return value==null?"":String.valueOf(value);}
    private static long number(Object value){return value instanceof Number?((Number)value).longValue():0L;}
    private static double decimal(Object value){return value instanceof Number?((Number)value).doubleValue():0D;}
    private static void rollback(Transaction tx){try{if(tx!=null&&tx.isActive())tx.rollback();}catch(Exception ignored){}}
    private static final class MemberContext{private final Anggota member;private final Long id;private MemberContext(Anggota member){this.member=member;this.id=member.getId();}}
    private static final class Page{private final int number,size;private Page(int number,int size){this.number=number;this.size=size;}private int offset(){return(number-1)*size;}}
}
