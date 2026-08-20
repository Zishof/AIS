package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Deposit;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.PenyesuaianSaldoAnggota;

/**
 * <h3>Penyesuaian Saldo Anggota &mdash; opname untuk saldo voucher/deposit member.</h3>
 *
 * <p>Konsepnya sama dengan Stok Opname pada barang: petugas memasukkan <b>saldo yang seharusnya</b>,
 * sistem membandingkannya dengan <b>saldo menurut hitungan sistem</b>, lalu selisihnya dicatat
 * beserta alasannya. Bedanya hanya pada cara koreksi diterapkan &mdash; saldo member tidak disimpan
 * sebagai satu kolom melainkan dihitung dari mutasi, sehingga penyesuaian membuat satu baris
 * {@link Deposit} senilai selisih (positif menambah, negatif mengurangi). Riwayat mutasi tetap utuh
 * dan hasil hitungan langsung cocok dengan hasil opname.</p>
 *
 * <p><b>Hak akses.</b> Yang boleh mengubah nilai saldo lewat penyesuaian ini HANYA pengguna yang
 * boleh menambah dan mengubah deposit, yaitu peran ber-{@code Tbmrole.bolehEntryTopup} &mdash;
 * gerbang yang sama persis dengan {@code topupSaldo} dan {@code depositUbah}. Pemeriksaan dilakukan
 * di server, bukan sekadar menyembunyikan tombol di klien.</p>
 *
 * <p>Aksi: {@code penyesuaian_saldo_cek} (baca saldo sistem terkini),
 * {@code penyesuaian_saldo_simpan} (terapkan koreksi), dan {@code penyesuaian_saldo_list}
 * (riwayat penyesuaian).</p>
 */
public final class PenyesuaianSaldoHelper {

    /** Penanda pada keterangan baris Deposit koreksi agar mudah dikenali di riwayat mutasi. */
    public static final String PENANDA = "[Penyesuaian Saldo]";

    private PenyesuaianSaldoHelper() {
    }

    public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
            throws Exception {
        if ("penyesuaian_saldo_cek".equals(action)) {
            cek(payload, hasil);
        } else if ("penyesuaian_saldo_simpan".equals(action)) {
            simpan(tbmuser, payload, hasil);
        } else if ("penyesuaian_saldo_list".equals(action)) {
            daftar(payload, hasil);
        } else {
            hasil.put("status", "91");
            hasil.put("description", "Aksi penyesuaian saldo tidak dikenal: " + action);
        }
    }

    /**
     * Gerbang tunggal: hanya peran yang boleh menambah &amp; mengubah deposit (topup) yang berhak
     * menyesuaikan saldo. Dipakai baik oleh {@code simpan} maupun sebagai rujukan klien.
     */
    private static boolean bolehUbahSaldo(Tbmuser tbmuser) {
        try {
            ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
            return role != null && role.getBolehEntryTopup() != null
                    && role.getBolehEntryTopup().booleanValue();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit PenyesuaianSaldoHelper.bolehUbahSaldo");
            return false;
        }
    }

    // ==================================================================== cek saldo

    /** Saldo sistem TERKINI milik satu member; dibaca ulang tepat sebelum petugas mengisi opname. */
    private static void cek(JSONObject payload, JSONObject hasil) throws Exception {
        String idMember = payload.optString("id_member", "").trim();
        if (idMember.isEmpty() || !Common.isNumber(idMember)) {
            hasil.put("status", "91");
            hasil.put("description", "Member wajib dipilih.");
            return;
        }
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = (AnggotaKoperasi) session.get(AnggotaKoperasi.class,
                    Long.valueOf(idMember));
            if (anggota == null) {
                hasil.put("status", "91");
                hasil.put("description", "Member tidak ditemukan.");
                return;
            }
            hasil.put("status", "00");
            hasil.put("idMember", anggota.getId());
            hasil.put("namaMember", anggota.getNama() == null ? "" : anggota.getNama());
            hasil.put("saldoSistem", saldoSistem(anggota));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    /** Saldo menurut sistem = perhitungan resmi DepositHelper (deposit masuk dikurangi pemakaian). */
    private static double saldoSistem(AnggotaKoperasi anggota) {
        try {
            Double v = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(anggota);
            return v == null ? 0 : v.doubleValue();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit PenyesuaianSaldoHelper.saldoSistem");
            return 0;
        }
    }

    // ==================================================================== simpan

    private static void simpan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
        if (!bolehUbahSaldo(tbmuser)) {
            hasil.put("status", "91");
            hasil.put("description", "Anda tidak memiliki hak akses untuk mengubah saldo member. "
                    + "Penyesuaian saldo hanya boleh dilakukan pengguna yang berhak menambah dan "
                    + "mengubah topup/deposit (hak \"Boleh Entry Topup\").");
            return;
        }
        String idMember = payload.optString("id_member", "").trim();
        if (idMember.isEmpty() || !Common.isNumber(idMember)) {
            hasil.put("status", "91");
            hasil.put("description", "Member wajib dipilih.");
            return;
        }
        String saldoFisikTeks = payload.optString("saldo_fisik", "").trim();
        if (saldoFisikTeks.isEmpty() || !Common.isNumber(saldoFisikTeks)) {
            hasil.put("status", "91");
            hasil.put("description", "Saldo seharusnya wajib diisi berupa angka.");
            return;
        }
        double saldoFisik = Double.parseDouble(saldoFisikTeks);
        if (saldoFisik < 0) {
            hasil.put("status", "91");
            hasil.put("description", "Saldo seharusnya tidak boleh bernilai negatif.");
            return;
        }
        String keterangan = payload.optString("keterangan", "").trim();
        if (keterangan.isEmpty()) {
            hasil.put("status", "91");
            hasil.put("description", "Alasan penyesuaian wajib diisi agar koreksi saldo dapat "
                    + "dipertanggungjawabkan (mis. \"koreksi topup ganda 19 Agu\").");
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = (AnggotaKoperasi) session.get(AnggotaKoperasi.class,
                    Long.valueOf(idMember));
            if (anggota == null) {
                hasil.put("status", "91");
                hasil.put("description", "Member tidak ditemukan.");
                return;
            }
            // Saldo sistem dibaca ULANG di sini (bukan memakai angka kiriman klien) supaya
            // selisihnya dihitung dari keadaan terkini, bukan dari layar yang mungkin sudah basi.
            double sistem = saldoSistem(anggota);
            double selisih = saldoFisik - sistem;
            if (Math.abs(selisih) < 0.005) {
                hasil.put("status", "91");
                hasil.put("description", "Saldo sistem sudah sama dengan saldo yang Anda masukkan "
                        + "(" + Common.numberFormat.get().format(sistem) + "), tidak ada yang perlu disesuaikan.");
                return;
            }

            ais.database.model.JenisPembayaran jenisPembayaran =
                    (ais.database.model.JenisPembayaran) session
                            .createCriteria(ais.database.model.JenisPembayaran.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"),
                                    Restrictions.eq("aktif", Boolean.TRUE)))
                            .add(Restrictions.isNull("jenisTabungan")).addOrder(Order.asc("id"))
                            .setMaxResults(1).uniqueResult();
            ais.database.model.JenisTabungan jenisTabungan =
                    (ais.database.model.JenisTabungan) session
                            .createCriteria(ais.database.model.JenisTabungan.class)
                            .add(Restrictions.eq("defaultTabungan", Boolean.TRUE))
                            .add(Restrictions.or(Restrictions.isNull("aktif"),
                                    Restrictions.eq("aktif", Boolean.TRUE)))
                            .setMaxResults(1).uniqueResult();
            if (jenisTabungan == null) {
                jenisTabungan = (ais.database.model.JenisTabungan) session
                        .createCriteria(ais.database.model.JenisTabungan.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"),
                                Restrictions.eq("aktif", Boolean.TRUE)))
                        .addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
            }
            if (jenisPembayaran == null || jenisTabungan == null) {
                hasil.put("status", "91");
                hasil.put("description", "Penyesuaian belum dapat disimpan karena Cara Pembayaran "
                        + "atau Jenis Tabungan default belum dikonfigurasi. Buka menu Keuangan, "
                        + "tetapkan satu data aktif sebagai default, lalu coba kembali.");
                return;
            }

            Date sekarang = new Date();
            String oleh = tbmuser == null ? "penyesuaian_saldo" : tbmuser.getUserId();

            session.getTransaction().begin();
            try {
                // 1. Baris Deposit senilai SELISIH -- inilah yang benar-benar menggeser saldo.
                Deposit deposit = new Deposit();
                deposit.setAnggotaKoperasi(anggota);
                deposit.setNominal(Double.valueOf(selisih));
                deposit.setJenisPembayaran(jenisPembayaran);
                deposit.setJenisTabungan(jenisTabungan);
                deposit.setWaktu(sekarang);
                deposit.setKeterangan(PENANDA + " " + keterangan);
                deposit.setOleh(oleh);
                deposit.setOlehId(oleh);
                session.save(deposit);

                // 2. Catatan opname-nya: saldo sistem & saldo seharusnya dibekukan sebagai bukti.
                PenyesuaianSaldoAnggota ps = new PenyesuaianSaldoAnggota();
                ps.setAnggotaKoperasi(anggota);
                ps.setSaldoSistem(Double.valueOf(sistem));
                ps.setSaldoFisik(Double.valueOf(saldoFisik));
                ps.setSelisih(Double.valueOf(selisih));
                ps.setWaktu(sekarang);
                ps.setKeterangan(keterangan);
                ps.setDeposit(deposit);
                ps.setOleh(oleh);
                ps.setOlehId(oleh);
                session.save(ps);

                session.getTransaction().commit();
            } catch (Exception e) {
                batalkanDiam(session);
                throw e;
            }

            hasil.put("status", "00");
            hasil.put("saldoSistemLama", sistem);
            hasil.put("saldoBaru", saldoFisik);
            hasil.put("selisih", selisih);
            hasil.put("description", "Saldo " + (anggota.getNama() == null ? "" : anggota.getNama())
                    + " disesuaikan dari " + Common.numberFormat.get().format(sistem) + " menjadi "
                    + Common.numberFormat.get().format(saldoFisik) + " (selisih "
                    + Common.numberFormat.get().format(selisih) + ").");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== riwayat

    private static void daftar(JSONObject payload, JSONObject hasil) throws Exception {
        String cari = payload.optString("cari", "").trim();
        String mulai = payload.optString("mulai", "").trim();
        String sampai = payload.optString("sampai", "").trim();
        int limit = Math.min(500, Math.max(1, payload.optInt("limit", 100)));

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Connection conn = session.connection();
            StringBuilder sql = new StringBuilder(
                    "SELECT p.id, p.waktu, COALESCE(a.nama,''), COALESCE(a.kode,''),"
                            + " COALESCE(p.saldo_sistem,0), COALESCE(p.saldo_fisik,0),"
                            + " COALESCE(p.selisih,0), COALESCE(p.keterangan,''), COALESCE(p.oleh,'')"
                            + " FROM koperasi.penyesuaian_saldo_anggota p"
                            + " LEFT JOIN koperasi.anggota_koperasi a ON a.id = p.anggota_koperasi"
                            + " WHERE 1=1");
            java.util.List<String> prm = new java.util.ArrayList<String>();
            if (!cari.isEmpty()) {
                sql.append(" AND (COALESCE(a.nama,'') ILIKE ? OR COALESCE(a.kode,'') ILIKE ?"
                        + " OR COALESCE(p.keterangan,'') ILIKE ?)");
                prm.add("%" + cari + "%");
                prm.add("%" + cari + "%");
                prm.add("%" + cari + "%");
            }
            if (!mulai.isEmpty()) {
                sql.append(" AND date(p.waktu) >= date(?)");
                prm.add(mulai);
            }
            if (!sampai.isEmpty()) {
                sql.append(" AND date(p.waktu) <= date(?)");
                prm.add(sampai);
            }
            sql.append(" ORDER BY p.waktu DESC, p.id DESC LIMIT ").append(limit);

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < prm.size(); i++) {
                ps.setString(i + 1, prm.get(i));
            }
            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("id", rs.getLong(1));
                j.put("waktu", rs.getTimestamp(2) == null ? ""
                        : Common.dateFormat3.get().format(rs.getTimestamp(2)));
                j.put("namaMember", rs.getString(3));
                j.put("kodeMember", rs.getString(4));
                j.put("saldoSistem", rs.getDouble(5));
                j.put("saldoFisik", rs.getDouble(6));
                j.put("selisih", rs.getDouble(7));
                j.put("keterangan", rs.getString(8));
                j.put("oleh", rs.getString(9));
                arr.put(j);
            }
            rs.close();
            ps.close();
            hasil.put("status", "00");
            hasil.put("data", arr);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    private static void batalkanDiam(Session session) {
        try {
            if (session != null && session.getTransaction() != null
                    && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e,
                    "auto-audit(empty-catch) PenyesuaianSaldoHelper.batalkanDiam");
        }
    }
}
