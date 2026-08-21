package ais.action.master.library.modern;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pesan;
import ais.database.model.PesanRuangan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.library.PermintaanPengadaanItem;
import ais.database.model.library.Perpustakaan;

/** Typed member-service endpoint backed by existing audited AIS entities. */
public final class LibraryEngagementApi {
    private LibraryEngagementApi() { }

    public static JSONObject handle(HttpServletRequest request) throws JSONException {
        Tbmuser user=Common.getCurrentUser(request);
        if(user==null)return error("Masuk terlebih dahulu untuk menggunakan layanan anggota.");
        String action=safe(request.getParameter("action"));
        boolean mutation="help_add".equals(action)||"ill_add".equals(action)||"acquisition_add".equals(action)||"booking_add".equals(action);
        if(mutation&&(!"POST".equalsIgnoreCase(request.getMethod())||!NewUiCsrfUtil.isValid(request)))return error("Permintaan tidak valid. Muat ulang halaman lalu coba lagi.");
        try{
            JSONObject result;
            if("references".equals(action))result=references();
            else if("requests".equals(action))result=requests(user);
            else if("help_add".equals(action))result=message(user,"ASK_LIBRARIAN",request);
            else if("ill_add".equals(action))result=message(user,"INTERLIBRARY_LOAN",request);
            else if("acquisition_add".equals(action))result=acquisition(user,request);
            else if("booking_add".equals(action))result=booking(user,request);
            else result=error("Operasi layanan anggota tidak dikenal.");
            result.put("csrf",NewUiCsrfUtil.getToken(request.getSession()));return result;
        }catch(Exception e){Common.tampilErrorJikaAdmin(e);return error("Layanan belum dapat memproses permintaan.").put("csrf",NewUiCsrfUtil.getToken(request.getSession()));}
    }

    @SuppressWarnings("unchecked") private static JSONObject references()throws Exception{
        Session s=null;try{s=HibernateUtil.openSession();Criteria c=s.createCriteria(Ruang.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",Boolean.TRUE))).addOrder(Order.asc("nama")).setMaxResults(100);Perpustakaan p=Common.getCurrentPerpustakaan();if(p!=null){if(p.getYayasan()!=null)c.add(Restrictions.eq("yayasan.id",p.getYayasan().getId()));if(p.getSekolah()!=null)c.add(Restrictions.eq("sekolah.id",p.getSekolah().getId()));if(p.getFakultas()!=null)c.add(Restrictions.eq("fakultas.id",p.getFakultas().getId()));if(p.getJurusan()!=null)c.add(Restrictions.eq("jurusan.id",p.getJurusan().getId()));}JSONArray rooms=new JSONArray();for(Ruang x:(List<Ruang>)c.list())rooms.put(new JSONObject().put("id",x.getId()).put("name",safe(x.getNama())).put("code",safe(x.getKodeRuangan())).put("capacity",x.getKapasitasRuangan()==null?0:x.getKapasitasRuangan()));return ok().put("rooms",rooms);}finally{HibernateUtil.closeSessionQuietly(s);}}

    @SuppressWarnings("unchecked") private static JSONObject requests(Tbmuser user)throws Exception{
        Session s=null;try{s=HibernateUtil.openSession();JSONArray data=new JSONArray();for(PermintaanPengadaanItem x:(List<PermintaanPengadaanItem>)s.createCriteria(PermintaanPengadaanItem.class).add(Restrictions.eq("olehId",user.getUserId())).add(Restrictions.like("keterangan","[USULAN ANGGOTA]%")).addOrder(Order.desc("id")).setMaxResults(30).list())data.put(new JSONObject().put("type","Usulan koleksi").put("code",safe(x.getKode())).put("description",safe(x.getKeterangan()).replaceFirst("^\\[USULAN ANGGOTA\\]\\s*","")).put("status",x.getTanggalPersetujuan()==null?"Sedang ditinjau":"Disetujui"));for(Pesan x:(List<Pesan>)s.createCriteria(Pesan.class).add(Restrictions.eq("olehId",user.getUserId())).add(Restrictions.or(Restrictions.like("isi","[ASK_LIBRARIAN]%"),Restrictions.like("isi","[INTERLIBRARY_LOAN]%"))).addOrder(Order.desc("id")).setMaxResults(30).list())data.put(new JSONObject().put("type",safe(x.getIsi()).startsWith("[ASK_LIBRARIAN]")?"Tanya pustakawan":"Pinjam antarperpustakaan").put("code","MSG-"+x.getId()).put("description",safe(x.getIsi()).replaceFirst("^\\[[^]]+\\]\\s*","")).put("status",Boolean.FALSE.equals(x.getAktif())?"Selesai":"Diajukan"));return ok().put("data",data);}finally{HibernateUtil.closeSessionQuietly(s);}}

    private static JSONObject message(Tbmuser user,String tag,HttpServletRequest r)throws Exception{String subject=text(r.getParameter("subject"),120),body=text(r.getParameter("message"),1200);if(subject==null||body==null)return error("Subjek dan uraian wajib diisi.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();Pesan x=new Pesan();x.setTbmuser((Tbmuser)s.get(Tbmuser.class,user.getUserId()));x.setIsi("["+tag+"] "+subject+" — "+body);x.setWaktu(new Date());x.setAktif(true);x.setSemua(false);x.setOlehId(user.getUserId());x.setOleh(user.getUserNama());tx=s.beginTransaction();s.save(x);tx.commit();return ok().put("message",tag.equals("ASK_LIBRARIAN")?"Pertanyaan dikirim ke antrean pustakawan.":"Permintaan pinjam antarperpustakaan diajukan.").put("id",x.getId());}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    private static JSONObject acquisition(Tbmuser user,HttpServletRequest r)throws Exception{String title=text(r.getParameter("title"),180),author=text(r.getParameter("author"),140),isbn=text(r.getParameter("isbn"),40),reason=text(r.getParameter("reason"),500),course=text(r.getParameter("course"),120),publisherUrl=text(r.getParameter("publisherUrl"),300),urgency=allowed(r.getParameter("urgency"),new String[]{"NORMAL","HIGH","COURSE"},"NORMAL");if(title==null||reason==null)return error("Judul dan alasan usulan wajib diisi.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();PermintaanPengadaanItem x=new PermintaanPengadaanItem();x.setKode("USR-"+System.currentTimeMillis());x.setTanggalPembuatan(new Date());x.setPerpustakaan(Common.getCurrentPerpustakaan());x.setDibuatOleh((Tbmuser)s.get(Tbmuser.class,user.getUserId()));x.setOlehId(user.getUserId());x.setOleh(user.getUserNama());x.setKeterangan("[USULAN ANGGOTA] Judul: "+title+" | Penulis: "+safe(author)+" | ISBN: "+safe(isbn)+" | Mata kuliah: "+safe(course)+" | Urgensi: "+urgency+" | Tautan: "+safe(publisherUrl)+" | Alasan: "+reason);tx=s.beginTransaction();s.save(x);tx.commit();return ok().put("message","Usulan koleksi berhasil diajukan.").put("code",x.getKode());}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    private static JSONObject booking(Tbmuser user,HttpServletRequest r)throws Exception{Long roomId=positiveLong(r.getParameter("roomId"));Date start=date(r.getParameter("start")),end=date(r.getParameter("end"));String purpose=text(r.getParameter("purpose"),240);if(roomId==null||start==null||end==null||!end.after(start)||purpose==null)return error("Ruang, waktu mulai/selesai, dan tujuan wajib diisi dengan benar.");Session s=null;Transaction tx=null;try{s=HibernateUtil.openSession();Ruang room=(Ruang)s.get(Ruang.class,roomId);if(room==null||Boolean.FALSE.equals(room.getAktif()))return error("Ruang tidak tersedia.");long conflict=((Number)s.createQuery("select count(id) from PesanRuangan where ruang.id=:room and mulai<:end and sampai>:start").setLong("room",roomId).setTimestamp("start",start).setTimestamp("end",end).uniqueResult()).longValue();if(conflict>0)return error("Ruang sudah dipesan pada rentang waktu tersebut.");PesanRuangan x=new PesanRuangan();x.setRuang(room);x.setMulai(start);x.setSampai(end);x.setTujuan("[PERPUSTAKAAN] "+purpose);x.setKeterangan("Booking fasilitas melalui portal perpustakaan");x.setDipesanOleh((Tbmuser)s.get(Tbmuser.class,user.getUserId()));x.setOlehId(user.getUserId());x.setOleh(user.getUserNama());tx=s.beginTransaction();s.save(x);tx.commit();return ok().put("message","Fasilitas berhasil dipesan.").put("id",x.getId());}catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(s);}}

    private static Date date(String value){try{return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(value);}catch(Exception e){return null;}}
    private static String allowed(String v,String[] values,String fallback){for(String x:values)if(x.equals(v))return x;return fallback;}
    private static String text(String v,int max){if(v==null)return null;v=v.trim().replaceAll("[\\r\\n]+"," ");return v.length()==0?null:v.length()>max?v.substring(0,max):v;}
    private static String safe(Object v){return v==null?"":String.valueOf(v).trim();}
    private static Long positiveLong(String v){try{long x=Long.parseLong(v);return x>0?Long.valueOf(x):null;}catch(Exception e){return null;}}
    private static JSONObject ok()throws JSONException{return new JSONObject().put("ok",true);}
    private static JSONObject error(String message)throws JSONException{return new JSONObject().put("ok",false).put("error",message).put("message",message);}
    private static void rollback(Transaction tx){try{if(tx!=null&&tx.isActive())tx.rollback();}catch(Exception ignored){}}
}
