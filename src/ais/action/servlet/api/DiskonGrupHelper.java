package ais.action.servlet.api;

import java.sql.*;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/** CRUD atomik untuk promo grup. Evaluasi transaksinya tetap dipusatkan di KantinHelper. */
public final class DiskonGrupHelper {
    private DiskonGrupHelper() {}

    private static Long toko(Tbmuser u, JSONObject r) throws Exception {
        if (u != null && u.getPedagang() != null) {
            if (u.getPedagang().getToko() != null) return u.getPedagang().getToko().getId();
            if (u.getTokoAktifMultiToko() != null) return u.getTokoAktifMultiToko();
        }
        return r.isNull("toko_id") ? null : Long.valueOf(String.valueOf(r.get("toko_id")));
    }

    private static void tutup(Session s) { if (s != null) try { s.close(); } catch (Exception ignored) {} }
    private static String json(JSONArray a) { return a == null ? "[]" : a.toString(); }
    private static Timestamp waktu(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String v=s.trim().replace('T',' '); if (v.length()==16) v += ":00";
        return Timestamp.valueOf(v);
    }

    public static void list(Tbmuser u, JSONObject r, JSONObject out) throws Exception {
        Long toko=toko(u,r); if(toko==null){ gagal(out,"Toko tidak diketahui."); return; }
        int page=Math.max(1,r.optInt("page",1)), size=Math.max(1,Math.min(100,r.optInt("page_size",15)));
        String q=r.optString("keyword","").trim().toLowerCase();
        Session s=HibernateUtil.getSessionFactory().openSession();
        try {
            Connection c=s.connection();
            String where=" WHERE (g.toko IS NULL OR g.toko=?) AND (?='' OR LOWER(g.nama_grup) LIKE ?)";
            PreparedStatement pc=c.prepareStatement("SELECT COUNT(*) FROM koperasi.grup_aturan_diskon g"+where);
            pc.setLong(1,toko); pc.setString(2,q); pc.setString(3,"%"+q+"%"); ResultSet rc=pc.executeQuery(); rc.next(); int total=rc.getInt(1); rc.close(); pc.close();
            PreparedStatement ps=c.prepareStatement("SELECT g.id,g.nama_grup,COALESCE(g.keterangan,''),g.persentase,g.nominal,g.cashback,g.maksimal_potongan,COALESCE(g.potongan_langsung,true),g.tanggal_mulai,g.tanggal_selesai,COALESCE(g.aktif,true),COALESCE(g.khusus_member,false),COALESCE(g.jenis_member_json,'[]'),COALESCE(g.tipe_member_json,'[]'),COALESCE(g.prioritas,100),COALESCE(g.dapat_digabung,false),COALESCE(g.dasar_perhitungan,'SETELAH_DISKON'),COALESCE(g.grup_eksklusif,''),COUNT(d.id) FROM koperasi.grup_aturan_diskon g LEFT JOIN koperasi.grup_aturan_diskon_detail d ON d.grup_aturan_diskon=g.id AND COALESCE(d.aktif,true)"+where+" GROUP BY g.id ORDER BY COALESCE(g.prioritas,100) DESC,g.id DESC LIMIT ? OFFSET ?");
            ps.setLong(1,toko); ps.setString(2,q); ps.setString(3,"%"+q+"%"); ps.setInt(4,size); ps.setInt(5,(page-1)*size);
            ResultSet rs=ps.executeQuery(); JSONArray a=new JSONArray();
            while(rs.next()){
                JSONObject j=new JSONObject(); j.put("id",rs.getLong(1)); j.put("namaGrup",rs.getString(2)); j.put("keterangan",rs.getString(3)); j.put("persentase",rs.getDouble(4)); j.put("nominal",rs.getDouble(5)); j.put("cashback",rs.getDouble(6)); j.put("maksimalPotongan",rs.getDouble(7)); j.put("potonganLangsung",rs.getBoolean(8)); j.put("tanggalMulai",rs.getTimestamp(9)==null?JSONObject.NULL:rs.getTimestamp(9).toString()); j.put("tanggalSelesai",rs.getTimestamp(10)==null?JSONObject.NULL:rs.getTimestamp(10).toString()); j.put("aktif",rs.getBoolean(11)); j.put("khususMember",rs.getBoolean(12)); j.put("jenisMemberJson",new JSONArray(rs.getString(13))); j.put("tipeMemberJson",new JSONArray(rs.getString(14))); j.put("prioritas",rs.getInt(15)); j.put("dapatDigabung",rs.getBoolean(16)); j.put("dasarPerhitungan",rs.getString(17)); j.put("grupEksklusif",rs.getString(18)); j.put("jumlahProduk",rs.getInt(19)); a.put(j);
            }
            rs.close(); ps.close(); out.put("status","00"); out.put("data",a); out.put("total",total);
        } finally { tutup(s); }
    }

    public static void detail(Tbmuser u, JSONObject r, JSONObject out) throws Exception {
        Long toko=toko(u,r), id=r.isNull("id")?null:Long.valueOf(String.valueOf(r.get("id")));
        if(toko==null||id==null){ gagal(out,"Grup diskon tidak diketahui."); return; }
        Session s=HibernateUtil.getSessionFactory().openSession();
        try { Connection c=s.connection(); PreparedStatement ps=c.prepareStatement("SELECT p.id,p.kode,COALESCE(p.barcode,''),p.nama,COALESCE(t.nama,'') FROM koperasi.grup_aturan_diskon_detail d JOIN koperasi.grup_aturan_diskon g ON g.id=d.grup_aturan_diskon JOIN koperasi.produk p ON p.id=d.produk LEFT JOIN koperasi.toko t ON t.id=p.toko WHERE d.grup_aturan_diskon=? AND COALESCE(d.aktif,true) AND (g.toko IS NULL OR g.toko=?) ORDER BY p.nama"); ps.setLong(1,id); ps.setLong(2,toko); ResultSet rs=ps.executeQuery(); JSONArray a=new JSONArray(); while(rs.next()){ JSONObject j=new JSONObject(); j.put("id",rs.getLong(1)); j.put("kode",rs.getString(2)); j.put("barcode",rs.getString(3)); j.put("nama",rs.getString(4)); j.put("tokoNama",rs.getString(5)); a.put(j); } rs.close(); ps.close(); out.put("status","00"); out.put("data",a); out.put("snapshotSesuai",true); }
        finally { tutup(s); }
    }

    public static void opsiMember(JSONObject out) throws Exception {
        Session s=HibernateUtil.getSessionFactory().openSession();
        try { Connection c=s.connection(); out.put("jenisMember",opsi(c,"SELECT id,nama FROM koperasi.jenis_anggota_koperasi WHERE COALESCE(aktif,true) ORDER BY nama")); out.put("tipeMember",opsi(c,"SELECT id,nama FROM koperasi.tipe_anggota_koperasi WHERE COALESCE(aktif,true) ORDER BY nama")); out.put("status","00"); }
        finally { tutup(s); }
    }
    private static JSONArray opsi(Connection c,String sql)throws Exception{ PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery(); JSONArray a=new JSONArray(); while(r.next()){JSONObject j=new JSONObject();j.put("id",r.getLong(1));j.put("nama",r.getString(2));a.put(j);}r.close();p.close();return a; }

    public static void cariProduk(Tbmuser u,JSONObject r,JSONObject out)throws Exception{
        Long toko=toko(u,r); if(toko==null){gagal(out,"Toko tidak diketahui.");return;} String q=r.optString("keyword","").trim().toLowerCase();
        Session s=HibernateUtil.getSessionFactory().openSession(); try{Connection c=s.connection(); PreparedStatement p=c.prepareStatement("SELECT p.id,p.kode,COALESCE(p.barcode,''),p.nama,COALESCE(t.nama,'') FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id=p.toko WHERE p.toko=? AND COALESCE(p.aktif,true) AND (?='' OR LOWER(COALESCE(p.kode,'')||' '||COALESCE(p.barcode,'')||' '||COALESCE(p.nama,'')) LIKE ?) ORDER BY p.nama LIMIT 50");p.setLong(1,toko);p.setString(2,q);p.setString(3,"%"+q+"%");out.put("data",produk(p));out.put("status","00");}finally{tutup(s);}
    }
    public static void resolveProduk(Tbmuser u,JSONObject r,JSONObject out)throws Exception{
        Long toko=toko(u,r); JSONArray keys=r.optJSONArray("kunci"); if(toko==null||keys==null){gagal(out,"Daftar produk tidak valid.");return;} JSONArray found=new JSONArray(), missing=new JSONArray(); Set<Long> seen=new LinkedHashSet<Long>(); Session s=HibernateUtil.getSessionFactory().openSession(); try{Connection c=s.connection();PreparedStatement p=c.prepareStatement("SELECT p.id,p.kode,COALESCE(p.barcode,''),p.nama,COALESCE(t.nama,'') FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id=p.toko WHERE p.toko=? AND (p.kode=? OR p.barcode=?) ORDER BY p.id LIMIT 1");for(int i=0;i<keys.length();i++){String k=String.valueOf(keys.get(i)).trim();p.setLong(1,toko);p.setString(2,k);p.setString(3,k);ResultSet rs=p.executeQuery();if(rs.next()&&seen.add(rs.getLong(1))){JSONObject j=new JSONObject();j.put("id",rs.getLong(1));j.put("kode",rs.getString(2));j.put("barcode",rs.getString(3));j.put("nama",rs.getString(4));j.put("tokoNama",rs.getString(5));found.put(j);}else missing.put(k);rs.close();}p.close();out.put("data",found);out.put("tidakDitemukan",missing);out.put("status","00");}finally{tutup(s);}
    }
    private static JSONArray produk(PreparedStatement p)throws Exception{ResultSet rs=p.executeQuery();JSONArray a=new JSONArray();while(rs.next()){JSONObject j=new JSONObject();j.put("id",rs.getLong(1));j.put("kode",rs.getString(2));j.put("barcode",rs.getString(3));j.put("nama",rs.getString(4));j.put("tokoNama",rs.getString(5));a.put(j);}rs.close();p.close();return a;}

    public static void simpan(Tbmuser u,JSONObject r,JSONObject out)throws Exception{
        Long toko=toko(u,r); JSONArray products=r.optJSONArray("produk"); if(toko==null){gagal(out,"Toko tidak diketahui.");return;} if(r.optString("nama_grup","").trim().isEmpty()||products==null||products.length()==0){gagal(out,"Nama grup dan minimal satu produk wajib diisi.");return;}
        Session s=HibernateUtil.getSessionFactory().openSession(); org.hibernate.Transaction tx=null;
        try{tx=s.beginTransaction();Connection c=s.connection();Long id=r.isNull("id")?null:Long.valueOf(String.valueOf(r.get("id")));boolean khusus=r.optBoolean("khusus_member",false);JSONArray jenis=r.optJSONArray("jenis_member_ids"),tipe=r.optJSONArray("tipe_member_ids");
            String olehId=u==null?"":String.valueOf(u.getUserId());
            if(id==null){PreparedStatement p=c.prepareStatement("INSERT INTO koperasi.grup_aturan_diskon(nama_grup,keterangan,toko,berlaku_semua_member,khusus_member,jenis_member_json,tipe_member_json,persentase,nominal,cashback,maksimal_potongan,prioritas,dapat_digabung,dasar_perhitungan,grup_eksklusif,potongan_langsung,tanggal_mulai,tanggal_selesai,aktif,detail_json,oleh_id,tanggal_dirubah) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now())",Statement.RETURN_GENERATED_KEYS);isiHeader(p,r,toko,khusus,jenis,tipe,products,olehId,1);p.executeUpdate();ResultSet k=p.getGeneratedKeys();if(!k.next())throw new SQLException("ID grup diskon tidak terbentuk");id=k.getLong(1);k.close();p.close();}
            else{PreparedStatement p=c.prepareStatement("UPDATE koperasi.grup_aturan_diskon SET nama_grup=?,keterangan=?,toko=?,berlaku_semua_member=?,khusus_member=?,jenis_member_json=?,tipe_member_json=?,persentase=?,nominal=?,cashback=?,maksimal_potongan=?,prioritas=?,dapat_digabung=?,dasar_perhitungan=?,grup_eksklusif=?,potongan_langsung=?,tanggal_mulai=?,tanggal_selesai=?,aktif=?,detail_json=?,oleh_id=?,tanggal_dirubah=now() WHERE id=? AND (toko IS NULL OR toko=?)");isiHeader(p,r,toko,khusus,jenis,tipe,products,olehId,1);p.setLong(22,id);p.setLong(23,toko);if(p.executeUpdate()!=1)throw new SQLException("Grup diskon tidak ditemukan atau bukan milik toko ini");p.close();PreparedStatement d=c.prepareStatement("DELETE FROM koperasi.grup_aturan_diskon_detail WHERE grup_aturan_diskon=?");d.setLong(1,id);d.executeUpdate();d.close();}
            PreparedStatement d=c.prepareStatement("INSERT INTO koperasi.grup_aturan_diskon_detail(grup_aturan_diskon,produk,aktif,oleh_id,tanggal_dirubah) SELECT ?,p.id,true,?,now() FROM koperasi.produk p WHERE p.id=? AND p.toko=? ON CONFLICT (grup_aturan_diskon,produk) DO NOTHING");for(int i=0;i<products.length();i++){JSONObject x=products.getJSONObject(i);d.setLong(1,id);d.setString(2,u==null?"":u.getUserId());d.setLong(3,x.getLong("id"));d.setLong(4,toko);d.addBatch();}d.executeBatch();d.close();tx.commit();out.put("status","00");out.put("id",id);
        }catch(Exception e){if(tx!=null)try{tx.rollback();}catch(Exception ignored){}throw e;}finally{tutup(s);}
    }
    private static void isiHeader(PreparedStatement p,JSONObject r,Long toko,boolean khusus,JSONArray jenis,JSONArray tipe,JSONArray products,String olehId,int i)throws Exception{p.setString(i++,r.optString("nama_grup","").trim());p.setString(i++,r.optString("keterangan",""));p.setLong(i++,toko);p.setBoolean(i++,!khusus);p.setBoolean(i++,khusus);p.setString(i++,json(jenis));p.setString(i++,json(tipe));p.setDouble(i++,r.optDouble("persentase",0));p.setDouble(i++,r.optDouble("nominal",0));p.setDouble(i++,r.optDouble("cashback",0));p.setDouble(i++,r.optDouble("maksimal_potongan",0));p.setInt(i++,Math.max(0,r.optInt("prioritas",100)));p.setBoolean(i++,r.optBoolean("dapat_digabung",false));String dasar=r.optString("dasar_perhitungan","SETELAH_DISKON").toUpperCase();p.setString(i++,"HARGA_AWAL".equals(dasar)?dasar:"SETELAH_DISKON");p.setString(i++,r.optString("grup_eksklusif","").trim());p.setBoolean(i++,r.optBoolean("potongan_langsung",true));p.setTimestamp(i++,waktu(r.optString("tanggal_mulai","")));p.setTimestamp(i++,waktu(r.optString("tanggal_selesai","")));p.setBoolean(i++,r.optBoolean("aktif",true));p.setString(i++,products.toString());p.setString(i++,olehId);}
    private static void gagal(JSONObject o,String m)throws Exception{o.put("status","91");o.put("description",m);}
}
