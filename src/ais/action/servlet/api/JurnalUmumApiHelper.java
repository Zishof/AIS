package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Closing;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.JenisTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.Workspace;

/**
 * <h3>Jurnal Umum untuk POS Desktop/Android</h3>
 *
 * <p><b>Untuk apa.</b> Mencatat jurnal manual (jurnal umum) langsung dari POS: koreksi, penyesuaian,
 * biaya yang tidak lewat modul lain, saldo awal, dan sebagainya. Selama ini pencatatan semacam ini
 * hanya bisa dilakukan di layar ZK {@code GrupTransaksiAction} / {@code TransaksiJurnalUmumHelper};
 * helper ini memindahkan logikanya ke API supaya tersedia di Desktop &amp; Android.</p>
 *
 * <p><b>Bentuk datanya sama persis dengan ZK</b> sehingga jurnal dari POS dan dari ZK bercampur
 * mulus di satu buku besar:</p>
 * <ul>
 *   <li>{@link GrupTransaksi} = kepala jurnal (kode, tanggal, jenis transaksi, keterangan, total
 *       debet/kredit, {@code jenisJurnal = "Umum"}),</li>
 *   <li>{@link Transaksi} = baris jurnal (akun, debet, kredit, keterangan) yang menunjuk kepalanya.</li>
 * </ul>
 *
 * <p><b>Aturan yang ditegakkan</b> (meniru {@code TransaksiJurnalUmumHelper#simpan}):</p>
 * <ol>
 *   <li><b>Seimbang</b> &mdash; total debet wajib sama dengan total kredit, dan minimal 2 baris.</li>
 *   <li><b>Satu baris satu sisi</b> &mdash; sebuah baris mengisi debet ATAU kredit, tidak keduanya.</li>
 *   <li><b>Periode tutup buku</b> &mdash; tanggal jurnal tidak boleh sebelum tanggal closing terakhir.</li>
 *   <li><b>Kode unik</b> &mdash; nomor jurnal dibuat otomatis dari kode Jenis Transaksi
 *       ({@code KODE/BULAN/URUT}, pola {@code generateCode} milik ZK) dan ditolak bila bentrok.</li>
 *   <li><b>Jurnal terposting dikunci</b> &mdash; setelah diposting, jurnal tidak dapat diubah atau
 *       dihapus; harus dibatalkan posting-nya lebih dulu (dan itu pun hanya untuk jurnal umum yang
 *       dibuat lewat modul ini, bukan jurnal hasil posting modul lain).</li>
 * </ol>
 *
 * <p><b>Draf vs Terposting.</b> Jurnal yang baru disimpan berstatus DRAF ({@code posting_history}
 * masih kosong) dan BELUM masuk laporan keuangan resmi &mdash; seluruh laporan berbasis jurnal
 * menyaring {@code posting_history is not null}. Menekan Posting-lah yang memasukkannya ke buku
 * besar. Pemisahan ini disengaja supaya kesalahan ketik masih bisa diperbaiki sebelum resmi.</p>
 */
public final class JurnalUmumApiHelper {

    /** Jenis PostingHistory untuk jurnal umum dari POS; dipakai juga sebagai pagar batal-posting. */
    public static final String JENIS_POSTING = "Jurnal Umum POS";

    /** Kontrak tanggal API ini: yyyy-MM-dd (bukan format tampilan Indonesia milik Common). */
    private static java.text.SimpleDateFormat iso() {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd");
        f.setLenient(false);
        return f;
    }

    /** Id workspace bisa 19 digit dan pada instalasi lama bernilai negatif. */
    private static long idDari(JSONObject payload, String nama) {
        if (payload == null) {
            return 0L;
        }
        String teks = payload.optString(nama + "Teks", "").trim();
        if (teks.length() == 0) {
            teks = payload.optString(nama, "").trim();
        }
        try {
            return teks.length() == 0 ? payload.optLong(nama, 0L) : Long.parseLong(teks);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private JurnalUmumApiHelper() {
    }

    /**
     * Penjaga hak per-aksi untuk menu Jurnal Umum.
     *
     * <p>Sebelumnya endpoint ini SAMA SEKALI tidak memeriksa hak per-aksi: siapa pun yang
     * dapat melihat menunya dapat menyimpan, menghapus, memposting, dan membatalkan posting.
     * Kotak centang di Tbmrole pun belum ada untuk menu ini, sehingga admin tidak punya cara
     * membatasinya.</p>
     *
     * <p>Pemetaan aksinya: {@code create} menyimpan draf, {@code delete} menghapus,
     * {@code approve} MEMPOSTING ke buku besar, {@code reject} membatalkan posting. Memposting
     * sengaja dipisah dari menyimpan -- menulis buku besar adalah kewenangan yang lazim
     * dipegang orang lain.</p>
     */
    private static boolean bolehAksi(Tbmuser tbmuser, JSONObject payload, JSONObject hasil, String aksi)
            throws Exception {
        if (ais.common.Common.getApakahAdminLain(tbmuser)) {
            return true;
        }
        ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
        if (role == null) {
            return true;
        }
        // Aturan yang sama dengan kelompok Akuntansi lain: kotak yang sudah diatur admin
        // menang, yang belum pernah diatur mengikuti visibilitas menunya.
        if (ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(role.getEbisnisMenu(),
                role.getRoleId(), "jurnal_umum", aksi)) {
            return true;
        }
        hasil.put("status", "99");
        hasil.put("message", "Grup pengguna Anda tidak memiliki hak " + aksi
                + " pada menu Jurnal Umum.");
        hasil.put("description", hasil.optString("message", ""));
        return false;
    }

    /**
     * Hak keempat wewenang menu Jurnal Umum, dikirim bersama daftar supaya layar dapat
     * MEMADAMKAN tombol yang sudah pasti ditolak.
     *
     * <p>Keempatnya sengaja terpisah: {@code approve} (memposting ke buku besar) bukan
     * turunan dari {@code create} (menyimpan draf), dan {@code reject} (membatalkan
     * posting) berbeda lagi. Menggabungkannya akan memberi wewenang yang tidak pernah
     * diberikan admin.</p>
     *
     * <p>Memakai pemeriksa yang tidak menulis pesan penolakan ke {@code hasil} -- di sini
     * kita sedang MELAPORKAN hak, bukan menolak permintaan.</p>
     */
    private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
        JSONObject j = new JSONObject();
        String[] aksi = { "create", "update", "delete", "approve", "reject" };
        boolean admin = ais.common.Common.getApakahAdminLain(tbmuser);
        ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
        for (int i = 0; i < aksi.length; i++) {
            boolean boleh = admin || role == null
                    || ais.common.EbisnisMenuKatalog.bolehAksiAkuntansi(role.getEbisnisMenu(),
                            role.getRoleId(), "jurnal_umum", aksi[i]);
            j.put(aksi[i], boleh);
        }
        return j;
    }

    public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
            throws Exception {
        if ("jurnal_umum_list".equals(action)) {
            daftar(payload, hasil);
            hasil.put("hak", hakAksesJson(tbmuser));
        } else if ("jurnal_umum_detail".equals(action)) {
            detail(payload, hasil);
        } else if ("jurnal_umum_simpan".equals(action)) {
            if (!bolehAksi(tbmuser, payload, hasil, "create")) { return; }
            simpan(tbmuser, payload, hasil);
        } else if ("jurnal_umum_hapus".equals(action)) {
            if (!bolehAksi(tbmuser, payload, hasil, "delete")) { return; }
            hapus(payload, hasil);
        } else if ("jurnal_umum_posting".equals(action)) {
            // Memposting = menulis ke buku besar. Kewenangan tersendiri, bukan turunan
            // dari boleh menyimpan draf.
            if (!bolehAksi(tbmuser, payload, hasil, "approve")) { return; }
            posting(tbmuser, payload, hasil, true);
        } else if ("jurnal_umum_batal_posting".equals(action)) {
            if (!bolehAksi(tbmuser, payload, hasil, "reject")) { return; }
            posting(tbmuser, payload, hasil, false);
        } else if ("jurnal_umum_cari_anggaran".equals(action)) {
            AnggaranKeuanganUtil.cari(payload, hasil);
        } else if ("jurnal_umum_jenis_transaksi".equals(action)) {
            jenisTransaksi(payload, hasil);
        } else {
            hasil.put("status", "99");
            hasil.put("message", "Aksi jurnal umum tidak dikenal: " + action);
        }
    }

    // ==================================================================== daftar

    /** Daftar kepala jurnal umum + total & status posting, disaring periode/kata kunci. */
    private static void daftar(JSONObject payload, JSONObject hasil) throws Exception {
        String mulai = payload.optString("mulai", "").trim();
        String sampai = payload.optString("sampai", "").trim();
        String cari = payload.optString("cari", "").trim();
        String status = payload.optString("status", "").trim();   // "", "draf", "terposting"
        int limit = Math.min(1000, Math.max(1, payload.optInt("limit", 200)));

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Connection conn = session.connection();
            StringBuilder sql = new StringBuilder(
                    "SELECT g.id, COALESCE(g.kode,''), g.tanggal_transaksi, COALESCE(g.keterangan,''),"
                            + " COALESCE(g.total_debet,0), COALESCE(g.total_kredit,0), g.posting_history,"
                            + " COALESCE(jt.nama,''), COALESCE(jt.kode,''),"
                            + " (SELECT COUNT(*) FROM akunting.transaksi t WHERE t.grup_transaksi = g.id),"
                            + " g.workspace, COALESCE(w.kode,''), COALESCE(w.nama,'')"
                            + " FROM akunting.grup_transaksi g"
                            + " LEFT JOIN akunting.jenis_transaksi jt ON jt.id = g.jenis_transaksi"
                            + " LEFT JOIN rab.workspace w ON w.id = g.workspace"
                            + " WHERE COALESCE(g.jenis_jurnal,'') = ?");
            List<Object> prm = new ArrayList<Object>();
            prm.add(Transaksi.JURNAL_UMUM);
            if (!mulai.isEmpty()) {
                sql.append(" AND date(g.tanggal_transaksi) >= date(?)");
                prm.add(mulai);
            }
            if (!sampai.isEmpty()) {
                sql.append(" AND date(g.tanggal_transaksi) <= date(?)");
                prm.add(sampai);
            }
            if (!cari.isEmpty()) {
                sql.append(" AND (g.kode ILIKE ? OR COALESCE(g.keterangan,'') ILIKE ?)");
                prm.add("%" + cari + "%");
                prm.add("%" + cari + "%");
            }
            if ("draf".equalsIgnoreCase(status)) {
                sql.append(" AND g.posting_history IS NULL");
            } else if ("terposting".equalsIgnoreCase(status)) {
                sql.append(" AND g.posting_history IS NOT NULL");
            }
            sql.append(" ORDER BY g.tanggal_transaksi DESC, g.id DESC LIMIT ").append(limit);

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < prm.size(); i++) {
                ps.setString(i + 1, (String) prm.get(i));
            }
            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();
            double totalDebet = 0, totalKredit = 0;
            int draf = 0;
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("id", rs.getLong(1));
                j.put("kode", rs.getString(2));
                j.put("tanggal", rs.getTimestamp(3) == null ? ""
                        : iso().format(rs.getTimestamp(3)));
                j.put("keterangan", rs.getString(4));
                j.put("totalDebet", rs.getDouble(5));
                j.put("totalKredit", rs.getDouble(6));
                rs.getLong(7);
                boolean terposting = !rs.wasNull();
                j.put("terposting", terposting);
                j.put("jenisTransaksi", rs.getString(8));
                j.put("kodeJenis", rs.getString(9));
                j.put("jumlahBaris", rs.getInt(10));
                long workspaceId = rs.getLong(11);
                boolean tanpaWorkspace = rs.wasNull();
                j.put("workspaceId", tanpaWorkspace ? JSONObject.NULL : Long.valueOf(workspaceId));
                j.put("workspaceIdTeks", tanpaWorkspace ? "" : String.valueOf(workspaceId));
                j.put("workspaceKode", rs.getString(12));
                j.put("workspaceNama", rs.getString(13));
                arr.put(j);
                totalDebet += rs.getDouble(5);
                totalKredit += rs.getDouble(6);
                if (!terposting) {
                    draf++;
                }
            }
            rs.close();
            ps.close();
            hasil.put("status", "00");
            hasil.put("data", arr);
            hasil.put("totalDebet", totalDebet);
            hasil.put("totalKredit", totalKredit);
            hasil.put("jumlahDraf", draf);
            hasil.put("tanggalClosing", tanggalClosingTeks(session));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== detail

    private static void detail(JSONObject payload, JSONObject hasil) throws Exception {
        long id = payload.optLong("id", 0);
        if (id <= 0) {
            hasil.put("status", "99");
            hasil.put("message", "Id jurnal tidak valid.");
            return;
        }
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            GrupTransaksi g = (GrupTransaksi) session.get(GrupTransaksi.class, Long.valueOf(id));
            if (g == null) {
                hasil.put("status", "99");
                hasil.put("message", "Jurnal tidak ditemukan.");
                return;
            }
            JSONObject kepala = new JSONObject();
            kepala.put("id", g.getId());
            kepala.put("kode", g.getKode() == null ? "" : g.getKode());
            kepala.put("tanggal", g.getTanggalTransaksi() == null ? ""
                    : iso().format(g.getTanggalTransaksi()));
            kepala.put("keterangan", g.getKeterangan() == null ? "" : g.getKeterangan());
            kepala.put("terposting", g.getPostingHistory() != null);
            kepala.put("jenisTransaksiId", g.getJenisTransaksi() == null ? JSONObject.NULL
                    : g.getJenisTransaksi().getId());
            kepala.put("jenisTransaksi", g.getJenisTransaksi() == null ? ""
                    : g.getJenisTransaksi().getNama());
            Workspace workspace = g.getWorkspace();
            kepala.put("workspaceId", workspace == null ? JSONObject.NULL : workspace.getId());
            kepala.put("workspaceIdTeks", workspace == null ? "" : String.valueOf(workspace.getId()));
            kepala.put("workspaceNama", workspace == null ? "" : workspace.getNama());
            kepala.put("workspaceLabel", workspace == null ? ""
                    : ((workspace.getKode() == null ? "" : workspace.getKode()) + " - "
                            + (workspace.getNama() == null ? "" : workspace.getNama())));

            Connection conn = session.connection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.id, t.akun, COALESCE(a.kode,''), COALESCE(a.nama,''), COALESCE(t.debet,0),"
                            + " COALESCE(t.kredit,0), COALESCE(t.keterangan,'')"
                            + " FROM akunting.transaksi t LEFT JOIN akunting.akun a ON a.id = t.akun"
                            + " WHERE t.grup_transaksi = ? ORDER BY t.debet DESC, t.id");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            JSONArray baris = new JSONArray();
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("id", rs.getLong(1));
                j.put("akunId", rs.getLong(2));
                j.put("kodeAkun", rs.getString(3));
                j.put("namaAkun", rs.getString(4));
                j.put("debet", rs.getDouble(5));
                j.put("kredit", rs.getDouble(6));
                j.put("keterangan", rs.getString(7));
                baris.put(j);
            }
            rs.close();
            ps.close();
            hasil.put("status", "00");
            hasil.put("kepala", kepala);
            hasil.put("baris", baris);
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== simpan

    /**
     * Simpan jurnal umum baru / perbarui yang masih draf. Seluruh kepala + barisnya ditulis dalam
     * SATU transaksi basis data supaya tidak pernah ada jurnal setengah jadi (kepala tanpa baris).
     */
    private static void simpan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
        long id = payload.optLong("id", 0);
        String tanggalTeks = payload.optString("tanggal", "").trim();
        String keterangan = payload.optString("keterangan", "").trim();
        long idJenis = payload.optLong("jenisTransaksiId", 0);
        long workspaceId = idDari(payload, "workspaceId");
        JSONArray baris = payload.optJSONArray("baris");

        if (tanggalTeks.isEmpty()) {
            tolak(hasil, "Tanggal jurnal wajib diisi.");
            return;
        }
        if (keterangan.isEmpty()) {
            tolak(hasil, "Keterangan jurnal wajib diisi supaya mudah ditelusuri di buku besar.");
            return;
        }
        if (baris == null || baris.length() < 2) {
            tolak(hasil, "Jurnal minimal 2 baris: satu sisi debet dan satu sisi kredit.");
            return;
        }

        Date tanggal;
        try {
            tanggal = iso().parse(tanggalTeks);
        } catch (Exception e) {
            tolak(hasil, "Format tanggal tidak dikenali (harap yyyy-MM-dd).");
            return;
        }

        double totalDebet = 0, totalKredit = 0;
        List<long[]> akunPer = new ArrayList<long[]>();
        for (int i = 0; i < baris.length(); i++) {
            JSONObject b = baris.getJSONObject(i);
            long akunId = b.optLong("akunId", 0);
            double d = b.optDouble("debet", 0);
            double k = b.optDouble("kredit", 0);
            if (akunId <= 0) {
                tolak(hasil, "Baris ke-" + (i + 1) + ": akun belum dipilih.");
                return;
            }
            if (d < 0 || k < 0) {
                tolak(hasil, "Baris ke-" + (i + 1) + ": nilai tidak boleh negatif.");
                return;
            }
            if (d > 0 && k > 0) {
                tolak(hasil, "Baris ke-" + (i + 1) + ": satu baris hanya boleh diisi debet ATAU kredit.");
                return;
            }
            if (d == 0 && k == 0) {
                tolak(hasil, "Baris ke-" + (i + 1) + ": nilainya masih nol.");
                return;
            }
            totalDebet += d;
            totalKredit += k;
            akunPer.add(new long[] { akunId });
        }
        if (Math.abs(totalDebet - totalKredit) > 0.005) {
            tolak(hasil, "Jurnal belum seimbang. Total debet "
                    + Common.numberFormat.get().format(totalDebet) + " vs total kredit "
                    + Common.numberFormat.get().format(totalKredit) + " (selisih "
                    + Common.numberFormat.get().format(Math.abs(totalDebet - totalKredit)) + ").");
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Date maxClosing = (Date) session.createCriteria(Closing.class)
                    .setProjection(Projections.max("tanggal")).uniqueResult();
            if (maxClosing != null && tanggal.before(maxClosing)) {
                tolak(hasil, "Tanggal jurnal (" + iso().format(tanggal)
                        + ") sudah masuk periode yang ditutup buku sampai "
                        + iso().format(maxClosing)
                        + ". Gunakan tanggal setelah itu, atau minta bagian keuangan membuka kembali periodenya.");
                return;
            }

            Workspace workspace = null;
            if (workspaceId != 0L) {
                workspace = (Workspace) session.get(Workspace.class, Long.valueOf(workspaceId));
                if (workspace == null) {
                    tolak(hasil, "Mata anggaran yang dipilih tidak ditemukan.");
                    return;
                }
                if (workspace.getTahunWorkspace() != null
                        && workspace.getTahunWorkspace().intValue() != Integer.parseInt(tanggalTeks.substring(0, 4))) {
                    tolak(hasil, "Tahun mata anggaran tidak sama dengan tahun tanggal jurnal.");
                    return;
                }
                Number anak = (Number) session.createQuery(
                        "select count(w.id) from Workspace w where w.parentId = :id and w.id <> :id"
                                + " and (w.carryOver = true or w.aktif is null or w.aktif = true)")
                        .setLong("id", workspaceId).uniqueResult();
                if (anak != null && anak.longValue() > 0L) {
                    tolak(hasil, "Pilih mata anggaran paling rinci (daun), bukan kelompok anggaran.");
                    return;
                }
            }

            GrupTransaksi g;
            if (id > 0) {
                g = (GrupTransaksi) session.get(GrupTransaksi.class, Long.valueOf(id));
                if (g == null) {
                    tolak(hasil, "Jurnal yang hendak diubah tidak ditemukan.");
                    return;
                }
                if (g.getPostingHistory() != null) {
                    tolak(hasil, "Jurnal ini sudah diposting sehingga tidak dapat diubah. "
                            + "Batalkan posting-nya lebih dulu bila memang perlu dikoreksi.");
                    return;
                }
            } else {
                g = new GrupTransaksi();
            }

            JenisTransaksi jt = idJenis > 0
                    ? (JenisTransaksi) session.get(JenisTransaksi.class, Long.valueOf(idJenis)) : null;

            String kode = g.getKode();
            if (kode == null || kode.trim().isEmpty()) {
                kode = buatKode(session, jt, tanggal);
                if (kode == null || kode.trim().isEmpty()) {
                    tolak(hasil, "Nomor jurnal belum dapat dibuat: pilih Jenis Transaksi yang berkode.");
                    return;
                }
                if (kodeSudahDipakai(session, kode, g.getId())) {
                    tolak(hasil, "Nomor jurnal " + kode + " sudah dipakai. Coba simpan ulang.");
                    return;
                }
            }

            String parentCode = "POSJU-" + Long.toHexString(
                    ais.ui.util.WaktuUtil.getDate().getTime()).toUpperCase();

            session.getTransaction().begin();
            try {
                g.setKode(kode);
                g.setTanggalTransaksi(tanggal);
                g.setKeterangan(keterangan);
                g.setJenisJurnal(Transaksi.JURNAL_UMUM);
                g.setJenisTransaksi(jt);
                g.setWorkspace(workspace);
                g.setTotalDebet(Double.valueOf(totalDebet));
                g.setTotalKredit(Double.valueOf(totalKredit));
                g.setTbmuser(tbmuser);
                g.setPegawai(tbmuser == null ? null : tbmuser.ambilPegawai());
                if (g.getParentCode() == null || g.getParentCode().trim().isEmpty()) {
                    g.setParentCode(parentCode);
                }
                Calendar cal = Calendar.getInstance();
                cal.setTime(tanggal);
                g.setBulan(Integer.valueOf(cal.get(Calendar.MONTH) + 1));
                g.setTahun(Integer.valueOf(cal.get(Calendar.YEAR)));
                if (g.getId() == null) {
                    session.save(g);
                } else {
                    // Bersihkan realisasi lama sekarang. AuditListener akan membuat ulang
                    // baris idempotent beberapa detik setelah commit bila workspace tetap dipilih.
                    AnggaranKeuanganUtil.lepaskan(session, "grup_transaksi", g.getId());
                    session.update(g);
                    session.createSQLQuery("DELETE FROM akunting.transaksi WHERE grup_transaksi = "
                            + g.getId().longValue()).executeUpdate();
                }

                for (int i = 0; i < baris.length(); i++) {
                    JSONObject b = baris.getJSONObject(i);
                    Transaksi t = new Transaksi();
                    t.setGrupTransaksi(g);
                    t.setAkun((Akun) session.get(Akun.class, Long.valueOf(b.optLong("akunId", 0))));
                    t.setDebet(Double.valueOf(b.optDouble("debet", 0)));
                    t.setKredit(Double.valueOf(b.optDouble("kredit", 0)));
                    t.setKeterangan(b.optString("keterangan", "").trim().isEmpty() ? keterangan
                            : b.optString("keterangan", "").trim());
                    t.setTanggalTransaksi(tanggal);
                    t.setJenisJurnal(Transaksi.JURNAL_UMUM);
                    t.setJenisTransaksi(jt);
                    t.setKode(kode);
                    t.setParentCode(g.getParentCode());
                    t.setBulan(g.getBulan());
                    t.setTahun(g.getTahun());
                    t.setMerupakanDebet(Boolean.valueOf(b.optDouble("debet", 0) > 0));
                    session.save(t);
                }
                session.getTransaction().commit();
            } catch (Exception e) {
                batalkanDiam(session);
                throw e;
            }

            hasil.put("status", "00");
            hasil.put("id", g.getId());
            hasil.put("kode", kode);
            hasil.put("message", "Jurnal " + kode + " tersimpan sebagai DRAF"
                    + (workspace == null ? "." : " dengan mata anggaran " + workspace.getNama() + ".")
                    + " Tekan Posting agar masuk buku besar dan laporan keuangan.");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== hapus

    private static void hapus(JSONObject payload, JSONObject hasil) throws Exception {
        long id = payload.optLong("id", 0);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            GrupTransaksi g = (GrupTransaksi) session.get(GrupTransaksi.class, Long.valueOf(id));
            if (g == null) {
                tolak(hasil, "Jurnal tidak ditemukan.");
                return;
            }
            if (g.getPostingHistory() != null) {
                tolak(hasil, "Jurnal yang sudah diposting tidak dapat dihapus. Batalkan posting lebih dulu.");
                return;
            }
            if (!Transaksi.JURNAL_UMUM.equals(g.getJenisJurnal())) {
                tolak(hasil, "Hanya jurnal umum yang dapat dihapus dari layar ini.");
                return;
            }
            // Penjaga periode tutup buku. Jalur SIMPAN sudah menolak tanggal sebelum closing
            // terakhir, tetapi jalur hapus ini dulu hanya memeriksa posting history -- padahal
            // jurnal umum diketik manual sehingga LAZIM ber-postingHistory null. Akibatnya entri
            // di dalam periode yang sudah ditutup masih bisa dihapus dan angka periode terkunci
            // ikut berubah. Dua penjagaan: penanda closing pada barisnya, dan tanggalnya terhadap
            // closing terakhir (menangkap baris periode tertutup yang belum sempat bercap).
            if (g.getClosing() != null) {
                tolak(hasil, "Jurnal ini sudah terkunci closing sehingga tidak dapat dihapus. "
                        + "Minta bagian keuangan membuka kembali periodenya lebih dulu.");
                return;
            }
            Date maxClosingHapus = (Date) session.createCriteria(Closing.class)
                    .setProjection(Projections.max("tanggal")).uniqueResult();
            if (maxClosingHapus != null && g.getTanggalTransaksi() != null
                    && g.getTanggalTransaksi().before(maxClosingHapus)) {
                tolak(hasil, "Jurnal bertanggal " + iso().format(g.getTanggalTransaksi())
                        + " berada di periode yang sudah ditutup buku sampai "
                        + iso().format(maxClosingHapus) + " sehingga tidak dapat dihapus. "
                        + "Minta bagian keuangan membuka kembali periodenya lebih dulu.");
                return;
            }
            session.getTransaction().begin();
            try {
                AnggaranKeuanganUtil.lepaskan(session, "grup_transaksi", Long.valueOf(id));
                session.createSQLQuery("DELETE FROM akunting.transaksi WHERE grup_transaksi = " + id)
                        .executeUpdate();
                session.delete(g);
                session.getTransaction().commit();
            } catch (Exception e) {
                batalkanDiam(session);
                throw e;
            }
            hasil.put("status", "00");
            hasil.put("message", "Jurnal dihapus.");
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== posting

    /**
     * Posting = memberi {@code PostingHistory} pada kepala jurnal beserta barisnya, sehingga jurnal
     * ikut terbaca laporan keuangan (semua laporan menyaring {@code posting_history is not null}).
     * Batal posting hanya diizinkan untuk jurnal yang diposting lewat modul ini.
     */
    private static void posting(Tbmuser tbmuser, JSONObject payload, JSONObject hasil, boolean posting)
            throws Exception {
        JSONArray ids = payload.optJSONArray("ids");
        List<Long> daftar = new ArrayList<Long>();
        if (ids != null) {
            for (int i = 0; i < ids.length(); i++) {
                daftar.add(Long.valueOf(ids.optLong(i)));
            }
        } else if (payload.optLong("id", 0) > 0) {
            daftar.add(Long.valueOf(payload.optLong("id", 0)));
        }
        if (daftar.isEmpty()) {
            tolak(hasil, "Tidak ada jurnal yang dipilih.");
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            int berhasil = 0;
            JSONArray masalah = new JSONArray();
            for (int i = 0; i < daftar.size(); i++) {
                Long id = daftar.get(i);
                try {
                    GrupTransaksi g = (GrupTransaksi) session.get(GrupTransaksi.class, id);
                    if (g == null) {
                        masalah.put("#" + id + ": tidak ditemukan.");
                        continue;
                    }
                    if (posting && g.getPostingHistory() != null) {
                        masalah.put(g.getKode() + ": sudah diposting.");
                        continue;
                    }
                    if (!posting && g.getPostingHistory() == null) {
                        masalah.put(g.getKode() + ": memang belum diposting.");
                        continue;
                    }
                    if (!posting && !JENIS_POSTING.equals(g.getPostingHistory().getJenis())) {
                        masalah.put(g.getKode() + ": diposting oleh modul lain ("
                                + g.getPostingHistory().getJenis() + "), batalkan dari modul asalnya.");
                        continue;
                    }
                    double d = g.getTotalDebet() == null ? 0 : g.getTotalDebet().doubleValue();
                    double k = g.getTotalKredit() == null ? 0 : g.getTotalKredit().doubleValue();
                    if (posting && Math.abs(d - k) > 0.005) {
                        masalah.put(g.getKode() + ": tidak seimbang, tidak dapat diposting.");
                        continue;
                    }

                    session.getTransaction().begin();
                    if (posting) {
                        PostingHistory ph = new PostingHistory(JENIS_POSTING);
                        ph.setTanggal(g.getTanggalTransaksi());
                        ph.setTbmuser(tbmuser);
                        ph.setKeterangan("Jurnal Umum " + g.getKode() + " - " + g.getKeterangan());
                        session.save(ph);
                        long idPh = ph.getId().longValue();
                        session.createSQLQuery("UPDATE akunting.grup_transaksi SET posting_history = " + idPh
                                + " WHERE id = " + id.longValue()).executeUpdate();
                        session.createSQLQuery("UPDATE akunting.transaksi SET posting_history = " + idPh
                                + ", tanggal_posting = now() WHERE grup_transaksi = " + id.longValue())
                                .executeUpdate();
                    } else {
                        long idPh = g.getPostingHistory().getId().longValue();
                        session.createSQLQuery("UPDATE akunting.transaksi SET posting_history = NULL,"
                                + " tanggal_posting = NULL WHERE grup_transaksi = " + id.longValue())
                                .executeUpdate();
                        session.createSQLQuery("UPDATE akunting.grup_transaksi SET posting_history = NULL"
                                + " WHERE id = " + id.longValue()).executeUpdate();
                        session.createSQLQuery("DELETE FROM akunting.posting_history WHERE id = " + idPh)
                                .executeUpdate();
                    }
                    session.getTransaction().commit();
                    berhasil++;
                } catch (Exception ex) {
                    batalkanDiam(session);
                    ais.common.ErrorAuditUtil.record(ex, "auto-audit JurnalUmumApiHelper.posting #" + id);
                    masalah.put("#" + id + ": " + ex.getMessage());
                }
            }
            hasil.put("status", "00");
            hasil.put("berhasil", berhasil);
            hasil.put("masalah", masalah);
            hasil.put("message", berhasil + (posting ? " jurnal diposting ke buku besar."
                    : " jurnal dikembalikan menjadi draf.")
                    + (masalah.length() > 0 ? " " + masalah.length() + " dilewati." : ""));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== master pendukung

    private static void jenisTransaksi(JSONObject payload, JSONObject hasil) throws Exception {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Connection conn = session.connection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT j.id, COALESCE(j.kode,''), COALESCE(j.nama,''), COALESCE(j.aktif,true),"
                            + " COALESCE(a.kode,''), COALESCE(a.nama,'')"
                            + " FROM akunting.jenis_transaksi j"
                            + " LEFT JOIN akunting.akun a ON a.id = j.akun"
                            + " WHERE COALESCE(j.aktif,true) = true ORDER BY j.kode, j.nama");
            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("id", rs.getLong(1));
                j.put("kode", rs.getString(2));
                j.put("nama", rs.getString(3));
                j.put("akunKode", rs.getString(5));
                j.put("akunNama", rs.getString(6));
                arr.put(j);
            }
            rs.close();
            ps.close();
            hasil.put("status", "00");
            hasil.put("data", arr);
            hasil.put("tanggalClosing", tanggalClosingTeks(session));
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    // ==================================================================== util

    /** Penomoran meniru {@code TransaksiJurnalUmumHelper.generateCode}: KODE/BULAN/URUT-5-digit. */
    private static String buatKode(Session session, JenisTransaksi jt, Date tanggal) {
        String prefix = jt == null ? "JU" : (jt.getKode() == null || jt.getKode().trim().isEmpty()
                ? "JU" : jt.getKode().trim());
        Calendar cal = Calendar.getInstance();
        cal.setTime(tanggal);
        String bulan = "0" + (cal.get(Calendar.MONTH) + 1);
        bulan = bulan.substring(bulan.length() - 2);
        String prex = prefix + "/" + bulan + "/";
        for (int coba = 0; coba < 50; coba++) {
            int jumlah = ((Number) session.createCriteria(GrupTransaksi.class)
                    .add(Restrictions.ilike("kode", prex, MatchMode.START))
                    .setProjection(Projections.rowCount()).uniqueResult()).intValue();
            String urut = "00000" + (jumlah + 1 + coba);
            String kode = prex + urut.substring(urut.length() - 5);
            if (!kodeSudahDipakai(session, kode, null)) {
                return kode;
            }
        }
        return null;
    }

    private static boolean kodeSudahDipakai(Session session, String kode, Long kecualiId) {
        org.hibernate.Criteria c = session.createCriteria(GrupTransaksi.class)
                .add(Restrictions.eq("kode", kode)).setProjection(Projections.rowCount());
        if (kecualiId != null) {
            c.add(Restrictions.ne("id", kecualiId));
        }
        return ((Number) c.uniqueResult()).intValue() > 0;
    }

    private static String tanggalClosingTeks(Session session) {
        try {
            Date maxClosing = (Date) session.createCriteria(Closing.class)
                    .setProjection(Projections.max("tanggal")).uniqueResult();
            return maxClosing == null ? "" : iso().format(maxClosing);
        } catch (Exception e) {
            return "";
        }
    }

    private static void tolak(JSONObject hasil, String pesan) throws Exception {
        hasil.put("status", "99");
        hasil.put("message", pesan);
    }

    private static void batalkanDiam(Session session) {
        try {
            if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) JurnalUmumApiHelper.batalkanDiam");
        }
    }
}
