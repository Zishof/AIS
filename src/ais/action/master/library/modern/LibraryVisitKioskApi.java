package ais.action.master.library.modern;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Anggota;
import ais.database.model.library.KunjunganAnggota;
import ais.database.model.library.Perpustakaan;

/** Typed API for scanner-first visit kiosk. */
public final class LibraryVisitKioskApi {
    private LibraryVisitKioskApi() { }

    public static JSONObject handle(HttpServletRequest request) throws JSONException {
        try {
            String action = text(request.getParameter("action"), 24);
            if ("list".equals(action)) return list(request);
            if (!"POST".equalsIgnoreCase(request.getMethod())) return error("Metode permintaan tidak diizinkan.");
            if (!NewUiCsrfUtil.isValid(request)) return error("Token keamanan tidak valid. Muat ulang layar kiosk.");
            if ("scan".equals(action)) return scan(request);
            if ("guest".equals(action)) return guest(request);
            return error("Operasi kiosk tidak dikenal.");
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e); return error("Layanan kunjungan belum dapat memproses permintaan.");
        }
    }

    private static JSONObject scan(HttpServletRequest request) throws Exception {
        String code=text(request.getParameter("kode"),120);if(code==null)return error("Masukkan kode identitas.");
        Session session=null;Transaction tx=null;try{session=HibernateUtil.openSession();Perpustakaan library=library(session);if(library==null)return error("Lokasi perpustakaan belum dikonfigurasi.");
            Anggota member=LibraryUtil.cariAnggotaDariIdentitas(session,code);if(member==null||Boolean.FALSE.equals(member.getAktif()))return error("Identitas anggota aktif tidak ditemukan.");
            Query existing=session.createSQLQuery("select count(id) from library.kunjungan_anggota where anggota=:member and perpustakaan=:library and date(tgl)=current_date").setLong("member",member.getId()).setLong("library",library.getId());
            if(number(existing.uniqueResult())>0)return ok("Kunjungan Anda hari ini sudah tercatat.");
            KunjunganAnggota visit=new KunjunganAnggota();Date now=new Date();visit.setAnggota(member);visit.setPerpustakaan(library);visit.setTanggal(now);visit.setTgl(now);visit.setKeterangan("Kiosk kunjungan");tx=session.beginTransaction();session.save(visit);tx.commit();return ok("Selamat datang, "+safe(member.getNama())+".");
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static JSONObject guest(HttpServletRequest request)throws Exception{
        String name=text(request.getParameter("nama"),120),address=text(request.getParameter("alamat"),240),note=text(request.getParameter("keterangan"),240);if(name==null||address==null)return error("Nama dan alamat/instansi wajib diisi.");
        Session session=null;Transaction tx=null;try{session=HibernateUtil.openSession();Perpustakaan library=library(session);if(library==null)return error("Lokasi perpustakaan belum dikonfigurasi.");
            Query existing=session.createSQLQuery("select count(id) from library.kunjungan_anggota where anggota is null and lower(nama)=:name and lower(alamat)=:address and perpustakaan=:library and date(tgl)=current_date").setString("name",name.toLowerCase()).setString("address",address.toLowerCase()).setLong("library",library.getId());
            if(number(existing.uniqueResult())>0)return ok("Kunjungan Anda hari ini sudah tercatat.");
            KunjunganAnggota visit=new KunjunganAnggota();Date now=new Date();visit.setPerpustakaan(library);visit.setNama(name);visit.setAlamat(address);visit.setKeterangan(note==null?"":note);visit.setTanggal(now);visit.setTgl(now);tx=session.beginTransaction();session.save(visit);tx.commit();return ok("Kunjungan berhasil dicatat. Terima kasih.");
        }catch(Exception e){rollback(tx);throw e;}finally{HibernateUtil.closeSessionQuietly(session);}
    }

    @SuppressWarnings("unchecked")
    private static JSONObject list(HttpServletRequest request)throws Exception{
        Session session=null;try{session=HibernateUtil.openSession();Perpustakaan library=library(session);if(library==null)return error("Lokasi perpustakaan belum dikonfigurasi.");int page=bounded(request.getParameter("page"),0,10000,0),limit=10;
            long total=number(session.createSQLQuery("select count(id) from library.kunjungan_anggota where perpustakaan=:library and date(tgl)=current_date").setLong("library",library.getId()).uniqueResult());
            List<KunjunganAnggota> rows=session.createCriteria(KunjunganAnggota.class).add(Restrictions.eq("perpustakaan",library)).add(Restrictions.sqlRestriction("date(tgl)=current_date")).addOrder(Order.desc("id")).setFirstResult(page*limit).setMaxResults(limit).list();JSONArray data=new JSONArray();SimpleDateFormat time=new SimpleDateFormat("HH:mm");
            for(KunjunganAnggota visit:rows){boolean member=visit.getAnggota()!=null;String name=member?visit.getAnggota().getNama():visit.getNama();data.put(new JSONObject().put("nama",mask(name)).put("status",member?"Anggota":"Tamu").put("waktu",visit.getTanggal()==null?"-":time.format(visit.getTanggal())));}
            return new JSONObject().put("ok",true).put("status","success").put("data",data).put("total",total).put("limit",limit).put("privacy","Nama disamarkan pada layar publik.");
        }finally{HibernateUtil.closeSessionQuietly(session);}
    }

    private static Perpustakaan library(Session session){return(Perpustakaan)session.createCriteria(Perpustakaan.class).add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",true))).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();}
    private static JSONObject ok(String message)throws JSONException{return new JSONObject().put("ok",true).put("status","success").put("message",message);}
    private static JSONObject error(String message)throws JSONException{return new JSONObject().put("ok",false).put("status","error").put("message",message).put("error",message);}
    private static String text(String value,int max){if(value==null||value.trim().length()==0)return null;value=value.trim();return value.length()>max?value.substring(0,max):value;}
    private static String mask(String name){if(name==null||name.trim().length()==0)return"Pengunjung";String[] parts=name.trim().split("\\s+");StringBuilder out=new StringBuilder();for(String part:parts){if(out.length()>0)out.append(' ');out.append(part.charAt(0));if(part.length()>1)out.append("***");}return out.toString();}
    private static int bounded(String raw,int min,int max,int fallback){try{int v=Integer.parseInt(raw);return v>=min&&v<=max?v:fallback;}catch(Exception e){return fallback;}}
    private static long number(Object value){return value instanceof Number?((Number)value).longValue():0L;}
    private static String safe(String value){return value==null?"":value;}
    private static void rollback(Transaction tx){try{if(tx!=null&&tx.isActive())tx.rollback();}catch(Exception ignored){}}
}
