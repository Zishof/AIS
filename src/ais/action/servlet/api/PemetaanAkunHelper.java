package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;

/**
 * Pemetaan otomatis akun ke Kelompok Laporan.
 *
 * <p><b>Masalahnya.</b> Laporan resmi berbasis jurnal (Laba Rugi, Neraca) hanya menampilkan akun yang
 * sudah dipetakan ke Kelompok Laporan. Akun yang belum dipetakan nilainya tidak ikut terhitung, dan
 * pada data nyata mayoritas akun memang belum dipetakan sehingga laporan resmi tampak timpang.</p>
 *
 * <p><b>Cara kerja.</b> Kelompok tidak ditebak dari kata kunci, melainkan diambil dari BAGAN AKUN itu
 * sendiri: tiap akun ditelusuri rantai induknya sampai akar, lalu
 * <ul>
 *   <li>jenis laporan ditentukan akar-nya (1/2/3/7/8 = Neraca, 4/5/6 = Rugi Laba), dan</li>
 *   <li>kelompoknya memakai nama akun induk pada jenjang ke-3 (jatuh ke jenjang ke-2 lalu akar bila
 *       jenjangnya lebih dangkal) &mdash; mis. 512.112 BEBAN ADM.BANK masuk kelompok
 *       "BEBAN ADMINISTRASI UMUM" (512.000), 111.101 KAS YAYASAN masuk "KAS" (111.000).</li>
 * </ul>
 * Dengan begitu nama kelompok selalu memakai istilah yang sudah dipakai lembaga pada bagan akunnya.</p>
 *
 * <p><b>Aturan aman.</b> Hanya MENAMBAH: akun yang sudah punya kelompok aktif tidak disentuh, tidak
 * ada baris yang dihapus atau dipindah. Kelompok yang sudah ada dipakai ulang (pembandingan nama
 * dinormalkan, ASET/AKTIVA dianggap sama) dan hanya dibuat baru bila memang belum ada. Semua
 * penulisan lewat Hibernate supaya terekam Envers, satu transaksi per baris.</p>
 *
 * <p>Aksi API: {@code pemetaan_akun_usulan} (pratinjau, tidak menulis apa pun) dan
 * {@code pemetaan_akun_terapkan}.</p>
 */
public final class PemetaanAkunHelper {

    private PemetaanAkunHelper() {
    }

    /** Satu akun beserta posisinya pada bagan. */
    private static final class Simpul {
        long id;
        String kode = "";
        String nama = "";
        Long parent;
    }

    /** Usulan pemetaan satu akun. */
    private static final class Usul {
        Simpul akun;
        String jenis;      // "Neraca" / "Rugi Laba"
        String kelompok;   // nama kelompok tujuan
        String kodeKelompok;
    }

    public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
        if ("pemetaan_akun_usulan".equals(action)) {
            jalankan(payload, hasil, false);
        } else if ("pemetaan_akun_terapkan".equals(action)) {
            jalankan(payload, hasil, true);
        } else {
            hasil.put("status", "99");
            hasil.put("message", "Aksi pemetaan akun tidak dikenal: " + action);
        }
    }

    /** Normalisasi nama kelompok utk pembandingan: huruf besar, spasi rapat, ASET disamakan AKTIVA. */
    private static String normal(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim().toUpperCase().replaceAll("\\s+", " ");
        v = v.replace("ASET", "AKTIVA");
        return v;
    }

    /** Jenis laporan dari digit pertama kode akar. */
    private static String jenisDariAkar(String kodeAkar) {
        String k = kodeAkar == null ? "" : kodeAkar.trim();
        if (k.length() == 0) {
            return "Neraca";
        }
        char c = k.charAt(0);
        if (c == '4' || c == '5' || c == '6') {
            return "Rugi Laba";
        }
        return "Neraca";
    }

    private static void jalankan(JSONObject payload, JSONObject hasil, boolean terapkan) throws Exception {
        int batas = payload == null ? 0 : payload.optInt("batasContoh", 0);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Connection conn = session.connection();

            // 1) seluruh bagan akun
            Map<Long, Simpul> peta = new LinkedHashMap<Long, Simpul>();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, COALESCE(kode,''), COALESCE(nama,''), parent FROM akunting.akun ORDER BY kode");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Simpul s = new Simpul();
                s.id = rs.getLong(1);
                s.kode = rs.getString(2);
                s.nama = rs.getString(3);
                long p = rs.getLong(4);
                s.parent = rs.wasNull() ? null : Long.valueOf(p);
                peta.put(Long.valueOf(s.id), s);
            }
            rs.close();
            ps.close();

            // 2) akun yang sudah punya kelompok aktif -> tidak disentuh
            Set<Long> sudah = new HashSet<Long>();
            ps = conn.prepareStatement("SELECT DISTINCT p.akun FROM akunting.kelompok_laporan_punya_akun p"
                    + " JOIN akunting.kelompok_laporan k ON k.id = p.kelompok_laporan"
                    + " WHERE (k.aktif IS NULL OR k.aktif)");
            rs = ps.executeQuery();
            while (rs.next()) {
                sudah.add(Long.valueOf(rs.getLong(1)));
            }
            rs.close();
            ps.close();

            // 3) kelompok yang sudah ada, per jenis laporan
            Map<String, Long> kelompokAda = new HashMap<String, Long>();   // "JENIS|NAMA" -> id
            Map<String, Long> jenisAda = new HashMap<String, Long>();      // "NERACA"/"RUGI LABA" -> id
            ps = conn.prepareStatement("SELECT id, COALESCE(keterangan,'') FROM akunting.jenis_laporan");
            rs = ps.executeQuery();
            while (rs.next()) {
                String ket = normal(rs.getString(2));
                if (ket.indexOf("NERACA") >= 0) {
                    jenisAda.put("NERACA", Long.valueOf(rs.getLong(1)));
                } else if (ket.indexOf("RUGI") >= 0 || ket.indexOf("LABA") >= 0) {
                    jenisAda.put("RUGI LABA", Long.valueOf(rs.getLong(1)));
                }
            }
            rs.close();
            ps.close();
            ps = conn.prepareStatement("SELECT k.id, COALESCE(NULLIF(TRIM(COALESCE(k.keterangan,'')),''),"
                    + " COALESCE(m.keterangan,'')), COALESCE(j.keterangan,'')"
                    + " FROM akunting.kelompok_laporan k"
                    + " LEFT JOIN akunting.master_grup_laporan m ON m.id = k.master_grup_laporan"
                    + " LEFT JOIN akunting.jenis_laporan j ON j.id = k.jenis_laporan"
                    + " WHERE (k.aktif IS NULL OR k.aktif)");
            rs = ps.executeQuery();
            while (rs.next()) {
                String nama = normal(rs.getString(2));
                String jenis = normal(rs.getString(3)).indexOf("NERACA") >= 0 ? "NERACA" : "RUGI LABA";
                if (nama.length() == 0) {
                    continue;
                }
                String kunci = jenis + "|" + nama;
                if (!kelompokAda.containsKey(kunci)) {
                    kelompokAda.put(kunci, Long.valueOf(rs.getLong(1)));
                }
            }
            rs.close();
            ps.close();

            // 4) susun usulan
            List<Usul> usulan = new ArrayList<Usul>();
            for (Map.Entry<Long, Simpul> e : peta.entrySet()) {
                Simpul akun = e.getValue();
                if (sudah.contains(Long.valueOf(akun.id))) {
                    continue;
                }
                List<Simpul> rantai = new ArrayList<Simpul>();
                Simpul kursor = akun;
                int aman = 0;
                while (kursor != null && aman++ < 20) {
                    rantai.add(0, kursor);
                    kursor = kursor.parent == null ? null : peta.get(kursor.parent);
                }
                if (rantai.isEmpty()) {
                    continue;
                }
                Simpul akar = rantai.get(0);
                int idx = rantai.size() > 2 ? 2 : (rantai.size() - 1);
                Simpul grup = rantai.get(idx);
                // Akun induk itu sendiri dikelompokkan pada jenjang di atasnya supaya tidak jadi
                // kelompok yang isinya hanya dirinya sendiri.
                if (grup.id == akun.id && rantai.size() > 1) {
                    grup = rantai.get(rantai.size() - 2);
                }
                Usul u = new Usul();
                u.akun = akun;
                u.jenis = jenisDariAkar(akar.kode);
                u.kelompok = grup.nama == null || grup.nama.trim().length() == 0 ? akar.nama : grup.nama.trim();
                u.kodeKelompok = grup.kode;
                usulan.add(u);
            }

            // 5) ringkasan per kelompok
            Map<String, int[]> ringkas = new LinkedHashMap<String, int[]>();   // "jenis|nama" -> {jumlah, kelompokBaru}
            for (int i = 0; i < usulan.size(); i++) {
                Usul u = usulan.get(i);
                String kunci = u.jenis + "|" + u.kelompok;
                int[] v = ringkas.get(kunci);
                if (v == null) {
                    boolean ada = kelompokAda.containsKey(normal(u.jenis) + "|" + normal(u.kelompok));
                    v = new int[] { 0, ada ? 0 : 1 };
                    ringkas.put(kunci, v);
                }
                v[0]++;
            }

            JSONArray arrRingkas = new JSONArray();
            for (Map.Entry<String, int[]> e : ringkas.entrySet()) {
                String[] bagi = e.getKey().split("\\|", 2);
                JSONObject j = new JSONObject();
                j.put("jenis", bagi[0]);
                j.put("kelompok", bagi.length > 1 ? bagi[1] : "");
                j.put("jumlahAkun", e.getValue()[0]);
                j.put("kelompokBaru", e.getValue()[1] == 1);
                arrRingkas.put(j);
            }

            JSONArray arrContoh = new JSONArray();
            int maxContoh = batas > 0 ? batas : usulan.size();
            for (int i = 0; i < usulan.size() && i < maxContoh; i++) {
                Usul u = usulan.get(i);
                JSONObject j = new JSONObject();
                j.put("kode", u.akun.kode);
                j.put("nama", u.akun.nama);
                j.put("jenis", u.jenis);
                j.put("kelompok", u.kelompok);
                j.put("kodeKelompok", u.kodeKelompok);
                arrContoh.put(j);
            }

            hasil.put("status", "00");
            hasil.put("jumlahBelumDipetakan", usulan.size());
            hasil.put("jumlahKelompok", ringkas.size());
            hasil.put("ringkasan", arrRingkas);
            hasil.put("usulan", arrContoh);

            if (!terapkan) {
                hasil.put("message", usulan.isEmpty()
                        ? "Semua akun sudah dipetakan."
                        : "Pratinjau: " + usulan.size() + " akun akan dipetakan ke " + ringkas.size() + " kelompok.");
                return;
            }

            // 6) terapkan
            int dibuatKelompok = 0;
            int dipetakan = 0;
            JSONArray masalah = new JSONArray();
            Map<String, Long> cache = new HashMap<String, Long>(kelompokAda);
            for (int i = 0; i < usulan.size(); i++) {
                Usul u = usulan.get(i);
                String kunciNorm = normal(u.jenis) + "|" + normal(u.kelompok);
                try {
                    Long idKelompok = cache.get(kunciNorm);
                    if (idKelompok == null) {
                        Long idJenis = jenisAda.get(normal(u.jenis).indexOf("NERACA") >= 0 ? "NERACA" : "RUGI LABA");
                        if (idJenis == null) {
                            masalah.put("Jenis Laporan '" + u.jenis + "' belum ada di master; akun " + u.akun.kode + " dilewati.");
                            continue;
                        }
                        session.beginTransaction();
                        KelompokLaporan kl = new KelompokLaporan();
                        kl.setKeterangan(u.kelompok);
                        kl.setJenisLaporan((JenisLaporan) session.load(JenisLaporan.class, idJenis));
                        kl.setAktif(Boolean.TRUE);
                        kl.setUrut(Double.valueOf(urutDariKode(u.kodeKelompok)));
                        session.save(kl);
                        session.getTransaction().commit();
                        idKelompok = kl.getId();
                        cache.put(kunciNorm, idKelompok);
                        dibuatKelompok++;
                    }
                    session.beginTransaction();
                    KelompokLaporanPunyaAkun pa = new KelompokLaporanPunyaAkun();
                    pa.setAkun((Akun) session.load(Akun.class, Long.valueOf(u.akun.id)));
                    pa.setKelompokLaporan((KelompokLaporan) session.load(KelompokLaporan.class, idKelompok));
                    session.save(pa);
                    session.getTransaction().commit();
                    dipetakan++;
                } catch (Exception ex) {
                    batalkanDiam(session);
                    ais.common.ErrorAuditUtil.record(ex, "auto-audit PemetaanAkunHelper.terapkan " + u.akun.kode);
                    masalah.put("Akun " + u.akun.kode + " " + u.akun.nama + ": " + ex.getMessage());
                }
            }
            hasil.put("dipetakan", dipetakan);
            hasil.put("kelompokBaru", dibuatKelompok);
            hasil.put("masalah", masalah);
            hasil.put("message", dipetakan + " akun dipetakan (" + dibuatKelompok + " kelompok baru dibuat)"
                    + (masalah.length() > 0 ? ", " + masalah.length() + " baris gagal." : "."));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /** Urutan tampil kelompok mengikuti kode akun induknya (mis. 512.000 -> 512) agar urut seperti bagan akun. */
    private static double urutDariKode(String kode) {
        if (kode == null) {
            return 9999;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kode.length(); i++) {
            char c = kode.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c == '.' || c == '-') {
                break;
            }
        }
        try {
            return sb.length() == 0 ? 9999 : Double.parseDouble(sb.toString());
        } catch (Exception e) {
            return 9999;
        }
    }

    private static void batalkanDiam(Session session) {
        try {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PemetaanAkunHelper.batalkanDiam");
        }
    }
}
