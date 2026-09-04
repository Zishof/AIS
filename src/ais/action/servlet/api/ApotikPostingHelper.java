package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.util.CommonAkunting;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.sirs.ApotikAkunMapping;
import ais.database.model.sirs.ApotikPostingLink;
import ais.database.model.sirs.TransaksiMedis;

/** Draf dan posting Penjualan/HPP Apotik yang terpisah penuh dari tabel Kantin. */
public final class ApotikPostingHelper {
    public static final String JENIS_PENJUALAN = "Penjualan Apotik";
    public static final String JENIS_HPP = "HPP Apotik";
    private static final double EPS = 0.005;

    private ApotikPostingHelper() { }

    private static final class Draf {
        long id;
        String kode;
        Date tanggal;
        double nilai;
        String alasan = "";
        final List<Akun> debet = new ArrayList<Akun>();
        final List<Double> nilaiDebet = new ArrayList<Double>();
        final List<Akun> kredit = new ArrayList<Akun>();
        final List<Double> nilaiKredit = new ArrayList<Double>();
        boolean siap() { return alasan.length() == 0 && nilai > EPS && Math.abs(total(nilaiDebet) - total(nilaiKredit)) < EPS; }
    }

    public static void proses(String action, Tbmuser pengguna, JSONObject payload, JSONObject hasil) throws Exception {
        if ("apotik_pemetaan_akun_audit".equals(action)) {
            auditPemetaan(hasil);
            return;
        }
        if ("apotik_pemetaan_akun_terapkan".equals(action)) {
            terapkanPemetaan(pengguna, payload, hasil);
            return;
        }
        boolean hpp = action.indexOf("_hpp_") >= 0;
        boolean terapkan = action.endsWith("_terapkan");
        if (!(action.startsWith("apotik_posting_penjualan_") || action.startsWith("apotik_posting_hpp_"))) {
            hasil.put("status", "99");
            hasil.put("message", "Aksi posting Apotik tidak dikenal: " + action);
            return;
        }
        if (terapkan && !bolehPosting(pengguna, hpp ? "posting_hpp" : "posting_penjualan")) {
            hasil.put("status", "91");
            hasil.put("description", "Anda tidak memiliki hak memposting jurnal Apotik.");
            return;
        }
        jalankan(hpp ? ApotikPostingLink.HPP : ApotikPostingLink.PENJUALAN,
                terapkan, pengguna, payload, hasil);
    }

    private static boolean bolehPosting(Tbmuser pengguna, String kunci) {
        if (Common.getApakahAdminLain(pengguna)) return true;
        ais.database.model.Tbmrole role = pengguna == null ? null : pengguna.hakAkses();
        if (role == null) return true;
        return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(role.getEbisnisMenu(), role.getRoleId(),
                kunci, "create");
    }

    private static void auditPemetaan(JSONObject hasil) throws Exception {
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            JSONArray daftar = new JSONArray();
            int kosong = 0;
            String[] peran = { ApotikAkunMapping.PENDAPATAN, ApotikAkunMapping.HPP,
                    ApotikAkunMapping.PERSEDIAAN, ApotikAkunMapping.PIUTANG, ApotikAkunMapping.UTANG_PBF };
            for (int i = 0; i < peran.length; i++) {
                ApotikAkunMapping m = mapping(s, peran[i]);
                JSONObject j = new JSONObject();
                j.put("peran", peran[i]);
                if (m == null || m.getAkun() == null) {
                    kosong++;
                    j.put("terpetakan", false);
                } else {
                    Akun a = m.getAkun();
                    j.put("terpetakan", true);
                    j.put("akunId", a.getId());
                    j.put("kode", a.getKode());
                    j.put("nama", a.getNama());
                    j.put("posisi", a.getDebetCredit().intValue() == Akun.DEBET ? "Debet" : "Credit");
                }
                daftar.put(j);
            }
            Number tanpaAkun = (Number) s.createSQLQuery("SELECT count(*) FROM koperasi.cara_pembayaran_koperasi"
                    + " WHERE aktif=true AND akun IS NULL").uniqueResult();
            hasil.put("status", "00");
            hasil.put("rincian", daftar);
            hasil.put("jumlahBelumDipetakan", kosong);
            hasil.put("caraBayarTanpaAkun", tanpaAkun == null ? 0 : tanpaAkun.intValue());
            hasil.put("lulus", kosong == 0 && (tanpaAkun == null || tanpaAkun.intValue() == 0));
            hasil.put("catatan", "Akun Apotik wajib dipilih eksplisit; akun contoh Kantin tidak diwariskan otomatis.");
        } finally {
            HibernateUtil.closeSessionQuietly(s);
        }
    }

    private static void terapkanPemetaan(Tbmuser pengguna, JSONObject p, JSONObject hasil) throws Exception {
        if (pengguna == null || !Common.getApakahAdminLain(pengguna)) {
            hasil.put("status", "91");
            hasil.put("description", "Hanya admin yang boleh mengubah pemetaan akun Apotik.");
            return;
        }
        JSONObject akun = p == null ? null : p.optJSONObject("akun");
        if (akun == null) {
            hasil.put("status", "99");
            hasil.put("message", "Objek akun berisi id untuk PENDAPATAN, HPP, PERSEDIAAN, PIUTANG, dan UTANG_PBF wajib diisi.");
            return;
        }
        String[] peran = { ApotikAkunMapping.PENDAPATAN, ApotikAkunMapping.HPP,
                ApotikAkunMapping.PERSEDIAAN, ApotikAkunMapping.PIUTANG, ApotikAkunMapping.UTANG_PBF };
        int[] posisi = { Akun.CREDIT, Akun.DEBET, Akun.DEBET, Akun.DEBET, Akun.CREDIT };
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            List<Akun> terpilih = new ArrayList<Akun>();
            for (int i = 0; i < peran.length; i++) {
                long id = akun.optLong(peran[i], 0);
                Akun a = id <= 0 ? null : (Akun) s.get(Akun.class, Long.valueOf(id));
                if (a == null || a.getDebetCredit().intValue() != posisi[i]) {
                    hasil.put("status", "99");
                    hasil.put("message", "Akun " + peran[i] + " tidak ditemukan atau posisi normalnya tidak sesuai.");
                    return;
                }
                terpilih.add(a);
            }
            tx = s.beginTransaction();
            for (int i = 0; i < peran.length; i++) {
                ApotikAkunMapping m = mapping(s, peran[i]);
                if (m == null) { m = new ApotikAkunMapping(); m.setPeran(peran[i]); }
                m.setAkun(terpilih.get(i));
                m.setAktif(Boolean.TRUE);
                m.setKeterangan("Pemetaan akun khusus modul Apotik");
                m.setOleh(pengguna.getUserId());
                m.setOlehId(pengguna.getUserId());
                s.saveOrUpdate(m);
            }
            tx.commit();
            hasil.put("status", "00");
            hasil.put("message", "Pemetaan akun Apotik tersimpan.");
        } catch (Exception e) {
            if (tx != null) try { tx.rollback(); } catch (Exception x) { ais.common.ErrorAuditUtil.record(x, "rollback ApotikPostingHelper.terapkanPemetaan"); }
            throw e;
        } finally {
            HibernateUtil.closeSessionQuietly(s);
        }
    }

    private static void jalankan(String jenis, boolean terapkan, Tbmuser pengguna, JSONObject p,
            JSONObject hasil) throws Exception {
        String mulai = p == null ? "" : p.optString("mulai", "").trim();
        String sampai = p == null ? "" : p.optString("sampai", "").trim();
        if (mulai.length() == 0 || sampai.length() == 0) {
            hasil.put("status", "99"); hasil.put("message", "Tanggal mulai dan sampai wajib diisi."); return;
        }
        Set<Long> dipilih = new HashSet<Long>();
        JSONArray ids = p == null ? null : p.optJSONArray("posting_ids");
        for (int i = 0; ids != null && i < ids.length(); i++) dipilih.add(Long.valueOf(ids.optLong(i)));
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            List<Draf> daftar = draf(s, jenis, mulai, sampai);
            JSONArray rincian = new JSONArray(); int siap = 0; double totalSiap = 0;
            for (int i = 0; i < daftar.size(); i++) {
                Draf d = daftar.get(i); JSONObject j = new JSONObject();
                j.put("id", d.id); j.put("referensi", d.kode); j.put("tanggal", Common.dateFormat3.get().format(d.tanggal));
                j.put("nilai", d.nilai); j.put("siap", d.siap()); j.put("alasan", d.alasan);
                j.put("akunDebit", namaAkun(d.debet)); j.put("akunKredit", namaAkun(d.kredit));
                j.put("debit", total(d.nilaiDebet)); j.put("kredit", total(d.nilaiKredit));
                PostingStatusUtil.tandaiBelum(j, d.siap()); rincian.put(j);
                if (d.siap()) { siap++; totalSiap += d.nilai; }
            }
            hasil.put("status", "00"); hasil.put("jenis", jenis.toLowerCase()); hasil.put("rincian", rincian);
            hasil.put("jumlahBelumDiposting", daftar.size()); hasil.put("jumlahSiapDiposting", siap); hasil.put("totalSiap", totalSiap);
            int batasRiwayat = PostingStatusUtil.batasRiwayat(p);
            hasil.put("batasRiwayat", batasRiwayat);
            JSONArray sudah = riwayat(s, jenis, mulai, sampai, batasRiwayat);
            hasil.put("rincianSudahDiposting", sudah); hasil.put("jumlahSudahDiposting", sudah.length());
            if (!terapkan) { hasil.put("message", siap + " dari " + daftar.size() + " transaksi siap diposting."); return; }
            if (pengguna == null) { hasil.put("status", "91"); hasil.put("message", "Sesi pengguna tidak ditemukan."); return; }
            int sukses = 0; JSONArray masalah = new JSONArray();
            for (int i = 0; i < daftar.size(); i++) {
                Draf d = daftar.get(i);
                if (!d.siap() || (!dipilih.isEmpty() && !dipilih.contains(Long.valueOf(d.id)))) continue;
                try { if (postingSatu(s, jenis, d, pengguna)) sukses++; else masalah.put(d.kode + ": ditolak periode closing."); }
                catch (Exception e) { masalah.put(d.kode + ": " + e.getMessage()); ais.common.ErrorAuditUtil.record(e, "ApotikPostingHelper.posting " + d.id); }
            }
            hasil.put("diposting", sukses); hasil.put("masalah", masalah); hasil.put("message", sukses + " jurnal Apotik terbentuk.");
        } finally { HibernateUtil.closeSessionQuietly(s); }
    }

    private static List<Draf> draf(Session s, String jenis, String mulai, String sampai) throws Exception {
        List<Draf> out = new ArrayList<Draf>();
        Map<Long,Draf> menurutId = new LinkedHashMap<Long,Draf>();
        String sql = "SELECT t.id,COALESCE(NULLIF(TRIM(t.kode),''),'Apotik #'||CAST(t.id AS text)),t.tanggal_transaksi"
                + " FROM sirs.transaksi_medis t WHERE t.sumber='APOTIK' AND t.jenis_transaksi='item'"
                + " AND date(t.tanggal_transaksi) BETWEEN date(?) AND date(?)"
                + " AND EXISTS (SELECT 1 FROM sirs.transaksi_medis_detail d WHERE d.transaksi=t.id)"
                + " AND NOT EXISTS (SELECT 1 FROM sirs.apotik_posting_link l WHERE l.transaksi=t.id AND l.jenis=?)"
                + " ORDER BY t.tanggal_transaksi,t.id";
        PreparedStatement ps = s.connection().prepareStatement(sql); ps.setString(1, mulai); ps.setString(2, sampai); ps.setString(3, jenis);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) { Draf d = new Draf(); d.id=rs.getLong(1); d.kode=rs.getString(2); d.tanggal=rs.getTimestamp(3); out.add(d); menurutId.put(Long.valueOf(d.id),d); }
        rs.close(); ps.close();
        if (ApotikPostingLink.PENJUALAN.equals(jenis)) isiDrafPenjualan(s,mulai,sampai,menurutId);
        else isiDrafHpp(s,mulai,sampai,menurutId);
        return out;
    }

    private static void isiDrafPenjualan(Session s,String mulai,String sampai,Map<Long,Draf> daftar)throws Exception{
        Akun pendapatan=akun(s,ApotikAkunMapping.PENDAPATAN);
        String sql="SELECT p.transaksi,p.cara_bayar,SUM(ABS(p.nominal))"
                + " FROM sirs.apotik_pembayaran_transaksi p JOIN sirs.transaksi_medis t ON t.id=p.transaksi"
                + " WHERE t.sumber='APOTIK' AND t.jenis_transaksi='item' AND date(t.tanggal_transaksi) BETWEEN date(?) AND date(?)"
                + " GROUP BY p.transaksi,p.cara_bayar ORDER BY p.transaksi";
        PreparedStatement ps=s.connection().prepareStatement(sql);ps.setString(1,mulai);ps.setString(2,sampai);ResultSet rs=ps.executeQuery();
        while(rs.next()){
            Draf d=daftar.get(Long.valueOf(rs.getLong(1)));if(d==null)continue;
            long caraId=rs.getLong(2);boolean caraKosong=rs.wasNull()||caraId<=0;double nilai=Math.abs(rs.getDouble(3));d.nilai+=nilai;
            CaraPembayaranKoperasi cara=caraKosong?null:(CaraPembayaranKoperasi)s.get(CaraPembayaranKoperasi.class,Long.valueOf(caraId));
            Akun a=cara==null?null:cara.getAkun();
            if(a==null)d.alasan="Cara pembayaran "+(cara==null?"(tidak ditemukan)":cara.getNama())+" belum mempunyai akun kas/bank/piutang.";
            else{d.debet.add(a);d.nilaiDebet.add(Double.valueOf(nilai));}
        }
        rs.close();ps.close();
        for(Draf d:daftar.values()){
            if(d.nilai<=EPS&&d.alasan.length()==0)d.alasan="Pembayaran transaksi belum tersedia atau bernilai nol.";
            if(pendapatan==null)d.alasan="Akun Pendapatan Apotik belum dipetakan.";
            else{d.kredit.add(pendapatan);d.nilaiKredit.add(Double.valueOf(d.nilai));}
        }
    }

    private static void isiDrafHpp(Session s,String mulai,String sampai,Map<Long,Draf> daftar)throws Exception{
        Akun hpp=akun(s,ApotikAkunMapping.HPP),persediaan=akun(s,ApotikAkunMapping.PERSEDIAAN);
        String sql="SELECT d.transaksi,COALESCE(SUM(k.qty*COALESCE(i.default_harga_beli,0)),0)"
                + " FROM sirs.apotik_batch_konsumsi k JOIN sirs.transaksi_medis_detail d ON d.id=k.transaksi_detail"
                + " JOIN sirs.transaksi_medis t ON t.id=d.transaksi JOIN sirs.kadaluarsa b ON b.id=k.kadaluarsa"
                + " JOIN sirs.item_medis i ON i.id=b.item WHERE t.sumber='APOTIK' AND t.jenis_transaksi='item'"
                + " AND date(t.tanggal_transaksi) BETWEEN date(?) AND date(?) GROUP BY d.transaksi";
        PreparedStatement ps=s.connection().prepareStatement(sql);ps.setString(1,mulai);ps.setString(2,sampai);ResultSet rs=ps.executeQuery();
        while(rs.next()){Draf d=daftar.get(Long.valueOf(rs.getLong(1)));if(d!=null)d.nilai=Math.abs(rs.getDouble(2));}
        rs.close();ps.close();
        for(Draf d:daftar.values()){
            if(hpp==null||persediaan==null)d.alasan="Akun HPP atau Persediaan Apotik belum dipetakan.";
            else if(d.nilai<=EPS)d.alasan="Nilai HPP nol; periksa harga beli item/batch transaksi.";
            else{d.debet.add(hpp);d.nilaiDebet.add(Double.valueOf(d.nilai));d.kredit.add(persediaan);d.nilaiKredit.add(Double.valueOf(d.nilai));}
        }
    }

    private static boolean postingSatu(Session s,String jenis,Draf d,Tbmuser u)throws Exception{
        Transaction tx=s.beginTransaction();
        try{
            Number ada=(Number)s.createSQLQuery("SELECT count(*) FROM sirs.apotik_posting_link WHERE transaksi="+d.id+" AND jenis='"+jenis+"'").uniqueResult();
            if(ada!=null&&ada.intValue()>0){tx.rollback();return true;}
            String ket=(ApotikPostingLink.PENJUALAN.equals(jenis)?"Penjualan":"HPP")+" Apotik "+d.kode;
            PostingHistory ph=new PostingHistory(ApotikPostingLink.PENJUALAN.equals(jenis)?JENIS_PENJUALAN:JENIS_HPP);
            ph.setTanggal(d.tanggal);ph.setTanggalPosting(d.tanggal);ph.setTbmuser(u);ph.setPosting(Boolean.TRUE);ph.setKeterangan(ket);s.save(ph);
            boolean ok=CommonAkunting.saveTransaksi(d.debet.toArray(new Akun[]{}),d.kredit.toArray(new Akun[]{}),null,null,ph,true,ket,d.tanggal,
                    d.nilaiDebet.toArray(new Double[]{}),d.nilaiKredit.toArray(new Double[]{}),Double.valueOf(0),null,null,s);
            if(!ok){tx.rollback();return false;}
            ApotikPostingLink l=new ApotikPostingLink();l.setTransaksi((TransaksiMedis)s.load(TransaksiMedis.class,Long.valueOf(d.id)));
            l.setJenis(jenis);l.setPostingHistory(ph);l.setNilai(Double.valueOf(d.nilai));l.setWaktu(new Date());l.setOleh(u.getUserId());l.setOlehId(u.getUserId());s.save(l);
            tx.commit();return true;
        }catch(Exception e){try{tx.rollback();}catch(Exception x){ais.common.ErrorAuditUtil.record(x,"rollback ApotikPostingHelper.postingSatu");}throw e;}
    }

    private static JSONArray riwayat(Session s,String jenis,String mulai,String sampai,int batas)throws Exception{
        JSONArray out=new JSONArray();String sql="SELECT t.id,COALESCE(NULLIF(TRIM(t.kode),''),'Apotik #'||CAST(t.id AS text)),l.nilai,t.tanggal_transaksi,l.posting_history,"
                + "COALESCE((SELECT MIN(g.kode) FROM akunting.grup_transaksi g WHERE g.posting_history=l.posting_history),''),ph.tanggalposting"
                + " FROM sirs.apotik_posting_link l JOIN sirs.transaksi_medis t ON t.id=l.transaksi JOIN akunting.posting_history ph ON ph.id=l.posting_history"
                + " WHERE l.jenis=? AND date(t.tanggal_transaksi) BETWEEN date(?) AND date(?) ORDER BY t.tanggal_transaksi DESC,t.id DESC LIMIT "+batas;
        PreparedStatement ps=s.connection().prepareStatement(sql);ps.setString(1,jenis);ps.setString(2,mulai);ps.setString(3,sampai);ResultSet rs=ps.executeQuery();
        while(rs.next())out.put(PostingStatusUtil.sudah(rs.getLong(1),rs.getString(2),rs.getDouble(3),rs.getTimestamp(4),rs.getLong(5),rs.getString(6),rs.getDate(7),""));
        rs.close();ps.close();return out;
    }

    private static ApotikAkunMapping mapping(Session s,String peran){return (ApotikAkunMapping)s.createCriteria(ApotikAkunMapping.class)
            .add(Restrictions.eq("peran",peran)).add(Restrictions.eq("aktif",Boolean.TRUE)).setMaxResults(1).uniqueResult();}
    private static Akun akun(Session s,String peran){ApotikAkunMapping m=mapping(s,peran);return m==null?null:m.getAkun();}
    private static String namaAkun(List<Akun> daftar){StringBuilder b=new StringBuilder();for(int i=0;i<daftar.size();i++){if(i>0)b.append(", ");Akun a=daftar.get(i);b.append(a.getKode()).append(" ").append(a.getNama());}return b.toString();}
    private static double total(List<Double> n){double t=0;for(int i=0;i<n.size();i++)t+=n.get(i).doubleValue();return t;}
}
