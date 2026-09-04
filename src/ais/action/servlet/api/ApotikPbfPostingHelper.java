package ais.action.servlet.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import ais.database.model.sirs.ApotikPbfDokumen;
import ais.database.model.sirs.ApotikPbfPembayaran;

/** Posting penerimaan PBF dan pembayaran utang PBF khusus modul Apotik. */
public final class ApotikPbfPostingHelper {
    public static final String JENIS_PBF = "Penerimaan PBF Apotik";
    public static final String JENIS_BAYAR = "Bayar Utang PBF Apotik";
    private static final double EPS = 0.005;

    private ApotikPbfPostingHelper() { }

    private static final class Draf {
        long id; String referensi; Date tanggal; double nilai; String alasan = "";
        Akun debit; Akun kredit;
        boolean siap() { return nilai > EPS && alasan.length() == 0 && debit != null && kredit != null; }
    }

    public static void proses(String action, Tbmuser pengguna, JSONObject p, JSONObject hasil) throws Exception {
        boolean pembayaran = action.startsWith("apotik_posting_bayar_hutang_pbf_");
        boolean penerimaan = action.startsWith("apotik_posting_pbf_");
        if (!pembayaran && !penerimaan) {
            hasil.put("status", "99"); hasil.put("message", "Aksi posting PBF tidak dikenal: " + action); return;
        }
        boolean terapkan = action.endsWith("_terapkan");
        if (terapkan && !bolehPosting(pengguna, pembayaran ? "posting_bayar_hutang" : "posting_kulakan")) {
            hasil.put("status", "91"); hasil.put("description", "Anda tidak memiliki hak memposting jurnal Apotik."); return;
        }
        jalankan(pembayaran, terapkan, pengguna, p, hasil);
    }

    private static boolean bolehPosting(Tbmuser pengguna, String kunci) {
        if (Common.getApakahAdminLain(pengguna)) return true;
        ais.database.model.Tbmrole role = pengguna == null ? null : pengguna.hakAkses();
        if (role == null) return true;
        return ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(role.getEbisnisMenu(), role.getRoleId(),
                kunci, "create");
    }

    private static void jalankan(boolean pembayaran, boolean terapkan, Tbmuser pengguna,
            JSONObject p, JSONObject hasil) throws Exception {
        String mulai = p == null ? "" : p.optString("mulai", "").trim();
        String sampai = p == null ? "" : p.optString("sampai", "").trim();
        if (mulai.length() == 0 || sampai.length() == 0) {
            hasil.put("status", "99"); hasil.put("message", "Tanggal mulai dan sampai wajib diisi."); return;
        }
        Set<Long> dipilih = new HashSet<Long>(); JSONArray ids = p == null ? null : p.optJSONArray("posting_ids");
        for (int i = 0; ids != null && i < ids.length(); i++) dipilih.add(Long.valueOf(ids.optLong(i)));
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            List<Draf> daftar = draf(s, pembayaran, mulai, sampai);
            JSONArray rincian = new JSONArray(); int siap = 0; double totalSiap = 0;
            for (Draf d : daftar) {
                JSONObject j = new JSONObject(); j.put("id", d.id); j.put("referensi", d.referensi);
                j.put("tanggal", Common.dateFormat3.get().format(d.tanggal)); j.put("nilai", d.nilai);
                j.put("siap", d.siap()); j.put("alasan", d.alasan);
                j.put("akunDebit", namaAkun(d.debit)); j.put("akunKredit", namaAkun(d.kredit));
                j.put("debit", d.siap() ? d.nilai : 0); j.put("kredit", d.siap() ? d.nilai : 0);
                PostingStatusUtil.tandaiBelum(j, d.siap()); rincian.put(j);
                if (d.siap()) { siap++; totalSiap += d.nilai; }
            }
            hasil.put("status", "00"); hasil.put("jenis", pembayaran ? "bayar_hutang_pbf" : "pbf");
            hasil.put("rincian", rincian); hasil.put("jumlahBelumDiposting", daftar.size());
            hasil.put("jumlahSiapDiposting", siap); hasil.put("jumlahSiap", siap); hasil.put("totalSiap", totalSiap);
            int batas = PostingStatusUtil.batasRiwayat(p); hasil.put("batasRiwayat", batas);
            JSONArray sudah = riwayat(s, pembayaran, mulai, sampai, batas);
            hasil.put("rincianSudahDiposting", sudah); hasil.put("jumlahSudahDiposting", sudah.length());
            if (!terapkan) { hasil.put("message", siap + " dari " + daftar.size() + " dokumen siap diposting."); return; }
            int sukses = 0; JSONArray masalah = new JSONArray();
            for (Draf d : daftar) {
                if (!d.siap() || (!dipilih.isEmpty() && !dipilih.contains(Long.valueOf(d.id)))) continue;
                try { if (postingSatu(s, pembayaran, d, pengguna)) sukses++; }
                catch (Exception e) { masalah.put(d.referensi + ": " + e.getMessage()); ais.common.ErrorAuditUtil.record(e, "ApotikPbfPostingHelper " + d.id); }
            }
            hasil.put("diposting", sukses); hasil.put("masalah", masalah);
            hasil.put("message", sukses + " jurnal PBF Apotik terbentuk.");
        } finally { HibernateUtil.closeSessionQuietly(s); }
    }

    /** Salin nilai JDBC sebelum lookup Hibernate agar ResultSet tidak ditutup oleh statement lain. */
    private static List<Draf> draf(Session s, boolean pembayaran, String mulai, String sampai) throws Exception {
        List<Draf> out = new ArrayList<Draf>();
        String sql = pembayaran
                ? "SELECT b.id,d.kode||'/BYR-'||CAST(b.id AS text),b.tanggal,b.nominal,b.cara_bayar"
                    + " FROM sirs.apotik_pbf_pembayaran b JOIN sirs.apotik_pbf_dokumen d ON d.id=b.dokumen"
                    + " WHERE b.posting_history IS NULL AND date(b.tanggal) BETWEEN date(?) AND date(?) ORDER BY b.tanggal,b.id"
                : "SELECT d.id,d.kode,d.tanggal,d.total,NULL FROM sirs.apotik_pbf_dokumen d"
                    + " WHERE d.posting_history IS NULL AND date(d.tanggal) BETWEEN date(?) AND date(?) ORDER BY d.tanggal,d.id";
        PreparedStatement ps = s.connection().prepareStatement(sql); ps.setString(1, mulai); ps.setString(2, sampai);
        ResultSet rs = ps.executeQuery(); List<Long> caraIds = new ArrayList<Long>();
        while (rs.next()) {
            Draf d = new Draf(); d.id = rs.getLong(1); d.referensi = rs.getString(2);
            d.tanggal = rs.getTimestamp(3); d.nilai = Math.abs(rs.getDouble(4));
            long cara = rs.getLong(5); caraIds.add(rs.wasNull() ? null : Long.valueOf(cara)); out.add(d);
        }
        rs.close(); ps.close();
        Akun utang = akun(s, ApotikAkunMapping.UTANG_PBF);
        Akun persediaan = akun(s, ApotikAkunMapping.PERSEDIAAN);
        for (int i = 0; i < out.size(); i++) {
            Draf d = out.get(i);
            if (d.nilai <= EPS) d.alasan = "Nilai dokumen nol; lengkapi harga beli sebelum posting.";
            if (utang == null) d.alasan = "Akun Utang PBF Apotik belum dipetakan.";
            if (pembayaran) {
                Long caraId = caraIds.get(i);
                CaraPembayaranKoperasi cara = caraId == null ? null : (CaraPembayaranKoperasi) s.get(CaraPembayaranKoperasi.class, caraId);
                Akun kas = cara == null ? null : cara.getAkun();
                if (kas == null) d.alasan = "Cara pembayaran belum mempunyai akun Kas/Bank.";
                d.debit = utang; d.kredit = kas;
            } else {
                if (persediaan == null) d.alasan = "Akun Persediaan Apotik belum dipetakan.";
                d.debit = persediaan; d.kredit = utang;
            }
        }
        return out;
    }

    private static boolean postingSatu(Session s, boolean pembayaran, Draf d, Tbmuser u) throws Exception {
        Transaction tx = s.beginTransaction();
        try {
            Object sumber = pembayaran ? s.get(ApotikPbfPembayaran.class, Long.valueOf(d.id))
                    : s.get(ApotikPbfDokumen.class, Long.valueOf(d.id));
            PostingHistory lama = pembayaran ? ((ApotikPbfPembayaran) sumber).getPostingHistory()
                    : ((ApotikPbfDokumen) sumber).getPostingHistory();
            if (lama != null) { tx.rollback(); return true; }
            String ket = (pembayaran ? "Pembayaran Utang PBF Apotik " : "Penerimaan PBF Apotik ") + d.referensi;
            PostingHistory ph = new PostingHistory(pembayaran ? JENIS_BAYAR : JENIS_PBF);
            ph.setTanggal(d.tanggal); ph.setTanggalPosting(d.tanggal); ph.setTbmuser(u);
            ph.setPosting(Boolean.TRUE); ph.setKeterangan(ket); s.save(ph);
            boolean ok = CommonAkunting.saveTransaksi(new Akun[] { d.debit }, new Akun[] { d.kredit }, null, null,
                    ph, true, ket, d.tanggal, new Double[] { Double.valueOf(d.nilai) },
                    new Double[] { Double.valueOf(d.nilai) }, Double.valueOf(0), null, null, s);
            if (!ok) { tx.rollback(); return false; }
            if (pembayaran) ((ApotikPbfPembayaran) sumber).setPostingHistory(ph);
            else ((ApotikPbfDokumen) sumber).setPostingHistory(ph);
            s.saveOrUpdate(sumber); tx.commit(); return true;
        } catch (Exception e) { try { tx.rollback(); } catch (Exception x) { ais.common.ErrorAuditUtil.record(x, "rollback ApotikPbfPostingHelper"); } throw e; }
    }

    private static JSONArray riwayat(Session s, boolean pembayaran, String mulai, String sampai, int batas) throws Exception {
        String sql = pembayaran
                ? "SELECT b.id,d.kode||'/BYR-'||CAST(b.id AS text),b.nominal,b.tanggal,b.posting_history,"
                    + "COALESCE((SELECT MIN(g.kode) FROM akunting.grup_transaksi g WHERE g.posting_history=b.posting_history),''),ph.tanggalposting"
                    + " FROM sirs.apotik_pbf_pembayaran b JOIN sirs.apotik_pbf_dokumen d ON d.id=b.dokumen"
                    + " JOIN akunting.posting_history ph ON ph.id=b.posting_history WHERE date(b.tanggal) BETWEEN date(?) AND date(?)"
                    + " ORDER BY b.tanggal DESC,b.id DESC LIMIT " + batas
                : "SELECT d.id,d.kode,d.total,d.tanggal,d.posting_history,"
                    + "COALESCE((SELECT MIN(g.kode) FROM akunting.grup_transaksi g WHERE g.posting_history=d.posting_history),''),ph.tanggalposting"
                    + " FROM sirs.apotik_pbf_dokumen d JOIN akunting.posting_history ph ON ph.id=d.posting_history"
                    + " WHERE date(d.tanggal) BETWEEN date(?) AND date(?) ORDER BY d.tanggal DESC,d.id DESC LIMIT " + batas;
        PreparedStatement ps = s.connection().prepareStatement(sql); ps.setString(1, mulai); ps.setString(2, sampai);
        ResultSet rs = ps.executeQuery(); JSONArray out = new JSONArray();
        while (rs.next()) out.put(PostingStatusUtil.sudah(rs.getLong(1), rs.getString(2), rs.getDouble(3),
                rs.getTimestamp(4), rs.getLong(5), rs.getString(6), rs.getDate(7), ""));
        rs.close(); ps.close(); return out;
    }

    private static ApotikAkunMapping mapping(Session s, String peran) {
        return (ApotikAkunMapping) s.createCriteria(ApotikAkunMapping.class).add(Restrictions.eq("peran", peran))
                .add(Restrictions.eq("aktif", Boolean.TRUE)).setMaxResults(1).uniqueResult();
    }
    private static Akun akun(Session s, String peran) { ApotikAkunMapping m = mapping(s, peran); return m == null ? null : m.getAkun(); }
    private static String namaAkun(Akun a) { return a == null ? "" : a.getKode() + " " + a.getNama(); }
}
